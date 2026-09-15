from pathlib import Path

root = Path("softphone-v2/android/app/src/main")

# 1) colors
p = root / "res/values/colors.xml"
t = p.read_text(encoding="utf-8")
t = t.replace(
    '<color name="background_color_dark_mode">#191919</color>',
    '<color name="background_color_dark_mode">#0A0E17</color>',
)
t = t.replace(
    '<color name="background_color_alt_dark_mode">#303030</color>',
    '<color name="background_color_alt_dark_mode">#121826</color>',
)
if "alea_top_bar" not in t:
    t = t.replace(
        '<color name="alea_launcher_bg">#0A0E17</color>',
        '<color name="alea_launcher_bg">#0A0E17</color>\n'
        '    <color name="alea_top_bar">#121826</color>\n'
        '    <color name="alea_surface">#1A2234</color>',
    )
p.write_text(t, encoding="utf-8")
print("colors ok")

# 2) top bar
tb = root / "res/layout/main_activity_top_bar.xml"
tt = tb.read_text(encoding="utf-8")
tt = tt.replace(
    'android:background="?attr/color_main1_500"',
    'android:background="@color/alea_top_bar"',
)
tb.write_text(tt, encoding="utf-8")
print("top bar ok")

# 3) main activity chrome default
ma = root / "res/layout/main_activity.xml"
mt = ma.read_text(encoding="utf-8")
mt2 = mt.replace("default=@drawable/color_main1_500", "default=@drawable/color_alea_top_bar")
mt2 = mt2.replace(
    "(viewModel.atLeastOneCall ? @drawable/color_success_500 : @drawable/color_main1_500))",
    "(viewModel.atLeastOneCall ? @drawable/color_success_500 : @drawable/color_alea_top_bar))",
)
ma.write_text(mt2, encoding="utf-8")
print("main_activity changed", mt != mt2)

(root / "res/drawable/color_alea_top_bar.xml").write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/alea_top_bar" />
</shape>
""",
    encoding="utf-8",
)

# 4) status bar
for theme in (
    root / "res/values-night/themes.xml",
    root / "res/values/themes.xml",
):
    tt = theme.read_text(encoding="utf-8")
    tt2 = tt.replace(
        '<item name="android:statusBarColor">?attr/color_main1_500</item>',
        '<item name="android:statusBarColor">@color/alea_launcher_bg</item>',
    )
    theme.write_text(tt2, encoding="utf-8")
    print(theme.name, "statusbar", tt != tt2)

# 5) night window bg -> ALEA navy
nt = root / "res/values-night/themes.xml"
n = nt.read_text(encoding="utf-8")
n = n.replace(
    '<item name="android:windowBackground">@color/background_color_dark_mode</item>',
    '<item name="android:windowBackground">@color/alea_launcher_bg</item>',
)
n = n.replace(
    '<item name="color_main2_000">@color/background_color_dark_mode</item>',
    '<item name="color_main2_000">@color/alea_launcher_bg</item>',
)
nt.write_text(n, encoding="utf-8")
print("night surfaces ok")

# 6) linphonerc
rc = root / "assets/linphonerc_default"
r = rc.read_text(encoding="utf-8")
r = r.replace(
    'contact="Linphone Android" <sip:linphone.android@unknown-host>',
    'contact="ALEA Softphone" <sip:alea.softphone@unknown-host>',
)
if "disable_chat_feature" not in r:
    r = r.replace(
        "enable_secure_mode=0",
        "enable_secure_mode=0\n"
        "disable_chat_feature=1\n"
        "disable_meetings_feature=1\n"
        "dark_mode=1",
    )
rc.write_text(r, encoding="utf-8", newline="\n")
print("rc ok")
print("--- ui section ---")
for line in rc.read_text(encoding="utf-8").splitlines():
    if "disable_" in line or "enable_secure" in line or "dark_mode" in line or "contact=" in line or "[ui]" in line:
        print(line)
