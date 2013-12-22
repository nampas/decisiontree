import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

class DecisionTree {

	private static final String TAG = DecisionTree.class.getSimpleName();

	// Data label values
	public static final char PARTY_D = 'D';
	public static final char PARTY_R = 'R';
	public static final char NOT_UNIFORM = 'x';
	
	// Vote values
	public static final char VOTE_YAY = '+';
	public static final char VOTE_NAY = '-';
	public static final char VOTE_PRESENT = '.';
	public static final char VOTE_NA = 'x'; // Vote n/a (for the root node, which isn't the result of a split)
	
	// Path to default voting data file
	private static final String VOTING_DATA_FILE = "voting-data.tsv"; 
	
	private Datum[] mVotingData;
	private int mNumIssues; // Number of issues in the data set
	private DTreeNode mRootNode;
	private double mTreeAccuracy;
	
	// Specifies how many spaces to skip before adding next element to tune set
	private int TUNE_SET_SPACING = 4; 
	
	// The natural log of 2. Pre-calculating this makes log2() a bit more efficient
	private static final double NAT_LOG_2 = 0.69314718056d;
	
	/**
	 * Main method accepts an argument for the data file location. 
	 * If no argument is provided, the program will use the default voting file
	 * example
	 * @param args
	 */
	public static void main(String[] args) {
		DecisionTree dTree;
		if(args.length > 0)
			dTree = new DecisionTree(args[0]);
		else 
			dTree = new DecisionTree();
		
		// Train and tune on the entire data set, and print the tree
		Log.i(TAG, "Training and tuning on entire data set");
		dTree.trainAndTune(dTree.getVotingData());
		Log.i(TAG, "Tree induced from training and pruning on entire data set:" 
				+ "\n" + dTree);
		
		// Do leave-one-out cross validation
		double louAccuracy = dTree.doLOUCrossValidation();
		DecimalFormat doubleFormat = new DecimalFormat("#.000");
		Log.i(TAG, "Tree accuracy " + doubleFormat.format(louAccuracy) + "%");
	}
	
	/**
	 * Builds a decision tree from the default voting file
	 */
	public DecisionTree() {
		this(VOTING_DATA_FILE);
	}
	
	/**
	 * Builds a decision tree from the specified voting file
	 * @param votingFilePath Path to voting file
	 */
	public DecisionTree(String votingFilePath) {
		mVotingData = Utils.parseFile(votingFilePath);
		mNumIssues = mVotingData[0].getNumFeatures();
		mRootNode = null;
	}
	
	/**
	 * Executes leave-one-out cross validation, and prints the average accuracy
	 * across all instances
	 */
	public double doLOUCrossValidation() {
		Log.i(TAG, "Executing leave-one-out cross validation");
		double totalAccuracy = 0;
		Datum[] testTune = new Datum[mVotingData.length-1];
		// Loop through all data elements, we leave one out each time
		for(int i = 0; i < mVotingData.length; i++) {
			// Inefficient, but easy. Copy over all elements except the ith, 
			// which will constitute our testing set
			for(int j = 0; j < mVotingData.length; j++) {
				if(j < i) {
					testTune[j] = mVotingData[j];
				} else if (j > i) {
					// ith element is not copied over, so offset index down one
					testTune[j-1] = mVotingData[j];
				}
			}
			// Train and tune with the left-one-out data set
			trainAndTune(testTune);
			// Test with the element left out, and increment total accuracy
			totalAccuracy += 
					findTreeAccuracy(new Datum[] {mVotingData[i]});
		}
		// Return average accuracy
		return totalAccuracy / mVotingData.length;
	}
	
	public void trainAndTune(Datum[] data) {
		// Split data into training and tuning sets
		Datum[][] trainTune = buildTrainTuneSets(data);

		// Initialize root with the training data, and start training
		mRootNode = new DTreeNode(trainTune[0]);
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
	private void tuneTree(Datum[] tuningData) {
		boolean morePruning = true;
		// Loop until any more pruning reduce the accuracy of the tree
		while(morePruning) {
			// The best prune will move up the recursive stack. If the best 
			// prune increases accuracy, do the prune. Otherwise end pruning.
			TuneWrapper bestPrune = tuneTreeHelper(mRootNode, tuningData);
			if(bestPrune.bestAccuracy > mTreeAccuracy) {
				DTreeNode pruneNode = bestPrune.bestNodeToPrune;
				pruneNode.setUniform(pruneNode.getMajorityParty());
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
	private TuneWrapper tuneTreeHelper(DTreeNode root, Datum[] tuningData) {
		
		DTreeNode bestNode = root;
		double bestAccuracy;
		// Try pruning this node and calculate new accuracy. Keep track of
		// the old uniform value, and reset to it after accuracy check so that
		// this node will not be considered a leaf upon recursion.
		char oldUniformVal = root.getUniformVal();
		root.setUniform(root.getMajorityParty());
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
		int bestVote = -1;
		Datum[][] bestSplit = null;
		
		int rootDataLength = root.getData().length;
		// Split on every issue, see which will maximize the gain
		for(int i = 0; i < mNumIssues; i++) {
			Datum[][] split = splitDataOnVote(root.getData(), i);
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
				bestVote = i;
				bestSplit = split;
			}
		}
		
		// If no vote splits lead to a gain, then make this node a leaf with 
		// the majority party as its uniform value. Otherwise we'll get an
		// infinite loop of splits on the first vote
		if(bestGain == 0) {
			root.setUniform(root.getMajorityParty());
			return;
		}
		
		// Set this node's vote index
		root.setSplitOn(bestVote);
		
		// Build and set children nodes
		DTreeNode yay = new DTreeNode(bestSplit[0], VOTE_YAY, root);
		DTreeNode nay = new DTreeNode(bestSplit[1], VOTE_NAY, root);
		DTreeNode present = new DTreeNode(bestSplit[2], VOTE_PRESENT, root);
		root.setChildren(new DTreeNode[] {yay, nay, present});
		
		// Recurse
		trainTreeHelper(yay); // yay branch
		trainTreeHelper(nay); // nay branch
		trainTreeHelper(present); // present branch
	}
	
	/**
	 * Separates the specified data into train and tune sets
	 * @param data Data to separate
	 * @return Two lists, first of which is training data, second of which is tuning data
	 */
	private Datum[][] buildTrainTuneSets(Datum[] data) {
		Datum[][] trainTune = new Datum[2][];
		
		// Initialize train and tune sets
		int tuneLength = data.length / TUNE_SET_SPACING;
		if(data.length % TUNE_SET_SPACING != 0) tuneLength++;
		Datum[] tuneSet = new Datum[tuneLength];
		Datum[] trainSet = 
				new Datum[data.length - tuneSet.length];

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
	 * Splits the data on the specified vote, returning lists 
	 * @param data Data to split
	 * @param voteIndex Vote to split on
	 * @return An array of three Representative arrays. Index 0 is the yay votes,
	 *             index 1 is the nay votes, and index 2 is the present votes. 
	 */
	private Datum[][] splitDataOnVote(Datum[] data,
												int voteIndex) 
	{
		Datum[][] splitLists = new Datum[3][];
		
		// On the specified vote, split data into yay, nay and present lists
		ArrayList<Datum> yays = new ArrayList<Datum>();
		ArrayList<Datum> nays = new ArrayList<Datum>();
		ArrayList<Datum> presents = new ArrayList<Datum>();
		for(Datum r : data) {
			char vote= r.getFeature(voteIndex);
			if(vote == VOTE_YAY) yays.add(r);
			else if(vote == VOTE_NAY) nays.add(r);
			else presents.add(r);
		}
		
		// Copy to arrays and add to respective locations in container array
		splitLists[0] = new Datum[yays.size()];
		yays.toArray(splitLists[0]);
		splitLists[1] = new Datum[nays.size()];
		nays.toArray(splitLists[1]);
		splitLists[2] = new Datum[presents.size()];
		presents.toArray(splitLists[2]);
		
		return splitLists;
	}
	
	/**
	 * Calculates the tree accuracy using the specified test data
	 * @param testData Data to test with
	 * @return Percentage of test data that tree correctly labels
	 */
	public double findTreeAccuracy(Datum[] testData) {
		int totalAccurate = 0;
		for(Datum r : testData) {
			DTreeNode curRoot = mRootNode;
			// Loop until we get to a leaf node
			while(!curRoot.isUniform()) {
				int splitOn = curRoot.getSplitOn();
				// Find the appropriate child node, and continue looping
				for(DTreeNode child : curRoot.getChildren()) {
					if(child.getVoteType() == r.getFeature(splitOn))
						curRoot = child;
				}
			}
			// Compare the uniform value (label) of the leaf node against
			// the label of the current Representative. If they're equal,
			// the tree is accurate in this case, so increment counter
			if(curRoot.getUniformVal() == r.getLabel())
				totalAccurate++;
		}
		
		// Return accuracy percentage
		return (double)totalAccurate * 100.0d / testData.length;
	}
	
	public Datum[] getVotingData() {
		return mVotingData;
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
	
	// *******************************
	//  Some static "utility" methods
	// *******************************
	
	/**
	 * Calculates the entropy of the given data array
	 * @param data Data to calculate entropy for
	 * @return Entropy of the specified data set
	 */
	public static double calculateEntropy(Datum[] data) {
		int dCount = 0;
		// Add up number of D party politicians
		for(Datum r : data)
			if(r.getLabel() == PARTY_D) 
				dCount++;
		
		// Calculate probabilities
		double dProb = data.length == 0 ? 0 : (double) dCount / data.length; 
		double rProb = 1.0d - dProb;
		
		// Now plug in to the binary entropy equation
		double entropy = -dProb * log2(dProb) - rProb * log2(rProb);
		
		return entropy;
	}
	
	// Randomly returns PARTY_D or PARTY_R
	public static char randomParty() {
		Random rand = new Random(System.currentTimeMillis());
		int num = rand.nextInt(2);
		if(num == 0) return PARTY_D;
		else return PARTY_R;
	}
	
	/**
	 * Calculates the log base 2 of the argument. log2(0) here is defined to be 0.
	 * With help from: http://www.linuxquestions.org/questions/programming-9/log-base-2-function-in-java-594619/
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