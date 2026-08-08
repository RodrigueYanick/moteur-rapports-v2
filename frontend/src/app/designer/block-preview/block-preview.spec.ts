import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockPreview } from './block-preview';

describe('BlockPreview', () => {
  let component: BlockPreview;
  let fixture: ComponentFixture<BlockPreview>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BlockPreview]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BlockPreview);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
