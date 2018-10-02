# Arrowhead Framework G4.0

## Installing Arrowhead on a Ubuntu Server using Debian Packages

### 1. Install Ubuntu Server (18.04)

Do a normal installation of Ubuntu server, remember to update:

`sudo apt update && sudo apt dist-upgrade`

### 2. Install MySQL

Install:

`sudo apt install mysql-server`

Check if running:

`sudo netstat -tap | grep mysql`

### 3. Install Java

To install Oracle Java, add the repository first:

`sudo add-apt-repository ppa:linuxuprising/java`

`sudo apt update`

`sudo apt install oracle-java10-installer`

Check Java version:

`java -version`

### 4a. Download an Arrowhead Debian Packages release

Check the GitHub releases site <https://github.com/arrowhead-f/core-java/releases> for the latest release and download
it: 

`wget -c https://github.com/arrowhead-f/core-java/releases/download/4.0-debian/debian_packages.zip`

Unpack it:

```bash
unzip debian_packages.zip
cd debian_packages/
```

### 4b. Build Arrowhead Debian Packages

To build the Debian packages yourself, start by cloning the repository:

`git clone https://github.com/arrowhead-f/core-java.git -b feature/debian`

Build them with:

`mvn package`

Copy to server:

```bash
scp common/target/arrowhead-common_4.0_all.deb \
    authorization/target/arrowhead-authorization_4.0_all.deb \
    serviceregistry_sql/target/arrowhead-serviceregistry-sql_4.0_all.deb \
    gateway/target/arrowhead-gateway_4.0_all.deb \
    eventhandler/target/arrowhead-eventhandler_4.0_all.deb \
    gatekeeper/target/arrowhead-gatekeeper_4.0_all.deb \
    orchestrator/target/arrowhead-orchestrator_4.0_all.deb \
    X.X.X.X:~/
```

### 5. Install Arrowhead Core Debian Packages

`sudo dpkg -i arrowhead-*.deb`

Currently they're not added to the default runlevel, so they will not restart on reboot unless added manually.

### 6. Hints

#### Add a new application system

You can use the script `ah_add_system` to generate certificate, a configuration template, and add the necessary
database entries for a new application system: 

```sudo ah_add_system SYSTEM_NAME HOST [SERVICE]```

If there is no service parameter only a consumer system will be generated, specify a service to generate a full provider
system. Examples:

```sudo ah_add_system client1 127.0.0.1```

```sudo ah_add_system SecureTemperatureSensor 127.0.0.1 IndoorTemperature```

#### Other hints

Log files (log4j) are available in: `/var/log/arrowhead/*`

Output from systems are available with: `journalctl -u arrowhead-*.service`

Restart services: `sudo systemctl restart arrowhead-\*.service`

Configuration and certificates are found under: `/etc/arrowhead`

Generated passwords can be found in `/var/cache/debconf/passwords.dat`

Mysql database: `sudo mysql -u root`, to see the Arrowhead tables:

```SQL
use arrowhead;
show tables;
```

`apt purge` can be used to remove configuration files, database, log files, etc. Use `sudo apt purge arrowhead-\*` to
remove everything arrowhead related.

For the provider and consumer example in the client skeletons, the script `sudo ah_quickstart_gen` can be used to
generate the necessary certificates and database entries. It will also output the certificate/keystore password. Note,
this script should only be used for test clouds on a clean installation.

For clouds installed in detached mode, a certificate for a second cloud can be generated with
`sudo ahcert_cloud ./ CLOUD_NAME`, e.g. `sudo ahcert_cloud ./ testcloud2`.

To switch to insecure mode of all core services, remove `-tls`in the service files and restart them, e.g.:

`cd /etc/systemd/system/; sudo find -name arrowhead-\*.service -exec sed -i 's|^\(ExecStart=.*\) -tls$|\1|' {} \; && sudo systemctl daemon-reload && sudo systemctl restart arrowhead*.service`

To switch back to secure mode add `-tls` again, e.g.:

`cd /etc/systemd/system/; sudo find -name arrowhead-\*.service -exec sed -i 's|^\(ExecStart=.*\)$|\1 -tls|' {} \; && sudo systemctl daemon-reload && sudo systemctl restart arrowhead*.service`

