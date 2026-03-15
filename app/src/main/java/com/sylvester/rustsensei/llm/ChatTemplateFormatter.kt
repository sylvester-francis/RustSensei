package com.sylvester.rustsensei.llm

import com.sylvester.rustsensei.data.ChatMessage

object ChatTemplateFormatter {

    private const val SYSTEM_PROMPT = """You are RustSensei, an expert Rust programming tutor. The student is an experienced Go, Python, and TypeScript developer learning Rust by building CLI tools.

Your teaching style:
- Draw parallels to Go/Python/TypeScript concepts they already know
- Explain ownership, borrowing, and lifetimes with practical examples
- When reviewing code, explain what the borrow checker is doing and why
- Keep explanations concise with code snippets
- Guide them to write the code themselves rather than giving full solutions
- Keep responses focused and under 300 words unless a longer explanation is needed"""

    private const val MAX_RAG_CONTEXT_CHARS = 2400
    // PERF: limit conversation history to avoid massive prompts
    private const val MAX_HISTORY_MESSAGES = 8

    fun formatMessages(
        messages: List<ChatMessage>,
        contextLength: Int = 2048,
        ragContext: String? = null
    ): String {
        val sb = StringBuilder()

        sb.append("<|im_start|>system\n")
        sb.append(SYSTEM_PROMPT)
        if (!ragContext.isNullOrBlank()) {
            val truncated = if (ragContext.length > MAX_RAG_CONTEXT_CHARS) {
                ragContext.take(MAX_RAG_CONTEXT_CHARS) + "\n[...truncated]"
            } else {
                ragContext
            }
            sb.append("\n\n[CONTEXT]\n")
            sb.append(truncated)
            sb.append("\n[/CONTEXT]")
        }
        sb.append("<|im_end|>\n")

        // ~4 chars per token heuristic
        val maxChars = contextLength * 4
        val systemChars = sb.length
        val generationReserve = 1536 // ~384 tokens for response

        // PERF: take only the last N messages, then apply char budget
        val recentMessages = messages.takeLast(MAX_HISTORY_MESSAGES)
        val messagesToInclude = mutableListOf<ChatMessage>()
        var charCount = systemChars + 50 + generationReserve

        for (msg in recentMessages.reversed()) {
            val msgChars = msg.content.length + 30
            if (charCount + msgChars > maxChars && messagesToInclude.isNotEmpty()) {
                break
            }
            messagesToInclude.add(0, msg)
            charCount += msgChars
        }

        for (msg in messagesToInclude) {
            sb.append("<|im_start|>")
            sb.append(msg.role)
            sb.append("\n")
            sb.append(msg.content)
            sb.append("<|im_end|>\n")
        }

        sb.append("<|im_start|>assistant\n")

        return sb.toString()
    }
}
