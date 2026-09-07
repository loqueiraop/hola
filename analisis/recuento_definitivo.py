"""
Recuento DEFINITIVO de citas. Los intentos anteriores fallaron por patrones
demasiado estrechos: no reconocian 'pág.', 'págs.', 'pp.', ni las citas sin coma.

Aqui se detecta cualquier parentesis que contenga un año de 4 cifras, que es la
definicion practica de una cita autor-año, y se listan para revision manual.
"""
import docx, re, collections
from docx.oxml.ns import qn

DOCX = "/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 - REVISADA.docx"
doc = docx.Document(DOCX)

def real(p):
    return "".join(t.text or "" for t in p._p.iter(qn('w:t')))

# Cualquier parentesis con un año dentro, mas las narrativas Autor (año)
PAREN_ANIO = re.compile(r"\([^()]{0,120}?(?:1[89]|20)\d{2}[^()]{0,40}?\)")
NARRATIVA = re.compile(r"[A-ZÁÉÍÓÚÑ][\w\.áéíóúñ]*(?:\s+(?:y|&|et\s+al\.?|de|del|la|,)?\s*"
                       r"[A-ZÁÉÍÓÚÑ][\w\.áéíóúñ]*){0,4}\s*\(\s*(?:1[89]|20)\d{2}[a-z]?\s*[,)]")
# descartar parentesis que son claramente NO citas (rangos de años, normas, cifras)
NO_CITA = re.compile(r"^\((?:[\d\s\-–.,%]+|"                      # solo numeros
                     r"(?:ver|véase|figura|tabla|anexo)[^)]*)\)$", re.I)

citas_por_parrafo = collections.defaultdict(list)
for i, p in enumerate(doc.paragraphs):
    t = real(p)
    encontrado = []
    for m in PAREN_ANIO.finditer(t):
        s = m.group(0)
        if NO_CITA.match(s):
            continue
        encontrado.append(s)
    for m in NARRATIVA.finditer(t):
        encontrado.append(m.group(0).rstrip(",("). strip())
    if encontrado:
        citas_por_parrafo[i] = encontrado

total = sum(len(v) for v in citas_por_parrafo.values())
print(f"TOTAL de citas detectadas: {total}  en {len(citas_por_parrafo)} parrafos\n")

# ---- auditoria del MARCO TEORICO ----
print("=" * 84)
print("MARCO TEORICO (parrafos 231-358): parrafos sustantivos y su atribucion")
print("=" * 84)
con = sin = 0
pal_con = pal_sin = 0
sin_lista = []
for i in range(231, 359):
    t = real(doc.paragraphs[i]).strip()
    nw = len(t.split())
    if nw < 25:
        continue
    if i in citas_por_parrafo:
        con += 1; pal_con += nw
    else:
        sin += 1; pal_sin += nw
        sin_lista.append((i, nw, t[:88]))
print(f"CON cita: {con} parrafos, {pal_con} palabras")
print(f"SIN cita: {sin} parrafos, {pal_sin} palabras")
print(f"Cobertura: {pal_con/(pal_con+pal_sin)*100:.0f}% de las palabras del marco teorico\n")
print("Los que siguen SIN cita:")
for i, nw, t in sin_lista:
    print(f"  [{i}] {nw:3d} pal | {t}")

# ---- densidad por seccion ----
SECC = [("Introduccion/Objetivos/Justificacion", 197, 230),
        ("MARCO TEORICO metodologias", 231, 358),
        ("MARCO TEORICO leche", 359, 383),
        ("DISENO METODOLOGICO", 384, 469),
        ("Estudio sectorial", 470, 572),
        ("Estudio de mercado", 573, 694),
        ("Estudio tecnico", 695, 956),
        ("Estudio organizacional", 957, 990),
        ("Estudio legal", 991, 1111),
        ("Estudio ambiental", 1112, 1140),
        ("Estudio financiero", 1141, 1423),
        ("Analisis de riesgos", 1424, 1616),
        ("Conclusiones/Recomendaciones", 1617, 1637)]
print("\n" + "=" * 84)
print(f"{'SECCION':<40}{'palabras':>9}{'citas':>7}{'1 cita cada':>16}")
print("=" * 84)
for nombre, a, b in SECC:
    w = sum(len(real(doc.paragraphs[i]).split()) for i in range(a, min(b + 1, len(doc.paragraphs))))
    c = sum(len(citas_por_parrafo.get(i, [])) for i in range(a, min(b + 1, len(doc.paragraphs))))
    print(f"{nombre:<40}{w:>9}{c:>7}{(f'{w//c} palabras' if c else '— ninguna'):>16}")
