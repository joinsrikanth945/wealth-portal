import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { TitleStrategy, provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor, errorInterceptor } from './core/auth/auth.interceptor';
import { SessionStore } from './core/auth/session-store';
import { loadAppConfig } from './core/config/app-config';
import { AppTitleStrategy } from './core/app-title-strategy';

// Zoneless change detection is the default for new Angular apps, so there is
// no zone.js and no provideZonelessChangeDetection() call needed.
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    { provide: TitleStrategy, useClass: AppTitleStrategy },
    // Runs before the first route: load runtime config, then check for an
    // existing Keycloak session so guards know who is signed in.
    provideAppInitializer(async () => {
      const session = inject(SessionStore);
      try {
        const config = await loadAppConfig();
        await session.init(config);
      } catch (error) {
        // Keycloak not reachable (e.g. still starting): boot anyway and let
        // the login page explain, rather than hanging on "Loading…".
        console.error('Sign-in service unavailable', error);
        session.markUnavailable();
      }
    }),
  ],
};
