package com.afgalindob.assistantapp.ui.dialogs.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.utils.DialogType
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnAccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer
import com.afgalindob.assistantapp.ui.theme.SurfaceVariant

@Composable
fun DeleteEntityDialog(
    title: String,
    type: DialogType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    val typeEntityStringRes: Int = when (type) {
        DialogType.TASK -> R.string.task
        DialogType.NOTE -> R.string.note
        DialogType.AUDIO -> R.string.audio
    }

    val typeEntityName = stringResource(typeEntityStringRes)
    AlertDialog(
        onDismissRequest = onDismiss,

        containerColor = SurfaceContainer,

        title = { Text(
            "${stringResource(R.string.delete)} $typeEntityName",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
            )
        },

        text = {
            Column {
                Text(
                    stringResource(R.string.delete_dialog, typeEntityName.lowercase()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = SurfaceVariant.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(
                    color = SurfaceVariant.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // BOTÓN CANCELAR
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceVariant.copy(alpha = 0.3f),
                        contentColor = OnSurfacePrimary
                    ),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                // BOTÓN ELIMINAR (Confirmar)
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentSecondary,
                        contentColor = OnAccentSecondary
                    ),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.delete),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        dismissButton = {}
    )
}