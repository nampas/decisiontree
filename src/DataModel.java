import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A data model for the decision tree. This includes a list of data, and the 
 * possible feature values for each feature.
 * @author Nathan P
 *
 */
public class DataModel {

	private Datum[] mData;
	private Character[] mFeatureValues;
	private Character[] mLabels;
	private int mNumFeatures;

	/**
	 * A private DataModel constructor for use by the constructor. The public
	 * should use the builder to instantiate instances of this class
	 * @param data All data in the data model
	 * @param featureValues A set of possible feature values
	 * @param labels A set of labels
	 * @param numFeatures The number of features that each datum contains
	 */
	private DataModel(Datum[] data, 
			Character[] featureValues, 
			Character[] labels,
			int numFeatures) {
		mData = data;
		mFeatureValues = featureValues;
		mLabels = labels;
		mNumFeatures = numFeatures;
	}

	/**
	 * Returns the feature value at the specified index. For each feature, a datum
	 * will be labeled with any one feature value.
	 * @return
	 */
	public Character getFeatureValue(int i) {
		return mFeatureValues[i];
	}

	/**
	 * Returns the number of feature values. This is the number of ways that 
	 * a datum can be labeled for a certain feature 
	 * @return
	 */
	public int getNumFeatureValues() {
		return mFeatureValues.length;
	}

	public Character[] getLabels() {
		return mLabels;
	}
	
	public Character getLabel(int i) {
		return mLabels[i];
	}

	public Datum[] getData() {
		return mData;
	}

	public int getDataSize() {
		return mData.length;
	}
	
	public int getNumFeatures() {
		return mNumFeatures;
	}

	/**
	 * A data model builder
	 * @author Nathan P
	 *
	 */
	public static class Builder {

		ArrayList<Datum> mData;
		private Set<Character> mFeatureValues;
		private Set<Character> mLabels;
		
		private int mNumFeatures;

		public Builder() {
			mData = new ArrayList<Datum>();
			mFeatureValues = new HashSet<Character>();
			mLabels = new HashSet<Character>(2);
			mNumFeatures = -1;
		}

		/**
		 * Adds a datum to the data set. Throws an IllegalStateException if 
		 * this data has a different feature length than other data in the 
		 * data model, or if the addition of this datum increases the label set
		 * above size 2.
		 * @param id The new datum's unique identifier
		 * @param label The new datum's label (classification)
		 * @param feature A string of feature values for the new datum
		 */
		public void addDatum(String id, char label, String features) {
			mData.add(new Datum(id, label, features));
			
			int featureLength = features.length();
			
			// Add features to the feature value set
			for(int i = 0; i < featureLength; i++) {
				mFeatureValues.add(features.charAt(i));
			}

			// Add label to the label set
			mLabels.add(label);
			// Throw an exception if the labeling has exceeded two types
			if(mLabels.size() > 2)
				throw new IllegalStateException("Label types have exceeded size 2. " 
						+ "The decision tree can only classify data models with "
						+ "binary classification");
			
			// Throw an exception if this datum has a different feature size
			if(mNumFeatures == -1)
				mNumFeatures = featureLength;
			else if(mNumFeatures != featureLength)
				throw new IllegalStateException("All data must have the same "
						+ "number of features");
		}

		/**
		 * Instantiates a DataModel from the data specified with addDatum() 
		 * @return A DataModel representing all the data specified by addDatum()
		 */
		public DataModel buildDataModel() {
			// Throw an error if there's only one label. You don't need a
			// decision tree to split this data silly.
			if(mLabels.size() < 2)
				throw new IllegalStateException("The data model contains only "
						+ "one label");

			// Put everything in arrays
			Datum[] data = new Datum[mData.size()];
			mData.toArray(data);
			Character[] featureValues = new Character[mFeatureValues.size()];
			mFeatureValues.toArray(featureValues);
			Character[] labels = new Character[mLabels.size()];
			mLabels.toArray(labels);

			return new DataModel(data, featureValues, labels, mNumFeatures);
		}

	}

	/**
	 * Represents an input datum for the decision tree
	 * @author Nathan P
	 */
	public static class Datum {

		// This datum's unique ID
		private String mIdentifier;

		private char mLabel;
		private String mFeatures;

		/**
		 * Private constructor. This class should only be instantiated with
		 * the builder
		 * @param id The new datum's unique identifier
		 * @param label The new datum's label (classification)
		 * @param feature A string of feature values for the new datum
		 */
		private Datum(String id, char label, String features) {
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
}
