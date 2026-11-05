package at.htlwels.sqldetective.cases;

/** Angefragter Fall existiert nicht (oder wurde wegen eines Formatfehlers nicht geladen). */
public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(String caseId) {
        super("Den Fall " + caseId + " gibt es nicht.");
    }
}
