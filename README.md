# GK's English Tamil Dictionary

Offline English dictionary for Android with Tamil translation of every meaning.

| Part | Source | Offline? |
|---|---|---|
| English meanings (~150k words, ~200k senses, examples) | Princeton WordNet via NLTK (free licence) | Yes, bundled in the APK as SQLite |
| Tamil translation of meanings | Google ML Kit On-Device Translation (free) | Yes, after the ~30 MB Tamil model is downloaded once |
| "Commonly used with" collocations | OCR of your scanned `Dictionary.pdf` | Yes, bundled in the same SQLite file |

## Build it on GitHub (no Android Studio needed)

1. Create a new GitHub repository and upload everything in this folder.
2. (Optional, for collocations) Add your PDF as `tools/source/Dictionary.pdf`.
   It is about 30 MB, which is fine for GitHub (limit 100 MB per file).
3. Go to the **Actions** tab, choose **Build GK's English Tamil Dictionary APK**, then **Run workflow**
   (it also runs on every push to `main`).
4. When it finishes, open the run and download the artifact `GKs-English-Tamil-Dictionary-apk`.
   Unzip it and install the `.apk` on your phone (allow "install unknown apps").

The first run OCRs the 926-page PDF (roughly 30-60 minutes on GitHub's runner). The result is cached,
so later builds are fast. To avoid repeating it, you can commit `tools/out/collocations.jsonl`
after the first run.

## Using the app

- Type a word, tap a suggestion or press search.
- You get the English meaning(s), the Tamil translation of each meaning, and the collocations.
- The first time you open the app, connect to the internet so the Tamil model can download.
  After that it works fully offline. Inflected forms (walked, cats, running) are matched to the base word.

## Project layout

```
.github/workflows/build-apk.yml   CI: OCR -> database -> APK
tools/build_dictionary_db.py      builds app/src/main/assets/dictionary.db from WordNet (+ collocations)
tools/ocr_collocations.py         OCR + headword splitting for the scanned PDF
app/                              Kotlin + Jetpack Compose app
```

## Notes and limits

- WordNet in NLTK is version 3.0 (3.1 is not packaged for NLTK). Swap in Kaikki.org Wiktionary data by
  changing `build_dictionary_db.py` if you want more modern words.
- OCR of the PDF is not perfect (small errors like "Imissed", hyphenated line breaks). It is meant for reference
  hints, not as authoritative text.
- ML Kit's Tamil output is machine translation: good for general meaning, but it can be awkward for
  technical senses. ML Kit does not allow bundling the model inside the APK, so the one-time download is required.
- The build produces a debug APK, which is fine for personal installation. A Play Store release would
  need a signing key.
- Not compiled or run in the environment where this was generated (no internet there), so the first
  GitHub run may need a small fix; the workflow log will show exactly where.
