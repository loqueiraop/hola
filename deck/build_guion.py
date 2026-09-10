"""
Genera "Guion exposicion - Contrato de franquicia.docx".

Registro: profesional. El auditorio tiene experiencia en gestión y negocios,
pero no conoce esta figura en particular, así que los términos técnicos se
definen sin didactismo ni tono coloquial.

El texto no reproduce lo escrito en las diapositivas: la diapositiva enuncia,
el guion explica y ejemplifica. Los tiempos de la tabla se calculan a partir del
conteo real de palabras, a 140 palabras por minuto.
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
    dict(n=1, titulo="Portada",
         pantalla="Título y nombres del grupo.",
         texto=[
             "Buenas tardes. Voy a presentarles el trabajo del Grupo 1 sobre el "
             "**contrato de franquicia**.",

             "Toda empresa que decide crecer enfrenta la misma restricción: la "
             "expansión exige capital, y el capital propio siempre es limitado. "
             "Las respuestas habituales son endeudarse o abrir más despacio. La "
             "franquicia plantea una tercera: expandirse con capital de terceros, "
             "cediéndoles el derecho a operar bajo la marca.",

             "El costo de esa decisión es alto. Obliga a entregarle a un tercero "
             "lo único que sostiene el valor de la empresa —su nombre y su forma "
             "de trabajar— y a confiar en que lo ejecutará con el mismo estándar, "
             "en un mercado donde el titular no está presente.",

             "El contrato de franquicia es el instrumento que vuelve "
             "administrable esa decisión. Define qué se transfiere, qué se cobra, "
             "qué se controla y qué ocurre cuando la relación se rompe. En los "
             "próximos quince minutos voy a recorrer cómo está construido, "
             "quiénes intervienen y qué asume cada parte.",
         ]),

    dict(n=2, titulo="Definición conceptual",
         pantalla="Dos tarjetas: «La franquicia» y «El contrato de franquicia». "
                  "Abajo, tres países.",
         texto=[
             "Franquiciar no consiste en vender un producto, sino en transferir "
             "la capacidad de reproducir un negocio. Esa capacidad se compone de "
             "tres elementos, con naturaleza y valor distintos.",

             "El primero es la marca. Su función económica es concreta: le ahorra "
             "al nuevo establecimiento los años que toma construir reputación en "
             "un mercado. El cliente ingresa con una expectativa ya formada.",

             "El segundo es el **know-how**, y es donde reside el valor real del "
             "sistema. Se traduce como saber hacer, y comprende el conjunto de "
             "procedimientos que la empresa depuró a lo largo de años de "
             "operación: estándares de producción, dimensionamiento del personal, "
             "protocolos de atención, manejo de reclamos, control de inventarios. "
             "No es materia patentable, porque no se trata de una invención sino "
             "de experiencia acumulada, buena parte de ella a costa de errores "
             "costosos. Al franquiciar, todo eso se documenta en manuales y se "
             "entrega a un tercero. De ahí la severidad de las cláusulas de "
             "confidencialidad: el franquiciado queda autorizado a utilizarlo, no "
             "a divulgarlo, y esa obligación sobrevive a la terminación del "
             "contrato.",

             "El tercero es el sistema de operación: configuración del local, "
             "protocolo de servicio, política publicitaria. No responde a un "
             "criterio estético, sino a la necesidad de que dos establecimientos "
             "con propietarios distintos resulten indistinguibles para el "
             "consumidor.",

             "Esos tres elementos, en conjunto, constituyen la franquicia.",

             "El contrato opera en otro plano. Es el instrumento donde se fijan "
             "la contraprestación, el plazo, el territorio, las facultades de "
             "cada parte y las consecuencias del incumplimiento. Presenta dos "
             "particularidades que lo separan de la contratación mercantil "
             "ordinaria.",

             "La primera: no se agota en un acto. En una compraventa hay una "
             "prestación, una contraprestación y la relación se extingue. Aquí la "
             "relación permanece activa durante toda la vigencia, con pagos "
             "periódicos, asistencia continua y supervisión permanente. Su "
             "estructura se aproxima más al arrendamiento que a la compraventa.",

             "La segunda es menos evidente: el contrato es **deliberadamente "
             "incompleto**. Ninguno anticipa un cambio en los hábitos de consumo "
             "a ocho años, ni una contingencia sanitaria. Y no se trata de una "
             "deficiencia de redacción. Un contrato que cerrara todas las "
             "variables impediría a la red ajustarse cuando el mercado se "
             "desplaza. Ese margen es lo que le da capacidad de adaptación.",

             "A ello se suma su condición de **contrato marco**: no regula una "
             "operación aislada, sino la incorporación a una red. Por eso las "
             "obligaciones del franquiciado no derivan únicamente del texto "
             "pactado, sino también de la ejecución cotidiana del know-how dentro "
             "de ese sistema.",

             "Por último, el grado de protección varía según la jurisdicción. "
             "**Argentina** tipificó la figura en su Código Civil y Comercial: "
             "ante un litigio existen artículos aplicables. **Ecuador** la "
             "reconoce en su Código de Comercio. **Colombia y Perú** la mantienen "
             "como contrato atípico, es decir, la ley no la nombra. No es una "
             "figura ilegal, pero implica que lo que el contrato no prevea, no lo "
             "prevé nadie. En esas jurisdicciones el contrato no respalda el "
             "acuerdo: lo constituye por completo.",
         ]),

    dict(n=3, titulo="¿Cómo se aplica?",
         pantalla="Cinco etapas numeradas. Abajo, «20 días» y «La fórmula del "
                  "modelo».",
         texto=[
             "El ciclo tiene cinco etapas, y la primera no es la firma: es la "
             "selección.",

             "El titular de la marca evalúa candidatos con criterio restrictivo, "
             "porque cada establecimiento que abre compromete su nombre. La "
             "capacidad de inversión no es suficiente. La literatura "
             "especializada identifica la selección de franquiciados como una de "
             "las decisiones determinantes del desempeño del sistema completo, y "
             "la razón es asimétrica: si el franquiciado opera mal, él pierde su "
             "inversión, pero la marca pierde posicionamiento en todo el mercado.",

             "Alcanzado el acuerdo, se formaliza. Y en ese momento no solo se "
             "define el precio: se define la salida. Plazo, condiciones de "
             "renovación, causales de terminación anticipada y mecanismo de "
             "solución de controversias. Todo eso se pacta al inicio, cuando "
             "existe voluntad de acuerdo, precisamente porque después no la habrá.",

             "Sigue la transferencia y la capacitación, que es la etapa menos "
             "visible y una de las más determinantes: entrenamiento del "
             "propietario y del personal, entrega de manuales, acompañamiento en "
             "la apertura. Es aquí donde el know-how cambia de titular en "
             "términos prácticos.",

             "Luego opera el establecimiento. El franquiciado asume la gestión "
             "completa —contratación, nómina, atención, obligaciones locales— "
             "pero la ejerce dentro de parámetros que no definió.",

             "Y la relación no termina ahí: continúa la supervisión. Visitas, "
             "verificación de estándares, auditoría de instalaciones y, en muchos "
             "contratos, revisión de los libros contables. Es una facultad "
             "intrusiva, y está aceptada desde el inicio por una razón práctica: "
             "cuando el servicio falla, el consumidor no atribuye la falla al "
             "propietario del local, la atribuye a la marca. El costo "
             "reputacional se distribuye sobre toda la red.",

             "Hay además una obligación previa a todo el ciclo, que a mi juicio "
             "es la que mejor caracteriza esta figura. Antes de la firma, el "
             "titular de la marca debe entregar al futuro franquiciado "
             "información veraz sobre el negocio: su identidad, la situación del "
             "sector, la estructura de la red. Y con **veinte días hábiles** de "
             "anticipación, para que la decisión se adopte con información "
             "completa. El fundamento es la asimetría: quien ofrece conoce el "
             "negocio en detalle y quien invierte lo desconoce casi por completo. "
             "El incumplimiento de este deber genera responsabilidad por daños "
             "**aunque el contrato nunca llegue a celebrarse**.",
         ]),

    dict(n=4, titulo="Partes involucradas",
         pantalla="Dos tarjetas, franquiciador y franquiciado. Abajo, la franja "
                  "del subfranquiciante.",
         texto=[
             "Intervienen dos partes, con denominaciones que se confunden con "
             "facilidad. El **franquiciador**, también llamado franquiciante u "
             "otorgante, es el titular de la marca. El **franquiciado**, o "
             "franquiciatario, es quien explota el establecimiento.",

             "El franquiciador aporta activos intangibles: el nombre comercial, "
             "el know-how, los manuales y la asistencia técnica. Y hay un "
             "requisito previo que suele omitirse: para franquiciar debió haber "
             "operado con éxito su propio negocio durante un período razonable. "
             "No se franquicia un proyecto; se franquicia un historial "
             "acreditado. Ese historial es lo que determina el valor de lo que "
             "ofrece.",

             "Su contraprestación llega por dos vías que no deben confundirse. El "
             "**canon de entrada** es un pago único, al inicio, por el derecho de "
             "incorporación a la red. Las **regalías** son pagos periódicos, "
             "generalmente calculados como un porcentaje de las ventas.",

             "Y recibe algo más que la contraprestación económica: recibe "
             "facultades de control. Puede exigir uniformidad en la imagen del "
             "local y en la operación, y la reserva del know-how transferido.",

             "El franquiciado aporta capital y gestión: la inversión inicial, las "
             "regalías, la operación diaria, el equipo humano. Y aporta un activo "
             "que el franquiciador no posee: el conocimiento del comportamiento "
             "de compra en su propio mercado.",

             "A su favor, la protección más relevante suele ser la "
             "**exclusividad territorial**: el compromiso de que la marca no "
             "abrirá otro establecimiento en su zona de influencia fragmentando "
             "su clientela. Si no se pactó expresamente, no existe.",

             "Entre sus obligaciones figura además la prohibición de competir con "
             "la red mientras el contrato esté vigente, correlato lógico de haber "
             "recibido el método.",

             "Y aquí está la confusión más frecuente. Aunque siga manuales "
             "ajenos, aunque se someta a auditorías, aunque no fije libremente "
             "sus precios, el franquiciado **no es un dependiente**. Es un "
             "empresario independiente. Su capital está en riesgo. Si el "
             "establecimiento fracasa, la pérdida es suya y no de la marca.",

             "De ahí se deriva el punto de mayor litigiosidad: quién responde "
             "frente al consumidor que sufre un daño en el establecimiento. La "
             "regla general atribuye la responsabilidad al franquiciado, incluso "
             "por los actos de sus dependientes, por su condición de empresario "
             "independiente. La presencia del signo distintivo en la fachada no "
             "basta para trasladar la responsabilidad al titular de la marca. Sin "
             "embargo, los tribunales españoles han admitido una excepción, "
             "particularmente en clínicas de medicina estética y odontología: "
             "cuando se acredita que el daño derivó de una instrucción concreta "
             "impuesta por el franquiciador, este responde. La existencia de "
             "estándares y de supervisión no es suficiente; debe probarse el nexo "
             "causal.",

             "Finalmente, cuando la red se internacionaliza aparece un tercer "
             "sujeto. Un operador adquiere los derechos sobre un territorio "
             "completo y otorga franquicias dentro de él. No es el titular "
             "originario de la marca, pero frente a esos franquiciados asume sus "
             "funciones.",
         ]),

    dict(n=5, titulo="Ventajas y desventajas",
         pantalla="Seis ventajas y seis desventajas.",
         texto=[
             "En la diapositiva están los doce puntos; no voy a leerlos. Prefiero "
             "detenerme en la lógica que los ordena, porque ambas columnas "
             "describen la misma decisión desde lados opuestos.",

             "Lo que se obtiene se reduce a dos cosas. La primera es tiempo: no "
             "se invierte en averiguar qué funciona, se ingresa a un modelo ya "
             "validado. De ahí que las franquicias registren tasas de "
             "supervivencia superiores a las de negocios independientes "
             "comparables; no es un efecto del azar, es que los errores costosos "
             "ya fueron asumidos por otro. La segunda es escala: publicidad, "
             "capacitación y compras se ejecutan para la red completa. Un "
             "establecimiento independiente negocia cien unidades; la red negocia "
             "cien mil. No obtienen el mismo precio ni las mismas condiciones de "
             "pago.",

             "Lo que se cede son también dos cosas. Se cede margen, y en una "
             "modalidad que merece precisión: las regalías se calculan sobre "
             "**las ventas, no sobre la utilidad**. Si en un mes las ventas caen "
             "a la mitad, la regalía se liquida igual. Un mes adverso para el "
             "establecimiento no es un mes adverso para la marca.",

             "Y se cede autonomía. El franquiciado puede identificar una "
             "oportunidad válida en su mercado y no poder implementarla porque el "
             "manual no lo autoriza. Existe además una exposición que no controla "
             "en absoluto: si la marca sufre un deterioro reputacional, incluso "
             "por hechos ocurridos en otro país, su establecimiento pierde "
             "afluencia esa semana. Su gestión pudo ser impecable y el costo lo "
             "asume igual.",

             "Y ahí está el núcleo del asunto: aquello que genera el valor de la "
             "red es lo mismo que restringe al franquiciado. No hay uno sin el "
             "otro. El contrato únicamente determina en qué punto se traza esa "
             "línea.",
         ]),

    dict(n=6, titulo="Síntesis e intro al video",
         pantalla="El invitado y las cifras de la operación.",
         texto=[
             "Para cerrar, tres conclusiones.",

             "**La primera:** el modelo funciona porque ninguna de las partes "
             "podría sola. Una dispone de la marca, el método y la experiencia "
             "acumulada, pero no del capital ni del conocimiento de cada mercado. "
             "La otra dispone del capital y del mercado, pero no de la marca ni "
             "del método. La franquicia existe para articular esas dos mitades.",

             "**La segunda,** y es la que más me llamó la atención de la "
             "investigación: el grado de exposición de las partes depende de la "
             "jurisdicción. Donde la figura está regulada existe un estándar "
             "mínimo de protección. Donde no lo está, como en Colombia o en Perú, "
             "ese estándar lo fija el contrato o no lo fija nadie. Un contrato "
             "deficiente en esas jurisdicciones no es un problema de técnica "
             "jurídica: es la ausencia de defensa.",

             "**La tercera:** la independencia entre las partes es efectiva, pero "
             "admite un límite. Se sostiene mientras el franquiciador oriente la "
             "operación. Cuando el control alcanza tal intensidad que sustituye "
             "la decisión del franquiciado, esa independencia deja de ser real, y "
             "así lo han reconocido los tribunales.",

             "Todo lo anterior es análisis normativo y doctrinal. Para "
             "contrastarlo con la práctica, entrevistamos a **Gerardo Marcano**, "
             "vicepresidente de operaciones de Grupo David, con veinte años en la "
             "organización y responsabilidad sobre más de cuarenta franquicias.",

             "Antes de la proyección, dos términos que él emplea. **Brand "
             "awareness** es el nivel de reconocimiento y preferencia de marca en "
             "un mercado determinado; para él es el factor que define si una "
             "apertura procede o no procede. Y **tropicalización** es la "
             "adaptación del producto al mercado local: ajuste de materiales, "
             "colores y surtido según clima y hábitos de consumo. Su ejemplo es "
             "ilustrativo: enviar ropa de invierno a una ciudad cálida equivale a "
             "perder la temporada completa.",

             "Presten atención especialmente a un punto: hasta dónde puede "
             "adaptarse el modelo sin comprometer el estándar. Lo que aquí se "
             "plantea en términos abstractos, para él constituye una decisión "
             "operativa cotidiana.",

             "Con esto cierro. Gracias.",
         ]),
]

CIERRE = [
    "La entrevista confirma algo que en el análisis apenas se intuye: lo que "
    "sostiene una franquicia no es el texto del contrato, sino el rigor con que "
    "se replica el estándar y el criterio con que se adapta a cada mercado. Con "
    "gusto respondo sus preguntas.",
]

PREGUNTAS = [
    ("¿Cuál es el monto de una franquicia?",
     "Depende del sector y del posicionamiento de la marca, pero la estructura "
     "es siempre la misma: un canon de entrada, único y al inicio, y regalías "
     "periódicas calculadas normalmente como porcentaje de las ventas."),
    ("¿Puede el franquiciado transferir su establecimiento?",
     "Solo con autorización del franquiciador, y así suele quedar pactado. La "
     "marca conserva la facultad de decidir quién se incorpora a su red."),
    ("¿Quién responde frente a un daño sufrido por un cliente en el local?",
     "Por regla general el franquiciado, por su condición de empresario "
     "independiente. Los tribunales españoles han responsabilizado al "
     "franquiciador cuando se acredita que el daño derivó de una instrucción "
     "concreta que él impuso. La existencia de estándares de calidad no basta: "
     "debe probarse el nexo causal."),
    ("¿Qué margen tiene el franquiciado para modificar la operación?",
     "Requiere autorización. Un laudo arbitral colombiano, el caso PANACA, "
     "precisó que el franquiciado debe respetar la filosofía de la marca, pero "
     "que los manuales tampoco pueden aplicarse de forma idéntica en cualquier "
     "plaza o mercado."),
    ("¿En qué se diferencia de una licencia de marca?",
     "La licencia autoriza únicamente el uso del signo distintivo. La franquicia "
     "transfiere además el know-how, los manuales, la capacitación y la "
     "asistencia continua, y viene acompañada de facultades de control sobre la "
     "operación."),
    ("¿Por qué Colombia no ha regulado la figura pese a su difusión?",
     "Existe una norma técnica de aplicación voluntaria, la NTC 5813, y el "
     "Ministerio de Comercio redactó en 2021 un proyecto de decreto que "
     "contemplaba ese plazo de veinte días hábiles. A la fecha de nuestra "
     "revisión, el proyecto seguía sin expedirse."),
    ("¿Cuál es la duración habitual del contrato?",
     "La fija el contrato y varía según el sector, pero siempre incorpora "
     "cláusulas de renovación y de terminación. Se trata de una relación de "
     "larga duración, y por eso la salida se negocia desde el inicio."),
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
    f"El video de la entrevista se proyecta después de los quince minutos, de modo "
    f"que estos quince corresponden íntegramente a la exposición. La diapositiva 6 "
    f"la cierra y deja el video montado. Leído a ritmo normal, unas {PPM} palabras "
    f"por minuto, el guion completo da {mmss(total)}.")
r.font.size = Pt(11)
r.font.color.rgb = GREY

# --- tabla de tiempos
tabla = doc.add_table(rows=1, cols=3)
tabla.alignment = WD_TABLE_ALIGNMENT.LEFT
anchos = [Cm(2.4), Cm(8.6), Cm(2.4)]
encabezados = ["Entra", "Diapositiva", "Dura"]
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
    valores = [mmss(b["inicio"]), f"{b['n']} · {b['titulo']}", mmss(b["seg"])]
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
finales = [mmss(total), "Fin de la exposición · empieza el video", ""]
for i, w in enumerate(anchos):
    c = fila.cells[i]
    c.width = w
    solo_borde_inferior(c)
    pp = c.paragraphs[0]
    pp.paragraph_format.space_after = Pt(0)
    rr = pp.add_run(finales[i])
    rr.font.bold = i < 2
    rr.font.size = Pt(10)
    rr.font.name = SANS
    rr.font.color.rgb = ACCENT

# --- válvulas de escape
p = doc.add_paragraph()
p.paragraph_format.space_before = Pt(12)
p.paragraph_format.space_after = Pt(3)
r = p.add_run("SI EL TIEMPO SE CORRE")
r.font.size = Pt(9)
r.font.bold = True
r.font.color.rgb = ACCENT

for t in ("Para recuperar unos 50 segundos: en la diapositiva 4, omitir el "
          "párrafo sobre responsabilidad frente al consumidor.",
          "Para recuperar unos 20 segundos: en la diapositiva 5, suprimir el "
          "caso del deterioro reputacional.",
          "Si sobra tiempo: desarrollar los ejemplos de la diapositiva 5, que es "
          "donde la explicación gana concreción."):
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
