#!/usr/bin/env python3
"""
Builds app/src/main/assets/dictionary.db

  entries(id, word, word_lc, pos, definition, example)   <- Princeton WordNet (via NLTK, free licence)
  collocations(word_lc, body)                            <- OCR of Dictionary.pdf (optional, tools/out/collocations.jsonl)

Usage:  pip install nltk && python tools/build_dictionary_db.py
"""
import json
import os
import sqlite3
import sys

import nltk

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "dictionary.db")
COLLOC = os.path.join(ROOT, "tools", "out", "collocations.jsonl")

POS = {"n": "noun", "v": "verb", "a": "adjective", "s": "adjective", "r": "adverb"}


def main():
    nltk.download("wordnet", quiet=True)
    from nltk.corpus import wordnet as wn

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    if os.path.exists(OUT):
        os.remove(OUT)
    con = sqlite3.connect(OUT)
    cur = con.cursor()
    cur.executescript(
        """
        PRAGMA journal_mode=OFF;
        CREATE TABLE entries(
            id INTEGER PRIMARY KEY, word TEXT NOT NULL, word_lc TEXT NOT NULL,
            pos TEXT NOT NULL, definition TEXT NOT NULL, example TEXT);
        CREATE TABLE collocations(word_lc TEXT NOT NULL, body TEXT NOT NULL);
        """
    )

    rows = []
    for name in sorted(wn.all_lemma_names()):
        word = name.replace("_", " ")
        for syn in wn.synsets(name):
            ex = syn.examples()[0] if syn.examples() else None
            rows.append((word, word.lower(), POS.get(syn.pos(), syn.pos()), syn.definition(), ex))
    cur.executemany(
        "INSERT INTO entries(word, word_lc, pos, definition, example) VALUES (?,?,?,?,?)", rows
    )
    print(f"entries: {len(rows)}")

    n = 0
    if os.path.exists(COLLOC):
        with open(COLLOC, encoding="utf-8") as f:
            for line in f:
                d = json.loads(line)
                cur.execute(
                    "INSERT INTO collocations(word_lc, body) VALUES (?,?)",
                    (d["word"].lower(), f"[{d['pos']}]\n{d['text']}"),
                )
                n += 1
    else:
        print("note: no collocations file found, skipping collocations", file=sys.stderr)
    print(f"collocation blocks: {n}")

    cur.executescript(
        """
        CREATE INDEX idx_entries_word ON entries(word_lc);
        CREATE INDEX idx_colloc_word ON collocations(word_lc);
        """
    )
    con.commit()
    cur.execute("VACUUM")
    con.close()
    print(f"wrote {OUT} ({os.path.getsize(OUT)/1e6:.1f} MB)")


if __name__ == "__main__":
    main()
