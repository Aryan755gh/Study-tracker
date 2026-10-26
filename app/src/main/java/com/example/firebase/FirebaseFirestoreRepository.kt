package com.example.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.DailyStudyLog
import com.example.model.GamificationState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseFirestoreRepository(private val context: Context) {

    private val db: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Sync user study profile to Firestore
     */
    suspend fun syncUserProfile(state: GamificationState, todayKey: String) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(uid)

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "xp" to state.xp,
            "level" to state.level,
            "streak" to state.streak,
            "minutesStudiedToday" to state.minutesStudiedToday,
            "totalFocusHours" to state.totalFocusHours.toDouble(),
            "questionsSolvedTotal" to state.questionsSolvedTotal,
            "lastActiveDate" to todayKey,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        auth.currentUser?.email?.let { payload["email"] = it }
        auth.currentUser?.displayName?.let { payload["displayName"] = it }

        try {
            userRef.set(payload, SetOptions.merge()).await()
            Log.d("Firestore", "User profile synced successfully")
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to sync user profile", e)
        }
    }

    /**
     * Record completed Pomodoro or focus session in Firestore
     */
    suspend fun recordStudySession(durationMinutes: Int, mode: String, soundUsed: String) {
        val uid = auth.currentUser?.uid ?: return
        val sessionRef = db.collection("users").document(uid).collection("sessions").document()

        val payload = hashMapOf<String, Any>(
            "id" to sessionRef.id,
            "userId" to uid,
            "durationMinutes" to durationMinutes,
            "mode" to mode,
            "soundUsed" to soundUsed,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            sessionRef.set(payload).await()
            Log.d("Firestore", "Study session recorded in cloud")
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to record study session", e)
        }
    }

    /**
     * Record daily study log & reflection in Firestore
     */
    suspend fun recordDailyLog(log: DailyStudyLog) {
        val uid = auth.currentUser?.uid ?: return
        val logRef = db.collection("users").document(uid).collection("logs").document(log.dateKey)

        val payload = hashMapOf<String, Any>(
            "id" to logRef.id,
            "userId" to uid,
            "dateKey" to log.dateKey,
            "topic" to log.topic,
            "questionsAttempted" to log.questionsAttempted,
            "questionsCorrect" to log.questionsCorrect,
            "accuracy" to log.accuracy,
            "notes" to log.notes,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            logRef.set(payload, SetOptions.merge()).await()
            Log.d("Firestore", "Daily log synced to cloud")
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to sync daily log", e)
        }
    }

    /**
     * Real-time observation of user cloud profile
     */
    fun observeCloudProfile(): Flow<Map<String, Any>?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Listen failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.data)
                } else {
                    trySend(null)
                }
            }

        awaitClose { registration.remove() }
    }
}
