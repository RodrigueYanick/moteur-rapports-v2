# Analyse du système de pagination et de dimensions

## 🔍 Problèmes identifiés

### 1. Dimensions et marges
- **Frontend (block-preview.ts)**:
  - `canvasWidth = 794px` (correspondant A4: 210mm)
  - `canvasHeight = 1123px` (correspondant A4: 297mm)
  - Aucune marge appliquée (overflow:hidden traite le débordement)

- **Backend (TemplateHtmlBuilder.java)**:
  - Conversion: `widthPx = (int) Math.round(widthMm * 96.0 / 25.4)`
  - Conversion: A4 → 794px × 1123px
  - CSS: `@page{margin:0;}`
  - Aucune marge réelle appliquée

✅ **Les dimensions sont cohérentes** entre frontend et backend

### 2. Logique de pagination Preview

**Algorithme Preview (block-preview.ts:114-130)**:
```typescript
for (const block of sourcePage.blocks) {
  const top = Math.max(0, block.y || 0);
  const height = block.hauteurBox ?? defaultDimensions(block.type).h;
  const pageOffset = Math.max(0, Math.floor((top + height - 1) / this.canvasHeight));
  // bloc envoyé à page = pageOffset
  // position Y recalculée: newY = Math.max(0, (block.y || 0) - pageOffset * this.canvasHeight)
}
```

**Problème détecté**:
- La formule `(top + height - 1)` est légèrement différente de la simple division
- Cela peut créer des pages "quasi-vides" quand un bloc dépasse très légèrement

### 3. Logique de pagination Backend (Mode FIXED)

**Algorithme Backend (TemplateHtmlBuilder.java:92-95)**:
```java
for (JsonNode page : root.path("pages")) {
    html.append(renderPage(page.path("blocs"), data, widthPx, heightPx));
}
```

**Problème détecté**:
- Le backend prend chaque page source et la rend **telle quelle**
- Il n'applique PAS la même logique de découpage que la Preview
- Donc si une page source a des blocs qui dépassent, ils ne sont pas redistribués
- Les blocs sont simplement coupés par `overflow:hidden`

### 4. Blocs qui dépassent

**Frontend (Preview)**:
- Blocs dépassant → redistribués à la page suivante
- Position Y recalculée pour la nouvelle page

**Backend**:
- Blocs dépassant → coupés par `overflow:hidden`
- Position Y conservée (peut être > canvasHeight)

❌ **INCOHÉRENCE MAJEURE**: La Preview et le PDF n'ont pas la même logique de pagination

### 5. Pages vides supplémentaires

**Possible source dans le CSS**:
```java
String breakStyle = isLastPage ? "page-break-after:auto;" : "page-break-after:always;";
```

- Chaque page (sauf la dernière) a `page-break-after:always`
- Si une page est vide → Puppeteer peut générer une page blanche
- Mais ça ne devrait pas créer UNE page entre chaque, sauf s'il y a autre chose

**Analyse**: Probablement lié au fait que le backend N'APPLIQUE PAS le découpage intelligent de la Preview

## 📋 Différences clés Frontend ↔ Backend

| Aspect | Frontend | Backend |
|--------|----------|---------|
| Découpage pages | Automatique (calcul Y/height) | Par page source uniquement |
| Blocs qui dépassent | Redistribués à la page suivante | Coupés (overflow:hidden) |
| Logique pagination | Commune à tous les blocs | Différente pour tableaux en AUTO |
| Marges | Aucune (0px) | Aucune (0mm) |
| Cohérence | À vérifier | À vérifier |

## ✅ Solution proposée

1. **Unifier la logique de pagination**:
   - Créer une classe/service `PageLayoutService` côté frontend
   - Exporter cette logique vers le backend
   - **Ou**: Appliquer la même formule de découpage dans les deux

2. **Normaliser les dimensions**:
   - Créer une constante commune `PageDimensions`
   - Marges explicites (même si 0)
   - Documentation claire

3. **Corriger le découpage des blocs**:
   - Frontend: blocs dépassant → redistribution ✓ (OK actuellement)
   - Backend: doit appliquer la même logique en mode FIXED

4. **Gérer les blocs qui dépassent les limites horizontales**:
   - Ajouter vérification `x + width > canvasWidth`
   - Appliquer clipping CSS ou JavaScript

5. **Tables dynamiques**:
   - Conserver leur logique spéciale
   - Paginer correctement

## 🎯 Implémentation (première étape)

1. ✅ Frontend Preview: déjà correct
2. ❌ Backend: appliquer la même logique de découpage
3. ❌ Éliminer les pages vides
4. ❌ Tester cohérence Preview ↔ PDF
