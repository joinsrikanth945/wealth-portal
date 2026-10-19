import { DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { api, errorMessage } from '../../core/api/api-url';
import { MifidApi } from '../../core/api/mifid-api';
import {
  ClientDecision,
  MifidScore,
  Questionnaire,
  QuestionnaireSection,
  ReviewDetail,
} from '../../core/api/models';
import { SignaturePadComponent } from '../../shared/signature-pad';

/**
 * Step-by-step suitability questionnaire. Each "Next" saves the step to the
 * server; the last step shows the risk score, records the client's decision
 * and, if they accept, their signature.
 */
@Component({
  selector: 'app-mifid-wizard-page',
  imports: [RouterLink, DecimalPipe, SignaturePadComponent],
  template: `
    <a [routerLink]="['/rm/clients', clientId()]" class="back-link">Back to client</a>

    @if (questionnaire.isLoading() || review.isLoading()) {
      <p class="muted">Loading questionnaire…</p>
    } @else if (questionnaire.error() || review.error()) {
      <p class="notice notice-danger">{{ message(questionnaire.error() ?? review.error()) }}</p>
    } @else if (review.value(); as r) {
      <header class="page-header">
        <p class="eyebrow">MiFID II suitability · questionnaire {{ r.questionnaireVersion }}</p>
        <h1 class="display">{{ r.clientName }}</h1>
      </header>

      @if (closed()) {
        <p class="notice">
          This review is closed ({{ r.profile.status }}). Start a new one from the client page.
        </p>
      } @else {
        <ol class="stepper" aria-label="Progress">
          @for (s of steps(); track s.id; let i = $index) {
            <li
              [class.done]="i < step()"
              [class.current]="i === step()"
              [attr.aria-current]="i === step() ? 'step' : null"
            >
              {{ i + 1 }}. {{ s.title }}
            </li>
          }
        </ol>

        <div class="wizard">
          <section class="card wizard-main">
            @if (currentSection(); as section) {
              <h2 class="card-title">{{ section.title }}</h2>
              @for (q of section.questions; track q.id) {
                <fieldset class="question" [class.invalid]="showErrors() && !answers()[q.id]">
                  <legend>{{ q.text }}</legend>
                  @if (q.help) {
                    <p class="muted small">{{ q.help }}</p>
                  }
                  <div class="choices">
                    @for (o of q.options; track o.id) {
                      <label class="choice" [class.selected]="answers()[q.id] === o.id">
                        <input
                          type="radio"
                          [name]="q.id"
                          [value]="o.id"
                          [checked]="answers()[q.id] === o.id"
                          (change)="answer(q.id, o.id)"
                        />
                        {{ o.label }}
                      </label>
                    }
                  </div>
                  @if (showErrors() && !answers()[q.id]) {
                    <p class="field-error">Please choose an answer.</p>
                  }
                </fieldset>
              }
            } @else {
              <h2 class="card-title">Summary and client decision</h2>
              @if (score(); as sc) {
                <div class="score-row">
                  <span class="score-big">{{ sc.riskScore ?? '—' }}</span>
                  <div>
                    <strong>{{ sc.riskLabel ?? 'Incomplete' }}</strong>
                    <div class="muted">
                      {{ sc.points }} points · complex products
                      {{ sc.complexProductsOk ? 'appropriate' : 'not appropriate' }}
                    </div>
                  </div>
                </div>
              }

              <fieldset class="question">
                <legend>Client decision</legend>
                <div class="choices">
                  <label class="choice" [class.selected]="decision() === 'ACCEPTED'">
                    <input
                      type="radio"
                      name="decision"
                      [checked]="decision() === 'ACCEPTED'"
                      (change)="decision.set('ACCEPTED')"
                    />
                    Accepted — the client agrees with this profile
                  </label>
                  <label class="choice" [class.selected]="decision() === 'UNDER_REVIEW'">
                    <input
                      type="radio"
                      name="decision"
                      [checked]="decision() === 'UNDER_REVIEW'"
                      (change)="decision.set('UNDER_REVIEW')"
                    />
                    Under review — the client wants time to read it
                  </label>
                  <label class="choice" [class.selected]="decision() === 'REFUSED'">
                    <input
                      type="radio"
                      name="decision"
                      [checked]="decision() === 'REFUSED'"
                      (change)="decision.set('REFUSED')"
                    />
                    Refused — keep the current profile
                  </label>
                </div>
              </fieldset>

              @if (decision() === 'ACCEPTED') {
                <div class="question">
                  <p class="legend-like">Client signature</p>
                  <app-signature-pad (signed)="signature.set($event)" />
                  @if (showErrors() && !signature()) {
                    <p class="field-error">The client must sign to accept.</p>
                  }
                </div>
              }
            }

            @if (error()) {
              <p class="notice notice-danger" role="alert">{{ error() }}</p>
            }

            <div class="wizard-actions">
              <button
                type="button"
                class="btn btn-ghost"
                [disabled]="busy() || step() === 0"
                (click)="back()"
              >
                Previous
              </button>
              <div class="actions">
                <button
                  type="button"
                  class="btn btn-ghost"
                  [disabled]="busy()"
                  (click)="saveAndClose()"
                >
                  Save and close
                </button>
                @if (currentSection()) {
                  <button
                    type="button"
                    class="btn btn-primary"
                    [disabled]="busy()"
                    (click)="next()"
                  >
                    {{ busy() ? 'Saving…' : 'Next' }}
                  </button>
                } @else {
                  <button
                    type="button"
                    class="btn btn-primary"
                    [disabled]="busy()"
                    (click)="finish()"
                  >
                    {{ busy() ? 'Saving…' : 'Finish' }}
                  </button>
                }
              </div>
            </div>
          </section>

          <aside class="card wizard-side">
            <h2 class="card-title">Live risk score</h2>
            @if (score(); as sc) {
              <div class="score-row">
                <span class="score-big">{{ sc.riskScore ?? '—' }}</span>
                <span class="muted">{{ sc.riskLabel ?? 'Answer every scored question' }}</span>
              </div>
              <div
                class="scale"
                role="img"
                [attr.aria-label]="'Risk scale ' + (sc.riskScore ?? 0) + ' of 5'"
              >
                @for (n of [1, 2, 3, 4, 5]; track n) {
                  <span [class.on]="sc.riskScore !== null && n <= sc.riskScore"></span>
                }
              </div>
              <p class="muted small">{{ sc.completionPercent | number: '1.0-0' }}% complete</p>
            }
            <button
              type="button"
              class="btn btn-ghost"
              [disabled]="busy()"
              (click)="cancelReview()"
            >
              Cancel review
            </button>
            <p class="muted small">Every answer is saved and kept in the audit trail.</p>
          </aside>
        </div>
      }
    }
  `,
})
export class MifidWizardPage {
  private readonly mifidApi = inject(MifidApi);
  private readonly router = inject(Router);

  readonly clientId = input.required<string>();
  readonly reviewId = input.required<string>();

  protected readonly questionnaire = httpResource<Questionnaire>(() => api('/mifid/questionnaire'));
  protected readonly review = httpResource<ReviewDetail>(() =>
    api(`/rm/mifid/reviews/${this.reviewId()}`),
  );

  /** Starts from the saved answers and resets whenever a different review loads. */
  protected readonly answers = linkedSignal<Record<string, string>>(() => ({
    ...(this.review.value()?.answers ?? {}),
  }));
  /** Latest score from the server (refreshed on every save). */
  protected readonly score = linkedSignal<MifidScore | null>(
    () => this.review.value()?.score ?? null,
  );

  protected readonly step = signal(0);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showErrors = signal(false);
  protected readonly decision = signal<ClientDecision>('ACCEPTED');
  protected readonly signature = signal<string | null>(null);

  protected readonly closed = computed(() => {
    const s = this.review.value()?.profile.status;
    return s !== undefined && s !== 'IN_PROGRESS' && s !== 'PENDING_VALIDATION';
  });

  protected readonly steps = computed(() => [
    ...(this.questionnaire.value()?.sections ?? []).map((s) => ({ id: s.id, title: s.title })),
    { id: 'summary', title: 'Summary' },
  ]);

  protected readonly currentSection = computed<QuestionnaireSection | null>(
    () => this.questionnaire.value()?.sections[this.step()] ?? null,
  );

  protected answer(questionId: string, optionId: string): void {
    this.answers.update((a) => ({ ...a, [questionId]: optionId }));
  }

  protected back(): void {
    this.showErrors.set(false);
    this.step.update((s) => Math.max(0, s - 1));
  }

  protected async next(): Promise<void> {
    const section = this.currentSection();
    if (!section) {
      return;
    }
    if (section.questions.some((q) => !this.answers()[q.id])) {
      this.showErrors.set(true);
      return;
    }
    if (await this.save()) {
      this.showErrors.set(false);
      this.step.update((s) => s + 1);
    }
  }

  protected async saveAndClose(): Promise<void> {
    if (await this.save()) {
      this.router.navigate(['/rm/clients', this.clientId()]);
    }
  }

  protected async finish(): Promise<void> {
    if (this.decision() === 'ACCEPTED' && !this.signature()) {
      this.showErrors.set(true);
      return;
    }
    if (!(await this.save())) {
      return;
    }
    await this.run(() =>
      firstValueFrom(
        this.mifidApi.finish(
          Number(this.reviewId()),
          this.decision(),
          this.decision() === 'ACCEPTED' ? this.signature() : null,
        ),
      ),
    );
    if (!this.error()) {
      this.router.navigate(['/rm/clients', this.clientId()]);
    }
  }

  protected async cancelReview(): Promise<void> {
    if (!confirm('Cancel this review? The client keeps their current profile.')) {
      return;
    }
    await this.run(() => firstValueFrom(this.mifidApi.cancel(Number(this.reviewId()))));
    if (!this.error()) {
      this.router.navigate(['/rm/clients', this.clientId()]);
    }
  }

  /** Sends the answers to the server; returns false if it failed. */
  private async save(): Promise<boolean> {
    const result = await this.run(() =>
      firstValueFrom(this.mifidApi.saveAnswers(Number(this.reviewId()), this.answers())),
    );
    if (result) {
      this.score.set(result.score);
    }
    return result !== null;
  }

  private async run<T>(call: () => Promise<T>): Promise<T | null> {
    this.busy.set(true);
    this.error.set(null);
    try {
      return await call();
    } catch (e) {
      this.error.set(errorMessage(e));
      return null;
    } finally {
      this.busy.set(false);
    }
  }

  protected message(error: unknown): string {
    return errorMessage(error);
  }
}
