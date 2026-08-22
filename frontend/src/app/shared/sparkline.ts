import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-sparkline',
  standalone: true,
  template: `
    <svg [attr.viewBox]="'0 0 100 32'" preserveAspectRatio="none" class="sparkline">
      <polyline [attr.points]="points()" fill="none" [attr.stroke]="color()" stroke-width="2" />
    </svg>
  `,
  styles: [
    `
      .sparkline {
        width: 100%;
        height: 32px;
        display: block;
      }
    `,
  ],
})
export class Sparkline {
  /** Lista de precios de cierre, en orden cronológico (el más viejo primero). */
  values = input.required<number[]>();
  /** Color de la línea — por default menta, pero se puede pasar el color de riesgo. */
  color = input<string>('#7fe0c0');

  points = computed(() => {
    const data = this.values();
    if (!data || data.length < 2) {
      return '';
    }

    const min = Math.min(...data);
    const max = Math.max(...data);
    const range = max - min || 1; // evita división por cero si el precio no varió

    return data
      .map((value, index) => {
        const x = (index / (data.length - 1)) * 100;
        // invertido porque en SVG el eje Y crece hacia abajo
        const y = 32 - ((value - min) / range) * 32;
        return `${x},${y}`;
      })
      .join(' ');
  });
}
