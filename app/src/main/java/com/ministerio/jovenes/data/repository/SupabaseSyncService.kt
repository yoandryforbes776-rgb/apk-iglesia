package com.ministerio.jovenes.data.repository

import com.ministerio.jovenes.data.local.*
import com.ministerio.jovenes.util.StoredSupabaseSession
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

internal data class RemoteMember(
    val syncId: String, val fullName: String, val birthDate: String?, val groupName: String?,
    val active: Boolean, val createdAt: Long, val updatedAt: Long
)
internal data class RemoteRecord(
    val syncId: String, val memberSyncId: String, val meetingId: Int, val attended: Boolean,
    val notes: String, val createdAt: Long, val updatedAt: Long, val rubricVersion: Int,
    val scores: Map<String, Int>, val penalties: Set<String>
)
internal data class RemoteDeletion(val syncId: String, val entityType: String, val deletedAt: Long)

data class SyncResult(val members: Int, val records: Int, val completedAt: Long)

internal class SupabaseSyncService {
    fun authenticate(settings: AppSettingsEntity, password: String): StoredSupabaseSession {
        val body=JSONObject().put("email",settings.supabaseEmail).put("password",password)
        return parseSession(request(settings,"POST","/auth/v1/token?grant_type=password",body.toString(),null))
    }

    fun refresh(settings: AppSettingsEntity, refreshToken: String): StoredSupabaseSession =
        parseSession(request(settings,"POST","/auth/v1/token?grant_type=refresh_token",JSONObject().put("refresh_token",refreshToken).toString(),null))

    fun claimChurch(settings: AppSettingsEntity, token: String): String {
        val body=JSONObject().put("workspace_code",settings.syncWorkspace.trim().lowercase()).put("display_name",settings.ministryName)
        return request(settings,"POST","/rest/v1/rpc/claim_church",body.toString(),token).trim().trim('"')
    }

    fun pullMembers(settings: AppSettingsEntity, token: String, churchId: String): List<RemoteMember> {
        val json=JSONArray(request(settings,"GET","/rest/v1/ministry_members?church_id=eq.${encode(churchId)}&select=*",null,token))
        return (0 until json.length()).map { i -> json.getJSONObject(i).let { o ->
            RemoteMember(o.getString("sync_id"),o.getString("full_name"),o.nullable("birth_date"),o.nullable("group_name"),o.optBoolean("active",true),o.getLong("created_at"),o.getLong("updated_at"))
        }}
    }

    fun pullRecords(settings: AppSettingsEntity, token: String, churchId: String): List<RemoteRecord> {
        val json=JSONArray(request(settings,"GET","/rest/v1/ministry_records?church_id=eq.${encode(churchId)}&select=*",null,token))
        return (0 until json.length()).map { i -> json.getJSONObject(i).let { o ->
            val scoreObject=o.optJSONObject("scores") ?: JSONObject()
            val scores=scoreObject.keys().asSequence().associateWith { scoreObject.optInt(it) }
            val penaltyArray=o.optJSONArray("penalties") ?: JSONArray()
            val penalties=(0 until penaltyArray.length()).map { penaltyArray.getString(it) }.toSet()
            RemoteRecord(o.getString("sync_id"),o.getString("member_sync_id"),o.getInt("meeting_id"),o.getBoolean("attended"),o.optString("notes"),o.getLong("created_at"),o.getLong("updated_at"),o.optInt("rubric_version",2),scores,penalties)
        }}
    }

    fun pullDeletions(settings: AppSettingsEntity, token: String, churchId: String): List<RemoteDeletion> {
        val json=JSONArray(request(settings,"GET","/rest/v1/ministry_deletions?church_id=eq.${encode(churchId)}&select=*",null,token))
        return (0 until json.length()).map { i -> json.getJSONObject(i).let { RemoteDeletion(it.getString("sync_id"),it.getString("entity_type"),it.getLong("deleted_at")) } }
    }

    fun pushMembers(settings: AppSettingsEntity, token: String, churchId: String, members: List<MemberEntity>) {
        if(members.isEmpty()) return
        val body=JSONArray().apply { members.forEach { m -> put(JSONObject()
            .put("workspace_id",settings.syncWorkspace).put("church_id",churchId).put("sync_id",m.syncId).put("full_name",m.fullName)
            .put("birth_date",m.birthDate ?: JSONObject.NULL).put("group_name",m.groupName ?: JSONObject.NULL)
            .put("active",m.active).put("created_at",m.createdAt).put("updated_at",m.updatedAt)) } }
        request(settings,"POST","/rest/v1/ministry_members?on_conflict=workspace_id,sync_id",body.toString(),token,"resolution=merge-duplicates")
    }

    fun pushRecords(settings: AppSettingsEntity, token: String, churchId: String, data: AppSnapshot) {
        if(data.records.isEmpty()) return
        val memberIds=data.members.associate { it.id to it.syncId }
        val body=JSONArray().apply { data.records.forEach { r ->
            val scores=JSONObject().apply { data.breakdown(r.id).forEach { put(it.aspect,it.points) } }
            val penalties=JSONArray().apply { data.applied.filter { it.recordId==r.id }.forEach { put(it.penaltyCode) } }
            put(JSONObject().put("workspace_id",settings.syncWorkspace).put("church_id",churchId).put("sync_id",r.syncId)
                .put("member_sync_id",memberIds[r.memberId]).put("meeting_id",r.meetingId).put("attended",r.attended)
                .put("notes",r.notes).put("created_at",r.createdAt).put("updated_at",r.updatedAt)
                .put("rubric_version",r.rubricVersion).put("scores",scores).put("penalties",penalties))
        }}
        request(settings,"POST","/rest/v1/ministry_records?on_conflict=workspace_id,sync_id",body.toString(),token,"resolution=merge-duplicates")
    }

    fun pushDeletions(settings: AppSettingsEntity, token: String, churchId: String, deletions: List<SyncDeletionEntity>) {
        if(deletions.isEmpty()) return
        val body=JSONArray().apply { deletions.forEach { put(JSONObject().put("workspace_id",settings.syncWorkspace).put("church_id",churchId).put("sync_id",it.syncId).put("entity_type",it.entityType).put("deleted_at",it.deletedAt)) } }
        request(settings,"POST","/rest/v1/ministry_deletions?on_conflict=workspace_id,sync_id",body.toString(),token,"resolution=merge-duplicates")
        deletions.forEach {
            val table=if(it.entityType=="MEMBER") "ministry_members" else "ministry_records"
            request(settings,"DELETE","/rest/v1/$table?church_id=eq.${encode(churchId)}&sync_id=eq.${encode(it.syncId)}",null,token)
        }
    }

    private fun parseSession(response: String): StoredSupabaseSession {
        val json=JSONObject(response); val expiresIn=json.optLong("expires_in",3600)
        return StoredSupabaseSession(json.getString("access_token"),json.getString("refresh_token"),System.currentTimeMillis()+expiresIn*1000)
    }

    private fun request(settings: AppSettingsEntity, method: String, path: String, body: String?, token: String?, prefer: String?=null): String {
        val base=settings.supabaseUrl.trim().trimEnd('/')
        val connection=(URI.create(base+path).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod=method; connectTimeout=15_000; readTimeout=25_000
            setRequestProperty("apikey",settings.supabaseAnonKey.trim())
            setRequestProperty("Authorization","Bearer ${token ?: settings.supabaseAnonKey.trim()}")
            setRequestProperty("Content-Type","application/json")
            setRequestProperty("Accept","application/json")
            prefer?.let { setRequestProperty("Prefer",it) }
            if(body!=null) { doOutput=true; outputStream.bufferedWriter().use { it.write(body) } }
        }
        val code=connection.responseCode
        val text=(if(code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if(code !in 200..299) throw IllegalStateException("Supabase respondió $code: ${text.take(240)}")
        return text
    }
    private fun encode(value: String)=URLEncoder.encode(value,"UTF-8")
    private fun JSONObject.nullable(key: String)=if(isNull(key)) null else optString(key).ifBlank { null }
}
