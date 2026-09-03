package com.examscanner.premium.onboarding

/**
 * OnboardingTooltips - Lightweight contextual-tooltip content provider (Req 28.3).
 *
 * Maps a stable feature key to plain-language help text for complex features such as
 * the discrimination index and mastery levels. UI layers (e.g. an info icon next to a
 * chart) look up text with [tooltipFor] rather than hard-coding help strings, keeping
 * guidance copy in one place.
 *
 * This is intentionally simple - a static content map, no state. It does not render UI.
 *
 * Requirements: 28.3
 */
object OnboardingTooltips {

    /** Stable keys for features that expose a contextual tooltip. */
    const val KEY_DISCRIMINATION_INDEX = "discrimination_index"
    const val KEY_DIFFICULTY_INDEX = "difficulty_index"
    const val KEY_MASTERY_LEVELS = "mastery_levels"
    const val KEY_MELC_MAPPING = "melc_mapping"
    const val KEY_ITEM_ANALYSIS = "item_analysis"

    private val TOOLTIPS: Map<String, String> = mapOf(
        KEY_DISCRIMINATION_INDEX to
            "Discrimination index shows how well a question separates high performers " +
            "from low performers. Values above 0.30 are good; near zero or negative " +
            "means the item may be confusing or mis-keyed.",
        KEY_DIFFICULTY_INDEX to
            "Difficulty index is the share of students who answered correctly. Higher " +
            "means easier. Aim for a mix - very high or very low values carry little " +
            "diagnostic value.",
        KEY_MASTERY_LEVELS to
            "Mastery levels summarize how well a student has learned each competency " +
            "(MELC): typically Beginning, Developing, Proficient, and Advanced, based " +
            "on the percentage of related questions answered correctly.",
        KEY_MELC_MAPPING to
            "MELC mapping links each question to a DepEd Most Essential Learning " +
            "Competency, so reports can show mastery per competency instead of just a " +
            "total score.",
        KEY_ITEM_ANALYSIS to
            "Item analysis breaks down each question - how many chose each option and " +
            "how the item performed - to help you spot weak or ambiguous questions."
    )

    /**
     * Return the tooltip text for [featureKey], or null when no tooltip is defined.
     */
    fun tooltipFor(featureKey: String): String? = TOOLTIPS[featureKey]

    /** Whether a contextual tooltip exists for [featureKey]. */
    fun hasTooltip(featureKey: String): Boolean = TOOLTIPS.containsKey(featureKey)
}
