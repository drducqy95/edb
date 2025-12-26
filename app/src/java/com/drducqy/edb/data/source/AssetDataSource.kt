package com.drducqy.edb.data.source

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Class này chịu trách nhiệm mở luồng đọc file từ thư mục Assets
 */
class AssetDataSource(private val context: Context) {

    // Hàm trả về BufferedReader để đọc từng dòng
    fun getFileStream(fileName: String): BufferedReader {
        val inputStream = context.assets.open(fileName)
        return BufferedReader(InputStreamReader(inputStream))
    }
}