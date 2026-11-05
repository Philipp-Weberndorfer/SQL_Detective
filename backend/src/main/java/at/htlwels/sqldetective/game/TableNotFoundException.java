package at.htlwels.sqldetective.game;

/** Die Tabelle gehoert nicht zum Fall - auch Systemtabellen und Tabellen anderer Faelle. */
public class TableNotFoundException extends RuntimeException {

    public TableNotFoundException(String table) {
        super("Die Tabelle " + table + " gibt es in diesem Fall nicht.");
    }
}
