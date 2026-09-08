import de.ottoextra.letter.send.CourierNewProtocol;
import de.ottoextra.letter.send.LetterV2Planner;
import de.ottoextra.letter.calibration.LetterMarkupSemantics;
import de.ottoextra.letter.format.LetterFormattingCodes;
import de.ottonien.library.bukkit.MessageManager;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.meta.BookMeta;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Runs the actual input JAR. BookMeta is a list-backed proxy, not a Paper server. */
public class CourierContractCheck {
    private static final Method APPEND;
    private static final MessageManager MESSAGES;
    static {
        try {
            APPEND = Class.forName("de.ottonien.courier.letter.LetterCreation")
                    .getDeclaredMethod("appendMessageToBook", BookMeta.class, String.class);
            APPEND.setAccessible(true);
            var config = new YamlConfiguration();
            config.set("Messages.Prefix", "&7[&6Courier&7]");
            MESSAGES = new MessageManager(config);
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    @SuppressWarnings("unchecked")
    private static List<String> actual(List<String> initial, String payload) throws Exception {
        var pages = new ArrayList<>(initial);
        String[] args = StringUtils.split(payload, ' ');
        if (args == null || args.length == 0) return pages;
        var joined = new StringBuilder();
        for (String arg : args) joined.append(arg).append(' ');
        BookMeta meta = (BookMeta) Proxy.newProxyInstance(BookMeta.class.getClassLoader(),
                new Class<?>[]{BookMeta.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getPages")) return new ArrayList<>(pages);
                    if (method.getName().equals("setPages") && arguments[0] instanceof List<?>) {
                        pages.clear();
                        pages.addAll((List<String>) arguments[0]);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
        APPEND.invoke(null, meta, MESSAGES.replacePlaceholdersToParameters(joined.toString()));
        return pages;
    }

    private static void equal(Object expected, Object observed, String label) {
        if (!expected.equals(observed)) throw new AssertionError(label + "\nexpected=" + expected + "\nactual=" + observed);
    }

    public static void main(String[] args) throws Exception {
        List<List<String>> initialStates = List.of(List.of(), List.of("Alt"),
                List.of("a".repeat(250)), List.of("erste", "x\n".repeat(11) + "x"));
        List<String> payloads = new ArrayList<>(List.of("", " ", "  a   b  ",
                "&aAb\\n\\nEnd", "%prefix% test", "\t", "\n", "\ta\t", "&\u212a",
                "a".repeat(260), "a ".repeat(300), "\u2007\\nEnd", "😀 Wort"));
        Random random = new Random(2100);
        String[] tokens = {"Wort", "&6Gold", "&r", "\\n", "%prefix%", "x".repeat(256), "😀", "&X", "  "};
        for (int i = 0; i < 500; i++) {
            var payload = new StringBuilder();
            for (int j = 0, n = 1 + random.nextInt(25); j < n; j++) {
                payload.append(tokens[random.nextInt(tokens.length)]).append(' ');
            }
            payloads.add(payload.toString());
        }
        int comparisons = 0;
        for (var initial : initialStates) for (String payload : payloads) {
            equal(actual(initial, payload), CourierNewProtocol.apply(initial, payload), "Protocol #" + comparisons++);
        }

        int plans = 0;
        for (int i = 0; i < 100; i++) {
            var document = new StringBuilder("§6Anrede\n\n");
            for (int j = 0, n = 10 + random.nextInt(150); j < n; j++) {
                document.append("Wort").append(j).append(j % 5 == 0 ? '\n' : ' ');
            }
            document.append("Ende");
            var plan = LetterV2Planner.planDocument("letter", List.of(document.toString()),
                    text -> LetterFormattingCodes.visibleLength(text) * 6);
            if (!plan.sendable()) throw new AssertionError("Unsendable corpus: " + plan.warnings());
            List<String> pages = List.of();
            for (String command : plan.commands()) {
                if (command.length() > 256) throw new AssertionError("Oversized command");
                pages = actual(pages, command.substring("letter ".length()));
            }
            equal(plan.predictedPages(), LetterV2Planner.displayPages(pages), "Plan #" + i);
            equal(LetterMarkupSemantics.plain(document.toString()).replaceAll("[\\s\u2007]+", ""),
                    LetterMarkupSemantics.plain(String.join("", pages)).replaceAll("[\\s\u2007]+", ""), "Content #" + i);
            for (String page : pages) for (var run : LetterMarkupSemantics.runs(page)) {
                if (!run.text().isBlank()) equal('6', run.color(), "Style #" + i);
            }
            plans++;
        }
        System.out.println("PASS: " + comparisons + " protocol comparisons + " + plans
                + " complete plans against courier-2.1.0.jar (seed 2100).");
        System.out.println("Scope: actual Courier/MessageManager bytecode; BookMeta proxy. No network, permissions, inventory, economy or delivery test.");
    }
}
