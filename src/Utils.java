import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class Utils {

	private static final String TAG = Utils.class.getSimpleName();
	
	/**
	 * Parses the specified voting file into an array of Representatives.
	 * Error checking is minimal, this parser mostly assumes that the file
	 * is "to spec"
	 * @param filePath Path to file
	 * @return An array of Representatives representing the data
	 */
	public static Datum[] parseFile(String filePath) {
		Log.i(TAG, "Parsing file at " + filePath);
		File file = new File(filePath);
		// We don't know number of representatives in the file, so use ArrayList
		ArrayList<Datum> mDataFromFile = new ArrayList<Datum>();
		try {
			Scanner scanner = new Scanner(file);
			while(scanner.hasNextLine()) {
				String[] tokens = scanner.nextLine().split("\\t"); // Split on tabs
				Datum r = new Datum(tokens[0], tokens[1].charAt(0), tokens[2]); // Assume a correctly formatted file
				mDataFromFile.add(r);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		}
		// Copy data to array and return
		Datum[] dataArray = new Datum[mDataFromFile.size()];
		mDataFromFile.toArray(dataArray);
		return dataArray;
		
	}
}
