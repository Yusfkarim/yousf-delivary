package com.yousif.delivery.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yousif.delivery.ui.theme.YousifTheme
import com.yousif.delivery.viewmodel.MainViewModel

@Composable
fun AppRoot(viewModel: MainViewModel = viewModel()) {
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }

    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    YousifTheme(darkTheme = darkTheme) {
        val activity = context as? ComponentActivity
        SideEffect {
            if (activity != null) {
                val scrim = Color.Transparent.toArgb()
                val style = if (darkTheme) {
                    SystemBarStyle.dark(scrim)
                } else {
                    SystemBarStyle.light(scrim, scrim)
                }
                activity.enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style,
                )
            }
        }

        if (loggedIn) {
            HomeScreen(viewModel)
        } else {
            LockScreen(viewModel)
        }
    }
}
