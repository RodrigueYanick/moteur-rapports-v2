import { TestBed } from '@angular/core/testing';
import { DesignerSelectionService } from './designer-selection.service';
import { DesignBlock } from '../models/design-block.model';

describe('DesignerSelectionService', () => {
  let service: DesignerSelectionService;

  const mockBlocks: DesignBlock[] = [
    { id: 'b1', type: 'texte', x: 50, y: 100, largeurBox: 100, hauteurBox: 50, rotation: 0 },
    { id: 'b2', type: 'texte', x: 200, y: 300, largeurBox: 150, hauteurBox: 60, rotation: 0 },
    { id: 'b3', type: 'texte', x: 400, y: 500, largeurBox: 120, hauteurBox: 40, rotation: 0 },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DesignerSelectionService);
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('devrait gérer la sélection simple', () => {
    service.select(mockBlocks[0]);
    expect(service.selectedBlock()).toBe(mockBlocks[0]);
    expect(service.selectedBlockIds()).toEqual(['b1']);
    expect(service.hasSelection()).toBe(true);
    expect(service.isMultiSelection()).toBe(false);
  });

  it('devrait gérer la multi-sélection', () => {
    service.select(mockBlocks[0], false, mockBlocks);
    service.select(mockBlocks[1], true, mockBlocks);

    expect(service.selectedBlockIds()).toEqual(['b1', 'b2']);
    expect(service.isMultiSelection()).toBe(true);
  });

  it('devrait aligner les blocs sélectionnés à gauche', () => {
    service.setSelectionIds(['b1', 'b2'], mockBlocks);

    const aligned = service.alignSelected(mockBlocks, 'left', () => {});
    expect(aligned).toBe(true);
    expect(mockBlocks[0].x).toBe(50);
    expect(mockBlocks[1].x).toBe(50);
  });

  it('devrait réinitialiser la sélection avec clear()', () => {
    service.select(mockBlocks[0]);
    service.clear();

    expect(service.selectedBlock()).toBeNull();
    expect(service.selectedBlockIds()).toEqual([]);
    expect(service.hasSelection()).toBe(false);
  });
});

