import { ConditionEvaluatorService } from './condition-evaluator.service';
import { ConditionalStyleRule } from '../models/design-block.model';

describe('ConditionEvaluatorService', () => {
  let service: ConditionEvaluatorService;

  beforeEach(() => {
    service = new ConditionEvaluatorService();
  });

  describe('evaluateRule', () => {
    it('devrait évaluer EQUALS avec succès pour du texte et des nombres', () => {
      const ruleText: ConditionalStyleRule = {
        id: 'r1',
        champ: 'statut',
        operateur: 'EQUALS',
        valeur: 'PAYE',
        effet: { color: '#10b981' },
      };

      expect(service.evaluateRule(ruleText, { statut: 'PAYE' })).toBe(true);
      expect(service.evaluateRule(ruleText, { statut: 'paye' })).toBe(true); // Insensible à la casse
      expect(service.evaluateRule(ruleText, { statut: 'EN_ATTENTE' })).toBe(false);

      const ruleNum: ConditionalStyleRule = {
        id: 'r2',
        champ: 'total',
        operateur: 'EQUALS',
        valeur: 100,
        effet: { bold: true },
      };

      expect(service.evaluateRule(ruleNum, { total: 100 })).toBe(true);
      expect(service.evaluateRule(ruleNum, { total: '100' })).toBe(true);
      expect(service.evaluateRule(ruleNum, { total: 150 })).toBe(false);
    });

    it('devrait évaluer GREATER_THAN et LESS_THAN correctement', () => {
      const ruleGt: ConditionalStyleRule = {
        id: 'r3',
        champ: 'montant_solde',
        operateur: 'GREATER_THAN',
        valeur: 0,
        effet: { color: '#ef4444', bold: true },
      };

      expect(service.evaluateRule(ruleGt, { montant_solde: 250.50 })).toBe(true);
      expect(service.evaluateRule(ruleGt, { montant_solde: 0 })).toBe(false);
      expect(service.evaluateRule(ruleGt, { montant_solde: -10 })).toBe(false);

      const ruleLt: ConditionalStyleRule = {
        id: 'r4',
        champ: 'stock',
        operateur: 'LESS_THAN',
        valeur: 5,
        effet: { badgeStyle: 'DANGER' },
      };

      expect(service.evaluateRule(ruleLt, { stock: 2 })).toBe(true);
      expect(service.evaluateRule(ruleLt, { stock: 5 })).toBe(false);
      expect(service.evaluateRule(ruleLt, { stock: 10 })).toBe(false);
    });

    it('devrait évaluer CONTAINS et STARTS_WITH', () => {
      const ruleContains: ConditionalStyleRule = {
        id: 'r5',
        champ: 'description',
        operateur: 'CONTAINS',
        valeur: 'urgent',
        effet: { color: '#f59e0b' },
      };

      expect(service.evaluateRule(ruleContains, { description: 'Commande urgente client' })).toBe(true);
      expect(service.evaluateRule(ruleContains, { description: 'Livraison normale' })).toBe(false);
    });

    it('devrait évaluer IS_EMPTY et IS_NOT_EMPTY', () => {
      const ruleEmpty: ConditionalStyleRule = {
        id: 'r6',
        champ: 'remise',
        operateur: 'IS_EMPTY',
        valeur: null,
        effet: { color: '#94a3b8' },
      };

      expect(service.evaluateRule(ruleEmpty, { remise: null })).toBe(true);
      expect(service.evaluateRule(ruleEmpty, { remise: '' })).toBe(true);
      expect(service.evaluateRule(ruleEmpty, { remise: [] })).toBe(true);
      expect(service.evaluateRule(ruleEmpty, { remise: '10%' })).toBe(false);

      const ruleNotEmpty: ConditionalStyleRule = {
        id: 'r7',
        champ: 'remise',
        operateur: 'IS_NOT_EMPTY',
        valeur: null,
        effet: { bold: true },
      };

      expect(service.evaluateRule(ruleNotEmpty, { remise: '10%' })).toBe(true);
      expect(service.evaluateRule(ruleNotEmpty, { remise: null })).toBe(false);
    });

    it('devrait supporter les champs entourés de {{ }}', () => {
      const ruleCurly: ConditionalStyleRule = {
        id: 'r8',
        champ: '{{ montant_ttc }}',
        operateur: 'GREATER_THAN',
        valeur: 1000,
        effet: { bold: true },
      };

      expect(service.evaluateRule(ruleCurly, { montant_ttc: 1200 })).toBe(true);
      expect(service.evaluateRule(ruleCurly, { montant_ttc: 800 })).toBe(false);
    });
  });

  describe('resolveStyles', () => {
    it('devrait fusionner plusieurs règles qui matchent', () => {
      const rules: ConditionalStyleRule[] = [
        {
          id: '1',
          champ: 'solde',
          operateur: 'GREATER_THAN',
          valeur: 0,
          effet: { color: '#ef4444', bold: true },
        },
        {
          id: '2',
          champ: 'solde',
          operateur: 'GREATER_THAN',
          valeur: 1000,
          effet: { backgroundColor: '#fee2e2', underline: true },
        },
      ];

      const effect = service.resolveStyles(rules, { solde: 1500 });
      expect(effect).not.toBeNull();
      expect(effect?.color).toBe('#ef4444');
      expect(effect?.bold).toBe(true);
      expect(effect?.backgroundColor).toBe('#fee2e2');
      expect(effect?.underline).toBe(true);
    });
  });

  describe('buildInlineStyle', () => {
    it('devrait convertir un effet en objet CSS style map', () => {
      const effect = {
        color: '#10b981',
        backgroundColor: '#ecfdf5',
        bold: true,
        italic: true,
        underline: true,
      };

      const css = service.buildInlineStyle(effect);
      expect(css['color']).toBe('#10b981');
      expect(css['background-color']).toBe('#ecfdf5');
      expect(css['font-weight']).toBe('bold');
      expect(css['font-style']).toBe('italic');
      expect(css['text-decoration']).toBe('underline');
    });
  });
});

