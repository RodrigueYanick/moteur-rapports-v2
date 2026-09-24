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
import * as XLSX from 'xlsx';
import { Variable } from '../../../models/variable.model';
import {
  LucideAngularModule,
  FileSpreadsheet,
  Upload,
  CheckCircle,
  ArrowRight,
  Table,
  AlertCircle,
  X,
  RefreshCw,
  Sparkles,
  FileText
} from 'lucide-angular';

export interface BatchImportItem {
  customId: string;
  data: Record<string, any>;
}

@Component({
  selector: 'app-excel-import-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './excel-import-modal.component.html',
  styleUrls: ['./excel-import-modal.component.scss'],
})
export class ExcelImportModalComponent implements OnChanges {
  @Input() visible: boolean = false;
  @Input() variables: Variable[] = [];
  @Input() mode: 'batch' | 'single-row' = 'batch';
  @Input() templateNom: string = '';

  @Output() importBatch = new EventEmitter<BatchImportItem[]>();
  @Output() importSingleRow = new EventEmitter<Record<string, any>>();
  @Output() cancelled = new EventEmitter<void>();

  // États du fichier
  isDragging = false;
  loadingFile = false;
  parseError: string | null = null;
  fileName: string = '';
  fileSize: string = '';

  // Données Excel analysées
  workbook: XLSX.WorkBook | null = null;
  sheetNames: string[] = [];
  selectedSheet: string = '';
  detectedHeaders: string[] = [];
  allDataRows: any[][] = [];
  previewRows: any[][] = [];
  totalRowsCount: number = 0;

  // Mapping des colonnes (index colonne Excel -> nom de variable ou '')
  columnMappings: { [colIndex: number]: string } = {};

  // Colonne personnalisée pour customId (mode batch)
  selectedIdColumn: number | 'auto' = 'auto';

  // Ligne sélectionnée (mode single-row)
  selectedRowIndex: number = 0;

  readonly icons = {
    spreadsheet: FileSpreadsheet,
    upload: Upload,
    checkCircle: CheckCircle,
    arrowRight: ArrowRight,
    table: Table,
    alert: AlertCircle,
    close: X,
    refresh: RefreshCw,
    sparkles: Sparkles,
    fileText: FileText,
  };

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      if (!this.fileName) {
        this.resetState();
      } else {
        this.autoMapColumns();
      }
    }
  }

  resetState(): void {
    this.workbook = null;
    this.sheetNames = [];
    this.selectedSheet = '';
    this.detectedHeaders = [];
    this.allDataRows = [];
    this.previewRows = [];
    this.totalRowsCount = 0;
    this.columnMappings = {};
    this.selectedIdColumn = 'auto';
    this.selectedRowIndex = 0;
    this.fileName = '';
    this.fileSize = '';
    this.parseError = null;
    this.isDragging = false;
  }

  close(): void {
    this.cancelled.emit();
  }

  // ==========================================
  // Gestion du Glisser-Déposer & Upload
  // ==========================================

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    if (event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      this.processFile(file);
    }
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.processFile(input.files[0]);
    }
  }

  processFile(file: File): void {
    const name = file.name.toLowerCase();
    if (!name.endsWith('.xlsx') && !name.endsWith('.xls') && !name.endsWith('.csv')) {
      this.parseError = 'Format de fichier non pris en charge. Veuillez sélectionner un fichier .xlsx, .xls ou .csv.';
      return;
    }

    this.loadingFile = true;
    this.parseError = null;
    this.fileName = file.name;
    this.fileSize = this.formatBytes(file.size);

    const reader = new FileReader();
    reader.onload = (e: any) => {
      try {
        const buffer = e.target.result;
        this.workbook = XLSX.read(buffer, { type: 'array', cellDates: true });
        this.sheetNames = this.workbook.SheetNames || [];

        if (this.sheetNames.length === 0) {
          throw new Error('Le classeur ne contient aucune feuille de calcul.');
        }

        this.selectedSheet = this.sheetNames[0];
        this.loadSheetData(this.selectedSheet);
        this.loadingFile = false;
        this.cdr.detectChanges();
      } catch (err: any) {
        console.error('Erreur de lecture du fichier Excel :', err);
        this.parseError = `Erreur lors de la lecture du fichier : ${err.message || 'fichier invalide ou corrompu'}`;
        this.loadingFile = false;
        this.cdr.detectChanges();
      }
    };

    reader.onerror = () => {
      this.parseError = 'Impossible de lire le fichier.';
      this.loadingFile = false;
      this.cdr.detectChanges();
    };

    reader.readAsArrayBuffer(file);
  }

  onSheetChange(): void {
    if (this.selectedSheet) {
      this.loadSheetData(this.selectedSheet);
    }
  }

  loadSheetData(sheetName: string): void {
    if (!this.workbook) return;
    const worksheet = this.workbook.Sheets[sheetName];
    if (!worksheet) return;

    // Convertit la feuille en matrice de données brute
    const rawRows: any[][] = XLSX.utils.sheet_to_json(worksheet, { header: 1, defval: '' }) as any[][];

    if (!rawRows || rawRows.length === 0) {
      this.parseError = 'La feuille sélectionnée est vide.';
      this.detectedHeaders = [];
      this.allDataRows = [];
      this.previewRows = [];
      this.totalRowsCount = 0;
      return;
    }

    // Détection de la ligne d'en-tête (première ligne non-vide)
    let headerIdx = 0;
    while (headerIdx < rawRows.length && (!rawRows[headerIdx] || rawRows[headerIdx].every(c => c === '' || c == null))) {
      headerIdx++;
    }

    if (headerIdx >= rawRows.length) {
      this.parseError = 'Aucune donnée trouvée dans cette feuille.';
      return;
    }

    this.detectedHeaders = (rawRows[headerIdx] || []).map((h, idx) => {
      const str = String(h != null ? h : '').trim();
      return str || `Colonne ${idx + 1}`;
    });

    const dataRows = rawRows.slice(headerIdx + 1).filter(r => r && r.some(c => c !== '' && c != null));
    this.allDataRows = dataRows;
    this.previewRows = dataRows.slice(0, 5);
    this.totalRowsCount = dataRows.length;
    this.selectedRowIndex = 0;

    // Lancement du mapping intelligent automatique
    this.autoMapColumns();
  }

  // ==========================================
  // Auto-Mapping Intelligent
  // ==========================================

  autoMapColumns(): void {
    this.columnMappings = {};
    const usedVars = new Set<string>();

    this.detectedHeaders.forEach((header, colIdx) => {
      const normHeader = this.normalizeString(header);
      let bestMatch: Variable | null = null;
      let highestScore = 0;

      for (const v of this.variables) {
        if (usedVars.has(v.nomVariable)) continue;

        const normVar = this.normalizeString(v.nomVariable);
        const normDesc = v.description ? this.normalizeString(v.description) : '';

        // Match exact
        if (normHeader === normVar) {
          bestMatch = v;
          highestScore = 100;
          break;
        }

        // Match par inclusion directe
        if (normHeader.includes(normVar) || normVar.includes(normHeader)) {
          if (highestScore < 80) {
            bestMatch = v;
            highestScore = 80;
          }
        }

        // Match avec la description
        if (normDesc && (normHeader.includes(normDesc) || normDesc.includes(normHeader))) {
          if (highestScore < 60) {
            bestMatch = v;
            highestScore = 60;
          }
        }

        // Mots-clés courants
        if (this.isCommonKeywordMatch(normHeader, normVar)) {
          if (highestScore < 90) {
            bestMatch = v;
            highestScore = 90;
          }
        }
      }

      if (bestMatch && highestScore >= 60) {
        this.columnMappings[colIdx] = bestMatch.nomVariable;
        usedVars.add(bestMatch.nomVariable);
      } else {
        this.columnMappings[colIdx] = '';
      }
    });

    // Auto-détection de la colonne ID
    const idColIdx = this.detectedHeaders.findIndex(h => {
      const n = this.normalizeString(h);
      return n === 'id' || n.includes('numero') || n.includes('reference') || n.includes('code');
    });
    this.selectedIdColumn = idColIdx !== -1 ? idColIdx : 'auto';
  }

  private isCommonKeywordMatch(header: string, varName: string): boolean {
    const pairs: [string[], string[]][] = [
      [['nom', 'client', 'nomclient', 'tiers'], ['client_nom', 'nom_client', 'client']],
      [['siret', 'rcs'], ['emetteur_siret', 'siret']],
      [['total', 'ttc', 'netapayer', 'net_payer', 'montantttc'], ['total_ttc', 'total']],
      [['ht', 'totalht', 'montantht'], ['total_ht', 'ht']],
      [['tva', 'montanttva'], ['montant_tva', 'taux_tva', 'tva']],
      [['adresse', 'rue', 'ville'], ['client_adresse', 'adresse']],
      [['email', 'mail', 'courriel'], ['client_email', 'email']],
      [['date', 'datefacture', 'emission'], ['date_facture', 'date']],
      [['echeance', 'datelimite'], ['date_echeance', 'echeance']],
      [['commande', 'numerocommande', 'numcommande'], ['numero_commande', 'commande']],
      [['facture', 'numerofacture', 'numfacture'], ['numero_facture', 'facture']],
      [['matricule'], ['salarie_matricule', 'matricule']],
      [['salarie', 'employe'], ['salarie_nom', 'salarie']],
      [['heures', 'volumeheures'], ['heures_travaillees', 'duree_heures']],
    ];

    for (const [headerKeywords, varKeywords] of pairs) {
      const matchHeader = headerKeywords.some(k => header.includes(k));
      const matchVar = varKeywords.some(k => varName.includes(k));
      if (matchHeader && matchVar) return true;
    }
    return false;
  }

  private normalizeString(str: string): string {
    return (str || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '') // Supprime les accents
      .replace(/[^a-z0-9]/g, ''); // Garde uniquement alphanumérique
  }

  getSampleValue(colIndex: number): string {
    if (!this.allDataRows || this.allDataRows.length === 0) return '-';
    for (const row of this.allDataRows) {
      const val = row[colIndex];
      if (val !== undefined && val !== null && String(val).trim() !== '') {
        return String(val);
      }
    }
    return '-';
  }

  get mappedColumnsCount(): number {
    return Object.values(this.columnMappings).filter(v => !!v).length;
  }

  // ==========================================
  // Validation et Import
  // ==========================================

  confirmImport(): void {
    if (this.totalRowsCount === 0) return;

    if (this.mode === 'batch') {
      const items: BatchImportItem[] = [];

      this.allDataRows.forEach((row, rowIdx) => {
        const itemData: Record<string, any> = {};

        // Récupération de l'identifiant personnalisé
        let customId = '';
        if (this.selectedIdColumn !== 'auto' && typeof this.selectedIdColumn === 'number') {
          customId = String(row[this.selectedIdColumn] || '').trim();
        }
        if (!customId) {
          customId = `RPT-${String(rowIdx + 1).padStart(3, '0')}`;
        }

        // Mapping des colonnes vers variables
        this.detectedHeaders.forEach((_, colIdx) => {
          const varName = this.columnMappings[colIdx];
          if (varName) {
            const rawVal = row[colIdx];
            const targetVar = this.variables.find(v => v.nomVariable === varName);
            itemData[varName] = this.castValue(rawVal, targetVar?.type);
          }
        });

        items.push({ customId, data: itemData });
      });

      this.importBatch.emit(items);
      this.close();
    } else {
      // Mode single-row (pour template-filler)
      const selectedRow = this.allDataRows[Number(this.selectedRowIndex)] || this.allDataRows[0] || [];
      const rowData: Record<string, any> = {};

      this.detectedHeaders.forEach((_, colIdx) => {
        const varName = this.columnMappings[colIdx];
        if (varName) {
          const rawVal = selectedRow[colIdx];
          const targetVar = this.variables.find(v => v.nomVariable === varName);
          rowData[varName] = this.castValue(rawVal, targetVar?.type);
        }
      });

      this.importSingleRow.emit(rowData);
      this.close();
    }
  }

  private castValue(val: any, type?: string): any {
    if (val === undefined || val === null) return '';
    if (type === 'FLOAT') {
      if (typeof val === 'number') return val;
      const num = parseFloat(String(val).replace(/\s/g, '').replace(',', '.'));
      return isNaN(num) ? 0 : num;
    }
    if (type === 'BOOLEAN') {
      if (typeof val === 'boolean') return val;
      const str = String(val).toLowerCase().trim();
      return str === 'true' || str === '1' || str === 'oui' || str === 'vrai' || str === 'yes';
    }
    if (type === 'DATE') {
      if (val instanceof Date) {
        return val.toISOString().substring(0, 10);
      }
      return String(val).trim();
    }
    return String(val).trim();
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'Ko', 'Mo', 'Go'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}

