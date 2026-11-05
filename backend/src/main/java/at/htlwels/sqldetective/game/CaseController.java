package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.auth.AuthService;
import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseRegistry;
import at.htlwels.sqldetective.game.CaseDtos.CaseDetail;
import at.htlwels.sqldetective.game.CaseDtos.CaseSummary;
import at.htlwels.sqldetective.cases.CaseModel.Mission;
import at.htlwels.sqldetective.game.CaseDtos.Column;
import at.htlwels.sqldetective.game.CaseDtos.MissionView;
import at.htlwels.sqldetective.game.CaseDtos.Reference;
import at.htlwels.sqldetective.game.CaseDtos.TableDetail;
import at.htlwels.sqldetective.game.CaseDtos.TableSummary;
import at.htlwels.sqldetective.cases.CaseModel.TableInfo;
import at.htlwels.sqldetective.game.ProgressRepository.CaseProgress;
import at.htlwels.sqldetective.game.ProgressService.CaseState;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Faelle aus Sicht des angemeldeten Spielers. Jede Antwort enthaelt nur die
 * oeffentlichen Felder aus {@link CaseDtos} - nie Loesungen.
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseRegistry cases;
    private final ProgressService progress;
    private final AuthService users;

    public CaseController(CaseRegistry cases, ProgressService progress, AuthService users) {
        this.cases = cases;
        this.progress = progress;
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "Alle Faelle mit Titel, Schwierigkeit, Schwerpunkt und Status des Spielers, sortiert nach Fallnummer")
    public List<CaseSummary> list(Authentication auth) {
        List<CaseProgress> rows = progress.allCases(userId(auth));
        return cases.all().stream()
                .map(c -> new CaseSummary(
                        c.id(),
                        number(c),
                        c.definition().title(),
                        c.definition().difficulty(),
                        c.definition().focus(),
                        progress.status(rows, c.id())))
                .toList();
    }

    @GetMapping("/{caseId}")
    @Operation(summary = "Ein Fall mit dem Fortschritt des angemeldeten Spielers")
    public CaseDetail detail(@PathVariable String caseId, Authentication auth) {
        LoadedCase c = cases.get(caseId);
        return toDetail(c, progress.state(userId(auth), c));
    }

    @PostMapping("/{caseId}/start")
    @Operation(summary = "Fall beginnen: beim ersten Mal AVAILABLE -> IN_PROGRESS, danach ohne Wirkung")
    public CaseDetail start(@PathVariable String caseId, Authentication auth) {
        LoadedCase c = cases.get(caseId);
        return toDetail(c, progress.start(userId(auth), c));
    }

    @GetMapping("/{caseId}/tables")
    @Operation(summary = "Tabellen des Falls mit Zeilenanzahl (Database Explorer)",
            description = "Nur die Tabellen dieses Falls - keine Systemtabellen, keine anderen Faelle. "
                    + "Kommt aus den beim Start geladenen Falldaten, nicht aus einer Spielerabfrage.")
    public List<TableSummary> tables(@PathVariable String caseId) {
        return cases.get(caseId).tables().stream()
                .map(t -> new TableSummary(t.name(), t.rowCount()))
                .toList();
    }

    @GetMapping("/{caseId}/tables/{table}")
    @Operation(summary = "Spalten einer Tabelle mit Datentyp, Primaerschluessel, Pflichtfeld und Fremdschluessel")
    public TableDetail table(@PathVariable String caseId, @PathVariable String table) {
        TableInfo t = cases.get(caseId).table(table).orElseThrow(() -> new TableNotFoundException(table));
        return new TableDetail(t.name(), t.rowCount(), t.columns().stream()
                .map(col -> new Column(col.name(), col.type(), col.primaryKey(), col.notNull(),
                        col.references() == null ? null
                                : new Reference(col.references().table(), col.references().column())))
                .toList());
    }

    private CaseDetail toDetail(LoadedCase c, CaseState state) {
        return new CaseDetail(
                c.id(),
                number(c),
                c.definition().title(),
                c.definition().difficulty(),
                c.definition().briefing(),
                state.status(),
                state.currentMissionId(),
                c.definition().missions().stream()
                        .map(m -> m.id())
                        .filter(state.solvedMissions()::contains)
                        .toList(),
                missions(c, state));
    }

    private static List<MissionView> missions(LoadedCase c, CaseState state) {
        List<Mission> all = c.definition().missions();
        return java.util.stream.IntStream.range(0, all.size())
                .mapToObj(i -> new MissionView(all.get(i).id(), i + 1, all.get(i).title(),
                        state.missionState(all.get(i).id())))
                .toList();
    }

    /** case-001 -> 001 */
    private static String number(LoadedCase c) {
        return c.id().substring("case-".length());
    }

    private long userId(Authentication auth) {
        return users.getByUsername(auth.getName()).id();
    }
}
