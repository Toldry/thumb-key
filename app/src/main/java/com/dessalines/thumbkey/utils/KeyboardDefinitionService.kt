package com.dessalines.thumbkey.utils

import android.util.Log
import com.dessalines.thumbkey.db.AppSettings
import com.dessalines.thumbkey.utils.KeyAction.CommitText
import com.dessalines.thumbkey.utils.KeyDisplay.TextDisplay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


fun applyModificationsToKeyboardLayouts(settings: AppSettings?) {
    if (settings == null || settings.keyModifications.isEmpty()) {
        return
    }
    var keyModifications: Map<String, KeyboardDefinitionModesSerializable>
    try {
        keyModifications = Json.decodeFromString<Map<String, KeyboardDefinitionModesSerializable>>(settings.keyModifications)
    } catch (e: Exception) {
        Log.d(TAG, "Error serializing key modifications: ${e.message}")
        return
    }

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

fun applyToKeyboardC(kbc: KeyboardC? ,kbcs: KeyboardCSerializable?) {
    if(kbc == null || kbcs == null) return
    kbc.arr.forEachIndexed { i, row ->
        row.forEachIndexed { j, keyItemC ->
            kbcs.arr.getOrNull(i)?.getOrNull(j)?.let {
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

fun applyToKeyItemC(kc: KeyC?, kcs: KeyCSerializable?): KeyC? {
    var retVal = kc
    if (kcs != null) {
        if (kc == null) {
            retVal = KeyC(
                display = TextDisplay(kcs.text ?: ""),
                action = CommitText(kcs.text ?: ""),
            )
        }
        else if (kcs.text != null && kc.display is TextDisplay) {
            kc.display.text = kcs.text
            kc.action = CommitText(kcs.text)
        }
        if (kcs.size != null) {
            kc!!.size = kcs.size
        }
    }
    return retVal
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
data class KeyboardCSerializable(
    val arr: List<List<KeyItemCSerializable?>>

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
)

@Serializable
data class KeyCSerializable(
    val text: String? = null,
    val size: FontSizeVariant? = null,
)



var a = """
{
  "HEMessagEase": {
    "main": {
      "arr": [
        [],
        [
          {},
          {
            "center": {
              "text": "𒍅",
              "size": "SMALL"
            },
            "topRight": {
              "text": "𒁂"
            }
          }
        ],
        [
          {},
          {},
          {
            "bottomRight": {
              "text": "𒇫"
            }
          }
        ]
      ]
    }
  }
}
"""