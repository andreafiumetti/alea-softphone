from pathlib import Path

cell = Path("softphone-v2/android/app/src/main/res/layout/history_list_cell.xml")
c = cell.read_text(encoding="utf-8")
c = c.replace(
    'android:background="@drawable/primary_cell_background"',
    'android:background="@drawable/alea_list_card_background"',
)
if 'android:layout_marginStart="12dp"' not in c:
    c = c.replace(
        'android:layout_height="wrap_content"',
        'android:layout_height="wrap_content"\n'
        '        android:layout_marginStart="12dp"\n'
        '        android:layout_marginEnd="12dp"\n'
        '        android:layout_marginTop="6dp"\n'
        '        android:layout_marginBottom="6dp"',
        1,
    )
# bump paddings if still small
c = c.replace('android:paddingTop="8dp"', 'android:paddingTop="16dp"')
c = c.replace('android:paddingBottom="8dp"', 'android:paddingBottom="16dp"')
c = c.replace('android:paddingTop="14dp"', 'android:paddingTop="16dp"')
c = c.replace('android:paddingBottom="14dp"', 'android:paddingBottom="16dp"')
cell.write_text(c, encoding="utf-8")
print("cell ok")

# contacts list cell if exists
for name in ("contacts_list_cell.xml", "contacts_list_contact_cell.xml"):
    p = Path("softphone-v2/android/app/src/main/res/layout") / name
    if p.exists():
        t = p.read_text(encoding="utf-8")
        if "primary_cell_background" in t:
            t = t.replace(
                'android:background="@drawable/primary_cell_background"',
                'android:background="@drawable/alea_list_card_background"',
            )
            if 'layout_marginStart="12dp"' not in t:
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
            print("patched", name)

# Make top bar title color cyan for unmistakable brand signal
tb = Path("softphone-v2/android/app/src/main/res/layout/main_activity_top_bar.xml")
tt = tb.read_text(encoding="utf-8")
tt2 = tt.replace(
    'android:textColor="?attr/color_on_main"',
    'android:textColor="@color/orange_main_500"',
    1,  # only first = title
)
tb.write_text(tt2, encoding="utf-8")
print("title cyan", tt != tt2)

# Rename bottom nav Calls label in IT+EN to something branded? Keep functional labels.
# Change app string for history empty - skip

print(cell.read_text(encoding="utf-8").splitlines()[21:45])
