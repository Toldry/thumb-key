package com.dessalines.thumbkey.ui.components.settings.modifykeys

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.dessalines.thumbkey.IMEService
import com.dessalines.thumbkey.R
import com.dessalines.thumbkey.db.AppSettingsViewModel
import com.dessalines.thumbkey.db.BehaviorUpdate
import com.dessalines.thumbkey.db.DEFAULT_AUTO_CAPITALIZE
import com.dessalines.thumbkey.db.DEFAULT_CIRCULAR_DRAG_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_CLOCKWISE_DRAG_ACTION
import com.dessalines.thumbkey.db.DEFAULT_COUNTERCLOCKWISE_DRAG_ACTION
import com.dessalines.thumbkey.db.DEFAULT_DRAG_RETURN_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_GHOST_KEYS_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_KEY_MODIFICATIONS
import com.dessalines.thumbkey.db.DEFAULT_MIN_SWIPE_LENGTH
import com.dessalines.thumbkey.db.DEFAULT_SLIDE_BACKSPACE_DEADZONE_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_SLIDE_CURSOR_MOVEMENT_MODE
import com.dessalines.thumbkey.db.DEFAULT_SLIDE_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_SLIDE_SENSITIVITY
import com.dessalines.thumbkey.db.DEFAULT_SLIDE_SPACEBAR_DEADZONE_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_SPACEBAR_MULTITAPS
import com.dessalines.thumbkey.db.ModifyKeysUpdate
import com.dessalines.thumbkey.ui.components.common.TestOutTextField
import com.dessalines.thumbkey.ui.components.settings.about.DONATE_URL
import com.dessalines.thumbkey.ui.components.settings.about.SettingsDivider
import com.dessalines.thumbkey.utils.CircularDragAction
import com.dessalines.thumbkey.utils.CursorAccelerationMode
import com.dessalines.thumbkey.utils.SimpleTopAppBar
import com.dessalines.thumbkey.utils.TAG
import com.dessalines.thumbkey.utils.applyModificationsToKeyboardLayouts
import com.dessalines.thumbkey.utils.openLink
import com.dessalines.thumbkey.utils.toBool
import com.dessalines.thumbkey.utils.toInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme

const val MODIFY_KEYS_URL = "https://github.com/dessalines/thumb-key#modify-keys"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyKeysScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to modify keys activity")

    val settings by appSettingsViewModel.appSettings.observeAsState()
    var keyModifications = (settings?.keyModifications) ?: DEFAULT_KEY_MODIFICATIONS
    var textState by remember { mutableStateOf(TextFieldValue(keyModifications)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val ctx = LocalContext.current

    fun updateModifyKeys() {
        appSettingsViewModel.updateModifyKeys(
            ModifyKeysUpdate(
                id = 1,
                keyModifications = keyModifications,
            ),
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.modify_keys),
                navController = navController,
            )
        },
        content = { padding ->
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .background(color = MaterialTheme.colorScheme.surface)
                        .imePadding(),
            ) {
                ProvidePreferenceTheme {
                    Preference(
                        title = { Text(stringResource(R.string.how_to_modify_keys)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.QuestionMark,
                                contentDescription = stringResource(R.string.how_to_modify_keys),
                            )
                        },
                        onClick = {
                            openLink(MODIFY_KEYS_URL, ctx)
                        },
                    )
                    TextField(
                        modifier =
                            Modifier.fillMaxWidth(),
                        value = textState,
                        onValueChange = { textState = it },
                        placeholder = {
                            Text(
                                stringResource(
                                    R.string.enter_key_modifications
                                )
                            )
                        },
                        colors =
                            TextFieldDefaults.colors(
//                                focusedContainerColor = Color.Transparent,
//                                unfocusedContainerColor = Color.Transparent,
//                                disabledContainerColor = Color.Transparent,
//                                focusedIndicatorColor = Color.Transparent,
//                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                    )
                    LaunchedEffect(key1 = textState) {
                        // Use LaunchedEffect to debounce the text input
                        delay(300)
                        keyModifications = textState.text
                        updateModifyKeys()
                        applyModificationsToKeyboardLayouts(settings)
                    }
                    SettingsDivider()
                    TestOutTextField()
                }
            }
        },
    )
}