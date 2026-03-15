package com.sylvester.rustsensei.llm

import com.sylvester.rustsensei.data.ChatMessage

object ChatTemplateFormatter {

    private const val SYSTEM_PROMPT = """You are RustSensei, an expert Rust programming tutor. The student is an experienced Go, Python, and TypeScript developer learning Rust by building CLI tools.

Your teaching style:
- Draw parallels to Go/Python/TypeScript concepts they already know
- Explain ownership, borrowing, and lifetimes with practical examples
- When reviewing code, explain what the borrow checker is doing and why
- Keep explanations concise with code snippets
- Guide them to write the code themselves rather than giving full solutions"""

    fun formatMessages(messages: List<ChatMessage>, contextLength: Int = 2048): String {
        val sb = StringBuilder()

        // Always include system prompt
        sb.append("<|im_start|>system\n")
        sb.append(SYSTEM_PROMPT)
        sb.append("<|im_end|>\n")

        // Add conversation messages (apply sliding window if needed)
        // Simple heuristic: ~4 chars per token on average
        val maxChars = contextLength * 4
        val systemChars = sb.length

        // Build messages from most recent backwards, keeping what fits
        val messagesToInclude = mutableListOf<ChatMessage>()
        var charCount = systemChars + 50 // reserve for assistant prompt

        for (msg in messages.reversed()) {
            val msgChars = msg.content.length + 30 // overhead for template tags
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

        // Add the assistant prompt to begin generation
        sb.append("<|im_start|>assistant\n")

        return sb.toString()
    }
}
