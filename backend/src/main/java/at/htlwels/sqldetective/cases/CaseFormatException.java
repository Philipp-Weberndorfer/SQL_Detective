package at.htlwels.sqldetective.cases;

/** Ein Fall entspricht nicht dem Fallformat - die Meldung nennt Fall und Feld. */
public class CaseFormatException extends RuntimeException {

    public CaseFormatException(String message) {
        super(message);
    }
}
