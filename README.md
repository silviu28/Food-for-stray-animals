<img width="1280" height="720" alt="thumbnail" src="https://github.com/user-attachments/assets/c50a8191-891a-447e-b813-67a17d550d3d" />

#### Food for Stray Animals
:heart: Contribute to a greater cause.
A companion app for a remote-controlled food dispenser. Feed animals in tight or inaccesible spaces in an easy and fun way. Just connect via Bluetooth and you're good to go, no strings attached.

#### Build
Prerequisites:
* Java 11 or higher
* Android SDK 35
* ADB/build-tools (optional, if you don't want to manually install the APK)
* An Android device or emulator, running Oreo or higher
  
To build the project, run in the project's root directory:

```

./gradlew build assembleDebug

```

For a release APK, run:

```

./gradlew build assembleRelease

```

The APK should reside somewhere here:
```

app/build/outputs/apk/androidTest/debug

```

If you're on Linux and the build command won't run, the Gradle wrapper might not have execution permissions. Run:
```

chmod +x gradlew

```

To deploy your app to a device, you must have ADB installed. Run:

```

adb install app/build/outputs/apk/debug/app-debug.apk (or wherever else your APK resides)

```

And that's it! The app should run on Android versions as low as Android Oreo (8.0/SDK 26), requiring Bluetooth permission instead of Nearby Devices.

⚠️ The app has been functionally tested only with Bluetooth connections to the Arduino Bluetooth module. Anything else might not work as expected.
