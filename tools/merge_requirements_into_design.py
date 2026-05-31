from __future__ import annotations

import copy
import shutil
import sys
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET


W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
W = f"{{{W_NS}}}"
NS = {"w": W_NS}
ET.register_namespace("w", W_NS)


STYLE_MAP = {
    "Normal": "a",
    "Heading1": "1",
    "Heading2": "2",
    "Heading3": "3",
    "ListParagraph": "a9",
}


def text_of(elem: ET.Element) -> str:
    return "".join(t.text or "" for t in elem.findall(".//w:t", NS)).strip()


def body_children(doc_root: ET.Element) -> list[ET.Element]:
    body = doc_root.find("w:body", NS)
    if body is None:
        raise RuntimeError("document.xml does not contain w:body")
    return list(body)


def find_para_child_index(children: list[ET.Element], needle: str) -> int:
    for i, child in enumerate(children):
        if child.tag == W + "p" and text_of(child) == needle:
            return i
    raise RuntimeError(f"Could not find paragraph: {needle}")


def extract_between(children: list[ET.Element], start_text: str, end_text: str) -> list[ET.Element]:
    start = find_para_child_index(children, start_text)
    end = find_para_child_index(children, end_text)
    return [copy.deepcopy(e) for e in children[start:end]]


def remap_styles(elem: ET.Element) -> None:
    for p_style in elem.findall(".//w:pStyle", NS):
        old = p_style.attrib.get(W + "val")
        if old in STYLE_MAP:
            p_style.set(W + "val", STYLE_MAP[old])


def remap_num_ids(elem: ET.Element, num_map: dict[str, str]) -> None:
    for num_id in elem.findall(".//w:numId", NS):
        old = num_id.attrib.get(W + "val")
        if old in num_map:
            num_id.set(W + "val", num_map[old])


def copy_numbering(target_zip: zipfile.ZipFile, source_zip: zipfile.ZipFile, out_zip: zipfile.ZipFile, inserted: list[ET.Element]) -> None:
    try:
        target_root = ET.fromstring(target_zip.read("word/numbering.xml"))
    except KeyError:
        target_root = ET.Element(W + "numbering")

    source_root = ET.fromstring(source_zip.read("word/numbering.xml"))
    used_abs = [
        int(e.attrib.get(W + "abstractNumId", "0"))
        for e in target_root.findall("w:abstractNum", NS)
        if e.attrib.get(W + "abstractNumId", "0").isdigit()
    ]
    used_num = [
        int(e.attrib.get(W + "numId", "0"))
        for e in target_root.findall("w:num", NS)
        if e.attrib.get(W + "numId", "0").isdigit()
    ]
    next_abs = max(used_abs, default=0) + 100
    next_num = max(used_num, default=0) + 100

    abs_map: dict[str, str] = {}
    num_map: dict[str, str] = {}

    for abs_num in source_root.findall("w:abstractNum", NS):
        old_abs = abs_num.attrib.get(W + "abstractNumId")
        if old_abs is None:
            continue
        new_abs = str(next_abs)
        next_abs += 1
        abs_map[old_abs] = new_abs
        new_abs_elem = copy.deepcopy(abs_num)
        new_abs_elem.set(W + "abstractNumId", new_abs)
        target_root.append(new_abs_elem)

    for num in source_root.findall("w:num", NS):
        old_num = num.attrib.get(W + "numId")
        abs_ref = num.find("w:abstractNumId", NS)
        old_abs = abs_ref.attrib.get(W + "val") if abs_ref is not None else None
        if old_num is None or old_abs not in abs_map:
            continue
        new_num = str(next_num)
        next_num += 1
        num_map[old_num] = new_num
        new_num_elem = copy.deepcopy(num)
        new_num_elem.set(W + "numId", new_num)
        new_num_elem.find("w:abstractNumId", NS).set(W + "val", abs_map[old_abs])
        target_root.append(new_num_elem)

    for elem in inserted:
        remap_num_ids(elem, num_map)

    out_zip.writestr("word/numbering.xml", ET.tostring(target_root, encoding="utf-8", xml_declaration=True))


def merge(source_docx: Path, target_docx: Path, output_docx: Path) -> None:
    work_docx = output_docx.with_suffix(".work.docx")
    shutil.copyfile(target_docx, work_docx)

    with zipfile.ZipFile(source_docx) as source_zip, zipfile.ZipFile(target_docx) as target_zip:
        source_root = ET.fromstring(source_zip.read("word/document.xml"))
        target_root = ET.fromstring(target_zip.read("word/document.xml"))

        source_children = body_children(source_root)
        target_body = target_root.find("w:body", NS)
        if target_body is None:
            raise RuntimeError("target document.xml does not contain w:body")
        target_children = list(target_body)

        inserted = []
        inserted.extend(extract_between(source_children, "3. 功能性需求", "6. 业务规则"))
        inserted.extend(extract_between(source_children, "7. 页面与交互需求", "8. 开发与运行环境"))

        for elem in inserted:
            remap_styles(elem)

        insert_after = find_para_child_index(target_children, "需求分析")
        insert_at = insert_after + 1
        for offset, elem in enumerate(inserted):
            target_body.insert(insert_at + offset, elem)

        document_xml = ET.tostring(target_root, encoding="utf-8", xml_declaration=True)

        with zipfile.ZipFile(work_docx, "r") as zin, zipfile.ZipFile(output_docx, "w", zipfile.ZIP_DEFLATED) as zout:
            handled = {"word/document.xml", "word/numbering.xml"}
            for item in zin.infolist():
                if item.filename in handled:
                    continue
                zout.writestr(item, zin.read(item.filename))
            zout.writestr("word/document.xml", document_xml)
            copy_numbering(target_zip, source_zip, zout, inserted)

    work_docx.unlink(missing_ok=True)


if __name__ == "__main__":
    merge(Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]))
    print(Path(sys.argv[3]).resolve())
