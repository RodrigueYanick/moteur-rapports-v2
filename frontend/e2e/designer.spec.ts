import { test, expect } from '@playwright/test';

test.describe('Studio Designer & Éléments de Modèle', () => {
  test.beforeEach(async ({ page }) => {
    // Initialise une session utilisateur valide
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
    });
  });

  test('Charge un modèle publié et affiche les boutons d action', async ({ page }) => {
    const mockTemplate = {
      id: 'template-001',
      nom: 'Facture Commerciale Standard',
      description: 'Modèle de facturation avec tableau dynamique',
      version: 2,
      statut: 'PUBLIE',
      dateModification: '2026-09-13T12:00:00',
      categorie: 'FINANCE',
      formatPapier: 'A4',
      modePagination: 'FIXED',
      pages: [
        {
          id: 'page-1',
          numero: 1,
          blocks: [
            {
              id: 'block-1',
              type: 'texte',
              x: 20,
              y: 20,
              largeur: 200,
              hauteur: 40,
              contenu: 'FACTURE N° {{ numeroFacture }}',
            },
          ],
        },
      ],
      variables: [
        { nom: 'numeroFacture', type: 'TEXT', valeurParDefaut: 'FAC-2026-001' },
      ],
    };

    await page.route('**/api/templates/template-001', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockTemplate),
      });
    });

    await page.route('**/api/templates/template-001/versions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: 'template-001', nom: 'Facture Commerciale Standard', version: 2, children: [] },
          flatHistory: [],
        }),
      });
    });

    await page.route('**/api/templates/template-001/documents', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/templates/template-001');

    // Vérifie le titre du template
    await expect(page.locator('.detail-header h1')).toContainText('Facture Commerciale Standard');

    // Vérifie les boutons d'action du modèle publié
    await expect(page.locator('.actions-bar')).toBeVisible();
    await expect(page.locator('.actions-bar')).toContainText('Générer par lot');
    await expect(page.locator('.actions-bar')).toContainText('Archiver');
    await expect(page.locator('.actions-bar')).toContainText('Nouvelle version');
  });
});

