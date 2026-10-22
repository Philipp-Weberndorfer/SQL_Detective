import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ApiError, api, type QueryResult } from './api';
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
    renderAt('/editor');
    expect(await screen.findByRole('heading', { name: 'Anmelden' })).toBeInTheDocument();
  });

  it('meldet an und kehrt danach zum Editor zurück', async () => {
    mocked.me.mockRejectedValue(new ApiError('', 'UNAUTHORIZED', 401));
    mocked.login.mockResolvedValue(user);
    renderAt('/editor');

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
    renderAt('/editor');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));

    expect(mocked.runQuery).toHaveBeenCalledWith('SELECT * FROM persons;');
    expect(await screen.findByRole('columnheader', { name: 'first_name' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'Mustermann' })).toBeInTheDocument();
  });

  it('zeigt während der Ausführung einen Ladezustand', async () => {
    let finish: (r: QueryResult) => void = () => undefined;
    mocked.runQuery.mockReturnValue(new Promise((resolve) => (finish = resolve)));
    renderAt('/editor');

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
    renderAt('/editor');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('SQL-Fehler');
    expect(alert).toHaveTextContent('column "gibts_nicht" does not exist');
  });

  it('führt die Abfrage per Strg+Enter aus', async () => {
    mocked.runQuery.mockResolvedValue(result);
    renderAt('/editor');

    const editor = await screen.findByRole('textbox');
    await userEvent.click(editor);
    await userEvent.keyboard('{Control>}{Enter}{/Control}');

    await waitFor(() => expect(mocked.runQuery).toHaveBeenCalled());
  });

  it('geht bei abgelaufener Sitzung zum Login', async () => {
    mocked.runQuery.mockRejectedValue(new ApiError('Bitte melde dich zuerst an.', 'UNAUTHORIZED', 401));
    renderAt('/editor');

    await userEvent.click(await screen.findByRole('button', { name: 'Ausführen' }));
    expect(await screen.findByRole('heading', { name: 'Anmelden' })).toBeInTheDocument();
  });
});
