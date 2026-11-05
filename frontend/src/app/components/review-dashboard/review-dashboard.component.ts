
import { Component, signal, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../navbar/navbar.component';
import { ReviewService } from '../../services/review.service';
import { DraftReviewLog, ReviewStats } from '../../models/api.models';

type Tab = 'pending' | 'all';
type Decision = 'APPROVED' | 'REJECTED' | 'NEEDS_REVISION';

@Component({
  selector: 'app-review-dashboard',
  standalone: true,
  imports: [FormsModule, NavbarComponent],
  templateUrl: './review-dashboard.component.html',
  styleUrl: './review-dashboard.component.css'
})
export class ReviewDashboardComponent implements OnInit {
  private reviewSvc = inject(ReviewService);

  tab = signal<Tab>('pending');
  drafts = signal<DraftReviewLog[]>([]);
  loading = signal(false);
  stats = signal<ReviewStats | null>(null);
  selected = signal<DraftReviewLog | null>(null);
  submitting = signal(false);
  errorMessage = signal('');
  validationMessage = signal('');

  managerComments = '';

  ngOnInit(): void {
    this.loadStats();
    this.loadDrafts();
  }

  setTab(tab: Tab): void {
    this.tab.set(tab);
    this.selected.set(null);
    this.managerComments = '';
    this.errorMessage.set('');
    this.loadDrafts();
  }

  loadStats(): void {
    this.reviewSvc.getStats().subscribe({
      next: stats => this.stats.set(stats),
      error: () => this.errorMessage.set('Could not load review statistics.')
    });
  }

  loadDrafts(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const request = this.tab() === 'pending'
      ? this.reviewSvc.getPendingReviews()
      : this.reviewSvc.getAllReviews();

    request.subscribe({
      next: response => {
        this.drafts.set(response.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Could not load drafts. Please try again.');
      }
    });
  }

  select(draft: DraftReviewLog): void {
    this.selected.set(draft);
    this.managerComments = '';
    this.validationMessage.set('');
  }

  decide(decision: Decision): void {
    const draft = this.selected();
    if (!draft || this.submitting()) return;

    if (
      (decision === 'REJECTED' || decision === 'NEEDS_REVISION') &&
      !this.managerComments.trim()
    ) {
      this.validationMessage.set(
        'Please add feedback so the creator understands what needs to change.'
      );
      return;
    }

    this.validationMessage.set('');
    this.submitting.set(true);

    this.reviewSvc.submitDecision(
      draft.id,
      decision,
      this.managerComments.trim()
    ).subscribe({
      next: updated => {
        this.selected.set(updated);
        this.submitting.set(false);
        this.loadStats();
        this.loadDrafts();
      },
      error: () => {
        this.submitting.set(false);
        this.validationMessage.set(
          'Your decision could not be saved. Please try again.'
        );
      }
    });
  }

  statusLbl(status: string): string {
    const labels: Record<string, string> = {
      PENDING_REVIEW: 'Pending',
      APPROVED: 'Approved',
      REJECTED: 'Rejected',
      NEEDS_REVISION: 'Needs Revision'
    };

    return labels[status] ?? status;
  }

  fmtDate(date: string): string {
    if (!date) return '';

    return new Date(date).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
