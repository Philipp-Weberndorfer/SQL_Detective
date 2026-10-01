package at.htlwels.sqldetective.query;

/** Die Abfrage hat das Zeitlimit ueberschritten und wurde abgebrochen (F34, F113). */
public class QueryTimeoutException extends RuntimeException {

    public QueryTimeoutException(String message) {
        super(message);
    }
}
