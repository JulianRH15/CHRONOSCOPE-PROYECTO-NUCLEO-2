// Tipos que devuelve la API de Spring Boot (ver Swagger en /swagger-ui.html)

export interface SesionVista {
  id: number; grupoId: number; grupoCodigo: string; cursoCodigo: string; cursoNombre: string;
  semestre: number; docenteId: number; docenteNombre: string; aulaId: number; aulaCodigo: string;
  aulaCapacidad: number; inscritos: number; dia: string; franja: number;
}

export interface Conflicto { tipo: string; severidad: 'DURA' | 'BLANDA'; mensaje: string; sesionIds: number[]; }

export interface HorarioVista {
  version: number; estado: 'BORRADOR' | 'PUBLICADO'; sesiones: SesionVista[]; conflictos: Conflicto[];
  puedeDeshacer: boolean; puedeRehacer: boolean; cambiadasVsPublicado: number[]; mensaje: string;
}

export interface Parametros {
  cupoMaximo: number; toleranciaPorcentaje: number; minimoEstudiantes: number;
  dias: string[]; franjas: string[]; franjaAlmuerzo: number;
}

export interface Docente { id: number; nombre: string; correo: string; dedicacion: string; maxHorasSemana: number; noDisponible: string[]; }
export interface Aula { id: number; codigo: string; capacidad: number; tipo: string; }
export interface Curso { id: number; codigo: string; nombre: string; semestre: number; tipoAula: string; }
export interface Catalogos { docentes: Docente[]; aulas: Aula[]; cursos: Curso[]; }

export interface GrupoAnalisis {
  grupoId: number; codigo: string; curso: string; semestre: number; docente: string; inscritos: number;
  minimo: number; cupoConTolerancia: number; estadoMatricula: 'SIN_DATOS' | 'OK' | 'BAJO_MINIMO' | 'SOBRE_CUPO' | 'CERRADO';
  estadoGrupo: string; grupoReceptor: string | null;
}
export interface AnalisisCierre {
  inscritosCargados: boolean; archivo: string | null; minimo: number; cupoMaximo: number;
  cupoConTolerancia: number; grupos: GrupoAnalisis[];
}
export interface CambioSesion { sesionId: number; grupo: string; antes: string; despues: string; motivo: string; }
export interface ResultadoCierre {
  grupoCerrado: string; grupoReceptor: string | null; estudiantesReasignados: number; inscritosReceptor: number;
  sesionesLiberadas: string[]; sesionesReoptimizadas: CambioSesion[]; sesionesIntactas: number;
  notificacionesPendientes: string[]; nuevaVersion: number;
}
export interface Notificacion { id: number; fecha: string; para: string; asunto: string; cuerpo: string; }
export interface VersionResumen { numero: number; estado: string; fecha: string; descripcion: string; sesiones: number; }
