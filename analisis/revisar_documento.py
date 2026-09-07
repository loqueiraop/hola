"""
Correcciones mecanicas sobre la tesis. NO altera el contenido: solo tipografia,
fuente y niveles de titulo.

Reglas de seguridad:
  - Se reescribe el ZIP entrada por entrada; solo se toca word/document.xml.
  - Nunca se modifica texto que pertenezca a un campo de Citavi (se revertiria
    al actualizar las citas y corromperia el resultado del campo).
  - Se excluyen la tabla de contenido y los indices (llevan puntos de relleno
    y tabulaciones que no hay que tocar).
  - Solo se ELIMINAN espacios sobrantes o se AÑADE el espacio que falta tras
    una coma. No se cambia ni una palabra.
"""
import zipfile, re, collections
from lxml import etree

SRC = "/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 - comillas corregidas.docx"
DST = "/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 - REVISADA.docx"
W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
XMLSPACE = "{http://www.w3.org/XML/1998/namespace}space"

zin = zipfile.ZipFile(SRC)
root = etree.fromstring(zin.read("word/document.xml"))

log = collections.Counter()
detalle = collections.defaultdict(list)

# ---------- utilidades ----------
def dentro_de_citavi(nodo):
    n = nodo.getparent()
    while n is not None:
        if n.tag == W + "sdt" and b"Citavi" in etree.tostring(n):
            return True
        n = n.getparent()
    return False

def estilo(p):
    pr = p.find(W + "pPr")
    if pr is None:
        return ""
    st = pr.find(W + "pStyle")
    return (st.get(W + "val") or "") if st is not None else ""

def texto(p):
    return "".join(t.text or "" for t in p.iter(W + "t"))

# ---------- 1. TIPOGRAFIA ----------
EXCLUIR_ESTILO = re.compile(r"^(ndicemanual|TDC|Tabladecontenido|Index|ndice)", re.I)

for p_i, p in enumerate(root.iter(W + "p")):
    st = estilo(p)
    txt = texto(p)
    # saltar indices y cualquier parrafo con puntos de relleno
    if EXCLUIR_ESTILO.match(st) or re.search(r"\.{4,}", txt):
        continue
    for t in p.iter(W + "t"):
        if not t.text or dentro_de_citavi(t):
            continue
        orig = t.text
        s = orig
        # a) espacio sobrante antes de , ; .
        s2 = re.sub(r"[ \u00a0]+([,;])", r"\1", s)
        if s2 != s:
            log["espacio antes de coma o punto y coma"] += 1
            detalle["espacio antes de coma o punto y coma"].append((p_i, s[:75]))
            s = s2
        s2 = re.sub(r"[ \u00a0]+\.(?=\s|$)", ".", s)
        if s2 != s:
            log["espacio antes de punto"] += 1
            detalle["espacio antes de punto"].append((p_i, s[:75]))
            s = s2
        # b) falta el espacio despues de una coma
        s2 = re.sub(r",(?=[A-Za-zÁÉÍÓÚÑÜáéíóúñü])", ", ", s)
        if s2 != s:
            log["coma sin espacio detras"] += 1
            detalle["coma sin espacio detras"].append((p_i, s[:75]))
            s = s2
        # c) espacios dobles internos
        s2 = re.sub(r"(?<=\S)[ ]{2,}(?=\S)", " ", s)
        if s2 != s:
            log["espacios dobles"] += 1
            detalle["espacios dobles"].append((p_i, s[:75]))
            s = s2
        if s != orig:
            t.text = s
            if s != s.strip():
                t.set(XMLSPACE, "preserve")

# ---------- 1b. espacios dobles que cruzan dos runs ----------
# ("monitoreo:" en un run + "   En este paso" en el siguiente)
for p_i, p in enumerate(root.iter(W + "p")):
    st = estilo(p)
    if EXCLUIR_ESTILO.match(st) or re.search(r"\.{4,}", texto(p)):
        continue
    nodos = [t for t in p.iter(W + "t") if not dentro_de_citavi(t)]
    for prev, cur in zip(nodos, nodos[1:]):
        if not prev.text or not cur.text:
            continue
        # el anterior acaba en texto y el actual empieza con 2+ espacios
        if prev.text.rstrip() == prev.text and re.match(r"[ ]{2,}\S", cur.text):
            detalle["espacios dobles entre runs"].append((p_i, cur.text[:60]))
            cur.text = " " + cur.text.lstrip(" ")
            cur.set(XMLSPACE, "preserve")
            log["espacios dobles entre runs"] += 1
        # el anterior acaba en 2+ espacios y el actual empieza con texto
        elif re.search(r"\S[ ]{2,}$", prev.text) and cur.text[:1] not in (" ", ""):
            detalle["espacios dobles entre runs"].append((p_i, prev.text[-60:]))
            prev.text = prev.text.rstrip(" ") + " "
            prev.set(XMLSPACE, "preserve")
            log["espacios dobles entre runs"] += 1

# ---------- 2. FUENTE: todo Calibri ----------
for rpr in root.iter(W + "rPr"):
    rf = rpr.find(W + "rFonts")
    if rf is None:
        continue
    for at in ("ascii", "hAnsi", "cs", "eastAsia"):
        v = rf.get(W + at)
        if v and "Calibri" not in v:
            rf.set(W + at, "Calibri")
            log[f"fuente {v} -> Calibri"] += 1

# ---------- 3. NIVELES DE TITULO ----------
# 'Estudio Administrativo' es Heading 3 entre hermanos Heading 2
# El bloque de 'Notas Legales' usa Heading 4 colgando de un Heading 2
ARREGLOS = {
    "Estudio Administrativo": ("Ttulo3", "Ttulo2"),
    "Impuesto sobre la Renta (ISR).": ("Ttulo4", "Ttulo3"),
    "Depreciación y amortización.": ("Ttulo4", "Ttulo3"),
    "Seguridad social y aportes patronales.": ("Ttulo4", "Ttulo3"),
    "Prestaciones laborales.": ("Ttulo4", "Ttulo3"),
    "Impuesto sobre Ventas (ISV)": ("Ttulo4", "Ttulo3"),
    "Arrastre de pérdidas fiscales.": ("Ttulo4", "Ttulo3"),
    "Impuesto Municipal de Industria, Comercio y Servicios": ("Ttulo4", "Ttulo3"),
    "Capital de trabajo.": ("Ttulo4", "Ttulo3"),
}
for p in root.iter(W + "p"):
    txt = texto(p).strip()
    if txt not in ARREGLOS:
        continue
    desde, hacia = ARREGLOS[txt]
    pr = p.find(W + "pPr")
    if pr is None:
        continue
    stn = pr.find(W + "pStyle")
    if stn is None:
        continue
    actual = stn.get(W + "val") or ""
    if actual.replace("í", "").replace("Í", "") == desde or actual == desde:
        stn.set(W + "val", hacia)
        log[f"nivel de titulo {desde} -> {hacia}"] += 1
        detalle["niveles de titulo"].append((0, txt[:70]))

# ---------- guardar ----------
nuevo = etree.tostring(root, xml_declaration=True, encoding="UTF-8", standalone=True)
with zipfile.ZipFile(DST, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        datos = nuevo if item.filename == "word/document.xml" else zin.read(item.filename)
        zi = zipfile.ZipInfo(item.filename, date_time=item.date_time)
        zi.compress_type = item.compress_type
        zi.external_attr = item.external_attr
        zi.internal_attr = item.internal_attr
        zi.create_system = item.create_system
        zout.writestr(zi, datos)
zin.close()

print(f"Documento generado:\n  {DST}\n")
print("=" * 70)
print("CORRECCIONES APLICADAS")
print("=" * 70)
for k, v in log.most_common():
    print(f"  {v:4d}  {k}")
print(f"\n  TOTAL: {sum(log.values())} correcciones")

for k in ("espacio antes de coma o punto y coma", "espacio antes de punto",
          "coma sin espacio detras", "espacios dobles", "niveles de titulo"):
    if detalle[k]:
        print(f"\n--- {k} (primeros ejemplos) ---")
        for p_i, s in detalle[k][:6]:
            print(f"    {s!r}")
