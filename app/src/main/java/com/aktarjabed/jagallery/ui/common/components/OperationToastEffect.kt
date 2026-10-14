package com.aktarjabed.jagallery.ui.common.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.aktarjabed.jagallery.ui.common.OperationEvent
import kotlinx.coroutines.flow.Flow

@Composable
fun OperationToastEffect(operationEvent: Flow<OperationEvent>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(operationEvent, lifecycleOwner.lifecycle) {
        operationEvent.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED).collect { event ->
            when (event) {
                is OperationEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is OperationEvent.Success -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
