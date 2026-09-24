import { test, expect } from '@playwright/test';

test.describe('Galerie de Starter Templates & Autocomplétion de Variables', () => {
  const createdTemplateId = 'tpl-starter-test-001';

  test.beforeEach(async ({ page }) => {
    // Initialise session utilisateur
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-starter');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-003',
          email: 'creator@test.com',
          nomComplet: 'Creator Test',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-TEST',
        })
      );
      localStorage.setItem('entrepriseCode', 'ENT-TEST');
      localStorage.setItem('onboarding_tour_completed', 'true');
    });
  });

  test('Galerie Starter Templates: affiche les modèles, filtre par catégorie et sélectionne un modèle', async ({ page }) => {
    await page.goto('/templates/new');

    // Vérifier l'en-tête et les titres
    await expect(page.locator('h1')).toContainText('Nouveau modèle de rapport');
    await expect(page.locator('.section-title h2')).toContainText('Choisissez un point de départ');

    // Vérifier la présence de la carte Feuille vierge et des 6 modèles types (total 7)
    const cards = page.locator('.starter-card');
    await expect(cards).toHaveCount(7);

    const blankCard = page.locator('.starter-card.blank-card');
    await expect(blankCard).toBeVisible();
    await expect(blankCard).toContainText('Feuille vierge');

    // Vérifier les starters types
    await expect(page.locator('.starter-card', { hasText: 'Facture commerciale' })).toBeVisible();
    await expect(page.locator('.starter-card', { hasText: 'Reçu / Bon de commande' })).toBeVisible();
    await expect(page.locator('.starter-card', { hasText: 'Bulletin de paie' })).toBeVisible();
    await expect(page.locator('.starter-card', { hasText: 'Rapport d’activité' })).toBeVisible();
    await expect(page.locator('.starter-card', { hasText: 'Certificat / Attestation' })).toBeVisible();
    await expect(page.locator('.starter-card', { hasText: 'Rapport d’Avancement' })).toBeVisible();

    // Tester le filtrage par catégorie
    await page.click('button.tab-btn:has-text("Ventes")');
    // La feuille vierge et les modèles de vente doivent être affichés
    await expect(page.locator('.starter-card', { hasText: 'Facture commerciale' })).toBeVisible();
    await expect(page.locator('.starter-card', { hasText: 'Rapport d’Avancement' })).toBeVisible();
    await expect(page.locator('.starter-card', { hasText: 'Bulletin de paie' })).not.toBeVisible();

    // Revenir à "Tous les modèles"
    await page.click('button.tab-btn:has-text("Tous les modèles")');
    await expect(page.locator('.starter-card', { hasText: 'Bulletin de paie' })).toBeVisible();

    // Sélectionner le modèle "Facture commerciale"
    const factureCard = page.locator('.starter-card', { hasText: 'Facture commerciale' });
    await factureCard.click();

    await expect(factureCard).toHaveClass(/selected/);

    // Vérifier que le formulaire est pré-rempli
    const nomInput = page.locator('#nom');
    await expect(nomInput).toHaveValue('Facture commerciale');

    // Vérifier la bannière d'aperçu des variables avec leurs types
    const previewBanner = page.locator('.starter-preview-banner');
    await expect(previewBanner).toBeVisible();
    await expect(previewBanner).toContainText('numero_facture');
    await expect(previewBanner).toContainText('client_nom');
    await expect(previewBanner).toContainText('total_ttc');

    // Vérifier la présence des badges de type
    await expect(previewBanner.locator('.var-type.type-string').first()).toBeVisible();
    await expect(previewBanner.locator('.var-type.type-date').first()).toBeVisible();
    await expect(previewBanner.locator('.var-type.type-float').first()).toBeVisible();
  });

  test('Création depuis Starter Template: soumission avec contenuDesign et création des variables', async ({ page }) => {
    let createRequestPayload: any = null;
    const createdVariables: any[] = [];

    // Intercepter la création du template
    await page.route('**/api/templates', async (route) => {
      if (route.request().method() === 'POST') {
        createRequestPayload = JSON.parse(route.request().postData() || '{}');
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: createdTemplateId,
            nom: createRequestPayload.nom || 'Facture Test',
            description: createRequestPayload.description || '',
            version: 1,
            statut: 'BROUILLON',
            contenuDesign: createRequestPayload.contenuDesign,
            categorie: createRequestPayload.categorie || 'VENTES',
            formatPapier: 'A4',
            modePagination: 'FIXED',
          }),
        });
      } else {
        await route.fallback();
      }
    });

    // Intercepter l'ajout des variables
    await page.route(`**/api/templates/${createdTemplateId}/variables`, async (route) => {
      if (route.request().method() === 'POST') {
        const vData = JSON.parse(route.request().postData() || '{}');
        createdVariables.push(vData);
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ id: `var-${createdVariables.length}`, ...vData }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(createdVariables),
        });
      }
    });

    // Mock des routes du designer pour la redirection
    await page.route(`**/api/templates/${createdTemplateId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: createdTemplateId,
          nom: 'Facture commerciale Pro',
          version: 1,
          statut: 'BROUILLON',
          categorie: 'VENTES',
          formatPapier: 'A4',
          modePagination: 'FIXED',
          contenuDesign: createRequestPayload ? createRequestPayload.contenuDesign : '{"pages":[]}',
        }),
      });
    });

    await page.route(`**/api/templates/${createdTemplateId}/versions`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ tree: { id: createdTemplateId, nom: 'Facture', version: 1, children: [] }, flatHistory: [] }),
      });
    });

    await page.route(`**/api/templates/${createdTemplateId}/documents`, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/templates/new');

    // Sélectionner Facture commerciale
    await page.locator('.starter-card', { hasText: 'Facture commerciale' }).click();

    // Personnaliser le nom
    await page.locator('#nom').fill('Facture commerciale Pro');

    // Cliquer sur le bouton de création
    await page.click('button[type="submit"]');

    // Attendre la redirection vers /templates/:id
    await page.waitForURL(`**/templates/${createdTemplateId}`);

    // Vérifier que le payload envoyé contenait les pages et blocs de la facture
    expect(createRequestPayload).not.toBeNull();
    expect(createRequestPayload.nom).toBe('Facture commerciale Pro');
    expect(createRequestPayload.contenuDesign).toBeDefined();

    const designObj = JSON.parse(createRequestPayload.contenuDesign);
    expect(designObj.pages).toBeDefined();
    expect(designObj.pages.length).toBeGreaterThan(0);
    // Vérifier la présence de blocs pré-configurés
    const blocks = designObj.pages[0].blocs;
    expect(blocks.some((b: any) => b.contenu === 'FACTURE')).toBe(true);

    // Vérifier que les variables ont bien été créées via l'API
    expect(createdVariables.length).toBeGreaterThanOrEqual(10);
    expect(createdVariables.some((v: any) => v.nomVariable === 'numero_facture')).toBe(true);
    expect(createdVariables.some((v: any) => v.nomVariable === 'total_ttc')).toBe(true);
  });

  test('Autocomplétion {{ in-place sur la feuille virtuelle et dans l’inspecteur de bloc', async ({ page }) => {
    const mockDesign = {
      id: 'tpl-autocomplete-001',
      nom: 'Modèle Autocomplétion',
      version: 1,
      statut: 'BROUILLON',
      categorie: 'VENTES',
      formatPapier: 'A4',
      modePagination: 'FIXED',
      contenuDesign: JSON.stringify({
        pages: [
          {
            nom: 'Page 1',
            blocs: [
              {
                id: 'txt-1',
                type: 'texte',
                x: 60,
                y: 80,
                largeurBox: 300,
                hauteurBox: 50,
                contenu: 'Client : ',
              },
            ],
          },
        ],
      }),
    };

    const mockVariables = [
      { id: 'v-1', nomVariable: 'client_nom', type: 'STRING', obligatoire: true, description: 'Nom du client' },
      { id: 'v-2', nomVariable: 'client_adresse', type: 'STRING', obligatoire: false, description: 'Adresse postale' },
      { id: 'v-3', nomVariable: 'total_ttc', type: 'FLOAT', obligatoire: true, description: 'Total TTC' },
      { id: 'v-4', nomVariable: 'date_facture', type: 'DATE', obligatoire: true, description: 'Date de facturation' },
    ];

    await page.route('**/api/templates/tpl-autocomplete-001', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockDesign) });
    });

    await page.route('**/api/templates/tpl-autocomplete-001/versions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ tree: { id: 'tpl-autocomplete-001', nom: 'Modèle Autocomplétion', version: 1, children: [] }, flatHistory: [] }),
      });
    });

    await page.route('**/api/templates/tpl-autocomplete-001/variables', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockVariables) });
    });

    await page.route('**/api/templates/tpl-autocomplete-001/documents', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/templates/tpl-autocomplete-001');

    // Attendre le chargement du canvas
    const canvas = page.locator('app-design-canvas .canvas');
    await expect(canvas).toBeVisible();

    const block = canvas.locator('.canvas-block').first();
    await expect(block).toBeVisible();

    // 1. Test de l'autocomplétion en édition in-place sur le canvas
    await block.dblclick();
    const inPlaceTextarea = canvas.locator('textarea.in-place-input');
    await expect(inPlaceTextarea).toBeVisible();

    // Taper {{ pour déclencher l'autocomplétion
    await inPlaceTextarea.fill('Client : {{');

    // Vérifier l'apparition du menu déroulant d'autocomplétion
    const dropdown = page.locator('.variable-autocomplete-dropdown');
    await expect(dropdown).toBeVisible();
    await expect(dropdown).toContainText('Variables disponibles');

    // Vérifier la présence des variables mockées et des badges
    await expect(dropdown).toContainText('client_nom');
    await expect(dropdown).toContainText('total_ttc');
    await expect(dropdown.locator('.vac-var-badge.type-string').first()).toBeVisible();

    // Taper 'tot' pour filtrer sur total_ttc
    await inPlaceTextarea.fill('Client : {{tot');
    await expect(dropdown).toContainText('total_ttc');
    await expect(dropdown).not.toContainText('client_nom');

    // Sélectionner avec la touche Entrée
    await inPlaceTextarea.press('Enter');

    // Vérifier que {{total_ttc}} a été inséré
    await expect(inPlaceTextarea).toHaveValue('Client : {{total_ttc}}');
    // Le menu déroulant doit se refermer après insertion
    await expect(dropdown).not.toBeVisible();

    // 2. Test de l'autocomplétion dans l'inspecteur latéral (Block Editor)
    // Cliquer pour fermer l'édition in-place et sélectionner le bloc
    await page.keyboard.press('Escape');
    await block.click();

    const blockEditor = page.locator('app-block-editor');
    await expect(blockEditor).toBeVisible();

    const contenuTextarea = blockEditor.locator('textarea[formControlName="contenu"]');
    await expect(contenuTextarea).toBeVisible();

    // Taper {{cl dans le textarea de l'éditeur
    await contenuTextarea.fill('Bonjour {{cl');
    await expect(dropdown).toBeVisible();
    await expect(dropdown).toContainText('client_nom');
    await expect(dropdown).toContainText('client_adresse');
    await expect(dropdown).not.toContainText('total_ttc');

    // Clic sur l'élément 'client_adresse' dans le menu déroulant
    const itemAdresse = dropdown.locator('.vac-item', { hasText: 'client_adresse' });
    await itemAdresse.click();

    // Vérifier l'insertion de {{client_adresse}}
    await expect(contenuTextarea).toHaveValue('Bonjour {{client_adresse}}');
    await expect(dropdown).not.toBeVisible();
  });
});

