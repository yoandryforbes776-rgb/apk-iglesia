package com.ministerio.jovenes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ministerio.jovenes.data.repository.AppSnapshot
import com.ministerio.jovenes.ui.components.StatCard
import com.ministerio.jovenes.ui.components.BrandHero
import com.ministerio.jovenes.ui.components.ScriptureCard
import com.ministerio.jovenes.ui.components.SectionHeading
import com.ministerio.jovenes.ui.theme.DeepIndigo
import com.ministerio.jovenes.ui.theme.Indigo
import com.ministerio.jovenes.ui.theme.Teal

@Composable fun LoginScreen(busy: Boolean, onLogin: (String,String)->Unit) {
    var user by remember { mutableStateOf("admin") }; var password by remember { mutableStateOf("") }; var visible by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DeepIndigo,Indigo,Color(0xFF7C3AED)))).padding(horizontal=24.dp),contentAlignment=Alignment.Center) {
        Card(Modifier.fillMaxWidth().widthIn(max=480.dp).verticalScroll(rememberScrollState()),shape=RoundedCornerShape(30.dp),elevation=CardDefaults.cardElevation(14.dp)) {
            Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
                BrandHero("Impulso Joven","Fe, propósito y una generación que deja huella",Icons.Default.AutoAwesome,compact=true)
                ScriptureCard("Ninguno tenga en poco tu juventud; sé ejemplo de los creyentes.","1 Timoteo 4:12",accent=Indigo)
                OutlinedTextField(user,{user=it},Modifier.fillMaxWidth(),label={Text("Usuario")},leadingIcon={Icon(Icons.Default.Person,null)},singleLine=true)
                OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("Contraseña")},leadingIcon={Icon(Icons.Default.Lock,null)},trailingIcon={IconButton({visible=!visible}) { Icon(if(visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,null) }},visualTransformation=if(visible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),singleLine=true)
                Button({onLogin(user,password)},Modifier.fillMaxWidth().height(52.dp),enabled=!busy&&password.isNotBlank(),shape=RoundedCornerShape(16.dp)) { if(busy) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp,color=Color.White) else { Icon(Icons.Default.Login,null); Spacer(Modifier.width(8.dp)); Text("Entrar como líder") } }
                Text("Primer acceso: admin / Admin123!",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tus datos permanecen en este dispositivo",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable fun DashboardScreen(data: AppSnapshot, goMembers:()->Unit, goRanking:()->Unit, goReports:()->Unit) {
    val ranking=data.ranking(); val recorded=data.records.count(); val average=if(ranking.isEmpty()) 0 else ranking.sumOf { it.total }/ranking.size
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        BrandHero("¡Paz y bendiciones!",data.settings.cycleName,Icons.Default.WbSunny)
        ScriptureCard("Yo sé los planes que tengo para ustedes: planes de bienestar y de esperanza.","Jeremías 29:11")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            StatCard("Miembros",data.members.count { it.active }.toString(),Icons.Default.Groups,Indigo,Modifier.weight(1f))
            StatCard("Registros",recorded.toString(),Icons.Default.FactCheck,Teal,Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            StatCard("Promedio",average.toString(),Icons.Default.Insights,MaterialTheme.colorScheme.tertiary,Modifier.weight(1f))
            StatCard("Meta","1200",Icons.Default.EmojiEvents,MaterialTheme.colorScheme.error,Modifier.weight(1f))
        }
        SectionHeading("Acciones rápidas","Todo lo que necesitas para acompañar al grupo")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(goMembers,Modifier.weight(1f)) { Icon(Icons.Default.PersonAdd,null); Spacer(Modifier.width(6.dp)); Text("Miembros") }
            FilledTonalButton(goReports,Modifier.weight(1f)) { Icon(Icons.Default.IosShare,null); Spacer(Modifier.width(6.dp)); Text("Exportar") }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { SectionHeading("Primeros lugares","Esfuerzo que inspira",Modifier.weight(1f)); TextButton(goRanking) { Text("Ver ranking") } }
        if(ranking.isEmpty()) Card { Text("Agrega miembros y registra encuentros para ver el progreso.",Modifier.padding(20.dp),color=MaterialTheme.colorScheme.onSurfaceVariant) }
        ranking.take(3).forEachIndexed { index,p ->
            Card(onClick={goRanking()},shape=RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Surface(shape=RoundedCornerShape(50),color=if(index==0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer) { Text("${index+1}",Modifier.padding(12.dp),fontWeight=FontWeight.Bold) }
                Column(Modifier.weight(1f)) { Text(p.member.fullName,fontWeight=FontWeight.SemiBold); Text(data.reward(p.total,p.attendedCount),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("${p.total}",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
            }}
        }
        Spacer(Modifier.height(80.dp))
    }
}
