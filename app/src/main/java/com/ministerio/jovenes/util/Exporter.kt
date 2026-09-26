package com.ministerio.jovenes.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.ministerio.jovenes.data.repository.AppSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object Exporter {
    private fun csvCell(value: Any?) = "\"${(value ?: "").toString().replace("\"", "\"\"")}\""
    fun writeCsv(data: AppSnapshot, output: OutputStream) = output.bufferedWriter().use { writer ->
        writer.appendLine("Miembro,Grupo,Encuentros registrados,Asistencias,Puntos,Reconocimiento")
        data.ranking().forEach { p ->
            writer.appendLine(listOf(p.member.fullName,p.member.groupName,p.meetingsCompleted,p.attendedCount,p.total,data.reward(p.total,p.attendedCount)).joinToString(",", transform=::csvCell))
        }
        writer.appendLine()
        writer.appendLine("Miembro,Encuentro,Asistencia,Puntos,Penalizaciones,Notas,Modificado")
        data.members.forEach { member -> data.records.filter { it.memberId==member.id }.sortedBy { it.meetingId }.forEach { r ->
            val penalties=data.penaltyBreakdown(r.id).joinToString("; ") { "${it.label} (${it.points})" }
            writer.appendLine(listOf(member.fullName,r.meetingId,if(r.attended) "Sí" else "No",data.score(r.id),penalties,r.notes,formatDate(r.updatedAt)).joinToString(",",transform=::csvCell))
        }}
    }

    fun writeBackup(data: AppSnapshot, output: OutputStream) {
        val root=JSONObject().put("format","impulso-joven-backup-v1").put("exportedAt",System.currentTimeMillis())
        root.put("settings",JSONObject().put("ministryName",data.settings.ministryName).put("cycleName",data.settings.cycleName)
            .put("majorThreshold",data.settings.majorThreshold).put("specialThreshold",data.settings.specialThreshold).put("diplomaThreshold",data.settings.diplomaThreshold))
        root.put("members",JSONArray().apply { data.members.forEach { put(JSONObject().put("id",it.id).put("fullName",it.fullName).put("photoUri",it.photoUri).put("birthDate",it.birthDate).put("groupName",it.groupName).put("active",it.active).put("createdAt",it.createdAt).put("updatedAt",it.updatedAt)) } })
        root.put("records",JSONArray().apply { data.records.forEach { r -> put(JSONObject().put("id",r.id).put("memberId",r.memberId).put("meetingId",r.meetingId).put("attended",r.attended).put("notes",r.notes).put("createdAt",r.createdAt).put("updatedAt",r.updatedAt).put("rubricVersion",r.rubricVersion)) } })
        root.put("scores",JSONArray().apply { data.scores.forEach { put(JSONObject().put("recordId",it.recordId).put("aspect",it.aspect).put("achieved",it.achieved).put("points",it.points)) } })
        root.put("penalties",JSONArray().apply { data.applied.forEach { put(JSONObject().put("recordId",it.recordId).put("penaltyCode",it.penaltyCode)) } })
        output.bufferedWriter().use { it.write(root.toString(2)) }
    }

    fun writePdf(data: AppSnapshot, output: OutputStream) {
        val document=PdfDocument(); val paint=Paint(Paint.ANTI_ALIAS_FLAG); var pageNumber=1
        var page=document.startPage(PdfDocument.PageInfo.Builder(595,842,pageNumber).create()); var canvas=page.canvas; var y=55f
        fun header() { paint.color=Color.rgb(49,46,129); paint.textSize=22f; paint.isFakeBoldText=true; canvas.drawText(data.settings.ministryName.take(42),40f,y,paint); y+=28; paint.color=Color.DKGRAY; paint.textSize=13f; paint.isFakeBoldText=false; canvas.drawText("${data.settings.cycleName} · Reporte final",40f,y,paint); y+=32 }
        fun nextPage() { document.finishPage(page); pageNumber++; page=document.startPage(PdfDocument.PageInfo.Builder(595,842,pageNumber).create()); canvas=page.canvas; y=55f; header() }
        header(); paint.textSize=11f
        data.ranking().forEachIndexed { index,p ->
            if(y>790) nextPage()
            paint.color=if(index<3) Color.rgb(79,70,229) else Color.DKGRAY; paint.isFakeBoldText=index<3
            canvas.drawText("${index+1}. ${p.member.fullName.take(34)}",40f,y,paint)
            canvas.drawText("${p.total}/1200",365f,y,paint); canvas.drawText("${p.attendedCount}/12",435f,y,paint); y+=16
            paint.color=Color.GRAY; paint.isFakeBoldText=false; paint.textSize=9f; canvas.drawText(data.reward(p.total,p.attendedCount),58f,y,paint); paint.textSize=11f; y+=23
        }
        y+=10; if(y>760) nextPage(); paint.color=Color.DKGRAY; paint.textSize=9f
        canvas.drawText("Generado: ${formatDate(System.currentTimeMillis())}. Puntaje máximo: 1200.",40f,y,paint)
        document.finishPage(page); document.writeTo(output); document.close(); output.close()
    }
    fun suggestedDate() = SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(Date())
    private fun formatDate(value: Long)=SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(value))
}
