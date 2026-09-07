"""
Densidad de citas por seccion, leyendo el texto REAL de cada parrafo.

ATENCION: las citas de esta tesis son marcadores de Citavi dentro de controles de
contenido inline (w:sdt). El atributo .text de python-docx NO las devuelve. Hay que
recorrer todos los nodos w:t descendientes del parrafo.
"""
import docx, re, collections
from docx.oxml.ns import qn

doc = docx.Document("/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 (1).docx")
ps = doc.paragraphs
MAY, MIN = "A-ZÁÉÍÓÚÑ", "a-záéíóúñ"

CITA_PAREN = re.compile(
    rf"\([^()\n]{{0,80}}?[{MAY}][^()\n]{{1,80}}?,?\s*(?:19|20)\d{{2}}[a-z]?"
    rf"(?:\s*[-–]\s*\d{{2,4}})?\s*(?:,\s*p+\.?\s*[\d\-]+)?\)")
CITA_NARR = re.compile(
    rf"[{MAY}][\w{MIN}\.]*(?:\s+(?:y|and|&|et\s+al\.?|de|del|la)?\s*[{MAY}][\w{MIN}\.]*){{0,4}}"
    rf"\s*\(\s*(?:19|20)\d{{2}}[a-z]?\s*\)")

def texto_real(p):
    """Todos los w:t del parrafo, incluidos los de controles inline (citas Citavi)."""
    return "".join(t.text or "" for t in p._p.iter(qn('w:t')))

def n_citas(t):
    pos = set()
    for rx in (CITA_PAREN, CITA_NARR):
        for m in rx.finditer(t):
            pos.add(m.end())
    return len(pos)

SECC = [
    ("Introduccion / Objetivos / Justificacion", 197, 230),
    ("MARCO TEORICO - metodologias (ONUDI, ML, JICA, ZOPP, CAD, SNIP)", 231, 358),
    ("MARCO TEORICO - leche pasteurizada", 359, 383),
    ("DISENO METODOLOGICO", 384, 469),
    ("Desarrollo: Estudio sectorial", 470, 572),
    ("Desarrollo: Estudio de mercado", 573, 694),
    ("Desarrollo: Estudio tecnico", 695, 956),
    ("Desarrollo: Estudio organizacional", 957, 990),
    ("Desarrollo: Estudio legal", 991, 1111),
    ("Desarrollo: Estudio ambiental", 1112, 1140),
    ("Desarrollo: Estudio financiero", 1141, 1423),
    ("Desarrollo: Analisis de riesgos", 1424, 1616),
    ("Conclusiones y recomendaciones", 1617, 1637),
    ("Anexos", 1638, 1698),
]

print(f"{'SECCION':<64}{'palabras':>9}{'citas':>7}{'1 cita cada':>14}")
print("-" * 94)
for nombre, a, b in SECC:
    w = c = 0
    for i in range(a, min(b + 1, len(ps))):
        t = texto_real(ps[i]); w += len(t.split()); c += n_citas(t)
    ratio = f"{w//c} palabras" if c else "— NINGUNA"
    print(f"{nombre[:63]:<64}{w:>9}{c:>7}{ratio:>14}")

print("\n\n=== MARCO TEORICO: parrafos sustantivos y su atribucion (231-358) ===")
con = sin = 0
pal_con = pal_sin = 0
sin_lista = []
for i in range(231, 359):
    t = texto_real(ps[i]).strip()
    nw = len(t.split())
    if nw < 25:
        continue
    c = n_citas(t)
    if c:
        con += 1; pal_con += nw
    else:
        sin += 1; pal_sin += nw; sin_lista.append((i, nw, t[:80]))
print(f"CON cita: {con} parrafos ({pal_con} palabras)")
print(f"SIN cita: {sin} parrafos ({pal_sin} palabras)")
print(f"Cobertura de atribucion: {pal_con/(pal_con+pal_sin)*100:.0f}% de las palabras\n")
print("Parrafos del marco teorico SIN ninguna cita:")
for i, nw, t in sin_lista:
    print(f"  [{i}] {nw:3d} pal | {t}")
