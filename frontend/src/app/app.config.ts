import { ApplicationConfig, ENVIRONMENT_INITIALIZER, Injector, inject, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { AuthInterceptor } from './interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    {
      provide: ENVIRONMENT_INITIALIZER,
      multi: true,
      useValue: () => {
        const injector = inject(Injector);
        // Enregistrement asynchrone du Web Component pour maintenir le bundle initial léger
        import('./widget/report-designer-widget.register').then(m => {
          m.registerReportDesignerWidget(injector);
        });
      }
    }
  ],
};
