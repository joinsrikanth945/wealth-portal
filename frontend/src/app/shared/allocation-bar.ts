import { DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { AllocationSlice } from '../core/api/models';
import { ASSET_CLASS_COLORS, ASSET_CLASS_LABELS } from './labels';

/** Horizontal stacked bar with a legend: how the portfolio is split. */
@Component({
  selector: 'app-allocation-bar',
  imports: [DecimalPipe],
  template: `
    <div class="alloc-bar" role="img" [attr.aria-label]="summary()">
      @for (s of slices(); track s.key) {
        <div class="alloc-seg" [style.flex]="s.weight" [style.background]="s.color"></div>
      }
    </div>
    <ul class="legend">
      @for (s of slices(); track s.key) {
        <li>
          <span class="swatch" [style.background]="s.color"></span>
          {{ s.label }} <strong>{{ s.weight | number: '1.1-1' }}%</strong>
        </li>
      }
    </ul>
  `,
})
export class AllocationBarComponent {
  readonly allocation = input.required<AllocationSlice[]>();

  protected readonly slices = computed(() =>
    this.allocation()
      .filter((a) => a.weightPercent > 0)
      .map((a) => ({
        key: a.assetClass,
        label: ASSET_CLASS_LABELS[a.assetClass] ?? a.assetClass,
        color: ASSET_CLASS_COLORS[a.assetClass] ?? '#9AA8B4',
        weight: a.weightPercent,
      })),
  );

  protected readonly summary = computed(
    () =>
      'Allocation: ' +
      this.slices()
        .map((s) => `${s.label} ${s.weight.toFixed(1)}%`)
        .join(', '),
  );
}
