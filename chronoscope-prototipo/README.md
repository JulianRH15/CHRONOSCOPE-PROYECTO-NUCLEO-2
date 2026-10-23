# ChronoScope — Prototipo de alta fidelidad

Prototipo funcional de dos épicas del sistema de generación y gestión de horarios
del Programa de Ingeniería de Sistemas (Universidad El Bosque).

| Épica | Qué se puede hacer | Requerimientos |
|---|---|---|
| **1. Ajuste y publicación del horario** | Ver la cuadrícula semanal por semestre, docente o aula; arrastrar sesiones; ver conflictos al instante; cambiar aula; deshacer/rehacer; aprobar y publicar (solo Director). | RF-25, RF-26, RF-27, RF-29, RF-31 |
| **2. Cierre de grupos** | Cargar el CSV de inscritos de Matrícula; ver grupos bajo el mínimo; cerrar un grupo; re-optimizar solo las sesiones afectadas; publicar el ajuste y notificar. | RF-32, RF-33, RF-34, RF-35, RF-36 |

**Tecnología:** Spring Boot 3.3 (Java 17) + API REST documentada con Swagger + interfaz en Angular 19.
La interfaz ya viene compilada dentro de `src/main/resources/static`, así que **solo hay que correr Spring Boot**.

## Cómo correrlo en Eclipse

1. Tener JDK 17 o superior.
2. `File > Import > Maven > Existing Maven Projects` y elegir esta carpeta (la que tiene `pom.xml`).
   La primera vez Maven descarga las dependencias (necesita internet, unos minutos).
3. Clic derecho en `ChronoScopeApplication.java` > `Run As > Java Application`.
4. Abrir:
   - Aplicación: <http://localhost:8080>
   - Swagger (API): <http://localhost:8080/swagger-ui.html>

También se puede correr por consola: `mvn spring-boot:run`.

## Guion rápido de demostración

1. **Horario** (rol *Comité de Área*): pasar a vista *Docente → Ana María Torres*, arrastrar
   CAL1-01 encima de CAL3-01 → aparece el conflicto "cruce de docente" en rojo. *Deshacer*.
2. Arrastrar cualquier sesión a la franja de *Almuerzo* → conflicto "franja bloqueada". *Deshacer*.
3. Cambiar el rol a *Director de Programa* → *Aprobar y publicar* (versión 1).
4. **Cierre de grupos** → *Usar archivo de Matrícula 2026-2* → aparecen CAL3-02 (7) y MYS-02 (9) bajo el mínimo.
5. *Cerrar grupo* CAL3-02 → los 7 estudiantes pasan a CAL3-01 (43) y sus 2 sesiones cambian
   de A-201 (40 puestos) a A-305 (45 puestos). Las otras 40 sesiones no cambian.
6. *Ver cambios en el horario* (Semestre 3: etiquetas "cambió") → volver → *Validar y publicar ajuste*.
7. **Notificaciones** y **Versiones** muestran los correos enviados y el historial v1 → v2.

El botón **Reiniciar demo** (arriba a la derecha) deja todo como al principio, útil para repetir el video.

## Estructura

```
src/main/java/co/edu/unbosque/chronoscope/
  controller/   HorarioController, CierreGruposController, GeneralController (API REST)
  service/      HorarioService (épica 1), CierreGruposService (épica 2),
                ValidadorConflictos (reglas duras y blandas), MotorHorarios (generar y re-optimizar)
  repository/   DatosRepository (datos de prueba en memoria)
  model/        Docente, Aula, Curso, Grupo, Sesion, Conflicto, VersionHorario, Notificacion
  dto/          Objetos que envía y recibe la API
src/main/resources/
  static/                         interfaz Angular ya compilada
  matricula_inscritos_2026-2.csv  archivo de ejemplo de Matrícula
  application.properties          cupo (40), tolerancia (10 %), mínimo (12)
frontend/                         código fuente Angular
```

## Modificar la interfaz (opcional, requiere Node.js 20+)

```
cd frontend
npm install
npx ng serve          # desarrollo en http://localhost:4200 (Spring Boot debe estar corriendo)
npx ng build          # recompila y deja el resultado en src/main/resources/static
```

## Alcance del prototipo

- Los datos viven en memoria (22 grupos, 10 docentes, 8 aulas); al reiniciar la aplicación vuelven al inicio.
- El motor usa una heurística voraz para el horario inicial; en el producto final esa estrategia
  es programación entera + GRASP con búsqueda tabú (patrón Strategy, sin cambiar el resto).
- El inicio de sesión y los correos son simulados (selector de rol y bandeja de notificaciones).
