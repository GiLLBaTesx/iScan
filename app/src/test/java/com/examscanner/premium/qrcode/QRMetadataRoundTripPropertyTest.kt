package com.examscanner.premium.qrcode

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * QRMetadataRoundTripPropertyTest - Property-based test for the QR metadata serialization
 * round trip.
 *
 * The physical QR bitmap path (ZXing + android.graphics.Bitmap) requires a device/Robolectric,
 * but the metadata contract itself — [QRCodeGenerator.ExamMetadata.toJson] and
 * [QRCodeGenerator.ExamMetadata.fromJson] — is pure org.json and fully JVM-testable. This test
 * exercises that contract: encoding then decoding any valid metadata must reproduce an
 * equivalent value.
 *
 * Requires real org.json on the unit-test classpath (testImplementation 'org.json:json'),
 * otherwise the bundled Android stub throws "not mocked".
 */
class QRMetadataRoundTripPropertyTest {

    // Feature: offline-assessment-transformation, Property 5: QR metadata round-trip
    @Property(tries = 200)
    fun `encoding then decoding ExamMetadata yields an equivalent value`(
        @ForAll("validMetadata") metadata: QRCodeGenerator.ExamMetadata
    ) {
        val json = metadata.toJson()
        val decoded = QRCodeGenerator.ExamMetadata.fromJson(json)

        assertEquals(metadata, decoded)
    }

    @Provide
    fun validMetadata(): Arbitrary<QRCodeGenerator.ExamMetadata> {
        val examIds: Arbitrary<Long> = Arbitraries.longs()
        val subjectIds: Arbitrary<Long> = Arbitraries.longs()
        val createdAts: Arbitrary<Long> = Arbitraries.longs()
        val totalQuestions: Arbitrary<Int> = Arbitraries.integers().between(0, 200)
        val versions: Arbitrary<Int> = Arbitraries.integers().between(1, 100)

        // JSON-safe but varied: printable ASCII plus a sampling of Unicode letters,
        // punctuation, and whitespace. org.json round-trips these faithfully. Excludes
        // control chars and lone surrogates which are not part of the valid input space.
        val examNames: Arbitrary<String> = Arbitraries.strings()
            .withCharRange(' ', '~')          // printable ASCII (includes quotes, backslash, braces)
            .withChars('é', 'ñ', 'ü', 'ç', '—', '“', '”', '•', '™')
            .ofMinLength(0)
            .ofMaxLength(60)

        return Combinators.combine(
            examIds,
            examNames,
            totalQuestions,
            subjectIds,
            createdAts,
            versions
        ).`as` { examId, examName, total, subjectId, createdAt, version ->
            QRCodeGenerator.ExamMetadata(
                examId = examId,
                examName = examName,
                totalQuestions = total,
                subjectId = subjectId,
                createdAt = createdAt,
                version = version
            )
        }
    }
}
