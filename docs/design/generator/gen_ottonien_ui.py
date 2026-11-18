"""Refined letter editor (V2b) and people book (P3) in the Ottonien Reformed palette.
Usage: python -I gen_ottonien_ui.py <out_dir>"""
import os, sys, math, random
OUTD = sys.argv[1]
os.makedirs(OUTD, exist_ok=True)
_p = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_personenui.py'), encoding='utf-8').read()
sys.argv = [sys.argv[0], OUTD]
exec(_p[:_p.index("for name, fn in (('personen_p1_register'")])
from PIL import Image

# --- Ottonien Reformed palette (references/gui.md) ---
PA, PA_L, PA_S, PA_D = hx('#C9AA81'), hx('#D4B78C'), hx('#B8926E'), hx('#513E2C')
CORD = [hx('#382A25'), hx('#493626'), hx('#855C2C'), hx('#885633')]
BOOK = [hx('#2E0F06'), hx('#4C1A0B'), hx('#652816'), hx('#75321E'), hx('#8A4128')]
PICTO = hx('#51493A')
TERRA_A, TERRA_B = hx('#AA6841'), hx('#9E5B39')
SL, SL2, SL_E, SL_S, SL_HOV, SL_DIS = hx('#222E3C'), hx('#243140'), hx('#344459'), hx('#1A2024'), hx('#323D4A'), hx('#141C24')
GOLDH = hx('#E2A650'); OCHRE = hx('#D6C194'); PENNANT = hx('#434D66')
INK_T = hx('#503D29'); INK_M = hx('#7A6248')
TURQ = (hx('#172933'), hx('#214E66'), hx('#349DBA'))


def flat_parchment(img, x, y, w, h, seed=1, torn=True, ornaments=True):
    random.seed(seed)
    rect(img, x, y, x + w, y + h, PA)
    rect(img, x, y, x + w, y + 1, PA_L); rect(img, x, y, x + 1, y + h, PA_L)
    rect(img, x, y + h - 1, x + w, y + h, PA_S); rect(img, x + w - 1, y, x + w, y + h, PA_S)
    if torn:
        for xx in range(x + 2, x + w - 2):
            if random.random() < .12: px(img, xx, y + h - 1, PA_D)
            if random.random() < .08: px(img, xx, y, PA_S)
        for yy in range(y + 2, y + h - 2):
            if random.random() < .10: px(img, x + w - 1, yy, PA_D)
    if ornaments:
        for cx, cy in ((x + 5, y + 5), (x + w - 6, y + 5), (x + 5, y + h - 6), (x + w - 6, y + h - 6)):
            for a in range(0, 360, 45):
                px(img, cx + round(math.cos(math.radians(a)) * 2), cy + round(math.sin(math.radians(a)) * 2), PA_S)
            px(img, cx, cy, PA_D)


def cord_frame(img, x, y, w, h, t=3):
    for i in range(w):
        for j in range(h):
            e = min(i, j, w - 1 - i, h - 1 - j)
            if e >= t: continue
            k = (i + j) % 4
            if e == 0: c = CORD[0]
            elif e == t - 1: c = CORD[1]
            else: c = CORD[3] if k < 2 else CORD[2]
            px(img, x + i, y + j, c)


def red_book(img, bx, by, bw, bh):
    cord_frame(img, bx - 3, by - 3, bw + 6, bh + 6)
    rect(img, bx, by, bx + bw, by + bh, BOOK[2])
    rect(img, bx, by, bx + bw, by + 1, BOOK[3]); rect(img, bx, by, bx + 1, by + bh, BOOK[3])
    rect(img, bx, by + bh - 1, bx + bw, by + bh, BOOK[1])
    # spine with raised bands
    rect(img, bx, by, bx + 7, by + bh, BOOK[1])
    for i in range(by + 10, by + bh - 8, 22):
        rect(img, bx + 1, i, bx + 6, i + 2, BOOK[3]); rect(img, bx + 1, i + 2, bx + 6, i + 3, BOOK[0])
    # blind-tooled border line
    rect(img, bx + 9, by + 2, bx + bw - 2, by + 3, BOOK[1]); rect(img, bx + 9, by + bh - 3, bx + bw - 2, by + bh - 2, BOOK[3])
    for cx, cy in ((bx + bw - 6, by + 3), (bx + bw - 6, by + bh - 6)):
        rect(img, cx, cy, cx + 3, cy + 3, G1); px(img, cx, cy, G3); px(img, cx + 2, cy + 2, G0)


def tile16(img, x, y, s=16, active=False):
    """Pack GUI tile: parchment, 1 px bevel, corners cut."""
    rect(img, x + 1, y, x + s - 1, y + s, PA_D); rect(img, x, y + 1, x + s, y + s - 1, PA_D)
    rect(img, x + 1, y + 1, x + s - 1, y + s - 1, PA_L if active else PA)
    rect(img, x + 1, y + 1, x + s - 1, y + 2, PA_L); rect(img, x + 1, y + 1, x + 2, y + s - 1, PA_L)
    rect(img, x + 1, y + s - 2, x + s - 1, y + s - 1, PA_S); rect(img, x + s - 2, y + 1, x + s - 1, y + s - 1, PA_S)


def slate_btn(img, x, y, w, h, label, hover=False, icon=None):
    o = GOLDH if hover else BLACK
    rect(img, x + 1, y, x + w - 1, y + h, o); rect(img, x, y + 1, x + w, y + h - 1, o)
    rect(img, x + 1, y + 1, x + w - 1, y + h - 1, SL_HOV if hover else SL)
    rect(img, x + 1, y + 1, x + w - 1, y + 2, SL_E); rect(img, x + 1, y + h - 2, x + w - 1, y + h - 1, SL_S)
    tx = x + w // 2
    if icon:
        icon(x + 4, y + (h - 9) // 2)
        tx += 6
    text(img, tx, y + (h - 8) // 2, label, GOLDH if hover else NAME, center=True)


def ribbon(img, x, y_bottom, w, h, c, label, active):
    dark = shade(c, .65); light = shade(c, 1.2)
    rect(img, x, y_bottom - h, x + w, y_bottom, dark)
    rect(img, x + 1, y_bottom - h + 1, x + w - 1, y_bottom, c)
    rect(img, x + 1, y_bottom - h + 1, x + w - 1, y_bottom - h + 2, light)
    text(img, x + w // 2, y_bottom - h + 4, label, NAME if active else OCHRE, center=True)


def tabs_row(img, x, by, items, active):
    for i, (lab, c) in enumerate(items):
        w = tw(lab) + 10
        ribbon(img, x, by + 2, w, 18 if i == active else 14, c if i == active else shade(c, .8), lab, i == active)
        x += w + 3


def picto_left(img, x, y): tri(img, x + 1, y + 2, -1, PICTO)
def picto_right(img, x, y): tri(img, x + 2, y + 2, 1, PICTO)
def picto_plus(img, x, y):
    rect(img, x + 3, y + 1, x + 5, y + 8, PICTO); rect(img, x + 1, y + 3, x + 8, y + 5, PICTO)
def picto_cut(img, x, y): scissors(img, x + 1, y + 1, PICTO)
def picto_quill(img, x, y):
    for i in range(7): px(img, x + 1 + i, y + 7 - i, PICTO)
    for i in range(2, 6): px(img, x + 2 + i, y + 7 - i, PICTO); px(img, x + 1 + i, y + 6 - i, PICTO)
    px(img, x, y + 8, PICTO)
def picto_seal(img, x, y):
    disc(img, x + 4, y + 4, 4, (WAX[1], WAX[2], WAX[3]))


TOOLS = [picto_left, picto_right, picto_plus, picto_cut, picto_quill]
TABS_BRIEF = (('Brief', TERRA_A), ('Buch', hx('#866C36')), ('Entwurfe', PENNANT))
TABS_PEOPLE = (('Personen', TERRA_A), ('Titel', hx('#866C36')), ('Import', PENNANT))


def tool_column(img, x, y):
    for i, fn in enumerate(TOOLS):
        tile16(img, x, y + i * 19)
        fn(img, x + 4, y + i * 19 + 4)


def ink_column(img, x, y):
    """16 Minecraft colours as ink drops on a parchment strip with cord edge."""
    cord_frame(img, x - 3, y - 3, 30, 8 * 11 + 6 + 6, t=2)
    flat_parchment(img, x - 1, y - 1, 26, 8 * 11 + 4, torn=False, ornaments=False)
    for i, c in enumerate(MC):
        cx, cy = x + 6 + (i % 2) * 12, y + 5 + (i // 2) * 11
        disc(img, cx, cy, 4, (shade(c, .55), c, shade(c, 1.25) if c != MC[15] else c))
    yy = y + 8 * 11 + 10
    for i, s in enumerate(('B', 'I', 'U', 'S')):
        tile16(img, x + (i % 2) * 13 - 1, yy + (i // 2) * 15, 13)
        text(img, x + (i % 2) * 13 + 5, yy + (i // 2) * 15 + 3, s, PICTO, center=True, shadow=False)
    rect(img, x, yy + 30, x + 24, yy + 43, PA_D); rect(img, x + 1, yy + 31, x + 23, yy + 42, PA)
    rect(img, x + 1, yy + 31, x + 23, yy + 32, PA_L); rect(img, x + 1, yy + 41, x + 23, yy + 42, PA_S)
    text(img, x + 12, yy + 33, 'Tx', PICTO, center=True, shadow=False)


def write_btn(img, x, y):
    slate_btn(img, x, y, 70, 20, 'Schreiben', hover=True, icon=lambda a, b: picto_seal(img, a, b))


def page_marker2(img, x, y, w, label):
    for xx in range(x, x + w, 4): rect(img, xx, y + 4, xx + 2, y + 5, PA_S)
    lw = tw(label) + 14
    rect(img, x + w // 2 - lw // 2, y, x + w // 2 + lw // 2, y + 9, PA)
    scissors(img, x + w // 2 - lw // 2 + 1, y + 1, PICTO)
    text(img, x + w // 2 - lw // 2 + 11, y + 1, label, INK_M, shadow=False)


def brief(mode):
    img = backdrop()
    bw, bh = 176, 200
    bx, by = (W - bw) // 2, 26
    red_book(img, bx, by, bw, bh)
    lx, ly, lw, lh = bx + 10, by + 6, bw - 16, bh - 12
    flat_parchment(img, lx, ly, lw, lh, seed=7)
    for i in range(4): rect(img, lx + i, ly + 1, lx + i + 1, ly + lh - 1, shade(PA, .88 + i * .03))
    if mode == 'edit':
        text(img, lx + 12, ly + 7, 'Arbeitsseite 1 / 2', INK_M, shadow=False)
        rect(img, lx + 10, ly + 17, lx + lw - 10, ly + 18, PA_S)
        write_lines(img, lx + 12, ly + 22, 12, lw - 22, (184, 146, 110, 255), n=8)
        page_marker2(img, lx + 8, ly + 22 + 8 * 12 + 1, lw - 16, 'Seite 2')
        text(img, lx + 12, ly + 22 + 9 * 12 + 2, 'Hochachtungsvoll,', INK_M, shadow=False)
        text(img, lx + 12, ly + 22 + 10 * 12 + 2, 'Ritter Ansgar', INK_T, shadow=False)
        text(img, lx + 13, ly + 22 + 10 * 12 + 2, 'Ritter Ansgar', INK_T, shadow=False)
    else:
        text(img, lx + 12, ly + 7, 'Wie im Buch', INK_T, shadow=False)
        text(img, lx + lw - 12 - tw('1 / 2'), ly + 7, '1 / 2', INK_M, shadow=False)
        rect(img, lx + 10, ly + 17, lx + lw - 10, ly + 18, PA_S)
        preview(img, lx + 12, ly + 22, 114, 10, 13)
        text(img, lx + lw // 2, ly + lh - 14, 'Nur Ansicht', INK_M, center=True, shadow=False)
    tabs_row(img, bx + 12, by - 3, TABS_BRIEF, 0 if mode == 'edit' else 1)
    ink_column(img, bx - 34, by + 6)
    tool_column(img, bx + bw + 6, by + 10)
    write_btn(img, bx + bw + 6, by + bh - 22)
    return img


def people():
    img = backdrop()
    bw, bh = 176, 200
    bx, by = 58, 26
    red_book(img, bx, by, bw, bh)
    lx, ly, lw, lh = bx + 10, by + 6, bw - 16, bh - 12
    flat_parchment(img, lx, ly, lw, lh, seed=31)
    for i in range(4): rect(img, lx + i, ly + 1, lx + i + 1, ly + lh - 1, shade(PA, .88 + i * .03))
    # search field in pack style: dark slate field
    sx, sy = lx + 10, ly + 7
    rect(img, sx, sy, sx + 104, sy + 13, BLACK); rect(img, sx + 1, sy + 1, sx + 103, sy + 12, SL_DIS)
    for p in ((2, 1), (3, 1), (1, 2), (4, 2), (1, 3), (4, 3), (2, 4), (3, 4), (5, 5), (6, 6)):
        px(img, sx + 3 + p[0], sy + 2 + p[1], OCHRE)
    text(img, sx + 13, sy + 3, 'Suche...', hx('#7F7E87'), shadow=False)
    text(img, lx + lw - 10 - tw('124'), ly + 9, '124', INK_M, shadow=False)
    # rows
    y0 = ly + 26
    for i, (name, acc, state, online, conflict, _) in enumerate(PEOPLE):
        yy = y0 + i * 20
        if i == SEL:
            rect(img, lx + 6, yy - 1, lx + lw - 6, yy + 18, PA_L)
            rect(img, lx + 6, yy - 1, lx + 8, yy + 18, TERRA_A)
        rect(img, lx + 11, yy + 2, lx + 21, yy + 12, PA_D); head(img, lx + 12, yy + 3, i)
        unknown = name == 'Unbekannt'
        text(img, lx + 25, yy, name, INK_M if unknown else INK_T, shadow=False)
        text(img, lx + 25, yy + 9, acc + ' - ' + state, INK_M, shadow=False)
        if online: disc(img, lx + lw - 13, yy + 4, 2, (hx('#2F5426'), hx('#5E9A3E'), hx('#86C25A')))
        if conflict: text(img, lx + lw - 15, yy + 9, '!', WAX[2], shadow=False)
        if i < len(PEOPLE) - 1: rect(img, lx + 10, yy + 18, lx + lw - 10, yy + 19, PA_S)
    tabs_row(img, bx + 12, by - 3, TABS_PEOPLE, 0)
    # filter tiles on the left (pack GUI tiles with pictogram + label)
    for k, (lab, c) in enumerate((('Alle', TERRA_A), ('Online', hx('#5E9A3E')), ('Fremd', PENNANT), ('Konflikt', hx('#B24147')))):
        yy = by + 12 + k * 22
        w = tw(lab) + 12
        x = bx - w - 3
        rect(img, x, yy, x + w + 3, yy + 16, shade(c, .6)); rect(img, x + 1, yy + 1, x + w + 3, yy + 15, c if k == 0 else shade(c, .8))
        rect(img, x + 1, yy + 1, x + w + 3, yy + 2, shade(c, 1.2))
        text(img, x + w // 2 + 1, yy + 4, lab, NAME if k == 0 else OCHRE, center=True)
    # dossier leaf with cord edge and wax seal
    dx, dy, dw, dh = bx + bw + 8, by + 12, 146, 178
    rect(img, dx + 3, dy + 3, dx + dw + 3, dy + dh + 3, (0, 0, 0, 110))
    cord_frame(img, dx - 2, dy - 2, dw + 4, dh + 4, t=2)
    flat_parchment(img, dx, dy, dw, dh, seed=32)
    wax_seal(img, dx + dw - 12, dy + 10, 7)
    name, acc, state, online, conflict, title = PEOPLE[SEL]
    x, y = dx + 9, dy + 10
    rect(img, x, y, x + 28, y + 28, CORD[0]); rect(img, x + 1, y + 1, x + 27, y + 27, G2); rect(img, x + 2, y + 2, x + 26, y + 26, CORD[1])
    head(img, x + 2, y + 2, SEL, 3)
    text(img, x + 34, y + 1, name, INK_T, shadow=False); text(img, x + 35, y + 1, name, INK_T, shadow=False)
    text(img, x + 34, y + 11, acc, INK_M, shadow=False)
    sw = tw(state) + 8
    rect(img, x + 34, y + 20, x + 34 + sw, y + 31, hx('#2F5426')); rect(img, x + 35, y + 21, x + 33 + sw, y + 30, hx('#5E9A3E'))
    text(img, x + 34 + sw // 2, y + 22, state, NAME, center=True)
    disc(img, x + 34 + sw + 7, y + 25, 3, (hx('#2F5426'), hx('#5E9A3E'), hx('#86C25A')))
    fy = y + 38
    for j, (lab, val, lock) in enumerate((('RP-Name', 'Ansgar', False), ('Titel', 'Ritter', True), ('Notiz', 'Schmied, Hafen', False))):
        yy = fy + j * 15
        text(img, x, yy, lab, INK_M, shadow=False)
        rect(img, x + 46, yy + 9, x + dw - 18, yy + 10, PA_S)
        text(img, x + 48, yy, val, INK_T, shadow=False)
        if lock:
            lx2 = x + dw - 26
            rect(img, lx2 + 1, yy, lx2 + 5, yy + 2, PICTO); rect(img, lx2, yy + 2, lx2 + 6, yy + 7, PICTO)
    yy = fy + 47
    text(img, x, yy, 'Farben', INK_M, shadow=False)
    yy += 12
    cx = x
    for lab, c in (('Chat', hx('#FFAA00')), ('Tab', hx('#55FF55')), ('Kopf', hx('#FFFFFF'))):
        tile16(img, cx, yy - 2, 11); rect(img, cx + 3, yy + 1, cx + 8, yy + 6, c)
        text(img, cx + 13, yy, lab, INK_T, shadow=False)
        cx += tw(lab) + 18
    figure(img, dx + dw - 24, dy + 112, SEL)
    slate_btn(img, dx + 8, dy + dh - 24, 64, 18, 'Speichern', hover=True)
    slate_btn(img, dx + 74, dy + dh - 24, 64, 18, 'Vergessen')
    return img


brief('edit').resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUTD, 'brief_ottonien_edit.png'))
brief('view').resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUTD, 'brief_ottonien_buch.png'))
people().resize((W * K, H * K), Image.NEAREST).save(os.path.join(OUTD, 'personen_ottonien.png'))
print('ok')
