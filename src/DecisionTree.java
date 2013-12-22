import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

class DecisionTree {

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
	
	private Representative[] mVotingData;
	private int mNumIssues; // Number of issues in the data set
	private DTreeNode mRootNode;
	private double mTreeAccuracy;
	
	// Specifies how many spaces to skip before adding next element to tune set
	private int TUNE_SET_SPACING = 4; 
	
	// The natural log of 2. Pre-calculating this makes log2() a bit more efficient
	private static final double NAT_LOG_2 = 0.69314718056d;
	
	/**
	 * Main method accepts an argument for the voting data file location. If no argument is provided,
	 * the program will use a default file
	 * @param args
	 */
	public static void main(String[] args) {
		DecisionTree dTree;
		if(args.length > 0)
			dTree = new DecisionTree(args[0]);
		else 
			dTree = new DecisionTree();
		
		// Train and tune on the entire data set, and print the tree
		System.out.println("Training and tuning on entire data set");
		dTree.trainAndTune(dTree.getVotingData());
		System.out.println("Tree induced from training and pruning on entire data set:\n" + dTree);
		
		// Do leave-one-out cross validation, and print accuracy
		double louAccuracy = dTree.doLOUCrossValidation();
		DecimalFormat doubleFormat = new DecimalFormat("#.000"); // With help from: http://stackoverflow.com/questions/8895337/how-do-i-limit-the-number-of-decimals-printed-for-a-double
		System.out.println("Tree accuracy " + doubleFormat.format(louAccuracy) + "%");
	}
	
	/**
	 * Builds a decision tree from the default voting file
	 */
	public DecisionTree() {
		this(VOTING_DATA_FILE);
	}
	
	/**
	 * Builds a decision tree from the specified voting file
	 * @param votingFilePath
	 */
	public DecisionTree(String votingFilePath) {
		mVotingData = parseFile(votingFilePath);
		mNumIssues = mVotingData[0].getVotes().length();
		mRootNode = null;
	}
	
	/**
	 * Executes leave-one-out cross validation, and prints out the average accuracy
	 * across all instances
	 */
	public double doLOUCrossValidation() {
		System.out.println("Doing leave-one-out cross validation");
		double totalAccuracy = 0;
		Representative[] testTune = new Representative[mVotingData.length-1];
		// Loop through all data elements, we leave one out each time
		for(int i = 0; i < mVotingData.length; i++) {
			// Inefficient, but easy. Copy over all elements except the ith, which
			// will constitute our testing set
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
			totalAccuracy += findTreeAccuracy(new Representative[] {mVotingData[i]});
		}
		// Return average accuracy
		return totalAccuracy / mVotingData.length;
	}
	
	public void trainAndTune(Representative[] data) {
		// Split data into training and tuning sets
		Representative[][] trainTune = buildTrainTuneSets(data);

		// Initialize root with the training data, and start training
		mRootNode = new DTreeNode(trainTune[0]);
		trainTreeHelper(mRootNode);

		// Find the unpruned accuracy
		mTreeAccuracy = findTreeAccuracy(trainTune[1]);
		System.exit(1);
		
		// Tune the induced tree.
		// Note: I have two tuning methods. tuneTreeClassNotes() implement the algorithm
		// described in the class slides. tuneTreeWiki() implements the algorithm described
		// on wikipedia. Both algorithms seem to result in the same tree: accuracies are equal.
		// Feel to try them both 
		tuneTreeClassNotes(trainTune[1]);
//		tuneTreeWiki(mRootNode, trainTune[1]);
	}
	
	/**
	 * Does reduced-error pruning on the tree, using the method described in the CS431
	 * class notes
	 * @param root 
	 * @param tuningData
	 */
	private void tuneTreeClassNotes(Representative[] tuningData) {
		boolean morePruning = true;
		// Loop until any more pruning reduce the accuracy of the tree
		while(morePruning) {
			// The best prune will move up the recursive stack. If the best prune increases
			// accuracy, do the prune. Otherwise, end pruning.
			TuneWrapper bestPrune = tuneClassNotesHelper(mRootNode, tuningData);
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
	 * Recursive helper method for doing reduced-error pruning using the method described in the CS431
	 * class notes
	 * @param root Current root node
	 * @param tuningData The data to tune on
	 * @return The best node to prune from the root's subtree. This includes the root itself
	 */
	private TuneWrapper tuneClassNotesHelper(DTreeNode root, Representative[] tuningData) {
		
		DTreeNode bestNode = root;
		double bestAccuracy;
		// Try pruning this node, and calculate new accuracy. Keep track of the old
		// uniform val, and reset to it after accuracy check so that upon recursion
		// this node will not be considered a leaf
		char oldUniformVal = root.getUniformVal();
		root.setUniform(root.getMajorityParty());
		bestAccuracy = findTreeAccuracy(tuningData);
		root.setUniform(oldUniformVal);

		// Recurse to children
		for(DTreeNode child : root.getChildren()) {
			TuneWrapper result = tuneClassNotesHelper(child, tuningData);
			// If any child branch returns a better accuracy, pass it on. 
			if(bestAccuracy < result.bestAccuracy) {
				bestNode = result.bestNodeToPrune;
				bestAccuracy = result.bestAccuracy;
			}
		}
		return new TuneWrapper(bestNode, bestAccuracy);
	}

	/**
	 * Recursive method for doing reduced-error pruning on the tree. This implements
	 * the algorithm described on Wikipedia: "Starting at the leaves, each node is 
	 * replaced with its most popular class. If the prediction accuracy is not affected 
	 * then the change is kept." http://en.wikipedia.org/wiki/Pruning_%28decision_trees%29
	 * @param root Current root node
	 * @param tuningData The data on which to tune
	 */
	private void tuneTreeWiki(DTreeNode root, Representative[] tuningData) {
		// End recursion on leaves
		if(root.isUniform()) return;
		
		// Recurse to end of branches first. We want to start at the direct parent
		// nodes of leaves, and move up the tree from there
		for(DTreeNode child : root.getChildren()) {
			tuneTreeWiki(child, tuningData);
		}
		
		// Change this node to a leaf, with a uniform value of the majority
		root.setUniform(root.getMajorityParty());
		
		// Now check if we have better accuracy on the tree. If so, keep the change,
		// otherwise revert this node to its existing state
		double newAccuracy = findTreeAccuracy(tuningData);
		if(newAccuracy > mTreeAccuracy) {
			mTreeAccuracy = newAccuracy;
			root.setChildren(new DTreeNode[0]); // Is now leaf, get rid of children
		} else {
			root.setUniform(NOT_UNIFORM);
		}		
	}
	
	/**
	 * Recursive helper method for training the tree
	 * @param root Current root node
	 */
	public void trainTreeHelper(DTreeNode root) {
		
		// End recursion when we've reached a uniformly labeled node
		if(root.isUniform())
			return;

		double bestGain = -1;
		int bestVote = -1;
		Representative[][] bestSplit = null;
		int rootDataLength = root.getData().length;
		// Split on every issue, see which will maximize the gain
		for(int i = 0; i < mNumIssues; i++) {
			Representative[][] split = splitDataOnVote(root.getData(), i);
			double currentEntropy = 0;
			for(int j = 0; j < split.length; j++) {
				// Add weighted entropy of this subset to running total
				currentEntropy += ((double)split[j].length / rootDataLength) * calculateEntropy(split[j]);
			}
			// Subtract the post-split weighted entropy from this node's entropy to calculate gain
			double gain = root.getEntropy() - currentEntropy;
			System.out.println(gain);
			
			// If this gain is better, update best-so-far
			if(gain > bestGain) {
				bestGain = gain;
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
	private Representative[][] buildTrainTuneSets(Representative[] data) {
		Representative[][] trainTune = new Representative[2][];
		
		// Initialize train and tune sets
		int tuneLength = data.length / TUNE_SET_SPACING;
		if(data.length % TUNE_SET_SPACING != 0) tuneLength++;
		Representative[] tuneSet = new Representative[tuneLength];
		Representative[] trainSet = new Representative[data.length - tuneSet.length];

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
	private Representative[][] splitDataOnVote(Representative[] data, int voteIndex) {
		Representative[][] splitLists = new Representative[3][];
		
		// On the specified vote, split data into yay, nay and present lists
		ArrayList<Representative> yays = new ArrayList<Representative>();
		ArrayList<Representative> nays = new ArrayList<Representative>();
		ArrayList<Representative> presents = new ArrayList<Representative>();
		for(Representative r : data) {
			char vote= r.getVote(voteIndex);
			if(vote == VOTE_YAY) yays.add(r);
			else if(vote == VOTE_NAY) nays.add(r);
			else presents.add(r);
		}
		
		// Copy to arrays and add to respective locations in container array
		splitLists[0] = new Representative[yays.size()];
		yays.toArray(splitLists[0]);
		splitLists[1] = new Representative[nays.size()];
		nays.toArray(splitLists[1]);
		splitLists[2] = new Representative[presents.size()];
		presents.toArray(splitLists[2]);
		
		return splitLists;
	}
	
	/**
	 * Calculates the tree accuracy using the specified test data
	 * @param testData Data to test with
	 * @return Percentage of test data that tree correctly labels
	 */
	public double findTreeAccuracy(Representative[] testData) {
		int totalAccurate = 0;
		for(Representative r : testData) {
			DTreeNode curRoot = mRootNode;
			// Loop until we get to a leaf node
			while(!curRoot.isUniform()) {
				int splitOn = curRoot.getSplitOn();
				// Find the appropriate child node, and continue looping
				for(DTreeNode child : curRoot.getChildren()) {
					if(child.getVoteType() == r.getVote(splitOn))
						curRoot = child;
				}
			}
			// Compare the uniform value (label) of the leaf node against
			// the label of the current Representative. If they're equal,
			// the tree is accurate in this case, so increment counter
			if(curRoot.getUniformVal() == r.getParty())
				totalAccurate++;
		}
		
		// Return accuracy percentage
		return (double)totalAccurate * 100.0d / testData.length;
	}
	
	public Representative[] getVotingData() {
		return mVotingData;
	}
	
	/**
	 * Parses the specified voting file into an array of Representatives.
	 * Error checking is minimal, this parser mostly assumes that the file
	 * is "to spec"
	 * @param filePath Path to file
	 * @return An array of Representatives representing the data
	 */
	private Representative[] parseFile(String filePath) {
		System.out.println("Parsing file at " + filePath);
		File file = new File(filePath);
		// We don't know number of representatives in the file, so use ArrayList
		ArrayList<Representative> mDataFromFile = new ArrayList<Representative>();
		try {
			Scanner scanner = new Scanner(file);
			while(scanner.hasNextLine()) {
				String[] tokens = scanner.nextLine().split("\\t"); // Split on tabs
				Representative r = new Representative(tokens[0], tokens[1].charAt(0), tokens[2]); // Assume a correctly formatted file
				mDataFromFile.add(r);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found at " + filePath);
			e.printStackTrace();
		}
		// Copy data to array and return
		Representative[] dataArray = new Representative[mDataFromFile.size()];
		mDataFromFile.toArray(dataArray);
		return dataArray;
		
	}
	
	@Override
	public String toString() {
		return stringHelper(mRootNode, 1);
	}
	
	// Recursive helper for building string representations of this DecisionTree
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
	public static double calculateEntropy(Representative[] data) {
		int dCount = 0;
		// Add up number of D party politicians
		for(Representative r : data)
			if(r.getParty() == PARTY_D) 
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