import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { VersionResumen } from '../../core/modelos';

/** Historial de versiones del horario (RF-31). */
@Component({
  selector: 'app-versiones',
  template: `
    <section class="encabezado">
      <div>
        <div class="sobretitulo">Trazabilidad</div>
        <h1>Versiones del horario</h1>
        <p>Cada publicación y cada ajuste por cierre de grupo queda guardado.</p>
      </div>
    </section>
    <section class="tarjeta lista">
      @for (v of versiones(); track v.numero) {
        <div class="version">
          <div class="num">v{{ v.numero }}</div>
          <div class="texto">
            <strong>{{ v.descripcion }}</strong>
            <span>{{ v.sesiones }} sesiones · {{ v.fecha }}</span>
          </div>
          <span class="etiqueta" [class]="'etiqueta ' + v.estado.toLowerCase()">{{ v.estado.toLowerCase() }}</span>
        </div>
      }
    </section>
  `,
  styles: [`
    .lista { padding: 6px 0; max-width: 760px; }
    .version { display: flex; align-items: center; gap: 14px; padding: 14px 18px; border-bottom: 1px solid #eef0f6; }
    .version:last-child { border-bottom: 0; }
    .num { width: 44px; height: 44px; border-radius: 12px; background: var(--navy); color: var(--lima);
      display: flex; align-items: center; justify-content: center; font-weight: 800; }
    .texto { flex: 1; display: flex; flex-direction: column; }
    .texto span { color: var(--texto-2); font-size: 12px; }
    .etiqueta { text-transform: capitalize; }
  `]
})
export class VersionesComponent implements OnInit {
  private api = inject(ApiService);
  versiones = signal<VersionResumen[]>([]);
  ngOnInit() { this.api.versiones().subscribe(v => this.versiones.set(v)); }
}
