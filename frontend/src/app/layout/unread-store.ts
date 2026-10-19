import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { api } from '../core/api/api-url';

/** Unread message count for the menu badge; refreshed every 30 seconds while signed in. */
@Injectable({ providedIn: 'root' })
export class UnreadStore {
  private readonly http = inject(HttpClient);
  private timer: ReturnType<typeof setInterval> | null = null;

  readonly count = signal(0);

  start(): void {
    if (this.timer) {
      return;
    }
    this.refresh();
    this.timer = setInterval(() => this.refresh(), 30_000);
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  refresh(): void {
    this.http.get<{ unread: number }>(api('/messages/unread-count')).subscribe({
      next: (r) => this.count.set(r.unread),
      error: () => {
        /* the badge is not critical */
      },
    });
  }
}
