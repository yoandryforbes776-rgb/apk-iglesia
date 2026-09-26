package com.ministerio.jovenes

import com.ministerio.jovenes.data.local.*
import com.ministerio.jovenes.data.repository.AppSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringTest {
    @Test fun `meeting score is capped at one hundred`() {
        val record=AttendanceRecordEntity(id=1,memberId=1,meetingId=1,attended=true)
        val scores=ASPECT_POINTS.map { AspectScoreEntity(recordId=1,aspect=it.key,achieved=true,points=it.value) }
        assertEquals(100,AppSnapshot(records=listOf(record),scores=scores).score(1))
    }
    @Test fun `penalties cannot make result negative`() {
        val record=AttendanceRecordEntity(id=1,memberId=1,meetingId=1,attended=true)
        val penalty=PenaltyTypeEntity("FIGHT","Pelea",-20)
        val applied=AppliedPenaltyEntity(1,"FIGHT")
        assertEquals(0,AppSnapshot(records=listOf(record),penaltyTypes=listOf(penalty),applied=listOf(applied)).score(1))
    }
}
