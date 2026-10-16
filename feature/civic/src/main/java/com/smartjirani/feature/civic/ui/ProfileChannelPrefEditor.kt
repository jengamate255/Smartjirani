package com.smartjirani.feature.civic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartjirani.core.designsystem.component.SmartButton
import com.smartjirani.feature.civic.R

data class ChannelOption(
    val value: String,
    val labelRes: Int,
) {
    companion object {
        val ALL =
            listOf(
                ChannelOption("in_app", R.string.civic_channel_pref_in_app),
                ChannelOption("sms", R.string.civic_channel_pref_sms),
                ChannelOption("whatsapp", R.string.civic_channel_pref_whatsapp),
                ChannelOption("sms,whatsapp", R.string.civic_channel_pref_sms),
            )

        fun resolve(current: String): ChannelOption = ALL.firstOrNull { it.value == current } ?: ALL[0]
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileChannelPrefEditor(
    current: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember(current) { mutableStateOf(ChannelOption.resolve(current)) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.civic_channel_pref_title),
            style = MaterialTheme.typography.titleMedium,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ChannelOption.ALL.forEach { option ->
                FilterChip(
                    selected = selected.value == option.value,
                    onClick = { selected = option },
                    label = { Text(stringResource(option.labelRes)) },
                )
            }
        }

        SmartButton(
            text = stringResource(R.string.civic_channel_pref_save),
            onClick = { onSave(selected.value) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
