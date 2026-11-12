import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { httpErrorInterceptor } from './core/http-error.interceptor';

/** App-wide providers (the frontend's equivalent of Spring configuration). */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // HttpClient for ApiService, with every error turned into an ApiError.
    provideHttpClient(withInterceptors([httpErrorInterceptor])),
  ],
};
