#!/usr/bin/env python3
"""Gera 24 XMLs de animacao: 4 tipos x 3 velocidades x 2 direcoes"""
import os

ANIM_DIR = os.path.join(os.path.dirname(__file__), "..", "res", "anim")

TRANSITIONS = {
    "sphere": {
        "enter": [
            ("rotate", 0, 360, 50, 50),
            ("alpha", 0.0, 1.0),
        ],
        "exit": [
            ("rotate", 0, -360, 50, 50),
            ("alpha", 1.0, 0.0),
        ],
    },
    "mesh": {
        "enter": [
            ("scale", 0.3, 1.0, 0.3, 1.0, 50, 50),
            ("alpha", 0.0, 1.0),
        ],
        "exit": [
            ("scale", 1.0, 0.3, 1.0, 0.3, 50, 50),
            ("alpha", 1.0, 0.0),
        ],
    },
    "cube": {
        "enter": [
            ("translateX", 100, 0),
            ("alpha", 0.0, 1.0),
        ],
        "exit": [
            ("translateX", 0, -100),
            ("alpha", 1.0, 0.0),
        ],
    },
    "spin": {
        "enter": [
            ("rotate", 0, 720, 50, 50),
            ("translateY", 100, 0),
            ("alpha", 0.0, 1.0),
        ],
        "exit": [
            ("rotate", 0, -720, 50, 50),
            ("translateY", 0, -100),
            ("alpha", 1.0, 0.0),
        ],
    },
}

SPEEDS = {"slow": 800, "medium": 400, "fast": 200}

ENTER_INTERP = "@android:anim/decelerate_interpolator"
EXIT_INTERP = "@android:anim/accelerate_interpolator"


def make_anim(anim_type, dur, interp):
    if anim_type == "alpha":
        return None  # handled specially
    return None  # handled below


def build_animations(parts, dur, interp):
    lines = []
    for part in parts:
        kind = part[0]
        if kind == "rotate":
            _, fr, to, px, py = part
            lines.append(
                f'    <rotate\n'
                f'        android:fromDegrees="{fr}"\n'
                f'        android:toDegrees="{to}"\n'
                f'        android:pivotX="{px}%"\n'
                f'        android:pivotY="{py}%"\n'
                f'        android:duration="{dur}"\n'
                f'        android:interpolator="{interp}" />'
            )
        elif kind == "alpha":
            _, fr, to = part
            lines.append(
                f'    <alpha\n'
                f'        android:fromAlpha="{fr}"\n'
                f'        android:toAlpha="{to}"\n'
                f'        android:duration="{dur}"\n'
                f'        android:interpolator="{interp}" />'
            )
        elif kind == "scale":
            _, fxS, txS, fyS, tyS, px, py = part
            lines.append(
                f'    <scale\n'
                f'        android:fromXScale="{fxS}"\n'
                f'        android:toXScale="{txS}"\n'
                f'        android:fromYScale="{fyS}"\n'
                f'        android:toYScale="{tyS}"\n'
                f'        android:pivotX="{px}%"\n'
                f'        android:pivotY="{py}%"\n'
                f'        android:duration="{dur}"\n'
                f'        android:interpolator="{interp}" />'
            )
        elif kind == "translateX":
            _, fr, to = part
            lines.append(
                f'    <translate\n'
                f'        android:fromXDelta="{fr}%"\n'
                f'        android:toXDelta="{to}%"\n'
                f'        android:duration="{dur}"\n'
                f'        android:interpolator="{interp}" />'
            )
        elif kind == "translateY":
            _, fr, to = part
            lines.append(
                f'    <translate\n'
                f'        android:fromYDelta="{fr}%"\n'
                f'        android:toYDelta="{to}%"\n'
                f'        android:duration="{dur}"\n'
                f'        android:interpolator="{interp}" />'
            )
    return "\n".join(lines)


TEMPLATE = '<?xml version="1.0" encoding="utf-8"?>\n<set xmlns:android="http://schemas.android.com/apk/res/android">\n{body}\n</set>\n'

count = 0
for tname, directions in TRANSITIONS.items():
    for speed_name, speed_ms in SPEEDS.items():
        for direction in ["enter", "exit"]:
            fname = f"transition_{tname}_{speed_name}_{direction}.xml"
            interp = ENTER_INTERP if direction == "enter" else EXIT_INTERP
            body = build_animations(directions[direction], speed_ms, interp)
            content = TEMPLATE.format(body=body)
            fpath = os.path.join(ANIM_DIR, fname)
            with open(fpath, "w", encoding="utf-8") as f:
                f.write(content)
            count += 1
            print(f"  {fname}")

print(f"\nGerados {count} arquivos de animacao")
