# CLAUDE.md

Rules for working in this repository.

## Git

- Never run `git commit` without an explicit request from the user in the current message.
- Never run `git push` without an explicit request from the user in the current message.

## Code style

- Do not write comments in code (no inline, block, or docstring comments), unless the user explicitly asks for a comment to be added.
- Always follow DRY: do not duplicate logic/code — extract repeated fragments into shared methods/classes instead of copy-pasting.
- Do not use magic number/values. It is better to create constants. 
- Constants names should be meaningful.

### Test class changes

If you create, modify, or delete a test class:

1. Run ALL tests in the affected test class.
2. Do not run only the newly added test methods.
3. Verify that all tests in the class pass.
4. If any test fails, investigate and fix the problem before considering the task complete.

### Test Coverage

Code coverage does not necessarily mean that a scenario is tested.

Do not consider a code path adequately tested merely because it is
executed by another test.

Each important business scenario and branch must have a dedicated test
that explicitly verifies the expected behavior.

Avoid relying on incidental or indirect coverage, where a code path is
executed as a side effect of another test but its behavior is not asserted.

## Mandatory verification

Before considering any task complete:

1. Identify all production files changed.
2. Identify all test classes affected by those changes.
3. Run the complete test suite of every affected test class.
4. If a test fails, investigate and fix the issue.
5. Re-run the affected test classes after every fix.
6. Only report the task as complete after all relevant tests pass.

A code change is not considered complete until its relevant tests have been executed.

### Before finishing a task

Always:

1. Run the relevant tests.
2. Verify that all tests pass.
3. Report which test commands were executed and their results.

Never consider a task complete if the relevant tests have not been executed.