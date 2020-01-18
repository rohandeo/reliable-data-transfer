import java.util.ArrayList;

public class PacketCreator{
	
	public static final int DATA_LENGTH = 405; 
	// 50 2D vectors = 400 bytes + 2 bytes sequence number + 2 bytes length of data buffer + 1 byte packet type
	// Total 405 bytes
	public static final int DACK_LENGTH = 3;
	// Total length = 1(Packet type) + 2(Ack Number) = 3 bytes
	public static final int REQ_LENGTH = 1;
	// Total Length = 1(Packet Type)
	public static final int RACK_LENGTH = 1;
	// Total Length = 1(Packet Type)
	public static final int CLUS_LENGTH = 17;
	// Total Length = 1(Packet Type) + 16(2 2D Vectors) = 17 bytes
	public static final int CACK_LENGTH = 1;
	// Total Length = 1(Packet Type)

	public byte[] createDATAPacket(short sequenceNumber, ArrayList<int[]> dataVectors){

		byte[] dataPacket = new byte[DATA_LENGTH];
		int j = 5;
		dataPacket[0] = 0x00; // Packet Type

		// bitmasking and bitwise right shift to convert short to byte
		dataPacket[1] = (byte)((sequenceNumber & 0b1111111100000000) >> 8);
		dataPacket[2] = (byte)(sequenceNumber & 0b0000000011111111);
		dataPacket[3] = (byte)((dataVectors.size() & 0b00000000000000001111111100000000) >> 8);
		dataPacket[4] = (byte)(dataVectors.size() & 0b00000000000000000000000011111111);

		for(int i = 0; i < dataVectors.size(); i++){

			// bitmasking and bitwise right shift to convert int to byte
			if(j < DATA_LENGTH ){
				dataPacket[j++] = (byte)((dataVectors.get(i)[0] & 0b11111111000000000000000000000000) >> 24);
				dataPacket[j++] = (byte)((dataVectors.get(i)[0] & 0b00000000111111110000000000000000) >> 16);
				dataPacket[j++] = (byte)((dataVectors.get(i)[0] & 0b00000000000000001111111100000000) >> 8);
				dataPacket[j++] = (byte)(dataVectors.get(i)[0] & 0b00000000000000000000000011111111);
				dataPacket[j++] = (byte)((dataVectors.get(i)[1] & 0b11111111000000000000000000000000) >> 24);
				dataPacket[j++] = (byte)((dataVectors.get(i)[1] & 0b00000000111111110000000000000000) >> 16);
				dataPacket[j++] = (byte)((dataVectors.get(i)[1] & 0b00000000000000001111111100000000) >> 8);
				dataPacket[j++] = (byte)(dataVectors.get(i)[1] & 0b00000000000000000000000011111111);
			}
			else
				System.out.println("DATA PACKET creation problem, number of bytes exceeding packet length");
		}
		return dataPacket;
	}

	public byte[] createDACKPacket(short ackNumber){

		byte[] dackPacket = new byte[DACK_LENGTH];
		dackPacket[0] = 0x01; // Packet Type

		// bitmasking and bitwise right shift to convert short to byte
		dackPacket[1] = (byte)((ackNumber & 0b1111111100000000) >> 8);
		dackPacket[2] = (byte)(ackNumber & 0b0000000011111111);
		return dackPacket;
	}

	public byte[] createREQPacket(){
		
		byte[] reqPacket = new byte[REQ_LENGTH];
		reqPacket[0] = 0x02; // Packet Type
		return reqPacket; 
	}

	public byte[] createRACKPacket(){
		
		byte[] rackPacket = new byte[RACK_LENGTH];
		rackPacket[0] = 0x03; // Packet Type
		return rackPacket; 
	}

	public byte[] createCLUSPacket(ArrayList<int[]> centroids){

		byte[] clusPacket = new byte[CLUS_LENGTH];
		clusPacket[0] = 0x04; // Packet Type
		int j = 1;

		// bitmasking and bitwise right shift to convert int to byte
		for(int i = 0; i < centroids.size(); i++){

			clusPacket[j++] = (byte)((centroids.get(i)[0] & 0b11111111000000000000000000000000) >> 24);
			clusPacket[j++] = (byte)((centroids.get(i)[0] & 0b00000000111111110000000000000000) >> 16);
			clusPacket[j++] = (byte)((centroids.get(i)[0] & 0b00000000000000001111111100000000) >> 8);
			clusPacket[j++] = (byte)(centroids.get(i)[0] & 0b00000000000000000000000011111111);
			clusPacket[j++] = (byte)((centroids.get(i)[1] & 0b11111111000000000000000000000000) >> 24);
			clusPacket[j++] = (byte)((centroids.get(i)[1] & 0b00000000111111110000000000000000) >> 16);
			clusPacket[j++] = (byte)((centroids.get(i)[1] & 0b00000000000000001111111100000000) >> 8);
			clusPacket[j++] = (byte)(centroids.get(i)[1] & 0b00000000000000000000000011111111);
		}
		return clusPacket;
	}

	public byte[] createCACKPacket(){

		byte[] cackPacket = new byte[CACK_LENGTH];
		cackPacket[0] = 0x05;
		return cackPacket;
	}

	public static void main(String[] args){}
}