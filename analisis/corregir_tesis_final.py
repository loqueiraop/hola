#!/usr/bin/env python3
"""Genera la corrección maestra de la tesis desde el DOCX original.

La transformación usa exclusivamente zipfile + lxml. Conserva todas las partes
OOXML, no actualiza índices y verifica los campos Citavi, tablas, dibujos,
relaciones y medios antes de aceptar el resultado.
"""
from __future__ import annotations

import copy
import hashlib
import json
import math
import re
import sys
import zipfile
from collections import Counter
from pathlib import Path
from typing import Any

from lxml import etree

ROOT = Path("/projects/sandbox/hola")
SRC = ROOT / "Tesis Jaime Fredy Horacio Avance 18 (1).docx"
DST = ROOT / "Tesis Jaime Fredy Horacio Avance 18 - CORREGIDA BORRADOR.docx"
REPORT = ROOT / "analisis/informe_correccion_tesis_final.json"

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
A = "{http://schemas.openxmlformats.org/drawingml/2006/main}"
XMLSPACE = "{http://www.w3.org/XML/1998/namespace}space"
PARSER = etree.XMLParser(remove_blank_text=False, resolve_entities=False, huge_tree=True)

EXPECTED = {
    "package_parts": 196,
    "sdt_total": 80,
    "citavi_placeholders": 75,
    "citavi_bibliographies": 1,
    "citavi_non_sdt": 1,
    "bibliography_entry_paragraphs": 58,
    "tables": 56,
    "drawings": 55,
    "media_parts": 33,
}

# Plantillas de párrafo. {CITAVI} conserva, en la misma posición lógica, cada
# control Citavi del párrafo; el instrText nunca se modifica.
REWRITES: dict[int, str] = {
    242: "La Organización de las Naciones Unidas para el Desarrollo Industrial (ONUDI) promueve un desarrollo industrial inclusivo y sostenible y, mediante cooperación técnica, ofrece una metodología ordenada para formular y evaluar proyectos {CITAVI}",
    244: "La metodología ONUDI organiza el ciclo de vida del proyecto en tres fases complementarias:",
    245: "1) Preinversión: reúne los estudios que sustentan la decisión sobre la viabilidad del proyecto.",
    246: "2) Inversión: comprende el montaje físico y las actividades necesarias para iniciar la operación.",
    247: "3) Operación: comienza con la puesta en marcha y la generación de los beneficios previstos; en esta fase el proyecto funciona como una empresa en actividad {CITAVI}",
    252: "El estudio de mercado reúne y analiza información sobre necesidades, hábitos y decisiones de compra para definir a quién se dirige la oferta y cómo puede atenderse ese segmento {CITAVI}",
    254: "El estudio técnico determina si la propuesta puede ejecutarse en las condiciones del entorno, define el proceso requerido y orienta el uso de los recursos para obtener el bien o servicio previsto {CITAVI}",
    256: "El estudio legal identifica las normas que condicionan la puesta en marcha y la operación del negocio, así como sus efectos sobre los costos y beneficios. La evaluación debe considerar, entre otros, los siguientes conceptos:",
    257: "Patentes, licencias y permisos municipales.",
    258: "Preparación de contratos laborales y comerciales.",
    259: "Verificación de la posesión y vigencia de los títulos de propiedad.",
    260: "Costos de inscripción en los registros públicos de propiedad.",
    261: "Registro y protección de marcas.",
    262: "Aranceles y autorizaciones de importación.",
    263: "Indemnizaciones asociadas con desahucios.",
    264: "Contratos con entidades de seguridad y prevención laboral.",
    265: "Obligaciones derivadas de accidentes de trabajo.",
    266: "Tratamiento fiscal de las depreciaciones y amortizaciones contables.",
    267: "Impuestos sobre las ganancias, la propiedad y el valor agregado.",
    268: "Regulaciones internacionales aplicables.",
    270: "El estudio administrativo define cómo se dirigirá el proyecto. Incluye la planeación estratégica, la estructura organizacional y la gestión del talento humano, de modo que funciones, perfiles y recursos queden alineados con los objetivos empresariales {CITAVI}",
    272: "El estudio de impacto ambiental integra distintas disciplinas para anticipar y valorar los efectos de una intervención, proponer medidas preventivas o correctivas y aportar evidencia para la decisión de la autoridad competente sobre su conveniencia ambiental y social {CITAVI}",
    278: "El enfoque de marco lógico sintetiza los elementos necesarios para diseñar, monitorear y evaluar un proyecto y facilita que los participantes compartan una estructura común de análisis (Rios, 2019).",
    279: "Al concentrar la información esencial en una matriz acordada con los involucrados, el método favorece la comunicación, orienta los esfuerzos y hace explícitos los objetivos del programa {CITAVI}",
    280: "La fuente consultada organiza el diseño del marco lógico en tres aspectos: coherencia, viabilidad y variabilidad {CITAVI}",
    281: "Su uso por organismos de desarrollo se explica por la capacidad de relacionar el problema, la justificación, los objetivos, la información necesaria y los criterios con los que se juzgará el desempeño {CITAVI}",
    283: "La matriz de marco lógico resume la lógica del proyecto en una estructura de cuatro filas y cuatro columnas.",
    284: "Las filas corresponden a cuatro niveles: fin, propósito, componentes y actividades {CITAVI}",
    285: "El fin describe la situación de desarrollo a la que el proyecto espera contribuir en el largo plazo.",
    286: "El propósito expresa el cambio directo que se espera observar al finalizar la intervención.",
    287: "Los componentes son los productos que deben entregarse durante la ejecución, mientras que las actividades reúnen las tareas necesarias para producirlos {CITAVI}",
    288: "Las columnas ordenan cuatro tipos de información:",
    289: "La primera presenta el resumen narrativo de los objetivos y de las actividades vinculadas con su cumplimiento {CITAVI}",
    290: "La segunda contiene los indicadores para seguir avances y resultados; la tercera identifica los medios de verificación de los datos utilizados para calcularlos.",
    291: "La cuarta registra los supuestos externos cuya ocurrencia puede afectar el logro de los objetivos {CITAVI}",
    292: "JICA. La Agencia de Cooperación Internacional del Japón es el organismo japonés de cooperación técnica orientado a apoyar el desarrollo socioeconómico y la colaboración internacional {CITAVI}",
    293: "JICA utiliza la evaluación para valorar la pertinencia y la eficacia de sus actividades en distintos momentos del ciclo —antes, durante, al cierre y después de la intervención—, mejorar la gestión y rendir cuentas sobre los resultados {CITAVI}",
    294: "El sistema de evaluación de JICA persigue tres propósitos:",
    295: "1) Incorporar los resultados de la evaluación en la operación y gestión de los proyectos.",
    296: "2) Fortalecer el aprendizaje del personal y de las organizaciones participantes para mejorar la ejecución.",
    297: "3) Difundir la información necesaria para sustentar la rendición de cuentas {CITAVI}",
    299: "El procedimiento descrito para proyectos JICA involucra al equipo del proyecto, especialistas japoneses, consultoría nacional y representantes de los grupos de trabajo y del comité consultivo {CITAVI}",
    300: "La secuencia metodológica se desarrolla en cuatro pasos:",
    301: "Primero se diagnostican las capacidades del grupo objetivo y sus necesidades de formación. Después se diseña el programa de capacitación, se selecciona la facilitación, se realiza un diagnóstico previo y se establece un cronograma detallado {CITAVI}",
    302: "El tercer paso comprende la ejecución y el seguimiento: se organizan el espacio y los equipos, se controla la asistencia y se recoge retroalimentación de participantes y facilitadores.",
    303: "El cuarto paso convierte lo aprendido en un plan de acción: los equipos formulan, presentan, ajustan, ejecutan y evalúan las actividades previstas {CITAVI}",
    305: "ZOPP —planificación de proyectos orientada a objetivos— articula técnicas participativas para analizar problemas, alternativas, riesgos y soluciones de manera transparente; su estructura se relaciona con el enfoque de marco lógico {CITAVI}",
    307: "Sus componentes centrales son una secuencia explícita de planificación, la documentación continua de los acuerdos mediante recursos visuales y el trabajo interdisciplinario con las partes interesadas y beneficiarias {CITAVI}",
    309: "La Metodología General Ajustada (MGA) funciona como un sistema de información para identificar, preparar, evaluar y programar proyectos de inversión, y guía los estudios ex ante que respaldan la decisión de invertir {CITAVI}",
    311: "La preinversión concentra la formulación, estructuración y evaluación técnica, social, ambiental, jurídica y financiera. En ella se delimita el problema y se comparan alternativas de solución {CITAVI}",
    312: "En el nivel de perfil se recopila información primaria y secundaria disponible sobre proyectos semejantes, mercados y beneficiarios para estimar, de forma inicial, costos y beneficios.",
    313: "En la fase siguiente se profundizan las alternativas seleccionadas mediante estudios técnicos que reducen la incertidumbre y permiten examinar cómo los cambios en demanda, oferta, inversión y operación afectan el valor presente neto.",
    314: "El nivel avanzado detalla la solución técnica y prepara su implementación: estructura de financiamiento, organización administrativa, cronograma y plan de seguimiento.",
    315: "La inversión ejecuta las actividades necesarias para entregar los bienes y servicios definidos en el alcance. El seguimiento del avance permite corregir desviaciones durante la implementación {CITAVI}",
    316: "La operación comprende el periodo en que los productos del proyecto entran en funcionamiento y generan los beneficios vinculados con sus objetivos {CITAVI}",
    318: "La gestión del ciclo del proyecto (PCM) proporciona una secuencia para organizar decisiones, consultar a las partes interesadas y utilizar información relevante durante toda la intervención.",
    319: "El ciclo considerado en esta revisión comprende seis fases: programación, identificación, formulación, implementación, evaluación y cierre.",
    320: "Las fases son progresivas: cada decisión se apoya en los resultados de la anterior y los hallazgos de evaluación y auditoría alimentan nuevas intervenciones {CITAVI}",
    322: "Programación: se acuerdan las prioridades de desarrollo y se traducen en una estrategia y un programa indicativo.",
    323: "Identificación: se somete la propuesta a una valoración inicial para aceptarla, ajustarla o descartarla antes de comprometer recursos.",
    324: "Formulación: se determina la factibilidad, se precisan los beneficios esperados y se completan las disposiciones técnicas, administrativas y financieras.",
    325: "Implementación: se ejecuta el plan, se controla el uso de recursos y se revisan resultados e informes para decidir si la financiación continúa o se adapta.",
    326: "Evaluación: se contrasta el desempeño alcanzado con los objetivos previstos mediante un estudio planificado y gestionado para ese fin.",
    327: "Cierre: una auditoría verifica la terminación del proyecto, el cumplimiento normativo y los demás criterios establecidos {CITAVI}",
    329: "El enfoque asociado con Sapag y Sapag ordena la preparación y evaluación de proyectos para sustentar decisiones de inversión.",
    330: "Su finalidad es examinar cada alternativa de forma sistemática, reducir errores de formulación y mejorar la probabilidad de alcanzar los resultados esperados {CITAVI}",
    332: "La secuencia revisada se organiza en seis etapas interrelacionadas:",
    333: "Identificación: se delimita la necesidad o el problema y se realiza una primera valoración de la pertinencia y viabilidad de la idea.",
    334: "Prefactibilidad: se comparan con mayor detalle las dimensiones técnica, económica y financiera para decidir si conviene profundizar el proyecto.",
    335: "Factibilidad: se estudian de manera exhaustiva los aspectos técnicos, económicos, financieros, legales y ambientales y se emite una decisión sobre viabilidad y rentabilidad.",
    336: "Diseño y planificación: se define la organización, se programan actividades y recursos y se preparan el cronograma y el presupuesto.",
    337: "Ejecución y control: se implementan las actividades, se monitorea el desempeño y se introducen ajustes cuando aparecen desviaciones.",
    338: "Evaluación y cierre: se comparan los resultados finales con los objetivos y metas y se documentan las conclusiones de la intervención {CITAVI}",
    339: "BPIN/SNIP. El Sistema Nacional de Inversión Pública orienta la identificación, formulación y evaluación de proyectos de inversión y distingue los niveles de idea, perfil, prefactibilidad y factibilidad {CITAVI}",
    341: "La viabilidad técnica aporta los elementos necesarios para comprobar que las alternativas seleccionadas pueden ejecutarse con los recursos y condiciones disponibles.",
    342: "La viabilidad ambiental identifica y cuantifica los efectos de la alternativa, verifica los requisitos de la autoridad competente y estima las medidas y los costos necesarios para prevenir o mitigar impactos.",
    343: "La viabilidad jurídica e institucional determina si cada alternativa se ajusta a las normas vigentes, corresponde al mandato del organismo y puede ser ejecutada durante todo el ciclo de vida del proyecto {CITAVI}",
    345: "Los criterios OCDE/CAD se utilizan para ordenar y hacer comparables las evaluaciones, identificar debilidades recurrentes y facilitar el aprendizaje entre equipos evaluadores {CITAVI}",
    347: "Eficacia: valora en qué medida se alcanzaron los objetivos y exige definir resultados e indicadores adecuados al contexto.",
    348: "Pertinencia: examina la correspondencia entre la intervención y las necesidades y prioridades de la población y del entorno.",
    349: "Eficiencia: relaciona los resultados cualitativos y cuantitativos con los insumos utilizados para determinar si los recursos se emplearon de manera adecuada.",
    350: "Impacto: considera los efectos significativos, positivos o negativos, previstos o imprevistos, que la intervención genera o puede generar.",
    351: "Cobertura: analiza si la respuesta alcanza a la población expuesta y si evita excluir a quienes enfrentan mayores necesidades.",
    352: "Coherencia: revisa la compatibilidad entre la intervención y las políticas humanitarias, de desarrollo, seguridad, comercio y otros ámbitos relacionados.",
    353: "La aplicación sostenida de estos criterios ha permitido mejorar la calidad de las evaluaciones y contrastar resultados entre intervenciones {CITAVI}",
    355: "AIKA es una herramienta desarrollada para valorar la sostenibilidad de proyectos de infraestructura mediante 58 parámetros o créditos agrupados en dimensiones ambientales, sociales, técnicas, económicas, financieras y de gobernanza {CITAVI}",
    357: "La herramienta asigna un grado de sostenibilidad de acuerdo con el porcentaje de puntos aplicables que obtiene el proyecto y con el cumplimiento de la normativa pertinente.",
    358: "AIKA vincula las decisiones del ciclo de vida de la infraestructura con criterios ambientales y de desarrollo equilibrado, de manera que las medidas adoptadas puedan justificarse y medirse (Cepeda 2025).",
    360: "La pasteurización es una etapa central de la industria láctea porque controla microorganismos patógenos y permite conservar las propiedades nutricionales del producto.",
    361: "La comercialización propuesta utiliza un canal detallista atendido por vendedores con rutas de visita definidas.",
    362: "La industria láctea hondureña tiene una presencia relevante y conserva oportunidades de crecimiento en transformación, distribución y productos especializados.",
    364: "La producción primaria se distribuye entre pequeños y medianos ganaderos, mientras que la industrialización y la distribución nacional se concentran en empresas de mayor tamaño, entre ellas Lacthosa con su marca Sula.",
    365: "Las importaciones de productos lácteos muestran que una parte de la demanda interna se cubre con oferta externa y que existe espacio para producción nacional.",
    366: "En los municipios rurales, una marca regional puede diferenciarse mediante identidad local y cercanía con productores y clientes.",
    368: "La alternativa no consiste en replicar la oferta de las empresas de mayor escala, sino en concentrarse en:",
    375: "Entre los factores que explican la evolución del sector se encuentran:",
    381: "La leche combina proteínas, grasas, carbohidratos, vitaminas y minerales. Su función como primer alimento de los mamíferos evidencia su densidad nutricional y su importancia en las primeras etapas de vida.",
    382: "La producción láctea se relaciona con la domesticación de especies herbívoras, elegidas históricamente por aportar leche, carne y pieles con menores riesgos de manejo que los animales carnívoros; esas especies siguen sosteniendo la actividad moderna {CITAVI}",
    383: "De acuerdo con {CITAVI} el proceso de leche pasteurizada comprende diez fases: ordeño; refrigeración y almacenamiento; transporte; recepción; control de calidad; estandarización; homogeneización; pasteurización; empaque; y, como etapa final, distribución y despacho.",
    385: "La investigación adoptó un enfoque mixto. El componente cuantitativo integró la encuesta, la estimación de la demanda, la evaluación financiera y la simulación de riesgos; el diseño del componente cualitativo contempló entrevistas semiestructuradas, percepciones de actores y análisis del entorno competitivo.",
    386: "La metodología ONUDI articuló los estudios sectorial, de mercado, técnico, organizacional, legal, ambiental y financiero, junto con el análisis de riesgos, en una secuencia común de evaluación de prefactibilidad.",
    387: "Para cada objetivo se definieron la información requerida, sus fuentes, los instrumentos de recolección y el procedimiento de análisis.",
    389: "El estudio sectorial se diseñó para combinar fuentes documentales —estadísticas oficiales, informes y literatura especializada— con entrevistas a productores, proveedores, distribuidores y representantes institucionales. La información prevista abarcó los factores políticos, económicos, sociales, tecnológicos, ecológicos y legales, además de las condiciones de rivalidad, proveedores, clientes, entrantes y sustitutos.",
    390: "Como instrumento cualitativo se definió una entrevista semiestructurada, sujeta a validación mediante juicio de expertos, con informantes seleccionados intencionalmente por su experiencia en el sector. Se planificó entrevistar entre diez y quince actores clave; como el documento no registra cuántas entrevistas se ejecutaron, sus resultados no se presentan como evidencia confirmada.",
    391: "El diseño previó examinar el entorno macro con PESTEL y la competencia con el modelo de las cinco fuerzas de Porter, categorizar las respuestas y triangularlas con fuentes secundarias.",
    393: "El estudio de mercado evaluó la demanda, la competencia, el producto y la estrategia de comercialización. Utilizó información primaria de establecimientos del canal detalle en la Región Centro y fuentes secundarias, incluidas estadísticas, informes sectoriales y bases de clientes, para delimitar el nicho de mercado.",
    394: "La encuesta estructurada fue validada por juicio de expertos. Se obtuvieron n=63 respuestas de una población declarada de N=500 establecimientos. El documento no conserva evidencia suficiente del procedimiento de selección ni del tratamiento de la no respuesta; por ello, los resultados se interpretan de forma descriptiva y no se les atribuye un margen de error probabilístico. Como referencia teórica, si se asumieran muestreo aleatorio simple, 95 % de confianza y máxima variabilidad (p=q=0,5), el error sería aproximadamente ±11,55 %, no ±5 %.",
    395: "Los datos se analizaron con estadística descriptiva para caracterizar hábitos, segmentar el mercado y estimar la demanda. La oferta competidora se comparó por producto, precio, comercialización y posicionamiento, y el análisis incorporó tendencias tecnológicas, regulatorias y culturales.",
    399: "La encuesta estructurada recopiló hábitos de compra, percepciones, problemas con el producto actual y disposición a cambiar de proveedor. Respondieron 63 establecimientos vinculados con el consumo de leche y derivados dentro de una población objetivo declarada de 500 tiendas y negocios; ante la ausencia de trazabilidad del procedimiento de selección, los resultados no se extrapolan estadísticamente a toda la población.",
    414: "El estudio técnico evaluó la viabilidad operativa mediante ingeniería de procesos. Integró el dimensionamiento y la localización, el flujo productivo, la tecnología, las materias primas y la distribución de planta para estimar recursos y costos.",
    416: "La ingeniería del proyecto cuantificó obras e infraestructura, capital de trabajo, materias primas, personal y costos de operación. El análisis se orientó a definir un proceso eficiente y a vincular las decisiones técnicas con el modelo financiero.",
    421: "El tamaño se determinó a partir de la demanda estimada, la capacidad de los equipos, las inversiones y los costos de operación. Las alternativas de capacidad se compararon por su utilización y su efecto en los flujos y en el valor actual neto.",
    423: "La localización se evaluó como una decisión de largo plazo mediante criterios de acceso al mercado, abastecimiento de leche cruda, disponibilidad de factores productivos y condiciones legales y tributarias.",
    473: "La industria láctea aporta a la economía hondureña mediante la actividad de productores de diferentes escalas. La fuente consultada estima una contribución del 15 % al PIB, 350,000 empleos directos y 250,000 indirectos, y destaca oportunidades de eficiencia en el uso de energía, materias primas y agua {CITAVI}",
    1618: "En respuesta al objetivo general, la evaluación integrada indica que la producción y distribución de leche fluida pasteurizada para un nicho de mercado en Honduras es prefactible en las dimensiones comercial, técnica, organizacional, legal, ambiental, financiera y de riesgos.",
    1619: "Respecto del objetivo sectorial, PESTEL y el modelo de las cinco fuerzas de Porter identificaron rivalidad elevada y productos sustitutos, junto con una oportunidad de diferenciación en segmentos que valoran calidad e inocuidad. Estos resultados respaldan una estrategia focalizada, sin asegurar por sí solos una ventaja competitiva sostenible.",
    1620: "En relación con el objetivo de mercado, las 63 encuestas aplicadas a establecimientos mostraron intención de compra, consumo frecuente de lácteos y disposición de pago compatible con el precio propuesto. La evidencia delimita una oportunidad en el canal HORECA y en comercios minoristas que priorizan frescura, inocuidad y calidad.",
    1621: "Para el objetivo técnico, Comayagua resultó la localización seleccionada por su acceso logístico, cercanía al abastecimiento de leche cruda y conexión con los mercados previstos. La tecnología y la capacidad inicial definidas cubren la demanda proyectada y permiten evaluar ampliaciones posteriores.",
    1622: "En los objetivos organizacional y legal se identificaron la estructura de gestión y los requisitos aplicables a la producción y comercialización. Su cumplimiento exige inversiones, permisos y controles, pero no se detectaron impedimentos técnicos que invaliden la propuesta.",
    1623: "El objetivo ambiental permitió reconocer como aspectos principales el consumo de agua, los efluentes y los residuos. Las medidas de prevención, tratamiento y seguimiento propuestas permiten gestionar esos impactos; su eficacia deberá comprobarse durante la factibilidad y la operación.",
    1624: "Para el objetivo financiero, el escenario sin financiamiento obtuvo un VAN de L 15,370,631, una TIR de 55.95 %, un periodo de recuperación de 3.67 años y una relación beneficio/costo de 1.24. Con financiamiento, el VAN fue L 15,459,686 y la TIR 63.62 %. Bajo los supuestos del modelo, ambos escenarios generan valor.",
    1625: "En cuanto al objetivo de riesgos, la simulación consideró once eventos. La pérdida esperada equivale al 15.1 % del valor actual neto promedio del inversionista; por tanto, el proyecto conserva margen frente a la exposición modelada, aunque requiere mantener controles y actualizar los supuestos.",
    1626: "En síntesis, los resultados justifican avanzar a una fase de factibilidad detallada, en la que deberán confirmarse la demanda, las cotizaciones, los permisos, el abastecimiento y las medidas de mitigación antes de decidir la inversión.",
}

STYLE_BY_INDEX = {
    # Se conservan los niveles originales de los estudios que ya coinciden con
    # el índice (por ejemplo, Estudio Administrativo y Análisis de riesgo).
    277: "Ttulo3", 282: "Ttulo4", 292: "Normal", 298: "Ttulo4",
    304: "Ttulo3", 306: "Ttulo4", 308: "Ttulo3", 310: "Ttulo4",
    317: "Ttulo3", 321: "Ttulo4", 328: "Ttulo3", 331: "Ttulo4",
    339: "Normal", 340: "Ttulo4", 344: "Ttulo3", 346: "Ttulo4",
    354: "Ttulo3", 356: "Ttulo4",
    474: "Ttulo2", 475: "Ttulo3", 479: "Ttulo4", 481: "Ttulo4",
    483: "Ttulo3", 484: "Ttulo4", 487: "Ttulo4", 489: "Ttulo3",
    490: "Ttulo4", 493: "Ttulo4", 495: "Ttulo4",
    **{i: "Normal" for i in list(range(1618, 1627)) + list(range(1628, 1636))},
}

STYLE_BY_EXACT_TEXT = {
    "Impuesto sobre la Renta (ISR).": "Ttulo3",
    "Depreciación y amortización.": "Ttulo3",
    "Seguridad social y aportes patronales.": "Ttulo3",
    "Prestaciones laborales.": "Ttulo3",
    "Impuesto sobre Ventas (ISV)": "Ttulo3",
    "Arrastre de pérdidas fiscales.": "Ttulo3",
    "Impuesto Municipal de Industria, Comercio y Servicios": "Ttulo3",
    "Capital de trabajo.": "Ttulo3",
}

CITATION_RESULTS = {
    "CitaviPlaceholder#db57d2b5-5047-427b-ba26-55e39b558c2d": "(Sapag Chain, 2008, pág. 22).",
    "CitaviPlaceholder#40586cf3-36a4-452b-8e98-e1ebd517a2b1": "(ONUDI, 2019, pág. 16).",
    "CitaviPlaceholder#5a5468d2-69b6-4441-a313-b7931eda8833": "(Domínguez Cedeño, 2018, pág. 7).",
    "CitaviPlaceholder#be2ccc5c-f34f-472b-91d4-0374ded4c98f": "(Sierra et al 2023)",
    "CitaviPlaceholder#771bcc9c-728c-4359-bfd5-0d468164c5e6": "(OHN 18, 2018).",
    "CitaviPlaceholder#8c4fd9fe-1ff0-4f0e-9d40-5a02d2de55e8": "(OHN 18, 2018).",
    "CitaviPlaceholder#dadabde3-befb-4f5e-a9e6-5bbd4f2a50c8": "(OHN 18, 2018).",
    "CitaviPlaceholder#50a43b28-8e13-4317-bdb6-be8a3c528117": "(OHN 18, 2018).",
    "CitaviPlaceholder#86772af3-e489-4d1f-99aa-f54396dde7be": "(Agencia de Regulación Sanitaria, 2026a).",
    "CitaviPlaceholder#2a2c6cb4-1954-4c0e-998a-f4e17d7c1672": "(Agencia de Regulación Sanitaria, 2026b).",
    "CitaviPlaceholder#06a9f38d-5982-4cf4-b527-382e4ade5870": "(Agencia de Regulación Sanitaria, 2024).",
    "CitaviPlaceholder#bee99d04-8945-45ee-b901-8873b25045ba": "(Agencia de Regulación Sanitaria, 2024).",
}

BIBLIOGRAPHY_REPLACEMENTS = {
    "ARSA, del #1.": "ARSA.",
    "Guia Ayuda al Ciudadano": "Guía de ayuda al ciudadano",
    "Solicitud Para Licencia Sanitaria para Establecimiento": "Solicitud para licencia sanitaria para establecimiento",
    "Marco logico.": "Marco lógico.",
    "gomez (2004)": "Gómez (2004)",
    "Planficación de Proyectos": "Planificación de proyectos",
    "Libro_Estudio_Mercado": "Libro de estudio de mercado",
    "luis (2024)": "Luis (2024)",
    "la !:eche Pasteurizada": "la leche pasteurizada",
    "Quinta edicion": "Quinta edición",
    "Segunda Edicion": "Segunda edición",
    "gestión ” financiera eficiente ”": "gestión financiera eficiente",
    "metodología JICA,.": "metodología JICA.",
    "https://www.bing.com/ck/a?!&&p=8489c8adb948637b63e7939927203f709c086b457b92f103b01bdf80634d9445JmltdHM9MTc3MTIwMDAwMA&ptn=3&ver=2&hsh=4&fclid=37d69565-f4a5-6530-0e77-8089f5b9641e&psq=Jica+metodologia+que+es&u=a1aHR0cDovL3d3dy5wdG9sb21lby51bmFtLm14OjgwODAveG1sdWkvYml0c3RyZWFtL2hhbmRsZS8xMzIuMjQ4LjUyLjEwMC84Ny9BNi5wZGYucGRmP3NlcXVlbmNlPTY": "https://www.ptolomeo.unam.mx:8080/xmlui/bitstream/handle/132.248.52.100/87/A6.pdf.pdf?sequence=6",
    "OHN 18: Leche cruda de vaca — Requisitos. OHN 18.": "OHN 18 (2018): Leche cruda de vaca — Requisitos. Norma OHN 18:2018.",
    "https://www.cnpml-honduras.org/sectorlacteos/?utm_source": "https://www.cnpml-honduras.org/sectorlacteos/",
    "implementación de la Programa Ejecutivo": "implementación del Programa Ejecutivo",
    "Redalyc.El estudio": "Redalyc. El estudio",
    "Boletin del instituto": "Boletín del instituto",
    "Tetra pack.": "Tetra Pak.",
    "Planificación de proyectos Orientado a Objetivos": "Planificación de proyectos orientada a objetivos",
}

EXACT_PARAGRAPH_REPLACEMENTS = {
    2: ("Estudio de prefactibilidad para la producción y distribución de leche pasteurizada a mercado nicho en Honduras",
        "Estudio de prefactibilidad para la producción y distribución de leche pasteurizada para un mercado de nicho en Honduras"),
    475: ("1. Condiciones macro del entorno (nivel país) Condiciones económicas generales",
          "Condiciones macro del entorno (nivel nacional)"),
    497: ("ANALISIS PESTEL:", "ANÁLISIS PESTEL:"),
    569: ("CINCO FUERZAS DE PORTER:", "Cinco fuerzas de Porter:"),
    800: ("Enfasis en calidad y valor percibido por porción.", "Énfasis en calidad y valor percibido por porción."),
    807: ("(p 1-4).", "(pp. 1–4)."),
    808: ("uno punto uno por ciento", "1,1 %"),
    809: ("Se puede decir que las tendencias muestran una industria orientada hacia el bienestar integral y el consumo consciente, y esto para el sector lácteo esto implica",
          "Las tendencias muestran una industria orientada hacia el bienestar integral y el consumo consciente; para el sector lácteo, esto implica"),
    1012: ("Agencia de Regulación Sanitaria(ARSA)", "Agencia de Regulación Sanitaria (ARSA)"),
    1659: ("¿Cómo percibe la calidad de este producto en base a su presentación?",
           "¿Cómo percibe la calidad de este producto con base en su presentación?"),
    1641: ("aplicado a 66 establecimientos", "aplicado a 63 establecimientos"),
    1680: ("La muestra final estuvo conformada por 66 establecimientos comerciales",
           "La muestra final estuvo conformada por 63 establecimientos comerciales"),
}

TABLE_PORTER_REPLACEMENTS = {
    "PROVEDORES": "PROVEEDORES",
    "IMPLICACION": "IMPLICACIÓN",
    "orgazativa": "organizativa",
    "estabilidad.La": "estabilidad. La",
    "responsables.Crear": "responsables. Crear",
    "enforcase": "enfocarse",
    "mayor demando": "mayor demanda",
    "mercado nicho": "mercado de nicho",
}

WARNINGS = [
    {
        "code": "WEISSBACH_AUTHOR_MISSING",
        "paragraph": 252,
        "tag": "CitaviPlaceholder#775bafee-bd31-4727-853b-ec73c8136f45",
        "detail": "El resultado visible conserva (Weissbach, 2021), pero el JSON Citavi incrustado no contiene autor. No se inventó ni se modificó instrText; debe corregirse en la base Citavi.",
    },
    {
        "code": "CITAVI_VISIBLE_RESULTS_ONLY",
        "detail": "Los resultados visibles solicitados se corrigieron sin tocar instrText. Una actualización posterior desde Citavi puede regenerarlos; conviene corregir también los registros externos.",
    },
    {
        "code": "SAMPLE_60_66_RECONCILED",
        "detail": "El original declaraba 60 establecimientos en metodología y 66 en dos pasajes del anexo, mientras los gráficos y resultados contienen 63 respuestas. Se unificó la muestra efectiva en 63; los 60 clientes activos del plan comercial se conservaron. Como no hay trazabilidad del procedimiento de selección, el análisis se declara descriptivo; ±11,55 % se presenta sólo como referencia teórica bajo muestreo aleatorio simple, no como precisión comprobada.",
    },
    {
        "code": "UNSOURCED_CONTEXT_NOT_INVENTED",
        "detail": "No se añadieron autores ni citas a afirmaciones sin fuente de las páginas 50–51. Se corrigieron jerarquía y mecánica, y se documenta la necesidad de verificación académica posterior.",
    },
    {
        "code": "INDICES_NOT_UPDATED",
        "detail": "La tabla de contenido y los índices manuales no se actualizaron, conforme al requisito; deberán actualizarse después del render final.",
    },
]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def parse(data: bytes) -> etree._Element:
    return etree.fromstring(data, parser=PARSER)


def serialize(root: etree._Element) -> bytes:
    return etree.tostring(root, xml_declaration=True, encoding="UTF-8", standalone=True)


def full_text(node: etree._Element) -> str:
    return "".join(t.text or "" for t in node.iter(W + "t"))


def paragraph_style(p: etree._Element) -> str:
    st = p.find("./" + W + "pPr/" + W + "pStyle")
    return st.get(W + "val", "") if st is not None else ""


def set_paragraph_style(p: etree._Element, value: str) -> None:
    ppr = p.find("./" + W + "pPr")
    if ppr is None:
        ppr = etree.Element(W + "pPr")
        p.insert(0, ppr)
    st = ppr.find("./" + W + "pStyle")
    if st is None:
        st = etree.Element(W + "pStyle")
        ppr.insert(0, st)
    st.set(W + "val", value)


def ancestor(node: etree._Element, tag: str) -> etree._Element | None:
    cur = node.getparent()
    while cur is not None:
        if cur.tag == tag:
            return cur
        cur = cur.getparent()
    return None


def sdt_tag(sdt: etree._Element) -> str:
    tag = sdt.find("./" + W + "sdtPr/" + W + "tag")
    return tag.get(W + "val", "") if tag is not None else ""


def is_citavi_sdt(sdt: etree._Element) -> bool:
    return sdt.tag == W + "sdt" and sdt_tag(sdt).startswith("CitaviPlaceholder#")


def in_citavi(node: etree._Element) -> bool:
    cur = node.getparent()
    while cur is not None:
        if is_citavi_sdt(cur):
            return True
        cur = cur.getparent()
    return False


def set_text_node(t: etree._Element, value: str) -> None:
    t.text = value
    if value[:1].isspace() or value[-1:].isspace():
        t.set(XMLSPACE, "preserve")
    elif XMLSPACE in t.attrib:
        del t.attrib[XMLSPACE]


def atomic_nodes(node: etree._Element) -> list[tuple[str, etree._Element]]:
    out: list[tuple[str, etree._Element]] = []

    def walk(cur: etree._Element) -> None:
        if is_citavi_sdt(cur):
            out.append(("sdt", cur))
            return
        if cur.tag == W + "t":
            out.append(("text", cur))
            return
        for child in cur:
            walk(child)

    walk(node)
    return out


def rewrite_paragraph(p: etree._Element, template: str) -> None:
    atoms = atomic_nodes(p)
    sdts = [n for kind, n in atoms if kind == "sdt"]
    parts = template.split("{CITAVI}")
    if len(parts) != len(sdts) + 1:
        raise ValueError(f"Plantilla incompatible: {len(sdts)} SDT y {len(parts)-1} marcadores")
    groups: list[list[etree._Element]] = [[] for _ in parts]
    group = 0
    for kind, node in atoms:
        if kind == "sdt":
            group += 1
        elif kind == "text":
            groups[group].append(node)
    for i, (value, nodes) in enumerate(zip(parts, groups)):
        if value and not nodes:
            # Inserta un run mínimo solo si el párrafo carece de w:t en ese tramo.
            run = etree.Element(W + "r")
            text = etree.SubElement(run, W + "t")
            set_text_node(text, value)
            if i < len(sdts):
                sdts[i].addprevious(run)
            else:
                p.append(run)
            continue
        for j, text in enumerate(nodes):
            set_text_node(text, value if j == 0 else "")


def citation_result_texts(sdt: etree._Element) -> tuple[list[etree._Element], etree._Element | None]:
    """Devuelve los w:t situados entre fldChar separate y fldChar end."""
    content = sdt.find("./" + W + "sdtContent")
    if content is None:
        raise ValueError(f"SDT sin sdtContent: {sdt_tag(sdt)}")
    inside_result = False
    texts: list[etree._Element] = []
    end_run: etree._Element | None = None
    for child in content:
        fld = child.find(".//" + W + "fldChar")
        if fld is not None:
            fld_type = fld.get(W + "fldCharType", "")
            if fld_type == "separate":
                inside_result = True
                continue
            if fld_type == "end":
                end_run = child
                break
        if inside_result:
            texts.extend(child.iter(W + "t"))
    return texts, end_run


def set_citation_visible(sdt: etree._Element, value: str) -> None:
    content = sdt.find("./" + W + "sdtContent")
    if content is None:
        raise ValueError(f"SDT sin sdtContent: {sdt_tag(sdt)}")
    texts, end_run = citation_result_texts(sdt)
    if not texts:
        if end_run is None:
            raise ValueError(f"Campo Citavi sin fldChar end: {sdt_tag(sdt)}")
        run = etree.Element(W + "r")
        text = etree.SubElement(run, W + "t")
        end_run.addprevious(run)
        texts = [text]
    for i, text in enumerate(texts):
        set_text_node(text, value if i == 0 else "")
    # Algunos resultados originales tenían un punto visible después de fldChar
    # end. Se elimina cuando la puntuación ya forma parte del resultado para
    # evitar tanto ".." como un espacio expandido antes del punto al justificar.
    result_ids = {id(text) for text in texts}
    if value.endswith((".", ",", ";", ":")):
        for extra in content.iter(W + "t"):
            if id(extra) not in result_ids and (extra.text or "").strip() in {".", ",", ";", ":"}:
                set_text_node(extra, "")


def replace_exact_outside_citavi(p: etree._Element, old: str, new: str) -> bool:
    atoms = [n for kind, n in atomic_nodes(p) if kind == "text"]
    for text in atoms:
        value = text.text or ""
        if old in value:
            set_text_node(text, value.replace(old, new, 1))
            return True
    joined = "".join(t.text or "" for t in atoms)
    if old not in joined:
        return False
    # El fallback solo es seguro cuando no hay un SDT Citavi intercalado.
    if any(kind == "sdt" for kind, _ in atomic_nodes(p)):
        return False
    replaced = joined.replace(old, new, 1)
    for i, t in enumerate(atoms):
        set_text_node(t, replaced if i == 0 else "")
    return True


def paragraph_has_ancestor(p: etree._Element, tag: str) -> bool:
    return ancestor(p, tag) is not None


def normalize_mechanics(p: etree._Element) -> int:
    style = paragraph_style(p)
    text = full_text(p)
    if re.match(r"^(ndicemanual|TDC|Tabladecontenido|Index|ndice)", style, re.I):
        return 0
    if re.search(r"\.{4,}", text) or paragraph_has_ancestor(p, W + "tbl"):
        return 0
    changes = 0
    editable = [t for t in p.iter(W + "t") if not in_citavi(t)]
    # Las diéresis que fingían comillas se corrigen por orden dentro del párrafo.
    total_dieresis = sum((t.text or "").count("¨") for t in editable)
    quote_no = 0
    for t in editable:
        original = t.text or ""
        value = original
        if "¨" in value:
            chars = []
            for char in value:
                if char == "¨":
                    chars.append("“" if quote_no == 0 else "”")
                    quote_no += 1
                else:
                    chars.append(char)
            value = "".join(chars)
        value = re.sub(r"(?<=\w)“\s+", " “", value)
        value = re.sub(r"[ \u00a0]+([,;:])", r"\1", value)
        value = re.sub(r"[ \u00a0]+\.(?=\s|$)", ".", value)
        value = re.sub(r",(?=[A-Za-zÁÉÍÓÚÑÜáéíóúñü])", ", ", value)
        value = re.sub(r"(?<=\S) {2,}(?=\S)", " ", value)
        if value != original:
            set_text_node(t, value)
            changes += 1
    # Normaliza espacios dobles que cruzan runs, sin entrar a Citavi.
    for left, right in zip(editable, editable[1:]):
        if not left.text or not right.text:
            continue
        if re.search(r"\S {2,}$", left.text) and not right.text.startswith(" "):
            set_text_node(left, left.text.rstrip() + " ")
            changes += 1
        elif not left.text.endswith(" ") and re.match(r" {2,}\S", right.text):
            set_text_node(right, " " + right.text.lstrip())
            changes += 1
    return changes


def replace_across_text_nodes(scope: etree._Element, old: str, new: str) -> int:
    count = 0
    while True:
        texts = list(scope.iter(W + "t"))
        values = [t.text or "" for t in texts]
        joined = "".join(values)
        start = joined.find(old)
        if start < 0:
            return count
        end = start + len(old)
        cursor = 0
        start_i = end_i = -1
        start_local = end_local = 0
        for i, value in enumerate(values):
            next_cursor = cursor + len(value)
            if start_i < 0 and start < next_cursor:
                start_i, start_local = i, start - cursor
            if end <= next_cursor:
                end_i, end_local = i, end - cursor
                break
            cursor = next_cursor
        if start_i < 0 or end_i < 0:
            raise RuntimeError(f"No se pudo mapear el reemplazo entre runs: {old!r}")
        if start_i != end_i:
            if ancestor(texts[start_i], W + "tc") is not ancestor(texts[end_i], W + "tc"):
                raise RuntimeError(f"El texto {old!r} cruza celdas; edición rechazada")
            prefix = values[start_i][:start_local]
            suffix = values[end_i][end_local:]
            set_text_node(texts[start_i], prefix + new)
            for i in range(start_i + 1, end_i):
                set_text_node(texts[i], "")
            set_text_node(texts[end_i], suffix)
        else:
            value = values[start_i]
            set_text_node(texts[start_i], value[:start_local] + new + value[end_local:])
        count += 1


def apply_porter_table_corrections(root: etree._Element, report: dict[str, Any]) -> None:
    target_tables = [tbl for tbl in root.iter(W + "tbl") if "PROVEDORES" in full_text(tbl) or ("Porter" in full_text(tbl) and "IMPLICACION" in full_text(tbl))]
    if len(target_tables) != 1:
        raise RuntimeError(f"Se esperaba una tabla Porter inequívoca y se encontraron {len(target_tables)}")
    table = target_tables[0]
    changes = []
    for old, new in TABLE_PORTER_REPLACEMENTS.items():
        hits = replace_across_text_nodes(table, old, new)
        if hits == 0:
            raise RuntimeError(f"No se encontró en la tabla Porter el texto verificado {old!r}")
        changes.append({"before": old, "after": new, "count": hits})
    report["replacements"].append({"kind": "porter_table_verified", "changes": changes})


def normalize_document_font_language(root: etree._Element, report: dict[str, Any]) -> None:
    count = 0
    for rpr in root.iter(W + "rPr"):
        normalize_rpr(rpr, set_size=False)
        count += 1
    for lang in root.iter(W + "lang"):
        for attr in ("val", "eastAsia", "bidi"):
            lang.set(W + attr, "es-CO")
    report["formatting"]["document_rpr_font_language_normalized"] = count


def non_calibri_rfonts(root: etree._Element) -> list[dict[str, str]]:
    bad = []
    for fonts in root.iter(W + "rFonts"):
        for attr in ("ascii", "hAnsi", "cs", "eastAsia"):
            value = fonts.get(W + attr)
            if value and value.casefold() != "calibri":
                bad.append({"attribute": attr, "value": value})
    return bad


def non_es_co_lang(root: etree._Element) -> list[dict[str, str]]:
    bad = []
    for lang in root.iter(W + "lang"):
        for attr in ("val", "eastAsia", "bidi"):
            value = lang.get(W + attr)
            if value and value != "es-CO":
                bad.append({"attribute": attr, "value": value})
    return bad


def ensure_child(parent: etree._Element, tag: str) -> etree._Element:
    child = parent.find("./" + tag)
    if child is None:
        child = etree.SubElement(parent, tag)
    return child


def normalize_rpr(rpr: etree._Element, set_size: bool = False) -> None:
    fonts = rpr.find("./" + W + "rFonts")
    if fonts is None:
        fonts = etree.Element(W + "rFonts")
        rpr.insert(0, fonts)
    for attr in ("ascii", "hAnsi", "cs", "eastAsia"):
        fonts.set(W + attr, "Calibri")
    lang = rpr.find("./" + W + "lang")
    if lang is None:
        lang = etree.SubElement(rpr, W + "lang")
    for attr in ("val", "eastAsia", "bidi"):
        lang.set(W + attr, "es-CO")
    if set_size:
        for tag in ("sz", "szCs"):
            size = rpr.find("./" + W + tag)
            if size is None:
                size = etree.SubElement(rpr, W + tag)
            size.set(W + "val", "24")


def normalize_styles(styles: etree._Element, report: dict[str, Any]) -> None:
    # Todas las referencias tipográficas explícitas de estilos quedan en Calibri
    # y todo idioma explícito queda en español (Colombia).
    for rpr in styles.iter(W + "rPr"):
        normalize_rpr(rpr, set_size=False)
    for lang in styles.iter(W + "lang"):
        for attr in ("val", "eastAsia", "bidi"):
            lang.set(W + attr, "es-CO")

    defaults = styles.find("./" + W + "docDefaults")
    if defaults is None:
        defaults = etree.Element(W + "docDefaults")
        styles.insert(0, defaults)
    rpr_default = defaults.find("./" + W + "rPrDefault")
    if rpr_default is None:
        rpr_default = etree.SubElement(defaults, W + "rPrDefault")
    default_rpr = ensure_child(rpr_default, W + "rPr")
    normalize_rpr(default_rpr, set_size=True)

    body_style_ids = {"Normal", "Sinespaciado", "Prrafodelista"}
    for style in styles.findall("./" + W + "style"):
        sid = style.get(W + "styleId", "")
        if sid in body_style_ids:
            rpr = style.find("./" + W + "rPr")
            if rpr is None:
                rpr = etree.SubElement(style, W + "rPr")
            normalize_rpr(rpr, set_size=True)
    report["formatting"]["styles_normalized"] = len(list(styles.iter(W + "style")))


def normalize_theme(theme: etree._Element) -> int:
    changed = 0
    for font_group in ("majorFont", "minorFont"):
        for group in theme.iter(A + font_group):
            latin = group.find("./" + A + "latin")
            if latin is not None and latin.get("typeface") != "Calibri":
                latin.set("typeface", "Calibri")
                changed += 1
    return changed


def is_semantic_paragraph(p: etree._Element) -> bool:
    style = paragraph_style(p)
    return bool(re.match(r"^(Ttulo|Heading|ndicemanual|APA|Figura|Tabla|CitaviBibliography)", style, re.I)) or paragraph_has_ancestor(p, W + "tbl") or p.find(".//" + W + "drawing") is not None


def apply_body_formatting(root: etree._Element, top: list[etree._Element], report: dict[str, Any]) -> None:
    converted = 0
    sized_runs = 0
    semantic_skips = 0
    for p in top:
        style = paragraph_style(p)
        words = len(full_text(p).split())
        if style == "Sinespaciado":
            if words >= 12 and p.find(".//" + W + "drawing") is None:
                set_paragraph_style(p, "Normal")
                converted += 1
            else:
                semantic_skips += 1
        if is_semantic_paragraph(p):
            continue
        for run in p.iter(W + "r"):
            if ancestor(run, W + "drawing") is not None:
                continue
            rpr = run.find("./" + W + "rPr")
            if rpr is None:
                rpr = etree.Element(W + "rPr")
                run.insert(0, rpr)
            normalize_rpr(rpr, set_size=True)
            sized_runs += 1
    report["formatting"].update({
        "no_spacing_body_to_normal": converted,
        "no_spacing_short_semantic_preserved": semantic_skips,
        "body_runs_set_12pt": sized_runs,
    })


def inventory(root: etree._Element, part_names: set[str]) -> dict[str, Any]:
    sdts = list(root.iter(W + "sdt"))
    placeholder_tags = [sdt_tag(s) for s in sdts if sdt_tag(s).startswith("CitaviPlaceholder#")]
    bib = [s for s in sdts if sdt_tag(s).startswith("CitaviBibliography")]
    non_sdt = 0
    instr_values: list[str] = []
    for instr in root.iter(W + "instrText"):
        value = instr.text or ""
        if "Citavi" in value:
            instr_values.append(value)
        if "CitaviPlaceholder" in value and ancestor(instr, W + "sdt") is None:
            non_sdt += 1
    bib_entries = 0
    for p in root.iter(W + "p"):
        if paragraph_style(p) == "CitaviBibliographyEntry" and full_text(p).strip():
            bib_entries += 1
    return {
        "package_parts": len(part_names),
        "sdt_total": len(sdts),
        "citavi_placeholders": len(placeholder_tags),
        "citavi_bibliographies": len(bib),
        "citavi_non_sdt": non_sdt,
        "bibliography_entry_paragraphs": bib_entries,
        "tables": len(list(root.iter(W + "tbl"))),
        "drawings": len(list(root.iter(W + "drawing"))),
        "media_parts": len([n for n in part_names if n.startswith("word/media/") and not n.endswith("/")]),
        "placeholder_tags": placeholder_tags,
        "citavi_instrtext_hashes": [sha256(v.encode("utf-8")) for v in instr_values],
    }


def validate_expected(inv: dict[str, Any], label: str) -> list[dict[str, Any]]:
    out = []
    for key, expected in EXPECTED.items():
        actual = inv[key]
        out.append({"name": f"{label}.{key}", "expected": expected, "actual": actual, "ok": actual == expected})
    return out


def format_error() -> float:
    n, N, z, p, q = 63, 500, 1.96, 0.5, 0.5
    return z * math.sqrt((p * q / n) * ((N - n) / (N - 1))) * 100


def main() -> int:
    if SRC == DST or "REVISADA" in SRC.name.upper():
        raise RuntimeError("La fuente debe ser el original y nunca REVISADA")
    if not SRC.exists():
        raise FileNotFoundError(SRC)

    report: dict[str, Any] = {
        "source": str(SRC), "destination": str(DST),
        "report": str(REPORT), "source_sha256": sha256(SRC.read_bytes()),
        "replacements": [], "citation_results": [], "style_changes": [],
        "formatting": {}, "warnings_and_skipped": WARNINGS, "validations": [],
        "methodology": {"n": 63, "N": 500, "confidence": "95 %", "p": 0.5, "q": 0.5,
                        "finite_population_error_percent_calculated": round(format_error(), 4),
                        "reported_rounded_error": "±11,55 %"},
    }

    with zipfile.ZipFile(SRC, "r") as zin:
        infos = zin.infolist()
        names = [i.filename for i in infos]
        original = {name: zin.read(name) for name in names}
        root = parse(original["word/document.xml"])
        body = root.find(".//" + W + "body")
        if body is None:
            raise RuntimeError("word/document.xml no contiene w:body")
        top = body.findall("./" + W + "p")
        before = inventory(root, set(names))
        report["inventory_before"] = {k: v for k, v in before.items() if k not in {"placeholder_tags", "citavi_instrtext_hashes"}}
        report["validations"].extend(validate_expected(before, "before"))

        # Reescrituras sustantivas identificadas por índice y texto original.
        for index, template in REWRITES.items():
            p = top[index]
            old = full_text(p)
            try:
                rewrite_paragraph(p, template)
            except Exception as exc:
                raise RuntimeError(f"Error al reescribir el párrafo {index}: {exc}") from exc
            new = full_text(p)
            report["replacements"].append({"kind": "professional_rewrite", "paragraph": index, "before": old, "after": new})

        # Reemplazos exactos y conservadores.
        for index, (old, new) in EXACT_PARAGRAPH_REPLACEMENTS.items():
            p = top[index]
            before_text = full_text(p)
            if old not in before_text:
                raise RuntimeError(f"No coincide el reemplazo exacto del párrafo {index}: {old!r}")
            if not replace_exact_outside_citavi(p, old, new):
                raise RuntimeError(f"El texto del párrafo {index} no era editable fuera de Citavi")
            report["replacements"].append({"kind": "exact_verified", "paragraph": index, "before": before_text, "after": full_text(p)})

        # Corrección global de la locución, fuera de índices, tablas y Citavi.
        market_changes = 0
        for i, p in enumerate(top):
            if i == 2 or re.match(r"^ndicemanual", paragraph_style(p), re.I):
                continue
            old = full_text(p)
            if "mercado nicho" in old and replace_exact_outside_citavi(p, "mercado nicho", "mercado de nicho"):
                report["replacements"].append({"kind": "mechanical_phrase", "paragraph": i, "before": old, "after": full_text(p)})
                market_changes += 1
        report["formatting"]["mercado_nicho_corrected"] = market_changes

        # Resultados visibles Citavi solicitados, sin tocar instrText.
        found_tags: dict[str, etree._Element] = {}
        for sdt in root.iter(W + "sdt"):
            tag = sdt_tag(sdt)
            if tag in CITATION_RESULTS:
                found_tags[tag] = sdt
        missing = sorted(set(CITATION_RESULTS) - set(found_tags))
        if missing:
            raise RuntimeError(f"No se encontraron los SDT Citavi requeridos: {missing}")
        for tag, new in CITATION_RESULTS.items():
            sdt = found_tags[tag]
            old = full_text(sdt)
            set_citation_visible(sdt, new)
            report["citation_results"].append({"tag": tag, "before": old, "after": full_text(sdt), "instrText_modified": False})

        # Corrige errores visibles y verificables de la bibliografía generada por
        # Citavi, sin modificar el campo ADDIN ni sus metadatos incrustados.
        bibliography_sdts = [s for s in root.iter(W + "sdt") if sdt_tag(s) == "CitaviBibliography"]
        if len(bibliography_sdts) != 1:
            raise RuntimeError(f"Se esperaba una bibliografía Citavi y se encontraron {len(bibliography_sdts)}")
        bibliography_changes = []
        for old, new in BIBLIOGRAPHY_REPLACEMENTS.items():
            hits = replace_across_text_nodes(bibliography_sdts[0], old, new)
            if hits != 1:
                raise RuntimeError(f"Reemplazo bibliográfico no inequívoco ({hits}): {old!r}")
            bibliography_changes.append({"before": old, "after": new, "count": hits})
        report["replacements"].append({"kind": "citavi_bibliography_visible_verified", "changes": bibliography_changes, "instrText_modified": False})

        # Jerarquía de títulos y cuerpos de conclusiones/recomendaciones.
        for index, target in STYLE_BY_INDEX.items():
            p = top[index]
            old = paragraph_style(p)
            if old != target:
                set_paragraph_style(p, target)
                report["style_changes"].append({"paragraph": index, "text": full_text(p), "before": old, "after": target})
        for i, p in enumerate(top):
            target = STYLE_BY_EXACT_TEXT.get(full_text(p).strip())
            if target and paragraph_style(p) != target:
                old = paragraph_style(p)
                set_paragraph_style(p, target)
                report["style_changes"].append({"paragraph": i, "text": full_text(p), "before": old, "after": target})

        mechanics = sum(normalize_mechanics(p) for p in top)
        report["formatting"]["mechanical_text_nodes_changed"] = mechanics
        apply_porter_table_corrections(root, report)
        apply_body_formatting(root, top, report)
        # Fuente e idioma se normalizan en todo el documento, incluidas tablas y
        # encabezados semánticos; el tamaño solo se impone al cuerpo.
        normalize_document_font_language(root, report)

        modified = dict(original)
        modified["word/document.xml"] = serialize(root)

        # Estilos generales, idioma y tamaño de cuerpo.
        styles = parse(original["word/styles.xml"])
        normalize_styles(styles, report)
        modified["word/styles.xml"] = serialize(styles)

        # Fuente del tema principal y de los 16 temas de gráficos. Las
        # referencias +mn-lt de los gráficos resolverán así a Calibri.
        theme_changed: dict[str, int] = {}
        theme_parts = [n for n in names if re.fullmatch(r"word/theme/theme(?:1|Override\d+)\.xml", n)]
        for name in theme_parts:
            theme = parse(original[name])
            changed = normalize_theme(theme)
            modified[name] = serialize(theme)
            theme_changed[name] = changed
        report["formatting"]["theme_latin_fonts_to_calibri"] = theme_changed

        # Normaliza referencias explícitas de fuente/idioma en partes textuales,
        # sin alterar tamaños de encabezados, pies, notas ni contenido gráfico.
        textual_parts = [n for n in names if re.fullmatch(r"word/(?:header\d+|footer\d+|footnotes|endnotes)\.xml", n)]
        normalized_parts = []
        for name in textual_parts:
            part = parse(original[name])
            touched = False
            for rpr in part.iter(W + "rPr"):
                normalize_rpr(rpr, set_size=False)
                touched = True
            for lang in part.iter(W + "lang"):
                for attr in ("val", "eastAsia", "bidi"):
                    lang.set(W + attr, "es-CO")
                touched = True
            if touched:
                modified[name] = serialize(part)
                normalized_parts.append(name)
        report["formatting"]["other_text_parts_font_language_normalized"] = normalized_parts

        # Escritura entrada por entrada preservando ZipInfo, orden y comentario.
        tmp = DST.with_suffix(DST.suffix + ".tmp")
        if tmp.exists():
            tmp.unlink()
        with zipfile.ZipFile(tmp, "w") as zout:
            zout.comment = zin.comment
            for info in infos:
                zout.writestr(copy.copy(info), modified[info.filename])
        tmp.replace(DST)

    # Validación desde el archivo ya serializado.
    with zipfile.ZipFile(SRC, "r") as za, zipfile.ZipFile(DST, "r") as zb:
        names_a, names_b = za.namelist(), zb.namelist()
        out_root = parse(zb.read("word/document.xml"))
        after = inventory(out_root, set(names_b))
        report["inventory_after"] = {k: v for k, v in after.items() if k not in {"placeholder_tags", "citavi_instrtext_hashes"}}
        report["validations"].extend(validate_expected(after, "after"))
        report["validations"].extend([
            {"name": "package_part_names_and_order_preserved", "ok": names_a == names_b, "expected": names_a, "actual": names_b},
            {"name": "citavi_placeholder_tag_sequence_preserved", "ok": before["placeholder_tags"] == after["placeholder_tags"], "expected_count": len(before["placeholder_tags"]), "actual_count": len(after["placeholder_tags"])},
            {"name": "all_citavi_instrText_preserved", "ok": before["citavi_instrtext_hashes"] == after["citavi_instrtext_hashes"], "expected_count": len(before["citavi_instrtext_hashes"]), "actual_count": len(after["citavi_instrtext_hashes"])},
        ])
        protected = [n for n in names_a if n.startswith("word/media/") or n.endswith(".rels")]
        protected_diff = [n for n in protected if sha256(za.read(n)) != sha256(zb.read(n))]
        report["validations"].append({"name": "relationships_and_media_byte_preserved", "ok": not protected_diff, "checked_parts": len(protected), "different_parts": protected_diff})

        # Validaciones textuales específicas.
        out_body = out_root.find(".//" + W + "body")
        out_top = out_body.findall("./" + W + "p") if out_body is not None else []
        all_text = "\n".join(full_text(p) for p in out_top)
        all_document_text = full_text(out_root)
        out_styles = parse(zb.read("word/styles.xml"))
        out_sdt_by_tag = {sdt_tag(s): s for s in out_root.iter(W + "sdt") if sdt_tag(s)}
        porter_tables = [tbl for tbl in out_root.iter(W + "tbl") if "PROVEEDORES" in full_text(tbl) or "PODER CON" in full_text(tbl)]
        porter_text = "\n".join(full_text(tbl) for tbl in porter_tables)
        original_root = parse(za.read("word/document.xml"))
        original_body = original_root.find(".//" + W + "body")
        original_top = original_body.findall("./" + W + "p") if original_body is not None else []
        index_positions = [i for i, p in enumerate(original_top) if re.match(r"^(ndicemanual|TDC|Tabladecontenido|Index|ndice)", paragraph_style(p), re.I) or re.search(r"\.{4,}", full_text(p))]
        specific = {
            "source_is_original_not_revisada": "REVISADA" not in SRC.name.upper(),
            "corrected_title_present": "leche pasteurizada para un mercado de nicho en Honduras" in full_text(out_top[2]),
            "empty_sierra_restored": "(Sierra et al 2023)" in full_text(out_top[315]),
            "sierra_result_inside_field": "".join(t.text or "" for t in citation_result_texts(out_sdt_by_tag["CitaviPlaceholder#be2ccc5c-f34f-472b-91d4-0374ded4c98f"])[0]) == "(Sierra et al 2023)",
            "sample_63_present": "n=63" in full_text(out_top[394]) and "Respondieron 63 establecimientos" in full_text(out_top[399]) and "63 establecimientos" in full_text(out_top[1641]) and "63 establecimientos comerciales" in full_text(out_top[1680]),
            "sample_60_claim_removed": "muestra de 60 establecimientos" not in all_text,
            "sample_66_absent": "66 establecimientos" not in all_text,
            "annex_sample_1641_corrected": "aplicado a 63 establecimientos" in full_text(out_top[1641]) and "66 establecimientos" not in full_text(out_top[1641]),
            "annex_sample_1680_corrected": "La muestra final estuvo conformada por 63 establecimientos comerciales" in full_text(out_top[1680]) and "66 establecimientos" not in full_text(out_top[1680]),
            "margin_5_claim_removed_from_method": "margen de error del 5%" not in full_text(out_top[394]),
            "margin_11_55_present_only_as_theoretical": "referencia teórica" in full_text(out_top[394]) and "±11,55 %" in full_text(out_top[394]) and "no se les atribuye un margen de error probabilístico" in full_text(out_top[394]),
            "sampling_results_not_extrapolated": "no se extrapolan estadísticamente" in full_text(out_top[399]),
            "interviews_not_overclaimed": "no se presentan como evidencia confirmada" in full_text(out_top[390]),
            "source_terms_preserved_in_framework": "coherencia, viabilidad y variabilidad" in full_text(out_top[280]),
            "pasteurization_ten_phases_preserve_distribution": "diez fases" in full_text(out_top[383]) and "distribución y despacho" in full_text(out_top[383]),
            "weissbach_visible_reference_preserved": "(Weissbach, 2021)." in full_text(out_top[252]),
            "citation_double_periods_absent": re.search(r"\)\.\s*\.", all_document_text) is None,
            "bibliography_verified_errors_removed": all(old not in all_document_text for old in BIBLIOGRAPHY_REPLACEMENTS),
            "bibliography_verified_corrections_present": all(new in all_document_text for new in BIBLIOGRAPHY_REPLACEMENTS.values()),
            "conclusions_heading_preserved": full_text(out_top[1617]).strip() == "Conclusiones" and paragraph_style(out_top[1617]) == "Ttulo1",
            "recommendations_heading_preserved": full_text(out_top[1627]).strip() == "Recomendaciones" and paragraph_style(out_top[1627]) == "Ttulo1",
            "porter_table_found_once": len(porter_tables) == 1,
            "porter_table_verified_errors_removed": all(old not in porter_text for old in TABLE_PORTER_REPLACEMENTS),
            "porter_table_corrected_terms_present": all(new in porter_text for new in TABLE_PORTER_REPLACEMENTS.values()),
            "document_explicit_rfonts_all_calibri": not non_calibri_rfonts(out_root),
            "styles_explicit_rfonts_all_calibri": not non_calibri_rfonts(out_styles),
            "document_language_all_es_CO": not non_es_co_lang(out_root),
            "styles_language_all_es_CO": not non_es_co_lang(out_styles),
            "indices_text_untouched": all(full_text(out_top[i]) == full_text(original_top[i]) for i in index_positions),
        }
        for name, ok in specific.items():
            report["validations"].append({"name": name, "ok": bool(ok)})

        changed_parts = [n for n in names_a if sha256(za.read(n)) != sha256(zb.read(n))]
        report["changed_package_parts"] = changed_parts

    failures = [v for v in report["validations"] if not v.get("ok")]
    report["validation_summary"] = {"passed": len(report["validations"]) - len(failures), "failed": len(failures), "all_ok": not failures}
    report["destination_sha256"] = sha256(DST.read_bytes())
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    if failures:
        print(json.dumps({"error": "Fallaron validaciones", "failures": failures, "report": str(REPORT)}, ensure_ascii=False, indent=2), file=sys.stderr)
        return 1
    print(json.dumps({"document": str(DST), "report": str(REPORT), "validations": report["validation_summary"], "changed_parts": report["changed_package_parts"]}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
