import { test, expect } from '@playwright/test';

test.describe('Parcours Authentification & Navigation', () => {
  test('Affiche le formulaire de connexion', async ({ page }) => {
    await page.goto('/auth/login');

    await expect(page.locator('input[type="email"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('Affiche une erreur en cas d identifiants invalides', async ({ page }) => {
    await page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Identifiants invalides' }),
      });
    });

    await page.goto('/auth/login');
    await page.fill('input[type="email"]', 'inconnu@test.com');
    await page.fill('input[type="password"]', 'mauvaispass');
    await page.click('button[type="submit"]');

    await expect(page.locator('.error-banner')).toContainText('invalides');
  });

  test('Connexion réussie et redirection vers la bibliothèque', async ({ page }) => {
    await page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'fake-jwt-token-xyz',
          tokenType: 'Bearer',
          user: {
            id: 'user-001',
            email: 'admin@acme.com',
            nom: 'Admin',
            prenom: 'ACME',
            nomComplet: 'Administrateur ACME',
            role: 'ADMIN_ENTREPRISE',
            codeEntreprise: 'ACME-01',
          },
        }),
      });
    });

    await page.route('**/api/templates**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.goto('/auth/login');
    await page.fill('input[type="email"]', 'admin@acme.com');
    await page.fill('input[type="password"]', 'motdepasse123');
    await page.click('button[type="submit"]');

    // Vérifie la redirection vers /bibliotheque
    await expect(page).toHaveURL(/.*bibliotheque/);

    // Vérifie la présence de l'utilisateur dans la barre de navigation
    await expect(page.locator('.navbar')).toContainText('ACME');
  });
});

