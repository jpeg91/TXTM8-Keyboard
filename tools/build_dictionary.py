#!/usr/bin/env python3
"""
Build the T9 dictionary asset from a word-frequency list.

Input : lines of "word count" (e.g. FrequencyWords' en_50k.txt)
Output: lines of "word score", most frequent first, ready for T9Engine.load()

    score = max(1, round(100 * ln(count)))

The log scale keeps scores in a small range (~1..1700) so that a user's own
picks (T9Engine.userWeight) can meaningfully re-rank words.

Usage:
    python3 tools/build_dictionary.py dictionaries/raw/en_50k.txt \
        -o dictionaries/en.txt --max-words 40000 --allow path/to/valid_words.txt

--allow is optional but recommended: a plain list of valid words (one per line,
e.g. a spell-checker word list). Subtitle corpora contain names, typos and
tokenizer junk; this keeps only real words. Known contractions are always kept.
"""
import argparse
import math
import re
import sys
import unicodedata
from collections import defaultdict

SCALE = 100.0
WORD_RE = re.compile(r"^[a-z]+(?:'[a-z]+)*$")
KEEP_SINGLE = {"a", "i"}

# Fragments left behind when a tokenizer splits "don't" -> "don" + "t", etc.
FRAGMENTS = {
    "ll", "ve", "re", "nt", "isn", "didn", "doesn", "wasn", "couldn",
    "wouldn", "shouldn", "aren", "hasn", "haven", "hadn", "weren", "ain",
    "mustn", "needn",
}

# Apostrophe words are only kept if listed here (possessives are dropped).
CONTRACTIONS = {
    "i'm", "i'll", "i've", "i'd", "you're", "you'll", "you've", "you'd",
    "he's", "he'll", "he'd", "she's", "she'll", "she'd", "it's", "it'll",
    "we're", "we'll", "we've", "we'd", "they're", "they'll", "they've",
    "they'd", "that's", "that'll", "there's", "what's", "who's", "let's",
    "here's", "where's", "how's", "don't", "doesn't", "didn't", "can't",
    "couldn't", "won't", "wouldn't", "shouldn't", "isn't", "aren't",
    "wasn't", "weren't", "hasn't", "haven't", "hadn't", "ain't", "mustn't",
    "ma'am", "o'clock",
}


def normalize(token: str) -> str:
    return unicodedata.normalize("NFC", token).lower().replace("\u2019", "'")


def strip_accents(s: str) -> str:
    d = unicodedata.normalize("NFD", s)
    return "".join(c for c in d if unicodedata.category(c) != "Mn")


def read_counts(path):
    counts = defaultdict(int)
    with open(path, encoding="utf-8") as f:
        for line in f:
            parts = line.split()
            if len(parts) != 2 or not parts[1].isdigit():
                continue
            counts[normalize(parts[0])] += int(parts[1])
    return counts


def read_allow(path):
    allow = set()
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            w = line.strip()
            if w:
                allow.add(normalize(w))
                allow.add(strip_accents(normalize(w)))
    return allow


def keep(word, allow):
    base = strip_accents(word)
    if not WORD_RE.match(base):
        return False
    if len(base) == 1 and base not in KEEP_SINGLE:
        return False
    if base in FRAGMENTS:
        return False
    if "'" in base:
        return base in CONTRACTIONS
    if allow is not None:
        return word in allow or base in allow
    return True


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("input")
    ap.add_argument("-o", "--output", required=True)
    ap.add_argument("--max-words", type=int, default=50000)
    ap.add_argument("--allow", help="file of valid words, one per line")
    ap.add_argument(
        "--note",
        default="Derived from hermitdave/FrequencyWords (OpenSubtitles), CC BY-SA 4.0. See dictionaries/ATTRIBUTION.md",
    )
    args = ap.parse_args()

    counts = read_counts(args.input)
    allow = read_allow(args.allow) if args.allow else None

    kept = [(w, c) for w, c in counts.items() if keep(w, allow)]
    kept.sort(key=lambda wc: (-wc[1], wc[0]))
    kept = kept[: args.max_words]

    with open(args.output, "w", encoding="utf-8", newline="\n") as out:
        out.write('# T9 dictionary: "word score" per line, score = round(100 * ln(count))\n')
        out.write(f"# {args.note}\n")
        for w, c in kept:
            out.write(f"{w} {max(1, round(SCALE * math.log(c)))}\n")

    print(f"read {len(counts)} words, kept {len(kept)} -> {args.output}", file=sys.stderr)


if __name__ == "__main__":
    main()
