import { test, expect } from '@playwright/test';

test.describe('Assistant Import Excel / CSV (Glisser-Déposer & Mapping Visuel)', () => {
  test.beforeEach(async ({ page }) => {
    // Session utilisateur simulée
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-excel-wizard');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-009',
          email: 'facturation@acme.com',
          nomComplet: 'Gestionnaire Facturation',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-001',
        })
      );
      localStorage.setItem('entrepriseCode', 'ENT-001');
      localStorage.setItem('onboarding_tour_completed', 'true');
    });
  });

  test('Génération par Lot: Assistant Excel importe un fichier CSV/Excel avec auto-mapping', async ({ page }) => {
    const mockTemplates = [
      { id: 'tpl-facture-pro', nom: 'Facture Commerciale Pro', statut: 'PUBLIE', version: 1 },
    ];

    const mockVariables = [
      { id: 'v1', nomVariable: 'client_nom', type: 'STRING', obligatoire: true, description: 'Nom du client' },
      { id: 'v2', nomVariable: 'total_ttc', type: 'FLOAT', obligatoire: true, description: 'Montant total TTC' },
      { id: 'v3', nomVariable: 'date_facture', type: 'DATE', obligatoire: false, description: 'Date de facturation' },
    ];

    const mockBatches: any[] = [];

    await page.route('**/api/templates**', async (route) => {
      const url = route.request().url();
      if (url.includes('/variables')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockVariables),
        });
      } else if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockTemplates),
        });
      } else {
        await route.continue();
      }
    });

    await page.route('**/api/batches**', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockBatches),
        });
      } else {
        await route.continue();
      }
    });

    await page.goto('/batches');

    // Cliquer sur "+ Nouveau Lot"
    const newBatchBtn = page.getByRole('button', { name: 'Nouveau lot', exact: true });
    await expect(newBatchBtn).toBeVisible();
    await newBatchBtn.click();

    // Vérifier la présence du modal de création de lot
    await expect(page.locator('.modal-card h2', { hasText: 'Lancer une Génération par Lot' })).toBeVisible();

    // Vérifier la présence de la bannière promotionnelle de l'assistant Excel
    const excelPromoBtn = page.locator('button.btn-open-excel-wizard');
    await expect(excelPromoBtn).toBeVisible();
    await expect(excelPromoBtn).toContainText('Importer Excel / CSV');

    // Cliquer pour ouvrir l'assistant d'importation
    await excelPromoBtn.click();

    // Vérifier que le modal d'import Excel s'ouvre
    const wizardModal = page.locator('app-excel-import-modal .modal-card');
    await expect(wizardModal).toBeVisible();
    await expect(wizardModal.locator('h2')).toContainText("Assistant d'Importation Excel / CSV");

    // Étape 1 : Téléversement d'un fichier CSV
    const csvContent = 'Numero Facture,Client,Total TTC,Date\nFAC-2026-101,Entreprise Alpha,1450.50,2026-09-15\nFAC-2026-102,Société Beta,820.00,2026-09-16\nFAC-2026-103,Groupe Gamma,3190.75,2026-09-17';
    
    // Déposer le fichier CSV dans l'input file du modal
    const fileInput = wizardModal.locator('input[type="file"]').first();
    await fileInput.setInputFiles({
      name: 'factures_septembre.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csvContent, 'utf-8'),
    });

    // Vérifier que le fichier est détecté et validé
    await expect(wizardModal.locator('.file-name')).toContainText('factures_septembre.csv');

    // Vérifier que l'aperçu des données est affiché
    await expect(wizardModal.locator('.section-title', { hasText: 'Aperçu des données brutes' })).toBeVisible();

    // Vérifier l'auto-mapping intelligent : "Client" -> client_nom, "Total TTC" -> total_ttc
    const mappingRowClient = wizardModal.locator('.mapping-row').filter({
      has: page.locator('.source-header-name', { hasText: 'Client' }),
    });
    await expect(mappingRowClient.locator('select.target-select')).toHaveValue('client_nom');

    const mappingRowTotal = wizardModal.locator('.mapping-row').filter({
      has: page.locator('.source-header-name', { hasText: 'Total TTC' }),
    });
    await expect(mappingRowTotal.locator('select.target-select')).toHaveValue('total_ttc');

    // Sélectionner la colonne "Numero Facture" comme identifiant unique
    const idColumnSelect = wizardModal.locator('.extra-options-box select.modal-select');
    await expect(idColumnSelect).toBeVisible();
    // Sélectionne la 2ème option (colonne Numero Facture index 0)
    await idColumnSelect.selectOption({ index: 1 });

    // Valider l'importation par lot
    const confirmBtn = wizardModal.locator('button.btn-confirm');
    await expect(confirmBtn).toContainText('Importer 3 rapports dans le lot');
    await confirmBtn.click();

    // Le modal Excel doit se fermer
    await expect(wizardModal).not.toBeVisible();

    // Le textarea JSON du modal parent doit être rempli automatiquement
    const jsonTextarea = page.locator('textarea.modal-textarea');
    const jsonValue = await jsonTextarea.inputValue();
    expect(jsonValue).toContain('FAC-2026-101');
    expect(jsonValue).toContain('Entreprise Alpha');
    expect(jsonValue).toContain('1450.5');

    // Vérifier le badge de détection de rapports
    await expect(page.locator('.count-badge')).toContainText('✓ 3 rapports détectés');

    // Vérifier le message de succès de l'assistant
    await expect(page.locator('.excel-success-banner')).toContainText('3 rapports importés avec succès');
  });

  test('Template Filler: Importer des données Excel pour pré-remplir le formulaire de test', async ({ page }) => {
    const templateId = 'tpl-filler-test';

    const mockTemplate = {
      id: templateId,
      nom: 'Devis Prestation',
      description: 'Modèle devis pour import excel',
      statut: 'BROUILLON',
      version: 1,
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
              contenu: 'Client: {{ client_nom }} - Montant: {{ montant }}',
            },
          ],
        },
      ],
    };

    const mockVariables = [
      { id: 'v1', nomVariable: 'client_nom', type: 'STRING', obligatoire: true, description: 'Nom du client' },
      { id: 'v2', nomVariable: 'montant', type: 'FLOAT', obligatoire: true, description: 'Montant du devis' },
    ];

    await page.route(`**/api/templates/${templateId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockTemplate),
      });
    });

    await page.route(`**/api/templates/${templateId}/versions`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: templateId, nom: 'Devis Prestation', version: 1, children: [] },
          flatHistory: [],
        }),
      });
    });

    await page.route(`**/api/templates/${templateId}/variables`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockVariables),
      });
    });

    await page.route(`**/api/templates/${templateId}/documents`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.goto(`/templates/${templateId}`);

    // Ouvrir le volet "Remplir le modèle" via le bouton du designer
    const fillModeBtn = page.getByRole('button', { name: 'Remplir (test)' }).first();
    await expect(fillModeBtn).toBeVisible();
    await fillModeBtn.click();

    // Vérifier la présence du bouton "Importer Excel" dans le panneau de remplissage
    const importExcelBtn = page.locator('button.btn-import-excel');
    await expect(importExcelBtn).toBeVisible();
    await importExcelBtn.click();

    // Vérifier l'ouverture du modal Excel
    const wizardModal = page.locator('app-excel-import-modal .modal-card');
    await expect(wizardModal).toBeVisible();

    // Téléverser un fichier CSV avec deux clients
    const csvContent = 'Client,Montant\nAcme Corp,5400\nGlobex Industries,8900';
    const fileInput = wizardModal.locator('input[type="file"]').first();
    await fileInput.setInputFiles({
      name: 'clients_prospects.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csvContent, 'utf-8'),
    });

    // Vérifier que le sélecteur de ligne est affiché en mode single-row
    const rowSelector = wizardModal.locator('.extra-options-box select.modal-select');
    await expect(rowSelector).toBeVisible();

    // Sélectionner la 2ème ligne (Globex Industries)
    await rowSelector.selectOption({ index: 1 });

    // Cliquer sur le bouton de confirmation
    const submitFillBtn = wizardModal.locator('button.btn-confirm');
    await expect(submitFillBtn).toContainText('Appliquer la ligne 2 au modèle');
    await submitFillBtn.click();

    // Le modal se ferme
    await expect(wizardModal).not.toBeVisible();

    // Les champs du formulaire dans template-filler doivent être automatiquement complétés
    const clientInput = page.locator('input[placeholder*="client_nom"]');
    await expect(clientInput).toHaveValue('Globex Industries');

    const montantInput = page.locator('input[placeholder*="montant"]');
    await expect(montantInput).toHaveValue('8900');
  });
});
