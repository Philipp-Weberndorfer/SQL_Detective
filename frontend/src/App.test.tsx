import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ApiError, api, type CaseDetail, type CaseSummary, type QueryResult } from './api';
import App from './App';
import { AuthProvider } from './auth';

vi.mock('./api', async (importOriginal) => {
  const original = await importOriginal<typeof import('./api')>();
  return {
    ...original,
    api: {
      me: vi.fn(),
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      runQuery: vi.fn(),
      cases: vi.fn(),
      caseDetail: vi.fn(),
      startCase: vi.fn(),
      tables: vi.fn(),
      table: vi.fn(),
      secureEvidence: vi.fn(),
      evidence: vi.fn(),
      conclusion: vi.fn(),
      submitConclusion: vi.fn(),
    },
  };
});

const mocked = vi.mocked(api);
const user = { id: 1, username: 'max', email: 'max@example.com', role: 'player' };

/** Fall 001, schon begonnen - der Editor erscheint ohne Briefing. */
const detail: CaseDetail = {
  id: 'case-001',
  number: '001',
  title: 'Der verschwundene Laptop',
  difficulty: 1,
  briefing: 'Freitag um 16:30 Uhr befand sich Notebook NB-042 noch im Netzwerklabor.',
  goal: 'Rekonstruiere, was zwischen Freitag 16:30 Uhr und Montag 07:15 Uhr passiert ist.',
  status: 'IN_PROGRESS',
  currentMissionId: 'm1',
  solvedMissionIds: [],
  missions: [
    { id: 'm1', number: 1, title: 'Wem war Notebook NB-042 zugeordnet?', state: 'CURRENT' },
    { id: 'm2', number: 2, title: 'Wer hat sich zuletzt auf NB-042 angemeldet?', state: 'LOCKED' },
  ],
};

/** Derselbe Fall, nachdem Mission 1 gelöst wurde. */
const afterMission1: CaseDetail = {
  ...detail,
  currentMissionId: 'm2',
  solvedMissionIds: ['m1'],
  missions: [
    { ...detail.missions[0], state: 'SOLVED' },
    { ...detail.missions[1], state: 'CURRENT' },
  ],
};

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.resetAllMocks();
  mocked.caseDetail.mockResolvedValue(detail);
  mocked.tables.mockResolvedValue([
    { name: 'access_logs', rowCount: 120 },
    { name: 'persons', rowCount: 32 },
  ]);
  mocked.evidence.mockResolvedValue({
    items: [{ id: 'e1', title: 'Gerätezuordnung', text: null, state: 'OPEN' }],
    secured: 0,
    total: 1,
  });
});

describe('Anmeldung', () => {
  it('leitet geschützte Seiten ohne Anmeldung auf den Login um', async () => {
    mocked.me.mockRejectedValue(new ApiError('Bitte melde dich zuerst an.', 'UNAUTHORIZED', 401));
    renderAt('/faelle/case-001');
    expect(await screen.findByRole('heading', { name: 'Anmelden' })).toBeInTheDocument();
  });

  it('meldet an und kehrt danach zum Editor zurück', async () => {
    mocked.me.mockRejectedValue(new ApiError('', 'UNAUTHORIZED', 401));
    mocked.login.mockResolvedValue(user);
    renderAt('/faelle/case-001');

    await userEvent.type(await screen.findByLabelText('Benutzername'), 'max');
    await userEvent.type(screen.getByLabelText('Passwort'), 'geheim123');
    await userEvent.click(screen.getByRole('button', { name: 'Anmelden' }));

    expect(mocked.login).toHaveBeenCalledWith('max', 'geheim123');
    expect(await screen.findByRole('button', { name: 'Ausführen' })).toBeInTheDocument();
  });

  it('zeigt die Fehlermeldung bei falschem Passwort', async () => {
    mocked.me.mockRejectedValue(new ApiError('', 'UNAUTHORIZED', 401));
    mocked.login.mockRejectedValue(new ApiError('Benutzername oder Passwort ist falsch.', 'UNAUTHORIZED', 401));
    renderAt('/login');

    await userEvent.type(await screen.findByLabelText('Benutzername'), 'max');
    await userEvent.type(screen.getByLabelText('Passwort'), 'falsch');
    await userEvent.click(screen.getByRole('button', { name: 'Anmelden' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Benutzername oder Passwort ist falsch.');
  });

  it('weist leere Felder ab, ohne das Backend zu fragen', async () => {
    mocked.me.mockRejectedValue(new ApiError('', 'UNAUTHORIZED', 401));
    renderAt('/login');
    await userEvent.click(await screen.findByRole('button', { name: 'Anmelden' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Bitte gib Benutzername und Passwort ein.');
    expect(mocked.login).not.toHaveBeenCalled();
  });
});

describe('Fallübersicht', () => {
  const cases: CaseSummary[] = [
    {
      id: 'case-001',
      number: '001',
      title: 'Der verschwundene Laptop',
      difficulty: 1,
      focus: ['SELECT', 'WHERE'],
      status: 'IN_PROGRESS',
    },
    { id: 'case-002', number: '002', title: 'Der manipulierte Stundenplan', difficulty: 2, focus: ['JOIN'], status: 'SOLVED' },
    { id: 'case-003', number: '003', title: 'Das verschwundene Preisgeld', difficulty: 2, focus: ['GROUP BY'], status: 'AVAILABLE' },
    { id: 'case-004', number: '004', title: 'Der geheimnisvolle Login', difficulty: 3, focus: ['Subqueries'], status: 'LOCKED' },
  ];

  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
    mocked.cases.mockResolvedValue(cases);
  });

  it('zeigt alle Fälle mit Nummer, Titel, Schwierigkeit und Schwerpunkt', async () => {
    renderAt('/faelle');

    expect(await screen.findByText('Der verschwundene Laptop')).toBeInTheDocument();
    expect(screen.getByText('#002')).toBeInTheDocument();
    expect(screen.getByLabelText('Schwierigkeit 3 von 4')).toBeInTheDocument();
    expect(screen.getByText('SELECT · WHERE')).toBeInTheDocument();
  });

  it('zeigt den Status jedes Falls als Text, gesperrte Fälle sind nicht anklickbar', async () => {
    renderAt('/faelle');

    expect(await screen.findByText('In Arbeit')).toBeInTheDocument();
    expect(screen.getByText('Gelöst')).toBeInTheDocument();
    expect(screen.getByText('Verfügbar')).toBeInTheDocument();
    expect(screen.getByText('Gesperrt')).toBeInTheDocument();
    expect(screen.getByText('Der geheimnisvolle Login').closest('a')).toBeNull();
    expect(screen.getByText('Das verschwundene Preisgeld').closest('a')).toHaveAttribute('href', '/faelle/case-003');
  });

  it('öffnet einen Fall per Klick', async () => {
    renderAt('/faelle');
    await userEvent.click(await screen.findByText('Der verschwundene Laptop'));
    expect(await screen.findByRole('button', { name: 'Ausführen' })).toBeInTheDocument();
  });

  it('leitet die alte Editor-Adresse auf die Fallübersicht um', async () => {
    renderAt('/editor');
    expect(await screen.findByRole('heading', { name: 'Fälle' })).toBeInTheDocument();
  });
});

describe('Case Briefing', () => {
  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
  });

  it('zeigt beim ersten Öffnen zuerst das Briefing und startet dann die Ermittlung', async () => {
    mocked.caseDetail.mockResolvedValue({ ...detail, status: 'AVAILABLE', currentMissionId: null });
    mocked.startCase.mockResolvedValue(detail);
    renderAt('/faelle/case-001');

    expect(await screen.findByRole('heading', { name: 'Der verschwundene Laptop' })).toBeInTheDocument();
    expect(screen.getByText('CASE #001')).toBeInTheDocument();
    expect(screen.getByText(/Notebook NB-042 noch im Netzwerklabor/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ausführen' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Ermittlung starten' }));

    expect(mocked.startCase).toHaveBeenCalledWith('case-001');
    expect(await screen.findByRole('button', { name: 'Ausführen' })).toBeInTheDocument();
  });

  it('geht bei einem begonnenen Fall direkt zum Editor', async () => {
    renderAt('/faelle/case-001');
    expect(await screen.findByRole('button', { name: 'Ausführen' })).toBeInTheDocument();
    expect(mocked.startCase).not.toHaveBeenCalled();
  });

  it('lässt sich während des Falls wieder öffnen', async () => {
    renderAt('/faelle/case-001');
    await userEvent.click(await screen.findByRole('button', { name: 'Briefing' }));

    expect(screen.getByText(/Notebook NB-042 noch im Netzwerklabor/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Zurück zur Ermittlung' }));

    expect(await screen.findByRole('button', { name: 'Ausführen' })).toBeInTheDocument();
    expect(mocked.startCase).not.toHaveBeenCalled();
  });
});

describe('Database Explorer', () => {
  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
  });

  it('listet neben dem Editor die Tabellen des Falls mit Zeilenanzahl', async () => {
    renderAt('/faelle/case-001');

    const explorer = await screen.findByRole('navigation', { name: 'Tabellen' });
    expect(await within(explorer).findByText('persons')).toBeInTheDocument();
    expect(within(explorer).getByText('120 Zeilen')).toBeInTheDocument();
    expect(mocked.tables).toHaveBeenCalledWith('case-001');
    // die feste Liste aus Sprint 1 ist weg
    expect(screen.queryByText(/Tabellen in Fall #001/)).not.toBeInTheDocument();
  });

  it('zeigt per Klick den Aufbau einer Tabelle, ein Fremdschlüssel öffnet die Zieltabelle', async () => {
    mocked.table.mockImplementation(async (_caseId, name) =>
      name === 'access_logs'
        ? {
            name: 'access_logs',
            rowCount: 120,
            columns: [
              { name: 'id', type: 'integer', primaryKey: true, notNull: true, references: null },
              { name: 'person_id', type: 'integer', primaryKey: false, notNull: true, references: { table: 'persons', column: 'id' } },
              { name: 'access_time', type: 'timestamp', primaryKey: false, notNull: true, references: null },
            ],
          }
        : {
            name: 'persons',
            rowCount: 32,
            columns: [{ name: 'first_name', type: 'varchar(50)', primaryKey: false, notNull: true, references: null }],
          },
    );
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: /access_logs/ }));

    const schema = await screen.findByRole('region', { name: 'Aufbau von access_logs' });
    expect(await within(schema).findByText('access_time')).toBeInTheDocument();
    expect(within(schema).getAllByText('timestamp')[0]).toBeInTheDocument();
    expect(within(schema).getByTitle('Primärschlüssel')).toBeInTheDocument();

    await userEvent.click(within(schema).getByRole('button', { name: '→ persons.id' }));
    expect(await screen.findByRole('region', { name: 'Aufbau von persons' })).toBeInTheDocument();
    expect(mocked.table).toHaveBeenLastCalledWith('case-001', 'persons');
    // der Editor ist noch da, der SQL-Text bleibt erhalten
    expect(screen.getByRole('textbox')).toHaveTextContent('SELECT * FROM persons;');
  });
});

describe('Missionsprüfung', () => {
  const base: QueryResult = { columns: ['x'], rows: [['1']], rowCount: 1, truncated: false, durationMs: 1 };

  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
  });

  it('meldet eine gelöste Mission und lädt den Fortschritt neu', async () => {
    mocked.runQuery.mockResolvedValue({
      ...base,
      mission: { missionId: 'm1', solved: true, nextMissionId: 'm2', allSolved: false },
    });
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));

    expect(await screen.findByRole('status', { name: 'Mission gelöst' })).toHaveTextContent(
      'Die nächste Mission ist freigeschaltet.',
    );
    await waitFor(() => expect(mocked.caseDetail).toHaveBeenCalledTimes(2));
  });

  it('zeigt bei einem Fehlversuch keine Fehlermeldung, nur dass die Mission offen ist', async () => {
    mocked.runQuery.mockResolvedValue({
      ...base,
      mission: { missionId: 'm1', solved: false, nextMissionId: null, allSolved: false },
    });
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));

    expect(await screen.findByText('Die Mission ist noch offen.')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(mocked.caseDetail).toHaveBeenCalledTimes(1);
  });
});

describe('Aktuelle Mission', () => {
  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
  });

  it('steht über dem Editor und wechselt nach dem Lösen ohne Neuladen zur nächsten', async () => {
    mocked.caseDetail.mockResolvedValueOnce(detail).mockResolvedValueOnce(afterMission1);
    mocked.runQuery.mockResolvedValue({
      columns: ['x'],
      rows: [],
      rowCount: 0,
      truncated: false,
      durationMs: 1,
      mission: { missionId: 'm1', solved: true, nextMissionId: 'm2', allSolved: false },
    });
    renderAt('/faelle/case-001');

    const banner = await screen.findByRole('region', { name: 'Aktuelle Mission' });
    expect(banner).toHaveTextContent('Mission 1 von 2');
    expect(banner).toHaveTextContent('Wem war Notebook NB-042 zugeordnet?');

    await userEvent.click(screen.getByRole('button', { name: 'Ausführen' }));

    expect(await screen.findByRole('status', { name: 'Mission gelöst' })).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole('region', { name: 'Aktuelle Mission' })).toHaveTextContent('Mission 2 von 2'),
    );
  });

  it('zeigt nach der letzten Mission den Hinweis auf den Fallabschluss', async () => {
    mocked.caseDetail.mockResolvedValue({
      ...detail,
      currentMissionId: null,
      solvedMissionIds: ['m1', 'm2'],
      missions: detail.missions.map((m) => ({ ...m, state: 'SOLVED' as const })),
    });
    renderAt('/faelle/case-001');

    expect(await screen.findByRole('region', { name: 'Aktuelle Mission' })).toHaveTextContent(
      'Alle Missionen sind gelöst.',
    );
  });
});

describe('Beweis sichern', () => {
  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
  });

  it('bietet nach einer gelösten Mission das Beweisstück an und sichert es per Klick', async () => {
    mocked.runQuery.mockResolvedValue({
      columns: ['x'],
      rows: [],
      rowCount: 0,
      truncated: false,
      durationMs: 1,
      mission: {
        missionId: 'm1',
        solved: true,
        nextMissionId: 'm2',
        allSolved: false,
        evidence: { id: 'e1', title: 'Gerätezuordnung', text: 'NB-042 ist Max Mustermann zugeordnet.', state: 'FOUND' },
      },
    });
    mocked.secureEvidence.mockResolvedValue({
      id: 'e1',
      title: 'Gerätezuordnung',
      text: 'NB-042 ist Max Mustermann zugeordnet.',
      state: 'SECURED',
    });
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));
    const offer = await screen.findByRole('region', { name: 'Neues Beweisstück' });
    expect(offer).toHaveTextContent('NB-042 ist Max Mustermann zugeordnet.');

    await userEvent.click(within(offer).getByRole('button', { name: 'Als Beweis sichern' }));

    expect(mocked.secureEvidence).toHaveBeenCalledWith('case-001', 'e1');
    expect(await within(offer).findByText(/Gesichert/)).toBeInTheDocument();
    expect(within(offer).queryByRole('button', { name: 'Als Beweis sichern' })).not.toBeInTheDocument();
  });
});

describe('Evidence Board', () => {
  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
  });

  it('zeigt gesicherte Beweise mit Text, offene nur als Platzhalter, und den Stand als Zahl', async () => {
    mocked.evidence.mockResolvedValue({
      items: [
        { id: 'e1', title: 'Gerätezuordnung', text: 'NB-042 ist Max Mustermann zugeordnet.', state: 'SECURED' },
        { id: 'e2', title: 'Letzte Anmeldung', text: null, state: 'OPEN' },
      ],
      secured: 1,
      total: 2,
    });
    renderAt('/faelle/case-001');

    const board = await screen.findByRole('region', { name: 'Evidence Board' });
    expect(await within(board).findByText('1 von 2 Beweisen gesichert')).toBeInTheDocument();
    expect(board).toHaveTextContent('NB-042 ist Max Mustermann zugeordnet.');
    expect(within(board).getByLabelText('gesichert')).toBeInTheDocument();
    expect(within(board).getByLabelText('noch offen')).toBeInTheDocument();
    expect(board).toHaveTextContent('Letzte Anmeldung');
  });

  it('sichert einen gefundenen Beweis direkt auf dem Board und lädt neu', async () => {
    mocked.evidence
      .mockResolvedValueOnce({
        items: [{ id: 'e1', title: 'Gerätezuordnung', text: 'NB-042 ist Max Mustermann zugeordnet.', state: 'FOUND' }],
        secured: 0,
        total: 1,
      })
      .mockResolvedValueOnce({
        items: [{ id: 'e1', title: 'Gerätezuordnung', text: 'NB-042 ist Max Mustermann zugeordnet.', state: 'SECURED' }],
        secured: 1,
        total: 1,
      });
    mocked.secureEvidence.mockResolvedValue({ id: 'e1', title: 'Gerätezuordnung', text: '', state: 'SECURED' });
    renderAt('/faelle/case-001');

    const board = await screen.findByRole('region', { name: 'Evidence Board' });
    await userEvent.click(await within(board).findByRole('button', { name: 'Sichern' }));

    expect(mocked.secureEvidence).toHaveBeenCalledWith('case-001', 'e1');
    expect(await within(board).findByText('1 von 1 Beweisen gesichert')).toBeInTheDocument();
  });
});

describe('Fall abschließen', () => {
  const allSolved: CaseDetail = {
    ...detail,
    currentMissionId: null,
    solvedMissionIds: ['m1', 'm2'],
    missions: detail.missions.map((m) => ({ ...m, state: 'SOLVED' as const })),
  };

  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
    mocked.caseDetail.mockResolvedValue(allSolved);
    mocked.evidence.mockResolvedValue({
      items: [
        { id: 'e1', title: 'Gerätezuordnung', text: 'NB-042 ist Max Mustermann zugeordnet.', state: 'SECURED' },
        { id: 'e2', title: 'Letzte Anmeldung', text: 'Tobias Gruber, Freitag 18:45.', state: 'SECURED' },
      ],
      secured: 2,
      total: 2,
    });
    mocked.conclusion.mockResolvedValue({
      question: 'Wer hat das Notebook mitgenommen?',
      options: ['Max Mustermann', 'Tobias Gruber'],
      maxEvidence: 1,
      solved: false,
    });
  });

  it('wählt Antwort und Beweis auf dem Board und löst den Fall', async () => {
    mocked.submitConclusion.mockResolvedValue({
      solved: true,
      message: 'Fall geloest!',
      missionsSolved: 2,
      missionsTotal: 2,
      evidenceSecured: 2,
      evidenceTotal: 2,
    });
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: 'Fall abschließen' }));
    const form = await screen.findByRole('form', { name: 'Fall abschließen' });
    await userEvent.click(await within(form).findByLabelText('Tobias Gruber'));

    const board = screen.getByRole('region', { name: 'Evidence Board' });
    await userEvent.click(within(board).getByRole('checkbox', { name: 'Letzte Anmeldung' }));
    // höchstens ein Beweis - der zweite lässt sich nicht mehr ankreuzen
    expect(within(board).getByRole('checkbox', { name: 'Gerätezuordnung' })).toBeDisabled();

    await userEvent.click(within(form).getByRole('button', { name: 'Schlussfolgerung vorlegen' }));

    expect(mocked.submitConclusion).toHaveBeenCalledWith('case-001', 'Tobias Gruber', ['e2']);
    const done = await screen.findByRole('region', { name: 'Fall gelöst' });
    expect(done).toHaveTextContent('2 von 2 Missionen gelöst');
  });

  it('zeigt bei einer nicht überzeugenden Beweisführung die Meldung und erlaubt einen neuen Versuch', async () => {
    mocked.submitConclusion.mockResolvedValue({
      solved: false,
      message: 'Deine Beweisfuehrung ueberzeugt noch nicht.',
      missionsSolved: 2,
      missionsTotal: 2,
      evidenceSecured: 2,
      evidenceTotal: 2,
    });
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: 'Fall abschließen' }));
    const form = await screen.findByRole('form', { name: 'Fall abschließen' });
    await userEvent.click(await within(form).findByLabelText('Max Mustermann'));
    await userEvent.click(within(form).getByRole('button', { name: 'Schlussfolgerung vorlegen' }));

    expect(await within(form).findByText('Deine Beweisfuehrung ueberzeugt noch nicht.')).toBeInTheDocument();
    expect(within(form).getByRole('button', { name: 'Schlussfolgerung vorlegen' })).toBeEnabled();
  });
});

describe('Ermittlungsziel und Teilziele', () => {
  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
  });

  it('zeigt Ziel und alle Fragen mit ihrem Zustand und aktualisiert sich nach dem Lösen', async () => {
    mocked.caseDetail.mockResolvedValueOnce(detail).mockResolvedValueOnce(afterMission1);
    mocked.runQuery.mockResolvedValue({
      columns: ['x'],
      rows: [],
      rowCount: 0,
      truncated: false,
      durationMs: 1,
      mission: { missionId: 'm1', solved: true, nextMissionId: 'm2', allSolved: false },
    });
    renderAt('/faelle/case-001');

    const panel = await screen.findByRole('region', { name: 'Ermittlung' });
    expect(panel).toHaveTextContent('Rekonstruiere, was zwischen Freitag 16:30 Uhr');
    expect(panel).toHaveTextContent('0 von 2 gelöst');
    expect(within(panel).getByLabelText('aktuell')).toBeInTheDocument();
    expect(within(panel).getByLabelText('gesperrt')).toBeInTheDocument();
    // auch gesperrte Fragen sind sichtbar
    expect(panel).toHaveTextContent('Wer hat sich zuletzt auf NB-042 angemeldet?');

    await userEvent.click(screen.getByRole('button', { name: 'Ausführen' }));

    await waitFor(() => expect(screen.getByRole('region', { name: 'Ermittlung' })).toHaveTextContent('1 von 2 gelöst'));
    const updated = screen.getByRole('region', { name: 'Ermittlung' });
    expect(within(updated).getByLabelText('gelöst')).toBeInTheDocument();
    expect(within(updated).getByLabelText('aktuell')).toBeInTheDocument();
  });
});

describe('SQL-Editor', () => {
  const result: QueryResult = {
    columns: ['first_name', 'last_name'],
    rows: [['Max', 'Mustermann']],
    rowCount: 1,
    truncated: false,
    durationMs: 3,
  };

  beforeEach(() => {
    mocked.me.mockResolvedValue(user);
  });

  it('führt die Abfrage per Button aus und zeigt das Ergebnis als Tabelle', async () => {
    mocked.runQuery.mockResolvedValue(result);
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));

    expect(mocked.runQuery).toHaveBeenCalledWith('case-001', 'SELECT * FROM persons;');
    expect(await screen.findByRole('columnheader', { name: 'first_name' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'Mustermann' })).toBeInTheDocument();
  });

  it('zeigt während der Ausführung einen Ladezustand', async () => {
    let finish: (r: QueryResult) => void = () => undefined;
    mocked.runQuery.mockReturnValue(new Promise((resolve) => (finish = resolve)));
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));
    expect(screen.getByRole('status', { name: 'Abfrage läuft' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Läuft …' })).toBeDisabled();

    finish(result);
    await waitFor(() => expect(screen.queryByRole('status', { name: 'Abfrage läuft' })).not.toBeInTheDocument());
  });

  it('zeigt Fehlermeldungen deutlich an', async () => {
    mocked.runQuery.mockRejectedValue(
      new ApiError('ERROR: column "gibts_nicht" does not exist', 'SQL', 400),
    );
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('SQL-Fehler');
    expect(alert).toHaveTextContent('column "gibts_nicht" does not exist');
  });

  it('führt die Abfrage per Strg+Enter aus', async () => {
    mocked.runQuery.mockResolvedValue(result);
    renderAt('/faelle/case-001');

    const editor = await screen.findByRole('textbox');
    await userEvent.click(editor);
    await userEvent.keyboard('{Control>}{Enter}{/Control}');

    await waitFor(() => expect(mocked.runQuery).toHaveBeenCalled());
  });

  it('geht bei abgelaufener Sitzung zum Login', async () => {
    mocked.runQuery.mockRejectedValue(new ApiError('Bitte melde dich zuerst an.', 'UNAUTHORIZED', 401));
    renderAt('/faelle/case-001');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));
    expect(await screen.findByRole('heading', { name: 'Anmelden' })).toBeInTheDocument();
  });
});
