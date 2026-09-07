import json, re, collections, unicodedata
import docx

DOCX = "/projects/sandbox/hola/Tesis Jaime Fredy Horacio Avance 18 (1).docx"
doc = docx.Document(DOCX)

paras = []
for i, p in enumerate(doc.paragraphs):
    paras.append({"idx": i, "style": p.style.name, "text": p.text.strip()})

# outline de titulos
outline = [p for p in paras if p["style"].lower().startswith(("heading", "título", "titulo"))
           and p["text"]]
print("=== ESTRUCTURA (titulos) ===")
for h in outline:
    lvl = re.sub(r"\D", "", h["style"]) or "1"
    print(f"{'  '*(int(lvl)-1)}[{h['idx']:5d}] {h['text'][:90]}")

def norm(s):
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9 ]", " ", s)

# indice de busqueda
full = []
for p in paras:
    full.append(norm(p["text"]))

data = json.load(open("/projects/sandbox/hola/analisis/matches.json"))

def find_para(snippet):
    """busca el parrafo del docx que contiene una secuencia de 6 palabras del snippet"""
    w = norm(snippet).split()
    for start in range(0, max(1, len(w) - 6), 3):
        needle = " ".join(w[start:start + 6])
        if len(needle) < 25:
            continue
        for j, t in enumerate(full):
            if needle in t:
                return j
    return None

hits = []
for r in data["records"]:
    j = find_para(r["text"][:250])
    r["docx_para"] = j
    hits.append(r)

json.dump(data, open("/projects/sandbox/hola/analisis/matches.json", "w"),
          ensure_ascii=False, indent=1)

ok = sum(1 for r in hits if r["docx_para"] is not None)
print(f"\nbloques localizados en el docx: {ok}/{len(hits)}")

# agrupar por seccion
def section_of(j):
    best = None
    for h in outline:
        if h["idx"] <= j:
            best = h
        else:
            break
    return best["text"] if best else "(sin titulo)"

sec = collections.Counter()
for r in hits:
    if r["docx_para"] is not None:
        sec[section_of(r["docx_para"])] += r["words"]
print("\n=== PALABRAS RESALTADAS POR SECCION ===")
for s, w in sec.most_common(30):
    print(f"  {w:5d}  {s[:80]}")
