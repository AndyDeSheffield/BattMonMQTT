AI generated text
# BattMonMQTT

Battery telemetry foreground service for Android, publishing via MQTT.

## Features
- Foreground service with persistent MQTT connection
- Publishes battery level + temperature every change and every minute
- Configurable broker settings via Jetpack Compose UI
- Auto-start on boot

## Requirements
- Android Studio (Windows)
- Amazon Fire tablet (others not tested) with ADB debugging enabled for sideloading
- HiveMQ MQTT client library
- Jetpack Compose + DataStore

## Setup
1. Clone the repo:  
   `git clone https://github.com/grafton/battmonmqtt.git`
2. Open in Android Studio.
3. Connect Fire tablet via USB, enable developer mode.
4. Run with ADB:  
   `adb devices` → confirm tablet is listed.  
   Click **Run** in Android Studio.

## Configuration
- Launch app → enter broker host, port, username, password, topic.
- Settings are saved via Jetpack DataStore.
- Service auto-starts after reboot.

## License
MIT or Apache 2.0 (choose one).
