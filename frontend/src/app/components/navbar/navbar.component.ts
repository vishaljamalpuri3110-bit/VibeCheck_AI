
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <div class="nav-brand">
        <span class="brand-icon">✦</span>
        <span class="brand-name">
          VibeCheck <span class="brand-ai">AI</span>
        </span>
        
      </div>

      <div class="nav-links">
        <a routerLink="/editor"
           routerLinkActive="active"
           class="nav-link">
          <span>⌨</span> Editor
        </a>

        @if (isManager()) {
          <a routerLink="/review"
             routerLinkActive="active"
             class="nav-link">
            <span>◈</span> Review
          </a>

          <a routerLink="/guidelines"
             routerLinkActive="active"
             class="nav-link">
            <span>✦</span> Guidelines
          </a>

          <a routerLink="/governance"
             routerLinkActive="active"
             class="nav-link">
            <span>◒</span> Governance
          </a>
        }

        @if (isAdmin()) {
          <a routerLink="/documents"
             routerLinkActive="active"
             class="nav-link">
            <span>▤</span> Documents
          </a>
        }
      </div>

      <div class="nav-user">
        <div class="user-pill">
          <div class="avatar">{{ initial() }}</div>
          <span class="uname">{{ username() }}</span>
        </div>

        <button class="btn-signout" (click)="logout()">
          Sign Out
        </button>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      display: flex;
      align-items: center;
      gap: 24px;
      padding: 0 30px;
      height: 64px;
      background: #fff;
      border-bottom: 1px solid #eae5de;
      position: sticky;
      top: 0;
      z-index: 200;
      flex-shrink: 0;
    }

    .nav-brand {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
    }

    .brand-icon {
      font-size: 19px;
      color: #a9664d;
    }

    .brand-name {
      font-size: 16px;
      font-weight: 750;
      color: #292824;
      letter-spacing: -0.5px;
    }

    .brand-ai {
      color: #a9664d;
    }

    .brand-team {
      margin-left: 5px;
      padding: 3px 8px;
      border: 1px solid #eee1d8;
      border-radius: 20px;
      background: #faf4f0;
      color: #98634e;
      font-size: 9px;
      font-weight: 650;
      white-space: nowrap;
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 5px;
      flex: 1;
    }

    .nav-link {
      display: flex;
      align-items: center;
      gap: 7px;
      padding: 9px 13px;
      border-radius: 8px;
      color: #777168;
      font-size: 12.5px;
      font-weight: 550;
      text-decoration: none;
      transition: all 0.2s ease;
    }

    .nav-link:hover {
      color: #a9664d;
      background: #faf4f0;
    }

    .nav-link.active {
      color: #a9664d;
      background: #f8eee9;
      font-weight: 650;
    }

    .nav-user {
      display: flex;
      align-items: center;
      gap: 15px;
      flex-shrink: 0;
    }

    .user-pill {
      display: flex;
      align-items: center;
      gap: 9px;
    }

    .avatar {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 31px;
      height: 31px;
      border-radius: 50%;
      background: #a9664d;
      color: #fff;
      font-size: 12px;
      font-weight: 700;
    }

    .uname {
      color: #39352f;
      font-size: 12.5px;
      font-weight: 600;
    }

    .btn-signout {
      padding: 7px 12px;
      border: 1px solid #e9e3dc;
      border-radius: 8px;
      background: #fff;
      color: #777168;
      font-size: 11.5px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-signout:hover {
      border-color: #e4c4bb;
      background: #fcf3f1;
      color: #b55348;
    }

    @media (max-width: 650px) {
      .navbar {
        height: auto;
        min-height: 64px;
        padding: 12px 15px;
        flex-wrap: wrap;
        gap: 12px;
      }

      .nav-links {
        order: 3;
        flex-basis: 100%;
      }

      .nav-user {
        margin-left: auto;
        gap: 8px;
      }

      .brand-team {
        display: none;
      }

      .uname {
        display: none;
      }
    }
  `]
})
export class NavbarComponent {
  private auth = inject(AuthService);

  username = this.auth.username;
  isManager = this.auth.isManager;
  isAdmin = this.auth.isAdmin;

  initial = computed(() =>
    (this.auth.username() ?? 'U')[0].toUpperCase()
  );

  logout(): void {
    this.auth.logout();
  }
}
