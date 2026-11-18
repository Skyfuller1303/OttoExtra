"""Preview of the Tactical Factions squad activity HUD, mirroring SquadActivityHud.java
(same constants, colours and layout). Usage: python -I gen_trupp_hud.py <out_dir>"""
import os, sys
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_rpbuch2.py'), encoding='utf-8').read()
exec(_b[:_b.index("personen2().resize")])
from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
S = 4  # output pixels per GUI pixel; lets the 0.75 footer scale render exactly (3 px)

# Constants from SquadActivityHud.java
MARGIN, MIN_W, MAX_W, HEADER_H, SQUAD_ROW, MEMBER_ROW, FOOTER_H = 4, 104, 158, 15, 14, 11, 14
PAD_X, DOT, HEAD = 4, 4, 8


def argb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255, (v >> 24) & 255)


COL_PANEL, COL_OUTLINE = argb(0x961E1E24), argb(0xD7141418)
COL_BEVEL_L, COL_BEVEL_D = argb(0xD7514F59), argb(0xD72E2D35)
COL_ROW_BG = argb(0x503B3A45)
COL_DIV_D, COL_DIV_L = argb(0xC8141418), argb(0xC83B3A45)
COL_TITLE, COL_NAME, COL_DIM = argb(0xFFD6C194), argb(0xFFF0EBE0), argb(0xFF8F8C96)
COL_ON, COL_ON_D = argb(0xFF86C25A), argb(0xFF2F5426)

SQUADS = [('Vorhut', hx('#55C46A'), [('Armon Schwarzhain', 6, True, True), ('Merle', 5, True, False),
                                     ('Wolfgang', 4, True, False), ('Aerthur von Nordeck', 3, False, False)]),
          ('Hafenwache', hx('#4FA3C7'), [('Joey R.', 1, True, False), ('Feldmaus', 7, False, False),
                                         ('Gauthier', 2, False, False)]),
          ('Späher', hx('#D9B44A'), [('Lorenz', 0, True, False), ('Wolfgang', 4, True, False)])]


def R(img, ox, oy, x0, y0, x1, y1, c):
    rect(img, (ox + x0) * S, (oy + y0) * S, (ox + x1) * S, (oy + y1) * S, c)


def T(img, ox, oy, x, y, s, c, k=S, shadow=True):
    text(img, (ox + x) * S if k == S else ox * S + x, (oy + y) * S if k == S else oy * S + y, s, c, k, False, shadow)


def build(only_online):
    lines = []
    for name, col, mem in SQUADS:
        on = sum(1 for m in mem if m[2])
        lines.append(('h', name, col, on, len(mem)))
        for m in mem:
            if only_online and not m[2]: continue
            lines.append(('m',) + m)
    return lines


def hud(img, corner, only_online, screen_w, screen_h):
    lines = build(only_online)
    total_on = sum(l[3] for l in lines if l[0] == 'h'); total = sum(l[4] for l in lines if l[0] == 'h')
    title, footer = 'Trupps', f'{total_on}/{total} online'
    used = sum(SQUAD_ROW if l[0] == 'h' else MEMBER_ROW for l in lines)
    w = max(MIN_W, tw(title) + 2 * PAD_X, round(tw(footer) * .75) + 2 * PAD_X)
    for l in lines:
        if l[0] == 'h':
            lead = PAD_X + 1 + DOT + 3 + tw(f'{l[3]}/{l[4]}') + 4; nm = l[1]
        else:
            lead = PAD_X + 6 + HEAD + 4; nm = l[1]
        w = max(w, lead + min(100, tw(nm)) + PAD_X)
    w = min(MAX_W, w); h = HEADER_H + used + FOOTER_H
    ox = screen_w - w - MARGIN if 'right' in corner else MARGIN
    oy = screen_h - h - MARGIN if 'bottom' in corner else MARGIN
    # panel
    r, b = w, h
    for a in ((1, 0, r - 1, 1), (1, b - 1, r - 1, b), (0, 1, 1, b - 1), (r - 1, 1, r, b - 1),
              (1, 1, 2, 2), (r - 2, 1, r - 1, 2), (1, b - 2, 2, b - 1), (r - 2, b - 2, r - 1, b - 1)):
        R(img, ox, oy, *a, COL_OUTLINE)
    R(img, ox, oy, 2, 1, r - 2, 2, COL_BEVEL_L); R(img, ox, oy, 1, 2, 2, b - 2, COL_BEVEL_L)
    R(img, ox, oy, 2, b - 2, r - 2, b - 1, COL_BEVEL_D); R(img, ox, oy, r - 2, 2, r - 1, b - 2, COL_BEVEL_D)
    R(img, ox, oy, 2, 2, r - 2, b - 2, COL_PANEL)
    T(img, ox, oy, PAD_X, 3, title, COL_TITLE)
    def divider(y):
        R(img, ox, oy, 2, y, w - 2, y + 1, COL_DIV_D); R(img, ox, oy, 2, y + 1, w - 2, y + 2, COL_DIV_L)
    divider(HEADER_H - 3)
    y = HEADER_H
    for l in lines:
        if l[0] == 'h':
            _, name, col, on, tot = l
            R(img, ox, oy, 4, y, w - 4, y + SQUAD_ROW - 1, COL_ROW_BG)
            R(img, ox, oy, 3, y + 1, 4, y + SQUAD_ROW - 2, COL_ROW_BG); R(img, ox, oy, w - 4, y + 1, w - 3, y + SQUAD_ROW - 2, COL_ROW_BG)
            dy = y + (SQUAD_ROW - 1 - DOT) // 2; dark = shade(col, .6)
            R(img, ox, oy, PAD_X + 2, dy, PAD_X + 4, dy + 3, col); R(img, ox, oy, PAD_X + 1, dy + 1, PAD_X + 2, dy + 3, col)
            R(img, ox, oy, PAD_X + 4, dy + 1, PAD_X + 5, dy + 3, dark); R(img, ox, oy, PAD_X + 2, dy + 3, PAD_X + 4, dy + 4, dark)
            base = (SQUAD_ROW - 9) // 2 + y
            cnt = f'{on}/{tot}'
            T(img, ox, oy, PAD_X + 1 + DOT + 3, base + 1, name, COL_NAME)
            T(img, ox, oy, w - PAD_X - tw(cnt), base + 1, cnt, COL_ON if on else COL_DIM)
            y += SQUAD_ROW
        else:
            _, name, hd, online, lead = l
            hx0, hy0 = PAD_X + 6, y + 1
            head(img, (ox + hx0) * S, (oy + hy0) * S, hd, S)
            if not online: R(img, ox, oy, hx0, hy0, hx0 + HEAD, hy0 + HEAD, (0, 0, 0, 153))
            if lead:
                for cx_, h_ in ((0, 3), (2, 2), (4, 3)):
                    R(img, ox, oy, hx0 + cx_, hy0 - h_, hx0 + cx_ + 1, hy0, G3)
            if online:
                R(img, ox, oy, hx0 + HEAD - 2, hy0 + HEAD - 2, hx0 + HEAD + 1, hy0 + HEAD + 1, COL_ON_D)
                R(img, ox, oy, hx0 + HEAD - 1, hy0 + HEAD - 1, hx0 + HEAD, hy0 + HEAD, COL_ON)
            T(img, ox, oy, hx0 + HEAD + 4, y + (MEMBER_ROW - 9) // 2 + 1, name, COL_NAME if online else COL_DIM)
            y += MEMBER_ROW
    divider(y)
    T(img, ox, oy, PAD_X * S, (y + 4) * S, footer, COL_DIM, k=3, shadow=False)


def scene(only_online, label):
    base = backdrop().resize((W * S, H * S), Image.NEAREST)
    hud(base, 'top_right', only_online, W, H)
    text(base, 8 * S, (H - 14) * S, label, NAME, S)
    return base


for name, only, label in (('tf_trupp_hud', False, 'Trupp-Anzeige (alle Mitglieder)'),
                          ('tf_trupp_hud_online', True, 'Trupp-Anzeige (nur online)')):
    scene(only, label).save(os.path.join(OUT, name + '.png'))
print('ok')
