package com.ministerio.jovenes.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MinistryDao {
    @Query("SELECT * FROM members ORDER BY active DESC, fullName COLLATE NOCASE")
    fun observeMembers(): Flow<List<MemberEntity>>
    @Query("SELECT * FROM meetings ORDER BY number")
    fun observeMeetings(): Flow<List<MeetingEntity>>
    @Query("SELECT * FROM attendance_records")
    fun observeRecords(): Flow<List<AttendanceRecordEntity>>
    @Query("SELECT * FROM aspect_scores")
    fun observeScores(): Flow<List<AspectScoreEntity>>
    @Query("SELECT * FROM penalty_types WHERE active = 1 ORDER BY points")
    fun observePenaltyTypes(): Flow<List<PenaltyTypeEntity>>
    @Query("SELECT * FROM applied_penalties")
    fun observeAppliedPenalties(): Flow<List<AppliedPenaltyEntity>>
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>
    @Query("SELECT * FROM change_history ORDER BY timestamp DESC LIMIT 100")
    fun observeHistory(): Flow<List<ChangeLogEntity>>

    @Query("SELECT * FROM attendance_records WHERE memberId=:memberId AND meetingId=:meetingId LIMIT 1")
    suspend fun record(memberId: Long, meetingId: Int): AttendanceRecordEntity?
    @Query("SELECT * FROM admin_users WHERE username=:username AND active=1 LIMIT 1")
    suspend fun admin(username: String): AdminUserEntity?
    @Query("SELECT COUNT(*) FROM admin_users")
    suspend fun adminCount(): Int

    @Insert suspend fun insertMember(member: MemberEntity): Long
    @Update suspend fun updateMember(member: MemberEntity)
    @Delete suspend fun deleteMember(member: MemberEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertRecord(record: AttendanceRecordEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertScores(scores: List<AspectScoreEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAppliedPenalties(items: List<AppliedPenaltyEntity>)
    @Insert suspend fun log(item: ChangeLogEntity)
    @Insert suspend fun insertAdmin(admin: AdminUserEntity): Long
    @Update suspend fun updateAdmin(admin: AdminUserEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSettings(settings: AppSettingsEntity)

    @Query("DELETE FROM aspect_scores WHERE recordId=:recordId") suspend fun deleteScores(recordId: Long)
    @Query("DELETE FROM applied_penalties WHERE recordId=:recordId") suspend fun deletePenalties(recordId: Long)
    @Query("DELETE FROM attendance_records WHERE id=:recordId") suspend fun deleteRecord(recordId: Long)

    @Query("SELECT * FROM members ORDER BY fullName") suspend fun membersSnapshot(): List<MemberEntity>
    @Query("SELECT * FROM meetings ORDER BY number") suspend fun meetingsSnapshot(): List<MeetingEntity>
    @Query("SELECT * FROM attendance_records") suspend fun recordsSnapshot(): List<AttendanceRecordEntity>
    @Query("SELECT * FROM aspect_scores") suspend fun scoresSnapshot(): List<AspectScoreEntity>
    @Query("SELECT * FROM penalty_types") suspend fun penaltyTypesSnapshot(): List<PenaltyTypeEntity>
    @Query("SELECT * FROM applied_penalties") suspend fun appliedSnapshot(): List<AppliedPenaltyEntity>
    @Query("SELECT * FROM app_settings WHERE id=1") suspend fun settingsSnapshot(): AppSettingsEntity?
}
