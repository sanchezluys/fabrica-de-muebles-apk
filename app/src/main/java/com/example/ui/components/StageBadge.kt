package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductionStage

@Composable
fun StageBadge(
    stage: ProductionStage,
    modifier: Modifier = Modifier,
    isDelayed: Boolean = false
) {
    val (bgColor, textColor, dotColor) = when (stage) {
        ProductionStage.RECEPCION -> Triple(Color(0xFFEDE7F6), Color(0xFF5E35B1), Color(0xFF7E57C2))
        ProductionStage.CORTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFFF9800))
        ProductionStage.ENSAMBLE -> Triple(Color(0xFFFBE9E7), Color(0xFFD84315), Color(0xFFFF5722))
        ProductionStage.ACABADO -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), Color(0xFF00897B))
        ProductionStage.CALIDAD -> Triple(Color(0xFFE8EAF6), Color(0xFF283593), Color(0xFF3F51B5))
        ProductionStage.LISTO -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFF4CAF50))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = " " + stage.shortName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun DelayAlertChip(
    exceededHours: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFEE2E2))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "⚠️ Retraso +${exceededHours}h",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB91C1C),
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun OnTimeChip(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFDCFCE7))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "✓ En tiempo",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D),
                fontSize = 11.sp
            )
        )
    }
}
