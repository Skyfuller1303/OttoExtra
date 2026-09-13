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
            if (!d.realm().isBlank()) {
                String owner = owner(d.realm(), d.realmRank(), translate);
                lead.add(d.directLord().isBlank() ? text(translate, "realm.full", owner)
                        : text(translate, "realm.vassal", owner, d.directLord()));
            } else if (!d.directLord().isBlank()) {
                lead.add(text(translate, "vassal", d.directLord()));
            } else if (d.independent()) {
                lead.add(text(translate, "independent"));
            }
        }
        if (!lead.isEmpty()) paragraphs.add(String.join(" ", lead));
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
