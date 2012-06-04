/*
 * CoapBase.java -- Common base class for client and server
 *
 * Copyright (C) 2011 Markus Becker <mab@comnets.uni-bremen.de>
 *
 */

package de.tzi.coap;

public class CoapBase {
    static {
	System.out.println("INF: load coap library");
	System.loadLibrary("coap");
	System.out.println("INF: loaded coap library");
    }
}
