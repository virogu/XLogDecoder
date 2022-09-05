// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.util.prefs.Preferences

private val preferences = Preferences.userRoot()
private const val KEY_LAST_WINDOWS_SIZE = "key-last-windows-size"

private val size by lazy {
//    val lastSize = preferences.get(KEY_LAST_WINDOWS_SIZE, "")
//    try {
//        if (lastSize.isNullOrEmpty()) {
//            throw IllegalArgumentException("lastSize is Null Or Empty")
//        }
//        val w = lastSize.split("*")[0].toFloat()
//        val h = lastSize.split("*")[1].toFloat()
//        DpSize(w.dp, h.dp)
//    } catch (e: Throwable) {
//        DpSize(700.dp, 620.dp)
//    }
    DpSize(700.dp, 620.dp)
}

fun main() = application {
    val icon = painterResource("icon.ico")
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        size = size,
        position = WindowPosition.Aligned(Alignment.Center),
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "XLog解密工具",
        state = state,
        undecorated = false,
        icon = icon,
    ) {
        Tray(icon = icon, menu = {
            if (state.isMinimized) {
                Item("显示主窗口", onClick = {
                    state.isMinimized = false
                })
            } else {
                Item("隐藏主窗口", onClick = {
                    state.isMinimized = true
                })
            }
            Item("退出", onClick = ::exitApplication)
        })
        App(window, this@application, state)
    }
}
