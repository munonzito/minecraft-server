#!/bin/bash
# Update PaperMC Server Script

JAR_FILE="paper.jar"
VERSION="1.21"
API_URL="https://api.papermc.io/v2/paper/$VERSION"

echo "Fetching latest PaperMC version for $VERSION..."

LATEST_VERSION=$(curl -s "$API_URL" | grep -o '"version":"[^"]*' | head -1 | cut -d'"' -f4)

if [ -z "$LATEST_VERSION" ]; then
    echo "ERROR: Failed to fetch version info"
    exit 1
fi

echo "Latest version: $LATEST_VERSION"

BUILD_API="$API_URL/$LATEST_VERSION"
LATEST_BUILD=$(curl -s "$BUILD_API/builds" | grep -o '"build":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$LATEST_BUILD" ]; then
    echo "ERROR: Failed to fetch build info"
    exit 1
fi

echo "Latest build: $LATEST_BUILD"

DOWNLOAD_URL="$BUILD_API/$LATEST_BUILD/download"

echo "Downloading from: $DOWNLOAD_URL"

if [ -f "$JAR_FILE" ]; then
    echo "Backing up old server jar..."
    cp "$JAR_FILE" "$JAR_FILE.backup"
fi

curl -s "$DOWNLOAD_URL" -o "$JAR_FILE"

echo "Server jar updated to $LATEST_VERSION build $LATEST_BUILD"
echo "Run ./start.sh to start the server"
