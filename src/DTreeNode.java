class DTreeNode {
	
	private final char PARTY_TIE = 'x';

	// If this node contains uniform data (is a leaf), this will be the 
	// uniform label
	private char mUniformVal; 
							 
	private double mEntropy;
	// The vote type that this node splits on, either VOTE_YAY, VOTE_NAY, 
	// VOTE_PRESENT or VOTE_NA
	private char mVoteType; 
	// The index of the vote on which this node splits
	private int mSplitOnVote; 
	private DTreeNode[] mChildren;
	// In case we need to get parent's majority
	private DTreeNode mParent; 
	private Representative[] mData;
	// The majority party at this leaf, or PARTY_TIE if tied
	private char mMajorityParty; 
	
	public char mPruneUniformVal;

	public DTreeNode(Representative[] data) {
		this(data, DecisionTree.VOTE_NA, null);
	}

	/**
	 * Builds a tree node
	 * @param data The data that this node contains
	 * @param voteType The vote type that this node represents, either 
	 *                 NAY, YAY or PRESENT
	 */
	public DTreeNode(Representative[] data, char voteType, DTreeNode parent) {
		mEntropy = DecisionTree.calculateEntropy(data);
		mChildren = new DTreeNode[0];
		mData = data;
		mVoteType = voteType;
		mParent = parent;
		mUniformVal = checkUniformity();
		mMajorityParty = setMajorityParty();
	}

	public Representative[] getData() {
		return mData;
	}

	public double getEntropy() {
		return mEntropy;
	}

	public void setUniform(char uniformVal) {
		mUniformVal = uniformVal;
	}

	/**
	 * Set the vote that this node splits on
	 * @param i The index of the vote on which this node splits
	 */
	public void setSplitOn(int i) {
		mSplitOnVote = i;
	}

	// Sets the specified nodes as the children, overwriting any child
	// associations this node may have already had
	public void setChildren(DTreeNode[] children) {
		mChildren = children;
	}

	/**
	 * Checks if this node is uniform. If so, it returns the uniform label 
	 * (PARTY_D or PARTY_R)
	 * @return PARTY_D or PARTY_R if uniformly labeled, NOT_UNIFORM otherwise 
	 */
	private char checkUniformity() {
		int dCount = 0;
		int rCount = 0;
		for(Representative r : mData) {
			if(r.getParty() == DecisionTree.PARTY_D) dCount++;
			else rCount++;
			// If there exists D and R labels in the node's data set, node is 
			// not uniform
			if(dCount > 0 && rCount > 0) 
				return DecisionTree.NOT_UNIFORM;
		}
		char party;
		if(dCount > 0) {
			party = DecisionTree.PARTY_D;
		} else if(rCount > 0) {
			party = DecisionTree.PARTY_R;
		} else {
			// This means we have an empty data list. This node will become a 
			// leaf whose uniform value is the majority party of the 
			// parent node (or random if no parent)
			party = mParent == null ? 
					DecisionTree.randomParty() : mParent.getMajorityParty();
		}
		return party;
	}

	/**
	 * Calculates and returns the majority party at this node
	 * @return The majority party at this node
	 */
	private char setMajorityParty()	{
		int dCount = 0;
		int rCount = 0;
		for(Representative r : mData) {
			if(r.getParty() == DecisionTree.PARTY_D) dCount++;
			else rCount++;
		}
		if(dCount > rCount) return DecisionTree.PARTY_D;
		else if(rCount > dCount) return DecisionTree.PARTY_R;
		// In case of a tie, pick a random party
		else return PARTY_TIE;
	}
		
	/**
	 * Gets the party with which the majority of this node's data affiliates.
	 * In the case of a tie, we recurse up the tree until we get to a node
	 * which has a majority. Finally, if we've recursed up the tree to the root 
	 * and still haven't found a majority, the tie is broken randomly
	 * @return PARTY_D or PARTY_R
	 */
	public char getMajorityParty() {
		return majorityPartyHelper(this);
	}
	
	/**
	 * Recursive helper for finding majority. This method is "wrapped" with 
	 * getMajorityParty() to make the API cleaner. It would be awkward for 
	 * callers to call this method on a DTreeNode instance, and then require 
	 * them to specify that instance as an argument
	 * @param root Root node of tree, to search up from
	 * @return The majority party at the closest ancestor 
	 */
	private char majorityPartyHelper(DTreeNode root) {
		char val = root.mMajorityParty;
		// If this node ties, recurse up
		if(val == PARTY_TIE) {
			// If no parent, just pick a random party
			if(root.mParent == null)
				val = DecisionTree.randomParty();
			else
				val = majorityPartyHelper(root.mParent);
		}
		return val;
	}

	public DTreeNode[] getChildren() {
		return mChildren;
	}

	// Checks if this is a uniform (leaf) node
	public boolean isUniform() {
		if (mUniformVal == DecisionTree.PARTY_D 
				|| mUniformVal == DecisionTree.PARTY_R)
			return true;
		else
			return false;
	}

	// Returns the uniform value of this node. This is PARTY_D, PARTY_R,
	// or NOT_UNIFORM if the node is not a leaf.
	public char getUniformVal() {
		return mUniformVal;
	}

	// Returns the vote index that this node splits on
	public int getSplitOn() {
		return mSplitOnVote;
	}

	// Returns the vote (YAY, NAY or PRESENT) that this node represents
	public char getVoteType() {
		return mVoteType;
	}

	@Override
	public String toString() {
		// We can take advantage of letter binary representations to convert 
		// from issue number to corresponding issue letter.
		// 'A' + 1 is B, 'A' + 2 is C, etc.
		char a = 'A'; //
		String str = "";
		if(mUniformVal == DecisionTree.NOT_UNIFORM)
			str = (mVoteType == DecisionTree.VOTE_NA ? " " : mVoteType)
				+ " Issue " + (char)(a + mSplitOnVote);
		else 
			str = mVoteType + " " + mUniformVal;
		return str +=  " (" + mData.length + " reps)";
	}
}