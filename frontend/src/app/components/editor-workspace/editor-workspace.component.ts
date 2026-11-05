
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../navbar/navbar.component';
import { ComplianceService } from '../../services/compliance.service';
import {
  ComplianceScanResponse,
  VerificationItem
} from '../../models/api.models';

@Component({
  selector: 'app-editor-workspace',
  standalone: true,
  imports: [FormsModule, NavbarComponent],
  templateUrl: './editor-workspace.component.html',
  styleUrl: './editor-workspace.component.css'
})
export class EditorWorkspaceComponent {
  private svc = inject(ComplianceService);

  draft = signal('');
  scanning = signal(false);
  result = signal<ComplianceScanResponse | null>(null);
  scanError = signal('');

  wordCount = computed(() => {
    const text = this.draft().trim();
    return text ? text.split(/\s+/).length : 0;
  });

  expandedItems = signal<number[]>([]);

  onEdit(value: string): void {
    this.draft.set(value);

    if (this.result()) {
      this.result.set(null);
      this.expandedItems.set([]);
    }

    this.scanError.set('');
  }

  clearDraft(): void {
    this.draft.set('');
    this.result.set(null);
    this.scanError.set('');
    this.expandedItems.set([]);
  }

  scan(): void {
    if (!this.draft().trim() || this.scanning()) return;

    this.scanning.set(true);
    this.result.set(null);
    this.scanError.set('');
    this.expandedItems.set([]);

    this.svc.scan({ draftText: this.draft() }).subscribe({
      next: response => {
        this.result.set(response);
        this.scanning.set(false);
      },
      error: () => {
        this.scanning.set(false);
        this.scanError.set(
          'We could not complete the compliance check. Please try again.'
        );
      }
    });
  }

  applyCorrection(item: VerificationItem): void {
    if (!item.suggestedCorrection) return;

    const currentDraft = this.draft();
    const correctedDraft = currentDraft.replace(
      item.claim,
      item.suggestedCorrection
    );

    if (correctedDraft === currentDraft) return;

    this.draft.set(correctedDraft);
    this.result.set(null);
    this.expandedItems.set([]);
    this.scanError.set('');
  }

  toggleDetails(index: number): void {
    this.expandedItems.update(current =>
      current.includes(index)
        ? current.filter(item => item !== index)
        : [...current, index]
    );
  }

  isExpanded(index: number): boolean {
    return this.expandedItems().includes(index);
  }

  stateLabel(state?: string): string {
    if (state === 'SUPPORTED') return 'Supported';
    if (state === 'CONTRADICTED') return 'Needs attention';
    return 'Unverified';
  }

  overallIcon(): string {
    const state = this.result()?.overallState;

    if (state === 'SUPPORTED') return '✓';
    if (state === 'CONTRADICTED') return '!';
    return '?';
  }

  stateIcon(state: string): string {
    if (state === 'SUPPORTED') return '✓';
    if (state === 'CONTRADICTED') return '!';
    return '?';
  }

  trackByRule(index: number): number {
    return index;
  }
}
