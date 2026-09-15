from pathlib import Path

h = Path("softphone-v2/android/app/src/main/res/layout/history_list_fragment.xml")
t = h.read_text(encoding="utf-8")
t = t.replace(
    'android:background="@drawable/shape_squircle_white_r20_top_background"',
    'android:background="@color/bc_white"',
)
t = t.replace(
    'android:background="@drawable/shape_squircle_white_r20_top_shadow"',
    'android:background="@android:color/transparent"',
)
h.write_text(t, encoding="utf-8")
print("sheet flat ok")
