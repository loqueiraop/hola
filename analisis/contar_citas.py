"""
Conteo RIGUROSO de citas en el texto de la tesis.

Metodo: se evalua CADA parrafo por separado (incluidos los parrafos dentro de
tablas y de content controls). Nunca se concatena el documento, porque las
expresiones regulares saltan los saltos de linea y fabrican coincidencias falsas
entre el final de un parrafo y el año que aparece en el siguiente.
"""
import docx, re, collections
from docx.oxml.ns import qn

doc = docx.Document("/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 (1).docx")
body = doc.element.body
MAY, MIN = "A-ZÁÉÍÓÚÑ", "a-záéíóúñ"

# (Autor, 2020) / (Autor et al. 2020) / (Autor, 2020, p. 15)  -- sin saltos de linea
CITA_PAREN = re.compile(
    rf"\([^()\n]{{0,80}}?[{MAY}][^()\n]{{1,80}}?,?\s*(?:19|20)\d{{2}}[a-z]?"
    rf"(?:\s*[-–]\s*\d{{2,4}})?\s*(?:,\s*p+\.?\s*[\d\-]+)?\)")
# Autor (2020) / Autor y Autor (2020) / Autor et al. (2020)  -- sin saltos de linea
CITA_NARR = re.compile(
    rf"[{MAY}][\w{MIN}\.]*(?:\s+(?:y|and|&|et\s+al\.?|de|del|la)?\s*[{MAY}][\w{MIN}\.]*){{0,4}}"
    rf"\s*\(\s*(?:19|20)\d{{2}}[a-z]?\s*\)")
# Normas y actos juridicos: no son citas de autor, se cuentan aparte
NORMA = re.compile(
    r"\b(?:Decreto|Acuerdo|Ley|Reglamento|NTON|RTCA|OHN|ISO|AOAC|IDF|CFR)\s*"
    r"(?:N[°º.]?\s*)?[\w\-/\.]*\d[\w\-/\.]*")

# Recolectar todos los parrafos con su contenedor
parrafos = []           # (indice_global, origen, estilo, texto)
top_idx = 0
for child in body:
    tag = child.tag.split('}')[1]
    if tag == 'p':
        txt = "".join(t.text or "" for t in child.iter(qn('w:t')))
        style = ""
        pr = child.find(qn('w:pPr'))
        if pr is not None:
            st = pr.find(qn('w:pStyle'))
            if st is not None:
                style = st.get(qn('w:val')) or ""
        parrafos.append((top_idx, "cuerpo", style, txt))
        top_idx += 1
    elif tag == 'tbl':
        for p in child.iter(qn('w:p')):
            parrafos.append((None, "tabla", "", "".join(t.text or "" for t in p.iter(qn('w:t')))))
    elif tag == 'sdt':
        for p in child.iter(qn('w:p')):
            parrafos.append((None, "sdt", "", "".join(t.text or "" for t in p.iter(qn('w:t')))))

# La bibliografia es el ultimo sdt: la excluimos del conteo de "citas en el texto"
corte = max((i for i, (_, o, _, t) in enumerate(parrafos)
             if o == "sdt" and t.strip() == "Bibliografía"), default=len(parrafos))
cuerpo = parrafos[:corte]
biblio = [t for _, _, _, t in parrafos[corte:] if t.strip()]

print(f"parrafos analizados: {len(cuerpo)} "
      f"({sum(1 for p in cuerpo if p[1]=='cuerpo')} de cuerpo, "
      f"{sum(1 for p in cuerpo if p[1]=='tabla')} en tablas, "
      f"{sum(1 for p in cuerpo if p[1]=='sdt')} en controles)")
print(f"entradas en la bibliografia: {len(biblio)-1}")
print(f"palabras de texto analizadas: {sum(len(t.split()) for *_, t in cuerpo)}\n")

citas, normas = [], []
for idx, origen, estilo, t in cuerpo:
    if not t.strip():
        continue
    for m in CITA_PAREN.finditer(t):
        citas.append((idx, origen, estilo, "parentetica", m.group(0).strip()))
    for m in CITA_NARR.finditer(t):
        citas.append((idx, origen, estilo, "narrativa", m.group(0).strip()))
    for m in NORMA.finditer(t):
        normas.append((idx, m.group(0).strip()))

# quitar solapes: una misma posicion contada por los dos patrones
vistos, limpias = set(), []
for c in citas:
    clave = (c[0], c[4])
    if clave not in vistos:
        vistos.add(clave)
        limpias.append(c)

print("=" * 80)
print(f"CITAS DE AUTOR-AÑO EN EL TEXTO: {len(limpias)}")
print(f"  en parrafos distintos:        {len({c[0] for c in limpias})}")
print(f"  fuentes distintas invocadas:  {len({c[4] for c in limpias})}")
print(f"  de ellas en tablas:           {sum(1 for c in limpias if c[1]=='tabla')}")
print("=" * 80)
for c in sorted(limpias, key=lambda x: (x[0] is None, x[0])):
    print(f"  parrafo {str(c[0]):>5} [{c[1]:<6}] {c[3]:<12} {c[4][:70]}")

print("\n" + "=" * 80)
print(f"REFERENCIAS NORMATIVAS (no son citas de autor): {len(normas)}"
      f" · {len({n for _, n in normas})} distintas")
print("=" * 80)
for n, k in collections.Counter(n for _, n in normas).most_common():
    print(f"  [{k}x] {n}")
