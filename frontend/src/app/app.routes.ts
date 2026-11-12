import { Routes } from '@angular/router';

/**
 * Pages. loadComponent loads a page's code only when it is first opened (lazy loading).
 * title sets the browser tab text.
 */
export const routes: Routes = [
  { path: '', title: 'Ask | EarningsLens', loadComponent: () => import('./features/ask/ask-page').then((m) => m.AskPage) },
  {
    path: 'corpus',
    title: 'Corpus | EarningsLens',
    loadComponent: () => import('./features/corpus/corpus-page').then((m) => m.CorpusPage),
  },
  {
    path: 'evaluation',
    title: 'Evaluation | EarningsLens',
    loadComponent: () => import('./features/evaluation/evaluation-page').then((m) => m.EvaluationPage),
  },
  {
    path: 'about',
    title: 'About | EarningsLens',
    loadComponent: () => import('./features/about/about-page').then((m) => m.AboutPage),
  },
  { path: '**', redirectTo: '' },
];
