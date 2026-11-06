import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../navbar/navbar.component';
import { BrandGovernanceService } from '../../services/brand-governance.service';
import { BrandGuideline, BrandTerm, BrandFactClaim } from '../../models/api.models';

type GuidelineTab = 'builder' | 'glossary' | 'facts' | 'history';

@Component({
  selector: 'app-brand-guidelines',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './brand-guidelines.component.html',
  styleUrl: './brand-guidelines.component.css'
})
export class BrandGuidelinesComponent implements OnInit {
  private service = inject(BrandGovernanceService);

  activeTab = signal<GuidelineTab>('builder');
  loading = signal(false);
  saveSuccess = signal('');
  errorMessage = signal('');

  // Active Guideline Builder
  guideline = signal<BrandGuideline>({
    brandName: 'VibeCheck AI',
    version: '1.0',
    missionStatement: '',
    targetAudience: '',
    toneOfVoice: '',
    primaryColors: '#F7F5F1,#FFFFFF,#292824,#A9664D',
    competitorWatchlist: '',
    guidelineContent: '',
    active: true
  });

  // History
  history = signal<BrandGuideline[]>([]);

  // Glossary Terms
  terms = signal<BrandTerm[]>([]);
  newTerm = {
    term: '',
    prohibited: true,
    approvedAlternative: '',
    category: 'PROMOTIONAL_HYPE',
    rationale: ''
  };

  // Facts & Claims
  facts = signal<BrandFactClaim[]>([]);
  newFact = {
    topic: '',
    verifiedClaim: '',
    sourceReference: '',
    category: 'PRODUCT'
  };

  ngOnInit(): void {
    this.loadActiveGuideline();
    this.loadTerms();
    this.loadFacts();
  }

  setTab(tab: GuidelineTab): void {
    this.activeTab.set(tab);
    this.errorMessage.set('');
    this.saveSuccess.set('');
    if (tab === 'history') {
      this.loadHistory();
    }
  }

  loadActiveGuideline(): void {
    this.loading.set(true);
    this.service.getActiveGuideline().subscribe({
      next: data => {
        this.guideline.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Could not load brand guidelines.');
      }
    });
  }

  loadHistory(): void {
    this.service.getGuidelineHistory().subscribe({
      next: data => this.history.set(data),
      error: () => this.errorMessage.set('Could not load version history.')
    });
  }

  saveGuideline(): void {
    this.errorMessage.set('');
    this.saveSuccess.set('');

    const g = this.guideline();
    // Validate hex colors
    if (g.primaryColors) {
      const colors = g.primaryColors.split(',');
      const hexRegex = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/;
      for (const c of colors) {
        const clean = c.trim();
        if (clean && !hexRegex.test(clean)) {
          this.errorMessage.set(`Invalid color format: '${clean}'. Use valid hex codes like #A9664D.`);
          return;
        }
      }
    }

    this.loading.set(true);
    this.service.saveGuideline(g).subscribe({
      next: saved => {
        this.guideline.set(saved);
        this.loading.set(false);
        this.saveSuccess.set('Brand guidelines saved and versioned successfully.');
      },
      error: err => {
        this.loading.set(false);
        this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Failed to save guidelines.');
      }
    });
  }

  // Terms / Glossary
  loadTerms(): void {
    this.service.getTerms().subscribe({
      next: data => this.terms.set(data),
      error: () => this.errorMessage.set('Could not load terms.')
    });
  }

  addTerm(): void {
    if (!this.newTerm.term.trim()) return;
    this.service.createTerm({
      term: this.newTerm.term.trim(),
      prohibited: this.newTerm.prohibited,
      approvedAlternative: this.newTerm.approvedAlternative.trim(),
      category: this.newTerm.category,
      rationale: this.newTerm.rationale.trim(),
      active: true
    }).subscribe({
      next: created => {
        this.terms.update(list => [...list, created]);
        this.newTerm.term = '';
        this.newTerm.approvedAlternative = '';
        this.newTerm.rationale = '';
        this.saveSuccess.set('Term added to brand glossary.');
      },
      error: () => this.errorMessage.set('Could not add term.')
    });
  }

  deactivateTerm(id?: number): void {
    if (!id) return;
    this.service.deactivateTerm(id).subscribe({
      next: () => {
        this.terms.update(list => list.filter(t => t.id !== id));
      }
    });
  }

  // Facts & Claims
  loadFacts(): void {
    this.service.getFacts().subscribe({
      next: data => this.facts.set(data),
      error: () => this.errorMessage.set('Could not load facts repository.')
    });
  }

  addFact(): void {
    if (!this.newFact.topic.trim() || !this.newFact.verifiedClaim.trim()) return;
    this.service.createFact({
      topic: this.newFact.topic.trim(),
      verifiedClaim: this.newFact.verifiedClaim.trim(),
      sourceReference: this.newFact.sourceReference.trim(),
      category: this.newFact.category,
      active: true
    }).subscribe({
      next: created => {
        this.facts.update(list => [...list, created]);
        this.newFact.topic = '';
        this.newFact.verifiedClaim = '';
        this.newFact.sourceReference = '';
        this.saveSuccess.set('Verified claim added to brand repository.');
      },
      error: () => this.errorMessage.set('Could not add claim.')
    });
  }

  deactivateFact(id?: number): void {
    if (!id) return;
    this.service.deactivateFact(id).subscribe({
      next: () => {
        this.facts.update(list => list.filter(f => f.id !== id));
      }
    });
  }

  getColorList(): string[] {
    const raw = this.guideline().primaryColors;
    if (!raw) return [];
    return raw.split(',').map(c => c.trim()).filter(c => c.length > 0);
  }
}
