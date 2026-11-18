"""Letter editor redesign drafts (pixel art), three versions at GUI resolution.
Usage: python -I gen_briefui.py <out_dir>"""
import os, sys, math, random
OUTD = sys.argv[1]
os.makedirs(OUTD, exist_ok=True)
_s = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_toasts.py'), encoding='utf-8').read()
sys.argv = [sys.argv[0], OUTD]
exec(_s[:_s.index('ICONS = {')])
exec(_s[_s.index('def world('):_s.index('W = 300')])
from PIL import Image

W, H = 400, 240
K = 3
INK = hx('#503D29'); INK_L = hx('#8A6E4E')
PARCH = [hx('#8C6E4C'), hx('#B8926E'), hx('#C9AA81'), hx('#D4B78C'), hx('#E0C9A2')]
WOOD = [hx('#2E2216'), hx('#45321C'), hx('#5E4C38'), hx('#6D5840'), hx('#80674C'), hx('#977C5E')]
LEATHER = [hx('#2A1A12'), hx('#3D2A1E'), hx('#5E3F2A'), hx('#815639'), hx('#A2754D')]
SLATE = hx('#222E3C'); SLATE_HI = hx('#344459'); SLATE_LO = hx('#141C24'); BLACK = hx('#0E0E10')
HOVER = hx('#E2A650'); TERRA = [hx('#6B3A22'), hx('#8C4E2F'), hx('#AA6841'), hx('#C4855A')]
WAX = [hx('#3A1416'), hx('#6E2226'), hx('#B24147'), hx('#C4545A'), hx('#E08A8E')]
MC = ['000000', '0000AA', '00AA00', '00AAAA', 'AA0000', 'AA00AA', 'FFAA00', 'AAAAAA',
      '555555', '5555FF', '55FF55', '55FFFF', 'FF5555', 'FF55FF', 'FFFF55', 'FFFFFF']
MC = [hx(c) for c in MC]
RED_INK = hx('#8E2B22')

LINES = [("Werter Freund,", None), ("", None), ("ich schreibe Euch aus", None), ("Kelterfels, wo die Ernte", 'kel'),
         ("reich ausfiel. Die Wege", None), ("nach Niederpolt sind", None), ("wieder frei.", None), ("", None),
         ("Hochachtungsvoll,", 'it'), ("Mattis von Kelterfels", 'b')]


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), 255)


def backdrop():
    """Dimmed in-game world behind the screen."""
    img = world(W, H)
    rect(img, 0, 0, W, H, (10, 10, 14, 150))
    return img


def write_lines(img, x, y, lh, rule_w=None, rule=None, caret_line=6, n=None):
    for i, (s, fmt) in enumerate(LINES[:n]):
        yy = y + i * lh
        if rule is not None and rule_w:
            rect(img, x - 2, yy + 9, x - 2 + rule_w, yy + 10, rule)
        if fmt == 'kel':
            w = text(img, x, yy, 'Kelterfels', RED_INK, shadow=False)
            text(img, x + w + 3, yy, s[len('Kelterfels'):].lstrip(), INK, shadow=False)
        elif fmt == 'b':
            text(img, x, yy, s, INK, shadow=False); text(img, x + 1, yy, s, INK, shadow=False)
        else:
            text(img, x, yy, s, INK_L if fmt == 'it' else INK, shadow=False)
        if i == caret_line:
            cx = x + tw(s) + 1
            rect(img, cx, yy - 1, cx + 1, yy + 9, INK)


def tri(img, x, y, d, c):
    """Small 5px arrow; d = -1 left, +1 right."""
    for i in range(3):
        for j in range(-i, i + 1):
            px(img, x + (2 - i if d > 0 else i), y + 2 + j, c)


def scissors(img, x, y, c):
    for p in ((0, 0), (1, 1), (2, 2), (4, 2), (5, 1), (6, 0), (3, 3), (2, 4), (4, 4), (1, 5), (5, 5), (0, 6), (1, 6), (5, 6), (6, 6)):
        px(img, x + p[0], y + p[1], c)


def quill(img, x, y, c, c2):
    for i in range(8):
        px(img, x + i, y + 7 - i, c)
        if 2 <= i <= 6: px(img, x + i + 1, y + 7 - i, c2); px(img, x + i, y + 6 - i, c2)
    px(img, x, y + 8, c)


def wrapped(maxw, n=None):
    """Book preview lines word-wrapped to maxw pixels (formatting kept per source line)."""
    out = []
    for s, f in LINES:
        if not s: out.append(('', None)); continue
        cur = ''
        for wd in s.split(' '):
            t = (cur + ' ' + wd).strip()
            if tw(t) > maxw and cur:
                out.append((cur, f)); cur = wd
            else:
                cur = t
        out.append((cur, f))
    return out[:n]


def preview(img, x, y, maxw, lh, n):
    for i, (s, f) in enumerate(wrapped(maxw, n)):
        col = RED_INK if f == 'kel' and s.startswith('Kelterfels') else INK
        if f == 'kel' and s.startswith('Kelterfels'):
            w = text(img, x, y + i * lh, 'Kelterfels', RED_INK, shadow=False)
            text(img, x + w + 3, y + i * lh, s[len('Kelterfels'):].lstrip(), INK, shadow=False)
        else:
            text(img, x, y + i * lh, s, INK_L if f == 'it' else INK, shadow=False)


# ---------- generic widgets ----------
def wood_panel(img, x, y, w, h):
    rect(img, x, y, x + w, y + h, WOOD[0])
    random.seed(3)
    for yy in range(y + 1, y + h - 1):
        plank = (yy - y) // 12
        base = WOOD[2 + plank % 2]
        rect(img, x + 1, yy, x + w - 1, yy + 1, base)
        if (yy - y) % 12 == 0: rect(img, x + 1, yy, x + w - 1, yy + 1, WOOD[1])
    for i in range(70):
        gx = random.randint(x + 2, x + w - 12); gy = random.randint(y + 2, y + h - 3)
        if (gy - y) % 12: rect(img, gx, gy, gx + random.randint(3, 10), gy + 1, WOOD[1 if i % 3 else 4])
    rect(img, x + 1, y + 1, x + w - 1, y + 2, WOOD[5]); rect(img, x + 1, y + 1, x + 2, y + h - 1, WOOD[4])
    rect(img, x + 1, y + h - 2, x + w - 1, y + h - 1, WOOD[1])


def parchment(img, x, y, w, h, torn=True, seed=1):
    random.seed(seed)
    rect(img, x, y, x + w, y + h, PARCH[2])
    for i in range(60):
        gx = random.randint(x + 2, x + w - 6); gy = random.randint(y + 2, y + h - 3)
        rect(img, gx, gy, gx + random.randint(2, 5), gy + 1, PARCH[1] if i % 2 else PARCH[3])
    rect(img, x, y, x + w, y + 1, PARCH[3]); rect(img, x, y, x + 1, y + h, PARCH[3])
    rect(img, x, y + h - 1, x + w, y + h, PARCH[1]); rect(img, x + w - 1, y, x + w, y + h, PARCH[1])
    if torn:
        for xx in range(x, x + w):
            if random.random() < .3: px(img, xx, y, (0, 0, 0, 0)) if False else px(img, xx, y, PARCH[1])
            if random.random() < .3: px(img, xx, y + h - 1, PARCH[0])
        for yy in range(y, y + h, 3):
            if random.random() < .4: px(img, x + w - 1, yy, PARCH[0])


def plaque(img, x, y, w, h, label=None, hover=False, icon=None):
    """Wooden button with gold rim."""
    rect(img, x + 1, y, x + w - 1, y + h, WOOD[0]); rect(img, x, y + 1, x + w, y + h - 1, WOOD[0])
    rect(img, x + 1, y + 1, x + w - 1, y + h - 1, G2 if hover else G1)
    rect(img, x + 2, y + 2, x + w - 2, y + h - 2, WOOD[3])
    rect(img, x + 2, y + 2, x + w - 2, y + 3, WOOD[4]); rect(img, x + 2, y + h - 3, x + w - 2, y + h - 2, WOOD[2])
    if label:
        text(img, x + w // 2, y + (h - 8) // 2, label, NAME if not hover else G3, center=True)
    if icon: icon(img, x, y, w, h)


def slate(img, x, y, w, h, label=None, hover=False, icon=None, col=None):
    """Reformed control: slate blue, black outline, gold outline on hover."""
    o = HOVER if hover else BLACK
    rect(img, x + 1, y, x + w - 1, y + h, o); rect(img, x, y + 1, x + w, y + h - 1, o)
    rect(img, x + 1, y + 1, x + w - 1, y + h - 1, SLATE)
    rect(img, x + 1, y + 1, x + w - 1, y + 2, SLATE_HI); rect(img, x + 1, y + h - 2, x + w - 1, y + h - 1, SLATE_LO)
    if label: text(img, x + w // 2, y + (h - 8) // 2, label, col or NAME, center=True)
    if icon: icon(img, x, y, w, h)


def ink_pot(img, x, y, c):
    rect(img, x + 2, y, x + 7, y + 1, BLACK)
    rect(img, x + 1, y + 1, x + 8, y + 2, WOOD[1])
    rect(img, x, y + 2, x + 9, y + 9, BLACK); rect(img, x + 1, y + 3, x + 8, y + 8, shade(c, 1.0) if c != MC[0] else hx('#2A2A2E'))
    px(img, x + 2, y + 4, (255, 255, 255, 120)); px(img, x + 2, y + 5, (255, 255, 255, 80))


def wax_seal(img, cx, cy, r):
    disc(img, cx, cy, r, (WAX[1], WAX[2], WAX[3]))
    for a in range(0, 360, 45):
        px(img, cx + round(math.cos(math.radians(a)) * (r - 2)), cy + round(math.sin(math.radians(a)) * (r - 2)), WAX[1])
    quill(img, cx - 4, cy - 4, WAX[4], WAX[3])


# ---------- V1 Schreibpult ----------
def v1():
    img = backdrop()
    px0, py0, pw, ph = 40, 12, 320, 216
    wood_panel(img, px0, py0, pw, ph)
    # parchment sheet with drop shadow
    sx, sy, sw, sh = px0 + 46, py0 + 10, 150, 172
    rect(img, sx + 3, sy + 3, sx + sw + 3, sy + sh + 3, (0, 0, 0, 90))
    parchment(img, sx, sy, sw, sh, seed=2)
    text(img, sx + 10, sy + 8, 'Seite 1 / 2', INK_L, shadow=False)
    text(img, sx + sw - 10 - tw('Buch: 2 S.'), sy + 8, 'Buch: 2 S.', INK_L, shadow=False)
    rect(img, sx + 8, sy + 19, sx + sw - 8, sy + 20, PARCH[1])
    write_lines(img, sx + 12, sy + 26, 12, sw - 20, (139, 110, 78, 110))
    # page break marker (scissors) between pages
    for xx in range(sx + 8, sx + sw - 8, 4): rect(img, xx, sy + sh - 18, xx + 2, sy + sh - 17, INK_L)
    scissors(img, sx + sw // 2 - 3, sy + sh - 21, INK)
    # ink pots (colours) on the left
    text(img, px0 + 8, py0 + 10, 'Tinte', G3)
    for i, c in enumerate(MC):
        ink_pot(img, px0 + 8 + (i % 2) * 14, py0 + 22 + (i // 2) * 13, c)
    for i, s in enumerate(('B', 'I', 'U', 'S')):
        plaque(img, px0 + 7 + (i % 2) * 15, py0 + 130 + (i // 2) * 15, 13, 13, s)
    plaque(img, px0 + 7, py0 + 160, 28, 13, 'Tx')
    # live preview book page on the right
    bx, by, bw, bh = sx + sw + 12, py0 + 10, 102, 130
    rect(img, bx + 2, by + 2, bx + bw + 2, by + bh + 2, (0, 0, 0, 90))
    parchment(img, bx, by, bw, bh, torn=False, seed=5)
    text(img, bx + bw // 2, by + 5, 'Vorschau', INK, center=True, shadow=False)
    preview(img, bx + 6, by + 18, bw - 12, 9, 11)
    text(img, bx + bw // 2, by + bh - 11, '1 / 2', INK_L, center=True, shadow=False)
    # side actions
    plaque(img, bx, by + bh + 6, 49, 16, 'Entwurf')
    plaque(img, bx + 53, by + bh + 6, 49, 16, 'Signatur')
    # bottom bar
    yb = py0 + ph - 26
    plaque(img, sx, yb, 18, 18, icon=lambda i, x, y, w, h: tri(i, x + 6, y + 6, -1, NAME))
    plaque(img, sx + 21, yb, 18, 18, icon=lambda i, x, y, w, h: tri(i, x + 8, y + 6, 1, NAME))
    plaque(img, sx + 42, yb, 18, 18, '+')
    plaque(img, sx + 63, yb, 18, 18, icon=lambda i, x, y, w, h: scissors(i, x + 6, y + 6, NAME))
    plaque(img, sx + 84, yb, 40, 18, 'Prufen')
    # wax seal "Schreiben"
    wax_seal(img, bx + 18, yb + 6, 13)
    text(img, bx + 36, yb + 2, 'Schreiben', G3)
    text(img, bx + 36, yb + 12, '& versiegeln', OCK)
    return img


# ---------- V2 Offenes Buch ----------
def v2():
    img = backdrop()
    bx, by, bw, bh = 52, 30, 296, 176
    # leather cover
    rect(img, bx - 2, by - 2, bx + bw + 2, by + bh + 2, LEATHER[0])
    rect(img, bx, by, bx + bw, by + bh, LEATHER[2])
    rect(img, bx, by, bx + bw, by + 1, LEATHER[3]); rect(img, bx, by + bh - 1, bx + bw, by + bh, LEATHER[1])
    for cx, cy in ((bx + 3, by + 3), (bx + bw - 6, by + 3), (bx + 3, by + bh - 6), (bx + bw - 6, by + bh - 6)):
        rect(img, cx, cy, cx + 3, cy + 3, G2); px(img, cx, cy, G3)
    # pages
    lx, ly, lw, lh = bx + 8, by + 6, 136, bh - 12
    rx = bx + bw // 2 + 4
    parchment(img, lx, ly, lw, lh, torn=False, seed=7)
    parchment(img, rx, ly, lw, lh, torn=False, seed=8)
    for i in range(6):  # gutter shading
        rect(img, lx + lw - 6 + i, ly, lx + lw - 5 + i, ly + lh, shade(PARCH[2], .80 + i * .03))
        rect(img, rx + i, ly, rx + i + 1, ly + lh, shade(PARCH[2], .95 - (5 - i) * .03))
    rect(img, lx + lw, ly - 2, rx, ly + lh + 2, LEATHER[1])
    text(img, lx + 10, ly + 6, 'Brief', INK, shadow=False)
    text(img, lx + lw - 12 - tw('Arbeitsseite 1/2'), ly + 6, 'Arbeitsseite 1/2', INK_L, shadow=False)
    write_lines(img, lx + 10, ly + 20, 12, lw - 22, (139, 110, 78, 110))
    text(img, rx + 12, ly + 6, 'Wie im Buch', INK, shadow=False)
    text(img, rx + lw - 10 - tw('1 / 2'), ly + 6, '1 / 2', INK_L, shadow=False)
    preview(img, rx + 12, ly + 20, lw - 22, 10, 14)
    # bookmark ribbons on top (tabs)
    for i, (lab, c) in enumerate((('Brief', WAX[2]), ('Entwurfe', hx('#349DBA')), ('Archiv', hx('#5E9A3E')))):
        x = bx + 14 + i * 46
        h = 18 if i == 0 else 14
        rect(img, x, by - h, x + 40, by + 2, shade(c, .7)); rect(img, x + 1, by - h + 1, x + 39, by + 2, c)
        text(img, x + 20, by - h + 4, lab, NAME, center=True)
    # colour dots on the leather (bottom strip)
    y = by + bh + 8
    for i, c in enumerate(MC):
        cx = bx + 6 + i * 11
        disc(img, cx + 4, y + 4, 4, (shade(c, .6), c, shade(c, 1.25) if c != MC[15] else c))
    for i, s in enumerate(('B', 'I', 'U', 'S', 'Tx')):
        x = bx + 182 + i * 15
        rect(img, x, y, x + 14, y + 10, LEATHER[1]); rect(img, x + 1, y + 1, x + 13, y + 9, LEATHER[3])
        text(img, x + 7, y + 1, s, G3, center=True)
    # action tabs on the right edge of the book
    acts = [('<', None), ('>', None), ('+', None), ('cut', None), ('Sig', None)]
    for i, (a, _) in enumerate(acts):
        x, yy = bx + bw + 2, by + 14 + i * 20
        rect(img, x, yy, x + 18, yy + 16, LEATHER[0]); rect(img, x, yy + 1, x + 17, yy + 15, LEATHER[3])
        if a == '<': tri(img, x + 6, yy + 5, -1, G3)
        elif a == '>': tri(img, x + 7, yy + 5, 1, G3)
        elif a == 'cut': scissors(img, x + 5, yy + 4, G3)
        elif a == 'Sig': quill(img, x + 4, yy + 3, G3, G2)
        else: text(img, x + 9, yy + 4, a, G3, center=True)
    # write button: big ribbon at bottom right
    x, yy = bx + bw - 26, by + bh + 4
    rect(img, x, yy, x + 70, yy + 18, WAX[0]); rect(img, x + 1, yy + 1, x + 69, yy + 17, WAX[2]); rect(img, x + 1, yy + 1, x + 69, yy + 2, WAX[3])
    text(img, x + 35, yy + 5, 'Schreiben', NAME, center=True)
    return img


# ---------- V3 Reformed (pack GUI rules) ----------
def cord(img, x, y, w, h):
    """Braided dark-brown cord frame, 3 px."""
    for i in range(w):
        for j in range(h):
            e = min(i, j, w - 1 - i, h - 1 - j)
            if e >= 3: continue
            c = (LEATHER[1], LEATHER[2], LEATHER[0])[e] if (i + j) % 4 < 2 else (LEATHER[0], LEATHER[3], LEATHER[1])[e]
            px(img, x + i, y + j, c)


def quatrefoil(img, x, y):
    rect(img, x, y, x + 10, y + 10, TERRA[2])
    for p in ((4, 1), (5, 1), (1, 4), (1, 5), (8, 4), (8, 5), (4, 8), (5, 8), (4, 4), (5, 5), (4, 5), (5, 4)):
        px(img, x + p[0], y + p[1], TERRA[3] if p[1] < 5 else TERRA[1])
    rect(img, x, y + 9, x + 10, y + 10, TERRA[1]); rect(img, x + 9, y, x + 10, y + 10, TERRA[1])


def tile(img, x, y, s=16):
    rect(img, x, y, x + s, y + s, PARCH[0])
    rect(img, x + 1, y + 1, x + s - 1, y + s - 1, PARCH[2])
    rect(img, x + 1, y + 1, x + s - 1, y + 2, PARCH[3]); rect(img, x + 1, y + 1, x + 2, y + s - 1, PARCH[3])
    rect(img, x + 1, y + s - 2, x + s - 1, y + s - 1, PARCH[1]); rect(img, x + s - 2, y + 1, x + s - 1, y + s - 1, PARCH[1])


def v3():
    img = backdrop()
    x0, y0, w0, h0 = 44, 14, 312, 212
    cord(img, x0, y0, w0, h0)
    rect(img, x0 + 3, y0 + 3, x0 + w0 - 3, y0 + h0 - 3, PARCH[2])
    # terracotta header with quatrefoil tiles
    rect(img, x0 + 3, y0 + 3, x0 + w0 - 3, y0 + 17, TERRA[2])
    for i in range(x0 + 4, x0 + w0 - 14, 11): quatrefoil(img, i, y0 + 5)
    tw_ = tw('Brief & Verkundung') + 10
    rect(img, x0 + w0 // 2 - tw_ // 2, y0 + 4, x0 + w0 // 2 + tw_ // 2, y0 + 16, TERRA[1])
    text(img, x0 + w0 // 2, y0 + 6, 'Brief & Verkundung', NAME, center=True)
    rect(img, x0 + 3, y0 + 17, x0 + w0 - 3, y0 + 18, LEATHER[1])
    # writing area (lighter inset)
    ax, ay, aw, ah = x0 + 10, y0 + 24, 160, 152
    rect(img, ax, ay, ax + aw, ay + ah, PARCH[1]); rect(img, ax + 1, ay + 1, ax + aw, ay + ah, PARCH[3])
    text(img, ax + 6, ay + 4, 'Seite 1 / 2', INK_L, shadow=False)
    write_lines(img, ax + 8, ay + 18, 12, aw - 14, (139, 110, 78, 90))
    # preview column
    vx = ax + aw + 8
    text(img, vx, ay, 'Buchvorschau', INK, shadow=False)
    rect(img, vx, ay + 10, vx + 70, ay + 100, PARCH[1]); rect(img, vx + 1, ay + 11, vx + 69, ay + 99, PARCH[4])
    preview(img, vx + 4, ay + 14, 62, 9, 9)
    # formatting tiles (GUI tiles, colour swatches inside)
    fx = vx + 78
    text(img, fx, ay, 'Tinte', INK, shadow=False)
    for i, c in enumerate(MC):
        tx, ty = fx + (i % 4) * 13, ay + 10 + (i // 4) * 13
        tile(img, tx, ty, 12); rect(img, tx + 3, ty + 3, tx + 9, ty + 9, c); rect(img, tx + 3, ty + 3, tx + 9, ty + 4, shade(c, 1.2))
    for i, s in enumerate(('B', 'I', 'U', 'S')):
        tx, ty = fx + i * 13, ay + 66
        tile(img, tx, ty, 12); text(img, tx + 6, ty + 2, s, INK, center=True, shadow=False)
    # action tiles with pictograms
    ty = ay + 108
    text(img, vx, ty - 2, 'Seite', INK, shadow=False)
    for i, fn in enumerate((lambda x, y: tri(img, x + 5, y + 5, -1, INK), lambda x, y: tri(img, x + 6, y + 5, 1, INK),
                            lambda x, y: text(img, x + 8, y + 4, '+', INK, center=True, shadow=False),
                            lambda x, y: scissors(img, x + 4, y + 4, INK), lambda x, y: quill(img, x + 3, y + 3, INK, INK_L))):
        x = vx + i * 19
        tile(img, x, ty + 8); fn(x, ty + 8)
    # bottom slate buttons
    yb = y0 + h0 - 28
    slate(img, x0 + 10, yb, 18, 18, icon=lambda i, x, y, w, h: rect(i, x + 4, y + 8, x + 14, y + 10, NAME) or rect(i, x + 8, y + 4, x + 10, y + 14, NAME))
    slate(img, x0 + 32, yb, 62, 18, 'Entwurfe')
    slate(img, x0 + 98, yb, 50, 18, 'Signatur')
    slate(img, x0 + 152, yb, 50, 18, 'Prufen')
    slate(img, x0 + w0 - 86, yb, 76, 18, 'Schreiben', hover=True, col=G3)
    return img


VARIANTS = [('brief_v1_schreibpult', v1), ('brief_v2_buch', v2), ('brief_v3_reformed', v3)]
for name, fn in VARIANTS:
    im = fn()
    im.resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUTD, name + '.png'))
print('ok')
