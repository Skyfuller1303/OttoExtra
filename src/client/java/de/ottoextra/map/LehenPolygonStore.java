package de.ottoextra.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.OttoExtra;
import de.ottoextra.logging.DebugLog;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LehenPolygonStore {

    private static final String RESOURCE = "/assets/ottoextra/map/lehen_polygons.json";

    private static final AtomicBoolean loading = new AtomicBoolean(false);
    private static volatile List<LehenPolygon> polygons = List.of();
    private static volatile List<BorderSegment> segments = List.of();
    private static volatile java.util.Map<String, Integer> groupColors = java.util.Map.of();
    private static volatile boolean loaded = false;

    public static java.util.Map<String, Integer> groupColors() {
        return groupColors;
    }

    private LehenPolygonStore() {
    }

    public static List<LehenPolygon> polygons() {
        return polygons;
    }

    public static List<BorderSegment> segments() {
        return segments;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void ensureLoaded() {
        if (loaded || !loading.compareAndSet(false, true)) {
            return;
        }
        Thread t = new Thread(LehenPolygonStore::loadNow, "ottoextra-map-polygons");
        t.setDaemon(true);
        t.start();
    }

    private static void loadNow() {
        try (InputStream in = LehenPolygonStore.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                OttoExtra.LOGGER.warn("[map] lehen_polygons.json fehlt — keine Grenzen verfuegbar.");
                loaded = true;
                return;
            }
            JsonObject root = JsonParser
                    .parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject polys = root.getAsJsonObject("polygons");

            JsonObject labels = root.has("labels") && root.get("labels").isJsonObject()
                    ? root.getAsJsonObject("labels") : null;
            List<LehenPolygon> result = new ArrayList<>();
            for (String key : polys.keySet()) {
                LehenPolygon p = parsePolygon(key, polys.get(key),
                        labels != null ? labels.get(key) : null);
                if (p != null) {
                    result.add(p);
                }
            }
            result = snapSharedBorders(result);
            polygons = List.copyOf(result);
            segments = buildSegments(result);

            if (root.has("group_colors") && root.get("group_colors").isJsonObject()) {
                java.util.Map<String, Integer> colors = new java.util.HashMap<>();
                JsonObject gc = root.getAsJsonObject("group_colors");
                for (String name : gc.keySet()) {
                    try {
                        String hex = gc.get(name).getAsString().replace("#", "").trim();
                        colors.put(de.ottoextra.regions.RegionNameKeys.normalize(name),
                                Integer.parseInt(hex, 16) & 0xFFFFFF);
                    } catch (Exception e) {
                        OttoExtra.LOGGER.warn("[map] group_colors '{}' unlesbar: {}", name, e.toString());
                    }
                }
                groupColors = java.util.Map.copyOf(colors);
            }
            loaded = true;
            DebugLog.debug("[map] {} Lehen-Polygone geladen ({} eindeutige Grenzsegmente).",
                    result.size(), segments.size());
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[map] Polygon-Laden fehlgeschlagen: {}", e.toString());
            loaded = true;
        }
    }

    private static final double SNAP_VERTEX_TOL = 16.0;
    private static final double SNAP_EDGE_TOL = 12.0;

    private static List<LehenPolygon> snapSharedBorders(List<LehenPolygon> polys) {

        List<List<double[]>> pts = new ArrayList<>();
        for (LehenPolygon p : polys) {
            List<double[]> l = new ArrayList<>(p.pointCount());
            for (int i = 0; i < p.pointCount(); i++) {
                l.add(new double[]{p.xs()[i], p.zs()[i]});
            }
            pts.add(l);
        }

        List<double[]> reps = new ArrayList<>();
        for (List<double[]> poly : pts) {
            for (double[] q : poly) {
                double[] rep = null;
                for (double[] r : reps) {
                    double dx = r[0] - q[0];
                    double dz = r[1] - q[1];
                    if (dx * dx + dz * dz <= SNAP_VERTEX_TOL * SNAP_VERTEX_TOL) {
                        rep = r;
                        break;
                    }
                }
                if (rep == null) {
                    reps.add(new double[]{q[0], q[1]});
                } else {
                    q[0] = rep[0];
                    q[1] = rep[1];
                }
            }
        }

        for (int bi = 0; bi < pts.size(); bi++) {
            List<double[]> b = pts.get(bi);
            for (int i = 0; i < b.size(); i++) {
                double[] a = b.get(i);
                double[] c = b.get((i + 1) % b.size());
                double ex = c[0] - a[0];
                double ez = c[1] - a[1];
                double len2 = ex * ex + ez * ez;
                if (len2 < 1) {
                    continue;
                }
                List<double[]> inserts = new ArrayList<>();
                for (double[] p : reps) {
                    if ((p[0] == a[0] && p[1] == a[1]) || (p[0] == c[0] && p[1] == c[1])) {
                        continue;
                    }
                    double t = ((p[0] - a[0]) * ex + (p[1] - a[1]) * ez) / len2;
                    if (t <= 0.001 || t >= 0.999) {
                        continue;
                    }
                    double px = a[0] + t * ex;
                    double pz = a[1] + t * ez;
                    double dx = p[0] - px;
                    double dz = p[1] - pz;
                    if (dx * dx + dz * dz <= SNAP_EDGE_TOL * SNAP_EDGE_TOL) {
                        inserts.add(new double[]{t, p[0], p[1]});
                    }
                }
                if (!inserts.isEmpty()) {
                    inserts.sort(java.util.Comparator.comparingDouble(q -> q[0]));
                    for (int k = 0; k < inserts.size(); k++) {
                        b.add(i + 1 + k, new double[]{inserts.get(k)[1], inserts.get(k)[2]});
                    }
                    i += inserts.size();
                }
            }
        }

        List<LehenPolygon> out = new ArrayList<>(polys.size());
        for (int pi = 0; pi < polys.size(); pi++) {
            LehenPolygon orig = polys.get(pi);
            List<double[]> poly = pts.get(pi);
            List<double[]> clean = new ArrayList<>(poly.size());
            for (double[] q : poly) {
                if (clean.isEmpty()) {
                    clean.add(q);
                    continue;
                }
                double[] last = clean.get(clean.size() - 1);
                if (Math.round(last[0]) != Math.round(q[0]) || Math.round(last[1]) != Math.round(q[1])) {
                    clean.add(q);
                }
            }
            while (clean.size() > 1) {
                double[] first = clean.get(0);
                double[] last = clean.get(clean.size() - 1);
                if (Math.round(first[0]) == Math.round(last[0])
                        && Math.round(first[1]) == Math.round(last[1])) {
                    clean.remove(clean.size() - 1);
                } else {
                    break;
                }
            }
            if (clean.size() < 3 || isSelfIntersecting(clean)) {
                out.add(orig);
                continue;
            }
            double[] xs = new double[clean.size()];
            double[] zs = new double[clean.size()];
            double minX = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (int i = 0; i < clean.size(); i++) {
                xs[i] = clean.get(i)[0];
                zs[i] = clean.get(i)[1];
                minX = Math.min(minX, xs[i]);
                minZ = Math.min(minZ, zs[i]);
                maxX = Math.max(maxX, xs[i]);
                maxZ = Math.max(maxZ, zs[i]);
            }
            out.add(new LehenPolygon(orig.key(), xs, zs,
                    orig.centroidX(), orig.centroidZ(), minX, minZ, maxX, maxZ, orig.labelOwner()));
        }
        return out;
    }

    private static boolean isSelfIntersecting(List<double[]> pts) {
        int n = pts.size();

        for (int i = 0; i < n; i++) {
            for (int j = i + 2; j < n; j++) {
                if (i == 0 && j == n - 1) {
                    continue;
                }
                if (pts.get(i)[0] == pts.get(j)[0] && pts.get(i)[1] == pts.get(j)[1]) {
                    return true;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            double[] a = pts.get(i);
            double[] b = pts.get((i + 1) % n);
            for (int j = i + 1; j < n; j++) {
                if (j == i || (j + 1) % n == i || (i + 1) % n == j) {
                    continue;
                }
                double[] c = pts.get(j);
                double[] d = pts.get((j + 1) % n);
                if (segmentsCross(a, b, c, d)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean segmentsCross(double[] a, double[] b, double[] c, double[] d) {
        return ccw(a, c, d) != ccw(b, c, d) && ccw(a, b, c) != ccw(a, b, d);
    }

    private static boolean ccw(double[] p, double[] q, double[] r) {
        return (r[1] - p[1]) * (q[0] - p[0]) > (q[1] - p[1]) * (r[0] - p[0]);
    }

    private static List<BorderSegment> buildSegments(List<LehenPolygon> polys) {
        record Key(long ax, long az, long bx, long bz) {
        }
        java.util.LinkedHashMap<Key, java.util.ArrayList<String>> owners = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<Key, double[]> coords = new java.util.LinkedHashMap<>();
        for (LehenPolygon poly : polys) {
            int n = poly.pointCount();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                long x1 = Math.round(poly.xs()[i]);
                long z1 = Math.round(poly.zs()[i]);
                long x2 = Math.round(poly.xs()[j]);
                long z2 = Math.round(poly.zs()[j]);
                if (x1 == x2 && z1 == z2) {
                    continue;
                }

                Key key = (x1 < x2 || (x1 == x2 && z1 <= z2))
                        ? new Key(x1, z1, x2, z2)
                        : new Key(x2, z2, x1, z1);
                owners.computeIfAbsent(key, k -> new java.util.ArrayList<>(2)).add(poly.key());
                coords.putIfAbsent(key, new double[]{
                        poly.xs()[i], poly.zs()[i], poly.xs()[j], poly.zs()[j]});
            }
        }
        List<BorderSegment> out = new ArrayList<>(owners.size());
        for (var e : owners.entrySet()) {
            double[] c = coords.get(e.getKey());
            out.add(new BorderSegment(c[0], c[1], c[2], c[3], List.copyOf(e.getValue())));
        }
        return List.copyOf(out);
    }

    private static LehenPolygon parsePolygon(String rawKey, JsonElement el, JsonElement labelEl) {
        try {

            int hash = rawKey.indexOf('#');
            String key = hash >= 0 ? rawKey.substring(0, hash) : rawKey;
            boolean labelOwner = hash < 0;
            JsonArray pts = el.getAsJsonArray();
            if (pts.size() < 3) {
                return null;
            }
            double[] xs = new double[pts.size()];
            double[] zs = new double[pts.size()];
            double sumX = 0;
            double sumZ = 0;
            double minX = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (int i = 0; i < pts.size(); i++) {
                JsonArray pt = pts.get(i).getAsJsonArray();
                double x = pt.get(0).getAsDouble();
                double z = pt.get(1).getAsDouble();
                if (Double.isNaN(x) || Double.isNaN(z)) {
                    return null;
                }
                xs[i] = x;
                zs[i] = z;
                sumX += x;
                sumZ += z;
                minX = Math.min(minX, x);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxZ = Math.max(maxZ, z);
            }

            double area2 = 0;
            double cxA = 0;
            double czA = 0;
            int n = pts.size();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                double cross = xs[i] * zs[j] - xs[j] * zs[i];
                area2 += cross;
                cxA += (xs[i] + xs[j]) * cross;
                czA += (zs[i] + zs[j]) * cross;
            }
            double anchorX;
            double anchorZ;
            if (Math.abs(area2) > 1e-3) {
                anchorX = cxA / (3 * area2);
                anchorZ = czA / (3 * area2);
            } else {
                anchorX = sumX / n;
                anchorZ = sumZ / n;
            }

            if (labelEl != null && labelEl.isJsonArray() && labelEl.getAsJsonArray().size() >= 2) {
                JsonArray a = labelEl.getAsJsonArray();
                double lx = a.get(0).getAsDouble();
                double lz = a.get(1).getAsDouble();
                if (!Double.isNaN(lx) && !Double.isNaN(lz)) {
                    anchorX = lx;
                    anchorZ = lz;
                }
            }
            return new LehenPolygon(key, xs, zs, anchorX, anchorZ, minX, minZ, maxX, maxZ, labelOwner);
        } catch (Exception e) {
            DebugLog.debug("[map] Polygon '{}' uebersprungen: {}", rawKey, e.toString());
            return null;
        }
    }
}
