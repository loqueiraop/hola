"""
Sistema de diseño para la presentación "Contrato de Franquicia".
Estética: minimalista, editorial, académica. Tipografía serif para títulos,
sans para cuerpo. Paleta reducida a 4 tonos + acento cálido.
"""

from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

# ---------------------------------------------------------------- lienzo
SW = 13.3333          # ancho de diapositiva (16:9)
SH = 7.5              # alto
M = 0.95              # margen izquierdo / derecho
CW = SW - 2 * M       # ancho de contenido = 11.4333

# ---------------------------------------------------------------- paleta
INK        = RGBColor(0x1B, 0x2A, 0x41)   # azul tinta, texto principal
INK_SOFT   = RGBColor(0x44, 0x55, 0x70)   # texto secundario
MUTED      = RGBColor(0x8A, 0x94, 0xA6)   # metadatos, notas al pie
BG         = RGBColor(0xF8, 0xF7, 0xF4)   # fondo hueso cálido
CARD       = RGBColor(0xFF, 0xFF, 0xFF)   # tarjetas
CARD_ALT   = RGBColor(0xF1, 0xEF, 0xEA)   # tarjetas secundarias
LINE       = RGBColor(0xDD, 0xD9, 0xD1)   # filetes y bordes
ACCENT     = RGBColor(0xC5, 0x6C, 0x3E)   # terracota, acento único
ACCENT_TN  = RGBColor(0xEC, 0xDC, 0xD1)   # acento translúcido
ON_DARK    = RGBColor(0xF3, 0xF1, 0xEC)   # texto sobre fondo tinta
ON_DARK_SF = RGBColor(0xA9, 0xB4, 0xC6)   # texto secundario sobre tinta

# ---------------------------------------------------------------- tipografía
SERIF = "Georgia"          # títulos y cifras
SANS  = "Calibri"          # cuerpo, etiquetas


# ---------------------------------------------------------------- primitivas
def letterspace(run, hundredths_pt):
    """Aplica tracking (espaciado entre letras). PowerPoint lo lee en 1/100 pt."""
    run.font._rPr.set("spc", str(int(hundredths_pt)))


def textbox(slide, x, y, w, h, *, anchor=MSO_ANCHOR.TOP, wrap=True):
    tb = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = wrap
    tf.vertical_anchor = anchor
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    return tf


def para(tf, text, *, first=False, size=12, font=SANS, color=INK, bold=False,
         italic=False, align=PP_ALIGN.LEFT, space_before=0, space_after=0,
         line=1.25, spc=None, caps=False):
    """Añade un párrafo formateado. first=True reutiliza el párrafo inicial."""
    p = tf.paragraphs[0] if first else tf.add_paragraph()
    p.alignment = align
    p.space_before = Pt(space_before)
    p.space_after = Pt(space_after)
    p.line_spacing = line
    r = p.add_run()
    r.text = text.upper() if caps else text
    f = r.font
    f.name = font
    f.size = Pt(size)
    f.bold = bold
    f.italic = italic
    f.color.rgb = color
    if spc:
        letterspace(r, spc)
    return p


def rich(tf, segments, *, first=False, align=PP_ALIGN.LEFT, space_before=0,
         space_after=0, line=1.25):
    """Párrafo con varios tramos: [(texto, {opciones}), ...]"""
    p = tf.paragraphs[0] if first else tf.add_paragraph()
    p.alignment = align
    p.space_before = Pt(space_before)
    p.space_after = Pt(space_after)
    p.line_spacing = line
    for text, o in segments:
        r = p.add_run()
        r.text = text
        f = r.font
        f.name = o.get("font", SANS)
        f.size = Pt(o.get("size", 12))
        f.bold = o.get("bold", False)
        f.italic = o.get("italic", False)
        f.color.rgb = o.get("color", INK)
        if o.get("spc"):
            letterspace(r, o["spc"])
    return p


def rect(slide, x, y, w, h, *, fill=None, line_color=None, line_w=0.75,
         shape=MSO_SHAPE.RECTANGLE):
    s = slide.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    s.shadow.inherit = False
    if fill is None:
        s.fill.background()
    else:
        s.fill.solid()
        s.fill.fore_color.rgb = fill
    if line_color is None:
        s.line.fill.background()
    else:
        s.line.color.rgb = line_color
        s.line.width = Pt(line_w)
    s.text_frame.word_wrap = True
    return s


def rule(slide, x, y, w, *, color=ACCENT, weight=0.035):
    """Filete horizontal fino."""
    return rect(slide, x, y, w, weight, fill=color)


def vrule(slide, x, y, h, *, color=LINE, weight=0.01):
    return rect(slide, x, y, weight, h, fill=color)


# ---------------------------------------------------------------- andamiaje
def canvas(prs, dark=False):
    """Diapositiva en blanco con fondo pintado."""
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    rect(slide, 0, 0, SW, SH, fill=(INK if dark else BG))
    return slide


def header(slide, eyebrow, title, *, subtitle=None, dark=False, title_size=31):
    """Encabezado estándar: etiqueta, título serif, filete de acento."""
    ink = ON_DARK if dark else INK
    soft = ON_DARK_SF if dark else INK_SOFT

    tf = textbox(slide, M, 0.60, CW, 0.24)
    para(tf, eyebrow, first=True, size=9.5, color=ACCENT, bold=True,
         caps=True, spc=190, line=1.0)

    tf = textbox(slide, M, 0.92, CW * 0.86, 0.62)
    para(tf, title, first=True, size=title_size, font=SERIF, color=ink, line=1.0)

    rule(slide, M, 1.66, 0.72)

    if subtitle:
        tf = textbox(slide, M, 1.86, CW * 0.80, 0.34)
        para(tf, subtitle, first=True, size=12.5, color=soft, italic=True,
             line=1.2)
    return slide


def footer(slide, number, *, dark=False):
    """Pie de página: rótulo corrido + número de diapositiva."""
    soft = ON_DARK_SF if dark else MUTED
    tf = textbox(slide, M, 6.94, CW * 0.6, 0.22)
    para(tf, "Contrato de franquicia · Acuerdos Comerciales · Grupo 1",
         first=True, size=8, color=soft, caps=True, spc=90, line=1.0)

    tf = textbox(slide, SW - M - 1.0, 6.94, 1.0, 0.22)
    para(tf, f"{number:02d}", first=True, size=8.5, font=SERIF, color=soft,
         align=PP_ALIGN.RIGHT, line=1.0)


def notes(slide, text):
    slide.notes_slide.notes_text_frame.text = text


# ---------------------------------------------------------------- componentes
def card(slide, x, y, w, h, *, fill=CARD, border=LINE, accent_top=False,
         accent_left=False):
    s = rect(slide, x, y, w, h, fill=fill, line_color=border)
    if accent_top:
        rect(slide, x, y, w, 0.045, fill=ACCENT)
    if accent_left:
        rect(slide, x, y, 0.045, h, fill=ACCENT)
    return s


def bullets(slide, x, y, w, h, items, *, size=10.5, gap=5.5, color=INK_SOFT,
            marker="—", marker_color=ACCENT, line=1.2):
    """Lista con guion de acento como viñeta, en dos columnas de texto alineadas."""
    tf = textbox(slide, x, y, w, h)
    for i, it in enumerate(items):
        rich(tf,
             [(f"{marker}  ", {"size": size, "color": marker_color, "bold": True}),
              (it, {"size": size, "color": color})],
             first=(i == 0), space_after=gap, line=line)
    return tf


def stat(slide, x, y, w, figure, label, *, fig_size=40, lab_size=10):
    tf = textbox(slide, x, y, w, 0.62)
    para(tf, figure, first=True, size=fig_size, font=SERIF, color=ACCENT, line=0.95)
    tf = textbox(slide, x, y + 0.66, w, 0.7)
    para(tf, label, first=True, size=lab_size, color=INK_SOFT, line=1.2)


def divider(prs, roman, title, blurb, items):
    """Portadilla de sección sobre fondo tinta."""
    slide = canvas(prs, dark=True)

    # marca de agua: numeral romano gigante, muy sutil
    tf = textbox(slide, SW - M - 4.2, 1.05, 4.2, 4.6, anchor=MSO_ANCHOR.MIDDLE)
    para(tf, roman, first=True, size=190, font=SERIF,
         color=RGBColor(0x28, 0x39, 0x52), align=PP_ALIGN.RIGHT, line=0.9)

    tf = textbox(slide, M, 2.42, 1.6, 0.24)
    para(tf, f"Sección {roman}", first=True, size=9.5, color=ACCENT, bold=True,
         caps=True, spc=190, line=1.0)

    tf = textbox(slide, M, 2.78, CW * 0.62, 1.0)
    para(tf, title, first=True, size=40, font=SERIF, color=ON_DARK, line=1.02)

    rule(slide, M, 4.06, 0.72)

    tf = textbox(slide, M, 4.30, CW * 0.52, 0.6)
    para(tf, blurb, first=True, size=12.5, color=ON_DARK_SF, italic=True, line=1.3)

    tf = textbox(slide, M, 5.16, CW * 0.62, 0.8)
    for i, it in enumerate(items):
        rich(tf, [("·  ", {"size": 10, "color": ACCENT, "bold": True}),
                  (it, {"size": 10, "color": ON_DARK_SF, "spc": 40})],
             first=(i == 0), space_after=4, line=1.15)
    return slide
