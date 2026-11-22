package com.example.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAdvisorRepository"

data class AdvisorMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

class AiAdvisorRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the LG Digital Quantitative Trading Copilot inside a crypto research & paper-execution desk.
                    Guidelines:
                    - Ground every answer in the live portfolio, market ticker, and deterministic strategy metrics supplied in the prompt context.
                    - Focus on risk management, drawdown limits, strategy behavior (SMA/EMA/MACD/RSI/Bollinger/Supertrend/Breakout/Grid/VWAP/ATR), and cross-exchange arbitrage spread mechanics (taker fees, slippage, rebalance reserve).
                    - Clearly state that the Android workspace executes paper orders and public market reads only (never claim to execute real exchange funds).
                    - Keep formatting crisp with bullet points and concise numbers. Never give personalized legal or tax advice.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamAdvisorReply(
        history: List<AdvisorMessage>,
        userPrompt: String,
        deskContextSummary: String
    ): Flow<String> = flow {
        val chatHistory: List<Content> = history
            .filter { !it.isError && it.text.isNotBlank() }
            .takeLast(8)
            .map { msg ->
                content(if (msg.isUser) "user" else "model") {
                    text(msg.text)
                }
            }

        val groundedPrompt = buildString {
            appendLine("[LIVE DESK TELEMETRY]")
            appendLine(deskContextSummary)
            appendLine("[USER QUESTION]")
            append(userPrompt)
        }

        try {
            val accumulated = StringBuilder()
            withTimeout(60_000L) {
                val chat = model.startChat(history = chatHistory)
                chat.sendMessageStream(groundedPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Advisor response timed out", e)
            throw IllegalStateException("AI Trading Advisor timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Firebase AI request failed.", e)
        }
    }
}
