package com.rbrubaker.multizone_prometheus;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import com.ghgande.j2mod.modbus.ModbusException;
import com.rbrubaker.multizone4j.CurrentZoneStatus;
import com.rbrubaker.multizone4j.MultiZoneDevice;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * @author Justin Brubaker
 *
 */
public class MultiZonePrometheus {
	
	private static boolean running = true;
	private static HTTPServer server;
	public static final String VERSION = "Version 0.2.0";
	private static Instant lastUpdate;
	
	private static ArrayList<Gauge> ppmGauges = new ArrayList<Gauge>();
	private static ArrayList<Gauge> alarmStatusGauges = new ArrayList<Gauge>();
	
	private static MultiZoneDevice multizone;
	
	public static void main(String[] args) {
		out("MultiZone-Prometheus " + VERSION);
		out("MultiZone-Prometheus is Copyright 2022 Rufus Brubaker Refrigeration. Software created by Justin Brubaker.");
		out("This Source Code Form is subject to the terms of the Mozilla Public\r\n"
				+ "  License, v. 2.0. If a copy of the MPL was not distributed with this\r\n"
				+ "  file, You can obtain one at http://mozilla.org/MPL/2.0/.");

		Runtime.getRuntime().addShutdownHook(new Thread() {
	        public void run() {
	            try {	            	
	                Thread.sleep(200);
	                if (server != null) {
	                	server.close();
	                }
	                
	                if (multizone != null) {
	                	multizone.disconnect();
	                }
	                
	                System.out.println("\nReceived shutdown request from system. (CTRL-C)");
	                
	                running = false;	                
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	        }
	    });
		
		String modbusAddressString = null;
		String baudRateString = null;
		String serialDeviceName = null;
		String customerName = null;
		
		int modbusAddress = -1;
		int baudRate = -1;
		
		if (args.length == 4) {
			modbusAddressString = args[0];
			baudRateString = args[1];
			serialDeviceName = args[2];
			customerName = args[3];
			customerName = customerName.toLowerCase().replace("-", "_");
			
			try {
				modbusAddress = Integer.parseInt(modbusAddressString);
				baudRate = Integer.parseInt(baudRateString);
			} catch (NumberFormatException e) {
				out("Please provide correct arguments. Couldn't read modbus address or baud rate. java -jar multizone-prometheus.jar <modbusAddress> <baudRate> <serialDeviceName> <customerName>");
				System.exit(0);
			}					
		} else {
			out("Please provide arguments. java -jar multizone-prometheus.jar <modbusAddress> <baudRate> <serialDeviceName> <customerName>");
			System.exit(0);
		}
		
		try {
			server = new HTTPServer(9999);
		} catch (IOException e) {			
			e.printStackTrace();
		}
		
		// Gauge PPM: multizone_%customerName%_zone1_ppm
		// Gauge AlarmStatus: multizone_%customerName%_zone1_alarmstatus		
		
		// Register Gauges		
		for (int i = 1; i <= 16; i++) {
			ppmGauges.add(Gauge.build().name("multizone_" + customerName + "_zone" + i + "_ppm")
					.help("The current PPM reading for zone " + i + ".").register());
			alarmStatusGauges.add(Gauge.build().name("multizone_" + customerName + "_zone" + i + "_alarmstatus")
					.help("The current alarm status for zone " + i + ".").register());
		}
		
		if (ppmGauges.size() != 16) {
			System.out.println("I failed to design good code. This needs to be 16.");
			System.exit(0);
		}
		
		// Create MultizoneDevice
		multizone = new MultiZoneDevice(modbusAddress, serialDeviceName, baudRate);		
		
		while (running) {
			if (lastUpdate == null) {
				lastUpdate = Instant.now();
			}
			
			if (Duration.between(lastUpdate, Instant.now()).getSeconds() >= 60) {
				// Contact Multizone
				ArrayList<CurrentZoneStatus> status = new ArrayList<CurrentZoneStatus>();	
				boolean readSuccessfully = true;
				System.out.println("Attempting to read from MultiZoneDevice...");
				try {
					status = multizone.getAllCurrentZoneStatuses();
				} catch (ModbusException e) {					
					e.printStackTrace();
					readSuccessfully = false;
				} catch (Exception e) {					
					e.printStackTrace();	
					readSuccessfully = false;
				}		
				
				if (readSuccessfully) {				
					System.out.println("Read successful...");
					// Update Gauges with the new information
					for (int i = 0; i < 16; i++) {
						ppmGauges.get(i).set(status.get(i).getPPM());
						alarmStatusGauges.get(i).set(status.get(i).getAlarmStatus());
					}
				}		
				
				lastUpdate = Instant.now();
			}			
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
		}
	}
	
	public static void out(String message) {
		System.out.println(message);
	}
}
