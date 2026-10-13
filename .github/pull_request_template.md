## What changes

<!-- One or two lines. Link the design in docs/design/ if there is one. -->

## Release label

- [ ] Exactly one of `release:feature` · `release:fix` · `release:major` · `release:none` is applied

## Three companions

- [ ] Exception handling — every new or touched path guarded, failures degrade honestly
- [ ] `Logger.e` — every guard and swallowed failure logs
- [ ] Tests — JUnit for pure logic; device checklist below for what only a phone can prove

## Device checklist

<!-- Steps that need a real phone. Write "none" if there are none. -->

## Checks

- [ ] Title starts with `cld-` (Claude) or `cgpt-` (ChatGPT)
- [ ] No keystore, key file or password added
- [ ] `docs/CHANGELOG.md` and `docs/BACKLOG.md` updated
