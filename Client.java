import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

// simulates the client's network transactions
public class Client{
	
	static private final int SERVER_PORT = 4269;
	static private final int CLIENT_PORT = 4200;

	//retrieve floating point values from raw input text file 
	public static float[] parseFloats(String raw) {

	    int i = 0; // iterator
	    String[] listBuffer = new String[2];
	    float[] coords = new float[2];
	    Pattern p = Pattern.compile("[-]?[0-9]*\\.?[0-9]+");
	    /* The regular expression translates to-
	     * [-]? = 0 or 1 instance of negative sign
	     * [0-9]* = 0 or more instances of a sequence of digits
	     * \.? = 0 or 1 instance of decimal point
	     * [0-9]+ = 1 or more instances of a sequence of digits
	     */
	    Matcher m = p.matcher(raw);

	    while (m.find()) {
	        listBuffer[i] = m.group();
	        coords[i] = Float.parseFloat(listBuffer[i]);
	        i++;
	    }
	    
	    return coords;
	}

	// prints a given arraylist of float arrays.
	public static void printVector(ArrayList<float[]> vectors){

		for(int i = 0; i < vectors.size(); i++)
			System.out.println(Arrays.toString(vectors.get(i)));		
	}

	// used for converting byte to unsigned integer for vector decoding
	public static int lastByteToUnsignedInt(int x){

		return (x & 0b00000000000000000000000011111111);
	}

	// used for converting byte to unsigned integer for vector decoding
	public static int secondLastByteToUnsignedInt(int x){

		return (x & 0b00000000000000001111111100000000);
	}

	// reads a given input file
	public static ArrayList<float[]> readFile(FileReader inputFile){

		BufferedReader inputLine = null;
		String nextLine = null;
		float[] coordinates = new float[2];
		ArrayList<float[]> vectors = new ArrayList<float[]>();
		boolean flag = true;
		int index = 0;

		inputLine = new BufferedReader(inputFile);
		while(flag){

			try{
				if(inputLine.ready()) // check if there exists a next line in the text
					flag = true;
				else{
					flag = false;
					break;
				}
			}
			catch(IOException a){
				System.out.println("I/O error");
			}

			try{
				nextLine = inputLine.readLine(); // read one line from the text
			}
			catch(IOException b){
				System.out.println("I/O error");
			}
		
			coordinates = parseFloats(nextLine); // extract floating point values from string 
			vectors.add(index, coordinates); // add the coordinates to our vector list
			index++;
		}
		return vectors;
	}

	// implements Client Data Vector Upload Phase
	public static void sendData(InetAddress destinationIP, ArrayList<float[]> vectors){

		PacketCreator creator = new PacketCreator();
		DatagramSocket dataSocket = null;
		DatagramPacket dataPacket = null;
		DatagramPacket dackPacket = null;
		byte[] dataBuffer = null;
		byte[] dackBuffer = new byte[PacketCreator.DACK_LENGTH];
		short ackNumber;
		short sequenceNumber = 0;
		int timeout = 1000; // initialize timeout value as 1000 milliseconds (1 second)
		int timeoutCounter = 1;
		int[] interimValues = null;
		int counter = 0;
		ArrayList<int[]> packetOfVectors = new ArrayList<int[]>();
		
		// create a socket for sending/receiving packets
		try {
			dataSocket = new DatagramSocket(CLIENT_PORT);
		}
		catch (SocketException c) {
            System.out.println("Cannot create socket with port " + CLIENT_PORT);
        }

        // loop until all vectors are sent
        while(counter < vectors.size()){

        	packetOfVectors.clear();
        	// allows a maximum of 50 vectors to be included in a DATA packet
        	for(int i = 0; i < 50; i++){

        		
        		if (counter == vectors.size())
					break;

				interimValues = new int[2];
				interimValues[0] = (int)(vectors.get(counter)[0] * 100);
				interimValues[1] = (int)(vectors.get(counter)[1] * 100);
				packetOfVectors.add(interimValues);
				counter++;
			}

			// create a DATA packet to send over the network
			dataBuffer = creator.createDATAPacket(sequenceNumber, packetOfVectors);
			// System.out.println(Arrays.toString(dataBuffer));
			dataPacket = new DatagramPacket(dataBuffer, PacketCreator.DATA_LENGTH, destinationIP, SERVER_PORT);

			// try sending the packet through the socket
			try {
				dataSocket.send(dataPacket);
			}
			catch(IOException d) {
				System.out.println("Send method failed!!");
			}

			// create a DACK packet to receive acknowledgements
			dackPacket = new DatagramPacket(dackBuffer, PacketCreator.DACK_LENGTH);
			
			// set a timeout event for our socket
			try {
				dataSocket.setSoTimeout(timeout);
			}
			catch (SocketException e) {
	            System.out.println("Cannot set given timeout " + timeout);
	        }

			try {
				dataSocket.receive(dackPacket);
			}

			// this catch block is executed in case of timeout events
			catch(IOException g){
				
				// if timeout event occurs 4 times, exit the program
				if(timeoutCounter == 4){
					System.out.println("Communication failure! Restart network and try again.");
					System.exit(0);
				}

				// double timeout value and resend the DATA Packet
				timeout = timeout * 2; // value in milliseconds
				timeoutCounter++;
				counter = counter - packetOfVectors.size();
				continue;
			}

			// checks for correct packet type
			if (dackPacket.getData()[0] == 0x01){

				// retreives the acknowledment number of the packet and verifies its value
				// if it is not equal to sequence number, resend the DATA packet
				ackNumber = (short)(((short)(dackPacket.getData()[1]) << 8 ) | (short)dackPacket.getData()[2]);
				if(ackNumber != sequenceNumber){
					counter = counter - packetOfVectors.size();
					continue;
 				}
			}

			// resend the DATA packet if the received packet type is incorrect
			else{
				counter = counter - packetOfVectors.size();
				continue;
			}

			// increment sequence number by 1 for next packet
			sequenceNumber++;
		}
		
		dataSocket.close();
	}

	// implements Client's Computational Request Phase
	public static void requestData(InetAddress destinationIP){

		PacketCreator creator = new PacketCreator();
		DatagramSocket reqSocket = null;
		DatagramPacket reqPacket = null;
		DatagramPacket rackPacket = null;
		byte[] reqBuffer = new byte[PacketCreator.REQ_LENGTH];
		byte[] rackBuffer = new byte[PacketCreator.RACK_LENGTH];
		int timeout = 1000; // initialize timeout value as 1000 milliseconds (1 second)
		int timeoutCounter = 1;

		// create new socket
		try {
			reqSocket = new DatagramSocket(CLIENT_PORT);
		}
		catch (SocketException h) {
	        System.out.println("Cannot create socket with port " + CLIENT_PORT);
	    }

		// loop until RACK packet is correctly received
		while(true){
			
			// create a REQ packet to signal end of DATA packets
	        reqBuffer = creator.createREQPacket();
	        reqPacket = new DatagramPacket(reqBuffer, PacketCreator.REQ_LENGTH, destinationIP, SERVER_PORT);

	        // send the REQ packet
	        try {
				reqSocket.send(reqPacket);
			}
			catch(IOException k) {
				System.out.println("Send method failed!!");
			}

			// create a RACK packet to receive acknowledgement for REQ packet
			rackPacket = new DatagramPacket(rackBuffer, PacketCreator.RACK_LENGTH);

			// set a timeout value for our socket
			try {
				reqSocket.setSoTimeout(timeout);
			}
			catch (SocketException l) {
	            System.out.println("Cannot set given timeout " + timeout);
	        }

	        try {
				reqSocket.receive(rackPacket);
			}

			// this block is executed in case of timeout event
			catch(IOException n){

				// if timeout occurs 4 times, exit the program
				if(timeoutCounter == 4){
					System.out.println("Communication failure! Restart network and try again.");
					System.exit(0);
				}

				// for each timeout event, double timeout value
				timeout = timeout * 2;
				timeoutCounter++;
				continue;
			}

			// if RACK packet has correct packet type, our job is done
			if (rackPacket.getData()[0] == 0x03)
				break;
			// if RACK packet has incorrect packet type, discard and repeat the process
			else
				continue;
			
		}
		reqSocket.close();
	}

	// implements the Client's Result Download Phase
	public static ArrayList<float[]> downloadData(InetAddress destinationIP){

		PacketCreator creator = new PacketCreator();
		DatagramSocket clusSocket = null;
		DatagramPacket clusPacket = null;
		DatagramPacket cackPacket = null;
		byte[] clusBuffer = new byte[PacketCreator.CLUS_LENGTH];
		byte[] cackBuffer = new byte[PacketCreator.CACK_LENGTH];
		int timeout = 30000; // 30 seconds
		ArrayList<float[]> centroids = new ArrayList<float[]>();
		float[] result = null;
		int[] temp = new int[4];
		int j = 0;
		int i =1;

		// create a new socket
		try {
			clusSocket = new DatagramSocket(CLIENT_PORT);
		}
		catch (SocketException o) {
	        System.out.println("Cannot create socket with port " + CLIENT_PORT);
	    }

	    // create a CLUS packet for receiving the cluster centroids
		clusPacket = new DatagramPacket(clusBuffer, PacketCreator.CLUS_LENGTH);

		// set a timeout value for our socket
		try {
				clusSocket.setSoTimeout(timeout);
			}
			catch (SocketException p) {
	            System.out.println("Cannot set given timeout " + timeout);
	        }

	    // receive a CLUS packet and if timeout error occurs, declare server failure
	    try {
				clusSocket.receive(clusPacket);
			}
			catch(IOException r){
				System.out.println("Timeout error occured during receiving CLUS packet! Server Failure!");
				System.exit(0);
			}

		// verify that the received packet has correct packet type
		if(clusPacket.getData()[0] == 0x04){

			// create a CACK packet for sending an acknowledgement
			cackBuffer = creator.createCACKPacket();
			cackPacket = new DatagramPacket(cackBuffer, PacketCreator.CACK_LENGTH, destinationIP, SERVER_PORT);

			// send the CACK packet
			try {
				clusSocket.send(cackPacket);
			}
			catch(IOException s) {
				System.out.println("Send method failed!!");
			}
		}

		// extract centroid values from CLUS packet
		for(i = 1; i < PacketCreator.CLUS_LENGTH; i++){

			temp[j++] = ((int)(clusPacket.getData()[i++]) << 24) | ((int)(clusPacket.getData()[i++]) << 16) | secondLastByteToUnsignedInt(((int)(clusPacket.getData()[i++]) << 8)) | lastByteToUnsignedInt((int)(clusPacket.getData()[i++]));
			temp[j++] = ((int)(clusPacket.getData()[i++]) << 24) | ((int)(clusPacket.getData()[i++]) << 16) | secondLastByteToUnsignedInt(((int)(clusPacket.getData()[i++]) << 8)) | lastByteToUnsignedInt((int)(clusPacket.getData()[i]));
		}
;	
		// add the final values in variable "centroids"
		for(j = 0; j < 4; j++){

			result = new float[2];
			result[0] = (float)temp[j++] / 100;
			result[1] = (float)temp[j] / 100;
			centroids.add(result);
		}
		clusSocket.close();
		return centroids;
	}

	public static void main(String[] args){

		FileReader inputFile = null; // create FileReader object to store reference of input file
		ArrayList<float[]> vectors = new ArrayList<float[]>();
		InetAddress destinationIP = null; // we are performing transactions with the LocalHost
		ArrayList<float[]> centroids = new ArrayList<float[]>();
		
		try{
			inputFile = new FileReader("data01.txt");
		}
		catch(FileNotFoundException t)
		{
			System.out.println("No such file");
		}

		vectors = readFile(inputFile);

		// get the address of the LocalHost (127.0.0.1)
		try{
			destinationIP = InetAddress.getLocalHost();
		}
		catch(UnknownHostException u){
			System.out.println("Cannot resolve LocalHost");
		}

		System.out.println("file read done, sending data");
		sendData(destinationIP, vectors);
		
		System.out.println("sending data done, requesting data");
		requestData(destinationIP);
		
		System.out.println("requesting data done, downloading data");
		centroids = downloadData(destinationIP);

		// print final result
		System.out.println("The received centroids are: ");
		printVector(centroids);
		System.out.println("Closing connection");
	}
}