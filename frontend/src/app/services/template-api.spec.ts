import { TestBed } from '@angular/core/testing';

import { TemplateApi } from './template-api';

describe('TemplateApi', () => {
  let service: TemplateApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TemplateApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
