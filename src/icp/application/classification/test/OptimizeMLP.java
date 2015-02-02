package icp.application.classification.test;

import java.util.Map;
import java.util.Random;

public class OptimizeMLP {
	
	
	public static void main(String[] args) throws InterruptedException {
		double accuracy = 0;
		double maxAccuracy = 0;
		Random random = new Random();
		while (accuracy < 0.7) { 
			int numberOfIters = 500 + random.nextInt(900);
			int middleNeurons =  4 + random.nextInt(8);
			System.out.println("New MLP classifier: number of iters: " + numberOfIters + " + middleNeurons: " + middleNeurons);
			TrainUsingOfflineProvider trainOfflineProvider = new TrainUsingOfflineProvider(numberOfIters, middleNeurons);
			
			TestClassificationAccuracy testAccuracy = new TestClassificationAccuracy( trainOfflineProvider.getClassifier());
			Map<String, Statistics> stats = testAccuracy.getStats();
			int okNumber = 0;
			for (Map.Entry<String, Statistics> entry : stats.entrySet()) {
				if (entry.getValue().getRank() == 1) {
					okNumber++;
			}
			
			}
			accuracy = (double) okNumber / stats.size();
			if (accuracy > maxAccuracy) {
				maxAccuracy = accuracy;
				System.out.println("New accuracy record: " + accuracy);
				trainOfflineProvider.getClassifier().save("data/bestOfAllTimes.txt");
				
			}
			else {
				System.out.println("No record: " + accuracy * 100);
			}
		}
		
	}

}