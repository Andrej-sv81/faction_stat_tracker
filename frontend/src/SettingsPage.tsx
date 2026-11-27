import { useState } from 'react';
import {
  defaultPreferences,
  loadPreferences,
  Preferences,
  savePreferences,
} from './preferences';

const panelClass =
  'rounded-2xl border border-slate-800 bg-slate-900/70 p-5 shadow-xl shadow-black/10 backdrop-blur-md';

interface SettingsPageProps {
  email: string;
}

export default function SettingsPage({ email }: SettingsPageProps) {
  const [preferences, setPreferences] = useState<Preferences>(loadPreferences);
  const [savedNotice, setSavedNotice] = useState('');

  function update<K extends keyof Preferences>(key: K, value: Preferences[K]) {
    const updated = { ...preferences, [key]: value };
    setPreferences(updated);
    try {
      savePreferences(updated);
      setSavedNotice('Preferences saved on this device.');
    } catch {
      setSavedNotice('Could not save preferences in this browser.');
    }
  }

  function reset() {
    setPreferences(defaultPreferences);
    try {
      savePreferences(defaultPreferences);
      setSavedNotice('Preferences restored and saved.');
    } catch {
      setSavedNotice('Could not save preferences in this browser.');
    }
  }

  return (
    <div className="mx-auto h-full min-h-0 max-w-4xl overflow-y-auto px-1 pb-8">
      <header className="mb-6">
        <p className="text-[10px] font-semibold uppercase tracking-[0.24em] text-violet-300">
          Chronicle preferences
        </p>
        <h1 className="mt-2 font-serif text-3xl text-slate-100">Settings</h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">
          Adjust how your world is presented. These preferences are saved in this browser and
          can grow into account-wide settings later.
        </p>
      </header>

      <div className="space-y-4">
        <section className={panelClass}>
          <div className="flex items-start gap-4">
            <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-amber-400/20 bg-amber-400/5 text-amber-300">
              ◉
            </span>
            <div className="min-w-0">
              <h2 className="font-serif text-lg text-amber-100">Realm keeper</h2>
              <p className="mt-1 break-all text-sm text-slate-400">{email}</p>
              <p className="mt-2 text-xs text-slate-600">
                Account management and security controls are handled by your sign-in session.
              </p>
            </div>
          </div>
        </section>

        <section className={panelClass}>
          <div className="mb-5">
            <h2 className="font-serif text-lg text-slate-100">Dashboard display</h2>
            <p className="mt-1 text-xs text-slate-500">
              Choose how much detail appears in the world view.
            </p>
          </div>
          <div className="divide-y divide-slate-800">
            <label className="flex cursor-pointer items-center justify-between gap-5 py-4">
              <span>
                <span className="block text-sm font-medium text-slate-200">Faction card spacing</span>
                <span className="mt-1 block text-xs text-slate-500">
                  Compact cards leave more room for large faction rosters.
                </span>
              </span>
              <select
                className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-200 outline-none focus:border-amber-400"
                onChange={(event) =>
                  update('cardDensity', event.target.value as Preferences['cardDensity'])
                }
                value={preferences.cardDensity}
              >
                <option value="comfortable">Comfortable</option>
                <option value="compact">Compact</option>
              </select>
            </label>

            <PreferenceToggle
              checked={preferences.showFactionDescriptions}
              description="Show each faction's short lore excerpt on its card."
              label="Faction descriptions"
              onChange={(checked) => update('showFactionDescriptions', checked)}
            />
            <PreferenceToggle
              checked={preferences.showPower}
              description="Show the power stat bar on faction cards."
              label="Power stat"
              onChange={(checked) => update('showPower', checked)}
            />
            <PreferenceToggle
              checked={preferences.confirmRandomTurn}
              description="Confirm before applying a random turn to the active world."
              label="Confirm random turns"
              onChange={(checked) => update('confirmRandomTurn', checked)}
            />

            <label className="flex cursor-pointer items-center justify-between gap-5 py-4">
              <span>
                <span className="block text-sm font-medium text-slate-200">Chronicle entries</span>
                <span className="mt-1 block text-xs text-slate-500">
                  Maximum number of recent entries shown in the side panel.
                </span>
              </span>
              <select
                className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-200 outline-none focus:border-amber-400"
                onChange={(event) =>
                  update('eventLimit', Number(event.target.value) as Preferences['eventLimit'])
                }
                value={preferences.eventLimit}
              >
                <option value={10}>10 entries</option>
                <option value={25}>25 entries</option>
                <option value={50}>50 entries</option>
              </select>
            </label>
          </div>
        </section>

        <div className="flex flex-wrap items-center justify-between gap-3 px-1">
          <p
            aria-live="polite"
            className={`text-xs ${
              savedNotice.startsWith('Could not') ? 'text-rose-300' : 'text-emerald-300'
            }`}
          >
            {savedNotice || 'Changes save automatically.'}
          </p>
          <button
            className="rounded-lg border border-slate-700 px-3 py-2 text-xs font-semibold text-slate-400 transition hover:border-amber-400/40 hover:text-amber-200"
            onClick={reset}
            type="button"
          >
            Restore defaults
          </button>
        </div>
      </div>
    </div>
  );
}

function PreferenceToggle({
  checked,
  description,
  label,
  onChange,
}: {
  checked: boolean;
  description: string;
  label: string;
  onChange: (checked: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-center justify-between gap-5 py-4">
      <span>
        <span className="block text-sm font-medium text-slate-200">{label}</span>
        <span className="mt-1 block text-xs text-slate-500">{description}</span>
      </span>
      <input
        checked={checked}
        className="h-4 w-4 accent-amber-400"
        onChange={(event) => onChange(event.target.checked)}
        type="checkbox"
      />
    </label>
  );
}
