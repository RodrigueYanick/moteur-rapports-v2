import { test, expect } from '@playwright/test';

test.describe('Personnalisation de la Feuille de Travail (/feuille-travail)', () => {
  test.beforeEach(async ({ page }) => {
    // Initialise une session authentifiée valide
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-workspace');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-009',
          email: 'admin@workspace.com',
          nomComplet: 'Admin Workspace',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-DEMO',
        })
      );
      localStorage.setItem('entrepriseCode', 'ENT-DEMO');
    });
  });

  test('La page charge instantanément sans rester bloquée sur le spinner', async ({ page }) => {
    const mockConfig = {
      codeEntreprise: 'ENT-DEMO',
      formatPapier: 'A4',
      largeurMm: 210,
      hauteurMm: 297,
      modePagination: 'FIXED',
      margeGaucheMm: 15,
      margeDroiteMm: 15,
      margeHautMm: 20,
      margeBasMm: 20,
      couleurFond: '#ffffff',
      headerActif: true,
      hauteurHeaderMm: 18,
      headerContenu: 'En-tête Officiel Entreprise',
      headerAlignement: 'GAUCHE',
      headerAfficherSurPremierePage: true,
      headerLigneSeparation: true,
      headerCouleurLigne: '#3b82f6',
      footerActif: true,
      hauteurFooterMm: 14,
      footerContenu: 'Page {page} / {pages}',
      footerAlignement: 'CENTRE',
      footerAfficherSurPremierePage: true,
      footerLigneSeparation: true,
      footerCouleurLigne: '#cccccc',
      numerotationPage: true,
      formatNumerotation: 'PAGE_X_SUR_Y'
    };

    await page.route('**/api/workspace-config', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockConfig),
        });
      } else {
        await route.continue();
      }
    });

    await page.goto('/feuille-travail');

    // 1. Vérifier que le titre principal s'affiche rapidement
    await expect(page.locator('h1')).toContainText('Personnaliser la feuille de travail');

    // 2. Vérifier que le spinner de chargement n'est plus présent
    await expect(page.locator('.loading-state')).not.toBeVisible();

    // 3. Vérifier que les formulaires et champs sont remplis avec les valeurs reçues
    await expect(page.locator('select#formatPapier')).toHaveValue('A4');
    await expect(page.locator('input#margeGaucheMm')).toHaveValue('15');
    await expect(page.locator('input#margeHautMm')).toHaveValue('20');
    await expect(page.locator('input#headerContenu')).toHaveValue('En-tête Officiel Entreprise');

    // 4. Vérifier que le panneau d'aperçu en direct est visible avec ses métriques
    await expect(page.locator('.preview-panel')).toBeVisible();
    await expect(page.locator('.preview-panel-header')).toContainText('A4');
  });

  test('La page réagit et bascule en mode personnalisé (CUSTOM) en temps réel', async ({ page }) => {
    await page.route('**/api/workspace-config', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            codeEntreprise: 'ENT-DEMO',
            formatPapier: 'A4',
            largeurMm: 210,
            hauteurMm: 297,
            modePagination: 'FIXED',
            margeGaucheMm: 10,
            margeDroiteMm: 10,
            margeHautMm: 10,
            margeBasMm: 10,
            couleurFond: '#ffffff',
            headerActif: false,
            footerActif: false
          }),
        });
      }
    });

    await page.goto('/feuille-travail');
    await expect(page.locator('select#formatPapier')).toBeVisible();

    // Changer le format en CUSTOM
    await page.selectOption('select#formatPapier', 'CUSTOM');

    // Les champs de dimensions libres doivent apparaître immédiatement
    await expect(page.locator('input#largeurMm')).toBeVisible();
    await expect(page.locator('input#hauteurMm')).toBeVisible();

    // Modifier la largeur
    await page.fill('input#largeurMm', '250');
    await page.fill('input#hauteurMm', '350');

    // L'aperçu doit refléter le format CUSTOM
    await expect(page.locator('.badge-format')).toContainText('CUSTOM');
  });

  test('Sauvegarde les modifications avec succès', async ({ page }) => {
    let putPayload: any = null;

    await page.route('**/api/workspace-config', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            codeEntreprise: 'ENT-DEMO',
            formatPapier: 'A4',
            margeGaucheMm: 10,
            margeDroiteMm: 10,
            margeHautMm: 10,
            margeBasMm: 10,
            couleurFond: '#ffffff',
            headerActif: false,
            footerActif: false
          }),
        });
      } else if (route.request().method() === 'PUT') {
        putPayload = route.request().postDataJSON();
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            ...putPayload,
            codeEntreprise: 'ENT-DEMO'
          }),
        });
      }
    });

    await page.goto('/feuille-travail');
    await expect(page.locator('input#margeGaucheMm')).toBeVisible();

    // Modifier la marge gauche
    await page.fill('input#margeGaucheMm', '25');

    // Cliquer sur le bouton Enregistrer
    await page.click('button:has-text("Enregistrer les modifications")');

    // Vérifier l'alerte de succès
    await expect(page.locator('.alert-success')).toBeVisible();
    await expect(page.locator('.alert-success')).toContainText('succès');
    expect(putPayload.margeGaucheMm).toBe(25);
  });
});

