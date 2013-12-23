import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.Scanner;


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
		DataModel dataModel;
		// Build the data model from the specified file or from the default path
		if(args.length > 0)
			dataModel = parseFile(args[0]);
		else 
			dataModel = parseFile(VOTING_DATA_FILE);
		
		DecisionTree dTree = new DecisionTree(dataModel);
		
		// Train and tune on the entire data set, and print the tree
		Log.i(TAG, "Training and tuning on entire data set");
		dTree.trainAndTune(dataModel.getData());
		Log.i(TAG, "Tree induced from training and pruning on entire data set:" 
				+ "\n" + dTree);
		
		// Do leave-one-out cross validation
		double louAccuracy = dTree.doLOUCrossValidation();
		DecimalFormat doubleFormat = new DecimalFormat("#.000");
		Log.i(TAG, "Tree accuracy " + doubleFormat.format(louAccuracy) + "%");
	}
	
	/**
	 * Parses the specified voting file into a data model.
	 * Error checking is minimal, this parser mostly assumes that the file
	 * is "to spec"
	 * @param filePath Path to file
	 * @return  representing the data
	 */
	public static DataModel parseFile(String filePath) {
		Log.i(TAG, "Parsing file at " + filePath);
		File file = new File(filePath);
		
		DataModel.Builder dataBuilder = 
						new DataModel.Builder();
		
		try {
			Scanner scanner = new Scanner(file);
			while(scanner.hasNextLine()) {
				String[] tokens = scanner.nextLine().split(TAB_DELIMITER); // Split on tabs
				dataBuilder.addDatum(tokens[0], tokens[1].charAt(0), tokens[2]); // Assume a correctly formatted file
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		}
		
		return dataBuilder.buildDataModel();
	}
}
