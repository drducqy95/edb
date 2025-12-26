package com.drducqy.edb.core

class DictionaryTrie {
    val root = TrieNode()
    val patterns = mutableListOf<Pair<Regex, String>>()

    // Danh sách từ cần bỏ qua (để kiểm tra nhanh)
    val ignoredPhrases = HashSet<String>()

    // Bảng chuyển đổi dấu câu (VD: 。-> .)
    val markMap = HashMap<String, String>()

    class TrieNode {
        // Dùng HashMap để tiết kiệm bộ nhớ cho các node lá
        var children: HashMap<Char, TrieNode>? = null
        var meaning: String? = null
        var isEndOfWord = false
    }

    fun clear() {
        root.children?.clear()
        root.children = null
        patterns.clear()
        ignoredPhrases.clear()
        markMap.clear()
        root.meaning = null
        root.isEndOfWord = false
    }

    fun insert(word: String, meaning: String) {
        if (word.isBlank()) return
        var node = root
        // Chuyển về chữ thường để tra cứu không phân biệt hoa thường
        for (char in word.lowercase()) {
            if (node.children == null) {
                node.children = HashMap(2)
            }
            // Tạo node con nếu chưa có
            var child = node.children!![char]
            if (child == null) {
                child = TrieNode()
                node.children!![char] = child
            }
            node = child
        }
        node.isEndOfWord = true
        node.meaning = meaning
    }

    fun addStructure(regexStr: String, replacement: String) {
        try {
            val regex = Regex(regexStr, RegexOption.IGNORE_CASE)
            patterns.add(regex to replacement)
        } catch (e: Exception) { }
    }

    // Thuật toán tìm kiếm cụm từ dài nhất (Longest Matching)
    fun searchLongestMatch(text: String, startIndex: Int): Pair<Int, String?> {
        var node = root
        var lastMatchLength = 0
        var lastMatchMeaning: String? = null
        var currentLength = 0

        for (i in startIndex until text.length) {
            val char = text[i].lowercaseChar()

            if (node.children == null) break
            val nextNode = node.children!![char] ?: break

            node = nextNode
            currentLength++

            if (node.isEndOfWord) {
                lastMatchLength = currentLength
                lastMatchMeaning = node.meaning
            }
        }

        return Pair(lastMatchLength, lastMatchMeaning)
    }
}