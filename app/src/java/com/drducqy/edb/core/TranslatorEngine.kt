package com.drducqy.edb.core

import com.drducqy.edb.data.repository.DictionaryRepository

class TranslatorEngine(
    private val dictionary: DictionaryTrie,
    private val repository: DictionaryRepository
) {
    private val fallbackStopWords = setOf(
        "đích", "liễu", "chi", "giả", "hồ", "nha", "nhé", "của", "rồi", "mà", "thì"
    )

    fun translate(text: String): String {
        if (text.isEmpty()) return ""

        // 1. CHUẨN HÓA (CHỈ TIẾNG TRUNG)
        // Nếu map rỗng (đang ở tiếng Anh), hàm này sẽ trả về nguyên văn -> An toàn
        val simplifiedText = convertTraditionalToSimplified(text.trim())

        // 2. REGEX
        var processedText = simplifiedText
        for ((regex, replacement) in dictionary.patterns) {
            processedText = regex.replace(processedText, replacement)
        }

        val result = StringBuilder()
        var i = 0
        val len = processedText.length

        while (i < len) {
            val currentChar = processedText[i]

            if (currentChar == '\n') {
                result.append(currentChar)
                i++
                continue
            }

            // 3. TRA TỪ
            val (matchLength, rawMeaning) = dictionary.searchLongestMatch(processedText, i)

            if (rawMeaning != null) {
                // Kiểm tra IGNORE
                val originalWord = processedText.substring(i, i + matchLength)
                if (rawMeaning == DictionaryRepository.IGNORE_TAG ||
                    dictionary.ignoredPhrases.contains(originalWord)) {
                    i += matchLength
                    continue
                }

                val endPosition = i + matchLength
                if (isChineseChar(processedText[i]) || isWordBoundary(processedText, endPosition)) {

                    val finalMeaning = rawMeaning.trim()

                    // Kiểm tra từ cấm tiếng Việt (chỉ nên áp dụng cho tiếng Trung nếu cần)
                    if (repository.t2sMap.isNotEmpty() && finalMeaning.lowercase() in fallbackStopWords) {
                        i += matchLength
                        continue
                    }

                    // Smart Spacing
                    if (result.isNotEmpty()) {
                        val lastChar = result.last()
                        if (!lastChar.isWhitespace() && lastChar != '\n' && lastChar != '(' && lastChar != '"') {
                            result.append(" ")
                        }
                    }

                    result.append(finalMeaning)
                    i += matchLength
                } else {
                    result.append(currentChar)
                    i++
                }
            } else {
                // XỬ LÝ DẤU CÂU (MARK)
                val charStr = currentChar.toString()
                if (dictionary.markMap.containsKey(charStr)) {
                    result.append(dictionary.markMap[charStr]!!)
                } else {
                    result.append(currentChar)
                }
                i++
            }
        }

        return formatSentenceCase(result.toString().trim())
    }

    private fun convertTraditionalToSimplified(text: String): String {
        // Tối ưu: Nếu không có map (tiếng Anh) thì return luôn
        if (repository.t2sMap.isEmpty()) return text

        val sb = StringBuilder()
        for (char in text) {
            val simplifiedChar = repository.t2sMap[char] ?: char
            sb.append(simplifiedChar)
        }
        return sb.toString()
    }

    private fun isChineseChar(char: Char): Boolean {
        return char.code in 0x4E00..0x9FFF
    }

    private fun isWordBoundary(text: String, index: Int): Boolean {
        if (index >= text.length) return true
        val nextChar = text[index]
        if (isChineseChar(nextChar)) return true
        return !nextChar.isLetter()
    }

    private fun formatSentenceCase(text: String): String {
        val sb = StringBuilder()
        var capitalizeNext = true
        for (char in text) {
            if (capitalizeNext && char.isLetter()) {
                sb.append(char.uppercaseChar())
                capitalizeNext = false
            } else {
                sb.append(char)
                if (char == '.' || char == '!' || char == '?' || char == '\n') {
                    capitalizeNext = true
                }
            }
        }
        var res = sb.toString()
        res = res.replace(" .", ".").replace(" ,", ",").replace(" !", "!").replace(" ?", "?").replace("  ", " ")
        return res
    }
}