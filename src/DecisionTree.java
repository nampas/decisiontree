import java.util.ArrayList;
import java.util.Set;

class DecisionTree {

	private static final String TAG = DecisionTree.class.getSimpleName();
	
	private DataModel mDataModel;
	private DTreeNode mRootNode;
	private double mTreeAccuracy;

	// Specifies how many spaces to skip before adding next element to tune set
	private int TUNE_SET_SPACING = 4; 
	
	// The natural log of 2. 
	// Pre-calculating this makes log2() a bit more efficient
	private static final double NAT_LOG_2 = 0.69314718056d;

	/**
	 * Builds a decision tree for the specified data model
	 * @param votingFilePath Path to voting file
	 */
	public DecisionTree(DataModel dataModel) {
		mDataModel = dataModel;
		mRootNode = null;
	}
	
	/**
	 * Executes leave-one-out cross validation, and prints the average accuracy
	 * across all instances
	 */
	public double doLOUCrossValidation() {
		Log.i(TAG, "Executing leave-one-out cross validation");
		
		DataModel.Datum[] data = mDataModel.getData();
		
		double totalAccuracy = 0;
		DataModel.Datum[] testTune = 
				new DataModel.Datum[data.length-1];
		
		// Loop through all data elements, we leave one out each time
		for(int i = 0; i < data.length; i++) {
			// Inefficient, but easy. Copy over all elements except the ith, 
			// which will constitute our testing set
			for(int j = 0; j < data.length; j++) {
				if(j < i) {
					testTune[j] = data[j];
				} else if (j > i) {
					// ith element is not copied over, so offset index down one
					testTune[j-1] = data[j];
				}
			}
			// Train and tune with the left-one-out data set
			trainAndTune(testTune);
			// Test with the element left out, and increment total accuracy
			totalAccuracy += 
					findTreeAccuracy(new DataModel.Datum[] {data[i]});
		}
		// Return average accuracy
		return totalAccuracy / data.length;
	}
	
	public void trainAndTune(DataModel.Datum[] data) {
		// Split data into training and tuning sets
		DataModel.Datum[][] trainTune = buildTrainTuneSets(data);

		// Initialize root with the training data and start training
		mRootNode = new DTreeNode(trainTune[0], 
								mDataModel.getLabels(), 
								calculateEntropy(trainTune[0]));
		trainTreeHelper(mRootNode);

		// Find the unpruned accuracy
		mTreeAccuracy = findTreeAccuracy(trainTune[1]);

		// Tune the induced tree
		tuneTree(trainTune[1]);
	}
	
	/**
	 * Does reduced-error pruning on the tree. This a greedy approach.
	 * @param root 
	 * @param tuningData
	 */
	private void tuneTree(DataModel.Datum[] tuningData) {
		boolean morePruning = true;
		// Loop until any more pruning reduce the accuracy of the tree
		while(morePruning) {
			// The best prune will move up the recursive stack. If the best 
			// prune increases accuracy, do the prune. Otherwise end pruning.
			TuneWrapper bestPrune = tuneTreeHelper(mRootNode, tuningData);
			if(bestPrune.bestAccuracy > mTreeAccuracy) {
				DTreeNode pruneNode = bestPrune.bestNodeToPrune;
				pruneNode.setUniform(pruneNode.getMajorityLabel());
				mTreeAccuracy = bestPrune.bestAccuracy;
			} else {
				morePruning = false;
			}
		}
	}
	
	/**
	 * Recursive helper method for doing reduced-error pruning
	 * @param root Current root node
	 * @param tuningData The data to tune on
	 * @return The best node to prune from the root's subtree, including 
	 *         the root 
	 */
	private TuneWrapper tuneTreeHelper(DTreeNode root, 
									DataModel.Datum[] tuningData) 
	{	
		DTreeNode bestNode = root;
		double bestAccuracy;
		// Try pruning this node and calculate new accuracy. Keep track of
		// the old uniform value, and reset to it after accuracy check so that
		// this node will not be considered a leaf upon recursion.
		Character oldUniformVal = root.getUniformVal();
		root.setUniform(root.getMajorityLabel());
		bestAccuracy = findTreeAccuracy(tuningData);
		root.setUniform(oldUniformVal);

		// Recurse to children
		for(DTreeNode child : root.getChildren()) {
			TuneWrapper result = tuneTreeHelper(child, tuningData);
			// If any child branch returns a better accuracy, pass it on. 
			if(bestAccuracy < result.bestAccuracy) {
				bestNode = result.bestNodeToPrune;
				bestAccuracy = result.bestAccuracy;
			}
		}
		return new TuneWrapper(bestNode, bestAccuracy);
	}
	
	/**
	 * Recursive helper method for training the tree
	 * @param root Current root node
	 */
	public void trainTreeHelper(DTreeNode root) {
		
		// End recursion when we've reached a uniformly labeled node
		if(root.isUniform())
			return;

		// Keep track of best gain seen so far
		double bestGain = -1;
		int bestFeature = -1;
		DataModel.Datum[][] bestSplit = null;
		
		int rootDataLength = root.getData().length;
		int numFeatures = mDataModel.getNumFeatures();
		// Split on every issue, see which will maximize the gain
		for(int i = 0; i < numFeatures; i++) {
			DataModel.Datum[][] split = splitDataOnFeature(root.getData(), i);
			double currentEntropy = 0;
			for(int j = 0; j < split.length; j++) {
				// Add weighted entropy of this subset to running total
				currentEntropy += ((double)split[j].length / rootDataLength) 
						* calculateEntropy(split[j]);
			}
			// Subtract the post-split weighted entropy from this node's 
			// entropy to calculate gain
			double curGain = root.getEntropy() - currentEntropy;
			
			// If this gain is better, update best-so-far
			if(curGain > bestGain) {
				bestGain = curGain;
				bestFeature = i;
				bestSplit = split;
			}
		}
		
		// If no feature splits lead to a gain, then make this node a leaf with 
		// the majority label as its uniform value
		if(bestGain == 0) {
			root.setUniform(root.getMajorityLabel());
			return;
		}
		
		// Set this node's feature index
		root.setSplitOn(bestFeature);
		
		// Build and set children nodes
		for(int i = 0; i < bestSplit.length; i++) {
			DTreeNode curNode = new DTreeNode(bestSplit[i], 
											mDataModel.getFeatureValue(i), 
											root, 
											mDataModel.getLabels(), 
											calculateEntropy(bestSplit[i]));
			root.addChild(curNode);
			
			// Recurse
			trainTreeHelper(curNode);
		}
	}
	
	/**
	 * Separates the specified data into train and tune sets
	 * @param data Data to separate
	 * @return Two lists, first of which is training data, second of which is 
	 *         tuning data
	 */
	private DataModel.Datum[][] buildTrainTuneSets(DataModel.Datum[] data) {
		DataModel.Datum[][] trainTune = new DataModel.Datum[2][];
		
		// Initialize train and tune sets
		int tuneLength = data.length / TUNE_SET_SPACING;
		if(data.length % TUNE_SET_SPACING != 0) tuneLength++;
		DataModel.Datum[] tuneSet = new DataModel.Datum[tuneLength];
		DataModel.Datum[] trainSet = 
				new DataModel.Datum[data.length - tuneSet.length];

		int tuneIndex = 0;
		int trainIndex = 0;
		
		// Split into sets
		for(int i = 0; i < data.length; i++) {
			// Every TUNE_SET_SPACING spaces, we add to to tuning set
			if(i % TUNE_SET_SPACING == 0)
				tuneSet[tuneIndex++] = data[i];
			else 
				trainSet[trainIndex++] = data[i];
		}
		trainTune[0] = trainSet;
		trainTune[1] = tuneSet;
		return trainTune;
	}
	
	/**
	 * Splits the data on the specified feature, returning lists of data for
	 * each feature value
	 * @param data Data to split
	 * @param featureIndex Feature on which to split
	 * @return An 2d array with all data split into separate lists based on
	 *         their feature value at the specified feature index 
	 */
	private DataModel.Datum[][] splitDataOnFeature(DataModel.Datum[] data,
												int featureIndex) 
	{
		int numFeatureVals = mDataModel.getNumFeatureValues();	

		// Initialize all the split lists
		ArrayList<ArrayList<DataModel.Datum>> splits = 
				new ArrayList<ArrayList<DataModel.Datum>>();
		for(int i = 0; i < numFeatureVals; i++)
			splits.add(new ArrayList<DataModel.Datum>());
		
		// On the specified feature, split data into lists of each feature value
		for(DataModel.Datum d : data) {
			Character featureVal = d.getFeature(featureIndex);
			// Add feature to the correct list
			for(int i = 0; i < numFeatureVals; i++) {
				if(mDataModel.getFeatureValue(i).equals(featureVal)) {
					splits.get(i).add(d);
					break;
				}
			}			
		}

		// Initialize the 2d array which will hold lists of data split by
		// feature value on the feature specified
		DataModel.Datum[][] splitLists = new DataModel.Datum[numFeatureVals][];
				
		// Copy to arrays and add to respective locations in container array
		for(int i = 0; i < numFeatureVals; i++) {
			ArrayList<DataModel.Datum> curList = splits.get(i);
			DataModel.Datum[] curFeatureVal = 
					new DataModel.Datum[curList.size()];
			curList.toArray(curFeatureVal);
			splitLists[i] = curFeatureVal;
		}
				
		return splitLists;
	}
	
	/**
	 * Calculates the tree accuracy using the specified test data
	 * @param testData Data to test with
	 * @return Percentage of test data that tree correctly labels
	 */
	public double findTreeAccuracy(DataModel.Datum[] testData) {
		int totalAccurate = 0;
		for(DataModel.Datum d : testData) {
			DTreeNode curRoot = mRootNode;
			// Loop until we get to a leaf node
			while(!curRoot.isUniform()) {
				int splitOn = curRoot.getSplitOn();
				// Find the appropriate child node, and continue looping
				for(DTreeNode child : curRoot.getChildren()) {
					if(child.getFeatureValue() == d.getFeature(splitOn))
						curRoot = child;
				}
			}
			// Compare the uniform value (label) of the leaf node against
			// the label of the current Representative. If they're equal,
			// the tree is accurate in this case, so increment counter
			if(curRoot.getUniformVal() == d.getLabel())
				totalAccurate++;
		}
		
		// Return accuracy percentage
		return (double)totalAccurate * 100.0d / testData.length;
	}

	@Override
	public String toString() {
		return stringHelper(mRootNode, 1);
	}
	
	/**
	 * Recursive helper for building string representations of this DecisionTree
	 * @param root Current root
	 * @param depth The depth of the root
	 * @return As tring representation of this node and its subtree
	 */
	private String stringHelper(DTreeNode root, int depth) {
		String str = root + "\n";
		for(DTreeNode r : root.getChildren()) {
			if(r != null) {
				// Add correct indentation
				for(int i = 0; i < depth; i++)
					str += "  ";
				str += stringHelper(r, depth+1);
			}
		}
		return str;
	}
	
	/**
	 * Calculates the entropy of the given data array
	 * @param data Data to calculate entropy for
	 * @return Entropy of the specified data set
	 */
	private double calculateEntropy(DataModel.Datum[] data) {
		
		int label1Count = 0;
		// Add up number of D party politicians
		for(DataModel.Datum d : data)
			if(d.getLabel() == mDataModel.getLabel(0)) 
				label1Count++;
		
		// Calculate probabilities
		double label1Prob = (data.length == 0) ? 
				0 : (double) label1Count / data.length; 
		double label2Prob = 1.0d - label1Prob;
		
		// Now plug in to the binary entropy equation
		double entropy = -label1Prob * log2(label1Prob) - 
				label2Prob * log2(label2Prob);
		
		return entropy;
	}
	
	/**
	 * Calculates the log base 2 of the argument. log2(0) here is defined to be 
	 * 0 for convenience purposes.
	 * @param x value to find log of
	 * @return The log base 2 of the argument. log2(0) is defined as 0.
	 */
	private static double log2(double x) {
		if(x == 0) 
			return 0;
		else 
			return Math.log(x) / NAT_LOG_2;
	}
	
	private class TuneWrapper {
		double bestAccuracy;
		DTreeNode bestNodeToPrune;
		
		public TuneWrapper(DTreeNode nodeToPrune, double accuracy) {
			bestAccuracy = accuracy;
			bestNodeToPrune = nodeToPrune;
		}
	}
}