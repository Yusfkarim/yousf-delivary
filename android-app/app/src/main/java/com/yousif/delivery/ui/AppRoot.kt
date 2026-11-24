package com.yousif.delivery.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yousif.delivery.viewmodel.MainViewModel

@Composable
fun AppRoot(viewModel: MainViewModel = viewModel()) {
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (loggedIn) {
        HomeScreen(viewModel)
    } else {
        LockScreen(viewModel)
    }
}
