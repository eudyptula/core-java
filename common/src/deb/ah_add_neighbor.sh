#!/bin/sh -e

. /usr/share/debconf/confmodule
. /usr/share/arrowhead/conf/ahconf.sh

if [ "$#" -lt 4 ]; then
    echo "Syntax: ${0} OPERATOR CLOUD_NAME HOST AUTH_INFO"
    exit 1
fi

AH_OPERATOR=${1}
CLOUD_NAME=${2}
CLOUD_HOST=${3}
CLOUD_64PUB=${4}

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

echo "Restarting gateway and gatekeeper" >&2
systemctl restart arrowhead-gateway.service arrowhead-gatekeeper.service
