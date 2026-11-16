const base = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export async function api<T>(path: string, options: RequestInit = {}) {
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const response = await fetch(base + path, { ...options, headers: { ...headers, ...(options.headers as Record<string, string> || {}) } });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || 'Request failed');
  }
  return response.json() as Promise<T>;
}
