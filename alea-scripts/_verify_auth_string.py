from pathlib import Path
p = Path(r"c:\Users\ALEA Defender\Desktop\ALEA PBX\softphone-v2\android\app\src\main\res\values-it\strings.xml")
raw = p.read_bytes()
s = raw.decode("utf-8")
print("C383", raw.count(bytes([0xC3, 0x83])))
for line in s.splitlines():
    if "account_settings_dialog_invalid_password_message" in line:
        print(repr(line[:250]))
        assert "è assente" in line
        print("auth OK")
        break
