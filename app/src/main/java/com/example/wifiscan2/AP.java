package com.example.wifiscan2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * AP（access point)点只包含 SSID,level
 * @author Guangliang Liu
 *
 */
public class AP implements Serializable {
	String SSID;
	String BSSID;
	int level;
}

