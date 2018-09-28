#!/bin/sh -e

. /usr/share/debconf/confmodule
. /usr/share/arrowhead/conf/ahconf.sh

ah_cert_export "/etc/arrowhead/cert" master "${1}"
ah_cert_signed "${1}" cloud "${2}.${AH_OPERATOR}.arrowhead.eu" /etc/arrowhead/cert master
chown $(logname) ${1}/master.crt ${1}/cloud.p12

echo
echo "Password for certificate stores: ${AH_PASS_CERT}" >&2