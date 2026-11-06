import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../navbar/navbar.component';
import { ComplianceService } from '../../services/compliance.service';
import { BrandGovernanceService } from '../../services/brand-governance.service';
import {
  ComplianceScanResponse,
  VerificationItem,
  ContentType,
  SubmissionDto,
  SubmissionComment
} from '../../models/api.models';

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
  imports: [CommonModule, FormsModule, NavbarComponent, DecimalPipe],
  templateUrl: './editor-workspace.component.html',
  styleUrl: './editor-workspace.component.css'
})
export class EditorWorkspaceComponent implements OnInit {
  private svc = inject(ComplianceService);
  private govService = inject(BrandGovernanceService);

  // Draft meta & persistence
  submissionId = signal<number | null>(null);
  draftTitle = signal('Untitled Draft');
  contentType = signal<ContentType>('GENERAL');
  submissionStatus = signal<string>('DRAFT');
  lastSavedAt = signal<string | null>(null);
  saving = signal(false);
  saveMessage = signal('');

  // Past user drafts list
  userDrafts = signal<SubmissionDto[]>([]);
  showDraftsModal = signal(false);

  // Content & Scan state
  appliedCorrections = signal<string[]>([]);
  draft = signal('');
  scanning = signal(false);
  result = signal<ComplianceScanResponse | null>(null);
  scanError = signal('');

  // Correction preview & modify
  pendingCorrection = signal<CorrectionPreview | null>(null);
  customCorrectionText = signal('');
  correctionError = signal('');

  // Undo / Redo
  undoStack = signal<string[]>([]);
  redoStack = signal<string[]>([]);
  reviewOutdated = signal(false);

  expandedItems = signal<number[]>([]);

  // Review submission & Comments
  creatorNote = signal('');
  showSubmitModal = signal(false);
  submittingForReview = signal(false);
  comments = signal<SubmissionComment[]>([]);
  newCommentText = signal('');
  loadingComments = signal(false);

  // "Write in our voice" assistant
  showVoiceAssistModal = signal(false);
  voicePrompt = signal('');
  generatingVoice = signal(false);
  voiceAssistNotes = signal('');

  wordCount = computed(() => {
    const text = this.draft().trim();
    return text ? text.split(/\s+/).length : 0;
  });

  canUndo = computed(() => this.undoStack().length > 0);
  canRedo = computed(() => this.redoStack().length > 0);

  ngOnInit(): void {
    this.loadMyDrafts();
  }

  loadMyDrafts(): void {
    this.govService.getMyDrafts().subscribe({
      next: drafts => {
        this.userDrafts.set(drafts);
        // If there's an existing recent draft and current draft is empty, restore it
        if (!this.draft() && drafts.length > 0) {
          this.loadDraft(drafts[0]);
        }
      },
      error: () => console.log('Could not load user drafts')
    });
  }

  loadDraft(d: SubmissionDto): void {
    this.submissionId.set(d.id);
    this.draftTitle.set(d.title);
    this.contentType.set(d.contentType);
    this.draft.set(d.content);
    this.submissionStatus.set(d.status);
    this.lastSavedAt.set(d.updatedAt);
    this.showDraftsModal.set(false);
    this.reviewOutdated.set(false);
    this.loadComments(d.id);
  }

  createNewDraft(): void {
    this.submissionId.set(null);
    this.draftTitle.set('Untitled Draft');
    this.contentType.set('GENERAL');
    this.draft.set('');
    this.submissionStatus.set('DRAFT');
    this.result.set(null);
    this.lastSavedAt.set(null);
    this.comments.set([]);
    this.showDraftsModal.set(false);
  }

  saveDraft(showNotice = true): void {
    if (!this.draft().trim()) return;

    this.saving.set(true);
    this.saveMessage.set('');

    const payload: Partial<SubmissionDto> = {
      id: this.submissionId() ?? undefined,
      title: this.draftTitle().trim() || 'Untitled Draft',
      contentType: this.contentType(),
      content: this.draft(),
      latestDraftReviewLogId: this.result()?.draftReviewLogId,
      overallState: this.result()?.overallState,
      voiceScore: this.result()?.voiceScore,
      voiceFeedback: this.result()?.voiceFeedback,
      creatorNote: this.creatorNote()
    };

    this.govService.saveDraft(payload).subscribe({
      next: saved => {
        this.submissionId.set(saved.id);
        this.lastSavedAt.set(saved.updatedAt);
        this.submissionStatus.set(saved.status);
        this.saving.set(false);
        if (showNotice) {
          this.saveMessage.set('Draft saved to database');
          setTimeout(() => this.saveMessage.set(''), 3000);
        }
        // Update userDrafts list
        this.govService.getMyDrafts().subscribe(list => this.userDrafts.set(list));
      },
      error: () => {
        this.saving.set(false);
        this.saveMessage.set('Save failed. Check network.');
      }
    });
  }

  submitForReview(): void {
    const id = this.submissionId();
    if (!id) {
      // Save draft first then submit
      this.saveDraft(false);
      setTimeout(() => this.executeSubmit(), 500);
    } else {
      this.executeSubmit();
    }
  }

  private executeSubmit(): void {
    const id = this.submissionId();
    if (!id) return;

    this.submittingForReview.set(true);
    this.govService.submitForReview(id, this.creatorNote()).subscribe({
      next: updated => {
        this.submissionStatus.set(updated.status);
        this.submittingForReview.set(false);
        this.showSubmitModal.set(false);
        this.saveMessage.set('Submitted to Manager for review!');
        setTimeout(() => this.saveMessage.set(''), 4000);
        this.loadComments(id);
      },
      error: () => {
        this.submittingForReview.set(false);
        alert('Could not submit for review. Please try again.');
      }
    });
  }

  loadComments(subId: number): void {
    this.loadingComments.set(true);
    this.govService.getComments(subId).subscribe({
      next: comments => {
        this.comments.set(comments);
        this.loadingComments.set(false);
      },
      error: () => this.loadingComments.set(false)
    });
  }

  addComment(): void {
    const subId = this.submissionId();
    const text = this.newCommentText().trim();
    if (!subId || !text) return;

    this.govService.addComment(subId, text).subscribe({
      next: c => {
        this.comments.update(list => [...list, c]);
        this.newCommentText.set('');
      }
    });
  }

  // Voice Assist
  openVoiceAssist(): void {
    this.showVoiceAssistModal.set(true);
    this.voicePrompt.set('');
    this.voiceAssistNotes.set('');
  }

  generateVoiceCopy(): void {
    if (!this.voicePrompt().trim() || this.generatingVoice()) return;
    this.generatingVoice.set(true);
    this.svc.writeInOurVoice(this.voicePrompt(), this.contentType()).subscribe({
      next: res => {
        this.generatingVoice.set(false);
        this.draft.set(res.generatedCopy);
        this.voiceAssistNotes.set(res.voiceToneNotes);
        this.showVoiceAssistModal.set(false);
        this.reviewOutdated.set(true);
        this.saveDraft(false);
      },
      error: () => {
        this.generatingVoice.set(false);
        alert('Could not generate copy in brand voice.');
      }
    });
  }

  onEdit(value: string): void {
    if (value === this.draft()) return;

    this.draft.set(value);

    if (this.result()) {
      this.reviewOutdated.set(true);
    }

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

    this.svc.scan({
      draftText: this.draft(),
      draftTitle: this.draftTitle(),
      contentType: this.contentType(),
      submissionId: this.submissionId() ?? undefined
    }).subscribe({
      next: response => {
        this.result.set(response);
        this.reviewOutdated.set(false);
        this.scanning.set(false);
        // Auto-save draft with new compliance result
        this.saveDraft(false);
      },
      error: () => {
        this.scanning.set(false);
        this.scanError.set('We could not complete the compliance check. Please try again.');
      }
    });
  }

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
      this.correctionError.set('The original wording is no longer in your draft. Please scan again.');
      return;
    }

    if (currentDraft.indexOf(original, startIndex + original.length) !== -1) {
      this.correctionError.set('This wording appears more than once. Please edit manually.');
      return;
    }

    this.customCorrectionText.set(replacement);
    this.pendingCorrection.set({
      original,
      replacement,
      startIndex,
      findingKey,
      validationState: item.correctionValidationState ?? 'NEEDS_REVIEW'
    });
  }

  acceptCorrection(useCustom = false): void {
    const preview = this.pendingCorrection();
    if (!preview) return;

    if (preview.validationState !== 'SAFE' && !useCustom) {
      this.correctionError.set('This correction has not passed validation. Please review it manually.');
      return;
    }

    const replacement = useCustom ? this.customCorrectionText().trim() : preview.replacement;
    if (!replacement) return;

    const currentDraft = this.draft();
    const actualText = currentDraft.slice(
      preview.startIndex,
      preview.startIndex + preview.original.length
    );

    if (actualText !== preview.original) {
      this.pendingCorrection.set(null);
      this.correctionError.set('Draft has changed. Please check compliance again.');
      return;
    }

    const updatedDraft =
      currentDraft.slice(0, preview.startIndex) +
      replacement +
      currentDraft.slice(preview.startIndex + preview.original.length);

    this.undoStack.update(history => [...history, currentDraft]);
    this.redoStack.set([]);
    this.draft.set(updatedDraft);

    this.appliedCorrections.update(items => [...items, preview.findingKey]);
    this.reviewOutdated.set(true);
    this.pendingCorrection.set(null);
    this.correctionError.set('');
    this.saveDraft(false);
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
  }

  toggleDetails(index: number): void {
    this.expandedItems.update(current =>
      current.includes(index) ? current.filter(item => item !== index) : [...current, index]
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
}
