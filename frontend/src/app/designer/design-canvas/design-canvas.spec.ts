import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DesignCanvas } from './design-canvas';

describe('DesignCanvas', () => {
  let component: DesignCanvas;
  let fixture: ComponentFixture<DesignCanvas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DesignCanvas],
    }).compileComponents();

    fixture = TestBed.createComponent(DesignCanvas);
    component = fixture.componentInstance;
    component.blocks = [
      { id: 'a', type: 'texte', x: 10, y: 20, contenu: 'A' },
      { id: 'b', type: 'texte', x: 40, y: 60, contenu: 'B' },
      { id: 'c', type: 'texte', x: 80, y: 100, contenu: 'C' },
    ] as any;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should keep the last selected block when multi-selecting and move all selected blocks', () => {
    component.setSelection(['a', 'b'], component.blocks[1]);

    expect(component.selectedBlockId).toBe('b');
    expect(component.selectedBlockIds).toEqual(['a', 'b']);

    component.moveSelectedBlocks(5, -2);

    expect(component.blocks[0].x).toBe(15);
    expect(component.blocks[0].y).toBe(18);
    expect(component.blocks[1].x).toBe(45);
    expect(component.blocks[1].y).toBe(58);
    expect(component.blocks[2].x).not.toBe(85);
  });
});
