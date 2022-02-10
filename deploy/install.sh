#!/bin/bash
# Installs or updates multizone-prometheus

sudo systemctl disable multizone-prometheus.service
sudo systemctl stop multizone-prometheus.service

sudo mkdir -p /multizone-prometheus/
sudo rm -f /multizone-prometheus/multizone-prometheus.jar
sudo cp -rf ./multizone-prometheus-v0.2.0.jar /multizone-prometheus/multizone-prometheus.jar

# Edit service file to allow the user to choose arguments.
sudo cp -rf ./multizone-prometheus-original.service ./multizone-prometheus.service
echo "Enter the modbus address of the multizone device:"
read ADDRESS

echo "Enter the baud rate of the multizone device:"
read BAUD

echo "Enter the path to the serial port you would like to use: (Often this is /dev/serial1)"
read SERIAL_PORT

echo "Enter the name of the customer. The name should include no spaces and only letters:"
read CUSTOMER_NAME

sudo sed -i 's/{ADDRESS}/$ADDRESS/g;s/{BAUD}/$BAUD/g;s/{SERIAL_PORT}/$SERIAL_PORT/g;s/{CUSTOMER_NAME}/$CUSTOMER_NAME/g' ./multizone-prometheus.service

sudo cp -rf ./multizone-prometheus.service /etc/systemd/system/multizone-prometheus.service

sudo systemctl daemon-reload
sudo systemctl enable multizone-promethus.service
sudo systemctl start multizone-prometheus.service
echo "Installed multizone-prometheus in /multizone-prometheus/"
