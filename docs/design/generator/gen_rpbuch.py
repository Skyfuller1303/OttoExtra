"""RP people book: Personen (with relation), Titel and Import screens. Usage: python -I gen_rpbuch.py <out_dir>"""
import os, sys, math
OUTD = sys.argv[1]; os.makedirs(OUTD, exist_ok=True)
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_personen_band.py'), encoding='utf-8').read()
exec(_b[:_b.index("people_band().resize")])
from PIL import Image

FRIEND = hx('#3F6E9E'); FOE = hx('#B24147'); NEUTRAL = hx('#8A7A66'); GREEN = hx('#5E9A3E')
TOP = (('Personen', TERRA_A), ('Titel', hx('#866C36')), ('Import', PENNANT))
BW, BH, BX, BY = 176, 200, 58, 34


def frame(active_top, side, active_side=0):
    img = backdrop()
    red_book(img, BX, BY, BW, BH)
    lx, ly, lw, lh = BX + 10, BY + 6, BW - 16, BH - 12
    flat_parchment(img, lx, ly, lw, lh, seed=31 + active_top)
    for i in range(4): rect(img, lx + i, ly + 1, lx + i + 1, ly + lh - 1, shade(PA, .88 + i * .03))
    x = BX + 18
    for k, (lab, c) in enumerate(TOP):
        w = tw(lab) + 10; L = 22 if k == active_top else 17
        ribbon_band(img, x, BY - L, BY + 6, w, c if k == active_top else shade(c, .85), 'top', lab, k == active_top)
        rect(img, x, BY + 6, x + w, BY + 7, shade(c, .45))
        x += w + 4
    for k, (lab, c) in enumerate(side):
        w = tw(lab) + 15 + (5 if k == active_side else 0)
        yy = BY + 16 + k * 20
        ribbon_h(img, BX - w, BX + 6, yy, 15, c if k == active_side else shade(c, .85), lab, k == active_side)
        rect(img, BX + 6, yy, BX + 7, yy + 15, shade(c, .45))
    return img, (lx, ly, lw, lh)


def search(img, lx, ly, lw, count):
    sx, sy = lx + 10, ly + 7
    rect(img, sx, sy, sx + 104, sy + 13, BLACK); rect(img, sx + 1, sy + 1, sx + 103, sy + 12, SL_DIS)
    for p in ((2, 1), (3, 1), (1, 2), (4, 2), (1, 3), (4, 3), (2, 4), (3, 4), (5, 5), (6, 6)):
        px(img, sx + 3 + p[0], sy + 2 + p[1], OCHRE)
    text(img, sx + 13, sy + 3, 'Suche...', hx('#7F7E87'), shadow=False)
    text(img, lx + lw - 10 - tw(count), ly + 9, count, INK_M, shadow=False)


def leaf(img, h=190, seed=32, seal=True):
    dx, dy, dw = BX + BW + 8, BY + 4, 150
    rect(img, dx + 3, dy + 3, dx + dw + 3, dy + h + 3, (0, 0, 0, 110))
    rect(img, dx - 1, dy - 1, dx + dw + 1, dy + h + 1, PA_D)
    flat_parchment(img, dx, dy, dw, h, seed=seed)
    if seal: wax_seal(img, dx + dw - 12, dy + 10, 7)
    return dx, dy, dw, h


def chip(img, x, y, label, c, on):
    w = tw(label) + 8
    if on:
        rect(img, x, y, x + w, y + 11, shade(c, .6)); rect(img, x + 1, y + 1, x + w - 1, y + 10, c)
        text(img, x + w // 2, y + 2, label, NAME, center=True)
    else:
        rect(img, x, y, x + w, y + 11, PA_S); rect(img, x + 1, y + 1, x + w - 1, y + 10, PA)
        text(img, x + w // 2, y + 2, label, INK_M, center=True, shadow=False)
    return w


def checkbox(img, x, y, label, on):
    rect(img, x, y, x + 9, y + 9, BLACK); rect(img, x + 1, y + 1, x + 8, y + 8, SL_DIS)
    if on:
        for i, (a, b) in enumerate(((2, 4), (3, 5), (4, 6), (5, 5), (6, 4), (7, 3))): px(img, x + a, y + b, NAME)
    text(img, x + 12, y + 1, label, INK_T, shadow=False)


def field(img, x, y, w, lab, val, labw=46):
    text(img, x, y, lab, INK_M, shadow=False)
    rect(img, x + labw, y + 9, x + w, y + 10, PA_S)
    text(img, x + labw + 2, y, val, INK_T, shadow=False)


# ---------------- Personen ----------------
REL = {0: FRIEND, 3: FOE, 4: FRIEND}


def personen():
    side = (('Alle', TERRA_A), ('Online', GREEN), ('Unbekannt', NEUTRAL), ('Freund', FRIEND), ('Feind', FOE))
    img, (lx, ly, lw, lh) = frame(0, side, 0)
    search(img, lx, ly, lw, '124')
    y0 = ly + 26
    for i, (name, acc, state, online, conflict, _) in enumerate(PEOPLE):
        yy = y0 + i * 20
        if i == SEL:
            rect(img, lx + 6, yy - 1, lx + lw - 6, yy + 18, PA_L); rect(img, lx + 6, yy - 1, lx + 8, yy + 18, TERRA_A)
        if i in REL: rect(img, lx + 9, yy + 1, lx + 10, yy + 14, REL[i])          # relation stripe
        rect(img, lx + 11, yy + 2, lx + 21, yy + 12, PA_D); head(img, lx + 12, yy + 3, i)
        text(img, lx + 25, yy, name, INK_M if name == 'Unbekannt' else INK_T, shadow=False)
        text(img, lx + 25, yy + 9, acc + ' - ' + state, INK_M, shadow=False)
        if online: disc(img, lx + lw - 13, yy + 4, 2, (hx('#2F5426'), GREEN, hx('#86C25A')))
        if conflict:   # API deviation: small ochre warning triangle
            for r in range(5): rect(img, lx + lw - 15 - r // 2, yy + 10 + r, lx + lw - 14 + r // 2, yy + 11 + r, hx('#C9A04A'))
        if i < len(PEOPLE) - 1: rect(img, lx + 10, yy + 18, lx + lw - 10, yy + 19, PA_S)
    dx, dy, dw, dh = leaf(img)
    x, y = dx + 9, dy + 10
    rect(img, x, y, x + 28, y + 28, CORD[0]); rect(img, x + 1, y + 1, x + 27, y + 27, G2); rect(img, x + 2, y + 2, x + 26, y + 26, CORD[1])
    head(img, x + 2, y + 2, SEL, 3)
    name, acc, state = PEOPLE[SEL][:3]
    text(img, x + 34, y + 1, name, INK_T, shadow=False); text(img, x + 35, y + 1, name, INK_T, shadow=False)
    text(img, x + 34, y + 11, acc + ' - ' + state, INK_M, shadow=False)
    # relation chips
    cx = x + 34
    for lab, c, on in (('Freund', FRIEND, True), ('Feind', FOE, False)):
        cx += chip(img, cx, y + 20, lab, c, on) + 3
    fy = y + 36
    field(img, x, fy, dw - 18, 'RP-Name', 'Ansgar')
    field(img, x, fy + 14, dw - 18, 'Titel', 'Ritter')
    lx2 = x + dw - 26; rect(img, lx2 + 1, fy + 14, lx2 + 5, fy + 16, PICTO); rect(img, lx2, fy + 16, lx2 + 6, fy + 21, PICTO)
    field(img, x, fy + 28, dw - 18, 'Notiz', 'Schmied, Hafen')
    checkbox(img, x, fy + 43, 'Gesperrt', False)
    # preview strip: how the name shows up in chat / tab / over the head
    py = fy + 56
    text(img, x, py, 'Vorschau', INK_M, shadow=False)
    rect(img, x, py + 10, x + 96, py + 44, hx('#18181C'))
    text(img, x + 3, py + 12, '<Ritter Ansgar> Moin', hx('#FFAA00'))
    text(img, x + 3, py + 22, 'Ritter Ansgar', hx('#55FF55'))
    text(img, x + 3, py + 32, 'Ritter Ansgar', hx('#FFFFFF'))
    figure(img, dx + dw - 24, py + 8, SEL)
    slate_btn(img, dx + 8, dy + dh - 24, 64, 18, 'Speichern', hover=True)
    slate_btn(img, dx + 74, dy + dh - 24, 64, 18, 'Vergessen')
    return img


# ---------------- Titel ----------------
TITLES = [('Ritter', 'Ritterin', 'Adel', hx('#FFAA00'), True), ('Graf', 'Grafin', 'Adel', hx('#FFAA00'), True),
          ('Bruder', 'Schwester', 'Klerus', hx('#AAAAAA'), True), ('Meister', 'Meisterin', 'Fertigkeit', hx('#55FFFF'), True),
          ('Vorkoster', 'Vorkosterin', 'Vorkoster', hx('#FF55FF'), False), ('Hauptmann', 'Hauptfrau', 'Allgemein', hx('#FFFFFF'), True),
          ('Magd', 'Knecht', 'Allgemein', hx('#FFFFFF'), True), ('Abt', 'Abtissin', 'Klerus', hx('#AAAAAA'), False)]


def titel():
    side = (('Alle', TERRA_A), ('Aktiv', GREEN), ('Inaktiv', NEUTRAL), ('Manuell', hx('#866C36')), ('Wiki', PENNANT))
    img, (lx, ly, lw, lh) = frame(1, side, 0)
    search(img, lx, ly, lw, '42')
    y0 = ly + 26
    for i, (m, f, cat, col, act) in enumerate(TITLES[:7]):
        yy = y0 + i * 18
        if i == 0:
            rect(img, lx + 6, yy - 1, lx + lw - 6, yy + 16, PA_L); rect(img, lx + 6, yy - 1, lx + 8, yy + 16, TERRA_A)
        rect(img, lx + 12, yy + 2, lx + 19, yy + 9, PA_D); rect(img, lx + 13, yy + 3, lx + 18, yy + 8, col)
        text(img, lx + 23, yy, m + ' / ' + f, INK_T if act else INK_M, shadow=False)
        text(img, lx + 23, yy + 8, cat + ('' if act else ' - inaktiv'), INK_M, shadow=False)
        rect(img, lx + 10, yy + 16, lx + lw - 10, yy + 17, PA_S)
    slate_btn(img, lx + 10, ly + lh - 22, lw - 20, 16, '+ Neuer Titel')
    dx, dy, dw, dh = leaf(img, seed=41)
    x, y = dx + 9, dy + 10
    text(img, x, y, 'Ritter', INK_T, shadow=False); text(img, x + 1, y, 'Ritter', INK_T, shadow=False)
    fy = y + 14
    field(img, x, fy, dw - 18, 'Name', 'Ritter', 50)
    field(img, x, fy + 14, dw - 18, 'Variante', 'Ritterin', 50)
    field(img, x, fy + 28, dw - 18, 'Variante', '-', 50)
    text(img, x, fy + 44, 'Kategorie', INK_M, shadow=False)
    cx = x
    for lab, on in (('Adel', True), ('Klerus', False), ('Fertigk.', False), ('+', False)):
        cx += chip(img, cx, fy + 54, lab, hx('#866C36'), on) + 2
    checkbox(img, x, fy + 69, 'Aktiv', True)
    text(img, x, fy + 83, 'Farben', INK_M, shadow=False)
    cx = x
    for lab, c in (('Titel', hx('#FFAA00')), ('Name', hx('#FFFFFF'))):
        tile16(img, cx, fy + 93, 11); rect(img, cx + 3, fy + 96, cx + 8, fy + 101, c)
        text(img, cx + 13, fy + 95, lab, INK_T, shadow=False)
        cx += tw(lab) + 20
    checkbox(img, x, fy + 108, 'uberschreibt Farbe', True)
    rect(img, x, fy + 121, x + dw - 18, fy + 133, hx('#18181C'))
    text(img, x + 3, fy + 123, 'Ritter', hx('#FFAA00')); text(img, x + 3 + tw('Ritter '), fy + 123, 'Ansgar', hx('#FFFFFF'))
    slate_btn(img, dx + 8, dy + dh - 24, 64, 18, 'Speichern', hover=True)
    slate_btn(img, dx + 74, dy + dh - 24, 64, 18, 'Loschen')
    return img


# ---------------- Import ----------------
def section(img, x, y, w, title, hint, buttons, icon):
    tile16(img, x, y); icon(img, x + 4, y + 4)
    text(img, x + 20, y + 1, title, INK_T, shadow=False); text(img, x + 21, y + 1, title, INK_T, shadow=False)
    text(img, x + 20, y + 10, hint, INK_M, shadow=False)
    bx = x
    for lab, hov in buttons:
        bw = tw(lab) + 14
        slate_btn(img, bx, y + 22, bw, 16, lab, hover=hov)
        bx += bw + 4
    rect(img, x, y + 44, x + w, y + 45, PA_S)


def ic_cloud(img, x, y):
    rect(img, x + 2, y + 3, x + 7, y + 7, PICTO); rect(img, x + 1, y + 4, x + 8, y + 7, PICTO); rect(img, x + 3, y + 2, x + 6, y + 3, PICTO)


def ic_file(img, x, y):
    rect(img, x + 1, y, x + 6, y + 8, PICTO); rect(img, x + 2, y + 1, x + 5, y + 7, PA); rect(img, x + 2, y + 3, x + 5, y + 4, PICTO); rect(img, x + 2, y + 5, x + 5, y + 6, PICTO)


def ic_chest(img, x, y):
    rect(img, x, y + 2, x + 8, y + 8, PICTO); rect(img, x + 1, y + 3, x + 7, y + 4, PA); rect(img, x + 3, y + 4, x + 5, y + 6, PA)


def importscreen():
    img, (lx, ly, lw, lh) = frame(2, ())
    x, w = lx + 10, lw - 20
    text(img, x, ly + 7, 'Personen ubernehmen', INK_T, shadow=False); text(img, x + 1, ly + 7, 'Personen ubernehmen', INK_T, shadow=False)
    rect(img, x, ly + 17, x + w, ly + 18, PA_S)
    section(img, x, ly + 24, w, 'Server-API', 'Namen und Titel', (('Nur Bekannte', True), ('Alle', False)), ic_cloud)
    section(img, x, ly + 72, w, 'OttoPlus-Datei', 'altes Mod-Archiv', (('Nur Bekannte', False), ('Alle', False)), ic_file)
    section(img, x, ly + 120, w, 'Sicherung', 'letzte: heute 20:14', (('Backup anlegen', False),), ic_chest)
    text(img, x, ly + lh - 14, 'Import: 12 erganzt, 3 neu', INK_M, shadow=False)
    # leaf: warning + danger zone
    dx, dy, dw, dh = leaf(img, h=152, seed=51, seal=False)
    x2 = dx + 9
    wax_seal(img, x2 + 6, dy + 14, 6)
    text(img, x2 + 16, dy + 10, 'Achtung', WAX[2], shadow=False); text(img, x2 + 17, dy + 10, 'Achtung', WAX[2], shadow=False)
    lines = ['Der Import kann RP-Wissen', 'verraten, das deine Figur', 'noch nicht hat.', '', 'Nur Bekannte erganzt', 'Personen, die du schon', 'kennst. Alle legt jeden', 'API-Spieler an.']
    for i, l in enumerate(lines): text(img, x2, dy + 26 + i * 10, l, INK_T if i < 3 else INK_M, shadow=False)
    rect(img, x2, dy + 112, dx + dw - 9, dy + 113, PA_S)
    text(img, x2, dy + 117, 'Vor jedem Import wird', INK_M, shadow=False)
    text(img, x2, dy + 126, 'automatisch gesichert.', INK_M, shadow=False)
    # danger: forget all, red slate
    fy = dy + dh + 8
    rect(img, dx, fy, dx + dw, fy + 20, BLACK); rect(img, dx + 1, fy + 1, dx + dw - 1, fy + 19, WAX[1])
    rect(img, dx + 1, fy + 1, dx + dw - 1, fy + 2, WAX[2])
    text(img, dx + dw // 2, fy + 6, 'Alle vergessen...', NAME, center=True)
    return img


for name, fn in (('rpbuch_personen', personen), ('rpbuch_titel', titel), ('rpbuch_import', importscreen)):
    fn().resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUTD, name + '.png'))
print('ok')
