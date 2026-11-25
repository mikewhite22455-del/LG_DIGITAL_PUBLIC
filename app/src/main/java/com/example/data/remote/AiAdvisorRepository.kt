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
                    You are the LG Digital Quantitative Trading Copilot inside a mobile crypto trading & research desk.
                    Guidelines:
                    - Ground every answer in the live portfolio, execution mode (Paper vs Live), market ticker, and deterministic strategy metrics supplied in the prompt context.
                    - Focus on risk management, position sizing, drawdown limits, strategy behavior (SMA/EMA/MACD/RSI/Bollinger/Supertrend/Breakout/Grid/VWAP/ATR), and cross-exchange arbitrage spread mechanics (taker fees, slippage, rebalance reserve).
                    - Clearly distinguish between Paper Mode (simulated local ledger) and Live Mode (direct user-signed exchange API execution on Kraken, Coinbase, KuCoin, or Bitstamp). Remind users that you provide analysis only and never execute orders automatically without operator action.
                    - Keep formatting crisp with concise bullet points and clear numbers. Never give personalized legal or tax advice.
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
        // Gemini startChat requires history to begin with a "user" turn, strictly alternate
        // "user" -> "model", and end on a "model" turn before sending the next user message.
        val filtered = history.filter {
            it.id != "welcome" && !it.isError && it.text.isNotBlank()
        }
        val alternatingTurns = mutableListOf<AdvisorMessage>()
        var expectUser = true
        for (msg in filtered) {
            when {
                expectUser && msg.isUser -> {
                    alternatingTurns.add(msg)
                    expectUser = false
                }
                !expectUser && !msg.isUser -> {
                    alternatingTurns.add(msg)
                    expectUser = true
                }
                !expectUser && msg.isUser -> {
                    // Replace unanswered dangling user turn so roles stay strictly alternating.
                    alternatingTurns[alternatingTurns.lastIndex] = msg
                }
                // Leading model messages while expectUser == true are intentionally skipped.
            }
        }
        if (alternatingTurns.isNotEmpty() && alternatingTurns.last().isUser) {
            alternatingTurns.removeAt(alternatingTurns.lastIndex)
        }
        val boundedTurns = if (alternatingTurns.size > 8) {
            val tail = alternatingTurns.takeLast(8)
            if (tail.firstOrNull()?.isUser == true) tail else tail.drop(1)
        } else {
            alternatingTurns
        }
        val chatHistory: List<Content> = boundedTurns.map { msg ->
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
                val responseStream = if (chatHistory.isEmpty()) {
                    model.generateContentStream(groundedPrompt)
                } else {
                    val chat = model.startChat(history = chatHistory)
                    chat.sendMessageStream(groundedPrompt)
                }
                responseStream.collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI Trading Advisor returned an empty response. Please try again.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Advisor response timed out", e)
            throw IllegalStateException("AI Trading Advisor timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "AI Trading Advisor request failed.", e)
        }
    }
}
