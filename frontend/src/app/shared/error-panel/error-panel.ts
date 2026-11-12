import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { ApiError, ApiErrorKind } from '../../core/models';

const TITLES: Record<ApiErrorKind, string> = {
  validation: 'The question was not accepted',
  'not-found': 'Not found',
  'rate-limited': 'Too many questions',
  unavailable: 'The language model is not available',
  server: 'Something went wrong',
  network: 'Cannot reach the server',
  unknown: 'Something went wrong',
};

/**
 * Error state with an optional "Try again" button (spec section 12).
 * output(): an event the parent can listen to, here (retry)="...".
 */
@Component({
  selector: 'app-error-panel',
  imports: [MatButtonModule],
  templateUrl: './error-panel.html',
  styleUrl: './error-panel.scss',
  host: { role: 'alert' },
})
export class ErrorPanel {
  readonly error = input.required<ApiError>();
  readonly retry = output<void>();

  readonly title = computed(() => TITLES[this.error().kind]);
}
