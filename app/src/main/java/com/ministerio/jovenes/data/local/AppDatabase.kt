package com.ministerio.jovenes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MemberEntity::class, MeetingEntity::class, AttendanceRecordEntity::class,
        AspectScoreEntity::class, PenaltyTypeEntity::class, AppliedPenaltyEntity::class,
        ChangeLogEntity::class, AdminUserEntity::class, AppSettingsEntity::class],
    version = 1,
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
                    arrayOf("DISOBEY","No obedecer instrucciones",-5)
                ).forEach { p -> db.execSQL("INSERT INTO penalty_types(code,label,points,active) VALUES(?,?,?,1)", p) }
                db.execSQL("INSERT INTO app_settings(id,ministryName,cycleName,majorThreshold,specialThreshold,diplomaThreshold,updatedAt) VALUES(1,?,?,?,?,?,?)", arrayOf("Ministerio de Adolescentes y Jóvenes","Ciclo de 12 encuentros",1100,1000,900,now))
            }
        }).build()
    }
}
