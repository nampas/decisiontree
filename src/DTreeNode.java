import java.util.ArrayList;
import java.util.Random;

/**
 * A node of the decision tree
 * @author Nathan P
 *
 */
class DTreeNode {

	// If this node contains uniform data (is a leaf), this will be the 
	// uniform label. Otherwise, it's null.
	private Character mUniformVal; 
							 
	// The feature type that this node splits on, or null if this is the root
	private Character mFeatureValue;	
	// The index of the feature on which this node splits
	private int mSplitOnFeature; 
	private ArrayList<DTreeNode> mChildren;
	private DTreeNode mParent; 
	private DataModel.Datum[] mData;
	private double mEntropy;
	
	// The majority label at this leaf, or null if tied
	private Character mMajorityLabel; 
	
	// A list of all possible labels
	private Character[] mLabels;

	/**
	 * Builds a root node
	 * @param data The data that this node contains
	 * @param labels A list of possible labels for the data set
	 * @param entropy This node's entropy
	 */
	public DTreeNode(DataModel.Datum[] data, 
					Character[] labels,
					double entropy) 
	{
		this(data, null, null, labels, entropy);
	}

	/**
	 * Builds an intermediary or leaf node
	 * @param data The data that this node contains
	 * @param featureVal The feature value that this node represents
	 * @param parent This node's parent
	 * @param labels A list of possible labels for the data set
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

	/**
	 * Gets the data that this node contains
	 * @return
	 */
	public DataModel.Datum[] getData() {
		return mData;
	}

	/**
	 * Gets this node's entropy
	 * @return
	 */
	public double getEntropy() {
		return mEntropy;
	}

	/**
	 * Set the uniform value of this node
	 * @param uniformVal The uniform value of this node, or null if not uniform
	 */
	public void setUniform(Character uniformVal) {
		mUniformVal = uniformVal;
	}

	/**
	 * Set the feature that this node splits on
	 * @param i The index of the feature on which this node splits
	 */
	public void setSplitOn(int i) {
		mSplitOnFeature = i;
	}

	/**
	 * Adds a child node to this node
	 * @param child A new child node
	 */
	public void addChild(DTreeNode child) {
		mChildren.add(child);
	}
	
	/**
	 * Checks if this node is uniform. If so, it returns the uniform label,
	 * otherwise returns null
	 * @return Uniform label, or null if not uniform
	 */
	private Character checkUniformity() {
		int label1Count = 0;
		int label2Count = 0;
		for(DataModel.Datum d : mData) {
			if(d.getLabel() == mLabels[0]) label1Count++;
			else label2Count++;
			// If both labels exist in the node's data set, node is not uniform
			if(label1Count > 0 && label2Count > 0) 
				return null;
		}
		char label;
		if(label1Count > 0) {
			label = mLabels[0];
		} else if(label2Count > 0) {
			label = mLabels[1];
		} else {
			// This means we have an empty data set. This node will become a 
			// leaf whose uniform value is the majority label of the 
			// parent node (or random if no parent)
			label = (mParent == null) ? 
					randomLabel() : mParent.getMajorityLabel();
		}
		return label;
	}
	
	/**
	 * Returns a random label
	 * @return
	 */
	private char randomLabel() {
		Random rand = new Random(System.currentTimeMillis());
		int num = rand.nextInt(2);
		if(num == 0) return mLabels[0];
		else return mLabels[1];
	}

	/**
	 * Calculates and returns the majority label at this node, or null if the
	 * node labels are tied
	 * @return The majority label at this node, or null if tied
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
	 * Gets the label with which the majority of this node's data affiliates.
	 * In the case of a tie, we recurse up the tree until we get to a node
	 * which has a majority. Finally, if we've recursed up the tree to the root 
	 * and still haven't found a majority, the tie is broken randomly
	 * @return Majority label, or random label in the case of a tie
	 */
	public char getMajorityLabel() {
		return getMajorityLabelHelper(this);
	}
	
	/**
	 * Recursive helper for finding the majority label at a node
	 * @param root Root node of tree
	 * @return The majority label at the closest ancestor with a majority, or a 
	 *         random label if no ancestors have a majority
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

	/**
	 * Gets this node's children
	 * @return
	 */
	public ArrayList<DTreeNode> getChildren() {
		return mChildren;
	}

	/**
	 * Checks if this is a uniform (leaf) node
	 * @return True if node is uniform (a leaf), false otherwise
	 */
	public boolean isUniform() {
		if (mUniformVal == null)
			return false;
		else
			return true;
	}

	/**
	 * Returns the uniform value of this node, or null if this node is not
	 * uniform
	 * @return Uniform value of node, or null if node is not uniform
	 */
	public Character getUniformVal() {
		return mUniformVal;
	}

	/**
	 * Returns the feature index on which this node splits
	 * @return
	 */
	public int getSplitOn() {
		return mSplitOnFeature;
	}

	/**
	 * Returns the feature value that this node represents
	 * @return
	 */
	public Character getFeatureValue() {
		return mFeatureValue;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		if(mUniformVal == null)
			str.append((mFeatureValue == null) ? " " : mFeatureValue
				+ " Feature " + mSplitOnFeature);
		else 
			str.append(mFeatureValue + " " + mUniformVal);
		return str.toString();
	}
}