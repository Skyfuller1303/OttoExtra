"""Idea sketches for the notice shown when a player enters a resource zone (OttoExtra Resources).
No text shadows. Usage: python -I gen_zonen_toast.py <icon_dir> <wappen_dir> <out_dir>"""
import os, sys
_ARGS = sys.argv[1:4]
ICONS, WAPPEN = _ARGS[0], _ARGS[1]
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_rpbuch2.py'), encoding='utf-8').read()
exec(_b[:_b.index("personen2().resize")])
from PIL import Image

os.makedirs(OUT, exist_ok=True)
S = 4
OUT = _ARGS[2]

PA, PA_L, PA_D, PA_S = hx('#C9A97C'), hx('#D8BC92'), hx('#B08F63'), hx('#BF9E70')
INK, INK_M, INK_F = hx('#35281C'), hx('#6E5A44'), hx('#8E785E')
TERRA, BRASS = hx('#9E4B32'), hx('#866C36')
OK, WARN, SOON = hx('#3F6B2C'), hx('#8A6410'), hx('#A0491E')
COVER = [hx('#141A21'), hx('#222B36'), hx('#313C49'), hx('#3B4756'), hx('#4F5C6D')]
D_PANEL, D_OUT, D_BEV = (30, 30, 36, 235), (20, 20, 24, 255), (81, 79, 89, 255)
D_TITLE, D_NAME, D_DIM = hx('#D6C194'), hx('#F0EBE0'), hx('#8F8C96')
D_READY, D_REGEN, D_SOON = hx('#86C25A'), hx('#D9B44A'), hx('#E08A4A')


def R(img, x0, y0, x1, y1, c):
    rect(img, x0 * S, y0 * S, x1 * S, y1 * S, c)


def T(img, x, y, s, c, small=False):
    text(img, x * S, y * S, s, c, 3 if small else S, False, False)


def tws(s):
    return round(tw(s) * .75)


def icon(img, name, x, y, k=1, folder=None):
    ic = Image.open(os.path.join(folder or ICONS, name)).convert('RGBA')
    ic = ic.resize((16 * k * S, 16 * k * S), Image.NEAREST)
    img.alpha_composite(ic, (x * S, y * S))


def bar(img, x, y, w, frac, col, track, h=3):
    R(img, x, y, x + w, y + h, track)
    if frac > 0:
        R(img, x, y, x + max(1, round(w * frac)), y + h, col)


def scene():
    img = backdrop().resize((W * S, H * S), Image.NEAREST)
    return img


def label(img, s):
    R(img, 0, H - 14, W, H, (0, 0, 0, 150))
    T(img, 6, H - 11, s, D_NAME)


def parchment_box(img, x, y, w, h, cover=True):
    if cover:
        R(img, x - 3, y - 3, x + w + 3, y + h + 3, COVER[0]); R(img, x - 2, y - 2, x + w + 2, y + h + 2, COVER[2])
        R(img, x - 2, y - 2, x + w + 2, y - 1, COVER[4])
    R(img, x, y, x + w, y + h, PA); R(img, x, y, x + w, y + 1, PA_L); R(img, x, y + h - 1, x + w, y + h, PA_D)


def dark_box(img, x, y, w, h):
    R(img, x, y, x + w, y + h, D_OUT); R(img, x + 1, y + 1, x + w - 1, y + h - 1, D_PANEL)
    R(img, x + 1, y + 1, x + w - 1, y + 2, D_BEV)


def variant_a():
    """Parchment card in book style, top centre."""
    img = scene()
    w, h = 190, 40; x, y = (W - w) // 2, 10
    parchment_box(img, x, y, w, h)
    icon(img, 'eisenerz.png', x + 6, y + 6, 2)
    T(img, x + 44, y + 5, 'Ressourcengebiet', INK_M, True)
    T(img, x + 44, y + 13, 'Eisenmine Nordstollen', INK)
    T(img, x + 44, y + 24, 'Eisen, Mine', INK_M, True)
    t = 'noch 1:35 h'
    T(img, x + w - 6 - tws(t), y + 24, t, WARN, True)
    bar(img, x + 44, y + 32, w - 50, .0, WARN, PA_D)
    label(img, 'A: Pergamentkarte oben mittig, Erholung als Balken')
    return img


def variant_b():
    """Ready state of A, plus list symbol and arms."""
    img = scene()
    w, h = 190, 40; x, y = (W - w) // 2, 10
    parchment_box(img, x, y, w, h)
    icon(img, 'eisenerz.png', x + 6, y + 6, 2)
    T(img, x + 44, y + 5, 'Ressourcengebiet', INK_M, True)
    wp = sorted(os.listdir(WAPPEN))[0]
    icon(img, wp, x + w - 22, y + 4, 1, WAPPEN)
    T(img, x + w - 24 - tws('Schwarzhain'), y + 5, 'Schwarzhain', INK_M, True)
    T(img, x + 44, y + 13, 'Eisenmine Nordstollen', INK)
    T(img, x + 44, y + 24, 'Eisen, Mine', INK_M, True)
    T(img, x + w - 6 - tws('bereit, abbauen'), y + 24, 'bereit, abbauen', OK, True)
    bar(img, x + 44, y + 32, w - 50, 1.0, OK, PA_D)
    label(img, 'B: wie A, bereit, mit Wappen des Gefolges')
    return img


def variant_c():
    """Slim dark ribbon, like the HUD, top centre under the boss bar position."""
    img = scene()
    w, h = 200, 20; x, y = (W - w) // 2, 8
    dark_box(img, x, y, w, h)
    icon(img, 'eisenerz.png', x + 3, y + 2)
    T(img, x + 23, y + 3, 'Eisenmine Nordstollen', D_NAME)
    t = '70 %, 0:42 h'
    T(img, x + w - 5 - tw(t), y + 3, t, D_REGEN)
    bar(img, x + 23, y + 14, w - 28, .7, D_REGEN, (20, 20, 24, 255), 2)
    label(img, 'C: schmales Band oben, dunkel wie das HUD')
    return img


def variant_d():
    """Left side card that slides in, stacked with the HUD language; shows timer action."""
    img = scene()
    w, h = 150, 50; x, y = 6, 70
    parchment_box(img, x, y, w, h)
    R(img, x, y, x + 3, y + h, TERRA)
    icon(img, 'salzkristall.png', x + 8, y + 6)
    T(img, x + 28, y + 6, 'Salzmine Alter Schacht', INK)
    T(img, x + 28, y + 16, 'Salz, Mine, Schwarzhain', INK_M, True)
    bar(img, x + 8, y + 27, w - 16, .7, WARN, PA_D)
    T(img, x + 8, y + 32, '70 % erholt', WARN, True)
    t = 'voll in 1:12 h'
    T(img, x + w - 8 - tws(t), y + 32, t, INK_M, True)
    T(img, x + 8, y + 40, 'Timer startet beim ersten Abbau.', INK_F, True)
    label(img, 'D: Karte links, mit Hinweis zum Timer')
    return img


def variant_e():
    """Unknown zone: entering an area that is in no list (nothing) vs leaving."""
    img = scene()
    w, h = 150, 32; x, y = (W - w) // 2, 10
    parchment_box(img, x, y, w, h)
    icon(img, 'steinkohle.png', x + 6, y + 7)
    T(img, x + 28, y + 5, 'Gebiet verlassen', INK_M, True)
    T(img, x + 28, y + 13, 'Kohlevorkommen Bach', INK)
    t = 'Timer läuft, 3:58 h'
    T(img, x + 28, y + 22, t, SOON, True)
    label(img, 'E: beim Verlassen nach Abbau: Timer gestartet')
    return img


for name, fn in (('toast_a_pergament', variant_a), ('toast_b_bereit_wappen', variant_b),
                 ('toast_c_band', variant_c), ('toast_d_links', variant_d), ('toast_e_verlassen', variant_e)):
    fn().save(os.path.join(OUT, name + '.png'))
print('ok')
