package com.ministerio.jovenes.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ministerio.jovenes.data.local.MemberEntity
import com.ministerio.jovenes.data.repository.AppSnapshot
import com.ministerio.jovenes.ui.components.EmptyState
import com.ministerio.jovenes.ui.components.ScorePill
import com.ministerio.jovenes.ui.components.ScriptureCard
import com.ministerio.jovenes.ui.components.SectionHeading
import com.ministerio.jovenes.util.DiplomaExporter

@Composable fun MembersScreen(data: AppSnapshot, open:(Long)->Unit, add:()->Unit) {
    var query by remember { mutableStateOf("") }; var archived by remember { mutableStateOf(false) }
    val list=data.members.filter { (archived || it.active) && (query.isBlank() || it.fullName.contains(query,true) || it.groupName.orEmpty().contains(query,true)) }
    Box(Modifier.fillMaxSize()) {
        Column { OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(16.dp),placeholder={Text("Buscar por nombre o grupo")},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon=if(query.isNotBlank()) ({ IconButton({query=""}) { Icon(Icons.Default.Close,null) } }) else null,singleLine=true,shape=RoundedCornerShape(18.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically) { Text("Mostrar archivados",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge); Switch(archived,{archived=it}) }
            ScriptureCard("Acuérdate de tu Creador en los días de tu juventud.","Eclesiastés 12:1",Modifier.padding(horizontal=16.dp,vertical=8.dp),MaterialTheme.colorScheme.secondary)
            if(list.isEmpty()) EmptyState(Icons.Default.Group,"Aún no hay miembros","Crea el primer perfil para comenzar el ciclo.") { Button(add) { Icon(Icons.Default.PersonAdd,null); Text(" Agregar miembro") } }
            else LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp,10.dp,16.dp,100.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) { items(list,key={it.id}) { member ->
                val p=data.progress(member)
                Card(onClick={open(member.id)},shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=if(member.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)) {
                        MemberAvatar(member,52)
                        Column(Modifier.weight(1f)) { Text(member.fullName,fontWeight=FontWeight.SemiBold); Text(member.groupName ?: "Sin grupo",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant); LinearProgressIndicator({p.total/1200f},Modifier.fillMaxWidth().padding(top=8.dp)) }
                        ScorePill(p.total,1200)
                    }
                }
            } }
        }
        FloatingActionButton(add,Modifier.align(Alignment.BottomEnd).padding(20.dp)) { Icon(Icons.Default.PersonAdd,"Agregar") }
    }
}

@Composable fun MemberAvatar(member: MemberEntity, size: Int) {
    if(member.photoUri!=null) AsyncImage(member.photoUri,null,Modifier.size(size.dp).clip(CircleShape),contentScale=ContentScale.Crop)
    else Surface(Modifier.size(size.dp),shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment=Alignment.Center) { Text(member.fullName.trim().take(1).uppercase(),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold) } }
}

@Composable fun MemberFormScreen(existing: MemberEntity?, save:(String,String?,String?,String?)->Unit) {
    var name by remember(existing) { mutableStateOf(existing?.fullName.orEmpty()) }; var photo by remember(existing) { mutableStateOf(existing?.photoUri) }; var birth by remember(existing) { mutableStateOf(existing?.birthDate.orEmpty()) }; var group by remember(existing) { mutableStateOf(existing?.groupName.orEmpty()) }
    val context=LocalContext.current
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION) }; photo=it.toString() } }
    Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
        val preview=existing?.copy(fullName=name.ifBlank { "Nuevo miembro" },photoUri=photo) ?: MemberEntity(fullName=name.ifBlank { "Nuevo miembro" },photoUri=photo)
        MemberAvatar(preview,96)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton({picker.launch(arrayOf("image/*"))}) { Icon(Icons.Default.PhotoCamera,null); Text(" Elegir foto") }; if(photo!=null) TextButton({photo=null}) { Text("Quitar") } }
        OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Nombre completo *")},leadingIcon={Icon(Icons.Default.Badge,null)},singleLine=true)
        OutlinedTextField(birth,{birth=it},Modifier.fillMaxWidth(),label={Text("Fecha de nacimiento (opcional)")},placeholder={Text("AAAA-MM-DD")},leadingIcon={Icon(Icons.Default.Cake,null)},singleLine=true)
        OutlinedTextField(group,{group=it},Modifier.fillMaxWidth(),label={Text("Grupo o categoría (opcional)")},leadingIcon={Icon(Icons.Default.Groups,null)},singleLine=true)
        Spacer(Modifier.weight(1f)); Button({save(name,photo,birth,group)},Modifier.fillMaxWidth().height(54.dp),enabled=name.isNotBlank(),shape=RoundedCornerShape(16.dp)) { Icon(Icons.Default.Save,null); Text(" Guardar miembro") }
    }
}

@Composable fun MemberDetailScreen(data: AppSnapshot, member: MemberEntity, edit:()->Unit, register:(Int)->Unit, archive:()->Unit, delete:()->Unit) {
    var confirmDelete by remember { mutableStateOf(false) }; val progress=data.progress(member); val context=LocalContext.current
    val diplomaName="diploma-${DiplomaExporter.safeName(member.fullName)}.pdf"
    val saveDiploma=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if(uri!=null) runCatching { DiplomaExporter.write(data,member,context.contentResolver.openOutputStream(uri)!!) }
            .onSuccess { Toast.makeText(context,"Diploma guardado. Ábrelo para imprimir.",Toast.LENGTH_LONG).show() }
            .onFailure { Toast.makeText(context,"No se pudo generar el diploma",Toast.LENGTH_LONG).show() }
    }
    fun shareDiploma() {
        runCatching {
            val dir=File(context.cacheDir,"diplomas").apply { mkdirs() }; val file=File(dir,diplomaName)
            file.outputStream().use { DiplomaExporter.write(data,member,it) }
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
            val intent=Intent(Intent.ACTION_SEND).apply { type="application/pdf"; putExtra(Intent.EXTRA_STREAM,uri); putExtra(Intent.EXTRA_TEXT,"Diploma de Excelencia de ${member.fullName} · ${progress.total}/1200 puntos"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            context.startActivity(Intent.createChooser(intent,"Compartir diploma por WhatsApp u otra aplicación"))
        }.onFailure { Toast.makeText(context,"No se pudo compartir el diploma",Toast.LENGTH_LONG).show() }
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp,8.dp,18.dp,100.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) { MemberAvatar(member,82); Text(member.fullName,style=MaterialTheme.typography.titleLarge); Text(member.groupName ?: "Sin grupo"); Text("${progress.total} / 1200",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary); LinearProgressIndicator({progress.total/1200f},Modifier.fillMaxWidth()); Text("${progress.attendedCount} asistencias · ${progress.meetingsCompleted} registros") } } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(edit,Modifier.weight(1f)) { Icon(Icons.Default.Edit,null); Text(" Editar") }; OutlinedButton(archive,Modifier.weight(1f)) { Icon(if(member.active) Icons.Default.Archive else Icons.Default.Unarchive,null); Text(if(member.active) " Archivar" else " Reactivar") } } }
        item { Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.tertiaryContainer)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.WorkspacePremium,null,Modifier.size(34.dp),tint=MaterialTheme.colorScheme.tertiary); Spacer(Modifier.width(12.dp)); Column { Text("Diploma de Excelencia",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium); Text("Personalizado con nombre, ${progress.total} puntos y reconocimiento",style=MaterialTheme.typography.bodySmall) } }; Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton({saveDiploma.launch(diplomaName)},Modifier.weight(1f)) { Icon(Icons.Default.Print,null); Text(" Guardar") }; Button({shareDiploma()},Modifier.weight(1f)) { Icon(Icons.Default.Share,null); Text(" WhatsApp") } } } } }
        item { ScriptureCard("Todo lo que hagan, háganlo de corazón, como para el Señor.","Colosenses 3:23",accent=MaterialTheme.colorScheme.secondary) }
        item { SectionHeading("Encuentros","Doce oportunidades para crecer y servir") }
        items(12) { index -> val number=index+1; val record=data.recordFor(member.id,number); val score=record?.let { data.score(it.id) }
            Card(onClick={register(number)},shape=RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) { Surface(shape=CircleShape,color=if(record!=null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant) { Text(number.toString(),Modifier.padding(12.dp),fontWeight=FontWeight.Bold) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text("Encuentro $number",fontWeight=FontWeight.SemiBold); Text(when { record==null -> "Pendiente"; record.attended -> "Asistió · ${data.penaltyBreakdown(record.id).size} penalizaciones"; else -> "Ausente" },style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }; if(score!=null) ScorePill(score) else Icon(Icons.Default.ChevronRight,null) } }
        }
        item { TextButton({confirmDelete=true},Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)) { Icon(Icons.Default.DeleteForever,null); Text(" Eliminar miembro y sus registros") } }
    }
    if(confirmDelete) AlertDialog({confirmDelete=false},title={Text("¿Eliminar definitivamente?")},text={Text("Se borrarán el perfil, sus 12 registros y el historial asociado. Esta acción no se puede deshacer.")},confirmButton={Button({confirmDelete=false;delete()},colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("Eliminar")}},dismissButton={TextButton({confirmDelete=false}){Text("Cancelar")}})
}
