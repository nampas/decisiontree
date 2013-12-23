import java.util.ArrayList;
import java.util.Random;

class DTreeNode {

	// If this node contains uniform data (is a leaf), this will be the 
	// uniform label
	private Character mUniformVal; 
							 
	private double mEntropy;
	// The feature type that this node splits on, or null if this is the root
	private Character mFeatureValue; 
	
	// The index of the vote on which this node splits
	private int mSplitOnVote; 
	private ArrayList<DTreeNode> mChildren;
	private DTreeNode mParent; 
	private DataModel.Datum[] mData;
	
	// The majority label at this leaf, or null if tied
	private Character mMajorityLabel; 
	
	// A list of all possible labels
	private Character[] mLabels;
	
	public char mPruneUniformVal;

	/**
	 * Builds a root node
	 * @param data
	 * @param labels
	 * @param entropy
	 */
	public DTreeNode(DataModel.Datum[] data, 
					Character[] labels,
					double entropy) 
	{
		this(data, null, null, labels, entropy);
	}

	/**
	 * Builds an intermediary or leaf tree node
	 * @param data The data that this node contains
	 * @param featureVal The feature valuee that this node represents
	 * @param parent This node's parent
	 * @param labels A list of possible labels
	 * @param entropy This node's entropy
	 */
	public DTreeNode(DataModel.Datum[] data, 
					Character featureVal, 
					DTreeNode parent, 
					Character[] labels,
					double entropy) 
	{
		mEntropy = entropy;
		mChildren = new ArrayList<DTreeNode>();
		mData = data;
		mFeatureValue = featureVal;
		mParent = parent;		
		mLabels = labels;
		
		mUniformVal = checkUniformity();
		mMajorityLabel = setMajorityLabel();
	}

	public DataModel.Datum[] getData() {
		return mData;
	}

	public double getEntropy() {
		return mEntropy;
	}

	public void setUniform(Character uniformVal) {
		mUniformVal = uniformVal;
	}

	/**
	 * Set the vote that this node splits on
	 * @param i The index of the vote on which this node splits
	 */
	public void setSplitOn(int i) {
		mSplitOnVote = i;
	}

	public void addChild(DTreeNode child) {
		mChildren.add(child);
	}
	
	/**
	 * Checks if this node is uniform. If so, it returns the uniform label 
	 * (PARTY_D or PARTY_R)
	 * @return PARTY_D or PARTY_R if uniformly labeled, NOT_UNIFORM otherwise 
	 */
	private Character checkUniformity() {
		int label1Count = 0;
		int label2Count = 0;
		for(DataModel.Datum d : mData) {
			if(d.getLabel() == mLabels[0]) label1Count++;
			else label2Count++;
			// If there exists both labels in the node's data set, node is 
			// not uniform
			if(label1Count > 0 && label2Count > 0) 
				return null;
		}
		char party;
		if(label1Count > 0) {
			party = mLabels[0];
		} else if(label2Count > 0) {
			party = mLabels[1];
		} else {
			// This means we have an empty data list. This node will become a 
			// leaf whose uniform value is the majority label of the 
			// parent node (or random if no parent)
			party = (mParent == null) ? 
					randomLabel() : mParent.getMajorityLabel();
		}
		return party;
	}
	
	/**
	 * Randomly returns a label
	 * @return
	 */
	private char randomLabel() {
		Random rand = new Random(System.currentTimeMillis());
		int num = rand.nextInt(2);
		if(num == 0) return mLabels[0];
		else return mLabels[1];
	}

	/**
	 * Calculates and returns the majority party at this node
	 * @return The majority party at this node
	 */
	private Character setMajorityLabel()	{
		int label1Count = 0;
		int label2Count = 0;
		for(DataModel.Datum d : mData) {
			if(d.getLabel() == mLabels[0]) label1Count++;
			else label2Count++;
		}
		if(label1Count > label2Count) return mLabels[0];
		else if(label2Count > label1Count) return mLabels[1];
		else return null;
	}
		
	/**
	 * Gets the party with which the majority of this node's data affiliates.
	 * In the case of a tie, we recurse up the tree until we get to a node
	 * which has a majority. Finally, if we've recursed up the tree to the root 
	 * and still haven't found a majority, the tie is broken randomly
	 * @return PARTY_D or PARTY_R
	 */
	public char getMajorityLabel() {
		return getMajorityLabelHelper(this);
	}
	
	/**
	 * Recursive helper for finding majority. This method is "wrapped" with 
	 * getMajorityParty() to make the API cleaner. It would be awkward for 
	 * callers to call this method on a DTreeNode instance, and then require 
	 * them to specify that instance as an argument
	 * @param root Root node of tree, to search up from
	 * @return The majority party at the closest ancestor 
	 */
	private char getMajorityLabelHelper(DTreeNode root) {
		Character val = root.mMajorityLabel;
		// If this node ties, recurse up
		if(val == null) {
			// If no parent, just pick a random party
			if(root.mParent == null)
				val = randomLabel();
			else
				val = getMajorityLabelHelper(root.mParent);
		}
		return val;
	}

	public ArrayList<DTreeNode> getChildren() {
		return mChildren;
	}

	// Checks if this is a uniform (leaf) node
	public boolean isUniform() {
		if (mUniformVal == null)
			return false;
		else
			return true;
	}

	// Returns the uniform value of this node. This is PARTY_D, PARTY_R,
	// or NOT_UNIFORM if the node is not a leaf.
	public Character getUniformVal() {
		return mUniformVal;
	}

	// Returns the vote index that this node splits on
	public int getSplitOn() {
		return mSplitOnVote;
	}

	// Returns the feature value that this node represents
	public Character getFeatureValue() {
		return mFeatureValue;
	}

	@Override
	public String toString() {
		// We can take advantage of letter binary representations to convert 
		// from issue number to corresponding issue letter.
		// 'A' + 1 is B, 'A' + 2 is C, etc.
		char a = 'A'; //
		String str = "";
		if(mUniformVal == null)
			str = (mFeatureValue == null) ? " " : mFeatureValue
				+ " Feature " + (char)(a + mSplitOnVote);
		else 
			str = mFeatureValue + " " + mUniformVal;
		return str +=  " (" + mData.length + " data)";
	}
}