"""
¿Turnitin leyo las citas de Citavi?

Las citas viven en campos (w:sdt / ADDIN CitaviPlaceholder). Si el conversor de
Turnitin no leyera esos campos, las citas NO aparecerian en el PDF del informe y
el filtro de "texto citado" no habria podido descontar nada.

Se comprueba buscando en el texto del PDF del informe las mismas 84 citas
detectadas en el .docx.
"""
import fitz, docx, re
from docx.oxml.ns import qn

PDF = "/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18.pdf"
DOCX = "/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 (1).docx"

# --- texto del cuerpo del informe (paginas 38-297) ---
d = fitz.open(PDF)
pdf_txt = []
for i in range(37, d.page_count):
    t = d[i].get_text()
    t = re.sub(r"Página \d+ de 297[^\n]*|Identificador de la entrega|trn:oid[^\s]*", " ", t)
    pdf_txt.append(t)
pdf_txt = re.sub(r"\s+", " ", " ".join(pdf_txt))

# --- citas del docx ---
doc = docx.Document(DOCX)
MAY, MIN = "A-ZÁÉÍÓÚÑ", "a-záéíóúñ"
CITA_PAREN = re.compile(
    rf"\([^()\n]{{0,80}}?[{MAY}][^()\n]{{1,80}}?,?\s*(?:19|20)\d{{2}}[a-z]?"
    rf"(?:\s*[-–]\s*\d{{2,4}})?\s*(?:,\s*p+\.?\s*[\d\-]+)?\)")

citas = []
for i, p in enumerate(doc.paragraphs):
    real = "".join(t.text or "" for t in p._p.iter(qn('w:t')))
    plano = p.text                       # sin los campos de Citavi
    for m in CITA_PAREN.finditer(real):
        c = m.group(0)
        # solo las que estan DENTRO de un campo (no en el texto plano)
        if c not in plano:
            citas.append((i, c))

print(f"citas que solo existen dentro de campos de Citavi: {len(citas)}\n")

def norm(s):
    return re.sub(r"\s+", " ", s).strip()

hallada = perdida = 0
faltantes = []
for i, c in citas:
    if norm(c) in pdf_txt:
        hallada += 1
    else:
        # probar sin espacios dobles internos
        alt = re.sub(r"\s+", " ", c)
        if alt in pdf_txt:
            hallada += 1
        else:
            perdida += 1
            faltantes.append((i, c))

print("=" * 72)
print(f"CITAS PRESENTES en el PDF que analizo Turnitin : {hallada}")
print(f"CITAS AUSENTES del PDF                         : {perdida}")
print("=" * 72)
if faltantes:
    print("\nNo localizadas literalmente (revisar a mano):")
    for i, c in faltantes:
        print(f"  parrafo {i}: {c[:70]}")

# Comprobacion puntual: el parrafo 208 y su cita FAOSTAT
print("\n--- comprobacion directa, parrafo 208 ---")
for clave in ("De acuerdo con datos de", "FAOSTAT 2022"):
    print(f"  {clave!r} en el PDF de Turnitin: {clave in pdf_txt}")

# ¿Aparece la bibliografia generada por Citavi?
print("\n--- bibliografia generada por Citavi ---")
for clave in ("Nassir Sapag Chain (2008)", "Franco, María Adelaida", "ALNAP (2023)"):
    print(f"  {clave!r} en el PDF: {clave in pdf_txt}")
