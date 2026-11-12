import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { LiveConfig } from '../../core/models';
import { CONFIG_DESCRIPTIONS } from '../evaluation/evaluation-page';

/** Links shown in "About the author" (Aum's own profile and the public repository). */
export const AUTHOR = {
  name: 'Aum Kanakiya',
  linkedin: 'https://www.linkedin.com/in/aum-kanakiya',
  github: 'https://github.com/Aum07/earningslens',
};

/**
 * About page (spec section 12, page 4; roadmap E6.6): how the system works, design decisions, known limitations
 * and the author. No metrics here: those live on the Evaluation page and come from latest.json (spec rule 7).
 */
@Component({
  selector: 'app-about-page',
  templateUrl: './about-page.html',
  styleUrl: './about-page.scss',
})
export class AboutPage {
  readonly author = AUTHOR;
  readonly descriptions = CONFIG_DESCRIPTIONS;

  /** The live API settings, so the page states what really runs (null while loading or when offline). */
  readonly live = toSignal(
    inject(ApiService)
      .liveConfig()
      .pipe(catchError(() => of(null as LiveConfig | null))),
    { initialValue: null },
  );
}
