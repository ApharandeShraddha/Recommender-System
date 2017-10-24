import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Driver {
	
	static FileOperations fieOps;
	static String inputFile;
	static String outputFile;

	 Driver() {
		// TODO Auto-generated constructor stub
		fieOps = new FileOperations(this,inputFile,outputFile);
	}
	
	 // Can we take this from user.
	int noOfUsers=943;
	int noOfItems=1682;
	//int noOfRatings=1682;

	/*int noOfUsers=12;
	int noOfItems=12;*/
	
	
	HashMap<Integer, HashMap<Integer, Double> > userItemRatings = new HashMap<>();
	
	//Missing entry map
	HashMap<Integer, HashMap<Integer, Double> > missingUserItemRatings = new HashMap<>();
	
	HashMap<Integer,Double> meanRatingsUser =new HashMap<>();
	HashMap<Integer,Double> meanRatingsItem =new HashMap<>();
	HashMap<Integer,Integer> totalItemRatings =new HashMap<>();
	HashMap<Integer, HashMap<Integer, Double> > itemUserRatingsReversed = new HashMap<>();
	
	
	HashMap<Integer, HashMap<Integer, Double> > itemSimilarity = new HashMap<>();
	HashMap<Integer, ArrayList<Integer>> neighborMap = new HashMap<>();
	
	

	
	public static void main(String[] args) {
		try {
			 inputFile=args[0];
			 outputFile= "Output.txt";
			 Driver driver=new Driver();
			 System.out.println("Loading the dataset");
			fieOps.parseFile();
			 System.out.println("Training the model");
			driver.populateEntries();
			//Training
			driver.calculateAverageRating();
			driver.adjustItemUserRating();
			driver.findSimilarity();
			driver.neighborItems();
			System.out.println("Evaluating the result...Please wait...");
			driver.evaluate(outputFile);
			System.out.println("Evaluated the result");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void neighborItems() {
		// TODO Auto-generated method stub
		
		Set<Integer> items = itemUserRatingsReversed.keySet();
		Iterator<Integer> itemIterator = items.iterator();
		while (itemIterator.hasNext()) {
		
		   int item = itemIterator.next();
		   
		 
			neighborMap.put(item, findNeighbors(item));
		   
		   
		   
		}
		
		
	}
	
	 // a list, of size at most neighbourhood size, of most similar neighbors of item
	private ArrayList<Integer> findNeighbors (int itemIn)	
	{

		PriorityQueue<int[]> pq = new PriorityQueue<int[]>(1682, new Comparator<int[]>() {
			public int compare(int[] a, int[] b)	{
				double sim_u1 ;
				
				if (a[0] == a[1])
					sim_u1 =1.0;
				else if (a[0] < a[1])
					sim_u1= itemSimilarity.get(a[0]).get(a[1]);
				else
					sim_u1= itemSimilarity.get(a[1]).get(a[0]);
				
				double sim_u2 = getSim(b[0], b[1]);
				if (sim_u1 == sim_u2) 
					return 0;
				else if (sim_u1 < sim_u2)
					return -1;
				else
					return 1;
			}
		});
		
		
		Set<Integer> items = itemUserRatingsReversed.keySet();
		Iterator<Integer> itemIterator = items.iterator();
		while (itemIterator.hasNext()) {
		
		   int itemInner = itemIterator.next();
		   
		   if(itemInner!=itemIn){
			   if (pq.size() < 1682) {
					int[] a=new int[2];
					a[0]=itemIn;
					a[1]=itemInner;
					pq.add(a);
				} else {
					int[] a = pq.peek();
					if (getSim(a[0], a[1]) < getSim(itemIn, itemInner))	{
						pq.remove();
						int[] b=new int[2];;
						
						b[0]=itemIn;
						b[1]=itemInner;
						pq.add(b);
					}
				}
		   }
			   
		}
		
					
		ArrayList<Integer> nbrs = new ArrayList<Integer>();
		for (int[] a : pq)
			nbrs.add(a[1]);
		return nbrs;
	}	

		private double getSim (int u,int v )	
	{
			if (u == v)
				return 1.0;
			else if (u < v)
				return itemSimilarity.get(u).get(v);
			else
				return itemSimilarity.get(v).get(u);
	}	
		
		// Run model on missing data and predict values and write to file.
		public void evaluate ( String predictionFileName ) 
					throws IOException	
		{
			StringBuilder sb=new StringBuilder();
			
			Set<Integer> items = missingUserItemRatings.keySet();
			Iterator<Integer> itemIterator = items.iterator();
			while (itemIterator.hasNext()) {
			
			   int user = itemIterator.next();
			   
				Set<Integer> itemsIn = missingUserItemRatings.get(user).keySet();
				Iterator<Integer> itemIteratorIn = itemsIn.iterator();
				while (itemIteratorIn.hasNext()) {
				
				   int item = itemIteratorIn.next();
		
				   long temp;
			if(missingUserItemRatings.get(user).get(item) ==0.0){
					double predict = predict(user, item);
					
					if ( predict== Double.NEGATIVE_INFINITY)
						predict=3;
				
					
					
					 temp=(long) Math.floor(predict);
			}
			else{
				 temp=(long) Math.floor(missingUserItemRatings.get(user).get(item));
			}
					
					sb.append((String.format("%d %d %d", user, item, temp)));
					sb.append("\n");
				}
			}
			

			writeResult(predictionFileName,sb.toString());
	
		}
		
		void writeResult(String predictionFileName,String lines) throws IOException	{
			BufferedWriter wr = new BufferedWriter(new FileWriter(predictionFileName));
			
			wr.write(lines);
			wr.flush();
			wr.close();
		}
		
		public double predict (	int user, int item )	
		{
			if (!neighborMap.containsKey(item))			
			return Double.NEGATIVE_INFINITY;
			
			double abc = 0.0;
			double norm = 0.0;
			int countRatings = 0;		
			
			for (Integer neighbor : neighborMap.get(item))	{
					 if(itemUserRatingsReversed.containsKey(neighbor) && itemUserRatingsReversed.get(neighbor).containsKey(user)){
						 	HashMap<Integer,Double> temp=itemUserRatingsReversed.get(neighbor);
								double rating = temp.get(user);
								double simi = getSim(item, neighbor);
								abc = abc+ rating * simi;
								norm = norm+ Math.abs(simi);
								countRatings++;
						   }
					 
			}
			
			if (countRatings >= 3){
				double prediction=meanRatingsItem.get(item) + ((abc + 1) / (norm + 1));
				if (prediction > 5.0)
					prediction = 5.0;
				else if (prediction < 1.0)
					prediction = 1.0;
				return prediction;
			}
			return Double.NEGATIVE_INFINITY;
		}
	
		
	
	  //Computes and stores vector cosine similarity between all item pairs 
	 
	private void findSimilarity() {
		// TODO Auto-generated method stub
		
		
		Set<Integer> items = itemUserRatingsReversed.keySet();
		Iterator<Integer> itemIterator = items.iterator();
		while (itemIterator.hasNext()) {
		
		   int item = itemIterator.next();
		   
			Set<Integer> itemsIn = itemUserRatingsReversed.keySet();
			Iterator<Integer> itemIteratorIn = itemsIn.iterator();
			while (itemIteratorIn.hasNext()) {
			
			   int itemIn = itemIteratorIn.next();
			   
			   if(item<itemIn){
				   
				  double similarity=  findVectorCosineSimilarity(item,itemIn);
				  
				  if(itemSimilarity.containsKey(item)){
					  HashMap<Integer,Double> similar=itemSimilarity.get(item);
					  similar.put(itemIn, similarity);
				  }else{
					  HashMap<Integer,Double> similar= new HashMap<>();
					  similar.put(itemIn, similarity);
					  itemSimilarity.put(item, similar);
				  }
				  
			   }
			   
			}
		}
		
		
	}
	
	
	  // the vector cosine similarity between rating vectors of items 
	 
	private double findVectorCosineSimilarity(int item, int itemIn) {
		// TODO Auto-generated method stub

		double number=0;
		   double userOuter=0.0;
		   double itemOuter=0.0;
		   Set<Integer> users = itemUserRatingsReversed.get(item).keySet();
			Iterator<Integer> usersIterator = users.iterator();
			while (usersIterator.hasNext()) {
			
			   int user = usersIterator.next();
			   
			   if(itemUserRatingsReversed.containsKey(itemIn)){
				   if(itemUserRatingsReversed.get(itemIn).containsKey(user)){
					   double ratingUser=  itemUserRatingsReversed.get(item).get(user);
					   double ratingItem=  itemUserRatingsReversed.get(itemIn).get(user);
					 
					 number= number +( ratingUser * ratingItem);
					 userOuter= userOuter+( ratingUser * ratingUser);
					 itemOuter= itemOuter+(ratingItem * ratingItem);
				   }
			   }
			}
			
			userOuter= Math.sqrt(userOuter);
			 itemOuter= Math.sqrt(itemOuter);
			
		double vectorSimilarity=(number + 1) / (userOuter * itemOuter + 1);
		
		return vectorSimilarity;
	}

	
	  // Normalize the transposed training data by subtracting item mean rating for each rating in the data set.
	 
	private void adjustItemUserRating() {
		// TODO Auto-generated method stub
		
		Set<Integer> items = itemUserRatingsReversed.keySet();
		Iterator<Integer> itemIterator = items.iterator();
		while (itemIterator.hasNext()) {
		
		   int item = itemIterator.next();
		   
		   Set<Integer> users = itemUserRatingsReversed.get(item).keySet();
			Iterator<Integer> usersIterator = users.iterator();
			while (usersIterator.hasNext()) {
			
			   int user = usersIterator.next();
			   Map<Integer,Double> userMap=itemUserRatingsReversed.get(item);
			   double adjustedRating=itemUserRatingsReversed.get(item).get(user)-meanRatingsItem.get(item);
			   userMap.put(user, adjustedRating);
			   
			   
			
			}
		   
		
		}


		
		
		
	}

	private void populateEntries(){
		
		missingUserItemRatings.putAll(userItemRatings);
		for (int user = 1; user <= noOfUsers; user++) {
			HashMap<Integer, Double> itemRatings = userItemRatings.get(user);
			if (itemRatings != null) {
				// check itemRatings mapping
				for (int item = 1; item <= noOfItems; item++) {
					if (itemRatings.get(item) == null) {
						HashMap<Integer,Double> temp= new HashMap<Integer,Double>();
						temp.put(item, 0.00);
						if(missingUserItemRatings.containsKey(user)){
							missingUserItemRatings.get(user).put(item, 0.00);
						}
						else
							missingUserItemRatings.put(user, temp);
					}
				}
			} else {
				// entry for missing user
				HashMap<Integer, Double> itemRatingsnew = new HashMap<Integer, Double>();

				for (int item = 1; item <= noOfItems; item++) {
					itemRatingsnew.put(item, 0.00);
					if(missingUserItemRatings.containsKey(user)){
						missingUserItemRatings.get(user).put(item, 0.00);
					}
					else
						missingUserItemRatings.put(user, itemRatingsnew);

				}

				//userItemRatings.put(user, itemRatingsnew);
			}
		}
	}
	
	//check if item ratings entry is present for that user.
	 void insertEntry(int user,int item,double rating) {
		// TODO Auto-generated method stub
		HashMap<Integer,Double> itemRatings= userItemRatings.get(user);
		
		if(itemRatings!=null){
			itemRatings.put(item, rating);
		

		}else{
			HashMap<Integer,Double> itemRatingsNew = new HashMap<Integer,Double>();
			itemRatingsNew.put(item, rating);
			userItemRatings.put(user,itemRatingsNew );
			

		}

		HashMap<Integer,Double> userRatings= itemUserRatingsReversed.get(item);
		
		if(userRatings!=null){
			userRatings.put(user, rating);
		}else{
			HashMap<Integer,Double> userRatingsNew = new HashMap<Integer,Double>();
			userRatingsNew.put(user, rating);
			itemUserRatingsReversed.put(item,userRatingsNew );
		}	
	}
	 
	 void calculateAverageRating(){
		 
		 	//calculate average rating of item
			
			for(Integer userKey:meanRatingsUser.keySet()){
				double totalRating=meanRatingsUser.get(userKey);
				double average=totalRating/userItemRatings.get(userKey).size();
				meanRatingsUser.put(userKey,average);
			}
			
		 	//calculate average rating of user
			
			for(Integer itemKey:meanRatingsItem.keySet()){
				double totalRating=meanRatingsItem.get(itemKey);
				double average=totalRating/totalItemRatings.get(itemKey);
				meanRatingsItem.put(itemKey,average);
			}
			
			
	 }

	 public void insertMeanEntry(int user, int item, double rating) {
			// TODO Auto-generated method stub
		 
		 /*
		  * add average ratings of user
		  */
			if(meanRatingsItem.containsKey(item)){
				double ratingNew=meanRatingsItem.get(item);
				ratingNew+=rating;
				meanRatingsItem.put(item,ratingNew);
				totalItemRatings.put(item, totalItemRatings.get(item) + 1);
			}else{
				meanRatingsItem.put(item,rating);
				totalItemRatings.put(item, 1);
			}
			
			
			
		
			/*
			  * add average ratings of item
			  */
			
			if(meanRatingsUser.containsKey(user)){
				double ratingNew=meanRatingsUser.get(user);
				ratingNew+=rating;
				meanRatingsUser.put(user,ratingNew);
			}else{
				meanRatingsUser.put(user,rating);
			}
			
			
			
	}
	 
	private void displayDataSet(){
		List<String> tempList=new ArrayList<>();
		
		
		
		System.out.println("USer "+ "Item "+"rating ");
		for (Integer user:missingUserItemRatings.keySet()) {
			HashMap<Integer,Double> itemRatings=missingUserItemRatings.get(user);
			for(Integer item:itemRatings.keySet()){
				double rating=itemRatings.get(item);
				if(rating==0){
					String line=user+" "+item+" "+rating;
					tempList.add(line);
				}
			}
		}
		
		Path filee = Paths.get("test.txt");
		
		try {
			Files.write(filee, tempList, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
}
