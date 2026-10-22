package at.htlwels.sqldetective.query;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erste Sicherheitsschicht: laesst nur lesende Abfragen durch (F31, F32, F111).
 *
 * Die zweite Schicht ist der Datenbankbenutzer game_readonly, der ohnehin nur
 * SELECT darf. Beide Schichten muessen bestehen bleiben - diese hier liefert
 * dem Spieler eine verstaendliche Meldung, die andere faengt ab, was hier
 * durchrutscht.
 *
 * Vorgehen:
 *   1. Zeichenketten und Kommentare neutralisieren, damit Tricks wie
 *      "SELECT 1 --; DROP TABLE persons;" oder "SELECT 'DROP TABLE x'"
 *      nicht zu falschen Ergebnissen fuehren.
 *   2. Pruefen, dass nur EIN Statement uebrig bleibt.
 *   3. Pruefen, dass es mit SELECT oder WITH beginnt.
 *   4. Pruefen, dass kein verbotenes Schluesselwort vorkommt.
 */
@Component
public class SqlValidator {

    /**
     * Verbotene Schluesselwoerter. Neben den offensichtlichen Schreibbefehlen
     * stehen hier auch COPY (kann ueber FROM PROGRAM Shell-Befehle ausfuehren)
     * und die Cursor- und Sitzungsbefehle.
     */
    private static final Set<String> FORBIDDEN = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "TRUNCATE",
            "GRANT", "REVOKE", "COPY", "CALL", "DO", "SET", "RESET",
            "VACUUM", "ANALYZE", "CLUSTER", "REINDEX", "LOCK", "COMMENT",
            "PREPARE", "EXECUTE", "DEALLOCATE", "DECLARE", "FETCH", "MOVE",
            "LISTEN", "NOTIFY", "UNLISTEN", "REFRESH", "IMPORT", "MERGE",
            "BEGIN", "COMMIT", "ROLLBACK", "SAVEPOINT",
            "INTO"      // SELECT ... INTO neue_tabelle legt eine Tabelle an
    );

    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new InvalidQueryException("Die Abfrage ist leer.");
        }

        String cleaned = neutralise(sql).trim();

        // Ein abschliessendes Semikolon ist erlaubt, mehr als ein Statement nicht.
        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        if (cleaned.contains(";")) {
            throw new InvalidQueryException(
                    "Es ist nur eine einzelne Abfrage erlaubt. Bitte entferne das zusaetzliche Semikolon.");
        }
        if (cleaned.isBlank()) {
            throw new InvalidQueryException("Die Abfrage enthaelt keinen ausfuehrbaren Befehl.");
        }

        Matcher m = WORD.matcher(cleaned);

        if (!m.find()) {
            throw new InvalidQueryException("Die Abfrage enthaelt keinen ausfuehrbaren Befehl.");
        }
        String first = cleaned.substring(m.start(), m.end()).toUpperCase(Locale.ROOT);
        if (!first.equals("SELECT") && !first.equals("WITH")) {
            throw new InvalidQueryException(
                    "Im Spielmodus sind nur lesende Abfragen erlaubt. Beginne mit SELECT.");
        }

        m.reset();
        while (m.find()) {
            String word = cleaned.substring(m.start(), m.end()).toUpperCase(Locale.ROOT);
            if (FORBIDDEN.contains(word)) {
                throw new InvalidQueryException(
                        "Der Befehl " + word + " ist im Spielmodus nicht erlaubt. "
                                + "Du kannst die Ermittlungsdatenbank nur lesen, nicht veraendern.");
            }
        }
    }

    /**
     * Ersetzt Kommentare durch ein Leerzeichen und den Inhalt von Zeichenketten
     * durch nichts. Dadurch kann weder ein Kommentar ein Schluesselwort
     * verstecken noch eine Zeichenkette einen Fehlalarm ausloesen.
     *
     * Beispiele:
     *   SELECT 1 --; DROP TABLE persons    ->  SELECT 1
     *   SELECT 'DROP TABLE persons'        ->  SELECT ''
     *   SELECT 1 /* DROP *&#47; FROM t     ->  SELECT 1   FROM t
     *   SELECT $$ -- $$; DROP TABLE x      ->  SELECT ''; DROP TABLE x
     *   SELECT E'\'' ; DROP TABLE x        ->  SELECT E'' ; DROP TABLE x
     *
     * Die Zerlegung muss genau der von PostgreSQL entsprechen. Wo sie
     * abweicht, liesse sich ein Befehl vor der Pruefung verstecken - deshalb
     * werden auch Dollar-Quoting und E-Strings (Backslash-Escapes) erkannt.
     */
    String neutralise(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        int i = 0;
        int n = sql.length();

        while (i < n) {
            char c = sql.charAt(i);

            // Zeilenkommentar
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                while (i < n && sql.charAt(i) != '\n') {
                    i++;
                }
                out.append(' ');
                continue;
            }

            // Blockkommentar, in PostgreSQL schachtelbar
            if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int depth = 1;
                i += 2;
                while (i < n && depth > 0) {
                    if (c(sql, i) == '/' && c(sql, i + 1) == '*') {
                        depth++;
                        i += 2;
                    } else if (c(sql, i) == '*' && c(sql, i + 1) == '/') {
                        depth--;
                        i += 2;
                    } else {
                        i++;
                    }
                }
                out.append(' ');
                continue;
            }

            // Dollar-Quoting: $$...$$ oder $tag$...$tag$ - Inhalt verwerfen
            if (c == '$' && !isIdentifierPart(c(sql, i - 1))) {
                int tagEnd = dollarTagEnd(sql, i);
                if (tagEnd > 0) {
                    String tag = sql.substring(i, tagEnd);
                    int close = sql.indexOf(tag, tagEnd);
                    out.append("''");
                    i = close < 0 ? n : close + tag.length();
                    continue;
                }
            }

            // Zeichenkette - Inhalt verwerfen, '' bleibt als Platzhalter stehen.
            // In E'...' maskiert ein Backslash das folgende Zeichen, auch ein Hochkomma.
            if (c == '\'') {
                char prev = c(sql, i - 1);
                boolean escapeString = (prev == 'E' || prev == 'e') && !isIdentifierPart(c(sql, i - 2));
                out.append("''");
                i++;
                while (i < n) {
                    if (escapeString && sql.charAt(i) == '\\') {
                        i += 2;
                    } else if (sql.charAt(i) == '\'') {
                        if (i + 1 < n && sql.charAt(i + 1) == '\'') {
                            i += 2;   // verdoppeltes Hochkomma innerhalb der Zeichenkette
                        } else {
                            i++;
                            break;
                        }
                    } else {
                        i++;
                    }
                }
                continue;
            }

            // Bezeichner in Anfuehrungszeichen bleiben erhalten, duerfen aber
            // keine Schluesselwoerter maskieren - deshalb als Platzhalter.
            if (c == '"') {
                out.append("\"x\"");
                i++;
                while (i < n && sql.charAt(i) != '"') {
                    i++;
                }
                i++;
                continue;
            }

            out.append(c);
            i++;
        }

        return out.toString();
    }

    private static char c(String s, int i) {
        return i >= 0 && i < s.length() ? s.charAt(i) : '\0';
    }

    private static boolean isIdentifierPart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
    }

    /**
     * Prueft, ob bei {@code start} ein Dollar-Quote-Tag beginnt ($$ oder $tag$),
     * und liefert die Position direkt dahinter - sonst -1. Ein Tag folgt den
     * Regeln fuer Bezeichner, darf aber kein $ enthalten und nicht mit einer
     * Ziffer beginnen ($1 ist ein Parameter, kein Quote).
     */
    private static int dollarTagEnd(String s, int start) {
        int i = start + 1;
        if (i < s.length() && Character.isDigit(s.charAt(i))) {
            return -1;
        }
        while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) {
            i++;
        }
        return c(s, i) == '$' ? i + 1 : -1;
    }
}
