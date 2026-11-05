
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent {
  isRegister = false;
  loading = false;
  error = '';

  loginData = { username: '', password: '' };

  regData = {
    username: '',
    email: '',
    password: ''
  };

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  fillDemo(role: 'creator' | 'manager' | 'admin') {
    const accounts = {
      creator: { username: 'creator', password: 'Creator@123' },
      manager: { username: 'manager', password: 'Manager@123' },
      admin: { username: 'admin', password: 'Admin@123' }
    };

    this.loginData = { ...accounts[role] };
    this.error = '';
  }

  login() {
  this.loading = true;
  this.error = '';

  this.authService.login(this.loginData).subscribe({
    next: () => {
      this.loading = false;

      if (this.authService.isAdmin()) {
        this.router.navigate(['/documents']);
      } else if (this.authService.isManager()) {
        this.router.navigate(['/review']);
      } else {
        this.router.navigate(['/editor']);
      }
    },
    error: (err) => {
      this.loading = false;
      this.error = err.error?.message || 'Invalid username or password.';
    }
  });
}
  register() {
    this.loading = true;
    this.error = '';

    this.authService.register(this.regData).subscribe({
      next: () => {
        this.loading = false;
        this.isRegister = false;
        this.loginData.username = this.regData.username;
        this.loginData.password = '';
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }
}
