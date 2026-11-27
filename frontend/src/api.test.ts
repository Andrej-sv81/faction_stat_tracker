import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from './api';

const fetchMock = vi.fn<typeof fetch>();

function jsonResponse(value: unknown, status = 200): Response {
  return new Response(JSON.stringify(value), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function csrfResponse(): Response {
  return jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' });
}

describe('frontend API client', () => {
  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('submits login credentials with cookies and returns an access-token pair', async () => {
    const tokens = {
      accessToken: 'access-value',
      tokenType: 'Bearer',
      expiresIn: 900,
      email: 'keeper@example.test',
    };
    fetchMock.mockResolvedValueOnce(jsonResponse(tokens));

    await expect(api.login('keeper@example.test', 'a-long-password')).resolves.toEqual(tokens);

    const [url, options] = fetchMock.mock.calls[0];
    expect(String(url)).toBe('http://localhost:8080/api/auth/login');
    expect(options?.method).toBe('POST');
    expect(options?.credentials).toBe('include');
    expect(JSON.parse(String(options?.body))).toEqual({
      email: 'keeper@example.test',
      password: 'a-long-password',
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('fetches a CSRF token before changing a world and sends it with the bearer token', async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse({ id: 'world-1', name: 'Ashen Marches' }, 201));

    await api.createWorld('Ashen Marches', 'A cold frontier', 'access-value');

    const [csrfUrl, csrfOptions] = fetchMock.mock.calls[0];
    expect(String(csrfUrl)).toBe('http://localhost:8080/api/auth/csrf');
    expect(csrfOptions?.credentials).toBe('include');

    const [worldUrl, worldOptions] = fetchMock.mock.calls[1];
    const headers = new Headers(worldOptions?.headers);
    expect(String(worldUrl)).toBe('http://localhost:8080/api/worlds');
    expect(worldOptions?.method).toBe('POST');
    expect(headers.get('Authorization')).toBe('Bearer access-value');
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-value');
    expect(JSON.parse(String(worldOptions?.body))).toEqual({
      name: 'Ashen Marches',
      description: 'A cold frontier',
    });
  });

  it('loads periodic simulation status and sends CSRF-protected start and stop requests', async () => {
    const status = {
      running: true,
      intervalMinutes: 5,
      severity: 3,
      lastError: null,
    };
    fetchMock
      .mockResolvedValueOnce(jsonResponse(status))
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse(status))
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse({ ...status, running: false }));

    await expect(api.simulationStatus('world/1', 'access-value')).resolves.toEqual(status);
    await api.startSimulation('world/1', 5, 3, 'access-value');
    await api.stopSimulation('world/1', 'access-value');

    const [statusUrl, statusOptions] = fetchMock.mock.calls[0];
    expect(String(statusUrl)).toBe('http://localhost:8080/api/worlds/world%2F1/simulation/status');
    expect(statusOptions?.method).toBeUndefined();
    expect(new Headers(statusOptions?.headers).get('Authorization')).toBe('Bearer access-value');

    const [startUrl, startOptions] = fetchMock.mock.calls[2];
    expect(String(startUrl)).toBe('http://localhost:8080/api/worlds/world%2F1/simulation/start');
    expect(startOptions?.method).toBe('POST');
    expect(JSON.parse(String(startOptions?.body))).toEqual({
      intervalMinutes: 5,
      severity: 3,
    });
    expect(new Headers(startOptions?.headers).get('X-XSRF-TOKEN')).toBe('csrf-value');

    const [stopUrl, stopOptions] = fetchMock.mock.calls[4];
    expect(String(stopUrl)).toBe('http://localhost:8080/api/worlds/world%2F1/simulation/stop');
    expect(stopOptions?.method).toBe('POST');
    expect(new Headers(stopOptions?.headers).get('X-XSRF-TOKEN')).toBe('csrf-value');
  });

  it('accepts the bodyless 202 response from a password recovery request', async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 202 }));

    await expect(api.requestPasswordReset('keeper@example.test')).resolves.toBeUndefined();
  });

  it('preserves the server error message and HTTP status', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ error: 'Invalid credentials' }, 401));

    await expect(api.login('keeper@example.test', 'wrong-password')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Invalid credentials',
      status: 401,
    } satisfies Partial<ApiError>);
  });
});
