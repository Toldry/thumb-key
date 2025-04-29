package com.dessalines.thumbkey.utils

import android.util.Log
import androidx.compose.runtime.MutableState
import com.charleskorn.kaml.Yaml
import com.dessalines.thumbkey.db.AppSettings
import com.dessalines.thumbkey.utils.KeyAction.CommitText
import com.dessalines.thumbkey.utils.KeyAction.Noop
import com.dessalines.thumbkey.utils.KeyDisplay.TextDisplay
import com.dessalines.thumbkey.utils.KeyboardLayout.Companion.restoreUnmodifiedKeyboardDefinitions
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

fun applyKeyModifications(
    settings: AppSettings?,
    keyModificationsError: MutableState<String?>? = null,
) {
    try {
        applyKeyModificationsInternal(settings)
        keyModificationsError?.value = null
    } catch (e: Exception) {
        keyModificationsError?.value = e.message ?: e.stackTraceToString()
    }
}

internal var changesPreviouslyApplied = false
internal var previousKeyModificationsHash: Int? = null

@OptIn(ExperimentalSerializationApi::class)
internal fun applyKeyModificationsInternal(settings: AppSettings?) {
    if (settings == null || settings.keyModifications.hashCode() == previousKeyModificationsHash) {
        return
    }

    if (changesPreviouslyApplied) {
        restoreUnmodifiedKeyboardDefinitions()
        changesPreviouslyApplied = false
    }

    previousKeyModificationsHash = settings.keyModifications.hashCode()

    if (settings.keyModifications.isEmpty()) return

    var serializer = MapSerializer(String.serializer(), KeyboardDefinitionModesSerializable.serializer())
    val keyModifications = Yaml.default.decodeFromString(serializer, settings.keyModifications)

    val layouts = keyboardLayoutsSetFromDbIndexString(settings.keyboardLayouts).toList()
    for (layout in layouts) {
        if (keyModifications[layout.name] == null) {
            continue
        }

        val modifications = keyModifications[layout.name]!!

        applyToKeyboardC(layout.keyboardDefinition.modes.main, modifications.main)
        applyToKeyboardC(layout.keyboardDefinition.modes.shifted, modifications.shifted)
        applyToKeyboardC(layout.keyboardDefinition.modes.numeric, modifications.numeric)
        applyToKeyboardC(layout.keyboardDefinition.modes.ctrled, modifications.ctrled)
        applyToKeyboardC(layout.keyboardDefinition.modes.alted, modifications.alted)

        changesPreviouslyApplied = true
        Log.d(TAG, "key modifications applied to ${layout.name}")
    }
}

internal fun applyToKeyboardC(
    keyboardC: KeyboardC?,
    keyboardCSerializable: KeyboardCSerializable?,
) {
    if (keyboardC == null || keyboardCSerializable == null) return

    keyboardC.arr.forEachIndexed { i, row ->
        row.forEachIndexed { j, keyItemC ->
            val propertyName = "key${i}_$j"
            val property = KeyboardCSerializable::class.members.find { it.name == propertyName }
            val keyItemCSerializable = property?.call(keyboardCSerializable) as? KeyItemCSerializable

            keyItemCSerializable?.let {
                keyItemC.topLeft = applyToKeyItemC(keyItemC.topLeft, it.topLeft)
                keyItemC.top = applyToKeyItemC(keyItemC.top, it.top)
                keyItemC.topRight = applyToKeyItemC(keyItemC.topRight, it.topRight)
                keyItemC.left = applyToKeyItemC(keyItemC.left, it.left)
                keyItemC.center = applyToKeyItemC(keyItemC.center, it.center) ?: KeyC(action = Noop)
                keyItemC.right = applyToKeyItemC(keyItemC.right, it.right)
                keyItemC.bottomLeft = applyToKeyItemC(keyItemC.bottomLeft, it.bottomLeft)
                keyItemC.bottom = applyToKeyItemC(keyItemC.bottom, it.bottom)
                keyItemC.bottomRight = applyToKeyItemC(keyItemC.bottomRight, it.bottomRight)

                keyItemC.widthMultiplier = it.widthMultiplier ?: keyItemC.widthMultiplier
                keyItemC.backgroundColor = it.backgroundColor ?: keyItemC.backgroundColor
                keyItemC.swipeType = it.swipeType ?: keyItemC.swipeType
                keyItemC.slideType = it.slideType ?: keyItemC.slideType
            }
        }
    }
}

internal fun applyToKeyItemC(
    keyC: KeyC?,
    keyCSerializable: KeyCSerializable?,
): KeyC? {
    var returnValue = keyC
    if (keyCSerializable == null) {
        return returnValue
    }
    if (keyCSerializable.remove) {
        return null
    }

    if (keyC == null) {
        returnValue =
            KeyC(
                action = CommitText(keyCSerializable.text ?: ""),
                display = TextDisplay(keyCSerializable.text ?: ""),
            )
    } else if (keyCSerializable.text != null && returnValue.display is TextDisplay) {
        returnValue.display.text = keyCSerializable.text
        returnValue.action = CommitText(keyCSerializable.text)
    }
    if (keyCSerializable.size != null) {
        returnValue.size = keyCSerializable.size
    }

    returnValue.size = keyCSerializable.size ?: returnValue.size
    returnValue.color = keyCSerializable.color ?: returnValue.color

    return returnValue
}

@Serializable
data class KeyboardDefinitionModesSerializable(
    val main: KeyboardCSerializable? = null,
    val shifted: KeyboardCSerializable? = null,
    val numeric: KeyboardCSerializable? = null,
    val ctrled: KeyboardCSerializable? = null,
    val alted: KeyboardCSerializable? = null,
)

@Serializable
@Suppress("PropertyName")
data class KeyboardCSerializable(
    val key0_0: KeyItemCSerializable? = null,
    val key0_1: KeyItemCSerializable? = null,
    val key0_2: KeyItemCSerializable? = null,
    val key0_3: KeyItemCSerializable? = null,
    val key1_0: KeyItemCSerializable? = null,
    val key1_1: KeyItemCSerializable? = null,
    val key1_2: KeyItemCSerializable? = null,
    val key1_3: KeyItemCSerializable? = null,
    val key2_0: KeyItemCSerializable? = null,
    val key2_1: KeyItemCSerializable? = null,
    val key2_2: KeyItemCSerializable? = null,
    val key2_3: KeyItemCSerializable? = null,
    val key3_0: KeyItemCSerializable? = null,
    val key3_1: KeyItemCSerializable? = null,
    val key3_2: KeyItemCSerializable? = null,
    val key3_3: KeyItemCSerializable? = null,
)

@Serializable
data class KeyItemCSerializable(
    val topLeft: KeyCSerializable? = null,
    val top: KeyCSerializable? = null,
    val topRight: KeyCSerializable? = null,
    val left: KeyCSerializable? = null,
    val center: KeyCSerializable? = null,
    val right: KeyCSerializable? = null,
    val bottomLeft: KeyCSerializable? = null,
    val bottom: KeyCSerializable? = null,
    val bottomRight: KeyCSerializable? = null,

    var widthMultiplier: Int? = null,
    var backgroundColor: ColorVariant? = null,
    var swipeType: SwipeNWay? = null,
    var slideType: SlideType? = null,
)

@Serializable
data class KeyCSerializable(
    val text: String? = null,
    val displayText: String? = null,
    val size: FontSizeVariant? = null,
    val color: ColorVariant? = null,
    val remove: Boolean = false,
)
