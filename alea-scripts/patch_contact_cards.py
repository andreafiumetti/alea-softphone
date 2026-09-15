from pathlib import Path

p = Path("softphone-v2/android/app/src/main/res/layout/contact_list_cell.xml")
t = p.read_text(encoding="utf-8")
if "primary_cell_background" in t:
    t = t.replace(
        'android:background="@drawable/primary_cell_background"',
        'android:background="@drawable/alea_list_card_background"',
    )
    if 'android:layout_marginStart="12dp"' not in t:
        t = t.replace(
            'android:layout_height="wrap_content"',
            'android:layout_height="wrap_content"\n'
            '        android:layout_marginStart="12dp"\n'
            '        android:layout_marginEnd="12dp"\n'
            '        android:layout_marginTop="6dp"\n'
            '        android:layout_marginBottom="6dp"',
            1,
        )
    p.write_text(t, encoding="utf-8")
    print("contacts patched")
else:
    print("no primary bg")
    for line in t.splitlines():
        if "background" in line:
            print(line)
