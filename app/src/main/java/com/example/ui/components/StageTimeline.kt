package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductionStage

@Composable
fun StageTimeline(
    currentStage: ProductionStage,
    isCompleted: Boolean,
    isDelayed: Boolean,
    modifier: Modifier = Modifier
) {
    val stages = ProductionStage.entries
    val currentIdx = if (isCompleted) stages.size else currentStage.stepIndex

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Línea de Tiempo de Fabricación",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step timeline nodes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                stages.forEachIndexed { index, stage ->
                    val isPast = index < currentIdx
                    val isCurrent = index == currentIdx && !isCompleted
                    val isPending = index > currentIdx

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Left connector line
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .background(
                                            if (isPast || isCurrent) Color(0xFF16A34A)
                                            else Color(0xFFD1D5DB)
                                        )
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            // Circle icon
                            val circleBg = when {
                                isPast -> Color(0xFF16A34A)
                                isCurrent && isDelayed -> Color(0xFFDC2626)
                                isCurrent -> Color(0xFFD97706)
                                else -> Color(0xFFE5E7EB)
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(circleBg)
                                    .then(
                                        if (isCurrent) Modifier.border(2.dp, Color.White, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isPast -> Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completado",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    isCurrent && isDelayed -> Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Con retraso",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    isCurrent -> Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = "En proceso",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    else -> Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF6B7280),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            // Right connector line
                            if (index < stages.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .background(
                                            if (isPast) Color(0xFF16A34A)
                                            else Color(0xFFD1D5DB)
                                        )
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stage.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isCurrent && isDelayed -> Color(0xFFDC2626)
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isPast -> Color(0xFF16A34A)
                                    else -> Color(0xFF6B7280)
                                },
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
