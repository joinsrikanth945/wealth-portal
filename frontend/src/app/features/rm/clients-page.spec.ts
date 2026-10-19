import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ClientListItem } from '../../core/api/models';
import { setAppConfig } from '../../core/config/app-config';
import { ClientsPage } from './clients-page';

const CLIENTS: ClientListItem[] = [
  {
    id: 1,
    clientNumber: 'C-100231',
    fullName: 'Clara Dupont',
    category: 'RETAIL',
    country: 'FR',
    portfolioCount: 2,
    totalValue: 150000,
    mifidStatus: 'VALID',
    riskScore: 3,
    riskLabel: 'Balanced',
    mifidValidUntil: '2027-03-14',
    mifidNextAction: 'NONE',
  },
  {
    id: 4,
    clientNumber: 'C-100234',
    fullName: "Liam O'Connor",
    category: 'RETAIL',
    country: 'IE',
    portfolioCount: 1,
    totalValue: 25000,
    mifidStatus: 'MISSING',
    riskScore: null,
    riskLabel: null,
    mifidValidUntil: null,
    mifidNextAction: 'PROFILE_NOW',
  },
];

describe('ClientsPage', () => {
  beforeEach(() => {
    setAppConfig({ apiBaseUrl: '/api', keycloak: { url: '', realm: '', clientId: '' } });
    TestBed.configureTestingModule({
      imports: [ClientsPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  async function render() {
    const fixture = TestBed.createComponent(ClientsPage);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne('/api/rm/clients').flush(CLIENTS);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('lists the clients from the API with their MiFID status', async () => {
    const el: HTMLElement = (await render()).nativeElement;

    expect(el.querySelectorAll('tbody tr').length).toBe(2);
    expect(el.textContent).toContain('Clara Dupont');
    expect(el.textContent).toContain('No profile');
  });

  it('shows only clients needing MiFID action when that filter is chosen', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;

    const attention = Array.from(el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('MiFID action needed'),
    )!;
    attention.click();
    fixture.detectChanges();

    const rows = el.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain("Liam O'Connor");
  });
});
