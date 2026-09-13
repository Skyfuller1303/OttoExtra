package de.ottoextra.privacy;

import de.ottoextra.config.OttoExtraPaths;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/** Shared process lock: offline export/deletion takes an exclusive lock on the same byte. */
public final class LocalDataSession {
    private static FileChannel channel;
    private static FileLock lock;
    private LocalDataSession() { }
    public static synchronized void initialize() {
        if (lock != null) return;
        try {
            Files.createDirectories(OttoExtraPaths.root());
            channel = FileChannel.open(OttoExtraPaths.root().resolve(".privacy-data.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            if (channel.size() == 0) channel.write(java.nio.ByteBuffer.wrap(new byte[]{0}));
            lock = channel.lock(0, 1, true);
            // Keep it until process exit, including late shutdown flushes from other modules.
        } catch (IOException error) {
            throw new IllegalStateException("Local data session could not be locked", error);
        }
    }
}
