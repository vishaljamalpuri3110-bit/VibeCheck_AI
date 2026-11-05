import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import {
  AuthRequest,
  AuthResponse,
  RegisterRequest
} from '../models/api.models';

const API_BASE = 'http://localhost:8080/api';
const TOKEN_KEY = 'vc_token';
const USER_KEY = 'vc_user';
const ROLES_KEY = 'vc_roles';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private _username = signal<string | null>(localStorage.getItem(USER_KEY));
  private _roles = signal<string>(localStorage.getItem(ROLES_KEY) ?? '');

  readonly isLoggedIn = computed(() => !!this._token());
  readonly username = computed(() => this._username());
  readonly roles = computed(() => this._roles());

  /** Checks complete role names, not partial string matches. */
  hasRole(role: string): boolean {
    return this._roles()
      .split(',')
      .map(value => value.trim())
      .includes(role);
  }

  readonly isAdmin = computed(() => this.hasRole('ROLE_ADMIN'));

  readonly isManager = computed(() =>
    this.hasRole('ROLE_MANAGER') || this.hasRole('ROLE_ADMIN')
  );

  readonly isCreator = computed(() =>
    this.hasRole('ROLE_CONTENT_CREATOR')
  );

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(request: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${API_BASE}/auth/login`,
      request
    ).pipe(
      tap(res => {
        this._token.set(res.token);
        this._username.set(res.username);
        this._roles.set(res.roles || '');

        localStorage.setItem(TOKEN_KEY, res.token);
        localStorage.setItem(USER_KEY, res.username);
        localStorage.setItem(ROLES_KEY, res.roles || '');
      })
    );
  }

  register(request: RegisterRequest): Observable<string> {
    return this.http.post(
      `${API_BASE}/auth/register`,
      request,
      { responseType: 'text' }
    );
  }

  logout(): void {
    this._token.set(null);
    this._username.set(null);
    this._roles.set('');

    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(ROLES_KEY);

    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this._token();
  }
}