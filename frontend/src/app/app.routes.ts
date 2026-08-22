import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./login/login').then((m) => m.Login),
  },
  {
    path: 'setups',
    loadComponent: () => import('./setups/setups').then((m) => m.Setups),
    canActivate: [authGuard],
  },
  {
    path: 'history',
    loadComponent: () => import('./history/history').then((m) => m.History),
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: 'login' },
];
