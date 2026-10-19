import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { HistoryPoint } from '../core/api/models';

const W = 640;
const H = 200;
const PAD = { top: 16, right: 16, bottom: 28, left: 44 };

/** Simple line chart of the month-end performance index. Plain SVG, no library. */
@Component({
  selector: 'app-performance-chart',
  imports: [DatePipe, DecimalPipe],
  template: `
    @if (geometry(); as g) {
      <svg
        [attr.viewBox]="'0 0 ' + width + ' ' + height"
        class="perf-chart"
        role="img"
        [attr.aria-label]="'Performance index from ' + g.firstLabel + ' to ' + g.lastLabel"
      >
        @for (t of g.ticks; track t.y) {
          <line
            [attr.x1]="pad.left"
            [attr.x2]="width - pad.right"
            [attr.y1]="t.y"
            [attr.y2]="t.y"
            class="grid"
          />
          <text [attr.x]="pad.left - 8" [attr.y]="t.y + 4" class="axis" text-anchor="end">
            {{ t.value | number: '1.0-0' }}
          </text>
        }
        <path [attr.d]="g.area" class="area" />
        <path [attr.d]="g.line" class="line" />
        @for (p of g.points; track p.x; let last = $last) {
          @if (last) {
            <circle [attr.cx]="p.x" [attr.cy]="p.y" r="4" class="dot" />
          }
        }
        <text [attr.x]="pad.left" [attr.y]="height - 6" class="axis">
          {{ g.first | date: 'MMM y' }}
        </text>
        <text [attr.x]="width - pad.right" [attr.y]="height - 6" class="axis" text-anchor="end">
          {{ g.last | date: 'MMM y' }}
        </text>
      </svg>
    } @else {
      <p class="muted">Not enough history to draw a chart yet.</p>
    }
  `,
})
export class PerformanceChartComponent {
  readonly history = input.required<HistoryPoint[]>();

  protected readonly width = W;
  protected readonly height = H;
  protected readonly pad = PAD;

  protected readonly geometry = computed(() => {
    const h = this.history();
    if (h.length < 2) {
      return null;
    }
    const values = h.map((p) => p.indexValue);
    const min = Math.floor(Math.min(...values) - 1);
    const max = Math.ceil(Math.max(...values) + 1);
    const x = (i: number) => PAD.left + (i * (W - PAD.left - PAD.right)) / (h.length - 1);
    const y = (v: number) => PAD.top + ((max - v) * (H - PAD.top - PAD.bottom)) / (max - min);

    const points = h.map((p, i) => ({ x: x(i), y: y(p.indexValue) }));
    const line = points
      .map((p, i) => `${i ? 'L' : 'M'}${p.x.toFixed(1)},${p.y.toFixed(1)}`)
      .join(' ');
    const bottom = H - PAD.bottom;
    const area = `${line} L${points.at(-1)!.x.toFixed(1)},${bottom} L${points[0].x.toFixed(1)},${bottom} Z`;
    const ticks = [min, (min + max) / 2, max].map((v) => ({ value: v, y: y(v) }));

    return {
      points,
      line,
      area,
      ticks,
      first: h[0].asOf,
      last: h.at(-1)!.asOf,
      firstLabel: h[0].asOf,
      lastLabel: h.at(-1)!.asOf,
    };
  });
}
