"""Letter editor without rope frame; tabs as fabric bookmark ribbons with cut ends.
Usage: python -I gen_baender.py <out_dir>"""
import os, sys
OUTD = sys.argv[1]; os.makedirs(OUTD, exist_ok=True)
_o = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_ottonien_ui.py'), encoding='utf-8').read()
exec(_o[:_o.index("brief('edit').resize")])
from PIL import Image


def cord_frame(img, x, y, w, h, t=3):
    """No rope any more: a 1 px dark leather outline only."""
    rect(img, x + t - 1, y + t - 1, x + w - t + 1, y + h - t + 1 - (6 if t == 2 else 0), BOOK[0])


def tabs_row(img, x, by, items, active):
    pass


def icon_letter(img, x, y, c):
    rect(img, x, y + 1, x + 9, y + 2, c); rect(img, x, y + 6, x + 9, y + 7, c)
    rect(img, x, y + 1, x + 1, y + 7, c); rect(img, x + 8, y + 1, x + 9, y + 7, c)
    for i in range(4): px(img, x + 1 + i, y + 2 + i // 1 if i < 3 else y + 4, c); px(img, x + 7 - i, y + 2 + i if i < 3 else y + 4, c)


def icon_book(img, x, y, c):
    rect(img, x, y + 1, x + 4, y + 2, c); rect(img, x + 5, y + 1, x + 9, y + 2, c)
    rect(img, x, y + 1, x + 1, y + 7, c); rect(img, x + 8, y + 1, x + 9, y + 7, c)
    rect(img, x + 4, y + 2, x + 5, y + 8, c); rect(img, x, y + 7, x + 9, y + 8, c)
    rect(img, x + 2, y + 3, x + 3, y + 4, c); rect(img, x + 6, y + 3, x + 7, y + 4, c)


def icon_stack(img, x, y, c):
    for k in range(3):
        rect(img, x + k, y + 4 - k * 2, x + 6 + k, y + 5 - k * 2, c)
        rect(img, x + 6 + k, y + 4 - k * 2, x + 7 + k, y + 8 - k * 2, c)
    rect(img, x, y + 4, x + 1, y + 9, c); rect(img, x, y + 8, x + 7, y + 9, c)


def ribbon_band(img, x, y0, y1, w, c, notch_at, label, active):
    """Fabric ribbon: hemmed straight top, soft folds (quantised tones, no pattern),
    darker where it disappears into the book."""
    import math
    tones = [shade(c, .62), shade(c, .78), shade(c, .9), c, shade(c, 1.1), shade(c, 1.2)]
    L = y1 - y0
    for yy in range(y0, y1):
        t = (yy - y0) / max(1, L - 1)
        for i in range(w):
            u = i / max(1, w - 1)
            # two soft diagonal folds drifting along the length + light from top left
            fold = math.sin((u * 2.2 + t * 0.9) * math.pi) * 0.9 + math.sin((u * 0.8 - t * 0.5) * math.pi) * 0.5
            v = 3 + fold - u * 0.8 - t * 1.4
            if i == 0 or i == w - 1: v = 0.6 if i == w - 1 else min(v, 1.5)
            k = max(0, min(5, int(round(v))))
            col = tones[k]
            # hemmed top: one light fold line, one shadow line beneath
            if notch_at == 'top':
                if yy == y0: col = shade(c, .7)
                elif yy == y0 + 1: col = tones[5] if 0 < i < w - 1 else col
                elif yy == y0 + 2: col = tones[1] if 0 < i < w - 1 else col
            px(img, x + i, yy, col)
    iy = y0 + 5
    text(img, x + w // 2, iy, label, NAME if active else OCHRE, center=True)


RIBS = (('Brief', TERRA_A, 'Brief'), ('Buch', hx('#866C36'), 'Buch'), ('Entwurfe', PENNANT, 'Entwurfe'))


def variant(mode, where):
    img = brief(mode)
    img2 = backdrop(); img2.alpha_composite(img.crop((0, 0, W, H - 8)), (0, 8)); img = img2
    bw, bh = 176, 200
    bx, by = (W - bw) // 2, 34
    active = 0 if mode == 'edit' else 1
    x = bx + 18
    for k, (lab, c, ic) in enumerate(RIBS):
        w = tw(lab) + 10
        if where == 'top':
            L = 22 if k == active else 17
            # ribbon comes out of the book's head edge: shadow where it passes the cover
            ribbon_band(img, x, by - L, by + 6, w, c if k == active else shade(c, .85), 'top', ic, k == active)
            rect(img, x, by + 6, x + w, by + 7, shade(c, .45))
        else:
            L = 30 if k == active else 22
            ribbon_band(img, x, by + bh - 4, by + bh + L, w, c if k == active else shade(c, .85), 'bottom', ic, k == active)
            rect(img, x, by + bh - 5, x + w, by + bh - 4, shade(c, .45))
        x += w + 4
    return img


for mode in ('edit', 'view'):
    for where in ('top',):
        variant(mode, where).resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUTD, f'brief_band_{where}_{mode}.png'))
print('ok')
