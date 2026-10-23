import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, mensajeError } from '../../core/api.service';
import { SesionUsuarioService } from '../../core/sesion-usuario.service';
import { Aula, Catalogos, Conflicto, HorarioVista, Parametros, SesionVista } from '../../core/modelos';

type VistaPor = 'SEMESTRE' | 'DOCENTE' | 'AULA';

const NOMBRES_DIA: Record<string, string> = {
  LUN: 'Lunes', MAR: 'Martes', MIE: 'Miércoles', JUE: 'Jueves', VIE: 'Viernes', SAB: 'Sábado'
};
const COLORES = ['#e9e5ff', '#e2f7f3', '#fff1d6', '#e6f0ff', '#f4e8ff', '#e9f8d6', '#ffe6ea', '#e8eef5'];
const BORDES = ['#6c4df6', '#21b8a0', '#e09a12', '#3b7be0', '#a347e8', '#6aa516', '#e0476a', '#5b6f8a'];

/**
 * Epica 1 - Ajuste del horario.
 * HU-01 ver la cuadricula con filtros, HU-02 mover con arrastrar y soltar,
 * HU-03 ver conflictos al instante, HU-04 deshacer/rehacer, HU-05 aprobar y publicar.
 */
@Component({
  selector: 'app-horario',
  imports: [RouterLink],
  templateUrl: './horario.component.html',
  styleUrl: './horario.component.css'
})
export class HorarioComponent implements OnInit {
  private api = inject(ApiService);
  sesion = inject(SesionUsuarioService);

  horario = signal<HorarioVista | null>(null);
  params = signal<Parametros | null>(null);
  catalogos = signal<Catalogos | null>(null);

  vistaPor = signal<VistaPor>('SEMESTRE');
  filtro = signal<number>(1);
  seleccionadaId = signal<number | null>(null);
  arrastrandoId = signal<number | null>(null);
  celdaSobre = signal<string | null>(null);
  resaltadas = signal<number[]>([]);
  ocupado = signal(false);

  readonly nombresDia = NOMBRES_DIA;

  // ------------------------------------------------------------ datos derivados

  opcionesFiltro = computed(() => {
    const cat = this.catalogos();
    if (!cat) return [];
    switch (this.vistaPor()) {
      case 'SEMESTRE': return [1, 2, 3, 4].map(n => ({ id: n, nombre: 'Semestre ' + n }));
      case 'DOCENTE': return cat.docentes.map(d => ({ id: d.id, nombre: d.nombre }));
      case 'AULA': return cat.aulas.map(a => ({ id: a.id, nombre: `${a.codigo} · ${a.capacidad} puestos` }));
    }
  });

  sesionesVisibles = computed(() => {
    const h = this.horario();
    if (!h) return [];
    const f = this.filtro();
    switch (this.vistaPor()) {
      case 'SEMESTRE': return h.sesiones.filter(s => s.semestre === f);
      case 'DOCENTE': return h.sesiones.filter(s => s.docenteId === f);
      case 'AULA': return h.sesiones.filter(s => s.aulaId === f);
    }
  });

  celdas = computed(() => {
    const mapa = new Map<string, SesionVista[]>();
    for (const s of this.sesionesVisibles()) {
      const k = this.clave(s.dia, s.franja);
      if (!mapa.has(k)) mapa.set(k, []);
      mapa.get(k)!.push(s);
    }
    return mapa;
  });

  conflictosPorSesion = computed(() => {
    const mapa = new Map<number, Conflicto[]>();
    for (const c of this.horario()?.conflictos ?? []) {
      for (const id of c.sesionIds) {
        if (!mapa.has(id)) mapa.set(id, []);
        mapa.get(id)!.push(c);
      }
    }
    return mapa;
  });

  duros = computed(() => (this.horario()?.conflictos ?? []).filter(c => c.severidad === 'DURA'));
  blandos = computed(() => (this.horario()?.conflictos ?? []).filter(c => c.severidad === 'BLANDA'));
  cambiadas = computed(() => new Set(this.horario()?.cambiadasVsPublicado ?? []));

  seleccionada = computed(() => {
    const id = this.seleccionadaId();
    return this.horario()?.sesiones.find(s => s.id === id) ?? null;
  });

  colorCurso = computed(() => {
    const mapa = new Map<string, number>();
    (this.catalogos()?.cursos ?? []).forEach((c, i) => mapa.set(c.codigo, i % COLORES.length));
    return mapa;
  });

  puedePublicar = computed(() =>
    this.sesion.esDirector() && this.horario()?.estado === 'BORRADOR' && this.duros().length === 0 && !this.ocupado());

  motivoNoPublicar = computed(() => {
    const h = this.horario();
    if (!h) return '';
    if (h.estado === 'PUBLICADO') return 'Esta versión ya está publicada.';
    if (!this.sesion.esDirector()) return 'Solo el Director de Programa puede aprobar y publicar.';
    if (this.duros().length > 0) return `Resuelve los ${this.duros().length} conflicto(s) duro(s) para poder publicar.`;
    return 'Listo para publicar.';
  });

  // ------------------------------------------------------------ ciclo de vida

  ngOnInit() {
    this.api.parametros().subscribe(p => this.params.set(p));
    this.api.catalogos().subscribe(c => this.catalogos.set(c));
    this.cargar();
  }

  cargar() {
    this.api.horario().subscribe(h => this.horario.set(h));
  }

  // ------------------------------------------------------------ filtros

  cambiarVista(v: VistaPor) {
    this.vistaPor.set(v);
    const primera = this.opcionesFiltro()[0];
    this.filtro.set(primera ? primera.id : 1);
  }

  cambiarFiltro(valor: string) {
    this.filtro.set(Number(valor));
  }

  // ------------------------------------------------------------ arrastrar y soltar

  alArrastrar(ev: DragEvent, s: SesionVista) {
    this.arrastrandoId.set(s.id);
    ev.dataTransfer?.setData('text/plain', String(s.id));
    if (ev.dataTransfer) ev.dataTransfer.effectAllowed = 'move';
  }

  alTerminarArrastre() {
    this.arrastrandoId.set(null);
    this.celdaSobre.set(null);
  }

  alPasarSobre(ev: DragEvent, dia: string, franja: number) {
    ev.preventDefault();
    this.celdaSobre.set(this.clave(dia, franja));
  }

  alSoltar(ev: DragEvent, dia: string, franja: number) {
    ev.preventDefault();
    const id = Number(ev.dataTransfer?.getData('text/plain') || this.arrastrandoId());
    this.alTerminarArrastre();
    if (!id) return;
    this.ejecutar(this.api.mover(id, { dia, franja }), true);
    this.seleccionadaId.set(id);
  }

  cambiarAula(aulaId: string) {
    const s = this.seleccionada();
    if (!s) return;
    this.ejecutar(this.api.mover(s.id, { aulaId: Number(aulaId) }), true);
  }

  // ------------------------------------------------------------ comandos

  deshacer() { this.ejecutar(this.api.deshacer(), true); }
  rehacer() { this.ejecutar(this.api.rehacer(), true); }

  publicar() {
    this.ejecutar(this.api.aprobar(), false, 'ok');
  }

  private ejecutar(obs: any, avisarConflicto: boolean, tipo: 'ok' | 'info' = 'info') {
    this.ocupado.set(true);
    obs.subscribe({
      next: (h: HorarioVista) => {
        this.horario.set(h);
        this.ocupado.set(false);
        // el mensaje ya se ve en la barra superior; el aviso flotante solo para publicar
        if (tipo === 'ok') this.sesion.avisar(h.mensaje, 'ok');
      },
      error: (e: any) => {
        this.ocupado.set(false);
        this.sesion.avisar(mensajeError(e), 'error');
      }
    });
  }

  // ------------------------------------------------------------ panel de conflictos

  irAConflicto(c: Conflicto) {
    const s = this.horario()?.sesiones.find(x => x.id === c.sesionIds[0]);
    if (!s) return;
    switch (this.vistaPor()) {
      case 'SEMESTRE': this.filtro.set(s.semestre); break;
      case 'DOCENTE': this.filtro.set(s.docenteId); break;
      case 'AULA': this.filtro.set(s.aulaId); break;
    }
    this.seleccionadaId.set(s.id);
  }

  // ------------------------------------------------------------ ayudas de plantilla

  clave(dia: string, franja: number) { return dia + '-' + franja; }

  esBloqueada(dia: string, franja: number) {
    const p = this.params();
    if (!p) return false;
    return franja === p.franjaAlmuerzo || (dia === 'SAB' && franja > 1);
  }

  etiquetaBloqueo(dia: string, franja: number) {
    return franja === this.params()?.franjaAlmuerzo ? 'Almuerzo' : 'Sin clase';
  }

  nivelConflicto(s: SesionVista): 'duro' | 'blando' | '' {
    const lista = this.conflictosPorSesion().get(s.id);
    if (!lista) return '';
    return lista.some(c => c.severidad === 'DURA') ? 'duro' : 'blando';
  }

  fondo(s: SesionVista) { return COLORES[this.colorCurso().get(s.cursoCodigo) ?? 0]; }
  borde(s: SesionVista) { return BORDES[this.colorCurso().get(s.cursoCodigo) ?? 0]; }

  nombreCorto(nombre: string) {
    const p = nombre.split(' ');
    return p.length > 2 ? `${p[0]} ${p[p.length - 2]}` : nombre;
  }

  aulasPara(s: SesionVista): Aula[] {
    return this.catalogos()?.aulas ?? [];
  }

  conflictosDe(s: SesionVista) { return this.conflictosPorSesion().get(s.id) ?? []; }
}
