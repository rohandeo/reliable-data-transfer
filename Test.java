import java.util.ArrayList;
import java.util.Arrays;


public class Test{
	
	private static void printVector(ArrayList<float[]> vectors){

		for(int i = 0; i < vectors.size(); i++)
			System.out.println(Arrays.toString(vectors.get(i)));		
	}

	private static int ghe(int x){

		return (x & 0b00000000000000000000000011111111);
	}

	private static int ghe1(int x){

		return (x & 0b00000000000000001111111100000000);
	}

	public static void main(String[] args){

		// short sequenceNumber = 0;
		// byte lavda = -1;
		// byte lasun = 4;
		// ArrayList<int[]> ok = new ArrayList<int[]>();
		// byte[] res = new byte[4];
		// int[] hm = {210,-129};
		// int[] pch = {-999, -670};
		// PacketCreator bc = new PacketCreator();
		// int chut;
		// int kkk;
	

	// 	ok.add(pch);
	// 	ok.add(hm);
	// 	printVector(ok);
	// 	res = bc.createDATAPacket(sequenceNumber, ok);
	// 	System.out.println(Arrays.toString(res));
		
	// 	for(int i = 5; i < 21; i+=4){
			
	// 	chut = (int)(res[i]) << 24 | (int)(res[i+1]) << 16 | ghe1((int)(res[i+2]) << 8) | ghe((int)(res[i+3]));
	// 	System.out.println(chut);
	// }
		// sequenceNumber = (short)(((short) lavda << 8 ) | (short)(lasun));
		// System.out.println(sequenceNumber);

		KMeans kmeans = new KMeans();
		Server server = new Server();
		ArrayList<float[]> vectors = new ArrayList<float[]>();
		ArrayList<ArrayList<float[]>> clusters = new ArrayList<ArrayList<float[]>>();
		float[] centroid1 = new float[2];
		float[] centroid2 = new float[2];

		System.out.println("receiving data");
		vectors = server.decodeVectors(server.receiveData());

		printVector(vectors);
		System.out.println(vectors.size());
		System.out.println("calculating clusters");
		clusters = KMeans.calculateClusters(vectors);
		centroid1 = KMeans.calculateCentroid(clusters.get(0));
		centroid2 = KMeans.calculateCentroid(clusters.get(1));
		System.out.println(Arrays.toString(centroid1) + "\t" + Arrays.toString(centroid2));

		server.sendData(centroid1, centroid2);

	}
}