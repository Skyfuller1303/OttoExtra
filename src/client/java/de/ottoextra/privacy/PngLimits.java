package de.ottoextra.privacy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Check IHDR before native decoding allocates pixel storage, including existing disk caches. */
public final class PngLimits {
    private static final byte[] SIGNATURE = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
    private PngLimits() { }

    public static void check(byte[] bytes, int maxBytes, int maxDimension) throws IOException {
        if (bytes == null || bytes.length < 33 || bytes.length > maxBytes
                || !Arrays.equals(SIGNATURE, Arrays.copyOf(bytes, 8))) throw new IOException("Invalid PNG size/signature");
        ByteBuffer header = ByteBuffer.wrap(bytes);
        if (header.getInt(8) != 13 || header.getInt(12) != 0x49484452) throw new IOException("Missing PNG IHDR");
        int width = header.getInt(16), height = header.getInt(20);
        if (width < 1 || height < 1 || width > maxDimension || height > maxDimension)
            throw new IOException("PNG dimensions exceed " + maxDimension);
    }

    public static byte[] read(Path file, int maxBytes, int maxDimension) throws IOException {
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(maxBytes + 1);
            check(bytes, maxBytes, maxDimension);
            return bytes;
        }
    }
}
