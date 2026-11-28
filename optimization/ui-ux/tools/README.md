# Tools to compare the app now with the app as first cloned

The "first clone" is the Android app as it was when the repository was cloned onto this machine (2026-11-22, `af2c9cf`). Its code is the tag `ui-baseline`: `git diff af2c9cf ui-baseline -- android/app/src/main` is empty. Everything below compares that with `HEAD`. The report that uses these tools is `../baseline-compare/README.md`.

| Tool | What it makes | Needs |
|---|---|---|
| `repo_numbers.py <audit dir>` | Markdown tables counted from git (files, lines, hard-coded strings, strings per language, tests) and from the audit tables (Accessibility Test Framework findings, before and now). Nothing typed in by hand | git, Python |
| `baseline_pairs.py <audit dir> <out dir>` | "before \| after" pictures per screen and theme. Before: the 2026-11-23 baseline pictures kept in git history (`audit/A/` at `c4280b8`); after: `run_audit.ps1 -Configs light,dark` on the same emulator size | Pillow |
| `frames_to_anim.py <dir> <prefix> <out>` | An animated WebP and GIF, and a contact sheet with the elapsed time under each frame, from frames named `<prefix>-<ms>-hw.png` (the frames `UiAuditTest` writes for the menu and for opening a book: the test advances the compose clock by hand, so each frame has an exact time) | Pillow |
| `mockup_frames.py <rendered dir> <board> <out dir>` | The same kind of frames for a mockup board: its CSS animations are paused at given times and screenshot by headless Chrome | Chrome, Pillow |
| `record_effects.ps1 -Serial <device> -Label <L> [-Repo <worktree>]` | A real video (`video/<L>/effects.mp4`) of a fixed sequence (scroll, menu, open a book and back, switch screens) at real speed, and the frame statistics of that pass (`gfxinfo.txt`) | device or emulator, Gradle |
| `video_compare.py ../video` | `compare.html`: the two videos side by side with play, pause, speed 0.25 and the frame-statistics table; `gfx-summary.md` | Python; a browser to play it |

## Why the video is made through a test

The app sets `FLAG_SECURE` (a security rule: no screenshots or recordings of loans and history), and that also blacks out `screenrecord`. `UiAuditTest#effectsVideo` clears the flag on the window of its own run only; the product code and every build keep it. A release build cannot be recorded this way without being changed: it needs a server to log in to, and R8 renames the classes the test uses. So both videos are of **debug builds**, after a **warm-up pass** (the first run through the code is the slow one in a debug build), on the same emulator and window size, with the test starting and stopping `screenrecord` for the second pass itself. The statistics are `dumpsys gfxinfo` for that pass only.

## Recording the app as first cloned

```powershell
git worktree add --detach C:\t\base ui-baseline
cd C:\t\base
git checkout c4280b8 -- android/app/build.gradle.kts android/gradle/libs.versions.toml android/app/src/androidTest   # the audit harness, which the tag does not have
copy <this repo>\android\local.properties android\
# add an effectsVideo test to the harness' UiAuditTest as on the current one, written for the five-tab bar (the file used is `baseline-effectsVideo.kt.txt`)
.\record_effects.ps1 -Serial emulator-5556 -Label BASE -Repo C:\t\base
.\record_effects.ps1 -Serial emulator-5556 -Label NOW
python video_compare.py ..\video
```

The baseline's `effectsVideo` is not in the repository (the baseline worktree is outside it): it is the same sequence as `UiAuditTest#effectsVideo` here, with the old screens (list, book, `Loans`, `Reserved`, `Catalog` tabs).
