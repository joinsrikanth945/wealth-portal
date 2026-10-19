import { Component, input } from '@angular/core';

export interface HomeTile {
  label: string;
  value: string;
  note?: string;
}

/** A row of figure tiles; real numbers arrive in later phases. */
@Component({
  selector: 'app-home-tiles',
  template: `
    <div class="tiles">
      @for (tile of tiles(); track tile.label) {
        <div class="tile">
          <span class="tile-label">{{ tile.label }}</span>
          <span class="tile-value">{{ tile.value }}</span>
          @if (tile.note) {
            <span class="tile-note">{{ tile.note }}</span>
          }
        </div>
      }
    </div>
  `,
})
export class HomeTilesComponent {
  readonly tiles = input.required<HomeTile[]>();
}
