
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../navbar/navbar.component';
import { ComplianceService } from '../../services/compliance.service';
import {
  ComplianceScanResponse,
  VerificationItem
} from '../../models/api.models';
import { DecimalPipe } from '@angular/common';
interface CorrectionPreview {
  original: string;
  replacement: string;
  startIndex: number;
  findingKey: string;
  validationState: string;
}

@Component({
  selector: 'app-editor-workspace',
  standalone: true,
  imports: [FormsModule, NavbarComponent,DecimalPipe],
  templateUrl: './editor-workspace.component.html',
  styleUrl: './editor-workspace.component.css'
})
export class EditorWorkspaceComponent {
  private svc = inject(ComplianceService);

  appliedCorrections = signal<string[]>([]);

  draft = signal('');
  scanning = signal(false);
  result = signal<ComplianceScanResponse | null>(null);
  scanError = signal('');

  // Correction preview and history
  pendingCorrection = signal<CorrectionPreview | null>(null);
  correctionError = signal('');

  undoStack = signal<string[]>([]);
  redoStack = signal<string[]>([]);

  // True when the current draft differs from the scanned draft
  reviewOutdated = signal(false);

  wordCount = computed(() => {
    const text = this.draft().trim();
    return text ? text.split(/\s+/).length : 0;
  });

  canUndo = computed(() => this.undoStack().length > 0);
  canRedo = computed(() => this.redoStack().length > 0);

  expandedItems = signal<number[]>([]);

  onEdit(value: string): void {
    if (value === this.draft()) return;

    this.draft.set(value);

    if (this.result()) {
      this.reviewOutdated.set(true);
    }

    // Manual editing starts a new history sequence.
    this.undoStack.set([]);
    this.redoStack.set([]);

    this.pendingCorrection.set(null);
    this.correctionError.set('');
    this.scanError.set('');
  }

  clearDraft(): void {
    this.draft.set('');
    this.result.set(null);
    this.scanError.set('');
    this.expandedItems.set([]);
    this.pendingCorrection.set(null);
    this.correctionError.set('');
    this.undoStack.set([]);
    this.redoStack.set([]);
    this.reviewOutdated.set(false);
  }

  scan(): void {
    if (!this.draft().trim() || this.scanning()) return;

    this.scanning.set(true);
    this.result.set(null);
    this.scanError.set('');
    this.expandedItems.set([]);
    this.pendingCorrection.set(null);
    this.correctionError.set('');

    this.svc.scan({ draftText: this.draft() }).subscribe({
      next: response => {
        this.result.set(response);
        this.reviewOutdated.set(false);
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

  /**
   * Opens a preview instead of immediately changing the draft.
   * Only proceeds when the claim occurs exactly once in the draft.
   */
previewCorrection(item: VerificationItem): void {
  const original = item.claim;
  const replacement = item.suggestedCorrection;
  const findingKey = original;

  this.pendingCorrection.set(null);
  this.correctionError.set('');

  if (!original?.trim() || !replacement?.trim()) {
    this.correctionError.set('No usable correction is available.');
    return;
  }

  if (this.appliedCorrections().includes(findingKey)) {
    this.correctionError.set('This correction has already been applied.');
    return;
  }

  if (original === replacement) {
    this.correctionError.set('The suggested correction is identical to the original.');
    return;
  }

  const currentDraft = this.draft();
  const startIndex = currentDraft.indexOf(original);

  if (startIndex === -1) {
    this.correctionError.set(
      'The original wording is no longer in your draft. Please scan again.'
    );
    return;
  }

  if (currentDraft.indexOf(original, startIndex + original.length) !== -1) {
    this.correctionError.set(
      'This wording appears more than once. Please edit the intended occurrence manually.'
    );
    return;
  }

  this.pendingCorrection.set({
    original,
    replacement,
    startIndex,
    findingKey,
    validationState: item.correctionValidationState ?? 'NEEDS_REVIEW'
  });
}

acceptCorrection(): void {
  const preview = this.pendingCorrection();

  if (!preview) return;
  if (preview.validationState !== 'SAFE') {
  this.correctionError.set(
    'This correction has not passed validation. Please review it manually.'
  );
  return;
}
  if (this.appliedCorrections().includes(preview.findingKey)) {
    this.pendingCorrection.set(null);
    this.correctionError.set('This correction has already been applied.');
    return;
  }

  const currentDraft = this.draft();

  const actualText = currentDraft.slice(
    preview.startIndex,
    preview.startIndex + preview.original.length
  );

  if (actualText !== preview.original) {
    this.pendingCorrection.set(null);
    this.correctionError.set(
      'Your draft has changed since this preview was created. Please scan again.'
    );
    return;
  }

  const updatedDraft =
    currentDraft.slice(0, preview.startIndex) +
    preview.replacement +
    currentDraft.slice(preview.startIndex + preview.original.length);

  this.undoStack.update(history => [...history, currentDraft]);
  this.redoStack.set([]);

  this.draft.set(updatedDraft);

  this.appliedCorrections.update(items => [
    ...items,
    preview.findingKey
  ]);

  this.reviewOutdated.set(true);
  this.pendingCorrection.set(null);
  this.correctionError.set('');
  this.scanError.set('');
}

  cancelCorrection(): void {
    this.pendingCorrection.set(null);
    this.correctionError.set('');
  }

  undo(): void {
    const history = this.undoStack();

    if (history.length === 0) return;

    const previousDraft = history[history.length - 1];

    this.redoStack.update(stack => [...stack, this.draft()]);
    this.undoStack.set(history.slice(0, -1));

    this.draft.set(previousDraft);
    this.reviewOutdated.set(true);
    this.pendingCorrection.set(null);
    this.correctionError.set('');
  }

  redo(): void {
    const history = this.redoStack();

    if (history.length === 0) return;

    const nextDraft = history[history.length - 1];

    this.undoStack.update(stack => [...stack, this.draft()]);
    this.redoStack.set(history.slice(0, -1));

    this.draft.set(nextDraft);
    this.reviewOutdated.set(true);
    this.pendingCorrection.set(null);
    this.correctionError.set('');
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
