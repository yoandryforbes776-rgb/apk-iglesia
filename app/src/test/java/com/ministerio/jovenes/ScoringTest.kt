package com.ministerio.jovenes

import com.ministerio.jovenes.data.local.*
import com.ministerio.jovenes.data.repository.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringTest {
    @Test fun `complete new rubric totals one hundred`() {
        val draft=RecordDraft(attended=true,attentive=true,word=true,prayer=true,worship=true,
            standing=true,answers=true,gameParticipation=GameParticipation.PARTICIPATES,punctuality=true)
        assertEquals(100,draft.aspectScores().values.sum())
    }
    @Test fun `respectful member who does not want to play receives five points`() {
        assertEquals(5,RecordDraft(attended=true,gameParticipation=GameParticipation.RESPECTFUL_NO_PLAY).aspectScores()["GAMES"])
    }
    @Test fun `penalties cannot make result negative`() {
        val record=AttendanceRecordEntity(id=1,memberId=1,meetingId=1,attended=true)
        val penalty=PenaltyTypeEntity("FIGHT","Pelea",-20)
        assertEquals(0,AppSnapshot(records=listOf(record),penaltyTypes=listOf(penalty),applied=listOf(AppliedPenaltyEntity(1,"FIGHT"))).score(1))
    }
}
