# Z-Craft

Mod de NeoForge para las versiones nuevas de Minecraft (26.x): razas, clases, estadísticas, ki y
transformaciones al estilo Dragon Ball.

| Rama | Minecraft | NeoForge |
|---|---|---|
| `main` | 26.2 | 26.2.0.93 o superior |

Los jars de cada versión se publican solos en la sección **Releases** cuando la compilación pasa.

## Qué trae (v0.2.0)

- **Personaje**: 6 razas (humano, saiyan, namekiano, demonio del frío, bioandroide, majin) y 8 clases
  con estadísticas iniciales y escalados distintos.
- **Estadísticas**: Fuerza, Golpe, Resistencia, Vitalidad, Poder de ki y Energía. Se suben gastando
  puntos de entrenamiento (TP), que se ganan golpeando y derrotando enemigos.
- **Ki**: barra de energía propia, carga manual y rayo de ki cargado cuyo daño depende de tu Poder de ki.
- **Transformaciones**: una línea por raza (por ejemplo Super Saiyan → SSJ2 → SSJ3), con
  multiplicadores de fuerza, defensa y ki, y drenaje de energía. Se desbloquean subiendo la
  habilidad de transformación con TP.
- **Defensa**: la Resistencia y la armadura absorben parte del daño recibido.

## Controles

| Tecla | Acción |
|---|---|
| `V` | Menú de personaje (crear, subir estadísticas) |
| `C` (mantener) | Cargar ki |
| `R` (mantener y soltar) | Rayo de ki |
| `G` | Transformarse / subir de forma |
| `H` | Volver a la forma base |

Comandos (modo creativo o consola): `/zcraft tp <cantidad>`, `/zcraft reset`.

## Compilar

Requiere Java 25. `./gradlew build` deja el jar en `build/libs/`.

## Licencia y créditos

GPL-3.0-or-later. Z-Craft adapta sistemas de [DragonMine Z](https://github.com/DragonMineZ/dragonminez);
el detalle está en [NOTICE.md](NOTICE.md).
