package de.ottoextra.tweaks.vote;

import de.ottoextra.OttoExtra;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

public final class VoteBrowserOpener {

    private final List<URI> voteUrls;
    private final Consumer<URI> uriOpener;

    public VoteBrowserOpener(List<URI> voteUrls) {
        this(voteUrls, uri -> Util.getOperatingSystem().open(uri));
    }

    VoteBrowserOpener(List<URI> voteUrls, Consumer<URI> uriOpener) {
        this.voteUrls = List.copyOf(voteUrls);
        this.uriOpener = uriOpener;
    }

    public List<URI> urls() { return voteUrls; }

    public void openAll() {
        for (URI voteUrl : voteUrls) {
            try {
                uriOpener.accept(voteUrl);
            } catch (RuntimeException exception) {
                OttoExtra.LOGGER.error("Vote-Seite konnte nicht geöffnet werden: {}", voteUrl, exception);
            }
        }
    }
}
