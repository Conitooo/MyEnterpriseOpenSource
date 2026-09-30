import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([(request, next) => {
      const token = sessionStorage.getItem('meos_token');
      return next(token && request.url.startsWith('/api/')
        ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request);
    }]))
  ]
};
