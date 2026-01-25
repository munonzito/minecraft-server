#!/bin/bash
# Minecraft Server Startup Script (PaperMC)

SERVER_JAR="paper.jar"
JAR_URL="https://api.papermc.io/v2/paper/1.21/latest/download"

# RAM settings (adjust based on your system)
XMX="4G"
XMS="4G"

echo "=================================="
echo "   Minecraft PaperMC Server"
echo "=================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed!"
    echo "Please install Java 17 or higher"
    exit 1
fi

echo "Java version:"
java -version
echo ""

# Download server jar if not present
if [ ! -f "$SERVER_JAR" ]; then
    echo "Downloading PaperMC server jar..."
    curl -s "$JAR_URL" -o "$SERVER_JAR"
    if [ $? -eq 0 ]; then
        echo "Downloaded $SERVER_JAR successfully!"
    else
        echo "ERROR: Failed to download server jar"
        exit 1
    fi
fi

# Check if EULA is accepted
if [ ! -f "eula.txt" ] || ! grep -q "eula=true" eula.txt; then
    echo ""
    echo "=================================="
    echo "       EULA AGREEMENT"
    echo "=================================="
    echo ""
    echo "You must accept the Minecraft EULA to run the server."
    echo "Please read the EULA at: https://aka.ms/MinecraftEULA"
    echo ""
    read -p "Do you accept the EULA? (y/n): " answer
    if [ "$answer" = "y" ]; then
        echo "eula=true" > eula.txt
        echo "EULA accepted!"
    else
        echo "EULA not accepted. Server will not start."
        exit 1
    fi
fi

# Start the server
echo ""
echo "Starting Minecraft PaperMC Server..."
echo "RAM: -Xms[$XMS] -Xmx[$XMX]"
echo ""

java -Xms"$XMS" -Xmx"$XMX" -jar "$SERVER_JAR" nogui
