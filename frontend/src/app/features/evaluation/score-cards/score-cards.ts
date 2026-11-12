import { Component, computed, input } from '@angular/core';
import type { ScoreCard } from '../evaluation-page';

/**
 * The live config's score cards (progress bar gold at 85% or more, red with a "weaker" pill below) and the
 * "Where it's weaker" callout that lists the weaker ones. Shows only what the Accuracy report passes in, which all
 * comes from latest.json.
 */
@Component({
  selector: 'app-score-cards',
  templateUrl: './score-cards.html',
  styleUrl: './score-cards.scss',
})
export class ScoreCards {
  readonly cards = input.required<ScoreCard[]>();
  readonly weaker = computed(() => this.cards().filter((c) => c.weaker));
}
