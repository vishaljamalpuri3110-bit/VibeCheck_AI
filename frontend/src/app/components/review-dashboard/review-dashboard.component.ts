import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../navbar/navbar.component';
import { BrandGovernanceService } from '../../services/brand-governance.service';
import { ReviewService } from '../../services/review.service';
import { SubmissionDto, SubmissionComment, ReviewStats } from '../../models/api.models';

type Tab = 'pending' | 'all';
type Decision = 'APPROVED' | 'REJECTED' | 'NEEDS_REVISION';

@Component({
  selector: 'app-review-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './review-dashboard.component.html',
  styleUrl: './review-dashboard.component.css'
})
export class ReviewDashboardComponent implements OnInit {
  private govSvc = inject(BrandGovernanceService);
  private reviewSvc = inject(ReviewService);

  tab = signal<Tab>('pending');
  submissions = signal<SubmissionDto[]>([]);
  loading = signal(false);
  stats = signal<ReviewStats | null>(null);
  selected = signal<SubmissionDto | null>(null);
  submitting = signal(false);
  errorMessage = signal('');
  validationMessage = signal('');

  managerComments = '';

  // Threaded comments
  comments = signal<SubmissionComment[]>([]);
  newReplyMessage = '';

  ngOnInit(): void {
    this.loadStats();
    this.loadSubmissions();
  }

  setTab(tab: Tab): void {
    this.tab.set(tab);
    this.selected.set(null);
    this.managerComments = '';
    this.errorMessage.set('');
    this.loadSubmissions();
  }

  loadStats(): void {
    this.reviewSvc.getStats().subscribe({
      next: stats => this.stats.set(stats),
      error: () => this.errorMessage.set('Could not load review statistics.')
    });
  }

  loadSubmissions(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const request = this.tab() === 'pending'
      ? this.govSvc.getPendingSubmissions()
      : this.govSvc.getAllSubmissions();

    request.subscribe({
      next: response => {
        this.submissions.set(response.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Could not load submissions. Please try again.');
      }
    });
  }

  select(sub: SubmissionDto): void {
    this.selected.set(sub);
    this.managerComments = '';
    this.validationMessage.set('');
    this.newReplyMessage = '';
    this.loadComments(sub.id);
  }

  loadComments(id: number): void {
    this.govSvc.getComments(id).subscribe({
      next: comments => this.comments.set(comments),
      error: () => console.log('Could not load submission comments')
    });
  }

  sendComment(): void {
    const sub = this.selected();
    const msg = this.newReplyMessage.trim();
    if (!sub || !msg) return;

    this.govSvc.addComment(sub.id, msg).subscribe({
      next: c => {
        this.comments.update(list => [...list, c]);
        this.newReplyMessage = '';
      }
    });
  }

  decide(decision: Decision): void {
    const sub = this.selected();
    if (!sub || this.submitting()) return;

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

    this.govSvc.submitDecision(
      sub.id,
      decision,
      this.managerComments.trim()
    ).subscribe({
      next: updated => {
        this.selected.set(updated);
        this.submitting.set(false);
        this.managerComments = '';
        this.loadStats();
        this.loadSubmissions();
        this.loadComments(updated.id);
      },
      error: () => {
        this.submitting.set(false);
        this.validationMessage.set('Your decision could not be saved. Please try again.');
      }
    });
  }

  statusLbl(status: string): string {
    const labels: Record<string, string> = {
      PENDING_REVIEW: 'In Review',
      IN_REVIEW: 'In Review',
      DRAFT: 'Draft',
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
