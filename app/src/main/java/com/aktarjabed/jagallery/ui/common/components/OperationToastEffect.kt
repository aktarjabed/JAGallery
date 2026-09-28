package com.aktarjabed.jagallery.ui.common.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.aktarjabed.jagallery.ui.common.OperationEvent
import kotlinx.coroutines.flow.Flow

@Composable
fun OperationToastEffect(operationEvent: Flow<OperationEvent>) {
    val context = LocalContext.current
    LaunchedEffect(operationEvent) {
        operationEvent.collect { event ->
            when (event) {
                is OperationEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is OperationEvent.Success -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
