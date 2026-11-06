import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { NavbarComponent } from '../navbar/navbar.component';
import { BrandGovernanceService } from '../../services/brand-governance.service';
import { GovernanceInsights } from '../../models/api.models';

@Component({
  selector: 'app-governance-dashboard',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './governance-dashboard.component.html',
  styleUrl: './governance-dashboard.component.css'
})
export class GovernanceDashboardComponent implements OnInit {
  private service = inject(BrandGovernanceService);

  loading = signal(false);
  errorMessage = signal('');
  insights = signal<GovernanceInsights | null>(null);

  ngOnInit(): void {
    this.loadInsights();
  }

  loadInsights(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.service.getInsights().subscribe({
      next: data => {
        this.insights.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Could not load governance analytics from the database.');
      }
    });
  }

  getContentTypes(): string[] {
    const map = this.insights()?.contentTypeDistribution;
    return map ? Object.keys(map) : [];
  }
}
