import { Component, input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/** Spinner plus a message, announced politely to screen readers (role="status"). */
@Component({
  selector: 'app-loading',
  imports: [MatProgressSpinnerModule],
  template: `<mat-progress-spinner mode="indeterminate" diameter="28" aria-hidden="true" /><span>{{ message() }}</span>`,
  styles: `:host { display: flex; gap: 12px; align-items: center; margin: 32px 0; color: var(--text-secondary); font-family: var(--font-mono); font-size: 12px; }`,
  host: { role: 'status' },
})
export class Loading {
  readonly message = input('Loading...');
}
