package com.drducqy.edb.data.repository

import android.content.Context
import android.util.Log
import com.drducqy.edb.core.DictionaryTrie
import com.drducqy.edb.data.source.AssetDataSource
import com.drducqy.edb.data.source.FileDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.net.URL

class DictionaryRepository(
    private val assetSource: AssetDataSource,
    private val fileSource: FileDataSource,
    private val trie: DictionaryTrie,
    private val context: Context
) {
    // Bảng mã chuyển đổi Phồn thể -> Giản thể (dùng cho tiếng Trung)
    val t2sMap = HashMap<Char, Char>()

    companion object {
        const val IGNORE_TAG = "[[IGNORE]]"
    }

    suspend fun loadData(langCode: String) = withContext(Dispatchers.IO) {
        System.gc() // Dọn dẹp bộ nhớ
        trie.clear()
        t2sMap.clear()

        Log.d("EDB_APP", "--- BẮT ĐẦU NẠP DỮ LIỆU: $langCode ---")
        val startTime = System.currentTimeMillis()

        if (langCode == "en") {
            // ====================================================
            // LOGIC NẠP TIẾNG ANH
            // ====================================================

            // 1. Nạp từ điển chính (Hỗ trợ cả format 109K và Key=Value)
            loadEnglishDictionary("en/English.txt")

            // 2. Nạp VietPhrase (nếu có, để hỗ trợ các cụm từ)
            loadAllVietPhrases("en")

            // 3. Nạp tên riêng & cấu trúc
            loadFromFile("en/Names.txt")
            loadStructureFile("en/Structure.txt")

        } else if (langCode == "zh") {
            // ====================================================
            // LOGIC NẠP TIẾNG TRUNG
            // ====================================================

            // 1. Nạp bảng mã Phồn -> Giản (Quan trọng nhất để chuẩn hóa đầu vào)
            loadTraditionalToSimplifiedMap("zh/ChinesePhonToGian.txt")

            // 2. Nạp Bảng Dấu Câu (Chuyển 。thành .)
            loadMarkDictionary("zh/Mark.txt")

            // 3. Nạp danh sách từ Bỏ qua (Ignored)
            loadIgnoredList("zh/IgnoredChinesePhrases.txt")

            // 4. Nạp từ điển theo thứ tự ưu tiên (Càng về sau càng ưu tiên cao)

            // Tầng Đáy: Phiên âm & Từ điển gốc
            loadFromFile("zh/ChinesePhienAmWords.txt")
            loadFromFile("zh/ThieuChuu.txt")
            loadFromFile("zh/LacViet.txt") // File này thường nhiều rác, code sẽ tự lọc

            // Tầng VietPhrase (Nghĩa chính)
            loadAllVietPhrases("zh")

            // Tầng Xưng hô
            loadFromFile("zh/Pronouns.txt")

            // Tầng Luật Nhân & Tên Riêng (Ưu tiên cao)
            loadFromFile("zh/LuatNhancu.txt")
            loadFromFile("zh/LuatNhan.txt")
            loadFromFile("zh/Names.txt")
            loadFromFile("zh/Names2.txt")

            // 5. Chốt chặn cuối cùng: Ép buộc các hư từ phải bị xóa
            forceIgnoreWords()
        }

        System.gc()
        val duration = System.currentTimeMillis() - startTime
        Log.d("EDB_APP", "--- HOÀN TẤT NẠP $langCode TRONG ${duration}ms ---")
    }

    /**
     * HÀM NẠP TỪ ĐIỂN TIẾNG ANH THÔNG MINH
     * Hỗ trợ cả 2 định dạng:
     * 1. 109K: @apple ... - quả táo
     * 2. Chuẩn: Hello=Xin chào
     */
    private fun loadEnglishDictionary(filePath: String) {
        try {
            var reader = fileSource.getReader(filePath)
            if (reader == null) reader = assetSource.getFileStream(filePath)

            var currentWord = ""

            reader.useLines { lines ->
                lines.forEach { line ->
                    val trimLine = line.trim()
                    if (trimLine.isEmpty()) return@forEach

                    // --- XỬ LÝ ĐỊNH DẠNG 109K (@word ... - nghĩa) ---
                    if (trimLine.startsWith("@")) {
                        // Tìm thấy từ mới (VD: @apple /'æpl/) -> Cắt bỏ phần phiên âm
                        currentWord = trimLine.substring(1).substringBefore("/").trim()
                    }
                    else if (trimLine.startsWith("-") && currentWord.isNotEmpty()) {
                        // Tìm thấy nghĩa (VD: - quả táo)
                        var meaning = trimLine.substring(1).trim()

                        // Làm sạch nghĩa (bỏ dấu ; hoặc , ở cuối)
                        if (meaning.contains(";")) meaning = meaning.substringBefore(";")
                        if (meaning.contains(",")) meaning = meaning.substringBefore(",")

                        if (meaning.isNotEmpty()) {
                            trie.insert(currentWord, meaning)
                            // Reset currentWord để chỉ lấy nghĩa đầu tiên (nghĩa phổ biến nhất)
                            currentWord = ""
                        }
                    }
                    // --- XỬ LÝ ĐỊNH DẠNG CHUẨN (Key=Value) ---
                    else if (trimLine.contains("=")) {
                        val parts = trimLine.split("=", limit = 2)
                        if (parts.size >= 2) {
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            if (key.isNotEmpty() && value.isNotEmpty()) {
                                trie.insert(key, value)
                            }
                        }
                    }
                }
            }
            Log.d("EDB_APP", "Đã nạp English Dictionary: $filePath")
        } catch (e: Exception) {
            Log.w("EDB_APP", "Lỗi nạp English Dict: ${e.message}")
        }
    }

    private fun forceIgnoreWords() {
        // Danh sách các từ tiếng Trung bắt buộc phải xóa (Hardcode)
        val hardcodedIgnores = listOf("的", "了", "之", "着", "地", "得")
        for (word in hardcodedIgnores) {
            trie.insert(word, IGNORE_TAG)
            trie.ignoredPhrases.add(word)
        }
    }

    private fun loadAllVietPhrases(langCode: String) {
        try {
            val files = context.assets.list(langCode) ?: emptyArray()
            val dictFiles = files.filter { it.startsWith("VietPhrase") && it.endsWith(".txt") }

            if (dictFiles.isEmpty()) {
                loadFromFile("$langCode/VietPhrase.txt")
            } else {
                for (fileName in dictFiles.sorted()) {
                    loadFromFile("$langCode/$fileName")
                }
            }
        } catch (e: Exception) {
            loadFromFile("$langCode/VietPhrase.txt")
        }
    }

    private fun loadTraditionalToSimplifiedMap(filePath: String) {
        try {
            var reader = fileSource.getReader(filePath)
            if (reader == null) reader = assetSource.getFileStream(filePath)

            reader.useLines { lines ->
                lines.forEach { line ->
                    // Định dạng: 萬=万
                    val parts = line.split("=")
                    if (parts.size >= 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                        t2sMap[parts[0][0]] = parts[1][0]
                    }
                }
            }
            Log.d("EDB_APP", "Đã nạp bảng Phồn-Giản: ${t2sMap.size} mục")
        } catch (e: Exception) { }
    }

    private fun loadMarkDictionary(filePath: String) {
        try {
            var reader = fileSource.getReader(filePath)
            if (reader == null) reader = assetSource.getFileStream(filePath)

            reader.useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size >= 2) {
                        trie.markMap[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun loadIgnoredList(filePath: String) {
        try {
            var reader = fileSource.getReader(filePath)
            if (reader == null) reader = assetSource.getFileStream(filePath)

            reader.useLines { lines ->
                lines.forEach { line ->
                    val word = line.trim()
                    if (word.isNotEmpty() && !word.startsWith("#")) {
                        trie.ignoredPhrases.add(word)
                        trie.insert(word, IGNORE_TAG)
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun loadFromFile(filePath: String) {
        try {
            var reader = fileSource.getReader(filePath)
            if (reader == null) reader = assetSource.getFileStream(filePath)

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isBlank() || line.startsWith("#")) return@forEach

                    val eqIndex = line.indexOf('=')
                    if (eqIndex > 0) {
                        val key = line.substring(0, eqIndex).trim()

                        // Nếu từ này đã bị đánh dấu Ignore thì không nạp đè
                        if (trie.ignoredPhrases.contains(key)) return@forEach

                        if (eqIndex + 1 < line.length) {
                            var value = line.substring(eqIndex + 1).trim()

                            // --- LOGIC LÀM SẠCH DỮ LIỆU ---
                            // Xóa ký tự rác đầu dòng (VD: /như thế -> như thế)
                            while (value.startsWith("/") || value.startsWith(";") || value.startsWith(",")) {
                                value = value.substring(1).trim()
                            }

                            // Chỉ lấy 1 nghĩa đầu tiên
                            val slashIndex = value.indexOf('/')
                            if (slashIndex > 0) value = value.substring(0, slashIndex).trim()
                            val commaIndex = value.indexOf(',')
                            if (commaIndex > 0) value = value.substring(0, commaIndex).trim()
                            val semiIndex = value.indexOf(';')
                            if (semiIndex > 0) value = value.substring(0, semiIndex).trim()

                            // Xóa pinyin [xxx] và dấu +
                            val bracketIndex = value.indexOf('[')
                            if (bracketIndex > 0) value = value.substring(0, bracketIndex).trim()
                            value = value.replace("✚", "")

                            if (key.isNotEmpty() && value.isNotEmpty()) {
                                trie.insert(key, value)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun loadStructureFile(filePath: String) {
        try {
            var reader = fileSource.getReader(filePath)
            if (reader == null) reader = assetSource.getFileStream(filePath)
            reader.useLines { lines ->
                lines.forEach { line ->
                    val eqIndex = line.indexOf('=')
                    if (eqIndex > 0) {
                        trie.addStructure(line.substring(0, eqIndex).trim(), line.substring(eqIndex + 1).trim())
                    }
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun downloadDictionary(urlString: String, saveFileName: String) = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connect()
            val input = connection.getInputStream()
            val outputFile = fileSource.getFile(saveFileName)
            val output = FileOutputStream(outputFile)
            input.use { inp -> output.use { out -> inp.copyTo(out) } }
        } catch (e: Exception) { }
    }
}