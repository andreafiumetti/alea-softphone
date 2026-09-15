from pathlib import Path

root = Path("softphone-v2/android/app/src/main")

# --- Force light theme (mock) ---
prefs = root / "java/org/linphone/core/CorePreferences.kt"
pt = prefs.read_text(encoding="utf-8")
pt = pt.replace(
    'get() = config.getBool("ui", "dark_mode_allowed", true)',
    'get() = config.getBool("ui", "dark_mode_allowed", false)',
)
prefs.write_text(pt, encoding="utf-8")

rc = root / "assets/linphonerc_default"
r = rc.read_text(encoding="utf-8")
r = r.replace("dark_mode=1", "dark_mode=0\ndark_mode_allowed=0")
if "dark_mode_allowed" not in r:
    r = r.replace("enable_secure_mode=0", "enable_secure_mode=0\ndark_mode_allowed=0")
rc.write_text(r, encoding="utf-8", newline="\n")

# --- Top bar: white, mock chrome ---
(root / "res/drawable/alea_top_bar_background.xml").write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/bc_white" />
        </shape>
    </item>
    <item android:gravity="bottom" android:height="1dp">
        <shape android:shape="rectangle">
            <solid android:color="#E5E7EB" />
        </shape>
    </item>
</layer-list>
""",
    encoding="utf-8",
)

# Dark icons on light top bar
(root / "res/color/main_top_bar_icon_color.xml").write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_enabled="false" android:color="?attr/color_grey_300" />
    <item android:state_pressed="true" android:color="?attr/color_main1_500" />
    <item android:color="?attr/color_main2_600" />
</selector>
""",
    encoding="utf-8",
)

tb = root / "res/layout/main_activity_top_bar.xml"
tt = tb.read_text(encoding="utf-8")
# Hide avatar
tt = tt.replace(
    """        <include
            android:id="@+id/avatar"
            android:onClick="@{() -> viewModel.openDrawerMenu()}"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="5dp"
            layout="@layout/contact_avatar"
            bind:model="@{viewModel.account}"
            app:layout_constraintStart_toEndOf="@id/drawer_menu"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"/>""",
    """        <include
            android:id="@+id/avatar"
            android:onClick="@{() -> viewModel.openDrawerMenu()}"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="5dp"
            android:visibility="gone"
            layout="@layout/contact_avatar"
            bind:model="@{viewModel.account}"
            app:layout_constraintStart_toEndOf="@id/drawer_menu"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"/>""",
)
# Title: dark, start after menu (avatar gone)
tt = tt.replace(
    'android:textColor="@color/orange_main_500"',
    'android:textColor="?attr/color_main2_900"',
)
tt = tt.replace(
    'app:layout_constraintStart_toEndOf="@id/avatar"',
    'app:layout_constraintStart_toEndOf="@id/drawer_menu"',
)
# Hide search by default visibility gone
tt = tt.replace(
    """        <ImageView
            style="@style/icon_main_top_bar_button_style"
            android:id="@+id/search_toggle"
            android:onClick="@{() -> viewModel.openSearchBar()}"
            android:layout_width="wrap_content"
            android:layout_height="0dp"
            android:src="@drawable/magnifying_glass"
            android:contentDescription="@string/content_description_open_filter"
            app:layout_constraintEnd_toStartOf="@id/extra_action"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent" />""",
    """        <ImageView
            style="@style/icon_main_top_bar_button_style"
            android:id="@+id/search_toggle"
            android:onClick="@{() -> viewModel.openSearchBar()}"
            android:layout_width="wrap_content"
            android:layout_height="0dp"
            android:src="@drawable/magnifying_glass"
            android:visibility="gone"
            android:contentDescription="@string/content_description_open_filter"
            app:layout_constraintEnd_toStartOf="@id/extra_action"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent" />""",
)
# Title end constraint: to extra_action directly
tt = tt.replace(
    'app:layout_constraintEnd_toStartOf="@id/search_toggle"',
    'app:layout_constraintEnd_toStartOf="@id/extra_action"',
)
# Group referenced ids still include avatar/search - ok if gone
tb.write_text(tt, encoding="utf-8")
print("top bar mock")

# --- history fragment: + instead of trash, hide FAB ---
hf = root / "res/layout/history_list_fragment.xml"
h = hf.read_text(encoding="utf-8")
h = h.replace(
    'android:background="@color/alea_launcher_bg"',
    'android:background="@color/bc_white"',
)
h = h.replace(
    """                    bind:showExtraAction="@{true}"
                    bind:extraActionIcon="@{@drawable/trash_simple}"
                    bind:extraActionClickListener="@{deleteAllClickListener}"
                    bind:extraActionContentDescription="@{@string/menu_delete_history}"
                    bind:extraActionEnabled="@{!viewModel.callLogs.empty}"
""",
    """                    bind:showExtraAction="@{true}"
                    bind:extraActionIcon="@{@drawable/plus_circle}"
                    bind:extraActionClickListener="@{startCallClickListener}"
                    bind:extraActionContentDescription="@{@string/content_description_call_start}"
                    bind:extraActionEnabled="@{true}"
""",
)
# Hide FAB
h = h.replace(
    """                <com.google.android.material.floatingactionbutton.FloatingActionButton
                    android:onClick="@{startCallClickListener}"
                    android:id="@+id/new_call"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="end|bottom"
                    android:layout_margin="16dp"
                    android:src="@drawable/phone_plus"
                    android:contentDescription="@string/content_description_call_start"
                    app:tint="?attr/color_on_main"
                    app:backgroundTint="?attr/color_main1_500"
                    app:shapeAppearanceOverlay="@style/rounded"
                    app:layout_constraintEnd_toEndOf="parent"
                    app:layout_constraintBottom_toTopOf="@id/bottom_nav_bar" />
""",
    """                <com.google.android.material.floatingactionbutton.FloatingActionButton
                    android:onClick="@{startCallClickListener}"
                    android:id="@+id/new_call"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="end|bottom"
                    android:layout_margin="16dp"
                    android:visibility="gone"
                    android:src="@drawable/phone_plus"
                    android:contentDescription="@string/content_description_call_start"
                    app:tint="?attr/color_on_main"
                    app:backgroundTint="?attr/color_main1_500"
                    app:shapeAppearanceOverlay="@style/rounded"
                    app:layout_constraintEnd_toEndOf="parent"
                    app:layout_constraintBottom_toTopOf="@id/bottom_nav_bar" />
""",
)
hf.write_text(h, encoding="utf-8")
print("history fragment")

# --- flat list cell like mock ---
(root / "res/drawable/alea_list_row_background.xml").write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="#F3F4F6" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/bc_white" />
        </shape>
    </item>
</selector>
""",
    encoding="utf-8",
)

cell = root / "res/layout/history_list_cell.xml"
cell.write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:bind="http://schemas.android.com/tools">

    <data>
        <import type="android.view.View" />
        <variable
            name="model"
            type="org.linphone.ui.main.history.model.CallLogModel" />
        <variable
            name="onClickListener"
            type="View.OnClickListener" />
        <variable
            name="onLongClickListener"
            type="View.OnLongClickListener" />
        <variable
            name="onCallClickListener"
            type="View.OnClickListener" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:onClick="@{onClickListener}"
        android:onLongClickListener="@{onLongClickListener}"
        android:onContextClick="@{onLongClickListener}"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingTop="14dp"
        android:paddingBottom="14dp"
        android:paddingStart="20dp"
        android:paddingEnd="20dp"
        android:background="@drawable/alea_list_row_background">

        <include
            android:id="@+id/avatar"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            layout="@layout/contact_avatar"
            bind:model="@{model.avatarModel}"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <androidx.appcompat.widget.AppCompatTextView
            style="@style/default_text_style_800"
            android:id="@+id/name"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="14dp"
            android:layout_marginEnd="8dp"
            android:text="@{model.avatarModel.name, default=`John Doe`}"
            android:textSize="16sp"
            android:textColor="?attr/color_main2_900"
            android:maxLines="1"
            android:ellipsize="end"
            app:layout_constraintStart_toEndOf="@id/avatar"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toTopOf="@id/date_time"/>

        <ImageView
            android:id="@+id/call_status"
            android:layout_width="12dp"
            android:layout_height="12dp"
            android:src="@{model.iconResId, default=@drawable/outgoing_call}"
            android:contentDescription="@null"
            app:layout_constraintStart_toStartOf="@id/name"
            app:layout_constraintTop_toTopOf="@id/date_time"
            app:layout_constraintBottom_toBottomOf="@id/date_time" />

        <androidx.appcompat.widget.AppCompatTextView
            style="@style/default_text_style_300"
            android:id="@+id/date_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="6dp"
            android:gravity="center"
            android:text="@{model.dateTime, default=`13 aout 2023`}"
            android:textSize="13sp"
            android:textColor="?attr/color_hint_text"
            app:layout_constraintStart_toEndOf="@id/call_status"
            app:layout_constraintTop_toBottomOf="@id/name"
            app:layout_constraintBottom_toTopOf="@id/separator"/>

        <!-- Hidden: mock has no trailing call icon -->
        <ImageView
            android:onClick="@{onCallClickListener}"
            android:id="@+id/call"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:visibility="gone"
            android:src="@drawable/phone"
            android:contentDescription="@string/content_description_call_start"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <View
            android:id="@+id/separator"
            android:layout_width="0dp"
            android:layout_height="1dp"
            android:background="#F3F4F6"
            android:importantForAccessibility="no"
            app:layout_constraintStart_toStartOf="@id/name"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"/>

    </androidx.constraintlayout.widget.ConstraintLayout>

</layout>
""",
    encoding="utf-8",
)
print("history cell flat")

# --- bottom nav light like mock ---
(root / "res/drawable/alea_bottom_nav_background.xml").write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/bc_white" />
        </shape>
    </item>
    <item android:gravity="top" android:height="1dp">
        <shape android:shape="rectangle">
            <solid android:color="#E5E7EB" />
        </shape>
    </item>
</layer-list>
""",
    encoding="utf-8",
)

# Day theme polish: light gray list bg feel - already white
# Soft accent for avatar in day theme
day = root / "res/values/themes.xml"
d = day.read_text(encoding="utf-8")
d = d.replace(
    '<item name="android:statusBarColor">@color/alea_launcher_bg</item>',
    '<item name="android:statusBarColor">@color/bc_white</item>',
)
d = d.replace(
    '<item name="color_avatar_background">@color/gray_main2_200</item>',
    '<item name="color_avatar_background">#E0F2FE</item>',
)
d = d.replace(
    '<item name="color_avatar_text">@color/gray_main2_600</item>',
    '<item name="color_avatar_text">#0369A1</item>',
)
day.write_text(d, encoding="utf-8")

# contacts list fragment white bg too
cf = root / "res/layout/contacts_list_fragment.xml"
if cf.exists():
    ct = cf.read_text(encoding="utf-8")
    ct = ct.replace(
        'android:background="@color/alea_launcher_bg"',
        'android:background="@color/bc_white"',
    )
    # FAB stay for contacts or hide - mock has + in header; leave for now
    cf.write_text(ct, encoding="utf-8")

print("done mock home pass")
