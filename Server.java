import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

// simulates the server's network transactions
public class Server{
	
	static private final int SERVER_PORT = 4269;
	static private final int CLIENT_PORT = 4200;
	static private InetAddress sourceIP = null;

	// used for converting byte to unsigned integer for vector decoding
	public static int lastByteToUnsignedInt(int x){

		return (x & 0b00000000000000000000000011111111);
	}

	// used for converting byte to unsigned integer for vector decoding
	public static int secondLastByteToUnsignedInt(int x){

		return (x & 0b00000000000000001111111100000000);
	}

	// decodes the received vector buffer of bytes into corresponding floating-point values
	public static ArrayList<float[]> decodeVectors(ArrayList<byte[]> vectorBuffer){

		int[] interimValues = null;
		float[] floatValues = null;
		ArrayList<float[]> vectors = new ArrayList<float[]>();

		for(int i = 0; i < vectorBuffer.size(); i++){

			for(int j = 5; j < vectorBuffer.get(i).length; j++){

				interimValues = new int[2]; // a new initialization is necessary since all arrays in java are immutable objects

				// bitwise shifting operations to retrieve intermediate integer value
				interimValues[0] = ((int)(vectorBuffer.get(i)[j++]) << 24) | ((int)(vectorBuffer.get(i)[j++]) << 16) | secondLastByteToUnsignedInt(((int)(vectorBuffer.get(i)[j++]) << 8)) | lastByteToUnsignedInt((int)(vectorBuffer.get(i)[j++]));
				interimValues[1] = ((int)(vectorBuffer.get(i)[j++]) << 24) | ((int)(vectorBuffer.get(i)[j++]) << 16) | secondLastByteToUnsignedInt(((int)(vectorBuffer.get(i)[j++]) << 8)) | lastByteToUnsignedInt((int)(vectorBuffer.get(i)[j]));

				floatValues = new float[2]; // a new initialization is necessary since all arrays in java are immutable objects

				// division by 100 to retrieve final float value
				floatValues[0] = (float)(interimValues[0]) / 100;
				floatValues[1] = (float)(interimValues[1]) / 100;
				vectors.add(floatValues); // add the final vector to our vector list
			}
		}
		return vectors;
	}

	// implements Server's Data Vector Upload Phase
	public static ArrayList<byte[]> receiveData(){

		PacketCreator creator = new PacketCreator();
		DatagramSocket dataSocket = null;
		DatagramPacket dataPacket = null;
		DatagramPacket dackPacket = null;
		DatagramPacket reqPacket = null;
		DatagramPacket rackPacket = null;
		
		short ackNumber = 0;
		short sequenceNumber;
		byte[] dataBuffer = null;
		byte[] dackBuffer = new byte[PacketCreator.DACK_LENGTH];
		byte[] reqBuffer = new byte[PacketCreator.REQ_LENGTH];
		byte[] rackBuffer = new byte[PacketCreator.RACK_LENGTH];
		int counter = 0;
		int timeout = 3000;
		ArrayList<byte[]> listOfAllBytes = new ArrayList<byte[]>();

		// create a new Socket for network transactions
		try {
			dataSocket = new DatagramSocket(SERVER_PORT);
		}
		catch (SocketException e) {
            System.out.println("Cannot create socket with port " + SERVER_PORT);
        }

        // loop until REQ packet is received or network error occurs
        while(true){

        	// create a DATA packet for receiving vectors
        	dataBuffer = new byte[PacketCreator.DATA_LENGTH];
        	dataPacket = new DatagramPacket(dataBuffer, PacketCreator.DATA_LENGTH);

        	// receive DATA packets via the socket
        	try {
				dataSocket.receive(dataPacket);
			}
			catch(IOException g){
				System.out.println("Receiving error in DATA packet");
			}

			// extract the Client's IP address from incoming DATA packets
			sourceIP = dataPacket.getAddress();

			// DATA packet has correct packet type
			if(dataPacket.getData()[0] == 0x00){
				
				// extract the sequence number of the packet
				sequenceNumber = (short)(((short)(dataPacket.getData()[1]) << 8 ) | (short)dataPacket.getData()[2]);
				
				// check whether the packet with correct sequence number has arrived
				if(sequenceNumber == ackNumber){

					listOfAllBytes.add(dataPacket.getData());// add the DATA packet's data buffer to a received list

					// create a DACK packet for acknowledging the DATA packet
					dackBuffer = creator.createDACKPacket(ackNumber);
					dackPacket = new DatagramPacket(dackBuffer, PacketCreator.DACK_LENGTH, sourceIP, CLIENT_PORT);
					ackNumber++; // increment ACK number for future DATA packets

					// send a DACK packet to acknowledge current DATA packet
					try {
						dataSocket.send(dackPacket);
					}
					catch(IOException s) {
						System.out.println("Send method failed!!");
					}
				}

				//send DACK packet to acknowledge last correctly received DATA packet
				else{

					// create DACK packet
					dackBuffer = creator.createDACKPacket(ackNumber);
					dackPacket = new DatagramPacket(dackBuffer, PacketCreator.DACK_LENGTH, sourceIP, CLIENT_PORT);
					
					// send last correctly received DATA packet's sequence number in the DACK packet
					try {
						dataSocket.send(dackPacket);
					}
					catch(IOException s) {
						System.out.println("Send method failed!!");
					}					
				}
			}

			// if REQ packet is received
			else if(dataPacket.getData()[0] == 0x02){

				// create RACK packet to acknowledge REQ packet
				rackBuffer = creator.createRACKPacket();
				rackPacket = new DatagramPacket(rackBuffer, PacketCreator.RACK_LENGTH, sourceIP, CLIENT_PORT);
				
				// send RACK packet
				try {
					dataSocket.send(rackPacket);
				}
				catch(IOException s) {
					System.out.println("Send method failed!!");
				}

				System.out.println("Waiting 3 seconds for additional REQ Packets");
				
				// create a REQ packet to receive additional REQ packets
				reqPacket = new DatagramPacket(reqBuffer, PacketCreator.REQ_LENGTH);

				// set a timeout value of 3 seconds
				try {
					dataSocket.setSoTimeout(timeout);
				}
				catch (SocketException p) {
		            System.out.println("Cannot set given timeout " + timeout);
		        }

		        // receive additional REQ packets, if any
		        try {
					dataSocket.receive(reqPacket);
				}
				catch(IOException g){
					break;
				}

				// if additional REQ packet is received, repeat the RACK packet sending process
				if(reqPacket.getData()[0] == 0x02)
					continue;
			}
        }

        dataSocket.close();
        return listOfAllBytes;
	}

	public static void sendData(float[] centroid1, float[] centroid2){

		PacketCreator creator = new PacketCreator();
		DatagramSocket clusSocket = null;
		DatagramPacket clusPacket = null;
		DatagramPacket cackPacket = null;
		byte[] clusBuffer = new byte[PacketCreator.CLUS_LENGTH];
		byte[] cackBuffer = new byte[PacketCreator.CACK_LENGTH];
		ArrayList<int[]> centroids = new ArrayList<int[]>();
		int[] temp = null;
		int timeout = 1000;
		int timeoutCounter = 1;

		// convert the floating point values into integer values
		// "centroids" variable will later be used for CLUS packet creation
		temp = new int[2];
		temp[0] = (int)(centroid1[0] * 100);
		temp[1] = (int)(centroid1[1] * 100);
		centroids.add(temp);
		temp = new int[2];
		temp[0] = (int)(centroid2[0] * 100);
		temp[1] = (int)(centroid2[1] * 100);
		centroids.add(temp);

		//create a socket for network transactions
		try {
			clusSocket = new DatagramSocket(SERVER_PORT);
		}
		catch (SocketException e) {
	        System.out.println("Cannot create socket with port " + SERVER_PORT);
	    }

	    // loop until CACK packet is correctly received or network error occurs 
	    while(true){

	    	// create CLUS packet
	        clusBuffer = creator.createCLUSPacket(centroids);
			clusPacket = new DatagramPacket(clusBuffer, PacketCreator.CLUS_LENGTH, sourceIP, CLIENT_PORT);

			// send CLUS packet to client
			try {
				clusSocket.send(clusPacket);
			}
			catch(IOException s) {
				System.out.println("Send method failed!!");
			}

			// create a CACK packet for receiving acknowledgements frm Client
			cackBuffer = creator.createCACKPacket();
		    cackPacket = new DatagramPacket(cackBuffer, PacketCreator.CACK_LENGTH);

		    // set initial timeout value as 1 second
		    try {
				clusSocket.setSoTimeout(timeout);
			}
			catch (SocketException l) {
		        System.out.println("Cannot set given timeout " + timeout);
		    }

		    // receive CACK packet
		    try {
				clusSocket.receive(cackPacket);
			}
			// in case of timeout, this block is executed
			catch(IOException n){

				// if 4th timeout event then exit program 
				if(timeoutCounter == 4){
					System.out.println("Communication failure! Restart network and try again.");
					System.exit(0);
				}

				// double timeout value each time it occurs
				timeout = timeout * 2;
				timeoutCounter++;
				continue;
			}
			
			break;
		}
		clusSocket.close();
	}

	public static void main(String[] args){

		KMeans kmeans = new KMeans();
		ArrayList<float[]> vectors = new ArrayList<float[]>();
		ArrayList<ArrayList<float[]>> clusters = new ArrayList<ArrayList<float[]>>();
		float[] centroid1 = new float[2];
		float[] centroid2 = new float[2];

		System.out.println("receiving data");
		vectors = decodeVectors(receiveData());

		System.out.println("calculating clusters");
		clusters = KMeans.calculateClusters(vectors);
		centroid1 = KMeans.calculateCentroid(clusters.get(0));
		centroid2 = KMeans.calculateCentroid(clusters.get(1));
		
		// System.out.println(Arrays.toString(centroid1) + "\t" + Arrays.toString(centroid2));

		// System.out.println(clusters.get(0).size() + "\t" + clusters.get(1).size());

		System.out.println("sending centroids");
		sendData(centroid1, centroid2);
		System.out.println("Server job done. Closing connection.");
	}
}