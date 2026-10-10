package com.danielvilha.lifepilot.feature.plan.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.model.TaskSuggestion
import java.time.LocalDate

@Composable
fun PlanSuggestionCard(
    task: Task,
    suggestion: TaskSuggestion,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${suggestion.suggestedOrder + 1}.",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "${task.priority} • ${task.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            task.dueDate?.let { dueDate ->
                Text(
                    text = "Due: $dueDate",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            HorizontalDivider()

            Text(
                text = "AI suggestion",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = suggestion.reason,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlanSuggestionCardPreview() {
    MaterialTheme {
        PlanSuggestionCard(
            task = Task(
                id = "task-1",
                title = "Finish project documentation",
                description = "Complete the LifePilot README",
                dueDate = LocalDate.now().plusDays(1),
                priority = Priority.HIGH,
                category = TaskCategory.WORK,
                completed = false,
                position = 0
            ),
            suggestion = TaskSuggestion(
                taskId = "task-1",
                reason = "This task has high priority and is due tomorrow.",
                suggestedOrder = 0
            )
        )
    }
}