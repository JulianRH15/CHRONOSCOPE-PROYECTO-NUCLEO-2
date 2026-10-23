import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AnalisisCierre, Catalogos, HorarioVista, Notificacion, Parametros, ResultadoCierre, VersionResumen } from './modelos';

/** Unico punto de contacto con la API REST de Spring Boot. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  parametros() { return this.http.get<Parametros>('/api/parametros'); }
  catalogos() { return this.http.get<Catalogos>('/api/catalogos'); }

  // Epica 1 - Ajuste y publicacion del horario
  horario() { return this.http.get<HorarioVista>('/api/horario'); }
  mover(id: number, cambio: { dia?: string; franja?: number; aulaId?: number }) {
    return this.http.put<HorarioVista>(`/api/horario/sesiones/${id}`, cambio);
  }
  deshacer() { return this.http.post<HorarioVista>('/api/horario/deshacer', {}); }
  rehacer() { return this.http.post<HorarioVista>('/api/horario/rehacer', {}); }
  aprobar() { return this.http.post<HorarioVista>('/api/horario/aprobar', {}); }
  versiones() { return this.http.get<VersionResumen[]>('/api/horario/versiones'); }

  // Epica 2 - Cierre de grupos
  analisis() { return this.http.get<AnalisisCierre>('/api/cierre/analisis'); }
  cargarEjemplo() { return this.http.post<AnalisisCierre>('/api/cierre/inscritos/ejemplo', {}); }
  cargarArchivo(archivo: File) {
    const datos = new FormData();
    datos.append('archivo', archivo);
    return this.http.post<AnalisisCierre>('/api/cierre/inscritos', datos);
  }
  cerrarGrupo(grupoId: number) { return this.http.post<ResultadoCierre>(`/api/cierre/grupos/${grupoId}/cerrar`, {}); }
  resultadoCierre() { return this.http.get<ResultadoCierre | null>('/api/cierre/resultado'); }
  publicarAjuste() { return this.http.post<Notificacion[]>('/api/cierre/publicar', {}); }

  notificaciones() { return this.http.get<Notificacion[]>('/api/notificaciones'); }
  reiniciar() { return this.http.post('/api/demo/reiniciar', {}); }
}

/** Extrae el mensaje de error que envia Spring Boot. */
export function mensajeError(e: any): string {
  return e?.error?.message || e?.message || 'Ocurrió un error inesperado';
}
