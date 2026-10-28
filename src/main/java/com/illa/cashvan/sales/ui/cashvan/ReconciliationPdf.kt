package com.illa.cashvan.sales.ui.cashvan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.illa.cashvan.R
import com.illa.cashvan.sales.domain.cashvan.CashvanPlanType
import com.illa.cashvan.sales.domain.checkout.PaymentType
import com.illa.cashvan.sales.domain.reconciliation.resolveOrderPaymentType
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.ReconciliationReport
import com.illa.cashvan.sales.ui.route.formatDateTime
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 36f
private const val NAVY = 0xFF2D2B5B.toInt()

data class ReconciliationAgent(val name: String, val code: String)

/**
 * The daily reconciliation report as an A4 PDF in the cache directory, laid out like the PWA's: header,
 * report details, money by payment type, stock by product, and the counted orders.
 */
fun writeReconciliationPdf(context: Context, report: ReconciliationReport, agent: ReconciliationAgent): File {
    val regular = ResourcesCompat.getFont(context, R.font.zain_regular) ?: Typeface.DEFAULT
    val bold = ResourcesCompat.getFont(context, R.font.zain_bold) ?: Typeface.DEFAULT_BOLD
    val writer = PdfWriter(regular, bold)

    writer.title("تقفيل الحساب اليومي")
    val now = ZonedDateTime.now(ZoneId.of("Africa/Cairo"))
    listOf(
        "الشركة" to "فرونت دوور لوجستيك للنقل والتوزيع",
        "تاريخ التقرير" to now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.US)),
        "اسم المندوب" to agent.name,
        "كود المندوب" to agent.code,
        "كود الخطة" to report.planCode,
        "نوع الخطة" to when (report.planType) {
            CashvanPlanType.PRE_SELL -> "بري سيل"
            CashvanPlanType.HYBRID -> "كاش فان + بري سيل"
            else -> "كاش فان"
        }
    ).forEach { (label, value) -> writer.row(listOf(label to 0.35f, value to 0.65f)) }

    writer.section("التقفيل")
    val totals = report.totals
    buildList {
        add("نقدي" to totals.cash)
        if (totals.credit > 0) add("آجل" to totals.credit)
        if (totals.fawry > 0) add("فوري" to totals.fawry)
        add("الإجمالي" to totals.cash + totals.credit + totals.fawry)
    }.forEach { (label, value) -> writer.row(listOf(label to 0.5f, money(value) to 0.5f)) }

    writer.section("تفاصيل المخزون")
    val stockColumns = listOf(0.4f, 0.18f, 0.14f, 0.14f, 0.14f)
    writer.header(listOf("المنتج", "قيمة المبيعات", "مخصص", "مباع", "متبقي").zip(stockColumns))
    report.groups.forEach { group ->
        writer.groupRow(group.principalName)
        group.rows.forEach { row ->
            writer.row(
                listOf(
                    "${row.productName} (${row.sku})",
                    money(row.totalValue),
                    quantity(row.assignedQuantity),
                    quantity(row.soldQuantity),
                    quantity(row.remainingQuantity)
                ).zip(stockColumns)
            )
        }
    }
    if (report.groups.isEmpty()) writer.row(listOf("لا توجد منتجات في الخطة" to 1f))

    writer.section("تفاصيل الطلبات")
    val orderColumns = listOf(0.06f, 0.16f, 0.2f, 0.22f, 0.12f, 0.1f, 0.14f)
    writer.header(listOf("#", "رقم الطلب", "تاريخ الإنشاء", "التاجر", "نوع الطلب", "طريقة الدفع", "الإجمالي").zip(orderColumns))
    report.orders.forEachIndexed { index, order ->
        writer.row(
            listOf(
                "${index + 1}",
                order.formattedCode,
                formatDateTime(order.createdAt),
                order.merchant?.signName ?: order.merchant?.name.orEmpty(),
                if (order.orderType == "pre_sell") "بري سيل" else "كاش فان",
                paymentLabel(resolveOrderPaymentType(order)),
                money(order.totalIncome ?: 0.0)
            ).zip(orderColumns)
        )
    }
    if (report.orders.isEmpty()) writer.row(listOf("لا توجد طلبات على هذه الخطة" to 1f))

    val file = File(context.cacheDir, "daily_reconciliation_${LocalDate.now(ZoneOffset.UTC)}.pdf")
    writer.finish(file)
    return file
}

internal fun paymentLabel(type: PaymentType): String = when (type) {
    PaymentType.CASH -> "نقدي"
    PaymentType.CREDIT -> "آجل"
    PaymentType.FAWRY -> "فوري"
}

private fun money(value: Double) = String.format(Locale.US, "%,.2f EGP", value)
private fun quantity(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)

/** Right-to-left rows that flow onto new pages. Column widths are fractions of the content width. */
private class PdfWriter(regular: Typeface, bold: Typeface) {
    private val document = PdfDocument()
    private var pageNumber = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = 0f
    private val contentWidth = PAGE_WIDTH - 2 * MARGIN

    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regular; textSize = 9f; textAlign = Paint.Align.RIGHT }
    private val strong = Paint(text).apply { typeface = bold; textSize = 9.5f }
    private val sectionPaint = Paint(strong).apply { textSize = 13f; color = NAVY }
    private val fill = Paint().apply { color = NAVY }
    private val light = Paint().apply { color = 0xFFF1F1F6.toInt() }
    private val line = Paint().apply { color = 0xFFE1E1EA.toInt(); strokeWidth = 0.5f }

    init {
        newPage()
    }

    fun title(value: String) {
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 64f, fill)
        canvas.drawText(value, PAGE_WIDTH - MARGIN, 40f, Paint(sectionPaint).apply { color = 0xFFFFFFFF.toInt(); textSize = 20f })
        y = 88f
    }

    fun section(value: String) {
        ensure(40f)
        y += 14f
        canvas.drawText(value, PAGE_WIDTH - MARGIN, y, sectionPaint)
        y += 10f
    }

    fun header(cells: List<Pair<String, Float>>) {
        ensure(22f)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, light)
        drawCells(cells, strong, 20f)
    }

    fun groupRow(value: String) {
        ensure(20f)
        canvas.drawText(value, PAGE_WIDTH - MARGIN - 4f, y + 14f, Paint(strong).apply { color = NAVY })
        y += 20f
    }

    fun row(cells: List<Pair<String, Float>>) {
        ensure(18f)
        drawCells(cells, text, 18f)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line)
    }

    private fun drawCells(cells: List<Pair<String, Float>>, paint: Paint, height: Float) {
        var right = PAGE_WIDTH - MARGIN
        cells.forEach { (value, fraction) ->
            val width = contentWidth * fraction
            canvas.drawText(ellipsize(value, paint, width - 8f), right - 4f, y + height * 0.7f, paint)
            right -= width
        }
        y += height
    }

    private fun ellipsize(value: String, paint: Paint, width: Float): String {
        if (paint.measureText(value) <= width) return value
        var end = value.length
        while (end > 1 && paint.measureText(value, 0, end) + paint.measureText("…") > width) end--
        return value.substring(0, end) + "…"
    }

    private fun ensure(height: Float) {
        if (y + height > PAGE_HEIGHT - MARGIN - 20f) {
            closePage()
            newPage()
            y = MARGIN
        }
    }

    private fun newPage() {
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
    }

    private fun closePage() {
        canvas.drawRect(0f, PAGE_HEIGHT - 18f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), fill)
        document.finishPage(page)
    }

    fun finish(file: File) {
        closePage()
        file.outputStream().use { document.writeTo(it) }
        document.close()
    }
}
