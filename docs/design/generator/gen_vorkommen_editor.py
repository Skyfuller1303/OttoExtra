"""Idea sketches for the editor that creates or edits a resource zone (OttoExtra Resources).
Same slate panel style as the HUD. Usage: python -I gen_vorkommen_editor.py <out_dir>"""
import os, sys
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_rpbuch2.py'), encoding='utf-8').read()
exec(_b[:_b.index("personen2().resize")])
from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
S = 4


def argb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255, (v >> 24) & 255)


PANEL, OUTLINE = argb(0xF01E1E24), argb(0xFF141418)
BEVEL_L, BEVEL_D = argb(0xFF514F59), argb(0xFF2E2D35)
FIELD, FIELD_EDGE, FIELD_FOCUS = argb(0xFF14141A), argb(0xFF3B3A45), argb(0xFFD6C194)
BTN, BTN_HI, BTN_LO = argb(0xFF2E3A4A), argb(0xFF46566C), argb(0xFF1A2028)
TITLE, TEXT, DIM, HINT = argb(0xFFD6C194), argb(0xFFF0EBE0), argb(0xFF8F8C96), argb(0xFF6C6974)
READY, WARN, DANGER = argb(0xFF86C25A), argb(0xFFD9B44A), argb(0xFFC0564A)
DIV_D, DIV_L = argb(0xFF141418), argb(0xFF3B3A45)

TYPES = [('Eisen', hx('#C9A58A')), ('Kohle', hx('#55555F')), ('Salz', hx('#E6E2EC')), ('Torf', hx('#7A5A3A')),
         ('Lehm', hx('#9AA3B5')), ('Garten', hx('#6FB35A')), ('Bienen', hx('#E0B43A')), ('Marker', hx('#B56FC7'))]


def R(img, x0, y0, x1, y1, c):
    rect(img, x0 * S, y0 * S, x1 * S, y1 * S, c)


def T(img, x, y, s, c, shadow=True):
    text(img, x * S, y * S, s, c, S, False, shadow)


def Ts(img, x, y, s, c):
    text(img, x * S, y * S, s, c, 3, False, False)


def frame(img, x, y, w, h, fill=PANEL):
    R(img, x + 1, y, x + w - 1, y + h, OUTLINE); R(img, x, y + 1, x + w, y + h - 1, OUTLINE)
    R(img, x + 1, y + 1, x + w - 1, y + h - 1, fill)
    R(img, x + 2, y + 1, x + w - 2, y + 2, BEVEL_L); R(img, x + 1, y + 2, x + 2, y + h - 2, BEVEL_L)
    R(img, x + 2, y + h - 2, x + w - 2, y + h - 1, BEVEL_D); R(img, x + w - 2, y + 2, x + w - 1, y + h - 2, BEVEL_D)


def divider(img, x0, x1, y):
    R(img, x0, y, x1, y + 1, DIV_D); R(img, x0, y + 1, x1, y + 2, DIV_L)


def field(img, x, y, w, value, focus=False, hint=None, h=13):
    R(img, x, y, x + w, y + h, FIELD_FOCUS if focus else FIELD_EDGE)
    R(img, x + 1, y + 1, x + w - 1, y + h - 1, FIELD)
    if value:
        T(img, x + 4, y + 3, value, TEXT, shadow=False)
        if focus:
            R(img, x + 5 + tw(value), y + 2, x + 6 + tw(value), y + h - 2, TEXT)
    elif hint:
        T(img, x + 4, y + 3, hint, HINT, shadow=False)


def button(img, x, y, w, label, col=BTN, tc=TEXT, h=14, on=False):
    R(img, x, y, x + w, y + h, OUTLINE)
    R(img, x + 1, y + 1, x + w - 1, y + h - 1, col)
    R(img, x + 1, y + 1, x + w - 1, y + 2, BTN_HI if col == BTN else shade(col, 1.25))
    R(img, x + 1, y + h - 2, x + w - 1, y + h - 1, BTN_LO if col == BTN else shade(col, .7))
    if on:
        R(img, x + 1, y + h - 2, x + w - 1, y + h - 1, TITLE)
    T(img, x + (w - tw(label)) // 2, y + 3, label, tc)


def gem(img, x, y, col):
    dark = shade(col, .6)
    R(img, x + 1, y, x + 3, y + 3, col); R(img, x, y + 1, x + 1, y + 3, col)
    R(img, x + 3, y + 1, x + 4, y + 3, dark); R(img, x + 1, y + 3, x + 3, y + 4, dark)


def label(img, x, y, s):
    Ts(img, x, y, s, DIM)


def type_chips(img, x, y, w, active):
    cw_, gap = (w - 3 * 3) // 4, 3
    for i, (name, col) in enumerate(TYPES):
        cx, cy = x + (i % 4) * (cw_ + gap), y + (i // 4) * 15
        sel = i == active
        R(img, cx, cy, cx + cw_, cy + 13, TITLE if sel else FIELD_EDGE)
        R(img, cx + 1, cy + 1, cx + cw_ - 1, cy + 12, argb(0xFF2E2D35) if sel else FIELD)
        gem(img, cx + 4, cy + 4, col)
        T(img, cx + 11, cy + 3, name, TEXT if sel else DIM, shadow=False)


def preview(img, x, y, w, h, cuboid):
    """Top-down sketch of the zone with the player marker."""
    R(img, x, y, x + w, y + h, FIELD_EDGE); R(img, x + 1, y + 1, x + w - 1, y + h - 1, hx('#26331F'))
    for i, c in enumerate((hx('#2C3A24'), hx('#32402A'), hx('#3A3F36'), hx('#45474A'))):
        R(img, x + 1 + i * 9, y + 1 + (i * 13) % (h - 20), x + 30 + i * 17, y + 22 + (i * 11) % (h - 26), c)
    R(img, x + 1, y + h - 22, x + w - 1, y + h - 16, hx('#2E4A66'))
    cx, cy = x + w // 2, y + h // 2 - 4
    zc = hx('#C9A58A')
    if cuboid:
        x0, y0, x1, y1 = cx - 26, cy - 15, cx + 22, cy + 17
        R(img, x0, y0, x1, y1, (zc[0], zc[1], zc[2], 70))
        for a in ((x0, y0, x1, y0 + 1), (x0, y1 - 1, x1, y1), (x0, y0, x0 + 1, y1), (x1 - 1, y0, x1, y1)):
            R(img, *a, zc)
        for px_, py_, n in ((x0, y0, '1'), (x1 - 1, y1 - 1, '2')):
            R(img, px_ - 2, py_ - 2, px_ + 3, py_ + 3, TITLE); pass
    else:
        r = 14
        for dy in range(-r, r + 1):
            half = int((r * r - dy * dy) ** .5)
            R(img, cx - half, cy + dy, cx + half + 1, cy + dy + 1, (zc[0], zc[1], zc[2], 70))
            R(img, cx - half, cy + dy, cx - half + 1, cy + dy + 1, zc); R(img, cx + half, cy + dy, cx + half + 1, cy + dy + 1, zc)
        R(img, cx - 1, cy - 1, cx + 2, cy + 2, TITLE)
    # player arrow
    R(img, cx + 7, cy + 6, cx + 10, cy + 9, TEXT); R(img, cx + 8, cy + 4, cx + 9, cy + 6, TEXT)
    Ts(img, x + 4, y + h - 11, 'Karte von oben, Pfeil = du', DIM)


def editor(cuboid, new):
    img = backdrop().resize((W * S, H * S), Image.NEAREST)
    rect(img, 0, 0, W * S, H * S, (0, 0, 0, 120))
    pw, ph = 400, 240
    x, y = (W - pw) // 2, (H - ph) // 2
    frame(img, x, y, pw, ph)
    T(img, x + 8, y + 6, 'Neues Vorkommen' if new else 'Vorkommen bearbeiten', TITLE)
    right = 'Liste: Schwarzhain'
    T(img, x + pw - 8 - tw(right), y + 6, right, DIM)
    divider(img, x + 2, x + pw - 2, y + 17)

    lx, lw = x + 10, 236            # left column: form
    rx = lx + lw + 10; rw = x + pw - 10 - rx   # right column: preview and times
    cy = y + 24

    label(img, lx, cy, 'Name'); cy += 8
    field(img, lx, cy, lw, 'Eisenmine Nordstollen' if not new else '', focus=new, hint='z. B. Eisenmine am Bach'); cy += 18

    label(img, lx, cy, 'Art'); cy += 8
    type_chips(img, lx, cy, lw, 0); cy += 33

    label(img, lx, cy, 'Umfang'); label(img, lx + 122, cy, 'Lehen (automatisch erkannt)'); cy += 8
    button(img, lx, cy, 56, 'Vorkommen', on=cuboid is False and False)
    button(img, lx + 58, cy, 56, 'Mine', on=True)
    field(img, lx + 122, cy, lw - 122, 'Schwarzhain', h=14); cy += 20

    label(img, lx, cy, 'Ort'); cy += 8
    if cuboid:
        R(img, lx, cy, lx + lw, cy + 30, FIELD_EDGE); R(img, lx + 1, cy + 1, lx + lw - 1, cy + 29, FIELD)
        T(img, lx + 5, cy + 4, 'Bereich aus Litematica', TEXT, shadow=False)
        Ts(img, lx + 5, cy + 17, '49 x 33 x 18, Mitte -1260 69 3418', DIM)
        button(img, lx + lw - 96, cy + 8, 92, 'In Punkt wandeln', h=14); cy += 35
        Ts(img, lx, cy + 1, 'Abbau gilt, wenn du im Bereich stehst.', DIM); cy += 18
    else:
        label(img, lx, cy + 3, 'Mitte')
        field(img, lx + 34, cy, 118, '-1260  64  3418')
        button(img, lx + 156, cy, 80, 'Meine Position', h=13); cy += 17
        label(img, lx, cy + 3, 'Radius')
        R(img, lx + 34, cy + 5, lx + 152, cy + 8, FIELD_EDGE); R(img, lx + 34, cy + 5, lx + 86, cy + 8, TITLE)
        R(img, lx + 84, cy + 1, lx + 89, cy + 12, TEXT); R(img, lx + 85, cy + 2, lx + 88, cy + 11, BTN)
        T(img, lx + 158, cy + 3, '14 Blöcke', TEXT, shadow=False); cy += 18
        Ts(img, lx, cy + 1, 'Abbau gilt, wenn du im Kreis stehst.', DIM); cy += 18

    label(img, lx, cy + 2, 'Notiz'); cy += 9
    field(img, lx, cy, lw, '', hint='optional, z. B. Eingang hinter der Mühle')

    # right column
    preview(img, rx, y + 24, rw, 106, cuboid)
    ty = y + 136
    label(img, rx, ty, 'Erholung'); ty += 9
    R(img, rx, ty, rx + rw, ty + 44, FIELD_EDGE); R(img, rx + 1, ty + 1, rx + rw - 1, ty + 43, FIELD)
    T(img, rx + 5, ty + 5, 'Standard für Eisenmine', TEXT, shadow=False)
    Ts(img, rx + 5, ty + 17, 'nach 2:00 h zu 70 % abbaubar', WARN)
    Ts(img, rx + 5, ty + 26, 'nach 4:00 h vollständig', READY)
    R(img, rx + 5, ty + 36, rx + rw - 5, ty + 38, FIELD_EDGE)
    R(img, rx + 5, ty + 36, rx + 5 + (rw - 10) // 2, ty + 38, WARN)
    R(img, rx + 5 + (rw - 10) // 2, ty + 36, rx + rw - 5, ty + 38, READY)
    ty += 48
    R(img, rx, ty + 1, rx + 9, ty + 10, FIELD_EDGE); R(img, rx + 1, ty + 2, rx + 8, ty + 9, FIELD)
    T(img, rx + 13, ty + 2, 'Eigene Zeit', DIM, shadow=False)
    field(img, rx + rw - 62, ty - 1, 62, '', hint='Minuten')

    divider(img, x + 2, x + pw - 2, y + ph - 25)
    by = y + ph - 19
    if not new:
        button(img, x + 10, by, 64, 'Löschen', col=argb(0xFF5A2A26), tc=argb(0xFFF0C8C2))
    Ts(img, x + (84 if not new else 10), by + 4, 'Sichtbar für alle mit dem Code der Liste.', DIM)
    button(img, x + pw - 10 - 70, by, 70, 'Speichern', col=argb(0xFF35552A), tc=argb(0xFFE4F2D8))
    button(img, x + pw - 10 - 70 - 4 - 66, by, 66, 'Abbrechen')
    return img


for name, cub, new in (('editor_punkt_neu', False, True), ('editor_litematica_bearbeiten', True, False)):
    editor(cub, new).save(os.path.join(OUT, name + '.png'))
print('ok')
