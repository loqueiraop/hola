# Lo que queda por corregir, y que hay que hacer dentro de Citavi

Estas incidencias **no se pueden arreglar editando el documento en Word**: el texto
que ves lo genera Citavi a partir de su base de datos. Si lo cambias a mano, al
actualizar las citas Word lo revierte. Hay que corregir la referencia en Citavi y
después actualizar los campos.

## 1. El apellido de Sapag está mal capturado — prioritario

En el párrafo 240 la cita sale así:

> …antes de proceder a un estudio de mayor precisión **(Chain 2008, 22)**.

Citavi tomó *"Nassir Sapag Chain"* y guardó **"Chain"** como apellido. Debería ser
`(Sapag Chain, 2008, p. 22)`. Corrige el campo Autor de esa referencia usando el
editor de nombres, para que quede `Sapag Chain, Nassir`.

Esta es la única cita a Sapag en toda la tesis, y conviene saberlo porque las
cuatro páginas con más coincidencia del documento son contenido suyo:

| Pág. | Coincidencia | Contenido | Párrafos |
|---|---|---|---|
| 24 | 90 % | Lista de factores legales (patentes, desahucios, aranceles, mutuales) | 257–268 |
| 23 | 88 % | Definición del estudio legal | 256 |
| 46 | 88 % | Ingeniería del proyecto | 414, 416 |
| 47 | 85 % | Tamaño y localización del proyecto | 421, 423 |

Ninguno de esos párrafos tiene cita. **Antes de añadirla, comprueba de dónde
tomaste cada pasaje**: si lo copiaste de otra tesis o de un blog que a su vez
resumía a Sapag, la cita correcta es esa fuente, no Sapag. Citar a Sapag un texto
que en realidad viene de otro sitio sería una atribución falsa.

## 2. Paréntesis de cierre mal formado

Párrafo 243, al final de la cita textual de la ONUDI:

> …en el sistema multilateral de comercio" (ORGANIZACION DE LAS NACIONES UNIDAS PARA EL DESARROLLO INDUSTRIAL 2019**.**

Termina en punto en lugar de cerrar el paréntesis. Está dentro del campo de Citavi.

## 3. Espacios dobles generados por Citavi

Quedan 4 en el documento y todos vienen de los campos, por eso no los toqué:

| Párrafo | Sale como | Debería |
|---|---|---|
| 240 | `(Chain 2008,  22)` | `(Sapag Chain, 2008, p. 22)` |
| 276 | `(2018),   "El análisis…` | un solo espacio |
| 303 | `personal superior   (Agencia…` | un solo espacio |
| 305 | `(Marcos  2020)` | `(Marcos, 2020)` |

## 4. Formato de autor irregular en 15 referencias

Cinco convenciones distintas conviviendo, todas por el mismo motivo: el nombre
está en el campo equivocado.

| Sale así | Debería |
|---|---|
| `(ANA JULIA SILVA OQUENDO 2020)` | `(Silva Oquendo, 2020)` |
| `(PEREZ, Ana María, et al. 2008)` | `(Pérez et al., 2008)` |
| `(Jorge Borjas Chávez 2013)` | `(Borjas Chávez, 2013)` |
| `(Guillermo Roa Rodriguez 2015)` | `(Roa Rodríguez, 2015)` |
| `(Federico Holmann 2001)` | `(Holmann, 2001)` |
| `(Luis 2024)` | apellido real del autor |
| `(Marcos  2020)` | `(Marcos, 2020)` |
| `(Cepeda 2025)` | `(Cepeda, 2025)` |
| `(Franco et al. 2012)` | `(Franco et al., 2012)` |
| `(Coria Daniel 2008)` | `(Coria, 2008)` |

## 5. Referencias duplicadas

`(Rios, 2019)` en el párrafo 278 y `(Juan Carlos Robles Ríos 2019)` en el 279 son
**la misma obra** con dos entradas. Usa la búsqueda de duplicados de Citavi y
fusiónalas.

## 6. Entradas de la bibliografía que no se citan en el texto

| Entrada | Qué hacer |
|---|---|
| `Nassir Sapag Chain (2018)` | citarla donde corresponda, o quitarla |
| `Libro_Estudio_Mercado (2021)` | el título es un nombre de archivo; corregir la referencia o eliminarla |
| `Vásquez, Gustavo (2012)` | citarla donde corresponda, o quitarla |

## 7. Estilo de la bibliografía

Hoy sale como `Autor (año): Título`, que no es APA 7. Si EAFIT exige APA, cámbialo
en el complemento de Citavi para Word: estilo de citación → *APA, 7th edition*.
Reformatea de golpe las 84 citas y las 57 entradas.

Ojo con el orden: **primero** corrige los campos de autor (puntos 1 y 4), **después**
cambia el estilo. Si lo haces al revés, APA seguirá generando `(Chain, 2008)`
porque para Citavi ese es el apellido.

## 8. Al terminar

1. Actualiza citas y bibliografía desde el complemento de Citavi.
2. **Actualiza la tabla de contenido y los índices.** Son texto manual, no campos
   automáticos, y ya están desfasados: el índice dice que la bibliografía está en
   la página 248 y en realidad empieza en la 250. Al corregir los niveles de título
   del apartado "Notas Legales" hay que reflejarlo también.
3. Revisa que no queden citas en blanco. En el párrafo 208 el texto dice
   *"De acuerdo con datos de (FAOSTAT 2022)"*, con el campo resuelto correctamente,
   pero conviene comprobar el resto.
