import { appConfig } from '../config/app-config';

/** Full URL of an API path, e.g. api('/rm/clients'). */
export function api(path: string): string {
  return `${appConfig().apiBaseUrl}${path}`;
}

/** Readable message from an HTTP error (RFC 9457 problem detail or plain). */
export function errorMessage(error: unknown): string {
  const e = error as { status?: number; error?: { detail?: string; message?: string } } | null;
  if (!e) {
    return 'Something went wrong.';
  }
  if (e.status === 0) {
    return "The server can't be reached. Is the backend running?";
  }
  if (e.status === 404) {
    return 'Not found, or you don’t have access to it.';
  }
  return e.error?.detail ?? e.error?.message ?? `Request failed (HTTP ${e.status}).`;
}
