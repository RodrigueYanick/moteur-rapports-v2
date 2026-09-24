import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
  OnDestroy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import {
  LucideAngularModule,
  Eye,
  ZoomIn,
  ZoomOut,
  RotateCcw,
  Printer,
  Download,
  Mail,
  X,
  Loader2,
  FileText
} from 'lucide-angular';

@Component({
  selector: 'app-pdf-viewer-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './pdf-viewer-modal.component.html',
  styleUrls: ['./pdf-viewer-modal.component.scss'],
})
export class PdfViewerModalComponent implements OnChanges, OnDestroy {
  @Input() visible = false;
  @Input() title = 'Aperçu du Document';
  @Input() pdfBlob: Blob | null = null;
  @Input() pdfUrl: string | null = null;
  @Input() canEmail = true;

  @Output() closed = new EventEmitter<void>();
  @Output() requestEmail = new EventEmitter<void>();

  safeUrl: SafeResourceUrl | null = null;
  currentBlobUrl: string | null = null;
  loading = false;
  zoomLevel = 100; // en pourcentage (50% à 200%)

  readonly icons = {
    eye: Eye,
    zoomIn: ZoomIn,
    zoomOut: ZoomOut,
    resetZoom: RotateCcw,
    printer: Printer,
    download: Download,
    mail: Mail,
    close: X,
    loader: Loader2,
    file: FileText,
  };

  constructor(
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      this.initPdfSource();
    }
    if ((changes['pdfBlob'] || changes['pdfUrl']) && this.visible) {
      this.initPdfSource();
    }
  }

  ngOnDestroy(): void {
    this.revokeBlobUrl();
  }

  private initPdfSource(): void {
    this.loading = true;
    this.revokeBlobUrl();

    if (this.pdfBlob) {
      this.currentBlobUrl = window.URL.createObjectURL(this.pdfBlob);
      this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.currentBlobUrl);
      this.loading = false;
    } else if (this.pdfUrl) {
      this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfUrl);
      this.loading = false;
    } else {
      this.safeUrl = null;
      this.loading = false;
    }
    this.cdr.detectChanges();
  }

  private revokeBlobUrl(): void {
    if (this.currentBlobUrl) {
      window.URL.revokeObjectURL(this.currentBlobUrl);
      this.currentBlobUrl = null;
    }
  }

  zoomIn(): void {
    if (this.zoomLevel < 200) {
      this.zoomLevel += 15;
    }
  }

  zoomOut(): void {
    if (this.zoomLevel > 50) {
      this.zoomLevel -= 15;
    }
  }

  resetZoom(): void {
    this.zoomLevel = 100;
  }

  printPdf(): void {
    const iframe = document.getElementById('inAppPdfIframe') as HTMLIFrameElement;
    if (iframe && iframe.contentWindow) {
      try {
        iframe.contentWindow.focus();
        iframe.contentWindow.print();
        return;
      } catch (e) {
        console.warn('Impression directe via iframe restreinte, ouverture en impression native');
      }
    }
    if (this.currentBlobUrl) {
      const w = window.open(this.currentBlobUrl);
      w?.print();
    }
  }

  downloadPdf(): void {
    if (!this.pdfBlob && !this.currentBlobUrl) return;
    const a = document.createElement('a');
    a.href = this.currentBlobUrl || this.pdfUrl || '';
    const cleanName = (this.title || 'document').replace(/[^a-zA-Z0-9._-]/g, '_');
    a.download = cleanName.endsWith('.pdf') ? cleanName : `${cleanName}.pdf`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  openEmailModal(): void {
    this.requestEmail.emit();
  }

  close(): void {
    this.visible = false;
    this.revokeBlobUrl();
    this.closed.emit();
  }
}

