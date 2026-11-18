"""Settings screen as a book: categories on the book page, settings on the leaf beside it.
Based on the Tactical Factions mockups (gen_tactical.py); reads the real labels and help
texts from docs/einstellungen-gui/beschriftungen.json. Content that does not fit scrolls
vertically only; long labels wrap instead of widening the leaf.

Usage: python -I gen_einstellungen_buch.py <out_dir>"""
import json, os, sys
_here = os.path.dirname(os.path.abspath(__file__))
_b = open(os.path.join(_here, 'gen_tactical.py'), encoding='utf-8').read()
exec(_b[:_b.index("# ---------------- 1: Politik")])
from PIL import Image

OUT = sys.argv[1]
DATA = json.load(open(os.path.join(_here, '..', '..', 'einstellungen-gui', 'beschriftungen.json'), encoding='utf-8'))

# Bindings, dark -> light. Blue is the Tactical Factions book, red the people book.
BINDINGS = {
    'rot': [hx('#2E0F06'), hx('#4C1A0B'), hx('#652816'), hx('#75321E'), hx('#8A4128')],
    'blau': [hx('#141A21'), hx('#222B36'), hx('#313C49'), hx('#3B4756'), hx('#4F5C6B')],
    'dunkelgruen': [hx('#0F1C13'), hx('#1A2F20'), hx('#26422D'), hx('#305238'), hx('#426B4B')],
    'gruenblau': [hx('#0E2026'), hx('#17333B'), hx('#21474F'), hx('#2A565F'), hx('#3A6E78')],
}
BINDING_NAMES = {'rot': 'Rot (Personenbuch)', 'blau': 'Blau (Politik-Buch)', 'dunkelgruen': 'Dunkelgrün',
                 'gruenblau': 'Grünblau'}
TERRA = TERRA_A
BRASS = hx('#866C36')
FIELD = hx('#141C24')
_REPL = {'„': '"', '“': '"', '”': '"', '…': '...', '–': '-', '›': '>', '→': '>', '×': 'x', 'ß': 'ss', '♀': 'w', '♂': 'm', ' ': ' '}


def clean(s):
    return ''.join(_REPL.get(c, c) for c in s)


def wrap(s, width):
    """Greedy word wrap by pixel width of the Minecraft font."""
    out, cur = [], ''
    for word in clean(s).split(' '):
        t = word if not cur else cur + ' ' + word
        if tw(t) <= width or not cur:
            cur = t
        else:
            out.append(cur); cur = word
    if cur: out.append(cur)
    return out


def para(img, x, y, s, width, col, max_lines=None):
    lines = wrap(s, width)
    if max_lines and len(lines) > max_lines:
        lines = lines[:max_lines]; lines[-1] = lines[-1][:-3] + '...'
    for i, l in enumerate(lines): text(img, x, y + i * 9, l, col)
    return y + len(lines) * 9


def structure():
    cats = []
    for key, v in DATA.items():
        if key.startswith('tweaks.vote.'): continue      # the Vote addon replaces the built-in reminder
        cat = next((c for c in cats if c['name'] == v['category']), None)
        if cat is None:
            cat = {'name': v['category'], 'desc': v['categoryDesc'], 'secs': []}; cats.append(cat)
        sec = next((s for s in cat['secs'] if s['name'] == v['section'] and s['tab'] == v['tab']), None)
        if sec is None:
            sec = {'name': v['section'], 'desc': v['sectionDesc'], 'tab': v['tab'], 'rows': []}; cat['secs'].append(sec)
        sec['rows'].append(v)
    return cats


CATS = structure()
LVL = {'Einfach': 0, 'Mehr': 1, 'Experte': 2}


def control_width(v):
    k = v['kind']
    if k == 'bool': return 0
    if k == 'action': return tw(clean(v['button'])) + 10
    if k == 'cycle': return min(96, max(tw(clean(x)) for x in v['values'].values()) + 18)
    if k == 'color': return 13
    if k in ('string', 'command'): return 70
    return 36


def draw_control(img, v, x_right, y):
    k = v['kind']; w = control_width(v); x = x_right - w
    d = v.get('default'); d = '' if d is None else str(d)
    if k == 'action':
        slate_btn(img, x, y - 2, w, 13, clean(v['button']))
    elif k == 'cycle':
        val = clean(v['values'].get(d, list(v['values'].values())[0]))
        rect(img, x, y - 2, x + w, y + 11, PA_D); rect(img, x + 1, y - 1, x + w - 1, y + 10, PA_L)
        while tw(val) > w - 16: val = val[:-2]
        text(img, x + 4, y, val, INK_T)
        for i in range(3): rect(img, x + w - 9 + i, y + 2 + i, x + w - 3 - i, y + 3 + i, PICTO)
    elif k == 'color':
        rect(img, x, y - 2, x + w, y + 11, BLACK)
        col = hx('#' + d.lstrip('#')) if len(d.lstrip('#')) == 6 else PA
        rect(img, x + 1, y - 1, x + w - 1, y + 10, col)
    else:
        rect(img, x, y - 2, x + w, y + 11, BLACK); rect(img, x + 1, y - 1, x + w - 1, y + 10, FIELD)
        val = clean(d) if d != '' else '-'
        while tw(val) > w - 6: val = val[:-1]
        text(img, x + w - 3 - tw(val), y, val, NAME)


def leaf_content(cat, width, expert, expanded, tab=None, hover=None):
    """Draws the settings of a category on a tall strip; returns (image, height, hover box)."""
    img = Image.new('RGBA', (width, 2400), PA)
    y = 0; hover_y = None
    for sec in cat['secs']:
        if tab is not None and sec['tab'] != tab: continue
        basic = [r for r in sec['rows'] if LVL[r['level']] == 0]
        extra = [r for r in sec['rows'] if LVL[r['level']] == 1 or (expert and LVL[r['level']] == 2)]
        if not basic and not extra: continue
        is_open = sec['name'] in expanded or not basic
        bold(img, 0, y, clean(sec['name']), TERRA); y += 10
        y = para(img, 0, y, sec['desc'], width - 2, INK_M) + 2
        for r in basic + (extra if is_open else []):
            cw_ = control_width(r)
            lab_x = 13 if r['kind'] == 'bool' else 2
            lines = wrap(r['label'], width - lab_x - cw_ - (8 if cw_ else 2))
            h = max(13, len(lines) * 9 + 4)
            if hover is not None and r['label'] == hover:
                rect(img, 0, y - 1, width, y + h - 2, PA_L); hover_y = y
            if r['kind'] == 'bool':
                on = str(r.get('default')).lower() == 'true'
                rect(img, 1, y, 10, y + 9, BLACK); rect(img, 2, y + 1, 9, y + 8, SL_DIS)
                if on:
                    for a, b in ((2, 4), (3, 5), (4, 6), (5, 5), (6, 4), (7, 3)): px(img, 1 + a, y + b, NAME)
            for i, l in enumerate(lines): text(img, lab_x, y + 1 + i * 9, l, INK_T)
            if LVL[r['level']] == 2: rect(img, width - 3, y + 2, width - 1, y + 6, BRASS)   # expert marker
            if cw_: draw_control(img, r, width - 6, y + 1)
            y += h
        if basic and extra:
            lab = 'Weniger Optionen' if is_open else 'Mehr Optionen (%d)' % len(extra)
            for xx in range(13, width - 6, 3): px(img, xx, y, PA_S)
            text(img, 13, y + 3, lab, BRASS)
            tx = 15 + tw(lab)
            for i in range(3):
                j = 2 - i if is_open else i
                rect(img, tx + j, y + 5 + i, tx + 6 - j, y + 6 + i, BRASS)
            y += 13
        y += 6
    return img, y, hover_y


def settings_book(binding, cat_index, expert=False, expanded=(), tab=None, scroll=None, hover=None, size=(456, 256),
                  search=None):
    global W, H, BOOK
    W, H = size; BOOK = BINDINGS[binding]
    img = backdrop()
    bx, by = 12, 30
    bw = 168 if W < 560 else 190
    bh = H - by - 26
    red_book(img, bx, by, bw, bh)
    lx, ly, lw, lh = bx + 10, by + 6, bw - 16, bh - 12
    flat_parchment(img, lx, ly, lw, lh, seed=47)
    for i in range(4): rect(img, lx + i, ly + 1, lx + i + 1, ly + lh - 1, shade(PA, .88 + i * .03))
    # top ribbons: how much is shown
    x = bx + 18
    for k, (lab, c) in enumerate((('Einfach', TERRA), ('Experte', BRASS))):
        on = (k == 1) == expert
        w = tw(lab) + 10; L = 22 if on else 17
        ribbon_band(img, x, by - L, by + 6, w, c if on else shade(c, .85), 'top', lab, on)
        rect(img, x, by + 6, x + w, by + 7, shade(c, .45)); x += w + 4
    # book page: search and categories
    searchbar(img, lx, ly, lw, search or 'Einstellung suchen...', filt=False)
    cat = CATS[cat_index]
    row_h = 14 if lh >= 200 else 13
    y0 = ly + 27
    for i, c in enumerate(CATS):
        yy = y0 + i * row_h
        n = sum(len(s['rows']) for s in c['secs'])
        if i == cat_index:
            rect(img, lx + 5, yy - 2, lx + lw - 5, yy + row_h - 3, PA_L)
            rect(img, lx + 5, yy - 2, lx + 7, yy + row_h - 3, TERRA)
            bold(img, lx + 11, yy, clean(c['name']))
        else:
            text(img, lx + 11, yy, clean(c['name']), INK_T)
        if lx + 13 + tw(clean(c['name'])) + (len(c['name']) if i == cat_index else 0) < lx + lw - 12 - tw(str(n)):
            text(img, lx + lw - 9 - tw(str(n)), yy, str(n), INK_M)
    # leaf: settings
    dx, dy = bx + bw + 8, by + 2
    dw, dh = W - dx - 10, bh - 4
    rect(img, dx + 3, dy + 3, dx + dw + 3, dy + dh + 3, (0, 0, 0, 110)); rect(img, dx - 1, dy - 1, dx + dw + 1, dy + dh + 1, PA_D)
    flat_parchment(img, dx, dy, dw, dh, seed=52)
    x, y = dx + 10, dy + 9
    bold(img, x, y, clean(cat['name']))
    tabs = [t for t in dict.fromkeys(s['tab'] for s in cat['secs']) if t]
    if tabs:
        tab = tab or tabs[0]; tx = dx + dw - 10
        for t in reversed(tabs):
            w = tw(t) + 8; tx -= w
            on = t == tab
            rect(img, tx, y - 2, tx + w, y + 10, PA_D if on else PA_S); rect(img, tx + 1, y - 1, tx + w - 1, y + 9, PA_L if on else PA)
            text(img, tx + 4, y, t, INK_T if on else INK_M); tx -= 3
    y = para(img, x, y + 11, cat['desc'], dw - 20, INK_M) + 2
    line(img, x, dx + dw - 10, y); y += 4
    help_h = 40
    view_h = dy + dh - help_h - 6 - y
    cwid = dw - 20 - 6
    strip, total, hover_y = leaf_content(cat, cwid, expert, set(expanded), tab, hover)
    if scroll is None: scroll = 0 if hover_y is None else hover_y - view_h // 3
    scroll = max(0, min(scroll, total - view_h))
    img.paste(strip.crop((0, scroll, cwid, scroll + view_h)), (x, y))
    if total > view_h:                                   # vertical scroll bar, never a horizontal one
        sx = dx + dw - 9
        rect(img, sx, y, sx + 3, y + view_h, PA_S)
        bh_ = max(12, view_h * view_h // total); by_ = y + (view_h - bh_) * scroll // max(1, total - view_h)
        rect(img, sx, by_, sx + 3, by_ + bh_, PA_D)
    # help strip at the foot of the leaf
    hy = dy + dh - help_h - 2
    line(img, x, dx + dw - 10, hy)
    hv = next((r for s in cat['secs'] for r in s['rows'] if r['label'] == hover), None)
    if hv:
        bold(img, x, hy + 4, clean(hv['label'])[:44], INK_T)
        para(img, x, hy + 14, hv['help'], dw - 20, INK_M, max_lines=3)
    else:
        text(img, x, hy + 4, 'Fahre mit der Maus über eine Einstellung.', INK_M)
        text(img, x, hy + 14, 'Hier steht dann, was sie bewirkt.', INK_M)
    # foot buttons
    fy = by + bh + 6
    slate_btn(img, bx, fy, tw('Ansicht: Buch') + 14, 16, 'Ansicht: Buch')
    w2 = tw('Speichern & Beenden') + 14
    slate_btn(img, W - 10 - w2, fy, w2, 16, 'Speichern & Beenden', hover=True)
    w1 = tw('Verwerfen') + 14
    slate_btn(img, W - 14 - w2 - w1, fy, w1, 16, 'Verwerfen')
    return img, (total, view_h)


def save(img, name, k):
    img.resize((img.width * k, img.height * k), Image.NEAREST).save(os.path.join(OUT, name + '.png'))


def bindings_sheet():
    """The same screen in the four bindings, side by side."""
    parts = []
    for b in ('dunkelgruen', 'gruenblau', 'blau', 'rot'):
        im, _ = settings_book(b, 1)
        text(im, 200, 12, "Einband: " + clean(BINDING_NAMES[b]), NAME)
        parts.append(im)
    sheet = Image.new('RGBA', (456 * 2 + 4, 256 * 2 + 4), BLACK)
    for i, p in enumerate(parts): sheet.paste(p, ((i % 2) * 460, (i // 2) * 260))
    return sheet


idx = {c['name']: i for i, c in enumerate(CATS)}
report = []
shots = (
    # name, binding, category, options, scale
    ('buch_dunkelgruen_chat', 'dunkelgruen', 'Chat', dict(hover='Lange Nachrichten automatisch aufteilen'), 3),
    ('buch_gruenblau_chat', 'gruenblau', 'Chat', dict(hover='Lange Nachrichten automatisch aufteilen'), 3),
    ('buch_dunkelgruen_namen_mehr', 'dunkelgruen', 'Namen & Personen', dict(expanded=('Namen über dem Kopf',), scroll=150, hover='Sichtbarkeit der Namensschilder'), 3),
    ('buch_dunkelgruen_karte_tabs', 'dunkelgruen', 'Karte & Minimap', dict(hover='Politische Karte'), 3),
    ('buch_dunkelgruen_lehen_experte', 'dunkelgruen', 'Lehen & Regionen', dict(expert=True, scroll=330, hover='Breite der Einblendung mindestens (Pixel)'), 3),
    ('buch_dunkelgruen_allgemein', 'dunkelgruen', 'Allgemein', dict(), 3),
    ('buch_dunkelgruen_chat_640x360', 'dunkelgruen', 'Chat', dict(size=(640, 360), hover='Kanal mit Umschalt+Tab wechseln'), 2),
    ('buch_gruenblau_briefe_640x360', 'gruenblau', 'Briefe', dict(size=(640, 360), expanded=('Schreiben & Formatieren',), hover='Signatur'), 2),
)
for name, binding, cat, opts, k in shots:
    im, (total, view) = settings_book(binding, idx[cat], **opts)
    save(im, name, k)
    report.append('%s: Inhalt %d px, sichtbar %d px%s' % (name, total, view, ', scrollt vertikal' if total > view else ''))
save(bindings_sheet(), 'einband_vergleich', 2)

# Fit check with the real font: how often does a label wrap, how long are help texts.
W, H = 456, 256
cwid_small, cwid_big = 456 - (12 + 168 + 8) - 10 - 26, 640 - (12 + 190 + 8) - 10 - 26
rows = [r for c in CATS for s in c['secs'] for r in s['rows']]
def wraps(width): return sum(1 for r in rows if len(wrap(r['label'], width - (13 if r['kind'] == 'bool' else 2) - control_width(r) - 8)) > 1)
def helps(width, n): return sum(1 for r in rows if len(wrap(r['help'], width)) > n)
report += ['', 'Einstellungen: %d' % len(rows),
           'Beschriftung zweizeilig bei 456x256: %d, bei 640x360: %d' % (wraps(cwid_small), wraps(cwid_big)),
           'Hilfetext länger als 3 Zeilen bei 456x256: %d, bei 640x360: %d' % (helps(cwid_small + 6, 3), helps(cwid_big + 6, 3))]
open(os.path.join(OUT, 'platzpruefung.txt'), 'w', encoding='utf-8').write('\n'.join(report) + '\n')
print('\n'.join(report))
