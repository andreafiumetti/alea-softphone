from pathlib import Path

root = Path("softphone-v2/android/app/src/main")

# --- colors: ALEA mock palette tokens ---
colors = root / "res/values/colors.xml"
t = colors.read_text(encoding="utf-8")
extras = """
    <color name="alea_line">#243044</color>
    <color name="alea_text">#E8EEF7</color>
    <color name="alea_muted">#9AA8BC</color>
    <color name="alea_avatar_bg">#0E3A46</color>
    <color name="alea_avatar_text">#67E8F9</color>
    <color name="alea_nav">#0D121C</color>
"""
if "alea_line" not in t:
    t = t.replace(
        '<color name="alea_surface">#1A2234</color>',
        '<color name="alea_surface">#1A2234</color>' + extras,
    )
# soften cyan a touch toward mock hover for 100
t = t.replace(
    '<color name="orange_main_100">#CFFAFE</color>',
    '<color name="orange_main_100">#A5F3FC</color>',
)
colors.write_text(t, encoding="utf-8")
print("colors")

# --- night theme: map to mock ---
nt = root / "res/values-night/themes.xml"
n = nt.read_text(encoding="utf-8")
replacements = {
    '<item name="color_main2_100">@color/gray_main2_800</item>':
        '<item name="color_main2_100">@color/alea_surface</item>',
    '<item name="color_main2_200">@color/gray_main2_700</item>':
        '<item name="color_main2_200">@color/alea_top_bar</item>',
    '<item name="color_background_contrast_in_dark_mode">@color/background_color_dark_mode</item>':
        '<item name="color_background_contrast_in_dark_mode">@color/alea_nav</item>',
    # also if already alea_launcher from prior pass
    '<item name="color_background_contrast_in_dark_mode">@color/alea_launcher_bg</item>':
        '<item name="color_background_contrast_in_dark_mode">@color/alea_nav</item>',
    '<item name="color_separator">@color/gray_main2_400</item>':
        '<item name="color_separator">@color/alea_line</item>',
    '<item name="color_text">@color/gray_main2_100</item>':
        '<item name="color_text">@color/alea_text</item>',
    '<item name="color_hint_text">@color/gray_main2_300</item>':
        '<item name="color_hint_text">@color/alea_muted</item>',
    '<item name="color_avatar_background">@color/gray_main2_700</item>':
        '<item name="color_avatar_background">@color/alea_avatar_bg</item>',
    '<item name="color_avatar_text">@color/gray_main2_200</item>':
        '<item name="color_avatar_text">@color/alea_avatar_text</item>',
    '<item name="color_text_field_background">@color/gray_main2_700</item>':
        '<item name="color_text_field_background">@color/alea_surface</item>',
}
for a, b in replacements.items():
    n = n.replace(a, b)
nt.write_text(n, encoding="utf-8")
print("night theme")

# --- list pages: no cyan sheet peek ---
for name in (
    "history_list_fragment.xml",
    "contacts_list_fragment.xml",
    "chat_list_fragment.xml",
    "meetings_list_fragment.xml",
):
    p = root / "res/layout" / name
    if not p.exists():
        continue
    s = p.read_text(encoding="utf-8")
    s2 = s.replace(
        'android:background="?attr/color_main1_500"',
        'android:background="@color/alea_launcher_bg"',
    )
    p.write_text(s2, encoding="utf-8")
    print("fragment bg", name, s != s2)

# --- history cells: mock-like hierarchy ---
cell = root / "res/layout/history_list_cell.xml"
c = cell.read_text(encoding="utf-8")
c = c.replace('android:paddingTop="8dp"', 'android:paddingTop="14dp"')
c = c.replace('android:paddingBottom="8dp"', 'android:paddingBottom="14dp"')
c = c.replace('android:textSize="14sp"', 'android:textSize="16sp"')
c = c.replace(
    'android:textColor="?attr/color_main2_800"',
    'android:textColor="@color/alea_text"',
)
c = c.replace(
    'app:tint="?attr/color_main2_500"',
    'app:tint="?attr/color_main1_500"',
)
# date color
if 'android:id="@+id/date_time"' in c and 'android:textColor="@color/alea_muted"' not in c:
    c = c.replace(
        '''android:id="@+id/date_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="5dp"
            android:gravity="center"
            android:text="@{model.dateTime, default=`13 aout 2023`}"
            android:textSize="12sp"''',
        '''android:id="@+id/date_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="5dp"
            android:gravity="center"
            android:text="@{model.dateTime, default=`13 aout 2023`}"
            android:textSize="12sp"
            android:textColor="@color/alea_muted"''',
)
cell.write_text(c, encoding="utf-8")
print("history cell")

# --- bottom nav: subtle top line via drawable ---
(nav_bg := root / "res/drawable/alea_bottom_nav_background.xml").write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/alea_nav" />
        </shape>
    </item>
    <item android:gravity="top" android:height="1dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/alea_line" />
        </shape>
    </item>
</layer-list>
""",
    encoding="utf-8",
)
bn = root / "res/layout/bottom_nav_bar.xml"
b = bn.read_text(encoding="utf-8")
b = b.replace(
    'android:background="?attr/color_background_contrast_in_dark_mode"',
    'android:background="@drawable/alea_bottom_nav_background"',
)
bn.write_text(b, encoding="utf-8")
print("bottom nav")

# --- top bar: thin accent underline ---
(tb_bg := root / "res/drawable/alea_top_bar_background.xml").write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/alea_top_bar" />
        </shape>
    </item>
    <item android:gravity="bottom" android:height="2dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/orange_main_500" />
        </shape>
    </item>
</layer-list>
""",
    encoding="utf-8",
)
tb = root / "res/layout/main_activity_top_bar.xml"
tt = tb.read_text(encoding="utf-8")
tt = tt.replace(
    'android:background="@color/alea_top_bar"',
    'android:background="@drawable/alea_top_bar_background"',
)
tb.write_text(tt, encoding="utf-8")
print("top bar accent line")

# color_alea_top_bar drawable used by main_activity can stay solid
print("done")
