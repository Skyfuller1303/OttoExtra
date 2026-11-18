"""Idea sketches for the single resource book of OttoExtra Resources: lists on the left page,
zones on the right page, slate-blue cover, no text shadows.
Usage: python -I gen_vorkommen_buch.py <out_dir>"""
import os, sys
_b = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_rpbuch2.py'), encoding='utf-8').read()
exec(_b[:_b.index("personen2().resize")])
from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
S = 4

COVER = [hx('#141A21'), hx('#222B36'), hx('#313C49'), hx('#3B4756'), hx('#4F5C6D')]
PA, PA_L, PA_D, PA_S = hx('#C9A97C'), hx('#D8BC92'), hx('#B08F63'), hx('#BF9E70')
INK, INK_M, INK_F = hx('#35281C'), hx('#6E5A44'), hx('#8E785E')
TERRA, BRASS = hx('#9E4B32'), hx('#866C36')
SLATE, SLATE_HI, SLATE_LO, SLATE_T = hx('#2E3A4A'), hx('#46566C'), hx('#1A2028'), hx('#E8E2D6')
OK_INK, WARN_INK, SOON_INK = hx('#3F6B2C'), hx('#8A6410'), hx('#A0491E')
TYPE_COL = {'Eisen': hx('#C9A58A'), 'Kohle': hx('#55555F'), 'Salz': hx('#E6E2EC'), 'Torf': hx('#7A5A3A'),
            'Lehm': hx('#9AA3B5'), 'Garten': hx('#6FB35A'), 'Bienen': hx('#E0B43A'), 'Marker': hx('#B56FC7')}

LISTS = [('Schwarzhain', 6, 2, 'geteilt', True), ('Nordeck', 3, 1, 'nur lesen', False), ('Privat', 2, 0, 'lokal', True)]
ZONES = [('Eisenmine Nordstollen', 'Eisen', 'Mine', 0, 100, 42), ('Kohlevorkommen Bach', 'Kohle', 'Vorkommen', 0, 100, 118),
         ('Salzmine Alter Schacht', 'Salz', 'Mine', 8, 70, 96), ('Lehmgrube Furt', 'Lehm', '', 21, 30, 203),
         ('Eisenvorkommen Hang', 'Eisen', 'Vorkommen', 74, 38, 61), ('Torfmoor Senke', 'Torf', '', 191, 20, 240)]


def R(img, x0, y0, x1, y1, c):
    rect(img, x0 * S, y0 * S, x1 * S, y1 * S, c)


def T(img, x, y, s, c=INK):
    text(img, x * S, y * S, s, c, S, False, False)


def Ts(img, x, y, s, c=INK_M):
    text(img, x * S, y * S, s, c, 3, False, False)


def fmt(m):
    return 'bereit' if m <= 0 else f'{m // 60}:{m % 60:02d} h' if m >= 60 else f'{m} min'


def tcol(m):
    return OK_INK if m <= 0 else SOON_INK if m <= 10 else WARN_INK


def gem(img, x, y, col, k=1):
    dark = shade(col, .6)
    R(img, x + k, y, x + 3 * k, y + 3 * k, col); R(img, x, y + k, x + k, y + 3 * k, col)
    R(img, x + 3 * k, y + k, x + 4 * k, y + 3 * k, dark); R(img, x + k, y + 3 * k, x + 3 * k, y + 4 * k, dark)
    R(img, x + k, y + k, x + k + 1, y + k + 1, shade(col, 1.25))


def slate(img, x, y, w, label, h=14, primary=False):
    col = hx('#35552A') if primary else SLATE
    R(img, x, y, x + w, y + h, COVER[0]); R(img, x + 1, y + 1, x + w - 1, y + h - 1, col)
    R(img, x + 1, y + 1, x + w - 1, y + 2, shade(col, 1.3)); R(img, x + 1, y + h - 2, x + w - 1, y + h - 1, shade(col, .7))
    T(img, x + (w - tw(label)) // 2, y + 3, label, SLATE_T)


def ink_field(img, x, y, w, value, hint, focus=False):
    R(img, x, y + 12, x + w, y + 13, TERRA if focus else INK_M)
    if value:
        T(img, x + 2, y + 2, value)
        if focus: R(img, x + 3 + tw(value), y + 1, x + 4 + tw(value), y + 11, INK)
    else:
        T(img, x + 2, y + 2, hint, INK_F)


def rule(img, x0, x1, y, c=PA_D):
    R(img, x0, y, x1, y + 1, c)


def book(img):
    """Slate-blue cover with two parchment pages; returns (left, right) page rects (x, y, w, h)."""
    bx, by, bw, bh = 14, 12, W - 28, H - 24
    R(img, bx + 2, by + 3, bx + bw + 2, by + bh + 3, (0, 0, 0, 90))
    R(img, bx, by, bx + bw, by + bh, COVER[0]); R(img, bx + 1, by + 1, bx + bw - 1, by + bh - 1, COVER[2])
    R(img, bx + 1, by + 1, bx + bw - 1, by + 3, COVER[4]); R(img, bx + 1, by + bh - 3, bx + bw - 1, by + bh - 1, COVER[1])
    mid = bx + 150
    pages = []
    for px_, pw_ in ((bx + 7, mid - bx - 9), (mid + 2, bx + bw - 7 - mid - 2)):
        py_, ph_ = by + 6, bh - 12
        R(img, px_, py_, px_ + pw_, py_ + ph_, PA_D); R(img, px_ + 1, py_ + 1, px_ + pw_ - 1, py_ + ph_ - 1, PA)
        R(img, px_ + 1, py_ + 1, px_ + pw_ - 1, py_ + 2, PA_L)
        pages.append((px_, py_, pw_, ph_))
    R(img, mid - 2, by + 4, mid + 2, by + bh - 4, COVER[1]); R(img, mid - 1, by + 4, mid, by + bh - 4, COVER[3])
    lx, ly, lw, lh = pages[0]; R(img, lx + lw - 4, ly + 1, lx + lw - 1, ly + lh - 1, PA_S)
    rx, ry, rw, rh = pages[1]; R(img, rx + 1, ry + 1, rx + 4, ry + rh - 1, PA_S)
    return pages


LIST_ICON = {'Schwarzhain': ('shield', hx('#3F6E9E')), 'Nordeck': ('tower', hx('#9E4B32')),
             'Privat': ('pick', hx('#6E5A44'))}
ICON_SHAPES = ['shield', 'tower', 'flag', 'pick']
ICON_COLORS = [hx('#3F6E9E'), hx('#9E4B32'), hx('#4E7A3A'), hx('#866C36'), hx('#6E4A7E'), hx('#6E5A44'),
               hx('#2F6E6A'), hx('#55555F')]


def list_icon(img, x, y, kind, col):
    """9x10 GUI px list symbol."""
    dark, light = shade(col, .6), shade(col, 1.3)
    if kind == 'shield':
        R(img, x, y, x + 9, y + 6, col); R(img, x + 1, y + 6, x + 8, y + 8, col); R(img, x + 3, y + 8, x + 6, y + 10, col)
        R(img, x + 1, y + 1, x + 3, y + 5, light); R(img, x + 6, y + 6, x + 8, y + 8, dark)
    elif kind == 'tower':
        R(img, x + 1, y + 3, x + 8, y + 10, col)
        for bx_ in (1, 4, 7):
            R(img, x + bx_, y + 1, x + bx_ + 1, y + 3, col)
        R(img, x + 1, y + 3, x + 3, y + 10, light); R(img, x + 4, y + 7, x + 5, y + 10, dark)
    elif kind == 'flag':
        R(img, x + 1, y, x + 2, y + 10, INK_M); R(img, x + 2, y + 1, x + 9, y + 6, col)
        R(img, x + 2, y + 1, x + 9, y + 2, light); R(img, x + 7, y + 3, x + 9, y + 4, dark)
    else:  # pick
        R(img, x + 4, y + 3, x + 5, y + 10, hx('#7A5A3A'))
        R(img, x + 1, y + 1, x + 8, y + 3, col); R(img, x, y + 2, x + 1, y + 4, col); R(img, x + 8, y + 2, x + 9, y + 4, col)
        R(img, x + 1, y + 1, x + 8, y + 2, light)


def left_page(img, page, active=0, code_focus=False, lists=None):
    x, y, w, h = page
    lists = LISTS if lists is None else lists
    T(img, x + 8, y + 7, 'Listen', TERRA); rule(img, x + 8, x + w - 10, y + 18)
    cy = y + 23
    if not lists:
        Ts(img, x + 11, cy + 2, 'Noch keine Liste.', INK_F); cy += 14
    for i, (name, n, ready, state, _) in enumerate(lists):
        if i == active:
            R(img, x + 5, cy - 2, x + w - 8, cy + 21, PA_L); R(img, x + 5, cy - 2, x + 7, cy + 21, TERRA)
        list_icon(img, x + 11, cy + 1, *LIST_ICON.get(name, ('flag', BRASS)))
        T(img, x + 24, cy, name, INK)
        Ts(img, x + 24, cy + 11, f'{n} Vorkommen, {state}', INK_M)
        r = f'{ready} bereit'
        Ts(img, x + w - 12 - round(tw(r) * .75), cy + 1, r, OK_INK if ready else INK_F)
        cy += 26
    T(img, x + 11, cy + 2, '+ Neue Liste', BRASS)
    fy = y + h - 34
    rule(img, x + 8, x + w - 10, fy - 5)
    Ts(img, x + 8, fy, 'Code von jemand anderem', INK_M)
    ink_field(img, x + 8, fy + 8, w - 70, '' if not code_focus else '932GS-5RFMQ', 'Code einfügen', code_focus)
    slate(img, x + w - 58, fy + 7, 48, 'Beitreten')


def header(img, page, title, sub, actions):
    x, y, w, h = page
    T(img, x + 10, y + 7, title, TERRA)
    Ts(img, x + 12 + tw(title), y + 9, sub, INK_M)
    ax = x + w - 8
    for label, wd, prim in reversed(actions):
        ax -= wd; slate(img, ax, y + 4, wd, label, primary=prim); ax -= 3
    rule(img, x + 10, x + w - 8, y + 20)


def rows_plain(img, page):
    """A: one line per zone, dot, name, distance, time."""
    x, y, w, h = page; cy = y + 25
    for i, (name, typ, scope, m, pct, d) in enumerate(ZONES):
        if i == 2: R(img, x + 6, cy - 2, x + w - 6, cy + 11, PA_L)
        gem(img, x + 11, cy + 2, TYPE_COL[typ])
        T(img, x + 19, cy, name)
        t = fmt(m); T(img, x + w - 10 - tw(t), cy, t, tcol(m))
        Ts(img, x + w - 70, cy + 2, f'{d} m', INK_F)
        cy += 14
    Ts(img, x + 10, y + h - 12, 'Linksklick startet, Rechtsklick setzt zurück, Doppelklick bearbeitet.', INK_F)


def rows_cards(img, page):
    """B: two-line rows with type caption and progress bar."""
    x, y, w, h = page; cy = y + 25
    for i, (name, typ, scope, m, pct, d) in enumerate(ZONES[:5]):
        if i == 2: R(img, x + 6, cy - 2, x + w - 6, cy + 23, PA_L)
        gem(img, x + 11, cy + 3, TYPE_COL[typ], 2)
        T(img, x + 24, cy, name)
        cap = typ + (', ' + scope if scope else '') + f', {d} m entfernt'
        Ts(img, x + 24, cy + 11, cap, INK_M)
        t = fmt(m); T(img, x + w - 10 - tw(t), cy, t, tcol(m))
        bx0, bx1 = x + w - 62, x + w - 10
        R(img, bx0, cy + 13, bx1, cy + 16, PA_D)
        R(img, bx0, cy + 13, bx0 + round((bx1 - bx0) * pct / 100), cy + 16, tcol(m))
        rule(img, x + 10, x + w - 8, cy + 23, PA_S)
        cy += 26
    Ts(img, x + 10, y + h - 12, '+ 1 weiteres, scrollen', INK_F)


def rows_grouped(img, page):
    """C: grouped by state: ready first, then running, one line each."""
    x, y, w, h = page; cy = y + 25
    for title, sel in (('Bereit', lambda z: z[3] <= 0), ('Erholt sich', lambda z: z[3] > 0)):
        zs = [z for z in ZONES if sel(z)]
        Ts(img, x + 10, cy, f'{title} ({len(zs)})', TERRA); rule(img, x + 10 + round(tw(f'{title} ({len(zs)})') * .75) + 4, x + w - 8, cy + 4, PA_D)
        cy += 11
        for name, typ, scope, m, pct, d in zs:
            gem(img, x + 11, cy + 2, TYPE_COL[typ])
            T(img, x + 19, cy, name)
            t = fmt(m) if m > 0 else f'{d} m'
            T(img, x + w - 10 - tw(t), cy, t, tcol(m) if m > 0 else INK_M)
            if m > 0:
                R(img, x + 19, cy + 10, x + w - 10, cy + 11, PA_D)
                R(img, x + 19, cy + 10, x + 19 + round((w - 29) * pct / 100), cy + 11, tcol(m))
            cy += 14
        cy += 5


def scene(rows, label, code_focus=False):
    img = backdrop().resize((W * S, H * S), Image.NEAREST)
    rect(img, 0, 0, W * S, H * S, (0, 0, 0, 110))
    left, right = book(img)
    left_page(img, left, 0, code_focus)
    header(img, right, 'Schwarzhain', '6 Vorkommen', [('Teilen', 40, False), ('+ Vorkommen', 70, True)])
    rows(img, right)
    text(img, 16 * S, (H - 10) * S, label, hx('#F0EBE0'), 3, False, True)
    return img


def new_list():
    img = backdrop().resize((W * S, H * S), Image.NEAREST)
    rect(img, 0, 0, W * S, H * S, (0, 0, 0, 110))
    left, right = book(img)
    left_page(img, left, -1)
    x, y, w, h = right
    T(img, x + 10, y + 7, 'Neue Liste', TERRA); rule(img, x + 10, x + w - 8, y + 20)
    cy = y + 28
    Ts(img, x + 10, cy, 'Name'); ink_field(img, x + 10, cy + 8, w - 20, 'Schwarzhain', '', True); cy += 30
    Ts(img, x + 10, cy, 'Notiz (optional)'); ink_field(img, x + 10, cy + 8, w - 20, '', 'z. B. Minen rund um die Burg'); cy += 30
    Ts(img, x + 10, cy, 'Lehen'); T(img, x + 10, cy + 10, 'Schwarzhain', INK); Ts(img, x + 12 + tw('Schwarzhain'), cy + 12, '(hier stehst du gerade)', INK_F); cy += 28
    R(img, x + 10, cy, x + 19, cy + 9, INK_M); R(img, x + 11, cy + 1, x + 18, cy + 8, PA_L)
    T(img, x + 24, cy + 1, 'Gleich mit anderen teilen', INK)
    Ts(img, x + 24, cy + 12, 'Erzeugt einen Code zum Weitergeben. Geht auch später.', INK_M)
    slate(img, x + w - 8 - 64, y + h - 22, 64, 'Anlegen', primary=True)
    slate(img, x + w - 8 - 64 - 3 - 60, y + h - 22, 60, 'Abbrechen')
    text(img, 16 * S, (H - 10) * S, 'Neue Liste: Name, Notiz, optional sofort teilen', hx('#F0EBE0'), 3, False, True)
    return img


scene(rows_plain, 'A: eine Zeile je Vorkommen').save(os.path.join(OUT, 'buch_a_zeilen.png'))
scene(rows_cards, 'B: zwei Zeilen mit Art, Entfernung und Balken').save(os.path.join(OUT, 'buch_b_karten.png'))
scene(rows_grouped, 'C: nach Zustand gruppiert', code_focus=True).save(os.path.join(OUT, 'buch_c_gruppiert.png'))
new_list().save(os.path.join(OUT, 'buch_neue_liste.png'))


def slate_col(img, x, y, w, label, col, h=14):
    R(img, x, y, x + w, y + h, COVER[0]); R(img, x + 1, y + 1, x + w - 1, y + h - 1, col)
    R(img, x + 1, y + 1, x + w - 1, y + 2, shade(col, 1.3)); R(img, x + 1, y + h - 2, x + w - 1, y + h - 1, shade(col, .7))
    T(img, x + (w - tw(label)) // 2, y + 3, label, SLATE_T)


DANGER = hx('#6A2E26')


def chip(img, x, y, w, label, sel, gem_col=None, h=13):
    R(img, x, y, x + w, y + h, TERRA if sel else PA_D)
    R(img, x + 1, y + 1, x + w - 1, y + h - 1, PA_L if sel else PA)
    tx = x + 4
    if gem_col:
        gem(img, x + 4, y + 3, gem_col, 2); tx = x + 15
    T(img, tx, y + 3, label, INK if sel else INK_M)


def checkbox(img, x, y, checked):
    R(img, x, y, x + 9, y + 9, INK_M); R(img, x + 1, y + 1, x + 8, y + 8, PA_L)
    if checked:
        for i, (cx_, cy_) in enumerate(((2, 4), (3, 5), (4, 6), (5, 5), (6, 4), (7, 3))):
            R(img, x + cx_, y + cy_, x + cx_ + 1, y + cy_ + 1, INK)


def spread():
    img = backdrop().resize((W * S, H * S), Image.NEAREST)
    rect(img, 0, 0, W * S, H * S, (0, 0, 0, 110))
    return img, book(img)


def label_bottom(img, s):
    text(img, 16 * S, (H - 10) * S, s, hx('#F0EBE0'), 3, False, True)


def editor_book():
    img, (left, right) = spread()
    x, y, w, h = left
    T(img, x + 10, y + 7, 'Neues Vorkommen', TERRA); rule(img, x + 10, x + w - 10, y + 20)
    cy = y + 26
    Ts(img, x + 10, cy, 'Name'); ink_field(img, x + 10, cy + 7, w - 20, '', 'z. B. Eisenmine am Bach', True); cy += 28
    Ts(img, x + 10, cy, 'Art'); cy += 8
    cw_ = (w - 20 - 4) // 2
    for i, (name, col) in enumerate(TYPE_COL.items()):
        chip(img, x + 10 + (i % 2) * (cw_ + 4), cy + (i // 2) * 15, cw_, name, i == 0, col)
    cy += 4 * 15 + 5
    Ts(img, x + 10, cy, 'Umfang'); cy += 8
    chip(img, x + 10, cy, cw_, 'Vorkommen', False); chip(img, x + 14 + cw_, cy, cw_, 'Mine', True); cy += 21
    Ts(img, x + 10, cy, 'Lehen'); Ts(img, x + 36, cy, '(erkannt, änderbar)', INK_F)
    ink_field(img, x + 10, cy + 7, w - 20, 'Schwarzhain', '')

    x, y, w, h = right
    list_icon(img, x + 10, y + 6, *LIST_ICON['Schwarzhain'])
    T(img, x + 23, y + 7, 'Schwarzhain', INK); Ts(img, x + w - 10 - round(tw('andere Liste') * .75), y + 9, 'andere Liste', BRASS)
    rule(img, x + 10, x + w - 8, y + 20)
    cy = y + 26
    Ts(img, x + 10, cy, 'Mitte'); ink_field(img, x + 10, cy + 7, 150, '-1260  64  3418', '')
    slate(img, x + w - 10 - 82, cy + 5, 82, 'Meine Position'); cy += 28
    Ts(img, x + 10, cy, 'Radius'); cy += 9
    R(img, x + 10, cy + 5, x + 180, cy + 7, PA_D); R(img, x + 10, cy + 5, x + 58, cy + 7, INK_M)
    R(img, x + 56, cy + 1, x + 61, cy + 11, BRASS); R(img, x + 57, cy + 2, x + 60, cy + 3, shade(BRASS, 1.4))
    T(img, x + 188, cy + 2, '14 Blöcke'); cy += 15
    Ts(img, x + 10, cy, 'Ein Partikelring zeigt den Radius in der Welt.', INK_F); cy += 12
    rule(img, x + 10, x + w - 8, cy, PA_S); cy += 6
    Ts(img, x + 10, cy, 'Erholung'); cy += 9
    T(img, x + 10, cy, 'nach 2:00 h zu 70 %', WARN_INK); T(img, x + 14 + tw('nach 2:00 h zu 70 %,'), cy, 'nach 4:00 h voll', OK_INK)
    T(img, x + 10 + tw('nach 2:00 h zu 70 %'), cy, ',', INK_M); cy += 15
    checkbox(img, x + 10, cy + 2, False); T(img, x + 24, cy + 3, 'Eigene Zeit', INK_M)
    ink_field(img, x + 24 + tw('Eigene Zeit') + 8, cy, 70, '', 'Minuten'); cy += 22
    Ts(img, x + 10, cy, 'Notiz'); ink_field(img, x + 10, cy + 7, w - 20, '', 'optional, z. B. Eingang hinter der Mühle')
    Ts(img, x + 10, y + h - 34, 'Sichtbar für alle mit dem Code der Liste.', INK_F)
    slate(img, x + w - 8 - 60, y + h - 22, 60, 'Speichern', primary=True)
    slate(img, x + w - 8 - 60 - 3 - 60, y + h - 22, 60, 'Abbrechen')
    label_bottom(img, 'Editor als Doppelseite: links was, rechts wo und wie lange')
    return img


def share_book():
    img, (left, right) = spread()
    left_page(img, left, 0)
    x, y, w, h = right
    T(img, x + 10, y + 7, 'Teilen', TERRA); Ts(img, x + 12 + tw('Teilen'), y + 9, 'Schwarzhain', INK_M)
    rule(img, x + 10, x + w - 8, y + 20)
    cy = y + 28
    for title, sub, code in (('Für deine Gruppe', 'sehen und bearbeiten', '932GS-5RFMQ-JMG2Y-S8555'),
                             ('Für andere', 'nur ansehen', 'R4092001788584BA284ADC90E')):
        T(img, x + 10, cy, title); Ts(img, x + 14 + tw(title), cy + 2, sub, INK_M); cy += 12
        bw_ = w - 20 - 92
        R(img, x + 10, cy, x + 10 + bw_, cy + 16, PA_D); R(img, x + 11, cy + 1, x + 9 + bw_, cy + 15, PA_L)
        T(img, x + 15, cy + 4, code, INK)
        slate(img, x + 14 + bw_, cy + 1, 50, 'Kopieren'); slate(img, x + 67 + bw_, cy + 1, 35, 'Link')
        cy += 28
    R(img, x + 10, cy, x + 12, cy + 22, SOON_INK)
    Ts(img, x + 17, cy + 1, 'Der Code für deine Gruppe ist wie ein Passwort. Wer ihn hat,', SOON_INK)
    Ts(img, x + 17, cy + 9, 'kann die Liste ändern und leeren. Nur an Vertraute geben.', SOON_INK)
    Ts(img, x + 17, cy + 17, 'Die Links führen auf ottoextra.dev/r/... mit Kopierknopf.', INK_M)
    slate_col(img, x + 10, y + h - 22, 82, 'Codes erneuern', DANGER)
    Ts(img, x + 96, y + h - 18, 'alte Codes werden ungültig', INK_F)
    slate(img, x + w - 8 - 50, y + h - 22, 50, 'Fertig', primary=True)
    label_bottom(img, 'Teilen: zwei Codes, kopieren oder als Link')
    return img


def list_settings_book():
    img, (left, right) = spread()
    left_page(img, left, 0)
    x, y, w, h = right
    T(img, x + 10, y + 7, 'Liste bearbeiten', TERRA); rule(img, x + 10, x + w - 8, y + 20)
    cy = y + 26
    Ts(img, x + 10, cy, 'Name'); ink_field(img, x + 10, cy + 7, w - 20, 'Schwarzhain', ''); cy += 26
    Ts(img, x + 10, cy, 'Notiz'); ink_field(img, x + 10, cy + 7, w - 20, 'Minen rund um die Burg', ''); cy += 28
    Ts(img, x + 10, cy, 'Symbol'); cy += 8
    for i, shape in enumerate(ICON_SHAPES):
        sx = x + 10 + i * 20
        R(img, sx, cy, sx + 17, cy + 16, TERRA if i == 0 else PA_D); R(img, sx + 1, cy + 1, sx + 16, cy + 15, PA_L if i == 0 else PA)
        list_icon(img, sx + 4, cy + 3, shape, ICON_COLORS[0])
    Ts(img, x + 100, cy - 8, 'Farbe')
    for i, col in enumerate(ICON_COLORS):
        sx = x + 100 + i * 14
        R(img, sx - 1, cy - 1 + 2, sx + 12, cy + 12 + 2, TERRA if i == 0 else PA)
        R(img, sx, cy + 2, sx + 11, cy + 13, col); R(img, sx, cy + 2, sx + 11, cy + 3, shade(col, 1.3))
    cy += 26
    Ts(img, x + 10, cy, 'Zugang'); cy += 9
    T(img, x + 10, cy, 'Geteilt, du kannst bearbeiten.', INK); T(img, x + 14 + tw('Geteilt, du kannst bearbeiten.'), cy, 'Codes zeigen', BRASS)
    cy += 14
    Ts(img, x + 10, cy, 'Name, Notiz und Symbol sehen alle, die der Liste beitreten.', INK_F)
    slate_col(img, x + 10, y + h - 22, 78, 'Liste löschen', DANGER)
    slate(img, x + w - 8 - 60, y + h - 22, 60, 'Speichern', primary=True)
    slate(img, x + w - 8 - 60 - 3 - 60, y + h - 22, 60, 'Abbrechen')
    label_bottom(img, 'Liste bearbeiten: Name, Notiz, eigenes Symbol')
    return img


def empty_book():
    img, (left, right) = spread()
    left_page(img, left, -1, lists=[])
    x, y, w, h = right
    T(img, x + 10, y + 7, 'So geht es', TERRA); rule(img, x + 10, x + w - 8, y + 20)
    steps = [('Liste anlegen', 'Eine Liste je Lehen oder Gruppe, links unter "+ Neue Liste".'),
             ('Vorkommen eintragen', 'Am Vorkommen "+ Vorkommen" oder per Befehl, Ring prüfen.'),
             ('Abbauen', 'Der Timer startet beim Abbau, das HUD zeigt die Restzeit.'),
             ('Teilen', 'Code weitergeben, dann sehen alle dieselben Timer.')]
    cy = y + 28
    for i, (title, sub) in enumerate(steps):
        R(img, x + 10, cy, x + 22, cy + 12, TERRA); T(img, x + 14, cy + 2, str(i + 1), PA_L)
        T(img, x + 28, cy, title); Ts(img, x + 28, cy + 11, sub, INK_M)
        cy += 30
    Ts(img, x + 10, cy, 'Oder unten links den Code eingeben, den dir jemand gegeben hat.', INK_F)
    slate(img, x + w - 8 - 70, y + h - 22, 70, '+ Neue Liste', primary=True)
    label_bottom(img, 'Erster Start: keine Liste, kurze Anleitung')
    return img


def chat_proposal():
    img = backdrop().resize((W * S, H * S), Image.NEAREST)
    import math
    cx, cy, rx, ry = W * .55, H * .64, 110, 26
    n = 44
    for i in range(n):
        a = 2 * math.pi * i / n
        px_, py_ = cx + rx * math.cos(a), cy + ry * math.sin(a)
        size = 2 if math.sin(a) > 0 else 1
        R(img, int(px_) - size, int(py_) - size, int(px_) + size, int(py_) + size, (255, 255, 240, 230))
        R(img, int(px_) - size - 1, int(py_), int(px_) + size + 1, int(py_) + 1, (255, 255, 220, 120))
    R(img, int(cx) - 1, int(cy) - 1, int(cx) + 2, int(cy) + 2, (255, 240, 160, 255))
    lines = [[('[Resources] ', (170, 170, 170)), ('Neues Vorkommen: ', (255, 255, 255)), ('Eisenmine Nordstollen', (255, 255, 85)),
              (' (Eisen, Mine), Radius 14', (170, 170, 170))],
             [('[Speichern]', (85, 255, 85)), (' ', None), ('[Verwerfen]', (255, 85, 85)), (' ', None), ('[Bearbeiten]', (255, 170, 0)),
              ('   Radius ', (170, 170, 170)), ('[-]', (85, 255, 255)), (' 14 ', (255, 255, 255)), ('[+]', (85, 255, 255))],
             [('Verfällt in 2 min. Der Ring zeigt die Grenze.', (120, 120, 120))]]
    by0 = H - 16 - len(lines) * 10
    R(img, 2, by0 - 2, 392, H - 14, (0, 0, 0, 128))
    for li, parts in enumerate(lines):
        tx = 4
        for s, col in parts:
            if col:
                text(img, tx * S, (by0 + li * 10) * S, s, col + (255,), S, False, True)
            tx += tw(s)
    label_bottom(img, '/ottoextra resources create: Ring in der Welt, Auswahl im Chat')
    return img


editor_book().save(os.path.join(OUT, 'buch_editor.png'))
share_book().save(os.path.join(OUT, 'buch_teilen.png'))
list_settings_book().save(os.path.join(OUT, 'buch_liste_bearbeiten.png'))
empty_book().save(os.path.join(OUT, 'buch_leer.png'))
chat_proposal().save(os.path.join(OUT, 'chat_vorschlag.png'))
print('ok')
