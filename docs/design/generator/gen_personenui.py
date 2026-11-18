"""People book redesign drafts in the leather/parchment book language (three versions).
Usage: python -I gen_personenui.py <out_dir>"""
import os, sys, random
OUTD = sys.argv[1]
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_briefui.py'), encoding='utf-8').read()
exec(_b[:_b.index('VARIANTS = [')])
from PIL import Image

GREEN = hx('#5E9A3E'); GREEN_L = hx('#86C25A'); TEAL = hx('#349DBA'); GOLD_T = hx('#866C36')
STATE_COL = {'Bekannt': GREEN, 'Manuell': GOLD_T, 'API': TEAL, 'Gesehen': hx('#8A7A66'), 'Gehort': hx('#8A7A66')}
PEOPLE = [
    ('Ritter Ansgar', 'Ansgar_K', 'Bekannt', True, False, 'Ritter'),
    ('Grafin Adelheid', 'Adel_h', 'API', False, False, 'Grafin'),
    ('Unbekannt', 'xBlockx', 'Gesehen', True, False, ''),
    ('Bruder Konrad', 'konradx', 'Manuell', False, False, 'Bruder'),
    ('Hauptmann Wido', 'Wido_W', 'Bekannt', True, True, 'Hauptmann'),
    ('Unbekannt', 'Feldmaus', 'Gehort', False, False, ''),
    ('Magd Irmgard', 'irmi', 'Bekannt', False, False, 'Magd'),
    ('Meister Ulf', 'UlfSchmied', 'Manuell', True, False, 'Meister'),
]
SEL = 0
SKINS = [(hx('#C69C7A'), hx('#4A2E1A')), (hx('#E0B896'), hx('#C9A04A')), (hx('#9A9A9A'), hx('#3A3A3A')),
         (hx('#B88A66'), hx('#7A7A7A')), (hx('#C69C7A'), hx('#2A1A10')), (hx('#9A9A9A'), hx('#3A3A3A')),
         (hx('#E0B896'), hx('#8E4A22')), (hx('#B88A66'), hx('#5A3A22'))]


def head(img, x, y, i, s=1):
    """8x8 pixel face (scaled by s); unknown players get a grey hooded face."""
    skin, hair = SKINS[i % len(SKINS)]
    face = ["HHHHHHHH", "HHHHHHHH", "SSSSSSSS", "SEWSSWES", "SSSSSSSS", "SSSnnSSS", "SSmmmmSS", "SSSSSSSS"]
    pal = {'H': hair, 'S': skin, 'E': hx('#3A2A20'), 'W': hx('#F0EBE0'), 'n': shade(skin, .8), 'm': shade(skin, .6)}
    for r, line in enumerate(face):
        for c, ch in enumerate(line):
            rect(img, x + c * s, y + r * s, x + c * s + s, y + r * s + s, pal[ch])


def frame_box(img, x, y, w, h, col=LEATHER[0]):
    rect(img, x - 1, y - 1, x + w + 1, y + h + 1, col)


def stamp(img, x, y, label, col):
    w = tw(label) + 8
    rect(img, x, y, x + w, y + 11, col); rect(img, x + 1, y + 1, x + w - 1, y + 10, PARCH[3])
    text(img, x + w // 2, y + 2, label, col, center=True, shadow=False)
    return w


def ink_field(img, x, y, w, label, value, lock=False):
    text(img, x, y, label, INK_L, shadow=False)
    vx = x + 46
    rect(img, vx, y + 9, vx + w - 46, y + 10, INK_L)
    text(img, vx + 2, y, value, INK, shadow=False)
    if lock:
        lx = vx + w - 54
        rect(img, lx + 1, y, lx + 5, y + 2, INK); rect(img, lx, y + 2, lx + 6, y + 7, INK)


def figure(img, x, y, i):
    """Front view of a player model (14x30) standing in the dossier."""
    skin, hair = SKINS[i % len(SKINS)]
    head(img, x + 3, y, i)
    rect(img, x + 3, y + 8, x + 11, y + 19, hx('#6E2226')); rect(img, x + 3, y + 8, x + 11, y + 9, hx('#B24147'))
    rect(img, x, y + 8, x + 3, y + 19, hx('#6E2226')); rect(img, x + 11, y + 8, x + 14, y + 19, hx('#6E2226'))
    rect(img, x, y + 18, x + 3, y + 20, skin); rect(img, x + 11, y + 18, x + 14, y + 20, skin)
    rect(img, x + 3, y + 19, x + 7, y + 30, hx('#3D3023')); rect(img, x + 7, y + 19, x + 11, y + 30, hx('#45321C'))
    rect(img, x + 3, y + 17, x + 11, y + 18, G2)


def leather_book(img, bx, by, bw, bh):
    rect(img, bx - 2, by - 2, bx + bw + 2, by + bh + 2, LEATHER[0])
    rect(img, bx, by, bx + bw, by + bh, LEATHER[2])
    rect(img, bx, by, bx + bw, by + 1, LEATHER[3]); rect(img, bx, by + bh - 1, bx + bw, by + bh, LEATHER[1])
    for cx, cy in ((bx + 3, by + 3), (bx + bw - 6, by + 3), (bx + 3, by + bh - 6), (bx + bw - 6, by + bh - 6)):
        rect(img, cx, cy, cx + 3, cy + 3, G2); px(img, cx, cy, G3)


def top_tabs(img, x, by, tabs, active):
    for i, (lab, c) in enumerate(tabs):
        h = 18 if i == active else 14
        w = tw(lab) + 10
        rect(img, x, by - h, x + w, by + 2, shade(c, .7)); rect(img, x + 1, by - h + 1, x + w - 1, by + 2, c)
        text(img, x + w // 2, by - h + 4, lab, NAME, center=True)
        x += w + 3


def search_line(img, x, y, w):
    rect(img, x, y, x + w, y + 12, PARCH[1]); rect(img, x + 1, y + 1, x + w - 1, y + 11, PARCH[3])
    # magnifier
    for p in ((2, 1), (3, 1), (1, 2), (4, 2), (1, 3), (4, 3), (2, 4), (3, 4), (5, 5), (6, 6)):
        px(img, x + 2 + p[0], y + 1 + p[1], INK_L)
    text(img, x + 12, y + 2, 'Suche...', INK_L, shadow=False)


def list_rows(img, x, y, w, rows=8, rh=19, heads=True):
    for i, (name, acc, state, online, conflict, _) in enumerate(PEOPLE[:rows]):
        yy = y + i * rh
        if i == SEL:
            rect(img, x - 2, yy - 1, x + w, yy + rh - 2, shade(PARCH[2], .88))
            rect(img, x - 2, yy - 1, x, yy + rh - 2, WAX[2])
        hx_ = x
        if heads:
            frame_box(img, x + 1, yy + 1, 8, 8, INK_L); head(img, x + 1, yy + 1, i)
            hx_ = x + 13
        unknown = name == 'Unbekannt'
        text(img, hx_, yy, name, INK_L if unknown else INK, shadow=False)
        text(img, hx_, yy + 9, acc + ' - ' + state, INK_L, shadow=False)
        if online: disc(img, x + w - 6, yy + 4, 2, (GREEN, GREEN, GREEN_L))
        if conflict:
            text(img, x + w - 8, yy + 9, '!', WAX[2], shadow=False)
        if i < rows - 1: rect(img, x, yy + rh - 2, x + w - 2, yy + rh - 1, (139, 110, 78, 70))


def dossier(img, x, y, w, i=SEL):
    name, acc, state, online, conflict, title = PEOPLE[i]
    # portrait in a gold frame
    rect(img, x, y, x + 28, y + 28, G0); rect(img, x + 1, y + 1, x + 27, y + 27, G2); rect(img, x + 2, y + 2, x + 26, y + 26, LEATHER[1])
    head(img, x + 2, y + 2, i, 3)
    text(img, x + 34, y + 1, name, INK, shadow=False); text(img, x + 35, y + 1, name, INK, shadow=False)
    text(img, x + 34, y + 11, acc, INK_L, shadow=False)
    stamp(img, x + 34, y + 20, state, STATE_COL[state])
    if online: disc(img, x + 34 + tw(state) + 16, y + 25, 3, (GREEN, GREEN, GREEN_L))
    yy = y + 36
    ink_field(img, x, yy, w, 'RP-Name', name.split(' ', 1)[-1])
    ink_field(img, x, yy + 14, w, 'Titel', title, lock=True)
    ink_field(img, x, yy + 28, w, 'Notiz', 'Schmied, Hafen')
    # colour pots for Chat / Tab / Schild
    text(img, x, yy + 44, 'Farben', INK_L, shadow=False)
    for j, (lab, c) in enumerate((('Chat', hx('#FFAA00')), ('Tab', hx('#55FF55')), ('Kopf', hx('#FFFFFF')))):
        cx = x + 46 + sum(tw(l) + 13 for l in ('Chat', 'Tab', 'Kopf')[:j])
        disc(img, cx + 3, yy + 48, 3, (shade(c, .6), c, c))
        text(img, cx + 9, yy + 44, lab, INK, shadow=False)
    return yy + 58


def buttons_ribbon(img, x, y, items):
    for lab, c in items:
        w = tw(lab) + 12
        rect(img, x, y, x + w, y + 16, shade(c, .6)); rect(img, x + 1, y + 1, x + w - 1, y + 15, c)
        rect(img, x + 1, y + 1, x + w - 1, y + 2, shade(c, 1.2))
        text(img, x + w // 2, y + 4, lab, NAME, center=True)
        x += w + 4


TABS = (('Personen', WAX[2]), ('Titel', GOLD_T), ('Import', TEAL))


# ---------- P1 Register: two-page book, index left, dossier right ----------
def p1():
    img = backdrop()
    bx, by, bw, bh = 62, 28, 320, 196
    leather_book(img, bx, by, bw, bh)
    top_tabs(img, bx + 14, by, TABS, 0)
    lx, ly, lw, lh = bx + 8, by + 6, 148, bh - 12
    rx = bx + bw // 2 + 4
    parchment(img, lx, ly, lw, lh, torn=False, seed=11)
    parchment(img, rx, ly, lw, lh, torn=False, seed=12)
    for i in range(6):
        rect(img, lx + lw - 6 + i, ly, lx + lw - 5 + i, ly + lh, shade(PARCH[2], .80 + i * .03))
        rect(img, rx + i, ly, rx + i + 1, ly + lh, shade(PARCH[2], .95 - (5 - i) * .03))
    rect(img, lx + lw, ly - 2, rx, ly + lh + 2, LEATHER[1])
    search_line(img, lx + 8, ly + 6, 96)
    text(img, lx + lw - 10 - tw('8/124'), ly + 8, '8/124', INK_L, shadow=False)
    list_rows(img, lx + 8, ly + 24, lw - 16)
    # A-Z thumb index on the right edge of the left page
    for k, ch in enumerate('ABDGHKMU'):
        yy = ly + 24 + k * 19
        rect(img, lx + lw - 6, yy, lx + lw + 0, yy + 12, LEATHER[3] if k else WAX[2])
        text(img, lx + lw - 5, yy + 2, ch, NAME, shadow=False) if False else None
    yend = dossier(img, rx + 10, ly + 8, lw - 20)
    figure(img, rx + lw - 30, ly + 8, SEL)
    buttons_ribbon(img, rx + 10, ly + lh - 22, (('Speichern', GREEN), ('Vergessen', WAX[2])))
    # filter ribbons on the left edge of the cover
    for k, (lab, c) in enumerate((('Alle', WAX[2]), ('Online', GREEN), ('Fremd', hx('#8A7A66')), ('Konflikt', hx('#B24147')))):
        yy = by + 20 + k * 22
        w = tw(lab) + 8
        rect(img, bx - w - 2, yy, bx, yy + 16, shade(c, .6)); rect(img, bx - w - 1, yy + 1, bx, yy + 15, c if k == 0 else shade(c, .85))
        text(img, bx - w // 2 - 1, yy + 4, lab, NAME, center=True)
    return img


# ---------- P2 Karteikasten: wooden card box with pulled-out card ----------
def p2():
    img = backdrop()
    x0, y0, w0, h0 = 28, 16, 344, 212
    wood_panel(img, x0, y0, w0, h0)
    text(img, x0 + 10, y0 + 8, 'Personenkartei', G3)
    # category dividers as tabs
    tx = x0 + 110
    for k, (lab, c) in enumerate((('Alle', WAX[2]), ('Online', GREEN), ('Unbekannt', hx('#8A7A66')), ('Konflikte', hx('#B24147')))):
        w = tw(lab) + 10
        rect(img, tx, y0 + 6, tx + w, y0 + 18, shade(c, .6)); rect(img, tx + 1, y0 + 7, tx + w - 1, y0 + 18, c if k == 0 else shade(c, .8))
        text(img, tx + w // 2, y0 + 8, lab, NAME, center=True)
        tx += w + 3
    # card stack (left): each row is a card edge
    cx, cy, cw = x0 + 10, y0 + 24, 150
    search_line(img, cx, cy, cw)
    for i, (name, acc, state, online, conflict, _) in enumerate(PEOPLE):
        yy = cy + 16 + i * 21
        rect(img, cx + 1, yy + 1, cx + cw + 1, yy + 20, (0, 0, 0, 80))
        col = PARCH[3] if i == SEL else PARCH[2]
        rect(img, cx, yy, cx + cw, yy + 19, PARCH[1]); rect(img, cx + 1, yy + 1, cx + cw - 1, yy + 18, col)
        rect(img, cx + 1, yy + 1, cx + 4, yy + 18, STATE_COL[state])
        frame_box(img, cx + 8, yy + 5, 8, 8, INK_L); head(img, cx + 8, yy + 5, i)
        text(img, cx + 21, yy + 2, name, INK_L if name == 'Unbekannt' else INK, shadow=False)
        text(img, cx + 21, yy + 10, acc, INK_L, shadow=False)
        if online: disc(img, cx + cw - 8, yy + 6, 2, (GREEN, GREEN, GREEN_L))
        if conflict: text(img, cx + cw - 9, yy + 10, '!', WAX[2], shadow=False)
    # pulled-out card (right)
    dx, dy, dw, dh = cx + cw + 12, y0 + 24, 166, 178
    rect(img, dx + 3, dy + 3, dx + dw + 3, dy + dh + 3, (0, 0, 0, 100))
    parchment(img, dx, dy, dw, dh, torn=False, seed=21)
    rect(img, dx, dy, dx + dw, dy + 4, STATE_COL[PEOPLE[SEL][2]])
    dossier(img, dx + 8, dy + 10, dw - 16)
    figure(img, dx + dw - 26, dy + 10, SEL)
    plaque(img, dx + 8, dy + dh - 22, 62, 16, 'Speichern')
    plaque(img, dx + 74, dy + dh - 22, 62, 16, 'Vergessen')
    return img


# ---------- P3 Einseitig: single page list, dossier leaf overlapping on the right ----------
def p3():
    img = backdrop()
    bw, bh = 176, 200
    bx, by = 60, 24
    leather_book(img, bx, by, bw, bh)
    rect(img, bx, by, bx + 6, by + bh, LEATHER[1])
    top_tabs(img, bx + 12, by, TABS, 0)
    lx, ly, lw, lh = bx + 10, by + 6, bw - 16, bh - 12
    parchment(img, lx, ly, lw, lh, torn=False, seed=31)
    search_line(img, lx + 8, ly + 6, 100)
    text(img, lx + lw - 8 - tw('124'), ly + 8, '124', INK_L, shadow=False)
    list_rows(img, lx + 8, ly + 24, lw - 16, rows=8, rh=20)
    # filter ribbons on the left
    for k, (lab, c) in enumerate((('Alle', WAX[2]), ('Online', GREEN), ('Fremd', hx('#8A7A66')), ('Konflikt', hx('#B24147')))):
        yy = by + 14 + k * 22
        w = tw(lab) + 8
        rect(img, bx - w - 2, yy, bx, yy + 16, shade(c, .6)); rect(img, bx - w - 1, yy + 1, bx, yy + 15, c if k == 0 else shade(c, .85))
        text(img, bx - w // 2 - 1, yy + 4, lab, NAME, center=True)
    # dossier leaf sliding out to the right, pinned with a wax seal
    dx, dy, dw, dh = bx + bw + 4, by + 16, 150, 170
    rect(img, dx + 3, dy + 3, dx + dw + 3, dy + dh + 3, (0, 0, 0, 110))
    parchment(img, dx, dy, dw, dh, torn=True, seed=32)
    wax_seal(img, dx + dw - 10, dy + 8, 7)
    dossier(img, dx + 8, dy + 10, dw - 16)
    figure(img, dx + dw - 24, dy + 112, SEL)
    buttons_ribbon(img, dx + 8, dy + dh - 22, (('Speichern', GREEN), ('Vergessen', WAX[2])))
    return img


for name, fn in (('personen_p1_register', p1), ('personen_p2_kartei', p2), ('personen_p3_einseitig', p3)):
    fn().resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUTD, name + '.png'))
print('ok')
