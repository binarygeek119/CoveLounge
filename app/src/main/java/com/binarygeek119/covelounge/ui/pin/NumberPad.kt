package com.binarygeek119.covelounge.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NumberPad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Rows 1-3, 4-6, 7-9
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (col in 0..2) {
                    val digit = row * 3 + col + 1
                    NumberButton(text = digit.toString()) { onDigit(digit) }
                }
            }
        }
        // Bottom row: backspace, 0
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberButton(text = "⌫") { onBackspace() }
            NumberButton(text = "0") { onDigit(0) }
            // Spacer for alignment
            Box(modifier = Modifier.size(64.dp))
        }
    }
}

@Composable
private fun NumberButton(text: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(64.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .background(if (focused) Color(0xFF2196F3) else Color(0x33FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal
        )
    }
}
