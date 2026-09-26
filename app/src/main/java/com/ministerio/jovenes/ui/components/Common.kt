package com.ministerio.jovenes.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppTopBar(title: String, subtitle: String?=null, back: (() -> Unit)?=null, actions: @Composable RowScope.() -> Unit={}) {
    TopAppBar(
        title={ Column { Text(title, fontWeight=FontWeight.Bold); subtitle?.let { Text(it,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) } } },
        navigationIcon={ if(back!=null) IconButton(onClick=back) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Volver") } }, actions=actions,
        colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.surface,scrolledContainerColor=MaterialTheme.colorScheme.surface)
    )
}

@Composable fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier=Modifier) {
    Card(modifier,shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(3.dp),border=androidx.compose.foundation.BorderStroke(1.dp,color.copy(alpha=.12f))) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Surface(shape=RoundedCornerShape(12.dp),color=color.copy(alpha=.12f)) { Icon(icon,null,Modifier.padding(8.dp).size(21.dp),tint=color) }
            Text(value,style=MaterialTheme.typography.headlineMedium,color=color,fontWeight=FontWeight.ExtraBold)
            Text(title,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun EmptyState(icon: ImageVector, title: String, text: String, action: (@Composable () -> Unit)?=null) {
    Column(Modifier.fillMaxWidth().padding(36.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.primaryContainer) { Icon(icon,null,Modifier.padding(20.dp).size(38.dp),tint=MaterialTheme.colorScheme.primary) }
        Text(title,style=MaterialTheme.typography.titleLarge); Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

@Composable fun ScorePill(score: Int, max: Int=100) {
    val color=when { score>=max*.85 -> MaterialTheme.colorScheme.secondary; score>=max*.65 -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.error }
    Surface(color=color.copy(alpha=.13f),shape=RoundedCornerShape(50)) { Text("$score/$max",Modifier.padding(horizontal=12.dp,vertical=6.dp),color=color,fontWeight=FontWeight.Bold) }
}
