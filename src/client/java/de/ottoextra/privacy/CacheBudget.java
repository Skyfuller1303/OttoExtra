package de.ottoextra.privacy;

import de.ottoextra.config.OttoExtraPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** No automatic removal of existing user data. At the quota, cache writes stop until cleanup. */
public final class CacheBudget {
    public static final long MAX_BYTES = 256L * 1024 * 1024;
    public static final int MAX_FILES = 10_000;
    private CacheBudget() { }
    @FunctionalInterface public interface Write { void run() throws IOException; }
    public static synchronized void write(Path target, long bytes, Write write) throws IOException {
        write(OttoExtraPaths.cacheDir(), target, bytes, write);
    }
    public static synchronized void write(Path directory, Path target, long bytes, Write write) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        Path normalized = target.toAbsolutePath().normalize();
        if (!normalized.startsWith(root) || bytes < 0 || bytes > MAX_BYTES) throw new IOException("Invalid cache write");
        if (Files.exists(root)) {
            Path realRoot = root.toRealPath();
            for (Path parent = normalized; parent != null && parent.startsWith(root); parent = parent.getParent()) {
                if (Files.exists(parent) && !parent.toRealPath().startsWith(realRoot))
                    throw new IOException("Cache path leaves storage directory");
            }
        }
        long total = bytes;
        int count = 0;
        if (Files.exists(root)) {
            try (var files = Files.walk(root)) {
                var iterator = files.iterator();
                while (iterator.hasNext()) {
                    Path path = iterator.next();
                    if (Files.isSymbolicLink(path)) throw new IOException("Linked cache path");
                    if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        if (++count >= MAX_FILES) throw new IOException("Cache file quota reached");
                        if (!path.equals(normalized)) total += Files.size(path);
                        if (total > MAX_BYTES) throw new IOException("Cache byte quota reached");
                    }
                }
            }
        }
        if (total > MAX_BYTES) throw new IOException("Cache byte quota reached");
        write.run();
    }
}
