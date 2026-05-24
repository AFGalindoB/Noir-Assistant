package com.afgalindob.assistantapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import com.afgalindob.assistantapp.ui.theme.SurfaceVariant

@Composable
fun SectionHeader(
    section: Int
){
    Row (
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ){

        HorizontalDivider(
            color = SurfaceVariant,
            thickness = 1.dp,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )

        Text(
            text = stringResource(section),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )

        HorizontalDivider(
            color = SurfaceVariant,
            thickness = 1.dp,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
    }
}