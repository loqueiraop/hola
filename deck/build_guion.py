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
VERSION = "v4"
FECHA = "10 de septiembre de 2026"

INK = RGBColor(0x1B, 0x2A, 0x41)
GREY = RGBColor(0x70, 0x78, 0x86)
ACCENT = RGBColor(0xA8, 0x53, 0x2B)
SERIF, SANS = "Georgia", "Calibri"


# ------------------------------------------------------------------ contenido
from guion_texto import BLOQUES, CIERRE, PREGUNTAS


# Aperturas válidas de párrafo: conectores explícitos, referencias anafóricas
# (eso, esa, acá) y marcas de enumeración ya anunciada (la primera, el segundo).
CONECTORES = (
    "y ", "pero ", "porque", "así que", "entonces", "ahora", "además", "sin embargo",
    "de hecho", "por eso", "justamente", "luego", "después", "cuando", "una vez",
    "veamos", "en esta", "en la", "en el", "la primera", "la segunda", "la tercera",
    "el primero", "el segundo", "el tercero", "el cuarto", "el contrato",
    "lo que", "de esa", "de ese",
    "por último", "acá ", "para ", "a esto", "a cambio", "del otro lado",
    "antes de", "eso ", "esa ", "esto ", "con esto", "presten", "cualquier",
    "la marca no", "la empresa", "lo único", "veinte", "al final",
)


def revisar_estilo():
    """Falla si el texto hablado se puede leer mal en voz alta."""
    problemas = []
    todo = [(f"d{b['n']}", t) for b in BLOQUES for t in b["texto"]]
    todo += [("cierre", t) for t in CIERRE]
    for donde, t in todo:
        limpio = t.replace("**", "")
        for signo, nombre in ((":", "dos puntos"), (";", "punto y coma"),
                              ("—", "guion largo")):
            if signo in limpio:
                problemas.append(f"{donde}: {nombre} → …{limpio[max(0, limpio.index(signo) - 40):limpio.index(signo) + 25]}…")
    # el primer párrafo de cada bloque puede abrir sin conector; el resto, no
    for b in BLOQUES:
        for t in b["texto"][1:]:
            ini = t.replace("**", "").lower()
            if not ini.startswith(CONECTORES):
                problemas.append(f"d{b['n']}: párrafo sin conector → …{ini[:55]}…")
    return problemas


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

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(14)
r = p.add_run(f"{VERSION.upper()} · {FECHA} · define red, estándar y manual "
              f"operativo la primera vez que aparecen")
r.font.size = Pt(9)
r.font.bold = True
r.font.color.rgb = ACCENT

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
                   f"Guion Contrato de Franquicia {VERSION}.docx")
doc.save(out)

fallos = revisar_estilo()
print("ESTILO:", "sin dos puntos y todos los párrafos con conector"
      if not fallos else f"{len(fallos)} problemas")
for f in fallos:
    print("  !", f)
print()

print(f"{'Bloque':38s} {'pal':>5s} {'dura':>7s}  {'entra':>7s}")
for b in BLOQUES:
    print(f"{str(b['n']) + ' · ' + b['titulo'][:32]:38s} {b['palabras']:5d} "
          f"{mmss(b['seg']):>7s}  {mmss(b['inicio']):>7s}")
print(f"\nEXPOSICION: {mmss(total)}  (margen {900 - total:+.0f} s)")
print(out)
