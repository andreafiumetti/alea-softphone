from pathlib import Path

# contacts list fragment: mock chrome
p = Path("softphone-v2/android/app/src/main/res/layout/contacts_list_fragment.xml")
t = p.read_text(encoding="utf-8")
t = t.replace(
    """                    bind:showExtraAction="@{viewModel.showFilter}"
                    bind:extraActionIcon="@{@drawable/funnel}"
                    bind:extraActionClickListener="@{filterClickListener}"
                    bind:extraActionContentDescription="@{@string/content_description_contacts_list_filters}"
                    bind:extraActionEnabled="@{true}"
""",
    """                    bind:showExtraAction="@{true}"
                    bind:extraActionIcon="@{@drawable/plus_circle}"
                    bind:extraActionClickListener="@{onNewContactClicked}"
                    bind:extraActionContentDescription="@{@string/content_description_create_new_contact}"
                    bind:extraActionEnabled="@{true}"
""",
)
t = t.replace(
    'android:background="@drawable/shape_squircle_white_r20_top_shadow"',
    'android:background="@android:color/transparent"',
)
t = t.replace(
    'android:background="@drawable/shape_squircle_white_r20_top_background"',
    'android:background="@color/bc_white"',
)
# hide FAB if present
if "FloatingActionButton" in t and 'android:id="@+id/new_contact"' in t:
    t = t.replace(
        'android:id="@+id/new_contact"',
        'android:id="@+id/new_contact"\n                    android:visibility="gone"',
        1,
    )
p.write_text(t, encoding="utf-8")
print("contacts fragment")

# contact list cell flat like history
cell = Path("softphone-v2/android/app/src/main/res/layout/contact_list_cell.xml")
c = cell.read_text(encoding="utf-8")
c = c.replace("@drawable/alea_list_card_background", "@drawable/alea_list_row_background")
c = c.replace("@drawable/primary_cell_background", "@drawable/alea_list_row_background")
# remove card margins if present
for m in (
    'android:layout_marginStart="12dp"\n        ',
    'android:layout_marginEnd="12dp"\n        ',
    'android:layout_marginTop="6dp"\n        ',
    'android:layout_marginBottom="6dp"\n        ',
):
    c = c.replace(m, "")
cell.write_text(c, encoding="utf-8")
print("contact cell")
