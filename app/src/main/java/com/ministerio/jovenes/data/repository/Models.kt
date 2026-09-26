package com.ministerio.jovenes.data.repository

import com.ministerio.jovenes.data.local.*

enum class GameParticipation(val points: Int) {
    NONE(0), RESPECTFUL_NO_PLAY(5), PARTICIPATES(15)
}

data class RecordDraft(
    val attended: Boolean = false,
    val attentive: Boolean = false,
    val word: Boolean = false,
    val prayer: Boolean = false,
    val worship: Boolean = false,
    val standing: Boolean = false,
    val answers: Boolean = false,
    val gameParticipation: GameParticipation = GameParticipation.NONE,
    val punctuality: Boolean = false,
    val penalties: Set<String> = emptySet(),
    val notes: String = ""
) {
    fun aspectScores() = linkedMapOf(
        "ATTENDANCE" to if (attended) ASPECT_POINTS.getValue("ATTENDANCE") else 0,
        "ATTENTIVE" to if (attentive) ASPECT_POINTS.getValue("ATTENTIVE") else 0,
        "WORD" to if (word) ASPECT_POINTS.getValue("WORD") else 0,
        "PRAYER" to if (prayer) ASPECT_POINTS.getValue("PRAYER") else 0,
        "WORSHIP" to if (worship) ASPECT_POINTS.getValue("WORSHIP") else 0,
        "STANDING" to if (standing) ASPECT_POINTS.getValue("STANDING") else 0,
        "ANSWERS" to if (answers) ASPECT_POINTS.getValue("ANSWERS") else 0,
        "GAMES" to if (attended) gameParticipation.points else 0,
        "PUNCTUALITY" to if (punctuality) ASPECT_POINTS.getValue("PUNCTUALITY") else 0
    )
}

data class MemberProgress(
    val member: MemberEntity,
    val total: Int,
    val meetingsCompleted: Int,
    val attendedCount: Int,
    val possible: Int = 1200
)

data class AppSnapshot(
    val members: List<MemberEntity> = emptyList(),
    val meetings: List<MeetingEntity> = emptyList(),
    val records: List<AttendanceRecordEntity> = emptyList(),
    val scores: List<AspectScoreEntity> = emptyList(),
    val penaltyTypes: List<PenaltyTypeEntity> = emptyList(),
    val applied: List<AppliedPenaltyEntity> = emptyList(),
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val history: List<ChangeLogEntity> = emptyList(),
    val cycles: List<CycleEntity> = emptyList(),
    val meetingPlans: List<MeetingPlanEntity> = emptyList()
) {
    val activeCycle get() = cycles.find { it.id==settings.activeCycleId }
    fun activeRecords() = records.filter { it.cycleId==settings.activeCycleId }
    fun recordFor(memberId: Long, meetingId: Int) = activeRecords().firstOrNull { it.memberId == memberId && it.meetingId == meetingId }
    fun score(recordId: Long): Int {
        val positive = scores.filter { it.recordId == recordId }.sumOf { it.points }
        val codes = applied.filter { it.recordId == recordId }.map { it.penaltyCode }.toSet()
        return (positive + penaltyTypes.filter { it.code in codes }.sumOf { it.points }).coerceIn(0, 100)
    }
    fun breakdown(recordId: Long) = scores.filter { it.recordId == recordId }
    fun penaltyBreakdown(recordId: Long) = applied.filter { it.recordId == recordId }.mapNotNull { a -> penaltyTypes.find { it.code == a.penaltyCode } }
    fun progress(member: MemberEntity): MemberProgress {
        val mine = activeRecords().filter { it.memberId == member.id }
        return MemberProgress(member, mine.sumOf { score(it.id) }, mine.size, mine.count { it.attended })
    }
    fun ranking() = members.filter { it.active }.map(::progress).sortedWith(compareByDescending<MemberProgress> { it.total }.thenBy { it.member.fullName })
    fun reward(total: Int, attendance: Int): String = when {
        total >= settings.majorThreshold && attendance == 12 -> "Premio mayor"
        total >= settings.specialThreshold -> "Reconocimiento especial"
        total >= settings.diplomaThreshold -> "Diploma de participación"
        else -> "Seguimiento y motivación"
    }
}
