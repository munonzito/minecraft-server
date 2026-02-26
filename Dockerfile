FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /minecraft

# Install curl for downloading
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Server configuration
ENV SERVER_JAR="paper.jar"
ENV XMX="4G"
ENV XMS="4G"
ENV PAPER_VERSION="1.21.4"
ENV PAPER_BUILD="232"

# Copy configuration files
COPY server.properties .
COPY bukkit.yml .
COPY spigot.yml .
COPY eula.txt .

# Create world and plugins directories
RUN mkdir -p world plugins

# Download and setup script
RUN curl -sL "https://api.papermc.io/v2/projects/paper/versions/1.21.11/builds/69/downloads/paper-1.21.11-69.jar" -o paper.jar

# Install rcon-cli
RUN curl -sL "https://github.com/itzg/rcon-cli/releases/download/1.6.7/rcon-cli_1.6.7_linux_arm64.tar.gz" | tar xz -C /usr/local/bin rcon-cli

# Expose Minecraft server port
EXPOSE 25565

# Set default command
CMD ["java", "-Xms2G", "-Xmx2G", "-jar", "paper.jar", "nogui"]
