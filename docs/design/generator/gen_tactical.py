"""Tactical Factions "Politik-Buch" mockups, based on the RP book (gen_rpbuch2.py) with a
blue-grey binding instead of the red one. Usage: python -I gen_tactical.py <out_dir>"""
import os, sys
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_rpbuch2.py'), encoding='utf-8').read()
exec(_b[:_b.index("personen2().resize")])
from PIL import Image

# Blue-grey binding (dark -> light), derived from the slate controls #222E3C / pennant #434D66.
BOOK = [hx('#141A21'), hx('#222B36'), hx('#313C49'), hx('#3B4756'), hx('#4F5C6B')]
ALLY, HOST, NEUT = FRIEND, FOE, NEUTRAL
INHERIT = hx('#7C8C9C')
REL = {'Verbuendet': ALLY, 'Verfeindet': HOST, 'Neutral': NEUT, 'Vererben': INHERIT, 'Freund': ALLY, 'Feind': HOST}
SHORT = {'Verbuendet': 'Freund', 'Verfeindet': 'Feind', 'Neutral': 'Neutral', 'Vererben': 'Erbt'}
TOPS = (('Politik', TERRA_A), ('Trupp', hx('#866C36')), ('Kampf', WAX[1]))
OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)


def shield(img, x, y, s, c1, c2, kind=0):
    """Small heater shield: outline, two-colour field, simple ordinary."""
    o = shade(c1, .35)
    for r in range(s):
        inset = 0 if r < s * .55 else int((r - s * .55) * 1.1)
        if inset * 2 >= s: break
        rect(img, x + inset, y + r, x + s - inset, y + r + 1, o)
        if 0 < r < s - 1 and inset * 2 + 2 < s:
            for cx in range(x + inset + 1, x + s - inset - 1):
                rel = (cx - x) / s
                if kind == 0: c = c1 if rel < .5 else c2                  # per pale
                elif kind == 1: c = c2 if s * .38 < r < s * .62 else c1   # fess
                elif kind == 2: c = c2 if abs(rel - .5) < .14 else c1      # pale
                else: c = c2 if (r < s * .5) == (rel < .5) else c1         # quarterly
                px(img, cx, y + r, c)
    px(img, x + 1, y + 1, shade(c1, 1.25))


ARMS = {'Holdstewik': (hx('#3F6E9E'), hx('#C9A04A'), 0), 'Geldern': (hx('#B58D36'), hx('#3A3A3A'), 1),
        'Wyndham': (hx('#5E9A3E'), hx('#D9D2C2'), 2), 'Kelterfels': (hx('#6E2226'), hx('#C9A04A'), 3),
        'Suedwall': (hx('#B24147'), hx('#D9D2C2'), 1), 'Niederpoelt': (hx('#8A7A66'), hx('#3F6E9E'), 0),
        'Schwarzhain': (hx('#2E2E33'), hx('#AA6841'), 2), 'Rabenfurt': (hx('#434D66'), hx('#D9D2C2'), 3)}


def book(active_top, side, active_side):
    img = backdrop()
    red_book(img, BX, BY, BW, BH)   # draws with the BOOK ramp above
    lx, ly, lw, lh = BX + 10, BY + 6, BW - 16, BH - 12
    flat_parchment(img, lx, ly, lw, lh, seed=41 + active_top)
    for i in range(4): rect(img, lx + i, ly + 1, lx + i + 1, ly + lh - 1, shade(PA, .88 + i * .03))
    x = BX + 18
    for k, (lab, c) in enumerate(TOPS):
        w = tw(lab) + 10; L = 22 if k == active_top else 17
        ribbon_band(img, x, BY - L, BY + 6, w, c if k == active_top else shade(c, .85), 'top', lab, k == active_top)
        rect(img, x, BY + 6, x + w, BY + 7, shade(c, .45)); x += w + 4
    for k, (lab, c) in enumerate(side):
        on = k == active_side
        w = tw(lab) + 15 + (5 if on else 0); yy = BY + 16 + k * 20
        ribbon_h(img, BX - w, BX + 6, yy, 15, c if on else shade(c, .85), lab, on)
        rect(img, BX + 6, yy, BX + 7, yy + 15, shade(c, .45))
    return img, (lx, ly, lw, lh)


def view_toggle(img, x, y, active):
    """Two tiles: realm list (lines) and diplomacy board (three columns); active one lit."""
    for k in range(2):
        tx = x + k * 17
        tile16(img, tx, y, active=(k == active))
        if k == active: rect(img, tx + 2, y + 14, tx + 14, y + 15, GOLDH)
        c = INK_T if k == active else PICTO
        if k == 0:
            for r in range(3):
                rect(img, tx + 4, y + 4 + r * 3, tx + 6, y + 6 + r * 3, c); rect(img, tx + 7, y + 4 + r * 3, tx + 12, y + 5 + r * 3, c)
        else:
            for col in range(3):
                rect(img, tx + 3 + col * 4, y + 4, tx + 6 + col * 4, y + 5, c)
                rect(img, tx + 3 + col * 4, y + 6, tx + 6 + col * 4, y + 12, shade(c, 1.3) if k != active else c)


def searchbar(img, lx, ly, lw, hint, filt=True, toggle=None):
    sx, sy, sw = lx + 9, ly + 7, lw - (36 if filt else 18) - (34 if toggle is not None else 0)
    rect(img, sx, sy, sx + sw, sy + 14, BLACK); rect(img, sx + 1, sy + 1, sx + sw - 1, sy + 13, SL_DIS)
    for p in ((2, 1), (3, 1), (1, 2), (4, 2), (1, 3), (4, 3), (2, 4), (3, 4), (5, 5), (6, 6)):
        px(img, sx + 3 + p[0], sy + 3 + p[1], OCHRE)
    text(img, sx + 15, sy + 3, hint, hx('#7F7E87'))
    if filt:
        tile16(img, sx + sw + 3, sy - 1)
        for i in range(4): rect(img, sx + sw + 7 + i, sy + 5 + i, sx + sw + 15 - i, sy + 6 + i, PICTO)
    if toggle is not None:
        view_toggle(img, sx + sw + (21 if filt else 3), sy - 1, toggle)


def leaf(img, h=200):
    dx, dy, dw = BX + BW + 8, BY + 2, 186
    rect(img, dx + 3, dy + 3, dx + dw + 3, dy + h + 3, (0, 0, 0, 110)); rect(img, dx - 1, dy - 1, dx + dw + 1, dy + h + 1, PA_D)
    flat_parchment(img, dx, dy, dw, h, seed=52)
    return dx, dy, dw, h


def bold(img, x, y, s, c=INK_T):
    text(img, x, y, s, c); text(img, x + 1, y, s, c)


def line(img, x0, x1, y):
    rect(img, x0, y, x1, y + 1, PA_S)


def cross(img, x, y, c=PICTO):
    for i in range(5): px(img, x + i, y + i, c); px(img, x + 4 - i, y + i, c)


def status_chips(img, x, y, active, labels, per_row=2):
    """Relation chips in a grid with equal widths so long labels never overflow the leaf."""
    cw = max(tw(l) for l in labels) + 8
    for i, lab in enumerate(labels):
        cx, cy = x + (i % per_row) * (cw + 3), y + (i // per_row) * 13
        c = REL[lab]
        if lab == active:
            rect(img, cx, cy, cx + cw, cy + 11, shade(c, .6)); rect(img, cx + 1, cy + 1, cx + cw - 1, cy + 10, c)
            text(img, cx + cw // 2, cy + 2, lab, NAME, center=True)
        else:
            rect(img, cx, cy, cx + cw, cy + 11, PA_S); rect(img, cx + 1, cy + 1, cx + cw - 1, cy + 10, PA)
            rect(img, cx + 1, cy + 1, cx + 3, cy + 10, c)
            text(img, cx + cw // 2 + 1, cy + 2, lab, INK_M, center=True)


# ---------------- 1: Politik, faction list + detail ----------------
FACTIONS = [('Holdstewik', 'Koenigreich', 0, 'Verbuendet', 'direkt'),
            ('Geldern', 'Herzogtum', 1, 'Verbuendet', 'geerbt'),
            ('Wyndham', 'Grafschaft', 2, 'Verbuendet', 'geerbt'),
            ('Kelterfels', 'Herzogtum', 0, 'Verfeindet', 'direkt'),
            ('Suedwall', 'Baronie', 1, 'Verfeindet', 'geerbt'),
            ('Niederpoelt', 'Dorf', 0, 'Neutral', 'standard'),
            ('Schwarzhain', 'Gefolge', 0, 'Verbuendet', 'lokal')]
SIDE_POL = (('Alle', TERRA_A), ('Verbuendet', ALLY), ('Neutral', NEUT), ('Verfeindet', HOST), ('Lokal', hx('#866C36')))


def rel_tag(img, x_right, y, rel, inherited):
    c = REL[rel]
    if inherited: c = shade(c, 1.18)
    w = tag(img, x_right, y, SHORT[rel], c)
    if inherited:   # small "inherited" hook left of the tag
        hx0 = x_right - w - 7
        rect(img, hx0, y + 2, hx0 + 1, y + 7, INK_M); rect(img, hx0, y + 6, hx0 + 4, y + 7, INK_M)
    return w


def faction_rows(img, lx, ly, lw, rows, sel=1, y0=None):
    y0 = y0 or ly + 26
    for i, (name, rank, depth, rel, src) in enumerate(rows):
        yy = y0 + i * 22
        if i == sel:
            rect(img, lx + 5, yy - 2, lx + lw - 5, yy + 19, PA_L); rect(img, lx + 5, yy - 2, lx + 7, yy + 19, TERRA_A)
        rect(img, lx + 9, yy, lx + 11, yy + 17, REL[rel] if src != 'standard' else PA_S)
        ix = lx + 13 + depth * 7
        if depth:   # tree connector
            rect(img, ix - 5, yy - 3, ix - 4, yy + 8, PA_S); rect(img, ix - 5, yy + 8, ix - 1, yy + 9, PA_S)
        c1, c2, k = ARMS[name]
        shield(img, ix, yy + 1, 16, c1, c2, k)
        bold(img, ix + 20, yy, name) if depth == 0 else text(img, ix + 20, yy, name, INK_T)
        text(img, ix + 20, yy + 10, rank, INK_M)
        rel_tag(img, lx + lw - 9, yy, rel, src == 'geerbt')
        if src == 'lokal':
            tag(img, lx + lw - 9, yy + 10, 'Lokal', hx('#866C36'))
        if i < len(rows) - 1: line(img, lx + 12, lx + lw - 10, yy + 20)


def politik():
    img, (lx, ly, lw, lh) = book(0, SIDE_POL, 0)
    searchbar(img, lx, ly, lw, 'Fraktion suchen...')
    faction_rows(img, lx, ly, lw, FACTIONS)
    text(img, lx + lw // 2, ly + lh - 13, '86 Fraktionen - Stand 08:46', INK_M, center=True)
    dx, dy, dw, dh = leaf(img)
    x, y = dx + 10, dy + 10
    rect(img, x, y, x + 30, y + 30, CORD[0]); rect(img, x + 1, y + 1, x + 29, y + 29, G2); rect(img, x + 2, y + 2, x + 28, y + 28, PA_L)
    c1, c2, k = ARMS['Geldern']; shield(img, x + 4, y + 3, 22, c1, c2, k)
    bold(img, x + 36, y + 1, 'Geldern')
    text(img, x + 36, y + 11, 'Herzogtum - Rhobar', INK_M)
    text(img, x + 36, y + 21, 'in Holdstewik', INK_M)
    fy = y + 38
    text(img, x, fy, 'Status', INK_M)
    cx = x
    fy += 10
    status_chips(img, x, fy, 'Vererben', ('Vererben', 'Verbuendet', 'Neutral', 'Verfeindet'))
    fy += 28
    # effective result
    rect(img, x, fy, dx + dw - 10, fy + 22, shade(PA, .93))
    rect(img, x, fy, x + 2, fy + 22, ALLY)
    text(img, x + 6, fy + 3, 'Wirksam:', INK_M); bold(img, x + 52, fy + 3, 'Verbuendet', ALLY)
    text(img, x + 6, fy + 12, 'geerbt von Holdstewik', INK_M)
    fy += 26
    text(img, x, fy, 'Lehnskette', INK_M); fy += 11
    for d, n in enumerate(('Holdstewik', 'Geldern', 'Wyndham')):
        xx = x + 4 + d * 10
        if d: rect(img, xx - 6, fy - 4, xx - 5, fy + 4, PA_S); rect(img, xx - 6, fy + 4, xx - 2, fy + 5, PA_S)
        c1, c2, k = ARMS[n]; shield(img, xx, fy, 9, c1, c2, k)
        (bold if n == 'Geldern' else text)(img, xx + 12, fy + 1, n, INK_T)
        fy += 11
    text(img, x, fy + 1, 'Mitglieder', INK_M); text(img, x + 60, fy + 1, '14 Spieler - 1 Vasall', INK_T)
    bw2 = (dw - 26) // 2
    slate_btn(img, dx + 10, dy + dh - 25, bw2, 18, 'Neu laden')
    slate_btn(img, dx + 16 + bw2, dy + dh - 25, bw2, 18, '+ Gruppe', hover=True)
    slate_btn(img, (W - 60) // 2 + 30, H - 19, 60, 16, 'Fertig')
    return img


# ---------------- 2: Politik, local group editor ----------------
LOCAL = [('Schwarzhain', 'Gefolge', 0, 'Verbuendet', 'lokal'),
         ('Rabenfurt', 'Lehen', 0, 'Verfeindet', 'lokal')]


def gruppe():
    img, (lx, ly, lw, lh) = book(0, SIDE_POL, 4)
    searchbar(img, lx, ly, lw, 'Fraktion suchen...')
    faction_rows(img, lx, ly, lw, LOCAL, sel=0)
    yy = ly + 26 + len(LOCAL) * 22 + 4
    text(img, lx + lw // 2, yy, 'Eigene Lehen und Gefolge', INK_M, center=True)
    text(img, lx + lw // 2, yy + 10, 'gelten nur fuer dich.', INK_M, center=True)
    slate_btn(img, lx + 9, ly + lh - 26, lw - 18, 18, '+ Gruppe hinzufuegen')
    dx, dy, dw, dh = leaf(img)
    x, y = dx + 10, dy + 9
    bold(img, x, y, 'Gruppe bearbeiten')
    wx, wy0 = dx + dw - 32, dy + 6
    tile16(img, wx, wy0, 22, active=True)
    c1, c2, k = ARMS['Schwarzhain']; shield(img, wx + 3, wy0 + 3, 16, c1, c2, k)
    text(img, wx - tw('Wappen') - 3, wy0 + 13, 'Wappen', INK_M)
    fy = y + 14
    def fld(yy, lab, val, ph=False, drop=False):
        text(img, x, yy, lab, INK_M)
        line(img, x + 54, dx + dw - 10, yy + 9)
        text(img, x + 56, yy, val, hx('#A08A6A') if ph else INK_T)
        if drop:
            for i in range(3): rect(img, dx + dw - 18 + i, yy + 2 + i, dx + dw - 13 - i, yy + 3 + i, PICTO)
    fy += 6
    fld(fy, 'Name', 'Schwarzhain')
    fld(fy + 13, 'Notiz', 'Gefolge am Hafen')
    text(img, x, fy + 27, 'Typ', INK_M)
    cx = x + 54
    for lab, on in (('Lehen', False), ('Gefolge', True)):
        cx += chip(img, cx, fy + 26, lab, hx('#866C36'), on) + 3
    fld(fy + 41, 'Oberlehen', 'Holdstewik', drop=True)
    text(img, x, fy + 55, 'Status', INK_M)
    status_chips(img, x + 54, fy + 54, 'Freund', ('Freund', 'Neutral'), per_row=2)
    status_chips(img, x + 54, fy + 67, None, ('Feind',), per_row=1)
    my = fy + 84
    line(img, x, dx + dw - 10, my - 2)
    text(img, x, my + 1, 'Mitglieder (3)', INK_M)
    for i, (acc, hd) in enumerate((('Cryola_Zaragon', 6), ('Arrtakh', 3), ('Feldmaus', 7))):
        ry = my + 11 + i * 10
        head(img, x, ry, hd, 1)
        text(img, x + 12, ry + 1, acc, INK_T)
        cross(img, dx + dw - 17, ry + 2)
    ay = my + 43
    rect(img, x, ay, dx + dw - 30, ay + 12, BLACK); rect(img, x + 1, ay + 1, dx + dw - 31, ay + 11, SL_DIS)
    text(img, x + 4, ay + 2, 'Accountname...', hx('#7F7E87'))
    slate_btn(img, dx + dw - 27, ay - 1, 17, 14, '+')
    bw2 = (dw - 26) // 2
    slate_btn(img, dx + 10, dy + dh - 25, bw2, 18, 'Speichern', hover=True)
    slate_btn(img, dx + 16 + bw2, dy + dh - 25, bw2, 18, 'Loeschen')
    slate_btn(img, (W - 60) // 2 + 30, H - 19, 60, 16, 'Fertig')
    return img


# ---------------- 3: Spieler-Wache ----------------
WATCH = [('Arrtakh', 'Aerthur von Nordeck', 3, True, 'Verfeindet'),
         ('Cryola_Zaragon', 'Armon Schwarzhain', 6, True, 'Verbuendet'),
         ('Baalland', 'Wolfgang', 4, False, 'Neutral'),
         ('CLICKZY10', 'Merle', 5, False, 'Verbuendet'),
         ('Fissel5058', 'Joey R.', 1, False, 'Verfeindet')]


def wache():
    side = (('Alle', TERRA_A), ('Online', GREEN), ('Offline', NEUT))
    img, (lx, ly, lw, lh) = book(1, side, 0)
    sx, sy = lx + 9, ly + 7
    rect(img, sx, sy, lx + lw - 58, sy + 14, BLACK); rect(img, sx + 1, sy + 1, lx + lw - 59, sy + 13, SL_DIS)
    text(img, sx + 4, sy + 3, 'Minecraft-Name...', hx('#7F7E87'))
    slate_btn(img, lx + lw - 55, sy, 46, 14, 'Hinzu')
    y0 = ly + 28
    for i, (acc, rp, hd, online, rel) in enumerate(WATCH):
        yy = y0 + i * 22
        if i == 0:
            rect(img, lx + 5, yy - 2, lx + lw - 5, yy + 19, PA_L); rect(img, lx + 5, yy - 2, lx + 7, yy + 19, TERRA_A)
        rect(img, lx + 9, yy, lx + 11, yy + 17, REL[rel] if rel != 'Neutral' else PA_S)
        rect(img, lx + 13, yy, lx + 31, yy + 18, PA_D); head(img, lx + 14, yy + 1, hd, 2)
        if online:
            rect(img, lx + 28, yy + 14, lx + 32, yy + 18, hx('#2F5426')); rect(img, lx + 29, yy + 15, lx + 31, yy + 17, hx('#86C25A'))
        text(img, lx + 36, yy, rp, INK_T)
        text(img, lx + 36, yy + 10, acc, INK_M)
        tag(img, lx + lw - 9, yy + 9, 'Online' if online else 'Offline', GREEN if online else NEUT)
        cross(img, lx + lw - 15, yy + 1)
        if i < len(WATCH) - 1: line(img, lx + 12, lx + lw - 10, yy + 20)
    text(img, lx + lw // 2, ly + lh - 13, '5 beobachtet - 2 online', INK_M, center=True)
    dx, dy, dw, dh = leaf(img, 150)
    x, y = dx + 10, dy + 10
    wax_seal(img, x + 6, y + 5, 6)
    bold(img, x + 16, y + 1, 'Spieler-Wache', WAX[2])
    ty = y + 18
    for s in ('Meldet dir im Chat, sobald', 'ein eingetragener Spieler', 'den Server betritt.'):
        text(img, x, ty, s, INK_T); ty += 10
    ty += 6
    text(img, x, ty, 'Beispiel', INK_M); ty += 11
    rect(img, x, ty, dx + dw - 10, ty + 24, hx('#18181C'))
    text(img, x + 4, ty + 3, 'Spieler-Wache:', hx('#FFAA00'))
    text(img, x + 4, ty + 13, 'Arrtakh ist jetzt online.', NAME)
    ty += 32
    line(img, x, dx + dw - 10, ty); ty += 5
    for s in ('Nur lokal gespeichert,', 'wird nicht geteilt.'):
        text(img, x, ty, s, INK_M); ty += 10
    slate_btn(img, (W - 60) // 2 + 30, H - 19, 60, 16, 'Fertig')
    return img


# ---------------- 4: Kampf (settings) ----------------
def kampf():
    side = (('Anzeige', TERRA_A), ('Schutz', WAX[1]), ('Tabliste', NEUT))
    img, (lx, ly, lw, lh) = book(2, side, 0)
    x, y = lx + 10, ly + 9
    bold(img, x, y, 'Kampfmodus')
    # toggle switch for the main mode
    tx = lx + lw - 42
    rect(img, tx, y - 1, tx + 31, y + 10, BLACK); rect(img, tx + 1, y, tx + 30, y + 9, shade(GREEN, .8))
    rect(img, tx + 21, y, tx + 30, y + 9, NAME); text(img, tx + 4, y + 1, 'AN', NAME)
    text(img, x, y + 11, 'Taste K schaltet um', INK_M)
    line(img, x - 2, lx + lw - 10, y + 22)
    cy = y + 28
    for lab, on in (('Spielernamen anzeigen', True), ('Politische Farben', True), ('Angreifer mit ! markieren', True)):
        checkbox(img, x, cy, lab, on); cy += 14
    line(img, x - 2, lx + lw - 10, cy + 1); cy += 7
    text(img, x, cy, 'Farben', INK_M); cy += 11
    for lab, c in (('Verbuendete', hx('#24DAFF')), ('Gegner', hx('#FF3324'))):
        rect(img, x, cy, x + 11, cy + 11, BLACK); rect(img, x + 1, cy + 1, x + 10, cy + 10, c)
        text(img, x + 15, cy + 2, lab, INK_T)
        text(img, lx + lw - 12 - tw('#24DAFF'), cy + 2, '#24DAFF' if lab == 'Verbuendete' else '#FF3324', INK_M)
        cy += 14
    line(img, x - 2, lx + lw - 10, cy + 1); cy += 7
    text(img, x, cy, 'Vorschau', INK_M); cy += 11
    rect(img, x, cy, lx + lw - 10, cy + 33, hx('#18181C'))
    text(img, x + 4, cy + 3, 'Aerthur von Nordeck', hx('#FF3324'))
    text(img, x + 4, cy + 13, 'Armon Schwarzhain', hx('#24DAFF'))
    text(img, x + 4, cy + 23, 'Wolfgang', NAME)
    dx, dy, dw, dh = leaf(img, 112)
    x, y = dx + 10, dy + 10
    wax_seal(img, x + 6, y + 5, 6)
    bold(img, x + 16, y + 1, 'Achtung', WAX[2])
    ty = y + 17
    for s in ('Friendly-Fire-Schutz kann', 'auf dem Server zum Bann', 'fuehren.'):
        text(img, x, ty, s, INK_T); ty += 10
    ty += 5
    checkbox(img, x, ty, 'Friendly Fire verhindern', False); ty += 14
    checkbox(img, x, ty, 'Neutrale ebenfalls', False)
    rect(img, x + 1, ty + 1, x + 8, ty + 8, shade(SL_DIS, .8))
    slate_btn(img, (W - 60) // 2 + 30, H - 19, 60, 16, 'Fertig')
    return img



# ---------------- Variant B: realms (grouped by top liege) ----------------
ARMS.update({'Aldmark': (hx('#5A4A7A'), hx('#C9A04A'), 1), 'Tannfeld': (hx('#3F6E4E'), hx('#D9D2C2'), 0),
             'Moorbach': (hx('#7A5A3A'), hx('#3F6E9E'), 2), 'Eschenau': (hx('#866C36'), hx('#2E2E33'), 3)})
REALMS = [('Holdstewik', 'Koenigreich', 'Verbuendet', True,
           [('Geldern', None), ('Wyndham', None), ('Rabenfurt', 'Neutral')]),
          ('Kelterfels', 'Herzogtum', 'Verfeindet', True,
           [('Suedwall', None), ('Niederpoelt', 'Neutral')]),
          ('Aldmark', 'Herzogtum', 'Neutral', False, ['x'] * 6)]


def pennant(img, x_right, y, label, c):
    """Swallow-tailed relation pennant hanging from the realm header."""
    w = tw(label) + 12
    x0 = x_right - w
    rect(img, x0, y, x_right, y + 11, shade(c, .6)); rect(img, x0 + 1, y + 1, x_right - 1, y + 10, c)
    for r in range(5):   # notch at the left end
        rect(img, x0, y + 1 + r, x0 + 4 - r, y + 2 + r, PA_L)
        rect(img, x0, y + 9 - r, x0 + 4 - r, y + 10 - r, PA_L)
    text(img, x0 + 7, y + 2, label, NAME)


def arrow(img, x, y, open_):
    for i in range(3):
        if open_: rect(img, x + i, y + i, x + 7 - i, y + 1 + i, PICTO)
        else: rect(img, x + i, y + i, x + 1 + i, y + 7 - i, PICTO)


def politik_reiche():
    img, (lx, ly, lw, lh) = book(0, SIDE_POL, 0)
    searchbar(img, lx, ly, lw, 'Suchen...', toggle=0)
    y = ly + 26
    for name, rank, rel, open_, vassals in REALMS:
        # realm header: tinted band, big arms, relation pennant
        rect(img, lx + 6, y - 1, lx + lw - 6, y + 21, PA_L)
        rect(img, lx + 6, y + 21, lx + lw - 6, y + 22, PA_S)
        arrow(img, lx + 9, y + 7, open_)
        c1, c2, k = ARMS[name]; shield(img, lx + 19, y + 1, 18, c1, c2, k)
        bold(img, lx + 41, y + 1, name)
        text(img, lx + 41, y + 11, f'{rank} - {len(vassals)} Lehen', INK_M)
        if rel: pennant(img, lx + lw - 8, y, SHORT[rel], REL[rel])
        y += 25
        if not open_: continue
        for i, (vname, own) in enumerate(vassals):
            last = i == len(vassals) - 1
            rect(img, lx + 27, y - 3, lx + 28, y + (4 if last else 10), PA_S); rect(img, lx + 27, y + 4, lx + 31, y + 5, PA_S)
            c1, c2, k = ARMS[vname]; shield(img, lx + 33, y, 9, c1, c2, k)
            text(img, lx + 45, y + 1, vname, INK_T)
            if own:   # deviates from the realm: show it, otherwise just a faint dot
                tag(img, lx + lw - 9, y, SHORT[own], REL[own])
            else:
                rect(img, lx + lw - 14, y + 4, lx + lw - 11, y + 7, shade(REL[rel], 1.1))
            y += 11
        y += 4
    text(img, lx + lw // 2, ly + lh - 13, '14 Reiche - 86 Lehen', INK_M, center=True)
    detail_leaf(img)
    slate_btn(img, (W - 60) // 2 + 30, H - 19, 60, 16, 'Fertig')
    return img


def detail_leaf(img):
    dx, dy, dw, dh = leaf(img)
    x, y = dx + 10, dy + 10
    rect(img, x, y, x + 30, y + 30, CORD[0]); rect(img, x + 1, y + 1, x + 29, y + 29, G2); rect(img, x + 2, y + 2, x + 28, y + 28, PA_L)
    c1, c2, k = ARMS['Rabenfurt']; shield(img, x + 4, y + 3, 22, c1, c2, k)
    bold(img, x + 36, y + 1, 'Rabenfurt')
    text(img, x + 36, y + 11, 'Baronie - Edric', INK_M)
    text(img, x + 36, y + 21, 'in Holdstewik', INK_M)
    fy = y + 38
    text(img, x, fy, 'Status', INK_M)
    status_chips(img, x, fy + 10, 'Neutral', ('Vererben', 'Verbuendet', 'Neutral', 'Verfeindet'))
    fy += 38
    rect(img, x, fy, dx + dw - 10, fy + 22, shade(PA, .93)); rect(img, x, fy, x + 2, fy + 22, NEUT)
    text(img, x + 6, fy + 3, 'Abweichend vom Reich', INK_M)
    text(img, x + 6, fy + 12, 'Holdstewik ist Freund', INK_M)
    bw2 = (dw - 26) // 2
    slate_btn(img, dx + 10, dy + dh - 25, bw2, 18, 'Neu laden')
    slate_btn(img, dx + 16 + bw2, dy + dh - 25, bw2, 18, '+ Gruppe', hover=True)


# ---------------- Variant C: diplomacy board (three camps) ----------------
CAMPS = [('Verbuendet', [('Holdstewik', 4), ('Schwarzhain', 0), ('Tannfeld', 3)]),
         ('Neutral', [('Aldmark', 6), ('Niederpoelt', 0), ('Moorbach', 2), ('Eschenau', 1)]),
         ('Verfeindet', [('Kelterfels', 2), ('Rabenfurt', 0)])]


def politik_tafel():
    global BW
    old = BW
    BW = 380
    side = (('Reiche', TERRA_A), ('Alle Lehen', hx('#866C36')), ('Lokal', NEUT))
    img, (lx, ly, lw, lh) = book(0, side, 0)
    BW = old
    searchbar(img, lx, ly, lw, 'Reich oder Lehen suchen...', toggle=1)
    colw = (lw - 24) // 3
    for ci, (rel, items) in enumerate(CAMPS):
        cx = lx + 9 + ci * (colw + 3)
        top = ly + 27
        c = REL[rel]
        # column: header banner in the camp colour, light field below
        rect(img, cx, top, cx + colw, top + 14, shade(c, .6)); rect(img, cx + 1, top + 1, cx + colw - 1, top + 13, c)
        text(img, cx + colw // 2, top + 3, rel, NAME, center=True)
        rect(img, cx, top + 14, cx + colw, ly + lh - 22, shade(PA, .94))
        rect(img, cx, top + 14, cx + 1, ly + lh - 22, PA_S); rect(img, cx + colw - 1, top + 14, cx + colw, ly + lh - 22, PA_S)
        for i, (name, vassals) in enumerate(items):
            yy = top + 18 + i * 30
            sel = name == 'Holdstewik'
            rect(img, cx + 3, yy, cx + colw - 3, yy + 27, PA_D if sel else PA_S)
            rect(img, cx + 4, yy + 1, cx + colw - 4, yy + 26, PA_L if sel else PA)
            if sel: rect(img, cx + 4, yy + 1, cx + colw - 4, yy + 2, GOLDH)
            c1, c2, k = ARMS[name]; shield(img, cx + 7, yy + 4, 20, c1, c2, k)
            bold(img, cx + 31, yy + 4, name)
            sub = f'+{vassals} Lehen' if vassals else ('Lokal' if name == 'Schwarzhain' else 'eigenstaendig')
            text(img, cx + 31, yy + 15, sub, INK_M)
            # vassal shields as a small row of dots
            for v in range(min(vassals, 4)):
                rect(img, cx + colw - 10 - v * 5, yy + 17, cx + colw - 7 - v * 5, yy + 21, shade(c, .9))
    text(img, lx + lw // 2, ly + lh - 15, 'Karte ziehen oder anklicken, um den Status zu aendern', INK_M, center=True)
    slate_btn(img, (W - 60) // 2 + 30, H - 19, 60, 16, 'Fertig')
    return img


# ---------------- Trupp: own combat squad ----------------
SQUAD_C = hx('#55C46A')   # own squad colour (configurable)
TRUPP = [('Cryola_Zaragon', 'Armon Schwarzhain', 6, 'online', 'Anfuehrer'),
         ('CLICKZY10', 'Merle', 5, 'online', None),
         ('Baalland', 'Wolfgang', 4, 'online', None),
         ('Arrtakh', 'Aerthur von Nordeck', 3, 'online', None),
         ('Fissel5058', 'Joey R.', 1, 'offline', None)]


def trupp():
    side = (('Trupp', TERRA_A), ('Online', GREEN), ('Offline', NEUT))
    img, (lx, ly, lw, lh) = book(1, side, 0)
    sx, sy = lx + 9, ly + 7
    rect(img, sx, sy, lx + lw - 58, sy + 14, BLACK); rect(img, sx + 1, sy + 1, lx + lw - 59, sy + 13, SL_DIS)
    text(img, sx + 4, sy + 3, 'Minecraft-Name...', hx('#7F7E87'))
    slate_btn(img, lx + lw - 55, sy, 46, 14, 'Hinzu')
    y0 = ly + 28
    for i, (acc, rp, hd, state, role) in enumerate(TRUPP):
        yy = y0 + i * 22
        if i == 0:
            rect(img, lx + 5, yy - 2, lx + lw - 5, yy + 19, PA_L); rect(img, lx + 5, yy - 2, lx + 7, yy + 19, TERRA_A)
        rect(img, lx + 9, yy, lx + 11, yy + 17, SQUAD_C if state != 'offline' else PA_S)
        rect(img, lx + 13, yy, lx + 31, yy + 18, PA_D); head(img, lx + 14, yy + 1, hd, 2)
        text(img, lx + 36, yy, rp, INK_M if state == 'offline' else INK_T)
        text(img, lx + 36, yy + 10, acc, INK_M)
        if role:   # small gold crown on the head marks the squad leader
            for cx_, h_ in ((0, 3), (2, 2), (4, 3)):
                rect(img, lx + 13 + cx_, yy - 2 - h_ + 3, lx + 14 + cx_, yy + 2, G3)
            rect(img, lx + 13, yy + 1, lx + 18, yy + 3, G2)
        tag(img, lx + lw - 9, yy + 10, state.capitalize(), GREEN if state == 'online' else NEUT)
        cross(img, lx + lw - 15, yy + 1)
        if i < len(TRUPP) - 1: line(img, lx + 12, lx + lw - 10, yy + 20)
    text(img, lx + lw // 2, ly + lh - 13, '5 im Trupp - 4 online', INK_M, center=True)
    # leaf: what the squad does in combat
    dx, dy, dw, dh = leaf(img)
    x, y = dx + 10, dy + 10
    bold(img, x, y, 'Kampftrupp')
    text(img, x, y + 11, 'Deine Leute fuer den Kampf.', INK_M)
    cy = y + 26
    for lab, on in (('Immer als Verbuendet', True), ('In der Tabliste oben', True),
                    ('Vor Friendly Fire schuetzen', True)):
        checkbox(img, x, cy, lab, on); cy += 13
    text(img, x, cy + 2, 'Truppfarbe', INK_M)
    rect(img, x + 60, cy + 1, x + 71, cy + 12, BLACK); rect(img, x + 61, cy + 2, x + 70, cy + 11, SQUAD_C)
    text(img, x + 76, cy + 3, '#55C46A', INK_M)
    cy += 18
    line(img, x, dx + dw - 10, cy); cy += 5
    text(img, x, cy, 'Vorschau Namen', INK_M); cy += 11
    rect(img, x, cy, dx + dw - 10, cy + 33, hx('#18181C'))
    text(img, x + 4, cy + 3, 'Armon Schwarzhain', SQUAD_C)
    text(img, x + 4, cy + 13, 'Gauthier', hx('#24DAFF'))
    text(img, x + 4, cy + 23, 'Wolfgang', SQUAD_C)
    text(img, dx + dw - 14 - tw('Trupp'), cy + 3, 'Trupp', SUB)
    text(img, dx + dw - 14 - tw('Verb.'), cy + 13, 'Verb.', SUB)
    text(img, dx + dw - 14 - tw('Trupp'), cy + 23, 'Trupp', SUB)
    bw2 = (dw - 26) // 2
    slate_btn(img, dx + 10, dy + dh - 25, bw2, 18, 'Leeren')
    slate_btn(img, dx + 16 + bw2, dy + dh - 25, bw2, 18, 'Aus Gruppe...', hover=True)
    slate_btn(img, (W - 60) // 2 + 30, H - 19, 60, 16, 'Fertig')
    return img


# ---------------- Trupps: several named squads ----------------
SQUADS = [('Vorhut', hx('#55C46A'), [(6, True), (5, True), (4, True), (3, False)]),
          ('Hafenwache', hx('#4FA3C7'), [(1, True), (7, False), (2, False)]),
          ('Spaeher', hx('#D9B44A'), [(0, True), (4, True)]),
          ('Reserve', hx('#B07AC9'), [(2, False), (5, False), (6, False)])]
MEMBERS = [('Armon Schwarzhain', 'Cryola_Zaragon', 6, True, True), ('Merle', 'CLICKZY10', 5, True, False),
           ('Wolfgang', 'Baalland', 4, True, False), ('Aerthur von Nordeck', 'Arrtakh', 3, False, False)]


def trupps():
    img, (lx, ly, lw, lh) = book(1, (), 0)
    bold(img, lx + 10, ly + 8, 'Deine Trupps')
    y = ly + 22
    for i, (name, col, mem) in enumerate(SQUADS):
        on = sum(1 for _, o in mem if o)
        sel = i == 0
        rect(img, lx + 6, y, lx + lw - 6, y + 32, PA_D if sel else PA_S)
        rect(img, lx + 7, y + 1, lx + lw - 7, y + 31, PA_L if sel else PA)
        rect(img, lx + 7, y + 1, lx + 10, y + 31, col)
        if sel: rect(img, lx + 10, y + 1, lx + lw - 7, y + 2, GOLDH)
        bold(img, lx + 15, y + 4, name)
        # x/y online counter, green when anyone is online
        lab = f'{on}/{len(mem)} online'
        tag(img, lx + lw - 10, y + 4, lab, GREEN if on else NEUT)
        for k, (hd, o) in enumerate(mem):
            hx0 = lx + 15 + k * 12
            rect(img, hx0 - 1, y + 16, hx0 + 10, y + 27, PA_D); head(img, hx0, y + 17, hd, 1)
            if not o: rect(img, hx0, y + 17, hx0 + 9, y + 26, (201, 170, 129, 140))
            else: rect(img, hx0 + 7, y + 24, hx0 + 10, y + 27, hx('#86C25A'))
        y += 36
    slate_btn(img, lx + 9, ly + lh - 26, lw - 18, 18, '+ Neuer Trupp')
    # leaf: selected squad
    dx, dy, dw, dh = leaf(img)
    x, y = dx + 10, dy + 9
    name, col, mem = SQUADS[0]
    text(img, x, y + 1, 'Name', INK_M); line(img, x + 40, dx + dw - 30, y + 10); bold(img, x + 42, y + 1, name)
    rect(img, dx + dw - 24, y - 1, dx + dw - 11, y + 12, BLACK); rect(img, dx + dw - 23, y, dx + dw - 12, y + 11, col)
    cy = y + 17
    text(img, x, cy, 'Mitglieder', INK_M)
    text(img, dx + dw - 10 - tw('3/4 online'), cy, '3/4 online', INK_M)
    cy += 11
    for rp, acc, hd, o, lead in MEMBERS:
        head(img, x, cy, hd, 1)
        if lead:
            for cx_, h_ in ((0, 3), (2, 2), (4, 3)):
                rect(img, x + cx_, cy - h_, x + 1 + cx_, cy, G3)
        rect(img, x + 11, cy + 2, x + 15, cy + 6, hx('#2F5426') if o else PA_S)
        if o: rect(img, x + 12, cy + 3, x + 14, cy + 5, hx('#86C25A'))
        text(img, x + 18, cy + 1, rp, INK_T if o else INK_M)
        cross(img, dx + dw - 17, cy + 2)
        cy += 11
    rect(img, x, cy + 1, dx + dw - 30, cy + 13, BLACK); rect(img, x + 1, cy + 2, dx + dw - 31, cy + 12, SL_DIS)
    text(img, x + 4, cy + 3, 'Minecraft-Name...', hx('#7F7E87'))
    slate_btn(img, dx + dw - 27, cy, 17, 14, '+')
    cy += 20
    line(img, x, dx + dw - 10, cy); cy += 5
    checkbox(img, x, cy, 'Immer als Verbuendet', True); cy += 13
    checkbox(img, x, cy, 'In der Tabliste oben', True); cy += 13
    checkbox(img, x, cy, 'Vor Friendly Fire schuetzen', False)
    bw2 = (dw - 26) // 2
    slate_btn(img, dx + 10, dy + dh - 25, bw2, 18, 'Speichern', hover=True)
    slate_btn(img, dx + 16 + bw2, dy + dh - 25, bw2, 18, 'Aufloesen')
    slate_btn(img, (W - 60) // 2 + 30, H - 19, 60, 16, 'Fertig')
    return img

for name, fn in (('tf_politik', politik), ('tf_politik_b_reiche', politik_reiche), ('tf_politik_c_tafel', politik_tafel), ('tf_gruppe', gruppe), ('tf_trupps', trupps), ('tf_kampf', kampf)):
    fn().resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUT, name + '.png'))
print('ok')
