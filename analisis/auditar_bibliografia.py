"""
¿Cuadra la bibliografia de Citavi con lo que realmente se cita en el texto?

Citavi genera la bibliografia a partir de las citas del documento. Si aparecen
entradas que no se citan en ninguna parte, es que se insertaron a mano o que
la bibliografia no se ha actualizado.
"""
import docx, re, unicodedata
from docx.oxml.ns import qn

doc = docx.Document("/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 (1).docx")
body = doc.element.body
MAY, MIN = "A-ZÁÉÍÓÚÑ", "a-záéíóúñ"

# --- 1. Entradas de la bibliografia (ultimo sdt) ---
sdt_bib = None
for sdt in body.findall(qn('w:sdt')):
    cont = sdt.find(qn('w:sdtContent'))
    if cont is None:
        continue
    paras = cont.findall(qn('w:p'))
    textos = ["".join(t.text or "" for t in p.iter(qn('w:t'))) for p in paras]
    if any(t.strip() == "Bibliografía" for t in textos):
        sdt_bib = [t for t in textos if t.strip() and t.strip() != "Bibliografía"]

# Una entrada empieza con Autor/Institucion y contiene "(año)"
entradas, actual = [], ""
for linea in sdt_bib:
    if re.search(r"\((?:19|20)\d{2}[a-z]?\)", linea) and actual:
        entradas.append(actual.strip()); actual = linea
    else:
        actual += " " + linea
if actual.strip():
    entradas.append(actual.strip())
entradas = [e for e in entradas if len(e) > 15]

print(f"Entradas reales en la bibliografia: {len(entradas)}\n")

# --- 2. Citas del texto ---
CITA = re.compile(
    rf"\([^()\n]{{0,80}}?[{MAY}][^()\n]{{1,80}}?,?\s*((?:19|20)\d{{2}})[a-z]?"
    rf"(?:\s*[-–]\s*\d{{2,4}})?\s*(?:,\s*p+\.?\s*[\d\-]+)?\)")
CITA_N = re.compile(
    rf"([{MAY}][\w{MIN}\.]*(?:\s+(?:y|and|&|et\s+al\.?|de|del|la)?\s*[{MAY}][\w{MIN}\.]*){{0,4}})"
    rf"\s*\(\s*((?:19|20)\d{{2}})[a-z]?\s*\)")

def clave(s):
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    return set(re.findall(r"[a-z]{4,}", s))

citadas = []
for p in doc.paragraphs:
    real = "".join(t.text or "" for t in p._p.iter(qn('w:t')))
    for m in CITA.finditer(real):
        citadas.append((m.group(0).strip("() "), m.group(1)))
    for m in CITA_N.finditer(real):
        citadas.append((m.group(1).strip(), m.group(2)))

print(f"Citas recogidas del texto: {len(citadas)}\n")

# --- 3. Emparejar por año + palabra de autor en comun ---
sin_citar = []
for e in entradas:
    m = re.search(r"\((?:19|20)\d{2}[a-z]?\)", e)
    anio = m.group(0).strip("()")[:4] if m else None
    autor_bib = clave(e[:m.start()] if m else e[:60])
    ok = False
    for txt, a in citadas:
        if a[:4] != anio:
            continue
        if autor_bib & clave(txt):
            ok = True; break
    if not ok:
        sin_citar.append(e)

print("=" * 78)
print(f"Entradas SIN cita localizable en el texto: {len(sin_citar)} de {len(entradas)}")
print("=" * 78)
for e in sin_citar:
    print(f"  · {e[:110]}")
