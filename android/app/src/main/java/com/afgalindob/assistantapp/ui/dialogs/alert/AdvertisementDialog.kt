package com.afgalindob.assistantapp.ui.dialogs.alert

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import com.afgalindob.assistantapp.ui.theme.ErrorColor
import com.afgalindob.assistantapp.ui.theme.OnAccentSecondary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer

@Composable
fun AdvertisementDialog(
    titleToShow: String,
    descriptionToShow: String,
    alert: Boolean = false,
    onConfirm: () -> Unit
){
    AlertDialog(
        onDismissRequest = {},
        containerColor = SurfaceContainer,

        title = {
            Text(
                text = titleToShow,
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                color = if (alert) ErrorColor else OnAccentSecondary,
                modifier = Modifier
                    .fillMaxWidth()
            )
        },
        text = {
            Text(
                text = descriptionToShow,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },

        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentSecondary,
                    contentColor = OnAccentSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.accept),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        dismissButton = {}

    )
}