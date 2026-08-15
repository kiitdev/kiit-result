# Contributing

Thanks for your interest in kiit-result.

## Where things stand right now

This project is early. It builds `Result<T, E>` on top of kiit-codes' taxonomy, and just went
through a significant round of review and testing to reach `1.0.x`. **I'm not accepting general
pull requests at this stage.** The priority right now is letting the design settle and get
real-world feedback before opening up broader contribution.

This isn't a permanent stance, just a "not yet."

## What's welcome right now

- **Bug reports.** If something is broken, incorrect, or the docs don't match the actual
  behavior, please open an [issue](../../issues).
- **Design feedback.** Disagree with a naming choice, a builder signature, or a default? Open a
  [Discussion](../../discussions). This is genuinely useful, and it's the best way to influence
  where the project goes next.
- **Questions.** Also welcome in Discussions, not Issues.

## Language ports

If you're interested in porting this to another language, please start with a Discussion, not
a PR. I'd like to talk through the approach first, what stays faithful to the original design,
what should adapt to the target language's own idioms, before any code gets written. This keeps
everyone's effort from being wasted, and gives the port a real shot at becoming an official,
linked module rather than staying disconnected.

**kiit-result depends directly on kiit-codes' taxonomy.** `Success`/`Failure` carry a `Passed`/
`Failed` status from that taxonomy, not a separate one invented for this module. A port of
kiit-result only makes sense alongside a real port of kiit-codes in the same language, see that
project's own [CONTRIBUTING.md](https://github.com/kiitdev/kiit-codes/blob/main/CONTRIBUTING.md)
for the language criteria that applies to both.

**Ports hosted here need to genuinely enforce the same guarantees the design depends on.** A real
sum type and compiler-checked exhaustive matching over the success/failure split, not just a
naming convention that happens to resemble it. A "port" in a language that can't enforce this is
a fundamentally different, weaker thing wearing the same name, and I'd rather not host something
that only looks like the real design.

This is about my principles on this design, not gatekeeping. An independent, unofficial
implementation in any language is always fine to build on your own. This only concerns what gets
hosted as an official Kiit port.

## Before opening a PR

If you've discussed something in an Issue or Discussion and I've said a PR would be welcome,
please make sure:

- The PR references the Discussion or Issue it came out of.
- Tests are included for any behavioral change.
- The change is scoped narrowly, one concern per PR.

PRs opened without prior discussion may be closed and asked to start with a Discussion instead.
Not out of unfriendliness, just to keep effort aligned before code gets written.

## Build, test, and publish

See [BUILD.md](./BUILD.md) for local build, test, and publish instructions.

## Code of conduct

Be respectful. Disagreement about design is welcome and expected, personal attacks are not.
