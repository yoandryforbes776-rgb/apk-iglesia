package com.ministerio.jovenes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ministerio.jovenes.MinisterioApp
import com.ministerio.jovenes.data.local.AppSettingsEntity
import com.ministerio.jovenes.data.local.MemberEntity
import com.ministerio.jovenes.data.repository.AppSnapshot
import com.ministerio.jovenes.data.repository.RecordDraft
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MinistryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MinisterioApp).repository
    val data = repository.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSnapshot())
    private val _loggedIn = MutableStateFlow(false)
    val loggedIn = _loggedIn.asStateFlow()
    private val _mustChangePassword = MutableStateFlow(false)
    val mustChangePassword = _mustChangePassword.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _syncing = MutableStateFlow(false)
    val syncing = _syncing.asStateFlow()
    private val _hasCloudSession = MutableStateFlow(repository.hasSupabaseSession())
    val hasCloudSession = _hasCloudSession.asStateFlow()

    init { viewModelScope.launch { delay(700); autoSync(showSuccess=false) } }

    fun clearMessage() { _message.value = null }
    fun login(user: String, password: String) = launch("Bienvenido") { _loggedIn.value = repository.login(user, password); if(!_loggedIn.value) error("Usuario o contraseña incorrectos") else _mustChangePassword.value=repository.mustChangePassword() }
    fun logout() { _loggedIn.value=false; _message.value="Sesión cerrada" }
    fun saveMember(existing: MemberEntity?, name: String, photo: String?, birth: String?, group: String?, done: () -> Unit) {
        if(name.isBlank()) { _message.value="El nombre es obligatorio"; return }
        launch("Miembro guardado", done) { repository.saveMember(existing,name,photo,birth,group) }
    }
    fun deleteMember(member: MemberEntity, done: () -> Unit) = launch("Miembro eliminado", done) { repository.deleteMember(member) }
    fun archive(member: MemberEntity) = launch(if(member.active) "Miembro archivado" else "Miembro reactivado") { repository.setMemberActive(member,!member.active) }
    fun saveRecord(memberId: Long, meeting: Int, draft: RecordDraft, done: () -> Unit) = launch("Encuentro guardado", done) { repository.saveRecord(memberId,meeting,draft) }
    fun deleteRecord(memberId: Long, meeting: Int, done: () -> Unit) = launch("Registro eliminado", done) { repository.deleteRecord(memberId,meeting) }
    fun createCycle(name: String, start: String?, end: String?) {
        if(name.isBlank()) { _message.value="Escribe el nombre del ciclo"; return }
        launch("Ciclo creado") { repository.createCycle(name,start,end) }
    }
    fun selectCycle(cycle: com.ministerio.jovenes.data.local.CycleEntity) = launch("Ciclo activo: ${cycle.name}") { repository.selectCycle(cycle) }
    fun closeCycle(cycle: com.ministerio.jovenes.data.local.CycleEntity) = launch("Ciclo cerrado") { repository.closeCycle(cycle) }
    fun saveGroupAttendance(meeting: Int, present: Set<Long>, done:()->Unit) = launch("Asistencia grupal guardada",done) { repository.saveGroupAttendance(meeting,present) }
    fun saveMeetingPlan(plan: com.ministerio.jovenes.data.local.MeetingPlanEntity) = launch("Encuentro planificado") { repository.saveMeetingPlan(plan) }

    fun saveSettings(settings: AppSettingsEntity) = launch("Configuración guardada") { repository.saveSettings(settings) }
    fun syncSupabase(settings: AppSettingsEntity, password: String) = launch("Sincronización con Supabase completada", shouldAutoSync=false) {
        repository.saveSettings(settings)
        repository.syncWithSupabase(password)
        _hasCloudSession.value=true
    }
    fun autoSync(showSuccess: Boolean=true) = viewModelScope.launch {
        if(_syncing.value) return@launch
        _syncing.value=true
        runCatching { repository.syncWithSavedSession() }
            .onSuccess { if(showSuccess && it!=null) _message.value="Datos sincronizados" }
            .onFailure { if(showSuccess) _message.value="Sincronización pendiente: ${it.message}" }
        _syncing.value=false
    }
    fun disconnectCloud() { repository.clearSupabaseSession(); _hasCloudSession.value=false; _message.value="Sesión de Supabase desconectada" }
    fun changePassword(current: String, replacement: String) {
        if(replacement.length<8) { _message.value="La contraseña nueva debe tener al menos 8 caracteres"; return }
        viewModelScope.launch { _busy.value=true; runCatching { repository.changePassword(current,replacement) }.onSuccess { ok -> if(ok) _mustChangePassword.value=false; _message.value=if(ok) "Contraseña actualizada" else "La contraseña actual no coincide" }.onFailure { _message.value=it.message }; _busy.value=false }
    }
    private fun launch(success: String, done: () -> Unit = {}, shouldAutoSync: Boolean = true, block: suspend () -> Unit) = viewModelScope.launch {
        _busy.value=true
        runCatching { block() }.onSuccess { _message.value=success; done(); if(shouldAutoSync) autoSync(showSuccess=false) }.onFailure { _message.value=it.message ?: "No se pudo completar la acción" }
        _busy.value=false
    }
}
