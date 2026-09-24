import { test, expect } from '@playwright/test';

test.describe('Designer Ergonomie & Confort Visuel', () => {
  const mockTemplate = {
    id: 'template-ergo-001',
    nom: 'Modèle Test Ergonomie',
    description: 'Test des fonctionnalités avancées du studio',
    version: 1,
    statut: 'BROUILLON',
    dateModification: '2026-09-13T12:00:00',
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
              type: 'texte',
              x: 50,
              y: 50,
              largeurBox: 120,
              hauteurBox: 40,
              contenu: 'Texte Initial 1',
            },
            {
              type: 'texte',
              x: 100,
              y: 120,
              largeurBox: 140,
              hauteurBox: 50,
              contenu: 'Texte Initial 2',
            },
          ],
        },
      ],
    }),
    pages: [],
    variables: [],
  };

  test.beforeEach(async ({ page }) => {
    page.on('console', msg => console.log('BROWSER:', msg.text()));
    // Initialise session utilisateur
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

    // Mocks API pour templates
    await page.route('**/api/templates/template-ergo-001', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockTemplate),
      });
    });

    await page.route('**/api/templates/template-ergo-001/versions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: 'template-ergo-001', nom: 'Modèle Test Ergonomie', version: 1, children: [] },
          flatHistory: [],
        }),
      });
    });

    await page.route('**/api/templates/template-ergo-001/pages/**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });

    await page.route('**/api/templates/template-ergo-001/variables', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/templates/template-ergo-001/documents', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });
  });

  test('In-Place Editing: double-clic sur un bloc texte ouvre un textarea inline pour modification directe', async ({ page }) => {
    await page.goto('/templates/template-ergo-001');

    // Attendre que le designer et le canvas soient chargés
    const canvas = page.locator('app-design-canvas .canvas');
    await expect(canvas).toBeVisible();

    const block1 = canvas.locator('.canvas-block').first();
    await expect(block1).toBeVisible();
    await expect(block1).toContainText('Texte Initial 1');

    // Double-clic direct sur le bloc pour démarrer l'édition in-place
    await block1.dblclick();

    // Vérifier l'apparition du textarea d'édition directe
    const inPlaceInput = block1.locator('textarea.in-place-input');
    await expect(inPlaceInput).toBeVisible();

    // Modifier le texte directement sur la feuille virtuelle
    await inPlaceInput.fill('Texte Modifié Sur Place');
    await inPlaceInput.press('Enter');

    // Vérifier que le bloc contient désormais le texte modifié et que le textarea a disparu
    await expect(inPlaceInput).not.toBeVisible();
    await expect(block1).toContainText('Texte Modifié Sur Place');
  });

  test('Sélection multiple & Barre d outils d alignement', async ({ page }) => {
    await page.goto('/templates/template-ergo-001');

    const canvas = page.locator('app-design-canvas .canvas');
    await expect(canvas).toBeVisible();

    const blocks = canvas.locator('.canvas-block');
    await expect(blocks).toHaveCount(2);

    const block1 = blocks.nth(0);
    const block2 = blocks.nth(1);

    // Sélection du premier bloc
    await block1.click();
    await expect(block1).toHaveClass(/selected/);
    await expect(block2).not.toHaveClass(/selected/);

    // Vérifier que la barre d'alignement multiple n'est PAS affichée pour 1 seul bloc
    await expect(page.locator('.alignment-tools')).not.toBeVisible();

    // Multi-sélection avec Shift + Click sur le 2e bloc
    await block2.click({ modifiers: ['Shift'] });

    // Les deux blocs doivent être sélectionnés
    await expect(block1).toHaveClass(/selected/);
    await expect(block2).toHaveClass(/selected/);

    // La barre d'alignement multiple doit maintenant apparaître avec le badge "2 blocs"
    const alignTools = page.locator('.alignment-tools');
    await expect(alignTools).toBeVisible();
    await expect(alignTools.locator('.selection-count-badge')).toContainText('2 blocs');

    // Clic sur le bouton d'alignement à gauche
    const alignLeftBtn = alignTools.locator('button[title="Aligner à gauche"]');
    await expect(alignLeftBtn).toBeVisible();
    await alignLeftBtn.click();

    // Vérifier que les positions X (style left) des deux blocs sont désormais alignées
    const left1 = await block1.evaluate((el) => el.style.left);
    const left2 = await block2.evaluate((el) => el.style.left);
    expect(left1).toBe(left2);
  });

  test('Smart Guides & Snapping: les lignes guides magnétiques apparaissent lors du déplacement à proximité d un autre bloc', async ({ page }) => {
    await page.goto('/templates/template-ergo-001');

    const canvas = page.locator('app-design-canvas .canvas');
    await expect(canvas).toBeVisible();

    const blocks = canvas.locator('.canvas-block');
    const block2 = blocks.nth(1);

    await block2.scrollIntoViewIfNeeded();
    const box2 = await block2.boundingBox();
    expect(box2).not.toBeNull();

    const startX = box2!.x + box2!.width / 2;
    const startY = box2!.y + box2!.height / 2;

    // Glisser le block 2 de 50px vers la gauche pour l'aligner avec block 1 (X=50)
    await page.mouse.move(startX, startY);
    await page.mouse.down();
    await page.mouse.move(startX - 50, startY, { steps: 5 });

    // Le guide magnétique intelligent vertical doit apparaître
    const verticalGuide = canvas.locator('.smart-guide-line.vertical');
    await expect(verticalGuide).toBeVisible();

    // Relâcher la souris
    await page.mouse.up();

    // Le guide doit disparaître après le relâchement
    await expect(verticalGuide).not.toBeVisible();
  });
});

