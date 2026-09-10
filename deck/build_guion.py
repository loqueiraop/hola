"""
Genera "Guion exposicion - Contrato de franquicia.docx".

Registro: español hablado, claro y adulto. El auditorio es de maestría, con
experiencia profesional, pero no conoce esta figura. Por eso los términos se
explican en una frase llana y se sigue adelante: sin jerga jurídica, sin
nominalizaciones burocráticas y sin analogías infantiles.

El texto no reproduce lo escrito en las diapositivas: la diapositiva enuncia,
el guion explica. Los tiempos de la tabla se calculan del conteo real de
palabras, a 140 palabras por minuto.
"""

import os
import re
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

PPM = 140

INK = RGBColor(0x1B, 0x2A, 0x41)
GREY = RGBColor(0x70, 0x78, 0x86)
ACCENT = RGBColor(0xA8, 0x53, 0x2B)
SERIF, SANS = "Georgia", "Calibri"


# ------------------------------------------------------------------ contenido
BLOQUES = [
    dict(n=1, titulo="Portada",
         pantalla="Título y nombres del grupo.",
         texto=[
             "Buenas tardes. Voy a presentarles el trabajo del Grupo 1 sobre el "
             "**contrato de franquicia**.",

             "Cualquier empresa que quiere crecer se topa con el mismo límite: "
             "abrir puntos nuevos cuesta capital, y el propio alcanza "
             "hasta cierto punto. Las salidas de siempre son endeudarse o crecer "
             "más lento.",

             "La franquicia propone otra: crecer con el dinero de otros. Alguien "
             "más pone la inversión, abre el local y lo opera bajo tu marca.",

             "Suena cómodo, pero tiene un precio alto. Para que funcione hay que "
             "entregarle a un tercero lo único que de verdad vale en la empresa: "
             "el nombre y la forma de trabajar. Y después confiar en que lo va a "
             "hacer igual de bien en una ciudad donde uno no está para vigilarlo.",

             "El contrato de franquicia es lo que vuelve manejable ese riesgo. "
             "Ahí queda definido qué se entrega, qué se cobra, qué se controla y "
             "qué pasa cuando la relación se rompe. Eso es lo que voy a explicar "
             "en estos quince minutos.",
         ]),

    dict(n=2, titulo="Definición conceptual",
         pantalla="Dos tarjetas: «La franquicia» y «El contrato de franquicia». "
                  "Abajo, tres países.",
         texto=[
             "Cuando una empresa franquicia, no está vendiendo un producto. Está "
             "vendiendo la capacidad de repetir su negocio. Y eso son tres cosas "
             "distintas.",

             "La primera es la marca, y sirve para algo muy concreto: el local "
             "nuevo se ahorra los años que cuesta que la gente confíe en un "
             "nombre. El cliente entra sabiendo qué va a encontrar.",

             "La segunda es el **know-how**, y ahí está el valor de verdad. Es un "
             "término del inglés que significa saber hacer. Son todos los "
             "procedimientos que la empresa fue afinando con los años: cómo se "
             "prepara cada producto, cuánta gente hace falta en cada turno, qué "
             "se hace cuando un cliente reclama, cómo se maneja el inventario. "
             "Nada de eso se puede patentar, porque no es un "
             "invento: es experiencia, y buena parte se aprendió cometiendo "
             "errores caros. Al franquiciar, todo eso se pone por escrito en "
             "manuales y se le entrega a alguien de afuera. Por eso las cláusulas "
             "de confidencialidad son tan duras: el franquiciado lo puede usar, "
             "pero no lo puede contar, y esa obligación le sigue aunque el "
             "contrato termine.",

             "La tercera es la forma de operar: cómo se ve el local, cómo se "
             "atiende, cómo se hace la publicidad. No es un tema de gusto. Es lo "
             "que permite que dos locales con dueños distintos le resulten "
             "iguales al cliente.",

             "Esas tres cosas juntas son la franquicia.",

             "El contrato es otra cosa. Es el documento donde se fija cuánto se "
             "paga, por cuánto tiempo, en qué zona, qué puede hacer cada uno y "
             "qué pasa si alguien incumple. Y tiene dos rasgos que lo diferencian "
             "de un contrato comercial común.",

             "El primero: no se cumple y se termina. En una compraventa uno paga, "
             "el otro entrega y ahí queda. Acá la relación sigue viva todo el "
             "tiempo que dure: hay pagos cada mes, asistencia permanente y "
             "supervisión constante. Se parece más a un arriendo que a una "
             "compra.",

             "El segundo es menos evidente: el contrato está **incompleto a "
             "propósito**. Ninguno prevé que en ocho años la gente compre de otra "
             "manera, ni que aparezca una pandemia. Y no es que el abogado lo "
             "haya hecho mal. Un contrato que amarre todas las variables le "
             "impide a la red moverse cuando el mercado se mueve. Ese margen es "
             "lo que le da capacidad de adaptarse.",

             "Hay algo más: es un **contrato marco**. No regula una operación "
             "puntual, regula la entrada a una red. Por eso las obligaciones del "
             "franquiciado no salen solo del texto que firmó; también salen de "
             "operar el negocio todos los días según ese método.",

             "Y una advertencia que cambia según el país. **Argentina** metió la "
             "franquicia en su Código Civil y Comercial: si hay un pleito, hay "
             "artículos que aplicar. **Ecuador** la reconoce en su Código de "
             "Comercio. **Colombia y Perú** la tratan como contrato atípico, o "
             "sea que la ley no la menciona. No es ilegal, pero implica que lo "
             "que el contrato no diga, no lo dice nadie. Ahí el contrato no "
             "respalda el acuerdo: el contrato es el acuerdo.",
         ]),

    dict(n=3, titulo="¿Cómo se aplica?",
         pantalla="Cinco etapas numeradas. Abajo, «20 días» y «La fórmula del "
                  "modelo».",
         texto=[
             "El proceso tiene cinco etapas, y la primera no es firmar: es elegir.",

             "La marca no le da una franquicia a cualquiera que tenga el dinero. "
             "Evalúa candidatos, y los evalúa con cuidado, porque cada local que "
             "abre lleva su nombre en la fachada. Los estudios coinciden en que "
             "elegir bien al franquiciado es una de las decisiones que más "
             "determina si la red funciona. Y el riesgo no se reparte igual: si "
             "opera mal, él pierde su inversión, pero la marca pierde prestigio "
             "en todo el mercado.",

             "Cuando hay acuerdo, se firma. Y ahí no solo se pacta el precio: se "
             "pacta la salida. Cuánto dura, cómo se renueva, en qué casos se "
             "puede cortar antes y quién decide si hay conflicto. Todo eso se "
             "define al principio, mientras las dos partes se están llevando "
             "bien, justamente porque después no se van a poner de acuerdo.",

             "Después viene la etapa que nadie ve y que es de las más "
             "importantes: la capacitación. Se entrena al dueño y al personal, se "
             "entregan los manuales, se acompaña la apertura. Ahí es donde el "
             "know-how cambia de manos en la práctica.",

             "El local abre. Y el franquiciado lleva el negocio completo: "
             "contrata, paga la nómina, atiende, responde por el arriendo. Pero "
             "lo hace dentro de reglas que no escribió él.",

             "Y la marca no desaparece: vuelve. Visita el local, revisa que se "
             "cumplan los estándares, audita las instalaciones y en muchos "
             "contratos también los libros contables. Es una facultad incómoda, y "
             "está aceptada desde el primer día por una razón práctica: cuando "
             "algo sale mal, el cliente no culpa al dueño del local, culpa a la "
             "marca. El daño se reparte entre todos los que están en la red.",

             "Hay una obligación anterior a todo esto que para mí es lo más "
             "interesante de la figura. Antes de firmar, la marca tiene que "
             "entregarle al futuro franquiciado información real del negocio: "
             "quién es, cómo está el sector, cómo está armada la red. Y con "
             "**veinte días hábiles** de anticipación, para que decida con todo "
             "sobre la mesa. Existe porque la diferencia de información es "
             "enorme: el que ofrece conoce el negocio a fondo y el que invierte "
             "casi no lo conoce. Y si esa información se esconde o se maquilla, "
             "hay responsabilidad por daños **aunque el contrato no se llegue a "
             "firmar**.",
         ]),

    dict(n=4, titulo="Partes involucradas",
         pantalla="Dos tarjetas, franquiciador y franquiciado. Abajo, la franja "
                  "del subfranquiciante.",
         texto=[
             "Son dos partes, y los nombres se parecen tanto que se confunden. El "
             "**franquiciador**, que también se llama franquiciante u otorgante, "
             "es el dueño de la marca. El **franquiciado**, o franquiciatario, es "
             "el que abre y opera el local.",

             "Lo que aporta el dueño de la marca no se puede tocar: el nombre, el "
             "know-how, los manuales y la asistencia técnica. Y hay un requisito "
             "previo del que casi no se habla: para poder franquiciar tuvo que "
             "haber operado su propio negocio con éxito durante un buen tiempo. "
             "No se franquicia una idea, se franquicia un historial. Y ese "
             "historial define cuánto vale lo que ofrece.",

             "Cobra de dos maneras distintas, y no son lo mismo. El **canon de "
             "entrada** es un pago único, al principio, por el derecho a entrar a "
             "la red. Las **regalías** son pagos que se repiten cada mes, y "
             "normalmente se calculan como un porcentaje de las ventas.",

             "Y además del dinero recibe control. Puede exigir que el local se "
             "vea igual, que se opere igual, y que el know-how no salga de ahí.",

             "Del otro lado, el franquiciado pone el capital y la gestión: la "
             "inversión inicial, las regalías, el día a día, el equipo. Y pone "
             "algo que la marca no tiene: sabe cómo compra la gente de su ciudad.",

             "A cambio recibe el paquete completo y, sobre todo, protección. La "
             "más importante suele ser la **exclusividad territorial**: el "
             "compromiso de que la marca no va a abrir otro local en su zona y le "
             "va a dividir la clientela. Si no quedó escrito, no existe.",

             "Entre sus obligaciones está no competir con la red mientras el "
             "contrato esté vigente, que es la contracara de haberle recibido el "
             "método.",

             "Y acá está la confusión más común. Aunque siga manuales ajenos, "
             "aunque lo auditen, aunque no fije sus precios, el franquiciado **no "
             "es un empleado**. Es un empresario independiente. Su capital está "
             "en riesgo. Si el local quiebra, la pérdida es suya y no de la marca.",

             "De ahí sale el punto que más se pelea en tribunales: quién responde "
             "cuando un cliente sufre un daño dentro del local. En principio "
             "responde el franquiciado, incluso por lo que hagan sus empleados, "
             "precisamente porque es independiente. Que el aviso de la marca esté "
             "en la puerta no alcanza para responsabilizarla. Pero los tribunales "
             "españoles hicieron una excepción, sobre todo en clínicas de "
             "estética y odontología: si se prueba que el daño vino de una "
             "instrucción concreta de la marca, la marca responde. Tener manuales "
             "y supervisar no basta.",

             "Y cuando la red sale al exterior aparece un tercero. Alguien compra "
             "los derechos de un país entero y después otorga franquicias ahí "
             "adentro. No es el dueño de la marca, pero para esos locales hace de "
             "dueño.",
         ]),

    dict(n=5, titulo="Ventajas y desventajas",
         pantalla="Seis ventajas y seis desventajas.",
         texto=[
             "En la diapositiva están los doce puntos; no los voy a leer. "
             "Prefiero explicarles la lógica, porque las dos columnas son la "
             "misma decisión vista de los dos lados.",

             "Lo que se gana son dos cosas. Tiempo: uno no se pone a averiguar "
             "qué funciona, entra a algo que ya funciona. Por eso las franquicias "
             "sobreviven más que los negocios independientes parecidos; no es "
             "suerte, es que los errores caros ya los pagó otro. Y escala: la "
             "publicidad, la capacitación y las compras se hacen para toda la "
             "red. Un local solo negocia cien unidades, la red negocia cien mil. "
             "No les dan el mismo precio ni el mismo plazo de pago.",

             "Lo que se cede son también dos cosas. Se cede margen, y de una "
             "forma que hay que entender bien: las regalías se calculan sobre "
             "**las ventas, no sobre la ganancia**. Si un mes las ventas caen a "
             "la mitad, la regalía se paga igual. Un mes malo para el local no es "
             "un mes malo para la marca.",

             "Y se cede autonomía. El franquiciado puede ver una oportunidad real "
             "en su mercado y no poder aprovecharla porque el manual no lo "
             "permite. Y hay una exposición que no controla en absoluto: si la "
             "marca se golpea en su reputación, aunque sea por algo que pasó en "
             "otro país, su local pierde clientes esa semana. Pudo haber "
             "trabajado impecable y el costo lo asume igual.",

             "Ahí está el fondo del asunto: lo mismo que le da valor a la red es "
             "lo que le quita libertad al franquiciado. No hay uno sin el otro. "
             "El contrato solo define dónde se pone esa raya.",
         ]),

    dict(n=6, titulo="Síntesis e intro al video",
         pantalla="El invitado y las cifras de la operación.",
         texto=[
             "Para cerrar, tres conclusiones.",

             "**La primera:** esto funciona porque ninguna de las dos partes "
             "podría sola. Una tiene la marca, el método y la experiencia, pero "
             "no el capital ni el conocimiento de cada mercado. La otra tiene el "
             "capital y el mercado, pero no la marca ni el método. La franquicia "
             "existe para juntar esas dos mitades.",

             "**La segunda,** y es la que más me llamó la atención al investigar: "
             "qué tan expuestas están las partes depende del país. Donde la ley "
             "regula la figura hay un mínimo garantizado. Donde no, como en "
             "Colombia o Perú, ese mínimo lo pone el contrato o no lo pone nadie. "
             "Un contrato mal hecho ahí no es un problema de forma: es quedarse "
             "sin defensa.",

             "**La tercera:** la independencia entre las partes es real, pero "
             "tiene un límite. Se sostiene mientras la marca oriente. Cuando el "
             "control es tan fuerte que reemplaza la decisión del franquiciado, "
             "esa independencia deja de existir, y los tribunales ya lo "
             "reconocieron.",

             "Todo esto es teoría. Para contrastarlo con la práctica "
             "entrevistamos a **Gerardo Marcano**, vicepresidente "
             "de operaciones de Grupo David. Lleva veinte años ahí y hoy responde "
             "por más de cuarenta franquicias.",

             "Antes de verlo, dos términos que él usa. **Brand awareness** es qué "
             "tanto una marca es conocida y deseada en un mercado; para él eso "
             "define si se abre o no se abre un local. Y **tropicalización** es "
             "adaptar el producto a cada mercado: cambiar telas, colores y "
             "surtido según el clima y lo que la gente usa. Su ejemplo lo resume "
             "bien: mandar ropa de invierno a una ciudad cálida es perder la "
             "temporada entera.",

             "Presten atención sobre todo a una cosa: hasta dónde se puede "
             "adaptar el modelo sin romper el estándar. Lo que acá suena "
             "abstracto, para él es una decisión de todos los días.",

             "Con esto cierro. Gracias.",
         ]),
]

CIERRE = [
    "La entrevista deja clara una cosa que en el análisis apenas se asoma: lo que "
    "sostiene una franquicia no es el texto del contrato, sino el rigor con que "
    "se repite el estándar y el criterio con que se adapta a cada mercado. Con "
    "gusto respondo sus preguntas.",
]

PREGUNTAS = [
    ("¿Cuánto cuesta una franquicia?",
     "Depende del sector y de qué tan fuerte sea la marca, pero la estructura es "
     "siempre la misma: un canon de entrada, que es único y al principio, y "
     "regalías mensuales, que normalmente son un porcentaje de las ventas."),
    ("¿El franquiciado puede vender su local?",
     "Solo con autorización de la marca, y así queda escrito casi siempre. La "
     "marca quiere decidir quién entra a su red."),
    ("¿Quién responde si un cliente se lastima en el local?",
     "En principio el franquiciado, porque es un empresario independiente. Los "
     "tribunales españoles han responsabilizado a la marca cuando se prueba que "
     "el daño vino de una instrucción concreta que ella dio. Tener estándares de "
     "calidad no basta: hay que probar que esa orden causó el daño."),
    ("¿Cuánto margen tiene el franquiciado para cambiar cosas?",
     "Necesita autorización. Un laudo arbitral colombiano, el caso PANACA, dejó "
     "dicho que el franquiciado debe respetar la filosofía de la marca, pero que "
     "los manuales tampoco se pueden aplicar igual en cualquier ciudad."),
    ("¿En qué se diferencia de una licencia de marca?",
     "La licencia solo autoriza usar el nombre. La franquicia entrega además el "
     "know-how, los manuales, la capacitación y la asistencia continua, y viene "
     "con control sobre cómo se opera."),
    ("¿Por qué Colombia no lo ha regulado si es tan común?",
     "Hay una norma técnica voluntaria, la NTC 5813, y el Ministerio de Comercio "
     "redactó en 2021 un proyecto de decreto que incluía ese plazo de veinte "
     "días. Cuando hicimos la revisión seguía sin expedirse."),
    ("¿Cuánto dura normalmente el contrato?",
     "Lo fija el contrato y cambia según el sector, pero siempre trae cláusulas "
     "de renovación y de terminación. Es una relación de largo plazo, y por eso "
     "la salida se negocia desde el principio."),
]


# --------------------------------------------------------------------- helpers
def contar(txt):
    return len(re.findall(r"[\wáéíóúñÁÉÍÓÚÑ'-]+", txt.replace("**", "")))


def mmss(seg):
    return f"{int(seg) // 60}:{int(seg) % 60:02d}"


def rich(p, texto, *, size=12.5, color=INK, font=SANS):
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
    f"El video de la entrevista se proyecta después de los quince minutos, así que "
    f"estos quince son todos de exposición. La diapositiva 6 la cierra y deja el "
    f"video montado. Leído a ritmo normal, unas {PPM} palabras por minuto, el guion "
    f"completo da {mmss(total)}.")
r.font.size = Pt(11)
r.font.color.rgb = GREY

tabla = doc.add_table(rows=1, cols=3)
tabla.alignment = WD_TABLE_ALIGNMENT.LEFT
anchos = [Cm(2.4), Cm(8.6), Cm(2.4)]
for i, (h, w) in enumerate(zip(["Entra", "Diapositiva", "Dura"], anchos)):
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
    for i, (v, w) in enumerate(zip([mmss(b["inicio"]),
                                    f"{b['n']} · {b['titulo']}",
                                    mmss(b["seg"])], anchos)):
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
for i, (v, w) in enumerate(zip([mmss(total),
                                "Fin de la exposición · empieza el video",
                                ""], anchos)):
    c = fila.cells[i]
    c.width = w
    solo_borde_inferior(c)
    pp = c.paragraphs[0]
    pp.paragraph_format.space_after = Pt(0)
    rr = pp.add_run(v)
    rr.font.bold = i < 2
    rr.font.size = Pt(10)
    rr.font.name = SANS
    rr.font.color.rgb = ACCENT

p = doc.add_paragraph()
p.paragraph_format.space_before = Pt(12)
p.paragraph_format.space_after = Pt(3)
r = p.add_run("SI EL TIEMPO SE CORRE")
r.font.size = Pt(9)
r.font.bold = True
r.font.color.rgb = ACCENT

for t in ("Para recuperar unos 50 segundos: en la diapositiva 4, saltarse el "
          "párrafo de quién responde ante un daño al cliente.",
          "Para recuperar unos 20 segundos: en la diapositiva 5, saltarse el caso "
          "del golpe a la reputación de la marca.",
          "Si sobra tiempo: desarrollar los ejemplos de la diapositiva 5, que es "
          "donde la explicación se vuelve concreta."):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.2
    r = p.add_run(t)
    r.font.size = Pt(10.5)
    r.font.color.rgb = GREY

for b in BLOQUES:
    doc.add_page_break()

    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(1)
    r = p.add_run(f"{mmss(b['inicio'])} – {mmss(b['inicio'] + b['seg'])}")
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
        rich(doc.add_paragraph(), t)

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
