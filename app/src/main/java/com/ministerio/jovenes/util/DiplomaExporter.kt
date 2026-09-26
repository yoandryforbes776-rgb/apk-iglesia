package com.ministerio.jovenes.util

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.ministerio.jovenes.data.local.MemberEntity
import com.ministerio.jovenes.data.repository.AppSnapshot
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object DiplomaExporter {
    fun write(data: AppSnapshot, member: MemberEntity, output: OutputStream) {
        val progress=data.progress(member)
        val document=PdfDocument()
        val page=document.startPage(PdfDocument.PageInfo.Builder(842,595,1).create())
        val canvas=page.canvas
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        val indigo=Color.rgb(49,46,129); val violet=Color.rgb(79,70,229); val teal=Color.rgb(13,148,136); val gold=Color.rgb(245,158,11)

        canvas.drawColor(Color.rgb(255,253,248))
        paint.style=Paint.Style.STROKE; paint.strokeWidth=10f; paint.color=indigo
        canvas.drawRoundRect(20f,20f,822f,575f,24f,24f,paint)
        paint.strokeWidth=3f; paint.color=gold
        canvas.drawRoundRect(34f,34f,808f,561f,18f,18f,paint)
        paint.style=Paint.Style.FILL
        canvas.drawCircle(76f,72f,44f,Paint(paint).apply { color=violet })
        canvas.drawCircle(766f,523f,44f,Paint(paint).apply { color=teal })
        // Cruz y libro abiertos dibujados, sin recursos externos.
        paint.color=Color.WHITE; canvas.drawRoundRect(71f,43f,81f,100f,5f,5f,paint); canvas.drawRoundRect(57f,59f,95f,69f,5f,5f,paint)
        paint.color=Color.WHITE; paint.style=Paint.Style.STROKE; paint.strokeWidth=4f
        val book=Path().apply { moveTo(730f,504f); quadraticTo(750f,495f,766f,510f); quadraticTo(782f,495f,802f,504f); lineTo(799f,535f); quadraticTo(780f,527f,766f,540f); quadraticTo(752f,527f,733f,535f); close() }
        canvas.drawPath(book,paint); paint.style=Paint.Style.FILL

        center(canvas,"DIPLOMA DE EXCELENCIA",88f,paint,indigo,31f,true)
        center(canvas,data.settings.ministryName.uppercase().take(70),120f,paint,teal,13f,true)
        center(canvas,"otorga el presente reconocimiento a",170f,paint,Color.DKGRAY,17f,false)
        center(canvas,member.fullName,231f,paint,violet,37f,true)
        paint.color=gold; canvas.drawRoundRect(182f,248f,660f,253f,3f,3f,paint)
        center(canvas,"por su compromiso, crecimiento espiritual, respeto y participación",292f,paint,Color.DKGRAY,17f,false)
        center(canvas,"durante los 12 encuentros de ${data.settings.cycleName}.",319f,paint,Color.DKGRAY,16f,false)

        val reward=data.reward(progress.total,progress.attendedCount)
        paint.color=Color.rgb(238,242,255); canvas.drawRoundRect(238f,345f,604f,417f,20f,20f,paint)
        center(canvas,"${progress.total} / 1200 PUNTOS",376f,paint,indigo,22f,true)
        center(canvas,reward.uppercase(),402f,paint,teal,14f,true)
        center(canvas,"“Todo lo que hagan, háganlo de corazón, como para el Señor.” · Colosenses 3:23",452f,paint,indigo,13f,false)

        paint.color=Color.GRAY; paint.strokeWidth=1.5f
        canvas.drawLine(125f,508f,325f,508f,paint); canvas.drawLine(517f,508f,717f,508f,paint)
        centerAt(canvas,"Firma del líder",225f,529f,paint,Color.DKGRAY,12f,false)
        centerAt(canvas,"Firma del pastor / director",617f,529f,paint,Color.DKGRAY,12f,false)
        center(canvas,"Emitido el ${SimpleDateFormat("dd 'de' MMMM 'de' yyyy",Locale("es")).format(Date())}",554f,paint,Color.GRAY,10f,false)

        document.finishPage(page); document.writeTo(output); document.close(); output.close()
    }

    fun safeName(name: String)=name.lowercase(Locale.getDefault()).replace(Regex("[^a-záéíóúñ0-9]+"),"-").trim('-')
    private fun center(canvas: Canvas,text:String,y:Float,paint:Paint,color:Int,size:Float,bold:Boolean)=centerAt(canvas,text,421f,y,paint,color,size,bold)
    private fun centerAt(canvas: Canvas,text:String,x:Float,y:Float,paint:Paint,color:Int,size:Float,bold:Boolean) {
        paint.color=color; paint.textSize=size; paint.textAlign=Paint.Align.CENTER; paint.typeface=if(bold) Typeface.create(Typeface.DEFAULT,Typeface.BOLD) else Typeface.create(Typeface.DEFAULT,Typeface.NORMAL)
        canvas.drawText(text.take(92),x,y,paint)
    }
}
