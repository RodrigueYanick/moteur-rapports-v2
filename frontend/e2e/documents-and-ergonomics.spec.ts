import { test, expect } from '@playwright/test';

test.describe('Consultation, Diffusion des Documents & Ergonomie E2E', () => {
  const mockDocuments = [
    {
      id: 'doc-101',
      nom: 'Facture Client F-2026-001',
      templateId: 'tpl-101',
      templateNom: 'Facture Standard',
      statut: 'FINALISE',
      dateCreation: '2026-09-14T01:00:00',
      dateModification: '2026-09-14T01:30:00',
      donnees: { totalTTC: 1450.5, clientNom: 'Acme Corp' },
    },
  ];

  const mockTemplate = {
    id: 'tpl-101',
    nom: 'Modèle Facture Pro',
    description: 'Modèle de facture commerciale pour test',
    version: 1,
    statut: 'BROUILLON',
    categorie: 'FACTURE',
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
              x: 20,
              y: 20,
              largeurBox: 100,
              hauteurBox: 30,
              contenu: 'Facture Acme',
            },
          ],
        },
      ],
    }),
    pages: [],
    variables: [],
  };

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-ergo');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-009',
          email: 'user@acme.com',
          nomComplet: 'Utilisateur Acme',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ACME',
        })
      );
      localStorage.setItem('entrepriseCode', 'ACME');
    });

    // Mock API documents avec wildcard
    await page.route('**/api/documents**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockDocuments),
      });
    });

    // Mock API templates
    await page.route('**/api/templates/tpl-101', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockTemplate),
      });
    });

    await page.route('**/api/templates/tpl-101/versions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: 'tpl-101', nom: 'Modèle Facture Pro', version: 1, children: [] },
          flatHistory: [],
        }),
      });
    });

    await page.route('**/api/templates/tpl-101/pages/**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });

    await page.route('**/api/templates/tpl-101/variables', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/templates/tpl-101/documents', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/templates', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([mockTemplate]),
      });
    });

    await page.route('**/api/templates/tpl-101/generate-pdf', async (route) => {
      const dummyPdfContent = '%PDF-1.4\n%Fake PDF content for test\n%%EOF';
      await route.fulfill({
        status: 200,
        contentType: 'application/pdf',
        body: Buffer.from(dummyPdfContent),
      });
    });
  });

  test('🌓 Bascule du thème Sombre / Clair (Dark / Light Mode)', async ({ page }) => {
    await page.goto('/documents');

    const themeToggleBtn = page.locator('.theme-toggle-btn');
    await expect(themeToggleBtn).toBeVisible();

    // Vérification de l'attribut data-theme initial
    const initialTheme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(['light', 'dark', null]).toContain(initialTheme);

    // Clic pour basculer en mode sombre
    await themeToggleBtn.click();
    const darkTheme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(darkTheme).toBe('dark');

    const storedThemeDark = await page.evaluate(() => localStorage.getItem('app_theme'));
    expect(storedThemeDark).toBe('dark');

    // Clic pour revenir en mode clair
    await themeToggleBtn.click();
    const lightTheme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(lightTheme).toBe('light');

    const storedThemeLight = await page.evaluate(() => localStorage.getItem('app_theme'));
    expect(storedThemeLight).toBe('light');
  });

  test('👁️ Visionneuse PDF In-App avec contrôles de zoom et fermeture', async ({ page }) => {
    await page.goto('/documents');

    // Repérage de la carte de document
    const card = page.locator('.card').first();
    await expect(card).toBeVisible();

    // Bouton Aperçu In-App
    const previewBtn = card.locator('.hover-btn.preview');
    await previewBtn.click({ force: true });

    // La modale de prévisualisation PDF s'ouvre
    const modalBackdrop = page.locator('.pdf-modal-backdrop');
    await expect(modalBackdrop).toBeVisible();

    // Vérifier les contrôles de la visionneuse
    await expect(modalBackdrop.locator('.doc-title')).toContainText('Facture Client F-2026-001');
    await expect(modalBackdrop.locator('.zoom-text')).toContainText('100%');

    // Zoom avant
    const zoomInBtn = modalBackdrop.locator('button[title="Zoom avant"]');
    await zoomInBtn.click();
    await expect(modalBackdrop.locator('.zoom-text')).toContainText('115%');

    // Zoom arrière
    const zoomOutBtn = modalBackdrop.locator('button[title="Zoom arrière"]');
    await zoomOutBtn.click();
    await expect(modalBackdrop.locator('.zoom-text')).toContainText('100%');

    // Bouton imprimer présent
    await expect(modalBackdrop.locator('button:has-text("Imprimer")')).toBeVisible();

    // Fermeture de la modale
    const closeBtn = modalBackdrop.locator('.btn-close');
    await closeBtn.click();
    await expect(modalBackdrop).not.toBeVisible();
  });

  test('✉️ Envoi direct par Email avec notification Toast de confirmation', async ({ page }) => {
    let emailSentPayload: any = null;
    await page.route('**/*send-email*', async (route) => {
      emailSentPayload = JSON.parse(route.request().postData() || '{}');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Le document a été transmis avec succès.',
          recipient: emailSentPayload?.recipient,
          documentId: 'doc-101',
        }),
      });
    });

    await page.goto('/documents');

    const card = page.locator('.card').first();
    const emailBtn = card.locator('.hover-btn.email');
    await emailBtn.click({ force: true });

    // Modale Email
    const emailModalCard = page.locator('.email-modal-card');
    await expect(emailModalCard).toBeVisible();

    // Vérification du sujet pré-rempli
    const subjectInput = emailModalCard.locator('input#emailSubject');
    await expect(subjectInput).toHaveValue('Document : Facture Client F-2026-001');

    // Renseignement du destinataire
    const recipientInput = emailModalCard.locator('input#emailRecipient');
    await recipientInput.fill('client.facturation@acme.com');

    // Clic sur Envoyer
    const submitBtn = emailModalCard.locator('button.btn-send');
    await submitBtn.click();

    // La modale se ferme
    await expect(emailModalCard).not.toBeVisible();

    // Le payload a bien été envoyé
    expect(emailSentPayload).not.toBeNull();
    expect(emailSentPayload.destinataire).toBe('client.facturation@acme.com');

    // Vérification de la notification Toast discrète
    const toast = page.locator('.toast-item.success');
    await expect(toast).toBeVisible();
    await expect(toast).toContainText('Email expédié');
  });

  test('🧭 Visite guidée au premier lancement (Onboarding Tour)', async ({ page }) => {
    // S'assurer que le tour n'est pas encore complété
    await page.addInitScript(() => {
      localStorage.removeItem('onboarding_tour_completed');
    });

    await page.goto('/templates/tpl-101');

    // Attendre que le designer soit monté
    const canvas = page.locator('app-design-canvas .canvas');
    await expect(canvas).toBeVisible();

    // Le popover onboarding doit apparaître
    const tourPopover = page.locator('.tour-popover');
    await expect(tourPopover).toBeVisible({ timeout: 6000 });

    // Étape 1
    await expect(tourPopover.locator('.step-title')).toContainText('1. Glissez vos blocs ici');
    await expect(tourPopover.locator('.header-tag')).toContainText('Étape 1/3');

    // Bouton suivant
    const nextBtn = tourPopover.locator('button.btn-nav.primary:has-text("Suivant")');
    await nextBtn.click();

    // Étape 2
    await expect(tourPopover.locator('.step-title')).toContainText('2. Liez vos données');
    await expect(tourPopover.locator('.header-tag')).toContainText('Étape 2/3');
    await nextBtn.click();

    // Étape 3
    await expect(tourPopover.locator('.step-title')).toContainText('3. Générez votre PDF');
    await expect(tourPopover.locator('.header-tag')).toContainText('Étape 3/3');

    // Clic sur Terminer
    const finishBtn = tourPopover.locator('button.btn-nav.primary:has-text("Terminer")');
    await finishBtn.click();

    // Le tour est fermé
    await expect(tourPopover).not.toBeVisible();

    // La clé de complétion est enregistrée
    const completed = await page.evaluate(() => localStorage.getItem('onboarding_tour_completed'));
    expect(completed).toBe('true');

    // On peut réouvrir le guide en cliquant sur le bouton 🧭 Guide dans la toolbar
    const guideBtn = page.locator('button.onboarding-btn');
    await guideBtn.click();
    await expect(tourPopover).toBeVisible();
  });
});

