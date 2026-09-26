import { test, expect } from '@playwright/test';

test.describe('Designer - Bloc Signature Électronique et Manuscrite (Étape 4)', () => {
  const mockTemplate = {
    id: 'template-signature-001',
    nom: 'Contrat Commercial avec Signature',
    description: 'Test du bloc signature électronique et manuscrite',
    version: 1,
    statut: 'BROUILLON',
    dateModification: '2026-09-26T12:00:00',
    categorie: 'COMMERCIAL',
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
              id: 'bloc-sig-01',
              type: 'signature',
              x: 50,
              y: 80,
              largeurBox: 240,
              hauteurBox: 120,
              signatureConfig: {
                mentionLegale: 'Lu et approuvé, bon pour accord',
                signataireNom: 'M. Jean Valjean',
                signataireQualite: 'Directeur Général',
                dateSignature: '26/09/2026',
                modeSignature: 'MANUSCRITE',
                afficherCadre: true,
                cadrePointille: true
              }
            }
          ]
        }
      ]
    }),
    pages: [],
    variables: [
      { nom: 'signataire_nom', type: 'TEXTE', exemple: 'Jean Valjean' }
    ]
  };

  test.beforeEach(async ({ page }) => {
    // Initialiser la session utilisateur
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-jwt-token-signature');
      localStorage.setItem(
        'auth_user',
        JSON.stringify({
          id: 'user-signature-001',
          email: 'qa-signature@test.com',
          nomComplet: 'QA Signature Tester',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-TEST'
        })
      );
      localStorage.setItem('entrepriseCode', 'ENT-TEST');
      localStorage.setItem('onboarding_tour_completed', 'true');
    });

    // Mocks API
    await page.route('**/api/templates/template-signature-001', async (route) => {
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

    await page.route('**/api/templates/template-signature-001/versions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          tree: { id: 'template-signature-001', nom: 'Contrat Commercial avec Signature', version: 1, children: [] },
          flatHistory: [],
          totalVersions: 1
        })
      });
    });

    await page.route('**/api/templates/template-signature-001/variables', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { nom: 'signataire_nom', type: 'TEXTE', description: 'Nom du signataire' }
        ])
      });
    });

    await page.route('**/api/templates/template-signature-001/documents', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/templates/template-signature-001/audit-logs', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });
  });

  test('devrait afficher, modifier et prévisualiser un bloc signature interactif', async ({ page }) => {
    // 1. Accéder au studio designer sur la page du template
    await page.goto('/templates/template-signature-001');

    // Vérifier la présence du canvas et du bloc signature
    const canvas = page.locator('app-design-canvas .canvas');
    await expect(canvas).toBeVisible({ timeout: 10000 });

    const sigBlock = canvas.locator('.canvas-signature');
    await expect(sigBlock).toBeVisible();
    await expect(sigBlock).toContainText('Lu et approuvé, bon pour accord');
    await expect(sigBlock).toContainText('M. Jean Valjean');
    await expect(sigBlock).toContainText('Directeur Général');

    // 2. Sélectionner le bloc signature pour ouvrir l'inspecteur
    await sigBlock.click();

    const inspector = page.locator('app-block-editor');
    await expect(inspector).toBeVisible();
    await expect(inspector).toContainText('✍️ Paramètres de Signature');

    // 3. Modifier la mention légale et le signataire
    const mentionInput = inspector.locator('input[formcontrolname="signatureMentionLegale"]');
    await expect(mentionInput).toBeVisible();
    await mentionInput.fill('Certifié exact et sincère, bon pour accord');

    const nomInput = inspector.locator('input[formcontrolname="signatureSignataireNom"]');
    await expect(nomInput).toBeVisible();
    await nomInput.fill('Alexandre Dumas');

    const qualiteInput = inspector.locator('input[formcontrolname="signatureSignataireQualite"]');
    await expect(qualiteInput).toBeVisible();
    await qualiteInput.fill('Président Directeur Général');

    // Déclencher la sauvegarde via le formulaire
    await qualiteInput.press('Enter');

    // 4. Vérifier la mise à jour en temps réel sur la feuille virtuelle (Canvas)
    await expect(sigBlock).toContainText('Certifié exact et sincère, bon pour accord');
    await expect(sigBlock).toContainText('Alexandre Dumas');
    await expect(sigBlock).toContainText('Président Directeur Général');

    // 5. Vérifier la prévisualisation dans l'aperçu (Block Preview)
    const previewContainer = page.locator('app-block-preview');
    await expect(previewContainer).toBeVisible();

    const previewSig = previewContainer.locator('.signature-block-preview');
    await expect(previewSig).toBeVisible();
    await expect(previewSig).toContainText('Certifié exact et sincère, bon pour accord');
    await expect(previewSig).toContainText('Alexandre Dumas');
    await expect(previewSig).toContainText('Président Directeur Général');

    // 6. Tester le canvas pad HTML5 de signature manuscrite
    const sigPad = inspector.locator('canvas.signature-pad-canvas, .sig-canvas-wrapper canvas');
    await expect(sigPad).toBeVisible();

    // Simuler un tracé à la souris sur le pad de signature
    const box = await sigPad.boundingBox();
    if (box) {
      await page.mouse.move(box.x + 20, box.y + 20);
      await page.mouse.down();
      await page.mouse.move(box.x + 80, box.y + 50);
      await page.mouse.move(box.x + 140, box.y + 30);
      await page.mouse.up();
    }

    // Cliquer sur "Enregistrer le tracé"
    const saveSigBtn = inspector.locator('button.sig-btn.save');
    await expect(saveSigBtn).toBeVisible();
    await saveSigBtn.click();

    // L'aperçu doit maintenant afficher l'image de la signature manuscrite
    const previewImg = previewSig.locator('img[alt="Signature"]');
    await expect(previewImg).toBeVisible();
  });
});
