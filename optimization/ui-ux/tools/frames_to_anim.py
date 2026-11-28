"""Turns a series of frames into one animation (animated WebP and GIF), so that an effect can be looked at in motion.

usage: python frames_to_anim.py <dir with the frames> <prefix> <out file without extension> [--width 360] [--tail 700] [--first-png <file>]

The frames are named `<prefix>-<elapsed ms>-hw.png`, as `UiAuditTest.recordFrames` and `menuFrames` write them (the compose clock of the
test is advanced by hand, so the elapsed time of each frame is exact). Each frame is shown for the time until the next one; the last one
stays for --tail ms. --first-png adds a still (for example the screen before the effect starts) as the frame at 0 ms.

Why WebP and GIF and not a video: no video encoder is installed here (and the app cannot be screen-recorded: it sets FLAG_SECURE, which
also blacks out screenrecord). The frames are drawn by the GPU renderer inside the app process, so this animation shows what the
phone draws, frame by frame, at the timing of the effect rather than at the speed of a recording.
"""
import os
import re
import sys

from PIL import Image


def main():
    args = sys.argv[1:]
    width, tail, first = 360, 700, None
    pos = []
    i = 0
    while i < len(args):
        if args[i] == '--width':
            width = int(args[i + 1]); i += 2
        elif args[i] == '--tail':
            tail = int(args[i + 1]); i += 2
        elif args[i] == '--first-png':
            first = args[i + 1]; i += 2
        else:
            pos.append(args[i]); i += 1
    if len(pos) != 3:
        raise SystemExit(__doc__)
    folder, prefix, out = pos
    frames = []
    for name in os.listdir(folder):
        m = re.fullmatch(re.escape(prefix) + r'-(\d+)-hw\.png', name)
        if m:
            frames.append((int(m.group(1)), os.path.join(folder, name)))
    frames.sort()
    if first:
        frames.insert(0, (0, first))
    if not frames:
        raise SystemExit('no frames named %s-<ms>-hw.png in %s' % (prefix, folder))
    images, durations = [], []
    for k, (ms, path) in enumerate(frames):
        im = Image.open(path).convert('RGB')
        im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
        images.append(im)
        durations.append(frames[k + 1][0] - ms if k + 1 < len(frames) else tail)
    durations = [max(d, 20) for d in durations]
    images[0].save(out + '.webp', save_all=True, append_images=images[1:], duration=durations, loop=0, quality=80, method=4)
    images[0].save(out + '.gif', save_all=True, append_images=images[1:], duration=durations, loop=0, optimize=True)
    # a contact sheet of the same frames, for places that cannot play an animation (the elapsed time under each frame)
    from PIL import ImageDraw
    cols = min(len(images), 6)
    rows = (len(images) + cols - 1) // cols
    w, h = images[0].size
    sheet = Image.new('RGB', (cols * w + (cols - 1) * 6, rows * (h + 20) - 0), (120, 120, 120))
    d = ImageDraw.Draw(sheet)
    for k, im in enumerate(images):
        x, y = (k % cols) * (w + 6), (k // cols) * (h + 20)
        sheet.paste(im, (x, y + 20))
        d.text((x + 4, y + 4), '%d ms' % frames[k][0], fill=(255, 255, 255))
    sheet.save(out + '-frames.png')
    print('frames:', len(images), 'total ms:', sum(durations[:-1]), '->', out + '.webp', out + '.gif', out + '-frames.png')


if __name__ == '__main__':
    main()
