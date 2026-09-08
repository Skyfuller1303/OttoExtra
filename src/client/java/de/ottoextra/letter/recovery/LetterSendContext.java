package de.ottoextra.letter.recovery;

import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.calibration.LetterMarkupSemantics;
import de.ottoextra.letter.send.CourierNewProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The server, player and material this persisted command sequence belongs to. */
public final class LetterSendContext {
    public String serverAddress = "";
    public String playerUuid = "";
    public int selectedSlot = -1;
    public String letterCommand = "";
    public List<String> expectedPages = new ArrayList<>();
    public boolean writtenExpected;
    public boolean submitted;
    public LetterDraft draft;

    public boolean matches(String server, String player, int slot) {
        return serverAddress != null && playerUuid != null
                && !serverAddress.isBlank() && !playerUuid.isBlank()
                && normalizeServer(serverAddress).equals(normalizeServer(server))
                && playerUuid.equals(player) && selectedSlot == slot;
    }

    public boolean matchesPages(List<String> observed) {
        if (expectedPages == null || observed == null || expectedPages.size() != observed.size()) {
            return false;
        }
        for (int i = 0; i < expectedPages.size(); i++) {
            if (!LetterMarkupSemantics.runs(expectedPages.get(i))
                    .equals(LetterMarkupSemantics.runs(observed.get(i)))) {
                return false;
            }
        }
        return true;
    }

    public void sent(String command) {
        String prefix = letterCommand + " ";
        if (command.startsWith(prefix)) {
            expectedPages = new ArrayList<>(CourierNewProtocol.apply(expectedPages,
                    command.substring(prefix.length())));
            writtenExpected = true;
        } else {
            submitted = true;
        }
    }

    public static boolean validCommands(List<String> commands, int sent) {
        return commands != null && !commands.isEmpty() && sent >= 0 && sent <= commands.size()
                && commands.stream().allMatch(command -> command != null && !command.isBlank()
                && command.length() <= 256 && command.chars().noneMatch(ch -> ch < 32));
    }

    private static String normalizeServer(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
