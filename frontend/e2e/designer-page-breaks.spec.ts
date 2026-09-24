import { test, expect } from '@playwright/test';

test.describe('Designer - Gestion des Ruptures de Page et Répétition des En-têtes (Étape 2)', () => {
  const mockTemplate = {
    id: 'template-breaks-001',
    nom: 'Modèle Facturation Multi-Pages',
    description: 'Test des ruptures de page et répétition des en-têtes',
    version: 1,
    statut: 'BROUILLON',
    dateModification: '2026-09-24T12:00:00',
    categorie: 'FINANCE',
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
              id: 'bloc-table-dynamic',
              type: 'tableau',
              source: '{{lignes}}',
              x: 50,
              y: 50,
              largeurBox: 450,
              hauteurBox: 200,
              repeterEnTeteChaquePage: true,
              eviterCoupureLignes: true,
              colonnes: [
                { variable: 'designation', titre: 'Désignation de la prestation' },
                { variable: 'montant', titre: 'Montant HT' }
              ]
            }
          ]
        }
      ]
    }),
    pages: [],
    variables: [
      { nom: 'lignes', type: 'LISTE', exemple: '[{"designation":"Prestation A","montant":500}]' }
    ]
  };

  test.beforeEach(async ({ page }) => {
    // Initialise la session utilisateur
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-123');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-breaks-001',
          email: 'qa-breaks@test.com',
          nomComplet: 'QA Studio Expert',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-TEST'
        })
      );
      localStorage.setItem('entrepriseCode', 'ENT-TEST');
      localStorage.setItem('onboarding_tour_completed', 'true');
    });

    // Mocks API
    await page.route('**/api/templates/template-breaks-001', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockTemplate)
        });
      } else if (route.request().method() === 'PUT') {
        const body = JSON.parse(route.request().postData() || '{}');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ ...mockTemplate, ...body })
        });
      } else {
        await route.continue();
      }
    });

    await page.route('**/api/templates/template-breaks-001/versions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: 'template-breaks-001', nom: 'Modèle Facturation Multi-Pages', version: 1, children: [] },
          flatHistory: [],
          totalVersions: 1
        })
      });
    });

    await page.route('**/api/templates/template-breaks-001/variables', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { nom: 'lignes', type: 'LISTE', description: 'Liste des prestations' }
        ])
      });
    });

    await page.route('**/api/templates/template-breaks-001/documents', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/templates/template-breaks-001/audit-logs', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });
  });

  test('devrait afficher et basculer les options de répétition d en-tête et rupture de ligne', async ({ page }) => {
    // 1. Accéder au studio designer sur la page du template
    await page.goto('/templates/template-breaks-001');

    // Vérifier que le canvas du designer est visible
    const canvas = page.locator('app-design-canvas .canvas');
    await expect(canvas).toBeVisible({ timeout: 10000 });

    // 2. Sélectionner le bloc tableau sur le canvas
    const tableBlock = canvas.locator('.canvas-block').first();
    await expect(tableBlock).toBeVisible();
    await tableBlock.click();

    // 3. Dans l'inspecteur à droite (app-block-editor), vérifier la présence de la section Ruptures de Page
    const blockEditor = page.locator('app-block-editor');
    await expect(blockEditor).toBeVisible();

    const pageBreaksSection = blockEditor.locator('.page-breaks-section');
    await expect(pageBreaksSection).toBeVisible();

    // Vérifier les libellés des options
    await expect(pageBreaksSection).toContainText("Répéter l'en-tête sur chaque page");
    await expect(pageBreaksSection).toContainText("Éviter la coupure des lignes");

    // Les deux cases doivent être cochées par défaut
    const repeterHeaderCheckbox = pageBreaksSection.locator('input[formControlName="repeterEnTeteChaquePage"]');
    const eviterCoupureCheckbox = pageBreaksSection.locator('input[formControlName="eviterCoupureLignes"]');

    await expect(repeterHeaderCheckbox).toBeChecked();
    await expect(eviterCoupureCheckbox).toBeChecked();

    // 4. Vérifier la prévisualisation (app-block-preview) : thead présent et page-break-inside sur tr
    const previewContainer = page.locator('app-block-preview');
    await expect(previewContainer).toBeVisible();

    const previewThead = previewContainer.locator('thead');
    await expect(previewThead).toBeVisible();

    const tableRows = previewContainer.locator('tbody tr');
    await expect(tableRows.first()).toHaveAttribute('style', /break-inside:avoid/i);

    // 5. Désactiver l'option "Éviter la coupure des lignes" et enregistrer
    await pageBreaksSection.scrollIntoViewIfNeeded();
    const eviterCoupureSlider = pageBreaksSection.locator('.switch').nth(1);
    await eviterCoupureSlider.click();
    await expect(eviterCoupureCheckbox).not.toBeChecked();

    const saveBlockBtn = blockEditor.locator('button.save-btn');
    await expect(saveBlockBtn).toBeVisible();
    await saveBlockBtn.click();

    // 6. Vérifier que la prévisualisation n'a plus break-inside:avoid sur les lignes tbody
    await expect(previewContainer.locator('tbody tr').first()).not.toHaveAttribute('style', /break-inside:avoid/i);
  });
});
