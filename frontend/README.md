# Moteur de Rapports — Frontend Studio (Angular 21)

Ce projet est l'atelier de conception visuelle WYSIWYG et studio de reporting interactif développé avec **Angular 21** (Standalone Components, Signals réactifs, Vite) et **TypeScript 5.8**.

---

## 🏛️ Architecture Modulaire

L'application respecte les principes de la *Clean Feature-Based Architecture* et le principe de responsabilité unique (SOLID) :

```
frontend/src/app/
├── auth/                       # Authentification (Login, Register, Modèle utilisateur)
├── core/                       # Services singletons & configuration globale
├── designer/                   # Studio WYSIWYG de conception
│   ├── block-editor/           # Inspecteur de propriétés de blocs
│   ├── block-preview/          # Prévisualisation dynamique avec Renderers
│   │   └── renderers/          # Pattern Stratégie : Text, Table, Chart, Shape, Barcode
│   ├── components/             # Sous-composants modulaires du studio
│   │   ├── designer-toolbar/   # Barre d'outils (Undo/Redo, Alignements, Zoom, Export)
│   │   └── designer-page-tabs/ # Gestion des onglets multipages (Ajout, Duplication, Renommage)
│   ├── design-canvas/          # Feuille virtuelle et moteur de manipulation drag-and-drop
│   ├── sidebar/                # Panneau latéral de calques, composants et variables
│   ├── services/               # State store (Signals), Presse-papiers, Géométrie, Historique
│   └── index.ts                # Barrel export public du module designer
├── documents/                  # Consultation, diffusion (Email, XLSX, PDF in-app)
├── batches/                    # Traitements par lot, Webhooks & Assistant Excel
├── templates/                  # Galerie & gestion des modèles de rapports
│   ├── starter-templates/      # Gabarits prédéfinis modulaires
│   │   ├── templates/          # Facture, Devis, Attestation, Bulletin de paie, etc.
│   │   └── starter-template.model.ts
│   └── template-detail/        # Vue studio de template
├── shared/                     # Composants, services et directives réutilisables
│   ├── components/             # CommandPalette (Ctrl+K), Toasts, Modales
│   ├── directives/             # Autocomplétion variable {{ in-place
│   ├── services/               # Thème (Clair/Sombre), Toasts, Onboarding
│   └── index.ts                # Barrel export global de la couche shared
└── widget/                     # SDK Web Component <report-designer-widget>
```

---

## 🧭 Path Aliases TypeScript

Pour éviter les chemins relatifs profonds (`../../../`), des alias TypeScript sont configurés dans `tsconfig.json` :

| Alias | Destination | Utilisation |
| :--- | :--- | :--- |
| `@shared/*` | `src/app/shared/*` | UI Kit, Modales, Palette de commande, Toasts |
| `@designer/*` | `src/app/designer/*` | Studio Designer, Canvas, Inspecteur, Renderers |
| `@models/*` | `src/app/models/*` | Modèles de données (Template, Variable, Document) |
| `@services/*` | `src/app/services/*` | Services HTTP backend (TemplateApi, Batch, Auth) |
| `@templates/*` | `src/app/templates/*`| Galerie de modèles et Starter Templates |
| `@auth/*` | `src/app/auth/*` | Parcours de connexion et d'inscription |
| `@guards/*` | `src/app/guards/*` | Protection des routes Angular |
| `@interceptors/*`| `src/app/interceptors/*` | Intercepteur JWT |

---

## 🎨 Moteur de Rendu Modulaire (`BlockHtmlRenderer`)

Dans `src/app/designer/block-preview/renderers/`, le rendu HTML des blocs est découplé via le **Pattern Stratégie** :
- `TextBlockRenderer` : Rendu des titres, paragraphes et séparateurs horizontaux avec substitution dynamique des variables.
- `TableBlockRenderer` : Tableaux statiques matriciels et tableaux dynamiques avec pagination intelligente inter-pages.
- `ChartBlockRenderer` : Graphiques vectoriels SVG (histogrammes, barres).
- `ShapeBlockRenderer` : Rectangles avec arrondis et cercles.
- `BarcodeBlockRenderer` : QR Codes de vérification, codes-barres, signatures manuscrites et images.

---

## 🚀 Commandes de Développement & Validation

### Serveur de développement local
```bash
npm start
# ou ng serve
# Accessible sur http://localhost:4200
```

### Compilation de production
```bash
npm run build
```

### Tests E2E Playwright (26 scénarios complets)
```bash
npm run test:e2e
```
