import zipfile, re, glob, os

aar = glob.glob(
    os.path.expanduser(
        r"~\.gradle\caches\modules-2\files-2.1\org.linphone\linphone-sdk-android\5.5.19\**\*.aar"
    ),
    recursive=True,
)[0]
cj = zipfile.ZipFile(zipfile.ZipFile(aar).open("classes.jar"))
for cls in [
    "org/linphone/core/PresenceActivity$Type.class",
    "org/linphone/core/PresenceBasicStatus.class",
    "org/linphone/core/ConsolidatedPresence.class",
]:
    data = cj.read(cls)
    strs = [s.decode("ascii", "ignore") for s in re.findall(rb"[\x20-\x7e]{2,}", data)]
    print("===", cls)
    print(strs)
