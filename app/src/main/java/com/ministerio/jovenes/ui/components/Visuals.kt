package com.ministerio.jovenes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ministerio.jovenes.ui.theme.DeepIndigo
import com.ministerio.jovenes.ui.theme.Indigo
import com.ministerio.jovenes.ui.theme.Teal

@Composable
fun BrandHero(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.AutoStories,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(listOf(DeepIndigo, Indigo, Color(0xFF7C3AED), Teal))
            ).height(if (compact) 142.dp else 180.dp)
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha=.08f), radius=size.minDimension*.38f, center=Offset(size.width*.92f,size.height*.18f))
                drawCircle(Color(0xFFFDE68A).copy(alpha=.13f), radius=size.minDimension*.24f, center=Offset(size.width*.72f,size.height*1.03f))
                drawCircle(Color.White.copy(alpha=.07f), radius=8f, center=Offset(size.width*.08f,size.height*.18f))
                drawCircle(Color.White.copy(alpha=.1f), radius=5f, center=Offset(size.width*.58f,size.height*.2f))
            }
            Column(
                Modifier.fillMaxHeight().fillMaxWidth(.72f).padding(22.dp),
                verticalArrangement=Arrangement.Center
            ) {
                Surface(shape=RoundedCornerShape(14.dp),color=Color.White.copy(alpha=.16f)) {
                    Icon(icon,null,Modifier.padding(9.dp).size(24.dp),tint=Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text(title,style=if(compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,color=Color.White,fontWeight=FontWeight.ExtraBold)
                Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=Color.White.copy(alpha=.84f),maxLines=2)
            }
            YouthFaithIllustration(Modifier.align(Alignment.CenterEnd).width(132.dp).fillMaxHeight().padding(end=10.dp))
        }
    }
}

@Composable
fun YouthFaithIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height
        // Resplandor detrás de la cruz.
        drawCircle(Color(0xFFFDE68A).copy(alpha=.22f),w*.31f,Offset(w*.56f,h*.31f))
        // Cruz.
        val cross=Color.White.copy(alpha=.94f)
        drawRoundRect(cross,Offset(w*.52f,h*.1f),Size(w*.09f,h*.39f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
        drawRoundRect(cross,Offset(w*.37f,h*.22f),Size(w*.39f,h*.09f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f,8f))
        // Tres jóvenes unidos.
        val colors=listOf(Color(0xFFFDE68A),Color(0xFF99F6E4),Color(0xFFC4B5FD))
        val xs=listOf(w*.27f,w*.53f,w*.79f)
        xs.forEachIndexed { i,x ->
            val headY=if(i==1) h*.56f else h*.61f
            drawCircle(colors[i],w*.095f,Offset(x,headY))
            val bodyTop=headY+w*.1f
            val path=Path().apply { moveTo(x,bodyTop); lineTo(x-w*.15f,h*.96f); lineTo(x+w*.15f,h*.96f); close() }
            drawPath(path,colors[i].copy(alpha=.96f))
        }
        // Brazos que expresan unidad y alabanza.
        drawLine(colors[0],Offset(w*.25f,h*.73f),Offset(w*.08f,h*.56f),strokeWidth=10f,cap=StrokeCap.Round)
        drawLine(colors[2],Offset(w*.8f,h*.74f),Offset(w*.94f,h*.53f),strokeWidth=10f,cap=StrokeCap.Round)
        drawLine(Color.White.copy(alpha=.75f),Offset(w*.37f,h*.75f),Offset(w*.45f,h*.72f),strokeWidth=7f,cap=StrokeCap.Round)
        drawLine(Color.White.copy(alpha=.75f),Offset(w*.63f,h*.72f),Offset(w*.71f,h*.76f),strokeWidth=7f,cap=StrokeCap.Round)
    }
}

@Composable
fun ScriptureCard(
    quote: String,
    reference: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier.fillMaxWidth(),
        shape=RoundedCornerShape(22.dp),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
        border=androidx.compose.foundation.BorderStroke(1.dp,accent.copy(alpha=.16f)),
        elevation=CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.Top) {
            Surface(shape=RoundedCornerShape(14.dp),color=accent.copy(alpha=.12f)) {
                Icon(Icons.Default.FormatQuote,null,Modifier.padding(10.dp),tint=accent)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("“$quote”",style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Medium)
                Text(reference.uppercase(),style=MaterialTheme.typography.labelMedium,color=accent,fontWeight=FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionHeading(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).height(if(subtitle==null) 24.dp else 38.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            subtitle?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
