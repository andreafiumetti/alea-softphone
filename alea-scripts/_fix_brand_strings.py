from pathlib import Path
import re

it = Path("softphone-v2/android/app/src/main/res/values-it/strings.xml")
si = it.read_bytes().decode("cp1252")
si = re.sub(
    r'<string name="welcome_page_1_message">.*?</string>',
    '<string name="welcome_page_1_message">Il softphone per <b>ALEA PBX</b> - chiamate aziendali sicure.</string>',
    si,
    count=1,
    flags=re.DOTALL,
)
it.write_text(si, encoding="utf-8", newline="\n")
print("IT fixed")

for p in Path("softphone-v2/android/app/src/main/res").rglob("strings.xml"):
    b = p.read_bytes()
    if b.startswith(b"\xef\xbb\xbf"):
        b = b[3:]
        p.write_bytes(b)
        print("bom", p)
    try:
        b.decode("utf-8")
    except UnicodeDecodeError as e:
        t = b.decode("cp1252")
        p.write_text(t, encoding="utf-8", newline="\n")
        print("converted", p, e)

for line in it.read_text(encoding="utf-8").splitlines():
    if "welcome_page_1_message" in line or "ENTITY appName" in line:
        print(line)
