
import { Component, signal, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../navbar/navbar.component';
import { DocumentService, UploadResult } from '../../services/document.service';
import { DocumentSummary } from '../../models/api.models';

@Component({
  selector: 'app-document-manager',
  standalone: true,
  imports: [FormsModule, NavbarComponent],
  templateUrl: './document-manager.component.html',
  styleUrl: './document-manager.component.css'
})
export class DocumentManagerComponent implements OnInit {
  private docService = inject(DocumentService);

  docs = signal<DocumentSummary[]>([]);
  loading = signal(false);
  loadError = signal('');

  file = signal<File | null>(null);
  docType = 'BRAND_HANDBOOK';
  dragging = signal(false);

  uploading = signal(false);
  progress = signal(0);
  uploadResult = signal<UploadResult | null>(null);
  uploadErr = signal('');

  ngOnInit(): void {
    this.loadDocs();
  }

  loadDocs(): void {
    this.loading.set(true);
    this.loadError.set('');

    this.docService.listDocuments().subscribe({
      next: documents => {
        this.docs.set(documents);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set('Could not load documents. Please try again.');
      }
    });
  }

  onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const selectedFile = input.files?.[0];

    if (selectedFile) {
      this.pick(selectedFile);
    }

    // Allows selecting the same file again after clearing it.
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);

    const droppedFile = event.dataTransfer?.files[0];

    if (droppedFile) {
      this.pick(droppedFile);
    }
  }

  pick(selectedFile: File): void {
    if (
      selectedFile.type !== 'application/pdf' &&
      !selectedFile.name.toLowerCase().endsWith('.pdf')
    ) {
      this.uploadErr.set('Please select a PDF file.');
      return;
    }

    this.file.set(selectedFile);
    this.uploadResult.set(null);
    this.uploadErr.set('');
    this.progress.set(0);
  }

  clearFile(): void {
    this.file.set(null);
    this.progress.set(0);
  }

  upload(): void {
    const selectedFile = this.file();

    if (!selectedFile || this.uploading()) {
      return;
    }

    this.uploading.set(true);
    this.uploadErr.set('');
    this.uploadResult.set(null);
    this.progress.set(0);

    const { progress$, result$ } =
      this.docService.uploadPdf(selectedFile, this.docType);

    progress$.subscribe({
      next: value => this.progress.set(value)
    });

    result$.subscribe({
      next: result => {
        this.uploadResult.set(result);
        this.uploading.set(false);
        this.progress.set(100);
        this.file.set(null);
        this.loadDocs();
      },
      error: error => {
        this.uploadErr.set(
          error?.error ?? 'Upload failed. Please try again.'
        );
        this.uploading.set(false);
      }
    });
  }

  deleteDoc(name: string): void {
    const confirmed = confirm(
      `Delete all chunks for "${name}"? This action cannot be undone.`
    );

    if (!confirmed) {
      return;
    }

    this.docService.deleteDocument(name).subscribe({
      next: () => this.loadDocs(),
      error: error => {
        alert('Delete failed: ' + (error?.message ?? 'Please try again.'));
      }
    });
  }

  fmtSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  getDocumentTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      BRAND_HANDBOOK: 'Brand handbook',
      STYLE_GUIDE: 'Style guide',
      LEGAL_GUIDELINES: 'Legal guidelines',
      GENERAL: 'General reference'
    };

    return labels[type] ?? type.replaceAll('_', ' ');
  }
}
