import { beforeEach, describe, expect, it } from 'vitest';
import { FillerDataService } from './filler-data';

describe('FillerDataService', () => {
  let service: FillerDataService;

  beforeEach(() => {
    service = new FillerDataService();
  });

  it('devrait être initialisé avec un objet vide', () => {
    expect(service.getValues()).toEqual({});
  });

  it('devrait définir les valeurs de test', () => {
    service.setValues({ clientNom: 'ACME Corp', montant: 1500 });
    expect(service.getValues()).toEqual({ clientNom: 'ACME Corp', montant: 1500 });
  });

  it('devrait mettre à jour une valeur individuelle', () => {
    service.setValues({ clientNom: 'ACME Corp', montant: 1500 });
    service.updateValue('montant', 2000);
    service.updateValue('tva', 400);

    expect(service.getValues()['montant']).toBe(2000);
    expect(service.getValues()['tva']).toBe(400);
    expect(service.getValues()['clientNom']).toBe('ACME Corp');
  });

  it('devrait réinitialiser les valeurs', () => {
    service.setValues({ foo: 'bar' });
    service.reset();
    expect(service.getValues()).toEqual({});
  });
});
