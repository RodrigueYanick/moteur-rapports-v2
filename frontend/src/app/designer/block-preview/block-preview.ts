import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DesignBlock } from '../models/design-block.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MockDataService } from '../services/mock-data.service';
import { LucideAngularModule, Sparkles } from 'lucide-angular';
import { FillerDataService } from '../services/filler-data';

@Component({
  selector: 'app-block-preview',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './block-preview.html',
  styleUrls: ['./block-preview.scss']
})
export class BlockPreview implements OnChanges {
  @Input() blocks: DesignBlock[] = [];
  safePreviewHtml: SafeHtml = '';
  private canvasWidth = 794;
  private canvasHeight = 1123;

  readonly icons = { preview: Sparkles };

  constructor(private sanitizer: DomSanitizer, private mockDataService: MockDataService, private fillerdata: FillerDataService) {}

  renderPreview(): void {
    const mockData = this.mockDataService.generate(this.blocks);
    const realValues = this.fillerdata.getValues();
    const mergedData = { ...mockData, ...realValues };
    let html = `<div style="position:relative;width:${this.canvasWidth}px;height:${this.canvasHeight}px;background:white;overflow:hidden;">`;
    for (const block of this.blocks) {
      const left = block.x || 0;
      const top = block.y || 0;
      html += `<div style="position:absolute;left:${left}px;top:${top}px;">`;
      html += this.renderBlockWithData(block, mergedData);
      html += `</div>`;
    }
    html += '</div>';
    this.safePreviewHtml = this.sanitizer.bypassSecurityTrustHtml(html);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['blocks']) {
      this.renderPreview();
    }
  }

  private renderBlockWithData(block: DesignBlock, mockData: Record<string, any>): string {
    switch (block.type) {
      case 'titre':
        return `<h1 style="font-size:${block.style?.fontSize || 18}px; font-weight:${block.style?.bold ? 'bold' : 'normal'}; font-style:${block.style?.italic ? 'italic' : 'normal'}; text-align:${block.style?.align || 'center'}; color:${block.style?.color || '#000000'};">${this.replaceVars(block.contenu || '', mockData)}</h1>`;

      case 'texte':
        return `<p style="font-size:${block.style?.fontSize || 12}px; font-weight:${block.style?.bold ? 'bold' : 'normal'}; font-style:${block.style?.italic ? 'italic' : 'normal'}; text-align:${block.style?.align || 'left'}; color:${block.style?.color || '#000000'};">${this.replaceVars(block.contenu || '', mockData)}</p>`;

      case 'tableau':
        return this.renderTableWithData(block, mockData);

      case 'ligne':
        return `<hr style="border-top:${block.style?.epaisseur || 1}px solid ${block.style?.couleur || '#000'}; width:${block.style?.largeur || 100}%;" />`;

      case 'image': {
        const url = this.replaceVars(block.url || '', mockData);
        const largeur = block.style?.largeur || 100;
        const align = block.style?.align || 'left';
        let imgStyle = `width:${largeur}px;`;
        if (align === 'center') imgStyle += 'display:block;margin:0 auto;';
        else if (align === 'right') imgStyle += 'display:block;margin-left:auto;';
        return `<img src="${this.escape(url)}" style="${imgStyle}" alt="aperçu" />`;
      }

      case 'rectangle':
      case 'cercle': {
        const w = block.largeurBox || 150;
        const h = block.hauteurBox || (block.type === 'cercle' ? w : 100);
        const fill = block.style?.fill || '#e5e7eb';
        const couleur = block.style?.couleur || '#94a3b8';
        const epaisseur = block.style?.epaisseur ?? 1;
        const radius = block.type === 'cercle' ? '50%' : `${block.style?.borderRadius ?? 0}px`;
        return `<div style="width:${w}px;height:${h}px;background:${fill};border:${epaisseur}px solid ${couleur};border-radius:${radius};box-sizing:border-box;"></div>`;
      }

      case 'qrcode':
      case 'codebarre': {
        const w = block.largeurBox || 100;
        const h = block.hauteurBox || (block.type === 'qrcode' ? w : 60);
        const label = block.type === 'qrcode' ? 'QR Code' : 'Code-barres';
        return `<div style="width:${w}px;height:${h}px;border:2px dashed #d1d5db;background:#f9fafb;display:flex;align-items:center;justify-content:center;color:#6b7280;font-size:10px;font-family:Arial,sans-serif;box-sizing:border-box;">${label} (généré au PDF)</div>`;
      }

      case 'signature': {
        const w = block.largeurBox || 180;
        const h = block.hauteurBox || 70;
        return `<div style="width:${w}px;height:${h}px;border-bottom:1px solid #333;display:flex;align-items:flex-end;justify-content:center;padding-bottom:4px;font-family:cursive;color:#999;font-size:12px;box-sizing:border-box;">Signature</div>`;
      }

      case 'graphique':
        return this.renderGraphiqueWithData(block, mockData);

      default:
        return '';
    }
  }

  private renderGraphiqueWithData(block: DesignBlock, mockData: Record<string, any>): string {
    const source = (block.source || '').replace('{{', '').replace('}}', '').trim();
    const items = mockData[source];
    const w = block.largeurBox || 300;
    const h = block.hauteurBox || 180;

    if (!Array.isArray(items) || items.length === 0) {
      return `<div style="width:${w}px;height:${h}px;border:1px dashed #ccc;display:flex;align-items:center;justify-content:center;color:#999;font-size:11px;font-family:Arial,sans-serif;box-sizing:border-box;">Graphique (${source})</div>`;
    }

    const max = Math.max(...items.map((i: any) => Number(i.value) || 0), 1);
    const barAreaHeight = h - 40;

    let bars = '';
    for (const item of items) {
      const value = Number(item.value) || 0;
      const barHeight = Math.max(2, (value / max) * barAreaHeight);
      bars += `<div style="display:flex;flex-direction:column;align-items:center;flex:1;">
        <div style="width:100%;background:#6d5efc;border-radius:3px 3px 0 0;height:${barHeight}px;"></div>
        <span style="font-size:9px;color:#555;margin-top:4px;text-align:center;">${this.escape(String(item.label))}</span>
      </div>`;
    }

    return `<div style="width:${w}px;height:${h}px;display:flex;align-items:flex-end;gap:6px;border-left:1px solid #ccc;border-bottom:1px solid #ccc;padding:8px;box-sizing:border-box;font-family:Arial,sans-serif;">${bars}</div>`;
  }

  private replaceVars(text: string, mockData: Record<string, any>): string {
    if (!text) return '';
    return text.replace(/\{\{(.+?)\}\}/g, (match, varName) => {
      const trimmed = varName.trim();
      return mockData[trimmed] !== undefined ? String(mockData[trimmed]) : match;
    });
  }

  private renderTableWithData(block: DesignBlock, mockData: Record<string, any>): string {
    const source = block.source || '';
    const sourceClean = source.replace('{{', '').replace('}}', '').trim();
    const rows = mockData[sourceClean] || [];

    const tableStyle = "border-collapse:collapse;width:100%;font-family:Arial, sans-serif;font-size:14px;color:#000;font-weight:400;";
    const cellStyle = "border:1px solid #d9d9d9;padding:3px 5px;min-width:90px;text-align:left;";
    const headerStyle = cellStyle + "background:#f5f5f5;font-weight:600;";

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
    table += '</table>';
    return table;
  }

  private escape(input: string): string {
    if (!input) return '';
    return input.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }
}