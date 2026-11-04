#!/usr/bin/env python3
"""Instantiate the official OFL Manrope variable font into explicit Android weights.

Run in CI after downloading and verifying the upstream font file. Generated fonts
are build artifacts, not committed to the repository.
"""
from pathlib import Path
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

source = Path(sys.argv[1])
output = source.parent
for name, weight in (
    ("regular", 400), ("medium", 500), ("semibold", 600),
    ("bold", 700), ("extrabold", 800),
):
    font = TTFont(source)
    instantiateVariableFont(font, {"wght": weight}, inplace=True)
    target = output / ("manrope_" + name + ".ttf")
    font.save(target)
    font.close()
    assert target.stat().st_size > 20000, f"Invalid font: {target}"
    print(f"Generated {target.name} at weight {weight}")
source.unlink()
