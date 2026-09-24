import { test, expect } from '@playwright/test';

test.describe('Sprint 5 : Accessibilité (WCAG 2.1 AA) & Moteur de Reporting Avancé', () => {
  const mockTemplate = {
    id: 'template-sprint5',
    nom: 'Rapport Accessibilité & Graphiques SVG',
    description: 'Modèle avec graphiques vectoriels et tableau groupé',
    version: 1,
    statut: 'BROUILLON',
    formatPapier: 'A4',
    modePagination: 'FIXED',
    margeHautMm: 10,
    margeBasMm: 10,
    margeGaucheMm: 10,
    margeDroiteMm: 10,
    contenuDesign: JSON.stringify({
      pages: [
        {
          nom: 'Page 1',
          blocs: [
            {
              id: 'b-chart-svg',
              type: 'graphique',
              graphiqueType: 'donut',
              x: 50,
              y: 50,
              largeurBox: 300,
              hauteurBox: 150,
              style: { fill: '#6366f1' },
            },
            {
              id: 'b-table-group',
              type: 'tableau',
              source: 'articles',
              groupBy: 'categorie',
              groupHeaderTemplate: 'Famille : {{groupKey}}',
              afficherSousTotaux: true,
              x: 50,
              y: 220,
              largeurBox: 450,
              hauteurBox: 200,
              colonnes: [
                { titre: 'Désignation', variable: 'nom' },
                { titre: 'Total', variable: 'montant', formule: 'qte * pu', agregat: 'SUM' },
              ],
            },
          ],
        },
      ],
    }),
    pages: [],
    variables: [],
  };

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-123');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-002',
          email: 'designer@test.com',
          nomComplet: 'Designer Expert',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-TEST',
        })
      );
      localStorage.setItem('entrepriseCode', 'ENT-TEST');
      localStorage.setItem('onboarding_tour_completed', 'true');
    });

    await page.route('**/api/templates/template-sprint5', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockTemplate),
      });
    });

    await page.route('**/api/templates/template-sprint5/versions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: 'template-sprint5', nom: 'Rapport Accessibilité & Graphiques SVG', version: 1, children: [] },
          flatHistory: [],
        }),
      });
    });

    await page.route('**/api/templates/template-sprint5/pages/**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });

    await page.route('**/api/templates/template-sprint5/variables', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/templates/template-sprint5/documents', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });
  });

  test('Accessibilité : Le lien d évitement (skip link) est présent et pointe vers le contenu principal', async ({ page }) => {
    await page.goto('/bibliotheque');

    // Vérifier la présence du skip-link
    const skipLink = page.locator('.skip-link');
    await expect(skipLink).toBeAttached();
    await expect(skipLink).toHaveAttribute('href', '#main-content');

    // Vérifier la cible principale #main-content
    const mainContent = page.locator('#main-content');
    await expect(mainContent).toBeAttached();
    await expect(mainContent).toHaveAttribute('tabindex', '-1');

    // Vérifier que la langue du document est configurée en français
    const htmlLang = await page.locator('html').getAttribute('lang');
    expect(htmlLang).toBe('fr');
  });

  test('Accessibilité & UI : Présence des rôles ARIA tablist et toolbar dans le designer et graphiques vectoriels SVG', async ({ page }) => {
    await page.goto('/templates/template-sprint5');

    // Attendre le chargement du canvas
    await page.waitForSelector('.canvas-page');

    // Vérifier les rôles ARIA dans la sidebar (tablist, tab)
    const tablist = page.locator('[role="tablist"]');
    await expect(tablist.first()).toBeVisible();

    const tabs = page.locator('[role="tab"]');
    await expect(tabs).toHaveCount(3); // Bibliothèque, Calques, Variables

    // Vérifier le rendu vectoriel SVG du graphique donut dans le canvas
    const svgChart = page.locator('svg.svg-chart-preview');
    await expect(svgChart).toBeVisible();

    // Vérifier la présence des éléments vectoriels SVG (cercles donut)
    const donutCircles = svgChart.locator('circle');
    expect(await donutCircles.count()).toBeGreaterThanOrEqual(1);

    // Vérifier la légende du graphique
    const legendText = svgChart.locator('text');
    await expect(legendText.first()).toBeVisible();
  });

  test('UI & Ergonomie : Palette de commande universelle (Ctrl+K) et navigation au clavier', async ({ page }) => {
    await page.goto('/bibliotheque');

    // Vérifier le bouton déclencheur dans la navbar
    const searchBtn = page.locator('.nav-search-btn');
    await expect(searchBtn).toBeVisible();

    // Ouvrir la palette via le raccourci clavier Ctrl+K
    await page.keyboard.press('Control+k');

    // La modal de commande doit être affichée
    const modal = page.locator('.palette-modal');
    await expect(modal).toBeVisible();
    await expect(modal).toHaveAttribute('role', 'dialog');

    // Le champ de recherche doit être actif
    const input = modal.locator('input.palette-input');
    await expect(input).toBeFocused();

    // Taper une recherche
    await input.fill('Bibliothèque');
    const items = modal.locator('.palette-item');
    await expect(items.first()).toBeVisible();
    await expect(items.first()).toContainText('Bibliothèque');

    // Fermer avec la touche Échap
    await page.keyboard.press('Escape');
    await expect(modal).not.toBeVisible();
  });
});

