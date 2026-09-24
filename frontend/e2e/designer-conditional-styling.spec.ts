import { test, expect } from '@playwright/test';

test.describe('Designer - Formatage Conditionnel (Étape 1)', () => {
  const mockTemplate = {
    id: 'template-cond-001',
    nom: 'Modèle Facturation Conditionnelle',
    description: 'Test du formatage conditionnel visuel et badges dynamiques',
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
              id: 'bloc-texte-statut',
              type: 'texte',
              x: 40,
              y: 40,
              largeurBox: 160,
              hauteurBox: 45,
              contenu: 'Statut : {{statut}}',
              style: { fontSize: 16 }
            },
            {
              id: 'bloc-table-lignes',
              type: 'tableau',
              source: '{{lignes}}',
              x: 40,
              y: 110,
              largeurBox: 400,
              hauteurBox: 120,
              colonnes: [
                { variable: 'designation', titre: 'Désignation' },
                { variable: 'montant', titre: 'Montant' }
              ]
            }
          ]
        }
      ]
    }),
    pages: [],
    variables: [
      { nom: 'statut', type: 'TEXTE', exemple: 'PAYE' },
      { nom: 'lignes', type: 'LISTE', exemple: '[{"designation":"Audit","montant":1200}]' }
    ]
  };

  test.beforeEach(async ({ page }) => {
    // Initialise la session utilisateur
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-123');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-cond-001',
          email: 'qa-designer@test.com',
          nomComplet: 'QA Studio Expert',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-TEST'
        })
      );
      localStorage.setItem('entrepriseCode', 'ENT-TEST');
      localStorage.setItem('onboarding_tour_completed', 'true');
    });

    // Mocks API
    await page.route('**/api/templates/template-cond-001', async (route) => {
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

    await page.route('**/api/templates/template-cond-001/versions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: 'template-cond-001', nom: 'Modèle Facturation Conditionnelle', version: 1, children: [] },
          flatHistory: [],
          totalVersions: 1
        })
      });
    });

    await page.route('**/api/templates/template-cond-001/variables', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { nom: 'statut', type: 'TEXTE', description: 'Statut du paiement' }
        ])
      });
    });

    await page.route('**/api/templates/template-cond-001/documents', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/templates/template-cond-001/audit-logs', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });
  });

  test('devrait configurer une règle conditionnelle et afficher le badge dans la prévisualisation', async ({ page }) => {
    // 1. Accéder au studio designer sur la page du template
    await page.goto('/templates/template-cond-001');

    // Vérifier que le canvas du designer est visible
    const canvas = page.locator('app-design-canvas .canvas');
    await expect(canvas).toBeVisible({ timeout: 10000 });

    // 2. Sélectionner le bloc de texte sur le canvas
    const textBlock = canvas.locator('.canvas-block').first();
    await expect(textBlock).toBeVisible();
    await textBlock.click();

    // 3. Dans l'inspecteur à droite (app-block-editor), vérifier que le bloc est sélectionné
    const blockEditor = page.locator('app-block-editor');
    await expect(blockEditor).toBeVisible();

    // 4. Ouvrir l'accordéon "Formatage conditionnel"
    const condSectionHeader = blockEditor.getByRole('button', { name: /Formatage conditionnel/i });
    await expect(condSectionHeader).toBeVisible();
    await condSectionHeader.click();

    // 5. Cliquer sur le bouton pour ajouter une règle conditionnelle
    const addRuleBtn = blockEditor.getByRole('button', { name: /Ajouter une règle conditionnelle/i });
    await expect(addRuleBtn).toBeVisible();
    await addRuleBtn.click();

    // 6. Remplir les champs de la règle : champ statut, valeur statut, badge SUCCESS
    const ruleCard = blockEditor.locator('.rule-card').first();
    await expect(ruleCard).toBeVisible();

    const champInput = ruleCard.locator('input[placeholder*="statut"]').first();
    await champInput.fill('statut');

    const valeurInput = ruleCard.locator('input[placeholder*="PAYE"]').first();
    await valeurInput.fill('statut');

    const badgeSelect = ruleCard.locator('.badge-select-field select');
    await badgeSelect.selectOption('SUCCESS');

    // 7. Enregistrer les modifications du bloc
    const saveBlockBtn = blockEditor.locator('button.save-btn');
    await expect(saveBlockBtn).toBeVisible();
    await saveBlockBtn.click();

    // 8. Vérifier la prévisualisation temps réel du rapport (app-block-preview)
    const previewContainer = page.locator('app-block-preview');
    await expect(previewContainer).toBeVisible();

    const badgeInPreview = previewContainer.locator('.badge-success');
    await expect(badgeInPreview).toBeVisible();
  });
});
