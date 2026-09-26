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
        ChangeLogEntity::class, AdminUserEntity::class, AppSettingsEntity::class],
    version = 2,
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
            }
        }).addMigrations(MIGRATION_1_2).build()

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
    }
}
