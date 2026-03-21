package com.zy.ppmusic.compose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zy.ppmusic.R
import com.zy.ppmusic.compose.theme.LocalMusicComposeTheme

class ComposeSandboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalMusicComposeTheme {
                Surface {
                    ComposeSandboxScreen()
                }
            }
        }
    }

    companion object {
        fun action(context: Context) {
            context.startActivity(Intent(context, ComposeSandboxActivity::class.java))
        }
    }
}

@Composable
fun ComposeSandboxScreen(
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(colors.primary.copy(alpha = 0.14f), colors.background)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AssistChip(
                onClick = {},
                label = { Text(text = stringResource(R.string.compose_sandbox_chip)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = colors.surface,
                    labelColor = colors.primary
                )
            )

            Text(
                text = stringResource(R.string.compose_sandbox_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )

            Text(
                text = stringResource(R.string.compose_sandbox_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground.copy(alpha = 0.78f)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.compose_sandbox_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.compose_sandbox_section_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface.copy(alpha = 0.78f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {},
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(text = stringResource(R.string.compose_sandbox_button))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F8F9)
@Composable
private fun ComposeSandboxScreenPreview() {
    LocalMusicComposeTheme {
        ComposeSandboxScreen()
    }
}
