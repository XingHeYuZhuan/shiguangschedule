package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import kotlin.test.Test
import kotlin.test.assertEquals

class CourseSchemeNormalizerTest {

    @Test
    fun mergesIdenticalSchemesAndUnionsWeeks() {
        val schemes = listOf(
            CourseScheme(
                day = 1,
                startSection = 1,
                endSection = 2,
                teacher = "赵迪",
                position = "致新楼西区-213",
                weeks = setOf(4, 5, 8, 9, 10, 11)
            ),
            CourseScheme(
                day = 1,
                startSection = 1,
                endSection = 2,
                teacher = "赵迪",
                position = "致新楼西区-213",
                weeks = setOf(6)
            )
        )

        val result = normalizeCourseSchemes("自然语言处理", schemes)

        assertEquals(1, result.size)
        assertEquals(setOf(4, 5, 6, 8, 9, 10, 11), result.first().weeks)
    }

    @Test
    fun keepsDifferentTeacherAsSeparateSchemes() {
        val schemes = listOf(
            CourseScheme(
                day = 5,
                startSection = 9,
                endSection = 10,
                teacher = "赵丹",
                position = "致新楼西区-318",
                weeks = setOf(13)
            ),
            CourseScheme(
                day = 5,
                startSection = 9,
                endSection = 10,
                teacher = "郑东升",
                position = "弘德楼B区-101",
                weeks = setOf(14)
            )
        )

        val result = normalizeCourseSchemes("形势与政策5", schemes)

        assertEquals(2, result.size)
    }

    @Test
    fun keepsDifferentTimeAsSeparateSchemes() {
        val schemes = listOf(
            CourseScheme(day = 1, startSection = 1, endSection = 2, weeks = setOf(4, 5)),
            CourseScheme(day = 1, startSection = 3, endSection = 4, weeks = setOf(4, 5))
        )

        val result = normalizeCourseSchemes("人工智能前沿专题研讨", schemes)

        assertEquals(2, result.size)
    }
}
