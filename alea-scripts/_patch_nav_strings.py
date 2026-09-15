#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1] / "android" / "app" / "src" / "main" / "res"


def set_string(path: Path, name: str, value: str) -> None:
    text = path.read_text(encoding="utf-8")
    pat = rf'(<string name="{re.escape(name)}">)(.*?)(</string>)'
    new, n = re.subn(pat, rf"\g<1>{value}\g<3>", text, count=1, flags=re.S)
    if n == 0:
        new = text.replace(
            "</resources>",
            f'    <string name="{name}">{value}</string>\n</resources>',
            1,
        )
        n = 1 if new != text else 0
    # keep CRLF if original used it
    if "\r\n" in text:
        new = new.replace("\r\n", "\n").replace("\n", "\r\n")
    path.write_text(new, encoding="utf-8", newline="")
    print(f"{path.name}: {name} -> {value!r} ({'ok' if n else 'FAIL'})")


def main() -> None:
    it = ROOT / "values-it" / "strings.xml"
    en = ROOT / "values" / "strings.xml"
    set_string(it, "bottom_navigation_conversations_label", "Chat")
    set_string(it, "bottom_navigation_meetings_label", "Casella")
    set_string(it, "bottom_navigation_calls_label", "Recenti")
    set_string(it, "bottom_navigation_dialpad_label", "Tastierino")
    set_string(en, "bottom_navigation_conversations_label", "Chat")
    set_string(en, "bottom_navigation_meetings_label", "Voicemail")
    set_string(en, "bottom_navigation_calls_label", "Recents")
    set_string(en, "bottom_navigation_dialpad_label", "Keypad")


if __name__ == "__main__":
    main()
