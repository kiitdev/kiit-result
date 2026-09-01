package kiit.result;

import kiit.codes.Err;
import kiit.codes.Failed;
import kiit.codes.Passed;
import kiit.codes.Status;
import kotlin.Unit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Exercises kiit-result from plain Java (not Kotlin). Guards the @JvmStatic/@JvmOverloads/
 * @file:JvmName annotations and the sealed-hierarchy `permits` metadata that make the library
 * usable idiomatically from Java, mirroring kiit-codes' own JavaInteropTest.java. See
 * samples/sample-java/src/main/java/sample/SampleApp.java for the same surface as a runnable demo.
 *
 * Note: kiit-result depends on the *published* kiit-codes:0.2.1 artifact, which predates
 * kiit-codes' own Java-interop pass, so kiit-codes Status values are built via their public
 * constructors below rather than via companion constants like `Succeeded.CREATED`. See
 * SampleApp.java's class-level comment for the full explanation.
 */
public class JavaInteropTest {

    @Test
    public void successConstructorHasOverloadsForConvenienceAndForJavaInterop() {
        // 4 ways in from Java: bare value, value+message (hand-rolled secondary), value+status
        // (new @JvmOverloads-generated overload), full value+status+action (primary constructor).
        Success<Integer> bare = new Success<>(42);
        assertEquals(Integer.valueOf(42), bare.getValue());
        assertEquals("SUCCESS", bare.getStatus().getName());

        Success<Integer> withMsg = new Success<>(42, "created via convenience overload");
        assertEquals("created via convenience overload", withMsg.getStatus().getMessage());

        Passed created = new Passed.Succeeded("CREATED", "A new resource was created.", "kiit");
        Success<Integer> withStatus = new Success<>(42, created);
        assertEquals("CREATED", withStatus.getStatus().getName());
        assertNull(withStatus.getAction());

        Action action = new Action("createUser");
        Success<Integer> full = new Success<>(42, created, action);
        assertEquals("createUser", full.getAction().getAction());
    }

    @Test
    public void failureConstructorHasOverloadsForConvenienceAndForJavaInterop() {
        Failure<String> bare = new Failure<>("boom");
        assertEquals("boom", bare.getError());
        assertEquals("UNEXPECTED", bare.getStatus().getName());

        Failed denied = new Failed.Restricted("DENIED", "The request was denied.", "kiit");
        Failure<String> withStatus = new Failure<>("boom", denied);
        assertEquals("DENIED", withStatus.getStatus().getName());
    }

    @Test
    public void resultSealedHierarchySupportsExhaustiveSwitchPatternMatching() {
        // JDK 21 pattern-matching switch, exhaustive with no `default`. Only compiles because
        // Result/Success/Failure are sealed and Kotlin emitted PermittedSubclasses at jvmTarget 21.
        Result<Integer, String> success = new Success<>(1);
        Result<Integer, String> failure = new Failure<>("boom");
        assertEquals("Success: 1", describe(success));
        assertEquals("Failure: boom", describe(failure));
    }

    @Test
    public void passedAndFailedGroupsSupportExhaustiveSwitchPatternMatching() {
        // Overload resolution picks the right 4-leaf switch based on the static type: Passed for
        // a Success's status, Failed for a Failure's.
        Passed pending = new Passed.Pending("QUEUED", "The request is waiting to be processed.", "kiit");
        Failed unserved = new Failed.Unserved("TIMEOUT", "The operation timed out.", "kiit");
        assertEquals("Pending: QUEUED", describeGroup(pending));
        assertEquals("Unserved: TIMEOUT", describeGroup(unserved));
    }

    @Test
    public void statusSealedHierarchySupportsExhaustiveSwitchPatternMatching() {
        // Same 8-leaf switch as kiit-codes' own JavaInteropTest, exercised via a Status pulled off
        // a Success/Failure rather than off a Status value directly.
        Status succeeded = new Success<>(1, new Passed.Succeeded("SUCCESS", "ok", "kiit")).getStatus();
        Status restricted = new Failure<>("boom", new Failed.Restricted("DENIED", "denied", "kiit")).getStatus();
        assertEquals("Succeeded: SUCCESS", describeStatus(succeeded));
        assertEquals("Restricted: DENIED", describeStatus(restricted));
    }

    @Test
    public void actionsFacadeAndWithActionHaveNoChainArgOverload() {
        // @file:JvmName("Actions") on Action.kt: Java sees kiit.result.Actions, not ActionKt.
        // @JvmOverloads on withAction: Java gets a no-`chain`-arg overload defaulting to true.
        Result<Integer, String> bare = new Success<>(42);
        Result<Integer, String> chained = Actions.withAction(bare, new Action("step2"));
        assertEquals("step2", chained.getAction().getAction());
    }

    @Test
    public void resultsFacadeExposesTopLevelExtensionFunctions() {
        // @file:JvmName("Results") on Result.kt: Java sees kiit.result.Results, not ResultKt.
        // Explicit type witness needed: Result<out T, out E>'s covariance shows up as
        // wildcard-bounded generics in the compiled signature, which confuses Java's inference of
        // the lambda parameter's type otherwise. Documented, unfixable Java friction.
        Success<Integer> bare = new Success<>(42);
        Result<Integer, String> mapped = Results.<Integer, Integer, String>flatMap(bare, v -> new Success<>(v + 1));
        assertEquals(Integer.valueOf(43), ((Success<Integer>) mapped).getValue());

        Failure<String> failBare = new Failure<>("boom");
        Integer orElse = Results.getOrElse(failBare, (err) -> -1);
        assertEquals(Integer.valueOf(-1), orElse);
    }

    @Test
    public void outcomesTriesAndOptionsHaveStaticEntryPoints() {
        // @JvmStatic on Outcomes/Tries/Options' own directly-declared members, no `.INSTANCE`
        // needed. The inherited Builder/PassedBuilder/FailedBuilder surface (success/restricted/
        // etc.) stays Outcomes.INSTANCE-only for now, a deliberately deferred, documented gap.
        Result<Integer, Err> attempted = Outcomes.attempt("chargeCard", () -> 100);
        assertEquals(Integer.valueOf(100), attempted.getOrNull());

        Result<Integer, Err> tagged = Outcomes.of(new Action("justiceLeague"), () -> Outcomes.attempt(() -> 7));
        assertEquals("justiceLeague", tagged.getAction().getAction());

        Result<Integer, Throwable> tried = Tries.attempt(() -> 5);
        assertEquals(Integer.valueOf(5), tried.getOrNull());

        Result<Integer, Unit> some = Options.some(9);
        Result<Integer, Unit> none = Options.none();
        assertEquals(Integer.valueOf(9), some.getOrNull());
        assertEquals("NOT_EXISTS", none.getStatus().getName());
    }

    @Test
    public void resultCompanionAttemptAndOutcomeAreStatic() {
        // @JvmStatic on Result.Companion.attempt/outcome: called as Result.attempt(...) /
        // Result.outcome(...) directly, not Result.Companion.attempt(...).
        Result<Integer, Throwable> tried = Result.attempt(() -> 3);
        assertEquals(Integer.valueOf(3), tried.getOrNull());

        Result<Integer, Err> outcome = Result.outcome(() -> 4);
        assertEquals(Integer.valueOf(4), outcome.getOrNull());
    }

    private static String describe(Result<?, ?> result) {
        return switch (result) {
            case Success<?> s -> "Success: " + s.getValue();
            case Failure<?> f -> "Failure: " + f.getError();
        };
    }

    private static String describeGroup(Passed passed) {
        return switch (passed) {
            case Passed.Succeeded s -> "Succeeded: " + s.getName();
            case Passed.Pending p -> "Pending: " + p.getName();
            case Passed.Excluded e -> "Excluded: " + e.getName();
            case Passed.Information i -> "Information: " + i.getName();
        };
    }

    private static String describeGroup(Failed failed) {
        return switch (failed) {
            case Failed.Restricted r -> "Restricted: " + r.getName();
            case Failed.Invalid i -> "Invalid: " + i.getName();
            case Failed.Rejected r -> "Rejected: " + r.getName();
            case Failed.Unserved u -> "Unserved: " + u.getName();
        };
    }

    private static String describeStatus(Status status) {
        return switch (status) {
            case Passed.Succeeded s -> "Succeeded: " + s.getName();
            case Passed.Pending p -> "Pending: " + p.getName();
            case Passed.Excluded e -> "Excluded: " + e.getName();
            case Passed.Information i -> "Information: " + i.getName();
            case Failed.Restricted r -> "Restricted: " + r.getName();
            case Failed.Invalid i -> "Invalid: " + i.getName();
            case Failed.Rejected r -> "Rejected: " + r.getName();
            case Failed.Unserved u -> "Unserved: " + u.getName();
        };
    }
}
