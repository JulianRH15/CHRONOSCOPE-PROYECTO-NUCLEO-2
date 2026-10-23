import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService } from './core/api.service';
import { Rol, SesionUsuarioService } from './core/sesion-usuario.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  sesion = inject(SesionUsuarioService);
  private api = inject(ApiService);
  private router = inject(Router);

  cambiarRol(valor: string) {
    this.sesion.rol.set(valor as Rol);
    this.sesion.avisar('Ahora estás como ' + this.sesion.nombreRol() + '.', 'info');
  }

  reiniciar() {
    this.api.reiniciar().subscribe(() => {
      this.sesion.avisar('Datos de demostración reiniciados.', 'ok');
      // recarga la pagina actual con datos nuevos
      const url = this.router.url;
      this.router.navigateByUrl('/versiones', { skipLocationChange: true })
        .then(() => this.router.navigateByUrl(url));
    });
  }
}
