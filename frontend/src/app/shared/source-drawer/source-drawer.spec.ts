import { Dialog, DialogRef } from '@angular/cdk/dialog';
import { Injector } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Citation } from '../../core/models';
import { SourceDrawer } from './source-drawer';

describe('SourceDrawer', () => {
  const citation: Citation = {
    marker: 3,
    chunkId: 'c3',
    ticker: 'MSFT',
    fiscalYear: 2026,
    fiscalQuarter: 2,
    docType: 'PRESS_RELEASE',
    page: null,
    section: 'Business Highlights',
    speaker: null,
    sourceUrl: 'https://example.test/msft-q2',
    snippet: 'x'.repeat(300),
  };

  let ref: DialogRef<unknown, SourceDrawer>;
  const drawer = () => document.querySelector('app-source-drawer') as HTMLElement;

  async function open(sentences: string[] = ['Revenue was $81.3 billion.']): Promise<void> {
    ref = SourceDrawer.open(TestBed.inject(Dialog), TestBed.inject(Injector), { citation, sentences });
    await new Promise((r) => setTimeout(r));
  }

  afterEach(() => ref?.close());

  it('shows the doc-meta rows, the passage and the supported sentence', async () => {
    await open();
    const rows = [...drawer().querySelectorAll('.meta-row')].map((r) =>
      [r.querySelector('dt')?.textContent?.trim(), r.querySelector('dd')?.textContent?.trim()],
    );
    expect(rows).toEqual([
      ['Company', 'MSFT'],
      ['Fiscal quarter', 'FY2026 Q2'],
      ['Document', 'Press release'],
      ['Section', 'Business Highlights'],
    ]);
    expect(drawer().querySelector('.passage')?.textContent).toBe('x'.repeat(300) + '...');
    expect(drawer().textContent).toContain('The first 300 characters of the passage');
    expect(drawer().querySelector('.supports')?.textContent).toBe('Revenue was $81.3 billion.');
    const link = drawer().querySelector('a')!;
    expect(link.getAttribute('href')).toBe('https://example.test/msft-q2');
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('is a labelled dialog, and leaves out "Supports this sentence" when no sentence cites the source', async () => {
    await open([]);
    const dialog = document.querySelector('[role="dialog"]')!;
    expect(dialog.getAttribute('aria-labelledby')).toBe('source-drawer-title');
    expect(drawer().querySelector('.supports')).toBeNull();
    expect(drawer().textContent).not.toContain('Supports this sentence');
  });

  it('closes with the Close button', async () => {
    await open();
    let closed = false;
    ref.closed.subscribe(() => (closed = true));
    drawer().querySelector<HTMLButtonElement>('.outline')!.click();
    expect(closed).toBe(true);
  });
});
