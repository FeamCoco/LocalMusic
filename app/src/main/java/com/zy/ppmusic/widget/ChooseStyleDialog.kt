package com.zy.ppmusic.widget

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import com.zy.ppmusic.R
import com.zy.ppmusic.compose.theme.LocalMusicComposeTheme
import com.zy.ppmusic.mvp.contract.IChooseNotifyStyleContract
import com.zy.ppmusic.mvp.presenter.ChooseNotifyStylePresenter
import com.zy.ppmusic.service.MediaService
import com.zy.ppmusic.utils.Constant

/**
 * @author stealfeam
 * @date 2018/6/16
 */
class ChooseStyleDialog : DialogFragment(), IChooseNotifyStyleContract.IChooseNotifyStyleView {
    private val presenter: ChooseNotifyStylePresenter by lazy {
        ChooseNotifyStylePresenter(this)
    }

    private var selectedStyleId: Int = R.id.rb_choose_custom

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val localCheckId = presenter.getLocalStyle()
        selectedStyleId = if (localCheckId >= 0) localCheckId else R.id.rb_choose_custom
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AppCompatDialog(requireActivity(), R.style.NotifyDialogStyle).apply {
            val composeView = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    LocalMusicComposeTheme {
                        ChooseStyleDialogContent(
                            selectedStyleId = selectedStyleId,
                            onSelected = ::handleStyleSelected,
                            onDismiss = ::dismiss
                        )
                    }
                }
            }
            setContentView(
                composeView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun handleStyleSelected(checkedId: Int) {
        selectedStyleId = checkedId
        val mediaController = MediaControllerCompat.getMediaController(requireActivity())
        val extra = Bundle()
        extra.putInt(Constant.CHOOSE_STYLE_EXTRA, checkedId)
        mediaController.sendCommand(MediaService.COMMAND_CHANGE_NOTIFY_STYLE, extra, null)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        presenter.changeStyle(selectedStyleId)
    }
}

@Composable
private fun ChooseStyleDialogContent(
    selectedStyleId: Int,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = stringResource(R.string.choose_notify_style),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ChooseStyleOption(
                    title = stringResource(R.string.choose_custom_style),
                    selected = selectedStyleId == R.id.rb_choose_custom,
                    onClick = { onSelected(R.id.rb_choose_custom) }
                )

                ChooseStyleOption(
                    title = stringResource(R.string.choose_system_style),
                    selected = selectedStyleId == R.id.rb_choose_system,
                    onClick = { onSelected(R.id.rb_choose_system) }
                )
            }
        }
    }
}

@Composable
private fun ChooseStyleOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            }
        )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F8F9)
@Composable
private fun ChooseStyleDialogContentPreview() {
    LocalMusicComposeTheme {
        var selectedStyleId by remember { mutableIntStateOf(R.id.rb_choose_custom) }
        ChooseStyleDialogContent(
            selectedStyleId = selectedStyleId,
            onSelected = { selectedStyleId = it },
            onDismiss = {}
        )
    }
}

