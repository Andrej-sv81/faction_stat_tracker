import { FormEvent, useState } from 'react';
import { api, ApiError, TokenPair } from './api';

type AuthMode = 'login' | 'register' | 'request-reset' | 'reset-password';

interface AuthScreenProps {
  initialMode: AuthMode;
  resetToken: string;
  notice?: string;
  onAuthenticated: (tokens: TokenPair) => void;
}

const fieldClass =
  'mt-2 w-full rounded-xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-slate-100 outline-none transition placeholder:text-slate-600 focus:border-amber-400 focus:ring-2 focus:ring-amber-400/15';

export default function AuthScreen({
  initialMode,
  resetToken,
  notice,
  onAuthenticated,
}: AuthScreenProps) {
  const [mode, setMode] = useState<AuthMode>(initialMode);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [token, setToken] = useState(resetToken);
  const [message, setMessage] = useState(notice ?? '');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const isPasswordForm = mode === 'login' || mode === 'register';

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setMessage('');
    setBusy(true);
    try {
      if (mode === 'login') {
        onAuthenticated(await api.login(email, password));
      } else if (mode === 'register') {
        onAuthenticated(await api.register(email, password));
      } else if (mode === 'request-reset') {
        await api.requestPasswordReset(email);
        setMessage(
          'If an account exists for that address, password reset instructions are on their way.',
        );
      } else {
        if (password !== confirmation) {
          setError('The passwords do not match.');
          return;
        }
        await api.confirmPasswordReset(token, password);
        setPassword('');
        setConfirmation('');
        setMode('login');
        setMessage('Your password has been updated. Sign in with your new password.');
      }
    } catch (reason) {
      setError(
        reason instanceof ApiError || reason instanceof Error
          ? reason.message
          : 'Something went wrong. Please try again.',
      );
    } finally {
      setBusy(false);
    }
  }

  function changeMode(nextMode: AuthMode) {
    setMode(nextMode);
    setError('');
    setMessage('');
  }

  const title =
    mode === 'register'
      ? 'Create your account'
      : mode === 'request-reset'
        ? 'Recover your account'
        : mode === 'reset-password'
          ? 'Choose a new password'
          : 'Return to your world';

  return (
    <main className="relative flex min-h-[100dvh] items-center justify-center overflow-hidden bg-[#090d16] px-4 py-8 text-slate-100">
      <div className="pointer-events-none absolute -left-28 top-[-7rem] h-96 w-96 rounded-full bg-amber-500/10 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-40 right-[-5rem] h-[30rem] w-[30rem] rounded-full bg-violet-700/10 blur-3xl" />
      <div className="relative grid w-full max-w-5xl overflow-hidden rounded-[2rem] border border-slate-800 bg-slate-900/70 shadow-2xl shadow-black/50 backdrop-blur-xl md:grid-cols-[1.05fr_0.95fr]">
        <section className="hidden flex-col justify-between border-r border-slate-800 bg-[radial-gradient(ellipse_at_25%_15%,rgba(180,120,33,0.17),transparent_44%),linear-gradient(145deg,rgba(30,41,59,0.78),rgba(9,13,22,0.9))] p-10 md:flex">
          <div>
            <div className="flex items-center gap-3">
              <span className="grid h-11 w-11 place-items-center rounded-2xl border border-amber-400/30 bg-amber-400/10 font-serif text-xl text-amber-300">
                F
              </span>
              <span className="font-serif text-lg tracking-wide text-amber-100">
                Faction Chronicle
              </span>
            </div>
            <p className="mt-24 max-w-sm font-serif text-4xl leading-tight text-slate-100">
              Every realm begins with a story.
            </p>
            <p className="mt-5 max-w-sm text-sm leading-7 text-slate-400">
              Shape rival powers, record turning points, and watch your world change one
              decision at a time.
            </p>
          </div>
          <p className="text-xs uppercase tracking-[0.24em] text-slate-600">
            A living atlas for imagined worlds
          </p>
        </section>

        <section className="p-6 sm:p-10">
          <div className="mb-8 flex items-center gap-3 md:hidden">
            <span className="grid h-10 w-10 place-items-center rounded-xl border border-amber-400/30 bg-amber-400/10 font-serif text-lg text-amber-300">
              F
            </span>
            <span className="font-serif tracking-wide text-amber-100">Faction Chronicle</span>
          </div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-amber-400">
            Realm keeper access
          </p>
          <h1 className="mt-3 font-serif text-3xl text-slate-50">{title}</h1>
          <p className="mt-2 text-sm leading-6 text-slate-400">
            {mode === 'register'
              ? 'Begin a new chronicle and keep your worlds in one place.'
              : mode === 'request-reset'
                ? 'Enter your email and we will send instructions if an account is found.'
                : mode === 'reset-password'
                  ? 'Use a strong password you have not used here before.'
                  : 'Sign in to continue building your world.'}
          </p>

          <form className="mt-8 space-y-5" onSubmit={submit}>
            {mode !== 'reset-password' && (
              <label className="block text-sm font-medium text-slate-300">
                Email address
                <input
                  autoComplete="email"
                  className={fieldClass}
                  maxLength={254}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                  type="email"
                  value={email}
                />
              </label>
            )}

            {isPasswordForm && (
              <label className="block text-sm font-medium text-slate-300">
                Password
                <input
                  autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                  className={fieldClass}
                  maxLength={100}
                  minLength={8}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                  type="password"
                  value={password}
                />
                {mode === 'register' && (
                  <span className="mt-2 block text-xs text-slate-500">
                    Use at least 8 characters.
                  </span>
                )}
              </label>
            )}

            {mode === 'reset-password' && (
              <>
                {!token && (
                  <label className="block text-sm font-medium text-slate-300">
                    Reset token
                    <input
                      autoComplete="one-time-code"
                      className={fieldClass}
                      onChange={(event) => setToken(event.target.value)}
                      required
                      value={token}
                    />
                  </label>
                )}
                <label className="block text-sm font-medium text-slate-300">
                  New password
                  <input
                    autoComplete="new-password"
                    className={fieldClass}
                    maxLength={100}
                    minLength={8}
                    onChange={(event) => setPassword(event.target.value)}
                    required
                    type="password"
                    value={password}
                  />
                </label>
                <label className="block text-sm font-medium text-slate-300">
                  Confirm new password
                  <input
                    autoComplete="new-password"
                    className={fieldClass}
                    maxLength={100}
                    minLength={8}
                    onChange={(event) => setConfirmation(event.target.value)}
                    required
                    type="password"
                    value={confirmation}
                  />
                </label>
              </>
            )}

            {error && (
              <p
                aria-live="polite"
                className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200"
                role="alert"
              >
                {error}
              </p>
            )}
            {message && (
              <p
                aria-live="polite"
                className="rounded-xl border border-emerald-500/25 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-200"
                role="status"
              >
                {message}
              </p>
            )}

            <button
              className="w-full rounded-xl bg-amber-400 px-5 py-3.5 text-sm font-bold text-slate-950 shadow-lg shadow-amber-950/20 transition hover:bg-amber-300 disabled:cursor-wait disabled:opacity-60"
              disabled={busy}
              type="submit"
            >
              {busy
                ? 'Please wait…'
                : mode === 'login'
                  ? 'Sign in'
                  : mode === 'register'
                    ? 'Create account'
                    : mode === 'request-reset'
                      ? 'Send recovery instructions'
                      : 'Update password'}
            </button>
          </form>

          <div className="mt-6 flex flex-wrap items-center justify-between gap-3 text-sm">
            {mode === 'login' ? (
              <>
                <button
                  className="text-slate-400 transition hover:text-amber-300"
                  onClick={() => changeMode('request-reset')}
                  type="button"
                >
                  Forgot password?
                </button>
                <button
                  className="font-medium text-amber-300 transition hover:text-amber-200"
                  onClick={() => changeMode('register')}
                  type="button"
                >
                  Create account
                </button>
              </>
            ) : (
              <button
                className="font-medium text-amber-300 transition hover:text-amber-200"
                onClick={() => changeMode('login')}
                type="button"
              >
                Back to sign in
              </button>
            )}
          </div>
        </section>
      </div>
    </main>
  );
}
