import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { api, ApiError, Faction, SimulationLog, WorldEvent, WorldSnapshot } from './api';
import { loadPreferences } from './preferences';

interface DashboardProps {
  token: string;
}

interface ChronicleEntry {
  id: string;
  title: string;
  category: string;
  description: string;
  occurredAt: string;
  deltas: Array<{ label: string; value: number; suffix?: string }>;
}

const panelClass =
  'rounded-2xl border border-slate-800/90 bg-slate-900/75 shadow-xl shadow-black/15 backdrop-blur-md';
const inputClass =
  'mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-slate-100 outline-none transition placeholder:text-slate-600 focus:border-amber-400 focus:ring-2 focus:ring-amber-400/15';

function formatTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Time unknown';
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date);
}

function eventDeltas(event: WorldEvent): ChronicleEntry['deltas'] {
  return [
    { label: 'Power', value: event.powerDelta },
    { label: 'Wealth', value: event.wealthDelta },
    { label: 'Manpower', value: event.manpowerDelta },
    { label: 'Influence', value: event.influenceDelta },
    { label: 'Territory', value: event.territoryDelta, suffix: '%' },
  ].filter((delta) => delta.value !== 0);
}

function makeChronicle(snapshot: WorldSnapshot): ChronicleEntry[] {
  const eventEntries = snapshot.events.map((event) => ({
    id: `event-${event.id}`,
    title: event.title,
    category: event.category || 'World event',
    description: event.details || event.type.replace(/_/g, ' ').toLowerCase(),
    occurredAt: event.occurredAt,
    deltas: eventDeltas(event),
  }));
  const unmatchedLogs = snapshot.logs
    .filter((log) => !snapshot.events.some((event) => log.message.startsWith(event.title)))
    .map((log: SimulationLog) => ({
      id: `log-${log.id}`,
      title: 'Chronicle entry',
      category: 'World log',
      description: log.message,
      occurredAt: log.createdAt,
      deltas: [],
    }));

  return [...eventEntries, ...unmatchedLogs].sort(
    (first, second) =>
      new Date(second.occurredAt).getTime() - new Date(first.occurredAt).getTime(),
  );
}

function StatBar({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div>
      <div className="mb-1.5 flex items-center justify-between text-[11px] uppercase tracking-[0.12em] text-slate-500">
        <span>{label}</span>
        <span className="font-semibold tabular-nums text-slate-300">{value}%</span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-slate-800">
        <div
          className={`h-full rounded-full ${color}`}
          style={{ width: `${Math.min(100, Math.max(0, value))}%` }}
        />
      </div>
    </div>
  );
}

function FactionCard({
  faction,
  showDescription,
  showPower,
  compact,
  onSelect,
}: {
  faction: Faction;
  showDescription: boolean;
  showPower: boolean;
  compact: boolean;
  onSelect: () => void;
}) {
  const [imageFailed, setImageFailed] = useState(false);
  useEffect(() => setImageFailed(false), [faction.imageUrl]);

  return (
    <button
      className={`group overflow-hidden rounded-2xl border border-slate-800 bg-slate-950/75 text-left transition duration-200 hover:-translate-y-0.5 hover:border-amber-500/40 hover:shadow-lg hover:shadow-amber-950/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amber-400 ${
        compact ? 'p-3' : 'p-4'
      }`}
      onClick={onSelect}
      type="button"
    >
      <div className="relative -mx-4 -mt-4 mb-4 h-20 overflow-hidden border-b border-slate-800 bg-[radial-gradient(ellipse_at_20%_20%,rgba(180,120,33,0.2),transparent_60%),linear-gradient(120deg,#1e293b,#111827)]">
        {faction.imageUrl && !imageFailed ? (
          <img
            alt=""
            className="h-full w-full object-cover opacity-65 transition duration-300 group-hover:scale-105"
            onError={() => setImageFailed(true)}
            src={faction.imageUrl}
          />
        ) : (
          <div className="grid h-full place-items-center font-serif text-4xl text-amber-300/70">
            ✦
          </div>
        )}
        <span className="absolute bottom-2 right-3 rounded-full border border-slate-700/80 bg-slate-950/80 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.15em] text-slate-300">
          {faction.alignment || 'Unaligned'}
        </span>
      </div>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="truncate font-serif text-lg text-amber-100">{faction.name}</h3>
          {showDescription && faction.description && (
            <p className="mt-1 line-clamp-2 min-h-10 text-xs leading-5 text-slate-500">
              {faction.description}
            </p>
          )}
        </div>
        <span className="rounded-lg border border-violet-400/15 bg-violet-400/5 px-2 py-1 text-[10px] font-semibold text-violet-300">
          {faction.influence} INF
        </span>
      </div>
      <div className="mt-4 space-y-3">
        <StatBar color="bg-emerald-500" label="Territory" value={faction.territory} />
        {showPower && <StatBar color="bg-violet-500" label="Power" value={faction.power} />}
      </div>
      <div className="mt-4 flex flex-wrap gap-2">
        <span className="rounded-lg border border-amber-400/15 bg-amber-400/5 px-2.5 py-1.5 text-xs text-amber-200">
          Wealth <strong className="ml-1 tabular-nums">{faction.wealth}</strong>
        </span>
        <span className="rounded-lg border border-emerald-400/15 bg-emerald-400/5 px-2.5 py-1.5 text-xs text-emerald-200">
          Manpower <strong className="ml-1 tabular-nums">{faction.manpower}</strong>
        </span>
      </div>
    </button>
  );
}

function EventLog({ entries, limit }: { entries: ChronicleEntry[]; limit: number }) {
  return (
    <section className={`${panelClass} flex min-h-0 flex-col`}>
      <header className="flex flex-none items-center justify-between border-b border-slate-800 px-5 py-4">
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-violet-300">
            World chronicle
          </p>
          <h2 className="mt-1 font-serif text-lg text-slate-100">Recent events</h2>
        </div>
        <span className="rounded-full border border-slate-700 bg-slate-950/70 px-2.5 py-1 text-xs tabular-nums text-slate-400">
          {entries.length}
        </span>
      </header>
      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4">
        {entries.length === 0 ? (
          <div className="grid min-h-40 place-items-center rounded-xl border border-dashed border-slate-800 px-6 text-center">
            <p className="text-sm leading-6 text-slate-500">
              The chronicle is quiet. Events will appear here as your world changes.
            </p>
          </div>
        ) : (
          entries.slice(0, limit).map((entry) => (
            <article
              className="rounded-xl border border-slate-800/90 bg-slate-950/55 p-3.5"
              key={entry.id}
            >
              <div className="flex items-start justify-between gap-3">
                <span
                  className={`rounded-md px-2 py-1 text-[9px] font-bold uppercase tracking-[0.13em] ${
                    /war|battle|conflict/i.test(entry.category)
                      ? 'bg-rose-500/10 text-rose-300'
                      : /global|arcane|magic/i.test(entry.category)
                        ? 'bg-violet-500/10 text-violet-300'
                        : 'bg-emerald-500/10 text-emerald-300'
                  }`}
                >
                  {entry.category}
                </span>
                <time className="shrink-0 pt-1 text-[10px] text-slate-600">
                  {formatTime(entry.occurredAt)}
                </time>
              </div>
              <h3 className="mt-3 text-sm font-semibold text-slate-200">{entry.title}</h3>
              <p className="mt-1 line-clamp-3 text-xs leading-5 text-slate-500">
                {entry.description}
              </p>
              {entry.deltas.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {entry.deltas.map((delta) => (
                    <span
                      className={`rounded-md border px-2 py-1 text-[10px] font-semibold tabular-nums ${
                        delta.value > 0
                          ? 'border-emerald-500/15 bg-emerald-500/5 text-emerald-300'
                          : 'border-rose-500/15 bg-rose-500/5 text-rose-300'
                      }`}
                      key={delta.label}
                    >
                      {delta.value > 0 ? '+' : ''}
                      {delta.value}
                      {delta.suffix ?? ''} {delta.label}
                    </span>
                  ))}
                </div>
              )}
            </article>
          ))
        )}
      </div>
    </section>
  );
}

function Modal({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  return (
    <div
      className="fixed inset-0 z-30 grid place-items-center overflow-y-auto bg-black/70 p-4 backdrop-blur-sm"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        aria-labelledby="modal-title"
        aria-modal="true"
        className="w-full max-w-lg rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl shadow-black/50"
        role="dialog"
      >
        <div className="mb-5 flex items-center justify-between gap-4">
          <h2 className="font-serif text-xl text-amber-100" id="modal-title">
            {title}
          </h2>
          <button
            aria-label="Close dialog"
            className="rounded-lg px-2 py-1 text-slate-500 hover:bg-slate-800 hover:text-slate-100"
            onClick={onClose}
            type="button"
          >
            ✕
          </button>
        </div>
        {children}
      </section>
    </div>
  );
}

export default function Dashboard({ token }: DashboardProps) {
  const [worlds, setWorlds] = useState<WorldSnapshot['world'][]>([]);
  const [worldId, setWorldId] = useState('');
  const [snapshot, setSnapshot] = useState<WorldSnapshot | null>(null);
  const [selectedFaction, setSelectedFaction] = useState<Faction | null>(null);
  const [modal, setModal] = useState<'world' | 'faction' | null>(null);
  const [loadingWorlds, setLoadingWorlds] = useState(true);
  const [loadingSnapshot, setLoadingSnapshot] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);
  const preferences = useMemo(() => loadPreferences(), []);

  useEffect(() => {
    let active = true;
    setLoadingWorlds(true);
    api
      .listWorlds(token)
      .then((availableWorlds) => {
        if (!active) return;
        setWorlds(availableWorlds);
        setWorldId((current) =>
          availableWorlds.some((world) => world.id === current)
            ? current
            : (availableWorlds[0]?.id ?? ''),
        );
        setError('');
      })
      .catch((reason: unknown) => {
        if (!active) return;
        setError(
          reason instanceof Error ? reason.message : 'Could not load your worlds.',
        );
      })
      .finally(() => {
        if (active) setLoadingWorlds(false);
      });
    return () => {
      active = false;
    };
  }, [token]);

  const loadSnapshot = useCallback(async () => {
    if (!worldId) {
      setSnapshot(null);
      return;
    }
    setLoadingSnapshot(true);
    try {
      const current = await api.getWorld(worldId, token);
      setSnapshot(current);
      setError('');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not load this world.');
    } finally {
      setLoadingSnapshot(false);
    }
  }, [token, worldId, refreshKey]);

  useEffect(() => {
    void loadSnapshot();
  }, [loadSnapshot]);

  const chronicle = useMemo(
    () => (snapshot ? makeChronicle(snapshot) : []),
    [snapshot],
  );

  async function createWorld(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusy(true);
    setError('');
    try {
      const world = await api.createWorld(
        String(form.get('name')).trim(),
        String(form.get('description')).trim(),
        token,
      );
      setWorlds((current) => [world, ...current]);
      setWorldId(world.id);
      setModal(null);
      setNotice('Your world is ready. Add a faction to begin its chronicle.');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not create the world.');
    } finally {
      setBusy(false);
    }
  }

  async function createFaction(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!worldId) return;
    const form = new FormData(event.currentTarget);
    setBusy(true);
    setError('');
    try {
      await api.createFaction(
        worldId,
        {
          name: String(form.get('name')).trim(),
          alignment: String(form.get('alignment')),
          power: 50,
          wealth: 50,
          manpower: 50,
          influence: 50,
          territory: 10,
          description: String(form.get('description')).trim(),
          imageUrl: String(form.get('imageUrl')).trim(),
        },
        token,
      );
      setModal(null);
      setNotice('Faction added to the world.');
      setRefreshKey((key) => key + 1);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not create the faction.');
    } finally {
      setBusy(false);
    }
  }

  async function triggerTurn() {
    if (!worldId) return;
    if (
      preferences.confirmRandomTurn &&
      !window.confirm(`Trigger a random turn in ${snapshot?.world.name ?? 'this world'}?`)
    ) {
      return;
    }
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await api.simulate(worldId, token);
      setNotice('A new turn has been recorded in the chronicle.');
      setRefreshKey((key) => key + 1);
    } catch (reason) {
      setError(reason instanceof ApiError || reason instanceof Error
        ? reason.message
        : 'Could not simulate a turn.');
    } finally {
      setBusy(false);
    }
  }

  const factionEvents = selectedFaction && snapshot
    ? snapshot.events.filter(
        (event) =>
          event.targetFactionId === selectedFaction.id ||
          event.secondTargetFactionId === selectedFaction.id ||
          (!event.targetFactionId && !event.secondTargetFactionId),
      )
    : [];

  return (
    <div className="flex h-full min-h-0 flex-col gap-3">
      <section className={`${panelClass} flex flex-none flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-5`}>
        <div className="min-w-0 flex-1">
          {worlds.length > 0 ? (
            <select
              aria-label="Active world"
              className="max-w-full bg-transparent font-serif text-xl text-amber-100 outline-none sm:text-2xl"
              onChange={(event) => {
                setWorldId(event.target.value);
                setNotice('');
              }}
              value={worldId}
            >
              {worlds.map((world) => (
                <option className="bg-slate-900" key={world.id} value={world.id}>
                  {world.name}
                </option>
              ))}
            </select>
          ) : (
            <h1 className="font-serif text-xl text-amber-100 sm:text-2xl">Your first realm</h1>
          )}
          <p className="mt-1 line-clamp-1 text-xs text-slate-500">
            {snapshot?.world.description || 'A new chronicle is waiting to be written.'}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            className="rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2 text-xs font-semibold text-slate-300 transition hover:border-slate-500 hover:text-white"
            onClick={() => setModal('world')}
            type="button"
          >
            + New world
          </button>
          <button
            className="rounded-xl bg-amber-400 px-3.5 py-2 text-xs font-bold text-slate-950 shadow-md shadow-amber-950/20 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-50"
            disabled={!worldId || busy || (snapshot?.factions.length ?? 0) === 0}
            onClick={() => void triggerTurn()}
            type="button"
          >
            {busy ? 'Working…' : 'Trigger random turn'}
          </button>
          <button
            className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-xs font-semibold text-emerald-200 transition hover:bg-emerald-500/15 disabled:opacity-50"
            disabled={!worldId}
            onClick={() => setModal('faction')}
            type="button"
          >
            + Add faction
          </button>
        </div>
      </section>

      {error && (
        <div
          className="flex flex-none items-center justify-between gap-3 rounded-xl border border-rose-500/25 bg-rose-500/10 px-4 py-2.5 text-sm text-rose-200"
          role="alert"
        >
          <span>{error}</span>
          <button
            className="text-xs underline decoration-rose-300/40 underline-offset-2"
            onClick={() => setError('')}
            type="button"
          >
            Dismiss
          </button>
        </div>
      )}
      {notice && (
        <div
          className="flex flex-none items-center justify-between gap-3 rounded-xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-2.5 text-sm text-emerald-200"
          role="status"
        >
          <span>{notice}</span>
          <button
            className="text-xs text-emerald-300/70 hover:text-emerald-200"
            onClick={() => setNotice('')}
            type="button"
          >
            Dismiss
          </button>
        </div>
      )}

      <div className="grid min-h-0 flex-1 grid-cols-1 grid-rows-[minmax(0,1.15fr)_minmax(0,0.85fr)] gap-3 overflow-hidden lg:grid-cols-[minmax(0,1.8fr)_minmax(18rem,0.88fr)] lg:grid-rows-1">
        <section className={`${panelClass} flex min-h-0 flex-col`}>
          <header className="flex flex-none items-center justify-between border-b border-slate-800 px-5 py-4">
            <div>
              <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-emerald-300">
                Realm powers
              </p>
              <h2 className="mt-1 font-serif text-lg text-slate-100">Factions</h2>
            </div>
            <span className="rounded-full border border-slate-700 bg-slate-950/70 px-2.5 py-1 text-xs tabular-nums text-slate-400">
              {snapshot?.factions.length ?? 0}
            </span>
          </header>
          <div className="min-h-0 flex-1 overflow-y-auto p-4">
            {loadingWorlds || loadingSnapshot ? (
              <div className="grid h-full min-h-36 place-items-center text-sm text-slate-500">
                Loading world…
              </div>
            ) : !snapshot ? (
              <div className="grid h-full min-h-36 place-items-center rounded-xl border border-dashed border-slate-800 px-6 text-center">
                <div>
                  <p className="font-serif text-lg text-slate-200">No worlds yet</p>
                  <p className="mt-2 text-sm text-slate-500">
                    Create a world to gather factions and begin its chronicle.
                  </p>
                  <button
                    className="mt-4 rounded-lg bg-amber-400 px-4 py-2 text-xs font-bold text-slate-950"
                    onClick={() => setModal('world')}
                    type="button"
                  >
                    Create your first world
                  </button>
                </div>
              </div>
            ) : snapshot.factions.length === 0 ? (
              <div className="grid h-full min-h-36 place-items-center rounded-xl border border-dashed border-slate-800 px-6 text-center">
                <div>
                  <p className="font-serif text-lg text-slate-200">A quiet beginning</p>
                  <p className="mt-2 text-sm text-slate-500">
                    Add your first faction before simulating a turn.
                  </p>
                  <button
                    className="mt-4 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-xs font-semibold text-emerald-200"
                    onClick={() => setModal('faction')}
                    type="button"
                  >
                    Add a faction
                  </button>
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 2xl:grid-cols-3">
                {snapshot.factions.map((faction) => (
                  <FactionCard
                    compact={preferences.cardDensity === 'compact'}
                    faction={faction}
                    key={faction.id}
                    onSelect={() => setSelectedFaction(faction)}
                    showDescription={preferences.showFactionDescriptions}
                    showPower={preferences.showPower}
                  />
                ))}
              </div>
            )}
          </div>
        </section>

        <EventLog entries={chronicle} limit={preferences.eventLimit} />
      </div>

      {modal === 'world' && (
        <Modal onClose={() => setModal(null)} title="Create a world">
          <form className="space-y-4" onSubmit={(event) => void createWorld(event)}>
            <label className="block text-sm text-slate-300">
              World name
              <input
                autoFocus
                className={inputClass}
                maxLength={120}
                name="name"
                placeholder="The Ashen Marches"
                required
              />
            </label>
            <label className="block text-sm text-slate-300">
              Description
              <textarea
                className={`${inputClass} min-h-24 resize-y`}
                maxLength={1000}
                name="description"
                placeholder="A short introduction to this realm…"
              />
            </label>
            <button
              className="w-full rounded-xl bg-amber-400 px-4 py-3 text-sm font-bold text-slate-950 disabled:opacity-60"
              disabled={busy}
              type="submit"
            >
              {busy ? 'Creating…' : 'Create world'}
            </button>
          </form>
        </Modal>
      )}

      {modal === 'faction' && (
        <Modal onClose={() => setModal(null)} title="Add a faction">
          <form className="space-y-4" onSubmit={(event) => void createFaction(event)}>
            <label className="block text-sm text-slate-300">
              Faction name
              <input
                autoFocus
                className={inputClass}
                maxLength={120}
                name="name"
                placeholder="House Veyr"
                required
              />
            </label>
            <label className="block text-sm text-slate-300">
              Alignment
              <select className={inputClass} defaultValue="Neutral" name="alignment">
                <option className="bg-slate-900">Neutral</option>
                <option className="bg-slate-900">Order</option>
                <option className="bg-slate-900">Rebel</option>
                <option className="bg-slate-900">Arcane</option>
              </select>
            </label>
            <label className="block text-sm text-slate-300">
              Crest image URL <span className="text-slate-600">(optional)</span>
              <input className={inputClass} maxLength={500} name="imageUrl" type="url" />
            </label>
            <label className="block text-sm text-slate-300">
              Lore
              <textarea
                className={`${inputClass} min-h-20 resize-y`}
                maxLength={1000}
                name="description"
                placeholder="What should the world remember about them?"
              />
            </label>
            <p className="text-xs leading-5 text-slate-500">
              New factions begin with balanced core stats. You can shape them through world
              events.
            </p>
            <button
              className="w-full rounded-xl bg-emerald-500 px-4 py-3 text-sm font-bold text-slate-950 disabled:opacity-60"
              disabled={busy}
              type="submit"
            >
              {busy ? 'Adding…' : 'Add faction'}
            </button>
          </form>
        </Modal>
      )}

      {selectedFaction && (
        <Modal onClose={() => setSelectedFaction(null)} title={selectedFaction.name}>
          <div className="space-y-5">
            <div className="flex flex-wrap gap-2">
              <span className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-300">
                {selectedFaction.alignment || 'Unaligned'}
              </span>
              <span className="rounded-lg border border-amber-400/15 bg-amber-400/5 px-3 py-2 text-xs text-amber-200">
                Wealth {selectedFaction.wealth}
              </span>
              <span className="rounded-lg border border-emerald-400/15 bg-emerald-400/5 px-3 py-2 text-xs text-emerald-200">
                Manpower {selectedFaction.manpower}
              </span>
              <span className="rounded-lg border border-violet-400/15 bg-violet-400/5 px-3 py-2 text-xs text-violet-200">
                Influence {selectedFaction.influence}
              </span>
            </div>
            <div className="space-y-3">
              <StatBar color="bg-emerald-500" label="Territory" value={selectedFaction.territory} />
              <StatBar color="bg-violet-500" label="Power" value={selectedFaction.power} />
            </div>
            <p className="text-sm leading-6 text-slate-400">
              {selectedFaction.description || 'No lore has been recorded for this faction yet.'}
            </p>
            <div>
              <h3 className="mb-3 text-xs font-bold uppercase tracking-[0.15em] text-slate-500">
                Events affecting this faction
              </h3>
              <div className="max-h-48 space-y-2 overflow-y-auto">
                {factionEvents.length === 0 ? (
                  <p className="text-sm text-slate-500">No related events yet.</p>
                ) : (
                  factionEvents.map((event) => (
                    <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3" key={event.id}>
                      <p className="text-sm font-medium text-slate-200">{event.title}</p>
                      <p className="mt-1 text-xs text-slate-600">{formatTime(event.occurredAt)}</p>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
