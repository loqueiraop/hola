#!/usr/bin/env python3
"""Segunda corrección quirúrgica: APA 7 visible, tablas y formato.

Trabaja sobre la entrega final ya auditada. Conserva los controles SDT, las
instrucciones ADDIN de Citavi, relaciones, medios y matrices de datos.
"""
from __future__ import annotations

import copy
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path
from typing import Any

from lxml import etree

ROOT = Path('/projects/sandbox/hola')
SRC = ROOT / 'Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.docx'
DST = ROOT / 'Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA APA7 BORRADOR.docx'
REPORT = ROOT / 'analisis/informe_correccion_apa7_formato.json'
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
XMLSPACE = '{http://www.w3.org/XML/1998/namespace}space'
PARSER = etree.XMLParser(remove_blank_text=False, resolve_entities=False, huge_tree=True)

# Resultados visibles identificados por UUID. No se modifica w:instrText.
APA7_RESULTS = {
    'CitaviPlaceholder#4abf87fd-2f49-4fc6-8ece-502a05e77ab7': '(Borjas Chávez, 2013)',
    'CitaviPlaceholder#2bc0d57d-5bc5-4e3a-9138-bd6ad5d74e3d': '(Holmann, 2001)',
    'CitaviPlaceholder#5db1b62b-854e-4320-9be7-8e0c0b81d886': '(García Oliva, 2008, p. 60)',
    'CitaviPlaceholder#de1f0255-e608-4a61-ba56-a0f7458527d1': '(Borjas Chávez, 2013)',
    'CitaviPlaceholder#c2928acf-4a6c-49cf-b884-9e8f02a17a20': 'FAOSTAT (2022)',
    'CitaviPlaceholder#802386d8-959d-4791-83d8-aa9ff24d403c': '(CATIE, 2016)',
    'CitaviPlaceholder#ec878e7f-57f0-41c6-8274-450c102ce9f7': '(Muñoz et al., 2017)',
    'CitaviPlaceholder#db57d2b5-5047-427b-ba26-55e39b558c2d': '(Sapag Chain, 2008, p. 22).',
    'CitaviPlaceholder#742ec5ac-cd5c-4bbd-aa92-e576e7f4d78e': '(Franco et al., 2012)',
    'CitaviPlaceholder#40586cf3-36a4-452b-8e98-e1ebd517a2b1': '(ONUDI, 2019, p. 16).',
    'CitaviPlaceholder#682444b1-a57a-4405-8f78-e80e2360a88f': '(Franco et al., 2012)',
    'CitaviPlaceholder#5a5468d2-69b6-4441-a313-b7931eda8833': '(Domínguez Cedeño, 2018, p. 7).',
    'CitaviPlaceholder#147412ad-9198-4a95-9c34-87184233b9db': '(Coria, 2008)',
    'CitaviPlaceholder#4a8c8afc-ae0e-40d4-a2ef-124b48641980': '(Robles Ríos, 2019)',
    'CitaviPlaceholder#2799e594-b30c-4d98-8fb8-ce7b2301d391': '(Silva Oquendo, 2020)',
    'CitaviPlaceholder#30c40303-e8dd-46b5-b167-270e37c1d374': '(Silva Oquendo, 2020)',
    'CitaviPlaceholder#187cba8f-523e-421d-b230-77cba1465c07': '(Gómez, 2004, p. 12)',
    'CitaviPlaceholder#e278abdd-b1ff-47fc-9339-0b5e49ddf3c3': '(Gómez, 2004, p. 12)',
    'CitaviPlaceholder#dab360b6-ed8c-4357-92ea-1f4c6a4594d8': '(Gómez, 2004, p. 12)',
    'CitaviPlaceholder#30e455fd-56ab-4a01-89a4-19265c8df13c': '(Gómez, 2004, p. 13)',
    'CitaviPlaceholder#14984e52-a6a0-4311-a3bc-a9a3f319f891': '(Pérez et al., 2008)',
    'CitaviPlaceholder#61b983b0-b43a-4989-b839-f08365806183': '(Holcim, 2024, p. 7)',
    'CitaviPlaceholder#43806d59-98f4-4f7e-928c-f27ad9a4bd5e': '(Holcim, 2024, p. 7)',
    'CitaviPlaceholder#5a0976f6-12e8-4796-9287-68447ad7e157': '(Agencia de Cooperación Internacional del Japón et al., 2014)',
    'CitaviPlaceholder#e45d4b43-eb33-4a55-9e6e-2579cd6754e2': '(Agencia de Cooperación Internacional del Japón et al., 2014)',
    'CitaviPlaceholder#ce147722-b16e-402f-9915-1230ee376470': '(Agencia de Cooperación Internacional del Japón et al., 2014)',
    'CitaviPlaceholder#22478968-0c92-4468-b551-98a0f46a8a7f': '(Marcos, 2020)',
    'CitaviPlaceholder#2662ef5b-fd32-4616-af13-72c48041704a': '(Fernández, 1989)',
    'CitaviPlaceholder#c33cf47d-7210-45f1-8e61-1513c94d6688': '(Roa Rodríguez, 2015)',
    'CitaviPlaceholder#983640d0-02c4-4497-a1e5-d8bca4969561': '(Sierra et al., 2023)',
    'CitaviPlaceholder#be2ccc5c-f34f-472b-91d4-0374ded4c98f': '(Sierra et al., 2023)',
    'CitaviPlaceholder#76d6f911-842a-4b9e-affb-72a1be758c11': '(Sierra et al., 2023)',
    'CitaviPlaceholder#5d6c971b-895c-4162-8a2c-41b71e98bde0': '(Axial ERP, 2022)',
    'CitaviPlaceholder#cddafd75-d3d2-4f42-916d-27ddd96c360b': '(Landau, 2024)',
    'CitaviPlaceholder#07af05f0-392e-4c10-b4d1-3db35fb9acda': '(Luis, 2024)',
    'CitaviPlaceholder#5e826c0a-65d3-4e42-a0c8-2dcca9c44b7d': '(Luis, 2024)',
    'CitaviPlaceholder#a986cda9-acc1-4261-a296-d0d702f32dc9': '(SNIP, 2020)',
    'CitaviPlaceholder#2f9cc5d2-51ec-45ec-9ff4-938b921cf95d': '(SNIP, 2020)',
    'CitaviPlaceholder#5d51c9ce-fcec-44fe-aff2-035ca9d36de1': '(ALNAP, 2023, p. 2)',
    'CitaviPlaceholder#089e7c43-128a-4869-b110-8505ae073840': '(ALNAP, 2023)',
    'CitaviPlaceholder#a70beada-53c7-4780-a23c-622defc6e8b3': '(Cepeda, 2025)',
    'CitaviPlaceholder#537a1e03-8706-4596-b674-f32c3010f915': '(CNP+L Honduras, 2017)',
    'CitaviPlaceholder#88b3cde4-13f5-456c-a04e-ab4206cdcf9e': ' Parikh y Trivedi (2025)',
    'CitaviPlaceholder#86627edf-e2af-49c4-a0e8-baa05db40b5a': '(Ramírez & Calderón, 1985)',
    'CitaviPlaceholder#2cf0aa66-2c3e-4b2f-b5fe-257913dcc296': '(Cáceres et al., 2021)',
    'CitaviPlaceholder#132e033a-71ee-43df-851e-4adb68954ef2': '(OECD & FAO, 2025)',
}

# Estas tablas comenzaban demasiado cerca del pie y su primer fragmento se
# abría en dos páginas. Las continuaciones largas de 25/26 empiezan en página
# nueva para que cada cuadrícula física quede cerrada.
TABLE_CAPTIONS_NEW_PAGE = {14, 15, 16, 17, 18, 20, 21, 22, 24, 25}
CONTINUATIONS_NEW_PAGE = {25, 26}
AD_HOC_HEADINGS_REMOVE_ITALIC = {749, 751, 753, 755, 757, 759, 761}

TEXT_REPLACEMENTS = {
    '(Rios, 2019)': '(Ríos, 2019)',
    '(p 1).': '(p. 1).',
    '(p 4).': '(p. 4).',
    '(p 38)': '(p. 38)',
    '(p 60).': '(p. 60).',
    '(p 34-42).': '(pp. 34–42).',
    '(p 1-105).': '(pp. 1–105).',
    'Parikh y Trivedi (2025), demuestra': 'Parikh y Trivedi (2025) demuestra',
    'Jordana (2009), define': 'Jordana (2009) define',
    'Cáceres (2013), demuestra': 'Cáceres (2013) demuestra',
    'Ruiz (1997), clasifica': 'Ruiz (1997) clasifica',
    'IFF (2025), señala': 'IFF (2025) señala',
    'Uribe (2003), desarrolla': 'Uribe (2003) desarrolla',
}


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def parse(data: bytes) -> etree._Element:
    return etree.fromstring(data, parser=PARSER)


def serialize(root: etree._Element) -> bytes:
    return etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)


def text(node: etree._Element) -> str:
    return ''.join(t.text or '' for t in node.iter(W + 't'))


def paragraph_style(p: etree._Element) -> str:
    node = p.find('./' + W + 'pPr/' + W + 'pStyle')
    return node.get(W + 'val', '') if node is not None else ''


def sdt_tag(sdt: etree._Element) -> str:
    node = sdt.find('./' + W + 'sdtPr/' + W + 'tag')
    return node.get(W + 'val', '') if node is not None else ''


def is_citavi_sdt(node: etree._Element) -> bool:
    return node.tag == W + 'sdt' and sdt_tag(node).startswith('CitaviPlaceholder#')


def in_citavi(node: etree._Element) -> bool:
    return any(is_citavi_sdt(a) for a in node.iterancestors())


def set_text(node: etree._Element, value: str) -> None:
    node.text = value
    if value[:1].isspace() or value[-1:].isspace():
        node.set(XMLSPACE, 'preserve')
    else:
        node.attrib.pop(XMLSPACE, None)


def citation_result_nodes(sdt: etree._Element) -> tuple[list[etree._Element], etree._Element | None]:
    content = sdt.find('./' + W + 'sdtContent')
    if content is None:
        raise RuntimeError(f'SDT sin contenido: {sdt_tag(sdt)}')
    active = False
    nodes: list[etree._Element] = []
    end_run = None
    for child in content:
        fld = child.find('.//' + W + 'fldChar')
        if fld is not None:
            kind = fld.get(W + 'fldCharType', '')
            if kind == 'separate':
                active = True
                continue
            if kind == 'end':
                end_run = child
                break
        if active:
            nodes.extend(child.iter(W + 't'))
    return nodes, end_run


def citation_result(sdt: etree._Element) -> str:
    return ''.join(node.text or '' for node in citation_result_nodes(sdt)[0])


def set_citation_result(sdt: etree._Element, value: str) -> None:
    nodes, end_run = citation_result_nodes(sdt)
    if not nodes:
        if end_run is None:
            raise RuntimeError(f'Campo sin cierre: {sdt_tag(sdt)}')
        run = etree.Element(W + 'r')
        node = etree.SubElement(run, W + 't')
        end_run.addprevious(run)
        nodes = [node]
    for index, node in enumerate(nodes):
        set_text(node, value if index == 0 else '')


def replace_outside_citavi(root: etree._Element, old: str, new: str) -> int:
    changed = 0
    for p in root.iter(W + 'p'):
        nodes = [n for n in p.iter(W + 't') if not in_citavi(n)]
        joined = ''.join(n.text or '' for n in nodes)
        if old not in joined:
            continue
        # Los reemplazos verificados no cruzan un control Citavi.
        cursor = 0
        start = joined.find(old)
        end = start + len(old)
        start_i = end_i = -1
        start_local = end_local = 0
        for i, value in enumerate(n.text or '' for n in nodes):
            next_cursor = cursor + len(value)
            if start_i < 0 and start < next_cursor:
                start_i, start_local = i, start - cursor
            if end <= next_cursor:
                end_i, end_local = i, end - cursor
                break
            cursor = next_cursor
        if start_i < 0 or end_i < 0:
            continue
        if start_i == end_i:
            value = nodes[start_i].text or ''
            set_text(nodes[start_i], value[:start_local] + new + value[end_local:])
        else:
            prefix = (nodes[start_i].text or '')[:start_local]
            suffix = (nodes[end_i].text or '')[end_local:]
            set_text(nodes[start_i], prefix + new)
            for i in range(start_i + 1, end_i):
                set_text(nodes[i], '')
            set_text(nodes[end_i], suffix)
        changed += 1
    return changed


def ensure_ppr(p: etree._Element) -> etree._Element:
    ppr = p.find('./' + W + 'pPr')
    if ppr is None:
        ppr = etree.Element(W + 'pPr')
        p.insert(0, ppr)
    return ppr


def ensure_on(parent: etree._Element, local: str) -> etree._Element:
    node = parent.find('./' + W + local)
    if node is None:
        node = etree.SubElement(parent, W + local)
    node.attrib.pop(W + 'val', None)
    return node


def remove_prop(rpr: etree._Element | None, names: tuple[str, ...]) -> int:
    if rpr is None:
        return 0
    count = 0
    for name in names:
        for node in list(rpr.findall('./' + W + name)):
            rpr.remove(node)
            count += 1
    return count


def prop_on(rpr: etree._Element | None, name: str) -> bool:
    if rpr is None:
        return False
    node = rpr.find('./' + W + name)
    return node is not None and node.get(W + 'val', '1').lower() not in {'0', 'false', 'off', 'none'}


def clean_long_body_bold(top: list[etree._Element]) -> list[dict[str, Any]]:
    changes = []
    for index, p in enumerate(top):
        raw = text(p).strip()
        st = paragraph_style(p)
        if len(raw) < 120 or re.match(r'^(Ttulo|Heading|ndicemanual|APA|Citavi)', st, re.I):
            continue
        runs = [r for r in p.iter(W + 'r') if text(r).strip()]
        total = sum(len(text(r)) for r in runs) or 1
        bold = sum(len(text(r)) for r in runs if prop_on(r.find('./' + W + 'rPr'), 'b')) / total
        if bold < 0.5:
            continue
        removed = 0
        for run in runs:
            removed += remove_prop(run.find('./' + W + 'rPr'), ('b', 'bCs'))
        if removed:
            changes.append({'paragraph': index, 'text': raw, 'bold_ratio_before': round(bold, 3), 'properties_removed': removed})
    return changes


def remove_heading_italics(styles: etree._Element, top: list[etree._Element]) -> dict[str, Any]:
    style_changes = []
    for style in styles.findall('./' + W + 'style'):
        sid = style.get(W + 'styleId', '')
        if not re.fullmatch(r'Ttulo\d+', sid):
            continue
        removed = remove_prop(style.find('./' + W + 'rPr'), ('i', 'iCs'))
        if removed:
            style_changes.append({'style': sid, 'properties_removed': removed})
    paragraph_changes = []
    for index, p in enumerate(top):
        if not re.fullmatch(r'Ttulo\d+', paragraph_style(p)) and index not in AD_HOC_HEADINGS_REMOVE_ITALIC:
            continue
        removed = 0
        for run in p.iter(W + 'r'):
            removed += remove_prop(run.find('./' + W + 'rPr'), ('i', 'iCs'))
        if removed:
            paragraph_changes.append({'paragraph': index, 'text': text(p).strip(), 'properties_removed': removed})
    return {'styles': style_changes, 'paragraphs': paragraph_changes}


def remove_manual_line_breaks(top: list[etree._Element]) -> list[dict[str, Any]]:
    changes = []
    for index, p in enumerate(top):
        if re.match(r'^ndicemanual', paragraph_style(p), re.I):
            continue
        count = 0
        for br in list(p.iter(W + 'br')):
            if br.get(W + 'type', 'textWrapping') != 'textWrapping':
                continue
            replacement = etree.Element(W + 't')
            set_text(replacement, ' ')
            br.getparent().replace(br, replacement)
            count += 1
        if count:
            changes.append({'paragraph': index, 'text': text(p).strip(), 'breaks_replaced_with_spaces': count})
    return changes


def normalize_spaces(root: etree._Element) -> int:
    changes = 0
    for p in root.iter(W + 'p'):
        if re.match(r'^ndicemanual', paragraph_style(p), re.I):
            continue
        # w:instrText no pertenece a esta lista; por ello se pueden normalizar
        # los w:t visibles, incluidos los resultados Citavi, sin tocar ADDIN.
        nodes = list(p.iter(W + 't'))
        for node in nodes:
            before = node.text or ''
            after = re.sub(r'[ \u00a0]{2,}', ' ', before)
            if before != after:
                set_text(node, after)
                changes += 1
        previous = None
        for node in nodes:
            if not node.text:
                continue
            if previous is not None and previous.text and previous.text.endswith(' ') and node.text.startswith(' '):
                set_text(node, node.text.lstrip())
                changes += 1
                if not node.text:
                    continue
            previous = node
    return changes


def set_border(container: etree._Element, edge: str, size: str = '4') -> None:
    borders = container.find('./' + W + 'tcBorders')
    if borders is None:
        borders = etree.SubElement(container, W + 'tcBorders')
    node = borders.find('./' + W + edge)
    if node is None:
        node = etree.SubElement(borders, W + edge)
    node.set(W + 'val', 'single')
    node.set(W + 'sz', size)
    node.set(W + 'space', '0')
    node.set(W + 'color', '000000')


def format_tables(root: etree._Element) -> dict[str, Any]:
    tables = list(root.iter(W + 'tbl'))
    cant_split = 0
    border_cells = 0
    for table in tables:
        rows = table.findall('./' + W + 'tr')
        for row in rows:
            trpr = row.find('./' + W + 'trPr')
            if trpr is None:
                trpr = etree.Element(W + 'trPr')
                row.insert(0, trpr)
            if trpr.find('./' + W + 'cantSplit') is None:
                etree.SubElement(trpr, W + 'cantSplit')
                cant_split += 1
        if not rows:
            continue
        for cell in rows[0].findall('./' + W + 'tc'):
            tcpr = cell.find('./' + W + 'tcPr')
            if tcpr is None:
                tcpr = etree.Element(W + 'tcPr')
                cell.insert(0, tcpr)
            set_border(tcpr, 'top')
            border_cells += 1
        for cell in rows[-1].findall('./' + W + 'tc'):
            tcpr = cell.find('./' + W + 'tcPr')
            if tcpr is None:
                tcpr = etree.Element(W + 'tcPr')
                cell.insert(0, tcpr)
            set_border(tcpr, 'bottom')
            border_cells += 1
    return {'tables': len(tables), 'rows_marked_cant_split': cant_split, 'edge_cells_reasserted': border_cells}


def add_table_page_breaks(top: list[etree._Element]) -> list[dict[str, Any]]:
    changes = []
    for index, p in enumerate(top):
        value = text(p).strip()
        match = re.fullmatch(r'Tabla\s+(\d+)(?:\s+\(continuación\)\..*)?', value, re.I)
        if not match:
            continue
        number = int(match.group(1))
        continuation = '(continuación)' in value.casefold()
        wanted = (not continuation and number in TABLE_CAPTIONS_NEW_PAGE) or (continuation and number in CONTINUATIONS_NEW_PAGE)
        if not wanted:
            continue
        ppr = ensure_ppr(p)
        ensure_on(ppr, 'pageBreakBefore')
        changes.append({'paragraph': index, 'caption': value})
    return changes


def add_compatibility_setting(settings: etree._Element) -> bool:
    compat = settings.find('./' + W + 'compat')
    if compat is None:
        compat = etree.SubElement(settings, W + 'compat')
    if compat.find('./' + W + 'doNotExpandShiftReturn') is not None:
        return False
    etree.SubElement(compat, W + 'doNotExpandShiftReturn')
    return True


def table_matrix(root: etree._Element) -> list[list[list[str]]]:
    return [[[text(cell) for cell in row.findall('./' + W + 'tc')] for row in table.findall('./' + W + 'tr')] for table in root.iter(W + 'tbl')]


def inventory(root: etree._Element) -> dict[str, Any]:
    tags = [sdt_tag(s) for s in root.iter(W + 'sdt') if sdt_tag(s).startswith('CitaviPlaceholder#')]
    instr = [i.text or '' for i in root.iter(W + 'instrText') if 'Citavi' in (i.text or '')]
    return {
        'sdt_total': len(list(root.iter(W + 'sdt'))),
        'citation_tags': tags,
        'citation_instr': instr,
        'tables': len(list(root.iter(W + 'tbl'))),
        'drawings': len(list(root.iter(W + 'drawing'))),
        'table_matrix': table_matrix(root),
    }


def main() -> int:
    if not SRC.exists():
        raise FileNotFoundError(SRC)
    report: dict[str, Any] = {'source': str(SRC), 'destination': str(DST), 'source_sha256': sha256(SRC.read_bytes()), 'citation_changes': [], 'formatting': {}, 'validations': []}
    with zipfile.ZipFile(SRC) as zin:
        infos = zin.infolist()
        names = [i.filename for i in infos]
        original = {name: zin.read(name) for name in names}
        root = parse(original['word/document.xml'])
        styles = parse(original['word/styles.xml'])
        settings = parse(original['word/settings.xml'])
        body = root.find('.//' + W + 'body')
        top = body.findall('./' + W + 'p') if body is not None else []
        before = inventory(root)

        by_tag = {sdt_tag(s): s for s in root.iter(W + 'sdt') if sdt_tag(s).startswith('CitaviPlaceholder#')}
        missing = sorted(set(APA7_RESULTS) - set(by_tag))
        if missing:
            raise RuntimeError(f'Faltan campos Citavi: {missing}')
        for tag, value in APA7_RESULTS.items():
            sdt = by_tag[tag]
            old = citation_result(sdt)
            if old != value:
                set_citation_result(sdt, value)
                report['citation_changes'].append({'tag': tag, 'before': old, 'after': value, 'instrText_modified': False})

        textual = []
        for old, new in TEXT_REPLACEMENTS.items():
            count = replace_outside_citavi(root, old, new)
            if count:
                textual.append({'before': old, 'after': new, 'count': count})
        report['formatting']['apa_textual_replacements'] = textual
        report['formatting']['manual_line_breaks'] = remove_manual_line_breaks(top)
        report['formatting']['space_nodes_normalized'] = normalize_spaces(root)
        report['formatting']['long_body_bold_removed'] = clean_long_body_bold(top)
        report['formatting']['heading_italics_removed'] = remove_heading_italics(styles, top)
        report['formatting']['table_page_breaks'] = add_table_page_breaks(top)
        report['formatting']['tables'] = format_tables(root)
        report['formatting']['do_not_expand_shift_return_added'] = add_compatibility_setting(settings)

        modified = dict(original)
        modified['word/document.xml'] = serialize(root)
        modified['word/styles.xml'] = serialize(styles)
        modified['word/settings.xml'] = serialize(settings)
        temp = DST.with_suffix(DST.suffix + '.tmp')
        if temp.exists():
            temp.unlink()
        with zipfile.ZipFile(temp, 'w') as zout:
            zout.comment = zin.comment
            for info in infos:
                zout.writestr(copy.copy(info), modified[info.filename])
        temp.replace(DST)

    with zipfile.ZipFile(SRC) as source, zipfile.ZipFile(DST) as out:
        out_root = parse(out.read('word/document.xml'))
        out_styles = parse(out.read('word/styles.xml'))
        out_settings = parse(out.read('word/settings.xml'))
        after = inventory(out_root)
        out_by_tag = {sdt_tag(s): s for s in out_root.iter(W + 'sdt') if sdt_tag(s).startswith('CitaviPlaceholder#')}
        visible_ok = all(citation_result(out_by_tag[tag]) == value for tag, value in APA7_RESULTS.items())
        citation_values = [citation_result(s) for s in out_by_tag.values()]
        malformed = [v for v in citation_values if re.search(r'\bpág\.|\bet al(?:\s|\.\s+)\d{4}|\([^)]*\b\d{4}(?:[a-z])?\)', v) and re.search(r'\S\s+\d{4}', v) and not re.search(r',\s*\d{4}', v)]
        long_bold = []
        body = out_root.find('.//' + W + 'body')
        for i, p in enumerate(body.findall('./' + W + 'p') if body is not None else []):
            raw = text(p).strip()
            if len(raw) < 120 or re.match(r'^(Ttulo|Heading|ndicemanual|APA|Citavi)', paragraph_style(p), re.I):
                continue
            runs = [r for r in p.iter(W + 'r') if text(r).strip()]
            total = sum(len(text(r)) for r in runs) or 1
            ratio = sum(len(text(r)) for r in runs if prop_on(r.find('./' + W + 'rPr'), 'b')) / total
            if ratio >= 0.5:
                long_bold.append({'paragraph': i, 'ratio': ratio, 'text': raw})
        italic_heading_styles = []
        for st in out_styles.findall('./' + W + 'style'):
            sid = st.get(W + 'styleId', '')
            if re.fullmatch(r'Ttulo\d+', sid) and (prop_on(st.find('./' + W + 'rPr'), 'i') or prop_on(st.find('./' + W + 'rPr'), 'iCs')):
                italic_heading_styles.append(sid)
        out_body = out_root.find('.//' + W + 'body')
        out_top = out_body.findall('./' + W + 'p') if out_body is not None else []
        text_wrap_breaks = [
            b for p in out_top for b in p.iter(W + 'br')
            if b.get(W + 'type', 'textWrapping') == 'textWrapping'
        ]
        rows_without_cant_split = [text(r) for r in out_root.iter(W + 'tr') if r.find('./' + W + 'trPr/' + W + 'cantSplit') is None]
        page_break_values = [text(p).strip() for p in out_root.iter(W + 'p') if p.find('./' + W + 'pPr/' + W + 'pageBreakBefore') is not None and text(p).strip().startswith('Tabla')]
        rel_media = [n for n in source.namelist() if n.endswith('.rels') or n.startswith('word/media/')]
        protected_diff = [n for n in rel_media if sha256(source.read(n)) != sha256(out.read(n))]
        changed_parts = [n for n in source.namelist() if sha256(source.read(n)) != sha256(out.read(n))]
        validations = {
            'zip_integrity': out.testzip() is None,
            'package_parts_and_order_preserved': source.namelist() == out.namelist(),
            'citavi_tags_preserved': before['citation_tags'] == after['citation_tags'],
            'citavi_instruction_payloads_preserved': before['citation_instr'] == after['citation_instr'],
            'apa7_results_applied': visible_ok,
            'no_known_malformed_citation_patterns': not malformed,
            'table_count_56_preserved': after['tables'] == before['tables'] == 56,
            'table_cell_text_and_structure_preserved': before['table_matrix'] == after['table_matrix'],
            'drawings_55_preserved': after['drawings'] == before['drawings'] == 55,
            'relationships_and_media_byte_preserved': not protected_diff,
            'no_manual_text_wrapping_breaks': not text_wrap_breaks,
            'no_long_body_majority_bold': not long_bold,
            'heading_styles_not_italic': not italic_heading_styles,
            'all_table_rows_cannot_split': not rows_without_cant_split,
            'compatibility_setting_present': out_settings.find('.//' + W + 'doNotExpandShiftReturn') is not None,
            'requested_table_page_breaks_present': len(page_break_values) >= len(TABLE_CAPTIONS_NEW_PAGE) + 4,
        }
        report['validations'] = [{'name': k, 'ok': v} for k, v in validations.items()]
        report['validation_details'] = {
            'malformed_citation_values': malformed,
            'long_body_majority_bold': long_bold,
            'italic_heading_styles': italic_heading_styles,
            'rows_without_cant_split': rows_without_cant_split,
            'table_page_break_values': page_break_values,
            'protected_differences': protected_diff,
        }
        report['changed_package_parts'] = changed_parts

    failures = [v for v in report['validations'] if not v['ok']]
    report['validation_summary'] = {'passed': len(report['validations']) - len(failures), 'total': len(report['validations']), 'failed': len(failures), 'all_ok': not failures}
    report['destination_sha256'] = sha256(DST.read_bytes())
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({'document': str(DST), 'report': str(REPORT), 'citation_changes': len(report['citation_changes']), 'validation': report['validation_summary'], 'changed_parts': report['changed_package_parts']}, ensure_ascii=False, indent=2))
    if failures:
        print(json.dumps({'failures': failures, 'details': report['validation_details']}, ensure_ascii=False, indent=2), file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
