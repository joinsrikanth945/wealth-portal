import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { api } from './api-url';
import { ClientDecision, ReviewDetail } from './models';

/** MiFID writes. Reads use httpResource directly in the pages. */
@Injectable({ providedIn: 'root' })
export class MifidApi {
  private readonly http = inject(HttpClient);

  startReview(clientId: number): Observable<ReviewDetail> {
    return this.http.post<ReviewDetail>(api(`/rm/clients/${clientId}/mifid/reviews`), {});
  }

  saveAnswers(reviewId: number, answers: Record<string, string>): Observable<ReviewDetail> {
    return this.http.put<ReviewDetail>(api(`/rm/mifid/reviews/${reviewId}/answers`), { answers });
  }

  finish(
    reviewId: number,
    decision: ClientDecision,
    signature: string | null,
  ): Observable<ReviewDetail> {
    return this.http.post<ReviewDetail>(api(`/rm/mifid/reviews/${reviewId}/finish`), {
      decision,
      signature,
    });
  }

  cancel(reviewId: number): Observable<ReviewDetail> {
    return this.http.post<ReviewDetail>(api(`/rm/mifid/reviews/${reviewId}/cancel`), {});
  }
}
