#!/usr/bin/env python3
"""Actualiza las páginas literales de los cuatro índices manuales desde un PDF renderizado.

La asociación se obtiene de los destinos reales de los hipervínculos exportados por
LibreOffice. Sólo se modifica el último w:t numérico de cada párrafo de índice.
"""
from __future__ import annotations

import copy
import hashlib
import json
import re
import zipfile
from pathlib import Path

import fitz
from lxml import etree

ROOT = Path("/projects/sandbox/hola")
SRC = ROOT / "Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA APA7 BORRADOR.docx"
PDF = ROOT / "analisis/render/Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA APA7 BORRADOR.pdf"
DST = ROOT / "Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.docx"
REPORT = ROOT / "analisis/informe_indices_apa7_final.json"

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
PARSER = etree.XMLParser(remove_blank_text=False, resolve_entities=False, huge_tree=True)
EXPECTED_INDEX_COUNTS = {
    # Incluye 19 entradas de contenido y 2 entradas del índice de anexos.
    "ndicemanualnivel1": 21,
    "ndicemanualnivel2": 53,
    "ndicemanualnivel3": 11,
    "ndicemanualtabla": 37,
    "ndicemanualfigura": 54,
}
EXPECTED_TOTAL = 176


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def parse(data: bytes) -> etree._Element:
    return etree.fromstring(data, parser=PARSER)


def serialize(root: etree._Element) -> bytes:
    return etree.tostring(root, xml_declaration=True, encoding="UTF-8", standalone=True)


def text(node: etree._Element) -> str:
    return "".join(t.text or "" for t in node.iter(W + "t"))


def style(p: etree._Element) -> str:
    s = p.find("./" + W + "pPr/" + W + "pStyle")
    return s.get(W + "val", "") if s is not None else ""


def literal_page(p: etree._Element) -> int:
    for node in reversed(list(p.iter(W + "t"))):
        value = (node.text or "").strip()
        if re.fullmatch(r"\d+", value):
            return int(value)
    raise RuntimeError(f"Párrafo sin nodo de página literal: {text(p)!r}")


def pdf_index_destinations() -> list[dict[str, object]]:
    doc = fitz.open(PDF)
    rows: list[dict[str, object]] = []
    # Los índices ocupan las páginas PDF 2-10. Sólo la última línea de cada
    # entrada envuelta contiene el número literal; esto elimina enlaces duplicados.
    for page_index in range(1, min(10, len(doc))):
        page = doc[page_index]
        words = page.get_text("words")
        links = sorted(page.get_links(), key=lambda item: (item["from"].y0, item["from"].x0))
        for link in links:
            target = link.get("page", -1)
            if target is None or target < 0:
                continue
            line = " ".join(
                word[4] for word in words if fitz.Rect(word[:4]).intersects(link["from"])
            ).strip()
            match = re.search(r"(\d+)\s*$", line)
            if not match:
                continue
            rows.append({
                "index_pdf_page": page_index + 1,
                "rendered_line": line,
                "old_visible_page": int(match.group(1)),
                "target_pdf_page": int(target) + 1,
            })
    if len(rows) != EXPECTED_TOTAL:
        raise RuntimeError(f"Se esperaban {EXPECTED_TOTAL} destinos y se obtuvieron {len(rows)}")
    return rows


def main() -> int:
    if not SRC.exists() or not PDF.exists():
        raise FileNotFoundError(f"Falta fuente o PDF: {SRC} / {PDF}")
    destinations = pdf_index_destinations()

    with zipfile.ZipFile(SRC, "r") as zin:
        infos = zin.infolist()
        original = {info.filename: zin.read(info.filename) for info in infos}
        root = parse(original["word/document.xml"])
        paragraphs = [p for p in root.iter(W + "p") if style(p).startswith("ndicemanual")]
        if len(paragraphs) != EXPECTED_TOTAL:
            raise RuntimeError(f"Se esperaban {EXPECTED_TOTAL} párrafos de índice y se obtuvieron {len(paragraphs)}")

        counts: dict[str, int] = {}
        updates: list[dict[str, object]] = []
        for p, destination in zip(paragraphs, destinations):
            st = style(p)
            counts[st] = counts.get(st, 0) + 1
            before = text(p)
            text_nodes = list(p.iter(W + "t"))
            page_node = None
            old_page = None
            for candidate in reversed(text_nodes):
                candidate_value = (candidate.text or "").strip()
                if re.fullmatch(r"\d+", candidate_value):
                    page_node = candidate
                    old_page = int(candidate_value)
                    break
            if page_node is None or old_page is None:
                raise RuntimeError(f"Entrada sin nodo de página literal: {before!r}")
            if old_page != destination["old_visible_page"]:
                raise RuntimeError(
                    f"Desalineación DOCX/PDF: {before!r}; DOCX={old_page}; PDF={destination['old_visible_page']}"
                )
            target_page = int(destination["target_pdf_page"])
            page_node.text = str(target_page)
            after = text(p)
            updates.append({
                "style": st,
                "before": before,
                "after": after,
                "old_page": old_page,
                "new_page": target_page,
                "changed": old_page != target_page,
            })

        if counts != EXPECTED_INDEX_COUNTS:
            raise RuntimeError(f"Conteos de índice inesperados: {counts}")

        modified = dict(original)
        modified["word/document.xml"] = serialize(root)
        temp = DST.with_suffix(DST.suffix + ".tmp")
        with zipfile.ZipFile(temp, "w") as zout:
            zout.comment = zin.comment
            for info in infos:
                zout.writestr(copy.copy(info), modified[info.filename])
        temp.replace(DST)

    with zipfile.ZipFile(SRC, "r") as za, zipfile.ZipFile(DST, "r") as zb:
        names_a, names_b = za.namelist(), zb.namelist()
        changed_parts = [name for name in names_a if sha256(za.read(name)) != sha256(zb.read(name))]
        out = parse(zb.read("word/document.xml"))
        out_paragraphs = [p for p in out.iter(W + "p") if style(p).startswith("ndicemanual")]
        visible = [literal_page(p) for p in out_paragraphs]
        expected = [int(row["target_pdf_page"]) for row in destinations]
        validations = {
            "zip_valid": zb.testzip() is None,
            "part_names_and_order_preserved": names_a == names_b,
            "only_document_xml_changed": changed_parts == ["word/document.xml"],
            "index_count_176": len(out_paragraphs) == EXPECTED_TOTAL,
            "all_visible_pages_equal_rendered_targets": visible == expected,
        }

    report = {
        "source": str(SRC),
        "rendered_pdf": str(PDF),
        "destination": str(DST),
        "source_sha256": sha256(SRC.read_bytes()),
        "destination_sha256": sha256(DST.read_bytes()),
        "counts": counts,
        "entries": len(updates),
        "changed_entries": sum(1 for update in updates if update["changed"]),
        "unchanged_entries": sum(1 for update in updates if not update["changed"]),
        "updates": updates,
        "changed_package_parts": changed_parts,
        "validations": validations,
        "all_ok": all(validations.values()),
    }
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    if not report["all_ok"]:
        raise RuntimeError(f"Falló la validación: {validations}")
    print(json.dumps({
        "document": str(DST),
        "report": str(REPORT),
        "entries": len(updates),
        "changed_entries": report["changed_entries"],
        "validations": validations,
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
