package io.agora.board.forge.yniffi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.board.forge.yniffi.theme.FoobarTheme
import io.agora.board.yjs.YContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FoobarTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    CenterButtons()
                }
            }
        }
    }
}

@Composable
fun CenterButtons() {
    val context = LocalContext.current

    fun runAllTests() {
        Thread {
            // 支持多个 snapshot 文件名
            val snapshotFiles = listOf("500K.bin", "980K.bin", "1800K.bin")

            // 记录所有结果
            data class Result(
                val mode: String,
                val file: String,
                val applyTime: Long,
                val encodeTime: Long,
                val encodedSize: Int
            )

            val results = mutableListOf<Result>()

            // 1. 使用 YContext(context) 实现
            for (file in snapshotFiles) {
                val tLoad = System.currentTimeMillis()
                val snapshot = Utils.getAssetsFile(context, file)
                val tLoaded = System.currentTimeMillis()

                val yContext = YContext(context)
                val tCtx = System.currentTimeMillis()
                val yDoc = yContext.createDoc()
                val tDoc = System.currentTimeMillis()

                val tApplyStart = System.currentTimeMillis()
                yContext.applyUpdate(yDoc, snapshot)
                val tApplyEnd = System.currentTimeMillis()

                val tEncodeStart = System.currentTimeMillis()
                val encoded = yContext.encodeStateAsUpdate(yDoc)
                val tEncodeEnd = System.currentTimeMillis()

                results.add(
                    Result(
                        mode = "YContext",
                        file = file,
                        applyTime = tApplyEnd - tApplyStart,
                        encodeTime = tEncodeEnd - tEncodeStart,
                        encodedSize = encoded.size
                    )
                )
                Log.d(
                    "TestButton",
                    "YContext-$file: load=${tLoaded - tLoad}ms, ctx=${tCtx - tLoaded}ms, doc=${tDoc - tCtx}ms, apply=${tApplyEnd - tApplyStart}ms, encode=${tEncodeEnd - tEncodeStart}ms"
                )
            }

            // 2. 使用 YDocument() 实现
            for (file in snapshotFiles) {
                val tLoad = System.currentTimeMillis()
                val snapshot = Utils.getAssetsFile(context, file)
                val tLoaded = System.currentTimeMillis()

                val yDoc = YDocument()
                val tDoc = System.currentTimeMillis()

                val tApplyStart = System.currentTimeMillis()
                yDoc.applyUpdate(snapshot)
                val tApplyEnd = System.currentTimeMillis()

                val tEncodeStart = System.currentTimeMillis()
                val encoded = yDoc.encodeStateAsUpdate()
                val tEncodeEnd = System.currentTimeMillis()

                results.add(
                    Result(
                        mode = "YDocument",
                        file = file,
                        applyTime = tApplyEnd - tApplyStart,
                        encodeTime = tEncodeEnd - tEncodeStart,
                        encodedSize = encoded.size
                    )
                )
                Log.d(
                    "TestButton",
                    "YDocument-$file: load=${tLoaded - tLoad}ms, doc=${tDoc - tLoaded}ms, apply=${tApplyEnd - tApplyStart}ms, encode=${tEncodeEnd - tEncodeStart}ms"
                )
            }

            // 3. 其他实现方式（如有，可补充）
            // for (file in snapshotFiles) { ... }

            // 输出表格
            val sb = StringBuilder()
            sb.append(
                String.format(
                    "%-10s %-12s %-18s %-25s %-12s\n",
                    "Mode",
                    "File",
                    "ApplyUpdate(ms)",
                    "EncodeStateAsUpdate(ms)",
                    "EncodedSize"
                )
            )

            for (r in results) {
                sb.append(
                    String.format(
                        "%-10s %-12s %-18d %-25d %,12d\n",
                        r.mode, r.file, r.applyTime, r.encodeTime, r.encodedSize
                    )
                )
            }
            Log.i("TestButton", "==== Test Results ====\n$sb")
        }.start()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                Log.d("TestButton", "Run All Tests clicked")
                runAllTests()
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Run All Tests")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FoobarTheme { CenterButtons() }
}
