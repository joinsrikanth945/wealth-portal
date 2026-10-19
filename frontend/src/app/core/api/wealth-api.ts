import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { api } from './api-url';
import {
  Message,
  Order,
  OrderRequest,
  ProposalDetail,
  ProposalRequest,
  Report,
  ReportType,
} from './models';

/** Writes for orders, proposals, reports and messages. Reads use httpResource in the pages. */
@Injectable({ providedIn: 'root' })
export class WealthApi {
  private readonly http = inject(HttpClient);

  // orders
  placeOrder(request: OrderRequest): Observable<Order> {
    return this.http.post<Order>(api('/orders'), request);
  }
  cancelOrder(id: number): Observable<Order> {
    return this.http.post<Order>(api(`/orders/${id}/cancel`), {});
  }
  approveOrder(id: number, note: string): Observable<Order> {
    return this.http.post<Order>(api(`/admin/orders/${id}/approve`), { note });
  }
  rejectOrder(id: number, note: string): Observable<Order> {
    return this.http.post<Order>(api(`/admin/orders/${id}/reject`), { note });
  }

  // proposals
  createProposal(body: ProposalRequest): Observable<ProposalDetail> {
    return this.http.post<ProposalDetail>(api('/rm/proposals'), body);
  }
  updateProposal(id: number, body: ProposalRequest): Observable<ProposalDetail> {
    return this.http.put<ProposalDetail>(api(`/rm/proposals/${id}`), body);
  }
  deleteProposal(id: number): Observable<void> {
    return this.http.delete<void>(api(`/rm/proposals/${id}`));
  }
  sendProposal(id: number): Observable<ProposalDetail> {
    return this.http.post<ProposalDetail>(api(`/rm/proposals/${id}/send`), {});
  }
  executeProposal(id: number): Observable<ProposalDetail> {
    return this.http.post<ProposalDetail>(api(`/rm/proposals/${id}/execute`), {});
  }
  validateProposal(id: number, signature: string): Observable<ProposalDetail> {
    return this.http.post<ProposalDetail>(api(`/proposals/${id}/validate`), { signature });
  }
  rejectProposal(id: number, reason: string): Observable<ProposalDetail> {
    return this.http.post<ProposalDetail>(api(`/proposals/${id}/reject`), { reason });
  }
  proposalPdf(id: number): Observable<Blob> {
    return this.http.get(api(`/proposals/${id}/pdf`), { responseType: 'blob' });
  }

  // reports
  requestReport(portfolioId: number, type: ReportType): Observable<Report> {
    return this.http.post<Report>(api(`/portfolios/${portfolioId}/reports`), { type });
  }
  downloadReport(id: number): Observable<Blob> {
    return this.http.get(api(`/reports/${id}/download`), { responseType: 'blob' });
  }
  deleteReport(id: number): Observable<void> {
    return this.http.delete<void>(api(`/reports/${id}`));
  }

  // messages
  sendMessage(clientId: number, body: string): Observable<Message> {
    return this.http.post<Message>(api(`/messages/threads/${clientId}`), { body });
  }
}

/** Saves a downloaded file (the API needs the token, so a plain link won't do). */
export function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
