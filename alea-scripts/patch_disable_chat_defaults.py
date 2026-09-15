from pathlib import Path

p = Path("softphone-v2/android/app/src/main/java/org/linphone/core/CorePreferences.kt")
t = p.read_text(encoding="utf-8")
t2 = t.replace(
    'config.getBool("ui", "disable_chat_feature", false)',
    'config.getBool("ui", "disable_chat_feature", true)',
).replace(
    'config.getBool("ui", "disable_meetings_feature", false)',
    'config.getBool("ui", "disable_meetings_feature", true)',
)
p.write_text(t2, encoding="utf-8")
print("changed", t != t2)
