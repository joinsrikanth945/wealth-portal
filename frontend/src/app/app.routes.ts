import { Routes } from '@angular/router';
import { authGuard, homeRedirectGuard, roleGuard, signedOutGuard } from './core/auth/auth.guard';
import { ShellComponent } from './layout/shell';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [signedOutGuard],
    title: 'Sign in',
    loadComponent: () => import('./features/login/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'forbidden',
    title: 'No access',
    loadComponent: () => import('./features/forbidden/forbidden-page').then((m) => m.ForbiddenPage),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', canActivate: [homeRedirectGuard], children: [] },
      {
        path: 'rm/home',
        canActivate: [roleGuard('RM')],
        title: 'Home',
        loadComponent: () => import('./features/rm/rm-home-page').then((m) => m.RmHomePage),
      },
      {
        path: 'rm/clients',
        canActivate: [roleGuard('RM')],
        title: 'Clients',
        loadComponent: () => import('./features/rm/clients-page').then((m) => m.ClientsPage),
      },
      {
        path: 'rm/clients/:clientId',
        canActivate: [roleGuard('RM')],
        title: 'Client',
        loadComponent: () => import('./features/rm/client-page').then((m) => m.ClientPage),
      },
      {
        path: 'rm/clients/:clientId/mifid/:reviewId',
        canActivate: [roleGuard('RM')],
        title: 'MiFID questionnaire',
        loadComponent: () =>
          import('./features/rm/mifid-wizard-page').then((m) => m.MifidWizardPage),
      },
      {
        path: 'portfolios/:id',
        title: 'Portfolio',
        loadComponent: () =>
          import('./features/portfolio/portfolio-page').then((m) => m.PortfolioPage),
      },
      {
        path: 'client/home',
        canActivate: [roleGuard('CLIENT')],
        title: 'Home',
        loadComponent: () =>
          import('./features/client/client-home-page').then((m) => m.ClientHomePage),
      },
      {
        path: 'admin/home',
        canActivate: [roleGuard('ADMIN')],
        title: 'Administration',
        loadComponent: () =>
          import('./features/admin/admin-home-page').then((m) => m.AdminHomePage),
      },
      {
        path: 'settings',
        title: 'Settings',
        loadComponent: () =>
          import('./features/settings/preferences-page').then((m) => m.PreferencesPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
