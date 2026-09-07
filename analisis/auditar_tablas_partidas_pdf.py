#!/usr/bin/env python3
"""Audita las 37 tablas lógicas del PDF y detecta continuaciones huérfanas.

Usa las páginas literales del índice de tablas del DOCX como inicio y busca la
nota/fuente final antes de la tabla siguiente. Si una tabla continúa en una
página posterior, esa página debe incluir un rótulo explícito
"Tabla N (continuación)"; de lo contrario el fragmento queda abierto.
"""
from __future__ import annotations

import hashlib
import json
import re
import zipfile
from pathlib import Path

import fitz
from lxml import etree

ROOT = Path('/projects/sandbox/hola')
DOCX = ROOT / 'Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.docx'
PDF = ROOT / 'analisis/render/Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.pdf'
OUT = ROOT / 'analisis/auditoria_tablas_partidas_final.json'
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'


def text(node: etree._Element) -> str:
    return ''.join(t.text or '' for t in node.iter(W + 't'))


def style(p: etree._Element) -> str:
    node = p.find('./' + W + 'pPr/' + W + 'pStyle')
    return node.get(W + 'val', '') if node is not None else ''


def literal_page(p: etree._Element) -> int:
    for node in reversed(list(p.iter(W + 't'))):
        value = (node.text or '').strip()
        if value.isdigit():
            return int(value)
    raise RuntimeError(text(p))


def main() -> int:
    with zipfile.ZipFile(DOCX) as archive:
        root = etree.fromstring(archive.read('word/document.xml'))
    entries = []
    for p in root.iter(W + 'p'):
        if style(p) != 'ndicemanualtabla':
            continue
        value = text(p)
        match = re.match(r'Tabla\s+(\d+)\b', value)
        if not match:
            raise RuntimeError(f'Entrada de tabla no reconocida: {value!r}')
        entries.append({'number': int(match.group(1)), 'start_page': literal_page(p), 'index_text': value})
    if [item['number'] for item in entries] != list(range(1, 38)):
        raise RuntimeError('El índice no contiene las tablas 1–37 en orden')

    doc = fitz.open(PDF)
    page_text = ['\n'.join(line.strip() for line in page.get_text('text').splitlines() if line.strip()) for page in doc]
    # La página real se obtiene del rótulo renderizado, no del número aún
    # almacenado en el índice, porque la corrección puede repaginar el borrador.
    for item in entries:
        number = item['number']
        rendered = [
            page_no for page_no, value in enumerate(page_text, start=1)
            if page_no > 10 and re.search(rf'(?m)^Tabla\s+{number}\s*$', value)
        ]
        if len(rendered) != 1:
            raise RuntimeError(f'Rótulo renderizado ambiguo para Tabla {number}: {rendered}')
        item['indexed_start_page'] = item['start_page']
        item['start_page'] = rendered[0]
    results = []
    orphan_pages = []
    missing_end = []
    for position, item in enumerate(entries):
        number = item['number']
        start = item['start_page']
        next_start = entries[position + 1]['start_page'] if position + 1 < len(entries) else min(len(doc), start + 10)
        search_end = min(len(doc), max(start + 1, next_start + (1 if next_start == start else 0)))
        final_page = None
        end_marker = None
        # La nota/fuente inmediatamente posterior cierra la tabla lógica.
        for page_no in range(start, search_end + 1):
            value = page_text[page_no - 1]
            markers = [
                line for line in value.splitlines()
                if re.match(r'^(Nota|Fuente)\b', line, flags=re.I)
            ]
            if markers:
                final_page = page_no
                end_marker = markers[-1]
                break
        if final_page is None:
            missing_end.append(number)
            final_page = start

        continuation_pages = []
        orphans = []
        for page_no in range(start + 1, final_page + 1):
            value = page_text[page_no - 1]
            explicit = bool(re.search(rf'Tabla\s+{number}\s*\(continuación\)', value, flags=re.I))
            if explicit:
                continuation_pages.append(page_no)
            else:
                orphans.append(page_no)
                orphan_pages.append({'table': number, 'page': page_no})
        results.append({
            **item,
            'final_page': final_page,
            'pages_spanned': final_page - start + 1,
            'end_marker': end_marker,
            'explicit_continuation_pages': continuation_pages,
            'orphan_continuation_pages': orphans,
            'ok': not orphans and number not in missing_end,
        })

    report = {
        'docx': str(DOCX),
        'pdf': str(PDF),
        'pdf_sha256': hashlib.sha256(PDF.read_bytes()).hexdigest(),
        'pdf_pages': len(doc),
        'tables_checked': len(results),
        'orphan_continuations': orphan_pages,
        'tables_without_detectable_note_or_source': missing_end,
        'all_ok': not orphan_pages and not missing_end,
        'tables': results,
    }
    OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({
        'output': str(OUT),
        'tables_checked': len(results),
        'orphan_continuations': orphan_pages,
        'tables_without_end_marker': missing_end,
        'all_ok': report['all_ok'],
    }, ensure_ascii=False, indent=2))
    return 0 if report['all_ok'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
