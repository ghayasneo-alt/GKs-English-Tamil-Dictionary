#!/usr/bin/env python3
"""
OCR the scanned collocations dictionary and split it into one text block per headword.

Needs: poppler-utils (pdftoppm) and tesseract-ocr.
Usage: python tools/ocr_collocations.py tools/source/Dictionary.pdf tools/out/collocations.jsonl [first_page last_page]
Pages 1-~12 are front matter; the default skips pages before 13.
"""
import json
import os
import re
import subprocess
import sys
import tempfile
from multiprocessing import Pool

HEAD = re.compile(
    r"^([a-z][a-z'\-]*(?: [a-z][a-z'\-]*){0,2})\s+"
    r"(noun|verb|adj|aaj|adv|prep|conj|pron|det|exclamation|number|adjective|adverb|preposition)\b\.?\s*$"
)
POSMAP = {"aaj": "adj", "adjective": "adj", "adverb": "adv", "preposition": "prep"}
BULLET = re.compile(r"^[@®©«•*eo]{1,3}\s*(?=[A-Z+~])")


def ocr_page(args):
    pdf, page = args
    with tempfile.TemporaryDirectory() as tmp:
        base = os.path.join(tmp, "p")
        subprocess.run(
            ["pdftoppm", "-r", "200", "-f", str(page), "-l", str(page), "-gray", "-png", pdf, base],
            check=True, stderr=subprocess.DEVNULL,
        )
        img = next(os.path.join(tmp, f) for f in os.listdir(tmp) if f.endswith(".png"))
        out = subprocess.run(["tesseract", img, "-"], capture_output=True, text=True)
        return page, out.stdout


def main():
    pdf, out_path = sys.argv[1], sys.argv[2]
    info = subprocess.run(["pdfinfo", pdf], capture_output=True, text=True).stdout
    total = int(re.search(r"Pages:\s+(\d+)", info).group(1))
    first = int(sys.argv[3]) if len(sys.argv) > 3 else 13
    last = int(sys.argv[4]) if len(sys.argv) > 4 else total
    with Pool(os.cpu_count() or 2) as pool:
        pages = sorted(pool.imap_unordered(ocr_page, [(pdf, p) for p in range(first, last + 1)], chunksize=4))

    entries, cur = [], None
    for _, text in pages:
        for raw in text.splitlines():
            line = raw.strip()
            if not line:
                continue
            m = HEAD.match(line)
            if m:
                pos = m.group(2)
                cur = {"word": m.group(1), "pos": POSMAP.get(pos, pos), "lines": []}
                entries.append(cur)
            elif cur is not None:
                line = BULLET.sub("• ", line)
                cur["lines"].append(line)

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        for e in entries:
            body = "\n".join(e["lines"]).strip()
            if body:
                f.write(json.dumps({"word": e["word"], "pos": e["pos"], "text": body}, ensure_ascii=False) + "\n")
    print(f"wrote {len(entries)} headwords from pages {first}-{last}")


if __name__ == "__main__":
    main()
