export interface Preferences {
  cardDensity: 'comfortable' | 'compact';
  showFactionDescriptions: boolean;
  showPower: boolean;
  eventLimit: 10 | 25 | 50;
  confirmRandomTurn: boolean;
}

export const defaultPreferences: Preferences = {
  cardDensity: 'comfortable',
  showFactionDescriptions: true,
  showPower: true,
  eventLimit: 25,
  confirmRandomTurn: true,
};

const storageKey = 'faction-tracker-preferences';

export function loadPreferences(): Preferences {
  try {
    const saved: unknown = JSON.parse(localStorage.getItem(storageKey) ?? 'null');
    if (typeof saved !== 'object' || saved === null) return defaultPreferences;
    const value = saved as Partial<Preferences>;
    return {
      cardDensity: value.cardDensity === 'compact' ? 'compact' : 'comfortable',
      showFactionDescriptions: value.showFactionDescriptions !== false,
      showPower: value.showPower !== false,
      eventLimit: [10, 25, 50].includes(value.eventLimit ?? 25)
        ? (value.eventLimit as Preferences['eventLimit'])
        : 25,
      confirmRandomTurn: value.confirmRandomTurn !== false,
    };
  } catch {
    return defaultPreferences;
  }
}

export function savePreferences(preferences: Preferences): void {
  localStorage.setItem(storageKey, JSON.stringify(preferences));
}
