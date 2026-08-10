import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportDesigner } from './report-designer';

describe('ReportDesigner', () => {
  let component: ReportDesigner;
  let fixture: ComponentFixture<ReportDesigner>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportDesigner],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportDesigner);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
