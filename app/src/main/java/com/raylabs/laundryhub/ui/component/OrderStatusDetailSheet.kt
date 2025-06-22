package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.home.state.LaundryStepUiModel
import com.raylabs.laundryhub.ui.home.state.OrderStatusDetailUiModel
import com.raylabs.laundryhub.ui.theme.Purple800

@Composable
fun OrderStatusDetailSheet(
    uiModel: OrderStatusDetailUiModel,
    onStepAction: (LaundryStepUiModel) -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Spacer(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(Color.LightGray, shape = CircleShape)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(8.dp))

        Text(
            "Order Status",
            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
            color = Color.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        Card(
            backgroundColor = Purple800,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Order #${uiModel.orderId}",
                        style = MaterialTheme.typography.body2.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        uiModel.customerName,
                        style = MaterialTheme.typography.h6.copy(color = Color.White)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        uiModel.packageType,
                        style = MaterialTheme.typography.body2.copy(color = Color.White)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Due Date",
                        style = MaterialTheme.typography.caption.copy(color = Color.White)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        uiModel.dueDate,
                        style = MaterialTheme.typography.body2.copy(color = Color.White)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        uiModel.groupStatus,
                        style = MaterialTheme.typography.body2.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            backgroundColor = Purple800,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                uiModel.steps.forEachIndexed { index, step ->
                    StepItem(step, onStepAction, isLoading)
                    if (index != uiModel.steps.lastIndex) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            val lineLength = size.width
                            val dotSize = 4.dp.toPx()
                            val gap = 6.dp.toPx()
                            var x = 0f
                            while (x < lineLength) {
                                drawCircle(
                                    color = Color(0xFFCCCCCC),
                                    radius = dotSize / 5,
                                    center = Offset(
                                        x + dotSize / 2,
                                        size.height / 2
                                    )
                                )
                                x += dotSize + gap
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            backgroundColor = Purple800,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paid by ${uiModel.paymentMethod}",
                    style = MaterialTheme.typography.body1.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Rp ${uiModel.totalPrice},-",
                    style = MaterialTheme.typography.body1.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun StepItem(
    step: LaundryStepUiModel,
    onStepAction: (LaundryStepUiModel) -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (step.isDone) Color(0xFF00C853) else Color(0xFFF5F1F9),
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .width(24.dp)
                .height(1.dp)
                .align(Alignment.CenterVertically)
                .background(Color(0xFFCCCCCC))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.label,
                style = MaterialTheme.typography.body1.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (step.isDone) Color(0xFFF5F1F9) else Color(0xFF2C2C54)
                )
            )

            if (step.isDone) {
                Text(
                    text = "On ${step.selectedMachine}, ${step.date}",
                    style = MaterialTheme.typography.body2.copy(color = Color(0xFFBDBDBD))
                )
            } else if (step.isCurrent) {
                if (step.selectedMachine.isBlank() && step.availableMachines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        step.availableMachines.forEach { machineName ->
                            OutlinedButton(
                                onClick = { onStepAction(step.copy(selectedMachine = machineName)) },
                                enabled = !isLoading,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFFF5F1F9)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF5F1F9)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = machineName, color = Color(0xFF2C2C54))
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { onStepAction(step) },
                        enabled = !isLoading,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0xFFF5F1F9)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF5F1F9)),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF2C2C54),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Start on ${step.selectedMachine}",
                                color = Color(0xFF2C2C54)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Waiting on ${step.selectedMachine}",
                    style = MaterialTheme.typography.body2.copy(color = Color(0xFF757575))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewOrderStatusDetailSheet() {
    val dummySteps = listOf(
        LaundryStepUiModel("Washing", "22-Jun 09:00", "Washer #1", true, true),
        LaundryStepUiModel("Drying", "22-Jun 09:00", "Dryer #2", true, true),
        LaundryStepUiModel("Ironing", "", "Iron Station 1", false, true),
        LaundryStepUiModel("Folding", "", "Folding Station", false, false),
        LaundryStepUiModel("Packing", "", "Packing Station", false, false),
        LaundryStepUiModel("Ready", "", "", false, false)
    )

    val dummyUiModel = OrderStatusDetailUiModel(
        orderId = "001",
        customerName = "Ny. Emy",
        packageType = "Express 6H",
        dueDate = "22-Jun-2025 17:00",
        totalPrice = "53.000",
        groupStatus = "Completed",
        paymentMethod = "Cash",
        steps = dummySteps
    )

    OrderStatusDetailSheet(
        uiModel = dummyUiModel,
        onStepAction = {},
        isLoading = false
    )
}