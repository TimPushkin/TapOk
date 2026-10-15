<img src="https://i.imgur.com/HGYVwjD.png" align="right" height="192" alt="App icon"/>

# TapOk – Android automation tool

**TapOk** is an Android application that allows automating tasks such as clicks and muting.

## Features

- Scanning the screen to find UI elements that can be detected by TapOk
- Detecting UI elements by text, accessibility description, view ID
- Performing an action when a certain element appears: clicking it, its ancestors or descendants, muting the device audio

Supports Android 11+, including Android TV.

## Downloads

- [APK](https://github.com/TimPushkin/TapOk/releases) – download, install, and update TapOk manually
  - Modern Android makes it hard to use accessibility services from apps installed via APK:
    - Play Protect forbids installing the APK — temporarily disable Play Protect, install the APK and enable it back
    - TapOk's accessibility service is greyed-out and cannot be enabled — tap the greyed-out service (this is important!), navigate to the apps list in the system settings and open TapOk's info there, allow "restricted settings" (usually under three dots in a corner), navigate back to the list of accessibility services and enable the service
- Google Play — planned
