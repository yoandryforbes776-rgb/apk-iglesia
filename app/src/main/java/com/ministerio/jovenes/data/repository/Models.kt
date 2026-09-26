package com.ministerio.jovenes.data.repository

import com.ministerio.jovenes.data.local.*

data class RecordDraft(
    val attended: Boolean = false,
    val word: Boolean = false,
    val worship: Boolean = false,
    val standing: Boolean = false,
    val answers: Boolean = false,
    val games: Boolean = false,
    val punctuality: Boolean = false,
    val penalties: Set<String> = emptySet(),
    val notes: String = ""
) {
    fun aspects() = linkedMapOf(
        "ATTENDANCE" to attended, "WORD" to word, "WORSHIP" to worship,
        "STANDING" to standing, "ANSWERS" to answers, "GAMES" to games,
        "PUNCTUALITY" to punctuality
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
    val history: List<ChangeLogEntity> = emptyList()
) {
    fun recordFor(memberId: Long, meetingId: Int) = records.firstOrNull { it.memberId == memberId && it.meetingId == meetingId }
    fun score(recordId: Long): Int {
        val positive = scores.filter { it.recordId == recordId }.sumOf { it.points }
        val codes = applied.filter { it.recordId == recordId }.map { it.penaltyCode }.toSet()
        return (positive + penaltyTypes.filter { it.code in codes }.sumOf { it.points }).coerceIn(0, 100)
    }
    fun breakdown(recordId: Long) = scores.filter { it.recordId == recordId }
    fun penaltyBreakdown(recordId: Long) = applied.filter { it.recordId == recordId }.mapNotNull { a -> penaltyTypes.find { it.code == a.penaltyCode } }
    fun progress(member: MemberEntity): MemberProgress {
        val mine = records.filter { it.memberId == member.id }
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
