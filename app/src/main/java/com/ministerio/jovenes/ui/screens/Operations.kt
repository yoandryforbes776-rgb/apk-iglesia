package com.ministerio.jovenes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ministerio.jovenes.data.local.CycleEntity
import com.ministerio.jovenes.data.local.MeetingPlanEntity
import com.ministerio.jovenes.data.repository.AppSnapshot
import com.ministerio.jovenes.ui.components.BrandHero
import com.ministerio.jovenes.ui.components.SectionHeading
import java.util.UUID

@Composable fun GroupAttendanceScreen(data: AppSnapshot, save:(Int,Set<Long>)->Unit, savePlan:(MeetingPlanEntity)->Unit) {
    var meeting by remember { mutableIntStateOf(1) }; var present by remember { mutableStateOf(emptySet<Long>()) }; var showPlan by remember { mutableStateOf(false) }
    val members=data.members.filter { it.active }
    LaunchedEffect(meeting,data.records,data.settings.activeCycleId) { present=members.filter { data.recordFor(it.id,meeting)?.attended==true }.map { it.id }.toSet() }
    val plan=data.meetingPlans.find { it.cycleId==data.settings.activeCycleId&&it.meetingId==meeting }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,10.dp,16.dp,110.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { BrandHero("Registro grupal","Marca la asistencia y después completa la evaluación individual",Icons.Default.FactCheck,compact=true) }
        item { SectionHeading("Selecciona el encuentro",data.activeCycle?.name ?: data.settings.cycleName) }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(-1,1).forEach { direction -> IconButton({meeting=(meeting+direction).coerceIn(1,12)},enabled=meeting+direction in 1..12) { Icon(if(direction<0) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,null) } }; Surface(Modifier.weight(1f),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.primaryContainer) { Text("Encuentro $meeting",Modifier.padding(14.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,fontWeight=FontWeight.Bold) }; IconButton({showPlan=true}) { Icon(Icons.Default.EditCalendar,"Planificar") } } }
        plan?.let { item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(14.dp)) { Text(it.title,fontWeight=FontWeight.Bold); listOfNotNull(it.scheduledDate,it.bibleTheme,it.leaderName,it.activity).forEach { value->Text(value,style=MaterialTheme.typography.bodySmall) } } } } }
        item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { SectionHeading("Lista de miembros","${present.size} presentes de ${members.size}",Modifier.weight(1f)); TextButton({present=members.map { it.id }.toSet()}) { Text("Todos") } } }
        items(members,key={it.id}) { member -> val checked=member.id in present
            Card(onClick={present=if(checked) present-member.id else present+member.id},colors=CardDefaults.cardColors(containerColor=if(checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) { Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) { Checkbox(checked,{on->present=if(on) present+member.id else present-member.id}); MemberAvatar(member,44); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(member.fullName,fontWeight=FontWeight.SemiBold); Text(member.groupName ?: "Sin grupo",style=MaterialTheme.typography.labelMedium) }; Icon(if(checked) Icons.Default.CheckCircle else Icons.Default.PersonOff,null,tint=if(checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline) } }
        }
        item { Button({save(meeting,present)},Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Default.Save,null); Text(" Guardar asistencia del grupo") } }
    }
    if(showPlan) MeetingPlanDialog(data.settings.activeCycleId,meeting,plan,{showPlan=false},{savePlan(it);showPlan=false})
}

@Composable private fun MeetingPlanDialog(cycleId:String,meeting:Int,current:MeetingPlanEntity?,dismiss:()->Unit,save:(MeetingPlanEntity)->Unit) {
    var title by remember { mutableStateOf(current?.title ?: "Encuentro $meeting") }; var date by remember { mutableStateOf(current?.scheduledDate.orEmpty()) }; var theme by remember { mutableStateOf(current?.bibleTheme.orEmpty()) }; var leader by remember { mutableStateOf(current?.leaderName.orEmpty()) }; var activity by remember { mutableStateOf(current?.activity.orEmpty()) }
    AlertDialog(dismiss,title={Text("Planificar encuentro $meeting")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedTextField(title,{title=it},label={Text("Título")}); OutlinedTextField(date,{date=it},label={Text("Fecha (AAAA-MM-DD)")}); OutlinedTextField(theme,{theme=it},label={Text("Tema o versículo")}); OutlinedTextField(leader,{leader=it},label={Text("Líder responsable")}); OutlinedTextField(activity,{activity=it},label={Text("Actividad o juego")}) }},confirmButton={Button({save(MeetingPlanEntity(cycleId,meeting,title,date.ifBlank{null},theme.ifBlank{null},leader.ifBlank{null},activity.ifBlank{null}))},enabled=title.isNotBlank()){Text("Guardar")}},dismissButton={TextButton(dismiss){Text("Cancelar")}})
}

@Composable fun CyclesScreen(data: AppSnapshot, create:(String,String?,String?)->Unit,select:(CycleEntity)->Unit,close:(CycleEntity)->Unit) {
    var dialog by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,10.dp,16.dp,100.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { BrandHero("Ciclos del ministerio","Conserva el historial y comienza nuevas etapas",Icons.Default.CalendarMonth,compact=true) }
        item { Button({dialog=true},Modifier.fillMaxWidth()) { Icon(Icons.Default.Add,null); Text(" Crear nuevo ciclo") } }
        items(data.cycles,key={it.id}) { cycle -> val active=cycle.id==data.settings.activeCycleId
            Card(onClick={select(cycle)},colors=CardDefaults.cardColors(containerColor=if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) { Row { Column(Modifier.weight(1f)) { Text(cycle.name,fontWeight=FontWeight.Bold); Text(listOfNotNull(cycle.startDate,cycle.endDate).joinToString(" — ").ifBlank { "Sin fechas" },style=MaterialTheme.typography.bodySmall) }; AssistChip({},label={Text(if(active) "ACTIVO" else cycle.status)}) }; if(cycle.status=="ACTIVE") TextButton({close(cycle)},enabled=!active) { Text("Cerrar ciclo") } } }
        }
    }
    if(dialog) NewCycleDialog({dialog=false}) { n,s,e->create(n,s,e);dialog=false }
}
@Composable private fun NewCycleDialog(dismiss:()->Unit,save:(String,String?,String?)->Unit) { var name by remember { mutableStateOf("") }; var start by remember { mutableStateOf("") }; var end by remember { mutableStateOf("") }; AlertDialog(dismiss,title={Text("Nuevo ciclo")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedTextField(name,{name=it},label={Text("Nombre")}); OutlinedTextField(start,{start=it},label={Text("Fecha inicial")}); OutlinedTextField(end,{end=it},label={Text("Fecha final")}) }},confirmButton={Button({save(name,start.ifBlank{null},end.ifBlank{null})},enabled=name.isNotBlank()){Text("Crear y activar")}},dismissButton={TextButton(dismiss){Text("Cancelar")}}) }
