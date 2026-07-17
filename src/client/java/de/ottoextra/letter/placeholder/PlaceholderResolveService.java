package de.ottoextra.letter.placeholder;
import de.ottoextra.letter.model.LetterPlaceholder;
import de.ottoextra.letter.model.PlaceholderResolveResult;
public final class PlaceholderResolveService {
    private final RpIdentityResolver resolver;
    public PlaceholderResolveService(RpIdentityResolver resolver) {
        this.resolver = resolver;
    }
    public PlaceholderResolveResult resolve(LetterPlaceholder p) {
        String player = p.playerName();
        String value = switch (p.type()) {
            case "name" -> resolver.rpName(player).orElse(null);
            case "title" -> resolver.title(player).orElse(null);
            case "full" -> {
                String name = resolver.rpName(player).orElse(null);
                if (name == null) {
                    yield null;
                }
                String title = resolver.title(player).orElse(null);
                yield title != null && !title.isBlank() ? title + " " + name : name;
            }
            case "mc" -> resolver.accountKnown(player) ? player : null;
            default -> null;
        };
        return new PlaceholderResolveResult(p, value, value != null ? "resolver" : "none");
    }
    public String apply(String text, PlaceholderResolveResult result) {
        if (!result.ok()) {
            return text;
        }
        LetterPlaceholder p = result.placeholder();
        return text.substring(0, p.start()) + result.resolved() + text.substring(p.end());
    }
}
