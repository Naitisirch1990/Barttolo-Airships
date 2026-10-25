# Barttolo Airships — Alfa 0.1.4 (corrección de aparición)

⚠️ **Compila e inicia Forge 1.20.1 en un servidor desechable, pero aún NO está validado visualmente en Minecraft con jugadores.** No instalar en el servidor principal.

## Corrección de la nave invisible / no generada

La Alfa 0.1.3 intentaba generar el modelo a una altura fija (+24 bloques) y rechazaba el lugar si había árboles, construcciones, terreno alto o chunks sin cargar. La 0.1.4:

- Busca una altura libre entre +16 y +80 sobre el jugador, sin alterar el terreno ni cargar chunks forzosamente.
- Informa la causa si la zona no es segura.
- Coloca al creador directamente a bordo, con una llave nueva y **90 segundos de combustible inicial** para poder descender. La nave sigue usando carbón o lava después.
- Añade `/airship diagnostico` (requiere OP nivel 2 o superior). El comando indica si el administrador tiene nivel OP 4, si hay un barco registrado y si faltan chunks en la zona.
- Mantiene `/airship crear` limitado al propietario con OP nivel 4, con una única nave por servidor.

### Prueba recomendada (únicamente mundo de pruebas)

1. Quita cualquier archivo `Barttolo-Airships-...0.1.3...` de la carpeta `mods` del **cliente y servidor**. Coloca solamente `Barttolo-Airships-Forge-1.20.1-0.1.4-alpha.jar` en ambos.
2. Reinicia Minecraft y Forge, entrá como OP de nivel 4 y elegí una zona extensa y despejada.
3. Ejecutá `/airship diagnostico` y revisá si ya hay alguna nave registrada.
4. Si no hay nave registrada, ejecutá `/airship crear`. Deberías subir automáticamente a la cubierta y recibir la llave. Utilizá **C** para descender y **V** para salir al aterrizar.
5. Si `/airship diagnostico` indica que **ya hay una nave registrada**, no intentes crear otra ni borres los archivos de datos guardados. Envíanos el mensaje y la posición para localizarla.
6. Si no aparece, adjuntá el chat o `logs/latest.log` para identificar el bloqueo.

### Estado del proyecto

- [Compilación Alfa 0.1.4](https://github.com/Naitisirch1990/Barttolo-Airships/actions/runs/40236068356)
- [Prueba de servidor aislado Alfa 0.1.4](https://github.com/Naitisirch1990/Barttolo-Airships/actions/runs/40236068371)

**Pendiente:** probar modelado/renderizado real, seis pasajeros y cofres con persistencia en mundo de pruebas.

---

## Documentación técnica de Alfa 0.1.3 (referencia)

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
