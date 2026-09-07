import fitz, collections, json

d = fitz.open("/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18.pdf")

def is_badge(r):
    return r.x1 < 40 and (r.x1 - r.x0) < 30

TOTAL_PALABRAS = 52958
per_page = {}
total_words_doc = 0
for pno in range(37, d.page_count):
    page = d[pno]
    words = [w for w in page.get_text("words") if not is_badge(fitz.Rect(w[:4]))]
    # descartar pie de pagina
    words = [w for w in words if w[1] < page.rect.height - 60]
    rects = [x["rect"] for x in page.get_drawings()
             if x["type"] == "f" and x.get("fill") and not is_badge(x["rect"])]
    hl = set()
    for i, w in enumerate(words):
        wr = fitz.Rect(w[:4])
        for r in rects:
            rr = fitz.Rect(r.x0 - 1, r.y0 + 1, r.x1 + 1, r.y1 - 1)
            if wr.intersects(rr):
                hl.add(i)
                break
    per_page[pno - 36] = (len(hl), len(words))   # pagina de tesis
    total_words_doc += len(words)

tot_hl = sum(v[0] for v in per_page.values())
print(f"palabras del cuerpo detectadas en PDF: {total_words_doc}")
print(f"palabras resaltadas (unicas): {tot_hl}")
print(f"=> {tot_hl/TOTAL_PALABRAS*100:.1f}% sobre las {TOTAL_PALABRAS} palabras que reporta Turnitin")
print(f"   (Turnitin reporta 23%. Objetivo <=20% => hay que eliminar/blindar ~{tot_hl - int(0.20*TOTAL_PALABRAS)} palabras coincidentes)\n")

ZONAS = [
    ("Portada, indices, resumen, introduccion, objetivos, justificacion", 1, 18),
    ("MARCO TEORICO (metodologias ONUDI/JICA/ZOPP/Marco Logico/Sapag + estudios)", 19, 39),
    ("MARCO TEORICO leche + DISENO METODOLOGICO", 40, 52),
    ("Estudio sectorial (desarrollo)", 53, 71),
    ("Estudio de mercado (desarrollo)", 72, 97),
    ("Estudio tecnico (desarrollo)", 98, 127),
    ("Estudio organizacional/administrativo", 128, 134),
    ("ESTUDIO LEGAL (desarrollo)", 135, 146),
    ("ESTUDIO AMBIENTAL (desarrollo)", 147, 154),
    ("Estudio financiero: flujos y costos", 155, 177),
    ("INDICADORES FINANCIEROS VAN/TIR/PRI + notas legales", 178, 201),
    ("Analisis de riesgos (Monte Carlo)", 202, 244),
    ("Conclusiones y recomendaciones", 245, 249),
    ("Bibliografia", 250, 255),
    ("Anexos", 256, 260),
]

print(f"{'ZONA':<74} {'pags':>9} {'coinc.':>7} {'% del doc':>10} {'% de la zona':>13}")
print("-" * 118)
rows = []
for nombre, a, b in ZONAS:
    h = sum(per_page.get(p, (0, 0))[0] for p in range(a, b + 1))
    w = sum(per_page.get(p, (0, 0))[1] for p in range(a, b + 1))
    rows.append((nombre, a, b, h, w))
    print(f"{nombre[:73]:<74} {f'{a}-{b}':>9} {h:>7} {h/TOTAL_PALABRAS*100:>9.1f}% "
          f"{(h/w*100 if w else 0):>12.1f}%")

print("\n\n=== PAGINAS MAS DENSAS (>=25% de la pagina coincidente y >=60 palabras) ===")
dens = [(p, h, w, h / w) for p, (h, w) in per_page.items() if w > 100 and h >= 60 and h / w >= .25]
for p, h, w, r in sorted(dens, key=lambda x: -x[3])[:35]:
    print(f"  tesis pag {p:3d} (PDF {p+37:3d}): {h:3d}/{w:3d} palabras = {r*100:4.1f}% de la pagina")

json.dump({str(k): v for k, v in per_page.items()},
          open("/projects/sandbox/hola/analisis/por_pagina.json", "w"))
