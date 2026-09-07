#!/usr/bin/env python3
"""Validación integral de la entrega APA 7 y de formato posterior al render."""
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

from corregir_formato_apa7 import APA7_RESULTS

ROOT = Path('/projects/sandbox/hola')
ORIGINAL = ROOT / 'Tesis Jaime Fredy Horacio Avance 18 (1).docx'
FINAL = ROOT / 'Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.docx'
PDF = ROOT / 'analisis/render/Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.pdf'
OUT = ROOT / 'analisis/verificacion_entrega_apa7.json'
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
W14 = '{http://schemas.microsoft.com/office/word/2010/wordml}'
A = '{http://schemas.openxmlformats.org/drawingml/2006/main}'
R = '{http://schemas.openxmlformats.org/package/2006/relationships}'


def sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def text(node: etree._Element) -> str:
    return ''.join(t.text or '' for t in node.iter(W + 't'))


def style(p: etree._Element) -> str:
    node = p.find('./' + W + 'pPr/' + W + 'pStyle')
    return node.get(W + 'val', '') if node is not None else ''


def sdt_tag(sdt: etree._Element) -> str:
    node = sdt.find('./' + W + 'sdtPr/' + W + 'tag')
    return node.get(W + 'val', '') if node is not None else ''


def citation_result(sdt: etree._Element) -> str:
    content = sdt.find('./' + W + 'sdtContent')
    if content is None:
        return ''
    active = False
    values: list[str] = []
    for child in content:
        fld = child.find('.//' + W + 'fldChar')
        if fld is not None:
            kind = fld.get(W + 'fldCharType', '')
            if kind == 'separate':
                active = True
                continue
            if kind == 'end':
                break
        if active:
            values.extend(t.text or '' for t in child.iter(W + 't'))
    return ''.join(values)


def normalized(value: str) -> str:
    return ' '.join(re.findall(r'\w+', value.casefold(), flags=re.UNICODE))


def on(prop: etree._Element | None) -> bool:
    return prop is not None and prop.get(W + 'val', '1').lower() not in {'0', 'false', 'off', 'none'}


def literal_index_page(p: etree._Element) -> int:
    for node in reversed(list(p.iter(W + 't'))):
        value = (node.text or '').strip()
        if value.isdigit():
            return int(value)
    raise RuntimeError(text(p))


def relationship_source(rels_name: str) -> str:
    if rels_name == '_rels/.rels':
        return ''
    parent, filename = rels_name.rsplit('/_rels/', 1)
    return posixpath.join(parent, filename[:-5])


def malformed_citation(value: str) -> bool:
    core = value.rstrip('.,;: ')
    if 'pág.' in value or re.search(r'\(\s+', value) or re.search(r'\s{2,}', value):
        return True
    if re.search(r'et al\.?\s+\d{4}', value):
        return True
    if core.startswith('(') and re.search(r'\d{4}[a-z]?\)?$', core):
        if not re.search(r',\s*\d{4}[a-z]?\)?$', core):
            return True
    if re.search(r',\s*p\.?\s*\d', value) and ', p. ' not in value and ', pp. ' not in value:
        return True
    return False


def main() -> int:
    checks: dict[str, bool] = {}
    details: dict[str, object] = {}

    with zipfile.ZipFile(ORIGINAL) as original, zipfile.ZipFile(FINAL) as final:
        names = final.namelist()
        checks['zip_integrity'] = final.testzip() is None
        checks['package_parts_196'] = len(names) == 196
        xml_errors = []
        for name in names:
            if name.endswith(('.xml', '.rels')):
                try:
                    etree.fromstring(final.read(name))
                except Exception as exc:
                    xml_errors.append({'part': name, 'error': str(exc)})
        checks['all_xml_parts_parse'] = not xml_errors
        details['xml_errors'] = xml_errors

        broken_relationships = []
        names_set = set(names)
        for rels_name in [name for name in names if name.endswith('.rels')]:
            rels = etree.fromstring(final.read(rels_name))
            source = relationship_source(rels_name)
            base = posixpath.dirname(source)
            for rel in rels.iter(R + 'Relationship'):
                if rel.get('TargetMode') == 'External':
                    continue
                target = rel.get('Target', '')
                resolved = posixpath.normpath(posixpath.join(base, target)).lstrip('/')
                if resolved not in names_set:
                    broken_relationships.append({'rels': rels_name, 'target': target, 'resolved': resolved})
        checks['all_internal_relationship_targets_exist'] = not broken_relationships
        details['broken_relationships'] = broken_relationships

        root = etree.fromstring(final.read('word/document.xml'))
        original_root = etree.fromstring(original.read('word/document.xml'))
        styles = etree.fromstring(final.read('word/styles.xml'))
        settings = etree.fromstring(final.read('word/settings.xml'))
        body = root.find('.//' + W + 'body')
        paragraphs = body.findall('./' + W + 'p') if body is not None else []
        all_text = '\n'.join(text(p) for p in paragraphs)
        all_document_text = text(root)
        para_ids = [
            paragraph.get(W14 + 'paraId')
            for paragraph in root.iter(W + 'p')
            if paragraph.get(W14 + 'paraId')
        ]
        duplicate_para_ids = sorted({value for value in para_ids if para_ids.count(value) > 1})
        checks['all_w14_paraIds_unique'] = len(para_ids) == len(set(para_ids))
        details['duplicate_w14_paraIds'] = duplicate_para_ids

        sdts = list(root.iter(W + 'sdt'))
        citations = [s for s in sdts if sdt_tag(s).startswith('CitaviPlaceholder#')]
        citation_by_tag = {sdt_tag(s): s for s in citations}
        citation_values = [citation_result(s) for s in citations]
        bibliographies = [s for s in sdts if sdt_tag(s) == 'CitaviBibliography']
        non_sdt_citavi = [
            instr for instr in root.iter(W + 'instrText')
            if 'CitaviPlaceholder' in (instr.text or '') and not any(a.tag == W + 'sdt' for a in instr.iterancestors())
        ]
        bibliography_entries = [p for p in root.iter(W + 'p') if style(p) == 'CitaviBibliographyEntry' and text(p).strip()]
        checks.update({
            'sdt_total_80': len(sdts) == 80,
            'citavi_citations_75': len(citations) == 75,
            'citavi_tags_unique_75': len(citation_by_tag) == 75,
            'citavi_bibliography_1': len(bibliographies) == 1,
            'citavi_non_sdt_field_1': len(non_sdt_citavi) == 1,
            'bibliography_entries_58': len(bibliography_entries) == 58,
            'all_46_targeted_apa_results_exact': all(tag in citation_by_tag and citation_result(citation_by_tag[tag]) == expected for tag, expected in APA7_RESULTS.items()),
            'all_75_citation_results_nonempty': all(value.strip() for value in citation_values),
            'all_75_citation_results_pass_apa_syntax_screen': not any(malformed_citation(value) for value in citation_values),
        })
        details['malformed_citation_results'] = [value for value in citation_values if malformed_citation(value)]

        original_instr = [i.text or '' for i in original_root.iter(W + 'instrText') if 'Citavi' in (i.text or '')]
        final_instr = [i.text or '' for i in root.iter(W + 'instrText') if 'Citavi' in (i.text or '')]
        checks['citavi_instruction_payloads_preserved'] = original_instr == final_instr

        media = [name for name in names if name.startswith('word/media/') and not name.endswith('/')]
        media_unchanged = all(name in original.namelist() and sha256(original.read(name)) == sha256(final.read(name)) for name in media)
        tables = list(root.iter(W + 'tbl'))
        table_rows = list(root.iter(W + 'tr'))
        checks.update({
            'tables_61_with_controlled_continuations': len(tables) == 61,
            'all_table_rows_cannot_split': all(row.find('./' + W + 'trPr/' + W + 'cantSplit') is not None for row in table_rows),
            'drawings_55': len(list(root.iter(W + 'drawing'))) == 55,
            'media_parts_33': len(media) == 33,
            'media_byte_identical_to_original': media_unchanged,
        })

        explicit_font_errors = []
        for part_name, part in (('document', root), ('styles', styles)):
            for fonts in part.iter(W + 'rFonts'):
                for attr in ('ascii', 'hAnsi', 'cs', 'eastAsia'):
                    value = fonts.get(W + attr)
                    if value and value.casefold() != 'calibri':
                        explicit_font_errors.append({'part': part_name, 'attribute': attr, 'value': value})
        theme_errors = []
        for name in [n for n in names if n.startswith('word/theme/theme') and n.endswith('.xml')]:
            theme = etree.fromstring(final.read(name))
            for latin in theme.iter(A + 'latin'):
                if latin.get('typeface', '').casefold() != 'calibri':
                    theme_errors.append({'part': name, 'value': latin.get('typeface', '')})
        checks['all_explicit_and_theme_fonts_calibri'] = not explicit_font_errors and not theme_errors
        details['font_errors'] = explicit_font_errors + theme_errors

        italic_heading_styles = []
        for st in styles.findall('./' + W + 'style'):
            sid = st.get(W + 'styleId', '')
            rpr = st.find('./' + W + 'rPr')
            if re.fullmatch(r'Ttulo\d+', sid) and rpr is not None and (on(rpr.find('./' + W + 'i')) or on(rpr.find('./' + W + 'iCs'))):
                italic_heading_styles.append(sid)
        checks['heading_styles_not_italic'] = not italic_heading_styles
        details['italic_heading_styles'] = italic_heading_styles

        majority_bold = []
        for index, p in enumerate(paragraphs):
            raw = text(p).strip()
            if len(raw) < 120 or re.match(r'^(Ttulo|Heading|ndicemanual|APA|Citavi)', style(p), re.I):
                continue
            runs = [r for r in p.iter(W + 'r') if text(r).strip()]
            total = sum(len(text(r)) for r in runs) or 1
            ratio = sum(len(text(r)) for r in runs if (lambda rpr: rpr is not None and on(rpr.find('./' + W + 'b')))(r.find('./' + W + 'rPr'))) / total
            if ratio >= 0.5:
                majority_bold.append({'paragraph': index, 'ratio': round(ratio, 3), 'text': raw})
        checks['no_long_body_paragraph_majority_bold'] = not majority_bold
        details['long_majority_bold'] = majority_bold

        body_text_breaks = [b for p in paragraphs for b in p.iter(W + 'br') if b.get(W + 'type', 'textWrapping') == 'textWrapping']
        checks['no_manual_line_breaks_in_body'] = not body_text_breaks
        checks['no_multiple_literal_spaces_in_body'] = re.search(r'\S[ \u00a0]{2,}\S', all_text) is None
        checks['word_compat_prevents_shift_return_expansion'] = settings.find('.//' + W + 'doNotExpandShiftReturn') is not None

        index_paragraphs = [p for p in root.iter(W + 'p') if style(p).startswith('ndicemanual')]
        checks['manual_index_entries_176'] = len(index_paragraphs) == 176
        details['manual_index_page_values'] = [literal_index_page(p) for p in index_paragraphs]

        table_numbers = sorted({int(m.group(1)) for p in root.iter(W + 'p') if not style(p).startswith('ndicemanual') for m in [re.match(r'^Tabla\s+(\d+)\b', text(p).strip())] if m})
        figure_numbers = sorted({int(m.group(1)) for p in root.iter(W + 'p') if not style(p).startswith('ndicemanual') for m in [re.match(r'^Figura\s+(\d+)\b', text(p).strip())] if m})
        checks['table_sequence_1_to_37'] = table_numbers == list(range(1, 38))
        checks['figure_sequence_1_to_54'] = figure_numbers == list(range(1, 55))
        checks.update({
            'sample_60_claim_absent': 'muestra de 60 establecimientos' not in all_text,
            'sample_66_claim_absent': '66 establecimientos' not in all_text,
            'sample_63_documented': all_text.count('63 establecimientos') >= 3,
            'citation_double_period_absent': re.search(r'\)\.\s*\.', all_document_text) is None,
            'known_bibliography_artifacts_absent': not any(v in all_document_text for v in ['ARSA, del #1', 'Guia Ayuda', 'Solicitud Para Licencia', 'Planficación', '!:eche', 'metodología JICA,.', 'www.bing.com/ck/']),
        })

    opened = Document(FINAL)
    checks['python_docx_opens'] = len(opened.paragraphs) > 0

    pdf = fitz.open(PDF)
    checks['pdf_opens_expected_range'] = 250 <= len(pdf) <= 265
    details['pdf_pages'] = len(pdf)
    pdf_text = '\n'.join(page.get_text('text') for page in pdf)
    pdf_norm = normalized(pdf_text)
    checks['all_75_citation_results_visible_in_pdf'] = all(normalized(value) in pdf_norm for value in citation_values)

    rendered_index_rows = []
    for page_index in range(1, min(10, len(pdf))):
        page = pdf[page_index]
        words = page.get_text('words')
        for link in page.get_links():
            target = link.get('page', -1)
            if target is None or target < 0:
                continue
            line = ' '.join(w[4] for w in words if fitz.Rect(w[:4]).intersects(link['from'])).strip()
            match = re.search(r'(\d+)\s*$', line)
            if match:
                rendered_index_rows.append((int(match.group(1)), int(target) + 1))
    checks['pdf_index_links_176'] = len(rendered_index_rows) == 176
    checks['pdf_index_visible_numbers_match_targets'] = all(visible == target for visible, target in rendered_index_rows)

    table21_pages = []
    for index, page in enumerate(pdf):
        value = page.get_text('text')
        if re.search(r'\bTabla\s+21\b', value):
            table21_pages.append({'page': index + 1, 'has_first_total': '204,700' in value, 'has_second_total': '89,500' in value, 'has_continuation': 'Tabla 21 (continuación)' in value})
    checks['table_21_complete_and_closed_on_one_page'] = any(item['has_first_total'] and item['has_second_total'] and item['has_continuation'] for item in table21_pages)
    details['table_21_pages'] = table21_pages

    table15_pages = []
    table22_pages = []
    for index, page in enumerate(pdf):
        value = page.get_text('text')
        if re.search(r'(?m)^Tabla 15\s*$', value):
            table15_pages.append({'page': index + 1, 'has_total': 'TOTAL INVERSIONES' in value, 'has_note': 'Nota. Elaboración propia, 2026.' in value})
        if re.search(r'(?m)^Tabla 22\s*$', value):
            table22_pages.append({'page': index + 1, 'has_continuation': 'Tabla 22 (continuación)' in value, 'has_final_value': '2,686,438' in value, 'has_note': 'Nota. Elaboración propia, 2026.' in value})
    checks['table_15_not_orphaned_across_pages'] = any(item['has_total'] and item['has_note'] for item in table15_pages)
    checks['table_22_not_orphaned_across_pages'] = any(item['has_continuation'] and item['has_final_value'] and item['has_note'] for item in table22_pages)
    details['table_15_pages'] = table15_pages
    details['table_22_pages'] = table22_pages

    abnormal_gaps = []
    max_gap = 0.0
    for page_no, page in enumerate(pdf, start=1):
        lines: dict[tuple[int, int], list[tuple]] = {}
        for word in page.get_text('words'):
            lines.setdefault((int(word[5]), int(word[6])), []).append(word)
        for words in lines.values():
            words.sort(key=lambda w: w[0])
            if len(words) < 4:
                continue
            gaps = [right[0] - left[2] for left, right in zip(words, words[1:])]
            line_max = max(gaps, default=0.0)
            max_gap = max(max_gap, line_max)
            if line_max > 60:
                abnormal_gaps.append({'page': page_no, 'gap_points': round(line_max, 2), 'line': ' '.join(w[4] for w in words)})
    checks['no_extreme_interword_gaps_over_60pt'] = not abnormal_gaps
    details['maximum_interword_gap_points'] = round(max_gap, 2)
    details['extreme_gap_lines'] = abnormal_gaps

    table_audit_path = ROOT / 'analisis/auditoria_tablas_partidas_final.json'
    table_audit = json.loads(table_audit_path.read_text(encoding='utf-8')) if table_audit_path.exists() else {}
    checks['all_37_logical_tables_have_closed_labeled_continuations'] = (
        table_audit.get('all_ok') is True
        and table_audit.get('tables_checked') == 37
        and table_audit.get('pdf_sha256') == sha256(PDF.read_bytes())
        and not table_audit.get('orphan_continuations')
    )
    details['logical_table_audit'] = {
        'report': str(table_audit_path),
        'tables_checked': table_audit.get('tables_checked'),
        'orphan_continuations': table_audit.get('orphan_continuations'),
        'pdf_hash_matches': table_audit.get('pdf_sha256') == sha256(PDF.read_bytes()),
    }

    failed = [name for name, ok in checks.items() if not ok]
    report = {
        'final_docx': str(FINAL),
        'final_pdf_render': str(PDF),
        'original_sha256': sha256(ORIGINAL.read_bytes()),
        'final_sha256': sha256(FINAL.read_bytes()),
        'final_size_bytes': FINAL.stat().st_size,
        'checks_passed': sum(checks.values()),
        'checks_total': len(checks),
        'all_ok': not failed,
        'failed_checks': failed,
        'checks': checks,
        'details': details,
    }
    OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({k: report[k] for k in ('final_docx', 'final_sha256', 'final_size_bytes', 'checks_passed', 'checks_total', 'all_ok', 'failed_checks')}, ensure_ascii=False, indent=2))
    return 0 if not failed else 1


if __name__ == '__main__':
    raise SystemExit(main())
