import { Routes } from '@angular/router';

import { authGuard } from './guards/auth.guard';
import { managerGuard } from './guards/manager.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  // Public home page
  {
    path: '',
    loadComponent: () =>
      import('./components/home/home.component')
        .then(m => m.HomeComponent)
  },

  // Login and registration
  {
    path: 'login',
    loadComponent: () =>
      import('./components/auth/auth.component')
        .then(m => m.AuthComponent)
  },

  // Admin only
  {
    path: 'documents',
    loadComponent: () =>
      import('./components/document-manager/document-manager.component')
        .then(m => m.DocumentManagerComponent),
    canActivate: [authGuard, adminGuard]
  },

  // All authenticated users
  {
    path: 'editor',
    loadComponent: () =>
      import('./components/editor-workspace/editor-workspace.component')
        .then(m => m.EditorWorkspaceComponent),
    canActivate: [authGuard]
  },

  // Manager and Admin
  {
    path: 'review',
    loadComponent: () =>
      import('./components/review-dashboard/review-dashboard.component')
        .then(m => m.ReviewDashboardComponent),
    canActivate: [authGuard, managerGuard]
  },

  // Guidelines Builder & Management: Manager and Admin
  {
    path: 'guidelines',
    loadComponent: () =>
      import('./components/brand-guidelines/brand-guidelines.component')
        .then(m => m.BrandGuidelinesComponent),
    canActivate: [authGuard, managerGuard]
  },

  // Governance Insights: Manager and Admin
  {
    path: 'governance',
    loadComponent: () =>
      import('./components/governance-dashboard/governance-dashboard.component')
        .then(m => m.GovernanceDashboardComponent),
    canActivate: [authGuard, managerGuard]
  },

  // Unknown routes return home
  {
    path: '**',
    redirectTo: ''
  }
];