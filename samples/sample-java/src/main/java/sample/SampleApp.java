package sample;

import kiit.codes.Err;
import kiit.codes.Failed;
import kiit.codes.Passed;
import kiit.codes.Status;
import kiit.result.Action;
import kiit.result.Actions;
import kiit.result.Failure;
import kiit.result.Options;
import kiit.result.Outcomes;
import kiit.result.Result;
import kiit.result.Results;
import kiit.result.Success;
import kiit.result.Tries;
import kotlin.Unit;

/**
 * Living documentation of kiit-result from plain Java — each section below only compiles because
 * of the @JvmStatic/@JvmOverloads/@file:JvmName annotations added to this library, and the
 * exhaustive switch requires the sealed Result hierarchy to emit PermittedSubclasses
 * (jvmTarget = JVM_21). See kiit-result/src/jvmTest/java/kiit/result/JavaInteropTest.java for the
 * same surface exercised as assertions.
 *
 * Note: kiit-result depends on the *published* kiit-codes:0.2.1 artifact (a Maven coordinate),
 * which predates kiit-codes' own Java-interop pass (@JvmField on its status constants, etc. —
 * still only in unpublished PRs). So this sample builds kiit-codes Status values via their public
 * constructors directly, rather than referencing constants like `Succeeded.CREATED` the way
 * kiit-codes' own Java sample can — those constants aren't public fields yet in the version this
 * depends on. Revisit once kiit-codes republishes with its own interop fixes.
 *
 * Also: `Option<T>`/`Outcome<T>`/`Try<T>` (kiit-result's typealiases for `Result<T, Unit>`/
 * `Result<T, Err>`/`Result<T, Throwable>`) are fully erased for Java — there's no `Option`/
 * `Outcome`/`Try` class to import; Java always sees the expanded `Result<T, E>` form.
 */
public class SampleApp {

    public static void main(String[] args) {
        // @JvmOverloads on Success/Failure's primary constructor, alongside the library's own
        // pre-existing hand-rolled convenience constructors — four ways to build a Success now.
        Success<Integer> bare = new Success<>(42);
        Success<Integer> withMsg = new Success<>(42, "created via convenience overload");
        Passed created = new Passed.Succeeded("CREATED", "A new resource was created.", "kiit");
        Success<Integer> withStatus = new Success<>(42, created);
        Success<Integer> full = new Success<>(42, created, new Action("createUser"));
        System.out.println("bare: " + bare.getValue() + " status=" + bare.getStatus().getName());
        System.out.println("withStatus: " + withStatus.getStatus().getName());
        System.out.println("full: " + full.getAction().getAction());

        Failure<String> failBare = new Failure<>("boom");
        Failed denied = new Failed.Restricted("DENIED", "The request was denied.", "kiit");
        Failure<String> failWithStatus = new Failure<>("boom", denied);
        System.out.println("failBare status: " + failBare.getStatus().getName());
        System.out.println("failWithStatus status: " + failWithStatus.getStatus().getName());

        // JDK 21 pattern-matching switch, exhaustive with no `default` — only compiles because
        // Result/Success/Failure are sealed and Kotlin emitted PermittedSubclasses at jvmTarget 21.
        System.out.println("describe(bare): " + describe(bare));
        System.out.println("describe(failBare): " + describe(failBare));

        // Exhaustive switches over the Passed/Failed *group* level (4 leaves each) and over the
        // combined Status hierarchy (8 leaves) — same pattern as kiit-codes' own sample, since
        // Success/Failure carry a Passed/Failed status. All three compile with no `default`
        // branch only because Passed, Failed, and Status are sealed and jvmTarget = JVM_21 makes
        // Kotlin emit PermittedSubclasses for each.
        System.out.println("group(withStatus): " + describeGroup(withStatus.getStatus()));
        System.out.println("group(failWithStatus): " + describeGroup(failWithStatus.getStatus()));
        System.out.println("status(withStatus): " + describeStatus(withStatus.getStatus()));
        System.out.println("status(failWithStatus): " + describeStatus(failWithStatus.getStatus()));

        // Outcomes/Tries/Options — each object's own directly-declared members are @JvmStatic;
        // the inherited Builder/PassedBuilder/FailedBuilder surface (success/restricted/etc.)
        // stays Outcomes.INSTANCE-only for now — a deliberately deferred, documented limitation.
        Result<Integer, Err> attempted = Outcomes.attempt("chargeCard", () -> 100);
        System.out.println("attempted: " + attempted.getOrNull());

        Result<Integer, Err> tagged =
            Outcomes.of(new Action("justiceLeague"), () -> Outcomes.attempt(() -> 7));
        System.out.println("tagged action: " + tagged.getAction().getAction());

        Result<Integer, Throwable> tried = Tries.attempt(() -> 5);
        System.out.println("tried: " + tried.getOrNull());

        Result<Integer, Unit> some = Options.some(9);
        Result<Integer, Unit> none = Options.none();
        System.out.println("some: " + some.getOrNull() + ", none status: " + none.getStatus().getName());

        // Action / withAction — @JvmOverloads gives Java the no-`chain`-arg form.
        Result<Integer, String> chained = Actions.withAction(bare, new Action("step2"));
        System.out.println("chained action: " + chained.getAction().getAction());

        // Results — the @file:JvmName("Results") facade for Result.kt's top-level extension
        // functions, not the auto-generated ResultKt. Explicit type witness needed here — the
        // wildcard-bounded generics from Result<out T, out E>'s covariance (a real, documented,
        // unfixable Java friction) confuse Java's inference of the lambda parameter's type.
        Result<Integer, String> mapped = Results.<Integer, Integer, String>flatMap(bare, v -> new Success<>(v + 1));
        Integer orElse = Results.getOrElse(failBare, () -> -1);
        System.out.println("mapped: " + ((Success<Integer>) mapped).getValue() + ", orElse: " + orElse);
    }

    private static String describe(Result<?, ?> result) {
        return switch (result) {
            case Success<?> s -> "Success: " + s.getValue();
            case Failure<?> f -> "Failure: " + f.getError();
        };
    }

    // Exhaustive, no-`default` switch over Passed's 4 leaf subtypes — overload resolution picks
    // this one when the static type of the argument is Passed (e.g. Success.getStatus()).
    private static String describeGroup(Passed passed) {
        return switch (passed) {
            case Passed.Succeeded s -> "Succeeded: " + s.getName();
            case Passed.Pending p -> "Pending: " + p.getName();
            case Passed.Excluded e -> "Excluded: " + e.getName();
            case Passed.Information i -> "Information: " + i.getName();
        };
    }

    // Exhaustive, no-`default` switch over Failed's 4 leaf subtypes — picked when the static type
    // is Failed (e.g. Failure.getStatus()).
    private static String describeGroup(Failed failed) {
        return switch (failed) {
            case Failed.Restricted r -> "Restricted: " + r.getName();
            case Failed.Invalid i -> "Invalid: " + i.getName();
            case Failed.Rejected r -> "Rejected: " + r.getName();
            case Failed.Unserved u -> "Unserved: " + u.getName();
        };
    }

    // Exhaustive, no-`default` switch over the full 8-leaf Status hierarchy (Passed's 4 + Failed's
    // 4 combined) — takes the common Status supertype, so it works for either a Success's or a
    // Failure's status without needing overload resolution to pick a branch.
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
