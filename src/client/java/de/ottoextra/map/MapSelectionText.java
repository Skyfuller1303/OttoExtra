package de.ottoextra.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

/** Localized prose, kept separate from rendering and the API's mutable data. */
final class MapSelectionText {
    private static final String PREFIX = "ottoextra.map.info.";

    private MapSelectionText() { }

    static List<String> paragraphs(MapSelectionInfo d, BiFunction<String, Object[], String> translate) {
        List<String> paragraphs = new ArrayList<>();
        List<String> lead = new ArrayList<>();
        if (!d.narrativeOnly()) {
            if (d.residents() != null) {
                String residents = text(translate, d.residents() == 1 ? "resident" : "residents", d.residents());
                String subject = text(translate, "subject." + gender(d.rank()), d.rank());
                lead.add(d.leader().isBlank() ? text(translate, "lead.short", subject, residents)
                        : text(translate, "lead.full", subject, d.leader(), residents));
            }
        }
        if (!lead.isEmpty()) paragraphs.add(String.join(" ", lead));
        if (!d.narrativeOnly()) {
            String relation = relation(d, translate);
            if (!relation.isEmpty()) paragraphs.add(relation);
        }
        if (!d.narrativeKey().isBlank()) {
            paragraphs.add(translate.apply(d.narrativeKey(), new Object[0]));
        } else if (!d.description().isBlank()) {
            paragraphs.add(d.description());
        }
        if (!d.npcVillage().isBlank()) {
            paragraphs.add(text(translate, "npc_village", d.npcVillage()));
        }
        if (paragraphs.isEmpty()) paragraphs.add(text(translate, "no_details"));
        return paragraphs;
    }

    /** One sentence naming the realm and the direct liege with their ranks, or the independence text. */
    private static String relation(MapSelectionInfo d, BiFunction<String, Object[], String> translate) {
        boolean hasRealm = !d.realm().isBlank();
        boolean hasLord = !d.directLord().isBlank();
        if (hasRealm && hasLord && !d.realm().equalsIgnoreCase(d.directLord())) {
            return text(translate, "realm.vassal", d.title(), owner(d.realm(), d.realmRank(), translate),
                    owner(d.directLord(), d.directLordRank(), translate));
        }
        if (hasLord) {
            return text(translate, "vassal", d.title(), owner(d.directLord(), d.directLordRank(), translate));
        }
        if (hasRealm) {
            return text(translate, "realm.full", d.title(), owner(d.realm(), d.realmRank(), translate));
        }
        return d.independent() ? text(translate, "independent") : "";
    }

    private static String owner(String name, String rank, BiFunction<String, Object[], String> translate) {
        String gender = gender(rank);
        if (gender.equals("feminine")) return text(translate, "owner.feminine", rank, name);
        String genitive = switch (rank.toLowerCase(Locale.ROOT)) {
            case "dorf" -> "Dorfes";
            case "orden" -> "Ordens";
            case "rittergut", "herzogtum", "fürstentum", "königreich", "kaiserreich", "kloster", "bistum", "erzbistum", "hochstift", "weiler" -> rank + "s";
            default -> "";
        };
        if (!genitive.isBlank()) return text(translate, "owner.genitive", genitive, name, rank);
        return text(translate, "owner.fallback", name);
    }

    private static String gender(String rank) {
        return switch (rank.toLowerCase(Locale.ROOT)) {
            case "dorf", "rittergut", "herzogtum", "fürstentum", "königreich", "kaiserreich", "kloster", "bistum", "erzbistum", "hochstift" -> "neuter";
            case "weiler", "orden" -> "masculine";
            case "siedlung", "stadt", "freiherrschaft", "herrschaft", "grafschaft", "markgrafschaft", "baronie", "burg", "ordensburg", "vogtei", "abtei" -> "feminine";
            default -> rank.isBlank() ? "fallback" : "custom";
        };
    }

    private static String text(BiFunction<String, Object[], String> translate, String suffix, Object... args) {
        return translate.apply(PREFIX + suffix, args);
    }
}
