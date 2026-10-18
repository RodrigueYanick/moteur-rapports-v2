import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { PublicDocumentViewDto } from '../../models/profile.model';

interface ViewApiResponse<T> {
  message: string;
  data: T;
  timestamp: string;
}

@Component({
  selector: 'app-document-viewer',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './document-viewer.component.html',
  styleUrls: ['./document-viewer.component.scss']
})
export class DocumentViewerComponent implements OnInit {
  shareToken = signal<string>('');
  loading = signal<boolean>(true);
  documentData = signal<PublicDocumentViewDto | null>(null);
  sanitizedHtml = signal<SafeHtml | null>(null);
  errorMessage = signal<string | null>(null);
  showQrModal = signal<boolean>(false);
  copied = signal<boolean>(false);

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('shareToken');
    if (!token) {
      this.errorMessage.set('Jeton de document manquant dans l\'URL.');
      this.loading.set(false);
      return;
    }

    this.shareToken.set(token);
    this.loadDocument(token);
  }

  loadDocument(token: string): void {
    this.loading.set(true);
    this.http.get<ViewApiResponse<PublicDocumentViewDto>>(`/api/public/documents/view/${token}`).subscribe({
      next: (res) => {
        if (res.data) {
          this.documentData.set(res.data);
          if (res.data.htmlContent) {
            this.sanitizedHtml.set(this.sanitizer.bypassSecurityTrustHtml(res.data.htmlContent));
          }
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set('Ce document n\'existe pas, a été supprimé ou n\'est plus partagé publiquement.');
      }
    });
  }

  downloadPdf(): void {
    const token = this.shareToken();
    if (!token) return;
    window.open(`/api/public/documents/view/${token}/pdf`, '_blank');
  }

  openQrModal(): void {
    this.showQrModal.set(true);
  }

  closeQrModal(): void {
    this.showQrModal.set(false);
    this.copied.set(false);
  }

  copyShareUrl(): void {
    const url = this.documentData()?.shareUrl || window.location.href;
    navigator.clipboard.writeText(url).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 3000);
    });
  }
}
