import { useEffect, useState } from 'react';
import AuthScreen from './AuthScreen';
import { api, ApiError, TokenPair } from './api';
import Dashboard from './Dashboard';
import SettingsPage from './SettingsPage';

interface Session extends TokenPair {
  expiresAt: number;
}

type Screen = 'dashboard' | 'settings';
type AuthMode = 'login' | 'reset-password';

function createSession(tokens: TokenPair): Session {
  return { ...tokens, expiresAt: Date.now() + tokens.expiresIn * 1000 };
}

function getResetToken(): string {
  return new URLSearchParams(window.location.hash.slice(1)).get('token') ?? '';
}

export default function App() {
  const isResetRoute = window.location.pathname.endsWith('/reset-password');
  const [session, setSession] = useState<Session | null>(null);
  const [booting, setBooting] = useState(!isResetRoute);
  const [bootError, setBootError] = useState('');
  const [authNotice, setAuthNotice] = useState('');
  const [screen, setScreen] = useState<Screen>('dashboard');
  const [logoutBusy, setLogoutBusy] = useState(false);

  useEffect(() => {
    if (isResetRoute) return;
    let active = true;
    api
      .refresh()
      .then((tokens) => {
        if (active) setSession(createSession(tokens));
      })
      .catch((reason: unknown) => {
        if (!active) return;
        if (reason instanceof ApiError && [401, 403].includes(reason.status)) {
          setSession(null);
        } else {
          setBootError(
            reason instanceof Error ? reason.message : 'The API could not be reached.',
          );
        }
      })
      .finally(() => {
        if (active) setBooting(false);
      });
    return () => {
      active = false;
    };
  }, [isResetRoute]);

  useEffect(() => {
    if (!session) return;
    const refreshDelay = Math.max(1000, session.expiresAt - Date.now() - 45_000);
    const timer = window.setTimeout(() => {
      api
        .refresh()
        .then((tokens) => setSession(createSession(tokens)))
        .catch(() => {
          setSession(null);
          setAuthNotice('Your session expired. Sign in again to continue.');
        });
    }, refreshDelay);
    return () => window.clearTimeout(timer);
  }, [session]);

  function handleAuthenticated(tokens: TokenPair) {
    setSession(createSession(tokens));
    setBootError('');
    setAuthNotice('');
    setScreen('dashboard');
  }

  async function logout() {
    if (!session) return;
    setLogoutBusy(true);
    setBootError('');
    try {
      await api.logout(session.accessToken);
      setSession(null);
      setAuthNotice('You have signed out.');
    } catch (reason) {
      setBootError(reason instanceof Error ? reason.message : 'Could not sign out.');
    } finally {
      setLogoutBusy(false);
    }
  }

  if (booting) {
    return (
      <main className="grid h-[100dvh] place-items-center bg-[#090d16] text-sm text-slate-400">
        <div className="text-center">
          <span className="mx-auto mb-4 grid h-12 w-12 animate-pulse place-items-center rounded-2xl border border-amber-400/20 bg-amber-400/5 font-serif text-xl text-amber-300">
            F
          </span>
          Restoring your chronicle…
        </div>
      </main>
    );
  }

  if (!session) {
    return (
      <>
        {bootError && (
          <div className="fixed inset-x-4 top-4 z-20 mx-auto max-w-xl rounded-xl border border-rose-500/30 bg-slate-950 px-4 py-3 text-center text-sm text-rose-200 shadow-xl">
            {bootError}
            <button
              className="ml-3 underline underline-offset-2"
              onClick={() => window.location.reload()}
              type="button"
            >
              Retry
            </button>
          </div>
        )}
        <AuthScreen
          initialMode={isResetRoute ? 'reset-password' : 'login'}
          notice={authNotice}
          onAuthenticated={handleAuthenticated}
          resetToken={getResetToken()}
        />
      </>
    );
  }

  return (
    <div className="flex h-[100dvh] min-h-0 flex-col overflow-hidden bg-[#090d16] text-slate-100">
      <header className="z-10 flex min-h-16 flex-none items-center justify-between gap-3 border-b border-slate-800/90 bg-slate-950/85 px-3 backdrop-blur-xl sm:px-6">
        <div className="flex min-w-0 items-center gap-2.5">
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-amber-400/25 bg-amber-400/5 font-serif text-lg text-amber-300">
            F
          </span>
          <span className="hidden truncate font-serif text-sm tracking-wide text-amber-100 sm:block">
            Faction Chronicle
          </span>
        </div>
        <nav aria-label="Main navigation" className="flex items-center gap-1.5">
          <button
            aria-pressed={screen === 'dashboard'}
            className={`rounded-lg px-2.5 py-2 text-xs font-semibold transition sm:px-3 ${
              screen === 'dashboard'
                ? 'bg-slate-800 text-amber-200'
                : 'text-slate-500 hover:bg-slate-900 hover:text-slate-200'
            }`}
            onClick={() => setScreen('dashboard')}
            type="button"
          >
            <span aria-hidden="true">◈ </span>
            <span className="hidden sm:inline">World</span>
          </button>
          <button
            aria-pressed={screen === 'settings'}
            className={`rounded-lg px-2.5 py-2 text-xs font-semibold transition sm:px-3 ${
              screen === 'settings'
                ? 'bg-slate-800 text-amber-200'
                : 'text-slate-500 hover:bg-slate-900 hover:text-slate-200'
            }`}
            onClick={() => setScreen('settings')}
            type="button"
          >
            <span aria-hidden="true">⚙ </span>
            <span className="hidden sm:inline">Settings</span>
          </button>
          <button
            className="ml-1 rounded-lg border border-slate-800 px-2.5 py-2 text-xs font-semibold text-slate-400 transition hover:border-rose-500/30 hover:text-rose-200 disabled:opacity-50 sm:px-3"
            disabled={logoutBusy}
            onClick={() => void logout()}
            type="button"
          >
            {logoutBusy ? '…' : <><span aria-hidden="true">↪ </span><span className="hidden sm:inline">Sign out</span></>}
          </button>
        </nav>
      </header>

      {bootError && (
        <div className="flex flex-none items-center justify-between gap-3 border-b border-rose-500/20 bg-rose-500/5 px-4 py-2 text-xs text-rose-200">
          <span>{bootError}</span>
          <button className="underline" onClick={() => setBootError('')} type="button">
            Dismiss
          </button>
        </div>
      )}

      <main className="min-h-0 flex-1 overflow-hidden p-3 sm:p-5">
        {screen === 'dashboard' ? (
          <Dashboard token={session.accessToken} />
        ) : (
          <SettingsPage email={session.email} />
        )}
      </main>
      <div className="pointer-events-none fixed bottom-[-10rem] left-1/3 h-72 w-72 rounded-full bg-amber-600/5 blur-3xl" />
    </div>
  );
}
