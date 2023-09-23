#!/bin/bash

if [ "$EUID" -ne 0 ]
  then echo "Please run as root"
  exit
fi

DEPLOYMENT_DIR=/home/properties/property-api/deployment

sudo ln -s $DEPLOYMENT_DIR/property-api.service /etc/systemd/system

sudo touch /etc/systemd/system/property-api.service.d/override.conf

if [ -d '/etc/systemd/system/property-api.service.d' ]; then
	echo 'Env dir already created'
else
	echo "Creating env file at '/etc/systemd/system/property-api.service.d/override.conf'. Enter environment variables here."
	sudo mkdir /etc/systemd/system/property-api.service.d
	sudo echo "[Service]" >> /etc/systemd/system/property-api.service.d/override.conf;
	sudo echo "Environment=\"PROPERTY_API_DB_HOST=<host>\"" >> /etc/systemd/system/property-api.service.d/override.conf;
	sudo echo "Environment=\"PROPERTY_API_DB_PORT=<port>\"" >> /etc/systemd/system/property-api.service.d/override.conf;
	sudo echo "Environment=\"PROPERTY_API_DB_PASSWORD=<password>\"" >> /etc/systemd/system/property-api.service.d/override.conf;
	sudo echo "Environment=\"PROPERTY_API_APIKEY=<apikey>\"" >> /etc/systemd/system/property-api.service.d/override.conf;
fi
