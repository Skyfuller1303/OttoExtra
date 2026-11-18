"""Idea sketches for the resource timer HUD (right screen edge) of OttoExtra Resources.
Same panel style as the squad HUD. Usage: python -I gen_minentimer_hud.py <out_dir>"""
import os, sys
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_rpbuch2.py'), encoding='utf-8').read()
exec(_b[:_b.index("personen2().resize")])
from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
S = 4
MARGIN, PAD_X, HEADER_H, ROW, GROUP_ROW, FOOTER_H = 4, 4, 15, 14, 12, 14


def argb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255, (v >> 24) & 255)


COL_PANEL, COL_OUTLINE = argb(0x961E1E24), argb(0xD7141418)
COL_BEVEL_L, COL_BEVEL_D = argb(0xD7514F59), argb(0xD72E2D35)
COL_ROW_BG = argb(0x503B3A45)
COL_DIV_D, COL_DIV_L = argb(0xC8141418), argb(0xC83B3A45)
COL_TITLE, COL_NAME, COL_DIM = argb(0xFFD6C194), argb(0xFFF0EBE0), argb(0xFF8F8C96)
COL_READY, COL_REGEN, COL_SOON = argb(0xFF86C25A), argb(0xFFD9B44A), argb(0xFFE08A4A)
COL_BAR_BG = argb(0xC8141418)

TYPE_COL = {'Eisen': hx('#C9A58A'), 'Kohle': hx('#55555F'), 'Salz': hx('#E6E2EC'),
            'Torf': hx('#7A5A3A'), 'Lehm': hx('#9AA3B5'), 'Garten': hx('#6FB35A'), 'Bienen': hx('#E0B43A')}

# (name, type, Lehen, minutes left (0 = ready), percent, distance in blocks)
ZONES = [
    ('Eisenmine Nordstollen', 'Eisen', 'Schwarzhain', 0, 100, 42),
    ('Kohlevorkommen Bach', 'Kohle', 'Schwarzhain', 0, 100, 118),
    ('Salzmine Alter Schacht', 'Salz', 'Schwarzhain', 8, 70, 96),
    ('Lehmgrube Furt', 'Lehm', 'Schwarzhain', 21, 30, 203),
    ('Eisenvorkommen Hang', 'Eisen', 'Schwarzhain', 74, 38, 61),
    ('Torfmoor Senke', 'Torf', 'Schwarzhain', 191, 20, 240),
    ('Klostergarten', 'Garten', 'Nordeck', 0, 100, 610),
    ('Kohlemine Tiefbau', 'Kohle', 'Nordeck', 133, 70, 655),
    ('Bienenstock Wiese', 'Bienen', 'Nordeck', 12, 60, 702),
    ('Eisenmine Grenzstein', 'Eisen', 'Hafenmark', 45, 62, 1320),
    ('Salzvorkommen Ufer', 'Salz', 'Hafenmark', 0, 100, 1408),
]


def R(img, ox, oy, x0, y0, x1, y1, c):
    rect(img, (ox + x0) * S, (oy + y0) * S, (ox + x1) * S, (oy + y1) * S, c)


def T(img, ox, oy, x, y, s, c, small=False):
    if small:
        text(img, (ox + x) * S, (oy + y) * S, s, c, 3, False, False)
    else:
        text(img, (ox + x) * S, (oy + y) * S, s, c, S, False, True)


def fmt(mins):
    if mins <= 0:
        return 'bereit'
    return f'{mins // 60}:{mins % 60:02d} h' if mins >= 60 else f'{mins} min'


def tcol(mins):
    return COL_READY if mins <= 0 else COL_SOON if mins <= 10 else COL_REGEN


def panel(img, ox, oy, w, h, title, right=None):
    r, b = w, h
    for a in ((1, 0, r - 1, 1), (1, b - 1, r - 1, b), (0, 1, 1, b - 1), (r - 1, 1, r, b - 1),
              (1, 1, 2, 2), (r - 2, 1, r - 1, 2), (1, b - 2, 2, b - 1), (r - 2, b - 2, r - 1, b - 1)):
        R(img, ox, oy, *a, COL_OUTLINE)
    R(img, ox, oy, 2, 1, r - 2, 2, COL_BEVEL_L); R(img, ox, oy, 1, 2, 2, b - 2, COL_BEVEL_L)
    R(img, ox, oy, 2, b - 2, r - 2, b - 1, COL_BEVEL_D); R(img, ox, oy, r - 2, 2, r - 1, b - 2, COL_BEVEL_D)
    R(img, ox, oy, 2, 2, r - 2, b - 2, COL_PANEL)
    T(img, ox, oy, PAD_X, 3, title, COL_TITLE)
    if right:
        T(img, ox, oy, w - PAD_X - tw(right), 3, right, COL_DIM)
    divider(img, ox, oy, w, HEADER_H - 3)


def divider(img, ox, oy, w, y):
    R(img, ox, oy, 2, y, w - 2, y + 1, COL_DIV_D); R(img, ox, oy, 2, y + 1, w - 2, y + 2, COL_DIV_L)


def gem(img, ox, oy, x, y, col):
    dark = shade(col, .6)
    R(img, ox, oy, x + 1, y, x + 3, y + 3, col); R(img, ox, oy, x, y + 1, x + 1, y + 3, col)
    R(img, ox, oy, x + 3, y + 1, x + 4, y + 3, dark); R(img, ox, oy, x + 1, y + 3, x + 3, y + 4, dark)


def zone_row(img, ox, oy, w, y, z, bar=True, dist=False):
    name, typ, _, mins, pct, d = z
    gem(img, ox, oy, PAD_X + 1, y + 3, TYPE_COL[typ])
    t = fmt(mins)
    tx = w - PAD_X - tw(t)
    room = tx - (PAD_X + 9) - 4
    nm = name
    while tw(nm) > room and len(nm) > 3:
        nm = nm[:-2].rstrip() + '.'
    T(img, ox, oy, PAD_X + 9, y + 1, nm, COL_NAME if mins <= 0 else argb(0xFFCFCBD6))
    T(img, ox, oy, tx, y + 1, t, tcol(mins))
    if bar and mins > 0:
        R(img, ox, oy, PAD_X + 9, y + 11, w - PAD_X, y + 12, COL_BAR_BG)
        R(img, ox, oy, PAD_X + 9, y + 11, PAD_X + 9 + round((w - 2 * PAD_X - 9) * pct / 100), y + 12, tcol(mins))


def group_row(img, ox, oy, w, y, name, ready, total):
    R(img, ox, oy, 4, y, w - 4, y + GROUP_ROW - 1, COL_ROW_BG)
    T(img, ox, oy, PAD_X + 2, y + 1, name, COL_TITLE)
    c = f'{ready}/{total}'
    T(img, ox, oy, w - PAD_X - 2 - tw(c), y + 1, c, COL_READY if ready else COL_DIM)


def place(w, h):
    return W - w - MARGIN, 34  # below the usual top-right area, right edge


def variant_lehen(img):
    """A: only the Lehen the player is in, sorted ready first, then by remaining time."""
    zs = sorted([z for z in ZONES if z[2] == 'Schwarzhain'], key=lambda z: z[3])
    w, h = 150, HEADER_H + len(zs) * ROW + FOOTER_H
    ox, oy = place(w, h)
    panel(img, ox, oy, w, h, 'Vorkommen', 'Schwarzhain')
    y = HEADER_H
    for z in zs:
        zone_row(img, ox, oy, w, y, z); y += ROW
    divider(img, ox, oy, w, y)
    ready = sum(1 for z in zs if z[3] <= 0)
    T(img, ox, oy, PAD_X, y + 4, f'{ready} bereit, nächstes in {fmt(min(z[3] for z in zs if z[3] > 0))}', COL_DIM, small=True)


def variant_alle(img):
    """B: all joined lists, grouped by Lehen, limited to a fixed number of rows."""
    limit, shown = 8, 0
    order = ['Schwarzhain', 'Nordeck', 'Hafenmark']
    lines = []
    for l in order:
        zs = sorted([z for z in ZONES if z[2] == l], key=lambda z: z[3])
        lines.append(('g', l, sum(1 for z in zs if z[3] <= 0), len(zs)))
        for z in zs:
            if shown < limit:
                lines.append(('z', z)); shown += 1
    rest = len(ZONES) - shown
    w = 150
    h = HEADER_H + sum(GROUP_ROW if l[0] == 'g' else ROW - 3 for l in lines) + FOOTER_H
    ox, oy = place(w, h)
    panel(img, ox, oy, w, h, 'Vorkommen', 'alle Lehen')
    y = HEADER_H
    for l in lines:
        if l[0] == 'g':
            group_row(img, ox, oy, w, y, l[1], l[2], l[3]); y += GROUP_ROW
        else:
            zone_row(img, ox, oy, w, y, l[1], bar=False); y += ROW - 3
    divider(img, ox, oy, w, y)
    T(img, ox, oy, PAD_X, y + 4, f'+ {rest} weitere in der Übersicht', COL_DIM, small=True)


def variant_knapp(img):
    """C: minimal, one line per state: ready count and the next three to become ready."""
    zs = [z for z in ZONES if z[2] == 'Schwarzhain']
    nxt = sorted([z for z in zs if z[3] > 0], key=lambda z: z[3])[:3]
    ready = [z for z in zs if z[3] <= 0]
    w, h = 126, HEADER_H + ROW + len(nxt) * (ROW - 3) + 3
    ox, oy = place(w, h)
    panel(img, ox, oy, w, h, 'Schwarzhain')
    y = HEADER_H
    R(img, ox, oy, 4, y, w - 4, y + ROW - 3, COL_ROW_BG)
    x = PAD_X + 2
    for z in ready:
        gem(img, ox, oy, x, y + 3, TYPE_COL[z[1]]); x += 7
    T(img, ox, oy, x + 2, y + 1, f'{len(ready)} bereit', COL_READY)
    y += ROW
    for z in nxt:
        zone_row(img, ox, oy, w, y, z, bar=False); y += ROW - 3


def scene(fn, label):
    base = backdrop().resize((W * S, H * S), Image.NEAREST)
    fn(base)
    text(base, 8 * S, (H - 14) * S, label, NAME, S)
    return base


for name, fn, label in (('hud_a_aktuelles_lehen', variant_lehen, 'A: nur das Lehen, in dem man steht (Standard)'),
                        ('hud_b_alle_lehen', variant_alle, 'B: alle Lehen, gruppiert und gekürzt'),
                        ('hud_c_knapp', variant_knapp, 'C: knapp, nur bereit und die nächsten drei')):
    scene(fn, label).save(os.path.join(OUT, name + '.png'))
print('ok')
