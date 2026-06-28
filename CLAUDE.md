# CarRentingTest — Claude Instructions

## Documentation Rule (MANDATORY)

For every action taken and every piece of code written in this project, Claude must document:

1. **What** — describe the action or code change performed.
2. **Why** — explain the reason: the problem it solves, the requirement it fulfills, or the trade-off it makes.

### How to apply

- Before/while writing code: state in chat what is about to change and why.
- Inside code: add a short comment above non-trivial blocks explaining the why (hidden constraint, invariant, workaround). Skip comments that only restate what the code does.
- After edits: end with a brief summary listing each change and its rationale.
- For shell/tool actions (file moves, installs, migrations, git ops): explain purpose before running.

### Format for end-of-turn summary

```
Changes:
- <file:line> — <what changed>. Why: <reason>.
- <action> — <what done>. Why: <reason>.
```

This rule overrides default "no comments / terse summary" guidance for this project.

## Git Workflow Rule (MANDATORY)

Every push must go through a new branch and a pull request. Never push directly to `master`.

### How to apply

1. Before making changes, create a new branch off the latest `master`:
   - Branch name format: `<type>/<short-kebab-description>` (e.g. `feat/rental-overlap-fix`, `fix/auth-token-expiry`, `chore/update-deps`).
2. Commit work on that branch.
3. Push the branch to `origin` with `-u` on first push.
4. Open a pull request targeting `master` using `gh pr create`.
5. PR title and body must describe **what** changed and **why**.

### Attribution Rule

- Do **not** mention "Claude", "Claude Code", "Codex", "AI", or any assistant tooling in:
  - Commit messages
  - PR titles
  - PR descriptions
  - Co-authored-by trailers
- Write all commits and PRs as if authored by the human developer.
