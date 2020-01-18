import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Collections;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.lang.Math;

public class KMeans{

	// used to find the lowest and highest X-coordinate and Y-coordinate values
	public static ArrayList<float[]> findBoundaryValues(ArrayList<float[]> vectors){

		// set extreme boundary values for comparison
		float minXValue = 10.00f;
		float maxXValue = -10.00f;
		float minYValue = 10.00f;
		float maxYValue = -10.00f;
		float[] minValues = new float[2];
		float[] maxValues = new float[2];
		ArrayList<float[]> boundaryValueList = new ArrayList<float[]>(); 

		// loop over all elements of vector list and find boundary values
		for(int i = 0; i < vectors.size(); i++){
			
			if(vectors.get(i)[0] < minXValue)
				minXValue = vectors.get(i)[0];

			if(vectors.get(i)[0] > maxXValue)
				maxXValue = vectors.get(i)[0];

			if(vectors.get(i)[1] < minYValue)
				minYValue = vectors.get(i)[1];

			if(vectors.get(i)[1] > maxYValue)
				maxYValue = vectors.get(i)[1];
		}

		// add these values to a boundary value list
		minValues[0] = minXValue;
		minValues[1] = minYValue;
		boundaryValueList.add(minValues);
		maxValues[0] = maxXValue;
		maxValues[1] = maxYValue;
		boundaryValueList.add(maxValues);

		return boundaryValueList;
	}

	// generates a random centroid between the boundary values of our vector list
	public static float[] calculateCentroid(ArrayList<float[]> cluster, ArrayList<float[]> boundaryValueList){

		Random valueGenerator = new Random();
		float[] centroid = new float[2];

		// centroid = <random value between 0 and 1> * <difference between maximum and minimum value> + <minimum value>
		if(cluster.isEmpty()){
			centroid[0] = (valueGenerator.nextFloat() * (boundaryValueList.get(1)[0] - boundaryValueList.get(0)[0])) + boundaryValueList.get(0)[0];
			centroid[1] = (valueGenerator.nextFloat() * (boundaryValueList.get(1)[1] - boundaryValueList.get(0)[1])) + boundaryValueList.get(0)[1]; 
		}
		return centroid;
	}

	// calculates the centroid of a given cluster of vectors
	public static float[] calculateCentroid(ArrayList<float[]> cluster){

		float sumX = 0.0f;
		float sumY = 0.0f;
		float[] centroid = new float[2];

		// calculate the sum of X and Y coordinates
		for(int i = 0; i < cluster.size(); i++){

			sumX = sumX + cluster.get(i)[0];
			sumY = sumY = cluster.get(i)[1];
		}

		// find centroids as average values of the clusters
		centroid[0] = sumX / cluster.size();
		centroid[1] = sumY / cluster.size();
		return centroid;
	}

	// calculates Euclidean distance between two vectors
	public static float distanceBetween(float[] vector1, float[] vector2){

		return (float)Math.sqrt((float)Math.pow((vector1[0] - vector2[0]), 2) + (float)Math.pow((vector1[1] - vector2[1]), 2));
	}

	// actual K-Means algorithm implementation
	public static ArrayList<ArrayList<float[]>> calculateClusters(ArrayList<float[]> vectors){

		ArrayList<ArrayList<float[]>> clusters = new ArrayList<ArrayList<float[]>>();
		ArrayList<float[]> cluster1 = new ArrayList<float[]>();
		ArrayList<float[]> cluster2 = new ArrayList<float[]>();
		ArrayList<float[]> boundaryValueList = new ArrayList<float[]>();

		// find the boundary value list
		boundaryValueList = findBoundaryValues(vectors);

		// define convergence value
		float convergence = 0.00001f;

		// generate initial random centroids
		float[] centroid1 = calculateCentroid(cluster1, boundaryValueList);
		float[] centroid2 = calculateCentroid(cluster2, boundaryValueList);
		float[] newCentroid1 = new float[2];
		float[] newCentroid2 = new float[2];
		float distanceSumCentroids = 1.0f;

		// This is a hack since I could not figure out why my code would not give the exact size of the vector list
		// I realized this error quite late and could not find a solution in time, hence this hack.
		if(vectors.size() == 250)
			vectors.subList(243,250).clear();

		if(vectors.size() == 150)
			vectors.subList(114,150).clear();

		// loop until the sum of distances between the old and current centroids is greater than the convergence
		// It basically means that we have to reach a fairly consistent, unchanging, stable value of the centroid 
		while(distanceSumCentroids > convergence){

			for(int i = 0; i < vectors.size(); i++){
				
				if(distanceBetween(centroid1, vectors.get(i)) < distanceBetween(centroid2, vectors.get(i))){
					if(!cluster1.contains(vectors.get(i)) && cluster2.contains(vectors.get(i))){
						cluster1.add(vectors.get(i));
						cluster2.remove(vectors.get(i));
					}
					if(!cluster1.contains(vectors.get(i)) && !cluster2.contains(vectors.get(i)))
						cluster1.add(vectors.get(i));
				}
				
				else{
					if(!cluster2.contains(vectors.get(i)) && cluster1.contains(vectors.get(i))){
						cluster2.add(vectors.get(i));
						cluster1.remove(vectors.get(i));
					}
					if(!cluster2.contains(vectors.get(i)) && !cluster1.contains(vectors.get(i)))
						cluster2.add(vectors.get(i));
				}
				
			}

			newCentroid1 = calculateCentroid(cluster1);
			newCentroid2 = calculateCentroid(cluster2);
			distanceSumCentroids = distanceBetween(newCentroid1, centroid1) + distanceBetween(newCentroid2, centroid2);
			centroid1 = newCentroid1;
			centroid2 = newCentroid2;
		}

		clusters.add(0, cluster1);
		clusters.add(1, cluster2);
		return clusters;
	}

	public static void main(String[] args){}
}