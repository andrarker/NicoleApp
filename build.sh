#!/usr/bin/env bash
set -e

ANDROID_SDK_DIR="/home/runner/android-sdk"
JAVA_HOME_PATH=$(dirname $(dirname $(which java)) 2>/dev/null || echo "")

# Set up Java
if [ -z "$JAVA_HOME_PATH" ]; then
  echo "ERROR: Java not found. Please ensure java-graalvm22.3 module is installed."
  exit 1
fi
export JAVA_HOME="$JAVA_HOME_PATH"

# Download Android SDK command-line tools if not present
if [ ! -d "$ANDROID_SDK_DIR/cmdline-tools/latest" ]; then
  echo "Downloading Android command-line tools..."
  mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
  curl -s "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
    -o /tmp/cmdline-tools.zip
  cd /tmp && unzip -q cmdline-tools.zip
  mv /tmp/cmdline-tools "$ANDROID_SDK_DIR/cmdline-tools/latest"
  cd -
  echo "Android command-line tools downloaded."
fi

export ANDROID_HOME="$ANDROID_SDK_DIR"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# Accept licenses
yes | sdkmanager --licenses > /dev/null 2>&1 || true

# Install required SDK components if not present
if [ ! -d "$ANDROID_SDK_DIR/platforms/android-35" ]; then
  echo "Installing Android SDK platform 35..."
  sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
fi

# Fix gradle wrapper jar if needed
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ] || [ "$(head -c 4 "$WRAPPER_JAR" | od -A n -t x1 | tr -d ' \n')" != "504b0304" ]; then
  echo "Downloading gradle wrapper jar..."
  curl -sL "https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar" \
    -o "$WRAPPER_JAR"
fi

# Write local.properties
echo "sdk.dir=$ANDROID_SDK_DIR" > local.properties

# Make gradlew executable
chmod +x ./gradlew

echo "Building NicoleApp debug APK..."
./gradlew assembleDebug --no-daemon

echo ""
echo "Build complete!"
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
  echo "APK: app/build/outputs/apk/debug/app-debug.apk"
  ls -lh app/build/outputs/apk/debug/app-debug.apk
fi
