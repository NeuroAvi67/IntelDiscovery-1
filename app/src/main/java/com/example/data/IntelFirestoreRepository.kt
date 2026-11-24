package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class IntelFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun discoveriesCol(uid: String) =
        db.collection("users").document(uid).collection("discoveries")

    private fun searchesCol(uid: String) =
        db.collection("users").document(uid).collection("searches")

    private fun messagesCol(uid: String) =
        db.collection("users").document(uid).collection("messages")

    fun observeDiscoveries(userId: String): Flow<List<DiscoveryRecord>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/discoveries"
        emitAll(
            discoveriesCol(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DiscoveryRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeSearches(userId: String): Flow<List<SearchHistoryItem>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/searches"
        emitAll(
            searchesCol(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SearchHistoryItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeMessages(userId: String): Flow<List<AssistantMessage>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/messages"
        emitAll(
            messagesCol(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AssistantMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveDiscovery(draft: DiscoveryDraft, pinned: Boolean = false): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/discoveries"
        return try {
            val uid = requireUserId()
            val cleanQuery = draft.query.trim().take(200)
            require(cleanQuery.isNotEmpty()) { "Query cannot be empty" }
            val cleanCategory = if (draft.category == DiscoveryCategory.ALL) {
                DiscoveryCategory.GENERAL.label
            } else {
                draft.category.label
            }
            val cleanSummary = draft.summary.trim().take(4000).ifEmpty { "Summary unavailable" }

            val docRef = discoveriesCol(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "query" to cleanQuery,
                "category" to cleanCategory,
                "summary" to cleanSummary,
                "verifiedFacts" to draft.verifiedFacts.map { it.trim().take(600) }.filter { it.isNotEmpty() }.take(10),
                "interpretations" to draft.interpretations.map { it.trim().take(600) }.filter { it.isNotEmpty() }.take(10),
                "aiSuggestions" to draft.aiSuggestions.map { it.trim().take(600) }.filter { it.isNotEmpty() }.take(10),
                "sourceNotes" to draft.sourceNotes.map { it.trim().take(600) }.filter { it.isNotEmpty() }.take(10),
                "pinned" to pinned,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleDiscoveryPin(discoveryId: String, newPinned: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/discoveries/$discoveryId"
        return try {
            val uid = requireUserId()
            require(discoveryId.isNotBlank()) { "Discovery ID cannot be blank" }
            val docRef = discoveriesCol(uid).document(discoveryId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "pinned" to newPinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDiscovery(discoveryId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/discoveries/$discoveryId"
        return try {
            val uid = requireUserId()
            require(discoveryId.isNotBlank()) { "Discovery ID cannot be blank" }
            val docRef = discoveriesCol(uid).document(discoveryId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logSearch(query: String, category: DiscoveryCategory, resultCount: Int): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/searches"
        return try {
            val uid = requireUserId()
            val cleanQuery = query.trim().take(200)
            require(cleanQuery.isNotEmpty()) { "Search query cannot be blank" }
            val docRef = searchesCol(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "query" to cleanQuery,
                "category" to category.label,
                "resultCount" to resultCount.coerceIn(0, 100),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearSearchHistory(): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/searches"
        return try {
            val verifiedUid = requireUserId()
            val docs = searchesCol(verifiedUid).get().await().documents
            docs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun appendMessage(role: String, content: String, modelName: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/messages"
        return try {
            val uid = requireUserId()
            require(role == "user" || role == "assistant") { "Invalid role" }
            val cleanContent = content.trim().take(16000)
            require(cleanContent.isNotEmpty()) { "Message content cannot be blank" }
            val docRef = messagesCol(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "role" to role,
                "content" to cleanContent,
                "modelName" to modelName.take(80).ifBlank { "gemini-3.8-flash" },
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearMessages(): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/messages"
        return try {
            val verifiedUid = requireUserId()
            val docs = messagesCol(verifiedUid).get().await().documents
            docs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
