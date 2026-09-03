package com.examscanner.premium.domain.importexport

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * RosterRoundTripPropertyTest - Property-based test for the CSV/Excel roster round trip.
 *
 * [ImportExportService.export] / [ImportExportService.importRoster] are suspend and go
 * through Context/MediaStore/Uri, so they cannot run on a plain JVM. The behavioural core
 * that Property 9 checks — the ENCODE↔DECODE symmetry of the CSV/XLSX writers and readers —
 * lives in [RosterCodec] and is fully JVM-testable (Apache POI runs on the JVM). This test
 * exercises that seam: for any set of valid roster records, encoding to CSV or XLSX and
 * decoding the produced bytes yields the original records.
 *
 * The roster header is fixed by Req 11.2 / 11.4: student_id, name, grade_level, section.
 * The same column structure is used for both formats (Req 11.6).
 *
 * Validates: Requirements 11.2, 11.4, 11.6
 */
class RosterRoundTripPropertyTest {

    /** The fixed roster columns (Req 11.2). Read and write agree on these (Req 11.4). */
    private val header = listOf("student_id", "name", "grade_level", "section")

    data class RosterRecord(
        val studentId: String,
        val name: String,
        val gradeLevel: String,
        val section: String
    ) {
        fun toRow(): List<String> = listOf(studentId, name, gradeLevel, section)
    }

    // Feature: offline-assessment-transformation, Property 9: CSV/Excel roster round-trip
    @Property(tries = 200)
    fun `CSV round-trip yields equivalent roster records`(
        @ForAll("rosters") records: List<RosterRecord>
    ) {
        val rows = records.map { it.toRow() }

        val bytes = RosterCodec.encodeCsv(header, rows)
        val decoded = RosterCodec.decodeCsv(bytes)

        // A roster with no data rows encodes to a header-only file; decoding a
        // header-only CSV gives header + no rows, which is the correct round trip.
        assertEquals(header, decoded.header)
        assertEquals(rows, decoded.rows)
    }

    // Feature: offline-assessment-transformation, Property 9: CSV/Excel roster round-trip
    @Property(tries = 200)
    fun `XLSX round-trip yields equivalent roster records`(
        @ForAll("rosters") records: List<RosterRecord>
    ) {
        val rows = records.map { it.toRow() }

        val bytes = RosterCodec.encodeXlsx(header, rows)
        val decoded = RosterCodec.decodeXlsx(bytes)

        assertEquals(header, decoded.header)
        assertEquals(rows, decoded.rows)
    }

    @Provide
    fun rosters(): Arbitrary<List<RosterRecord>> = record().list().ofMaxSize(30)

    @Provide
    fun record(): Arbitrary<RosterRecord> =
        Combinators.combine(studentId(), freeText(), freeText(), freeText())
            .`as` { id, name, grade, section -> RosterRecord(id, name, grade, section) }

    /**
     * student_id: a non-empty, safe token. Kept alphanumeric-ish so it is a valid student
     * ID and unambiguous in both formats (Req 11.4).
     */
    private fun studentId(): Arbitrary<String> = Arbitraries.strings()
        .withCharRange('a', 'z')
        .withCharRange('A', 'Z')
        .withCharRange('0', '9')
        .withChars('-', '_')
        .ofMinLength(1)
        .ofMaxLength(16)

    /**
     * A free-text field (name / grade_level / section) that deliberately includes the
     * CSV-hostile characters — commas, double quotes, and newlines — plus a sampling of
     * Unicode, so the CSV escaping path is exercised.
     *
     * Input-space constraints that both codecs must satisfy for a faithful round trip:
     *  - No leading/trailing whitespace: the XLSX reader trims cell values, and CSV field
     *    values here are compared exactly, so surrounding whitespace is out of scope.
     *  - No blank record collapses the row: both readers drop all-blank rows, so we require
     *    every free-text value to be non-blank (student_id is already non-empty regardless).
     */
    private fun freeText(): Arbitrary<String> = Arbitraries.strings()
        .withCharRange(' ', '~')                 // printable ASCII incl. , " \ { } etc.
        .withChars(',', '"', '\n')               // force the CSV escaping path
        .withChars('é', 'ñ', 'ü', '—', '“', '”') // a little Unicode
        .ofMinLength(1)
        .ofMaxLength(40)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
