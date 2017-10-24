import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class FileOperations {
	static Driver driver;
	static String inputFile="";
	static String outputFile="";
	 FileOperations(Driver driverIn, String inputFileIn,String outputFileIn) {
		// TODO Auto-generated constructor stub
		driver =driverIn;
		inputFile=inputFileIn;
		outputFile=outputFileIn;
	}

	void parseFile() throws Exception {
			
			
			// TODO Auto-generated method stub
			
			BufferedReader reader=new BufferedReader(new FileReader(inputFile));
			String line = reader.readLine();
			while(line!=null && !line.isEmpty()){
				String splitArray[]=line.split(" ");
				int user=Integer.parseInt(splitArray[0]);
				int item =Integer.parseInt(splitArray[1]);
				int rating = Integer.parseInt(splitArray[2]);
				
				
				//put all entries from file into map
				driver.insertEntry(user, item, rating);
				driver.insertMeanEntry(user, item, rating);
				line=reader.readLine();
			}
			
			reader.close();
		}
	
	void writeFile() throws IOException{
		
		
		
		BufferedWriter br = new BufferedWriter(new FileWriter(outputFile));
		for (Integer user:driver.userItemRatings.keySet()) {
			HashMap<Integer,Double> itemRatings=driver.userItemRatings.get(user);
			for (Integer item : itemRatings.keySet()) {
				double rating = itemRatings.get(item);
				br.write(user + " " + item + " " + rating);
				br.write("\n");

			}

			
		}
		br.close();
	
	}

}
