#!/bin/sh -e

. /usr/share/debconf/confmodule
. /usr/share/arrowhead/conf/ahconf.sh

ah_cert_signed "${1}" ${2} "${2}.${AH_CLOUD_NAME}.${AH_OPERATOR}.arrowhead.eu" /etc/arrowhead/cert cloud
chown $(logname) ${1}/${2}.p12
