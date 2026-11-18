"""People book with fabric ribbons on top (tabs) and on the left (filters). Usage: python -I gen_personen_band.py <out_dir>"""
import os, sys, math
OUTD = sys.argv[1]; os.makedirs(OUTD, exist_ok=True)
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_baender.py'), encoding='utf-8').read()
exec(_b[:_b.index("for mode in ('edit', 'view'):")])
from PIL import Image


def ribbon_h(img, x0, x1, y, h, c, label, active):
    """Horizontal fabric ribbon coming out of the book's left edge; hemmed left end."""
    tones = [shade(c, .62), shade(c, .78), shade(c, .9), c, shade(c, 1.1), shade(c, 1.2)]
    L = x1 - x0
    for xx in range(x0, x1):
        t = (x1 - 1 - xx) / max(1, L - 1)      # 0 at the book, 1 at the free end
        for j in range(h):
            u = j / max(1, h - 1)
            cj = (h - 1) / 2
            off = round(3 * (1 - abs(j - cj) / cj))   # shallow V cut at the free end
            if xx < x0 + off: continue
            fold = math.sin((u * 2.0 + t * 0.9) * math.pi) * 0.9 + math.sin((u * 0.7 - t * 0.6) * math.pi) * 0.5
            v = 3 + fold - u * 0.9 - (1 - t) * 1.4
            if j == 0: v = min(v, 1.5)
            if j == h - 1: v = 0.6
            col = tones[max(0, min(5, int(round(v))))]
            if xx == x0 + off: col = shade(c, .7)
            elif xx == x0 + off + 1 and 0 < j < h - 1: col = tones[5]
            elif xx == x0 + off + 2 and 0 < j < h - 1: col = tones[1]
            px(img, xx, y + j, col)
    text(img, x0 + 6 + (L - 8) // 2, y + (h - 8) // 2, label, NAME if active else OCHRE, center=True)


def people_band():
    base = people()
    img = backdrop(); img.alpha_composite(base.crop((0, 0, W, H - 8)), (0, 8))
    bw, bh = 176, 200
    bx, by = 58, 34
    clean = backdrop()
    img.paste(clean.crop((0, 0, bx - 1, H)), (0, 0))           # wipe old filter tiles
    img.paste(clean.crop((bx - 3, 0, bx + bw + 3, by - 4)), (bx - 3, 0))
    # top ribbons
    x = bx + 18
    for k, (lab, c) in enumerate((('Personen', TERRA_A), ('Titel', hx('#866C36')), ('Import', PENNANT))):
        w = tw(lab) + 10; L = 22 if k == 0 else 17
        ribbon_band(img, x, by - L, by + 6, w, c if k == 0 else shade(c, .85), 'top', lab, k == 0)
        rect(img, x, by + 6, x + w, by + 7, shade(c, .45))
        x += w + 4
    # left side ribbons (filters)
    for k, (lab, c) in enumerate((('Alle', TERRA_A), ('Online', hx('#5E9A3E')), ('Fremd', PENNANT), ('Konflikt', hx('#B24147')))):
        w = tw(lab) + 15 + (5 if k == 0 else 0)
        yy = by + 16 + k * 20
        ribbon_h(img, bx - w, bx + 6, yy, 15, c if k == 0 else shade(c, .85), lab, k == 0)
        rect(img, bx + 6, yy, bx + 7, yy + 15, shade(c, .45))
    return img


people_band().resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUTD, 'personen_band.png'))
print('ok')
