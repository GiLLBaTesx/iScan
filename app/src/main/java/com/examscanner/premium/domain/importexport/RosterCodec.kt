package com.examscanner.premium.domain.importexport

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader

/**
 * RosterCodec - the pure, Android-free encode/decode seam behind [ImportExportService]'s
 * CSV and XLSX I/O.
 *
 * [ImportExportService.export] / [ImportExportService.importRoster] are suspend and go
 * through Context/MediaStore/Uri, so they cannot run on a plain JVM. The actual byte-level
 * table serialization (CSV escaping + XLSX via Apache POI) has no Android dependency, so it
 * lives here and operates on [ByteArray] / [InputStream]. [ImportExportService] delegates to
 * these functions and only owns the Context/Uri plumbing.
 *
 * Read and write are symmetric (same header names, same column meanings), so a
 * write-then-read round trip preserves the data (Property 9 / Task 7.6).
 *
 * Both formats share the same column structure (Req 11.6). The roster shape is
 * student_id, name, grade_level, section (Req 11.2), and roster import reads the same
 * columns back (Req 11.4).
 */
internal object RosterCodec {

    /** Header + data rows of a table, regardless of source/target format. */
    data class ParsedFile(val header: List<String>, val rows: List<List<String>>)

    // region CSV --------------------------------------------------------------------

    /** Encodes [header] + [rows] as UTF-8 CSV bytes, quoting fields that need it. */
    fun encodeCsv(header: List<String>, rows: List<List<String>>): ByteArray {
        val sb = StringBuilder()
        sb.append(header.joinToString(",") { escapeCsv(it) }).append('\n')
        rows.forEach { row ->
            sb.append(row.joinToString(",") { escapeCsv(it) }).append('\n')
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /** Decodes CSV [bytes] into a header row plus data rows. */
    fun decodeCsv(bytes: ByteArray): ParsedFile = decodeCsv(ByteArrayInputStream(bytes))

    /** Decodes CSV from [input] into a header row plus data rows. */
    fun decodeCsv(input: InputStream): ParsedFile {
        val rows = mutableListOf<List<String>>()
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
            // Honour quoted fields that may span multiple physical lines (embedded \n).
            var record = readCsvRecord(reader)
            while (record != null) {
                if (record.isNotBlank()) rows.add(parseCsvLine(record))
                record = readCsvRecord(reader)
            }
        }
        if (rows.isEmpty()) return ParsedFile(emptyList(), emptyList())
        return ParsedFile(header = rows.first(), rows = rows.drop(1))
    }

    /**
     * Reads one logical CSV record, which may span multiple physical lines when a quoted
     * field contains an embedded newline. Returns null at end of input.
     */
    private fun readCsvRecord(reader: BufferedReader): String? {
        val first = reader.readLine() ?: return null
        val sb = StringBuilder(first)
        // If quotes are unbalanced, the record continues on the next physical line(s).
        while (countUnescapedQuotes(sb) % 2 != 0) {
            val next = reader.readLine() ?: break
            sb.append('\n').append(next)
        }
        return sb.toString()
    }

    private fun countUnescapedQuotes(sb: CharSequence): Int {
        var count = 0
        for (ch in sb) if (ch == '"') count++
        return count
    }

    private fun escapeCsv(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }

    /** CSV line parser that honours quoted fields and escaped quotes. */
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i < line.length - 1 && line[i + 1] == '"') {
                        current.append('"'); i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString()); current.clear()
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    // endregion

    // region XLSX -------------------------------------------------------------------

    /** Encodes [header] + [rows] as .xlsx bytes via Apache POI. */
    fun encodeXlsx(header: List<String>, rows: List<List<String>>): ByteArray {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Sheet1")
            val headerRow = sheet.createRow(0)
            header.forEachIndexed { i, value ->
                headerRow.createCell(i).setCellValue(value)
            }
            rows.forEachIndexed { r, row ->
                val sheetRow = sheet.createRow(r + 1)
                row.forEachIndexed { c, value ->
                    sheetRow.createCell(c).setCellValue(value)
                }
            }
            val out = ByteArrayOutputStream()
            workbook.write(out)
            return out.toByteArray()
        }
    }

    /** Decodes .xlsx [bytes] into a header row plus data rows. */
    fun decodeXlsx(bytes: ByteArray): ParsedFile = decodeXlsx(ByteArrayInputStream(bytes))

    /** Decodes .xlsx from [input] into a header row plus data rows. */
    fun decodeXlsx(input: InputStream): ParsedFile {
        WorkbookFactory.create(input).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val all = mutableListOf<List<String>>()
            for (row in sheet) {
                val cells = ArrayList<String>()
                val lastCell = row.lastCellNum.toInt()
                for (c in 0 until lastCell) {
                    cells.add(readCellAsString(row, c))
                }
                if (cells.any { it.isNotBlank() }) all.add(cells)
            }
            if (all.isEmpty()) return ParsedFile(emptyList(), emptyList())
            return ParsedFile(header = all.first(), rows = all.drop(1))
        }
    }

    private fun readCellAsString(row: Row, index: Int): String {
        val cell = row.getCell(index) ?: return ""
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                val n = cell.numericCellValue
                if (n == Math.floor(n) && !n.isInfinite()) n.toLong().toString() else n.toString()
            }
            org.apache.poi.ss.usermodel.CellType.FORMULA -> cell.cellFormula
            else -> ""
        }.trim()
    }

    // endregion
}
