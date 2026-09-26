package com.ministerio.jovenes.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ministerio.jovenes.data.repository.AppSnapshot
import com.ministerio.jovenes.ui.components.EmptyState
import com.ministerio.jovenes.ui.components.ScorePill
import com.ministerio.jovenes.ui.components.ScriptureCard
import com.ministerio.jovenes.ui.components.BrandHero
import com.ministerio.jovenes.ui.components.SectionHeading
import com.ministerio.jovenes.util.Exporter
import java.text.SimpleDateFormat
import java.util.*

@Composable fun RankingScreen(data: AppSnapshot, open:(Long)->Unit) {
    val ranking=data.ranking()
    if(ranking.isEmpty()) EmptyState(Icons.Default.EmojiEvents,"Ranking pendiente","Agrega miembros para comenzar a comparar su progreso.")
    else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,12.dp,16.dp,100.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { BrandHero("Corre para alcanzar la meta","Compromiso, Palabra, adoración y sana convivencia",Icons.Default.EmojiEvents,compact=true) }
        item { ScriptureCard("Corran de tal manera que obtengan el premio.","1 Corintios 9:24",accent=MaterialTheme.colorScheme.tertiary) }
        itemsIndexed(ranking,key={_,p->p.member.id}) { index,p -> Card(onClick={open(p.member.id)},shape=RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) { Surface(shape=CircleShape,color=when(index){0->MaterialTheme.colorScheme.tertiaryContainer;1,2->MaterialTheme.colorScheme.secondaryContainer;else->MaterialTheme.colorScheme.surfaceVariant}) { Box(Modifier.size(42.dp),contentAlignment=Alignment.Center){Text("${index+1}",fontWeight=FontWeight.Bold)} }; MemberAvatar(p.member,46); Column(Modifier.weight(1f)) { Text(p.member.fullName,fontWeight=FontWeight.SemiBold); Text("${p.attendedCount}/12 asistencias",style=MaterialTheme.typography.labelMedium); Text(data.reward(p.total,p.attendedCount),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary) }; ScorePill(p.total,1200) } }
        }
    }
}

@Composable fun ReportsScreen(data: AppSnapshot, open:(Long)->Unit) {
    val context=LocalContext.current
    fun toast(ok:Boolean)=Toast.makeText(context,if(ok) "Archivo guardado correctamente" else "No se pudo crear el archivo",Toast.LENGTH_LONG).show()
    val csv=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if(uri!=null) toast(runCatching { context.contentResolver.openOutputStream(uri)!!.use { Exporter.writeCsv(data,it) } }.isSuccess) }
    val pdf=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> if(uri!=null) toast(runCatching { Exporter.writePdf(data,context.contentResolver.openOutputStream(uri)!!) }.isSuccess) }
    val backup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null) toast(runCatching { context.contentResolver.openOutputStream(uri)!!.use { Exporter.writeBackup(data,it) } }.isSuccess) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,12.dp,16.dp,100.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { BrandHero("Reporte final del ciclo","${data.members.count { it.active }} miembros · ${data.records.size} registros · máximo 1200 puntos",Icons.Default.Assessment) }
        item { ScriptureCard("No nos cansemos de hacer el bien, porque a su tiempo cosecharemos.","Gálatas 6:9",accent=MaterialTheme.colorScheme.secondary) }
        item { SectionHeading("Exportar y respaldar","Conserva y comparte el fruto del ciclo") }
        item { ExportCard(Icons.Default.PictureAsPdf,"Reporte PDF","Documento listo para imprimir con ranking y reconocimientos") { pdf.launch("reporte-impulso-joven-${Exporter.suggestedDate()}.pdf") } }
        item { ExportCard(Icons.Default.TableView,"Datos CSV / Excel","Resumen y detalle de encuentros compatible con Excel") { csv.launch("datos-impulso-joven-${Exporter.suggestedDate()}.csv") } }
        item { ExportCard(Icons.Default.Backup,"Copia de seguridad JSON","Respaldo local de miembros, puntajes y configuración") { backup.launch("respaldo-impulso-joven-${Exporter.suggestedDate()}.json") } }
        item { SectionHeading("Resultados","Reconoce el crecimiento de cada joven") }
        if(data.ranking().isEmpty()) item { Text("No hay resultados todavía.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        itemsIndexed(data.ranking(),key={_,p->p.member.id}) { index,p -> Card(onClick={open(p.member.id)}) { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Text("${index+1}. ${p.member.fullName}",Modifier.weight(1f),fontWeight=FontWeight.Bold); ScorePill(p.total,1200) }; LinearProgressIndicator({p.total/1200f},Modifier.fillMaxWidth()); Text(data.reward(p.total,p.attendedCount),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold); Text("${p.attendedCount} asistencias · ${p.meetingsCompleted} encuentros evaluados",style=MaterialTheme.typography.labelMedium) } }
        }
    }
}

@Composable private fun ExportCard(icon: androidx.compose.ui.graphics.vector.ImageVector,title:String,text:String,action:()->Unit) { Card(onClick=action,shape=RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) { Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.secondaryContainer) { Icon(icon,null,Modifier.padding(12.dp),tint=MaterialTheme.colorScheme.secondary) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title,fontWeight=FontWeight.Bold); Text(text,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.Download,null) } } }

@Composable fun SettingsScreen(data: AppSnapshot, save:(com.ministerio.jovenes.data.local.AppSettingsEntity)->Unit, changePassword:(String,String)->Unit, logout:()->Unit) {
    var ministry by remember(data.settings.updatedAt) { mutableStateOf(data.settings.ministryName) }; var cycle by remember(data.settings.updatedAt) { mutableStateOf(data.settings.cycleName) }
    var major by remember(data.settings.updatedAt) { mutableStateOf(data.settings.majorThreshold.toString()) }; var special by remember(data.settings.updatedAt) { mutableStateOf(data.settings.specialThreshold.toString()) }; var diploma by remember(data.settings.updatedAt) { mutableStateOf(data.settings.diplomaThreshold.toString()) }
    var current by remember { mutableStateOf("") }; var replacement by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        BrandHero("Lidera con sabiduría","Configura el ciclo y protege la información",Icons.Default.Settings,compact=true)
        ScriptureCard("Sobre toda cosa guardada, guarda tu corazón.","Proverbios 4:23")
        SectionHeading("Identidad del ciclo")
        OutlinedTextField(ministry,{ministry=it},Modifier.fillMaxWidth(),label={Text("Nombre del ministerio")},singleLine=true)
        OutlinedTextField(cycle,{cycle=it},Modifier.fillMaxWidth(),label={Text("Nombre del ciclo")},singleLine=true)
        SectionHeading("Niveles de recompensa","Metas claras que motivan el crecimiento")
        Text("Los valores deben mantener este orden: premio mayor > reconocimiento > diploma.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        ThresholdField("Premio mayor",major){major=it}; ThresholdField("Reconocimiento especial",special){special=it}; ThresholdField("Diploma de participación",diploma){diploma=it}
        val m=major.toIntOrNull(); val s=special.toIntOrNull(); val d=diploma.toIntOrNull(); val valid=m!=null&&s!=null&&d!=null&&m in 1..1200&&d<s&&s<m
        Button({save(data.settings.copy(ministryName=ministry.trim(),cycleName=cycle.trim(),majorThreshold=m!!,specialThreshold=s!!,diplomaThreshold=d!!))},Modifier.fillMaxWidth(),enabled=valid&&ministry.isNotBlank()&&cycle.isNotBlank()) { Icon(Icons.Default.Save,null); Text(" Guardar configuración") }
        HorizontalDivider(); SectionHeading("Seguridad del líder","El cuidado de los datos también es servicio")
        OutlinedTextField(current,{current=it},Modifier.fillMaxWidth(),label={Text("Contraseña actual")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
        OutlinedTextField(replacement,{replacement=it},Modifier.fillMaxWidth(),label={Text("Contraseña nueva (mín. 8)")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
        OutlinedButton({changePassword(current,replacement);current="";replacement=""},Modifier.fillMaxWidth(),enabled=current.isNotBlank()&&replacement.length>=8) { Icon(Icons.Default.Password,null); Text(" Cambiar contraseña") }
        HorizontalDivider(); SectionHeading("Actividad reciente")
        data.history.take(8).forEach { h -> ListItem(headlineContent={Text(h.summary)},supportingContent={Text("${h.action} · ${SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(Date(h.timestamp))}")},leadingContent={Icon(Icons.Default.History,null)}) }
        OutlinedButton(logout,Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Logout,null); Text(" Cerrar sesión") }
        Text("Impulso Joven 1.0 · Datos locales y sin conexión",Modifier.fillMaxWidth(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(80.dp))
    }
}
@Composable private fun ThresholdField(label:String,value:String,change:(String)->Unit) { OutlinedTextField(value,{if(it.all(Char::isDigit))change(it)},Modifier.fillMaxWidth(),label={Text(label)},suffix={Text("pts")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true) }
