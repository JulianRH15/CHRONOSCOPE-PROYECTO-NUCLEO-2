import { Injectable, computed, signal } from '@angular/core';

export type Rol = 'COMITE' | 'DIRECTOR';

/** Rol simulado (en el producto final viene del inicio de sesion institucional). */
@Injectable({ providedIn: 'root' })
export class SesionUsuarioService {
  readonly rol = signal<Rol>('COMITE');
  readonly esDirector = computed(() => this.rol() === 'DIRECTOR');
  readonly nombreRol = computed(() => this.rol() === 'DIRECTOR' ? 'Director de Programa' : 'Comité de Área');
  /** Aviso global (toast) */
  readonly aviso = signal<{ texto: string; tipo: 'ok' | 'error' | 'info' } | null>(null);
  private temporizador: any;

  avisar(texto: string, tipo: 'ok' | 'error' | 'info' = 'info') {
    this.aviso.set({ texto, tipo });
    clearTimeout(this.temporizador);
    this.temporizador = setTimeout(() => this.aviso.set(null), 5000);
  }
}
