import { test, expect } from '@playwright/test';

test.describe('Designer - Filigranes Dynamiques (Watermarks - Étape 3)', () => {
  const mockTemplate = {
    id: 'template-watermark-001',
    nom: 'Rapport Commercial avec Filigrane',
    description: 'Test du filigrane dynamique en atelier designer',
    version: 1,
    statut: 'BROUILLON',
    dateModification: '2026-09-24T12:00:00',
    categorie: 'COMMERCIAL',
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
              id: 'bloc-texte-01',
              type: 'texte',
              contenu: 'Contenu contractuel confidentiel',
              x: 50,
              y: 50,
              largeurBox: 300,
              hauteurBox: 40
            }
          ]
        }
      ],
      watermark: {
        actif: false,
        texte: 'SPECIMEN',
        type: 'TEXTE',
        rotation: -45,
        opacite: 15,
        couleur: '#94a3b8',
        fontSize: 54,
        afficherSur: 'TOUTES'
      }
    }),
    pages: [],
    variables: [
      { nom: 'statut', type: 'TEXTE', exemple: 'PROVISOIRE' }
    ]
  };

  test.beforeEach(async ({ page }) => {
    // Initialiser la session utilisateur
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-watermark');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-watermark-001',
          email: 'qa-watermark@test.com',
          nomComplet: 'QA Watermark Tester',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-TEST'
        })
      );
      localStorage.setItem('entrepriseCode', 'ENT-TEST');
      localStorage.setItem('onboarding_tour_completed', 'true');
    });

    // Mocks API
    await page.route('**/api/templates/template-watermark-001', async (route) => {
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

    await page.route('**/api/templates/template-watermark-001/versions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: 'template-watermark-001', nom: 'Rapport Commercial avec Filigrane', version: 1, children: [] },
          flatHistory: [],
          totalVersions: 1
        })
      });
    });

    await page.route('**/api/templates/template-watermark-001/variables', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { nom: 'statut', type: 'TEXTE', description: 'Statut du document' }
        ])
      });
    });

    await page.route('**/api/templates/template-watermark-001/documents', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/templates/template-watermark-001/audit-logs', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });
  });

  test('devrait configurer un filigrane dynamique et l afficher sur le canvas et l apercu', async ({ page }) => {
    // 1. Accéder au studio designer sur la page du template
    await page.goto('/templates/template-watermark-001');

    // Vérifier que le canvas du designer est visible
    const canvas = page.locator('app-design-canvas .canvas');
    await expect(canvas).toBeVisible({ timeout: 10000 });

    // Initialement, aucun filigrane ne doit être affiché
    await expect(canvas.locator('.watermark-layer')).not.toBeVisible();

    // 2. Ouvrir le modal Paramètres de page via la toolbar
    const settingsBtn = page.locator('.paper-select');
    await expect(settingsBtn).toBeVisible();
    await settingsBtn.click();

    const modal = page.locator('.modal-content');
    await expect(modal).toBeVisible();
    await expect(modal).toContainText('Paramètres de mise en page');
    await expect(modal).toContainText('Filigrane (Watermark)');

    // 3. Activer le filigrane
    const watermarkSection = modal.locator('.watermark-settings-section');
    await watermarkSection.scrollIntoViewIfNeeded();
    const watermarkSlider = watermarkSection.locator('.slider');
    await watermarkSlider.click();

    // Renseigner le texte dynamique avec variable {{statut}}
    const textInput = modal.locator('[data-testid="watermark-text-input"]');
    await expect(textInput).toBeVisible();
    await textInput.fill('CONFIDENTIEL - {{statut}}');

    // 4. Cliquer sur Appliquer
    const applyBtn = modal.locator('button.primary-btn').filter({ hasText: 'Appliquer' });
    await applyBtn.click();
    await expect(modal).not.toBeVisible();

    // 5. Vérifier l'affichage du filigrane sur le canvas de conception
    const canvasWatermark = canvas.locator('.watermark-layer');
    await expect(canvasWatermark).toBeVisible();
    await expect(canvasWatermark).toContainText('CONFIDENTIEL - {{statut}}');

    // 6. Vérifier l'affichage du filigrane sur le panneau d'aperçu (Block Preview)
    const previewContainer = page.locator('app-block-preview');
    await expect(previewContainer).toBeVisible();

    const previewWatermark = previewContainer.locator('.watermark-layer');
    await expect(previewWatermark).toBeVisible();
    // Dans l'aperçu, les variables sont résolues dynamiquement via mock data ou conservées si non trouvées
    await expect(previewWatermark).toContainText('CONFIDENTIEL -');
  });
});

