#!/bin/sh

AH_SYSTEMS_DIR="/etc/arrowhead/systems"

db_get arrowhead-common/mysql_password; AH_PASS_DB=$RET
db_get arrowhead-common/cert_password; AH_PASS_CERT=$RET
db_get arrowhead-common/cloudname; AH_CLOUD_NAME=$RET
db_get arrowhead-common/operator; AH_OPERATOR=$RET
db_get arrowhead-common/company; AH_COMPANY=$RET
db_get arrowhead-common/country; AH_COUNTRY=$RET

ah_cert () {
    dst_path=${1}
    dst_name=${2}
    cn=${3}

    file="${dst_path}/${dst_name}.p12"

    if [ ! -f "${file}" ]; then
        keytool -genkeypair \
            -alias ${dst_name} \
            -keyalg RSA \
            -keysize 2048 \
            -dname "CN=${cn}, OU=${AH_OPERATOR}, O=${AH_COMPANY}, C=${AH_COUNTRY}" \
            -validity 3650 \
            -keypass ${AH_PASS_CERT} \
            -keystore ${file} \
            -storepass ${AH_PASS_CERT} \
            -storetype PKCS12 \
            -ext BasicConstraints:"Subject is a CA\nPath Length Constraint: None"

        chown :arrowhead ${file}
        chmod 640 ${file}
    fi
}

ah_cert_export () {
    src_path=${1}
    dst_name=${2}
    dst_path=${3}

    src_file="${src_path}/${dst_name}.p12"
    dst_file="${dst_path}/${dst_name}.crt"

    if [ ! -f "${dst_file}" ]; then
        keytool -exportcert \
            -rfc \
            -alias ${dst_name} \
            -storepass ${AH_PASS_CERT} \
            -keystore ${src_file} \
        | openssl x509 \
            -out ${dst_file}

        chown :arrowhead ${dst_file}
        chmod 640 ${dst_file}
    fi
}

ah_cert_import () {
    src_path=${1}
    src_name=${2}
    dst_path=${3}
    dst_name=${4}

    src_file="${src_path}/${src_name}.crt"
    dst_file="${dst_path}/${dst_name}.p12"

    keytool -import \
        -trustcacerts \
        -file ${src_file} \
        -alias ${src_name} \
        -keystore ${dst_file} \
        -keypass ${AH_PASS_CERT} \
        -storepass ${AH_PASS_CERT} \
        -storetype PKCS12 \
        -noprompt
}

ah_cert_signed () {
    dst_path=${1}
    dst_name=${2}
    cn=${3}
    src_path=${4}
    src_name=${5}

    src_file="${src_path}/${src_name}.p12"
    dst_file="${dst_path}/${dst_name}.p12"
    
    if [ ! -f "${dst_file}" ]; then
        ah_cert ${dst_path} ${dst_name} ${cn}

        keytool -export \
            -alias ${src_name} \
            -storepass ${AH_PASS_CERT} \
            -keystore ${src_file} \
        | keytool -import \
            -trustcacerts \
            -alias ${src_name} \
            -keystore ${dst_file} \
            -keypass ${AH_PASS_CERT} \
            -storepass ${AH_PASS_CERT} \
            -storetype PKCS12 \
            -noprompt

        keytool -certreq \
            -alias ${dst_name} \
            -keypass ${AH_PASS_CERT} \
            -keystore ${dst_file} \
            -storepass ${AH_PASS_CERT} \
        | keytool -gencert \
            -alias ${src_name} \
            -keypass ${AH_PASS_CERT} \
            -keystore ${src_file} \
            -storepass ${AH_PASS_CERT} \
        | keytool -importcert \
            -alias ${dst_name} \
            -keypass ${AH_PASS_CERT} \
            -keystore ${dst_file} \
            -storepass ${AH_PASS_CERT} \
            -noprompt
    fi
}

ah_cert_trust () {
    dst_path=${1}
    src_path=${2}
    src_name=${3}

    src_file="${src_path}/${src_name}.p12"
    dst_file="${dst_path}/truststore.p12"
    
    if [ ! -f "${dst_file}" ]; then
        keytool -export \
            -alias ${src_name} \
            -storepass ${AH_PASS_CERT} \
            -keystore ${src_file} \
        | keytool -import \
            -trustcacerts \
            -alias ${src_name} \
            -keystore ${dst_file} \
            -keypass ${AH_PASS_CERT} \
            -storepass ${AH_PASS_CERT} \
            -storetype PKCS12 \
            -noprompt

        chown :arrowhead ${dst_file}
        chmod 640 ${dst_file}
    fi
}

ah_db_arrowhead_cloud () {
    mysql -u root < /usr/share/arrowhead/db/create_arrowhead_cloud_tbl_empty.sql
}

ah_db_arrowhead_service () {
    mysql -u root < /usr/share/arrowhead/db/create_arrowhead_service_tbl_empty.sql
}

ah_db_arrowhead_service_interface_list () {
    mysql -u root < /usr/share/arrowhead/db/create_arrowhead_service_interface_list_tbl_empty.sql
}

ah_db_arrowhead_system () {
    mysql -u root < /usr/share/arrowhead/db/create_arrowhead_system_tbl_empty.sql
}

ah_db_hibernate_sequence () {
    mysql -u root < /usr/share/arrowhead/db/create_hibernate_sequence_tbl_empty.sql
}

ah_db_logs () {
    mysql -u root < /usr/share/arrowhead/db/create_logs_tbl_empty.sql
}

ah_db_own_cloud () {
    mysql -u root < /usr/share/arrowhead/db/create_own_cloud_tbl_empty.sql
}

ah_db_user () {
    if [ $(mysql -u root -sse "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'arrowhead')") != 1 ]; then
        mysql -e "CREATE USER arrowhead@localhost IDENTIFIED BY '${AH_PASS_DB}';"
        mysql -e "GRANT ALL PRIVILEGES ON arrowhead.* TO arrowhead@'localhost';"
        mysql -e "FLUSH PRIVILEGES;"
    fi
}

ah_log4j_conf () {
    system_name=${1}

    file="${AH_SYSTEMS_DIR}/${system_name}/log4j.properties"
    
    if [ ! -f "${file}" ]; then
        /bin/cat <<EOF >${file}
# Define the root logger with appender file
log4j.rootLogger=INFO, DB, FILE

# Database related config
# Define the DB appender
log4j.appender.DB=org.apache.log4j.jdbc.JDBCAppender
# Set Database Driver
log4j.appender.DB.driver=com.mysql.jdbc.Driver
# Set Database URL
log4j.appender.DB.URL=jdbc:mysql://127.0.0.1:3306/arrowhead?useSSL=false
# Set database user name and password
log4j.appender.DB.user=arrowhead
log4j.appender.DB.password=${AH_PASS_DB}
# Set the SQL statement to be executed.
log4j.appender.DB.sql=INSERT INTO logs VALUES(DEFAULT,'%d{yyyy-MM-dd HH:mm:ss}','%C','%p','%m')
# Define the layout for file appender
log4j.appender.DB.layout=org.apache.log4j.PatternLayout
# Disable Hibernate verbose logging
log4j.logger.org.hibernate=fatal

# File related config
# Define the file appender
log4j.appender.FILE=org.apache.log4j.FileAppender
# Set the name of the file
log4j.appender.FILE.File=/var/log/arrowhead/${system_name}.log
# Set the immediate flush to true (default)
log4j.appender.FILE.ImmediateFlush=true
# Set the threshold to debug mode
log4j.appender.FILE.Threshold=debug
# Set the append to false, overwrite
log4j.appender.FILE.Append=false
# Define the layout for file appender
log4j.appender.FILE.layout=org.apache.log4j.PatternLayout
log4j.appender.FILE.layout.conversionPattern=%d{yyyy-MM-dd HH:mm:ss}, %C, %p, %m%n
EOF
        chown root:arrowhead ${file}
        chmod 640 ${file}
    fi
}
