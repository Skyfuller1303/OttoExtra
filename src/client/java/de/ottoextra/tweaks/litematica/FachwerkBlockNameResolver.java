package de.ottoextra.tweaks.litematica;

/** Namen der vom Fachwerk-Plugin belegten Notenblock-Zustände. */
public final class FachwerkBlockNameResolver {

    private static final String PREFIX = "ottoextra.litematica.fachwerk.";
    private static final String[] CYCLE_INSTRUMENTS = {
            "basedrum", "snare", "bass", "flute", "bell", "guitar",
            "chime", "xylophone", "banjo", "didgeridoo", "bit"
    };
    private static final int[] CYCLE_LAST_NOTES = {
            24, 24, 24, 24, 24, 24, 24, 24, 15, 24, 10
    };

    private FachwerkBlockNameResolver() {
    }

    public static String translationKey(String instrument, int note, boolean powered) {
        if (powered || instrument == null || note < 0 || note > 24) {
            return null;
        }

        String part = switch (instrument) {
            case "basedrum" -> note <= 3 ? "weiss_balken"
                    : note <= 19 ? "weiss_pfahl" : "weiss_strebe";
            case "snare" -> note <= 2 ? "weiss_strebe"
                    : note <= 10 ? "weiss_zierde"
                    : note <= 14 ? "backstein_balken" : "backstein_pfahl";
            case "bass" -> note <= 5 ? "backstein_pfahl"
                    : note <= 13 ? "backstein_strebe"
                    : note <= 21 ? "backstein_zierde"
                    : note >= 22 ? "grau_balken" : null;
            case "flute" -> note == 0 ? "grau_balken"
                    : note <= 16 ? "grau_pfahl" : "grau_strebe";
            case "bell" -> note <= 7 ? "grau_zierde"
                    : note <= 11 ? "natur_balken" : "natur_pfahl";
            case "guitar" -> note <= 2 ? "natur_pfahl"
                    : note <= 10 ? "natur_strebe"
                    : note <= 18 ? "natur_zierde"
                    : note <= 22 ? "gelb_balken" : "gelb_pfahl";
            case "chime" -> note <= 13 ? "gelb_pfahl"
                    : note <= 21 ? "gelb_strebe" : "gelb_zierde";
            case "xylophone" -> note <= 4 ? "gelb_zierde"
                    : note <= 8 ? "braun_balken" : "braun_pfahl";
            case "banjo" -> note <= 7 ? "braun_strebe"
                    : note <= 15 ? "braun_zierde" : null;
            case "didgeridoo" -> note <= 3 ? "rot_balken"
                    : note <= 19 ? "rot_pfahl" : "rot_strebe";
            case "bit" -> note <= 2 ? "rot_strebe"
                    : note <= 10 ? "rot_zierde" : null;
            default -> null;
        };
        return part == null ? null : PREFIX + part;
    }

    static boolean isBackwardShorter(String currentInstrument, int currentNote,
                                     String targetInstrument, int targetNote) {
        String currentPart = translationKey(currentInstrument, currentNote, false);
        String targetPart = translationKey(targetInstrument, targetNote, false);
        if (currentPart == null || !currentPart.equals(targetPart)) {
            return false;
        }

        int current = -1;
        int target = -1;
        int size = 0;
        for (int i = 0; i < CYCLE_INSTRUMENTS.length; i++) {
            for (int note = 0; note <= CYCLE_LAST_NOTES[i]; note++) {
                if (!targetPart.equals(translationKey(CYCLE_INSTRUMENTS[i], note, false))) {
                    continue;
                }
                if (CYCLE_INSTRUMENTS[i].equals(currentInstrument) && note == currentNote) {
                    current = size;
                }
                if (CYCLE_INSTRUMENTS[i].equals(targetInstrument) && note == targetNote) {
                    target = size;
                }
                size++;
            }
        }
        if (current < 0 || target < 0 || size < 2) {
            return false;
        }
        int forward = Math.floorMod(target - current, size);
        int backward = Math.floorMod(current - target, size);
        return backward < forward;
    }
}
