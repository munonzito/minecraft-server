# Minecraft Server - PaperMC

A production-ready Minecraft server setup based on PaperMC, configured for easy deployment and management.

## Features

- **PaperMC Server** - Optimized performance with plugin support
- **Docker Support** - Easy deployment to any VM/cloud
- **Pre-configured** - Optimized settings for smooth gameplay
- **Auto-updates** - Script provided for updating to latest version
- **Cross-platform** - Works on macOS, Linux, and via Docker

## Quick Start

### Option 1: Docker (Recommended for VM/Cloud)

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd minecraft_server
   ```

2. **Accept the EULA**
   ```bash
   echo "eula=true" > eula.txt
   ```

3. **Start the server**
   ```bash
   docker-compose up -d
   ```

4. **View logs**
   ```bash
   docker-compose logs -f
   ```

### Option 2: Native (macOS/Linux)

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd minecraft_server
   ```

2. **Make scripts executable**
   ```bash
   chmod +x start.sh update-server.sh
   ```

3. **Start the server**
   ```bash
   ./start.sh
   ```
   The script will guide you through EULA acceptance and download the server jar.

## Configuration

### RAM Settings

Edit the startup files to adjust RAM allocation:

**Docker (docker-compose.yml):**
```yaml
environment:
  - XMX=4G  # Max RAM
  - XMS=4G  # Min RAM
```

**Native (start.sh):**
```bash
XMX="4G"  # Max RAM
XMS="4G"  # Min RAM
```

### Server Settings

Edit `server.properties` to customize:

- **max-players** - Maximum concurrent players (default: 10)
- **gamemode** - survival, creative, adventure, spectator
- **difficulty** - peaceful, easy, normal, hard
- **pvp** - Enable/disable PvP combat
- **motd** - Server description shown in multiplayer menu
- **spawn-protection** - Radius of protected spawn area (default: 0)

### Player Management

**Add Operator (Admin):**
```bash
# In server console or after stopping server
echo "player_name" >> ops.json
```

**Enable Whitelist:**
1. Set `white-list=true` in `server.properties`
2. Add players to `whitelist.json`:
   ```json
   [
     {"uuid": "player-uuid", "name": "player_name"}
   ]
   ```

## Connecting to the Server

### Local Connection
- Server IP: `localhost`
- Port: `25565` (default)

### Remote Connection (For Friends)

1. **Find your public IP:**
   ```bash
   curl ifconfig.me
   ```

2. **Port Forward** (Home Network Only):
   - Forward port **25565** on your router to your server's local IP
   - Use the default protocol (TCP)

3. **Connect:** Share `your-public-ip:25565` with friends

   Example: `123.45.67.89:25565`

## Using on a VM

### Prerequisites for VM
- Ubuntu/Debian Linux (recommended)
- Docker & Docker Compose installed
- At least 4GB RAM available
- Port 25565 open in firewall

### VM Setup Commands

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose -y

# Clone and start
git clone <your-repo-url>
cd minecraft_server
echo "eula=true" > eula.txt
docker-compose up -d
```

### VM Firewall Rules

```bash
# Ubuntu/Debian
sudo ufw allow 25565/tcp

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=25565/tcp
sudo firewall-cmd --reload
```

## Maintenance

### Update Server

**Native:**
```bash
./update-server.sh
```

**Docker:**
```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Backup World

```bash
tar -czf world-backup-$(date +%Y%m%d).tar.gz world/ world_nether/ world_the_end/
```

### View Logs

**Docker:**
```bash
docker-compose logs -f
```

**Native:**
Server logs are in the `logs/` directory.

## Adding Plugins

PaperMC supports Spigot/Bukkit plugins. To add plugins:

1. Download plugins from [SpigotMC](https://www.spigotmc.org/resources/)
2. Place `.jar` files in the `plugins/` directory
3. Restart the server

**Recommended plugins:**
- EssentialsX - Core commands and features
- WorldGuard - Region protection
- CoreProtect - Block logging and rollback
- LuckPerms - Permissions management
- Vault - Economy/chat permissions API

## Troubleshooting

### Server won't start
- Check `eula.txt` contains `eula=true`
- Verify Java 17+ is installed (native mode)
- Check logs in `logs/latest.log`

### Friends can't connect
- Verify port 25565 is forwarded (home network)
- Check firewall allows port 25565
- Ensure using public IP, not local IP

### Performance issues
- Increase RAM allocation in XMX setting
- Reduce `max-players` in server.properties
- Reduce `view-distance` and `simulation-distance`

## Requirements

### Minimal (5 players)
- RAM: 4GB
- CPU: 2 cores
- Storage: 10GB

### Recommended (10-20 players)
- RAM: 6-8GB
- CPU: 4 cores
- Storage: 20GB

### With Plugins/Mods
- RAM: 8GB+
- CPU: 4+ cores
- Storage: 30GB+

## License

This server setup is provided for personal use. PaperMC is licensed under GPL-3.0.

## Links

- [PaperMC Documentation](https://docs.papermc.io/)
- [Minecraft Server Download](https://www.minecraft.net/en-us/download/server/)
- [SpigotMC Resources](https://www.spigotmc.org/resources/)

## Contributing

Feel free to submit issues and enhancement requests!
