export interface TokenPair {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  email: string;
}

export interface World {
  id: string;
  name: string;
  description: string | null;
}

export interface Faction {
  id: string;
  version: number;
  name: string;
  alignment: string | null;
  power: number;
  wealth: number;
  manpower: number;
  influence: number;
  territory: number;
  imageUrl: string | null;
  description: string | null;
}

export interface WorldEvent {
  id: string;
  version: number;
  title: string;
  type: string;
  category: string | null;
  targetFactionId: string | null;
  secondTargetFactionId: string | null;
  severity: number;
  powerDelta: number;
  wealthDelta: number;
  manpowerDelta: number;
  influenceDelta: number;
  territoryDelta: number;
  details: string | null;
  occurredAt: string;
}

export interface SimulationLog {
  id: string;
  message: string;
  createdAt: string;
}

export interface WorldSnapshot {
  world: World;
  factions: Faction[];
  events: WorldEvent[];
  logs: SimulationLog[];
}

interface CsrfResponse {
  headerName: string;
  token: string;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(
  /\/$/,
  '',
);

async function csrfHeader(): Promise<[string, string]> {
  const response = await fetch(`${apiBaseUrl}/api/auth/csrf`, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new ApiError('Could not establish a secure connection to the server.', response.status);
  }
  const csrf = (await response.json()) as CsrfResponse;
  return [csrf.headerName, csrf.token];
}

async function request<T>(
  path: string,
  options: {
    method?: string;
    body?: unknown;
    token?: string;
  } = {},
): Promise<T> {
  const method = options.method ?? 'GET';
  const headers = new Headers({ Accept: 'application/json' });
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  if (options.token) {
    headers.set('Authorization', `Bearer ${options.token}`);
  }

  const publicAuthEndpoint =
    path === '/api/auth/login' || path === '/api/auth/register';
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && !publicAuthEndpoint) {
    const [csrfName, csrfToken] = await csrfHeader();
    headers.set(csrfName, csrfToken);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    method,
    headers,
    credentials: 'include',
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  if (!response.ok) {
    let message = `Request failed (${response.status}).`;
    try {
      const body = (await response.json()) as { error?: string };
      if (body.error) message = body.error;
    } catch {
      // Some server errors have no JSON body.
    }
    throw new ApiError(message, response.status);
  }

  if ([202, 204].includes(response.status)) return undefined as T;
  return (await response.json()) as T;
}

export const api = {
  login: (email: string, password: string) =>
    request<TokenPair>('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    }),
  register: (email: string, password: string) =>
    request<TokenPair>('/api/auth/register', {
      method: 'POST',
      body: { email, password },
    }),
  refresh: () =>
    request<TokenPair>('/api/auth/refresh', {
      method: 'POST',
    }),
  logout: (token: string) =>
    request<void>('/api/auth/logout', {
      method: 'POST',
      token,
    }),
  requestPasswordReset: (email: string) =>
    request<void>('/api/auth/password-reset/request', {
      method: 'POST',
      body: { email },
    }),
  confirmPasswordReset: (token: string, newPassword: string) =>
    request<void>('/api/auth/password-reset/confirm', {
      method: 'POST',
      body: { token, newPassword },
    }),
  listWorlds: (token: string) => request<World[]>('/api/worlds', { token }),
  getWorld: (worldId: string, token: string) =>
    request<WorldSnapshot>(`/api/worlds/${encodeURIComponent(worldId)}`, { token }),
  createWorld: (name: string, description: string, token: string) =>
    request<World>('/api/worlds', {
      method: 'POST',
      body: { name, description },
      token,
    }),
  createFaction: (
    worldId: string,
    faction: {
      name: string;
      alignment: string;
      power: number;
      wealth: number;
      manpower: number;
      influence: number;
      territory: number;
      description: string;
      imageUrl: string;
    },
    token: string,
  ) =>
    request<Faction>(`/api/worlds/${encodeURIComponent(worldId)}/factions`, {
      method: 'POST',
      body: faction,
      token,
    }),
  simulate: (worldId: string, token: string) =>
    request<unknown>(`/api/worlds/${encodeURIComponent(worldId)}/simulate`, {
      method: 'POST',
      token,
    }),
};
