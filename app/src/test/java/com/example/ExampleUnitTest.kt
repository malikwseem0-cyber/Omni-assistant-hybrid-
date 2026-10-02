package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun cosine_similarity_calculation_isAccurate() {
        val v1 = listOf(1.0f, 0.0f, 0.0f)
        val v2 = listOf(1.0f, 0.0f, 0.0f)
        val v3 = listOf(0.0f, 1.0f, 0.0f)

        fun sim(a: List<Float>, b: List<Float>): Float {
            var dot = 0.0
            var n1 = 0.0
            var n2 = 0.0
            for (i in a.indices) {
                dot += a[i] * b[i]
                n1 += a[i] * a[i]
                n2 += b[i] * b[i]
            }
            return (dot / (sqrt(n1) * sqrt(n2))).toFloat()
        }

        assertEquals(1.0f, sim(v1, v2), 0.001f)
        assertEquals(0.0f, sim(v1, v3), 0.001f)
    }
}
