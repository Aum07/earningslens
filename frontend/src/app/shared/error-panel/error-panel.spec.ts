import { TestBed } from '@angular/core/testing';
import { ApiError } from '../../core/models';
import { ErrorPanel } from './error-panel';

describe('ErrorPanel', () => {
  async function render(error: ApiError) {
    const fixture = TestBed.createComponent(ErrorPanel);
    fixture.componentRef.setInput('error', error);
    await fixture.whenStable();
    return fixture;
  }

  it('shows a title and the message, as an alert', async () => {
    const fixture = await render(new ApiError('unavailable', 503, 'The language model is not available right now.'));
    const el = fixture.nativeElement as HTMLElement;
    expect(el.getAttribute('role')).toBe('alert');
    expect(el.querySelector('h2')?.textContent).toBe('The language model is not available');
    expect(el.textContent).toContain('The language model is not available right now.');
  });

  it('emits retry when "Try again" is clicked', async () => {
    const fixture = await render(new ApiError('network', 0, 'Cannot reach the server.'));
    let retries = 0;
    fixture.componentInstance.retry.subscribe(() => retries++);
    (fixture.nativeElement as HTMLElement).querySelector('button')!.click();
    expect(retries).toBe(1);
  });

  it('has no retry button for a validation error, and lists the field errors', async () => {
    const fixture = await render(
      new ApiError('validation', 400, 'Invalid request', [{ field: 'question', message: 'size must be between 3 and 500' }]),
    );
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('button')).toBeNull();
    expect(el.querySelector('li')?.textContent).toBe('question: size must be between 3 and 500');
  });
});
