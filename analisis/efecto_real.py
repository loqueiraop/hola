"""
¿Cuanto baja el porcentaje SOLO por el arreglo de comillas?

Depende de si esos 4 pasajes estaban resaltados en el informe. Si Turnitin no
los habia marcado contra ninguna fuente, excluirlos como cita no cambia nada.
"""
import json, docx, re, unicodedata
from docx.oxml.ns import qn

doc = docx.Document("/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 - REVISADA.docx")

def real(p):
    return "".join(t.text or "" for t in p._p.iter(qn('w:t')))

def norm(s):
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9 ]", " ", re.sub(r"\s+", " ", s)).strip()

data = json.load(open("/projects/sandbox/hola/analisis/matches.json"))
# todo el texto resaltado del informe, normalizado
resaltado = norm(" ".join(r["text"] for r in data["records"]))

CITAS = {
    234: "El estudio de prefactibilidad es un análisis en la etapa preliminar",
    243: "La ONUDI promueve el desarrollo industrial inclusivo y sostenible",
    250: "Conjunto de investigaciones teóricas y aplicadas que permiten generar",
    254: "Determina la factibilidad técnica del proyecto, al permitir diseñar",
}

print("=" * 84)
print("¿ESTABAN RESALTADAS LAS 4 CITAS QUE ARREGLE?")
print("=" * 84)
total_palabras_cita = 0
resaltadas = 0
for i, inicio in CITAS.items():
    t = real(doc.paragraphs[i])
    # extraer el contenido entre las comillas tipograficas
    m = re.search(r"\u201c(.+?)\u201d", t, re.S)
    contenido = m.group(1) if m else t
    nw = len(contenido.split())
    total_palabras_cita += nw
    # ¿aparece algun tramo de 6 palabras de la cita en el texto resaltado?
    w = norm(contenido).split()
    hit = None
    for k in range(0, max(1, len(w) - 6), 2):
        frag = " ".join(w[k:k + 6])
        if len(frag) > 25 and frag in resaltado:
            hit = frag
            break
    estado = "RESALTADA" if hit else "no marcada"
    if hit:
        resaltadas += 1
    print(f"\n  parrafo {i}  ({nw} palabras entre comillas)  ->  {estado}")
    print(f"    cita: {contenido[:95]}…")
    if hit:
        print(f"    coincide en el informe: “{hit}”")

print("\n" + "=" * 84)
print("EFECTO ESTIMADO DEL ARREGLO DE COMILLAS")
print("=" * 84)
TOT = 52958
print(f"  palabras dentro de las 4 citas:        {total_palabras_cita}")
print(f"  de las 4 citas, resaltadas:            {resaltadas} de 4")
print(f"  si Turnitin las excluye todas:         {total_palabras_cita/TOT*100:.2f} puntos")
print(f"  resultado esperado:                    23,0 %  ->  ~{23 - total_palabras_cita/TOT*100:.1f} %")
