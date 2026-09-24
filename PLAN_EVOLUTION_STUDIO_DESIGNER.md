# 🚀 Plan Directeur d'Évolution du Studio Designer WYSIWYG

> **Document de Référence Technique & Spécifications d'Implémentation**  
> Ce document décrit de manière exhaustive l'architecture, la modélisation des données, les composants frontend/backend et la stratégie de test (unitaire, intégration, régression) pour les **4 évolutions majeures du Studio Designer**.

---

## Sommaire

1. [Vue d'Ensemble & Objectifs](#1-vue-densemble--objectifs)
2. [Étape 1 : Règles de Formatage Conditionnel Visuel (Conditional Styling)](#étape-1--règles-de-formatage-conditionnel-visuel-conditional-styling)
3. [Étape 2 : Gestion des Ruptures de Page & Répétition des En-têtes (Multi-Page Header Repeat)](#étape-2--gestion-des-ruptures-de-page--répétition-des-en-têtes-multi-page-header-repeat)
4. [Étape 3 : Filigranes Dynamiques (Watermarks)](#étape-3--filigranes-dynamiques-watermarks)
5. [Étape 4 : Bloc Signature Électronique / Manuscrite](#étape-4--bloc-signature-électronique--manuscrite)
6. [Stratégie de Qualification, Tests & Non-Régression (QA)](#stratégie-de-qualification-tests--non-régression-qa)
7. [Matrice des Fichiers Impactés](#matrice-des-fichiers-impactés)

---

## 1. Vue d'Ensemble & Objectifs

L'objectif de cette évolution est de hisser le **Studio Designer** au niveau des leaders du marché (CraftMyPDF, Jaspersoft, Docupilot) en apportant :
* **Une expressivité visuelle sans code** (formatage conditionnel, filigranes).
* **Une conformité documentaire irréprochable** sur les rapports volumineux (ruptures de pages propres avec répétition d'en-tête et sous-totaux de report).
* **Une valeur légale renforcée** pour les devis, contrats et factures (blocs signatures électroniques et manuscrites).

Chaque étape sera validée individuellement par :
* Des **tests unitaires** (logique de condition, calculs et parseurs).
* Des **tests d'intégration backend** (Spring Boot MockMvc, compilation PDF Gotenberg).
* Des **tests E2E Playwright** (interactions réelles dans le Studio, aperçu réactif et export).

---

## Étape 1 : Règles de Formatage Conditionnel Visuel (Conditional Styling)

### 1.1 Principe & Cas d'Usage
Permettre aux utilisateurs de définir des règles visuelles dynamiques appliquées aux blocs et aux cellules de tableaux selon la valeur des données à l'exécution :
* *Finances* : Afficher en **rouge et gras** si `solde_du > 0`, ou en **vert** si `statut == 'PAYE'`.
* *Stocks / Alertes* : Afficher un **badge coloré** (Rouge = Critique, Orange = Réapprovisionnement, Vert = Conforme).
* *Tableaux* : Mettre en évidence les lignes de sous-totaux ou les totaux négatifs avec un fond grisé ou coloré.

### 1.2 Modélisation des Données (TypeScript & Java)

#### Modèle TypeScript : `ConditionalStyleRule`
```typescript
export type ConditionOperator =
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'GREATER_THAN'
  | 'GREATER_OR_EQUAL'
  | 'LESS_THAN'
  | 'LESS_OR_EQUAL'
  | 'CONTAINS'
  | 'STARTS_WITH'
  | 'IS_EMPTY'
  | 'IS_NOT_EMPTY';

export interface ConditionalStyleEffect {
  color?: string;
  backgroundColor?: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  badgeStyle?: 'NONE' | 'SUCCESS' | 'WARNING' | 'DANGER' | 'INFO';
}

export interface ConditionalStyleRule {
  id: string;
  champ: string;              // ex: "total_ttc", "statut", "solde"
  operateur: ConditionOperator; // Opérateur de comparaison
  valeur: any;                // Valeur cible (ex: 0, "PAYE", "ANNULE")
  effet: ConditionalStyleEffect; // Styles appliqués si la règle est vérifiée
}
```

* Ajout de `conditionalStyles?: ConditionalStyleRule[];` dans `DesignBlock` et `TableColumn`.

### 1.3 Implémentation Frontend
1. **Inspecteur de Bloc (`BlockEditorComponent`)** :
   * Ajout d'une nouvelle section accordéon : **🎨 Formatage Conditionnel**.
   * Liste des règles actives avec bouton `+ Ajouter une règle`.
   * Formulaire interactif : Sélecteur de champ / variable, sélecteur d'opérateur, champ valeur, sélecteurs de couleurs et cases Gras/Italique/Badge.
2. **Moteur de Prévisualisation (`BlockPreviewComponent` & Renderers)** :
   * Création d'un service utilitaire `ConditionEvaluatorService` dans `@designer/services`.
   * Application réactive des styles en temps réel sur la feuille virtuelle lors de la saisie dans le *Template Filler*.

### 1.4 Implémentation Backend (`TemplateHtmlBuilder.java`)
* Ajout d'un évaluateur de règles conditionnelles dans `TemplateHtmlBuilder`.
* Lors du rendu HTML de chaque bloc ou cellule de tableau :
  * Si la règle est satisfaite, fusion des styles inline CSS (`color`, `background-color`, `font-weight: bold`, `text-decoration: underline`).
  * Si `badgeStyle` est défini : encapsulation du texte dans un badge stylisé (`<span class="badge badge-success">...</span>`).

---

## Étape 2 : Gestion des Ruptures de Page & Répétition des En-têtes (Multi-Page Header Repeat)

### 2.1 Principe & Cas d'Usage
Sur les documents volumineux (factures multi-pages de 100 articles, bilans d'activités, relevés bancaires) :
* L'en-tête du tableau (`Désignation | Qté | Prix Unitaire | Total`) doit obligatoirement **se répéter au sommet de chaque nouvelle page**.
* Possibilité d'afficher un **report intermédiaire en bas de page** (*"Report page précédente"* / *"À reporter"*).
* Empêcher qu'une ligne de tableau ne soit coupée en deux entre deux pages (règle CSS `page-break-inside: avoid`).

### 2.2 Modélisation des Données
```typescript
// Extension des propriétés de bloc tableau
export interface TablePaginationConfig {
  repeterEnTeteSurChaquePage: boolean; // Par défaut true
  afficherReportIntermediaire: boolean; // Par défaut false
  variableSousTotalReport?: string;   // ex: "montant_ht"
  texteReportBas?: string;             // ex: "À reporter"
  texteReportHaut?: string;            // ex: "Report page précédente"
}
```

### 2.3 Implémentation Frontend
* Dans l'inspecteur du bloc tableau : Section **📑 Pagination & Rupture de page**.
* Contrôles : Checkbox *Répéter l'en-tête sur chaque page*, Checkbox *Activer les reports de totaux*.

### 2.4 Implémentation Backend (`TemplateHtmlBuilder.java`)
1. **Structure HTML sémantique pour Gotenberg / Flying Saucer** :
   * Utilisation stricte de `<thead>`, `<tbody>` et `<tfoot>`.
   * Application de règles CSS Paged Media :
     ```css
     thead { display: table-header-group; }
     tfoot { display: table-footer-group; }
     tr { page-break-inside: avoid; }
     ```
2. **Calcul des sous-totaux de report dynamiques** :
   * Dans `splitTableIntoFragments` : pour chaque page intermédiaire, injection d'une ligne de bas de page avec la somme cumulée des lignes de la page, et d'une ligne de haut de page sur le fragment suivant.

---

## Étape 3 : Filigranes Dynamiques (Watermarks)

### 3.1 Principe & Cas d'Usage
Apposer une mention transversale ou diagonale d'arrière-plan sur les pages du document :
* Statut légal : *"CONFIDENTIEL"*, *"BROUILLON"*, *"DUPLICATA"*, *"SPECIMEN"*.
* Dynamique par variable : `{{ statut_document }}` (affiche automatiquement *"ANNULÉ"* ou *"PAYÉ"* selon la donnée reçue).

### 3.2 Modélisation des Données
```typescript
export interface PageWatermark {
  actif: boolean;
  texte: string;          // ex: "CONFIDENTIEL" ou "{{ statut_facture }}"
  angle?: number;         // ex: -45 deg (défaut)
  opacite?: number;       // ex: 0.15 (défaut)
  couleur?: string;       // ex: "#94a3b8" (gris) ou "#ef4444" (rouge)
  taillePolice?: number;  // ex: 72 px
  repeterSurToutesLesPages?: boolean;
}
```
* Ajout de `watermark?: PageWatermark;` dans `DesignPage` et dans les propriétés globales du modèle `ReportTemplate`.

### 3.3 Implémentation Frontend
1. **Barre d'outils / Configuration de page (`DesignerToolbarComponent`)** :
   * Nouveau bouton ou modal de paramétrage de page : **💧 Filigrane**.
   * Sélecteurs : Texte libre ou Variable, Angle (-45°, 0°, 45°), Curseur d'opacité (5% à 100%), Sélecteur de couleur.
2. **Toile Virtuelle (`DesignCanvasComponent`)** :
   * Affichage en arrière-plan d'un élément SVG ou texte pivoté CSS derrière les blocs sans gêner la sélection :
     ```html
     <div class="canvas-watermark" *ngIf="page.watermark?.actif">
       {{ evaluateWatermark(page.watermark.texte) }}
     </div>
     ```

### 3.4 Implémentation Backend (`TemplateHtmlBuilder.java`)
* Injection du conteneur de filigrane dans chaque page HTML générée avec positionnement absolu centré, rotation CSS et `z-index: 0` :
  ```css
  .watermark-container {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%) rotate(-45deg);
    font-size: 72px;
    font-weight: 800;
    text-transform: uppercase;
    color: #94a3b8;
    opacity: 0.15;
    pointer-events: none;
    z-index: 0;
    user-select: none;
    white-space: nowrap;
  }
  ```

---

## Étape 4 : Bloc Signature Électronique / Manuscrite

### 4.1 Principe & Cas d'Usage
Permettre d'insérer un cartouche de signature certifié ou d'émargement :
* Mention légale personnalisable : *"Lu et approuvé, bon pour accord le {{ date }}"*.
* Zone de signature manuscrite tactile / souris ou tampon d'entreprise (image).
* Cartouche avec nom du signataire et qualité (*"Le Directeur Général"*).

### 4.2 Modélisation des Données
```typescript
export interface SignatureBlockConfig {
  mentionLegale: string;         // "Lu et approuvé, bon pour accord"
  signataireNom?: string;        // "{{ signataire_nom }}"
  signataireQualite?: string;    // "Directeur Général"
  dateSignature?: string;        // "{{ date_signature }}"
  modeSignature: 'MANUSCRITE' | 'IMAGE' | 'CADRE_VIERGE';
  signatureImageUrl?: string;    // URL ou base64
  afficherCadre: boolean;
  cadrePointille: boolean;
}
```

### 4.3 Implémentation Frontend
1. **Palette d'outils (`SidebarComponent`)** :
   * Bouton composant **✍️ Signature** ajouté aux blocs disponibles.
2. **Inspecteur de Bloc (`BlockEditorComponent`)** :
   * Onglet spécifique avec mention légale, nom du signataire, et **modal de signature manuscrite HTML5 Canvas** pour signer directement à l'écran.
3. **Renderer Dédié (`SignatureBlockRenderer`)** :
   * Composant de rendu Strategy Pattern affichant le cartouche, la ligne de signature, et l'image ou le tracé vectoriel.

### 4.4 Implémentation Backend (`TemplateHtmlBuilder.java`)
* Prise en charge du type `signature` avec rendu CSS soigné :
  * Cadre avec bordure continue ou pointillée.
  * Mention légale en italique.
  * Image de la signature ou espace d'émargement propre.
  * Nom et qualité alignés.

---

## Stratégie de Qualification, Tests & Non-Régression (QA)

Pour **chaque étape**, le protocole de vérification strict suivant sera appliqué avant de passer à l'étape suivante :

1. **Tests Unitaires Frontend (Jasmine / Karma)** :
   * Test de la logique d'évaluation des conditions et styles.
   * Test du composant Canvas de signature et de l'encodage base64.
2. **Tests Unitaires Backend (JUnit 5 & AssertJ)** :
   * Tests de `TemplateHtmlBuilder` validant que le HTML produit contient bien les classes CSS conditionnelles, les filigranes et les balises `<thead/tfoot>`.
3. **Tests d'Intégration & Non-Régression Globale** :
   * Exécution de la suite backend : `mvn clean test` (97+ tests doivent réussir à 100%).
   * Exécution de la suite frontend : `npm run build` (0 warning critique, 0 erreur de typage).
4. **Nouveaux Tests E2E Playwright (.spec.ts)** :
   * `e2e/designer-conditional-styling.spec.ts` (Étape 1).
   * `e2e/designer-multipage-header-repeat.spec.ts` (Étape 2).
   * `e2e/designer-watermark.spec.ts` (Étape 3).
   * `e2e/designer-signature-block.spec.ts` (Étape 4).
   * Maintien du taux de succès à **100% sur l'ensemble de la suite Playwright**.

---

## Matrice des Fichiers Impactés

| Fichier | Couche | Rôle |
| :--- | :--- | :--- |
| `frontend/src/app/designer/models/design-block.model.ts` | Frontend Model | Définition des types `ConditionalStyleRule`, `PageWatermark`, `SignatureBlockConfig` |
| `frontend/src/app/designer/block-editor/block-editor.html` & `.ts` | Frontend UI | Formulaires d'édition des règles conditionnelles, filigranes et signatures |
| `frontend/src/app/designer/block-preview/renderers/` | Frontend Render | Renderers de blocs avec application des styles conditionnels et nouveau `SignatureBlockRenderer` |
| `frontend/src/app/designer/design-canvas/design-canvas.html` | Frontend Canvas | Rendu visuel du filigrane d'arrière-plan |
| `frontend/src/app/designer/sidebar/sidebar.html` & `.ts` | Frontend Palette | Ajout du bloc interactif Signature dans la palette |
| `backend/src/main/java/com/rapports/moteur/service/TemplateHtmlBuilder.java` | Backend Engine | Injection des styles conditionnels, fragmentation des en-têtes avec répétition, filigranes HTML/CSS et blocs signature |
| `backend/src/test/java/com/rapports/moteur/service/TemplateHtmlBuilderTest.java` | Backend Test | Tests JUnit des nouveaux comportements de rendu |
| `frontend/e2e/*.spec.ts` | QA E2E | Scénarios Playwright de non-régression |

