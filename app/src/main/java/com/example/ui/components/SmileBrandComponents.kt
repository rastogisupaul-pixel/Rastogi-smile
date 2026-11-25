package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SmileGreen
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.utils.MediaUtils
import kotlin.math.abs

@Composable
fun SmileLogoMark(
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadiusFraction: Float = 0.26f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val corner = w * cornerRadiusFraction

        // Yellow rounded square background (#FFD600)
        drawRoundRect(
            color = SmileYellow,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Crisp white speech bubble outline with bottom-left tail
        val cx = w * 0.50f
        val cy = h * 0.48f
        val r = w * 0.30f
        val strokeW = (w * 0.068f).coerceAtLeast(2.2f)

        val bubblePath = Path().apply {
            moveTo(cx - r * 0.85f, cy + r * 0.50f)
            cubicTo(
                cx - r * 1.08f, cy + r * 0.12f,
                cx - r * 1.02f, cy - r * 0.52f,
                cx - r * 0.58f, cy - r * 0.82f
            )
            cubicTo(
                cx - r * 0.15f, cy - r * 1.08f,
                cx + r * 0.45f, cy - r * 1.02f,
                cx + r * 0.78f, cy - r * 0.65f
            )
            cubicTo(
                cx + r * 1.08f, cy - r * 0.25f,
                cx + r * 1.05f, cy + r * 0.38f,
                cx + r * 0.65f, cy + r * 0.75f
            )
            cubicTo(
                cx + r * 0.28f, cy + r * 1.05f,
                cx - r * 0.22f, cy + r * 1.03f,
                cx - r * 0.55f, cy + r * 0.85f
            )
            lineTo(cx - r * 0.98f, cy + r * 1.05f)
            close()
        }

        drawPath(
            path = bubblePath,
            color = SmileWhite,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bold black smiling face (#111111)
        val eyeRadius = w * 0.048f
        val eyeY = cy - r * 0.14f
        drawCircle(
            color = SmileInk,
            radius = eyeRadius,
            center = Offset(cx - r * 0.30f, eyeY)
        )
        drawCircle(
            color = SmileInk,
            radius = eyeRadius,
            center = Offset(cx + r * 0.30f, eyeY)
        )

        val smileRectTopLeft = Offset(cx - r * 0.42f, cy - r * 0.15f)
        val smileRectSize = Size(r * 0.84f, r * 0.66f)
        drawArc(
            color = SmileInk,
            startAngle = 18f,
            sweepAngle = 144f,
            useCenter = false,
            topLeft = smileRectTopLeft,
            size = smileRectSize,
            style = Stroke(width = (w * 0.062f).coerceAtLeast(2f), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun UserAvatar(
    avatarData: String,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    isGroup: Boolean = false,
    showOnlineDot: Boolean = false,
    online: Boolean = false
) {
    val bitmap = remember(avatarData) { MediaUtils.decodeBase64Bitmap(avatarData) }
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(SmileYellowSoft)
                .border(1.5.dp, SmileYellow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "$name avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isGroup) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "$name group",
                    tint = SmileInk,
                    modifier = Modifier.size(size * 0.52f)
                )
            } else {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.40f).sp
                    ),
                    color = SmileInk
                )
            }
        }
        if (showOnlineDot && online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .size((size * 0.26f).coerceAtLeast(10.dp))
                    .clip(CircleShape)
                    .background(SmileGreen)
                    .border(2.dp, SmileWhite, CircleShape)
            )
        }
    }
}

@Composable
fun DeliveryTicks(
    isRead: Boolean,
    isDelivered: Boolean,
    tintDefault: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(4.dp))
        if (isRead || isDelivered) {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = if (isRead) "Read" else "Delivered",
                tint = if (isRead) SmileGreen else tintDefault,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = tintDefault,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
fun QrCodeMatrixCanvas(
    data: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val grid = remember(data) {
        val n = 21
        Array(n) { r ->
            BooleanArray(n) { c ->
                val inTopLeft = r < 7 && c < 7
                val inTopRight = r < 7 && c >= n - 7
                val inBottomLeft = r >= n - 7 && c < 7
                when {
                    inTopLeft -> isFinderCell(r, c)
                    inTopRight -> isFinderCell(r, c - (n - 7))
                    inBottomLeft -> isFinderCell(r - (n - 7), c)
                    else -> {
                        val hash = abs((data.hashCode() * 31 + r * 17 + c * 53))
                        hash % 2 == 0
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(SmileWhite)
            .border(3.dp, SmileYellow, RoundedCornerShape(20.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = this.size.width * 0.1f
            val cellSize = (this.size.width - pad * 2) / 21f
            for (r in 0 until 21) {
                for (c in 0 until 21) {
                    if (grid[r][c]) {
                        drawRect(
                            color = SmileInk,
                            topLeft = Offset(pad + c * cellSize, pad + r * cellSize),
                            size = Size(cellSize * 0.94f, cellSize * 0.94f)
                        )
                    }
                }
            }
        }
    }
}

private fun isFinderCell(r: Int, c: Int): Boolean {
    if (r == 0 || r == 6 || c == 0 || c == 6) return true
    if (r in 2..4 && c in 2..4) return true
    return false
}
