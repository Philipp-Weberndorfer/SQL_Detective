package at.htlwels.sqldetective.game;

/** Eine Spielregel verbietet die Aktion, z. B. einen noch nicht gefundenen Beweis sichern (409 RULE). */
public class RuleException extends RuntimeException {

    public RuleException(String message) {
        super(message);
    }
}
