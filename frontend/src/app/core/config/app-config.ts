/**
 * Runtime configuration, loaded from /app-config.json before the app starts.
 * Keeping it outside the bundle means the same Docker image works locally,
 * on staging and on Azure: only the JSON file changes.
 */
export interface AppConfig {
  apiBaseUrl: string;
  keycloak: {
    url: string;
    realm: string;
    clientId: string;
  };
}

let current: AppConfig | null = null;

export async function loadAppConfig(): Promise<AppConfig> {
  const response = await fetch('/app-config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Could not load /app-config.json (HTTP ${response.status})`);
  }
  current = (await response.json()) as AppConfig;
  return current;
}

/** For unit tests: set the config without fetching it. */
export function setAppConfig(config: AppConfig): void {
  current = config;
}

export function appConfig(): AppConfig {
  if (!current) {
    throw new Error('App config requested before it was loaded');
  }
  return current;
}
