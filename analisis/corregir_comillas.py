"""
Corrige los 7 caracteres ¨ (U+00A8, dieresis) sustituyendolos por comillas
tipograficas correctas “ ”, para que Turnitin reconozca las citas textuales.

Metodo conservador:
  - Se lee el .docx como ZIP y se reescribe entrada por entrada.
  - SOLO se toca word/document.xml. Todo lo demas (imagenes, estilos, campos
    de Citavi, numeracion, encabezados) se copia byte a byte sin abrirlo.
  - Dentro de document.xml solo se modifica el TEXTO de los nodos w:t que
    contienen ¨. No se añade, borra ni reordena ningun elemento XML.
  - El archivo original no se modifica: se escribe una copia nueva.

Regla de apertura/cierre: si el parrafo tiene dos ¨, el primero abre y el
segundo cierra. Si tiene uno solo, es el que abre (el cierre ya existe como ”).
"""
import zipfile, shutil, re
from lxml import etree

ORIG = "/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 (1).docx"
DEST = "/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 - comillas corregidas.docx"
W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
DIERESIS = "\u00a8"

zin = zipfile.ZipFile(ORIG)
doc_xml = zin.read("word/document.xml")
root = etree.fromstring(doc_xml)

cambios = []
for p_i, p in enumerate(root.iter(W + "p")):
    nodos = [t for t in p.iter(W + "t")]
    completo = "".join(t.text or "" for t in nodos)
    n = completo.count(DIERESIS)
    if n == 0:
        continue
    antes = completo
    # orden de sustitucion para este parrafo
    if n >= 2:
        reemplazos = ["\u201c"] + ["\u201d"] * (n - 1)
    else:
        reemplazos = ["\u201c"]          # el cierre ya existe como ”
    k = 0
    for t in nodos:
        if not t.text or DIERESIS not in t.text:
            continue
        s = t.text
        salida = []
        for ch in s:
            if ch == DIERESIS:
                salida.append(reemplazos[k]); k += 1
            else:
                salida.append(ch)
        s = "".join(salida)
        # Si la comilla de apertura quedo pegada a la palabra anterior y con un
        # espacio detras ( tecnico“ Determina ), se mueve el espacio delante.
        s = re.sub(r"(?<=\w)\u201c ", " \u201c", s)
        t.text = s
    despues = "".join(t.text or "" for t in nodos)
    cambios.append((p_i, n, antes, despues))

nuevo_xml = etree.tostring(root, xml_declaration=True,
                           encoding="UTF-8", standalone=True)

# Reescribir el ZIP preservando todo lo demas
with zipfile.ZipFile(DEST, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        datos = zin.read(item.filename)
        if item.filename == "word/document.xml":
            datos = nuevo_xml
        zi = zipfile.ZipInfo(item.filename, date_time=item.date_time)
        zi.compress_type = item.compress_type
        zi.external_attr = item.external_attr
        zi.internal_attr = item.internal_attr
        zi.create_system = item.create_system
        zout.writestr(zi, datos)
zin.close()

print(f"Copia creada: {DEST}\n")
print(f"Parrafos modificados: {len(cambios)}")
for p_i, n, a, d in cambios:
    print(f"\n--- parrafo XML #{p_i} · {n} caracteres ¨ ---")
    for etiqueta, txt in (("ANTES ", a), ("DESPUES", d)):
        frag = txt[:150] + (" […] " + txt[-90:] if len(txt) > 240 else "")
        print(f"  {etiqueta}: {frag}")
