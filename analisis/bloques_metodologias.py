"""Delimita cada metodologia del marco teorico: parrafos, palabras, citas que ya
usa y cuanto texto tiene resaltado. Es el mapa para condensarlas."""
import docx, json, re, collections, unicodedata
from docx.oxml.ns import qn

doc = docx.Document("/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 - REVISADA.docx")
ps = doc.paragraphs

def real(p):
    return "".join(t.text or "" for t in p._p.iter(qn('w:t')))

def norm(s):
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9 ]", " ", re.sub(r"\s+", " ", s)).strip()

data = json.load(open("/projects/sandbox/hola/analisis/matches.json"))
resaltado = norm(" ".join(r["text"] for r in data["records"]))

PAREN = re.compile(r"\([^()]{0,120}?(?:1[89]|20)\d{2}[^()]{0,40}?\)")

BLOQUES = [
    ("ONUDI",                                   241, 247, "LA QUE SE USA"),
    ("Marco Logico EML/LFA",                    277, 291, "descartable"),
    ("JICA",                                    292, 303, "descartable"),
    ("ZOPP",                                    304, 307, "descartable"),
    ("MGA",                                     308, 316, "descartable"),
    ("Ciclo de gestion de proyectos (PCM)",     317, 327, "descartable"),
    ("Sapag (BID/CEPAL)",                       328, 338, "descartable"),
    ("BPIN / SNIP",                             339, 343, "descartable"),
    ("Criterios CAD-OCDE",                      344, 353, "descartable"),
    ("AIKA",                                    354, 358, "descartable"),
]

print(f"{'METODOLOGIA':<38}{'parrafos':>12}{'palabras':>9}{'citas':>7}{'% resalt.':>11}")
print("=" * 82)
tot_pal = tot_desc = 0
detalle = {}
for nombre, a, b, estado in BLOQUES:
    pal = citas = 0
    res = 0
    refs = []
    for i in range(a, b + 1):
        t = real(ps[i])
        w = t.split()
        pal += len(w)
        for m in PAREN.finditer(t):
            citas += 1
            refs.append(m.group(0))
        # cuanto de este parrafo aparece en el texto resaltado
        nw = norm(t).split()
        marcadas = 0
        for k in range(0, max(1, len(nw) - 6)):
            if " ".join(nw[k:k + 6]) in resaltado:
                marcadas += 1
        res += min(marcadas + 6, len(nw)) if marcadas else 0
    pct = res / pal * 100 if pal else 0
    detalle[nombre] = refs
    tot_pal += pal
    if estado == "descartable":
        tot_desc += pal
    marca = "  <-- se conserva" if estado == "LA QUE SE USA" else ""
    print(f"{nombre:<38}{f'{a}-{b}':>12}{pal:>9}{citas:>7}{pct:>10.0f}%{marca}")

print("=" * 82)
print(f"{'TOTAL del bloque de metodologias':<38}{'241-358':>12}{tot_pal:>9}")
print(f"{'  de eso, las 9 que no se usan':<38}{'':>12}{tot_desc:>9}  "
      f"= {tot_desc/52958*100/1.19:.1f} puntos del informe")

print("\n" + "=" * 82)
print("CITAS QUE YA USA CADA METODOLOGIA (pasan a la tabla comparativa)")
print("=" * 82)
for nombre, refs in detalle.items():
    unicas = sorted(set(refs))
    print(f"\n  {nombre}")
    if unicas:
        for r in unicas:
            print(f"      {r}")
    else:
        print("      (ninguna: hay que añadirla)")
