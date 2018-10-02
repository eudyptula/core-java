#!/bin/bash -e

. /usr/share/debconf/confmodule
. /usr/share/arrowhead/conf/ahconf.sh

if [ "$#" -lt 2 ]; then
    exho "Syntax: ${0} SYSTEM_NAME HOST [SERVICE]"
    exit 1
fi

SYSTEM_NAME=${1}; shift
SYSTEM_HOST=${1}; shift
SERVICE=${@}

SYSTEM_DIR="${AH_SYSTEMS_DIR}/${SYSTEM_NAME}"
SYSTEM_STORE="${AH_SYSTEMS_DIR}/${SYSTEM_NAME}/${SYSTEM_NAME}.p12"
ARROWHEAD_IP=($(hostname -I))
SYSTEM_CN="${SYSTEM_NAME}.${AH_CLOUD_NAME}.${AH_OPERATOR}.arrowhead.eu"

if [ -d "${SYSTEM_DIR}" ]; then
    echo "'${SYSTEM_DIR}' already exist, please remove system or use a different name." >&2
    exit 1;
fi

mkdir -p "${SYSTEM_DIR}"

echo "Generating certificate '${SYSTEM_CN}'" >&2
ah_cert_signed \
    "${SYSTEM_DIR}" \
    "${SYSTEM_NAME}" \
    "${SYSTEM_CN}" \
    /etc/arrowhead/cert \
    cloud

if [ ! -z "${SERVICE}" ]; then
    ahcert_export \
        authorization \
        "${SYSTEM_DIR}"
fi

SYSTEM_64PUB=$(\
    sudo keytool -exportcert -rfc -keystore "${SYSTEM_STORE}" -storepass ${AH_PASS_CERT} -v -alias "${SYSTEM_NAME}" \
    | openssl x509 -pubkey -noout \
    | sed '1d;$d' \
    | tr -d '\n'\
)

echo "Registering system '${SYSTEM_NAME}' in database" >&2
db_cmd="
    LOCK TABLES arrowhead_system WRITE, hibernate_sequence WRITE, arrowhead_service WRITE;
    INSERT INTO arrowhead_system (id, address, authentication_info, system_name)
        SELECT next_val, '${SYSTEM_HOST}', '${SYSTEM_64PUB}' , '${SYSTEM_NAME}' FROM hibernate_sequence;
    UPDATE hibernate_sequence SET next_val = next_val + 1;
"

if [ ! -z "${SERVICE}" ]; then
    if [ $(mysql -u root arrowhead -sse "SELECT EXISTS(SELECT 1 FROM arrowhead_service WHERE service_definition = '${SERVICE}')") != 1 ]; then
        echo "Registering service '${SERVICE}' in database" >&2
        db_cmd="${db_cmd}
            INSERT INTO arrowhead_service (id, service_definition)
                SELECT next_val, '${SERVICE}' FROM hibernate_sequence;
            UPDATE hibernate_sequence SET next_val = next_val + 1;
        "
    fi
fi

db_cmd="${db_cmd} UNLOCK TABLES;"

mysql -u root arrowhead -e "${db_cmd}"

if [ -z "${SERVICE}" ]; then
    echo "Generating consumer-only properties file" >&2
    echo "" > "${SYSTEM_DIR}/app.properties"
else
    echo "Generating full provider properties file" >&2
    echo "
######################
# MANDATORY PARAMETERS
######################

# Parameters of the offered service which will be registered in the SR
service_name=${SERVICE}
# Resource path where the service will be offered (address:port/service_uri)
service_uri=${SERVICE}
# Interfaces the service is offered through (comma separated list)
interfaces=JSON, XML
# Metadata key-value pairs (key1-value1, key2-value2)
metadata=unit-celsius

# Provider system name to be registered into the SR
insecure_system_name=${SYSTEM_NAME}
secure_system_name=${SYSTEM_NAME}
fi
" > "${SYSTEM_DIR}/app.properties"
fi

echo "
################################################
# NON-MANDATORY PARAMETERS (defaults are showed)
################################################

# Webserver parameters
address=0.0.0.0
insecure_port=8460
secure_port=8461

# Service Registry
sr_address=${ARROWHEAD_IP}
sr_insecure_port=8442
sr_secure_port=8443

# Orchestrator
orch_address=${ARROWHEAD_IP}
orch_insecure_port=8440
orch_secure_port=8441

#####################################################################
# MANDATORY PARAMETERS ONLY IN SECURE MODE (invoked w/ -tls argument)
#####################################################################

# Certificate related paths and passwords
keystore=${SYSTEM_STORE}
keystorepass=${AH_PASS_CERT}
keypass=${AH_PASS_CERT}
truststore=${SYSTEM_STORE}
truststorepass=${AH_PASS_CERT}
authorization_cert=${SYSTEM_DIR}/authorization.crt
" >> "${SYSTEM_DIR}/app.properties"

chown root:arrowhead "${SYSTEM_DIR}/app.properties"
chmod 640 "${SYSTEM_DIR}/app.properties"

echo >&2
echo "System files stored in '${SYSTEM_DIR}'" >&2
echo "Please verify that 'app.properties' is correct" >&2
