package com.drducqy.edb.data.source

import android.content.Context
import java.io.File
import java.io.BufferedReader
import java.io.FileReader

class FileDataSource(private val context: Context) {

    // Lấy đường dẫn file trong bộ nhớ ứng dụng (/data/data/com.../files/)
    fun getFile(fileName: String): File {
        return File(context.filesDir, fileName)
    }

    // Đọc file nếu tồn tại
    fun getReader(fileName: String): BufferedReader? {
        val file = getFile(fileName)
        return if (file.exists()) {
            BufferedReader(FileReader(file))
        } else {
            null
        }
    }
}