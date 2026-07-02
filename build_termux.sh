#!/bin/bash
# =========================================================================
# Termux Easy-Build & Setup Assistant for Project Organizer (Git-Pro-Manager)
# =========================================================================
#
# This script automates compiling the Android APK directly on your device.
# It handles updating repositories, resolving JDK 17 dependencies, 
# configuring paths, setting executable permissions, and building the APK.

clear
echo "========================================================="
echo "       TERMUX EASY-BUILD & SETUP ASSISTANT       "
echo "========================================================="
echo ""

# 1. Environment Verification
if [ -d "/data/data/com.termux" ]; then
    echo "[+] Running inside native Termux on Android."
else
    echo "[-] ERROR: This script expects to run inside Termux on Android."
    echo "    On other systems, please use: ./gradlew assembleDebug"
    exit 1
fi

# 2. Diagnose & Fix Old/Broken Google Play Store Repositories
echo ""
echo "[*] Auditing Termux repository configuration..."
SOURCES_FILE="/data/data/com.termux/files/usr/etc/apt/sources.list"

if [ -f "$SOURCES_FILE" ]; then
    if grep -q "termux.net" "$SOURCES_FILE"; then
        echo "[!] WARNING: You are using the legacy, obsolete Google Play Store"
        echo "    version of Termux (last updated 2020). Its packages are broken"
        echo "    and do not support modern items like OpenJDK 17."
        echo ""
        echo "[*] Redirecting repository to a fully operational mirror..."
        
        # Backup original configuration
        cp "$SOURCES_FILE" "${SOURCES_FILE}.bak"
        
        # Write clean stable repository pointing to main archive
        echo "deb https://packages.termux.org/apt/termux-main stable main" > "$SOURCES_FILE"
        echo "[+] Successfully configured modern repository mirrors!"
    else
        echo "[+] Repository mirrors appear modern and active."
    fi
fi

# 3. Synchronize Packages & Package Upgrades
echo ""
echo "[*] Synchronizing package lists..."
pkg update -y || apt-get update -y

# 4. Enforce Dependencies (Git & OpenJDK 17)
echo ""
echo "[*] Installing critical build dependencies..."
echo "[*] Ensuring Git is installed..."
pkg install git -y || apt-get install git -y

echo "[*] Installing OpenJDK 17 (Java Development Kit)..."
pkg install openjdk-17 -y || apt-get install openjdk-17 -y

# 5. Set JAVA_HOME and verify compiler support
echo ""
echo "[*] Resolving Java Development Kit path..."
export JAVA_HOME="/data/data/com.termux/files/usr/opt/openjdk-17"

if [ ! -d "$JAVA_HOME" ]; then
    # Dynamically find installed JVM path as fallback
    FOUND_JVM=$(find /data/data/com.termux/files/usr/lib/jvm -maxdepth 2 -name "openjdk-17" -o -name "java-17-openjdk" 2>/dev/null | head -n 1)
    if [ -n "$FOUND_JVM" ]; then
        export JAVA_HOME="$FOUND_JVM"
    fi
fi

if [ -d "$JAVA_HOME" ]; then
    echo "[+] Found OpenJDK 17 at: $JAVA_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
    java -version
else
    echo "[-] WARNING: OpenJDK 17 path could not be auto-located."
    echo "    Attempting build with current system Java environment..."
fi

# 6. Configure Permissions
echo ""
echo "[*] Modifying Gradle wrapper permissions..."
chmod +x ./gradlew

# 7. Compile Release / Debug APK
echo ""
echo "[*] Commencing compilation of 'Project Organizer' APK..."
echo "--------------------------------------------------------"
./gradlew assembleDebug
GRADLE_EXIT=$?
echo "--------------------------------------------------------"

if [ $GRADLE_EXIT -eq 0 ]; then
    echo ""
    echo "========================================================="
    echo "   [+] BUILD SUCCESSFUL!"
    echo "========================================================="
    echo ""
    APK_RELATIVE_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ -f "$APK_RELATIVE_PATH" ]; then
        echo "[+] APK compiled successfully at:"
        echo "    $APK_RELATIVE_PATH"
        echo ""
        
        # Prompt to copy directly to device Downloads folder
        DOWNLOADS_FOLDER="/sdcard/Download"
        if [ -d "$DOWNLOADS_FOLDER" ]; then
            cp "$APK_RELATIVE_PATH" "$DOWNLOADS_FOLDER/ProjectOrganizer-debug.apk"
            echo "[+] Copied APK to your device Downloads folder as 'ProjectOrganizer-debug.apk'!"
            echo "    You can install it directly from your File Manager now!"
        else
            echo "[*] To copy the APK to your public user storage, run:"
            echo "    1. termux-setup-storage (grant storage permission if requested)"
            echo "    2. cp $APK_RELATIVE_PATH ~/storage/downloads/ProjectOrganizer-debug.apk"
        fi
    fi
else
    echo ""
    echo "[-] ERROR: Compilation failed."
    echo "    Ensure your device has at least 1.5GB of free space and a stable internet connection."
fi
echo "========================================================="
