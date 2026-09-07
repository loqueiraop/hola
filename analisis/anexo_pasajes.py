import json

d = json.load(open("/projects/sandbox/hola/analisis/matches.json"))
recs = [r for r in d["records"] if r["words"] >= 25]
recs.sort(key=lambda r: (r["pdf_page"], -r["words"]))

out = ["# Anexo: pasajes coincidentes detectados en el informe Turnitin",
       "",
       "Extraido automaticamente de los resaltados del PDF del informe.",
       "`Pag. tesis` = numeracion impresa de la tesis. `Pag. PDF` = pagina del archivo del informe.",
       "`Parrafo` = indice del parrafo en el .docx (0-based, contando solo parrafos de nivel superior).",
       "Solo se listan bloques de 25 palabras o mas.",
       "",
       f"Total de bloques listados: {len(recs)}",
       ""]

for r in recs:
    tp = r["pdf_page"] - 37
    src = r["source"] if r["source"] != "?" else "(fuente no identificada en el margen)"
    out.append(f"### Pag. tesis {tp} · Pag. PDF {r['pdf_page']} · {r['words']} palabras")
    out.append(f"- **Fuente Turnitin:** #{r['source_no']} — {src[:150]}")
    out.append(f"- **Parrafo .docx:** {r.get('docx_para')}")
    out.append(f"- **Texto coincidente:** {r['text'][:700]}")
    out.append("")

open("/projects/sandbox/hola/analisis/ANEXO_PASAJES.md", "w").write("\n".join(out))
print("bloques listados:", len(recs))
