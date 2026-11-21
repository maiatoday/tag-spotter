# Project Instructions

## Security & Secrets

- **Never commit API keys or secrets**: Never commit or push any file containing an API key, App Check debug token, private key, OAuth secret, or personal credential (e.g. `local.properties`, `.env*`, `GoogleService-Info.plist`).
- **Keep secrets out of logs and code**: Never hardcode API keys or tokens in source files, tests, docs, or commit messages, and never print secret values when inspecting configuration files (check key presence only).
- **Verify before committing**: Always review `git status` and `git diff --cached` before committing or pushing to ensure no secrets or ignored config files are staged.
- **Manual configuration**: Whenever a task needs an API key, App Check token, or cloud console action, prompt the user rather than fabricating or committing credentials.

## Git Workflow

- **Maintain a linear commit history**: Avoid merge commits ("merge bubbles").
- **Prefer rebase**: Always rebase feature or working branches onto the latest target branch (`git pull --rebase`, `git rebase main`) instead of merging the target branch into the feature branch.
- **Prefer fast-forward merges**: When integrating a branch into `main` (or another target branch), rebase it first and merge using fast-forward only (`git merge --ff-only <branch>`). Do not use `--no-ff`.
