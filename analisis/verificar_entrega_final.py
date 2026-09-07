#!/usr/bin/env python3
"""Validación estructural, editorial y de render de la entrega final."""
from __future__ import annotations

import hashlib
import json
import posixpath
import re
import zipfile
from pathlib import Path

import fitz
from docx import Document
from lxml import etree

ROOT = Path("/projects/sandbox/hola")
ORIGINAL = ROOT / "Tesis Jaime Fredy Horacio Avance 18 (1).docx"
FINAL = ROOT / "Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.docx"
PDF = ROOT / "analisis/render/Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.pdf"
OUT = ROOT / "analisis/verificacion_entrega_final.json"
W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
A = "{http://schemas.openxmlformats.org/drawingml/2006/main}"
R = "{http://schemas.openxmlformats.org/package/2006/relationships}"


def sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def text(node: etree._Element) -> str:
    return "".join(t.text or "" for t in node.iter(W + "t"))


def style(p: etree._Element) -> str:
    value = p.find("./" + W + "pPr/" + W + "pStyle")
    return value.get(W + "val", "") if value is not None else ""


def sdt_tag(sdt: etree._Element) -> str:
    value = sdt.find("./" + W + "sdtPr/" + W + "tag")
    return value.get(W + "val", "") if value is not None else ""


def normalized(value: str) -> str:
    return " ".join(re.findall(r"\w+", value.casefold(), flags=re.UNICODE))


def literal_index_page(p: etree._Element) -> int:
    for node in reversed(list(p.iter(W + "t"))):
        value = (node.text or "").strip()
        if value.isdigit():
            return int(value)
    raise RuntimeError(text(p))


def relationship_source(rels_name: str) -> str:
    if rels_name == "_rels/.rels":
        return ""
    parent, filename = rels_name.rsplit("/_rels/", 1)
    if not filename.endswith(".rels"):
        raise ValueError(rels_name)
    return posixpath.join(parent, filename[:-5])


def main() -> int:
    checks: dict[str, bool] = {}
    details: dict[str, object] = {}

    with zipfile.ZipFile(ORIGINAL) as original, zipfile.ZipFile(FINAL) as final:
        checks["zip_integrity"] = final.testzip() is None
        names = final.namelist()
        checks["package_parts_196"] = len(names) == 196
        xml_errors = []
        for name in names:
            if name.endswith((".xml", ".rels")):
                try:
                    etree.fromstring(final.read(name))
                except Exception as exc:
                    xml_errors.append({"part": name, "error": str(exc)})
        checks["all_xml_parts_parse"] = not xml_errors
        details["xml_errors"] = xml_errors

        broken_relationships = []
        names_set = set(names)
        for rels_name in [name for name in names if name.endswith(".rels")]:
            rels = etree.fromstring(final.read(rels_name))
            source = relationship_source(rels_name)
            base = posixpath.dirname(source)
            for rel in rels.iter(R + "Relationship"):
                if rel.get("TargetMode") == "External":
                    continue
                target = rel.get("Target", "")
                resolved = posixpath.normpath(posixpath.join(base, target)).lstrip("/")
                if resolved not in names_set:
                    broken_relationships.append({"rels": rels_name, "target": target, "resolved": resolved})
        checks["all_internal_relationship_targets_exist"] = not broken_relationships
        details["broken_relationships"] = broken_relationships

        root = etree.fromstring(final.read("word/document.xml"))
        original_root = etree.fromstring(original.read("word/document.xml"))
        styles = etree.fromstring(final.read("word/styles.xml"))
        body = root.find(".//" + W + "body")
        paragraphs = body.findall("./" + W + "p") if body is not None else []
        all_text = "\n".join(text(p) for p in paragraphs)
        all_document_text = text(root)

        sdts = list(root.iter(W + "sdt"))
        citations = [s for s in sdts if sdt_tag(s).startswith("CitaviPlaceholder#")]
        bibliographies = [s for s in sdts if sdt_tag(s) == "CitaviBibliography"]
        citation_tags = [sdt_tag(s) for s in citations]
        non_sdt_citavi = [
            instr for instr in root.iter(W + "instrText")
            if "CitaviPlaceholder" in (instr.text or "") and not any(a.tag == W + "sdt" for a in instr.iterancestors())
        ]
        bibliography_entries = [
            p for p in root.iter(W + "p")
            if style(p) == "CitaviBibliographyEntry" and text(p).strip()
        ]
        checks.update({
            "sdt_total_80": len(sdts) == 80,
            "citavi_citations_75": len(citations) == 75,
            "citavi_tags_unique_75": len(set(citation_tags)) == 75,
            "citavi_bibliography_1": len(bibliographies) == 1,
            "citavi_non_sdt_field_1": len(non_sdt_citavi) == 1,
            "citavi_visible_results_75": sum(bool(text(s).strip()) for s in citations) == 75,
            "bibliography_entries_58": len(bibliography_entries) == 58,
        })

        original_instr = [instr.text or "" for instr in original_root.iter(W + "instrText") if "Citavi" in (instr.text or "")]
        final_instr = [instr.text or "" for instr in root.iter(W + "instrText") if "Citavi" in (instr.text or "")]
        checks["citavi_instruction_payloads_preserved"] = original_instr == final_instr

        sierra = next(s for s in citations if sdt_tag(s) == "CitaviPlaceholder#be2ccc5c-f34f-472b-91d4-0374ded4c98f")
        result_text = []
        active = False
        for child in sierra.find("./" + W + "sdtContent"):
            fld = child.find(".//" + W + "fldChar")
            if fld is not None and fld.get(W + "fldCharType") == "separate":
                active = True
                continue
            if fld is not None and fld.get(W + "fldCharType") == "end":
                break
            if active:
                result_text.extend(t.text or "" for t in child.iter(W + "t"))
        checks["sierra_result_inside_field"] = "".join(result_text) == "(Sierra et al 2023)"

        media = [name for name in names if name.startswith("word/media/") and not name.endswith("/")]
        media_unchanged = all(name in original.namelist() and sha256(original.read(name)) == sha256(final.read(name)) for name in media)
        checks.update({
            "tables_56": len(list(root.iter(W + "tbl"))) == 56,
            "drawings_55": len(list(root.iter(W + "drawing"))) == 55,
            "media_parts_33": len(media) == 33,
            "media_byte_identical_to_original": media_unchanged,
        })

        explicit_font_errors = []
        for part_name, part in (("document", root), ("styles", styles)):
            for fonts in part.iter(W + "rFonts"):
                for attr in ("ascii", "hAnsi", "cs", "eastAsia"):
                    value = fonts.get(W + attr)
                    if value and value.casefold() != "calibri":
                        explicit_font_errors.append({"part": part_name, "attribute": attr, "value": value})
        theme_font_errors = []
        for name in [n for n in names if n.startswith("word/theme/theme") and n.endswith(".xml")]:
            theme = etree.fromstring(final.read(name))
            for latin in theme.iter(A + "latin"):
                value = latin.get("typeface", "")
                if value.casefold() != "calibri":
                    theme_font_errors.append({"part": name, "value": value})
        checks["document_and_styles_explicit_fonts_calibri"] = not explicit_font_errors
        checks["all_theme_latin_fonts_calibri"] = not theme_font_errors
        details["font_errors"] = explicit_font_errors + theme_font_errors

        index_paragraphs = [p for p in root.iter(W + "p") if style(p).startswith("ndicemanual")]
        checks["manual_index_entries_176"] = len(index_paragraphs) == 176
        details["manual_index_page_values"] = [literal_index_page(p) for p in index_paragraphs]

        table_numbers = sorted({
            int(match.group(1))
            for p in root.iter(W + "p")
            if not style(p).startswith("ndicemanual")
            for match in [re.match(r"^Tabla\s+(\d+)\b", text(p).strip())]
            if match
        })
        figure_numbers = sorted({
            int(match.group(1))
            for p in root.iter(W + "p")
            if not style(p).startswith("ndicemanual")
            for match in [re.match(r"^Figura\s+(\d+)\b", text(p).strip())]
            if match
        })
        checks["table_sequence_1_to_37"] = table_numbers == list(range(1, 38))
        checks["figure_sequence_1_to_54"] = figure_numbers == list(range(1, 55))

        checks.update({
            "sample_60_claim_absent": "muestra de 60 establecimientos" not in all_text,
            "sample_66_claim_absent": "66 establecimientos" not in all_text,
            "sample_63_documented": all_text.count("63 establecimientos") >= 3,
            "five_percent_claim_removed": "margen de error del 5%" not in all_text,
            "probabilistic_limitation_present": "no se les atribuye un margen de error probabilístico" in all_text,
            "citation_double_period_absent": re.search(r"\)\.\s*\.", all_document_text) is None,
            "old_title_phrase_absent": "leche pasteurizada a mercado nicho" not in all_text.casefold(),
            "corrected_title_present": "leche pasteurizada para un mercado de nicho" in all_text.casefold(),
            "known_bibliography_artifacts_absent": not any(value in all_document_text for value in ["ARSA, del #1", "Guia Ayuda", "Solicitud Para Licencia", "Planficación", "!:eche", "metodología JICA,.", "www.bing.com/ck/"]),
        })

    # python-docx opening is an independent OPC sanity check.
    opened = Document(FINAL)
    checks["python_docx_opens"] = len(opened.paragraphs) > 0

    pdf = fitz.open(PDF)
    checks["pdf_opens_253_pages"] = len(pdf) == 253
    pdf_text = "\n".join(page.get_text("text") for page in pdf)
    pdf_norm = normalized(pdf_text)
    checks["all_75_citation_results_visible_in_pdf"] = all(normalized(text(s)) in pdf_norm for s in citations)

    rendered_index_rows = []
    for page_index in range(1, 10):
        page = pdf[page_index]
        words = page.get_text("words")
        for link in page.get_links():
            target = link.get("page", -1)
            if target is None or target < 0:
                continue
            line = " ".join(w[4] for w in words if fitz.Rect(w[:4]).intersects(link["from"])).strip()
            match = re.search(r"(\d+)\s*$", line)
            if match:
                rendered_index_rows.append((int(match.group(1)), int(target) + 1))
    checks["pdf_index_links_176"] = len(rendered_index_rows) == 176
    checks["pdf_index_visible_numbers_match_targets"] = all(visible == target for visible, target in rendered_index_rows)

    expected_headings = {
        "Resumen": 11,
        "Abstract": 12,
        "Introducción": 13,
        "Marco Teórico": 20,
        "DESARROLLO DEL ESTUDIO": 47,
        "Conclusiones": 239,
        "Recomendaciones": 241,
        "Bibliografía": 243,
        "Anexos": 249,
    }
    heading_pages = {}
    for heading, expected_page in expected_headings.items():
        found = []
        for index, page in enumerate(pdf):
            lines = [line.strip() for line in page.get_text("text").splitlines() if line.strip()]
            if any(line.casefold() == heading.casefold() for line in lines):
                found.append(index + 1)
        heading_pages[heading] = found
        checks[f"heading_{heading}_page_{expected_page}"] = expected_page in found
    details["heading_pages"] = heading_pages

    failed = [name for name, ok in checks.items() if not ok]
    report = {
        "final_docx": str(FINAL),
        "final_pdf_render": str(PDF),
        "original_sha256": sha256(ORIGINAL.read_bytes()),
        "final_sha256": sha256(FINAL.read_bytes()),
        "final_size_bytes": FINAL.stat().st_size,
        "checks_passed": sum(checks.values()),
        "checks_total": len(checks),
        "all_ok": not failed,
        "failed_checks": failed,
        "checks": checks,
        "details": details,
    }
    OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: report[k] for k in ("final_docx", "final_sha256", "final_size_bytes", "checks_passed", "checks_total", "all_ok", "failed_checks")}, ensure_ascii=False, indent=2))
    return 0 if not failed else 1


if __name__ == "__main__":
    raise SystemExit(main())
