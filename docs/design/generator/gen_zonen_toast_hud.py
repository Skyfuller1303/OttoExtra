"""Zone notice sketches in the style of the animal info toast and the activity HUD (OttoExtra Resources).
Uses the measures and colours of AnimalToastCards (compact 196x30, detailed 160x62) and the activity HUD
panel. No text shadows. Usage: python -I gen_zonen_toast_hud.py <icon_dir> <wappen_dir> <out_dir>"""
import os, sys
_ARGS = sys.argv[1:4]
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_rpbuch2.py'), encoding='utf-8').read()
exec(_b[:_b.index("personen2().resize")])
from PIL import Image

ICONS, WAPPEN, OUT = _ARGS
os.makedirs(OUT, exist_ok=True)
S = 4


def argb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255, (v >> 24) & 255)


PANEL, OUTLINE = argb(0x961E1E24), argb(0xD7141418)
BEV_L, BEV_D = argb(0xD7514F59), argb(0xD72E2D35)
D2, D3 = argb(0xFF2E2D35), argb(0xFF3B3A45)
NAME, OCHRE, SUB, LABEL = argb(0xFFF0EBE0), argb(0xFFD6C194), argb(0xFFA9A6B0), argb(0xFF8F8C96)
READY = [argb(0xFF2F5426), argb(0xFF5E9A3E), argb(0xFF86C25A)]
WAIT = [argb(0xFF96752C), argb(0xFFCD9F3D), argb(0xFFDBB451)]
SOON = [argb(0xFF7A3A1E), argb(0xFFC0602E), argb(0xFFE08A4A)]
GREY = [argb(0xFF3B3A45), argb(0xFF514F59), argb(0xFFA9A6B0)]


def R(img, x0, y0, x1, y1, c):
    rect(img, x0 * S, y0 * S, x1 * S, y1 * S, c)


def T(img, x, y, s, c, small=False):
    text(img, x * S, y * S, s, c, 3 if small else S, False, False)


def tws(s):
    return round(tw(s) * .75)


def icon(img, name, x, y, folder=None):
    ic = Image.open(os.path.join(folder or ICONS, name)).convert('RGBA').resize((16 * S, 16 * S), Image.NEAREST)
    img.alpha_composite(ic, (x * S, y * S))


def panel(img, x, y, w, h):
    r, b = x + w, y + h
    for a in ((x + 1, y, r - 1, y + 1), (x + 1, b - 1, r - 1, b), (x, y + 1, x + 1, b - 1), (r - 1, y + 1, r, b - 1),
              (x + 1, y + 1, x + 2, y + 2), (r - 2, y + 1, r - 1, y + 2), (x + 1, b - 2, x + 2, b - 1),
              (r - 2, b - 2, r - 1, b - 1)):
        R(img, *a, OUTLINE)
    R(img, x + 2, y + 1, r - 2, y + 2, BEV_L); R(img, x + 1, y + 2, x + 2, b - 2, BEV_L)
    R(img, x + 2, b - 2, r - 2, b - 1, BEV_D); R(img, r - 2, y + 2, r - 1, b - 2, BEV_D)
    R(img, x + 2, y + 2, r - 2, b - 2, PANEL)


def portrait(img, x, y, name, folder=None):
    R(img, x - 2, y - 2, x + 18, y + 18, OUTLINE); R(img, x - 1, y - 1, x + 17, y + 17, D3)
    icon(img, name, x, y, folder)


def bar(img, x, y, w, h, frac, ramp):
    R(img, x, y, x + w, y + h, OUTLINE); R(img, x + 1, y + 1, x + w - 1, y + h - 1, D2)
    f = int((w - 2) * max(0, min(1, frac)))
    if f > 0:
        R(img, x + 1, y + 1, x + 1 + f, y + h - 1, ramp[1]); R(img, x + 1, y + 1, x + 1 + f, y + 2, ramp[2])


def compact(img, x, y, ore, name, status, ramp, pct, frac, right_small=None):
    """Like the animal 'Leiste': status stripe, portrait, name, status line, value + bar right."""
    w, h = 196, 30
    panel(img, x, y, w, h)
    R(img, x + 2, y + 2, x + 5, y + h - 2, ramp[1]); R(img, x + 2, y + 2, x + 5, y + 3, ramp[2])
    portrait(img, x + 9, y + 7, ore)
    T(img, x + 31, y + 6, name, OCHRE)
    T(img, x + 31, y + 17, status, ramp[2])
    rx = x + w - 52
    T(img, rx + 46 - tw(pct), y + 6, pct, NAME)
    bar(img, rx, y + 17, 46, 4, frac, ramp)
    if right_small:
        T(img, rx + 46 - tws(right_small), y + 23, right_small, LABEL, True)


def detailed(img, x, y, ore, header, ramp, name, sub, frac, caption, right_small, arms=None, gefolge=None):
    """Like the animal 'Siegel': status band, portrait, name, sub line, bar with caption."""
    w, h = 160, 62
    panel(img, x, y, w, h)
    R(img, x + 3, y + 3, x + w - 3, y + 15, ramp[0]); R(img, x + 3, y + 3, x + w - 3, y + 4, ramp[1])
    T(img, x + (w - tw(header)) // 2, y + 5, header, NAME)
    portrait(img, x + 8, y + 22, ore)
    T(img, x + 31, y + 21, name, OCHRE)
    T(img, x + 31, y + 31, sub, SUB)
    if arms:
        icon(img, arms, x + w - 22, y + 20, WAPPEN)
    bw = w - 40
    bar(img, x + 31, y + 44, bw, 5, frac, ramp)
    T(img, x + 31, y + 51, caption, LABEL)
    if right_small:
        T(img, x + 31 + bw - tws(right_small), y + 52, right_small, LABEL, True)


def scene(lbl):
    img = backdrop().resize((W * S, H * S), Image.NEAREST)
    R(img, 0, H - 14, W, H, (0, 0, 0, 150))
    T(img, 6, H - 11, lbl, NAME)
    return img


ARMS = sorted(os.listdir(WAPPEN))[0]
CX_C, CX_D = (W - 196) // 2, (W - 160) // 2

img = scene('F: Leiste wie Tier-Toast, Vorkommen erholt sich')
compact(img, CX_C, 8, 'eisenerz.png', 'Eisenmine Nordstollen', 'Erholt sich', WAIT, '70 %', .7, 'voll in 1:12 h')
img.save(os.path.join(OUT, 'toast_f_leiste.png'))

img = scene('G: Leiste, bereit')
compact(img, CX_C, 8, 'salzkristall.png', 'Salzmine Alter Schacht', 'Bereit zum Abbau', READY, '100 %', 1.0)
img.save(os.path.join(OUT, 'toast_g_leiste_bereit.png'))

img = scene('H: Siegel wie Tier-Toast, mit Wappen des Gefolges')
detailed(img, CX_D, 8, 'eisenerz.png', 'Erholt sich', WAIT, 'Eisenmine Nordst.', 'Eisen, Mine', .7,
         '70 % erholt', 'voll in 1:12 h', ARMS)
img.save(os.path.join(OUT, 'toast_h_siegel.png'))

img = scene('I: Siegel, bereit')
detailed(img, CX_D, 8, 'steinkohle.png', 'Bereit zum Abbau', READY, 'Kohlevorkommen', 'Kohle, Vorkommen', 1.0,
         'voll erholt', 'Schwarzhain', ARMS)
img.save(os.path.join(OUT, 'toast_i_siegel_bereit.png'))

img = scene('J: Leiste beim Verlassen, Timer gestartet')
compact(img, CX_C, 8, 'steinkohle.png', 'Kohlevorkommen Bach', 'Timer gestartet', SOON, '0 %', 0.0, 'voll in 4:00 h')
img.save(os.path.join(OUT, 'toast_j_leiste_verlassen.png'))

names = ['toast_f_leiste', 'toast_g_leiste_bereit', 'toast_h_siegel', 'toast_i_siegel_bereit', 'toast_j_leiste_verlassen']
ims = [Image.open(os.path.join(OUT, n + '.png')).crop((W * S // 4, 0, W * S * 3 // 4, 90 * S)) for n in names]
cw, ch = ims[0].size
sheet = Image.new('RGBA', (cw, ch * len(ims)), (0, 0, 0, 255))
for i, im in enumerate(ims):
    sheet.paste(im, (0, i * ch))
sheet.save(os.path.join(OUT, 'toast_hud_uebersicht.png'))
print('ok')
