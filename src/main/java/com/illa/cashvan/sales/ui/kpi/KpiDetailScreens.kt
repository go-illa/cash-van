package com.illa.cashvan.sales.ui.kpi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.domain.format.formatEGP
import com.illa.cashvan.sales.domain.kpi.CAIRO_ZONE
import com.illa.cashvan.sales.domain.kpi.KpiComparisonKind
import com.illa.cashvan.sales.domain.kpi.KpiPeriod
import com.illa.cashvan.sales.domain.kpi.currentCairoPeriod
import com.illa.cashvan.sales.feature.kpi.presentation.viewmodel.DEFAULT_DAILY_TARGET
import com.illa.cashvan.sales.feature.kpi.presentation.viewmodel.SalesKpiViewModel
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesDropdownField
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToInt

private val WORK_DAY_NAMES = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")

@Composable
private fun KpiPage(
    title: String,
    onBack: () -> Unit,
    loading: Boolean,
    period: KpiPeriod?,
    onPeriod: ((KpiPeriod) -> Unit)?,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(SalesColors.Background)) {
        SalesTopBar(title = title, onBack = onBack, actions = {
            if (period != null && onPeriod != null) MonthPicker(period, onPeriod)
        })
        if (loading) {
            LoadingBox()
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { content() }
        }
    }
}

private fun pct(part: Int, total: Int): Double = if (total > 0) part * 100.0 / total else 0.0
private fun oneDecimal(value: Double) = String.format(Locale.US, "%.1f", value)

/** "زيارات الأسبوع": Saturday to Thursday visits against the daily target × 6. */
@Composable
fun KpiVisitsScreen(viewModel: SalesKpiViewModel, onBack: () -> Unit) {
    val state by viewModel.visits.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadVisits() }
    val week = state.week
    KpiPage("زيارات الأسبوع", onBack, state.isLoading, null, null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.shiftWeek(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "الأسبوع السابق") }
            Text(
                if (week?.isCurrent == true) "الأسبوع الحالي" else week?.label.orEmpty(),
                fontFamily = ZainBold,
                color = if (week?.isCurrent == true) SalesColors.Primary else SalesColors.Progress,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { viewModel.shiftWeek(1) }, enabled = week?.isCurrent != true) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "الأسبوع التالي")
            }
        }
        KpiCard {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                KpiRing((state.total.toFloat() / state.weeklyTarget).coerceAtMost(1f), SalesColors.Progress, 160.dp, 12.dp) {
                    Text("${state.total}", fontSize = 30.sp, fontFamily = ZainBold)
                    Text("من ${state.weeklyTarget}", fontSize = 12.sp, color = SalesColors.Muted)
                }
                Text("عملت ${state.total} زيارة من ${state.weeklyTarget}", fontFamily = ZainBold, modifier = Modifier.padding(top = 8.dp))
                val remaining = state.weeklyTarget - state.total
                Text(
                    when {
                        remaining <= 0 -> "أتممت هدف الأسبوع 🎉"
                        week?.isCurrent == true -> "فاضل $remaining زيارة على هدف الأسبوع"
                        else -> "حققت ${pct(state.total, state.weeklyTarget).roundToInt()}٪ من هدف الأسبوع"
                    },
                    color = SalesColors.Muted,
                    fontFamily = ZainRegular
                )
            }
            val todayIndex = (java.time.ZonedDateTime.now(CAIRO_ZONE).dayOfWeek.value % 7 + 1) % 7
            val elapsed = if (week?.isCurrent == true) minOf(todayIndex + 1, WORK_DAY_NAMES.size) else WORK_DAY_NAMES.size
            val average = state.total.toDouble() / elapsed
            LabeledBar("متوسط الزيارات اليومي", "${oneDecimal(average)} زيارة / يوم", (average / state.dailyTarget).toFloat(), SalesColors.Progress)
            Text("الهدف اليومي ${state.dailyTarget} زيارة", fontSize = 12.sp, color = SalesColors.Muted)
        }
        KpiCard {
            Text("تفاصيل الأسبوع", fontFamily = ZainBold, fontSize = 16.sp)
            val todayIndex = (java.time.ZonedDateTime.now(CAIRO_ZONE).dayOfWeek.value % 7 + 1) % 7
            WORK_DAY_NAMES.forEachIndexed { index, day ->
                val isToday = week?.isCurrent == true && index == todayIndex
                val future = week?.isCurrent == true && index > todayIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isToday) SalesColors.Primary.copy(alpha = 0.06f) else Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(day + if (isToday) "  • النهاردة" else "", fontFamily = ZainRegular, modifier = Modifier.weight(1f))
                    Text(if (future) "—" else "${state.dailyCounts.getOrElse(index) { 0 }}", fontFamily = ZainBold)
                }
            }
        }
    }
}

/** "التغطية": unique merchants visited out of those assigned, per month and route. */
@Composable
fun KpiCoverageScreen(viewModel: SalesKpiViewModel, onBack: () -> Unit, onShowUnvisited: () -> Unit) {
    val state by viewModel.coverage.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    LaunchedEffect(period) { viewModel.loadCoverage() }
    KpiPage("التغطية", onBack, state.isLoading, period, viewModel::selectPeriod) {
        val routes = listOf<Pair<String?, String>>(null to "كل المسارات") + state.routes.map { it.id to (it.name ?: it.id) }
        SalesDropdownField(
            label = "نطاق التغطية",
            options = routes,
            selected = routes.firstOrNull { it.first == state.routeId },
            optionLabel = { it.second },
            onSelected = { viewModel.loadCoverage(it.first) }
        )
        val visited = state.month?.unique_merchants_visited ?: 0
        val total = state.month?.total_merchants ?: 0
        val scopeName = routes.firstOrNull { it.first == state.routeId }?.takeIf { it.first != null }?.second
        KpiCard {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                KpiRing((pct(visited, total) / 100).toFloat(), SalesColors.Success, 160.dp, 12.dp) {
                    Text("${pct(visited, total).roundToInt()}%", fontSize = 28.sp, fontFamily = ZainBold)
                    Text("$visited/$total", fontSize = 12.sp, color = SalesColors.Muted)
                }
                Text("زرت $visited تاجر من $total", fontFamily = ZainBold, modifier = Modifier.padding(top = 8.dp))
                Text(
                    when {
                        total == 0 -> "لا يوجد تجار مخصصون في هذا النطاق"
                        visited >= total -> "أتممت تغطية النطاق المحدد 🎉"
                        else -> "فاضل ${total - visited} تاجر لتغطية ${scopeName ?: "كل مساراتك"}"
                    },
                    color = SalesColors.Muted,
                    fontFamily = ZainRegular
                )
            }
        }
        if (total > visited) {
            KpiCard {
                Text("زوّد تغطيتك بزيارة التجار غير المزارين", fontFamily = ZainBold, color = SalesColors.Success)
                Text("اعرض التجار اللي لسه ما زرتهمش وابدأ بالأقرب.", fontFamily = ZainRegular, color = SalesColors.Muted)
                SalesPrimaryButton(text = "اعرض التجار غير المزارين", onClick = onShowUnvisited, containerColor = SalesColors.Success)
            }
        }
        val scope = state.scope
        if (scope != null) {
            SectionTitle(if (scope.kind == KpiComparisonKind.WEEK) "مقارنة أسبوعية" else "مقارنة شهرية")
            KpiCard {
                Text("الأرقام خاصة بـ ${scopeName ?: "كل المسارات"}", fontSize = 12.sp, color = SalesColors.Muted)
                val now = state.primary?.unique_merchants_visited ?: 0
                val before = state.comparison?.unique_merchants_visited ?: 0
                val primaryTotal = state.primary?.total_merchants ?: total
                val comparisonTotal = state.comparison?.total_merchants ?: total
                LabeledBar(scope.primary.label, "$now تاجر · ${pct(now, primaryTotal).roundToInt()}٪", (pct(now, primaryTotal) / 100).toFloat(), SalesColors.Success)
                LabeledBar(scope.comparison.label, "$before تاجر · ${pct(before, comparisonTotal).roundToInt()}٪", (pct(before, comparisonTotal) / 100).toFloat(), SalesColors.Muted)
                if (total > 0) {
                    val delta = now - before
                    Text(
                        when {
                            delta > 0 -> "التغطية زادت $delta تاجر عن ${scope.comparison.label}."
                            delta < 0 -> "التغطية أقل بـ ${-delta} تاجر عن ${scope.comparison.label}."
                            else -> "التغطية مساوية لـ ${scope.comparison.label}."
                        },
                        color = when {
                            delta > 0 -> SalesColors.Success
                            delta < 0 -> SalesColors.Progress
                            else -> SalesColors.Muted
                        },
                        fontFamily = ZainBold
                    )
                }
            }
        }
    }
}

/** "نسبة الزيارات الناجحة": visits that ended in an order, with the top no-order reason. */
@Composable
fun KpiConversionScreen(viewModel: SalesKpiViewModel, onBack: () -> Unit) {
    val state by viewModel.conversion.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    LaunchedEffect(period) { viewModel.loadConversion() }
    KpiPage("نسبة الزيارات الناجحة", onBack, state.isLoading, period, viewModel::selectPeriod) {
        val strike = state.kpi?.strikeRate
        val withOrder = strike?.withOrder ?: 0
        val completed = strike?.completed ?: 0
        val strikePct = strike?.percentage ?: 0.0
        KpiCard {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PercentRing(strikePct, size = 160.dp, stroke = 12.dp, caption = "نسبة الزيارات الناجحة")
                Text(
                    when {
                        completed == 0 -> "لا توجد زيارات بعد"
                        withOrder == 0 -> "لا توجد طلبات من الزيارات بعد"
                        else -> "من كل ١٠ زيارات، حوالي ${(strikePct / 10).roundToInt()} بيطلبوا"
                    },
                    fontFamily = ZainBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            StatLine("🧾 زيارات بطلب", "$withOrder", SalesColors.Success)
            StatLine("📋 زيارات من غير طلب", "${completed - withOrder}")
            StatLine("📞 طلبات تليفون", "${state.kpi?.adHocVisits?.total ?: 0}", caption = "مش محسوبة زيارة")
        }
        state.scope?.let { scope ->
            SectionTitle(if (scope.kind == KpiComparisonKind.WEEK) "مقارنة أسبوعية" else "مقارنة شهرية")
            KpiCard {
                Text("نسبة الزيارات التي نتج عنها طلب", fontSize = 12.sp, color = SalesColors.Muted)
                val now = state.primary?.strike_rate ?: 0.0
                val before = state.comparison?.strike_rate ?: 0.0
                val delta = now - before
                Text(
                    (if (delta > 0) "+" else "") + oneDecimal(delta) + "٪",
                    fontFamily = ZainBold,
                    color = when {
                        delta > 0 -> SalesColors.Success
                        delta < 0 -> SalesColors.Progress
                        else -> SalesColors.Muted
                    }
                )
                LabeledBar(scope.primary.label, "${oneDecimal(now)}٪", (now / 100).toFloat(), SalesColors.Primary)
                LabeledBar(scope.comparison.label, "${oneDecimal(before)}٪", (before / 100).toFloat(), SalesColors.Muted)
            }
        }
        state.topReason?.let {
            Text(
                "💡 أكتر سبب لعدم الطلب: «$it». ركّز على معالجة هذا السبب.",
                fontFamily = ZainBold,
                color = SalesColors.Progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SalesColors.Progress.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )
        }
    }
}

/** Working days so far in the period, Fridays excluded; the whole month for past months. */
private fun workingDaysElapsed(period: KpiPeriod): Int {
    val month = YearMonth.of(period.year, period.month)
    val today = LocalDate.now(CAIRO_ZONE)
    val lastDay = if (period == currentCairoPeriod()) today.dayOfMonth else month.lengthOfMonth()
    return (1..lastDay).count { month.atDay(it).dayOfWeek != DayOfWeek.FRIDAY }
}

/** "تقرير الأداء": coverage, visits against the target, and sales for the month. */
@Composable
fun KpiReportScreen(viewModel: SalesKpiViewModel, onBack: () -> Unit) {
    val state by viewModel.report.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    LaunchedEffect(period) { viewModel.loadReport() }
    KpiPage("تقرير الأداء", onBack, state.isLoading, period, viewModel::selectPeriod) {
        val kpi = state.kpi
        val isCurrent = kpi?.period?.isCurrentMonth ?: (period == currentCairoPeriod())
        val dailyTarget = kpi?.todayVisits?.target?.takeIf { it > 0 } ?: DEFAULT_DAILY_TARGET
        val workingDays = workingDaysElapsed(period)
        val totalVisits = kpi?.totals?.totalVisits ?: 0
        KpiCard {
            Text("ملخص الأداء", fontFamily = ZainBold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PercentRing(state.coverage?.percentage ?: kpi?.completionRate?.percentage ?: 0.0, size = 80.dp, stroke = 7.dp)
                    Text("تغطية التجار", fontSize = 12.sp, fontFamily = ZainBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isCurrent) {
                        PercentRing(kpi?.todayVisits?.percentage ?: 0.0, size = 80.dp, stroke = 7.dp)
                        Text("زيارات النهاردة", fontSize = 12.sp, fontFamily = ZainBold)
                    } else {
                        Text(oneDecimal(totalVisits.toDouble() / maxOf(workingDays, 1)), fontSize = 24.sp, fontFamily = ZainBold)
                        Text("متوسط الزيارات / يوم", fontSize = 12.sp, fontFamily = ZainBold)
                    }
                }
            }
        }
        KpiCard {
            Text("زيارات الشهر", fontFamily = ZainBold, fontSize = 16.sp)
            val target = dailyTarget * workingDays
            LabeledBar("اللي اتعمل", "$totalVisits", (totalVisits.toFloat() / maxOf(target, 1)), SalesColors.Primary)
            StatLine("الهدف", "$target")
            val today = kpi?.todayVisits
            val average = totalVisits.toDouble() / maxOf(workingDays, 1)
            val (text, color) = if (isCurrent && today != null) {
                (if (today.actual >= today.target) "أتممت هدف اليوم ✓" else "زيارات النهاردة ${today.actual} من ${today.target} — فاضل ${today.target - today.actual}") to
                    when {
                        today.actual >= today.target -> SalesColors.Success
                        today.actual * 2 >= today.target -> SalesColors.Progress
                        else -> SalesColors.Danger
                    }
            } else {
                (if (average >= dailyTarget) "متوسط الزيارات اليومي ${oneDecimal(average)} — حققت الهدف اليومي ✓" else "متوسط الزيارات اليومي ${oneDecimal(average)} من $dailyTarget") to
                    when {
                        average >= dailyTarget -> SalesColors.Success
                        average * 2 >= dailyTarget -> SalesColors.Progress
                        else -> SalesColors.Danger
                    }
            }
            Text(text, color = color, fontFamily = ZainBold, fontSize = 13.sp)
        }
        KpiCard {
            val strike = kpi?.strikeRate?.percentage ?: 0.0
            StatLine("💵 قيمة المبيعات", formatEGP(kpi?.totals?.totalOrderValue ?: 0.0))
            StatLine("🧾 عدد الطلبات", "${kpi?.totals?.totalOrders ?: 0}")
            StatLine(
                "📈 نسبة الزيارات الناجحة",
                "${oneDecimal(strike)}%",
                when {
                    strike >= 75 -> SalesColors.Success
                    strike >= 50 -> SalesColors.Progress
                    else -> SalesColors.Danger
                }
            )
            StatLine("📞 طلبات التليفون", "${kpi?.adHocVisits?.total ?: 0}", caption = "مش محسوبة زيارة")
        }
    }
}
