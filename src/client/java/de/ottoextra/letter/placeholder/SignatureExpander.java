package de.ottoextra.letter.placeholder;

import de.ottoextra.letter.format.LetterFormattingCodes;
import de.ottoextra.letter.model.LetterPlaceholder;
import de.ottoextra.letter.model.PlaceholderResolveResult;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Setzt die Absender-Platzhalter der Signatur ein. Die Signatur verwendet die kurze Form
 * {@code {{name}}}, {@code {{title}}}, {@code {{full}}} und {@code {{mc}}} ohne Spielername;
 * sie steht für den eigenen Account. Beim Versand werden Platzhalter nicht automatisch
 * ersetzt, daher wird hier sofort aufgelöst. Ist der Wert unbekannt, bleibt die reguläre Form
 * {@code {{full:Account}}} stehen, die im Editor mit Tab aufgelöst werden kann.
 */
public final class SignatureExpander {

    public static final String DEFAULT_SIGNATURE = "§oHochachtungsvoll,\n§l{{full}}";

    private static final Pattern SELF_PLACEHOLDER = Pattern.compile(
            "\\{\\{\\s*(name|title|full|mc)\\s*}}", Pattern.CASE_INSENSITIVE);

    private SignatureExpander() {
    }

    public static String expand(String template, String accountName, RpIdentityResolver resolver) {
        String source = template == null ? "" : template;
        String account = accountName == null ? "" : accountName.trim();
        PlaceholderResolveService service = new PlaceholderResolveService(resolver);
        Matcher matcher = SELF_PLACEHOLDER.matcher(source);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String type = matcher.group(1).toLowerCase(Locale.ROOT);
            String replacement;
            if (account.isEmpty()) {
                replacement = matcher.group();
            } else {
                LetterPlaceholder placeholder = new LetterPlaceholder(
                        "{{" + type + ":" + account + "}}", type, account, 0, 0);
                PlaceholderResolveResult result = service.resolve(placeholder);
                replacement = result.ok() ? result.resolved() : placeholder.raw();
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        String expanded = out.toString();
        // Formatierung der Signatur soll den folgenden Text nicht einfärben.
        if (!expanded.isEmpty()
                && !LetterFormattingCodes.activePrefixBefore(expanded, expanded.length()).isEmpty()) {
            expanded += LetterFormattingCodes.SECTION + "r";
        }
        return expanded;
    }
}
