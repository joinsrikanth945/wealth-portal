import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { appConfig } from '../config/app-config';
import { SessionStore } from './session-store';

/** Adds the Keycloak access token to every call to our own API. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const apiBase = appConfig().apiBaseUrl;
  if (!req.url.startsWith(apiBase)) {
    return next(req);
  }
  const session = inject(SessionStore);
  return from(session.token()).pipe(
    switchMap((token) =>
      next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req),
    ),
  );
};

/** Central handling of "not signed in" and "not allowed". */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 401) {
          router.navigate(['/login']);
        } else if (error.status === 403) {
          router.navigate(['/forbidden']);
        }
      }
      return throwError(() => error);
    }),
  );
};
