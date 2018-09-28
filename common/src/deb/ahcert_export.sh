#!/bin/sh -e

. /usr/share/debconf/confmodule
. /usr/share/arrowhead/conf/ahconf.sh

ah_cert_export "/etc/arrowhead/${1}" "${1}" ${2}
chown $(logname) ${2}/${1}.crt
