"""People book re-plan: no text shadows, wider leaf, larger heads, state tags. Usage: python -I gen_rpbuch2.py <out_dir>"""
import os, sys
OUTD = sys.argv[1]; os.makedirs(OUTD, exist_ok=True)
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_rpbuch.py'), encoding='utf-8').read()
exec(_b[:_b.index("for name, fn in (('rpbuch_personen'")])
from PIL import Image
W, H = 456, 256
_text = text
def text(img, x, y, s, col, k=1, center=False, shadow=False):
    return _text(img, x, y, s, col, k, center, False)

PEOPLE2 = [('Lorenz', 'D3monicBread', 'Manuell', True, False, 'Rustmann', FRIEND),
           ('Joey R.', 'Fissel5058', 'Gesperrt', True, False, '', None),
           ('Gauthier', 'schwaechlich', 'Manuell', True, False, '', None),
           ('Aerthur von Nordeck', 'Arrtakh', 'API', True, True, 'Adlige', FOE),
           ('Wolfgang', 'Baalland', 'API', True, False, 'Gemeine', None),
           ('Merle', 'CLICKZY10', 'API', False, False, 'Gemeine', FRIEND),
           ('Armon Schwarzhain', 'Cryola_Zaragon', 'API', False, False, '', None),
           ('Unbekannt', 'Feldmaus', 'Gehort', False, False, '', None)]
TAGC = {'Manuell': hx('#866C36'), 'API': PENNANT, 'Gesperrt': WAX[1], 'Gehort': NEUTRAL, 'Bekannt': GREEN}
BX, BY, BW, BH = 68, 30, 182, 204


def tag(img, x_right, y, label, c):
    w = tw(label) + 6
    rect(img, x_right - w, y, x_right, y + 9, c)
    text(img, x_right - w + 3, y + 1, label, NAME)
    return w


def personen2():
    side = (('Alle', TERRA_A), ('Online', GREEN), ('Unbekannt', NEUTRAL), ('Freund', FRIEND), ('Feind', FOE))
    img = backdrop()
    red_book(img, BX, BY, BW, BH)
    lx, ly, lw, lh = BX + 10, BY + 6, BW - 16, BH - 12
    flat_parchment(img, lx, ly, lw, lh, seed=31)
    for i in range(4): rect(img, lx + i, ly + 1, lx + i + 1, ly + lh - 1, shade(PA, .88 + i * .03))
    x = BX + 18
    for k, (lab, c) in enumerate(TOP):
        w = tw(lab) + 10; L = 22 if k == 0 else 17
        ribbon_band(img, x, BY - L, BY + 6, w, c if k == 0 else shade(c, .85), 'top', lab, k == 0)
        rect(img, x, BY + 6, x + w, BY + 7, shade(c, .45)); x += w + 4
    for k, (lab, c) in enumerate(side):
        w = tw(lab) + 15 + (5 if k == 0 else 0); yy = BY + 16 + k * 20
        ribbon_h(img, BX - w, BX + 6, yy, 15, c if k == 0 else shade(c, .85), lab, k == 0)
        rect(img, BX + 6, yy, BX + 7, yy + 15, shade(c, .45))
    # search: magnifier with its own padding, filter tile at the end, no count here
    sx, sy, sw = lx + 9, ly + 7, lw - 36
    rect(img, sx, sy, sx + sw, sy + 14, BLACK); rect(img, sx + 1, sy + 1, sx + sw - 1, sy + 13, SL_DIS)
    for p in ((2, 1), (3, 1), (1, 2), (4, 2), (1, 3), (4, 3), (2, 4), (3, 4), (5, 5), (6, 6)):
        px(img, sx + 3 + p[0], sy + 3 + p[1], OCHRE)
    text(img, sx + 15, sy + 3, 'Suche...', hx('#7F7E87'))
    tile16(img, sx + sw + 3, sy - 1)
    for i in range(4): rect(img, sx + sw + 7 + i, sy + 5 + i, sx + sw + 15 - i, sy + 6 + i, PICTO)
    # rows: 16 px head, name + title line, account + state tag line
    y0 = ly + 26
    for i, (name, acc, state, online, conflict, title, rel) in enumerate(PEOPLE2[:6]):
        yy = y0 + i * 22
        if i == 0:
            rect(img, lx + 5, yy - 2, lx + lw - 5, yy + 19, PA_L); rect(img, lx + 5, yy - 2, lx + 7, yy + 19, TERRA_A)
        if rel: rect(img, lx + 9, yy, lx + 11, yy + 17, rel)
        rect(img, lx + 13, yy, lx + 31, yy + 18, PA_D); head(img, lx + 14, yy + 1, i, 2)
        if online: rect(img, lx + 28, yy + 14, lx + 32, yy + 18, hx('#2F5426')); rect(img, lx + 29, yy + 15, lx + 31, yy + 17, hx('#86C25A'))
        text(img, lx + 36, yy, name, INK_M if name == 'Unbekannt' else INK_T)
        text(img, lx + 36, yy + 10, acc, INK_M)
        tw_ = tag(img, lx + lw - 9, yy + 9, state, TAGC.get(state, NEUTRAL))
        if conflict:
            for r in range(5): rect(img, lx + lw - 14 - tw_ - r // 2, yy + 10 + r, lx + lw - 13 - tw_ + r // 2, yy + 11 + r, hx('#C9A04A'))
        if i < 5: rect(img, lx + 12, yy + 20, lx + lw - 10, yy + 21, PA_S)
    text(img, lx + lw // 2, ly + lh - 13, '239 Personen', INK_M, center=True)
    # leaf
    dx, dy, dw, dh = BX + BW + 8, BY + 2, 186, 200
    rect(img, dx + 3, dy + 3, dx + dw + 3, dy + dh + 3, (0, 0, 0, 110)); rect(img, dx - 1, dy - 1, dx + dw + 1, dy + dh + 1, PA_D)
    flat_parchment(img, dx, dy, dw, dh, seed=32); wax_seal(img, dx + dw - 12, dy + 11, 7)
    x, y = dx + 10, dy + 10
    rect(img, x, y, x + 28, y + 28, CORD[0]); rect(img, x + 1, y + 1, x + 27, y + 27, G2); rect(img, x + 2, y + 2, x + 26, y + 26, CORD[1])
    head(img, x + 2, y + 2, 0, 3)
    text(img, x + 34, y + 1, 'Lorenz', INK_T); text(img, x + 35, y + 1, 'Lorenz', INK_T)
    text(img, x + 34, y + 11, 'D3monicBread', INK_M)
    w = tw('Manuell') + 6; rect(img, x + 34, y + 20, x + 34 + w, y + 29, TAGC['Manuell']); text(img, x + 37, y + 21, 'Manuell', NAME)
    fy = y + 36
    text(img, x, fy + 1, 'Beziehung', INK_M)
    cx = x + 52
    for lab, c, on in (('Freund', FRIEND, True), ('Feind', FOE, False)):
        cx += chip(img, cx, fy, lab, c, on) + 3
    rect(img, x, fy + 15, dx + dw - 10, fy + 16, PA_S)
    fy += 21
    def fld(yy, lab, val, reset=True):
        text(img, x, yy, lab, INK_M)
        rect(img, x + 52, yy + 9, dx + dw - 10, yy + 10, PA_S)
        text(img, x + 54, yy, val, INK_T)
        if reset:
            rx = dx + dw - 18
            for p in ((1, 0), (2, 0), (3, 0), (0, 1), (4, 1), (0, 2), (0, 3), (4, 3), (1, 4), (2, 4), (3, 4), (4, 0), (5, 0)):
                px(img, rx + p[0], yy + 1 + p[1], PICTO)
    fld(fy, 'RP-Name', 'Lorenz'); fld(fy + 14, 'Titel', 'Rustmann'); fld(fy + 28, 'Notizen', '', False)
    text(img, x + 54, fy + 28, 'Notizen...', hx('#A08A6A'))
    checkbox(img, x, fy + 44, 'Gesperrt', False); checkbox(img, x + 80, fy + 44, 'Titel fest', False)
    py = fy + 60
    text(img, x, py, 'Vorschau', INK_M)
    fw = tw('Farben >') + 8; chip(img, dx + dw - 10 - fw, py - 1, 'Farben >', hx('#866C36'), False)
    rect(img, x, py + 11, x + 132, py + 46, hx('#18181C'))
    text(img, x + 4, py + 14, '<Rustmann Lorenz> Moin', hx('#FFFFFF'))
    text(img, x + 4, py + 24, 'Rustmann Lorenz', hx('#FFAA00'))
    text(img, x + 4, py + 34, 'Rustmann Lorenz', hx('#FFAA00'))
    figure(img, dx + dw - 28, py + 14, 0)
    bw2 = (dw - 26) // 2
    slate_btn(img, dx + 10, dy + dh - 25, bw2, 18, 'Speichern', hover=True)
    slate_btn(img, dx + 16 + bw2, dy + dh - 25, bw2, 18, 'Vergessen')
    slate_btn(img, (W - 60) // 2 + 30, H - 19, 60, 16, 'Fertig')
    return img


def slate_btn(img, x, y, w, h, label, hover=False, icon=None):
    o = GOLDH if hover else BLACK
    rect(img, x + 1, y, x + w - 1, y + h, o); rect(img, x, y + 1, x + w, y + h - 1, o)
    rect(img, x + 1, y + 1, x + w - 1, y + h - 1, SL_HOV if hover else SL)
    rect(img, x + 1, y + 1, x + w - 1, y + 2, SL_E); rect(img, x + 1, y + h - 2, x + w - 1, y + h - 1, SL_S)
    text(img, x + w // 2, y + (h - 8) // 2, label, GOLDH if hover else NAME, center=True)


personen2().resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUTD, 'rpbuch2_personen.png'))
print('ok')
