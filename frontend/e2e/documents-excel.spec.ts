import { test, expect } from '@playwright/test';

test.describe('Documents & Export Excel (.xlsx) E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-docs');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-003',
          email: 'user@acme.com',
          nomComplet: 'Utilisateur Entreprise',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ACME',
        })
      );
      localStorage.setItem('entrepriseCode', 'ACME');
    });
  });

  test('Affiche les documents et propose l export Excel', async ({ page }) => {
    const mockDocuments = [
      {
        id: 'doc-101',
        nom: 'Bilan Comptable T3',
        templateId: 'tpl-101',
        templateNom: 'Modèle Bilan',
        statut: 'FINALISE',
        dateCreation: '2026-09-13T10:00:00',
        dateModification: '2026-09-13T10:05:00',
        donnees: { chiffreAffaires: 50000 },
      },
    ];

    await page.route('**/api/documents**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockDocuments),
      });
    });

    await page.route('**/api/templates**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/documents');

    // Vérifie le titre de la page et le nom du document
    await expect(page.locator('h1')).toContainText('Mes documents');
    await expect(page.locator('h3')).toContainText('Bilan Comptable T3');

    // Vérifie la présence du bouton d'export Excel
    const excelBtn = page.locator('button:has-text("Excel")');
    await expect(excelBtn).toBeVisible();
  });
});

