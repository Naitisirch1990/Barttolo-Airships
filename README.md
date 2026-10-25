# Barttolo Airships 🚢 — Alfa 0.1.3 (Forge 1.20.1)

**Estado: COMPILA con Java 17 y ForgeGradle, pero AÚN NO VALIDADO EN JUEGO.**
**No instalar en el servidor principal ni en el mundo original de RingWorld.**

## Una nave, seis ocupantes

- **Una sola nave por servidor**, con modelo de 54 × 49 × 34 bloques generado a partir del schematic proporcionado para el proyecto.
- **Un capitán + hasta cinco amigos**. Pasajeros anclados a posiciones de la cubierta; la animación de pie es experimental, no permite caminar libremente.
- Modelo visual permanente e indestructible, **sin colocar ni destruir bloques del mundo**.
- **Cuatro cofres instalables de 27 espacios cada uno**, con inventarios virtuales guardados por la entidad; sólo interactuar detenidos.
- **Llave personalizada** (gancho de cuerda NBT y brillo), carbón, carbón vegetal, bloque de carbón o lava como combustible.
- Sólo administradores OP 4 pueden crear, entregar llaves y eliminar la nave. El primer creador es el propietario registrado.
- Se comprueba el área del casco y chunks cargados; la colisión con estructuras no es exacta.
- LOD visual de 2474 / 1477 / 458 voxeles; FPS y TPS **sin medir**.

## Comandos

Administración:
```mcfunction
/airship crear
/airship llave
/airship eliminar
```

Jugadores:
```mcfunction
/airship abordar
/airship tomarcontrol
/airship salir
/airship combustible
/airship instalarcofre 1
/airship cofre 1
/airship quitarcofre 1
/airship estado
```

Los comandos de cofres admiten números del 1 al 4.

## Controles configurables

| Tecla | Acción |
| --- | --- |
| W / S | Avanzar / retroceder |
| A / D | Girar |
| Espacio | Ascender |
| C | Descender (configurable en Controles) |
| V | Solicitar desembarque seguro (configurable en Controles) |

**Nota:** Shift provoca desmontaje en Minecraft 1.20.1; se utiliza C para descender, mientras la protección de desembarque accidental sigue bajo pruebas.

## Código y compilación

La rama `development/alpha-0.1.3` contiene los archivos Java legibles, recursos y una **copia verificada por SHA-256** en `bundle/`.

- [Compilación inicial exitosa](https://github.com/Naitisirch1990/Barttolo-Airships/actions/runs/40228454488) (ZIP artefacto con JAR experimental; no instalar en producción).
- [Comprobación de arranque de servidor aislado](https://github.com/Naitisirch1990/Barttolo-Airships/actions/workflows/server-smoke.yml).
- [PR de desarrollo para revisión](https://github.com/Naitisirch1990/Barttolo-Airships/pull/1).

Para compilar localmente, con Java 17 y Gradle 8.x:

```sh
gradle --no-daemon build
```

Instalación futura, **sólo en pruebas:** JAR en servidor Forge 1.20.1 y en **cada cliente** con el mismo mod. No necesita alterar RingWorld ni RingWorld-Nether.

## Limitaciones y riesgos que faltan probar

1. Arranque real de Forge (sólo la compilación ya está confirmada).
2. Vuelo, renderizado real y tripulación de 2 a 6 jugadores.
3. Seguridad y continuidad de cofres tras reiniciar el servidor y descargar chunks.
4. Salida V / Shift, pérdida de conexión y relevo de capitán.
5. FPS y TPS con la curvatura de RingWorld en un **mundo clonado**, no el original.
6. La autorización de propietario basada en UUID **requiere configurar correctamente la autenticación**. `online-mode=false` permite suplantación y no es seguro para un servidor abierto.

**El objetivo es llegar a una versión estable, no entregar un mod sin pruebas.**
