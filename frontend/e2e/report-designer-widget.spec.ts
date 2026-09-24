import { test, expect } from '@playwright/test';

test.describe('Sprint 6 : Web Component SDK (<report-designer-widget>)', () => {
  test('Le Web Component report-designer-widget est bien enregistré dans customElements', async ({ page }) => {
    // Navigation vers la page d'accueil ou bibliothèque
    await page.goto('/');

    // Attendre que le composant soit enregistré dans le registre des éléments personnalisés
    const isDefined = await page.waitForFunction(() => {
      return !!window.customElements.get('report-designer-widget');
    }, null, { timeout: 10000 });

    expect(isDefined).toBeTruthy();
  });

  test('Instanciation dynamique et rendu du composant <report-designer-widget>', async ({ page }) => {
    await page.goto('/');

    // Attend l'enregistrement du custom element
    await page.waitForFunction(() => !!window.customElements.get('report-designer-widget'));

    // Injecte dynamiquement le tag <report-designer-widget> dans le document
    await page.evaluate(() => {
      const widget = document.createElement('report-designer-widget');
      widget.setAttribute('theme', 'dark');
      widget.setAttribute('id', 'test-e2e-widget');
      document.body.appendChild(widget);
    });

    const widgetEl = page.locator('#test-e2e-widget');
    await expect(widgetEl).toBeAttached();

    // Vérifie que l'élément a bien son conteneur et son thème
    await expect(widgetEl.locator('.report-designer-widget-container')).toBeAttached();
    await expect(widgetEl.locator('.report-designer-widget-container')).toHaveAttribute('data-theme', 'dark');

    // Vérifie la présence du designer ou de la feuille de conception dans le widget
    await expect(widgetEl.locator('app-report-designer')).toBeAttached();
  });
});

