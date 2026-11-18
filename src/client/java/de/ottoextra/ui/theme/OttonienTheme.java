package de.ottoextra.ui.theme;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeichenhilfen für das Buch-Design des Briefeditors (rotes Lederbuch im Kordelrahmen,
 * Pergamentseite, Lesezeichen, Tintentropfen, Werkzeugkacheln). Alles besteht aus ganzzahligen
 * {@link DrawContext#fill}-Aufrufen, es werden keine Texturen benötigt.
 */
public final class OttonienTheme {

    public static final int PA = 0xFFC9AA81;
    public static final int PA_L = 0xFFD4B78C;
    public static final int PA_S = 0xFFB8926E;
    public static final int PA_D = 0xFF513E2C;
    public static final int INK = 0xFF503D29;
    public static final int INK_MUTED = 0xFF7A6248;
    public static final int PICTO = 0xFF51493A;

    private static final int[] BOOK = {0xFF2E0F06, 0xFF4C1A0B, 0xFF652816, 0xFF75321E, 0xFF8A4128};
    private static final int[] GREEN_BOOK = {0xFF0F1C13, 0xFF1A2F20, 0xFF26422D, 0xFF305238, 0xFF426B4B};
    private static final int[] GOLD ={0xFF47391D, 0xFF866C36, 0xFFB58D36, 0xFFDBB451};

    public static final int TERRACOTTA = 0xFFAA6841;
    public static final int BRASS = 0xFF866C36;
    public static final int SLATE_TAB = 0xFF434D66;

    private static final int SLATE = 0xFF222E3C;
    private static final int SLATE_EDGE = 0xFF344459;
    private static final int SLATE_SHADOW = 0xFF1A2024;
    private static final int SLATE_HOVER = 0xFF323D4A;
    public static final int GOLD_HOVER = 0xFFE2A650;
    public static final int LABEL = 0xFFF0EBE0;
    public static final int LABEL_MUTED = 0xFFD6C194;
    private static final int BLACK = 0xFF0E0E10;
    private static final int OUTLINE = 0xFF141418;
    private static final int[] WAX = {0xFF6E2226, 0xFFB24147, 0xFFC4545A};

    private OttonienTheme() {
    }

    public static int shade(int argb, double factor) {
        int r = clamp((int) (((argb >> 16) & 0xFF) * factor));
        int g = clamp((int) (((argb >> 8) & 0xFF) * factor));
        int b = clamp((int) ((argb & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static void rect(DrawContext ctx, int x0, int y0, int x1, int y1, int color) {
        if (x1 > x0 && y1 > y0) {
            ctx.fill(x0, y0, x1, y1, color);
        }
    }

    private static void px(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 1, y + 1, color);
    }

    private static boolean hash(int x, int y, int seed, int permille) {
        int h = x * 73856093 ^ y * 19349663 ^ seed * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0x3FF) * 1000 / 1024 < permille;
    }

    /** Flache Pergamentseite mit leicht eingerissenen Rändern und Eckornamenten. */
    public static void parchment(DrawContext ctx, int x, int y, int w, int h, int seed,
                                 boolean torn, boolean ornaments) {
        rect(ctx, x, y, x + w, y + h, PA);
        rect(ctx, x, y, x + w, y + 1, PA_L);
        rect(ctx, x, y, x + 1, y + h, PA_L);
        rect(ctx, x, y + h - 1, x + w, y + h, PA_S);
        rect(ctx, x + w - 1, y, x + w, y + h, PA_S);
        if (torn) {
            for (int xx = x + 2; xx < x + w - 2; xx++) {
                if (hash(xx, 1, seed, 120)) {
                    px(ctx, xx, y + h - 1, PA_D);
                }
                if (hash(xx, 2, seed, 80)) {
                    px(ctx, xx, y, PA_S);
                }
            }
            for (int yy = y + 2; yy < y + h - 2; yy++) {
                if (hash(3, yy, seed, 100)) {
                    px(ctx, x + w - 1, yy, PA_D);
                }
            }
        }
        if (ornaments) {
            int[][] corners = {{x + 5, y + 5}, {x + w - 6, y + 5}, {x + 5, y + h - 6},
                    {x + w - 6, y + h - 6}};
            int[][] ring = {{2, 0}, {1, 1}, {0, 2}, {-1, 1}, {-2, 0}, {-1, -1}, {0, -2}, {1, -1}};
            for (int[] c : corners) {
                for (int[] d : ring) {
                    px(ctx, c[0] + d[0], c[1] + d[1], PA_S);
                }
                px(ctx, c[0], c[1], PA_D);
            }
        }
    }

    /** Rotes Lederbuch mit Kordelrahmen, Rücken mit Bünden und goldenen Eckbeschlägen. */
    public static void redBook(DrawContext ctx, int bx, int by, int bw, int bh) {
        book(ctx, bx, by, bw, bh, BOOK);
    }

    /** Dunkelgrünes Lederbuch, gleiche Form wie {@link #redBook}. */
    public static void greenBook(DrawContext ctx, int bx, int by, int bw, int bh) {
        book(ctx, bx, by, bw, bh, GREEN_BOOK);
    }

    /** Einband in einer Farbrampe (dunkel nach hell, 5 Töne). */
    private static void book(DrawContext ctx, int bx, int by, int bw, int bh, int[] ramp) {
        // Kein Rahmen: nur eine 1 px dunkle Lederkante.
        rect(ctx, bx - 1, by - 1, bx + bw + 1, by + bh + 1, ramp[0]);
        rect(ctx, bx, by, bx + bw, by + bh, ramp[2]);
        rect(ctx, bx, by, bx + bw, by + 1, ramp[3]);
        rect(ctx, bx, by, bx + 1, by + bh, ramp[3]);
        rect(ctx, bx, by + bh - 1, bx + bw, by + bh, ramp[1]);
        rect(ctx, bx, by, bx + 7, by + bh, ramp[1]);
        for (int i = by + 10; i < by + bh - 8; i += 22) {
            rect(ctx, bx + 1, i, bx + 6, i + 2, ramp[3]);
            rect(ctx, bx + 1, i + 2, bx + 6, i + 3, ramp[0]);
        }
        rect(ctx, bx + 9, by + 2, bx + bw - 2, by + 3, ramp[1]);
        rect(ctx, bx + 9, by + bh - 3, bx + bw - 2, by + bh - 2, ramp[3]);
        int[][] studs = {{bx + bw - 6, by + 3}, {bx + bw - 6, by + bh - 6}};
        for (int[] s : studs) {
            rect(ctx, s[0], s[1], s[0] + 3, s[1] + 3, GOLD[1]);
            px(ctx, s[0], s[1], GOLD[3]);
            px(ctx, s[0] + 2, s[1] + 2, GOLD[0]);
        }
    }

    /** Pergamentkachel (Pack-GUI-Stil): 1 px Abschrägung, abgeschnittene Ecken. */
    public static void tile(DrawContext ctx, int x, int y, int w, int h, boolean bright) {
        rect(ctx, x + 1, y, x + w - 1, y + h, PA_D);
        rect(ctx, x, y + 1, x + w, y + h - 1, PA_D);
        rect(ctx, x + 1, y + 1, x + w - 1, y + h - 1, bright ? PA_L : PA);
        rect(ctx, x + 1, y + 1, x + w - 1, y + 2, PA_L);
        rect(ctx, x + 1, y + 1, x + 2, y + h - 1, PA_L);
        rect(ctx, x + 1, y + h - 2, x + w - 1, y + h - 1, PA_S);
        rect(ctx, x + w - 2, y + 1, x + w - 1, y + h - 1, PA_S);
    }

    /** Schieferplatte; beim Überfahren goldener Rand. Beschriftung zeichnet der Aufrufer. */
    public static void slateButton(DrawContext ctx, int x, int y, int w, int h, boolean hover) {
        int outline = hover ? GOLD_HOVER : BLACK;
        rect(ctx, x + 1, y, x + w - 1, y + h, outline);
        rect(ctx, x, y + 1, x + w, y + h - 1, outline);
        rect(ctx, x + 1, y + 1, x + w - 1, y + h - 1, hover ? SLATE_HOVER : SLATE);
        rect(ctx, x + 1, y + 1, x + w - 1, y + 2, SLATE_EDGE);
        rect(ctx, x + 1, y + h - 2, x + w - 1, y + h - 1, SLATE_SHADOW);
    }

    private static final java.util.Map<String, int[]> RIBBON_CACHE = new java.util.HashMap<>();

    private static int[] ribbonTones(int c) {
        return new int[]{shade(c, .62), shade(c, .78), shade(c, .9), c, shade(c, 1.1), shade(c, 1.2)};
    }

    private static int toneIndex(double v) {
        return Math.max(0, Math.min(5, (int) Math.round(v)));
    }

    /** Stoffband senkrecht: gesäumte Oberkante, weiche Falten (6 Töne). Höhe = y1 - y0. */
    private static int[] verticalRibbonPixels(int w, int length, int c) {
        int[] tones = ribbonTones(c);
        int[] out = new int[w * length];
        for (int row = 0; row < length; row++) {
            double t = row / (double) Math.max(1, length - 1);
            for (int i = 0; i < w; i++) {
                double u = i / (double) Math.max(1, w - 1);
                double fold = Math.sin((u * 2.2 + t * 0.9) * Math.PI) * 0.9
                        + Math.sin((u * 0.8 - t * 0.5) * Math.PI) * 0.5;
                double v = 3 + fold - u * 0.8 - t * 1.4;
                if (i == 0 || i == w - 1) {
                    v = i == w - 1 ? 0.6 : Math.min(v, 1.5);
                }
                int col = tones[toneIndex(v)];
                if (row == 0) {
                    col = shade(c, .7);
                } else if (row == 1 && i > 0 && i < w - 1) {
                    col = tones[5];
                } else if (row == 2 && i > 0 && i < w - 1) {
                    col = tones[1];
                }
                out[row * w + i] = col;
            }
        }
        return out;
    }

    /**
     * Lesezeichenband, das über der Buchoberkante hängt und 6 px über den Einband läuft; an der
     * Einbandkante liegt eine 1 px Schattenlinie. {@code y0} = obere Bandkante (gesäumt),
     * {@code y1} = untere Kante (Einband). Der Aufrufer schreibt die Beschriftung.
     */
    public static void ribbon(DrawContext ctx, int x, int y0, int y1, int w, int color) {
        int length = y1 - y0;
        if (length <= 0 || w <= 0) {
            return;
        }
        int[] pixels = RIBBON_CACHE.computeIfAbsent("v" + w + "," + length + "," + color,
                k -> verticalRibbonPixels(w, length, color));
        for (int row = 0; row < length; row++) {
            int start = 0;
            for (int i = 1; i <= w; i++) {
                if (i == w || pixels[row * w + i] != pixels[row * w + start]) {
                    ctx.fill(x + start, y0 + row, x + i, y0 + row + 1, pixels[row * w + start]);
                    start = i;
                }
            }
        }
        ctx.fill(x, y1, x + w, y1 + 1, shade(color, .45));
    }

    /** Stoffband waagerecht aus der linken Buchkante: flacher V-Schnitt am freien Ende (links). */
    private static int[] horizontalRibbonPixels(int length, int h, int c) {
        int[] tones = ribbonTones(c);
        int[] out = new int[length * h];
        double center = (h - 1) / 2.0;
        for (int col = 0; col < length; col++) {
            double t = (length - 1 - col) / (double) Math.max(1, length - 1);
            for (int j = 0; j < h; j++) {
                double u = j / (double) Math.max(1, h - 1);
                int off = (int) Math.round(3 * (1 - Math.abs(j - center) / center));
                if (col < off) {
                    continue; // 0 = nicht zeichnen
                }
                double fold = Math.sin((u * 2.0 + t * 0.9) * Math.PI) * 0.9
                        + Math.sin((u * 0.7 - t * 0.6) * Math.PI) * 0.5;
                double v = 3 + fold - u * 0.9 - (1 - t) * 1.4;
                if (j == 0) {
                    v = Math.min(v, 1.5);
                }
                if (j == h - 1) {
                    v = 0.6;
                }
                int color = tones[toneIndex(v)];
                if (col == off) {
                    color = shade(c, .7);
                } else if (col == off + 1 && j > 0 && j < h - 1) {
                    color = tones[5];
                } else if (col == off + 2 && j > 0 && j < h - 1) {
                    color = tones[1];
                }
                out[col * h + j] = color;
            }
        }
        return out;
    }

    /** Waagerechtes Band von x0 (freies Ende) bis x1 (Buchkante) auf Höhe y, Dicke h. */
    public static void ribbonHorizontal(DrawContext ctx, int x0, int x1, int y, int h, int color) {
        int length = x1 - x0;
        if (length <= 0 || h <= 0) {
            return;
        }
        int[] pixels = RIBBON_CACHE.computeIfAbsent("h" + length + "," + h + "," + color,
                k -> horizontalRibbonPixels(length, h, color));
        for (int j = 0; j < h; j++) {
            int start = -1;
            for (int col = 0; col <= length; col++) {
                int px = col < length ? pixels[col * h + j] : 0;
                if (start >= 0 && (col == length || px != pixels[start * h + j])) {
                    ctx.fill(x0 + start, y + j, x0 + col, y + j + 1, pixels[start * h + j]);
                    start = -1;
                }
                if (start < 0 && col < length && px != 0) {
                    start = col;
                }
            }
        }
        ctx.fill(x1, y, x1 + 1, y + h, shade(color, .45));
    }

    // ---- Piktogramme (9x9, linke obere Ecke x, y) ----

    public static void pictoLeft(DrawContext ctx, int x, int y, int c) {
        triangle(ctx, x + 1, y + 2, -1, c);
    }

    public static void pictoRight(DrawContext ctx, int x, int y, int c) {
        triangle(ctx, x + 2, y + 2, 1, c);
    }

    private static void triangle(DrawContext ctx, int x, int y, int dir, int c) {
        for (int i = 0; i < 3; i++) {
            int px = x + (dir > 0 ? 2 - i : i);
            rect(ctx, px, y + 2 - i, px + 1, y + 3 + i, c);
        }
    }

    public static void pictoPlus(DrawContext ctx, int x, int y, int c) {
        rect(ctx, x + 3, y + 1, x + 5, y + 8, c);
        rect(ctx, x + 1, y + 3, x + 8, y + 5, c);
    }

    public static void pictoMinus(DrawContext ctx, int x, int y, int c) {
        rect(ctx, x + 1, y + 3, x + 8, y + 5, c);
    }

    public static void pictoScissors(DrawContext ctx, int x, int y, int c) {
        int[][] pts = {{0, 0}, {1, 1}, {2, 2}, {4, 2}, {5, 1}, {6, 0}, {3, 3}, {2, 4}, {4, 4},
                {1, 5}, {5, 5}, {0, 6}, {1, 6}, {5, 6}, {6, 6}};
        for (int[] p : pts) {
            px(ctx, x + p[0], y + p[1], c);
        }
    }

    public static void pictoQuill(DrawContext ctx, int x, int y, int c) {
        for (int i = 0; i < 7; i++) {
            px(ctx, x + 1 + i, y + 7 - i, c);
        }
        for (int i = 2; i < 6; i++) {
            px(ctx, x + 2 + i, y + 7 - i, c);
            px(ctx, x + 1 + i, y + 6 - i, c);
        }
        px(ctx, x, y + 8, c);
    }

    public static void pictoSeal(DrawContext ctx, int x, int y) {
        disc(ctx, x + 4, y + 4, 4, WAX[0], WAX[1], WAX[2]);
    }

    // ---- Tintentropfen ----

    private record Run(int dx, int dy, int len, int tone) {
    }

    private static final java.util.Map<Integer, List<Run>> DISC_RUNS = new java.util.HashMap<>();

    /** Runder Tropfen mit dunkler Umrandung (r = 4 entspricht 8 px Durchmesser). */
    public static void disc(DrawContext ctx, int cx, int cy, int r, int dark, int base, int light) {
        List<Run> runs = DISC_RUNS.computeIfAbsent(r, OttonienTheme::buildDisc);
        for (Run run : runs) {
            int color = switch (run.tone()) {
                case 0 -> OUTLINE;
                case 1 -> light;
                case 3 -> dark;
                default -> base;
            };
            rect(ctx, cx + run.dx(), cy + run.dy(), cx + run.dx() + run.len(), cy + run.dy() + 1, color);
        }
    }

    private static List<Run> buildDisc(int r) {
        List<Run> runs = new ArrayList<>();
        for (int yy = -r; yy <= r; yy++) {
            int runStart = 0;
            int runTone = -1;
            for (int xx = -r; xx <= r + 1; xx++) {
                int tone = xx > r ? -1 : toneAt(xx, yy, r);
                if (tone != runTone) {
                    if (runTone >= 0) {
                        runs.add(new Run(runStart, yy, xx - runStart, runTone));
                    }
                    runStart = xx;
                    runTone = tone;
                }
            }
        }
        return runs;
    }

    private static int toneAt(int xx, int yy, int r) {
        double d = Math.hypot(xx + .5, yy + .5);
        if (d > r + .2) {
            return -1;
        }
        if (d > r - 1) {
            return 0;
        }
        if (xx + yy < -r * .6) {
            return 1;
        }
        if (xx + yy > r * .7) {
            return 3;
        }
        return 2;
    }

    /** Beim Zeichnen der Farbtropfen: heller bzw. dunkler Farbton. */
    public static void inkDrop(DrawContext ctx, int cx, int cy, int rgb) {
        int base = 0xFF000000 | rgb;
        int light = rgb == 0xFFFFFF ? base : shade(base, 1.25);
        disc(ctx, cx, cy, 4, shade(base, .55), base, light);
    }

    // ---- Seitenmarkierung ----

    /** Gestrichelte Linie von x bis x + w auf Höhe y. */
    public static void dashedLine(DrawContext ctx, int x, int y, int w, int color) {
        for (int xx = x; xx < x + w; xx += 4) {
            rect(ctx, xx, y, Math.min(xx + 2, x + w), y + 1, color);
        }
    }

    // ---- Dossierblatt (dünner Seilrand, Wachssiegel, Goldrahmen) ----

    /** Dünner geflochtener Rand (2 px) um das Rechteck (x, y, w, h). */
    public static void thinCord(DrawContext ctx, int x, int y, int w, int h) {
        int dark = 0xFF382A25;
        int light = 0xFF855C2C;
        rect(ctx, x, y, x + w, y + h, dark);
        rect(ctx, x + 1, y + 1, x + w - 1, y + h - 1, 0xFF493626);
        for (int i = 1; i < w - 1; i += 4) {
            rect(ctx, x + i, y, Math.min(x + i + 2, x + w - 1), y + 1, light);
            rect(ctx, x + i + 2, y + h - 1, Math.min(x + i + 4, x + w - 1), y + h, light);
        }
        for (int j = 1; j < h - 1; j += 4) {
            rect(ctx, x, y + j, x + 1, Math.min(y + j + 2, y + h - 1), light);
            rect(ctx, x + w - 1, y + j + 2, x + w, Math.min(y + j + 4, y + h - 1), light);
        }
    }

    /** Wachssiegel mit kleinem Federzeichen. */
    public static void waxSeal(DrawContext ctx, int cx, int cy, int r) {
        disc(ctx, cx, cy, r, WAX[0], WAX[1], WAX[2]);
        pictoQuill(ctx, cx - 4, cy - 4, 0xFFE08A8E);
    }

    /** Goldrahmen (w x h) um ein Porträt. */
    public static void goldFrame(DrawContext ctx, int x, int y, int w, int h) {
        rect(ctx, x, y, x + w, y + h, GOLD[0]);
        rect(ctx, x + 1, y + 1, x + w - 1, y + h - 1, GOLD[2]);
        rect(ctx, x + 1, y + 1, x + w - 1, y + 2, GOLD[3]);
        rect(ctx, x + 2, y + 2, x + w - 2, y + h - 2, 0xFF493626);
    }

    /** Waagerechtes Filterband links vom Buch. */
    public static void sideRibbon(DrawContext ctx, int x, int y, int w, int h, int color, boolean active) {
        int c = active ? color : shade(color, .8);
        rect(ctx, x, y, x + w, y + h, shade(color, .6));
        rect(ctx, x + 1, y + 1, x + w, y + h - 1, c);
        rect(ctx, x + 1, y + 1, x + w, y + 2, shade(color, 1.2));
    }

    public static final int GREEN = 0xFF5E9A3E;
    public static final int GREEN_DARK = 0xFF2F5426;
    public static final int GREEN_LIGHT = 0xFF86C25A;
    public static final int WAX_RED = 0xFFB24147;
    public static final int FIELD_DARK = 0xFF141C24;
    public static final int FIELD_HINT = 0xFF7F7E87;
    public static final int GOLD_TONE = 0xFFB58D36;

    /** Magnifier icon (6x6) at x, y. */
    public static void pictoSearch(DrawContext ctx, int x, int y, int c) {
        int[][] pts = {{2, 1}, {3, 1}, {1, 2}, {4, 2}, {1, 3}, {4, 3}, {2, 4}, {3, 4}, {5, 5}, {6, 6}};
        for (int[] p : pts) {
            px(ctx, x + p[0], y + p[1], c);
        }
    }

    public static void fillDisc(DrawContext ctx, int cx, int cy, int r, int dark, int base, int light) {
        disc(ctx, cx, cy, r, dark, base, light);
    }

    /** Dunkelroter Gefahren-Knopf (z. B. "Alle vergessen"). */
    public static void dangerButton(DrawContext ctx, int x, int y, int w, int h, boolean hover) {
        rect(ctx, x, y, x + w, y + h, hover ? GOLD_HOVER : BLACK);
        rect(ctx, x + 1, y + 1, x + w - 1, y + h - 1, WAX[0]);
        rect(ctx, x + 1, y + 1, x + w - 1, y + 2, WAX[1]);
        rect(ctx, x + 1, y + h - 2, x + w - 1, y + h - 1, shade(WAX[0], .7));
    }

    public static final int WAX_TEXT = 0xFFB24147;
    public static final int WAX_DARK = 0xFF6E2226;
    public static final int FRIEND_BLUE = 0xFF3F6E9E;
    public static final int FOE_RED = 0xFFB24147;
    public static final int NEUTRAL_TAN = 0xFF8A7A66;
    public static final int WARN_OCHRE = 0xFFC9A04A;
    public static final int PREVIEW_DARK = 0xFF18181C;

    /** Kleines Warndreieck (5 px hoch) mit Spitze bei (x, y). */
    public static void pictoWarning(DrawContext ctx, int x, int y, int c) {
        for (int r = 0; r < 5; r++) {
            rect(ctx, x - r / 2, y + r, x + 1 + r / 2, y + r + 1, c);
        }
    }

    /** Chip mit Rand: aktiv in Farbe, sonst Pergament. */
    public static void chip(DrawContext ctx, int x, int y, int w, int h, int color, boolean on) {
        if (on) {
            rect(ctx, x, y, x + w, y + h, shade(color, .6));
            rect(ctx, x + 1, y + 1, x + w - 1, y + h - 1, color);
        } else {
            rect(ctx, x, y, x + w, y + h, PA_S);
            rect(ctx, x + 1, y + 1, x + w - 1, y + h - 1, PA);
        }
    }

    /** Dossierblatt mit Schlagschatten und 1 px Rand #513E2C. */
    public static void leaf(DrawContext ctx, int x, int y, int w, int h, int seed, boolean seal) {
        rect(ctx, x + 3, y + 3, x + w + 3, y + h + 3, 0x6E000000);
        rect(ctx, x - 1, y - 1, x + w + 1, y + h + 1, PA_D);
        parchment(ctx, x, y, w, h, seed, true, true);
        if (seal) {
            waxSeal(ctx, x + w - 12, y + 10, 7);
        }
    }

    public static void pictoDown(DrawContext ctx, int x, int y, int c) {
        for (int i = 0; i < 3; i++) {
            rect(ctx, x + i, y + i, x + 7 - i, y + i + 1, c);
        }
    }

    public static void pictoCloud(DrawContext ctx, int x, int y, int c) {
        rect(ctx, x + 2, y + 3, x + 7, y + 7, c);
        rect(ctx, x + 1, y + 4, x + 8, y + 7, c);
        rect(ctx, x + 3, y + 2, x + 6, y + 3, c);
    }

    public static void pictoFile(DrawContext ctx, int x, int y, int c) {
        rect(ctx, x + 1, y, x + 6, y + 8, c);
        rect(ctx, x + 2, y + 1, x + 5, y + 7, PA);
        rect(ctx, x + 2, y + 3, x + 5, y + 4, c);
        rect(ctx, x + 2, y + 5, x + 5, y + 6, c);
    }

    public static void pictoChest(DrawContext ctx, int x, int y, int c) {
        rect(ctx, x, y + 2, x + 8, y + 8, c);
        rect(ctx, x + 1, y + 3, x + 7, y + 4, PA);
        rect(ctx, x + 3, y + 4, x + 5, y + 6, PA);
    }

    /** Blattsymbol (9x10) für Entwürfe/Dateien. */
    public static void pictoPage(DrawContext ctx, int x, int y, int c) {
        rect(ctx, x + 1, y, x + 8, y + 10, c);
        rect(ctx, x + 2, y + 1, x + 7, y + 9, PA_L);
        for (int r = 3; r <= 7; r += 2) {
            rect(ctx, x + 3, y + r, x + 6, y + r + 1, c);
        }
    }
}
