/**
 * Settings for the production build (`ng build`). angular.json swaps in environment.development.ts for `ng serve`.
 * apiBaseUrl is empty for now, so requests go to the same host as the page ("/api/v1/..."). The real Azure
 * backend URL is set in Phase 8 (deployment).
 */
export const environment = {
  production: true,
  apiBaseUrl: '',
};
