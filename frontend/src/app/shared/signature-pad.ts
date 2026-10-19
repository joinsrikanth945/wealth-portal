import { Component, ElementRef, afterNextRender, output, signal, viewChild } from '@angular/core';

/**
 * The client signs with a finger, stylus or mouse. Emits a PNG data URL
 * after each stroke, or null when cleared.
 */
@Component({
  selector: 'app-signature-pad',
  template: `
    <div class="sig-wrap">
      <canvas
        #canvas
        class="sig-canvas"
        width="560"
        height="180"
        aria-label="Signature area: sign with your finger, a stylus or the mouse"
        role="img"
        (pointerdown)="start($event)"
        (pointermove)="move($event)"
        (pointerup)="end()"
        (pointerleave)="end()"
      ></canvas>
      <div class="sig-actions">
        <span class="muted">{{ hasInk() ? 'Signed' : 'Sign above' }}</span>
        <button type="button" class="btn btn-ghost" (click)="clear()" [disabled]="!hasInk()">
          Clear
        </button>
      </div>
    </div>
  `,
})
export class SignaturePadComponent {
  readonly signed = output<string | null>();

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private ctx: CanvasRenderingContext2D | null = null;
  private drawing = false;
  protected readonly hasInk = signal(false);

  constructor() {
    afterNextRender(() => {
      this.ctx = this.canvas().nativeElement.getContext('2d');
      if (this.ctx) {
        this.ctx.lineWidth = 2.5;
        this.ctx.lineCap = 'round';
        this.ctx.lineJoin = 'round';
        this.ctx.strokeStyle = '#12202F';
      }
    });
  }

  protected start(e: PointerEvent): void {
    if (!this.ctx) {
      return;
    }
    this.drawing = true;
    this.canvas().nativeElement.setPointerCapture(e.pointerId);
    const p = this.point(e);
    this.ctx.beginPath();
    this.ctx.moveTo(p.x, p.y);
  }

  protected move(e: PointerEvent): void {
    if (!this.drawing || !this.ctx) {
      return;
    }
    const p = this.point(e);
    this.ctx.lineTo(p.x, p.y);
    this.ctx.stroke();
    this.hasInk.set(true);
  }

  protected end(): void {
    if (!this.drawing) {
      return;
    }
    this.drawing = false;
    if (this.hasInk()) {
      this.signed.emit(this.canvas().nativeElement.toDataURL('image/png'));
    }
  }

  protected clear(): void {
    const c = this.canvas().nativeElement;
    this.ctx?.clearRect(0, 0, c.width, c.height);
    this.hasInk.set(false);
    this.signed.emit(null);
  }

  /** Pointer position in canvas pixels, whatever size the canvas is displayed at. */
  private point(e: PointerEvent): { x: number; y: number } {
    const c = this.canvas().nativeElement;
    const r = c.getBoundingClientRect();
    return {
      x: ((e.clientX - r.left) * c.width) / r.width,
      y: ((e.clientY - r.top) * c.height) / r.height,
    };
  }
}
