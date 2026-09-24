import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TemplateApiService } from '../../../services/template-api';
import { ToastService } from '../../services/toast.service';
import {
  LucideAngularModule,
  Mail,
  Send,
  X,
  FileText,
  Loader2,
  CheckCircle,
  ExternalLink
} from 'lucide-angular';

@Component({
  selector: 'app-document-email-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './document-email-modal.component.html',
  styleUrls: ['./document-email-modal.component.scss'],
})
export class DocumentEmailModalComponent implements OnChanges {
  @Input() visible = false;
  @Input() templateId = '';
  @Input() documentId = '';
  @Input() documentNom = '';
  @Input() defaultRecipient = '';

  @Output() closed = new EventEmitter<void>();
  @Output() sent = new EventEmitter<{ destinataire: string; objet: string }>();

  recipient = '';
  subject = '';
  messageBody = '';
  sending = false;
  sendError: string | null = null;

  readonly icons = {
    mail: Mail,
    send: Send,
    close: X,
    file: FileText,
    loader: Loader2,
    check: CheckCircle,
    external: ExternalLink,
  };

  constructor(
    private api: TemplateApiService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      this.initForm();
    }
  }

  private initForm(): void {
    this.sendError = null;
    this.recipient = this.defaultRecipient || '';
    const cleanDoc = this.documentNom || 'Votre Document';
    this.subject = `Document : ${cleanDoc}`;
    this.messageBody = `Bonjour,\n\nVeuillez trouver ci-joint votre document « ${cleanDoc} » généré au format PDF.\n\nRestant à votre entière disposition pour tout renseignement complémentaire.\n\nCordialement,`;
    this.cdr.detectChanges();
  }

  get isFormValid(): boolean {
    if (!this.recipient.trim()) return false;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.recipient.trim())) return false;
    if (!this.subject.trim()) return false;
    return true;
  }

  sendEmail(): void {
    if (!this.isFormValid || this.sending) return;

    this.sending = true;
    this.sendError = null;

    const payload = {
      destinataire: this.recipient.trim(),
      objet: this.subject.trim(),
      message: this.messageBody.trim(),
    };

    this.api.sendDocumentEmail(this.templateId, this.documentId, payload).subscribe({
      next: (res) => {
        this.sending = false;
        this.toast.success(
          `Le document a été envoyé avec succès à ${this.recipient.trim()}`,
          'Email expédié'
        );
        this.sent.emit({
          destinataire: this.recipient.trim(),
          objet: this.subject.trim(),
        });
        this.close();
      },
      error: (err) => {
        console.error('Erreur expédition email', err);
        this.sending = false;
        const errMsg =
          err.error?.message ||
          "Échec de l'envoi de l'email. Vérifiez l'adresse saisie ou réessayez.";
        this.sendError = errMsg;
        this.toast.error(
          errMsg,
          "Erreur d'envoi"
        );
        this.cdr.detectChanges();
      },
    });
  }

  openSystemMailClient(): void {
    const encSub = encodeURIComponent(this.subject.trim());
    const encBody = encodeURIComponent(this.messageBody.trim());
    const mailtoUrl = `mailto:${encodeURIComponent(this.recipient.trim())}?subject=${encSub}&body=${encBody}`;
    window.open(mailtoUrl, '_blank');
  }

  close(): void {
    this.visible = false;
    this.closed.emit();
  }
}

