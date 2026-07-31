import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DesignCanvas } from './design-canvas';

describe('DesignCanvas', () => {
  let component: DesignCanvas;
  let fixture: ComponentFixture<DesignCanvas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DesignCanvas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DesignCanvas);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
