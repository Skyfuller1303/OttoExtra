"""Toast design drafts (top of screen) in the zone-banner design language."""
import os, sys, math
from PIL import Image, ImageDraw

S = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(S, 'hudgui', 'toasts'); os.makedirs(OUT, exist_ok=True)
BANNERS = sys.argv[1]
KEL = os.path.join(BANNERS, '7064e3e4-abf0-4a37-a951-00ec8798c3e5.png')

def hx(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)
OUTL = hx('#141418'); D1 = hx('#1E1E24'); D2 = hx('#2E2D35'); D3 = hx('#3B3A45'); D4 = hx('#514F59')
G0 = hx('#47391D'); G1 = hx('#866C36'); G2 = hx('#B58D36'); G3 = hx('#DBB451')
NAME = hx('#F0EBE0'); OCK = hx('#D6C194'); SUB = hx('#A9A6B0')
# Type accents (pack ramps)
ACC = {
    'aktion': (hx('#96752C'), hx('#CD9F3D'), hx('#DBB451')),   # gold: vote, call to action
    'system': (hx('#214E66'), hx('#349DBA'), hx('#5FB8CF')),   # turquoise: resourcepack, data
    'warnung': (hx('#6E2226'), hx('#B24147'), hx('#C4545A')),  # red: offline, error
    'tier': (hx('#815639'), hx('#A2754D'), hx('#B6875D')),     # brown: animals
}
BTN = hx('#222E3C'); BTN_HI = hx('#344459'); BTN_LO = hx('#1A2024')

FONT = Image.open(os.path.join(S, 'mcfont', 'ascii.png')).convert('RGBA')
UML = {'ä': 'a', 'ö': 'o', 'ü': 'u', 'Ö': 'O', 'Ä': 'A', 'Ü': 'U'}
def glyph(ch):
    o = ord(UML.get(ch, ch)); o = o if o < 256 else ord('?')
    return FONT.crop(((o % 16) * 8, (o // 16) * 8, (o % 16) * 8 + 8, (o // 16) * 8 + 8))
def cw(ch):
    if ch == ' ': return 4
    bb = glyph(ch).split()[3].getbbox(); return (bb[2] if bb else 0) + 1
def tw(s, k=1): return sum(cw(c) for c in s) * k
def text(img, x, y, s, col, k=1, center=False, shadow=True):
    w = tw(s) + 1; im = Image.new('RGBA', (w, 9), (0, 0, 0, 0))
    def blit(ox, oy, c):
        xx = ox
        for ch in s:
            if ch != ' ':
                g = glyph(ch); a = g.split()[3]
                im.paste(Image.new('RGBA', g.size, c), (xx, oy), a)
                if ch in UML:
                    bb = a.getbbox(); im.putpixel((xx + bb[0], oy + (1 if ch.islower() else 0)), c); im.putpixel((xx + bb[2] - 1, oy + (1 if ch.islower() else 0)), c)
            xx += cw(ch)
    if shadow: blit(1, 1, (col[0] // 4, col[1] // 4, col[2] // 4, 255))
    blit(0, 0, col)
    if k != 1: im = im.resize((im.size[0] * k, im.size[1] * k), Image.NEAREST)
    if center: x -= im.size[0] // 2
    img.alpha_composite(im, (x, y)); return im.size[0]

def px(img, x, y, c):
    if 0 <= x < img.size[0] and 0 <= y < img.size[1]: img.alpha_composite(Image.new('RGBA', (1, 1), c), (x, y))
def rect(img, x0, y0, x1, y1, c):
    if x1 > x0 and y1 > y0: img.alpha_composite(Image.new('RGBA', (x1 - x0, y1 - y0), c), (x0, y0))

def plate(img, x, y, w, h, fill=D1, alpha=235):
    f = (fill[0], fill[1], fill[2], alpha)
    for yy in range(h):
        for xx in range(w):
            if xx in (0, w - 1) and yy in (0, h - 1): continue
            d = min(xx, yy, w - 1 - xx, h - 1 - yy); ic = xx in (1, w - 2) and yy in (1, h - 2)
            if d == 0 or ic: c = OUTL
            elif d == 1:
                tl = yy == 1 or xx == 1; br = yy == h - 2 or xx == w - 2
                c = D4 if tl and not br else D2 if br and not tl else D3
            else: c = f
            px(img, x + xx, y + yy, c)

def disc(img, cx, cy, r, ramp):
    dark, base, light = ramp
    for yy in range(-r, r + 1):
        for xx in range(-r, r + 1):
            d = math.hypot(xx + .5, yy + .5)
            if d > r + .2: continue
            if d > r - 1: c = OUTL
            elif xx + yy < -r * .6: c = light
            elif xx + yy > r * .7: c = dark
            else: c = base
            px(img, cx + xx, cy + yy, c)

ICONS = {
    'urne': ["..XXXXX..", "..X...X..", "XXXXXXXXX", "X.......X", "X..XXX..X", "X.......X", "X.......X", "XXXXXXXXX"],
    'pinsel': ["......XX.", ".....XXX.", "....XXX..", "...XXX...", "..XX.....", ".XXX.....", "XXX......", "XX......."],
    'warn': ["....X....", "...XXX...", "...X.X...", "..XX.XX..", "..X...X..", ".X..X..X.", ".X.....X.", "XXXXXXXXX"],
    'huf': [".XXXXXXX.", "XX.....XX", "X.......X", "X.......X", "X.......X", "XX.....XX", ".XX...XX.", ".XX...XX."],
    'karte': ["XXX.XXX.X", "X.XXX.XXX", "X.X.X.X.X", "X.X.X.X.X", "X.X.X.X.X", "XXX.XXX.X", "..XXX.XXX", "........."],
}
def icon(img, name, x, y, col):
    for r, line in enumerate(ICONS[name]):
        for c, ch in enumerate(line):
            if ch == 'X': px(img, x + c, y + r, col)

def keycap(img, x, y, key, label):
    w = tw(key) + 6
    rect(img, x + 1, y, x + w - 1, y + 11, OUTL); rect(img, x, y + 1, x + w, y + 10, OUTL)
    rect(img, x + 1, y + 1, x + w - 1, y + 10, BTN); rect(img, x + 1, y + 1, x + w - 1, y + 2, BTN_HI); rect(img, x + 1, y + 9, x + w - 1, y + 10, BTN_LO)
    text(img, x + 3, y + 2, key, NAME)
    return w + 3 + text(img, x + w + 3, y + 2, label, SUB, shadow=True)

def world(w, h):
    bg = Image.new('RGBA', (w, h)); d = ImageDraw.Draw(bg)
    for y in range(h):
        t = y / h
        c = (int(140 + 40 * t), int(178 + 30 * t), int(222 + 20 * t), 255) if t < .6 else (88, 118, 58, 255)
        d.line([(0, y), (w, y)], fill=c)
    for i in range(0, w, 16):
        hh = 14 + (i * 37 % 19); d.rectangle([i, int(h * .6) - hh, i + 15, int(h * .6)], fill=(74, 92, 58, 255))
    return bg

W = 300
# ---------- T1 Siegel: plate, wax-seal disc with icon, time bar ----------
def siegel(kind, ic, title, desc, key=None, progress=0.6, banner=None):
    h = 34 + (12 if key else 0)
    img = world(W, h + 12); tw_ = max(tw(title), tw(desc), (tw(key[0]) + 9 + tw(key[1])) if key else 0)
    w = 8 + 24 + 8 + tw_ + 12; x = (W - w) // 2; y = 6
    plate(img, x, y, w, h)
    ramp = ACC[kind]
    if banner:
        img.alpha_composite(Image.open(banner).convert('RGBA').resize((24, 24), Image.NEAREST), (x + 7, y + 5))
    else:
        disc(img, x + 19, y + 17 if not key else y + 18, 11, ramp)
        icon(img, ic, x + 15, y + 13 if not key else y + 14, (ramp[0][0] // 2, ramp[0][1] // 2, ramp[0][2] // 2, 255))
    text(img, x + 40, y + 6, title, OCK)
    text(img, x + 40, y + 17, desc, NAME)
    if key: keycap(img, x + 40, y + 29, *key)
    # remaining-time bar inside bottom bevel
    bw = w - 6; filled = int(bw * progress)
    rect(img, x + 3, y + h - 4, x + 3 + bw, y + h - 3, OUTL)
    rect(img, x + 3, y + h - 4, x + 3 + filled, y + h - 3, ramp[1])
    return img

# ---------- T2 Wimpel: compact single-line pill with gold diamonds ----------
def wimpel(kind, ic, msg):
    img = world(W, 30); w = 10 + 9 + 5 + tw(msg) + 12; x = (W - w) // 2; y = 6; h = 15
    plate(img, x, y, w, h)
    ramp = ACC[kind]
    icon(img, ic, x + 8, y + 4 - 1, ramp[2])
    text(img, x + 22, y + 4, msg, NAME)
    for sx in (x - 4, x + w + 1):  # gold diamonds at both ends
        px(img, sx + 1, y + 6, G3); px(img, sx, y + 7, G2); px(img, sx + 1, y + 7, G2); px(img, sx + 2, y + 7, G2); px(img, sx + 1, y + 8, G0)
    rect(img, x + 2, y + h - 2, x + w - 2, y + h - 1, ramp[1])  # thin accent underline (type color)
    return img

# ---------- T3 Herold: accent edge left, title row with small kind label, action chip right ----------
def herold(kind, ic, label, title, desc, key=None):
    img = world(W, 50); x = 40; w = W - 80; y = 6; h = 32
    plate(img, x, y, w, h)
    ramp = ACC[kind]
    rect(img, x + 2, y + 2, x + 4, y + h - 2, ramp[1]); rect(img, x + 2, y + 2, x + 4, y + 3, ramp[2])
    icon(img, ic, x + 9, y + 6, ramp[2])
    text(img, x + 22, y + 6, label.upper(), (ramp[2][0], ramp[2][1], ramp[2][2], 255))
    text(img, x + 9, y + 18, title, NAME)
    text(img, x + 11 + tw(title) + 3, y + 18, '- ' + desc, SUB) if desc else None
    if key:
        kw = tw(key[0]) + 6 + 3 + tw(key[1])
        keycap(img, x + w - 6 - kw, y + 5, *key)
    return img

rows = [
    ('T1  Siegel (Kachel mit Siegel, Restzeit-Balken)', [
        siegel('aktion', 'urne', 'Das Siegel wartet', 'Öffne die vier Wahllokale.', key=('V', 'Abstimmen'), progress=.7),
        siegel('system', 'pinsel', 'Ottonien-Look aktualisiert', 'Resourcepack 1.1.90 installiert.', progress=.35),
        siegel('warnung', 'warn', 'Offline', 'Nutze gecachten Resourcepack.', progress=.85),
    ]),
    ('T2  Wimpel (kompakt, eine Zeile)', [
        wimpel('system', 'pinsel', 'Server-Resourcepack ist aktuell'),
        wimpel('system', 'karte', 'Kartendaten neu geladen: 85 Lehen'),
        wimpel('warnung', 'warn', 'Offline - nutze gecachten Resourcepack'),
        wimpel('tier', 'huf', 'Pferd "Falbe" gespeichert'),
    ]),
    ('T3  Herold (Typ-Kante, Aktion rechts)', [
        herold('aktion', 'urne', 'Abstimmung', 'Ottonien zählt auf dich', 'vier Seiten', key=('V', 'Öffnen')),
        herold('system', 'pinsel', 'Resourcepack', 'Ottonien-Look aktualisiert', '1.1.90'),
        herold('tier', 'huf', 'Tiere', 'Pferd gespeichert', 'Hof Kelterfels'),
    ]),
]
K = 3; pad = 12
cols = 2
cells = []
for title, imgs in rows:
    cells.append(('title', title)); cells += [('img', im) for im in imgs]
# layout: each row block: title line, then images in 2 columns
blocks = []
for title, imgs in rows:
    rws = (len(imgs) + cols - 1) // cols
    hh = max(im.size[1] for im in imgs)
    blocks.append((title, imgs, rws, hh))
sheet_w = cols * W * K + (cols + 1) * pad
sheet_h = sum(30 + rws * (hh * K + pad) for _, _, rws, hh in blocks) + pad
sheet = Image.new('RGBA', (sheet_w, sheet_h), hx('#101014'))
yy = pad
for title, imgs, rws, hh in blocks:
    t = Image.new('RGBA', (tw(title) + 2, 9), (0, 0, 0, 0)); text(t, 0, 0, title, OCK)
    sheet.alpha_composite(t.resize((t.size[0] * 2, 18), Image.NEAREST), (pad, yy + 4)); yy += 30
    for i, im in enumerate(imgs):
        cx = pad + (i % cols) * (W * K + pad); cy = yy + (i // cols) * (hh * K + pad)
        sheet.alpha_composite(im.resize((im.size[0] * K, im.size[1] * K), Image.NEAREST), (cx, cy))
    yy += rws * (hh * K + pad)
sheet.save(os.path.join(OUT, 'toast_entwuerfe.png'))
print('ok', sheet.size)
