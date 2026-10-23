import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withHashLocation } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Hash (#/horario) para que Spring Boot sirva la app sin configuracion extra
    provideRouter(routes, withHashLocation()),
    provideHttpClient()
  ]
};
