"""Mock-ups for a workshop side panel next to Ottonien crafting stations (first: Zeugtisch).

The left part is the real server GUI: a 6-row chest titled "Zeugtisch" drawn with the pack's generic_54.png
and the pack's item/GUI textures. The right part is the proposed OttoExtra panel in three variants
(A parchment column, B slate compact, C parchment with tabs) plus state images and an overview.
No text shadows. Minecraft pixel font from mcfont/ascii.png. GUI scale 3.

Known from live data (MinecraftAgent qa/0.4.6/live-recipes.json): slot 3 "Feuer machen" (3 x Stock,
+ 0:15 Brenndauer), slot 15 Eisenbarren, slot 24 Stahlbarren, slot 33 Gehaerteter Stahlbarren (icon count 2),
hotbar 1 = 45 Eisenbarren, hotbar 6 = 12 Stahlbarren. Everything else in the mock is an assumption.

Usage: python -I gen_werkstatt.py <pack_assets_dir> <out_dir>
  pack_assets_dir = Resourcepack/ottonien-reformed-update/assets
"""
import os, sys, random
from PIL import Image

ASSETS, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)
HERE = os.path.dirname(os.path.abspath(__file__))
K = 3  # GUI scale


def hx(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


# --- Ottonien Reformed palette (ottonien-pixelstil references/gui.md, existing OttoExtra mocks) ---
PA, PA_L, PA_S, PA_D = hx('#C9AA81'), hx('#D4B78C'), hx('#B8926E'), hx('#513E2C')
CORD = [hx('#382A25'), hx('#493626'), hx('#855C2C'), hx('#885633')]
INK_T, INK_M, INK_F = hx('#503D29'), hx('#7A6248'), hx('#9A8264')
TERRA = hx('#AA6841'); BRASS = hx('#866C36'); PICTO = hx('#51493A')
SL, SL_E, SL_S, SL_HOV, SL_DIS = hx('#222E3C'), hx('#344459'), hx('#1A2024'), hx('#323D4A'), hx('#141C24')
BLACK = hx('#000000'); GOLDH = hx('#E2A650'); OCHRE = hx('#D6C194'); NAME = hx('#F0EBE0')
SUB, LABEL, DIM = hx('#A9A6B0'), hx('#8F8C96'), hx('#5E5C66')
D1, D2, D3, D4, OUTL = hx('#1E1E24'), hx('#2E2D35'), hx('#3B3A45'), hx('#514F59'), hx('#141418')
OK_INK, WARN_INK, BAD_INK = hx('#3F6B2C'), hx('#8A6410'), hx('#963338')
FIRE = [hx('#7A3A1E'), hx('#C0602E'), hx('#E08A4A')]
LOW = [hx('#6E2226'), hx('#B24147'), hx('#C4545A')]
READY = [hx('#2F5426'), hx('#5E9A3E'), hx('#86C25A')]
GOLD = [hx('#96752C'), hx('#CD9F3D'), hx('#DBB451')]
TITLE = hx('#404040')

# --- font (Minecraft ascii.png, umlaut dots added like gen_toasts.py, no shadow) ---
FONT = Image.open(os.path.join(HERE, 'mcfont', 'ascii.png')).convert('RGBA')
UML = {'ä': 'a', 'ö': 'o', 'ü': 'u', 'Ä': 'A', 'Ö': 'O', 'Ü': 'U'}


def glyph(ch):
    o = ord(UML.get(ch, ch)); o = o if o < 256 else ord('?')
    return FONT.crop(((o % 16) * 8, (o // 16) * 8, (o % 16) * 8 + 8, (o // 16) * 8 + 8))


def cw(ch):
    if ch == ' ': return 4
    bb = glyph(ch).split()[3].getbbox(); return (bb[2] if bb else 0) + 1


def tw(s): return sum(cw(c) for c in s) - 1 if s else 0


def px(img, x, y, c):
    if 0 <= x < img.size[0] and 0 <= y < img.size[1]: img.alpha_composite(Image.new('RGBA', (1, 1), c), (x, y))


def rect(img, x0, y0, x1, y1, c):
    if x1 > x0 and y1 > y0: img.alpha_composite(Image.new('RGBA', (x1 - x0, y1 - y0), c), (x0, y0))


def T(img, x, y, s, c=INK_T, maxw=None, right=False, center=False):
    """Draw text without shadow. maxw guards against overflow (raises instead of clipping silently)."""
    w = tw(s)
    if maxw is not None and w > maxw:
        if os.environ.get('WERKSTATT_LAX'): print(f'TOO WIDE {s!r} {w} > {maxw}')
        else: raise ValueError(f'text too wide: {s!r} {w} > {maxw}')
    if right: x -= w
    if center: x -= w // 2
    xx = x
    for ch in s:
        if ch != ' ':
            g = glyph(ch); a = g.split()[3]
            img.alpha_composite(Image.composite(Image.new('RGBA', g.size, c), Image.new('RGBA', g.size, (0, 0, 0, 0)), a), (xx, y))
            if ch in UML:
                bb = a.getbbox(); dy = 0 if ch.islower() else -2
                px(img, xx + bb[0], y + dy, c); px(img, xx + bb[2] - 1, y + dy, c)
        xx += cw(ch)
    return w


def fmt(sec):
    return f'{sec // 60}:{sec % 60:02d}'


# --- textures ---
def tex(rel):
    return Image.open(os.path.join(ASSETS, rel)).convert('RGBA')


def item(name):
    return tex(f'ottonien/textures/item/{name}.png')


def put(img, name, x, y):
    img.alpha_composite(item(name), (x, y))


def count(img, x, y, n):
    """Stack count, bottom right of a 16x16 item (vanilla position, no shadow per the mock rules)."""
    if n > 1: T(img, x + 17, y + 9, str(n), hx('#FFFFFF'), right=True)


# --- backdrop: dimmed blocky world ---
def backdrop(w, h, seed=7, dim=180):
    random.seed(seed)
    img = Image.new('RGBA', (w, h), hx('#3A4A5A'))
    tones = [hx('#4E6B3A'), hx('#5B7A43'), hx('#6B6B6B'), hx('#7A7A72'), hx('#5A4A38'), hx('#6E5A44')]
    for by in range(0, h, 16):
        for bx in range(0, w, 16):
            c = tones[(bx // 16 * 3 + by // 16 * 5 + random.randint(0, 2)) % len(tones)] if by > h // 3 else hx('#5C7A96')
            rect(img, bx, by, bx + 16, by + 16, c)
            rect(img, bx, by + 15, bx + 16, by + 16, shade(c, .85))
    rect(img, 0, 0, w, h, (10, 10, 14, dim))
    return img


# --- the real server GUI: 6-row chest "Zeugtisch" ---
def slot_xy(i):
    if i < 54: return 8 + 18 * (i % 9), 18 + 18 * (i // 9)
    if i < 81: j = i - 54; return 8 + 18 * (j % 9), 139 + 18 * (j // 9)
    return 8 + 18 * (i - 81), 197


# (slot, texture, count). Slots 3, 15, 24, 33 and hotbar 82 / 87 are from the live snapshot, the rest is assumed.
SERVER = [(0, 'gui/barren', 1), (1, 'gui/zwischenprodukte', 1), (3, 'gui/feuer_machen_stock', 1),
          (4, 'gui/feuer_machen_steinkohle', 1), (5, 'gui/feuer_machen_torf', 1), (7, 'gui/zeugschmieden', 1),
          (8, 'gui/tipp', 1), (15, 'item/eisenbarren', 2), (24, 'item/stahlbarren', 2),
          (33, 'item/gehaerteter_stahlbarren', 2), (51, 'gui/weitere_anzeigen', 1)]
INV = {'stock': 23, 'eisenerz': 18, 'steinkohle': 9, 'torf': 3, 'eisenbarren': 45, 'stahlbarren': 12}


def player_items(inv):
    return [(81, 'item/stock', inv['stock']), (82, 'item/eisenbarren', inv['eisenbarren']),
            (87, 'item/stahlbarren', inv['stahlbarren']), (54, 'item/eisenerz', inv['eisenerz']),
            (55, 'item/steinkohle', inv['steinkohle']), (56, 'item/torf', inv['torf'])]


def chest(img, gx, gy, inv, target=None):
    img.alpha_composite(tex('minecraft/textures/gui/container/generic_54.png').crop((0, 0, 176, 222)), (gx, gy))
    T(img, gx + 8, gy + 6, 'Zeugtisch', TITLE)
    # The pack sets container.inventory to "" (de_de.json), so no "Inventar" label is drawn.
    for s, name, n in SERVER + player_items(inv):
        if name.startswith('item/') and inv.get(name[5:], 1) <= 0 and s >= 54: continue
        x, y = slot_xy(s); put(img, name, gx + x, gy + y); count(img, gx + x, gy + y, n)
    if target is not None:   # server slot the panel action would click
        x, y = slot_xy(target)
        rect(img, gx + x, gy + y, gx + x + 16, gy + y + 16, (255, 255, 255, 80))
        for a in ((x - 1, y - 1, x + 17, y), (x - 1, y + 16, x + 17, y + 17), (x - 1, y, x, y + 16), (x + 16, y, x + 17, y + 16)):
            rect(img, gx + a[0], gy + a[1], gx + a[2], gy + a[3], GOLDH)


# --- shared widgets ---
def cord_frame(img, x, y, w, h, t=3):
    for i in range(w):
        for j in range(h):
            e = min(i, j, w - 1 - i, h - 1 - j)
            if e >= t: continue
            c = CORD[0] if e == 0 else CORD[1] if e == t - 1 else (CORD[3] if (i + j) % 4 < 2 else CORD[2])
            px(img, x + i, y + j, c)


def parchment(img, x, y, w, h, seed=3):
    random.seed(seed)
    cord_frame(img, x, y, w, h)
    rect(img, x + 3, y + 3, x + w - 3, y + h - 3, PA)
    rect(img, x + 3, y + 3, x + w - 3, y + 4, PA_L); rect(img, x + 3, y + 3, x + 4, y + h - 3, PA_L)
    rect(img, x + 3, y + h - 4, x + w - 3, y + h - 3, PA_S); rect(img, x + w - 4, y + 3, x + w - 3, y + h - 3, PA_S)
    for xx in range(x + 5, x + w - 5):
        if random.random() < .08: px(img, xx, y + h - 4, PA_D)


def rule(img, x0, x1, y, c=PA_S): rect(img, x0, y, x1, y + 1, c)


def slate_btn(img, x, y, w, h, label, hover=False, disabled=False):
    if disabled:
        rect(img, x + 1, y, x + w - 1, y + h, BLACK); rect(img, x, y + 1, x + w, y + h - 1, BLACK)
        rect(img, x + 1, y + 1, x + w - 1, y + h - 1, SL_DIS)
        T(img, x + w // 2, y + (h - 8) // 2 + 1, label, DIM, maxw=w - 4, center=True); return
    o = GOLDH if hover else BLACK
    rect(img, x + 1, y, x + w - 1, y + h, o); rect(img, x, y + 1, x + w, y + h - 1, o)
    rect(img, x + 1, y + 1, x + w - 1, y + h - 1, SL_HOV if hover else SL)
    rect(img, x + 1, y + 1, x + w - 1, y + 2, SL_E); rect(img, x + 1, y + h - 2, x + w - 1, y + h - 1, SL_S)
    T(img, x + w // 2, y + (h - 8) // 2 + 1, label, GOLDH if hover else NAME, maxw=w - 4, center=True)


def checkbox(img, x, y, on, disabled=False):
    rect(img, x, y, x + 9, y + 9, DIM if disabled else BLACK)
    rect(img, x + 1, y + 1, x + 8, y + 8, SL_DIS if not disabled else hx('#2A2A30'))
    if on:
        for a, b in ((2, 4), (3, 5), (4, 6), (5, 5), (6, 4), (7, 3)): px(img, x + a, y + b, NAME)


def lock(img, x, y, c):
    rect(img, x + 1, y, x + 5, y + 1, c); rect(img, x + 1, y, x + 2, y + 3, c); rect(img, x + 4, y, x + 5, y + 3, c)
    rect(img, x, y + 3, x + 6, y + 8, c)


def flame(img, x, y, ramp):
    shape = ['...o...', '..oo...', '..ooo..', '.oooo..', '.ooiio.', 'ooiiioo', 'oiiiiio', '.ooooo.']
    for j, row in enumerate(shape):
        for i, ch in enumerate(row):
            if ch == 'o': px(img, x + i, y + j, ramp[1])
            elif ch == 'i': px(img, x + i, y + j, ramp[2])


def bar(img, x, y, w, h, frac, ramp, back=PA_D, edge=None):
    rect(img, x, y, x + w, y + h, edge or back)
    rect(img, x + 1, y + 1, x + w - 1, y + h - 1, back if edge else shade(back, 1.3))
    f = int((w - 2) * max(0, min(1, frac)))
    if f > 0:
        rect(img, x + 1, y + 1, x + 1 + f, y + h - 1, ramp[1]); rect(img, x + 1, y + 1, x + 1 + f, y + 2, ramp[2])


def slot_box(img, x, y, sel=False, dark=True):
    """18x18 slot frame for the slate variant (item at x+1, y+1)."""
    rect(img, x, y, x + 18, y + 18, GOLDH if sel else OUTL)
    rect(img, x + 1, y + 1, x + 17, y + 17, D3 if dark else PA_S)


def check(img, x, y, c):
    for a, b in ((0, 3), (1, 4), (2, 5), (3, 4), (4, 3), (5, 2), (6, 1)): px(img, x + a, y + b, c); px(img, x + a, y + b + 1, c)


def cross(img, x, y, c):
    for i in range(6): px(img, x + i, y + i, c); px(img, x + 5 - i, y + i, c)


def cursor(img, x, y):
    shape = ['b.........', 'bb........', 'bwb.......', 'bwwb......', 'bwwwb.....', 'bwwwwb....', 'bwwwwwb...',
             'bwwwwwwb..', 'bwwwbbbb..', 'bwbwb.....', 'bb.bwb....', '....bwb...', '.....bb...']
    for j, row in enumerate(shape):
        for i, ch in enumerate(row):
            if ch == 'b': px(img, x + i, y + j, BLACK)
            elif ch == 'w': px(img, x + i, y + j, hx('#FFFFFF'))


def tooltip(img, x, y, lines):
    """Pack tooltip: near-black field, grey-violet frame with cut corners, ochre title, no shadow."""
    w = max(tw(s) for s, _ in lines) + 8; h = len(lines) * 10 + 5
    rect(img, x + 1, y, x + w - 1, y + h, hx('#18181C')); rect(img, x, y + 1, x + w, y + h - 1, hx('#18181C'))
    for a in ((x + 1, y + 1, x + w - 1, y + 2), (x + 1, y + h - 2, x + w - 1, y + h - 1), (x + 1, y + 1, x + 2, y + h - 1),
              (x + w - 2, y + 1, x + w - 1, y + h - 1)):
        rect(img, *a, D4)
    for i, (s, c) in enumerate(lines):
        T(img, x + 4, y + 4 + i * 10, s, c)
    return w, h


# --- data ---
# (label, texture, items per click, seconds per click, assumed?)
FUELS = [('Stock', 'stock', 3, 15, False), ('Steinkohle', 'steinkohle', 1, 40, True), ('Torf', 'torf', 1, 60, True)]
# (label, texture, short ingredients, needs per batch {item: n}, fuel per batch (coal, peat))
RECIPES = [('Eisenbarren', 'eisenbarren', '4 Erz, 2 Kohle', {'eisenerz': 4}, (2, 1)),
           ('Stahlbarren', 'stahlbarren', '4 Eisen, 4 Kohle', {'eisenbarren': 4}, (4, 2)),
           ('Geh. Stahlbarren', 'gehaerteter_stahlbarren', '4 Stahl, 6 Kohle', {'stahlbarren': 4}, (6, 3))]


def fuel_total(inv, f):
    label, key, per, sec, _ = f
    n = inv[key] // per
    return n, n * sec


def batches(inv, r):
    _, _, _, need, (coal, peat) = r
    n = min(inv[k] // v for k, v in need.items())
    fuel = inv['steinkohle'] // coal + inv['torf'] // peat
    return min(n, fuel)


def missing(inv, r):
    _, _, _, need, (coal, peat) = r
    for k, v in need.items():
        if inv[k] < v: return f'fehlt {v - inv[k]} ' + {'eisenerz': 'Erz', 'eisenbarren': 'Eisen', 'stahlbarren': 'Stahl'}[k]
    if inv['steinkohle'] < coal and inv['torf'] < peat: return 'fehlt Kohle'
    return None


# ============ Variant A: parchment column ============
def panel_a(img, x, y, inv, fire=12, fire_max=15, fuel_sel=0, hover=None, crafting=None, fuel_hint=False):
    w, h = 136, 222
    parchment(img, x, y, w, h, seed=5)
    ix, iw = x + 7, w - 14
    # fire status
    T(img, ix, y + 7, 'Brennstoff', INK_T)
    low = fire <= 5
    ramp = LOW if low else FIRE
    tcol = BAD_INK if low else INK_T
    tstr = fmt(fire) if fire > 0 else 'aus'
    tx = T(img, ix + iw, y + 7, tstr, tcol, right=True)
    flame(img, ix + iw - tx - 10, y + 7, ramp)
    bar(img, ix, y + 17, iw, 4, fire / fire_max, ramp)
    # fuels
    cy = y + 24
    for i, f in enumerate(FUELS):
        label, key, per, sec, assumed = f
        n, total = fuel_total(inv, f)
        if i == fuel_sel:
            rect(img, ix - 2, cy - 1, ix + iw + 1, cy + 18, PA_L); rect(img, ix - 2, cy - 1, ix, cy + 18, TERRA)
        put(img, f'item/{key}', ix + 2, cy)
        T(img, ix + 21, cy, label, INK_T, maxw=60)
        T(img, ix + 21, cy + 9, f'{per} = {fmt(sec)}' + ('*' if assumed else ''), INK_M, maxw=54)
        T(img, ix + iw, cy, f'{inv[key]}', INK_T, right=True)
        T(img, ix + iw, cy + 9, fmt(total) + ('*' if assumed else ''), OK_INK if n else BAD_INK, right=True)
        if i < 2: rule(img, ix, ix + iw, cy + 18)
        cy += 19
    bw = (iw - 3) // 2
    slate_btn(img, ix, cy + 2, bw, 16, 'Nachlegen', hover=fuel_hint)
    slate_btn(img, ix + bw + 3, cy + 2, bw, 16, 'Auffüllen')
    # recipes
    cy += 23
    T(img, ix, cy, 'Rezepte', INK_T)
    T(img, ix + iw, cy, 'L 1x  R alle', INK_F, right=True)
    rule(img, ix, ix + iw, cy + 10, PA_D)
    cy += 13
    for i, r in enumerate(RECIPES):
        label, key, short, need, fuel = r
        b = batches(inv, r); miss = missing(inv, r)
        if hover == i:
            rect(img, ix - 2, cy - 1, ix + iw + 2, cy + 19, GOLDH); rect(img, ix - 1, cy, ix + iw + 1, cy + 18, PA_L)
        put(img, f'item/{key}', ix + 1, cy + 1)
        if miss:
            rect(img, ix + 1, cy + 1, ix + 17, cy + 17, (201, 170, 129, 140))
        T(img, ix + 20, cy + 1, label, INK_T if not miss else INK_M, maxw=iw - 34)
        if crafting and crafting[0] == i:
            bar(img, ix + 20, cy + 11, iw - 36, 4, crafting[1], GOLD if crafting[1] < 1 else READY)
        else:
            T(img, ix + 20, cy + 10, miss or short, BAD_INK if miss else INK_M, maxw=iw - 34)
        T(img, ix + iw - 1, cy + 5, f'{b}x', OK_INK if b else BAD_INK, right=True)
        cy += 21
    # long run, separated and disabled
    assert cy <= y + h - 41, cy - y
    longrun(img, ix, iw, y + h - 41)
    return w, h


def longrun(img, ix, iw, cy):
    """Separated, disabled 'Dauerbetrieb' toggle with the rules hint (parchment variants)."""
    rule(img, ix, ix + iw, cy, PA_D)
    checkbox(img, ix, cy + 5, False, disabled=True)
    T(img, ix + 13, cy + 6, 'Dauerbetrieb', INK_F)
    lock(img, ix + iw - 6, cy + 6, INK_F)
    T(img, ix, cy + 17, 'nur falls Serverregeln', INK_F, maxw=iw)
    T(img, ix, cy + 26, 'es erlauben', INK_F, maxw=iw)


# ============ Variant B: slate compact ============
def slate_panel(img, x, y, w, h):
    r, b = x + w, y + h
    rect(img, x + 1, y, r - 1, b, OUTL); rect(img, x, y + 1, r, b - 1, OUTL)
    rect(img, x + 1, y + 1, r - 1, b - 1, D1)
    rect(img, x + 2, y + 1, r - 2, y + 2, D4); rect(img, x + 1, y + 2, x + 2, b - 2, D4)
    rect(img, x + 2, b - 2, r - 2, b - 1, D2); rect(img, r - 2, y + 2, r - 1, b - 2, D2)


def panel_b(img, x, y, inv, fire=12, fire_max=15, fuel_sel=0):
    w, h = 130, 210
    slate_panel(img, x, y, w, h)
    ix, iw = x + 6, w - 12
    T(img, ix, y + 6, 'Zeugtisch', OCHRE)
    tx = T(img, ix + iw, y + 6, fmt(fire), NAME, right=True)
    flame(img, ix + iw - tx - 10, y + 6, FIRE)
    bar(img, ix, y + 16, iw, 4, fire / fire_max, FIRE, back=D2, edge=OUTL)
    cy = y + 25
    cwid = iw // 3
    for i, f in enumerate(FUELS):
        label, key, per, sec, assumed = f
        cx = ix + i * cwid + (cwid - 18) // 2
        slot_box(img, cx, cy, sel=i == fuel_sel)
        put(img, f'item/{key}', cx + 1, cy + 1); count(img, cx + 1, cy + 1, inv[key])
        T(img, ix + i * cwid + cwid // 2, cy + 21, fmt(sec) + ('*' if assumed else ''), NAME if i == fuel_sel else LABEL,
          center=True, maxw=cwid)
    cy += 32
    n, total = fuel_total(inv, FUELS[fuel_sel])
    T(img, ix, cy, f'Vorrat {n}x', SUB); T(img, ix + iw, cy, fmt(total), READY[2], right=True)
    cy += 11
    bw = (iw - 3) // 2
    slate_btn(img, ix, cy, bw, 16, 'Nachlegen'); slate_btn(img, ix + bw + 3, cy, bw, 16, 'Auffüllen')
    cy += 22
    rule(img, ix, ix + iw, cy, D3)
    T(img, ix, cy + 4, 'Rezepte', OCHRE); T(img, ix + iw, cy + 4, 'L 1x R alle', LABEL, right=True)
    cy += 15
    for r in RECIPES:
        label, key, short, need, fuel = r
        b = batches(inv, r)
        slot_box(img, ix, cy); put(img, f'item/{key}', ix + 1, cy + 1)
        T(img, ix + 22, cy + 1, label, NAME, maxw=iw - 22)
        T(img, ix + 22, cy + 10, short, LABEL, maxw=iw - 22 - 15)
        T(img, ix + iw, cy + 10, f'{b}x', READY[2] if b else LOW[2], right=True)
        cy += 21
    rule(img, ix, ix + iw, cy + 1, D3)
    checkbox(img, ix, cy + 6, False, disabled=True)
    T(img, ix + 13, cy + 7, 'Dauerbetrieb', DIM)
    lock(img, ix + iw - 6, cy + 7, DIM)
    T(img, ix, cy + 18, 'nur falls Serverregeln', DIM, maxw=iw)
    T(img, ix, cy + 27, 'es erlauben', DIM, maxw=iw)
    return w, h


# ============ Variant C: parchment with ribbon tabs ============
def ribbon(img, x, y_bottom, w, h, c, label, active):
    dark, light = shade(c, .65), shade(c, 1.2)
    rect(img, x, y_bottom - h, x + w, y_bottom, dark)
    rect(img, x + 1, y_bottom - h + 1, x + w - 1, y_bottom, c)
    rect(img, x + 1, y_bottom - h + 1, x + w - 1, y_bottom - h + 2, light)
    T(img, x + w // 2, y_bottom - h + 3, label, NAME if active else OCHRE, center=True, maxw=w - 4)


def panel_c(img, x, y, inv, tab=0, fire=12, fire_max=15, fuel_sel=0):
    w, h = 136, 208
    tabs = (('Brennstoff', TERRA), ('Rezepte', BRASS))
    tx = x + 6
    for i, (lab, c) in enumerate(tabs):
        tw_ = tw(lab) + 10
        ribbon(img, tx, y + 14, tw_, 16 if i == tab else 13, c if i == tab else shade(c, .8), lab, i == tab)
        tx += tw_ + 3
    y += 14
    parchment(img, x, y, w, h, seed=9)
    rect(img, x + 6 + (0 if tab == 0 else tw(tabs[0][0]) + 13), y, x + 6 + (tw(tabs[0][0]) + 10 if tab == 0 else
         tw(tabs[0][0]) + 13 + tw(tabs[1][0]) + 10), y + 3, tabs[tab][1])
    ix, iw = x + 7, w - 14
    if tab == 0:
        flame(img, ix, y + 7, FIRE)
        T(img, ix + 10, y + 7, f'Feuer {fmt(fire)} übrig', INK_T, maxw=iw - 10)
        bar(img, ix, y + 17, iw, 5, fire / fire_max, FIRE)
        cy = y + 26
        cwid = iw // 3
        for i, f in enumerate(FUELS):
            label, key, per, sec, assumed = f
            cx = ix + i * cwid
            if i == fuel_sel:
                rect(img, cx, cy, cx + cwid - 2, cy + 30, PA_L); rect(img, cx, cy + 28, cx + cwid - 2, cy + 30, TERRA)
            put(img, f'item/{key}', cx + (cwid - 2 - 16) // 2, cy + 2)
            T(img, cx + (cwid - 2) // 2, cy + 19, fmt(sec) + ('*' if assumed else ''), INK_T if i == fuel_sel else INK_M,
              center=True, maxw=cwid - 2)
        cy += 35
        label, key, per, sec, assumed = FUELS[fuel_sel]
        n, total = fuel_total(inv, FUELS[fuel_sel])
        T(img, ix, cy, label, INK_T)
        T(img, ix, cy + 10, f'{per} je Feuer, +{fmt(sec)}', INK_M, maxw=iw)
        T(img, ix, cy + 20, f'Vorrat {inv[key]} = {n}x', INK_M); T(img, ix + iw, cy + 20, fmt(total), OK_INK, right=True)
        cy += 32
        bw = (iw - 3) // 2
        slate_btn(img, ix, cy, bw, 16, 'Nachlegen'); slate_btn(img, ix + bw + 3, cy, bw, 16, 'Auffüllen')
        cy += 23
        T(img, ix, cy, 'Hinweise', INK_T); rule(img, ix, ix + iw, cy + 10, PA_D)
        checkbox(img, ix, cy + 14, True); T(img, ix + 13, cy + 15, 'Feuer unter 0:05', INK_T, maxw=iw - 13)
        checkbox(img, ix, cy + 26, True); T(img, ix + 13, cy + 27, 'Ergebnis fertig', INK_T, maxw=iw - 13)
    else:
        cy = y + 7
        for i, r in enumerate(RECIPES):
            label, key, short, need, (coal, peat) = r
            b = batches(inv, r)
            put(img, f'item/{key}', ix, cy)
            T(img, ix + 19, cy, label, INK_T, maxw=iw - 19)
            T(img, ix + 19, cy + 9, short, INK_M, maxw=iw - 19)
            T(img, ix + 19, cy + 18, f'oder {peat} Torf', INK_F, maxw=iw - 19)
            bw = (iw - 19 - 3) // 2
            slate_btn(img, ix + 19, cy + 28, bw, 14, '1x', hover=i == 0)
            slate_btn(img, ix + 19 + bw + 3, cy + 28, bw, 14, f'alle ({b})' if b else 'alle', disabled=not b)
            if i < 2: rule(img, ix, ix + iw, cy + 46)
            cy += 50
    longrun(img, ix, iw, y + h - 41)
    return w, h + 14


# ============ notices (strip above the panel) ============
def notice(img, x, y, w, kind, text_):
    ramp = {'low': LOW, 'ready': READY, 'miss': FIRE}[kind]
    rect(img, x + 1, y, x + w - 1, y + 16, OUTL); rect(img, x, y + 1, x + w, y + 15, OUTL)
    rect(img, x + 1, y + 1, x + w - 1, y + 15, D1)
    rect(img, x + 1, y + 1, x + 4, y + 15, ramp[1]); rect(img, x + 1, y + 1, x + 4, y + 2, ramp[2])
    if kind == 'low': flame(img, x + 8, y + 4, LOW)
    elif kind == 'ready': check(img, x + 7, y + 4, READY[2])
    else: cross(img, x + 8, y + 5, FIRE[2])
    T(img, x + 19, y + 5, text_, NAME, maxw=w - 22)


# ============ scenes ============
MX, MT, MB = 10, 26, 8
GW, GH = 176, 222
CAP = 14


def scene(panel_w, caption, extra_w=0):
    W = MX + GW + 4 + panel_w + MX + extra_w
    H = MT + GH + MB + CAP
    img = backdrop(W, H)
    rect(img, 0, H - CAP, W, H, (0, 0, 0, 200))
    T(img, 6, H - CAP + 3, caption, NAME, maxw=W - 12)
    return img


def save(img, name):
    img.resize((img.size[0] * K, img.size[1] * K), Image.NEAREST).save(os.path.join(OUT, name))
    return img


PX = MX + GW + 4
LOGICAL = {}

img = scene(136, 'A: Pergament-Spalte, Werte aus Inventar. * = Annahme')
chest(img, MX, MT, INV); panel_a(img, PX, MT, INV)
LOGICAL['A'] = save(img, 'werkstatt_a_pergament.png')

img = scene(130, 'B: Schiefer kompakt. * = Annahme')
chest(img, MX, MT, INV); panel_b(img, PX, MT, INV)
LOGICAL['B'] = save(img, 'werkstatt_b_schiefer.png')

img = scene(136, 'C: Reiter Brennstoff/Rezepte. * = Annahme')
chest(img, MX, MT, INV); panel_c(img, PX, MT - 14, INV, tab=0)
LOGICAL['C'] = save(img, 'werkstatt_c_reiter.png')

img = scene(136, 'C: Reiter Rezepte, Knöpfe 1x / alle')
chest(img, MX, MT, INV, target=15); panel_c(img, PX, MT - 14, INV, tab=1)
save(img, 'werkstatt_c_reiter_rezepte.png')

# states (variant A)
img = scene(136, 'Feuer fast aus, Nachlegen klickt Slot 3')
chest(img, MX, MT, INV, target=3); panel_a(img, PX, MT, INV, fire=4, fuel_hint=True)
notice(img, PX, MT - 20, 136, 'low', 'Feuer fast aus: 0:04')
save(img, 'werkstatt_zustand_feuer_niedrig.png')

img = scene(136, 'Ergebnis fertig, aus der Aktionsleiste')
inv2 = dict(INV, eisenerz=14, steinkohle=7, eisenbarren=47)
chest(img, MX, MT, inv2); panel_a(img, PX, MT, inv2, fire=9, crafting=(0, 1.0))
notice(img, PX, MT - 20, 136, 'ready', 'Eisenbarren fertig: +2')
save(img, 'werkstatt_zustand_fertig.png')

img = scene(136, 'Zustand: Rezept-Hover, Tooltip mit Links/Rechts, Ziel-Slot 15 markiert', extra_w=128)
chest(img, MX, MT, INV, target=15); panel_a(img, PX, MT, INV, hover=0)
row_y = MT + 24 + 3 * 19 + 23 + 13   # first recipe row in panel_a
cursor(img, PX + 7 + 95, row_y + 4)
tooltip(img, PX + 136 + 4, row_y - 6, [('Eisenbarren', OCHRE), ('Linksklick: 1x herstellen', NAME),
                                      ('Rechtsklick: alle (4x)', NAME), ('Je Mal: 4 Eisenerz', SUB),
                                      ('2 Steinkohle oder 1 Torf', SUB), ('Benötigt: Hitze', SUB),
                                      ('Klickt Server-Slot 15', LABEL)])
save(img, 'werkstatt_zustand_hover.png')

img = scene(136, 'Zustand: zu wenig Material, Rezept gesperrt, Zeile zeigt was fehlt', extra_w=128)
inv3 = dict(INV, eisenerz=2)
chest(img, MX, MT, inv3); panel_a(img, PX, MT, inv3, hover=0)
notice(img, PX, MT - 20, 136, 'miss', 'Zu wenig Eisenerz: 2/4')
cursor(img, PX + 7 + 95, row_y + 4)
tooltip(img, PX + 136 + 4, row_y - 6, [('Eisenbarren', OCHRE), ('Nicht genug Material', LOW[2]),
                                      ('Eisenerz: 2 von 4', SUB), ('Steinkohle: 9, reicht', SUB),
                                      ('Klick tut nichts', LABEL)])
save(img, 'werkstatt_zustand_material_fehlt.png')

# overview of the first concept (per-click buttons): A, B, C side by side at GUI scale 2
ims = [LOGICAL[k] for k in 'ABC']
gap = 6
OW = sum(i.size[0] for i in ims) + gap * (len(ims) + 1)
OH = max(i.size[1] for i in ims) + gap * 2
ov = Image.new('RGBA', (OW, OH), hx('#101014'))
cx = gap
for i in ims:
    ov.alpha_composite(i, (cx, gap)); cx += i.size[0] + gap
ov.resize((OW * 2, OH * 2), Image.NEAREST).save(os.path.join(OUT, 'werkstatt_uebersicht_klick.png'))


# =====================================================================================
# Concept 2 "Auftrag" (job): choose recipe, set quantity, choose fuel, start. The job then
# clicks the server buttons itself, re-opens the table after every recipe (the server closes it)
# and stops on fixed safety conditions. No randomised timing or other concealment.
# =====================================================================================
import math

FIRE_PER_CRAFT = 9          # * assumed seconds of fire one craft uses (live lore gives no duration)
YIELD = 2                   # * assumed output per craft (recipe icon count is 2)
JOB_INV = {'stock': 30, 'eisenerz': 96, 'steinkohle': 44, 'torf': 3, 'eisenbarren': 45, 'stahlbarren': 12}
FUEL_KEY = {0: 'stock', 1: 'steinkohle', 2: 'torf'}
NICE = {'eisenerz': 'Eisenerz', 'eisenbarren': 'Eisenbarren', 'stahlbarren': 'Stahlbarren',
        'steinkohle': 'Steinkohle', 'torf': 'Torf', 'stock': 'Stock'}


def job_need(r, n, fuel):
    """Materials for n crafts of recipe r with fuel index; recipe coal and fire fuel are added up."""
    _, _, _, need, (coal, peat) = r
    out = {k: v * n for k, v in need.items()}
    out['steinkohle'] = out.get('steinkohle', 0) + coal * n          # recipe uses coal (peat as alternative)
    label, key, per, sec, _ = FUELS[fuel]
    fires = math.ceil(n * FIRE_PER_CRAFT / sec)
    out[key] = out.get(key, 0) + fires * per
    return out, fires


def job_max(inv, r, fuel):
    n = 0
    while n < 999:
        need, _ = job_need(r, n + 1, fuel)
        if any(inv[k] < v for k, v in need.items()): break
        n += 1
    return n


def box18(img, x, y, sel=False):
    rect(img, x + 1, y, x + 17, y + 18, GOLDH if sel else PA_D); rect(img, x, y + 1, x + 18, y + 17, GOLDH if sel else PA_D)
    rect(img, x + 1, y + 1, x + 17, y + 17, PA_L if sel else PA)
    if not sel: rect(img, x + 1, y + 16, x + 17, y + 17, PA_S)


def small_btn(img, x, y, w, h, label, hover=False):
    slate_btn(img, x, y, w, h, label, hover=hover)


def num_field(img, x, y, w, h, value):
    rect(img, x, y, x + w, y + h, BLACK); rect(img, x + 1, y + 1, x + w - 1, y + h - 1, SL_DIS)
    tx = T(img, x + w // 2, y + (h - 8) // 2 + 1, value, NAME, center=True, maxw=w - 6)
    rect(img, x + w // 2 + tx - tx // 2 + 1, y + 3, x + w // 2 + tx - tx // 2 + 2, y + h - 3, OCHRE)   # caret


def red_btn(img, x, y, w, h, label):
    rect(img, x + 1, y, x + w - 1, y + h, BLACK); rect(img, x, y + 1, x + w, y + h - 1, BLACK)
    rect(img, x + 1, y + 1, x + w - 1, y + h - 1, hx('#8A2C30'))
    rect(img, x + 1, y + 1, x + w - 1, y + 2, LOW[1]); rect(img, x + 1, y + h - 2, x + w - 1, y + h - 1, LOW[0])
    T(img, x + w // 2, y + (h - 8) // 2 + 1, label, NAME, center=True, maxw=w - 4)


def recipe_picker(img, ix, iw, y, sel):
    for i, r in enumerate(RECIPES):
        box18(img, ix + i * 20, y, sel == i); put(img, f'item/{r[1]}', ix + i * 20 + 1, y + 1)
    nx = ix + 3 * 20 + 3
    T(img, nx, y + 1, RECIPES[sel][0], INK_T, maxw=ix + iw - nx)
    T(img, nx, y + 10, f'{YIELD} Stück/Mal*', INK_M, maxw=ix + iw - nx)


def qty_row(img, ix, iw, y, n, mx):
    T(img, ix, y + 3, 'Menge', INK_T)
    x = ix + 33
    small_btn(img, x, y, 13, 14, '-'); x += 15
    num_field(img, x, y, 28, 14, str(n)); x += 30
    small_btn(img, x, y, 13, 14, '+'); x += 16
    small_btn(img, x, y, ix + iw - x, 14, f'Max {mx}')
    T(img, ix + 33, y + 17, 'Shift + Klick: 10er', INK_F, maxw=iw - 33)


def need_lines(img, ix, iw, y, inv, need, fuel_key):
    for k, v in need.items():
        have = inv[k]
        name = NICE[k] + (' (Feuer)' if k == fuel_key and k != 'steinkohle' else '')
        if k == 'steinkohle' and fuel_key == 'steinkohle': name = 'Steinkohle (+Feuer)'
        ok = have >= v
        val = f'{v} / {have}' if ok else f'{v} / {have}, fehlt {v - have}'
        if tw(name) + tw(val) + 4 > iw: name = NICE[k]
        T(img, ix, y, name, INK_M, maxw=iw - tw(val) - 4)
        T(img, ix + iw, y, val, OK_INK if ok else BAD_INK, right=True)
        y += 10
    return y


def fuel_chips(img, ix, iw, y, sel, inv):
    for i, f in enumerate(FUELS):
        box18(img, ix + i * 20, y, sel == i); put(img, f'item/{f[1]}', ix + i * 20 + 1, y + 1)
    nx = ix + 63
    label, key, per, sec, assumed = FUELS[sel]
    T(img, nx, y + 1, label, INK_T, maxw=ix + iw - nx)
    T(img, nx, y + 10, f'{per} = {fmt(sec)}' + ('*' if assumed else ''), INK_M, maxw=ix + iw - nx)


def safety_lines(img, ix, iw, y):
    """Fixed stop rules, shown as text (variant A)."""
    T(img, ix, y, 'Stopp: Weggehen, Schaden,', INK_F, maxw=iw)
    T(img, ix, y + 9, 'andere GUI, leer, voll,', INK_F, maxw=iw)
    T(img, ix, y + 18, 'Chat !stop*, kein Fokus', INK_F, maxw=iw)


def safety_strip(img, ix, iw, y):
    """Stop conditions as checkboxes (variant C)."""
    rule(img, ix, ix + iw, y, PA_D)
    T(img, ix, y + 3, 'Stoppt bei', INK_T); T(img, ix + iw, y + 3, 'feste Takte', INK_F, right=True)
    col = iw // 2
    items = [('Weggehen', True), ('Schaden', True), ('anderer GUI', True), ('leer / voll', True),
             ('Chat !stop*', True), ('Fokus weg', True)]
    for i, (lab, on) in enumerate(items):
        cx, cy = ix + (i % 2) * col, y + 14 + (i // 2) * 11
        checkbox(img, cx, cy, on); T(img, cx + 12, cy + 1, lab, INK_T, maxw=col - 13)


def auftrag_a(img, x, y, inv, sel=0, n=20, fuel=0):
    w, h = 150, 256
    parchment(img, x, y, w, h, seed=13)
    ix, iw = x + 7, w - 14
    r = RECIPES[sel]
    T(img, ix, y + 7, 'Auftrag', INK_T)
    tx = T(img, ix + iw, y + 7, '0:12', INK_T, right=True); flame(img, ix + iw - tx - 10, y + 7, FIRE)
    recipe_picker(img, ix, iw, y + 19, sel)
    mx = job_max(inv, r, fuel)
    qty_row(img, ix, iw, y + 43, n, mx)
    rule(img, ix, ix + iw, y + 71, PA_D)
    need, fires = job_need(r, n, fuel)
    T(img, ix, y + 74, f'Braucht für {n}x', INK_T)
    cy = need_lines(img, ix, iw, y + 85, inv, need, FUEL_KEY[fuel])
    T(img, ix, cy + 1, f'{fires} Feuer* = {fires * FUELS[fuel][2]} {FUELS[fuel][0]}', INK_M, maxw=iw)
    cy += 13
    rule(img, ix, ix + iw, cy, PA_D)
    T(img, ix, cy + 3, 'Brennstoff', INK_T)
    fuel_chips(img, ix, iw, cy + 13, fuel, inv)
    cy += 35
    checkbox(img, ix, cy, True); T(img, ix + 12, cy + 1, 'Feuer auto. nachlegen', INK_T, maxw=iw - 12)
    cy += 12
    short = [k for k, v in need.items() if inv[k] < v]
    if short:
        T(img, ix, cy, f'{NICE[short[0]]} reicht nur für {mx}x', BAD_INK, maxw=iw)
    cy += 11
    slate_btn(img, ix, cy, iw - 21, 16, f'Herstellen ({n}x)', disabled=bool(short))
    slate_btn(img, ix + iw - 18, cy, 18, 16, '+')
    assert cy + 16 <= y + h - 42, cy - y
    rule(img, ix, ix + iw, y + h - 38, PA_D)
    safety_lines(img, ix, iw, y + h - 34)
    return w, h


def auftrag_laeuft(img, x, y, inv, done=6, n=20, step=1, fire=9):
    w, h = 150, 256
    parchment(img, x, y, w, h, seed=17)
    ix, iw = x + 7, w - 14
    r = RECIPES[0]
    T(img, ix, y + 7, 'Auftrag läuft', INK_T)
    tx = T(img, ix + iw, y + 7, fmt(fire), INK_T, right=True); flame(img, ix + iw - tx - 10, y + 7, FIRE)
    box18(img, ix, y + 19, True); put(img, f'item/{r[1]}', ix + 1, y + 20)
    T(img, ix + 22, y + 20, f'{done} / {n} {r[0]}', INK_T, maxw=iw - 22)
    T(img, ix + 22, y + 29, f'noch ca. {fmt((n - done) * 12)}*', INK_M, maxw=iw - 22)
    bar(img, ix, y + 41, iw, 6, done / n, GOLD)
    T(img, ix, y + 52, 'Schritt', INK_T)
    steps = ['Zeugtisch öffnen', 'Feuer nachlegen', 'Rezept klicken', 'Auf Ergebnis warten']
    cy = y + 63
    for i, s_ in enumerate(steps):
        if i == step:
            rect(img, ix - 2, cy - 2, ix + iw + 1, cy + 9, PA_L); rect(img, ix - 2, cy - 2, ix, cy + 9, TERRA)
            for k in range(4): rect(img, ix + 2 + k, cy + k, ix + 3 + k, cy + 7 - k, GOLD[0])
            T(img, ix + 10, cy, s_, INK_T)
        elif i < step:
            check(img, ix + 1, cy, OK_INK); T(img, ix + 10, cy, s_, INK_M)
        else:
            rect(img, ix + 2, cy + 3, ix + 5, cy + 5, INK_F); T(img, ix + 10, cy, s_, INK_F)
        cy += 11
    rule(img, ix, ix + iw, cy, PA_D)
    T(img, ix, cy + 3, 'Übrig im Inventar', INK_T)
    cy += 14
    rest = [('Eisenerz', inv['eisenerz'], (n - done) * 4), ('Steinkohle', inv['steinkohle'], (n - done) * 2),
            ('Stock (Feuer)', inv['stock'], math.ceil((n - done) * FIRE_PER_CRAFT / 15) * 3)]
    for name, have, need in rest:
        val = f'{have}, braucht {need}'
        if tw(name) + tw(val) + 4 > iw: name = name.split(' ')[0]
        T(img, ix, cy, name, INK_M, maxw=iw - tw(val) - 4)
        T(img, ix + iw, cy, val, OK_INK if have >= need else BAD_INK, right=True)
        cy += 10
    T(img, ix, cy + 1, 'Nachlegen bei < 0:03*', INK_F, maxw=iw)
    cy += 12
    red_btn(img, ix, cy, iw, 20, 'Stopp  [K]*')
    cy += 23
    slate_btn(img, ix, cy, iw, 16, 'Pausieren')
    cy += 20
    T(img, ix, cy, 'Danach: Stahlbarren 5x', INK_M, maxw=iw)
    assert cy + 8 <= y + h - 40, cy - y
    rule(img, ix, ix + iw, y + h - 38, PA_D)
    safety_lines(img, ix, iw, y + h - 34)
    return w, h


C_TABS = (('Auftrag', TERRA), ('Brennstoff', BRASS), ('Warteschlange', hx('#434D66')))


def tabs_c(img, x, y, tab):
    tx = x + 6; spans = []
    for i, (lab, c) in enumerate(C_TABS):
        w_ = tw(lab) + 8
        ribbon(img, tx, y + 14, w_, 16 if i == tab else 13, c if i == tab else shade(c, .8), lab, i == tab)
        spans.append((tx, tx + w_)); tx += w_ + 2
    return spans, tx - x + 4


def c_width():
    return max(150, sum(tw(l) + 8 for l, _ in C_TABS) + 2 * (len(C_TABS) - 1) + 12)


def auftrag_c(img, x, y, inv, tab=0, sel=0, n=16, fuel=1, jobs=None, total='Gesamt 27 Durchgänge, ca. 5:24*'):
    w, h = c_width(), 236
    spans, _ = tabs_c(img, x, y, tab)
    y += 14
    parchment(img, x, y, w, h, seed=19)
    rect(img, spans[tab][0], y, spans[tab][1], y + 3, C_TABS[tab][1])
    ix, iw = x + 7, w - 14
    if tab == 0:
        r = RECIPES[sel]
        recipe_picker(img, ix, iw, y + 7, sel)
        mx = job_max(inv, r, fuel)
        qty_row(img, ix, iw, y + 30, n, mx)
        T(img, ix, y + 58, 'Brennstoff', INK_T)
        checkbox(img, ix + iw - 9, y + 58, True); T(img, ix + iw - 12, y + 59, 'auto. nachlegen', INK_M, right=True)
        fuel_chips(img, ix, iw, y + 68, fuel, inv)
        need, fires = job_need(r, n, fuel)
        rule(img, ix, ix + iw, y + 90, PA_D)
        cy = need_lines(img, ix, iw, y + 94, inv, need, FUEL_KEY[fuel])
        T(img, ix, cy, f'{fires} Feuer* = {fires * FUELS[fuel][2]} {FUELS[fuel][0]}', INK_M, maxw=iw)
        cy += 11
        if FUEL_KEY[fuel] == 'steinkohle':
            left = inv['steinkohle'] - need['steinkohle']
            rect(img, ix - 2, cy - 2, ix + iw + 1, cy + 18, hx('#D9BE8A')); rect(img, ix - 2, cy - 2, ix, cy + 18, WARN_INK)
            T(img, ix + 3, cy, 'Kohle ist auch Zutat!', WARN_INK, maxw=iw - 3)
            T(img, ix + 3, cy + 9, f'Danach bleiben {left} Steinkohle', INK_M, maxw=iw - 3)
            cy += 22
        slate_btn(img, ix, cy, iw - 21, 16, f'Herstellen ({n}x)', hover=True)
        slate_btn(img, ix + iw - 18, cy, 18, 16, '+')
        assert cy + 16 <= y + h - 66, cy - y
    elif tab == 2:
        jobs = jobs or [('eisenbarren', 'Eisenbarren', 20, 'läuft 6/20', 6 / 20, None),
                ('stahlbarren', 'Stahlbarren', 5, 'wartet', 0, None),
                ('gehaerteter_stahlbarren', 'Geh. Stahlbarren', 2, 'fehlt 4 Stahl*', 0, BAD_INK)]
        cy = y + 7
        for i, (key, name, cnt, st, frac, col) in enumerate(jobs):
            if i == 0: rect(img, ix - 2, cy - 2, ix + iw + 1, cy + 25, PA_L); rect(img, ix - 2, cy - 2, ix, cy + 25, TERRA)
            T(img, ix + 2, cy + 5, str(i + 1), INK_F)
            box18(img, ix + 9, cy, i == 0); put(img, f'item/{key}', ix + 10, cy + 1)
            T(img, ix + 31, cy + 1, f'{name} {cnt}x', INK_T, maxw=iw - 31 - 30)
            if frac:
                bar(img, ix + 31, cy + 12, 52, 4, frac, GOLD); T(img, ix + 86, cy + 11, st, INK_M, maxw=iw - 86 - 28)
            else:
                T(img, ix + 31, cy + 11, st, col or INK_M, maxw=iw - 31 - 30)
            bx = ix + iw - 26
            small_btn(img, bx, cy, 12, 11, '^'); small_btn(img, bx, cy + 13, 12, 11, 'v')
            small_btn(img, bx + 14, cy + 6, 12, 11, 'x')
            if i < 2: rule(img, ix, ix + iw, cy + 26)
            cy += 29
        slate_btn(img, ix, cy, iw, 14, '+ Auftrag hinzufügen')
        cy += 18
        T(img, ix, cy, total, INK_M, maxw=iw)
        cy += 11
        checkbox(img, ix, cy, False); T(img, ix + 12, cy + 1, 'Bei Fehler nächsten Auftrag', INK_T, maxw=iw - 12)
        cy += 13
        bw = (iw - 3) // 2
        slate_btn(img, ix, cy, bw, 16, 'Alle starten', hover=True); red_btn(img, ix + bw + 3, cy, bw, 16, 'Stopp')
        assert cy + 16 <= y + h - 66, cy - y
    safety_strip(img, ix, iw, y + h - 62)
    return w, h + 14


# ---- HUD while the GUI is closed (activity-HUD style, cf. gen_zonen_toast_hud.py) ----
def hud_card(img, x, y, state):
    w, h = 204, 42
    rect(img, x + 1, y, x + w - 1, y + h, OUTL); rect(img, x, y + 1, x + w, y + h - 1, OUTL)
    rect(img, x + 1, y + 1, x + w - 1, y + h - 1, (30, 30, 36, 235))
    rect(img, x + 2, y + 1, x + w - 2, y + 2, D4); rect(img, x + 1, y + 2, x + 2, y + h - 2, D4)
    ramp = {'run': GOLD, 'pause': [D3, D4, SUB], 'stop': LOW}[state]
    rect(img, x + 2, y + 2, x + 5, y + h - 2, ramp[1]); rect(img, x + 2, y + 2, x + 5, y + 3, ramp[2])
    rect(img, x + 8, y + 7, x + 28, y + 27, OUTL); rect(img, x + 9, y + 8, x + 27, y + 26, D3)
    put(img, 'item/eisenbarren', x + 10, y + 9)
    head = {'run': 'Zeugtisch: Eisenbarren', 'pause': 'Zeugtisch pausiert', 'stop': 'Zeugtisch gestoppt'}[state]
    T(img, x + 32, y + 6, head, OCHRE, maxw=w - 70)
    T(img, x + w - 6, y + 6, '6/20', NAME, right=True)
    sub = {'run': 'Schritt: warte auf Ergebnis', 'pause': 'Weiter mit [K]*, Stopp mit [L]*',
           'stop': 'Grund: Schaden erhalten'}[state]
    T(img, x + 32, y + 16, sub, LOW[2] if state == 'stop' else SUB, maxw=w - 38)
    bar(img, x + 32, y + 27, w - 38, 4, 6 / 20, ramp, back=D2, edge=OUTL)
    right = {'run': 'Stopp: [K]*', 'pause': '', 'stop': 'Panel öffnen: [O]*'}[state]
    T(img, x + 32, y + 33, 'Feuer 0:09' if state == 'run' else '', LABEL)
    if right: T(img, x + w - 6, y + 33, right, LABEL, right=True)


def hud_scene():
    W, H = 480, 200
    img = backdrop(W, H, seed=11, dim=60)
    rect(img, 0, 0, W, H - CAP, (40, 50, 60, 0))
    # undim the world a bit: the HUD shows with no screen open
    world = Image.new('RGBA', (W, H), (0, 0, 0, 0)); img.alpha_composite(world)
    hb = tex('minecraft/textures/gui/sprites/hud/hotbar.png')
    img.alpha_composite(hb, ((W - hb.size[0]) // 2, H - CAP - hb.size[1] - 2))
    sel = tex('minecraft/textures/gui/sprites/hud/hotbar_selection.png')
    img.alpha_composite(sel, ((W - hb.size[0]) // 2 - 1, H - CAP - hb.size[1] - 3))
    hx0, hy0 = (W - hb.size[0]) // 2 + 3, H - CAP - hb.size[1] + 1
    for i, (key, n) in enumerate((('stock', 33), ('eisenbarren', 57))):
        put(img, f'item/{key}', hx0 + i * 20, hy0); count(img, hx0 + i * 20, hy0, n)
    # crosshair
    cx, cy = W // 2, (H - CAP) // 2
    rect(img, cx - 4, cy, cx + 5, cy + 1, NAME); rect(img, cx, cy - 4, cx + 1, cy + 5, NAME)
    hud_card(img, 6, 6, 'run'); hud_card(img, 6, 52, 'pause'); hud_card(img, 6, 98, 'stop')
    rect(img, 0, H - CAP, W, H, (0, 0, 0, 200))
    T(img, 6, H - CAP + 3, 'HUD bei geschlossener GUI: läuft, pausiert, gestoppt. * = Annahme', NAME, maxw=W - 12)
    return img


AW = 150
img = scene(AW, 'Auftrag A: Rezept, Menge, Brennstoff, Start. * = Annahme', extra_w=0)
H_A = MT + 248 + MB + CAP
def tall(img, ph):
    """Extend a scene so a panel taller than the chest fits (caption stays at the bottom)."""
    W = img.size[0]; H = MT + max(GH, ph) + MB + CAP
    if H == img.size[1]: return img
    cap = img.crop((0, img.size[1] - CAP, W, img.size[1]))
    big = backdrop(W, H); big.alpha_composite(cap, (0, H - CAP)); return big
img = tall(img, 256)
chest(img, MX, MT, JOB_INV); auftrag_a(img, PX, MT, JOB_INV, n=20, fuel=0)
NEW = {'A': save(img, 'werkstatt_auftrag_a.png')}

CW = c_width()
img = tall(scene(CW, 'Auftrag C: Reiter, Kohle als Brennstoff warnt. * = Annahme'), 236)
chest(img, MX, MT, JOB_INV); auftrag_c(img, PX, MT - 14, JOB_INV, tab=0, n=16, fuel=1)
NEW['C'] = save(img, 'werkstatt_auftrag_c.png')

run_inv = dict(JOB_INV, eisenerz=72, steinkohle=32, stock=33, eisenbarren=57)
img = tall(scene(AW, 'Auftrag läuft: Fortschritt, aktueller Schritt, Stopp. * = Annahme'), 256)
chest(img, MX, MT, run_inv, target=3); auftrag_laeuft(img, PX, MT, run_inv)
NEW['L'] = save(img, 'werkstatt_auftrag_laeuft.png')

img = tall(scene(CW, 'Warteschlange: mehrere Aufträge, sortieren, entfernen. * = Annahme'), 236)
chest(img, MX, MT, run_inv); auftrag_c(img, PX, MT - 14, run_inv, tab=2)
NEW['Q'] = save(img, 'werkstatt_warteschlange.png')

NEW['H'] = save(hud_scene(), 'werkstatt_hud_auftrag.png')

# overview (concept 2): row 1 A, C, running; row 2 queue, HUD. GUI scale 2.
gap = 6
rows = [[NEW['A'], NEW['C'], NEW['L']], [NEW['Q'], NEW['H']]]
OW = max(sum(i.size[0] for i in r) + gap * (len(r) + 1) for r in rows)
OH = sum(max(i.size[1] for i in r) for r in rows) + gap * (len(rows) + 1)
ov = Image.new('RGBA', (OW, OH), hx('#101014'))
yy = gap
for r in rows:
    xx = gap
    for i in r:
        ov.alpha_composite(i, (xx, yy)); xx += i.size[0] + gap
    yy += max(i.size[1] for i in r) + gap
ov.resize((OW * 2, OH * 2), Image.NEAREST).save(os.path.join(OUT, 'werkstatt_uebersicht_auftrag.png'))


# =====================================================================================
# Concept 3: any recipe of the Zeugtisch. Right-click a recipe item in the server GUI (the click is
# kept client-side, not sent), the panel reads name + lore + stack count and creates a job card.
# The job remembers the navigation path (category button, page, slot, item name) to find the
# recipe again after the server reopens the table. Left click stays vanilla (one normal craft).
# =====================================================================================
import re

ITEM_TEX = {'eisenbarren': 'item/eisenbarren', 'stahlbarren': 'item/stahlbarren', 'eisenerz': 'item/eisenerz',
            'steinkohle': 'item/steinkohle', 'torf': 'item/torf', 'stock': 'item/stock'}
INVG = {'stock': 30, 'eisenbarren': 45, 'eisenerz': 96, 'steinkohle': 44, 'torf': 3, 'stahlbarren': 12,
        'gehaerteter_stahlbarren': 0}


def item_key(name):
    n = name.lower().replace('ä', 'ae').replace('ö', 'oe').replace('ü', 'ue').replace('ß', 'ss')
    return n.replace('gehaerteter ', 'gehaerteter_').replace(' ', '_')


def parse_lore(lore):
    """Mock of the generic parser: lines after 'Herstellen:' are 'N x Name', alternatives joined by ' oder '.
    'Benötigt: Hitze' marks heat. Returns (groups, heat) or (None, heat) when no ingredient line is readable."""
    groups, heat, on = [], False, False
    for line in lore:
        t = line.strip()
        if t.lower().startswith('benötigt'):
            heat |= 'hitze' in t.lower(); on = False; continue
        if t.lower().startswith('herstellen'): on = True; continue
        if on and t:
            alts = []
            for part in t.split(' oder '):
                m = re.fullmatch(r'(\d+)\s*x\s*(.+)', part.strip())
                if not m: alts = None; break
                alts.append((int(m.group(1)), m.group(2).strip()))
            if alts: groups.append(alts)
            else: return None, heat
    return (groups or None), heat


def wrap(s, maxw):
    out, line = [], ''
    for word in s.split(' '):
        cand = (line + ' ' + word).strip()
        if tw(cand) <= maxw: line = cand
        else: out.append(line); line = word
    if line: out.append(line)
    return out


# Live lore (live-recipes.json) and assumed lore for tool recipes on the "Zeugschmieden" page.
LORE = {
    'Eisenbarren': ['Gewöhnlich', '', 'Ein schwerer Barren aus Eisen.', '', 'Herstellen:', '4 x Eisenerz',
                    '2 x Steinkohle oder 1 x Torf', '', 'Benötigt: Hitze'],
    'Eisenspitzhacke': ['Gewöhnlich', '', 'Eine robuste Spitzhacke.*', '', 'Herstellen:', '3 x Eisenbarren',
                        '2 x Stock', '', 'Benötigt: Hitze'],
    'Stemmeisen': ['Gewöhnlich', '', 'Hebelt Truhen und Türen auf.*', '', 'Siehe Werkstattbuch.*'],
}
# Assumed layout of the "Zeugschmieden" category page (names and textures are real pack items).
ZEUG_PAGE = [(0, 'gui/zurueck', 1), (3, 'gui/feuer_machen_stock', 1), (4, 'gui/feuer_machen_steinkohle', 1),
             (5, 'gui/feuer_machen_torf', 1), (7, 'gui/zeugschmieden', 1), (8, 'gui/tipp', 1),
             (9, 'item/improvisierte_spitzhacke', 1), (10, 'item/eisenbeil', 1), (11, 'item/eisenspitzhacke', 1),
             (18, 'item/eisenhacke', 1), (19, 'item/eisenschaufel', 1), (20, 'item/stahlspitzhacke', 1),
             (27, 'item/schere', 1), (28, 'item/stemmeisen', 1), (29, 'item/dietrich', 1),
             (49, 'gui/vorherige_seite', 1), (53, 'gui/naechste_seite', 1)]


def chest_page(img, gx, gy, inv, page, marked=None, cursor_slot=None):
    img.alpha_composite(tex('minecraft/textures/gui/container/generic_54.png').crop((0, 0, 176, 222)), (gx, gy))
    T(img, gx + 8, gy + 6, 'Zeugtisch', TITLE)
    for s, name, n in page + player_items(inv):
        x, y = slot_xy(s); put(img, name, gx + x, gy + y); count(img, gx + x, gy + y, n)
    if marked is not None:
        x, y = slot_xy(marked)
        for a in ((x - 1, y - 1, x + 17, y), (x - 1, y + 16, x + 17, y + 17), (x - 1, y, x, y + 16), (x + 16, y, x + 17, y + 16)):
            rect(img, gx + a[0], gy + a[1], gx + a[2], gy + a[3], GOLDH)
        bx, by = gx + x + 11, gy + y - 3                        # "+" badge, top right of the slot
        rect(img, bx, by, bx + 8, by + 8, BLACK); rect(img, bx + 1, by + 1, bx + 7, by + 7, READY[1])
        rect(img, bx + 2, by + 3, bx + 6, by + 5, NAME); rect(img, bx + 3, by + 2, bx + 5, by + 6, NAME)
    if cursor_slot is not None:
        x, y = slot_xy(cursor_slot)
        rect(img, gx + x, gy + y, gx + x + 16, gy + y + 16, (255, 255, 255, 80))


def job_card(img, ix, iw, y, inv, name, key, out_n, path, n, fuel, lore):
    groups, heat = parse_lore(lore)
    top = y
    box18(img, ix, y, True); put(img, f'item/{key}', ix + 1, y + 1); count(img, ix + 1, y + 1, out_n)
    T(img, ix + 22, y + 1, name, INK_T, maxw=iw - 22 - 22)
    star(img, ix + iw - 9, y + 1, GOLD)
    T(img, ix + 22, y + 10, path, INK_F, maxw=iw - 22)
    y += 22
    if groups:
        T(img, ix, y, f'Zutaten für {n}x (aus Lore)', INK_M, maxw=iw); y += 10
        for alts in groups:
            cnt, item = alts[0]
            k = item_key(item); need = cnt * n; have = inv.get(k, 0)
            if k == FUEL_KEY[fuel]:
                need += math.ceil(n * FIRE_PER_CRAFT / FUELS[fuel][3]) * FUELS[fuel][2]
            ok = have >= need
            label = f'{item}' + (f' o. {alts[1][1]}' if len(alts) > 1 else '')
            val = f'{need} / {have}' if ok else f'{need} / {have}, fehlt {need - have}'
            if tw(label) + tw(val) + 4 > iw: label = item
            T(img, ix, y, label, INK_T, maxw=iw - tw(val) - 4)
            T(img, ix + iw, y, val, OK_INK if ok else BAD_INK, right=True)
            y += 10
        mx = 0
        while True:
            m = mx + 1; good = True
            for alts in groups:
                cnt, item = alts[0]; k = item_key(item); need = cnt * m
                if k == FUEL_KEY[fuel]: need += math.ceil(m * FIRE_PER_CRAFT / FUELS[fuel][3]) * FUELS[fuel][2]
                good &= inv.get(k, 0) >= need
            if not good or m > 999: break
            mx = m
        mx_label = f'Max {mx}'
    else:
        lines = wrap('Zutaten unbekannt - nur Menge, stoppt wenn Server ablehnt', iw - 6)
        hh = len(lines) * 9 + 4
        rect(img, ix - 2, y - 2, ix + iw + 1, y + hh, hx('#D9BE8A')); rect(img, ix - 2, y - 2, ix, y + hh, WARN_INK)
        for i, l in enumerate(lines): T(img, ix + 3, y + i * 9, l, WARN_INK, maxw=iw - 4)
        y += hh + 3
        T(img, ix, y, 'Lore gelesen:', INK_M); y += 10
        for l in [l for l in lore if l][:3]:
            T(img, ix + 4, y, l, INK_F, maxw=iw - 4); y += 9
        mx_label = 'Max ?'
    fires = math.ceil(n * FIRE_PER_CRAFT / FUELS[fuel][3])
    heat_txt = f'Hitze: ja, {fires} Feuer*' if heat else ('Hitze: nein' if groups else 'Hitze: unbekannt, Feuer an*')
    T(img, ix, y, heat_txt, INK_M, maxw=iw); y += 13
    T(img, ix, y + 3, 'Menge', INK_T)
    x = ix + 33
    small_btn(img, x, y, 13, 14, '-'); x += 15
    num_field(img, x, y, 28, 14, str(n)); x += 30
    small_btn(img, x, y, 13, 14, '+'); x += 16
    small_btn(img, x, y, ix + iw - x, 14, mx_label)
    y += 18
    fuel_chips(img, ix, iw, y, fuel, inv)
    checkbox(img, ix + iw - 9, y + 4, True); T(img, ix + iw - 12, y + 5, 'auto.', INK_M, right=True)
    y += 23
    slate_btn(img, ix, y, iw - 21, 16, f'Herstellen ({n}x)', hover=True)
    slate_btn(img, ix + iw - 18, y, 18, 16, '+')
    return y + 16


def star(img, x, y, ramp):
    shape = ['...o...', '..ooo..', 'ooooooo', '.ooooo.', '..ooo..', '.oo.oo.', '.o...o.']
    for j, row in enumerate(shape):
        for i, ch in enumerate(row):
            if ch == 'o': px(img, x - 3 + i, y + j, ramp[1] if j > 1 else ramp[2])


SAVED = [('eisenbarren', 'Eisenbarren', 'Startseite, Slot 15'),
         ('eisenspitzhacke', 'Eisenspitzhacke', 'Zeugschmieden, Slot 11*'),
         ('stahlbarren', 'Stahlbarren', 'Startseite, Slot 24'),
         ('schere', 'Schere', 'Zeugschmieden, Slot 27*'),
         ('stemmeisen', 'Stemmeisen', 'Zutaten unbekannt')]


def saved_list(img, ix, iw, y, rows, big=False):
    T(img, ix, y, 'Gemerkte Rezepte', INK_T); T(img, ix + iw, y, f'{len(SAVED)}', INK_F, right=True)
    rule(img, ix, ix + iw, y + 10, PA_D); y += 13
    for i, (key, name, path) in enumerate(SAVED[:rows]):
        star(img, ix + 3, y + 5, GOLD)
        put(img, f'item/{key}', ix + 9, y)
        T(img, ix + 28, y + (0 if big else 4), name, INK_T, maxw=iw - 28 - 44)
        if big: T(img, ix + 28, y + 9, path, INK_F, maxw=iw - 28 - 44)
        small_btn(img, ix + iw - 42, y + 2, 28, 12, 'Neu')
        small_btn(img, ix + iw - 12, y + 2, 12, 12, 'x')
        y += 20 if big else 18
    return y


def panel_any(img, x, y, inv, mode, n=10, fuel=1):
    """Variant C, tab 'Auftrag', for any recipe. mode: empty | job | unknown | saved."""
    w = c_width()
    h = 252
    spans, _ = tabs_c(img, x, y, 0)
    y += 14
    parchment(img, x, y, w, h, seed=23)
    rect(img, spans[0][0], y, spans[0][1], y + 3, C_TABS[0][1])
    ix, iw = x + 7, w - 14
    cy = y + 6
    for l in wrap('Rechtsklick auf ein Rezept im Zeugtisch: zum Auftrag hinzufügen', iw):
        T(img, ix, cy, l, INK_M, maxw=iw); cy += 9
    cy += 2; rule(img, ix, ix + iw, cy, PA_D); cy += 5
    if mode == 'empty':
        T(img, ix, cy, 'Noch kein Auftrag.', INK_F); cy += 14
        cy = saved_list(img, ix, iw, cy, 3)
    elif mode == 'job':
        cy = job_card(img, ix, iw, cy, inv, 'Eisenspitzhacke', 'eisenspitzhacke', 1,
                      'Zeugschmieden > S.1 > Slot 11*', n, fuel, LORE['Eisenspitzhacke'])
        cy += 6
        cy = saved_list(img, ix, iw, cy, 2)
    elif mode == 'unknown':
        cy = job_card(img, ix, iw, cy, inv, 'Stemmeisen', 'stemmeisen', 1,
                      'Zeugschmieden > S.1 > Slot 28*', 5, fuel, LORE['Stemmeisen'])
    elif mode == 'saved':
        cy = saved_list(img, ix, iw, cy, 5, big=True)
        cy += 4
        T(img, ix, cy, '"Neu" legt einen Auftrag an und', INK_F, maxw=iw)
        T(img, ix, cy + 9, 'sucht das Rezept über den Pfad.', INK_F, maxw=iw)
        cy += 20
    assert cy <= y + h - 19, (mode, cy - y)
    rule(img, ix, ix + iw, y + h - 18, PA_D)
    T(img, ix, y + h - 15, 'Stopp-Regeln: 6 aktiv', INK_F, maxw=iw)
    return w, h + 14


CW = c_width()
NEW3 = {}

# right-click flow: before | after
def flow_half(caption, after):
    img = tall(scene(CW, caption), 252)
    if after:
        chest_page(img, MX, MT, INVG, ZEUG_PAGE, marked=11)
        panel_any(img, PX, MT - 14, INVG, 'job')
    else:
        chest_page(img, MX, MT, INVG, ZEUG_PAGE, cursor_slot=11)
        panel_any(img, PX, MT - 14, INVG, 'empty')
        x, y = slot_xy(11)
        cursor(img, MX + x + 8, MT + y + 6)
        tooltip(img, MX + x + 20, MT + y + 2, [('Eisenspitzhacke', OCHRE), ('Gewöhnlich', SUB), ('Herstellen:', SUB),
                                             ('3 x Eisenbarren', SUB), ('2 x Stock', SUB), ('Benötigt: Hitze', SUB),
                                             ('Rechtsklick: Auftrag', GOLDH)])
    return img

a = flow_half('Vorher: Rechtsklick auf ein Rezept (Seite Zeugschmieden*)', False)
b = flow_half('Nachher: Slot markiert, Auftrag rechts angelegt', True)
flow = Image.new('RGBA', (a.size[0] + b.size[0] + 4, a.size[1]), hx('#101014'))
flow.alpha_composite(a, (0, 0)); flow.alpha_composite(b, (a.size[0] + 4, 0))
NEW3['flow'] = save(flow, 'werkstatt_rechtsklick.png')

img = tall(scene(CW, 'Beliebiges Rezept: Zutaten aus der Lore. * = Annahme'), 252)
chest_page(img, MX, MT, INVG, ZEUG_PAGE, marked=11); panel_any(img, PX, MT - 14, INVG, 'job', n=10, fuel=1)
NEW3['any'] = save(img, 'werkstatt_auftrag_beliebig.png')

img = tall(scene(CW, 'Lore ohne lesbare Zutaten: nur Menge. * = Annahme'), 252)
chest_page(img, MX, MT, INVG, ZEUG_PAGE, marked=28); panel_any(img, PX, MT - 14, INVG, 'unknown', fuel=1)
NEW3['unk'] = save(img, 'werkstatt_rezept_unbekannt.png')

img = tall(scene(CW, 'Gemerkte Rezepte: schnell wieder als Auftrag. * = Annahme'), 252)
chest(img, MX, MT, INVG); panel_any(img, PX, MT - 14, INVG, 'saved')
NEW3['saved'] = save(img, 'werkstatt_gemerkt.png')

mixed = [('eisenbarren', 'Eisenbarren', 20, 'läuft 6/20', 6 / 20, None),
         ('eisenspitzhacke', 'Eisenspitzhacke', 10, 'Zeugschmieden, wartet', 0, None),
         ('stahlbarren', 'Stahlbarren', 5, 'Startseite, wartet', 0, None)]
img = tall(scene(CW, 'Warteschlange aus mehreren Kategorien. * = Annahme'), 236)
chest(img, MX, MT, INVG); auftrag_c(img, PX, MT - 14, INVG, tab=2, jobs=mixed, total='Gesamt 35 Durchgänge, ca. 7:00*')
NEW3['queue'] = save(img, 'werkstatt_warteschlange.png')

gap = 6
rows = [[NEW3['flow']], [NEW3['any'], NEW3['unk'], NEW3['saved']], [NEW3['queue'], NEW['L'], NEW['H']]]
OW = max(sum(i.size[0] for i in r) + gap * (len(r) + 1) for r in rows)
OH = sum(max(i.size[1] for i in r) for r in rows) + gap * (len(rows) + 1)
ov = Image.new('RGBA', (OW, OH), hx('#101014'))
yy = gap
for r in rows:
    xx = gap
    for i in r:
        ov.alpha_composite(i, (xx, yy)); xx += i.size[0] + gap
    yy += max(i.size[1] for i in r) + gap
ov.resize((OW * 2, OH * 2), Image.NEAREST).save(os.path.join(OUT, 'werkstatt_uebersicht.png'))


# =====================================================================================
# Concept 4: the panel starts collapsed. Three handles at the right edge of the Zeugtisch GUI:
# W1 hanging pennant, W2 vertical cloth strip, W3 icon button. The open/closed state is
# remembered per station (*). Badges: queue count, running "6/20", red dot when stopped.
# =====================================================================================
PEN = [shade(hx('#434D66'), .62), shade(hx('#434D66'), .8), hx('#434D66'), shade(hx('#434D66'), 1.18)]
CLOTH_R = [shade(TERRA, .62), shade(TERRA, .8), TERRA, shade(TERRA, 1.15)]
WOOD = [hx('#382A25'), hx('#503E2E'), hx('#6F5D4B'), hx('#86745F')]
EMB = hx('#C8AA86')        # pergament emblem on the pennant (HUD-Leiste pennant heart)
FRINGE = hx('#B58D36')


def anvil(img, x, y, c):
    for j, row in enumerate(['ooooooo..', '.oooooooo', '..ooooo..', '...ooo...', '..ooooo..']):
        for i, ch in enumerate(row):
            if ch == 'o': px(img, x + i, y + j, c)


def rod(img, x0, x1, y):
    rect(img, x0, y, x1, y + 3, WOOD[1]); rect(img, x0, y, x1, y + 1, WOOD[3]); rect(img, x0, y + 2, x1, y + 3, WOOD[0])
    rect(img, x0 - 1, y, x0, y + 3, WOOD[0]); rect(img, x1, y, x1 + 1, y + 3, WOOD[0])


def pennant(img, x, y, w=24, body=34, point=12, state='closed', ramp=PEN):
    """Cloth pennant hanging from a short rod; pointed end with a small fringe. state: closed | hover."""
    if state == 'hover': y += 3                       # hover: drops a little and gets a gold edge
    for cx in (x + 3, x + w - 4):                     # cord loops from rod to cloth
        rect(img, cx, y - 4, cx + 1, y + 1, CORD[2])
    rod(img, x - 3, x + w + 3, y - 6)
    edge = GOLDH if state == 'hover' else ramp[0]
    tot = body + point
    for j in range(tot):
        if j < body: l, r = 0, w
        else:
            k = j - body + 1; l, r = round(k * (w / 2) / point), w - round(k * (w / 2) / point)
        if r - l <= 0: continue
        for i in range(l, r):
            c = ramp[2]
            if i <= l + 1: c = ramp[3]
            elif i >= r - 3: c = ramp[1]
            if (j + 3) % 9 == 0 and l + 2 < i < r - 3: c = ramp[1]     # soft horizontal fold
            px(img, x + i, y + j, c)
        px(img, x + l, y + j, edge); px(img, x + r - 1, y + j, edge)
        if j >= body and (j % 2 == 0):               # fringe along the point
            px(img, x + l - 1, y + j + 1, FRINGE); px(img, x + r, y + j + 1, FRINGE)
    rect(img, x, y, x + w, y + 1, edge)
    anvil(img, x + (w - 9) // 2, y + 10, EMB)
    rect(img, x + 4, y + 19, x + w - 4, y + 20, EMB)
    rect(img, x + 4, y + 5, x + w - 4, y + 6, EMB)  # small pergament band under the emblem
    return (x, y, w, tot)


def strip(img, x, y, state='closed', label='WERKSTATT', ramp=CLOTH_R):
    """Narrow vertical cloth strip with stacked capitals (more readable than rotated text at 8 px)."""
    w = 14; h = 14 + len(label) * 9 + 4
    edge = GOLDH if state == 'hover' else ramp[0]
    rect(img, x, y, x + w, y + h, edge); rect(img, x + 1, y + 1, x + w - 1, y + h - 1, ramp[2])
    rect(img, x + 1, y + 1, x + 2, y + h - 1, ramp[3]); rect(img, x + w - 3, y + 1, x + w - 1, y + h - 1, ramp[1])
    for yy in range(y + h, y + h + 4):                # fringe at the lower end
        for xx in range(x + 1, x + w - 1, 2): px(img, xx, yy, FRINGE)
    rect(img, x - 1, y - 2, x + w + 1, y + 1, WOOD[1]); rect(img, x - 1, y - 2, x + w + 1, y - 1, WOOD[3])
    anvil(img, x + 3, y + 4, EMB)
    for i, ch in enumerate(label):
        T(img, x + w // 2 - tw(ch) // 2, y + 13 + i * 9, ch, EMB)
    return (x, y, w, h + 4)


def icon_btn(img, x, y, state='closed'):
    o = GOLDH if state in ('hover', 'active') else PA_D
    rect(img, x + 1, y, x + 17, y + 18, o); rect(img, x, y + 1, x + 18, y + 17, o)
    rect(img, x + 1, y + 1, x + 17, y + 17, PA_L if state == 'active' else PA)
    rect(img, x + 1, y + 1, x + 17, y + 2, PA_L); rect(img, x + 1, y + 1, x + 2, y + 17, PA_L)
    rect(img, x + 1, y + 16, x + 17, y + 17, PA_S); rect(img, x + 16, y + 1, x + 17, y + 17, PA_S)
    anvil(img, x + 5, y + 7, PICTO)
    return (x, y, 18, 18)


def badge(img, x, y, kind, label=''):
    """Badge anchored with its right edge at x. kind: count | run | stop | new."""
    if kind == 'stop':
        rect(img, x - 7, y + 1, x - 1, y + 6, BLACK); rect(img, x - 6, y, x - 2, y + 7, BLACK)
        rect(img, x - 6, y + 1, x - 2, y + 6, LOW[1]); px(img, x - 5, y + 2, LOW[2]); return
    fill, ink = {'count': (GOLD[1], hx('#2A1E12')), 'run': (D1, NAME), 'new': (READY[1], NAME)}[kind]
    w = tw(label) + 5 + (8 if kind == 'run' else 0)
    rect(img, x - w + 1, y, x - 1, y + 11, BLACK); rect(img, x - w, y + 1, x, y + 10, BLACK)
    rect(img, x - w + 1, y + 1, x - 1, y + 10, fill)
    tx = x - w + 3
    if kind == 'run': flame(img, tx - 1, y + 2, FIRE); tx += 8
    T(img, tx, y + 2, label, ink)


def glow(img, x, y, w, h):
    for i, c in enumerate((GOLDH, GOLD[2])):
        rect(img, x - 2 - i, y - 2 - i, x + w + 2 + i, y - 1 - i, c); rect(img, x - 2 - i, y + h + 1 + i, x + w + 2 + i, y + h + 2 + i, c)
        rect(img, x - 2 - i, y - 2 - i, x - 1 - i, y + h + 2 + i, c); rect(img, x + w + 1 + i, y - 2 - i, x + w + 2 + i, y + h + 2 + i, c)


HX = MX + GW + 3            # handles start right of the chest frame
HANDLE_W = 64


def handle(img, kind, state='closed'):
    if kind == 'w1': return pennant(img, MX + GW + 3, MT + 8, state=state)
    if kind == 'w2': return strip(img, HX, MT + 18, state=state)
    return icon_btn(img, HX + 1, MT + 4, state=state)


def closed_scene(kind, caption, state='closed', badges=(), inv=None, page=None, marked=None, ph=None):
    img = scene(HANDLE_W, caption)
    if ph: img = tall(img, ph)                      # extend before drawing; tall() starts a fresh backdrop
    inv = inv or INVG
    if page: chest_page(img, MX, MT, inv, page, marked=marked)
    else: chest(img, MX, MT, inv)
    x, y, w, h = handle(img, kind, state)
    by = y
    for kb, lab in badges:
        bw = 7 if kb == 'stop' else tw(lab) + 5 + (8 if kb == 'run' else 0)
        badge(img, x + w + 3 + bw, by, kb, lab); by += 13
    return img, (x, y, w, h)


def open_scene(kind, caption, mode='job'):
    img = tall(scene(CW + 4, caption, extra_w=24), 252)
    chest_page(img, MX, MT, INVG, ZEUG_PAGE, marked=11 if mode == 'job' else None)
    panel_any(img, PX, MT - 14, INVG, mode)
    hx0 = PX + CW + 4
    if kind == 'w1':                                   # rolled-up stub: rod + short cloth, click rolls the panel in
        rod(img, hx0 - 1, hx0 + 19, MT - 4)
        rect(img, hx0 + 2, MT - 1, hx0 + 16, MT + 9, PEN[2]); rect(img, hx0 + 2, MT - 1, hx0 + 3, MT + 9, PEN[3])
        rect(img, hx0 + 2, MT + 9, hx0 + 16, MT + 12, WOOD[1]); rect(img, hx0 + 2, MT + 11, hx0 + 16, MT + 12, WOOD[0])
        for i in range(3): rect(img, hx0 + 9 - i, MT + 2 + i, hx0 + 10 + i, MT + 3 + i, EMB)   # "^" roll up
    elif kind == 'w2':
        x, y, w, h = 14, 0, 0, 0
        rect(img, hx0, MT + 4, hx0 + 14, MT + 34, CLOTH_R[0]); rect(img, hx0 + 1, MT + 5, hx0 + 13, MT + 33, CLOTH_R[2])
        rect(img, hx0 + 1, MT + 5, hx0 + 2, MT + 33, CLOTH_R[3])
        anvil(img, hx0 + 3, MT + 9, EMB)
        T(img, hx0 + 7 - tw('>') // 2, MT + 20, '>', EMB)
    else:
        icon_btn(img, hx0, MT - 12, 'active')
    return img


def unroll_scene(caption):
    """W1 intermediate: the pennant has widened into a parchment scroll that rolls down."""
    img = tall(scene(CW + 4, caption, extra_w=24), 252)
    chest(img, MX, MT, INVG)
    tmp = Image.new('RGBA', img.size, (0, 0, 0, 0))
    panel_any(tmp, PX, MT - 14, INVG, 'empty')
    cut = MT + 70
    img.alpha_composite(tmp.crop((PX, MT, PX + CW, cut)), (PX, MT))
    rod(img, PX - 3, PX + CW + 3, MT - 6)
    rect(img, PX - 2, cut, PX + CW + 2, cut + 6, WOOD[1]); rect(img, PX - 2, cut, PX + CW + 2, cut + 1, WOOD[3])
    rect(img, PX - 2, cut + 5, PX + CW + 2, cut + 6, WOOD[0])
    for xx in range(PX, PX + CW, 2): px(img, xx, cut + 7, FRINGE)
    return img


def pair(images, name, gap=4):
    W = sum(i.size[0] for i in images) + gap * (len(images) - 1); H = max(i.size[1] for i in images)
    out = Image.new('RGBA', (W, H), hx('#101014')); x = 0
    for i in images:
        out.alpha_composite(i, (x, H - i.size[1])); x += i.size[0] + gap
    return save(out, name)


W4 = {}
c1, _ = closed_scene('w1', 'W1 zu', badges=[('count', '2')])
h1, _ = closed_scene('w1', 'Hover', state='hover', badges=[('count', '2')])
u1 = unroll_scene('W1 rollt auf (optional)')
o1 = open_scene('w1', 'W1 offen, ^ rollt wieder ein')
W4['w1'] = pair([c1, h1, u1, o1], 'werkstatt_wimpel_w1.png')

c2, _ = closed_scene('w2', 'W2 zu', badges=[('count', '2')])
h2, _ = closed_scene('w2', 'Hover', state='hover', badges=[('count', '2')])
o2 = open_scene('w2', 'W2 offen, Streifen klappt ein')
W4['w2'] = pair([c2, h2, o2], 'werkstatt_wimpel_w2.png')

c3, _ = closed_scene('w3', 'W3 zu', badges=[('count', '2')])
h3, _ = closed_scene('w3', 'Hover', state='hover', badges=[('count', '2')])
o3 = open_scene('w3', 'W3 offen, Knopf aktiv')
W4['w3'] = pair([c3, h3, o3], 'werkstatt_wimpel_w3.png')

# badge states: crop of the chest's right edge with the handle, per variant
STATES = [('Leer', []), ('2 in Liste', [('count', '2')]), ('Läuft', [('run', '6/20'), ('count', '2')]),
          ('Gestoppt', [('stop', ''), ('count', '2')])]
cells = []
for kind in ('w1', 'w2', 'w3'):
    row = []
    for lab, bs in STATES:
        img, _ = closed_scene(kind, '', badges=bs)
        cell = img.crop((MX + GW - 50, MT - 12, MX + GW + HANDLE_W + 10, MT + 132))
        rect(cell, 0, cell.size[1] - 12, cell.size[0], cell.size[1], (0, 0, 0, 200))
        T(cell, 4, cell.size[1] - 10, f'{kind.upper()} {lab}', NAME, maxw=cell.size[0] - 6)
        row.append(cell)
    cells.append(row)
cw_, ch_ = cells[0][0].size
grid = Image.new('RGBA', (cw_ * 4 + 3 * 4, ch_ * 3 + 2 * 4), hx('#101014'))
for r, row in enumerate(cells):
    for c, cell in enumerate(row): grid.alpha_composite(cell, (c * (cw_ + 4), r * (ch_ + 4)))

# right-click while collapsed and a job runs: option A auto-opens, option B flashes the badge
optA = tall(scene(CW + 4, 'Option A: Rechtsklick öffnet das Panel', extra_w=24), 252)
chest_page(optA, MX, MT, INVG, ZEUG_PAGE, marked=11); panel_any(optA, PX, MT - 14, INVG, 'job')
optB, (bx, byy, bw_, bh_) = closed_scene('w1', 'Option B: bleibt zu, Abzeichen blinkt', page=ZEUG_PAGE, marked=11,
                                         badges=[('run', '6/20'), ('new', '+1')], ph=252)
glow(optB, bx, byy, bw_, bh_)
B = Image.new('RGBA', (optA.size[0] + optB.size[0] + 4, max(optA.size[1], optB.size[1])), hx('#101014'))
B.alpha_composite(optA, (0, 0)); B.alpha_composite(optB, (optA.size[0] + 4, 0))
GW_ = max(grid.size[0], B.size[0])
sheet = Image.new('RGBA', (GW_, grid.size[1] + 4 + B.size[1]), hx('#101014'))
sheet.alpha_composite(grid, (0, 0)); sheet.alpha_composite(B, (0, grid.size[1] + 4))
W4['badges'] = save(sheet, 'werkstatt_wimpel_badges.png')

gap = 6
rows = [[W4['w1']], [W4['w2'], W4['w3']], [W4['badges']]]
OW = max(sum(i.size[0] for i in r) + gap * (len(r) + 1) for r in rows)
OH = sum(max(i.size[1] for i in r) for r in rows) + gap * (len(rows) + 1)
ov = Image.new('RGBA', (OW, OH), hx('#101014'))
yy = gap
for r in rows:
    xx = gap
    for i in r:
        ov.alpha_composite(i, (xx, yy)); xx += i.size[0] + gap
    yy += max(i.size[1] for i in r) + gap
ov.resize((OW * 2, OH * 2), Image.NEAREST).save(os.path.join(OUT, 'werkstatt_wimpel_uebersicht.png'))


# =====================================================================================
# Concept 5: a brown tab that grows out of the right frame edge of the Zeugtisch GUI,
# 16 GUI px below the GUI top, with a neutral settings pictogram (no anvil, no floating button).
# Frame colours sampled from the pack's generic_54.png: outline #382A25, cord #493626 / #855C2C.
# =====================================================================================
ICON_DIR = os.path.join(OUT, 'icons'); os.makedirs(ICON_DIR, exist_ok=True)
F_OUT, F_BODY, F_CORD = hx('#382A25'), hx('#493626'), hx('#855C2C')
F_HI, F_LO = hx('#57412D'), hx('#3F2F22')      # one step lighter / darker than the frame body
F_HOV = hx('#5A4431')                         # hover body
ICON_INK = PA                                  # #C9AA81 parchment pictogram


def gear_mask(size, r_out, r_body, r_hole, teeth, cx=None, cy=None, phase=0.0):
    cx = size / 2 if cx is None else cx; cy = size / 2 if cy is None else cy
    out = set()
    for y in range(size):
        for x in range(size):
            dx, dy = x + .5 - cx, y + .5 - cy
            r = math.hypot(dx, dy); a = (math.degrees(math.atan2(dy, dx)) + phase) % (360 / teeth)
            tooth = a < (360 / teeth) * .5
            if r <= r_hole: continue
            if r <= r_body or (r <= r_out and tooth): out.add((x, y))
    return out


def icon_png(pixels, name, size=16):
    im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for x, y in pixels: im.putpixel((x, y), ICON_INK)
    im.save(os.path.join(ICON_DIR, name))
    return im


def mirror4(pts, size=16):
    """Mirror a quarter pattern to all four quadrants (symmetric pixel gear)."""
    out = set()
    for x, y in pts:
        for xx, yy in ((x, y), (size - 1 - x, y), (x, size - 1 - y), (size - 1 - x, size - 1 - y)):
            out.add((xx, yy))
    return out


# 8-tooth gear, built per quarter (top left) and mirrored: ring between r 2.3 and 5.3 around the
# icon centre, teeth top/left (2 px long, 4 px wide after mirroring) and an L-shaped diagonal tooth.
_q = {(x, y) for y in range(8) for x in range(8) if 2.3 <= math.hypot(x + .5 - 8, y + .5 - 8) <= 5.3}
_q |= {(6, 1), (7, 1), (6, 2), (7, 2), (1, 6), (1, 7), (2, 6), (2, 7), (2, 2), (3, 2), (2, 3), (3, 3), (4, 3), (3, 4)}
_gear = mirror4(_q)
ZAHNRAD = icon_png(_gear, 'zahnrad.png')

# Two meshing gears, built with the same quarter-and-mirror method: big (12 px) bottom left, small (8 px) top right.
def gear_q(size, r_in, r_out, extra):
    c = size / 2
    q = {(x, y) for y in range(size // 2) for x in range(size // 2) if r_in <= math.hypot(x + .5 - c, y + .5 - c) <= r_out}
    return mirror4(q | set(extra), size)


_big = gear_q(12, 1.6, 3.9, [(5, 0), (5, 1), (0, 5), (1, 5), (1, 1), (2, 1), (1, 2), (2, 2)])
_small = gear_q(8, 1.0, 2.8, [(3, 0), (0, 3)])
_dbl = {(x, y + 4) for x, y in _big} | {(x + 8, y - 1) for x, y in _small if y - 1 >= 0}
_dbl = {(x, y) for x, y in _dbl if 0 <= x < 16 and 0 <= y < 16}
DOPPELT = icon_png(_dbl, 'zahnrad_klein_doppelt.png')

# Hammer (head top left, handle to bottom right) crossed with tongs (jaws top right).
_HT = ['................',
       '.#####......##..',
       '.#####.....#..#.',
       '.#####.....#..#.',
       '...###......##..',
       '.....##....##...',
       '......##..##....',
       '.......####.....',
       '.......####.....',
       '......##..##....',
       '.....##....##...',
       '....##......##..',
       '...##........##.',
       '..##..........#.',
       '.##.............',
       '................']
KREUZ = icon_png({(x, y) for y, row in enumerate(_HT) for x, ch in enumerate(row) if ch == '#'}, 'werkzeug_kreuz.png')
MINI = Image.new('RGBA', (10, 10), (0, 0, 0, 0))
for x, y in gear_q(10, 1.3, 3.3, [(4, 0), (0, 4), (1, 1), (2, 1), (1, 2)]): MINI.putpixel((x, y), ICON_INK)

TAB_Y = 16                                   # GUI px below the GUI top edge
TABS = {'t1': (20, 18), 't2': (20, 22), 't3': (14, 18)}   # protrusion beyond the frame (w) x height (h)


def draw_tab(img, gx, gy, kind, state='closed', icon=ZAHNRAD):
    """Tab growing out of the frame: column gx+175 (frame outline) is opened into the tab body."""
    w, h = TABS[kind]
    x0, y0 = gx + 175, gy + TAB_Y
    x1, y1 = x0 + w, y0 + h - 1                 # x1 = right outline column, y1 = bottom outline row
    out = GOLDH if state == 'hover' else F_OUT
    body = F_HOV if state == 'hover' else F_BODY
    rect(img, x0 + 1, y0, x1, y0 + 1, out); rect(img, x0 + 1, y1, x1, y1 + 1, out)
    rect(img, x1, y0 + 1, x1 + 1, y1, out)
    if kind == 't2':
        inner = F_OUT if state != 'hover' else F_BODY
        rect(img, x0, y0 + 1, x1, y1, inner)
        for yy in list(range(y0 + 1, y0 + 3)) + list(range(y1 - 2, y1)):     # cord bands top / bottom
            for xx in range(x0, x1):
                px(img, xx, yy, F_CORD if (xx + yy) % 4 < 2 else F_BODY)
        for yy in range(y0 + 1, y1):                                          # cord band right
            for xx in (x1 - 2, x1 - 1):
                px(img, xx, yy, F_CORD if (xx + yy) % 4 < 2 else F_BODY)
        img.alpha_composite(icon, (x0 + 1, y0 + 3))
    else:
        rect(img, x0, y0 + 1, x1, y1, body)
        rect(img, x0 + 1, y0 + 1, x1, y0 + 2, F_HI if state != 'hover' else shade(F_HOV, 1.15))
        rect(img, x0 + 1, y1 - 1, x1, y1, F_LO); rect(img, x1 - 1, y0 + 1, x1, y1, F_LO)
        if kind == 't1': img.alpha_composite(icon, (x0 + 2, y0 + 1))
        else: img.alpha_composite(MINI, (x0 + 2, y0 + 4))
    return x0, y0, w, h


def bridge(img, gx, gy, kind, px0):
    """Open state: the panel's left frame (starting at px0) opens into the tab, so tab and panel read as one."""
    w, h = TABS[kind]
    y0 = gy + TAB_Y
    a, b = (y0 + 3, y0 + h - 3) if kind == 't2' else (y0 + 1, y0 + h - 1)
    col = F_OUT if kind == 't2' else F_BODY
    x1 = gx + 175 + w
    rect(img, x1, y0, px0, y0 + 1, F_OUT); rect(img, x1, y0 + h - 1, px0, y0 + h, F_OUT)   # outline runs on into the panel
    rect(img, x1 - (2 if kind == 't2' else 1), a, px0 + 3, b, col)
    if kind != 't2':
        rect(img, x1 - 1, y0 + 1, px0 + 3, y0 + 2, F_HI); rect(img, x1 - 1, y0 + h - 2, px0 + 3, y0 + h - 1, F_LO)
    else:                                                          # cord bands continue into the panel's cord frame
        for yy in list(range(y0 + 1, y0 + 3)) + list(range(y0 + h - 3, y0 + h - 1)):
            for xx in range(x1 - 2, px0 + 3):
                px(img, xx, yy, F_CORD if (xx + yy) % 4 < 2 else F_BODY)


def reiter_closed(kind, caption, state='closed', badges=(), ph=None, icon=ZAHNRAD):
    img = scene(56, caption)
    chest(img, MX, MT, INVG)
    x, y, w, h = draw_tab(img, MX, MT, kind, state, icon)
    bx = x + w + 2 if kind != 't3' else x + w - 3          # t3: badge sits on the tab's right edge
    by = y - (4 if kind == 't3' else 0)
    for kb, lab in badges:
        bw = 7 if kb == 'stop' else tw(lab) + 5 + (8 if kb == 'run' else 0)
        badge(img, bx + bw, by, kb, lab); by += 13
    return img


def reiter_open(kind, caption, icon=ZAHNRAD):
    w, _ = TABS[kind]
    gap = w - 4
    img = tall(scene(CW + gap, caption), 252)
    chest_page(img, MX, MT, INVG, ZEUG_PAGE, marked=11)
    pxo = MX + 175 + w
    panel_any(img, pxo, MT - 14, INVG, 'job')
    draw_tab(img, MX, MT, kind, 'open', icon)
    bridge(img, MX, MT, kind, pxo)
    return img


def badge_strip(kind):
    cells = []
    for lab, bs in (('Liste', [('count', '2')]), ('Läuft', [('run', '6/20')]), ('Gestoppt', [('stop', '')])):
        im = reiter_closed(kind, '', badges=bs)
        c = im.crop((MX + 140, MT - 8, MX + 176 + 56, MT + 52))
        rect(c, 0, c.size[1] - 11, c.size[0], c.size[1], (0, 0, 0, 200)); T(c, 3, c.size[1] - 10, lab, NAME)
        cells.append(c)
    W = sum(c.size[0] for c in cells) + 4 * (len(cells) - 1)
    st = Image.new('RGBA', (W, cells[0].size[1]), hx('#101014')); x = 0
    for c in cells: st.alpha_composite(c, (x, 0)); x += c.size[0] + 4
    return st


R5 = {}
NAMES = {'t1': 'T1 Reiter + Zahnrad', 't2': 'T2 Reiter mit Kordel', 't3': 'T3 schmal'}
for kind in ('t1', 't2', 't3'):
    c = reiter_closed(kind, f'{NAMES[kind]}: zu', badges=[('count', '2')])
    hv = reiter_closed(kind, 'Hover', state='hover', badges=[('count', '2')])
    o = reiter_open(kind, f'{NAMES[kind]}: offen')
    top = [c, hv, o]
    W = sum(i.size[0] for i in top) + 8
    strip_ = badge_strip(kind)
    H = max(i.size[1] for i in top) + 4 + strip_.size[1]
    sheet = Image.new('RGBA', (W, H), hx('#101014')); x = 0
    for i in top: sheet.alpha_composite(i, (x, max(i.size[1] for i in top) - i.size[1])); x += i.size[0] + 4
    sheet.alpha_composite(strip_, (0, H - strip_.size[1]))
    R5[kind] = save(sheet, f'werkstatt_reiter_{kind}.png')

# zoom of the joint, 1 GUI px = 8 image px
zs = []
for kind in ('t1', 't2'):
    im = scene(56, ''); chest(im, MX, MT, INVG); draw_tab(im, MX, MT, kind)
    c = im.crop((MX + 160, MT + 8, MX + 200, MT + 44)).resize((40 * 8, 36 * 8), Image.NEAREST)
    lab = Image.new('RGBA', (c.size[0] // 3, 12), (0, 0, 0, 255)); T(lab, 2, 2, NAMES[kind], NAME, maxw=c.size[0] // 3 - 4)
    lab = lab.resize((lab.size[0] * 3, 36), Image.NEAREST)
    cell = Image.new('RGBA', (c.size[0], c.size[1] + 40), hx('#101014'))
    cell.alpha_composite(c, (0, 0)); cell.alpha_composite(lab, (0, c.size[1] + 4))
    zs.append(cell)
icons_row = Image.new('RGBA', (3 * 16 * 8 + 2 * 16, 16 * 8), F_BODY)
for i, ic in enumerate((ZAHNRAD, KREUZ, DOPPELT)):
    icons_row.alpha_composite(ic.resize((128, 128), Image.NEAREST), (i * (128 + 16), 0))
Z = Image.new('RGBA', (max(sum(z.size[0] for z in zs) + 16, icons_row.size[0]), zs[0].size[1] + 16 + icons_row.size[1]), hx('#101014'))
Z.alpha_composite(zs[0], (0, 0)); Z.alpha_composite(zs[1], (zs[0].size[0] + 16, 0)); Z.alpha_composite(icons_row, (0, zs[0].size[1] + 16))
Z.save(os.path.join(OUT, 'werkstatt_reiter_zoom.png'))

gap = 6
rows = [[R5['t1']], [R5['t2']], [R5['t3']]]
OW = max(sum(i.size[0] for i in r) + gap * (len(r) + 1) for r in rows)
OH = sum(max(i.size[1] for i in r) for r in rows) + gap * (len(rows) + 1)
ov = Image.new('RGBA', (OW, OH), hx('#101014'))
yy = gap
for r in rows:
    xx = gap
    for i in r: ov.alpha_composite(i, (xx, yy)); xx += i.size[0] + gap
    yy += max(i.size[1] for i in r) + gap
ov.resize((OW * 2, OH * 2), Image.NEAREST).save(os.path.join(OUT, 'werkstatt_reiter_uebersicht.png'))
print('werkstatt ok')
