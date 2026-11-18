"""Idea sketches for the animal register (Tierregister / Tieruebersicht) in the book style of OttoExtra
Resources: slate-blue cover, parchment pages, no text shadows. Reuses the helpers of gen_vorkommen_buch.py.
Usage: python -I gen_tierbuch.py <animal_icon_dir> <animalinfo_icon_dir> <out_dir>"""
import os, sys
_ARGS = sys.argv[1:4]
_here = os.path.dirname(os.path.abspath(__file__))
_src = open(os.path.join(_here, 'gen_vorkommen_buch.py'), encoding='utf-8').read().splitlines()
_src = [l for l in _src if '.save(os.path.join(OUT' not in l and l.strip() != "print('ok')"]
sys.argv = [sys.argv[0], _ARGS[2]]
__file__ = os.path.join(_here, 'gen_vorkommen_buch.py')
exec('\n'.join(_src))
from PIL import Image

ANIMALS, AICONS, OUT = _ARGS
FEMALE, MALE = hx('#B0466E'), hx('#3F6E9E')

# (name, species, icon, gender f/m, farm, status, minutes, percent)
HERD = [('Kastanie', 'Reitpferd', 'horse_ee203.png', 'f', 'Eichengrund', 'zuchtbereit', 0, 100),
        ('Donar', 'Kaltblut', 'horse_ee203.png', 'm', 'Eichengrund', 'Zucht wartet', 14, 75),
        ('Berta', 'Kuh', 'cow_ee201.png', 'f', 'Eichengrund', 'melkbar', 0, 100),
        ('Wolle', 'Schaf', 'sheep_ee200.png', 'f', 'Mühlbach', 'Mast 40/100', 0, 40),
        ('Ferkel 3', 'Schwein', 'pig_ee202.png', 'm', 'Mühlbach', 'jung, 1:20 h', 80, 30),
        ('Henne', 'Huhn', 'chicken_ee206.png', 'f', 'Mühlbach', 'zuchtbereit', 0, 100)]
FARMS = [('Alle Höfe', 6, 3, None), ('Eichengrund', 3, 2, ('shield', hx('#4E7A3A'))),
         ('Mühlbach', 3, 1, ('tower', hx('#9E4B32')))]


def png(img, path, x, y, size):
    ic = Image.open(path).convert('RGBA').resize((size * S, size * S), Image.NEAREST)
    img.alpha_composite(ic, (x * S, y * S))


def animal(img, name, x, y, size):
    png(img, os.path.join(ANIMALS, name), x, y, size)


def aicon(img, name, x, y):
    png(img, os.path.join(AICONS, name + '.png'), x, y, 9)


def scol(m, pct):
    return OK_INK if m <= 0 and pct >= 100 else SOON_INK if 0 < m <= 15 else WARN_INK


def farm_page(img, page, active=0):
    x, y, w, h = page
    T(img, x + 8, y + 7, 'Höfe', TERRA); rule(img, x + 8, x + w - 10, y + 18)
    cy = y + 23
    for i, (name, n, ready, ic) in enumerate(FARMS):
        if i == active:
            R(img, x + 5, cy - 2, x + w - 8, cy + 21, PA_L); R(img, x + 5, cy - 2, x + 7, cy + 21, TERRA)
        if ic:
            list_icon(img, x + 11, cy + 1, *ic)
        else:
            animal(img, 'cow_ee201.png', x + 11, cy + 1, 8)
        T(img, x + 24, cy, name, INK)
        Ts(img, x + 24, cy + 11, f'{n} Tiere', INK_M)
        r = f'{ready} bereit'; Ts(img, x + w - 12 - round(tw(r) * .75), cy + 2, r, OK_INK if ready else INK_F)
        cy += 26
    Ts(img, x + 11, cy + 2, '+ Neuer Hof', BRASS)
    rule(img, x + 8, x + w - 10, y + h - 37)
    Ts(img, x + 8, y + h - 33, 'Suchen', INK_M)
    ink_field(img, x + 8, y + h - 27, w - 18, '', 'Name, Hof oder UUID')
    return page


def overview_book():
    img, (left, right) = spread()
    farm_page(img, left, 0)
    x, y, w, h = right
    T(img, x + 10, y + 7, 'Alle Höfe', TERRA); Ts(img, x + 12 + tw('Alle Höfe'), y + 9, '6 Tiere, API aktuell', INK_M)
    slate(img, x + w - 8 - 50, y + 4, 50, '+ Tier', primary=True)
    slate(img, x + w - 8 - 50 - 3 - 62, y + 4, 62, 'Abgleichen')
    rule(img, x + 10, x + w - 8, y + 20)
    cx = x + 10
    Ts(img, cx, y + 25, 'Sortieren', INK_F); cx += 36
    for i, lab in enumerate(('Hof', 'Name', 'Zustand')):
        cw = tw(lab) * 3 // 4 + 8
        chip(img, cx, y + 23, cw, '', i == 2, h=11)
        Ts(img, cx + 4, y + 25, lab, INK if i == 2 else INK_M); cx += cw + 3
    cy = y + 39
    for i, (name, sp, ic, g, farm, st, m, pct) in enumerate(HERD[:5]):
        if i == 1: R(img, x + 6, cy - 2, x + w - 6, cy + 23, PA_L)
        R(img, x + 9, cy + 1, x + 27, cy + 19, PA_D); animal(img, ic, x + 10, cy + 2, 16)
        T(img, x + 32, cy, name)
        aicon(img, 'weiblich' if g == 'f' else 'maennlich', x + 34 + tw(name), cy)
        Ts(img, x + 32, cy + 11, f'{sp}, Hof {farm}', INK_M)
        col = scol(m, pct)
        T(img, x + w - 10 - tw(st), cy, st, col)
        bx0, bx1 = x + w - 62, x + w - 10
        R(img, bx0, cy + 13, bx1, cy + 16, PA_D); R(img, bx0, cy + 13, bx0 + round((bx1 - bx0) * pct / 100), cy + 16, col)
        rule(img, x + 10, x + w - 8, cy + 23, PA_S)
        cy += 26
    Ts(img, x + 10, y + h - 12, '+ 1 weiteres, scrollen. Doppelklick öffnet den Eintrag.', INK_F)
    label_bottom(img, 'Tierübersicht: Höfe links, Tiere rechts wie die Vorkommen')
    return img


def entry_book():
    img, (left, right) = spread()
    x, y, w, h = left
    T(img, x + 10, y + 7, 'Tierregister', TERRA); Ts(img, x + 12 + tw('Tierregister'), y + 9, 'Nutztiere', INK_M)
    rule(img, x + 10, x + w - 10, y + 20)
    R(img, x + 22, y + 28, x + w - 22, y + 120, PA_D); R(img, x + 23, y + 29, x + w - 23, y + 119, PA_S)
    animal(img, 'horse_ee203.png', x + (w - 64) // 2, y + 42, 64)
    Ts(img, x + 26, y + 112, 'Modell des Tiers', INK_F)
    T(img, x + 10, y + 128, 'Reitpferd', INK)
    aicon(img, 'weiblich', x + 10, y + 141); T(img, x + 22, y + 141, 'Stute', FEMALE)
    Ts(img, x + 10, y + 156, 'UUID', INK_F)
    Ts(img, x + 10, y + 164, '06cb65df-d251-4a03-', INK_F)
    Ts(img, x + 10, y + 172, '8270-31fd416942ac', INK_F)
    slate(img, x + 10, y + h - 22, 64, 'Übersicht')

    x, y, w, h = right
    T(img, x + 10, y + 7, 'Eintrag', TERRA); rule(img, x + 10, x + w - 8, y + 20)
    cy = y + 26
    Ts(img, x + 10, cy, 'Name'); ink_field(img, x + 10, cy + 7, w - 20, '', 'z. B. Kastanie', True); cy += 28
    Ts(img, x + 10, cy, 'Hof'); cy += 9
    list_icon(img, x + 10, cy, 'shield', hx('#4E7A3A')); T(img, x + 23, cy + 1, 'Eichengrund')
    Ts(img, x + w - 10 - round(tw('anderer Hof') * .75), cy + 3, 'anderer Hof', BRASS); cy += 16
    rule(img, x + 10, x + w - 8, cy, PA_S); cy += 6
    Ts(img, x + 10, cy, 'Werte'); cy += 10
    stats = [('Tempo', '14,52 Blöcke/s', 'zucht'), ('Sprung', '1,46 Blöcke', 'jung'),
             ('Zucht', 'bereit', 'zucht'), ('Alter', 'erwachsen', 'jung')]
    colw = (w - 20) // 2
    for i, (lab, val, ic) in enumerate(stats):
        sx, sy = x + 10 + (i % 2) * colw, cy + (i // 2) * 24
        Ts(img, sx, sy, lab, INK_F)
        T(img, sx, sy + 8, val, OK_INK if val == 'bereit' else INK)
    cy += 50
    Ts(img, x + 10, cy, 'Mast'); cy += 8
    R(img, x + 10, cy, x + w - 10, cy + 4, PA_D); cy += 9
    Ts(img, x + 10, cy, '0 von 100, Füttern erhöht die Mast.', INK_F); cy += 14
    Ts(img, x + 10, cy, 'Notiz'); ink_field(img, x + 10, cy + 7, w - 20, '', 'optional, z. B. Abstammung')
    slate(img, x + w - 8 - 60, y + h - 22, 60, 'Speichern', primary=True)
    slate(img, x + w - 8 - 60 - 3 - 60, y + h - 22, 60, 'Abbrechen')
    label_bottom(img, 'Tierregister: links das Tier, rechts Name, Hof und Werte')
    return img


def farm_pick_book():
    img, (left, right) = spread()
    farm_page(img, left, 1)
    x, y, w, h = right
    T(img, x + 10, y + 7, 'Hof wählen', TERRA); Ts(img, x + 12 + tw('Hof wählen'), y + 9, 'für Kastanie', INK_M)
    rule(img, x + 10, x + w - 8, y + 20)
    cy = y + 26
    for i, (name, n, ready, ic) in enumerate(FARMS[1:] + [('Kein Hof', 0, 0, None)]):
        if i == 0:
            R(img, x + 6, cy - 2, x + w - 6, cy + 17, PA_L); R(img, x + 6, cy - 2, x + 8, cy + 17, TERRA)
        if ic: list_icon(img, x + 12, cy + 2, *ic)
        T(img, x + 26, cy + 1, name, INK if ic else INK_M)
        if ic: Ts(img, x + w - 12 - round(tw(f'{n} Tiere') * .75), cy + 3, f'{n} Tiere', INK_M)
        rule(img, x + 10, x + w - 8, cy + 18, PA_S)
        cy += 21
    cy += 6
    Ts(img, x + 10, cy, 'Neuer Hof'); ink_field(img, x + 10, cy + 7, w - 80, '', 'Name des Hofs')
    slate(img, x + w - 8 - 60, cy + 5, 60, 'Anlegen')
    slate(img, x + w - 8 - 60, y + h - 22, 60, 'Fertig', primary=True)
    label_bottom(img, 'Hof wählen: statt Knopf eine Liste auf der rechten Seite')
    return img


def empty_book():
    img, (left, right) = spread()
    x, y, w, h = left
    T(img, x + 8, y + 7, 'Höfe', TERRA); rule(img, x + 8, x + w - 10, y + 18)
    Ts(img, x + 11, y + 25, 'Noch kein Hof.', INK_F); Ts(img, x + 11, y + 37, '+ Neuer Hof', BRASS)
    x, y, w, h = right
    T(img, x + 10, y + 7, 'So geht es', TERRA); rule(img, x + 10, x + w - 8, y + 20)
    steps = [('Hof anlegen', 'Links unter "+ Neuer Hof", z. B. je Stall oder Weide.'),
             ('Tier eintragen', 'Tier ansehen und Taste drücken oder "+ Tier".'),
             ('Abgleichen', 'Holt Zucht, Mast und Alter aus der API.'),
             ('Im Blick behalten', 'Rechts siehst du, welches Tier bereit ist.')]
    cy = y + 28
    for i, (t, s) in enumerate(steps):
        R(img, x + 10, cy, x + 22, cy + 12, TERRA); T(img, x + 14, cy + 2, str(i + 1), PA_L)
        T(img, x + 28, cy, t); Ts(img, x + 28, cy + 11, s, INK_M); cy += 30
    slate(img, x + w - 8 - 70, y + h - 22, 70, '+ Neuer Hof', primary=True)
    label_bottom(img, 'Erster Start: noch kein Hof, kurze Anleitung')
    return img


overview_book().save(os.path.join(OUT, 'tier_buch_uebersicht.png'))
entry_book().save(os.path.join(OUT, 'tier_buch_eintrag.png'))
farm_pick_book().save(os.path.join(OUT, 'tier_buch_hof_waehlen.png'))
empty_book().save(os.path.join(OUT, 'tier_buch_leer.png'))
print('tier ok')
