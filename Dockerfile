FROM openjdk:21-slim

# Set working directory
WORKDIR /minecraft

# Install curl for downloading
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Server configuration
ENV SERVER_JAR="paper.jar"
ENV XMX="4G"
ENV XMS="4G"
ENV PAPER_VERSION="1.21"

# Copy configuration files
COPY server.properties .
COPY bukkit.yml .
COPY spigot.yml .
COPY paper.yml .
COPY eula.txt .

# Create world and plugins directories
RUN mkdir -p world plugins

# Download and setup script
RUN echo '#!/bin/sh\n\
if [ ! -f "$SERVER_JAR" ]; then\n\
    echo "Downloading PaperMC $PAPER_VERSION..."\n\
    curl -s "https://api.papermc.io/v2/paper/$PAPER_VERSION/latest/download" -o "$SERVER_JAR"\n\
fi\n\
echo "Starting Minecraft PaperMC Server..."\n\
java -Xms$XMS -Xmx$XMX -jar "$SERVER_JAR" nogui' > start.sh && chmod +x start.sh

# Expose Minecraft server port
EXPOSE 25565

# Set default command
CMD ["./start.sh"]
