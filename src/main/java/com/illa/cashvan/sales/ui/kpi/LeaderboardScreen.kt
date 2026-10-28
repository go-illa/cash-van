package com.illa.cashvan.sales.ui.kpi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.domain.kpi.LeaderboardTab
import com.illa.cashvan.sales.domain.kpi.leaderboardFooter
import com.illa.cashvan.sales.domain.kpi.leaderboardListValue
import com.illa.cashvan.sales.domain.kpi.leaderboardPodium
import com.illa.cashvan.sales.domain.kpi.leaderboardPodiumValue
import com.illa.cashvan.sales.feature.kpi.presentation.viewmodel.LeaderboardViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import org.koin.androidx.compose.koinViewModel

private val MEDALS = listOf(Color(0xFFF59E0B), Color(0xFF94A3B8), Color(0xFFB45309))

/** "المتصدرون": this Cairo month's ranking of active sales agents. */
@Composable
fun LeaderboardScreen(onBack: () -> Unit, viewModel: LeaderboardViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier
        .fillMaxSize()
        .background(SalesColors.Background)) {
        SalesTopBar(title = "المتصدرون", onBack = onBack, actions = {
            Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = SalesColors.Progress, modifier = Modifier.padding(end = 12.dp))
        })
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LeaderboardTab.entries.forEach { tab ->
                val selected = state.tab == tab
                Text(
                    tab.label,
                    textAlign = TextAlign.Center,
                    fontFamily = ZainBold,
                    color = if (selected) Color.White else SalesColors.Primary,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) SalesColors.Primary else SalesColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .clickable { viewModel.selectTab(tab) }
                        .padding(vertical = 10.dp)
                )
            }
        }
        val sorted = state.sorted
        when {
            state.isLoading -> LoadingBox()
            sorted.isEmpty() -> EmptyState(icon = Icons.Outlined.EmojiEvents, title = "لا توجد بيانات لهذا الشهر")
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    // Podium in display order 2nd, 1st (raised), 3rd.
                    val podium = leaderboardPodium(sorted)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SalesColors.Primary, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        listOfNotNull(podium.firstOrNull { it.rank == 2 }, podium.firstOrNull { it.rank == 1 }, podium.firstOrNull { it.rank == 3 }).forEach { entry ->
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(if (entry.rank == 1) 56.dp else 44.dp)
                                        .background(MEDALS[entry.rank - 1], CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Color.White)
                                }
                                val me = entry.row.agentId == state.myAgentId
                                Text(entry.row.agentName + if (me) " (أنت)" else "", color = Color.White, fontFamily = ZainBold, fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center)
                                Text(leaderboardPodiumValue(entry.row, state.tab), color = Color(0xFFFDE68A), fontFamily = ZainBold)
                                Text("المركز ${entry.rank}", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                Spacer(Modifier.height(if (entry.rank == 1) 16.dp else 0.dp))
                            }
                        }
                    }
                }
                itemsIndexed(sorted, key = { _, row -> row.agentId }) { index, row ->
                    val me = row.agentId == state.myAgentId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (me) SalesColors.Primary.copy(alpha = 0.08f) else Color.White, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            fontFamily = ZainBold,
                            color = MEDALS.getOrNull(index) ?: SalesColors.Muted,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(row.agentName + if (me) " (أنت)" else "", fontFamily = ZainBold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(leaderboardListValue(row, state.tab), fontFamily = ZainRegular, fontSize = 13.sp)
                    }
                }
                item {
                    Text(leaderboardFooter(sorted.size, state.tab), color = SalesColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
