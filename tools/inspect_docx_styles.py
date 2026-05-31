from __future__ import annotations

import sys
import zipfile
from xml.etree import ElementTree as ET


NS = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"


with zipfile.ZipFile(sys.argv[1]) as zf:
    root = ET.fromstring(zf.read("word/styles.xml"))

for style in root.findall("w:style", NS):
    sid = style.attrib.get(W + "styleId", "")
    typ = style.attrib.get(W + "type", "")
    name = style.find("w:name", NS)
    val = name.attrib.get(W + "val", "") if name is not None else ""
    if typ == "paragraph":
        print(f"{sid}\t{val}")
