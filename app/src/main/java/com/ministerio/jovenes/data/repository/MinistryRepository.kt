package com.ministerio.jovenes.data.repository

import androidx.room.withTransaction
import com.ministerio.jovenes.data.local.*
import com.ministerio.jovenes.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.UUID

class MinistryRepository(private val db: AppDatabase) {
    private val dao = db.dao()

    val snapshot = combine(
        combine(dao.observeMembers(), dao.observeMeetings(), dao.observeRecords(), dao.observeScores()) { a,b,c,d -> arrayOf(a,b,c,d) },
        combine(dao.observePenaltyTypes(), dao.observeAppliedPenalties(), dao.observeSettings(), dao.observeHistory()) { a,b,c,d -> arrayOf(a,b,c,d) }
    ) { first, second ->
        @Suppress("UNCHECKED_CAST")
        AppSnapshot(
            first[0] as List<MemberEntity>, first[1] as List<MeetingEntity>, first[2] as List<AttendanceRecordEntity>, first[3] as List<AspectScoreEntity>,
            second[0] as List<PenaltyTypeEntity>, second[1] as List<AppliedPenaltyEntity>, (second[2] as AppSettingsEntity?) ?: AppSettingsEntity(), second[3] as List<ChangeLogEntity>
        )
    }

    suspend fun ensureAdmin() {
        if (dao.adminCount() == 0) {
            val salt = PasswordHasher.newSalt()
            dao.insertAdmin(AdminUserEntity(username = "admin", displayName = "Líder", salt = salt, passwordHash = PasswordHasher.hash("Admin123!", salt)))
        }
    }

    suspend fun login(username: String, password: String): Boolean {
        ensureAdmin()
        val admin = dao.admin(username.trim()) ?: return false
        val ok = PasswordHasher.verify(password, admin.salt, admin.passwordHash)
        if (ok) dao.updateAdmin(admin.copy(lastLoginAt = System.currentTimeMillis()))
        return ok
    }

    suspend fun changePassword(current: String, replacement: String): Boolean {
        val admin = dao.admin("admin") ?: return false
        if (!PasswordHasher.verify(current, admin.salt, admin.passwordHash)) return false
        val salt = PasswordHasher.newSalt()
        dao.updateAdmin(admin.copy(salt = salt, passwordHash = PasswordHasher.hash(replacement, salt)))
        dao.log(ChangeLogEntity(entityType="ADMIN", entityId=admin.id.toString(), action="PASSWORD", summary="Contraseña actualizada"))
        return true
    }

    suspend fun saveMember(existing: MemberEntity?, name: String, photo: String?, birth: String?, group: String?) = db.withTransaction {
        val now = System.currentTimeMillis()
        if (existing == null) {
            val id = dao.insertMember(MemberEntity(fullName=name.trim(), photoUri=photo, birthDate=birth?.ifBlank { null }, groupName=group?.ifBlank { null }, syncId=UUID.randomUUID().toString()))
            dao.log(ChangeLogEntity(entityType="MEMBER", entityId=id.toString(), action="CREATE", summary="Miembro creado: ${name.trim()}"))
        } else {
            dao.updateMember(existing.copy(fullName=name.trim(), photoUri=photo, birthDate=birth?.ifBlank { null }, groupName=group?.ifBlank { null }, updatedAt=now))
            dao.log(ChangeLogEntity(entityType="MEMBER", entityId=existing.id.toString(), action="UPDATE", summary="Miembro actualizado: ${name.trim()}"))
        }
    }

    suspend fun deleteMember(member: MemberEntity) = db.withTransaction {
        dao.recordsSnapshot().filter { it.memberId==member.id }.mapNotNull { it.syncId }
            .forEach { dao.saveDeletion(SyncDeletionEntity(it,"RECORD")) }
        member.syncId?.let { dao.saveDeletion(SyncDeletionEntity(it,"MEMBER")) }
        dao.deleteMember(member)
        dao.log(ChangeLogEntity(entityType="MEMBER", entityId=member.id.toString(), action="DELETE", summary="Miembro eliminado: ${member.fullName}"))
    }

    suspend fun setMemberActive(member: MemberEntity, active: Boolean) = db.withTransaction {
        dao.updateMember(member.copy(active=active, updatedAt=System.currentTimeMillis()))
        dao.log(ChangeLogEntity(entityType="MEMBER", entityId=member.id.toString(), action=if(active) "RESTORE" else "ARCHIVE", summary="${member.fullName}: ${if(active) "activo" else "archivado"}"))
    }

    suspend fun saveRecord(memberId: Long, meetingId: Int, draft: RecordDraft) = db.withTransaction {
        val existing = dao.record(memberId, meetingId)
        val safe = if (draft.attended) draft else RecordDraft(attended=false, notes=draft.notes)
        val record = AttendanceRecordEntity(
            id=existing?.id ?: 0, memberId=memberId, meetingId=meetingId, attended=safe.attended,
            notes=safe.notes.trim(), createdAt=existing?.createdAt ?: System.currentTimeMillis(), updatedAt=System.currentTimeMillis(),
            syncId=existing?.syncId ?: UUID.randomUUID().toString()
        )
        val id = dao.upsertRecord(record)
        val finalId = if (record.id == 0L) id else record.id
        dao.deleteScores(finalId)
        dao.deletePenalties(finalId)
        val scores = safe.aspectScores().map { (aspect, points) ->
            AspectScoreEntity(recordId=finalId, aspect=aspect, achieved=points > 0, points=points)
        }
        dao.upsertScores(scores)
        if (safe.attended) dao.upsertAppliedPenalties(safe.penalties.map { AppliedPenaltyEntity(finalId, it) })
        dao.log(ChangeLogEntity(entityType="RECORD", entityId=finalId.toString(), action=if(existing==null) "CREATE" else "UPDATE", summary="Encuentro $meetingId: ${if(safe.attended) "asistió" else "ausente"}"))
    }

    suspend fun deleteRecord(memberId: Long, meetingId: Int) = db.withTransaction {
        dao.record(memberId, meetingId)?.let {
            it.syncId?.let { syncId -> dao.saveDeletion(SyncDeletionEntity(syncId,"RECORD")) }
            dao.deleteRecord(it.id)
            dao.log(ChangeLogEntity(entityType="RECORD", entityId=it.id.toString(), action="DELETE", summary="Registro del encuentro $meetingId eliminado"))
        }
    }

    suspend fun saveSettings(settings: AppSettingsEntity) {
        dao.saveSettings(settings.copy(updatedAt=System.currentTimeMillis()))
        dao.log(ChangeLogEntity(entityType="SETTINGS", entityId="1", action="UPDATE", summary="Configuración general actualizada"))
    }

    suspend fun syncWithSupabase(password: String): SyncResult = withContext(Dispatchers.IO) {
        val settings=dao.settingsSnapshot() ?: AppSettingsEntity()
        require(settings.supabaseUrl.startsWith("https://")) { "La URL de Supabase debe comenzar con https://" }
        require(settings.supabaseAnonKey.isNotBlank() && settings.supabaseEmail.isNotBlank() && settings.syncWorkspace.isNotBlank()) { "Completa la configuración de Supabase" }
        require(password.isNotBlank()) { "Escribe la contraseña de Supabase" }
        val service=SupabaseSyncService()
        val token=service.authenticate(settings,password)

        db.withTransaction {
            dao.membersSnapshot().filter { it.syncId.isNullOrBlank() }.forEach { dao.updateMember(it.copy(syncId=UUID.randomUUID().toString())) }
            dao.recordsSnapshot().filter { it.syncId.isNullOrBlank() }.forEach { dao.upsertRecord(it.copy(syncId=UUID.randomUUID().toString())) }
        }

        val localDeletions=dao.deletionsSnapshot()
        service.pushDeletions(settings,token,localDeletions)
        val remoteDeletions=service.pullDeletions(settings,token)
        db.withTransaction {
            remoteDeletions.forEach { deleted ->
                dao.saveDeletion(SyncDeletionEntity(deleted.syncId,deleted.entityType,deleted.deletedAt))
                if(deleted.entityType=="MEMBER") dao.memberBySyncId(deleted.syncId)?.let { dao.deleteMember(it) }
                else dao.recordBySyncId(deleted.syncId)?.let { dao.deleteRecord(it.id) }
            }
        }
        val deletedIds=(localDeletions.map { it.syncId }+remoteDeletions.map { it.syncId }).toSet()
        val remoteMembers=service.pullMembers(settings,token).filterNot { it.syncId in deletedIds }
        val remoteRecords=service.pullRecords(settings,token).filterNot { it.syncId in deletedIds }

        db.withTransaction {
            remoteMembers.forEach { remote ->
                val local=dao.memberBySyncId(remote.syncId)
                if(local==null) dao.insertMember(MemberEntity(fullName=remote.fullName,birthDate=remote.birthDate,groupName=remote.groupName,active=remote.active,createdAt=remote.createdAt,updatedAt=remote.updatedAt,syncId=remote.syncId))
                else if(remote.updatedAt>local.updatedAt) dao.updateMember(local.copy(fullName=remote.fullName,birthDate=remote.birthDate,groupName=remote.groupName,active=remote.active,createdAt=remote.createdAt,updatedAt=remote.updatedAt))
            }
            remoteRecords.forEach { remote ->
                val member=dao.memberBySyncId(remote.memberSyncId) ?: return@forEach
                val local=dao.recordBySyncId(remote.syncId) ?: dao.record(member.id,remote.meetingId)
                if(local==null || remote.updatedAt>local.updatedAt) {
                    val id=dao.upsertRecord(AttendanceRecordEntity(id=local?.id ?: 0,memberId=member.id,meetingId=remote.meetingId,attended=remote.attended,notes=remote.notes,createdAt=remote.createdAt,updatedAt=remote.updatedAt,rubricVersion=remote.rubricVersion,syncId=remote.syncId))
                    val recordId=local?.id ?: id
                    dao.deleteScores(recordId); dao.deletePenalties(recordId)
                    dao.upsertScores(remote.scores.map { (aspect,points) -> AspectScoreEntity(recordId=recordId,aspect=aspect,achieved=points>0,points=points) })
                    dao.upsertAppliedPenalties(remote.penalties.map { AppliedPenaltyEntity(recordId,it) })
                } else if(local.syncId!=remote.syncId) {
                    // Unifica el identificador cuando dos dispositivos crearon el mismo encuentro offline.
                    dao.upsertRecord(local.copy(syncId=remote.syncId))
                }
            }
        }

        val finalData=AppSnapshot(
            members=dao.membersSnapshot(),meetings=dao.meetingsSnapshot(),records=dao.recordsSnapshot(),scores=dao.scoresSnapshot(),
            penaltyTypes=dao.penaltyTypesSnapshot(),applied=dao.appliedSnapshot(),settings=settings
        )
        service.pushMembers(settings,token,finalData.members)
        service.pushRecords(settings,token,finalData)
        val completed=System.currentTimeMillis()
        dao.saveSettings(settings.copy(lastSyncAt=completed))
        dao.log(ChangeLogEntity(entityType="SYNC",entityId=settings.syncWorkspace,action="SYNC",summary="Sincronización con Supabase completada"))
        SyncResult(finalData.members.size,finalData.records.size,completed)
    }
}
