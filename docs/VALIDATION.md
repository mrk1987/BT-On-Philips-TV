# Export validation

On 2026-10-24, the standalone `panel/build.ps1`, `overlay/build.ps1` and `pairing/build.ps1` all
completed successfully with JDK 17 and Android SDK platform/build-tools 34. APK signing verification
passed. Locally generated debug signing keys and build outputs are excluded from Git.

The panel source was copied from the generic version installed and visually checked on the TV.
The overlay manifest/resource was copied from the working profile overlay. The parameterized pairing
helper compiled successfully; the live successful pairing used its earlier fixed-address prototype.
No root flash or live TV configuration change was performed while exporting these repositories.

Known identifiers, personal filesystem paths, private-key headers and token-like strings were checked
in the tracked-source candidate files. No private firmware images, traces, ADB keys, theme backups or
device databases are included. This check does not establish compatibility with other TV models.

The user confirmed that the physical blue button both opens the panel and closes it when already open.
