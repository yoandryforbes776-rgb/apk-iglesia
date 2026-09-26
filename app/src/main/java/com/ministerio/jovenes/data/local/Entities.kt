package com.ministerio.jovenes.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "members", indices = [Index("fullName")])
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val photoUri: String? = null,
    val birthDate: String? = null,
    val groupName: String? = null,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String? = null
)

@Entity(tableName = "meetings", indices = [Index(value = ["number"], unique = true)])
data class MeetingEntity(
    @PrimaryKey val id: Int,
    val number: Int,
    val title: String,
    val scheduledDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(entity = MemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MeetingEntity::class, parentColumns = ["id"], childColumns = ["meetingId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["memberId", "meetingId"], unique = true), Index("meetingId")]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val meetingId: Int,
    val attended: Boolean,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modifiedBy: Long = 1,
    @ColumnInfo(defaultValue = "2") val rubricVersion: Int = 2,
    val syncId: String? = null
)

@Entity(
    tableName = "aspect_scores",
    foreignKeys = [ForeignKey(entity = AttendanceRecordEntity::class, parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["recordId", "aspect"], unique = true)]
)
data class AspectScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val aspect: String,
    val achieved: Boolean,
    val points: Int
)

@Entity(tableName = "penalty_types")
data class PenaltyTypeEntity(
    @PrimaryKey val code: String,
    val label: String,
    val points: Int,
    val active: Boolean = true
)

@Entity(
    tableName = "applied_penalties",
    primaryKeys = ["recordId", "penaltyCode"],
    foreignKeys = [
        ForeignKey(entity = AttendanceRecordEntity::class, parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PenaltyTypeEntity::class, parentColumns = ["code"], childColumns = ["penaltyCode"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("penaltyCode")]
)
data class AppliedPenaltyEntity(val recordId: Long, val penaltyCode: String)

@Entity(tableName = "change_history", indices = [Index("entityType"), Index("timestamp")])
data class ChangeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val action: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val adminId: Long = 1
)

@Entity(tableName = "admin_users", indices = [Index(value = ["username"], unique = true)])
data class AdminUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val salt: String,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val ministryName: String = "Ministerio de Adolescentes y Jóvenes",
    val cycleName: String = "Ciclo de 12 encuentros",
    val majorThreshold: Int = 1100,
    val specialThreshold: Int = 1000,
    val diplomaThreshold: Int = 900,
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val supabaseUrl: String = "",
    @ColumnInfo(defaultValue = "''") val supabaseAnonKey: String = "",
    @ColumnInfo(defaultValue = "''") val supabaseEmail: String = "",
    @ColumnInfo(defaultValue = "''") val syncWorkspace: String = "",
    val lastSyncAt: Long? = null
)

@Entity(tableName = "sync_deletions")
data class SyncDeletionEntity(
    @PrimaryKey val syncId: String,
    val entityType: String,
    val deletedAt: Long = System.currentTimeMillis()
)

val ASPECT_POINTS = linkedMapOf(
    "ATTENDANCE" to 15,
    "ATTENTIVE" to 10,
    "WORD" to 10,
    "PRAYER" to 10,
    "WORSHIP" to 10,
    "STANDING" to 5,
    "ANSWERS" to 15,
    "GAMES" to 15,
    "PUNCTUALITY" to 10
)
