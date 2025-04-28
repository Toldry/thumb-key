package com.dessalines.thumbkey.utils

import androidx.compose.runtime.MutableState
import com.dessalines.thumbkey.db.AppSettings
import com.dessalines.thumbkey.utils.KeyAction.CommitText
import com.dessalines.thumbkey.utils.KeyDisplay.TextDisplay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun applyKeyModifications(
    settings: AppSettings?,
    keyModificationsError: MutableState<String>,
) {
    try {
        applyKeyModificationsInternal(settings)
        keyModificationsError.value = ""
    } catch (e: Exception) {
        keyModificationsError.value = simplifyErrorMessage(e)
    }
}

fun simplifyErrorMessage(e: Exception): String {
    val matchResult =
        Regex(
            """^(Encountered an unknown key .*)\n(Use 'ignoreUnknownKeys.*)\n(.*)$""",
            RegexOption.MULTILINE,
        ).find(e.message.toString())
    return if (matchResult == null) {
        e.message.toString()
    } else {
        "${matchResult.groups[1]!!.value}\n${matchResult.groups[3]!!.value}"
    }
}


@OptIn(ExperimentalSerializationApi::class)
internal fun applyKeyModificationsInternal(settings: AppSettings?) {
    if (settings == null || settings.keyModifications.isEmpty()) {
        return
    }

    val json =
        Json {
            ignoreUnknownKeys = false
            allowTrailingComma = true
            allowComments = true
        }
    var keyModifications: Map<String, KeyboardDefinitionModesSerializable> =
        json.decodeFromString(settings.keyModifications)

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
                keyItemC.swipeType = it.swipeType
                keyItemC.topLeft = applyToKeyItemC(keyItemC.topLeft, it.topLeft)
                keyItemC.top = applyToKeyItemC(keyItemC.top, it.top)
                keyItemC.topRight = applyToKeyItemC(keyItemC.topRight, it.topRight)
                keyItemC.left = applyToKeyItemC(keyItemC.left, it.left)
                keyItemC.center = applyToKeyItemC(keyItemC.center, it.center)!!
                keyItemC.right = applyToKeyItemC(keyItemC.right, it.right)
                keyItemC.bottomLeft = applyToKeyItemC(keyItemC.bottomLeft, it.bottomLeft)
                keyItemC.bottom = applyToKeyItemC(keyItemC.bottom, it.bottom)
                keyItemC.bottomRight = applyToKeyItemC(keyItemC.bottomRight, it.bottomRight)
            }
        }
    }
}

internal fun applyToKeyItemC(
    keyC: KeyC?,
    keyCSerializable: KeyCSerializable?,
): KeyC? {
    var returnValue = keyC
    if (keyCSerializable == null ||
        keyCSerializable.remove
    ) {
        return null
    }

    if (keyC == null) {
        returnValue =
            KeyC(
                display = TextDisplay(keyCSerializable.text ?: ""),
                action = CommitText(keyCSerializable.text ?: ""),
            )
    } else if (keyCSerializable.text != null && keyC.display is TextDisplay) {
        returnValue.display.text = keyCSerializable.text
        returnValue.action = CommitText(keyCSerializable.text)
    }
    if (keyCSerializable.size != null) {
        returnValue.size = keyCSerializable.size
    }
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
    val swipeType: SwipeNWay = SwipeNWay.EIGHT_WAY,
)

@Serializable
data class KeyCSerializable(
    val text: String? = null,
    val size: FontSizeVariant? = null,
    val remove: Boolean = false,
)
