package com.xgy.androidkaraoke.ui.components

import android.content.Intent
import android.speech.RecognizerIntent
import android.view.KeyEvent as AndroidKeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.theme.KaraokeColors

/** 返回后再按确定：输入框还占着焦点，系统不会自己把键盘叫出来，要强制再开一次。 */
@Composable
fun rememberShowTvIme(focus: FocusRequester): () -> Unit {
    val view = LocalView.current
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(focus, view, keyboard) {
        {
            runCatching { focus.requestFocus() }
            keyboard?.show()
            view.post {
                val imm = view.context.getSystemService(InputMethodManager::class.java) ?: return@post
                val target = view.findFocus() ?: view
                imm.restartInput(target)
                @Suppress("DEPRECATION")
                imm.showSoftInput(target, InputMethodManager.SHOW_FORCED)
            }
        }
    }
}

/** 系统语音界面，用遥控器麦克风。说完的字填回搜索框。 */
@Composable
fun rememberVoiceInput(
    onResult: (String) -> Unit,
    onUnavailable: () -> Unit,
): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val result = rememberUpdatedState(onResult)
    val unavailable = rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { activityResult ->
        if (activityResult.resultCode != android.app.Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val said = activityResult.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (said.isNotEmpty()) result.value(said)
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "说出歌名或歌星")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        val can = intent.resolveActivity(context.packageManager) != null
        if (!can) {
            unavailable.value()
        } else {
            runCatching { launcher.launch(intent) }.onFailure { unavailable.value() }
        }
    }
}

fun androidx.compose.ui.input.key.KeyEvent.isRemoteVoiceKey(): Boolean {
    val code = nativeKeyEvent.keyCode
    return code == AndroidKeyEvent.KEYCODE_VOICE_ASSIST ||
        code == AndroidKeyEvent.KEYCODE_SEARCH ||
        code == AndroidKeyEvent.KEYCODE_ASSIST
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
    password: Boolean = false,
) {
    val focus = remember { FocusRequester() }
    val showIme = rememberShowTvIme(focus)
    val view = LocalView.current
    LaunchedEffect(requestFocus) {
        if (requestFocus) showIme()
    }
    FocusSurface(onClick = showIme, modifier = modifier, corner = 12.dp) { focused ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (focused) KaraokeColors.surfaceStrong else KaraokeColors.surface,
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (value.isEmpty()) {
                Text(hint, color = KaraokeColors.textTertiary, fontSize = 17.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = KaraokeColors.textPrimary, fontSize = 18.sp),
                cursorBrush = SolidColor(KaraokeColors.textPrimary),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        val confirm = event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                            event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                            event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
                        if (!confirm || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        if (event.nativeKeyEvent.repeatCount > 0) return@onPreviewKeyEvent true
                        val imm = view.context.getSystemService(InputMethodManager::class.java)
                        val target = view.findFocus()
                        @Suppress("DEPRECATION")
                        val imeOpen = target != null && imm?.isActive(target) == true
                        // 键盘开着时把确定交给输入法，用来上屏。收起后再按确定才重新打开。
                        if (imeOpen) return@onPreviewKeyEvent false
                        showIme()
                        true
                    },
            )
        }
    }
}
