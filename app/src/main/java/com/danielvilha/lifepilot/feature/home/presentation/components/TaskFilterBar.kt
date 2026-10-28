package com.danielvilha.lifepilot.feature.home.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.danielvilha.lifepilot.domain.model.TaskFilter

@Composable
fun TaskFilterBar(
    selectedFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = TaskFilter.entries

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        filters.forEachIndexed { index, filter ->
            SegmentedButton(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = filters.size
                ),
                label = {
                    Text(
                        text = when (filter) {
                            TaskFilter.ALL -> "All"
                            TaskFilter.PENDING -> "Pending"
                            TaskFilter.COMPLETED -> "Completed"
                        }
                    )
                }
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun TaskFilterBarPreview() {
    TaskFilterBar(
        selectedFilter = TaskFilter.PENDING,
        onFilterSelected = {}
    )
}