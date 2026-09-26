package com.ministerio.jovenes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ministerio.jovenes.data.local.MemberEntity
import com.ministerio.jovenes.data.repository.AppSnapshot
import com.ministerio.jovenes.data.repository.RecordDraft
import com.ministerio.jovenes.data.repository.GameParticipation
import java.text.SimpleDateFormat
import java.util.*

@Composable fun RecordScreen(data: AppSnapshot, member: MemberEntity, meeting: Int, save:(RecordDraft)->Unit, remove:()->Unit) {
    val existing=data.recordFor(member.id,meeting)
    val achieved=existing?.let { r -> data.breakdown(r.id).associate { it.aspect to it.achieved } }.orEmpty()
    var draft by remember(existing?.id, existing?.updatedAt) { mutableStateOf(if(existing==null) RecordDraft() else RecordDraft(
        attended=existing.attended, attentive=achieved["ATTENTIVE"]==true, word=achieved["WORD"]==true,
        prayer=achieved["PRAYER"]==true, worship=achieved["WORSHIP"]==true,
        standing=achieved["STANDING"]==true, answers=achieved["ANSWERS"]==true,
        gameParticipation=when(data.breakdown(existing.id).firstOrNull { it.aspect=="GAMES" }?.points ?: 0) {
            in 1..14 -> GameParticipation.RESPECTFUL_NO_PLAY
            15 -> GameParticipation.PARTICIPATES
            else -> GameParticipation.NONE
        }, punctuality=achieved["PUNCTUALITY"]==true,
        penalties=data.penaltyBreakdown(existing.id).map { it.code }.toSet(),notes=existing.notes
    )) }
    var confirmRemove by remember { mutableStateOf(false) }
    val positives=draft.aspectScores().values.sum()
    val deductions=data.penaltyTypes.filter { it.code in draft.penalties }.sumOf { it.points }
    val total=if(draft.attended) (positives+deductions).coerceIn(0,100) else 0
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) { Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically) { MemberAvatar(member,54); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(member.fullName,fontWeight=FontWeight.Bold); Text("Encuentro $meeting") }; Column(horizontalAlignment=Alignment.End) { Text(total.toString(),style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary); Text("de 100 puntos",style=MaterialTheme.typography.labelSmall) } } }
        Card(shape=RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) { Icon(if(draft.attended) Icons.Default.HowToReg else Icons.Default.PersonOff,null,tint=if(draft.attended) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Asistencia",fontWeight=FontWeight.Bold); Text(if(draft.attended) "+15 puntos" else "Ausente: el encuentro vale 0",style=MaterialTheme.typography.labelMedium) }; Switch(draft.attended,{ value -> draft=if(value) draft.copy(attended=true) else RecordDraft(attended=false,notes=draft.notes) }) } }
        if(!draft.attended) Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)) { Row(Modifier.padding(16.dp)) { Icon(Icons.Default.Info,null); Spacer(Modifier.width(10.dp)); Text("Los demás aspectos y penalizaciones se deshabilitan cuando el miembro no asiste.") } }
        Text("Compromiso y participación",style=MaterialTheme.typography.titleLarge)
        AspectToggle("Atender y prestar atención a la clase",10,draft.attentive,draft.attended) { draft=draft.copy(attentive=it) }
        AspectToggle("Leer la Palabra",10,draft.word,draft.attended) { draft=draft.copy(word=it) }
        AspectToggle("Orar con reverencia",10,draft.prayer,draft.attended) { draft=draft.copy(prayer=it) }
        AspectToggle("Alabar o adorar a Dios",10,draft.worship,draft.attended) { draft=draft.copy(worship=it) }
        AspectToggle("Mantenerse de pie al alabar",5,draft.standing,draft.attended) { draft=draft.copy(standing=it) }
        AspectToggle("Responder preguntas",15,draft.answers,draft.attended) { draft=draft.copy(answers=it) }
        GameParticipationSelector(draft.gameParticipation,draft.attended) { draft=draft.copy(gameParticipation=it) }
        AspectToggle("Puntualidad",10,draft.punctuality,draft.attended) { draft=draft.copy(punctuality=it) }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("Penalizaciones",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge); if(deductions<0) AssistChip({},label={Text("$deductions puntos")},leadingIcon={Icon(Icons.Default.Warning,null)}) }
        Text("Selecciona solo hechos observados. El puntaje nunca será menor que 0.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        data.penaltyTypes.forEach { penalty -> val checked=penalty.code in draft.penalties
            Card(colors=CardDefaults.cardColors(containerColor=if(checked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)) { Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) { Checkbox(checked,{ enabled -> draft=draft.copy(penalties=if(enabled) draft.penalties+penalty.code else draft.penalties-penalty.code) },enabled=draft.attended); Text(penalty.label,Modifier.weight(1f)); Text(penalty.points.toString(),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.error) } }
        }
        OutlinedTextField(draft.notes,{draft=draft.copy(notes=it)},Modifier.fillMaxWidth(),label={Text("Observaciones del líder")},minLines=3,enabled=true)
        if(existing!=null) Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(14.dp)) { Text("Trazabilidad",fontWeight=FontWeight.Bold); Text("Creado: ${date(existing.createdAt)}",style=MaterialTheme.typography.labelMedium); Text("Última modificación: ${date(existing.updatedAt)}",style=MaterialTheme.typography.labelMedium) } }
        Button({save(draft)},Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)) { Icon(Icons.Default.Save,null); Text(" Guardar registro · $total puntos") }
        if(existing!=null) TextButton({confirmRemove=true},Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)) { Text("Eliminar registro del encuentro") }
        Spacer(Modifier.height(24.dp))
    }
    if(confirmRemove) AlertDialog({confirmRemove=false},title={Text("Eliminar registro")},text={Text("El encuentro volverá a estado pendiente.")},confirmButton={Button({confirmRemove=false;remove()}){Text("Eliminar")}},dismissButton={TextButton({confirmRemove=false}){Text("Cancelar")}})
}

@Composable private fun GameParticipationSelector(value: GameParticipation, enabled: Boolean, onChange: (GameParticipation) -> Unit) {
    Card(shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=if(value!=GameParticipation.NONE&&enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.SportsEsports,null); Spacer(Modifier.width(10.dp)); Text("Participación en los juegos",Modifier.weight(1f),fontWeight=FontWeight.SemiBold); Text("hasta +15",color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold) }
            GameParticipation.entries.forEach { option ->
                val label=when(option){ GameParticipation.NONE->"No participa"; GameParticipation.RESPECTFUL_NO_PLAY->"No desea jugar, pero respeta"; GameParticipation.PARTICIPATES->"Participa correctamente" }
                Row(Modifier.fillMaxWidth().clickable(enabled=enabled){onChange(option)},verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(selected=value==option,onClick={onChange(option)},enabled=enabled)
                    Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                    Text("+${option.points}",fontWeight=FontWeight.Bold,color=if(enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)
                }
            }
            if(value==GameParticipation.RESPECTFUL_NO_PLAY) Text("Recibe 5 puntos porque permanece respetuoso aunque no desea jugar.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun AspectToggle(label:String,points:Int,checked:Boolean,enabled:Boolean,onChange:(Boolean)->Unit) {
    Card(shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=if(checked&&enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) { Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) { Checkbox(checked,onChange,enabled=enabled); Text(label,Modifier.weight(1f),fontWeight=if(checked) FontWeight.SemiBold else FontWeight.Normal); Text("+$points",color=if(enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,fontWeight=FontWeight.Bold) } }
}
private fun date(value:Long)=SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(value))
