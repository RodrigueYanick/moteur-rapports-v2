import { TestBed } from '@angular/core/testing';
import { DesignerHistoryService } from './designer-history.service';
import { DesignPage } from '../models/design-block.model';

describe('DesignerHistoryService', () => {
  let service: DesignerHistoryService;

  const mockPages1: DesignPage[] = [
    { id: 'p1', nom: 'Page 1', blocks: [{ id: 'b1', type: 'texte', x: 10, y: 10, rotation: 0, contenu: 'Initial' }] }
  ];
  const mockPages2: DesignPage[] = [
    { id: 'p1', nom: 'Page 1', blocks: [{ id: 'b1', type: 'texte', x: 20, y: 20, rotation: 0, contenu: 'Modifié' }] }
  ];
  const mockPages3: DesignPage[] = [
    { id: 'p1', nom: 'Page 1', blocks: [{ id: 'b1', type: 'texte', x: 30, y: 30, rotation: 0, contenu: 'Troisième' }] }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DesignerHistoryService);
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('devrait initialiser l\'historique avec un premier état', () => {
    service.initialize(mockPages1);
    expect(service.canUndo).toBe(false);
    expect(service.canRedo).toBe(false);
  });

  it('devrait permettre d\'annuler après avoir enregistré un second état', () => {
    service.initialize(mockPages1);
    service.pushState(mockPages2);

    expect(service.canUndo).toBe(true);
    expect(service.canRedo).toBe(false);

    const restored = service.undo(mockPages2);
    expect(restored).not.toBeNull();
    expect(restored![0].blocks[0].contenu).toBe('Initial');
    expect(service.canUndo).toBe(false);
    expect(service.canRedo).toBe(true);
  });

  it('devrait permettre de rétablir après une annulation', () => {
    service.initialize(mockPages1);
    service.pushState(mockPages2);

    service.undo(mockPages2);
    expect(service.canRedo).toBe(true);

    const redone = service.redo(mockPages1);
    expect(redone).not.toBeNull();
    expect(redone![0].blocks[0].contenu).toBe('Modifié');
    expect(service.canUndo).toBe(true);
    expect(service.canRedo).toBe(false);
  });

  it('devrait vider la pile de rétablissement si une nouvelle action survient après annulation', () => {
    service.initialize(mockPages1);
    service.pushState(mockPages2);
    service.undo(mockPages2);

    expect(service.canRedo).toBe(true);
    service.pushState(mockPages3);
    expect(service.canRedo).toBe(false);
  });
});

