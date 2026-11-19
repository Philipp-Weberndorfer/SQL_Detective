package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.cases.CaseModel.ExpectedResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vergleicht das Ergebnis einer Spielerabfrage mit dem erwarteten Ergebnis
 * einer Mission (F44, F45, fall-format.md Regel 2).
 *
 * Geprueft wird das Ergebnis, nicht der Wortlaut:
 *   - Spalten werden ueber ihren Namen zugeordnet, ohne Gross-/Kleinschreibung;
 *     die Reihenfolge ist egal, zusaetzliche Spalten (z. B. SELECT *) auch
 *   - Zeilen werden als Multimenge verglichen: fehlende, zusaetzliche oder
 *     doppelte Zeilen sind falsch; die Reihenfolge zaehlt nur bei orderMatters
 *   - Werte werden vereinheitlicht: 5, 5.0 und "5" sind gleich, ebenso
 *     Zeitstempel mit und ohne ".0"
 *
 * Die Klasse kennt keine Datenbank und ist damit ohne Container testbar.
 */
public final class ResultMatcher {

    /** Mehr Spaltenkombinationen probieren wir nicht (mehrdeutige Namen bei SELECT * ueber JOINs). */
    private static final int MAX_COMBINATIONS = 256;

    private static final Pattern TIMESTAMP =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2})[ T](\\d{2}:\\d{2})(:\\d{2})?(\\.\\d+)?");

    private ResultMatcher() {
    }

    public static boolean matches(ExpectedResult expected, List<String> columns, List<List<Object>> rows,
                                  boolean truncated) {
        if (truncated || rows.size() != expected.rows().size()) {
            return false;
        }

        // Kandidaten je erwarteter Spalte - bei SELECT * ueber einen JOIN kann "id" mehrfach vorkommen
        List<List<Integer>> candidates = new ArrayList<>();
        for (String wanted : expected.columns()) {
            List<Integer> found = new ArrayList<>();
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).equalsIgnoreCase(wanted)) {
                    found.add(i);
                }
            }
            if (found.isEmpty()) {
                return false;
            }
            candidates.add(found);
        }

        List<List<String>> wantedRows = expected.rows().stream().map(ResultMatcher::normalizeRow).toList();
        int[] choice = new int[candidates.size()];
        for (int tried = 0; tried < MAX_COMBINATIONS; tried++) {
            if (rowsMatch(wantedRows, project(rows, candidates, choice), expected.orderMatters())) {
                return true;
            }
            if (!next(choice, candidates)) {
                return false;
            }
        }
        return false;
    }

    private static List<List<String>> project(List<List<Object>> rows, List<List<Integer>> candidates, int[] choice) {
        List<List<String>> projected = new ArrayList<>(rows.size());
        for (List<Object> row : rows) {
            List<String> values = new ArrayList<>(candidates.size());
            for (int c = 0; c < candidates.size(); c++) {
                values.add(normalize(row.get(candidates.get(c).get(choice[c]))));
            }
            projected.add(values);
        }
        return projected;
    }

    private static boolean rowsMatch(List<List<String>> wanted, List<List<String>> actual, boolean orderMatters) {
        if (orderMatters) {
            return wanted.equals(actual);
        }
        return sorted(wanted).equals(sorted(actual));
    }

    private static List<List<String>> sorted(List<List<String>> rows) {
        Comparator<List<String>> byValues = (a, b) -> {
            for (int i = 0; i < a.size(); i++) {
                int cmp = Comparator.nullsFirst(Comparator.<String>naturalOrder()).compare(a.get(i), b.get(i));
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        };
        return rows.stream().sorted(byValues).toList();
    }

    /** Naechste Kombination wie ein Zaehler; false, wenn alle durch sind. */
    private static boolean next(int[] choice, List<List<Integer>> candidates) {
        for (int i = choice.length - 1; i >= 0; i--) {
            if (choice[i] + 1 < candidates.get(i).size()) {
                choice[i]++;
                return true;
            }
            choice[i] = 0;
        }
        return false;
    }

    private static List<String> normalizeRow(List<Object> row) {
        return row.stream().map(ResultMatcher::normalize).toList();
    }

    static String normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return number(n.toString());
        }
        if (value instanceof Boolean b) {
            return b.toString();
        }
        String text = value.toString();
        Matcher ts = TIMESTAMP.matcher(text);
        if (ts.matches()) {
            String seconds = Objects.requireNonNullElse(ts.group(3), ":00");
            String fraction = ts.group(4) == null ? "" : ts.group(4).replaceAll("0+$", "");
            return ts.group(1) + " " + ts.group(2) + seconds + (fraction.equals(".") ? "" : fraction);
        }
        if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
            return text.toLowerCase(Locale.ROOT);
        }
        return isNumber(text) ? number(text) : text;
    }

    private static boolean isNumber(String text) {
        return text.matches("-?\\d+(\\.\\d+)?");
    }

    private static String number(String text) {
        try {
            BigDecimal d = new BigDecimal(text).stripTrailingZeros();
            return d.signum() == 0 ? "0" : d.toPlainString();
        } catch (NumberFormatException e) {
            return text;
        }
    }
}
