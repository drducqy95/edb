package com.drducqy.edb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drducqy.edb.ui.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    // 1. Kết nối ViewModel
    val viewModel: MainViewModel = viewModel()
    val input by viewModel.inputText.collectAsState()
    val output by viewModel.outputText.collectAsState()
    val isReady by viewModel.isReady.collectAsState()

    // 2. Lấy Context và Clipboard Manager chuẩn
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    // Trạng thái nút ngôn ngữ đang chọn (để đổi màu nút)
    var currentLang by remember { mutableStateOf("en") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Translator",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isReady) {
                    Text("✅ Sẵn sàng", color = Color(0xFF4CAF50), fontSize = 14.sp)
                } else {
                    Text("⏳ Đang nạp...", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- NGÔN NGỮ (MỚI THÊM VÀO) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        currentLang = "en"
                        viewModel.switchLanguage("en")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentLang == "en") MaterialTheme.colorScheme.primary else Color.LightGray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🇬🇧 Tiếng Anh")
                }

                Button(
                    onClick = {
                        currentLang = "zh"
                        viewModel.switchLanguage("zh")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentLang == "zh") MaterialTheme.colorScheme.primary else Color.LightGray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🇨🇳 Tiếng Trung")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TOOLBAR BUTTONS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Nút DÁN (PASTE)
                Button(
                    onClick = {
                        val clipData = clipboardManager.primaryClip
                        val item = clipData?.getItemAt(0)
                        val pasteText = item?.text?.toString()

                        if (!pasteText.isNullOrEmpty()) {
                            viewModel.onTextChanged(pasteText)
                            Toast.makeText(context, "Đã dán văn bản!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard trống!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)), // Màu cam nhạt
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📋 Dán")
                }

                // Nút XÓA
                Button(
                    onClick = { viewModel.onTextChanged("") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)), // Màu đỏ nhạt
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🗑️ Xóa")
                }

                // Nút SAO CHÉP (COPY)
                Button(
                    onClick = {
                        if (output.isNotEmpty()) {
                            val clip = ClipData.newPlainText("Translated Text", output)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "Đã copy bản dịch!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6)), // Màu xanh nhạt
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sao chép")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- INPUT ---
            OutlinedTextField(
                value = input,
                onValueChange = { viewModel.onTextChanged(it) },
                label = { Text("Văn bản gốc") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Nhập hoặc dán văn bản vào đây...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                maxLines = Int.MAX_VALUE
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Bản dịch:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // --- OUTPUT ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val displayText = if (output.isEmpty()) "Kết quả sẽ hiện ở đây..." else output
                val displayColor = if (output.isEmpty()) Color.Gray else Color.Black

                Text(
                    text = displayText,
                    fontSize = 18.sp,
                    color = displayColor,
                    lineHeight = 28.sp
                )
            }
        }
    }
}