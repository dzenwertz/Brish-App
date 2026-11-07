#!/usr/bin/env python3
"""Logo de Brish (2.4): la misma «b» con la chispa de captura, ahora en blanco sobre azul.

Genera:
  design/logo/brish-logo.svg, brish-logo-512.png       (web / tienda)
  res/drawable/ic_brish_logo.xml                        (logo dentro de la app)
  res/drawable/ic_brish_icon_bg.xml / ic_brish_icon_fg.xml (icono adaptativo, Android 8+)
  res/mipmap-*/ic_brish.png, ic_brish_round.png         (Android 7, vía rsvg-convert)
Uso: python3 design/logo/generar_logo.py
"""
import subprocess, tempfile
from pathlib import Path

TOP, BOTTOM = "#3395FF", "#0063D9"     # degradado vertical sutil del azul del sistema
GLYPH, SPARK = "#FFFFFF", "#CFE4FF"
STEM = "M35,34.5a5.5,5.5 0,0 1,11 0v37a5.5,5.5 0,0 1,-11 0z"
BOWL = "M42,61a15,15 0,1 0,30 0a15,15 0,1 0,-30 0"
SPARK_PATH = "M70,26C71.2,31.6 72.4,32.8 78,34C72.4,35.2 71.2,36.4 70,42C68.8,36.4 67.6,35.2 62,34C67.6,32.8 68.8,31.6 70,26Z"
SQUIRCLE = "M30,0h48a30,30 0,0 1,30 30v48a30,30 0,0 1,-30 30h-48a30,30 0,0 1,-30 -30v-48a30,30 0,0 1,30 -30z"

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "Android/app/src/main/res"
OUT = ROOT / "design/logo"

def a(c):  # #RRGGBB -> #FFRRGGBB
    return "#FF" + c[1:]

GRADIENT_XML = f'''        <aapt:attr name="android:fillColor">
            <gradient android:type="linear" android:startX="0" android:startY="0" android:endX="0" android:endY="108">
                <item android:offset="0" android:color="{a(TOP)}" />
                <item android:offset="1" android:color="{a(BOTTOM)}" />
            </gradient>
        </aapt:attr>'''

GLYPH_XML = f'''    <path android:fillColor="{a(GLYPH)}" android:pathData="{STEM}" />
    <path android:fillColor="#00000000" android:strokeColor="{a(GLYPH)}" android:strokeWidth="11" android:pathData="{BOWL}" />
    <path android:fillColor="{a(SPARK)}" android:pathData="{SPARK_PATH}" />'''

def vector(body, comment):
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- {comment} Generado por design/logo/generar_logo.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
{body}
</vector>
'''

def svg(shape):
    clip = (f'<path d="{SQUIRCLE}"/>' if shape == "square" else '<circle cx="54" cy="54" r="54"/>')
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">
  <defs><linearGradient id="g" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{TOP}"/><stop offset="1" stop-color="{BOTTOM}"/></linearGradient>
  <clipPath id="c">{clip}</clipPath></defs>
  <g clip-path="url(#c)"><rect width="108" height="108" fill="url(#g)"/></g>
  <path d="{STEM}" fill="{GLYPH}"/>
  <path d="{BOWL}" fill="none" stroke="{GLYPH}" stroke-width="11"/>
  <path d="{SPARK_PATH}" fill="{SPARK}"/>
</svg>
'''

if __name__ == "__main__":
    (RES / "drawable/ic_brish_logo.xml").write_text(vector(
        f'    <path android:pathData="{SQUIRCLE}">\n{GRADIENT_XML}\n    </path>\n' + GLYPH_XML,
        "Logo completo para usar dentro de la app (splash y cabeceras)."), encoding="utf-8")
    (RES / "drawable/ic_brish_icon_bg.xml").write_text(vector(
        f'    <path android:pathData="M0,0h108v108h-108z">\n{GRADIENT_XML}\n    </path>',
        "Fondo del icono adaptativo: azul del sistema con degradado sutil."), encoding="utf-8")
    (RES / "drawable/ic_brish_icon_fg.xml").write_text(vector(
        GLYPH_XML, "La «b» de Brish en blanco y la chispa de la captura, dentro de la zona segura de 66 dp."), encoding="utf-8")
    OUT.joinpath("brish-logo.svg").write_text(svg("square"), encoding="utf-8")
    with tempfile.TemporaryDirectory() as tmp:
        sq, rd = Path(tmp, "sq.svg"), Path(tmp, "rd.svg")
        sq.write_text(svg("square")); rd.write_text(svg("round"))
        subprocess.run(["rsvg-convert", "-w", "512", "-h", "512", str(sq), "-o", str(OUT / "brish-logo-512.png")], check=True)
        for density, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
            d = RES / f"mipmap-{density}"
            subprocess.run(["rsvg-convert", "-w", str(px), "-h", str(px), str(sq), "-o", str(d / "ic_brish.png")], check=True)
            subprocess.run(["rsvg-convert", "-w", str(px), "-h", str(px), str(rd), "-o", str(d / "ic_brish_round.png")], check=True)
    print("logo generado")
