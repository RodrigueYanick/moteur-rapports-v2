import { TestBed } from '@angular/core/testing';
import { DesignerGeometryService } from './designer-geometry.service';
import { DesignBlock } from '../models/design-block.model';

describe('DesignerGeometryService', () => {
  let service: DesignerGeometryService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DesignerGeometryService);
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('devrait calculer les dimensions A4 standards à 96 DPI', () => {
    const dims = service.getPaperDimensions('A4');
    // 210mm * 96 / 25.4 ≈ 794px, 297mm * 96 / 25.4 ≈ 1123px
    expect(dims.width).toBe(794);
    expect(dims.height).toBe(1123);
  });

  it('devrait calculer les dimensions d\'un format personnalisé', () => {
    const dims = service.getPaperDimensions('CUSTOM', 100, 200);
    // 100mm * 96 / 25.4 ≈ 378px, 200mm * 96 / 25.4 ≈ 756px
    expect(dims.width).toBe(378);
    expect(dims.height).toBe(756);
  });

  it('devrait calculer la zone utile avec marges en mm', () => {
    const area = service.getUsableArea(800, 1200, 10, 10, 15, 20);
    // 10mm ≈ 38px, 15mm ≈ 57px, 20mm ≈ 76px
    expect(area.minX).toBe(38);
    expect(area.minY).toBe(57);
    expect(area.maxX).toBe(800 - 38);
    expect(area.maxY).toBe(1200 - 76);
    expect(area.width).toBe(800 - 38 - 38);
    expect(area.height).toBe(1200 - 57 - 76);
  });

  it('devrait contraindre un bloc pour ne pas déborder à gauche ou au-dessus de la zone utile', () => {
    const area = service.getUsableArea(800, 1200, 10, 10, 10, 10);
    const block: DesignBlock = {
      id: 'b1',
      type: 'texte',
      x: 0,
      y: -20,
      largeurBox: 100,
      hauteurBox: 40,
      rotation: 0,
    };

    service.clampBlockToUsableArea(block, area);
    expect(block.x).toBe(area.minX);
    expect(block.y).toBe(area.minY);
  });
});

