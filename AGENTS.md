# Project Instructions

## Git Workflow

- **Maintain a linear commit history**: Avoid merge commits ("merge bubbles").
- **Prefer rebase**: Always rebase feature or working branches onto the latest target branch (`git pull --rebase`, `git rebase main`) instead of merging the target branch into the feature branch.
- **Prefer fast-forward merges**: When integrating a branch into `main` (or another target branch), rebase it first and merge using fast-forward only (`git merge --ff-only <branch>`). Do not use `--no-ff`.
