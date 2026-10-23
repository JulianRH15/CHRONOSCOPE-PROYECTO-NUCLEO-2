import { Routes } from '@angular/router';
import { HorarioComponent } from './pages/horario/horario.component';
import { CierreComponent } from './pages/cierre/cierre.component';
import { NotificacionesComponent } from './pages/notificaciones/notificaciones.component';
import { VersionesComponent } from './pages/versiones/versiones.component';

export const routes: Routes = [
  { path: '', redirectTo: 'horario', pathMatch: 'full' },
  { path: 'horario', component: HorarioComponent, title: 'Horario · ChronoScope' },
  { path: 'cierre', component: CierreComponent, title: 'Cierre de grupos · ChronoScope' },
  { path: 'versiones', component: VersionesComponent, title: 'Versiones · ChronoScope' },
  { path: 'notificaciones', component: NotificacionesComponent, title: 'Notificaciones · ChronoScope' },
  { path: '**', redirectTo: 'horario' }
];
