package com.ministerio.jovenes.data.repository

import androidx.room.withTransaction
import com.ministerio.jovenes.data.local.*
import com.ministerio.jovenes.util.PasswordHasher
import kotlinx.coroutines.flow.combine

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
            val id = dao.insertMember(MemberEntity(fullName=name.trim(), photoUri=photo, birthDate=birth?.ifBlank { null }, groupName=group?.ifBlank { null }))
            dao.log(ChangeLogEntity(entityType="MEMBER", entityId=id.toString(), action="CREATE", summary="Miembro creado: ${name.trim()}"))
        } else {
            dao.updateMember(existing.copy(fullName=name.trim(), photoUri=photo, birthDate=birth?.ifBlank { null }, groupName=group?.ifBlank { null }, updatedAt=now))
            dao.log(ChangeLogEntity(entityType="MEMBER", entityId=existing.id.toString(), action="UPDATE", summary="Miembro actualizado: ${name.trim()}"))
        }
    }

    suspend fun deleteMember(member: MemberEntity) = db.withTransaction {
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
            notes=safe.notes.trim(), createdAt=existing?.createdAt ?: System.currentTimeMillis(), updatedAt=System.currentTimeMillis()
        )
        val id = dao.upsertRecord(record)
        val finalId = if (record.id == 0L) id else record.id
        dao.deleteScores(finalId)
        dao.deletePenalties(finalId)
        val scores = safe.aspects().map { (aspect, achieved) ->
            AspectScoreEntity(recordId=finalId, aspect=aspect, achieved=achieved, points=if(achieved) ASPECT_POINTS.getValue(aspect) else 0)
        }
        dao.upsertScores(scores)
        if (safe.attended) dao.upsertAppliedPenalties(safe.penalties.map { AppliedPenaltyEntity(finalId, it) })
        dao.log(ChangeLogEntity(entityType="RECORD", entityId=finalId.toString(), action=if(existing==null) "CREATE" else "UPDATE", summary="Encuentro $meetingId: ${if(safe.attended) "asistió" else "ausente"}"))
    }

    suspend fun deleteRecord(memberId: Long, meetingId: Int) = db.withTransaction {
        dao.record(memberId, meetingId)?.let {
            dao.deleteRecord(it.id)
            dao.log(ChangeLogEntity(entityType="RECORD", entityId=it.id.toString(), action="DELETE", summary="Registro del encuentro $meetingId eliminado"))
        }
    }

    suspend fun saveSettings(settings: AppSettingsEntity) {
        dao.saveSettings(settings.copy(updatedAt=System.currentTimeMillis()))
        dao.log(ChangeLogEntity(entityType="SETTINGS", entityId="1", action="UPDATE", summary="Configuración general actualizada"))
    }
}
