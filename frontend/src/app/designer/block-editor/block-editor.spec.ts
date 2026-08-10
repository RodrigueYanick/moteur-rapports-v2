import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockEditor } from './block-editor';

describe('BlockEditor', () => {
  let component: BlockEditor;
  let fixture: ComponentFixture<BlockEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BlockEditor],
    }).compileComponents();

    fixture = TestBed.createComponent(BlockEditor);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
