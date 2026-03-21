package com.zy.ppmusic.mvp.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zy.ppmusic.R
import com.zy.ppmusic.compose.theme.LocalMusicComposeTheme
import com.zy.ppmusic.utils.DateUtil
import com.zy.ppmusic.utils.FileUtils
import com.zy.ppmusic.utils.PrintLog
import com.zy.ppmusic.utils.StreamUtils
import java.io.File
import java.io.PrintWriter
import kotlin.system.exitProcess

/**
 * @author stealfeam
 * @date 2018-04-03 15:04:35
 * 错误页面，用来处理应用崩溃显示友好界面
 */
class ErrorActivity : AppCompatActivity() {

    companion object {
        const val ERROR_INFO = "ERROR_INFO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.setBackgroundColor(Color.TRANSPARENT)

        val errorInfo = getErrorInfo()
        writeMsgToLocal(errorInfo)

        setContent {
            LocalMusicComposeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ErrorScreen(
                        errorSummary = errorInfo.javaClass.simpleName.takeIf { it.isNotBlank() },
                        onConfirm = ::closeApp,
                    )
                }
            }
        }
    }

    private fun writeMsgToLocal(errorInfo: Throwable) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            PrintLog.e(errorInfo.message ?: "error msg is null")
            return
        }
        val errorFile = File(FileUtils.downloadFile + "/music_error_log.txt")
        if (!errorFile.exists()) errorFile.createNewFile()
        val writer = PrintWriter(errorFile)
        writer.println("---- " + DateUtil.get().getTime(System.currentTimeMillis()) + " ----")
        errorInfo.printStackTrace(writer)
        writer.flush()
        writer.close()
        StreamUtils.closeIo(writer)
    }

    private fun getErrorInfo(): Throwable {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(ERROR_INFO, Throwable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(ERROR_INFO) as? Throwable
        } ?: IllegalStateException("missing crash info")
    }

    private fun closeApp() {
        finish()
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}

@Composable
private fun ErrorScreen(
    errorSummary: String?,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            colors.primary.copy(alpha = 0.14f),
            colors.secondary.copy(alpha = 0.08f),
            colors.background,
        ),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(140.dp)
                .background(
                    color = colors.primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(40.dp),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(180.dp)
                .background(
                    color = colors.tertiary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(56.dp),
                ),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colors.errorContainer.copy(alpha = 0.72f),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = colors.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(24.dp),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "恢复页",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onErrorContainer,
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onErrorContainer,
                        )
                    }
                }
                Text(
                    text = "应用遇到未预期的问题",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "当前流程已经中断。建议先退出应用，稍后重新打开后再继续使用。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface.copy(alpha = 0.80f),
                    textAlign = TextAlign.Center,
                )
                errorSummary?.let { summary ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.7f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "错误摘要",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurfaceVariant,
                            )
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = colors.primary.copy(alpha = 0.08f),
                ) {
                    Text(
                        text = "如果系统权限允许，错误日志会尝试写入本地，便于后续排查。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "退出应用")
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F8F9)
@Composable
private fun ErrorScreenPreview() {
    LocalMusicComposeTheme {
        ErrorScreen(errorSummary = "IllegalStateException", onConfirm = {})
    }
}
