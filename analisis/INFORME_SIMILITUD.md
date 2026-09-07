# Análisis del informe Turnitin — 23 % de similitud

**Documento:** *Estudio de prefactibilidad para la producción y distribución de leche pasteurizada a mercado nicho en Honduras*
**Entrega:** `trn:oid:::3117:620399841` · 4 sept 2026 · 52.958 palabras · 260 páginas
**Meta institucional:** ≤ 20 % · **Actual:** 23 % · **Brecha:** hay que retirar ~1.600 palabras coincidentes como mínimo (recomendado: 3.000–4.000 para tener margen)

---

## 1. La conclusión en una frase

No hay un capítulo copiado que se pueda borrar de un golpe. El 23 % son **cientos de coincidencias pequeñas** (la fuente más grande aporta solo 1 %) y se concentran en un sitio muy concreto: **el Marco Teórico y el Diseño Metodológico** (páginas 19–52), donde el texto describe metodologías y definiciones de manual **sin citar de dónde salen**. Ese tramo son 34 de 260 páginas —13 % de la tesis— pero cargan con **8 de los 23 puntos**.

Dato revelador: el informe indica **17 % en “Trabajos entregados (trabajos del estudiante)”**, casi igual que el 17 % de internet. Eso significa que el texto coincide con **cientos de otras tesis** de universidades distintas, cada una con <1 %. Es la firma inconfundible de definiciones genéricas de libro de texto: todos los tesistas copian los mismos párrafos de Sapag, de ONUDI, del marco lógico y de la interpretación del VAN/TIR.

**La buena noticia:** casi todo lo que hay que arreglar coincide con lo que un jurado te va a señalar de todas formas. Bajar de 20 % aquí no es cosmética, es mejorar la tesis.

---

## 2. Cómo se reparte el 23 %

Medición hecha sobre los resaltados del PDF, palabra por palabra:

| Zona | Págs. tesis | Puntos del 23 % | % de esa zona resaltado |
|---|---|---|---|
| Portada, índices, resumen, introducción, justificación | 1–18 | 1,3 | 19 % |
| **MARCO TEÓRICO — metodologías y estudios** | **19–39** | **5,6** | **55 %** ⚠️ |
| **Marco teórico leche + DISEÑO METODOLÓGICO** | **40–52** | **2,4** | **41 %** ⚠️ |
| Estudio sectorial | 53–71 | 1,9 | 23 % |
| Estudio de mercado | 72–97 | 2,1 | 20 % |
| Estudio técnico | 98–127 | 2,5 | 20 % |
| Estudio organizacional | 128–134 | 0,8 | 25 % |
| Estudio legal | 135–146 | 1,2 | 24 % |
| Estudio ambiental | 147–154 | 1,4 | 34 % |
| Estudio financiero (flujos y costos) | 155–177 | 3,1 | 30 % |
| **Indicadores VAN/TIR/PRI + notas legales** | **178–201** | **3,2** | **31 %** ⚠️ |
| Análisis de riesgos (Monte Carlo) | 202–244 | 1,6 | **9,5 %** ✅ |
| Conclusiones y recomendaciones | 245–249 | 0,4 | 25 % |
| Bibliografía | 250–255 | 0 | 0 % (bien filtrada) |
| Anexos | 256–260 | 0 | 0 % |

Fíjate en el contraste: el **análisis de riesgos con Monte Carlo** (43 páginas, lo más original y propio de la tesis) solo tiene **9,5 %** de coincidencia. Donde escribiste con tus datos, Turnitin no encuentra nada. El problema está donde se describe teoría.

### Las 12 páginas más críticas

| Pág. tesis | Resaltado | Qué contiene |
|---|---|---|
| 49 | **93 %** | Legislación laboral (jornada 44 h, contratos) en el diseño metodológico |
| 24 | **90 %** | Lista de factores legales de Sapag (patentes, desahucios, aranceles…) |
| 46 | **88 %** | Estudio técnico: definición de manual |
| 23 | **88 %** | Estudio legal + estudio técnico (definiciones) |
| 47 | **85 %** | Tamaño y localización del proyecto (texto de Sapag) |
| 25 | **82 %** | Estudio ambiental, financiero, análisis de riesgo (definiciones) |
| 48 | **79 %** | Marco regulatorio, régimen tributario |
| 36 | **76 %** | Metodología (viabilidad ambiental/jurídica) |
| 22 | **76 %** | ONUDI |
| 33 | **71 %** | Ciclo de gestión de proyectos |
| 37 | **68 %** | Criterios CAD-OCDE / ALNAP |
| 28 | **66 %** | JICA |

El detalle pasaje por pasaje (188 bloques de ≥25 palabras, con página, fuente y número de párrafo del Word) está en **`ANEXO_PASAJES.md`**.

---

## 3. Las cuatro causas reales

### Causa 1 — Hay bibliografía, pero casi no hay citas en el texto

La bibliografía existe y es buena: **~50 fuentes** (Sapag 2008 y 2018, Franco/Montoya/Gómez 2012 sobre ONUDI, Pérez 2008 sobre JICA, Fernández 1989 sobre ZOPP, Robles Ríos 2019 sobre marco lógico, Dominguez 2018, Coria 2008, ALNAP 2023…). Turnitin la filtró correctamente: **0 % de coincidencia ahí**.

El problema es lo contrario: en 53.000 palabras solo detecté **11 citas en el texto**, y casi todas son fuentes de datos (BCH, USDA, Banco Mundial, Datosmacro). De las ~50 obras de la bibliografía, **prácticamente ninguna se cita en el cuerpo**.

Por eso Turnitin marca el Marco Teórico completo: leyó los párrafos de ONUDI, JICA y ZOPP, los encontró casi idénticos a sus fuentes originales, y **no vio ninguna atribución que le permitiera descontarlos**.

### Causa 2 — Cuatro citas textuales usan `¨` (diéresis) en lugar de comillas

Esto es un error técnico con efecto directo y medible en el porcentaje. Turnitin excluye el "texto citado" (el filtro está activo en tu informe), pero **solo reconoce comillas de verdad**: `"…"` o `“…”`. El carácter `¨` (U+00A8, diéresis) **no es una comilla** y Turnitin lo ignora.

Resultado: estas cuatro citas, que tú marcaste como textuales, **están contando el 100 % dentro del 23 %**:

| Párrafo | Texto | Problema |
|---|---|---|
| 234 | `Según ¨El estudio de prefactibilidad es un análisis en la etapa preliminar…¨ (p 1)` | `¨` en vez de comillas **y falta el autor** |
| 243 | `Según la Guía de transferencia Metodológica ¨La ONUDI promueve…¨` | `¨` en vez de comillas, falta año y página |
| 250 | `Según ABECÉ (2018), … son un ¨Conjunto de investigaciones teóricas…¨ (p 1)` | `¨` en vez de comillas |
| 254 | `Según Dominguez (2018), el estudio técnico¨ Determina la factibilidad…”` | abre con `¨` y **cierra con `”`** → par desbalanceado |

Además el documento tiene **14 comillas de apertura `“` frente a 16 de cierre `”`**: hay pares mal cerrados en otros sitios, y una comilla sin pareja anula la exclusión de todo el fragmento.

### Causa 3 — Citas sin autor

Tres pasajes llevan número de página pero **no dicen de quién es la cita**:

- Párrafo 234: `Según ¨…¨ (p 1)` → falta el autor (por la bibliografía, es **Lifeder, 2023**)
- Párrafo 274: `Según  “El análisis financiero es fundamental…” (p 1)` → falta el autor (es **Nava Rosillón**)
- Párrafo 276: `Según El análisis de los riesgos es esencial…” (p 38)` → falta el autor, y **no abre comilla** (es **Córdoba Restrepo y Agredo Leiva, 2018**)

Esto no solo infla el porcentaje: en una defensa, una cita textual sin autor es una observación grave por sí sola.

### Causa 4 — El Diseño Metodológico define conceptos en lugar de explicar tu método

Aquí está el hallazgo más accionable. Compara dos párrafos de tu propia tesis:

**Párrafo 421 (Tamaño del proyecto), pág. 47 — 85 % resaltado:**
> "Es la parte referida a establecer el nivel de inversiones necesarias para operar normalmente… Es necesario tener en cuenta las economías de escala que se podrían dar y el apalancamiento operativo, eligiendo el tamaño de planta que presente un mayor valor presente neto."

Eso es la **definición de Sapag**, palabra por palabra. No dice nada sobre tu proyecto.

**Párrafo 391 (Estudio sectorial) — apenas resaltado:**
> "El procedimiento de análisis se realizará mediante la aplicación del modelo PESTEL… y del modelo de las Cinco Fuerzas de Porter… Las respuestas obtenidas en las entrevistas se categorizarán y se triangularán con la información documental."

Eso **sí** es diseño metodológico: dice qué vas a hacer tú, con qué instrumento y cómo lo vas a analizar. Y Turnitin no lo marca.

Los párrafos **414, 416, 421, 423 y 429–453** están en el primer grupo: son definiciones de manual, algunas **repetidas** de lo que ya dijiste en el Marco Teórico (localización y tamaño aparecen dos veces en la tesis, págs. 23–25 y 46–47). Los párrafos **446–451** son un resumen del Código del Trabajo de Honduras copiado de fuentes web, dentro de un capítulo de metodología donde no corresponde.

### Causa 5 (menor) — Interpretación de indicadores calcada y triplicada

En las páginas 178–201 el problema es distinto: frases interpretativas que aparecen en cientos de tesis.

> "Al ser un VAN positivo, se concluye que los flujos futuros descontados exceden los costos de inversión" (párrafo 1343)
> "La Tasa Interna de Retorno alcanzó un valor de 63.62 %, resultado significativamente superior a la tasa mínima de rendimiento requerida del 15 %" (párrafo 1345)

Coinciden con tesis de IPChile, Uniminuto, Universidad Andrés Bello, Instituto IACC… porque es el molde estándar. Y encima está **triplicado dentro de tu propia tesis**: los párrafos 1300–1322 (sin financiamiento), 1335–1351 (con financiamiento) y 1374–1378 (resumen) repiten la misma estructura y las mismas frases con distintos números.

---

## 4. Plan de acción priorizado

Ordenado por puntos recuperados frente a esfuerzo. Con los tres primeros bloques deberías quedar entre **14 % y 16 %**.

### Prioridad 1 — Comprimir las metodologías comparadas · **–3,5 a –4,5 puntos**
**Páginas 22–38 (párrafos 242–358).** Son 17 páginas describiendo ONUDI, marco lógico EML/LFA, JICA, ZOPP, criterios CAD-OCDE y el ciclo de Sapag. Pero **la tesis usa ONUDI** (párrafos 386, 393, 462 lo dicen explícitamente): las demás metodologías son un desvío descriptivo que no alimenta ninguna decisión posterior.

Qué hacer:
1. Mantén ONUDI desarrollada, reescrita con tus palabras y **citando a Franco, Montoya y Gómez (2012)** y a la guía de ONUDI.
2. Sustituye JICA, ZOPP, marco lógico y CAD-OCDE por **una tabla comparativa de elaboración propia** (columnas: metodología / origen / enfoque / fases / aplicabilidad a un proyecto agroindustrial privado) más **un párrafo tuyo** que justifique por qué eliges ONUDI y descartas las demás.
3. Esa justificación es exactamente lo que hoy falta y lo que un jurado pregunta: *¿por qué ONUDI y no marco lógico?*

Pasas de ~17 páginas mayormente resaltadas a ~3 páginas de análisis propio. Es la acción de mayor rendimiento de toda la lista, y **fortalece** el capítulo.

### Prioridad 2 — Reescribir el Diseño Metodológico en clave de "qué haré yo" · **–1,5 a –2 puntos**
**Párrafos 414, 416, 421, 423, 429–453 (págs. 45–49).** Toma como modelo tus propios párrafos 389–391, 393–395 y 467–469, que están bien hechos y sin marcas.

Para cada apartado, escribe: qué información necesitas → de qué fuente → con qué instrumento → cómo la analizarás → qué producto entrega.

Ejemplos concretos:
- **Tamaño del proyecto (421):** en vez de la definición de economías de escala, escribe que determinarás la capacidad instalada a partir de la demanda estimada en la encuesta a los 66 establecimientos HoReCa, evaluando 2–3 alternativas de capacidad y seleccionando la de mayor VAN.
- **Localización (423):** en vez de "es una decisión de largo plazo…", indica que aplicarás un método de factores ponderados sobre los municipios candidatos de la Región Centro, con los criterios y pesos que definas.
- **Estudio legal (429–453):** elimina el resumen del Código del Trabajo (446–451). En metodología basta con: revisarás la normativa aplicable (ARSA, SENASA, SAR, Código del Trabajo, Ley General del Ambiente) y construirás una matriz de cumplimiento requisito–norma–artículo–responsable. El contenido sustantivo ya está en el capítulo de desarrollo (págs. 135–146).
- Borra la duplicación: si tamaño y localización ya están definidos en el Marco Teórico, no los redefinas aquí.

### Prioridad 3 — Volver la interpretación financiera específica de tu proyecto · **–1,5 a –2 puntos**
**Párrafos 1301–1302, 1308–1322, 1336–1337, 1342–1351, 1374–1378 (págs. 183–201).**

1. **Elimina las definiciones repetidas.** Qué es el VAN, la TIR o el WACC va una sola vez en el Marco Teórico, con cita. En resultados no se vuelve a definir.
2. **Unifica las dos lecturas de indicadores en una sola tabla comparativa** (sin financiamiento / con financiamiento) y un análisis único del contraste. Hoy el mismo texto está tres veces.
3. **Interpreta lo tuyo, no el manual.** En lugar de "un VAN positivo indica que los beneficios superan la inversión", escribe algo que solo pueda decirse de este proyecto: que un VAN de L 15,4 millones sobre una inversión de L 1,78 millones implica un múltiplo poco habitual que conviene contrastar con la agresividad del supuesto de captura de mercado en el nicho HoReCa; que la TIR sube de 55,95 % a 63,62 % con deuda al 10,5 % frente a un costo de capital propio de 15 %, y qué significa ese apalancamiento en riesgo de cobertura; que el PRI de 3,67 años debe leerse junto al VaR de tu Monte Carlo.

Este bloque baja el porcentaje **y** mejora sustancialmente el capítulo: hoy describe, después analizará.

### Prioridad 4 — Arreglar comillas y citas · **–0,3 a –0,5 puntos, y cierra un riesgo grave**
1. Reemplaza los **7 caracteres `¨`** por comillas tipográficas `“ ”` (párrafos 234, 243, 250, 254).
2. Revisa el balance de comillas: hay 14 `“` y 16 `”`. Cada par debe abrir y cerrar.
3. Añade los autores faltantes: **234 → (Lifeder, 2023, p. 1)**; **274 → (Nava Rosillón, p. 1)**; **276 → (Córdoba Restrepo y Agredo Leiva, 2018, p. 38)**.
4. Verifica que las ~50 obras de la bibliografía tengan al menos una cita en el texto. Si alguna no se cita, o la citas donde corresponde, o sale de la bibliografía.
5. La bibliografía está en formato `Autor (año): Título` (estilo Citavi), **no APA 7**. Confirma con tu asesor si EAFIT exige APA; el cuerpo del documento sí usa estilos APA para tablas y figuras, así que hay inconsistencia.

### Prioridad 5 — Normativa y tablas técnicas · **–0,5 a –1 punto**
**Págs. 106–108** (parámetros de calidad de leche cruda: TRAM, células somáticas, densidad, métodos AOAC/ISO) y **pág. 136** (Constitución, art. 145; Ley General del Ambiente).

- Las tablas de norma **consérvalas**: son datos oficiales verificables y su valor está en ser exactos. Añade debajo la nota de fuente completa (`Nota. Tomado del Acuerdo 0632-ARSA-2023`) y cita la norma en el texto.
- El texto corrido de artículos legales sí conviene tratarlo: reduce la transcripción a la frase imprescindible entre comillas con su cita, y resume el resto en tus palabras explicando **qué implica para la planta** (qué permiso, qué plazo, qué costo). Eso es análisis; transcribir no lo es.

### Prioridad 6 — Consultar sobre los índices · **hasta –1,3 puntos, sin tocar el texto**
La página 2 (tabla de contenido) sale **51,7 % resaltada**, y los índices de tablas y figuras también aparecen marcados: las líneas de puntos con títulos genéricos coinciden con otras tesis. Son ~690 palabras (1,3 puntos) de material puramente estructural.

Pregunta a tu asesor (Elkin Gómez, que es quien tiene la vista de instructor) si puede reprocesar el informe **excluyendo la portada, la tabla de contenido y los índices**. Muchas instituciones lo hacen de forma estándar. No cuesta nada preguntar y puede valer más de un punto.

---

## 5. Estimación de resultado

| Acción | Puntos |
|---|---|
| Base actual | **23,0** |
| P1 · Comprimir metodologías comparadas | −4,0 |
| P2 · Reescribir Diseño Metodológico | −1,7 |
| P3 · Interpretación financiera específica | −1,7 |
| P4 · Comillas y citas | −0,4 |
| P5 · Normativa y tablas | −0,7 |
| **Subtotal** | **≈ 14,5** |
| P6 · Excluir índices (si lo autorizan) | −1,3 → **≈ 13,2** |

Los rangos son estimaciones sobre el conteo de palabras resaltadas; el porcentaje exacto depende de cómo quede la redacción final. Aun cumpliendo solo P1 y P4 quedarías cerca de 18,5 %, ya bajo el umbral, pero **no te quedes en el mínimo**: si el texto reescrito vuelve a coincidir con alguna fuente, un 19,5 % no da margen. Apunta a 15 %.

---

## 6. Lo que NO debes hacer

Digo esto en serio, porque circula mucho y sale muy caro:

- **No cambies palabras por sinónimos** para "romper" las coincidencias. Turnitin detecta paráfrasis y el informe incluye alertas de integridad; además el texto queda ilegible y el jurado lo nota antes que el software.
- **No conviertas texto en imágenes** ni uses capturas para que el detector no lo lea. Es ocultamiento deliberado y es la falta más grave del catálogo.
- **No uses caracteres ocultos, texto en blanco, letras de otro alfabeto ni espaciados raros.** Turnitin marca estas manipulaciones explícitamente y quedan registradas en el informe.
- **No borres contenido sustantivo solo para diluir el porcentaje.** Recortar el marco teórico funciona porque ese material es prescindible y su lugar lo ocupa un análisis mejor; no recortes tus resultados.

El camino correcto es el único que además te sirve: **atribuir lo que es de otros y escribir con tu voz lo que es tuyo.** Un 23 % con 11 citas en 53.000 palabras es, en el fondo, un problema de citación, no de plagio. Se arregla citando.

---

## 7. Orden de trabajo sugerido

1. **Hoy (1 h):** Prioridad 4 completa —comillas y autores—. Es mecánica y elimina el riesgo más visible en una defensa.
2. **Hoy (5 min):** escribe a tu asesor por la exclusión de índices (Prioridad 6). Corre en paralelo.
3. **Días 1–3:** Prioridad 1. Es el bloque grande. Redacta primero la tabla comparativa de metodologías y el párrafo de justificación; con eso sustituyes las 14 páginas de golpe.
4. **Día 4:** Prioridad 2, siguiendo el modelo de tus propios párrafos 389–391.
5. **Día 5:** Prioridad 3, unificando las dos secciones de indicadores.
6. **Día 6:** Prioridad 5 y revisión de que cada obra de la bibliografía esté citada en el texto.
7. **Antes de reentregar:** vuelve a pasar Turnitin. Si tu institución limita los reintentos, pide a tu asesor una revisión en modo borrador.

---

## Archivos de este análisis

| Archivo | Contenido |
|---|---|
| `INFORME_SIMILITUD.md` | Este informe |
| `ANEXO_PASAJES.md` | Los 188 pasajes coincidentes de ≥25 palabras, con página de la tesis, fuente Turnitin y número de párrafo del Word |
| `extraer.py` | Extrae los resaltados y el catálogo de 477 fuentes del PDF |
| `cuantificar.py` | Cuenta palabras coincidentes por página y por zona |
| `mapear.py` | Cruza cada coincidencia con el párrafo correspondiente del .docx |
| `anexo_pasajes.py` | Genera el anexo |
| `matches.json`, `por_pagina.json` | Datos crudos |

**Nota sobre los números:** mi conteo directo de palabras resaltadas da 27,4 % frente al 23 % de Turnitin, porque mi medición captura palabras que quedan parcialmente bajo el borde del resaltado y cuenta cifras de tablas. Las **cifras absolutas están sobreestimadas ~19 %**, pero la **distribución relativa entre secciones es fiable**, y es lo que guía el plan. Los puntos estimados en la sección 5 ya vienen ajustados por ese factor.
