package at.htlwels.sqldetective.query;

/**
 * Wird geworfen, wenn eine Abfrage die Validierung nicht besteht.
 * Die Meldung ist fuer Spieler gedacht und muss verstaendlich sein.
 */
public class InvalidQueryException extends RuntimeException {

    public InvalidQueryException(String message) {
        super(message);
    }
}
