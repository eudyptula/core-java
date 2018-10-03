#!/bin/sh -e

. /usr/share/debconf/confmodule
. /usr/share/arrowhead/conf/ahconf.sh

if [ "$#" -lt 2 ]; then
    echo "Syntax: ${0} CLOUD_NAME HOST"
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

# This restarts gateway and gatekeeper implicitly
ah_add_neighbor ${AH_OPERATOR} ${CLOUD_NAME} ${CLOUD_HOST} ${CLOUD_64PUB}

echo >&2
echo "Certificate stored in '${AH_CLOUDS_DIR}'" >&2
echo "Password for certificate stores: ${AH_PASS_CERT}" >&2

db_get arrowhead-gatekeeper/address; OWN_HOST="$RET"

OWN_64PUB=$(\
    sudo keytool -exportcert -rfc -keystore "${AH_SYSTEMS_DIR}/gatekeeper/gatekeeper.p12" -storepass ${AH_PASS_CERT} -v -alias "gatekeeper" \
    | openssl x509 -pubkey -noout \
    | sed '1d;$d' \
    | tr -d '\n'\
)
echo >&2
echo "On the new cloud, you should call:" >&2
echo "ah_add_neighbor ${AH_OPERATOR} ${AH_CLOUD_NAME} ${OWN_HOST} ${OWN_64PUB}" >&2
