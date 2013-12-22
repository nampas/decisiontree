
/**
 * Represents an input datum for the decision tree
 * @author Nathan P
 *
 */
class Datum {

	private String mIdentifier; // This datum's unique ID
	private char mLabel; // The datum's label
	private String mFeatures; // The datum's features
	
	public Datum(String id, char label, String features) {
		mLabel = label;
		mIdentifier = id;
		mFeatures = features;
	}

	public String getId() {
		return mIdentifier;
	}
	
	public int getNumFeatures() {
		return mFeatures.length();
	}
	
	public String getFeatures() {
		return mFeatures;
	}
	
	public char getFeature(int i) {
		return mFeatures.charAt(i);
	}
	
	public char getLabel() {
		return mLabel;
	}
	
	public boolean equalFeatures(Datum compare) {
		return mFeatures.equals(compare.getFeatures());
	}
}
