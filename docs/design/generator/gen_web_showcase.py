"""Showcase images of the OttoExtra 0.18.0 features for the website draft (OttoExtra_Web_Styling/design-entwurf).

The game cannot be driven for real screenshots, so every image is an in-world composite at GUI scale 3:
a 640x360 logical screen (1920x1080) of which a 16:9 window is cut out and saved 3x. The HUD parts follow
the current client code (WerkstattHud, VoteReminderToast, AnimalToastCards, ConditionsHud, ConditionsDisplay,
PlaketteStyle, RegionActivityHud, TopCenterStack, HudFlow). Rule of the mocks: no text shadows, even where
the game draws one (animal toast, activity HUD, banner name). Player, horse and world are illustrations.

Every image is written as <name>.png (native, 3x of its window) and <name>.webp (960x540 like the other
images of the page; lossless when the window is 320x180, otherwise downscaled).

Usage: python -I gen_web_showcase.py <pack_assets_dir> <wappen_dir> <out_dir> <scratch_dir>
  pack_assets_dir = Resourcepack/ottonien-reformed-update/assets
  wappen_dir      = OttoExtra-Resources/input/wappen (16x16 coats of arms)
  out_dir         = OttoExtra_Web_Styling/design-entwurf/media/0.18
  scratch_dir     = a temporary folder for the side outputs of the reused generators
"""
import os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS, WAPPEN, OUT, SCRATCH = sys.argv[1:5]
os.makedirs(OUT, exist_ok=True)


def _load(script, out):
    """Runs another generator in its own namespace (its side outputs go to scratch) and returns it."""
    path = os.path.join(HERE, script)
    ns = {'__file__': path, '__name__': 'reused'}
    argv = sys.argv
    sys.argv = [path, ASSETS, os.path.join(SCRATCH, out)]
    try:
        exec(compile(open(path, encoding='utf-8').read(), path, 'exec'), ns)
    finally:
        sys.argv = argv
    return ns


# The Werkstatt generator draws its scenes on backdrop(); make every backdrop a window of one big world so a
# scene can be pasted onto a larger canvas without seams.
W = _load('gen_werkstatt.py', 'werkstatt')
_orig_backdrop = W['backdrop']
BIG_OFF = 512
_BIG_DARK = _orig_backdrop(1600, 1200, 7, 180)
W['backdrop'] = lambda w, h, seed=7, dim=180: _BIG_DARK.crop((BIG_OFF, BIG_OFF, BIG_OFF + w, BIG_OFF + h))

H = _load('gen_hud_vote_werkstatt.py', 'hud')
for _k in ('hx', 'rect', 'px', 'T', 'tw', 'panel', 'stripe', 'portrait', 'hbar', 'key', 'key_w', 'vote_c',
           'zone_toast', 'flame', 'check', 'lock', 'item16', 'tex', 'put', 'SIEGEL', 'NAME', 'OCHRE', 'SUB',
           'LABEL', 'OUTL', 'D2', 'D3', 'GOLD', 'GREY', 'LOW', 'READY', 'FIRE', 'WHITE', 'BLACK'):
    globals()[_k] = H[_k]

K = 3
SW, SH = 640, 360
OE_TEX = os.path.join(HERE, '..', '..', '..', 'src', 'main', 'resources', 'assets', 'ottoextra', 'textures')


def own_icon(rel): return Image.open(os.path.join(OE_TEX, rel)).convert('RGBA')


def arms(name, size=16):
    im = Image.open(os.path.join(WAPPEN, name)).convert('RGBA')
    return im if size == 16 else im.resize((size, size), Image.NEAREST)


# --- world: the bright in-world backdrop of the _ingame mocks, crosshair and hotbar ---
_BIG_WORLD = _orig_backdrop(SW, SH, 7, 70)


def world(hotbar=True, crosshair=True):
    img = _BIG_WORLD.copy()
    cx, cy = SW // 2, SH // 2
    if crosshair:
        rect(img, cx - 4, cy, cx + 5, cy + 1, (230, 230, 230, 200)); rect(img, cx, cy - 4, cx + 1, cy + 5, (230, 230, 230, 200))
    if hotbar:
        hb = tex('minecraft/textures/gui/sprites/hud/hotbar.png'); hs = tex('minecraft/textures/gui/sprites/hud/hotbar_selection.png')
        hx0 = (SW - 182) // 2; img.alpha_composite(hb, (hx0, SH - 22)); img.alpha_composite(hs, (hx0 - 1, SH - 23))
        put(img, 'item/eisenspitzhacke', hx0 + 3, SH - 19)
    return img


def boss_bar(img, i, title, frac):
    by = 12 + 19 * i; bx = (SW - 182) // 2
    T(img, SW // 2, by - 9, title, WHITE, center=True)
    rect(img, bx, by, bx + 182, by + 5, hx('#3D0E2C')); rect(img, bx + 1, by + 1, bx + 181, by + 4, hx('#6E1F4E'))
    rect(img, bx + 1, by + 1, bx + 1 + int(180 * frac), by + 4, hx('#E04CA6'))
    rect(img, bx + 1, by + 1, bx + 1 + int(180 * frac), by + 2, hx('#F28CCB'))


def stack_top(bars):
    """TopCenterStackState.topBelowBossBars: 4 px below the last bar, or 4 px without bars."""
    return 12 + 19 * (bars - 1) + 5 + 4 if bars else 4


SAVED = []


def export(img, name, window, alt):
    """Cuts the 16:9 window (x, y, w, h) out of the logical screen and writes PNG (3x) and WEBP (960x540)."""
    x, y, w, h = window
    assert w * 9 == h * 16, window
    crop = img.crop((x, y, x + w, y + h))
    big = crop.resize((w * K, h * K), Image.NEAREST).convert('RGB')
    big.save(os.path.join(OUT, name + '.png'))
    web = big if big.size == (960, 540) else big.resize((960, 540), Image.LANCZOS)
    web.save(os.path.join(OUT, name + '.webp'), lossless=big.size == (960, 540), quality=92, method=6)
    SAVED.append((name, crop, alt))
    return crop


# ---------------------------------------------------------------------------------------------------------
# HUD parts after the current client code
# ---------------------------------------------------------------------------------------------------------
def werk_bar(img, y, text, state='run', clock='0:42', key_label='', locked=False, x=None):
    """WerkstattHud.draw / WerkstattBarLayout: 20 px high, at least 182 wide, bar fill in ramp[2]."""
    ramp = {'run': GOLD, 'pause': GREY, 'stop': LOW, 'done': READY}[state]
    col = {'run': NAME, 'pause': SUB, 'stop': LOW[2], 'done': READY[2]}[state]
    right = 8 if state == 'done' else 7 + 2 + tw(clock) + (9 if locked else 0) + (tw(key_label) + 4 if key_label else 0)
    w = max(182, 26 + tw(text) + 8 + right + 6); h = 20
    if x is None: x = (SW - w) // 2
    panel(img, x, y, w, h); stripe(img, x, y, h, ramp)
    put(img, 'item/salz', x + 7, y + 2)
    T(img, x + 26, y + 4, text, col)
    rx = x + w - 6
    if state == 'done':
        check(img, rx - 7, y + 4, READY[2])
    else:
        cw_ = tw(clock); fx = rx - cw_ - 2 - 7
        flame(img, fx, y + 4, FIRE if state == 'run' else GREY)
        T(img, rx - cw_, y + 4, clock, LABEL if state == 'stop' else SUB)
        left = fx
        if locked: left -= 9; lock(img, left, y + 4, OCHRE)
        if key_label: left -= 4 + tw(key_label); T(img, left, y + 4, key_label, LABEL)
    bw = w - 26 - 6
    rect(img, x + 26, y + 14, x + 26 + bw, y + 17, OUTL); rect(img, x + 27, y + 15, x + 25 + bw, y + 16, D2)
    frac = 1.0 if state == 'done' else 23 / 27
    f = int((bw - 2) * frac)
    if f > 0: rect(img, x + 27, y + 15, x + 27 + f, y + 16, ramp[2])
    return x, w, h


HORSE = ["..........mm....",
         ".........mbbl...",
         "........mbbbbl..",
         "........mbbbwbb.",
         ".......mbbbb.dd.",
         "......mbbbb.....",
         "..lbbbbbbbb.....",
         ".lbbbbbbbbbb....",
         "mlbbbbbbbbbd....",
         "m.bbbbbbbbd.....",
         "m.bbd.dbbdd.....",
         "..bd..d..bd.....",
         "..dd..d..dd.....",
         "..dd..d...d.....",
         "..kk..k...k.....",
         "................"]
HORSE_C = {'b': hx('#8A5E36'), 'l': hx('#A87444'), 'd': hx('#5C3D22'), 'm': hx('#3A281A'), 'k': hx('#2A2018'),
           'w': hx('#E8E0D0')}


def sprite(rows, colours, s=1):
    im = Image.new('RGBA', (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in colours: im.putpixel((i, j), colours[ch])
    return im.resize((im.size[0] * s, im.size[1] * s), Image.NEAREST) if s > 1 else im


def animal_toast(img, x, y):
    """AnimalToastCards.detailed (160x62) for a waiting horse; age line left out (drawn at 0.75 scale)."""
    w, h = 160, 62
    ramp = GOLD  # WAITING ramp
    panel(img, x, y, w, h)
    rect(img, x + 3, y + 3, x + w - 3, y + 15, ramp[0]); rect(img, x + 3, y + 3, x + w - 3, y + 4, ramp[1])
    status = 'Wartet noch 3h 20m'
    lead = 11; gx = x + (w - lead - tw(status)) // 2
    img.alpha_composite(own_icon('gui/animalinfo/wartet.png'), (gx, y + 4))
    T(img, gx + lead, y + 5, status, NAME)
    portrait(img, x + 8, y + 22, sprite(HORSE, HORSE_C))
    T(img, x + 31, y + 21, 'Kastanie', OCHRE)
    img.alpha_composite(own_icon('gui/animalinfo/weiblich.png'), (x + 31 + tw('Kastanie') + 3, y + 21))
    T(img, x + 31, y + 31, 'Stute', SUB)
    hbar(img, x + 31, y + 44, w - 40, 5, .45, ramp)
    T(img, x + 31, y + 51, 'Paarung', LABEL)
    return h


def conditions_hud(img, x, y, names):
    """ConditionsHud: black outline, BG 0xE81B1B22, title gold, Leiden red, one name per line."""
    title = 'Zustände'
    w = max([tw(title)] + [tw(n) for n, _ in names]) + 10 + 1
    h = 16 + len(names) * 10
    rect(img, x, y, x + w, y + h, BLACK); rect(img, x + 1, y + 1, x + w - 1, y + h - 1, (27, 27, 34, 232))
    T(img, x + 5, y + 4, title, hx('#E8C77A'))
    for i, (n, leiden) in enumerate(names):
        T(img, x + 5, y + 15 + i * 10, n, hx('#E07070') if leiden else hx('#8CC8E8'))
    return w, h


def activity_hud(img, x, y, rows):
    """RegionActivityHud with the footer hidden: title, divider, one row per Gefolge with dot and arms."""
    title = 'Aktivitäten'
    w = max(104, tw(title) + 8, max(27 + tw(n) + 4 for n, _, _ in rows))
    h = 15 + 16 * len(rows) + 3
    panel(img, x, y, w, h)
    T(img, x + 4, y + 3, title, OCHRE)
    rect(img, x + 2, y + 12, x + w - 2, y + 13, (20, 20, 24, 200)); rect(img, x + 2, y + 13, x + w - 2, y + 14, (59, 58, 69, 200))
    lv = {'high': (hx('#CD9F3D'), hx('#DBB451'), hx('#96752C')), 'very': (hx('#B24147'), hx('#C4545A'), hx('#6E2226'))}
    for i, (name, level, arm) in enumerate(rows):
        ry = y + 15 + 16 * i
        c = (59, 58, 69, 80)
        rect(img, x + 4, ry, x + w - 4, ry + 15, c); rect(img, x + 3, ry + 1, x + 4, ry + 14, c); rect(img, x + w - 4, ry + 1, x + w - 3, ry + 14, c)
        col, light, dark = lv[level]; dx, dy = x + 5, ry + 5
        rect(img, dx + 1, dy, dx + 3, dy + 3, col); rect(img, dx, dy + 1, dx + 1, dy + 3, col)
        rect(img, dx + 1, dy + 3, dx + 3, dy + 4, dark); rect(img, dx + 3, dy + 1, dx + 4, dy + 3, dark); px(img, dx + 1, dy + 1, light)
        img.alpha_composite(arms(arm, 12), (x + 12, ry + 2))
        T(img, x + 28, ry + 4, name, NAME)
    return w, h


def text_scaled(img, x, y, s, c, scale):
    tmp = Image.new('RGBA', (tw(s) + 2, 10), (0, 0, 0, 0)); T(tmp, 0, 1, s, c)
    tmp = tmp.resize((tmp.size[0] * scale, tmp.size[1] * scale), Image.NEAREST)
    img.alpha_composite(tmp, (x, y - scale))


def _lerp(a, b, f):
    f = max(0.0, min(1.0, f)); return tuple(round(a[i] + (b[i] - a[i]) * f) for i in range(3))


def plakette(img, name, sub, arm, top=6):
    """PlaketteStyle after the intro (no sheen): gradient plate, bevel, gold brackets, arms, name 2x, separator."""
    D0, D1_, D2_ = (0x14, 0x14, 0x18), (0x1E, 0x1E, 0x24), (0x2E, 0x2D, 0x35)
    nameW, subW = tw(name) * 2, tw(sub)
    textW = max(nameW, subW)
    w = 8 + 24 + 8 + 14 + textW
    blockH = 16 + 2 + 1 + 3 + 8
    h = max(24 + 14, blockH + 8)
    x = (SW - w) // 2; y = top
    rows = h - 2
    for i in range(rows):
        f = (i / (rows - 1)) * 2 - 1
        c = _lerp(D1_, D2_, -f * .55) if f < 0 else _lerp(D1_, D0, f * .7)
        rect(img, x + 1, y + 1 + i, x + w - 1, y + 2 + i, c + (235,))
    L, Dk = hx('#514F59'), hx('#2E2D35')
    rect(img, x + 2, y + 1, x + w - 2, y + 2, L); rect(img, x + 1, y + 2, x + 2, y + h - 2, L)
    rect(img, x + 2, y + h - 2, x + w - 2, y + h - 1, Dk); rect(img, x + w - 2, y + 2, x + w - 1, y + h - 2, Dk)
    O = hx('#141418')
    for a in ((x + 1, y, x + w - 1, y + 1), (x + 1, y + h - 1, x + w - 1, y + h), (x, y + 1, x + 1, y + h - 1),
              (x + w - 1, y + 1, x + w, y + h - 1), (x + 1, y + 1, x + 2, y + 2), (x + w - 2, y + 1, x + w - 1, y + 2),
              (x + 1, y + h - 2, x + 2, y + h - 1), (x + w - 2, y + h - 2, x + w - 1, y + h - 1)):
        rect(img, *a, O)
    ax, ay = x + 8, y + (h - 24) // 2
    img.alpha_composite(arms(arm, 24), (ax, ay))
    tx = ax + 24 + 8; ty = y + (h - blockH) // 2
    text_scaled(img, tx, ty, name, hx('#F0EBE0'), 2)
    sy = ty + 16 + 2
    solid = textW - 10
    rect(img, tx, sy, tx + solid, sy + 1, hx('#866C36'))
    for i in range(10):
        a = round(255 * (1 - (i + 1) / 11)); px(img, tx + solid + i, sy, hx('#866C36', a))
    T(img, tx, sy + 4, sub, hx('#D6C194'))
    SH_, GD, GL = hx('#47391D'), hx('#B58D36'), hx('#DBB451')
    l, r, t, b = x + 3, x + w - 4, y + 3, y + h - 4

    def br(cx, cy, dx, dy, c, cc):
        rect(img, cx + 1 if dx > 0 else cx - 4, cy, cx + 5 if dx > 0 else cx, cy + 1, c)
        rect(img, cx, cy + 1 if dy > 0 else cy - 4, cx + 1, cy + 5 if dy > 0 else cy, c)
        px(img, cx, cy, cc)
    for (cx, cy, dx, dy) in ((l, t, 1, 1), (r, t, -1, 1), (l, b, 1, -1), (r, b, -1, -1)):
        br(cx + 1, cy + 1, dx, dy, SH_, SH_)
    for (cx, cy, dx, dy) in ((l, t, 1, 1), (r, t, -1, 1), (l, b, 1, -1), (r, b, -1, -1)):
        br(cx, cy, dx, dy, GD, GL)
    return x, w, h


CROSS = ["..XXX..", "..XXX..", "XXXXXXX", "XXXXXXX", "XXXXXXX", "..XXX..", "..XXX.."]


def name_tag(img, cx, y, name):
    """Vanilla name tag: 25 % black plate, white name; ConditionsDisplay appends ' ✚' in RED (FF5555)."""
    w = tw(name) + 4 + 7
    x = cx - w // 2
    rect(img, x - 1, y - 1, x + w + 1, y + 8, (0, 0, 0, 64))
    T(img, x, y, name, WHITE)
    for j, row in enumerate(CROSS):
        for i, ch in enumerate(row):
            if ch == 'X': px(img, x + tw(name) + 4 + i, y + j, hx('#FF5555'))


PLAYER = ["..hhhh..", ".hhhhhh.", ".hssssh.", ".sesses.", ".ssssss.", "..smms..", "cccccccc", "acccccca",
          "acccccca", "acccccca", "acbbbbca", "s.cccc.s", "..pppp..", "..pp.pp.", "..pp.pp.", "..kk.kk."]
PLAYER_C = {'h': hx('#3A2618'), 's': hx('#C69C7A'), 'e': hx('#2A2A40'), 'm': hx('#8A5A48'), 'c': hx('#5A6E3A'),
            'a': hx('#4C5E30'), 'b': hx('#6E4A2A'), 'p': hx('#46423C'), 'k': hx('#2A2420')}


# ---------------------------------------------------------------------------------------------------------
# 1. Werkstatt: Zeugtisch with the opened T2 tab panel (Auftrag/Brennstoff/Warteschlange)
# ---------------------------------------------------------------------------------------------------------
sc = W['reiter_open']('t2', ' ')
sc = sc.crop((0, 0, sc.size[0], sc.size[1] - W['CAP']))
cw_, ch_ = SW, SH
# Minecraft centres the 176x222 chest GUI on the screen; the panel hangs to its right.
ox, oy = (SW - 176) // 2 - W['MX'], (SH - 222) // 2 - W['MT']
canvas = _BIG_DARK.crop((BIG_OFF - ox, BIG_OFF - oy, BIG_OFF - ox + cw_, BIG_OFF - oy + ch_))
canvas.alpha_composite(sc, (ox, oy))
export(canvas, 'werkstatt-zeugtisch-panel', (160, 52, 480, 270),
       'Zeugtisch-Menü mit aufgeklapptem Werkstatt-Panel: Reiter Auftrag, Brennstoff und Warteschlange, '
       'gewählte Eisenspitzhacke mit Zutaten, Menge und Herstellen-Knopf')

# 2. Werkstatt bar under a boss bar
img = world(crosshair=False)
boss_bar(img, 0, 'Belagerung von Schwarzhain', .62)
werk_bar(img, stack_top(1), 'Salz 23/27 · 4 fehlgeschlagen')
export(img, 'werkstatt-fortschritt', (160, 0, 320, 180),
       'Schmale Werkstatt-Leiste oben in der Mitte unter einer Bossleiste: Salz 23 von 27, 4 fehlgeschlagen, '
       'Feuer noch 0:42')

# 3. Vote reminder card C (key Enter, dismiss X like the default key bindings)
img = world(crosshair=False)
vote_c(img, (SW - 196) // 2, stack_top(0), k='Enter', dk='X', left=.7)
export(img, 'vote-erinnerung', (160, 0, 320, 180),
       'Vote-Erinnerung oben in der Mitte: Siegel, Spruch des Tages, Enter zum Abstimmen, X für später')

# 4. Top-centre stack: Werkstatt bar, animal toast, zone toast, vote card
img = world(crosshair=False)
boss_bar(img, 0, 'Belagerung von Schwarzhain', .62)
y = stack_top(1)
_, _, h = werk_bar(img, y, 'Salz 23/27 · 4 fehlgeschlagen'); y += h + 2
y += animal_toast(img, (SW - 160) // 2, y) + 2
y += zone_toast(img, (SW - 196) // 2, y) + 2
vote_c(img, (SW - 196) // 2, y, k='Enter', dk='X', left=.7)
export(img, 'hud-stapel-oben-mitte', (80, 0, 480, 270),
       'Gestapelte Anzeigen oben in der Mitte unter einer Bossleiste: Werkstatt-Leiste, Tier-Info, '
       'Zonen-Hinweis und Vote-Erinnerung mit gleichem Abstand')

# 5. Leiden / Zustände: other player with red cross, own conditions list
img = world()
pl = sprite(PLAYER, PLAYER_C, 4)
pxx, pyy = 300 - pl.size[0] // 2, 150
img.alpha_composite(pl, (pxx, pyy))
name_tag(img, 300, pyy - 14, 'Alrik vom Mühlhof')
conditions_hud(img, 4, 60, [('Übermüdet', True), ('Verdreckt', True)])
export(img, 'leiden-zustaende', (0, 0, 480, 270),
       'Mitspieler mit rotem Kreuz am Namensschild für ein Leiden, links die eigene Zustandsliste mit '
       'Übermüdet und Verdreckt')

# 6. Animal info toast while looking at a horse (no name tag)
img = world()
hz = sprite(HORSE, HORSE_C, 5)
img.alpha_composite(hz, (SW // 2 - 44, SH // 2 - 40))
animal_toast(img, (SW - 160) // 2, stack_top(0))
export(img, 'tier-info-pferd', (80, 0, 480, 270),
       'Tier-Info beim Anschauen eines Pferdes: Karte mit Name Kastanie, Stute, wartet noch 3h 20m auf die '
       'nächste Paarung')

# 7. Region entry banner "Plakette"
img = world(crosshair=False)
plakette(img, 'Aehrstein', 'Lehen von Gefolge Aehrstein', 'wappen_aehrstein_gruen_golden.png')
export(img, 'lehen-plakette', (160, 0, 320, 180),
       'Lehensbanner im Plakettenstil beim Betreten: Wappen, Lehensname Aehrstein und Untertitel mit '
       'goldenen Eckwinkeln')

# 8. HUD column on the right: before / after
ROWS = [('Aehrstein', 'very', 'wappen_aehrstein_gruen_golden.png'), ('Achental', 'high', 'wappen_achental_schwarz_silber.png'),
        ('Alteria', 'high', 'wappen_alteria.png')]
CONDS = [('Übermüdet', True), ('Verdreckt', True)]


def right_col(after):
    img = world()
    aw, ah = activity_hud(img, 0, 0, ROWS)  # measure
    img = world()
    y = 4
    aw, ah = activity_hud(img, SW - 4 - aw, y, ROWS)
    if after: y += ah + 2
    tmp = Image.new('RGBA', (400, 40)); bx, bw, bh = werk_bar(tmp, 0, 'Salz 23/27', x=0)
    werk_bar(img, y, 'Salz 23/27', x=SW - 4 - bw)
    if after: y += bh + 2
    tmp = Image.new('RGBA', (200, 60)); cw2, chh = conditions_hud(tmp, 0, 0, CONDS)
    conditions_hud(img, SW - 4 - cw2, y, CONDS)
    return img


export(right_col(False), 'hud-spalte-vorher', (320, 0, 320, 180),
       'Vorher: drei Anzeigen oben rechts liegen übereinander und verdecken sich')
export(right_col(True), 'hud-spalte-nachher', (320, 0, 320, 180),
       'Nachher: Aktivitäten, Werkstatt-Leiste und Zustände schieben sich untereinander, 2 Pixel Abstand')

# --- contact sheet ---
tiles = []
for name, crop, _ in SAVED:
    t = crop.resize((320, 180), Image.NEAREST) if crop.size == (320, 180) else crop.resize((320, 180), Image.LANCZOS)
    lab = Image.new('RGBA', (320, 192), (12, 12, 16, 255)); lab.alpha_composite(t.convert('RGBA'), (0, 12))
    T(lab, 4, 2, name, NAME, maxw=312); tiles.append(lab)
cols = 2; rows = (len(tiles) + 1) // 2
sheet = Image.new('RGBA', (cols * 320 + 12, rows * 192 + (rows + 1) * 4), (0, 0, 0, 255))
for i, t in enumerate(tiles):
    sheet.alpha_composite(t, (4 + (i % 2) * 324, 4 + (i // 2) * 196))
sheet.resize((sheet.size[0] * 2, sheet.size[1] * 2), Image.NEAREST).convert('RGB').save(os.path.join(OUT, 'uebersicht.png'))
for name, _, alt in SAVED:
    print(name, '|', alt)
