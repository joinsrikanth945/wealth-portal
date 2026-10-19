import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, effect, inject, input, linkedSignal, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { api, errorMessage } from '../../core/api/api-url';
import { Thread, ThreadSummary } from '../../core/api/models';
import { WealthApi } from '../../core/api/wealth-api';
import { SessionStore } from '../../core/auth/session-store';
import { UnreadStore } from '../../layout/unread-store';

/** Secure messages. RMs see one conversation per client; a client sees just theirs. */
@Component({
  selector: 'app-messages-page',
  imports: [DatePipe, RouterLink],
  template: `
    <header class="page-header">
      <p class="eyebrow">Secure messages</p>
      <h1 class="display">Messages</h1>
    </header>

    <div class="messages">
      @if (!isClient()) {
        <nav class="card thread-list" aria-label="Conversations">
          @for (t of threads.value(); track t.clientId) {
            <a
              class="thread-item"
              [class.active]="t.clientId === activeId()"
              [routerLink]="['/messages', t.clientId]"
            >
              <span class="row-between">
                <strong>{{ t.clientName }}</strong>
                @if (t.unread) {
                  <span class="count-badge" [attr.aria-label]="t.unread + ' unread'">{{
                    t.unread
                  }}</span>
                }
              </span>
              <span class="muted small">{{ t.lastMessage ?? 'No messages yet' }}</span>
            </a>
          } @empty {
            <p class="muted small">No clients.</p>
          }
        </nav>
      }

      <section class="card conversation">
        @if (!activeId()) {
          <p class="muted">Choose a conversation.</p>
        } @else if (thread.error()) {
          <p class="notice notice-danger">{{ message(thread.error()) }}</p>
        } @else if (thread.value(); as t) {
          <h2 class="card-title">{{ isClient() ? 'Your relationship manager' : t.clientName }}</h2>
          <ol class="bubbles" aria-live="polite">
            @for (m of t.messages; track m.id) {
              <li class="bubble" [class.mine]="m.mine">
                <span class="bubble-meta"
                  >{{ m.senderName }} · {{ m.createdAt | date: 'd MMM, HH:mm' }}</span
                >
                <span class="pre-line">{{ m.body }}</span>
              </li>
            } @empty {
              <li class="muted small">No messages yet. Write the first one below.</li>
            }
          </ol>
          <form class="composer" (submit)="$event.preventDefault(); send(t)">
            <label for="body" class="sr-only">Message</label>
            <textarea
              id="body"
              rows="3"
              maxlength="4000"
              placeholder="Write a message"
              [value]="draft()"
              (input)="draft.set($any($event.target).value)"
            ></textarea>
            @if (error()) {
              <p class="field-error">{{ error() }}</p>
            }
            <div class="actions">
              <button type="submit" class="btn btn-primary" [disabled]="busy() || !draft().trim()">
                Send
              </button>
            </div>
          </form>
          <p class="muted small">Never send passwords or card numbers by message.</p>
        } @else {
          <p class="muted">Loading…</p>
        }
      </section>
    </div>
  `,
})
export class MessagesPage {
  private readonly wealth = inject(WealthApi);
  private readonly session = inject(SessionStore);
  private readonly unread = inject(UnreadStore);

  /** /messages/:clientId */
  readonly clientId = input<string | undefined>();

  protected readonly isClient = computed(
    () =>
      this.session.hasRole('CLIENT') &&
      !this.session.hasRole('RM') &&
      !this.session.hasRole('ADMIN'),
  );
  protected readonly threads = httpResource<ThreadSummary[]>(() => api('/messages/threads'));
  /** The open conversation: from the URL, or a client's only one. */
  protected readonly activeId = linkedSignal(() => {
    const fromUrl = this.clientId();
    if (fromUrl) {
      return Number(fromUrl);
    }
    return this.isClient() ? (this.threads.value()?.[0]?.clientId ?? null) : null;
  });
  protected readonly thread = httpResource<Thread>(() =>
    this.activeId() ? api(`/messages/threads/${this.activeId()}`) : undefined,
  );
  protected readonly draft = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    // Opening a conversation marks it read: refresh the list and the menu badge.
    effect(() => {
      if (this.thread.hasValue()) {
        this.unread.refresh();
        if (!this.isClient()) {
          this.threads.reload();
        }
      }
    });
  }

  protected async send(t: Thread): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.wealth.sendMessage(t.clientId, this.draft().trim()));
      this.draft.set('');
      this.thread.reload();
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected message(e: unknown): string {
    return errorMessage(e);
  }
}
