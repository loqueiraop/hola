#!/usr/bin/env python3
"""Comprueba que cada tabla financiera y sus continuaciones previstas estén rotuladas/cerradas."""
from __future__ import annotations
import json,re
from pathlib import Path
import fitz
ROOT=Path('/projects/sandbox/hola')
PDF=ROOT/'analisis/render/Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA FINAL.pdf'
OUT=ROOT/'analisis/verificacion_tablas_financieras_pdf.json'
doc=fitz.open(PDF)
pages=[page.get_text('text') for page in doc]
checks={}
details={}
# Marcadores inequívocos de cierre o de la última cuadrícula lógica.
markers={
 14:'Meses de capital de trabajo',
 15:'TOTAL INVERSIONES',
 16:'51,680,445',
 17:'29,192,969',
 18:'Cargas patronales mensuales',
 19:'2,119,963',
 20:'3,044,290',
 21:'89,500',
 22:'2,686,438',
 23:'Saldo final',
 24:'12,202',
 25:'50,291,726',
 26:'49,999,080',
}
for number,marker in markers.items():
    caption_pages=[i+1 for i,value in enumerate(pages) if re.search(rf'\bTabla\s+{number}\b',value)]
    marker_pages=[i+1 for i,value in enumerate(pages) if marker in value]
    shared=sorted(set(caption_pages)&set(marker_pages))
    checks[f'table_{number}_caption_and_final_marker_share_page']=bool(shared)
    details[str(number)]={'caption_pages':caption_pages,'marker_pages':marker_pages,'shared_pages':shared,'marker':marker}
# Regresiones concretas observadas durante la revisión.
checks['table_15_complete_on_caption_page']=any(re.search(r'(?m)^Tabla 15\s*$',pages[p-1]) and 'TOTAL INVERSIONES' in pages[p-1] and 'Nota. Elaboración propia, 2026.' in pages[p-1] for p in details['15']['caption_pages'])
checks['table_22_complete_on_caption_page']=any(re.search(r'(?m)^Tabla 22\s*$',pages[p-1]) and 'Tabla 22 (continuación)' in pages[p-1] and '2,686,438' in pages[p-1] and 'Nota. Elaboración propia, 2026.' in pages[p-1] for p in details['22']['caption_pages'])
report={'pdf':str(PDF),'pdf_pages':len(doc),'checks':checks,'all_ok':all(checks.values()),'details':details}
OUT.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'output':str(OUT),'all_ok':report['all_ok'],'failed':[k for k,v in checks.items() if not v]},ensure_ascii=False,indent=2))
raise SystemExit(0 if report['all_ok'] else 1)
