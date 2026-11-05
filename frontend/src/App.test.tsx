import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ApiError, api, type CaseSummary, type QueryResult } from './api';
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
    },
  };
});

const mocked = vi.mocked(api);
const user = { id: 1, username: 'max', email: 'max@example.com', role: 'player' };

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
    expect(await screen.findByRole('heading', { name: 'Ermittlungsdatenbank' })).toBeInTheDocument();
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
    expect(await screen.findByRole('heading', { name: 'Ermittlungsdatenbank' })).toBeInTheDocument();
  });

  it('leitet die alte Editor-Adresse auf die Fallübersicht um', async () => {
    renderAt('/editor');
    expect(await screen.findByRole('heading', { name: 'Fälle' })).toBeInTheDocument();
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
