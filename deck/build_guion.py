"""
Genera "Guion exposicion - Contrato de franquicia.docx".

El texto no repite lo que está escrito en las diapositivas: la diapositiva dice
qué, el guion dice por qué y cómo, con ejemplos. Los tiempos de la tabla se
calculan a partir del conteo real de palabras, a 140 palabras por minuto.
"""

import os
import re
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

PPM = 140  # palabras por minuto a ritmo de exposición

INK = RGBColor(0x1B, 0x2A, 0x41)
GREY = RGBColor(0x70, 0x78, 0x86)
ACCENT = RGBColor(0xA8, 0x53, 0x2B)
SERIF, SANS = "Georgia", "Calibri"


# ------------------------------------------------------------------ contenido
BLOQUES = [
    dict(n=1, titulo="Portada", quien="Fredy",
         pantalla="Título y nombres del grupo.",
         texto=[
             "Buenas tardes. Somos el Grupo 1.",

             "Voy a empezar con una pregunta. ¿Cuántos de ustedes han almorzado "
             "en un local de una cadena conocida? Casi todos. Ahora, ¿de quién "
             "era ese local?",

             "Lo normal es pensar que es de la empresa dueña de la marca. Y casi "
             "nunca lo es. Es de un particular, alguien de la ciudad, que pagó "
             "para poder usar ese nombre y que firmó un contrato aceptando "
             "operar exactamente como la marca le diga.",

             "De eso vamos a hablar hoy: del **contrato de franquicia**. Es uno "
             "de los acuerdos comerciales más extendidos del mundo, está en la "
             "esquina de su casa, y aun así casi nadie sabe qué dice por dentro.",

             "En los próximos quince minutos van a saber qué se compra "
             "exactamente cuando alguien compra una franquicia, cuánto le cuesta, "
             "y qué es lo que entrega en el camino.",
         ]),

    dict(n=2, titulo="Definición conceptual", quien="Fredy",
         pantalla="Dos tarjetas: «La franquicia» y «El contrato de franquicia». "
                  "Abajo, tres países.",
         texto=[
             "Cuando una empresa franquicia, lo que vende no es un producto. "
             "Vende la posibilidad de repetir su negocio.",

             "Y eso se reparte en tres cosas muy distintas.",

             "La primera es la más visible: el letrero. Una marca que la gente ya "
             "reconoce le ahorra al negocio los tres o cuatro años que toma "
             "ganarse la confianza de un barrio. El cliente entra porque ya sabe "
             "qué va a encontrar.",

             "La segunda es el **know-how**, y aquí me quiero detener, porque es "
             "la que menos se entiende. Know-how viene del inglés y significa "
             "saber hacer. Piensen en un restaurante que lleva veinte años "
             "abierto. En esos veinte años aprendió a qué temperatura sale mejor "
             "cada plato, cuánta gente necesita un sábado, en qué orden armar los "
             "pedidos para que no se enfríen, qué hacer cuando un cliente "
             "reclama. Nada de eso está patentado, porque no es un invento: es "
             "experiencia, y se aprendió perdiendo dinero. Cuando la empresa "
             "franquicia, le entrega todo eso escrito, en manuales, a alguien que "
             "acaba de conocer. Por eso el contrato lo blinda: el franquiciado lo "
             "puede usar, pero no lo puede contar, ni durante ni después.",

             "La tercera es la forma de operar: cómo se ve el local, cómo se "
             "atiende, cómo se hace la publicidad. Y esto no es capricho "
             "estético. Es lo que permite que ustedes entren a un local en "
             "Bogotá y a otro en Quito, con dueños que no se conocen entre sí, y "
             "no noten la diferencia.",

             "Eso, todo junto, es una franquicia.",

             "Ahora, una cosa es el negocio y otra es el papel. El contrato es "
             "donde queda escrito cuánto se paga, por cuánto tiempo, en qué zona, "
             "qué puede hacer cada uno y qué pasa si alguien incumple.",

             "Y tiene dos cosas que suelen sorprender.",

             "La primera: no se parece a una compraventa. Cuando yo les compro "
             "algo, les doy plata, me dan la cosa y la relación se acaba. Aquí no "
             "se acaba: se sigue pagando cada mes, se sigue recibiendo "
             "asistencia, se sigue siendo supervisado. Se parece mucho más a un "
             "arriendo que a una compra.",

             "La segunda: está **incompleto a propósito**. Ningún contrato de "
             "franquicia dice qué hacer si en ocho años cambia la forma en que la "
             "gente compra, o si aparece una pandemia. Y no es descuido del "
             "abogado. Si el contrato amarrara todo, la red no podría moverse "
             "cuando el mercado se mueve. Ese margen es lo que la mantiene viva.",

             "Y una advertencia que cambia según dónde estén parados. En "
             "**Argentina**, si mañana hay un pleito por una franquicia, hay "
             "artículos concretos en el Código Civil y Comercial a los que "
             "acudir. En **Ecuador** el Código de Comercio también la reconoce. "
             "En **Colombia y Perú**, no: la ley no menciona esta figura. No es "
             "ilegal, pero significa que si el contrato no dice algo, "
             "probablemente nadie lo dice. Ahí el contrato no es un respaldo del "
             "acuerdo: es todo el acuerdo.",
         ]),

    dict(n=3, titulo="¿Cómo se aplica?", quien="María Isabel",
         pantalla="Cinco etapas numeradas. Abajo, «20 días» y «La fórmula del "
                  "modelo».",
         texto=[
             "Veamos cómo pasa esto en la vida real.",

             "No arranca con una firma; arranca con una entrevista. La marca "
             "escoge, y escoge con desconfianza, porque cada local que se abre "
             "lleva su nombre en la fachada. Tener el dinero no alcanza. Hay "
             "estudios que muestran que escoger bien al franquiciado es una de "
             "las decisiones que más determina si toda la red funciona. Y la "
             "lógica es dura: si el franquiciado lo hace mal, él pierde su "
             "inversión, pero la marca pierde reputación en toda la ciudad.",

             "Cuando hay acuerdo, se firma. Y en ese momento no solo se fija el "
             "precio: se fija cómo se sale. Cuánto dura, cómo se renueva, en qué "
             "casos se puede terminar antes y quién decide si hay pelea. Todo eso "
             "se define al principio, cuando las dos partes están contentas, "
             "precisamente porque después ya no se van a poner de acuerdo.",

             "Después viene la parte que nadie ve: la capacitación. Semanas de "
             "entrenamiento para el dueño y para los empleados, manuales, "
             "acompañamiento en la apertura. Aquí es donde el know-how cambia de "
             "manos de verdad.",

             "Abre el local. Y a partir de ahí el franquiciado hace casi todo: "
             "contrata, paga la nómina, atiende, responde por el arriendo. Pero "
             "lo hace dentro de reglas que no escribió él.",

             "Y la marca no se va: vuelve. Visita el local, revisa que los "
             "estándares se cumplan, audita las instalaciones y en muchos "
             "contratos también los libros contables. Suena invasivo, y lo es, "
             "pero está aceptado desde el primer día. La razón es práctica: "
             "cuando un cliente tiene una mala experiencia, no dice «qué mal ese "
             "dueño». Dice «qué mal esa marca». El daño se reparte entre todos.",

             "Y hay una regla previa a todo esto que a nosotros nos pareció lo "
             "más interesante de la figura. Antes de firmar, la marca está "
             "obligada a entregarle a quien va a invertir información real del "
             "negocio: quién es, cómo está el sector, cómo funciona la red. Y con "
             "**veinte días hábiles** de anticipación, para que la persona lo lea "
             "con calma y no firme en caliente. Existe porque el desequilibrio es "
             "brutal: quien vende conoce el negocio a fondo, y quien compra está "
             "apostando a ciegas. Y si esa información se oculta o se maquilla, "
             "hay responsabilidad legal **aunque el contrato nunca se llegue a "
             "firmar**.",
         ]),

    dict(n=4, titulo="Partes involucradas", quien="Xilena",
         pantalla="Dos tarjetas, franquiciador y franquiciado. Abajo, la franja "
                  "del subfranquiciante.",
         texto=[
             "Aquí hay dos, y sus nombres se parecen tanto que se confunden. El "
             "**franquiciador** es el dueño de la marca. El **franquiciado** es "
             "quien abre el local. Si se pierden, piensen en el dueño de la "
             "receta y el dueño del local.",

             "El dueño de la marca pone lo que no se puede tocar: el nombre, el "
             "saber hacer, los manuales, la asistencia. Y antes de poder "
             "franquiciar tuvo que hacer funcionar su propio negocio durante "
             "años. Nadie franquicia una idea; se franquicia un historial. Ese "
             "historial es exactamente lo que está vendiendo.",

             "A cambio recibe plata por dos vías distintas, y no son lo mismo. "
             "Una es el **canon de entrada**: un pago único, al "
             "principio, por el derecho a entrar. Piénsenlo como el derecho de "
             "admisión. La otra son las **regalías**: un pago que se repite todos "
             "los meses, casi siempre calculado como un porcentaje de las ventas.",

             "Y recibe algo más que plata: recibe control. Puede exigir que el "
             "local se vea igual, que se opere igual, y que el know-how no salga "
             "de ahí.",

             "Del otro lado, el franquiciado pone el dinero y el cuerpo. La "
             "inversión inicial, las regalías, el día a día del negocio, la "
             "gente. Y pone algo que la marca no tiene y necesita: sabe cómo "
             "compra la gente de su ciudad.",

             "Lo que recibe, además del paquete, es protección. La más valiosa "
             "suele ser la **exclusividad territorial**: el compromiso de que la "
             "marca no le va a abrir otro local a tres cuadras y le va a partir "
             "la clientela. Si eso no quedó escrito, no existe.",

             "Y aquí está la confusión más común. Aunque siga manuales ajenos, "
             "aunque lo auditen, aunque no pueda cambiar los precios, el "
             "franquiciado **no es un empleado**. Es un empresario. Su plata está "
             "en juego. Si el local quiebra, la pérdida es suya, no de la marca.",

             "Eso lleva a una pregunta que en la práctica se pelea mucho: si un "
             "cliente sufre un daño dentro del local, ¿a quién demanda? Por regla "
             "general responde el franquiciado, incluso por lo que hagan sus "
             "empleados, justamente porque es independiente. El letrero de la "
             "marca en la puerta no basta para responsabilizar a la marca. Pero "
             "los tribunales españoles han hecho una excepción, sobre todo en "
             "clínicas estéticas y odontológicas: cuando se prueba que el daño "
             "vino de una instrucción concreta que impuso la marca, la marca "
             "responde. Tener manuales y supervisar no alcanza; hay que probar "
             "que la orden causó el daño.",

             "Y cuando esto cruza fronteras aparece un tercero. Alguien compra "
             "los derechos de un país entero y después reparte franquicias ahí "
             "dentro. No es el dueño de la marca, pero para los locales de ese "
             "país se comporta como si lo fuera.",
         ]),

    dict(n=5, titulo="Ventajas y desventajas", quien="María Daniela",
         pantalla="Seis ventajas y seis desventajas.",
         texto=[
             "En la diapositiva están los doce puntos; no los voy a leer. "
             "Prefiero contarles la lógica, porque las dos listas son en realidad "
             "la misma decisión.",

             "Lo que se gana se resume en dos cosas. Se gana tiempo: uno no "
             "empieza a averiguar qué funciona, entra a algo que ya funciona. Por "
             "eso las franquicias sobreviven más que los negocios "
             "independientes; no es suerte, es que los errores caros ya los pagó "
             "otro. Y se gana escala: la publicidad, la capacitación y las "
             "compras se hacen para toda la red. Un local independiente le pide "
             "al proveedor cien unidades; la red le pide cien mil. No les cobran "
             "el mismo precio ni les dan el mismo plazo para pagar.",

             "Lo que se pierde también son dos cosas. Se pierde plata, y de una "
             "forma que a veces no se ve venir: las regalías son un porcentaje de "
             "**las ventas, no de las ganancias**. Si un mes las ventas caen a la "
             "mitad, la regalía se paga igual. Un mes malo para el negocio no es "
             "un mes malo para la marca.",

             "Y se pierde libertad. El franquiciado puede tener una idea "
             "excelente para su ciudad y no poder aplicarla porque el manual no "
             "lo permite. Hay algo peor todavía, que no depende de él en "
             "absoluto: si la marca queda mal en las noticias, aunque sea por "
             "algo que pasó en otro país, su local pierde clientes esa semana. "
             "Trabajó impecable y de todas formas paga la cuenta.",

             "Y ahí está el nudo del asunto: lo mismo que hace valiosa a la red "
             "es lo que le quita libertad al franquiciado. No se puede tener uno "
             "sin el otro. El contrato solo decide en qué punto se traza esa raya.",
         ]),

    dict(n=6, titulo="Síntesis e intro al video", quien="María Daniela",
         pantalla="El invitado y las cifras de la operación.",
         texto=[
             "Para cerrar, tres cosas que nos quedaron claras.",

             "**La primera:** esto funciona porque ninguno de los dos podría "
             "solo. Uno tiene la marca y la experiencia, pero no el capital ni el "
             "conocimiento de cada ciudad. El otro tiene el capital y la ciudad, "
             "pero no la marca ni el método. La franquicia existe para juntar "
             "esas dos mitades.",

             "**La segunda,** y es la que más nos sorprendió: qué tan expuestas "
             "están las partes depende del país. Donde la ley regula la figura "
             "hay un piso mínimo de protección. Donde no, como en Colombia o en "
             "Perú, ese piso lo pone el contrato o no lo pone nadie. Un contrato "
             "mal redactado ahí no es un problema de forma: es quedarse sin "
             "defensa.",

             "**Y la tercera:** la independencia entre las partes es cierta, pero "
             "tiene un límite. Se sostiene mientras la marca oriente. Cuando la "
             "marca controla tanto que decide por el otro, esa independencia deja "
             "de ser real, y los tribunales lo han reconocido.",

             "Ahora, todo esto es teoría. Quisimos oírlo de alguien que lo hace "
             "todos los días, así que entrevistamos a **Gerardo Marcano**, "
             "vicepresidente de operaciones de Grupo David. Lleva veinte años ahí "
             "y hoy responde por más de cuarenta franquicias.",

             "Antes de verlo, dos términos suyos, para que no los pierdan. Va a "
             "decir **brand awareness**: es cuánta gente conoce y quiere una "
             "marca en una ciudad determinada. Para él, eso decide si se abre o "
             "no se abre. Y va a decir **tropicalización**: es adaptar el "
             "producto a cada mercado, cambiar telas, colores y surtido según el "
             "clima y el gusto local. Su ejemplo lo dice todo: mandar ropa de "
             "invierno a una ciudad caliente es perder la temporada.",

             "Escuchen sobre todo una cosa: hasta dónde se puede adaptar el "
             "modelo sin romperlo. Eso, que aquí sonaba abstracto, para él es una "
             "decisión de todos los días.",

             "Con esto cerramos. Gracias.",
         ]),
]

CIERRE = [
    "Como vieron, lo que sostiene una franquicia no es el texto del contrato, "
    "sino la disciplina con que se repite el estándar y la maña con que se adapta "
    "a cada mercado. Quedamos atentos a sus preguntas.",
]

PREGUNTAS = [
    ("¿Cuánto cuesta una franquicia?",
     "Depende del sector y de la marca, pero siempre son dos pagos distintos: el "
     "canon de entrada, que es único y al inicio, y las regalías, que son "
     "mensuales y normalmente un porcentaje de las ventas."),
    ("¿El franquiciado puede vender su local?",
     "Solo con autorización de la marca, y así suele quedar escrito. La marca "
     "quiere decidir quién entra a su red."),
    ("¿Quién responde si un cliente sufre un daño en el local?",
     "Por regla general el franquiciado, porque es un empresario independiente. "
     "Los tribunales españoles han responsabilizado a la marca cuando se prueba "
     "que el daño vino de una instrucción concreta que ella impuso. Tener "
     "estándares de calidad no basta: hay que probar ese nexo."),
    ("¿Y si el franquiciado quiere cambiar algo del negocio?",
     "Necesita autorización. Un laudo arbitral colombiano, el caso PANACA, "
     "precisó que el franquiciado debe seguir la filosofía de la marca, pero que "
     "los manuales tampoco pueden aplicarse igual en cualquier ciudad."),
    ("¿En qué se diferencia de una licencia de marca?",
     "La licencia autoriza solo el uso del nombre. La franquicia transfiere "
     "además el know-how, los manuales, la capacitación y la asistencia "
     "continua, y viene con control sobre cómo se opera."),
    ("¿Por qué en Colombia no está regulado si es tan común?",
     "Hay una norma técnica voluntaria, la NTC 5813, y el Ministerio de Comercio "
     "redactó un proyecto de decreto en 2021 que preveía ese plazo de veinte "
     "días. Cuando hicimos esta revisión, el proyecto seguía sin expedirse."),
    ("¿Cuánto durará el contrato normalmente?",
     "Lo fija el contrato y varía mucho, pero siempre trae cláusulas de "
     "renovación y de terminación. Es una relación de larga duración: por eso se "
     "negocia la salida desde el principio."),
]


# --------------------------------------------------------------------- helpers
def contar(txt):
    return len(re.findall(r"[\wáéíóúñÁÉÍÓÚÑ'-]+", txt.replace("**", "")))


def mmss(seg):
    return f"{int(seg) // 60}:{int(seg) % 60:02d}"


def rich(p, texto, *, size=12.5, color=INK, font=SANS):
    """Escribe el texto interpretando **negrita**."""
    for i, tramo in enumerate(texto.split("**")):
        if not tramo:
            continue
        r = p.add_run(tramo)
        r.bold = (i % 2 == 1)
        r.font.size = Pt(size)
        r.font.name = font
        r.font.color.rgb = color
    return p


def sombrear(celda, hexcolor):
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:fill"), hexcolor)
    celda._tc.get_or_add_tcPr().append(shd)


def solo_borde_inferior(celda, hexcolor="D8D4CC"):
    tcPr = celda._tc.get_or_add_tcPr()
    borders = OxmlElement("w:tcBorders")
    for lado in ("top", "left", "right"):
        e = OxmlElement(f"w:{lado}")
        e.set(qn("w:val"), "nil")
        borders.append(e)
    b = OxmlElement("w:bottom")
    b.set(qn("w:val"), "single")
    b.set(qn("w:sz"), "4")
    b.set(qn("w:color"), hexcolor)
    borders.append(b)
    tcPr.append(borders)


# ----------------------------------------------------------------- documento
doc = Document()

sec = doc.sections[0]
sec.top_margin = sec.bottom_margin = Cm(2.0)
sec.left_margin = sec.right_margin = Cm(2.3)

est = doc.styles["Normal"]
est.font.name = SANS
est.font.size = Pt(12.5)
est.font.color.rgb = INK
pf = est.paragraph_format
pf.line_spacing = 1.4
pf.space_after = Pt(9)

# --- cabecera
p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(2)
r = p.add_run("CONTRATO DE FRANQUICIA")
r.font.name = SANS
r.font.size = Pt(9.5)
r.font.bold = True
r.font.color.rgb = ACCENT

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(4)
r = p.add_run("Guion de exposición")
r.font.name = SERIF
r.font.size = Pt(26)
r.font.color.rgb = INK

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(16)
r = p.add_run("15 minutos · Acuerdos Comerciales · Grupo 1 · María Isabel Acosta, "
              "Xilena Blanco, María Daniela García, Fredy Valladares")
r.font.size = Pt(10.5)
r.font.color.rgb = GREY

# --- cálculo de tiempos a partir del texto real
for b in BLOQUES:
    b["palabras"] = sum(contar(t) for t in b["texto"])
    b["seg"] = b["palabras"] / PPM * 60
acumulado = 0.0
for b in BLOQUES:
    b["inicio"] = acumulado
    acumulado += b["seg"]
total = acumulado

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(8)
r = p.add_run(
    f"El video de la entrevista va después de los quince minutos, así que estos "
    f"quince son de exposición hablada. La diapositiva 6 cierra la exposición y "
    f"deja el video montado. Leído a ritmo normal, unas {PPM} palabras por "
    f"minuto, el guion completo da {mmss(total)}.")
r.font.size = Pt(11)
r.font.color.rgb = GREY

# --- tabla de tiempos
tabla = doc.add_table(rows=1, cols=4)
tabla.alignment = WD_TABLE_ALIGNMENT.LEFT
anchos = [Cm(2.0), Cm(6.6), Cm(2.2), Cm(4.2)]
encabezados = ["Entra", "Diapositiva", "Dura", "Expone"]
for i, (h, w) in enumerate(zip(encabezados, anchos)):
    c = tabla.rows[0].cells[i]
    c.width = w
    sombrear(c, "1B2A41")
    pp = c.paragraphs[0]
    pp.paragraph_format.space_after = Pt(0)
    rr = pp.add_run(h.upper())
    rr.font.size = Pt(8.5)
    rr.font.bold = True
    rr.font.name = SANS
    rr.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

for b in BLOQUES:
    fila = tabla.add_row()
    valores = [mmss(b["inicio"]), f"{b['n']} · {b['titulo']}", mmss(b["seg"]),
               b["quien"]]
    for i, (v, w) in enumerate(zip(valores, anchos)):
        c = fila.cells[i]
        c.width = w
        solo_borde_inferior(c)
        pp = c.paragraphs[0]
        pp.paragraph_format.space_after = Pt(0)
        rr = pp.add_run(v)
        rr.font.size = Pt(10)
        rr.font.name = SANS
        rr.font.color.rgb = INK

fila = tabla.add_row()
for i, w in enumerate(anchos):
    c = fila.cells[i]
    c.width = w
    solo_borde_inferior(c)
    pp = c.paragraphs[0]
    pp.paragraph_format.space_after = Pt(0)
    if i == 0:
        rr = pp.add_run(mmss(total))
        rr.font.bold = True
    elif i == 1:
        rr = pp.add_run("Fin de la exposición · empieza el video")
        rr.font.bold = True
    else:
        rr = pp.add_run("")
    rr.font.size = Pt(10)
    rr.font.name = SANS
    rr.font.color.rgb = ACCENT if i < 2 else INK

# --- válvulas de escape
p = doc.add_paragraph()
p.paragraph_format.space_before = Pt(12)
p.paragraph_format.space_after = Pt(3)
r = p.add_run("SI EL TIEMPO SE CORRE")
r.font.size = Pt(9)
r.font.bold = True
r.font.color.rgb = ACCENT

for t in ("Para ganar unos 45 segundos: en la diapositiva 4, omitir el párrafo "
          "de a quién demanda el cliente.",
          "Para ganar unos 20 segundos: en la diapositiva 5, saltarse el ejemplo "
          "de la marca en las noticias.",
          "Si sobra tiempo: en la 5, desarrollar más los ejemplos. Es lo que el "
          "público agradece."):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.2
    r = p.add_run(t)
    r.font.size = Pt(10.5)
    r.font.color.rgb = GREY

# --- bloques
for b in BLOQUES:
    doc.add_page_break()

    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(1)
    r = p.add_run(f"{mmss(b['inicio'])} – {mmss(b['inicio'] + b['seg'])}"
                  f"   ·   {b['quien'].upper()}")
    r.font.size = Pt(9.5)
    r.font.bold = True
    r.font.color.rgb = ACCENT

    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(3)
    r = p.add_run(f"{b['n']} · {b['titulo']}")
    r.font.name = SERIF
    r.font.size = Pt(20)
    r.font.color.rgb = INK

    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(14)
    r = p.add_run(f"En pantalla: {b['pantalla']}")
    r.font.size = Pt(10)
    r.font.italic = True
    r.font.color.rgb = GREY

    for t in b["texto"]:
        p = doc.add_paragraph()
        rich(p, t)

# --- después del video
doc.add_page_break()
p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(1)
r = p.add_run("DESPUÉS DEL VIDEO")
r.font.size = Pt(9.5)
r.font.bold = True
r.font.color.rgb = ACCENT

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(3)
r = p.add_run("7 · Gracias y preguntas")
r.font.name = SERIF
r.font.size = Pt(20)
r.font.color.rgb = INK

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(14)
r = p.add_run("En pantalla: «Gracias».")
r.font.size = Pt(10)
r.font.italic = True
r.font.color.rgb = GREY

for t in CIERRE:
    rich(doc.add_paragraph(), t)

p = doc.add_paragraph()
p.paragraph_format.space_before = Pt(18)
p.paragraph_format.space_after = Pt(10)
r = p.add_run("Si preguntan")
r.font.name = SERIF
r.font.size = Pt(16)
r.font.color.rgb = INK

for q, a in PREGUNTAS:
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.line_spacing = 1.25
    r = p.add_run(q)
    r.font.size = Pt(11.5)
    r.font.bold = True
    r.font.color.rgb = INK

    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(11)
    p.paragraph_format.line_spacing = 1.3
    r = p.add_run(a)
    r.font.size = Pt(11.5)
    r.font.color.rgb = RGBColor(0x44, 0x55, 0x70)


out = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "Guion exposicion - Contrato de franquicia.docx")
doc.save(out)

print(f"{'Bloque':38s} {'pal':>5s} {'dura':>7s}  {'entra':>7s}")
for b in BLOQUES:
    print(f"{str(b['n']) + ' · ' + b['titulo'][:32]:38s} {b['palabras']:5d} "
          f"{mmss(b['seg']):>7s}  {mmss(b['inicio']):>7s}")
print(f"\nEXPOSICION: {mmss(total)}  (margen {900 - total:+.0f} s)")
print(out)
