#!/bin/sh -e

. /usr/share/debconf/confmodule
. /usr/share/arrowhead/conf/ahconf.sh

if [ "$#" -lt 2 ]; then
    exho "Syntax: ${0} CLOUD_NAME HOST"
    exit 1
fi

CLOUD_NAME=${1}
CLOUD_HOST=${2}

CLOUD_STORE="${AH_CLOUDS_DIR}/${CLOUD_NAME}.p12"

if [ ! -f "${AH_CONF_DIR}/master.p12" ]; then
    echo "Keystore for master certificate not found." >&2
    echo "Generating new clouds only works when existing cloud have been installed in detached mode." >&2
    exit 1;
fi

if [ -f "${AH_CLOUDS_DIR}/${CLOUD_NAME}.p12" ]; then
    echo "'${CLOUD_NAME}' already exist, please remove cloud or use a different name." >&2
    exit 1;
fi

echo "Generating certificate for '${CLOUD_NAME}'" >&2
ah_cert_signed "${AH_CLOUDS_DIR}" ${CLOUD_NAME} "${CLOUD_NAME}.${AH_OPERATOR}.arrowhead.eu" ${AH_CONF_DIR} master

CLOUD_64PUB=$(\
    sudo keytool -exportcert -rfc -keystore "${CLOUD_STORE}" -storepass ${AH_PASS_CERT} -v -alias "${CLOUD_NAME}" \
    | openssl x509 -pubkey -noout \
    | sed '1d;$d' \
    | tr -d '\n'\
)

echo "Registering cloud '${CLOUD_NAME}' in database" >&2
mysql -u root arrowhead <<EOF
    LOCK TABLES arrowhead_cloud WRITE, hibernate_sequence WRITE, neighbor_cloud WRITE;
    INSERT INTO arrowhead_cloud
        (id, address, authentication_info, cloud_name, gatekeeper_service_uri, operator, port, is_secure)
        SELECT
            next_val,
            '${CLOUD_HOST}',
            '${CLOUD_64PUB}',
            '${CLOUD_NAME}',
            'gatekeeper',
            '${AH_OPERATOR}',
            '8447',
            'Y'
            FROM hibernate_sequence;
    INSERT INTO neighbor_cloud (cloud_id) SELECT next_val FROM hibernate_sequence;
    UPDATE hibernate_sequence SET next_val = next_val + 1;
    UNLOCK TABLES;
EOF

echo >&2
echo "Certificate stored in '${AH_CLOUDS_DIR}'" >&2
echo "Password for certificate stores: ${AH_PASS_CERT}" >&2
