import fitz, re, json, collections

d = fitz.open("/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18.pdf")

# 1) Catalogo de fuentes (paginas 3-37)
sources = {}
buf = []
for i in range(2, 37):
    for line in d[i].get_text().split("\n"):
        line = line.strip()
        if not line or "de 297 -" in line or "trn:oid" in line:
            continue
        buf.append(line)
cur = None
pend = []
for line in buf:
    if re.fullmatch(r"\d{1,3}", line) and (cur is None or line != cur):
        if cur is not None:
            sources[int(cur)] = " ".join(pend)
        cur = line
        pend = []
    else:
        pend.append(line)
if cur is not None:
    sources[int(cur)] = " ".join(pend)

# 2) Resaltados en el cuerpo (paginas 38-297)
def is_badge(r):
    return r.x1 < 40 and (r.x1 - r.x0) < 30

records = []
for pno in range(37, d.page_count):
    page = d[pno]
    drs = [x for x in page.get_drawings() if x["type"] == "f" and x.get("fill")]
    badges = []
    hl = []
    for x in drs:
        r = x["rect"]
        if is_badge(r):
            badges.append((r, tuple(round(c, 3) for c in x["fill"])))
        else:
            hl.append((r, tuple(round(c, 3) for c in x["fill"])))
    # numero dentro de cada badge
    words = page.get_text("words")
    badge_info = []
    for r, col in badges:
        txt = "".join(w[4] for w in words if fitz.Rect(w[:4]).intersects(r))
        badge_info.append((r, col, txt.strip()))
    # agrupar resaltados por color
    bycol = collections.defaultdict(list)
    for r, col in hl:
        bycol[col].append(r)
    for col, rects in bycol.items():
        rects.sort(key=lambda r: (round(r.y0), r.x0))
        frag = []
        for r in rects:
            rr = fitz.Rect(r.x0 - 1, r.y0 + 1, r.x1 + 1, r.y1 - 1)
            t = " ".join(w[4] for w in words
                         if fitz.Rect(w[:4]).intersects(rr)
                         and not is_badge(fitz.Rect(w[:4])))
            frag.append(t)
        text = re.sub(r"\s+", " ", " ".join(frag)).strip()
        # buscar badge cercano verticalmente
        top = min(r.y0 for r in rects)
        num = None
        best = 1e9
        for br, bcol, btxt in badge_info:
            dist = abs(br.y0 - top)
            if dist < best and dist < 8 and btxt.isdigit():
                best, num = dist, int(btxt)
        records.append({
            "pdf_page": pno + 1,
            "source_no": num,
            "source": sources.get(num, "?"),
            "words": len(text.split()),
            "text": text,
        })

json.dump({"sources": sources, "records": records},
          open("/projects/sandbox/hola/analisis/matches.json", "w"),
          ensure_ascii=False, indent=1)

print("fuentes catalogadas:", len(sources))
print("bloques resaltados:", len(records))
print("palabras resaltadas (aprox):", sum(r["words"] for r in records))
