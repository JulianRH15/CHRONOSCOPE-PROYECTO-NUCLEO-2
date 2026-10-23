import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { Notificacion } from '../../core/modelos';

/** Bandeja de correos simulados (en el producto final se envian por SMTP institucional). */
@Component({
  selector: 'app-notificaciones',
  imports: [DatePipe],
  template: `
    <section class="encabezado">
      <div>
        <div class="sobretitulo">Servicio de correo (simulado)</div>
        <h1>Notificaciones enviadas</h1>
        <p>Avisos de publicación y de cierre de grupos a docentes y estudiantes.</p>
      </div>
    </section>
    <section class="tarjeta lista">
      @for (n of notificaciones(); track n.id) {
        <article class="correo">
          <div class="avatar">{{ n.para.charAt(0) }}</div>
          <div class="texto">
            <div class="fila"><strong>{{ n.asunto }}</strong><time>{{ n.fecha | date: 'dd/MM HH:mm' }}</time></div>
            <div class="para">Para: {{ n.para }}</div>
            <p>{{ n.cuerpo }}</p>
          </div>
        </article>
      } @empty {
        <p class="vacio">Aún no se ha enviado ninguna notificación. Se envían al publicar el horario o un ajuste por cierre.</p>
      }
    </section>
  `,
  styles: [`
    .lista { padding: 6px 0; }
    .correo { display: flex; gap: 14px; padding: 14px 18px; border-bottom: 1px solid #eef0f6; }
    .correo:last-child { border-bottom: 0; }
    .avatar { width: 34px; height: 34px; border-radius: 50%; background: var(--lavanda); color: #4a2fd0;
      display: flex; align-items: center; justify-content: center; font-weight: 800; flex: none; }
    .texto { flex: 1; min-width: 0; }
    .fila { display: flex; justify-content: space-between; gap: 12px; }
    time { color: var(--texto-2); font-size: 12px; white-space: nowrap; }
    .para { color: var(--texto-2); font-size: 12px; }
    p { margin: 6px 0 0; font-size: 13px; }
    .vacio { padding: 18px; color: var(--texto-2); margin: 0; }
  `]
})
export class NotificacionesComponent implements OnInit {
  private api = inject(ApiService);
  notificaciones = signal<Notificacion[]>([]);
  ngOnInit() { this.api.notificaciones().subscribe(n => this.notificaciones.set(n)); }
}
