import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { appConfig } from '../config/app-config';

export interface SessionStartResponse {
  /** When this user last signed in before now; null on the very first sign-in. */
  previousLoginAt: string | null;
  firstLogin: boolean;
}

export interface MeResponse {
  username: string;
  displayName: string;
  email: string | null;
  roles: string[];
  lastLoginAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class SessionApi {
  private readonly http = inject(HttpClient);
  private readonly base = () => appConfig().apiBaseUrl;

  /** Records this sign-in on the server and returns the previous one. */
  start(): Observable<SessionStartResponse> {
    return this.http.post<SessionStartResponse>(`${this.base()}/session/start`, {});
  }

  me(): Observable<MeResponse> {
    return this.http.get<MeResponse>(`${this.base()}/me`);
  }
}
