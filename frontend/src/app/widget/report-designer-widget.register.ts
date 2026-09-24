import { Injector } from '@angular/core';
import { createCustomElement } from '@angular/elements';

/**
 * Enregistre le Web Component `<report-designer-widget>` dans le CustomElementRegistry du navigateur
 * de façon asynchrone pour préserver le découpage en chunks (Lazy Loading).
 */
export async function registerReportDesignerWidget(injector: Injector): Promise<void> {
  if (typeof window !== 'undefined' && window.customElements) {
    if (!customElements.get('report-designer-widget')) {
      const { ReportDesignerWidgetComponent } = await import('./report-designer-widget.component');
      const el = createCustomElement(ReportDesignerWidgetComponent, { injector });
      customElements.define('report-designer-widget', el);
      console.log('✅ Web Component <report-designer-widget> enregistré avec succès.');
    }
  }
}

