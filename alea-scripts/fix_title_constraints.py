from pathlib import Path

tb = Path("softphone-v2/android/app/src/main/res/layout/main_activity_top_bar.xml")
t = tb.read_text(encoding="utf-8")
t = t.replace(
    'app:layout_constraintTop_toTopOf="@id/avatar"',
    'app:layout_constraintTop_toTopOf="parent"',
)
t = t.replace(
    'app:layout_constraintBottom_toBottomOf="@id/avatar"',
    'app:layout_constraintBottom_toBottomOf="parent"',
)
tb.write_text(t, encoding="utf-8")
print("ok")
