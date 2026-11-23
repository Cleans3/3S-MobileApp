# P08 rank 1 (BookCover.kt): crash check per protocol A10, 2026-11-21

AVD lowend34 (API 34, swiftshader_indirect, headless, emulator process in a no-swap cgroup scope). One trial = fresh emulator boot, then N cycles of
`am start` (10 s) and `am force-stop` (8 s); the emulator process is polled every second. "died" = qemu-system-x86_64 exits with SIGSEGV (exit 139).

| app | build | cycles | result |
|---|---|---|---|
| original corpus app, unmodified (apk-corpus/agy-g38flash-B1-naive-r1.apk) | debuggable, targetSdk 34, Compose BOM 2024.02 | 3 + 4 + 4 | no crash (3 cold launches, then two 4-cycle trials) |
| the same APK, manifest debuggable flag cleared, re-signed with the debug key | non-debuggable | 5 + 5 | no crash |
| port 1: BookCover alone in a 72 dp cover slot (optlab, release R8, target 36) | non-debuggable | 7 launches at n = 1, 4, 12, 200 | crash within about 5 s of the first draw, 7 of 7 |
| port 2: BookCard + BookCover in a LazyVerticalGrid(Adaptive 150 dp), as the corpus CatalogueScreen (release R8, target 36) | non-debuggable | 2 trials | died at launch 3 (59 s) and launch 3 (55 s) |
| port 2, noR8 build (target 36) | non-debuggable | 1 trial | died at launch 2 (29 s) |
| port 2, release R8, target 34 | non-debuggable | 2 trials | died at launch 3 (54 s) and launch 5 (97 s) |
| port 2, noR8, target 34 | non-debuggable | 1 trial | no crash in 4 cycles |
| port 2, debuggable twin of noR8 (target 36) | debuggable | 2 trials | no crash in 4 cycles each |
| port 2 B (flat cover) | release R8 | 1 trial | no crash in 4 cycles |

| port 2 (release R8, target 36), emulator process under gdb | non-debuggable | 1 trial | died at launch 4. Across all five gdb trials below the first launch after boot never crashed |
| port 2 with `--ei n 4` (4 books, like the original's first screen) | non-debuggable | 1 trial | died at launch 3: the number of cards is not the trigger |
| port 2 after `cmd package compile -m verify`, and after `-m speed` | non-debuggable | 1 trial each | died at launch 2 in both: the compile mode (JIT only or AOT) is not the trigger |
| port 2 with `pm clear` before every launch | non-debuggable | 1 trial | died at launch 3: the persistent HWUI shader cache is not the trigger |
| original corpus app (unmodified) with a swipe loop (15 up and down flings per cycle, 40 s) | debuggable | 4 cycles | no crash |

| port 2 on `-gpu guest` (the emulator falls back to lavapipe mode; the GLES translator is still SwiftShader) | non-debuggable | 1 trial | died at launch 3 (exit 139) |
| port 2 on `-gpu swangle_indirect` (ANGLE on SwiftShader Vulkan, no SwiftShader GLES JIT) | non-debuggable | 1 trial | no crash in 8 launches (then the P08 benchmark window, see the REPORT) |

Reading: the unmodified original does not crash, not even repackaged as non-debuggable, so A10 treats the crash as a port defect. The port's slot geometry (72 dp)
was one defect candidate and is fixed (port 2 uses the corpus caller and grid), but port 2 still crashes in non-debuggable builds, and the cause is not isolated.
Not the cause alone: R8, targetSdk (34 and 36 both crash), the cover slot size, the debuggable flag alone (a debuggable port does not crash, like the original).
Host crash mechanism (gdb, `qemu-crash-gdb-port2.txt`, two crashes with the same signature): SIGSEGV in a host RenderThread; the faulting address is in the JIT heap, so it is inside a shader routine that SwiftShader (libvk_swiftshader, gles_swiftshader) compiled at run time. The faulting instruction is a SIMD load at a large base+index offset (`movdqu 0x184f0(%rax,%rcx,1)`, then `pshufd` of the 4th component, as for an interpolated colour with alpha): an out-of-bounds read in a generated pixel routine. The guest log shows nothing before it. The platform renderer (HWUI/Skia) is the same for the original and the port, so what differs is the draw sequence Compose emits, and Compose differs (BOM 2024.02 against 2026.09).
Ruled out by the extra trials above: R8, targetSdk, slot size, number of cards, compile mode, the persistent shader cache, the debuggable flag alone, screen load (the original also survives a scroll loop).
Resolution for the study: the same AVD with `-gpu swangle_indirect` does not crash (8 of 8 launches, then a full benchmark window), so P08 is measured on that GPU path (both variants, documented in the REPORT §9 and `results/environment-p08-swangle.json`). The cause of the difference between the original and the port is not isolated: Compose BOM 2024.02 against 2026.09 (a probe needs the optlab build on the old BOM with Kotlin 2.4.21, not a drop-in change), theme and window setup. Not isolated, and not fixable by a mechanical change to the port.
Files: `port-72dp-slot-crash-logcat.txt` (guest logcat of a crashing launch; the guest log shows nothing before the host process dies), `original-app-catalogue.png` (the original app showing BookCover in its catalogue grid).
