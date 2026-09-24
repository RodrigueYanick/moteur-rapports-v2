import { Injectable } from '@angular/core';
import { ConditionalStyleRule, ConditionalStyleEffect } from '../models/design-block.model';

@Injectable({
  providedIn: 'root',
})
export class ConditionEvaluatorService {

  /**
   * Évalue si une règle de style conditionnel est satisfaite par rapport aux données fournies.
   */
  evaluateRule(rule: ConditionalStyleRule, data: Record<string, any>): boolean {
    if (!rule || !rule.champ) return false;

    // Normalisation du nom de champ (nettoyage des {{ }} si saisis par l'utilisateur)
    const rawKey = rule.champ.replace(/[{}]/g, '').trim();
    const actualValue = this.extractValue(data, rawKey);
    const targetValue = rule.valeur;

    switch (rule.operateur) {
      case 'EQUALS':
        return this.areEqual(actualValue, targetValue);

      case 'NOT_EQUALS':
        return !this.areEqual(actualValue, targetValue);

      case 'GREATER_THAN':
        return this.compareNumeric(actualValue, targetValue, (a, b) => a > b);

      case 'GREATER_OR_EQUAL':
        return this.compareNumeric(actualValue, targetValue, (a, b) => a >= b);

      case 'LESS_THAN':
        return this.compareNumeric(actualValue, targetValue, (a, b) => a < b);

      case 'LESS_OR_EQUAL':
        return this.compareNumeric(actualValue, targetValue, (a, b) => a <= b);

      case 'CONTAINS':
        if (actualValue == null) return false;
        return String(actualValue).toLowerCase().includes(String(targetValue ?? '').toLowerCase());

      case 'STARTS_WITH':
        if (actualValue == null) return false;
        return String(actualValue).toLowerCase().startsWith(String(targetValue ?? '').toLowerCase());

      case 'IS_EMPTY':
        return actualValue == null || String(actualValue).trim() === '' || (Array.isArray(actualValue) && actualValue.length === 0);

      case 'IS_NOT_EMPTY':
        return actualValue != null && String(actualValue).trim() !== '' && (!Array.isArray(actualValue) || actualValue.length > 0);

      default:
        return false;
    }
  }

  /**
   * Parcourt une liste de règles et fusionne les effets des règles qui correspondent.
   */
  resolveStyles(rules: ConditionalStyleRule[] | undefined | null, data: Record<string, any>): ConditionalStyleEffect | null {
    if (!rules || rules.length === 0 || !data) return null;

    let mergedEffect: ConditionalStyleEffect | null = null;

    for (const rule of rules) {
      if (this.evaluateRule(rule, data)) {
        if (!mergedEffect) {
          mergedEffect = { ...rule.effet };
        } else {
          // Fusion des propriétés avec priorité à la règle la plus récente
          const current: ConditionalStyleEffect = mergedEffect;
          mergedEffect = {
            ...current,
            ...rule.effet,
            color: rule.effet.color || current.color,
            backgroundColor: rule.effet.backgroundColor || current.backgroundColor,
            bold: rule.effet.bold ?? current.bold,
            italic: rule.effet.italic ?? current.italic,
            underline: rule.effet.underline ?? current.underline,
            badgeStyle: (rule.effet.badgeStyle && rule.effet.badgeStyle !== 'NONE') ? rule.effet.badgeStyle : current.badgeStyle,
          };
        }
      }
    }

    return mergedEffect;
  }

  /**
   * Convertit un effet conditionnel en objet de styles CSS inline pour [ngStyle].
   */
  buildInlineStyle(effect: ConditionalStyleEffect | null | undefined): Record<string, string> {
    if (!effect) return {};

    const styleMap: Record<string, string> = {};
    if (effect.color) styleMap['color'] = effect.color;
    if (effect.backgroundColor) styleMap['background-color'] = effect.backgroundColor;
    if (effect.bold) styleMap['font-weight'] = 'bold';
    if (effect.italic) styleMap['font-style'] = 'italic';
    if (effect.underline) styleMap['text-decoration'] = 'underline';

    return styleMap;
  }

  private extractValue(data: Record<string, any>, path: string): any {
    if (!data) return undefined;
    if (path in data) return data[path];

    // Support de la notation avec point (ex: "client.nom")
    const parts = path.split('.');
    let current: any = data;
    for (const p of parts) {
      if (current == null) return undefined;
      current = current[p];
    }
    return current;
  }

  private areEqual(a: any, b: any): boolean {
    if (a === b) return true;
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;

    // Comparaison numérique si les deux sont des nombres valides
    const numA = Number(a);
    const numB = Number(b);
    if (!isNaN(numA) && !isNaN(numB) && String(a).trim() !== '' && String(b).trim() !== '') {
      return numA === numB;
    }

    // Comparaison insensible à la casse pour les chaînes
    return String(a).trim().toLowerCase() === String(b).trim().toLowerCase();
  }

  private compareNumeric(a: any, b: any, comparator: (n1: number, n2: number) => boolean): boolean {
    const numA = Number(a);
    const numB = Number(b);
    if (isNaN(numA) || isNaN(numB)) return false;
    return comparator(numA, numB);
  }
}

