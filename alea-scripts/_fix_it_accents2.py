#!/usr/bin/env python3
"""Fix remaining non-UTF8 / mojibake in Italian strings.xml"""
from pathlib import Path
import re

p = Path(__file__).resolve().parents[1] / "android/app/src/main/res/values-it/strings.xml"
raw = p.read_bytes()
# Replace lone Latin-1 accented bytes that aren't valid UTF-8 sequences
# Valid UTF-8 multibyte starts with C2-F4; lone E0-F4 as single bytes are illegal
# Find invalid UTF-8 and try latin-1 reinterpretation of whole file if needed

try:
    text = raw.decode("utf-8")
except UnicodeDecodeError as e:
    print("not valid utf-8", e)
    text = raw.decode("utf-8", errors="replace")

# Explicit fix for the auth dialog message
old = text
replacements = {
    "Ã¨": "è",
    "Ã©": "é",
    "Ã ": "à",
    "Ã¹": "ù",
    "Ã²": "ò",
    "Ã¬": "ì",
    "Ã€": "À",
    "Ãˆ": "È",
    "â€™": "'",
    "â€œ": '"',
    "â€": '"',
}
for a, b in replacements.items():
    if a in text:
        n = text.count(a)
        text = text.replace(a, b)
        print(f"replaced {a!r} -> {b!r} x{n}")

# Ensure the specific auth string is correct
pat = r'(<string name="account_settings_dialog_invalid_password_message">)(.*?)(</string>)'
def fix_auth(m):
    body = (
        "Connessione non riuscita: l\\'autenticazione dell\\'account \\n%s "
        "è assente o non valida.\\n\\n"
        "Puoi reinserire la password oppure controllare la configurazione "
        "dell\\'account nelle impostazioni."
    )
    return m.group(1) + body + m.group(3)

text2, n = re.subn(pat, fix_auth, text, count=1, flags=re.S)
print("auth message rewritten", n)

if "\r\n" in old:
    text2 = text2.replace("\r\n", "\n").replace("\n", "\r\n")

p.write_bytes(text2.encode("utf-8"))
# verify
raw2 = p.read_bytes()
assert b"\xc3\xa8 assente" in raw2
assert b"\xc3\x83\xc2\xa8" not in raw2
print("OK", p)
