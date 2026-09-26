package com.ministerio.jovenes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ministerio.jovenes.ui.components.AppTopBar
import com.ministerio.jovenes.ui.screens.*

data class MainTab(val route:String,val label:String,val icon:ImageVector)
private val tabs=listOf(MainTab("home","Inicio",Icons.Default.Home),MainTab("members","Miembros",Icons.Default.Groups),MainTab("ranking","Ranking",Icons.Default.EmojiEvents),MainTab("reports","Reportes",Icons.Default.Assessment),MainTab("settings","Ajustes",Icons.Default.Settings))

@Composable fun MinistryRoot(vm: MinistryViewModel= viewModel()) {
    val logged by vm.loggedIn.collectAsState(); val mustChangePassword by vm.mustChangePassword.collectAsState(); val busy by vm.busy.collectAsState(); val syncing by vm.syncing.collectAsState(); val message by vm.message.collectAsState(); val data by vm.data.collectAsState()
    val snackbar=remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    Box(Modifier.fillMaxSize()) {
        if(!logged) LoginScreen(busy,vm::login)
        else {
            val nav=rememberNavController(); val entry by nav.currentBackStackEntryAsState(); val route=entry?.destination?.route.orEmpty(); val main=route in tabs.map { it.route }
            Scaffold(
                containerColor=MaterialTheme.colorScheme.background,
                topBar={ if(main) AppTopBar(when(route){"home"->data.settings.ministryName;"members"->"Miembros";"ranking"->"Ranking general";"reports"->"Reporte final";else->"Configuración"},when(route){"home"->"12 encuentros · 1200 puntos";"ranking"->"El compromiso transforma";else->null},actions={ IconButton({vm.autoSync()}) { if(syncing) CircularProgressIndicator(Modifier.size(21.dp),strokeWidth=2.dp) else Icon(Icons.Default.CloudSync,"Sincronizar") } }) else when(route) {
                    "member/new" -> AppTopBar("Nuevo miembro",back={nav.popBackStack()})
                    "member/{id}/edit" -> AppTopBar("Editar miembro",back={nav.popBackStack()})
                    "member/{id}/meeting/{meeting}" -> AppTopBar("Registrar encuentro",back={nav.popBackStack()})
                    "group-attendance" -> AppTopBar("Asistencia grupal",back={nav.popBackStack()})
                    "cycles" -> AppTopBar("Gestión de ciclos",back={nav.popBackStack()})
                    else -> AppTopBar("Detalle del miembro",back={nav.popBackStack()})
                } },
                bottomBar={ AnimatedVisibility(main) { NavigationBar(containerColor=MaterialTheme.colorScheme.surface,tonalElevation=10.dp) { val destination=entry?.destination; tabs.forEach { tab -> NavigationBarItem(selected=destination?.hierarchy?.any { it.route==tab.route }==true,onClick={nav.navigate(tab.route){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}},icon={Icon(tab.icon,tab.label)},label={Text(tab.label)}) } } } }
            ) { padding ->
                NavHost(nav,"home",Modifier.padding(padding)) {
                    composable("home") { DashboardScreen(data,{nav.navigate("members")},{nav.navigate("ranking")},{nav.navigate("reports")},{nav.navigate("group-attendance")},{nav.navigate("cycles")}) }
                    composable("members") { MembersScreen(data,{nav.navigate("member/$it")},{nav.navigate("member/new")}) }
                    composable("ranking") { RankingScreen(data){nav.navigate("member/$it")} }
                    composable("reports") { ReportsScreen(data){nav.navigate("member/$it")} }
                    composable("settings") { SettingsScreen(data,vm::saveSettings,vm::syncSupabase,vm::changePassword,vm::logout) }
                    composable("group-attendance") { GroupAttendanceScreen(data,{meeting,present->vm.saveGroupAttendance(meeting,present){nav.popBackStack()}},vm::saveMeetingPlan) }
                    composable("cycles") { CyclesScreen(data,vm::createCycle,vm::selectCycle,vm::closeCycle) }
                    composable("member/new") { MemberFormScreen(null) { name,photo,birth,group -> vm.saveMember(null,name,photo,birth,group){nav.popBackStack()} } }
                    composable("member/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})) { back -> val id=back.arguments?.getLong("id") ?: 0; data.members.find { it.id==id }?.let { member -> MemberDetailScreen(data,member,{nav.navigate("member/$id/edit")},{nav.navigate("member/$id/meeting/$it")},{vm.archive(member)},{vm.deleteMember(member){nav.popBackStack()}}) } }
                    composable("member/{id}/edit",arguments=listOf(navArgument("id"){type=NavType.LongType})) { back -> val id=back.arguments?.getLong("id") ?: 0; data.members.find { it.id==id }?.let { member -> MemberFormScreen(member) { name,photo,birth,group -> vm.saveMember(member,name,photo,birth,group){nav.popBackStack()} } } }
                    composable("member/{id}/meeting/{meeting}",arguments=listOf(navArgument("id"){type=NavType.LongType},navArgument("meeting"){type=NavType.IntType})) { back -> val id=back.arguments?.getLong("id") ?: 0; val meeting=back.arguments?.getInt("meeting") ?: 1; data.members.find { it.id==id }?.let { member -> RecordScreen(data,member,meeting,{draft->vm.saveRecord(id,meeting,draft){nav.popBackStack()}},{vm.deleteRecord(id,meeting){nav.popBackStack()}}) } }
                }
            }
        }
        if(busy&&logged) Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.scrim.copy(alpha=.18f)) { Box(contentAlignment=Alignment.Center) { Card { Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(26.dp),strokeWidth=3.dp); Spacer(Modifier.width(12.dp)); Text("Guardando…") } } } }
        SnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).padding(bottom=if(logged) 82.dp else 16.dp))
        if(logged && mustChangePassword) ForcePasswordDialog(vm::changePassword)
    }
}

@Composable private fun ForcePasswordDialog(change: (String,String)->Unit) {
    var current by remember { mutableStateOf("") }; var replacement by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest={},icon={Icon(Icons.Default.Security,null)},title={Text("Protege tu cuenta")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { Text("Antes de continuar debes reemplazar la contraseña inicial Admin123! por una contraseña personal."); OutlinedTextField(current,{current=it},label={Text("Contraseña actual")},visualTransformation=PasswordVisualTransformation(),singleLine=true); OutlinedTextField(replacement,{replacement=it},label={Text("Nueva contraseña")},visualTransformation=PasswordVisualTransformation(),singleLine=true); OutlinedTextField(confirm,{confirm=it},label={Text("Confirmar contraseña")},visualTransformation=PasswordVisualTransformation(),singleLine=true); if(confirm.isNotBlank()&&confirm!=replacement) Text("Las contraseñas no coinciden",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.labelMedium) }},
        confirmButton={Button({change(current,replacement)},enabled=current.isNotBlank()&&replacement.length>=8&&replacement==confirm){Text("Cambiar y continuar")}})
}
