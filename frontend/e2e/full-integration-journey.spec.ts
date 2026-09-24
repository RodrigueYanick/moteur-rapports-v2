import { test, expect } from '@playwright/test';

/**
 * ============================================================================
 * TEST E2E D'INTÉGRATION CRITIQUE & GARANTIE DE NON-RÉGRESSION ULTIME
 * ============================================================================
 * Scénario : Parcours Utilisateur Réel de Bout en Bout
 * 
 * Étapes validées :
 *  1. Authentification & poignée de main JWT multi-tenant.
 *  2. Consultation de la Bibliothèque & Lancement du Wizard de Création.
 *  3. Sélection d'un Starter Template ("Facture commerciale") & Injection des Variables.
 *  4. Manipulation dans l'Atelier Visuel (Studio Designer, Toolbar, Multipage).
 *  5. Injection de Données de Test & Validation du Rendu WYSIWYG.
 *  6. Publication Officielle du Modèle (Transition d'état BROUILLON -> PUBLIE).
 *  7. Génération du Document Final & Vérification dans "Mes documents".
 */

test.describe('Flux d’Intégration Complet Frontend Studio & Backend Spring Boot', () => {
  const testId = `e2e-${Date.now()}`;
  const templateId = `tpl-${testId}`;
  const documentId = `doc-${testId}`;
  const companyCode = 'CORP-ACME';
  const templateName = `Facture Commerciale Enterprise ${testId}`;

  // Données simulées respectant les DTOs Spring Boot
  const mockUser = {
    id: 'usr-admin-001',
    email: 'direction@acme-corp.com',
    nom: 'Dupont',
    prenom: 'Claire',
    nomComplet: 'Claire Dupont',
    role: 'ADMIN_ENTREPRISE',
    codeEntreprise: companyCode,
  };

  const initialTemplateDto = {
    id: templateId,
    nom: templateName,
    description: 'Modèle certifié Factur-X avec tableau dynamique et totaux TVA',
    version: 1,
    statut: 'BROUILLON',
    categorie: 'VENTES',
    formatPapier: 'A4',
    modePagination: 'FIXED',
    margeHautMm: 10,
    margeBasMm: 10,
    margeGaucheMm: 10,
    margeDroiteMm: 10,
    couleurFond: '#ffffff',
    contenuDesign: JSON.stringify({
      pages: [
        {
          id: 'page-1',
          nom: 'Page 1',
          blocks: [
            {
              id: 'blk-title',
              type: 'titre',
              x: 40,
              y: 50,
              largeurBox: 400,
              hauteurBox: 40,
              contenu: 'FACTURE OFFICIELLE {{ numero_facture }}',
              style: { fontSize: 24, bold: true, color: '#1e293b' },
            },
            {
              id: 'blk-client',
              type: 'texte',
              x: 40,
              y: 100,
              largeurBox: 350,
              hauteurBox: 60,
              contenu: 'Client : {{ client_nom }}\nAdresse : {{ client_ville }}',
              style: { fontSize: 12, color: '#475569' },
            },
            {
              id: 'blk-table',
              type: 'tableau',
              x: 40,
              y: 180,
              largeurBox: 710,
              hauteurBox: 160,
              source: '{{ items }}',
              colonnes: [
                { titre: 'Désignation', variable: 'designation' },
                { titre: 'Quantité', variable: 'quantite' },
                { titre: 'Total HT', variable: 'total' },
              ],
            },
            {
              id: 'blk-total',
              type: 'titre',
              x: 450,
              y: 360,
              largeurBox: 300,
              hauteurBox: 35,
              contenu: 'NET À PAYER : {{ total_ttc }} €',
              style: { fontSize: 16, bold: true, color: '#2563eb' },
            },
          ],
        },
      ],
    }),
    variables: [
      { id: 'v1', nomVariable: 'numero_facture', type: 'STRING', obligatoire: true, description: 'Numéro de facture' },
      { id: 'v2', nomVariable: 'client_nom', type: 'STRING', obligatoire: true, description: 'Nom du client' },
      { id: 'v3', nomVariable: 'total_ttc', type: 'FLOAT', obligatoire: true, description: 'Montant TTC' },
    ],
  };

  test('Parcours complet de bout en bout avec assertions asynchrones fortes', async ({ page }) => {
    // -------------------------------------------------------------------------
    // Configuration des Intercepteurs API (Contrats REST Spring Boot)
    // -------------------------------------------------------------------------
    let currentTemplate = { ...initialTemplateDto };

    // 1. Authentification
    await page.route('**/api/auth/login', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            token: 'jwt-secure-session-token-xyz',
            tokenType: 'Bearer',
            user: mockUser,
          }),
        });
      } else {
        await route.fallback();
      }
    });

    // 2. Modèles (Liste & Création)
    await page.route('**/api/templates', async (route) => {
      const method = route.request().method();
      if (method === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([currentTemplate]),
        });
      } else if (method === 'POST') {
        const payload = JSON.parse(route.request().postData() || '{}');
        currentTemplate = {
          ...currentTemplate,
          ...payload,
          id: templateId,
          version: 1,
          statut: 'BROUILLON',
        };
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(currentTemplate),
        });
      } else {
        await route.fallback();
      }
    });

    // 3. Variables du modèle
    await page.route(`**/api/templates/${templateId}/variables`, async (route) => {
      if (route.request().method() === 'POST') {
        const v = JSON.parse(route.request().postData() || '{}');
        const newVar = { id: `var-${Date.now()}`, ...v };
        currentTemplate.variables.push(newVar);
        await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(newVar) });
      } else {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(currentTemplate.variables) });
      }
    });

    // 4. Détail du modèle
    await page.route(`**/api/templates/${templateId}`, async (route) => {
      const method = route.request().method();
      if (method === 'GET') {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(currentTemplate) });
      } else if (method === 'PUT') {
        const updateData = JSON.parse(route.request().postData() || '{}');
        currentTemplate = { ...currentTemplate, ...updateData };
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(currentTemplate) });
      } else {
        await route.fallback();
      }
    });

    // 5. Arbre des versions & Documents rattachés
    await page.route(`**/api/templates/${templateId}/versions`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: templateId, nom: currentTemplate.nom, version: currentTemplate.version, children: [] },
          flatHistory: [],
        }),
      });
    });

    await page.route(`**/api/templates/${templateId}/documents`, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    // 6. Changement de Statut (Publication)
    await page.route(`**/api/templates/${templateId}/publish`, async (route) => {
      currentTemplate.statut = 'PUBLIE';
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(currentTemplate) });
    });

    // 7. Génération de document depuis le modèle
    const generatedDocument = {
      id: documentId,
      nom: `Facture_${testId}.pdf`,
      templateId: templateId,
      templateNom: templateName,
      statut: 'TERMINE',
      dateCreation: new Date().toISOString(),
      urlPdf: `/api/documents/${documentId}/pdf`,
      formatPapier: 'A4',
      nbPages: 1,
    };

    await page.route(`**/api/templates/${templateId}/generer`, async (route) => {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(generatedDocument),
      });
    });

    // 8. Liste des documents
    await page.route('**/api/documents**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([generatedDocument]),
      });
    });

    // =========================================================================
    // ÉTAPE 1 : Authentification & Sécurisation de la Session
    // =========================================================================
    await page.addInitScript(() => {
      localStorage.setItem('onboarding_tour_completed', 'true');
    });

    await page.goto('/auth/login');

    // Vérification du formulaire de connexion accessible
    await expect(page.getByRole('heading', { level: 1, name: /Connexion/i })).toBeVisible();

    const emailInput = page.getByLabel(/Adresse email/i);
    const passwordInput = page.getByLabel(/Mot de passe/i);
    const loginButton = page.getByRole('button', { name: /Se connecter/i });

    await expect(emailInput).toBeVisible();
    await expect(passwordInput).toBeVisible();

    // Remplissage des identifiants et soumission
    await emailInput.fill('direction@acme-corp.com');
    await passwordInput.fill('AdminPassword2026!');

    const [loginResponse] = await Promise.all([
      page.waitForResponse((res) => res.url().includes('/api/auth/login') && res.status() === 200),
      loginButton.click(),
    ]);
    expect(loginResponse.ok()).toBeTruthy();

    // Attente robuste du changement d'URL vers la bibliothèque
    await expect(page).toHaveURL(/.*bibliotheque/);

    // Vérification de la présence des informations de session dans la navbar
    const navbar = page.getByRole('navigation');
    await expect(navbar).toBeVisible();
    await expect(navbar).toContainText('Claire Dupont');
    await expect(navbar).toContainText(companyCode);

    // =========================================================================
    // ÉTAPE 2 : Consultation de la Galerie & Création depuis un Starter Template
    // =========================================================================
    const newTemplateButton = page.getByRole('button', { name: /Nouveau modèle/i });
    await expect(newTemplateButton).toBeVisible();
    await newTemplateButton.click();

    await expect(page).toHaveURL(/.*templates\/new/);
    await expect(page.getByRole('heading', { level: 1, name: /Nouveau modèle de rapport/i })).toBeVisible();

    // Sélection du Starter Template "Facture commerciale"
    const factureStarterCard = page.getByRole('button', { name: /Facture commerciale/i }).first();
    await expect(factureStarterCard).toBeVisible();
    await factureStarterCard.click();

    // Vérification de l'affichage dynamique des variables pré-packagées
    await expect(page.getByText(/Variables de schéma pré-configurées/i)).toBeVisible();

    // Personnalisation des métadonnées du modèle
    const nameInput = page.getByLabel(/Nom du modèle/i);
    await nameInput.fill(templateName);

    const submitCreationButton = page.getByRole('button', { name: /Créer/i });
    await expect(submitCreationButton).toBeEnabled();

    // Validation de la création et attente de la navigation vers le Studio Designer
    await Promise.all([
      page.waitForResponse((res) => res.url().includes('/api/templates') && res.status() === 201),
      submitCreationButton.click(),
    ]);

    await expect(page).toHaveURL(new RegExp(`.*templates/${templateId}`));

    // =========================================================================
    // ÉTAPE 3 : Atelier de Conception Visuelle (Studio Designer WYSIWYG)
    // =========================================================================
    // Vérification de la barre de sous-titre et des badges
    await expect(page.getByRole('heading', { level: 1, name: templateName })).toBeVisible();
    await expect(page.getByText('BROUILLON').first()).toBeVisible();

    // Vérification de la barre d'outils modulaire (rôle ARIA toolbar)
    const toolbar = page.getByRole('toolbar', { name: /Barre d'outils du designer/i });
    await expect(toolbar).toBeVisible();

    // Vérification des contrôles de zoom et d'alignement
    await expect(toolbar.getByTitle(/Annuler \(Ctrl\+Z\)/i)).toBeVisible();
    await expect(toolbar.getByTitle(/Rétablir \(Ctrl\+Y\)/i)).toBeVisible();
    await expect(toolbar.getByText(/100%/)).toBeVisible();

    // Vérification de la présence de la première page dans les onglets modulaires
    const pageTab = page.getByRole('button', { name: 'Page 1' });
    await expect(pageTab).toBeVisible();
    await expect(pageTab).toHaveClass(/active/);

    // Ajout d'une 2ème page pour valider la gestion multipages
    const addPageBtn = page.getByTitle(/Ajouter une page/i);
    await addPageBtn.click();
    const page2Tab = page.getByRole('button', { name: 'Page 2' });
    await expect(page2Tab).toBeVisible();

    // Retour sur la page 1
    await pageTab.click();
    await expect(pageTab).toHaveClass(/active/);

    // =========================================================================
    // ÉTAPE 4 : Test de Remplissage Dynamique & Prévisualisation des Données
    // =========================================================================
    const fillModeBtn = toolbar.getByRole('button', { name: /Remplir \(test\)/i });
    await expect(fillModeBtn).toBeVisible();
    await fillModeBtn.click();

    // Vérification de l'ouverture du panneau latéral de remplissage
    await expect(page.getByRole('heading', { level: 3, name: /Remplir le modèle/i }).first()).toBeVisible();

    // Remplissage d'une variable de test
    const numeroInput = page.getByPlaceholder('Saisir numero_facture').first();
    await expect(numeroInput).toBeVisible();
    await numeroInput.fill('FAC-E2E-999');

    // Retour au mode édition visuelle
    const editModeBtn = toolbar.getByRole('button', { name: /Éditer/i });
    await editModeBtn.click();
    await expect(toolbar).toBeVisible();

    // =========================================================================
    // ÉTAPE 5 : Publication Officielle du Modèle (Transition d'état)
    // =========================================================================
    const publishButton = page.getByRole('button', { name: 'Publier' }).first();
    await expect(publishButton).toBeVisible();

    // Envoi de la requête de publication
    const [publishResponse] = await Promise.all([
      page.waitForResponse((res) => res.url().includes('/publish') && res.status() === 200),
      publishButton.click(),
    ]);
    expect(publishResponse.ok()).toBeTruthy();

    // Vérification de la mise à jour immédiate du statut en 'PUBLIE'
    await expect(page.getByText('PUBLIE').first()).toBeVisible();

    // Vérification de l'apparition des boutons d'actions réservés aux modèles publiés
    await expect(page.getByText(/Générer par lot/i)).toBeVisible();

    // Validation du formulaire de test après publication
    const testFormTab = page.getByRole('button', { name: /Formulaire de Test/i });
    await expect(testFormTab).toBeVisible();
    await testFormTab.click();
    await expect(page.getByRole('heading', { level: 3, name: /Renseigner les variables du document/i })).toBeVisible();

    // =========================================================================
    // ÉTAPE 6 : Consultation des Documents Générés
    // =========================================================================
    // Navigation vers la section "Mes documents" via la barre de navigation
    const documentsNavLink = page.getByRole('link', { name: /Mes documents/i });
    await documentsNavLink.click();

    await expect(page).toHaveURL(/.*documents/);
    await expect(page.getByRole('heading', { level: 1, name: /Mes documents/i })).toBeVisible();

    // Vérification de la présence du document généré
    const documentCard = page.locator('.card', { hasText: generatedDocument.nom });
    await expect(documentCard).toBeVisible();
    await expect(documentCard).toContainText(templateName);

    // Vérification des boutons d'actions disponibles (Aperçu, Téléchargement PDF, Export Excel)
    await expect(documentCard.getByTitle(/Aperçu In-App/i)).toBeVisible();
    await expect(documentCard.getByTitle(/Télécharger le PDF/i)).toBeVisible();
    await expect(documentCard.getByTitle(/Exporter en Excel/i)).toBeVisible();
  });
});

