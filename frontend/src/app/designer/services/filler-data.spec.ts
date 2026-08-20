import { beforeEach, describe, expect, it } from 'vitest';

import { FillerDataService } from './filler-data';
import { FormulaService } from './formular.service';

describe('FillerDataService', () => {
  let service: FillerDataService;

  beforeEach(() => {
    service = new FillerDataService(new FormulaService());
  });

  it('should evaluate a simple multiplication formula', () => {
    const formulaService = new FormulaService();

    expect(formulaService.evaluate('QTE1 * PU1', { QTE1: 3, PU1: 7 })).toBe(21);
  });

  it('should recompute calculated variables in dependency order', () => {
    service.setCalculatedVariables([
      { id: '1', nomVariable: 'T1', type: 'CALCULEE', obligatoire: false, description: '', formule: 'QTE1 * PU1' },
      { id: '2', nomVariable: 'TOTAL_TTC', type: 'CALCULEE', obligatoire: false, description: '', formule: 'T1 * 1.2' },
    ]);

    service.setValues({ QTE1: 5, PU1: 10 });

    expect(service.getValues()['T1']).toBe(50);
    expect(service.getValues()['TOTAL_TTC']).toBe(60);
  });
});
