"""
¿Cuanto del 23 % es ruido de configuracion y cuanto es solapamiento real?

Criterio: una fuente que aporta 15 palabras sueltas es una colocacion del
idioma academico. Una que aporta 150 palabras es solapamiento de verdad.
"""
import json, collections

d = json.load(open("/projects/sandbox/hola/analisis/matches.json"))
recs = [r for r in d["records"] if r["source_no"]]
porf = collections.defaultdict(int)
for r in recs:
    porf[r["source_no"]] += r["words"]

TOT = 52958
FACTOR = 1.19          # mi conteo sobreestima ~19 % frente a Turnitin
def puntos(pal):
    return pal / TOT * 100 / FACTOR

TRAMOS = [("menos de 10 palabras", 0, 10),
          ("10 a 24 palabras", 10, 25),
          ("25 a 49 palabras", 25, 50),
          ("50 a 99 palabras", 50, 100),
          ("100 a 199 palabras", 100, 200),
          ("200 palabras o mas", 200, 10 ** 9)]

print(f"{'APORTE POR FUENTE':<24}{'fuentes':>9}{'palabras':>10}{'puntos':>9}{'naturaleza'}")
print("-" * 78)
acum_ruido = acum_real = 0
for nombre, lo, hi in TRAMOS:
    fs = [f for f, w in porf.items() if lo <= w < hi]
    pal = sum(porf[f] for f in fs)
    p = puntos(pal)
    nat = "colocacion del idioma" if hi <= 25 else ("mixto" if hi <= 100 else "solapamiento real")
    if hi <= 25:
        acum_ruido += p
    elif lo >= 100:
        acum_real += p
    print(f"{nombre:<24}{len(fs):>9}{pal:>10}{p:>9.1f}   {nat}")

print("-" * 78)
print(f"{'TOTAL':<24}{len(porf):>9}{sum(porf.values()):>10}{puntos(sum(porf.values())):>9.1f}")

print(f"\n  Ruido puro (fuentes con <25 palabras):        {acum_ruido:.1f} puntos")
print(f"  Solapamiento real (fuentes con 100+):         {acum_real:.1f} puntos")
print(f"  Zona intermedia (25-99 palabras):             "
      f"{puntos(sum(w for w in porf.values() if 25 <= w < 100)):.1f} puntos")

print("\n" + "=" * 78)
print("LAS 15 FUENTES CON MAS SOLAPAMIENTO REAL")
print("=" * 78)
for f, w in sorted(porf.items(), key=lambda x: -x[1])[:15]:
    print(f"  {w:4d} palabras ({puntos(w):.1f} pts)  #{f}: {d['sources'].get(str(f),'?')[:66]}")
