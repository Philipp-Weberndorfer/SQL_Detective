package at.htlwels.sqldetective.game;

/** Zustand eines Falls fuer einen Spieler (F11). */
public enum CaseStatus {
    LOCKED,         // Voraussetzungen nicht erfuellt (Regeln folgen mit SCRUM-41)
    AVAILABLE,      // noch nie geoeffnet
    IN_PROGRESS,
    SOLVED
}
