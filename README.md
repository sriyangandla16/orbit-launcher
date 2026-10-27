# Orbit Launcher

A fast, clean, Apple TV–style home screen for **Amazon Fire TV Stick** (and Android TV).

No ads. No sponsored rows. No autoplaying trailers. Just your apps.

Built in plain Java with **zero libraries**, so it stays tiny and smooth even on old sticks with 1 GB of RAM, like the Fire TV Stick 2nd gen (model LY73PR).

> Made by Sriyan Gandla · [TECHNO ROBO X on YouTube](#) <!-- add your channel link -->

---

## Features

- Big clock and date, dark glassy background
- **Favourites** row (up to 4 apps) with large tiles
- **All apps** grid with official TV banners when the app has one
- Smooth "lift" animation and app name when a tile is focused
- Hold **OK** (or press **Menu**) on a tile to:
  add/remove Favourites, move left/right, hide the app, open App info
- **Home button takeover** on Fire TV (see "How it works")
- **Amazon Home** button for when you need Amazon's screen (e.g. Prime Video)
- Opens automatically after the stick restarts
- Works as a normal home screen app on Android TV / Google TV too

## How it works

1. **The launcher** is an ordinary app. It asks Android for every installed app and draws them as tiles.
2. **The Home button trick.** Fire OS always opens Amazon's home screen when you press Home and won't let you choose another one. Orbit includes a tiny *accessibility service* that is told when a new screen opens. When it sees Amazon's home screen, it opens Orbit on top. You'll see a quick flash of Amazon's screen first.
   The service only watches Amazon's launcher, and it **cannot read what's on your screen**.
3. **Nothing is permanently changed.** Uninstall Orbit and your Fire TV is exactly as it was.

## Install

### What you need
- A computer with **ADB** ([Android platform-tools](https://developer.android.com/tools/releases/platform-tools))
- The Fire TV and computer on the **same Wi-Fi**

### 1. Prepare the Fire TV
1. **Settings → Device** (or **My Fire TV**) **→ About** → click the device name **7 times** to unlock Developer options.
2. **Developer options →** turn on **ADB debugging** and **Apps from Unknown Sources**.
3. **About → Network** → note the **IP address**.

### 2. Get the APK
Download `OrbitLauncher.apk` from **Releases**, or from the latest **Actions** run (it's built automatically).

### 3. Run the installer
Put `OrbitLauncher.apk` in the `scripts` folder, then:

- Windows: `scripts\install.bat 192.168.1.50`
- Mac/Linux: `./scripts/install.sh 192.168.1.50`

(use your TV's IP). Accept the "Allow USB debugging?" popup on the TV. Then press **Home** on the remote.

### Remove it
- Windows: `scripts\uninstall.bat 192.168.1.50`
- Mac/Linux: `./scripts/uninstall.sh 192.168.1.50`

## Troubleshooting

**Home still opens Amazon's screen.**
Open Orbit → **Customise**. Check the status at the top says ON. Under "Amazon screens detected" you'll see the names of Amazon's screens. If none contains `HomeActivity`, switch **Replace** to "every Amazon home screen", or run `scripts/debug-log` while pressing Home and open an issue with the output.

**I can't get to Prime Video.**
Use the **Amazon Home** button at the top of Orbit. Takeover pauses until you open Orbit again (or for 30 minutes).

## Build it yourself
Open the folder in Android Studio and press Run, or:

```
./gradlew assembleRelease
```

The APK is signed with a public **test key** (`app/orbit-test.keystore`) so updates install over each other. Use your own private key if you distribute builds widely.

## Not affiliated
Orbit is an independent project. It is not made by, endorsed by or connected to Amazon or Apple. "Fire TV" is a trademark of Amazon.

## License
MIT
