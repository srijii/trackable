
## Git & Version Control Management

The AI agent is responsible for managing Git throughout development.

### Before starting work
- Check the current Git branch and working tree status.
- Inspect existing changes before modifying files.
- Never overwrite, discard, or revert the user's existing changes.
- Do not switch branches without asking the user.

### After completing each task
- Review all modified and newly created files.
- Check the changes using git diff.
- Run relevant builds, tests, and verification.
- Summarize what was implemented.
- Show the important files changed.
- Ask the user whether they want to commit the completed task.

### Commit approval
- NEVER commit without explicit user approval.
- If the user approves, create a meaningful commit.
- Use conventional commit messages where appropriate.
- Keep commits focused on the completed task.
- If the user declines, leave the changes uncommitted and continue.
- Never include unrelated user changes in a commit.

### Ongoing Git management
- Keep track of the current branch.
- Monitor git status throughout development.
- Avoid unnecessary commits.
- Resolve merge conflicts only after explaining the situation.
- Never force push, reset, or rewrite Git history without explicit permission.
- Never push to a remote repository without explicit permission.

### Final report
At the end of the overall task, report:
- Tasks completed
- Commits created
- Files changed
- Tests and builds performed
- Remaining uncommitted changes

