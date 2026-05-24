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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer
import com.afgalindob.assistantapp.ui.theme.SurfaceVariant

@Composable
fun GetServerUsernameDialog(
    initialValue: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
){
    var username by remember { mutableStateOf(initialValue ?: "") }

    val isUsernameValid = username.trim().isNotEmpty()
    AlertDialog(
        onDismissRequest = {},
        containerColor = SurfaceContainer,

        title = {
            Text(
                text = "Ingresa el nombre asignado en el servidor",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de usuario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Debe ser exacto al del servidor (distingue mayúsculas y minúsculas).",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfacePrimary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
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

                // BOTÓN ACEPTAR
                Button(
                    onClick = { onConfirm(username.trim()) },
                    enabled = isUsernameValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUsernameValid) AccentSecondary else SurfaceVariant.copy(alpha = 0.3f),
                        contentColor = OnSurfacePrimary
                    ),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.accept),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        dismissButton = {}

    )
}