package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.v2ray.ang.R

@Composable
internal fun MainImportDialog(
    isSubscription: Boolean,
    onDismiss: () -> Unit,
    onAction: (MainAction) -> Unit,
) {
    // Config links may contain credentials. Keep the draft in this visible dialog only.
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (isSubscription) R.string.acc_add_subscription else R.string.home_import_text))
        },
        text = {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(if (isSubscription) R.string.sub_setting_url else R.string.title_server)) },
                    singleLine = isSubscription,
                    minLines = if (isSubscription) 1 else 3,
                    keyboardOptions = KeyboardOptions(keyboardType = if (isSubscription) KeyboardType.Uri else KeyboardType.Text),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank(),
                onClick = {
                    val config = input.trim()
                    onDismiss()
                    onAction(
                        if (isSubscription) MainAction.ImportSubscriptionUrl(config)
                        else MainAction.ImportBatchConfig(config)
                    )
                },
            ) {
                Text(stringResource(if (isSubscription) R.string.home_add_and_update else R.string.home_import_text))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
