package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.metadata.Lyrics

/*
 * Fork: lyrics in English or Vietnamese are never translated — the listener reads both. Everything
 * else (Korean, Japanese, Chinese, Spanish…) still is.
 *
 * There is no language tag on lyrics, so the language is read off the text itself, which is good
 * enough for these two:
 *  - Vietnamese has letters no other language this app meets uses (ă â đ ê ô ơ ư and the stacked
 *    tone marks: ạ ả ấ ầ ẩ ẫ ậ …). A sheet where a fair share of lines carry one is Vietnamese —
 *    including the many Vietnamese songs with English lines mixed in.
 *  - English is plain a–z with no other script, AND a real share of its words are the small words
 *    English cannot avoid ("the", "you", "and"…). Plain a–z alone would also match Indonesian or
 *    Spanish written without accents, which should still be translated.
 */

/** True when these lyrics are English or Vietnamese, the two languages never translated. */
fun Lyrics.isEnglishOrVietnamese(): Boolean {
    val text =
        lines
            .orEmpty()
            .joinToString("\n") { it.words.stripTimestamps() }
            .ifBlank { return false }
    return text.isVietnamese() || text.isEnglish()
}

private fun String.isVietnamese(): Boolean {
    val lines = lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return false
    val vietnameseLines = lines.count { line -> line.any { it.lowercaseChar() in VIETNAMESE_LETTERS } }
    return vietnameseLines >= lines.size * VIETNAMESE_LINE_SHARE
}

private fun String.isEnglish(): Boolean {
    val letters = filter { it.isLetter() }
    if (letters.isEmpty()) return false
    val asciiShare = letters.count { it in 'a'..'z' || it in 'A'..'Z' }.toFloat() / letters.length
    if (asciiShare < ENGLISH_ASCII_SHARE) return false
    val words = lowercase().split(Regex("[^a-z']+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return false
    val common = words.count { it in ENGLISH_COMMON_WORDS }
    return common >= words.size * ENGLISH_COMMON_SHARE
}

// Rich-synced lines carry <mm:ss.xx> markers inside the text.
private fun String.stripTimestamps(): String = replace(Regex("<\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?>"), " ")

private const val VIETNAMESE_LETTERS =
    "ăâđêôơư" +
        "áàảãạấầẩẫậắằẳẵặ" +
        "éèẻẽẹếềểễệ" +
        "íìỉĩị" +
        "óòỏõọốồổỗộớờởỡợ" +
        "úùủũụứừửữự" +
        "ýỳỷỹỵ"

private val ENGLISH_COMMON_WORDS =
    setOf(
        "the", "a", "an", "and", "or", "but", "i", "i'm", "you", "you're", "me", "my", "your", "we", "it", "it's",
        "is", "are", "was", "be", "to", "of", "in", "on", "for", "with", "that", "this", "so", "all", "just",
        "don't", "can't", "know", "love", "like", "when", "what", "if", "not", "no", "oh", "baby", "now", "never",
    )

// A quarter of the lines with a Vietnamese letter is plenty: Vietnamese text has one in almost
// every line, and a song in another language almost never has any.
private const val VIETNAMESE_LINE_SHARE = 0.25f

// Almost all letters a–z, and at least one word in seven one of the common words above — ordinary
// English runs well above that, other a–z languages well below it.
private const val ENGLISH_ASCII_SHARE = 0.95f
private const val ENGLISH_COMMON_SHARE = 1f / 7f
