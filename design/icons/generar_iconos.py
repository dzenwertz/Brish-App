#!/usr/bin/env python3
"""Set de iconos de Brish (línea fina, estilo símbolos de sistema).

Una sola definición genera:
  - design/icons/<nombre>.svg            (web, stroke="currentColor")
  - Android/app/src/main/res/drawable/ic_b_<nombre>.xml  (VectorDrawable, se tiñe con el tema)

Cuadrícula 24×24, trazo 1.8, extremos y uniones redondeados, sin relleno.
Uso:  python3 design/icons/generar_iconos.py
"""
from pathlib import Path

STROKE = 1.8

def circle(cx, cy, r):
    return f"M{cx - r} {cy}a{r} {r} 0 1 0 {2 * r} 0a{r} {r} 0 1 0 {-2 * r} 0"

def rrect(x, y, w, h, r):
    return (f"M{x + r} {y}h{w - 2 * r}a{r} {r} 0 0 1 {r} {r}v{h - 2 * r}a{r} {r} 0 0 1 {-r} {r}"
            f"h{-(w - 2 * r)}a{r} {r} 0 0 1 {-r} {-r}v{-(h - 2 * r)}a{r} {r} 0 0 1 {r} {-r}z")

def dot(x, y):  # punto: segmento mínimo con extremo redondo y trazo más grueso
    return ("dot", f"M{x} {y}h0.01")

ICONS = {
    # Pestañas y navegación
    "sun": [circle(12, 12, 4), "M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"],
    "calendar": [rrect(3.5, 5, 17, 15.5, 3), "M3.5 9.8h17M8 3v4M16 3v4", dot(8, 14), dot(12, 14), dot(16, 14)],
    "note": ["M6.5 3.5h7.2l4.3 4.3v11.2a1.5 1.5 0 0 1-1.5 1.5h-10a1.5 1.5 0 0 1-1.5-1.5V5a1.5 1.5 0 0 1 1.5-1.5z", "M13.5 3.5v4.5h4.5M8.5 12.5h7M8.5 16h5"],
    "habits": ["M5.5 20v-5.5M12 20V9M18.5 20V4.5"],
    "plus": ["M12 5v14M5 12h14"],
    # Áreas de vida
    "work": [rrect(5, 5, 14, 10, 1.6), "M3 18.5h18"],
    "home": ["M4 11.2 12 4.5l8 6.7", "M6 9.6V19a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V9.6", "M10 20v-5h4v5"],
    "health": ["M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7a4.3 4.3 0 0 1 7.5 2.8C19.5 15.4 12 20 12 20z"],
    "person": [circle(12, 8, 3.5), "M5 20a7 7 0 0 1 14 0"],
    "event": [rrect(3.5, 5, 17, 15.5, 3), "M3.5 9.8h17M8 3v4M16 3v4", "M9 15l2 2 4-4"],
    # Hábitos y objetos
    "water": ["M12 3.5s6 6.4 6 10.8a6 6 0 0 1-12 0C6 9.9 12 3.5 12 3.5z"],
    "dumbbell": ["M6.5 7.5v9M17.5 7.5v9M3.5 10v4M20.5 10v4M6.5 12h11"],
    "moon": ["M19.5 14.2A7.8 7.8 0 1 1 9.8 4.5a6.2 6.2 0 0 0 9.7 9.7z"],
    "book": ["M12 6.5c-1.8-1.5-4.5-2-7.5-2v14c3 0 5.7.5 7.5 2 1.8-1.5 4.5-2 7.5-2v-14c-3 0-5.7.5-7.5 2z", "M12 6.5v14"],
    "food": ["M7.5 3v6.5a2 2 0 0 0 4 0V3M9.5 3v18", "M17 21V3c-2 1-3.2 3.5-3.2 7v4H17"],
    "idea": ["M9.5 18h5M10.5 21h3", "M12 3a6 6 0 0 0-3.6 10.8c.7.5 1.1 1.2 1.1 2V16h5v-.2c0-.8.4-1.5 1.1-2A6 6 0 0 0 12 3z"],
    "lock": [rrect(5, 10.5, 14, 10, 2.5), "M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"],
    "list": ["M9.5 7h10M9.5 12h10M9.5 17h10", dot(5, 7), dot(5, 12), dot(5, 17)],
    "bolt": ["M13.2 3 5.5 13.2h6L10.8 21l7.7-10.2h-6z"],
    "cloud": ["M7 18.5a4.5 4.5 0 0 1-.6-9 6 6 0 0 1 11.6 1.6 3.7 3.7 0 0 1-.5 7.4z", "M9.5 14l2 2 3.5-3.5"],
    "leaf": ["M5 19c0-8.3 5.2-13.5 14-14 0 9-5.2 14-13.5 14z", "M5 19l7.5-7.5"],
    "check": [circle(12, 12, 8.5), "M8.4 12.3l2.4 2.4 4.8-4.9"],
    "focus": [circle(12, 13, 7.5), "M12 13V9.5M9.5 2.5h5M18.5 5.5l1.5-1.5"],
}

ROOT = Path(__file__).resolve().parents[2]
SVG_DIR = ROOT / "design" / "icons"
XML_DIR = ROOT / "Android" / "app" / "src" / "main" / "res" / "drawable"

def parts(spec):
    for p in spec:
        yield (p, 2.6) if isinstance(p, tuple) and p[0] == "dot" else (p, STROKE)

def write_svg(name, spec):
    paths = "\n".join(
        f'  <path d="{d[1] if isinstance(d, tuple) else d}" stroke-width="{w}"/>' for d, w in parts(spec)
    )
    SVG_DIR.joinpath(f"{name}.svg").write_text(
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" '
        'stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">\n' + paths + "\n</svg>\n",
        encoding="utf-8")

def write_xml(name, spec):
    paths = "\n".join(
        f'    <path android:pathData="{d[1] if isinstance(d, tuple) else d}" android:strokeColor="#FF000000" '
        f'android:strokeWidth="{w}" android:strokeLineCap="round" android:strokeLineJoin="round" '
        f'android:fillColor="#00000000"/>' for d, w in parts(spec)
    )
    XML_DIR.joinpath(f"ic_b_{name}.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n<!-- Generado por design/icons/generar_iconos.py: no editar a mano. -->\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n'
        + paths + "\n</vector>\n", encoding="utf-8")

if __name__ == "__main__":
    for name, spec in ICONS.items():
        write_svg(name, spec)
        write_xml(name, spec)
    # Hoja de contacto para revisar el set de un vistazo.
    cells = "".join(
        f'<figure><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">'
        + "".join(f'<path d="{d[1] if isinstance(d, tuple) else d}" stroke-width="{w}"/>' for d, w in parts(spec))
        + f"</svg><figcaption>{name}</figcaption></figure>" for name, spec in ICONS.items())
    SVG_DIR.joinpath("muestrario.html").write_text(
        "<!doctype html><meta charset=utf-8><title>Iconos de Brish</title><style>"
        "body{font:14px -apple-system,Inter,sans-serif;background:#F2F2F7;color:#1C1C1E;padding:24px}"
        "main{display:grid;grid-template-columns:repeat(auto-fill,96px);gap:12px}"
        "figure{margin:0;background:#fff;border-radius:14px;padding:14px;text-align:center;color:#007AFF}"
        "svg{width:40px;height:40px}figcaption{color:#6C6C70;font-size:12px;margin-top:6px}"
        "@media(prefers-color-scheme:dark){body{background:#000;color:#F2F2F7}figure{background:#1C1C1E;color:#0A84FF}}"
        f"</style><h1>Iconos de Brish</h1><main>{cells}</main>", encoding="utf-8")
    print(f"{len(ICONS)} iconos generados")
