# Rebuilding Windows after the Smart App Control break (2.5.0.12)

## What broke

2.5.0.12 fails to start on Windows 11 PCs that have Smart App Control on:

> SimpMusic.exe - Bad Image: `...\bin\jimage.dll` is either not designed to run on Windows or it
> contains an error ... Error status 0xc0e90002.

`0xC0E90002` is `STATUS_SYSTEM_INTEGRITY_POLICY_VIOLATION`: Windows refused to load the DLL. The
file is not corrupt.

Conveyor signs every **unsigned** binary in the package with our self-signed `CN=SimpMusic`
certificate. Smart App Control does not trust that certificate, even after `install.bat` puts it
in Trusted People.

| Build | Bundled JDK | Signer of `jimage.dll`, `jvm.dll`, `java.dll` |
|---|---|---|
| 2.5.0.10 | jdk.java.net OpenJDK 21.0.2 | Oracle America (works) |
| 2.5.0.12 | Microsoft OpenJDK 21.0.12 | CN=SimpMusic (blocked) |

Microsoft's build ships its DLLs unsigned, so all 40 Java DLLs ended up signed only by us.

## The fix in this commit

`desktopApp/build.gradle.kts` now bundles **Eclipse Temurin** by default. Its Windows DLLs are
signed by Eclipse Foundation. Temurin 21.0.13 also has the Apple Silicon C1 crash fix
(JDK-8310844, JDK-8335662), so it serves the Mac too. The include path can be overridden with
`-PconveyorJdk=...`.

## Build steps on the Mac

1. **Confirm the include name.** I could not check Conveyor's stdlib from Windows, so
   `/stdlib/jdk/21.0.13/eclipse.conf` is a best guess at the vendor name and version.

   ```sh
   conveyor jdk-table | grep -i -E 'temurin|eclipse|azul|zulu' | grep ' 21\.'
   ```

   Pick the newest 21.0.x Temurin row (or Zulu, which Azul also signs). If its path differs from
   the default, change the default in `desktopApp/build.gradle.kts` (search for `conveyorJdk`) and
   commit, so every machine builds the same thing. To try a path once without editing:
   `./gradlew :desktopApp:writeConveyorConfig -PconveyorJdk=/stdlib/jdk/<ver>/<vendor>.conf`.

2. **Check that it resolves**:

   ```sh
   ./gradlew :desktopApp:writeConveyorConfig --no-configuration-cache
   conveyor -Kapp.machines=windows.amd64 json | grep -E '"(vendor|version)"' | head
   ```

3. **Build the Windows zip, without `--upload` yet**:

   ```sh
   scripts/build-windows.sh
   ```

4. **Verify the signatures before publishing.** Copy
   `build/windows/SimpMusic-Windows-installer.zip` to a Windows PC and run:

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\windows\fork\check-signatures.ps1 SimpMusic-Windows-installer.zip
   ```

   It must print `OK` and name Eclipse Foundation (or Azul) as the signer of `jimage.dll`.
   `FAIL` means the JDK's DLLs reached the package unsigned again: do not publish.
   `libmpv-2.dll` is reported as a note. Upstream ships it unsigned, and 2.5.0.10 ran with it
   the same way.

5. **Publish**: `scripts/build-windows.sh --upload`, plus the download site as usual
   (`publish.mjs`).

6. **Rebuild the Macs too.** The JDK change applies to every platform: run
   `scripts/build-mac.sh --publish` and check that the app still launches on Apple Silicon.

Bump the version before releasing, so the updater offers the fixed build over 2.5.0.12.

## For users stuck on 2.5.0.12 until then

2.5.0.10 still launches with Smart App Control on:
`https://nhacnheo.pages.dev/f/windows/2.5.0.10-mw8ywr2e/SimpMusic-Windows-installer.zip`
