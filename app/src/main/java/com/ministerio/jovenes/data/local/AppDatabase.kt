package com.ministerio.jovenes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MemberEntity::class, MeetingEntity::class, AttendanceRecordEntity::class,
        AspectScoreEntity::class, PenaltyTypeEntity::class, AppliedPenaltyEntity::class,
        ChangeLogEntity::class, AdminUserEntity::class, AppSettingsEntity::class, SyncDeletionEntity::class, CycleEntity::class, MeetingPlanEntity::class],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): MinistryDao
    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context, AppDatabase::class.java, "ministerio_jovenes.db"
        ).addCallback(object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                (1..12).forEach { n ->
                    db.execSQL("INSERT INTO meetings(id,number,title,scheduledDate,createdAt,updatedAt) VALUES(?,?,?,NULL,?,?)", arrayOf(n,n,"Encuentro $n",now,now))
                }
                listOf(
                    arrayOf("LATE_LIGHT","Llegar tarde leve",-5),
                    arrayOf("LATE_MODERATE","Llegar tarde moderado",-10),
                    arrayOf("VERY_LATE","Llegar muy tarde",-15),
                    arrayOf("BAD_ATTITUDE","Mala actitud",-10),
                    arrayOf("DISRESPECT","Irrespeto o interrupciones",-15),
                    arrayOf("FIGHT","Pelea o agresión",-20),
                    arrayOf("PHONE","Uso inapropiado del celular",-5),
                    arrayOf("DISOBEY","No obedecer instrucciones",-5),
                    arrayOf("PRAY_EYES_OPEN","Orar con los ojos abiertos",-5),
                    arrayOf("TALK_DISTRACTED","Hablar en clase o entretenerse",-10),
                    arrayOf("BAD_GAME_BEHAVIOR","Mal comportamiento en los juegos",-10),
                    arrayOf("NO_WORSHIP_BEHAVIOR","Hablar o no adorar/alabar durante la alabanza",-10),
                    arrayOf("SIT_DURING_WORSHIP","Sentarse mientras se alaba a Dios",-5)
                ).forEach { p -> db.execSQL("INSERT INTO penalty_types(code,label,points,active) VALUES(?,?,?,1)", p) }
                db.execSQL("INSERT INTO app_settings(id,ministryName,cycleName,majorThreshold,specialThreshold,diplomaThreshold,updatedAt) VALUES(1,?,?,?,?,?,?)", arrayOf("Ministerio de Adolescentes y Jóvenes","Ciclo de 12 encuentros",1100,1000,900,now))
                db.execSQL("UPDATE app_settings SET supabaseUrl=?, supabaseAnonKey=? WHERE id=1", arrayOf(DEFAULT_SUPABASE_URL,DEFAULT_SUPABASE_PUBLISHABLE_KEY))
                db.execSQL("INSERT INTO cycles(id,name,status,createdAt,updatedAt) VALUES('default-cycle','Ciclo de 12 encuentros','ACTIVE',?,?)",arrayOf(now,now))
            }
        }).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN rubricVersion INTEGER NOT NULL DEFAULT 2")
                db.execSQL("UPDATE aspect_scores SET points = CASE aspect WHEN 'ATTENDANCE' THEN CASE WHEN achieved=1 THEN 15 ELSE 0 END WHEN 'WORD' THEN CASE WHEN achieved=1 THEN 10 ELSE 0 END WHEN 'WORSHIP' THEN CASE WHEN achieved=1 THEN 10 ELSE 0 END WHEN 'STANDING' THEN CASE WHEN achieved=1 THEN 5 ELSE 0 END WHEN 'ANSWERS' THEN CASE WHEN achieved=1 THEN 15 ELSE 0 END WHEN 'GAMES' THEN CASE WHEN achieved=1 THEN 15 ELSE 0 END ELSE points END")
                val penalties = listOf(
                    arrayOf("PRAY_EYES_OPEN", "Orar con los ojos abiertos", -5),
                    arrayOf("TALK_DISTRACTED", "Hablar en clase o entretenerse", -10),
                    arrayOf("BAD_GAME_BEHAVIOR", "Mal comportamiento en los juegos", -10),
                    arrayOf("NO_WORSHIP_BEHAVIOR", "Hablar o no adorar/alabar durante la alabanza", -10),
                    arrayOf("SIT_DURING_WORSHIP", "Sentarse mientras se alaba a Dios", -5)
                )
                penalties.forEach { db.execSQL("INSERT OR IGNORE INTO penalty_types(code,label,points,active) VALUES(?,?,?,1)", it) }
                db.execSQL("UPDATE attendance_records SET rubricVersion=2")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE members ADD COLUMN syncId TEXT")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN syncId TEXT")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN supabaseUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN supabaseAnonKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN supabaseEmail TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN syncWorkspace TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN lastSyncAt INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_deletions (syncId TEXT NOT NULL PRIMARY KEY, entityType TEXT NOT NULL, deletedAt INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE app_settings SET supabaseUrl=? WHERE id=1 AND supabaseUrl=''", arrayOf(DEFAULT_SUPABASE_URL))
                db.execSQL("UPDATE app_settings SET supabaseAnonKey=? WHERE id=1 AND supabaseAnonKey=''", arrayOf(DEFAULT_SUPABASE_PUBLISHABLE_KEY))
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN supabaseChurchId TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE admin_users ADD COLUMN mustChangePassword INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now=System.currentTimeMillis()
                db.execSQL("CREATE TABLE IF NOT EXISTS cycles (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, startDate TEXT, endDate TEXT, status TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("INSERT OR IGNORE INTO cycles(id,name,status,createdAt,updatedAt) SELECT 'default-cycle',cycleName,'ACTIVE',updatedAt,updatedAt FROM app_settings WHERE id=1")
                db.execSQL("CREATE TABLE IF NOT EXISTS meeting_plans (cycleId TEXT NOT NULL, meetingId INTEGER NOT NULL, title TEXT NOT NULL, scheduledDate TEXT, bibleTheme TEXT, leaderName TEXT, activity TEXT, updatedAt INTEGER NOT NULL, PRIMARY KEY(cycleId,meetingId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meeting_plans_cycleId ON meeting_plans(cycleId)")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN cycleId TEXT NOT NULL DEFAULT 'default-cycle'")
                db.execSQL("DROP INDEX IF EXISTS index_attendance_records_memberId_meetingId")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_attendance_records_memberId_meetingId_cycleId ON attendance_records(memberId,meetingId,cycleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_records_cycleId ON attendance_records(cycleId)")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN activeCycleId TEXT NOT NULL DEFAULT 'default-cycle'")
            }
        }
    }
}
