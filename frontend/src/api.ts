// Zugriff auf das Backend. Alle Aufrufe gehen an /api auf demselben Ursprung
// (Vite-Proxy bzw. nginx), damit das Sitzungs-Cookie automatisch mitgeht.

export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

export interface QueryResult {
  columns: string[];
  rows: unknown[][];
  rowCount: number;
  truncated: boolean;
  durationMs: number;
}

/** Fehler mit der lesbaren Meldung aus dem Backend (ApiError). */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly type: string,
    readonly status: number,
  ) {
    super(message);
  }
}

/** Das Backend legt das CSRF-Token im Cookie XSRF-TOKEN ab. */
function csrfToken(): string | undefined {
  const cookie = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='));
  return cookie ? decodeURIComponent(cookie.substring('XSRF-TOKEN='.length)) : undefined;
}

async function request<T>(method: string, path: string, body?: unknown, retried = false): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const token = csrfToken();
  if (method !== 'GET' && token) {
    headers['X-XSRF-TOKEN'] = token;
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: 'same-origin',
    });
  } catch {
    throw new ApiError('Der Server ist nicht erreichbar. Läuft das Backend?', 'NETWORK', 0);
  }

  // Fehlte das CSRF-Cookie noch, hat das Backend es mit dieser Antwort gesetzt.
  if (response.status === 403 && !token && !retried) {
    return request<T>(method, path, body, true);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(data?.message ?? `Unerwarteter Fehler (${response.status}).`, data?.type ?? 'UNKNOWN', response.status);
  }
  return data as T;
}

export const api = {
  me: () => request<User>('GET', '/api/auth/me'),
  login: (username: string, password: string) => request<User>('POST', '/api/auth/login', { username, password }),
  register: (username: string, email: string, password: string) =>
    request<User>('POST', '/api/auth/register', { username, email, password }),
  logout: () => request<void>('POST', '/api/auth/logout'),
  runQuery: (sql: string) => request<QueryResult>('POST', '/api/queries', { sql }),
};
