package com.drducqy.edb.core

import java.util.HashMap

/**
 * Node của cây Trie.
 * - children: Chứa các ký tự tiếp theo.
 * - meaning: Nghĩa tiếng Việt (chỉ có dữ liệu nếu node này là kết thúc của 1 từ).
 * - isEndOfWord: Cờ đánh dấu điểm kết thúc.
 */
class TrieNode {
    val children: MutableMap<Char, TrieNode> = HashMap()
    var meaning: String? = null
    var isEndOfWord: Boolean = false
}