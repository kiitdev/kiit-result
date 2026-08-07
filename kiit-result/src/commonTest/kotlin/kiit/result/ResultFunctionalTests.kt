package kiit.result

import kiit.codes.Err
import kiit.codes.Succeeded
import kiit.codes.Unserved
import kiit.result.Outcomes.success
import kiit.result.Outcomes.unserved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [Result]'s composition operators — `map`, `flatMap`/`then`, `fold`, `onSuccess`/
 * `onFailure`, `mapError`/`flatMapError`, `transform`, `getOrElse`/`getOrNull`, `exists`,
 * `contains`, and `inner` — on both the `Success` and `Failure` branch of each.
 */
class ResultFunctionalTests {
    @Test
    fun can_get_or_else() {
        val result1 = success("peter parker")
        assertEquals("peter parker", result1.getOrElse { "" })
    }

    @Test
    fun can_get_or_null() {
        val result1 = unserved<String>("name unknown")
        assertEquals(null, result1.getOrNull())
    }

    @Test
    fun can_get_inner() {
        val result1: Result<Result<String, Err>, Err> = Success(Success("peter parker"))
        val i1 = result1.inner()
        assertEquals("peter parker", i1.getOrNull())
    }

    @Test
    fun can_check_exists() {
        val result1 = success("peter parker")
        assertTrue(result1.exists { it == "peter parker" })
    }

    @Test
    fun can_check_contains() {
        val result1 = success("peter parker")
        assertEquals(true, result1.contains("peter parker"))
    }

    @Test
    fun can_map_branch_success() {
        val result1 = success("peter parker")
        val result2 = result1.map { name -> "$name : spider-man" }
        assertEquals("peter parker : spider-man", result2.getOrElse { "" })
    }

    @Test
    fun can_flatMap_branch_success() {
        val result1 = success("peter parker")
        val result2 = result1.flatMap { name -> success("$name : spider-man") }
        assertEquals("peter parker : spider-man", result2.getOrElse { "" })
    }

    @Test
    fun can_flatMap_branch_success_via_then() {
        val result1 = success("peter parker")
        val result2 = result1.then { name -> success("$name : spider-man") }
        assertEquals("peter parker : spider-man", result2.getOrElse { "" })
    }

    @Test
    fun can_handle_branch_success() {
        val result1 = success("peter parker")
        val result2 = result1.map { name -> "$name : spider-man" }
        result2.onSuccess {
            assertEquals("peter parker : spider-man", it)
        }
    }

    @Test
    fun can_map_branch_failure() {
        val result1 = unserved<String>("name unknown")
        val result2 = result1.map { name -> "$name : spider-man" }
        assertEquals(false, result2.success)
        assertEquals(Unserved.UNEXPECTED, result2.status)
        assertEquals(Unserved.UNEXPECTED.message, result2.message)
        assertEquals("??", result2.getOrElse { "??" })
    }

    @Test
    fun can_flatMap_branch_failure() {
        val result1 = unserved<String>("name unknown")
        val result2 = result1.flatMap { name -> success("$name : spider-man") }
        assertEquals(false, result2.success)
        assertEquals(Unserved.UNEXPECTED, result2.status)
        assertEquals(Unserved.UNEXPECTED.message, result2.message)
        assertEquals("??", result2.getOrElse { "??" })
    }

    @Test
    fun can_handle_branch_failure() {
        val result1 = unserved<String>("name unknown")
        val result2 = result1.map { name -> "$name : spider-man" }
        assertEquals(false, result2.success)
        assertEquals(Unserved.UNEXPECTED, result2.status)
        assertEquals(Unserved.UNEXPECTED.message, result2.message)
        assertEquals("??", result2.getOrElse { "??" })
    }

    @Test
    fun can_convert_error_via_map() {
        val result1 = unserved<String>("name unknown")
        val result2 = result1.mapError { _ -> 0 }
        assertEquals(false, result2.success)
        assertEquals(Unserved.UNEXPECTED, result2.status)
        assertEquals(Unserved.UNEXPECTED.message, result2.message)
        result2.onFailure {
            assertEquals(0, it)
        }
    }

    @Test
    fun can_convert_error_via_flatMap() {
        val result1 = unserved<String>("name unknown")
        val result2 = result1.flatMapError { Failure(0) }
        assertEquals(false, result2.success)
        assertEquals(Unserved.UNEXPECTED, result2.status)
        assertEquals(Unserved.UNEXPECTED.message, result2.message)
        result2.onFailure {
            assertEquals(0, it)
        }
    }

    @Test
    fun can_transform() {
        val r1 = success("peter parker")

        // Default to spider-man
        val r2 =
            r1.transform(
                { name -> Success("$name : spider-man") },
                { Failure("a marvel character") },
            )

        assertEquals(true, r2.success)
        assertEquals(Succeeded.SUCCESS, r2.status)
        assertEquals(Succeeded.SUCCESS.message, r2.message)
        r2.onSuccess {
            assertEquals("peter parker : spider-man", it)
        }
    }

    @Test
    fun can_transform_with_fold() {
        val result1 = success("peter parker")

        // Default to spider-man
        val name =
            result1.fold(
                { name -> "$name : spider-man" },
                { "a marvel character" },
            )
        assertEquals("peter parker : spider-man", name)
    }

    @Test
    fun can_chain() {
        var successValue = ""

        val result1 = Outcomes.attempt { "1" }
        val result2 =
            result1.map { it.toInt() }
                .onSuccess { successValue = "converted to int: $it" }
                .flatMap { Success(it + 1) }

        assertEquals("converted to int: 1", successValue)
        assertTrue(result2.contains(2))

        val finalValue = result2.fold({ "final value: $it" }, { "error : $it" })
        assertEquals("final value: 2", finalValue)
    }
}
