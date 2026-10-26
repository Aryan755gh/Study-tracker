package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "StudyAiCoach"

sealed interface AiCoachState {
    data object Idle : AiCoachState
    data object Loading : AiCoachState
    data class Success(val response: String) : AiCoachState
    data class Error(val message: String) : AiCoachState
}

class StudyAiCoach {

    // Lazy initialization per Firebase AI Logic guidelines
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the StudyOS AI Academic Mentor & Coach specializing in core biology, biochemistry, molecular biology, and competitive science examinations (such as TIFR Biology, CSIR-NET, JAM, and GATE).
                    You provide crisp, high-yield, conceptually rigorous explanations.
                    When analyzing question mistakes, always categorize them into:
                    - [K] Knowledge Gap
                    - [R] Recall Failure
                    - [A] Application Error
                    - [D] Data Interpretation
                    - [S] Strategy Error
                    Keep explanations focused, direct, structured with bullet points, and actionable.
                    """.trimIndent()
                )
            }
        )
    }

    /**
     * Ask a concept doubt or lecture question
     */
    suspend fun explainConcept(topic: String, userQuestion: String): String {
        return try {
            withTimeout(60_000L) {
                val prompt = "Topic: $topic\nDoubt / Concept: $userQuestion\nPlease provide a clear, high-yield academic explanation with key takeaways."
                val response = model.generateContent(prompt)
                response.text ?: "No explanation returned from Gemini."
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Concept explanation timed out", e)
            throw IllegalStateException("AI response timed out after 60 seconds. Please check your connection and retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI generation failed", e)
            throw e
        }
    }

    /**
     * Analyze mistake notes and classify into K/R/A/D/S error log
     */
    suspend fun analyzeMistakes(topic: String, notes: String): String {
        return try {
            withTimeout(60_000L) {
                val prompt = """
                    Topic: $topic
                    Student's mistake / difficulty notes: $notes
                    
                    Classify each difficulty into [K] (Knowledge), [R] (Recall), [A] (Application), [D] (Data interpretation), or [S] (Strategy).
                    Provide 2 actionable revision action items for tomorrow's first study block.
                """.trimIndent()
                val response = model.generateContent(prompt)
                response.text ?: "No analysis returned from Gemini."
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Mistake analysis timed out", e)
            throw IllegalStateException("AI analysis timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI analysis failed", e)
            throw e
        }
    }
}
