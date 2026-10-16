package com.smartjirani.feature.civic.official

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartjirani.feature.civic.R

fun filterLabelRes(filter: CivicOfficialFilter): Int =
    when (filter) {
        CivicOfficialFilter.ALL -> R.string.civic_official_filter_all
        CivicOfficialFilter.AWAITING_VERIFICATION -> R.string.civic_official_filter_awaiting_verification
        CivicOfficialFilter.OVERDUE -> R.string.civic_official_filter_overdue
        CivicOfficialFilter.MINE -> R.string.civic_official_filter_mine
        CivicOfficialFilter.NEW_TODAY -> R.string.civic_official_filter_new_today
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CivicOfficialFilterChips(
    current: CivicOfficialFilter,
    onSelect: (CivicOfficialFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        CivicOfficialFilter.values().forEach { filter ->
            FilterChip(
                selected = filter == current,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filterLabelRes(filter))) },
            )
        }
    }
}
