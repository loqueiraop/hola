#!/usr/bin/env python3
"""Estima el efecto de las correcciones sobre el informe Turnitin original.

No sustituye un nuevo análisis Turnitin: compara los pasajes resaltados del PDF
original con el texto del DOCX final y calibra el cambio contra el 23 % oficial.
"""
from __future__ import annotations

import json
import re
import zipfile
from pathlib import Path

from lxml import etree

ROOT = Path("/projects/sandbox/hola")
MATCHES = ROOT / "analisis/matches.json"
CORRECTION_REPORT = ROOT / "analisis/informe_correccion_tesis_final.json"
FINAL = ROOT / "Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.docx"
OUT = ROOT / "analisis/estimacion_similitud_final.json"
W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
OFFICIAL_ORIGINAL_PERCENT = 23.0
TURNITIN_WORDS = 52958
UNIQUE_HIGHLIGHT_WORDS_EXTRACTED = 14523
HIGHLIGHT_EXTRACTION_PERCENT = 27.4


def normalize(value: str) -> str:
    return " ".join(re.findall(r"\w+", value.casefold(), flags=re.UNICODE))


def main() -> int:
    matches = json.loads(MATCHES.read_text(encoding="utf-8"))["records"]
    correction = json.loads(CORRECTION_REPORT.read_text(encoding="utf-8"))
    changed_paragraphs = {
        row["paragraph"]
        for row in correction["replacements"]
        if isinstance(row.get("paragraph"), int)
    }

    with zipfile.ZipFile(FINAL) as archive:
        root = etree.fromstring(archive.read("word/document.xml"))
    body = root.find(".//" + W + "body")
    paragraphs = body.findall("./" + W + "p") if body is not None else []

    affected = [row for row in matches if row.get("docx_para") in changed_paragraphs]
    exact_remaining = []
    eight_word_window_remaining = []
    for row in affected:
        index = row["docx_para"]
        final_text = normalize("".join(t.text or "" for t in paragraphs[index].iter(W + "t")))
        match_text = normalize(row["text"])
        if match_text and match_text in final_text:
            exact_remaining.append(row)
        tokens = match_text.split()
        survives = any(" ".join(tokens[i : i + 8]) in final_text for i in range(max(0, len(tokens) - 7)))
        if survives:
            eight_word_window_remaining.append(row)

    total_record_words = sum(row["words"] for row in matches)
    affected_words = sum(row["words"] for row in affected)
    exact_remaining_words = sum(row["words"] for row in exact_remaining)
    removed_record_words = affected_words - exact_remaining_words
    overlap_adjustment = UNIQUE_HIGHLIGHT_WORDS_EXTRACTED / total_record_words
    turnitin_calibration = OFFICIAL_ORIGINAL_PERCENT / HIGHLIGHT_EXTRACTION_PERCENT
    calibrated_removed_words = removed_record_words * overlap_adjustment * turnitin_calibration
    estimated_reduction_points = calibrated_removed_words / TURNITIN_WORDS * 100
    mechanical_projection = OFFICIAL_ORIGINAL_PERCENT - estimated_reduction_points

    report = {
        "status": "estimate_not_official_turnitin_result",
        "official_original_similarity_percent": OFFICIAL_ORIGINAL_PERCENT,
        "official_original_word_count": TURNITIN_WORDS,
        "official_filters": ["bibliography excluded", "quoted text excluded", "matches under 8 words excluded"],
        "original_match_records": len(matches),
        "original_match_record_words_non_deduplicated": total_record_words,
        "original_highlight_words_unique_extracted": UNIQUE_HIGHLIGHT_WORDS_EXTRACTED,
        "changed_paragraphs": len(changed_paragraphs),
        "affected_original_match_records": len(affected),
        "affected_paragraphs": len({row["docx_para"] for row in affected}),
        "affected_match_words_non_deduplicated": affected_words,
        "exact_full_match_records_remaining_in_same_paragraph": len(exact_remaining),
        "exact_full_match_words_remaining": exact_remaining_words,
        "records_with_any_8_word_window_remaining_in_same_paragraph": len(eight_word_window_remaining),
        "calculation": {
            "record_overlap_adjustment": round(overlap_adjustment, 6),
            "turnitin_to_highlight_calibration": round(turnitin_calibration, 6),
            "calibrated_removed_equivalent_words": round(calibrated_removed_words),
            "estimated_reduction_percentage_points": round(estimated_reduction_points, 2),
            "mechanical_projection_percent": round(mechanical_projection, 2),
        },
        "honest_working_range_percent": [17.0, 20.0],
        "interpretation": "The corrections likely place the document near or below 20%, but only a new Turnitin run of the final DOCX can certify compliance.",
        "limitations": [
            "Turnitin can identify new or fuzzy matches not present in the original report.",
            "Highlighted source records can overlap; the calculation adjusts globally but cannot reproduce Turnitin's proprietary deduplication.",
            "Pagination and total word count changed after editing.",
            "This estimate must not be presented as an official Turnitin percentage.",
        ],
    }
    OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
