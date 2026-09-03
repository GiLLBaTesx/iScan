package com.examscanner.premium.data

object SampleMelcsData {
    
    fun getMathGrade7Q1(): List<MelcEntity> {
        return listOf(
            MelcEntity(
                code = "M7NS-Ia-1",
                description = "Describe the set of integers, rational numbers, and irrational numbers",
                gradeLevel = "Grade 7",
                subject = "Mathematics",
                quarter = 1
            ),
            MelcEntity(
                code = "M7NS-Ib-1",
                description = "Perform operations on rational numbers",
                gradeLevel = "Grade 7",
                subject = "Mathematics",
                quarter = 1
            ),
            MelcEntity(
                code = "M7NS-Ic-d-1",
                description = "Express rational numbers from fraction form to decimal form and vice versa",
                gradeLevel = "Grade 7",
                subject = "Mathematics",
                quarter = 1
            ),
            MelcEntity(
                code = "M7AL-Ie-1",
                description = "Translate verbal phrases to mathematical phrases and vice versa",
                gradeLevel = "Grade 7",
                subject = "Mathematics",
                quarter = 1
            ),
            MelcEntity(
                code = "M7AL-If-1",
                description = "Evaluate algebraic expressions for given values of the variables",
                gradeLevel = "Grade 7",
                subject = "Mathematics",
                quarter = 1
            )
        )
    }
    
    fun getScienceGrade7Q1(): List<MelcEntity> {
        return listOf(
            MelcEntity(
                code = "S7LT-Ia-1",
                description = "Identify parts of the microscope and their functions",
                gradeLevel = "Grade 7",
                subject = "Science",
                quarter = 1
            ),
            MelcEntity(
                code = "S7LT-Ib-2",
                description = "Focus specimens using the compound microscope",
                gradeLevel = "Grade 7",
                subject = "Science",
                quarter = 1
            ),
            MelcEntity(
                code = "S7LT-Ic-3",
                description = "Differentiate plant and animal cells according to presence or absence of certain organelles",
                gradeLevel = "Grade 7",
                subject = "Science",
                quarter = 1
            ),
            MelcEntity(
                code = "S7LT-Id-4",
                description = "Explain why the cell is considered the basic structural and functional unit of all organisms",
                gradeLevel = "Grade 7",
                subject = "Science",
                quarter = 1
            ),
            MelcEntity(
                code = "S7LT-Ie-5",
                description = "Differentiate unicellular and multicellular organisms",
                gradeLevel = "Grade 7",
                subject = "Science",
                quarter = 1
            )
        )
    }
    
    fun getEnglishGrade7Q1(): List<MelcEntity> {
        return listOf(
            MelcEntity(
                code = "EN7LC-I-a-1",
                description = "Identify the structure of a simple two-level outline",
                gradeLevel = "Grade 7",
                subject = "English",
                quarter = 1
            ),
            MelcEntity(
                code = "EN7RC-I-a-7",
                description = "Distinguish between general and specific statements",
                gradeLevel = "Grade 7",
                subject = "English",
                quarter = 1
            ),
            MelcEntity(
                code = "EN7WC-I-a-2.2",
                description = "Use the passive and active voice meaningfully in varied contexts",
                gradeLevel = "Grade 7",
                subject = "English",
                quarter = 1
            ),
            MelcEntity(
                code = "EN7V-I-a-10.1",
                description = "Give technical and operational definitions",
                gradeLevel = "Grade 7",
                subject = "English",
                quarter = 1
            ),
            MelcEntity(
                code = "EN7LR-I-a-2.2",
                description = "Explain how the elements specific to a genre contribute to the theme of a particular literary selection",
                gradeLevel = "Grade 7",
                subject = "English",
                quarter = 1
            )
        )
    }
    
    fun getAllSampleMelcs(): List<MelcEntity> {
        return getMathGrade7Q1() + getScienceGrade7Q1() + getEnglishGrade7Q1()
    }
}
