import { TestBed } from '@angular/core/testing';

import { FillerData } from './filler-data';

describe('FillerData', () => {
  let service: FillerData;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FillerData);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
