import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateFiller } from './template-filler';

describe('TemplateFiller', () => {
  let component: TemplateFiller;
  let fixture: ComponentFixture<TemplateFiller>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateFiller]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TemplateFiller);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
