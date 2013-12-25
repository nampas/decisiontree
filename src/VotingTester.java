import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.Scanner;

/**
 * A main class for testing the decision tree.
 * @author Nathan P
 *
 */
public class VotingTester {

	public static final String TAG = VotingTester.class.getSimpleName();
	
	// Path to default voting data file
	private static final String VOTING_DATA_FILE = "voting-data.tsv";
	
	private static final String TAB_DELIMITER = "\\t";
	
	/**
	 * Main method accepts an argument for the data file location. 
	 * If no argument is provided, the program will use the default voting file
	 * example
	 * @param args
	 */
	public static void main(String[] args) {
		// Build the data model from the specified file or from the default path
		DataModel dataModel = (args.length > 0) ? 
				parseFile(args[0]) : parseFile(VOTING_DATA_FILE);
		
		DecisionTree dTree = new DecisionTree(dataModel);
		
		// Train and tune on the entire data set, and print the tree
		Log.i(TAG, "Training and tuning on entire data set");
		dTree.trainAndTune();
		Log.i(TAG, "Tree induced from training and pruning on entire data set:" 
				+ "\n" + dTree);
		
		// Do leave-one-out cross validation to determine accuracy
		Log.i(TAG, "Executing leave-one-out cross validation");
		double louAccuracy = dTree.doLOUCrossValidation();
		DecimalFormat doubleFormat = new DecimalFormat("#.000");
		Log.i(TAG, "Tree accuracy " + doubleFormat.format(louAccuracy) + "%");
	}
	
	/**
	 * Parses the specified voting file into a data model.
	 * Error checking is minimal, this parser mostly assumes that the file
	 * is "to spec"
	 * @param filePath Path to file
	 * @return A data model representing the file's data
	 */
	public static DataModel parseFile(String filePath) {
		Log.i(TAG, "Parsing file at " + filePath);
		
		File file = new File(filePath);
		DataModel.Builder dataBuilder = new DataModel.Builder();
		try {
			Scanner scanner = new Scanner(file);
			while(scanner.hasNextLine()) {
				// Split on tabs
				String[] tokens = scanner.nextLine().split(TAB_DELIMITER);
				// Ensure we have a complete datum
				if(tokens.length != 3) {
					scanner.close();
					throw new IllegalArgumentException("Illegal line in "
							+ "data file. Each data entry must have a unique "
							+ "identifier, a label, and a string of feaures");
				}
				dataBuilder.addDatum(tokens[0], tokens[1].charAt(0), tokens[2]);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		}

		return dataBuilder.buildDataModel();
	}
}
