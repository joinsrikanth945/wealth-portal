import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Instrument } from '../../core/api/models';
import { setAppConfig } from '../../core/config/app-config';
import { OrderTicketPage } from './order-ticket-page';

const ASML: Instrument = {
  id: 1,
  code: 'NL0010273215',
  name: 'ASML Holding',
  assetClass: 'EQUITY',
  currency: 'EUR',
  exchange: 'Euronext',
  riskClass: 6,
  complex: false,
  lastPrice: 702.4,
  priceDate: '2026-10-16',
  underlyingId: null,
  optionType: null,
  strike: null,
  expiry: null,
  contractSize: null,
};

describe('OrderTicketPage form rules', () => {
  // The page's protected members are reached through this loose view in tests.
  let page: any;

  beforeEach(() => {
    setAppConfig({ apiBaseUrl: '/api', keycloak: { url: '', realm: '', clientId: '' } });
    TestBed.configureTestingModule({
      imports: [OrderTicketPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(OrderTicketPage);
    fixture.componentRef.setInput('id', '1');
    page = fixture.componentInstance;
  });

  it('needs an instrument and a quantity before it can be previewed', () => {
    expect(page.request()).toBeUndefined();

    page.instrument.set(ASML);
    expect(page.request()).toBeUndefined();

    page.model.update((m: object) => ({ ...m, quantity: 10 }));
    expect(page.request()).toMatchObject({ instrumentId: 1, quantity: 10, limitType: 'MARKET' });
  });

  it('shows and requires a limit price only for limit orders', () => {
    page.instrument.set(ASML);
    page.model.update((m: object) => ({ ...m, quantity: 10 }));
    expect(page.ticket.limitPrice().hidden()).toBe(true);

    page.model.update((m: object) => ({ ...m, limitType: 'LIMIT' }));
    expect(page.ticket.limitPrice().hidden()).toBe(false);
    expect(page.request()).toBeUndefined(); // limit price missing

    page.model.update((m: object) => ({ ...m, limitPrice: 690 }));
    expect(page.request()).toMatchObject({ limitType: 'LIMIT', limitPrice: 690 });
  });

  it('asks for a date only for good-till-date orders', () => {
    page.instrument.set(ASML);
    page.model.update((m: object) => ({ ...m, quantity: 5, timeInForce: 'GTD' }));
    expect(page.ticket.validUntil().hidden()).toBe(false);
    expect(page.request()).toBeUndefined();

    page.model.update((m: object) => ({ ...m, validUntil: '2026-12-31' }));
    expect(page.request()).toMatchObject({ timeInForce: 'GTD', validUntil: '2026-12-31' });
  });
});
