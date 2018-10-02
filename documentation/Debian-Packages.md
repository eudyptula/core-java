# Guide to Debian Packages

The following changes are required to add Debian/Ubuntu package generation to a Arrowhead core system.

## File: pom.xml

- Set `artifactId` to `arrowhead-SYSTEMNAME`, for example: `arrowhead-authorization`. If multiple versions of the same
core system exist add a postfix, like `arrowhead-serviceregistry-sql`.

- Set `classpathPrefix` to `../lib/`.

- Under `<plugins>` add:

```xml
<plugin>
<artifactId>jdeb</artifactId>
<groupId>org.vafer</groupId>
<version>1.5</version>
<executions>
  <execution>
    <phase>package</phase>
    <goals>
      <goal>jdeb</goal>
    </goals>
    <configuration>
      <!--<snapshotExpand>true</snapshotExpand>-->
      <!-- expand "SNAPSHOT" to what is in the "USER" env variable -->
      <!--<snapshotEnv>USER</snapshotEnv>-->
      <verbose>true</verbose>
      <controlDir>${basedir}/src/deb/control</controlDir>
      <dataSet>

        <data>
          <type>file</type>
          <src>${project.build.directory}/${project.build.finalName}.jar</src>
          <mapper>
            <type>perm</type>
            <prefix>/usr/share/arrowhead/systems</prefix>
            <user>arrowhead</user>
            <filemode>755</filemode>
          </mapper>
        </data>

      </dataSet>
    </configuration>
  </execution>
</executions>
</plugin>
```

- Additional data sections should be added for files to be installed with the core systems. Examples:

The service definition file, used to start and stop the daemon (should most likely always be there):    
```xml
<data>
  <type>file</type>
  <src>${project.basedir}/src/deb/arrowhead-authorization.service</src>
  <mapper>
    <type>perm</type>
    <prefix>/etc/systemd/system</prefix>
    <filemode>664</filemode>
  </mapper>
</data>
```

Sql files to create empty database tables:
```xml
<data>
  <type>file</type>
  <src>${project.basedir}/src/deb/create_authorization_db_empty.sql</src>
  <dst>/usr/share/arrowhead/db/create_authorization_db_empty.sql</dst>
</data>
```

Additional libraries:
```xml
<data>
  <type>file</type>
  <src>${project.build.directory}/lib/bcprov-jdk15on-1.59.jar</src>
  <mapper>
    <type>perm</type>
    <prefix>/usr/share/arrowhead/lib</prefix>
  </mapper>
</data>
```

## File: src/deb/SYSTEMNAME.service 

Service definition file, used to start/stop the daemon. Also contains a list of dependencies, which should be started
before this system.

```
[Unit]
Description=arrowhead-gatekeeper
After=network.target mysql.target arrowhead-serviceregistry-sql.service arrowhead-authorization.service arrowhead-gateway.service
Requires=arrowhead-serviceregistry-sql.service arrowhead-authorization.service arrowhead-gateway.service

[Service]
WorkingDirectory=/etc/arrowhead/systems/gatekeeper
ExecStart=/usr/bin/java --add-modules java.xml.bind -jar /usr/share/arrowhead/systems/arrowhead-gatekeeper-4.0.jar -d -daemon -tls
ExecStartPost=/bin/bash -c 'sleep 2; while ! grep -m1 "Startup completed." /var/log/arrowhead/gatekeeper.log; do sleep 2; done'
TimeoutStopSec=5
Type=simple
User=arrowhead
Group=arrowhead

[Install]
WantedBy=default.target
```

## File: src/deb/create_SYSTEMNAME_db_empty.sql 

Script to create empty MySQL tables if such is required - can be omitted if not.

Note that 'DROP [...]' statements should be removed and 'IF NOT EXISTS' added to all 'CREATE [...]' statements.

```mysql
CREATE DATABASE  IF NOT EXISTS `arrowhead` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `arrowhead`;
-- MySQL dump 10.13  Distrib 5.7.17, for Win64 (x86_64)
--
-- Host: localhost    Database: service_registry
-- ------------------------------------------------------
-- Server version	5.7.21-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `service_registry`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE  IF NOT EXISTS `service_registry` (
  `id` int(11) NOT NULL,
  `end_of_validity` datetime DEFAULT NULL,
  `metadata` varchar(255) DEFAULT NULL,
  `port` int(11) DEFAULT NULL,
  `service_uri` varchar(255) DEFAULT NULL,
  `udp` char(1) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  `arrowhead_service_id` int(11) DEFAULT NULL,
  `provider_system_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3q3tqiu7f92u946p33plj5fxq` (`arrowhead_service_id`,`provider_system_id`),
  KEY `FK4lc944mp4x24pr09wuxbb08ky` (`provider_system_id`),
  CONSTRAINT `FK4lc944mp4x24pr09wuxbb08ky` FOREIGN KEY (`provider_system_id`) REFERENCES `arrowhead_system` (`id`),
  CONSTRAINT `FKr0x7pvbi16w5b6ao6q43t606p` FOREIGN KEY (`arrowhead_service_id`) REFERENCES `arrowhead_service` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-05-24 13:45:03
```

## File: src/deb/control 

Description file for the package manager. Contains an description of the system, the maintainer, and which packages
should be installed before this one. In general, all should depend on arrowhead-common.

```
Package: [[name]]
Version: [[version]]
Section: contrib/java
Priority: optional
Architecture: all
Maintainer: Thomas Pedersen <tp@cs.aau.dk>
Homepage: http://www.arrowhead.eu
Description: Arrowhead Gatekeeper System
Distribution: development
Depends: default-jre-headless, virtual-mysql-server, arrowhead-common, arrowhead-serviceregistry-sql, arrowhead-authorization, arrowhead-gateway
```

## File: src/deb/postinst

The general structure of the post-installation script:

```bash
#!/bin/sh
# postinst script for arrowhead-authorization
#
# see: dh_installdeb(1)

set -e

. /usr/share/debconf/confmodule

SERVICE_NAME="authorization"
PKG_NAME="arrowhead-authorization"

# summary of how this script can be called:
#        * <postinst> `configure' <most-recently-configured-version>
#        * <old-postinst> `abort-upgrade' <new version>
#        * <conflictor's-postinst> `abort-remove' `in-favour' <package>
#          <new-version>
#        * <postinst> `abort-remove'
#        * <deconfigured's-postinst> `abort-deconfigure' `in-favour'
#          <failed-install-package> <version> `removing'
#          <conflicting-package> <version>
# for details, see https://www.debian.org/doc/debian-policy/ or
# the debian-policy package

case "$1" in
    configure)
        . /usr/share/arrowhead/conf/ahconf.sh
    ;;

    abort-upgrade|abort-remove|abort-deconfigure)
    ;;

    *)
        echo "postinst called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

# dh_installdeb will replace this with shell code automatically
# generated by other debhelper scripts.

#DEBHELPER#

exit 0
```

The following sections should be added under configure:

- Creation of MySQL databases. Functions in ahconf.sh from arrowhead-common package can be used to generate default
  tables. Default tables (like the 'system' and 'service') should be created by all systems that need them - if they
  already exist, nothing will be done.
  
```bash
echo "Configuring MySQL database..." >&2
ah_db_logs
ah_db_arrowhead_cloud
ah_db_arrowhead_service
ah_db_arrowhead_service_interface_list
ah_db_arrowhead_system
ah_db_hibernate_sequence
mysql -u root < /usr/share/arrowhead/db/create_authorization_db_empty.sql
ah_db_own_cloud
```
  
- If database is used, it should also create DB user by calling ah_db_user function from ahconf.sh

```bash
ah_db_user
```

- Create a directory for configuration under `/etc/arrowhead/systems`

```bash
if [ ! -d "${AH_SYSTEMS_DIR}/${SERVICE_NAME}" ]; then
    mkdir -p ${AH_SYSTEMS_DIR}/${SERVICE_NAME}
fi
```

- Generate a signed system certificate in this dir (use functions in ahconf.sh again)

```bash
if [ ! -f "${AH_SYSTEMS_DIR}/${SERVICE_NAME}/${SERVICE_NAME}.p12" ]; then
    ah_cert_signed "${AH_SYSTEMS_DIR}/${SERVICE_NAME}" ${SERVICE_NAME} "${SERVICE_NAME}.${AH_CLOUD_NAME}.${AH_OPERATOR}.arrowhead.eu" /etc/arrowhead/cert cloud
    ah_cert_import "/etc/arrowhead/cert" "master" "${AH_SYSTEMS_DIR}/${SERVICE_NAME}" ${SERVICE_NAME}
fi
```

- Insert data into MySQL database if required (Gatekeeper currently does this)

```bash
if [ $(mysql -u root arrowhead -sse "SELECT COUNT(*) FROM arrowhead_cloud;") -eq 0 ]; then
    pubkey64=$(\
        keytool -export \
            -alias ${SERVICE_NAME} \
            -storepass ${AH_PASS_CERT}\
            -keystore ${AH_SYSTEMS_DIR}/${SERVICE_NAME}/${SERVICE_NAME}.p12 \
        | openssl x509 \
            -inform der \
            -pubkey \
            -noout \
        | tail -n +2 | head -n -1 | sed ':a;N;$!ba;s/\n//g')

    mysql -u root arrowhead <<EOF
LOCK TABLES arrowhead_cloud WRITE;
INSERT INTO arrowhead_cloud VALUES (1,'localhost','${pubkey64}','${AH_CLOUD_NAME}','${SERVICE_NAME}','${AH_OPERATOR}',8447,'Y');
UNLOCK TABLES;
EOF
fi

if [ $(mysql -u root arrowhead -sse "SELECT COUNT(*) FROM own_cloud;") -eq 0 ]; then
    mysql -u root arrowhead <<EOF
LOCK TABLES own_cloud WRITE;
INSERT INTO own_cloud VALUES (1);
UNLOCK TABLES;
EOF
fi
```

- Create 'app.properties' file in this dir

```bash
if [ ! -f "${AH_SYSTEMS_DIR}/${SERVICE_NAME}/app.properties" ]; then
    /bin/cat <<EOF >${AH_SYSTEMS_DIR}/${SERVICE_NAME}/app.properties
# Database parameters
db_user=arrowhead
db_password=${AH_PASS_DB}
db_address=jdbc:mysql://127.0.0.1:3306/arrowhead?useSSL=false

##########################################
# MANDATORY PARAMETERS ONLY IN SECURE MODE
##########################################

# Certificate related paths and passwords
keystore=${AH_SYSTEMS_DIR}/${SERVICE_NAME}/${SERVICE_NAME}.p12
keystorepass=${AH_PASS_CERT}
keypass=${AH_PASS_CERT}
truststore=/etc/arrowhead/cert/truststore.p12
truststorepass=${AH_PASS_CERT}

################################################
# NON-MANDATORY PARAMETERS (defaults are showed)
################################################

# Webserver parameters
address=0.0.0.0
insecure_port=8444
secure_port=8445

# Service Registry
sr_address=0.0.0.0
sr_insecure_port=8442
sr_secure_port=8443

# Other
enable_auth_for_cloud=false

EOF
    chown root:arrowhead ${AH_SYSTEMS_DIR}/${SERVICE_NAME}/app.properties
    chmod 640 ${AH_SYSTEMS_DIR}/${SERVICE_NAME}/app.properties
fi
```

- Create log4j conf file in this dir (use 'ah_log4j_conf' function from ahconf.sh)

```bash
ah_log4j_conf ${SERVICE_NAME}
```

- Reload/restart the daemon

```bash
echo "Restarting ${PKG_NAME}..." >&2
systemctl daemon-reload
systemctl restart ${PKG_NAME}.service
```

The general thought with the postinst script was that it should support installing the core systems on different
servers. This is not support by dependencies though, so will not work in practice.

## File: src/deb/postrm 

Should delete files and directories created by postinst.

```bash
#!/bin/sh
# postrm script for arrowhead-gatekeeper
#
# see: dh_installdeb(1)

set -e

SERVICE_NAME="gatekeeper"
PKG_NAME="arrowhead-gatekeeper"

# summary of how this script can be called:
#        * <postrm> `remove'
#        * <postrm> `purge'
#        * <old-postrm> `upgrade' <new-version>
#        * <new-postrm> `failed-upgrade' <old-version>
#        * <new-postrm> `abort-install'
#        * <new-postrm> `abort-install' <old-version>
#        * <new-postrm> `abort-upgrade' <old-version>
#        * <disappearer's-postrm> `disappear' <overwriter>
#          <overwriter-version>
# for details, see https://www.debian.org/doc/debian-policy/ or
# the debian-policy package


case "$1" in
    purge)
        AH_SYSTEMS_DIR="/etc/arrowhead/systems"
        
        rm -f \
            /var/log/arrowhead/${SERVICE_NAME}.log \
            ${AH_SYSTEMS_DIR}/${SERVICE_NAME}/app.properties \
            ${AH_SYSTEMS_DIR}/${SERVICE_NAME}/log4j.properties \
            ${AH_SYSTEMS_DIR}/${SERVICE_NAME}/${SERVICE_NAME}.p12
        rmdir ${AH_SYSTEMS_DIR}/${SERVICE_NAME} 2>/dev/null || true
        rmdir /var/log/arrowhead 2>/dev/null || true
        echo PURGE | debconf-communicate ${PKG_NAME}
    ;;
    remove|upgrade|failed-upgrade|abort-install|abort-upgrade|disappear)
    ;;

    *)
        echo "postrm called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

# dh_installdeb will replace this with shell code automatically
# generated by other debhelper scripts.

#DEBHELPER#

exit 0

```

## File: src/deb/preinst 

Currently does nothing.

```bash
#!/bin/sh
# preinst script for arrowhead-authorization
#
# see: dh_installdeb(1)

set -e

# summary of how this script can be called:
#        * <new-preinst> `install'
#        * <new-preinst> `install' <old-version>
#        * <new-preinst> `upgrade' <old-version>
#        * <old-preinst> `abort-upgrade' <new-version>
# for details, see https://www.debian.org/doc/debian-policy/ or
# the debian-policy package


case "$1" in
    install|upgrade)
    ;;

    abort-upgrade)
    ;;

    *)
        echo "preinst called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

# dh_installdeb will replace this with shell code automatically
# generated by other debhelper scripts.

#DEBHELPER#

exit 0

```

## File: src/deb/prerm 

Currently only stops the daemon.

```bash
#!/bin/sh
# prerm script for arrowhead-authorization
#
# see: dh_installdeb(1)

set -e

PKG_NAME="arrowhead-authorization"

# summary of how this script can be called:
#        * <prerm> `remove'
#        * <old-prerm> `upgrade' <new-version>
#        * <new-prerm> `failed-upgrade' <old-version>
#        * <conflictor's-prerm> `remove' `in-favour' <package> <new-version>
#        * <deconfigured's-prerm> `deconfigure' `in-favour'
#          <package-being-installed> <version> `removing'
#          <conflicting-package> <version>
# for details, see https://www.debian.org/doc/debian-policy/ or
# the debian-policy package


case "$1" in
    remove)
        systemctl stop ${PKG_NAME}.service
    ;;

    upgrade|deconfigure)
    ;;

    failed-upgrade)
    ;;

    *)
        echo "prerm called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

# dh_installdeb will replace this with shell code automatically
# generated by other debhelper scripts.

#DEBHELPER#

exit 0

```

## File: Java source files 

Mostly, no changes are required in these, as `ArrowheadMain.java` covers this. But the gatekeeper does not inherit from
this class, thus the following changes was necessary:

1. Property files like "log4j.properties" should be loaded from the current directory if they are present here, else
from 'config/' as before.

```java
static {
  if (new File("log4j.properties").exists()) {
    PropertyConfigurator.configure("log4j.properties");
  } else {
    PropertyConfigurator.configure("config" + File.separator + "log4j.properties");
  }
  props = Utility.getProp("app.properties");
  USE_GATEWAY = props.getBooleanProperty("use_gateway", false);
  TIMEOUT = props.getIntProperty("timeout", 30000);
}
```

2. When it is fully started it should execute "log.info("Startup completed.");" to notify the .service script.

```java
// For Systemd scripts to detect when we're done (do not change)
log.info("Startup completed.");
```