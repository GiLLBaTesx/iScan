package com.examscanner.premium.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Built-in grading scales and answer sheet templates
 * Pre-populated data for teacher convenience
 */
object BuiltInData {
    
    // ==================== GRADING SCALES ====================
    
    fun getBuiltInGradingScales(): List<GradingScaleEntity> {
        return listOf(
            depEdK12Scale(),
            traditionalScale(),
            internationalBaccalaureateScale()
        )
    }
    
    private fun depEdK12Scale(): GradingScaleEntity {
        val transmutation = JSONArray().apply {
            // DepEd K-12 Grading Scale (60-100)
            put(JSONObject().apply {
                put("min", 96)
                put("max", 100)
                put("grade", "Outstanding")
                put("level", "Advanced")
                put("color", "#34C759") // Green
            })
            put(JSONObject().apply {
                put("min", 90)
                put("max", 95)
                put("grade", "Very Satisfactory")
                put("level", "Proficient")
                put("color", "#007AFF") // Blue
            })
            put(JSONObject().apply {
                put("min", 85)
                put("max", 89)
                put("grade", "Satisfactory")
                put("level", "Approaching")
                put("color", "#5AC8FA") // Light Blue
            })
            put(JSONObject().apply {
                put("min", 80)
                put("max", 84)
                put("grade", "Fairly Satisfactory")
                put("level", "Developing")
                put("color", "#FFCC00") // Yellow
            })
            put(JSONObject().apply {
                put("min", 75)
                put("max", 79)
                put("grade", "Passing")
                put("level", "Developing")
                put("color", "#FF9500") // Orange
            })
            put(JSONObject().apply {
                put("min", 60)
                put("max", 74)
                put("grade", "Did Not Meet Expectations")
                put("level", "Beginning")
                put("color", "#FF3B30") // Red
            })
        }
        
        return GradingScaleEntity(
            name = "DepEd K-12",
            scaleType = "DEPED_K12",
            minGrade = 60,
            maxGrade = 100,
            passingGrade = 75,
            transmutationJson = transmutation.toString(),
            isBuiltIn = true
        )
    }
    
    private fun traditionalScale(): GradingScaleEntity {
        val transmutation = JSONArray().apply {
            put(JSONObject().apply {
                put("min", 95)
                put("max", 100)
                put("grade", "Excellent")
                put("level", "A")
                put("color", "#34C759")
            })
            put(JSONObject().apply {
                put("min", 90)
                put("max", 94)
                put("grade", "Very Good")
                put("level", "B+")
                put("color", "#007AFF")
            })
            put(JSONObject().apply {
                put("min", 85)
                put("max", 89)
                put("grade", "Good")
                put("level", "B")
                put("color", "#5AC8FA")
            })
            put(JSONObject().apply {
                put("min", 80)
                put("max", 84)
                put("grade", "Fair")
                put("level", "C")
                put("color", "#FFCC00")
            })
            put(JSONObject().apply {
                put("min", 75)
                put("max", 79)
                put("grade", "Passing")
                put("level", "D")
                put("color", "#FF9500")
            })
            put(JSONObject().apply {
                put("min", 0)
                put("max", 74)
                put("grade", "Failing")
                put("level", "F")
                put("color", "#FF3B30")
            })
        }
        
        return GradingScaleEntity(
            name = "Traditional (75-100)",
            scaleType = "TRADITIONAL",
            minGrade = 75,
            maxGrade = 100,
            passingGrade = 75,
            transmutationJson = transmutation.toString(),
            isBuiltIn = true
        )
    }
    
    private fun internationalBaccalaureateScale(): GradingScaleEntity {
        val transmutation = JSONArray().apply {
            put(JSONObject().apply {
                put("min", 95)
                put("max", 100)
                put("grade", "7")
                put("level", "Excellent")
                put("color", "#34C759")
            })
            put(JSONObject().apply {
                put("min", 90)
                put("max", 94)
                put("grade", "6")
                put("level", "Very Good")
                put("color", "#007AFF")
            })
            put(JSONObject().apply {
                put("min", 80)
                put("max", 89)
                put("grade", "5")
                put("level", "Good")
                put("color", "#5AC8FA")
            })
            put(JSONObject().apply {
                put("min", 70)
                put("max", 79)
                put("grade", "4")
                put("level", "Satisfactory")
                put("color", "#FFCC00")
            })
            put(JSONObject().apply {
                put("min", 60)
                put("max", 69)
                put("grade", "3")
                put("level", "Mediocre")
                put("color", "#FF9500")
            })
            put(JSONObject().apply {
                put("min", 0)
                put("max", 59)
                put("grade", "1-2")
                put("level", "Poor")
                put("color", "#FF3B30")
            })
        }
        
        return GradingScaleEntity(
            name = "International Baccalaureate",
            scaleType = "IB",
            minGrade = 0,
            maxGrade = 100,
            passingGrade = 60,
            transmutationJson = transmutation.toString(),
            isBuiltIn = true
        )
    }
    
    // ==================== ANSWER SHEET TEMPLATES ====================
    
    fun getBuiltInTemplates(): List<TemplateEntity> {
        return listOf(
            template25Items4Choices(),
            template50Items4Choices(),
            template60Items5Choices(),
            template100Items4Choices(),
            trueFalse50Items()
        )
    }
    
    private fun template25Items4Choices(): TemplateEntity {
        return TemplateEntity(
            name = "25 Items (A-D)",
            totalQuestions = 25,
            numberOfChoices = 4,
            templateType = "STANDARD",
            sectionsJson = "[]",
            isBuiltIn = true,
            headerText = "Answer Sheet - 25 Questions",
            includeSchoolLogo = true,
            qrCodePosition = "TOP_RIGHT",
            filePath = "",
            fileType = "PDF"
        )
    }
    
    private fun template50Items4Choices(): TemplateEntity {
        return TemplateEntity(
            name = "50 Items (A-D)",
            totalQuestions = 50,
            numberOfChoices = 4,
            templateType = "STANDARD",
            sectionsJson = "[]",
            isBuiltIn = true,
            headerText = "Answer Sheet - 50 Questions",
            includeSchoolLogo = true,
            qrCodePosition = "TOP_RIGHT",
            filePath = "",
            fileType = "PDF"
        )
    }
    
    private fun template60Items5Choices(): TemplateEntity {
        val sections = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "Part I")
                put("startQuestion", 1)
                put("endQuestion", 60)
                put("choices", 5)
                put("pointsEach", 1)
            })
        }
        
        return TemplateEntity(
            name = "60 Items (A-E) - NAT Format",
            totalQuestions = 60,
            numberOfChoices = 5,
            templateType = "STANDARD",
            sectionsJson = sections.toString(),
            isBuiltIn = true,
            headerText = "National Achievement Test Format",
            includeSchoolLogo = true,
            qrCodePosition = "TOP_RIGHT",
            filePath = "",
            fileType = "PDF"
        )
    }
    
    private fun template100Items4Choices(): TemplateEntity {
        val sections = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "Part I")
                put("startQuestion", 1)
                put("endQuestion", 50)
                put("choices", 4)
                put("pointsEach", 1)
            })
            put(JSONObject().apply {
                put("name", "Part II")
                put("startQuestion", 51)
                put("endQuestion", 100)
                put("choices", 4)
                put("pointsEach", 1)
            })
        }
        
        return TemplateEntity(
            name = "100 Items (A-D) - 2 Parts",
            totalQuestions = 100,
            numberOfChoices = 4,
            templateType = "MULTI_SECTION",
            sectionsJson = sections.toString(),
            isBuiltIn = true,
            headerText = "Comprehensive Exam - 100 Items",
            includeSchoolLogo = true,
            qrCodePosition = "TOP_RIGHT",
            filePath = "",
            fileType = "PDF"
        )
    }
    
    private fun trueFalse50Items(): TemplateEntity {
        return TemplateEntity(
            name = "50 Items (True/False)",
            totalQuestions = 50,
            numberOfChoices = 2,
            templateType = "TRUE_FALSE",
            sectionsJson = "[]",
            isBuiltIn = true,
            headerText = "True or False Answer Sheet",
            includeSchoolLogo = true,
            qrCodePosition = "TOP_RIGHT",
            filePath = "",
            fileType = "PDF"
        )
    }
}
