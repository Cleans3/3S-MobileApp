# Lab 1: Java Card applets (Bài thực hành số 1)

- **Date:** 2026-11-04
- **Subject:** Smart card development (see `../../../smart-card/`)
- **Handout:** Cao Vinh, CNTT
- **Language:** Java Card (Classic applets, Java Card 2.2.x API: `short`/`byte` only, no `String`)

The handout asks you to first work through the "pocket calculator applet" (Tạo Applet máy tính bỏ túi) from the lab guide, section 1.5, as Bài 0. Then write Bài 1 to 4:

| Bài | Task | Applet | Package AID | Applet AID |
|---|---|---|---|---|
| 0 | Pocket calculator from the guide: add, subtract, multiply, divide | `kma.lab1.bai0.Calculator` | `112233445500` | `11223344550000` |
| 1 | Print "Xin chao, KMA" | `kma.lab1.bai1.HelloApplet` | `4B4D414C3101` | `4B4D414C310101` |
| 2 | Three functions with fixed values: full name, date of birth, both | `kma.lab1.bai2.ProfileApplet` | `4B4D414C3102` | `4B4D414C310201` |
| 3 | Area of a square and a rectangle | `kma.lab1.bai3.AreaApplet` | `4B4D414C3103` | `4B4D414C310301` |
| 4 | Enter one string of student ID, full name, date of birth and hometown, then print each field and the whole string | `kma.lab1.bai4.StudentApplet` | `4B4D414C3104` | `4B4D414C310401` |

Bài 0 keeps the guide's AID, so its test transcript applies without changes. For Bài 1–4, the AID prefix `4B4D414C31` is ASCII "KMAL1".

## Layout

- `src/kma/lab1/baiN/`: one package per exercise, so each one builds into its own CAP file.
- `apdu/baiN.scr`: an `apdutool` script for each exercise. The comment above every command describes it, and the text after `->` is the expected response.
- `tools/StepRunner.java`: runs a script on jCardSim and explains each step.
- `run.sh`, `jcardsim.cfg`: compile everything and run the scripts on the simulator.

## Commands

**Bài 0** (CLA `A0`, as in the guide)
- `INS 00` adds, `01` subtracts, `02` multiplies, `03` divides.
- The operands are `P1` and `P2`, read as signed bytes (-128..127).
- The result is a 2-byte signed short. For example, 2 - 5 returns `FF FD` (-3).
- A different CLA returns `6E 00`.

There are two changes from the guide's code:
- Dividing by zero returns `6B 00`. The guide's code would crash on it with `6F 00`.
- `setIncomingAndReceive()` is gone, because these commands carry no data.

The applet still refuses an Le below 2 with `67 00`. jCardSim 2.2.2 can't test that check, because its `setOutgoing()` always returns 256. A real card or cref honours the Le you send.

Bài 1–4 use CLA `80`.

**Bài 1**
- `INS 01`: returns "Xin chao, KMA".

**Bài 2**
- `INS 01`: full name.
- `INS 02`: date of birth.
- `INS 03`: "full name - date of birth".

The values are constants at the top of `ProfileApplet.java`: "Tran Hoang Dung" and "36/67/4200".

**Bài 3**
- `INS 01` with data `side` (1 byte): area of the square.
- `INS 02` with data `length width` (1 byte each): area of the rectangle.
- The result is 2 bytes, unsigned big-endian. For example, a 4 x 7 rectangle returns `00 1C` (28).
- Each side is 0..255, so the area fits in 16 bits. The largest is 255 x 255 = `FE 01` (65025).
- A wrong data length returns `67 00`.

**Bài 4**
- `INS 01` with data `id|full name|date of birth|hometown` stores the string. It must contain exactly 3 `|` separators, or the card returns `6A 80`. The limit is 255 bytes.
- `INS 02` with `P1` = 1..4 returns one field: 1 student ID, 2 full name, 3 date of birth, 4 hometown. Any other `P1` returns `6A 86`.
- `INS 03` returns the whole string.
- Reading before anything is stored returns `69 85`.
- The record lives in persistent memory, so it survives a power cycle. The data and its length are written in one transaction.
- The card stores raw bytes, so UTF-8 Vietnamese works too, but `apdutool` shows only hex.

## Running

**On the simulator (no card needed).** Needs a JDK (`sudo apt-get install openjdk-21-jdk-headless`).

```bash
./run.sh      # all exercises, Bai 0 to 4
./run.sh -i 0 # install and select Bai 0, then type your own APDUs (e.g. a0000203 = 2 + 3)
./run.sh 3    # Bai 3 only
```

`run.sh` does the following:
1. Downloads jCardSim 2.2.2 from Maven Central into `.lib/` and checks its SHA-1.
2. Compiles `src/` and `tools/` against it.
3. Runs each `apdu/baiN.scr` through `StepRunner` on a fresh simulated card. `jcardsim.cfg` maps each AID to its applet class. Each script's first command, `CREATE APPLET` (`80 B8`), creates the applet instance.

`StepRunner` prints each step on its own. For every command it shows:
- what the command does,
- the command bytes, split into CLA, INS, P1, P2, Lc and Le,
- the data sent and the reply, each as hex and also as text or as a number,
- the status word and what it means,
- the expected result taken from the script.

The last full run was on 2026-11-04 with OpenJDK 21. Every command in all five scripts returned the response in its comment.

**On a real card or the Oracle cref simulator.**
1. Convert each package into a CAP file with the Java Card SDK, using the NetBeans Java Card project or `converter`, with the AIDs above.
2. Install the CAP file.
3. Run `apdutool apdu/baiN.scr`.

cref accepts the `CREATE APPLET` line. On a real card, install with GlobalPlatform instead (for example `gp --install`), and that line will fail, which is harmless.
