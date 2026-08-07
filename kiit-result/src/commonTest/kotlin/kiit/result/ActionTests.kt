package kiit.result

import kiit.codes.Err
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


/**
 * Tests [Action], [withAction], its propagation through map/mapError/toOutcome/toTry,
 * and the Action-tagging overloads on [Outcomes].
 */
class ActionTests {
    @Test
    fun can_attach_action_with_no_prior_action() {
        val result = Success(42).withAction(Action("chargeCard", xid = "req-1"))
        assertEquals("chargeCard", result.action?.action)
        assertEquals("req-1", result.action?.xid)
        assertNull(result.action?.previous)
    }

    @Test
    fun can_chain_action_by_default() {
        val inner = Success(42).withAction(Action("chargeCard"))
        val outer = inner.withAction(Action("processOrder"))

        assertEquals("processOrder", outer.action?.action)
        assertEquals("chargeCard", outer.action?.previous?.action)
    }

    @Test
    fun can_opt_out_of_chaining() {
        val inner = Success(42).withAction(Action("chargeCard"))
        val outer = inner.withAction(Action("processOrder"), chain = false)

        assertEquals("processOrder", outer.action?.action)
        assertNull(outer.action?.previous)
    }

    @Test
    fun action_survives_map() {
        val tagged = Success(42).withAction(Action("chargeCard"))
        val mapped = tagged.map { it + 1 }

        assertEquals(43, mapped.getOrNull())
        assertEquals("chargeCard", mapped.action?.action)
    }

    @Test
    fun action_survives_map_error() {
        val tagged = Failure<String>("boom").withAction(Action("chargeCard"))
        val mapped = tagged.mapError { it.length }

        assertEquals("chargeCard", mapped.action?.action)
    }

    @Test
    fun action_survives_to_outcome() {
        val tagged = Failure<String>("boom").withAction(Action("chargeCard"))
        val outcome = tagged.toOutcome()

        assertEquals("chargeCard", outcome.action?.action)
    }

    @Test
    fun action_survives_to_try() {
        val tagged = Failure<String>("boom").withAction(Action("chargeCard"))
        val tried = tagged.toTry()

        assertEquals("chargeCard", tried.action?.action)
    }

    @Test
    fun outcomes_attempt_tags_success() {
        val result = Outcomes.attempt("chargeCard", xid = "req-1") { 42 }
        assertEquals(true, result.success)
        assertEquals(42, result.getOrNull())
        assertEquals("chargeCard", result.action?.action)
        assertEquals("req-1", result.action?.xid)
    }

    @Test
    fun outcomes_attempt_tags_caught_exception() {
        val result = Outcomes.attempt("chargeCard") { throw RuntimeException("declined") }
        assertEquals(false, result.success)
        assertEquals("chargeCard", result.action?.action)
    }

    @Test
    fun outcomes_of_tags_existing_outcome_without_catching() {
        val result = Outcomes.of("getUser", xid = "req-2") { Outcomes.attempt { "alice" } }
        assertEquals(true, result.success)
        assertEquals("alice", result.getOrNull())
        assertEquals("getUser", result.action?.action)
        assertEquals("req-2", result.action?.xid)
    }

    @Test
    fun outcomes_attempt_still_works_unrenamed_signature() {
        val result = Outcomes.attempt { 42 }
        assertEquals(true, result.success)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun tries_attempt_still_catches_exceptions() {
        val result = Tries.attempt { throw RuntimeException("boom") }
        assertEquals(false, result.success)
    }
}
