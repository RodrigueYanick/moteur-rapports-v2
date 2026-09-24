import { test, expect } from '@playwright/test';

test.describe('Traitements par Lot & Webhooks E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-batch');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-001',
          email: 'operator@acme.com',
          nomComplet: 'Opérateur Facturation',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-001',
        })
      );
      localStorage.setItem('entrepriseCode', 'ENT-001');
    });
  });

  test('Affiche le tableau de bord des lots et permet de configurer un nouveau lot', async ({ page }) => {
    const mockTemplates = [
      { id: 'tpl-1', nom: 'Relevé Mensuel', statut: 'PUBLIE', version: 1 },
    ];

    const mockBatches = [
      {
        id: 'batch-abc-123',
        templateId: 'tpl-1',
        templateNom: 'Relevé Mensuel',
        statut: 'TERMINE',
        totalItems: 5,
        processedItems: 5,
        successCount: 5,
        failureCount: 0,
        progressionPourcentage: 100.0,
        webhookUrl: 'https://erp.acme.com/hook',
        webhookStatut: 'ENVOYE',
        webhookTentatives: 1,
        dateCreation: '2026-09-13T14:30:00',
        dureeSecondes: 4,
      },
    ];

    await page.route('**/api/templates**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockTemplates),
      });
    });

    await page.route('**/api/batches', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockBatches),
        });
      } else if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 202,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'batch-new-999',
            templateId: 'tpl-1',
            templateNom: 'Relevé Mensuel',
            statut: 'EN_ATTENTE',
            totalItems: 3,
            processedItems: 0,
            successCount: 0,
            failureCount: 0,
            progressionPourcentage: 0.0,
            webhookStatut: 'EN_ATTENTE',
            dateCreation: new Date().toISOString(),
          }),
        });
      }
    });

    await page.route('**/api/batches/webhooks/test', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          succes: true,
          statusCode: 200,
          message: 'Connectivité validée avec succès',
          tempsReponseMs: 42,
        }),
      });
    });

    await page.goto('/batches');

    // Vérifie le titre et les cartes KPI
    await expect(page.locator('h1')).toContainText('Génération par Lot & Webhooks');
    await expect(page.locator('.kpi-grid')).toBeVisible();

    // Vérifie la présence du lot existant avec son bouton Archive ZIP
    await expect(page.locator('.template-name')).toContainText('Relevé Mensuel');
    await expect(page.locator('.btn-zip')).toBeVisible();

    // Ouvre le modal de création
    await page.click('button:has-text("Nouveau lot")');
    await expect(page.locator('.modal-card')).toBeVisible();

    // Clique sur "Charger un exemple"
    await page.click('button:has-text("Charger un exemple")');
    await expect(page.locator('.count-badge')).toContainText('rapports détectés');

    // Teste le webhook
    await page.fill('input[type="url"]', 'https://erp.acme.com/api/test-webhook');
    await page.click('button:has-text("Tester l\'URL")');
    await expect(page.locator('.test-result-box')).toContainText('Connectivité validée');
  });
});

