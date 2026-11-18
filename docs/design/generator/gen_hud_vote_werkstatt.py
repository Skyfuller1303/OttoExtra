"""HUD mock-ups: vote reminder toast (A/B/C) and the Werkstatt progress card at the top centre (P1/P2/P3).

Style of the approved animal/zone HUD toasts (gen_zonen_toast_hud.py, F-J): translucent dark panel with
outline and bevel, status stripe, portrait frame, ochre name, ramp-coloured status. P2 uses the pack's
parchment and cord frame instead. No text shadows anywhere. Minecraft pixel font from mcfont/ascii.png.
Everything is drawn on a 640x360 logical screen (1920x1080 at GUI scale 3) and saved 3x.

Vanilla boss bar geometry (BossBarHud): bar 182x5 centred, first bar at y=12, every further bar +19,
title text 9 px above its bar. The Werkstatt card goes 4 px below the last bar, or to y=4 without bars.

Facts from the code: the vote toast sits at the top centre (y=6), the vote key is a key binding with
default ENTER, there are 4 vote URLs, and the mod does not track single sites. The Werkstatt HUD has the
phases running/paused/stopped/finished, the step texts from de_de.json and the view-lock hint
"Sicht gesperrt - Stopp mit %s". Values marked * in the captions are assumptions.

Usage: python -I gen_hud_vote_werkstatt.py <pack_assets_dir> <hud_out_dir>
  pack_assets_dir = Resourcepack/ottonien-reformed-update/assets, hud_out_dir = OttoExtra/docs/design/hud
"""
import os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
_src = open(os.path.join(HERE, 'gen_werkstatt.py'), encoding='utf-8').read()


def _chunk(start, end):
    return _src[_src.index(start):_src.index(end)]


exec(_src[:_src.index('# --- the real server GUI')])
exec(_chunk('def cord_frame', 'def checkbox'))
exec(_chunk('def lock(', 'def slot_box'))
exec(_chunk('def check(', 'def cursor'))

ROOT = OUT
VOTE_OUT, WERK_OUT = os.path.join(ROOT, 'vote'), os.path.join(ROOT, 'werkstatt')
os.makedirs(VOTE_OUT, exist_ok=True); os.makedirs(WERK_OUT, exist_ok=True)
OE_TEX = os.path.join(HERE, '..', '..', '..', 'src', 'main', 'resources', 'assets', 'ottoextra', 'textures')
SIEGEL = Image.open(os.path.join(OE_TEX, 'gui', 'siegelshop.png')).convert('RGBA')

_glyph = glyph


def glyph(ch):
    """Middle dot is not in ascii.png: one pixel at x height."""
    if ch == '·':
        g = Image.new('RGBA', (8, 8), (0, 0, 0, 0)); g.putpixel((0, 3), (255, 255, 255, 255)); return g
    return _glyph(ch)


SW, SH, CAP = 640, 360, 14
PANEL, OUTLINE = (30, 30, 36, 150), (20, 20, 24, 215)
BEV_L, BEV_D = (81, 79, 89, 215), (46, 45, 53, 215)
WAIT = GOLD
SOON = FIRE
GREY = [D3, D4, SUB]
WHITE = hx('#FFFFFF')


# --- HUD pieces in the zone toast style ---
def panel(img, x, y, w, h):
    r, b = x + w, y + h
    for a in ((x + 1, y, r - 1, y + 1), (x + 1, b - 1, r - 1, b), (x, y + 1, x + 1, b - 1), (r - 1, y + 1, r, b - 1),
              (x + 1, y + 1, x + 2, y + 2), (r - 2, y + 1, r - 1, y + 2), (x + 1, b - 2, x + 2, b - 1),
              (r - 2, b - 2, r - 1, b - 1)):
        rect(img, *a, OUTLINE)
    rect(img, x + 2, y + 1, r - 2, y + 2, BEV_L); rect(img, x + 1, y + 2, x + 2, b - 2, BEV_L)
    rect(img, x + 2, b - 2, r - 2, b - 1, BEV_D); rect(img, r - 2, y + 2, r - 1, b - 2, BEV_D)
    rect(img, x + 2, y + 2, r - 2, b - 2, PANEL)


def stripe(img, x, y, h, ramp):
    rect(img, x + 2, y + 2, x + 5, y + h - 2, ramp[1]); rect(img, x + 2, y + 2, x + 5, y + 3, ramp[2])


def portrait(img, x, y, icon):
    rect(img, x - 2, y - 2, x + 18, y + 18, OUTL); rect(img, x - 1, y - 1, x + 17, y + 17, D3)
    img.alpha_composite(icon, (x, y))


def hbar(img, x, y, w, h, frac, ramp):
    rect(img, x, y, x + w, y + h, OUTL); rect(img, x + 1, y + 1, x + w - 1, y + h - 1, D2)
    f = int((w - 2) * max(0, min(1, frac)))
    if f > 0:
        rect(img, x + 1, y + 1, x + 1 + f, y + h - 1, ramp[1]); rect(img, x + 1, y + 1, x + 1 + f, y + 2, ramp[2])


def key(img, x, y, label, dark=True):
    """Key cap, 10 px high. Returns its width."""
    w = tw(label) + 6
    rect(img, x + 1, y, x + w - 1, y + 10, OUTL if dark else CORD[0]); rect(img, x, y + 1, x + w, y + 9, OUTL if dark else CORD[0])
    rect(img, x + 1, y + 1, x + w - 1, y + 9, D3 if dark else SL)
    rect(img, x + 1, y + 8, x + w - 1, y + 9, D2 if dark else SL_S)
    T(img, x + 3, y + 1, label, NAME)
    return w


def key_w(label): return tw(label) + 6


def clock(img, x, y, c):
    for a, b in ((2, 0), (3, 0), (4, 0), (1, 1), (5, 1), (0, 2), (6, 2), (0, 3), (6, 3), (0, 4), (6, 4), (1, 5), (5, 5),
                 (2, 6), (3, 6), (4, 6), (3, 2), (3, 3), (4, 3)):
        px(img, x + a, y + b, c)


def item16(name): return item(f'item/{name}')


# --- screen: dimmed world, crosshair, hotbar, optional vanilla boss bars ---
def screen(caption, bars=0, seed=7):
    img = backdrop(SW, SH + CAP, seed, dim=70)
    cx, cy = SW // 2, SH // 2
    rect(img, cx - 4, cy, cx + 5, cy + 1, (230, 230, 230, 200)); rect(img, cx, cy - 4, cx + 1, cy + 5, (230, 230, 230, 200))
    hb = tex('minecraft/textures/gui/sprites/hud/hotbar.png'); hs = tex('minecraft/textures/gui/sprites/hud/hotbar_selection.png')
    hx0 = (SW - 182) // 2; img.alpha_composite(hb, (hx0, SH - 22)); img.alpha_composite(hs, (hx0 - 1, SH - 23))
    put(img, 'item/eisenspitzhacke', hx0 + 3, SH - 19)
    names = ['Belagerung von Schwarzhain*', 'Wetterwarnung: Sturm*']
    for i in range(bars):
        by = 12 + 19 * i; bx = (SW - 182) // 2
        T(img, SW // 2, by - 9, names[i], WHITE, center=True)
        rect(img, bx, by, bx + 182, by + 5, hx('#3D0E2C')); rect(img, bx + 1, by + 1, bx + 181, by + 4, hx('#6E1F4E'))
        rect(img, bx + 1, by + 1, bx + 1 + int(180 * (.62 - .3 * i)), by + 4, hx('#E04CA6'))
        rect(img, bx + 1, by + 1, bx + 1 + int(180 * (.62 - .3 * i)), by + 2, hx('#F28CCB'))
    rect(img, 0, SH, SW, SH + CAP, (0, 0, 0, 230))
    T(img, 6, SH + 3, caption, NAME, maxw=SW - 12)
    return img


def top_y(bars): return 12 + 19 * (bars - 1) + 5 + 4 if bars else 4


def save(img, folder, name):
    img.resize((img.size[0] * K, img.size[1] * K), Image.NEAREST).save(os.path.join(folder, name))
    return img


# --- the zone toast F, used as the "other" toast in the stacks ---
def zone_toast(img, x, y):
    w, h = 196, 30
    panel(img, x, y, w, h); stripe(img, x, y, h, WAIT)
    portrait(img, x + 9, y + 7, item16('eisenerz'))
    T(img, x + 31, y + 6, 'Eisenmine Nordstollen', OCHRE, maxw=110)
    T(img, x + 31, y + 17, 'Erholt sich', WAIT[2])
    rx = x + w - 52
    T(img, rx + 46, y + 6, '70 %', NAME, right=True)
    hbar(img, rx, y + 17, 46, 4, .7, WAIT)
    return h


# =============================== VOTE ===============================
VW = 196


def vote_a(img, x, y, k='V'):
    """A: one-liner. Seal, title, key hint right."""
    h = 20
    title = 'Heute schon gewählt?'
    w = max(VW, 28 + tw(title) + 8 + key_w(k) + 3 + tw('Abstimmen') + 6)
    x = x + VW // 2 - w // 2
    panel(img, x, y, w, h); stripe(img, x, y, h, WAIT)
    img.alpha_composite(SIEGEL, (x + 8, y + 2))
    kx = x + w - 6 - tw('Abstimmen') - 3 - key_w(k)
    T(img, x + 28, y + 6, title, OCHRE, maxw=kx - 6 - (x + 28))
    key(img, kx, y + 5, k); T(img, kx + key_w(k) + 3, y + 6, 'Abstimmen', SUB)
    return h


def pips(img, x, y, done, total, ramp_done, w=46):
    gap = 2; pw = (w - gap * (total - 1)) // total
    for i in range(total):
        px0 = x + i * (pw + gap)
        rect(img, px0, y, px0 + pw, y + 4, OUTL)
        if i < done:
            rect(img, px0 + 1, y + 1, px0 + pw - 1, y + 3, ramp_done[1]); rect(img, px0 + 1, y + 1, px0 + pw - 1, y + 2, ramp_done[2])
        else:
            rect(img, px0 + 1, y + 1, px0 + pw - 1, y + 3, D2)


def vote_b(img, x, y, done=2, total=4, nxt='03:42'):
    """B: compact card with site count and time to the next round."""
    h = 30
    fin = done >= total
    ramp = READY if fin else WAIT
    panel(img, x, y, VW, h); stripe(img, x, y, h, ramp)
    portrait(img, x + 9, y + 7, SIEGEL)
    rx = x + VW - 52
    T(img, x + 31, y + 6, 'Alles gewählt' if fin else 'Das Siegel wartet', OCHRE, maxw=rx - 4 - (x + 31))
    T(img, x + 31, y + 17, f'{done} von {total} erledigt' if fin else f'{done} von {total} Seiten offen', ramp[2],
      maxw=rx - 4 - (x + 31))
    clock(img, rx + 46 - tw(nxt) - 10, y + 6, LABEL); T(img, rx + 46, y + 6, nxt, NAME, right=True)
    pips(img, rx, y + 17, done, total, READY)
    T(img, rx + 46, y + 23, '', LABEL)
    return h


def vote_c(img, x, y, k='V', dk='X', left=.6):
    """C: detailed card with header band, key hint and dismiss hint, thin remaining-time line."""
    h = 58
    panel(img, x, y, VW, h)
    rect(img, x + 3, y + 3, x + VW - 3, y + 15, WAIT[0]); rect(img, x + 3, y + 3, x + VW - 3, y + 4, WAIT[1])
    T(img, x + VW // 2, y + 5, 'Vote-Erinnerung', NAME, center=True)
    portrait(img, x + 9, y + 22, SIEGEL)
    T(img, x + 33, y + 21, 'Setz dein Zeichen', OCHRE, maxw=VW - 39)
    T(img, x + 33, y + 31, 'Für Ottonien. Für heute.', SUB, maxw=VW - 39)
    fy = y + 42
    w1 = key(img, x + 33, fy, k); T(img, x + 33 + w1 + 3, fy + 1, 'Abstimmen', NAME)
    later = 'Später'
    kx = x + VW - 6 - tw(later) - 3 - key_w(dk)
    key(img, kx, fy, dk); T(img, kx + key_w(dk) + 3, fy + 1, later, LABEL)
    rect(img, x + 3, y + h - 4, x + VW - 3, y + h - 3, D2)
    rect(img, x + 3, y + h - 4, x + 3 + int((VW - 6) * left), y + h - 3, WAIT[1])
    return h


VX = (SW - VW) // 2
VOTES = [('a', 'A: Einzeiler, Siegel + [V] Abstimmen*', vote_a),
         ('b', 'B: Karte, 2 von 4 Seiten offen*, nächste Runde in 03:42*', vote_b),
         ('c', 'C: Karte mit Taste [V]* und Wegklicken [X]*, Restzeit-Linie', vote_c)]
VLOG = {}
for tag, cap, fn in VOTES:
    img = screen(cap + ' - Spielansicht, GUI 3'); fn(img, VX, 6)
    VLOG[tag] = save(img, VOTE_OUT, f'vote_{tag}_ingame.png')
    img = screen(cap + ' - gestapelt unter Zonen-Toast')
    hz = zone_toast(img, VX, 6); fn(img, VX, 6 + hz + 2)
    VLOG[tag + '_st'] = save(img, VOTE_OUT, f'vote_{tag}_gestapelt.png')
img = screen('B nach dem Voten: alle Seiten erledigt, Countdown zur nächsten Runde*')
vote_b(img, VX, 6, 4, 4, '05:12')
VLOG['b_done'] = save(img, VOTE_OUT, 'vote_b_erledigt.png')


def crop_top(im, h, w=300):
    return im.crop(((SW - w) // 2, 0, (SW + w) // 2, h))


def labelled(im, text):
    out = Image.new('RGBA', (im.size[0], im.size[1] + 12), (12, 12, 16, 255))
    T(out, 4, 2, text, NAME, maxw=im.size[0] - 8); out.alpha_composite(im, (0, 12)); return out


def sheet(tiles, cols, folder, name, pad=4):
    cw_ = max(t.size[0] for t in tiles)
    rows = (len(tiles) + cols - 1) // cols
    rh = [max(t.size[1] for t in tiles[r * cols:(r + 1) * cols]) for r in range(rows)]
    out = Image.new('RGBA', (cols * cw_ + (cols + 1) * pad, sum(rh) + (rows + 1) * pad), (0, 0, 0, 255))
    for i, t in enumerate(tiles):
        out.alpha_composite(t, (pad + (i % cols) * (cw_ + pad), pad + sum(rh[:i // cols]) + (i // cols) * pad))
    save(out, folder, name)


sheet([labelled(crop_top(VLOG['a'], 34), 'A Einzeiler'), labelled(crop_top(VLOG['a_st'], 64), 'A gestapelt'),
       labelled(crop_top(VLOG['b'], 42), 'B Seitenzahl'), labelled(crop_top(VLOG['b_st'], 72), 'B gestapelt'),
       labelled(crop_top(VLOG['b_done'], 42), 'B erledigt'), labelled(crop_top(VLOG['c_st'], 104), 'C gestapelt'),
       labelled(crop_top(VLOG['c'], 70), 'C Taste + Wegklicken')], 2, VOTE_OUT, 'vote_uebersicht.png')

# ============================= WERKSTATT =============================
SALZ = item16('salz')
STATES = {
    'run': dict(ramp=GOLD, frac=23 / 27),
    'pause': dict(ramp=GREY, frac=23 / 27),
    'stop': dict(ramp=LOW, frac=23 / 27),
    'done': dict(ramp=READY, frac=1.0),
}
FIRE_T = '0:42'


def fire_w(): return 7 + 2 + tw(FIRE_T)


def draw_fire(img, x, y, c=SUB, ramp=FIRE):
    flame(img, x, y, ramp); T(img, x + 9, y, FIRE_T, c)


def p1(img, cx, y, st, locked=False):
    """P1: slim bar. Icon, one text line, fire timer right, thin bar under the text."""
    s = STATES[st]; ramp = s['ramp']
    text = {'run': 'Salz 23/27 · 4 fehlgeschlagen', 'pause': 'Salz 23/27 · pausiert',
            'stop': 'Salz gestoppt: weggegangen', 'done': '23 von 27 hergestellt, 4 fehlgeschlagen'}[st]
    col = {'run': NAME, 'pause': SUB, 'stop': LOW[2], 'done': READY[2]}[st]
    right = 8 if st == 'done' else fire_w() + (9 if locked else 0)
    w = max(182, 26 + tw(text) + 8 + right + 6)
    x = cx - w // 2; h = 20
    panel(img, x, y, w, h); stripe(img, x, y, h, ramp)
    img.alpha_composite(SALZ, (x + 7, y + 2))
    T(img, x + 26, y + 4, text, col)
    rx = x + w - 6
    if st == 'done':
        check(img, rx - 7, y + 4, READY[2])
    else:
        draw_fire(img, rx - fire_w(), y + 4, SUB if st != 'stop' else LABEL, FIRE if st == 'run' else GREY)
        if locked: lock(img, rx - fire_w() - 9, y + 4, OCHRE)
    hbar(img, x + 26, y + 14, w - 32, 3, s['frac'], ramp)
    return h, x, w


def p2(img, cx, y, st, locked=False):
    """P2: wide banner like a boss bar, in the pack's parchment with cord frame."""
    s = STATES[st]; ramp = s['ramp']
    w, h = 256, 38; x = cx - w // 2
    parchment(img, x, y, w, h, seed=5)
    img.alpha_composite(SALZ, (x + 7, y + 11))
    c0, c1 = x + 28, x + w - 8
    title = {'run': 'Salz 23/27 · 4 fehlgeschlagen', 'pause': 'Salz 23/27 · pausiert', 'stop': 'Salz gestoppt',
             'done': '23 von 27 hergestellt, 4 fehlgeschlagen'}[st]
    tcol = {'run': INK_T, 'pause': INK_M, 'stop': BAD_INK, 'done': OK_INK}[st]
    T(img, (c0 + c1) // 2, y + 5, title, tcol, maxw=c1 - c0, center=True)
    bar(img, c0, y + 15, c1 - c0, 6, s['frac'], ramp, back=PA_D)
    step = {'run': 'Warte auf Ergebnis', 'pause': 'Weiter im Werkstatt-Panel', 'stop': 'Grund: weggegangen',
            'done': 'Fertig'}[st]
    scol = {'run': INK_M, 'pause': INK_M, 'stop': BAD_INK, 'done': OK_INK}[st]
    sy = y + 25
    if st in ('run', 'pause'):
        draw_fire(img, c0, sy, INK_M, FIRE if st == 'run' else [PA_D, INK_F, PA_L])
    if st == 'run':
        hint = 'Stopp'; kw = key_w('K')
        T(img, c1 - kw - 3, sy, hint, INK_F, right=True); key(img, c1 - kw, sy - 1, 'K', dark=False)
    T(img, (c0 + c1) // 2, sy, step, scol, center=True, maxw=c1 - c0 - 2 * 40)
    if st == 'done': check(img, (c0 + c1) // 2 - tw(step) // 2 - 11, sy, OK_INK)
    if locked:
        lt = 'Sicht gesperrt · Stopp mit'
        lw = 6 + 4 + tw(lt) + 3 + key_w('K') + 6
        lx = cx - lw // 2; ly = y + h - 1
        rect(img, lx, ly, lx + lw, ly + 13, BLACK); rect(img, lx + 1, ly, lx + lw - 1, ly + 12, SL)
        rect(img, lx + 1, ly + 11, lx + lw - 1, ly + 12, SL_S)
        lock(img, lx + 5, ly + 2, GOLDH); T(img, lx + 15, ly + 3, lt, NAME)
        key(img, lx + 15 + tw(lt) + 3, ly + 1, 'K', dark=True)
        return h + 12, x, w
    return h, x, w


def pill_shape(img, x, y, w, h):
    rect(img, x + 2, y, x + w - 2, y + h, OUTLINE); rect(img, x + 1, y + 1, x + w - 1, y + h - 1, OUTLINE)
    rect(img, x, y + 2, x + w, y + h - 2, OUTLINE)
    rect(img, x + 2, y + 1, x + w - 2, y + h - 1, PANEL); rect(img, x + 1, y + 2, x + w - 1, y + h - 2, PANEL)
    rect(img, x + 2, y + 1, x + w - 2, y + 2, BEV_L)


def p3(img, cx, y, st, expanded=False, locked=False, step='Feuer nachlegen'):
    """P3: minimal pill; unfolds into a card for about 3 s* when the step changes, stays open on stop/finish."""
    s = STATES[st]; ramp = s['ramp']
    head = {'run': 'Salz 23/27', 'pause': 'Salz pausiert', 'stop': 'Salz gestoppt', 'done': 'Salz fertig'}[st]
    hcol = {'run': NAME, 'pause': SUB, 'stop': LOW[2], 'done': READY[2]}[st]
    if not expanded:
        lead = 7 if not locked else 6
        w = 6 + lead + 4 + tw(head) + 6 + (key_w('K') + 3 if locked else 0)
        h = 14; x = cx - w // 2
        pill_shape(img, x, y, w, h)
        if locked: lock(img, x + 6, y + 3, OCHRE)
        elif st == 'run': flame(img, x + 6, y + 3, FIRE)
        else: rect(img, x + 7, y + 5, x + 11, y + 9, ramp[1])
        T(img, x + 6 + lead + 4, y + 3, head, hcol)
        if locked: key(img, x + w - 6 - key_w('K') + 3, y + 2, 'K')
        f = int((w - 8) * s['frac']); rect(img, x + 4, y + h - 3, x + 4 + f, y + h - 2, ramp[1])
        return h, x, w
    w, h = 220, 34; x = cx - w // 2
    panel(img, x, y, w, h); stripe(img, x, y, h, ramp)
    img.alpha_composite(SALZ, (x + 8, y + 4))
    line1 = {'run': 'Salz 23/27 · 4 fehlgeschlagen', 'pause': 'Salz 23/27 · pausiert', 'stop': 'Salz gestoppt',
             'done': '23 von 27 hergestellt'}[st]
    T(img, x + 28, y + 4, line1, OCHRE if st == 'run' else hcol, maxw=w - 28 - 6 - (fire_w() + 4 if st == 'run' else 0))
    if st == 'run': draw_fire(img, x + w - 6 - fire_w(), y + 4)
    line2 = {'run': 'Schritt: ' + step, 'pause': 'Weiter im Werkstatt-Panel', 'stop': 'Grund: weggegangen',
             'done': '4 fehlgeschlagen'}[st]
    c2 = {'run': GOLD[2], 'pause': SUB, 'stop': LOW[2], 'done': SUB}[st]
    T(img, x + 28, y + 14, line2, c2, maxw=w - 34)
    hbar(img, x + 28, y + 25, w - 34, 4, s['frac'], ramp)
    return h, x, w


VARS = [('p1', 'P1 schmale Leiste', p1), ('p2', 'P2 Banner Pergament', p2), ('p3', 'P3 Pille', p3)]
CX = SW // 2
WLOG = {}
for tag, title, fn in VARS:
    img = screen(f'{title}: läuft, oben Mitte, keine Bossleiste - GUI 3')
    fn(img, CX, top_y(0), 'run')
    WLOG[tag] = save(img, WERK_OUT, f'werkstatt_{tag}_ingame.png')


def tile(fn, st, label, **kw):
    t = backdrop(300, 64, 11, dim=70)
    h, x, w = fn(t, 150, 4, st, **kw)
    t = t.crop((0, 0, 300, h + 8))
    return labelled(t, label)


STATE_LBL = [('run', 'läuft'), ('pause', 'pausiert'), ('stop', 'gestoppt, mit Grund'),
             ('done', 'fertig')]
for tag, title, fn in VARS:
    tiles = [tile(fn, st, f'{tag.upper()} {lb}') for st, lb in STATE_LBL]
    tiles.append(tile(fn, 'run', f'{tag.upper()} läuft, Sicht gesperrt', locked=True))
    if tag == 'p3':
        tiles.insert(1, tile(fn, 'run', 'P3 Schrittwechsel: klappt auf*', expanded=True))
        tiles[3] = tile(fn, 'stop', 'P3 gestoppt: bleibt offen', expanded=True)
        tiles[4] = tile(fn, 'done', 'P3 fertig: bleibt offen*', expanded=True)
    sheet(tiles, 1, WERK_OUT, f'werkstatt_{tag}_zustaende.png')
    WLOG[tag + '_tiles'] = tiles

# Below vanilla boss bars, plus the vote toast flowing below the card
img = screen('P1 unter 1 Bossleiste, Vote-Toast A fliesst darunter*', bars=1)
h, _, _ = p1(img, CX, top_y(1), 'run'); vote_a(img, VX, top_y(1) + h + 2)
WLOG['b1'] = save(img, WERK_OUT, 'werkstatt_p1_bossleiste.png')
img = screen('P2 unter 2 Bossleisten', bars=2)
p2(img, CX, top_y(2), 'run')
WLOG['b2'] = save(img, WERK_OUT, 'werkstatt_p2_bossleiste.png')
img = screen('P3 unter 1 Bossleiste, aufgeklappt', bars=1)
p3(img, CX, top_y(1), 'run', expanded=True)
WLOG['b3'] = save(img, WERK_OUT, 'werkstatt_p3_bossleiste.png')

over = [labelled(crop_top(WLOG[t], 34 if t != 'p2' else 46, 300), f'{t.upper()} oben Mitte') for t in ('p1', 'p2', 'p3')]
over += [labelled(crop_top(WLOG['b1'], 70, 300), 'P1 + Bossleiste + Vote'),
         labelled(crop_top(WLOG['b2'], 86, 300), 'P2 + 2 Bossleisten'),
         labelled(crop_top(WLOG['b3'], 64, 300), 'P3 + Bossleiste')]
for tag, _, _ in VARS:
    over += WLOG[tag + '_tiles']
sheet(over, 3, WERK_OUT, 'werkstatt_hud_uebersicht.png')
print('ok')
