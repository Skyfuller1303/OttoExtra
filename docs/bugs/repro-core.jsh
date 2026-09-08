import de.ottoextra.chat.LongChatSplitter;
import de.ottoextra.letter.send.LetterV2Planner;
import java.util.*;

var unicodeParts = LongChatSplitter.split("x".repeat(253) + "\uD83D\uDE00" + "abc", 256, " >");
System.out.println("unicode partCount=" + unicodeParts.size() + ", highBeforeMarker=" + Character.isHighSurrogate(unicodeParts.get(0).charAt(253)) + ", lowAtNextStart=" + Character.isLowSurrogate(unicodeParts.get(1).charAt(0)));

var nested = LongChatSplitter.split("(".repeat(300), 256, " >");
System.out.println("nested parts=" + nested.size() + ", firstLength=" + nested.getFirst().length() + ", lastLength=" + nested.getLast().length() + ", lastEqualsInput=" + nested.getLast().equals("(".repeat(300)));

var p = LetterV2Planner.planDocument("letter", List.of("\nHallo"), String::length);
System.out.println("leadingBlank predicted=" + p.predictedPages() + ", commands=" + p.commands());
var p2 = LetterV2Planner.planDocument("letter", List.of("\n\nHallo"), String::length);
System.out.println("leadingTwoBlanks predicted=" + p2.predictedPages() + ", commands=" + p2.commands());

// Logic model of the current comparator branches, not Minecraft instances.
record Row(String name, boolean spectator, int listOrder, String lehen) {}
Comparator<Row> vanilla = Comparator.<Row>comparingInt(r -> -r.listOrder()).thenComparingInt(r -> r.spectator() ? 1 : 0).thenComparing(Row::name);
Comparator<Row> wrapped = (left, right) -> { if (left.spectator() != right.spectator()) return vanilla.compare(left, right); int result = left.lehen().compareTo(right.lehen()); return result != 0 ? result : vanilla.compare(left, right); };
var a = new Row("A", false, 0, "A");
var b = new Row("B", false, 2, "B");
var s = new Row("S", true, 1, "");
System.out.println("comparator A<B=" + (wrapped.compare(a,b)<0) + ", B<S=" + (wrapped.compare(b,s)<0) + ", S<A=" + (wrapped.compare(s,a)<0));
List<Row> rows = new ArrayList<>();
for (int j = 0; j < 32; j++) { String suffix = String.format("%02d", j); rows.add(new Row("A" + suffix, false, 0, "A")); rows.add(new Row("B" + suffix, false, 2, "B")); rows.add(new Row("S" + suffix, true, 1, "")); }
Collections.shuffle(rows, new Random(49));
try { rows.sort(wrapped); System.out.println("comparator: no exception on this runtime"); } catch (IllegalArgumentException expected) { System.out.println("comparator: " + expected); }
/exit

