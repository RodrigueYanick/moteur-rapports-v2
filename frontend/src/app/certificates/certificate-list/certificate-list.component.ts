import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Shield, ShieldCheck, Key, Upload, Trash2, CheckCircle2, AlertTriangle, RefreshCw, Plus, X, Lock } from 'lucide-angular';
import { CertificateService } from '../../services/certificate.service';
import { CompanyCertificate } from '../../models/certificate.model';
import { ToastService } from '../../shared/services/toast.service';

@Component({
  selector: 'app-certificate-list',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './certificate-list.component.html',
  styleUrls: ['./certificate-list.component.scss']
})
export class CertificateListComponent implements OnInit {
  certificates: CompanyCertificate[] = [];
  loading = false;
  uploading = false;
  errorMsg = '';
  successMsg = '';

  // Modal
  showUploadModal = false;
  uploadAlias = '';
  uploadPassword = '';
  selectedFile: File | null = null;
  selectedFileName = '';

  readonly icons = {
    shield: Shield,
    shieldCheck: ShieldCheck,
    key: Key,
    upload: Upload,
    trash: Trash2,
    check: CheckCircle2,
    alert: AlertTriangle,
    refresh: RefreshCw,
    plus: Plus,
    close: X,
    lock: Lock
  };

  constructor(
    private certificateService: CertificateService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCertificates();
  }

  loadCertificates(): void {
    this.loading = true;
    this.errorMsg = '';
    this.certificateService.getAll().subscribe({
      next: (data) => {
        this.certificates = data || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = 'Impossible de charger les certificats de l\'entreprise.';
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  openUploadModal(): void {
    this.uploadAlias = '';
    this.uploadPassword = '';
    this.selectedFile = null;
    this.selectedFileName = '';
    this.showUploadModal = true;
  }

  closeUploadModal(): void {
    this.showUploadModal = false;
  }

  onFileSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      this.selectedFile = file;
      this.selectedFileName = file.name;
    }
  }

  submitUpload(): void {
    if (!this.selectedFile || !this.uploadAlias.trim()) {
      this.toast.error('Veuillez fournir un alias et un fichier .p12/.pfx', 'Champs requis');
      return;
    }

    this.uploading = true;
    this.certificateService.upload(this.selectedFile, this.uploadAlias.trim(), this.uploadPassword || undefined).subscribe({
      next: (cert) => {
        this.uploading = false;
        this.closeUploadModal();
        this.toast.success('Certificat cryptographique importé avec succès', 'Succès');
        this.loadCertificates();
      },
      error: (err) => {
        this.uploading = false;
        const msg = err.error?.message || err.message || 'Erreur lors de l\'importation du certificat';
        this.toast.error(msg, 'Échec de l\'importation');
        this.cdr.detectChanges();
      }
    });
  }

  toggleActive(cert: CompanyCertificate): void {
    this.certificateService.toggleActive(cert.id).subscribe({
      next: (updated) => {
        cert.active = updated.active;
        this.toast.success(`Certificat ${updated.active ? 'activé' : 'désactivé'}`, 'Statut mis à jour');
        this.cdr.detectChanges();
      },
      error: (err) => {
        const msg = err.error?.message || 'Erreur lors de la modification du statut';
        this.toast.error(msg, 'Erreur');
      }
    });
  }

  deleteCert(cert: CompanyCertificate): void {
    if (!confirm(`Supprimer définitivement le certificat « ${cert.alias} » ?`)) {
      return;
    }

    this.certificateService.delete(cert.id).subscribe({
      next: () => {
        this.toast.success('Certificat supprimé avec succès', 'Supprimé');
        this.certificates = this.certificates.filter(c => c.id !== cert.id);
        this.cdr.detectChanges();
      },
      error: (err) => {
        const msg = err.error?.message || 'Erreur lors de la suppression';
        this.toast.error(msg, 'Erreur');
      }
    });
  }
}
