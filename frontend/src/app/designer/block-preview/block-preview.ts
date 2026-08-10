import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DesignBlock, DesignPage } from '../models/design-block.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MockDataService } from '../services/mock-data.service';
import { LucideAngularModule, Sparkles, Plus, Minus, RotateCcw } from 'lucide-angular';
import { FillerDataService } from '../services/filler-data';

@Component({
  selector: 'app-block-preview',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './block-preview.html',
  styleUrls: ['./block-preview.scss']
})
export class BlockPreview implements OnChanges {
  @Input() pages: DesignPage[] = [];
  safePreviewPages: { nom: string; html: SafeHtml }[] = [];
  private canvasWidth = 794;
  private canvasHeight = 1123;

  zoom = 0.5;
  minZoom = 0.25;
  maxZoom = 1.5;
  step = 0.05;

  readonly icons = {
    preview: Sparkles,
    plus: Plus,
    minus: Minus,
    reset: RotateCcw
  };

  constructor(
    private sanitizer: DomSanitizer,
    private mockDataService: MockDataService,
    private fillerdata: FillerDataService
  ) {}

  renderPreview(): void {
    const allBlocks = this.pages.flatMap(p => p.blocks);
    const mockData = this.mockDataService.generate(allBlocks);
    const realValues = this.fillerdata.getValues();
    const mergedData = { ...mockData, ...realValues };

    this.safePreviewPages = this.pages.map(page => {
      let html = `<div style="position:relative;width:${this.canvasWidth}px;height:${this.canvasHeight}px;background:white;overflow:hidden;margin:0 auto;">`;
      for (const block of page.blocks) {
        const left = block.x || 0;
        const top = block.y || 0;
        html += `<div style="position:absolute;left:${left}px;top:${top}px;">`;
        html += this.renderBlockWithData(block, mergedData);
        html += `</div>`;
      }
      html += '</div>';
      return { nom: page.nom, html: this.sanitizer.bypassSecurityTrustHtml(html) };
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['pages']) {
      this.renderPreview();
    }
  }

  // --- Zoom ---
  zoomIn(): void { this.zoom = Math.min(this.zoom + this.step, this.maxZoom); }
  zoomOut(): void { this.zoom = Math.max(this.zoom - this.step, this.minZoom); }
  resetZoom(): void { this.zoom = 0.5; }

  // --- Dimensions par défaut ---
  private defaultDimensions(type: string): { w: number; h: number } {
    const map: Record<string, {w:number, h:number}> = {
      titre: {w:400, h:40},
      texte: {w:400, h:40},
      tableau: {w:750, h:150},
      ligne: {w:780, h:10},
      image: {w:150, h:150},
      rectangle: {w:150, h:100},
      cercle: {w:100, h:100},
      qrcode: {w:100, h:100},
      codebarre: {w:160, h:60},
      signature: {w:180, h:70},
      graphique: {w:300, h:180},
    };
    return map[type] || {w:200, h:50};
  }

  // --- Wrapper qui impose les dimensions ---
  private wrapBlock(block: DesignBlock, innerHtml: string): string {
    const fallback = this.defaultDimensions(block.type);
    const w = block.largeurBox ?? fallback.w;
    const h = block.hauteurBox ?? fallback.h;
    const parts: string[] = [];
    if (block.rotation) parts.push(`transform:rotate(${block.rotation}deg);transform-origin:center center;`);
    if (block.opacite !== undefined && block.opacite !== 100) parts.push(`opacity:${block.opacite / 100};`);
    return `<div style="width:${w}px;height:${h}px;overflow:hidden;box-sizing:border-box;position:relative;${parts.join('')}">${innerHtml}</div>`;
  }

  // --- Rendu de chaque bloc avec son wrapper ---
  private renderBlockWithData(block: DesignBlock, mockData: Record<string, any>): string {
    let innerHtml = '';
    switch (block.type) {
      case 'titre':
      case 'texte': {
        const tag = block.type === 'titre' ? 'h1' : 'p';
        const style = `font-size:${block.style?.fontSize || 12}px; font-weight:${block.style?.bold ? 'bold' : 'normal'}; font-style:${block.style?.italic ? 'italic' : 'normal'}; text-decoration:${block.style?.underline ? 'underline' : 'none'}; text-align:${block.style?.align || 'left'}; color:${block.style?.color || '#000000'}; font-family:${block.style?.fontFamily || 'inherit'}; margin:0;`;
        innerHtml = `<${tag} style="${style}">${this.replaceVars(block.contenu || '', mockData)}</${tag}>`;
        break;
      }
      case 'tableau':
        innerHtml = this.renderTableWithData(block, mockData);
        break;
      case 'ligne': {
        const epaisseur = block.style?.epaisseur || 1;
        const couleur = block.style?.couleur || '#000';
        const largeur = block.style?.largeur || 100;
        innerHtml = `<hr style="border-top:${epaisseur}px solid ${couleur}; width:${largeur}%; margin:0; border-bottom:none;" />`;
        break;
      }
      case 'image': {
        const url = this.replaceVars(block.url || '', mockData);
        const largeur = block.style?.largeur || 100;
        const align = block.style?.align || 'left';
        let imgStyle = `width:${largeur}px;`;
        if (align === 'center') imgStyle += 'display:block;margin:0 auto;';
        else if (align === 'right') imgStyle += 'display:block;margin-left:auto;';
        innerHtml = `<img src="${this.escape(url)}" style="${imgStyle}" alt="aperçu" />`;
        break;
      }
      case 'rectangle':
      case 'cercle': {
        const fill = block.style?.fill || '#e5e7eb';
        const couleur = block.style?.couleur || '#94a3b8';
        const epaisseur = block.style?.epaisseur ?? 1;
        const radius = block.type === 'cercle' ? '50%' : `${block.style?.borderRadius ?? 0}px`;
        // On utilise 100% pour remplir le wrapper
        innerHtml = `<div style="width:100%; height:100%; background:${fill}; border:${epaisseur}px solid ${couleur}; border-radius:${radius}; box-sizing:border-box;"></div>`;
        break;
      }
      case 'qrcode':
      case 'codebarre': {
        const label = block.type === 'qrcode' ? 'QR Code' : 'Code-barres';
        innerHtml = `<div style="width:100%; height:100%; border:2px dashed #d1d5db; background:#f9fafb; display:flex; align-items:center; justify-content:center; color:#6b7280; font-size:10px; font-family:Arial,sans-serif; box-sizing:border-box;">${label} (généré au PDF)</div>`;
        break;
      }
      case 'signature': {
        innerHtml = `<div style="width:100%; height:100%; border-bottom:1px solid #333; display:flex; align-items:flex-end; justify-content:center; padding-bottom:4px; font-family:cursive; color:#999; font-size:12px; box-sizing:border-box;">Signature</div>`;
        break;
      }
      case 'graphique': {
        innerHtml = this.renderGraphiqueWithData(block, mockData);
        break;
      }
      default:
        return '';
    }
    // Tous les blocs sont wrappés avec leurs dimensions
    return this.wrapBlock(block, innerHtml);
  }

  // --- Tableau ---
  private renderTableWithData(block: DesignBlock, mockData: Record<string, any>): string {
    const bordure = block.style?.bordureCouleur || '#d9d9d9';
    const texteDefaut = block.style?.texteCouleurDefaut || '#000';

    if (block.lignes) {
      let table = `<table style="border-collapse:collapse;width:100%;font-family:Arial, sans-serif;font-size:14px;">`;
      for (const row of block.lignes) {
        table += '<tr>';
        for (const cell of row) {
          const bg = cell.bgColor ? `background:${cell.bgColor};` : '';
          const color = `color:${cell.textColor || texteDefaut};`;
          table += `<td style="border:1px solid ${bordure};padding:3px 5px;min-width:90px;${bg}${color}">${this.replaceVars(cell.value || '', mockData)}</td>`;
        }
        table += '</tr>';
      }
      return table + '</table>';
    }

    const source = block.source || '';
    const sourceClean = source.replace('{{', '').replace('}}', '').trim();
    const rows = mockData[sourceClean] || [];

    const tableStyle = `border-collapse:collapse;width:100%;font-family:Arial, sans-serif;font-size:14px;color:${texteDefaut};font-weight:400;`;
    const cellStyle = `border:1px solid ${bordure};padding:3px 5px;min-width:90px;text-align:left;`;
    const headerStyle = cellStyle + 'background:#f5f5f5;font-weight:600;';

    let table = `<table style="${tableStyle}">`;
    table += '<tr>';
    for (const col of block.colonnes || []) {
      table += `<th style="${headerStyle}">${this.escape(col.titre)}</th>`;
    }
    table += '</tr>';
    for (const row of rows) {
      table += '<tr>';
      for (const col of block.colonnes || []) {
        const val = row[col.variable] !== undefined ? row[col.variable] : '';
        table += `<td style="${cellStyle}">${this.escape(String(val))}</td>`;
      }
      table += '</tr>';
    }
    return table + '</table>';
  }

  // --- Graphique ---
  private renderGraphiqueWithData(block: DesignBlock, mockData: Record<string, any>): string {
    const source = (block.source || '').replace('{{', '').replace('}}', '').trim();
    const items = mockData[source];
    const w = block.largeurBox || 300;
    const h = block.hauteurBox || 180;
    if (!Array.isArray(items) || items.length === 0) {
      return `<div style="width:100%; height:100%; border:1px dashed #ccc; display:flex; align-items:center; justify-content:center; color:#999; font-size:11px; font-family:Arial,sans-serif; box-sizing:border-box;">Graphique (${source})</div>`;
    }
    const max = Math.max(...items.map((i: any) => Number(i.value) || 0), 1);
    const barAreaHeight = h - 40; // on connaît h
    let bars = '';
    for (const item of items) {
      const value = Number(item.value) || 0;
      const barHeight = Math.max(2, (value / max) * barAreaHeight);
      bars += `<div style="display:flex;flex-direction:column;align-items:center;flex:1;">
        <div style="width:100%; background:#6d5efc; border-radius:3px 3px 0 0; height:${barHeight}px;"></div>
        <span style="font-size:9px; color:#555; margin-top:4px; text-align:center;">${this.escape(String(item.label))}</span>
      </div>`;
    }
    return `<div style="width:100%; height:100%; display:flex; align-items:flex-end; gap:6px; border-left:1px solid #ccc; border-bottom:1px solid #ccc; padding:8px; box-sizing:border-box; font-family:Arial,sans-serif;">${bars}</div>`;
  }

  // --- Utilitaires ---
  private replaceVars(text: string, mockData: Record<string, any>): string {
    if (!text) return '';
    return text.replace(/\{\{(.+?)\}\}/g, (match, varName) => {
      const trimmed = varName.trim();
      return mockData[trimmed] !== undefined ? String(mockData[trimmed]) : match;
    });
  }

  private escape(input: string): string {
    if (!input) return '';
    return input.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  
}