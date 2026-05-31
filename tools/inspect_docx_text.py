from __future__ import annotations

import sys
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET


NS = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}


def para_text(p: ET.Element) -> str:
    return "".join(t.text or "" for t in p.findall(".//w:t", NS))


def style_id(p: ET.Element) -> str:
    node = p.find("./w:pPr/w:pStyle", NS)
    return node.attrib.get(f"{{{NS['w']}}}val", "") if node is not None else ""


def main(path: str) -> None:
    with zipfile.ZipFile(path) as zf:
        root = ET.fromstring(zf.read("word/document.xml"))
    i = 0
    for p in root.findall(".//w:body/w:p", NS):
        text = para_text(p).strip()
        if text:
            print(f"{i:04d}\t{style_id(p)}\t{text}")
        i += 1


if __name__ == "__main__":
    main(sys.argv[1])
