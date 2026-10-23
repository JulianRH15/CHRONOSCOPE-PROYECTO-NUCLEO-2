import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, mensajeError } from '../../core/api.service';
import { SesionUsuarioService } from '../../core/sesion-usuario.service';
import { AnalisisCierre, GrupoAnalisis, HorarioVista, Notificacion, ResultadoCierre } from '../../core/modelos';

/**
 * Epica 2 - Cierre de grupos por baja matricula.
 * HU-06 cargar inscritos, HU-07 ver grupos bajo el minimo, HU-08 cerrar un grupo,
 * HU-09 re-optimizar solo lo afectado, HU-10 publicar el ajuste y notificar.
 */
@Component({
  selector: 'app-cierre',
  imports: [RouterLink],
  templateUrl: './cierre.component.html',
  styleUrl: './cierre.component.css'
})
export class CierreComponent implements OnInit {
  private api = inject(ApiService);
  sesion = inject(SesionUsuarioService);

  analisis = signal<AnalisisCierre | null>(null);
  horario = signal<HorarioVista | null>(null);
  resultado = signal<ResultadoCierre | null>(null);
  enviadas = signal<Notificacion[]>([]);
  porCerrar = signal<GrupoAnalisis | null>(null);
  soloAlertas = signal(true);
  ocupado = signal(false);
  errorArchivo = signal<string | null>(null);

  paso = computed(() => {
    if (this.enviadas().length) return 4;
    if (this.resultado()) return 3;
    if (this.analisis()?.inscritosCargados) return 2;
    return 1;
  });

  publicado = computed(() => this.horario()?.estado === 'PUBLICADO');

  grupos = computed(() => {
    const lista = this.analisis()?.grupos ?? [];
    const filtrada = this.soloAlertas()
      ? lista.filter(g => g.estadoMatricula !== 'OK' && g.estadoMatricula !== 'SIN_DATOS')
      : lista;
    const orden: Record<string, number> = { BAJO_MINIMO: 0, SOBRE_CUPO: 1, CERRADO: 2, OK: 3, SIN_DATOS: 4 };
    return [...filtrada].sort((a, b) => orden[a.estadoMatricula] - orden[b.estadoMatricula] || a.inscritos - b.inscritos);
  });

  totales = computed(() => {
    const l = this.analisis()?.grupos ?? [];
    return {
      total: l.filter(g => g.estadoMatricula !== 'CERRADO').length,
      bajo: l.filter(g => g.estadoMatricula === 'BAJO_MINIMO').length,
      sobre: l.filter(g => g.estadoMatricula === 'SOBRE_CUPO').length,
      cerrados: l.filter(g => g.estadoMatricula === 'CERRADO').length
    };
  });

  ngOnInit() {
    this.api.analisis().subscribe(a => this.analisis.set(a));
    this.api.horario().subscribe(h => this.horario.set(h));
    this.api.resultadoCierre().subscribe(r => this.resultado.set(r ?? null));
  }

  // ---------------------------------------------------------- paso 1: cargar inscritos

  usarEjemplo() {
    this.errorArchivo.set(null);
    this.api.cargarEjemplo().subscribe({
      next: a => { this.analisis.set(a); this.sesion.avisar('Inscritos cargados desde Matrícula.', 'ok'); },
      error: e => this.errorArchivo.set(mensajeError(e))
    });
  }

  subirArchivo(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) return;
    this.errorArchivo.set(null);
    this.api.cargarArchivo(archivo).subscribe({
      next: a => { this.analisis.set(a); this.sesion.avisar('Archivo validado y cargado.', 'ok'); },
      error: e => this.errorArchivo.set(mensajeError(e))
    });
    input.value = '';
  }

  // ---------------------------------------------------------- paso 2-3: cerrar

  pedirCierre(g: GrupoAnalisis) {
    if (!this.sesion.esDirector()) {
      this.sesion.avisar('Solo el Director de Programa puede cerrar grupos. Cambia el rol arriba a la derecha.', 'error');
      return;
    }
    this.porCerrar.set(g);
  }

  confirmarCierre() {
    const g = this.porCerrar();
    if (!g) return;
    this.ocupado.set(true);
    this.api.cerrarGrupo(g.grupoId).subscribe({
      next: r => {
        this.resultado.set(r);
        this.enviadas.set([]);
        this.porCerrar.set(null);
        this.ocupado.set(false);
        this.api.analisis().subscribe(a => this.analisis.set(a));
        this.api.horario().subscribe(h => this.horario.set(h));
        this.sesion.avisar(`Grupo ${r.grupoCerrado} cerrado. Revisa el impacto antes de publicar.`, 'ok');
      },
      error: e => {
        this.ocupado.set(false);
        this.porCerrar.set(null);
        this.sesion.avisar(mensajeError(e), 'error');
      }
    });
  }

  // ---------------------------------------------------------- paso 4: publicar

  publicar() {
    this.ocupado.set(true);
    this.api.publicarAjuste().subscribe({
      next: n => {
        this.enviadas.set(n);
        this.resultado.set(null);
        this.ocupado.set(false);
        this.api.horario().subscribe(h => this.horario.set(h));
        this.sesion.avisar('Ajuste publicado. Se enviaron ' + n.length + ' notificaciones.', 'ok');
      },
      error: e => { this.ocupado.set(false); this.sesion.avisar(mensajeError(e), 'error'); }
    });
  }

  otroCierre() {
    this.enviadas.set([]);
  }

  // ---------------------------------------------------------- ayudas

  textoEstado(e: string) {
    return ({ BAJO_MINIMO: 'Bajo el mínimo', SOBRE_CUPO: 'Sobre el cupo', OK: 'Normal', CERRADO: 'Cerrado', SIN_DATOS: 'Sin datos' } as any)[e];
  }

  porcentaje(g: GrupoAnalisis) {
    return Math.min(100, Math.round((g.inscritos / g.cupoConTolerancia) * 100));
  }

  totalReceptor(g: GrupoAnalisis) {
    const r = this.analisis()?.grupos.find(x => x.codigo === g.grupoReceptor);
    return r ? r.inscritos + g.inscritos : null;
  }
}
