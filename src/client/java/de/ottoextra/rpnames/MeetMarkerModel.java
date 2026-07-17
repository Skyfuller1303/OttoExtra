package de.ottoextra.rpnames;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.OttoExtra;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
public final class MeetMarkerModel {
    private static final float TEX = 16.0f;
    public record Quad(float[][] pos, float[][] uv, float nx, float ny, float nz) {
    }
    private static volatile List<Quad> quads;
    private MeetMarkerModel() {
    }
    public static List<Quad> quads() {
        List<Quad> q = quads;
        if (q == null) {
            synchronized (MeetMarkerModel.class) {
                q = quads;
                if (q == null) {
                    q = load();
                    quads = q;
                }
            }
        }
        return q;
    }
    private static List<Quad> load() {
        List<Quad> out = new ArrayList<>();
        try (InputStream in = MeetMarkerModel.class.getResourceAsStream(
                "/assets/ottoextra/models/entity/meet_marker.json")) {
            if (in == null) {
                OttoExtra.LOGGER.warn("[meet] Model meet_marker.json nicht gefunden");
                return out;
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray elements = root.getAsJsonArray("elements");
            if (elements == null) {
                return out;
            }
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (var el : elements) {
                float[] f = arr(el.getAsJsonObject().getAsJsonArray("from"));
                float[] t = arr(el.getAsJsonObject().getAsJsonArray("to"));
                minX = Math.min(minX, Math.min(f[0], t[0]));
                minY = Math.min(minY, Math.min(f[1], t[1]));
                minZ = Math.min(minZ, Math.min(f[2], t[2]));
                maxX = Math.max(maxX, Math.max(f[0], t[0]));
                maxY = Math.max(maxY, Math.max(f[1], t[1]));
                maxZ = Math.max(maxZ, Math.max(f[2], t[2]));
            }
            float cx = (minX + maxX) / 2f;
            float cy = (minY + maxY) / 2f;
            float cz = (minZ + maxZ) / 2f;
            for (var el : elements) {
                JsonObject e = el.getAsJsonObject();
                float[] f = arr(e.getAsJsonArray("from"));
                float[] t = arr(e.getAsJsonArray("to"));
                float x0 = (Math.min(f[0], t[0]) - cx) / TEX;
                float y0 = (Math.min(f[1], t[1]) - cy) / TEX;
                float z0 = (Math.min(f[2], t[2]) - cz) / TEX;
                float x1 = (Math.max(f[0], t[0]) - cx) / TEX;
                float y1 = (Math.max(f[1], t[1]) - cy) / TEX;
                float z1 = (Math.max(f[2], t[2]) - cz) / TEX;
                JsonObject faces = e.getAsJsonObject("faces");
                if (faces == null) {
                    continue;
                }
                addFace(out, faces, "down", new float[][]{
                        {x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}}, 0, -1, 0);
                addFace(out, faces, "up", new float[][]{
                        {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}, 0, 1, 0);
                addFace(out, faces, "north", new float[][]{
                        {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}}, 0, 0, -1);
                addFace(out, faces, "south", new float[][]{
                        {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}}, 0, 0, 1);
                addFace(out, faces, "west", new float[][]{
                        {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}}, -1, 0, 0);
                addFace(out, faces, "east", new float[][]{
                        {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}}, 1, 0, 0);
            }
        } catch (Throwable th) {
            OttoExtra.LOGGER.warn("[meet] Model-Parsing fehlgeschlagen: {}", th.toString());
        }
        return out;
    }
    private static void addFace(List<Quad> out, JsonObject faces, String name,
                                float[][] pos, float nx, float ny, float nz) {
        JsonObject face = faces.has(name) ? faces.getAsJsonObject(name) : null;
        if (face == null) {
            return;
        }
        float[] uv = arr(face.getAsJsonArray("uv"));
        float u0 = uv[0] / TEX, v0 = uv[1] / TEX, u1 = uv[2] / TEX, v1 = uv[3] / TEX;
        float[][] uvs = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
        out.add(new Quad(pos, uvs, nx, ny, nz));
    }
    private static float[] arr(JsonArray a) {
        float[] r = new float[a.size()];
        for (int i = 0; i < a.size(); i++) {
            r[i] = a.get(i).getAsFloat();
        }
        return r;
    }
}
