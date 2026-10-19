import { Component, computed, input } from '@angular/core';
import { MifidDisplayStatus } from '../core/api/models';
import { MIFID_STATUS, Tone } from './labels';

@Component({
  selector: 'app-mifid-badge',
  template: `<span class="status status-{{ tone() }}">{{ label() }}</span>`,
})
export class MifidBadgeComponent {
  readonly status = input.required<MifidDisplayStatus>();
  protected readonly label = computed(() => MIFID_STATUS[this.status()]?.label ?? this.status());
  protected readonly tone = computed<Tone>(() => MIFID_STATUS[this.status()]?.tone ?? 'neutral');
}
