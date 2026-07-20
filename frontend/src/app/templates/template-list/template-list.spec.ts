import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateList } from './template-list';

describe('TemplateList', () => {
  let component: TemplateList;
  let fixture: ComponentFixture<TemplateList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TemplateList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
