package de.unijena.cs.fusion.mimirTest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import de.cs.unijena.fusion.ranking_metrics.Metrics;
import de.cs.unijena.fusion.ranking_metrics.PrecRecF;
import de.cs.unijena.fusion.ranking_metrics.PrecisionAtRank;
import de.cs.unijena.fusion.ranking_metrics.Rating;
import de.cs.unijena.fusion.ranking_metrics.RatingBefchina;
import de.cs.unijena.fusion.ranking_metrics.RatingBioCaddie;
import de.cs.unijena.fusion.ranking_metrics.ResultPerQuery;
import de.unijena.cs.fusion.mimir.Document;
import de.unijena.cs.fusion.mimir.MimirSearch;


public class MimirTest
{
	private static final Logger logger = LoggerFactory.getLogger(MimirTest.class);
	
	/*
	 * BEF-China settings
	 */
	static String INDEX_URL_BEFCHINA="http://localhost:8080/mimir-cloud-6.2-SNAPSHOT/f8ec0055-ab4f-40a0-9b4c-d60d982cbdfd/search/";
	
	
	//GoldStandard BEFChina
	private static String PATH_TO_GROUND_TRUTH_BEFCHINA = "F:\\code\\Evaluation\\befchina\\befchina_gold_standard.txt";
		
	//output folder path Befchina
	private static String OUTPUT_PATH_BEFCHINA = "F:\\code\\RankingResults\\befchina";
			
		
	//path to URI queries Befchina
	private static final String URI_QUERIES_BEFCHINA = "F:\\code\\Evaluation\\befchina\\queries\\URI.txt";
	
	
	//path to narrower queries Befchina
	private static final String URI_NARROWER_QUERIES_BEFCHINA = "F:\\code\\Evaluation\\befchina\\queries\\narrower.txt";
	
	//path to simple broader queries Befchina
private static final String SIMPLE_BROADER_QUERIES_BEFCHINA = "F:\\code\\Evaluation\\befchina\\queries\\broader.txt";
		
	//path to broader queries Befchina
	private static final String CORE_RELATION_QUERIES_BEFCHINA = "F:\\code\\Evaluation\\befchina\\queries\\coreRelation.txt";
	
	private static final String ADAPTED_QUERIES_BEFCHINA = "F:\\code\\Evaluation\\befchina\\queries\\adapted.txt";
		
	
	/*
	 * BioCADDIE settings
	 */
	private static String INDEX_URL_BioCADDIE="http://localhost:8080/mimir-cloud-6.2-SNAPSHOT/68b03370-8acb-4381-863d-b800be17bd5d/search/"; 
	
	//GoldStandard BioCaddie
	private static String PATH_TO_GROUND_TRUTH_BIOCADDIE = "F:\\code\\Evaluation\\biocaddie\\biocaddie_ground_truth.txt";
		
	//output folder path BioCADDIE
	private static String OUTPUT_PATH_BIOCADDIE = "F:\\code\\RankingResults\\biocaddie";
						
	//path to URI queries BioCADDIE
	private static final String URI_QUERIES_BIOCADDIE = "F:\\code\\Evaluation\\biocaddie\\questions\\URI.txt";
			
    //path to narrower queries BioCADDIE
    private static final String URI_NARROWER_QUERIES_BIOCADDIE = "F:\\code\\Evaluation\\biocaddie\\questions\\narrower.txt";
		
	//path to broader queries BioCADDIE
	private static final String URI_BROADER_QUERIES_BIOCADDIE = "F:\\code\\Evaluation\\biocaddie\\questions\\broader.txt";
	
	//path to partOf queries BioCADDIE
	private static final String URI_CORE_RELATIONS_QUERIES_BIOCADDIE = "F:\\code\\Evaluation\\biocaddie\\questions\\coreRelation.txt";
	
	private static final String URI_ADAPTED_QUERIES_BIOCADDIE ="F:\\code\\Evaluation\\biocaddie\\questions\\adapted.txt";
		
	
    
	
	private static ArrayList<String> broaderQueries;
	
	private static ArrayList<String> narrowerQueries;
    
	private static ArrayList<String> URIQueries;
	
	//private static ArrayList<String> URIBroaderQueries;
	
	
	private static ArrayList<String> coreRelationsQueries;
	
	private static ArrayList<String> adaptedQueries;
	
	
	private static HashMap<Integer, List<Rating>> groundTruth = null;
    
    HashMap<Integer, List<Integer>> predictionsPerQuery = null;
    
  //threshold what prediction values to consider as 'relevant'
  	//by default all prediction values larger than 0 are considered as relevant (binary rating)
  	//change this value if you have a Likert-scale (0 - not relevant, 1 - partially relevant, 2 - highly relevant) 
  	//for your predictions and only want to consider, e.g., highly relevant values
  	private static final int RELEVANCE_THRESHOLD = 0;


	
	public static void main( String[] args )
    {
        
        
        if(args[1] != null && args[1].equals("befchina")) {
        	
        	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
            LocalDateTime now = LocalDateTime.now();  
        	System.out.println(dtf.format(now)); 
        	
        	System.out.println("***** "+dtf.format(now)+" - Evaluation with BEF-China test collection started ..... *****");
		
        	if(args.length >0) {
            	OUTPUT_PATH_BEFCHINA = OUTPUT_PATH_BEFCHINA + "\\"+args[0];
            	
            	//init Evaluation, load gold standards and queries
    	    	init(args[1],PATH_TO_GROUND_TRUTH_BEFCHINA, URI_QUERIES_BEFCHINA, URI_NARROWER_QUERIES_BEFCHINA, SIMPLE_BROADER_QUERIES_BEFCHINA, null, null, null,null, PARTOF_QUERIES_BEFCHINA,CORE_RELATION_QUERIES_BEFCHINA,ADAPTED_QUERIES_BEFCHINA);
    	    	
    	    	//Befchina Evaluation
    	    	befchinaEvaluation(args[1]);
    	    	
            	now = LocalDateTime.now();
    	        System.out.println("***** " +dtf.format(now)+" - Evaluation with BEF-China test collection .... done. *****");
            }else {
            	System.err.println("Please provide a valid output path.");
            }
        	
	    	
        }
        else if(args[1] != null && args[1].equals("biocaddie")) {
        	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
            LocalDateTime now = LocalDateTime.now();  
        	System.out.println(dtf.format(now)); 
        	
        	OUTPUT_PATH_BIOCADDIE = OUTPUT_PATH_BIOCADDIE + "\\"+args[0];
        	
        	System.out.println("***** "+dtf.format(now)+" - Evaluation with BioCADDIE test collection started ..... *****");
		
        
	    	//init Evaluation, load gold standards and queries
        	//init(args[1],PATH_TO_GROUND_TRUTH_BIOCADDIE, URI_QUERIES_BIOCADDIE, URI_NARROWER_QUERIES_BIOCADDIE, URI_BROADER_QUERIES_BIOCADDIE, URI_BROADER_AND_NARROWER_QUERIES_BIOCADDIE, SIMPLE_BROADER_QUERIES_BIOCADDIE, NARROWER_OF_BROADER_QUERIES_BIOCADDIE,BROADER_OF_INST_QUERIES_BIOCADDIE);
        	init(args[1],PATH_TO_GROUND_TRUTH_BIOCADDIE, URI_QUERIES_BIOCADDIE, URI_NARROWER_QUERIES_BIOCADDIE, URI_BROADER_QUERIES_BIOCADDIE, null, null, null,null,URI_PART_OF_QUERIES_BIOCADDIE,URI_CORE_RELATIONS_QUERIES_BIOCADDIE,URI_ADAPTED_QUERIES_BIOCADDIE);
	    	
	    	
	    	//BioCaddie Evaluation
	    	biocaddieEvaluation(args[1]);
	    	
        	now = LocalDateTime.now();
	        System.out.println("***** " +dtf.format(now)+" - Evaluation with BioCADDIE test collection .... done. *****");
        }else {
        	System.err.println("The system only supports the following test collection corpora as second argument: 'befchina' (372 files, domain: biodiversity research) or 'bioCaddie' (~7940000, domain: bio-medicine)'");
        }
    }



	private static void befchinaEvaluation(String corpusName) {
		// total number of documents in the index
    	long totalNumDocsInIndex = 372;
    	//String corpusName = "Befchina";
     	
    	
		
		  EvaluationThread simpleURIThread = new EvaluationThread("URIQueries",
		  corpusName, INDEX_URL_BEFCHINA, totalNumDocsInIndex, URIQueries, groundTruth,
		  OUTPUT_PATH_BEFCHINA );
		  
		  new Thread(simpleURIThread).start();
		  
		  EvaluationThread narrowerThread = new EvaluationThread("NarrowerQueries",
		  corpusName, INDEX_URL_BEFCHINA, totalNumDocsInIndex, narrowerQueries,
		  groundTruth, OUTPUT_PATH_BEFCHINA );
		  
		  new Thread(narrowerThread).start();
		  
		  EvaluationThread broaderThread = new EvaluationThread("BroaderQueries",
		  corpusName, INDEX_URL_BEFCHINA, totalNumDocsInIndex, broaderQueries,
		  groundTruth, OUTPUT_PATH_BEFCHINA );
		  
		  new Thread(broaderThread).start();
		  
		  EvaluationThread coreRelationsThread = new EvaluationThread("CoreRelationsQueries", 
				corpusName, INDEX_URL_BEFCHINA, totalNumDocsInIndex, coreRelationsQueries, groundTruth, OUTPUT_PATH_BEFCHINA );       
	      new Thread(coreRelationsThread).start();
	    	
	    	
	      EvaluationThread adaptedThread = new
	   			  EvaluationThread("AdaptedQueries", corpusName, INDEX_URL_BEFCHINA,
	   			  totalNumDocsInIndex, adaptedQueries, groundTruth,
	   			  OUTPUT_PATH_BEFCHINA );
	   			  
	   	  new Thread(adaptedThread).start();
		  
		
	}
	
	/**
	 * BioCaddie Evaluation
	 * please note: as the connection to the git server can be limited, please only run two threads in parallel
	 * @param corpusName
	 */
	private static void biocaddieEvaluation(String corpusName) {
		
		// total number of documents in the index 

    	long totalNumDocsInIndex = 794983;
    	
    	//String corpusName = "BioCaddie";
     	
    	
    	EvaluationThread simpleURIThread = new EvaluationThread("SimpleURIQueries", corpusName, INDEX_URL_BioCADDIE, totalNumDocsInIndex, URIQueries, groundTruth, OUTPUT_PATH_BIOCADDIE );
        
    	new Thread(simpleURIThread).start();
    	
    	EvaluationThread narrowerThread = new EvaluationThread("NarrowerQueries", corpusName, INDEX_URL_BioCADDIE, totalNumDocsInIndex, narrowerQueries, groundTruth, OUTPUT_PATH_BIOCADDIE );
        
    	new Thread(narrowerThread).start();
    	
    	
    	
    	EvaluationThread coreRelationsThread = new EvaluationThread("CoreRelationsQueries", corpusName, INDEX_URL_BioCADDIE, totalNumDocsInIndex, coreRelationsQueries, groundTruth, OUTPUT_PATH_BIOCADDIE );
        
    	new Thread(coreRelationsThread).start();
    	
    	
    	EvaluationThread broaderThread = new EvaluationThread("BroaderQueries", corpusName, INDEX_URL_BioCADDIE, totalNumDocsInIndex, broaderQueries, groundTruth, OUTPUT_PATH_BIOCADDIE );
        
    	new Thread(broaderThread).start();
    	
    	EvaluationThread adaptedThread = new
	   			  EvaluationThread("AdaptedQueries", corpusName, INDEX_URL_BioCADDIE,
	   			  totalNumDocsInIndex, adaptedQueries, groundTruth,
	   			  OUTPUT_PATH_BIOCADDIE );
	   			  
	   			  new Thread(adaptedThread).start();
    	
    	
	}
    
	

	@SuppressWarnings("unchecked")
	private static void init(String corpusName, String pathToGroundTruth, String pathToURIqueries, String pathToNarrowerQueries, String pathToBroaderQueries, String pathToSimpleBroaderQueries, String pathToCoreRelationsQueries,String pathToAdaptedQueries){
    	
		//read the Benchmark Ratings - ground truth
		try (Stream<String> stream = Files.lines(Paths.get(pathToGroundTruth))) {

			if(corpusName.equals("befchina")) {
				//2. convert all content to Rating elements
				//3. convert it into a Map
				groundTruth = (HashMap<Integer, List<Rating>>) stream
					.filter(line -> !"".equals(line))
			        .map(line -> createRatingBefchina(line))
					//.collect(Collectors.toMap(r -> r.getQueryId(), r -> r));
			        .collect(Collectors.groupingBy(Rating::getQueryId));
			}
			else if(corpusName.equals("biocaddie")) {
				//2. convert all content to Rating elements
				//3. convert it into a Map
				List<Rating>groundTruthPreList = (List<Rating>) stream
					.filter(line -> !"".equals(line))
			        .map(line -> createRatingCaddie(line))
				    .collect(Collectors.toCollection(ArrayList::new));
					//.collect(Collectors.toMap(r -> r.getQueryId(), r -> r));
			        //.collect(Collectors.groupingBy(Rating::getQueryId));
				
				groundTruth = (HashMap<Integer, List<Rating>>)groundTruthPreList.stream()
						.filter(x -> x.getRating()>RELEVANCE_THRESHOLD)
						.collect(Collectors.groupingBy(Rating::getQueryId));
				
				System.out.println(groundTruth);
			}
			        
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//read the benchmark queries
		try (Stream<String> stream = Files.lines(Paths.get(pathToNarrowerQueries))) {

			
			narrowerQueries = (ArrayList<String>) stream
					.filter(line -> !"".equals(line))
					.collect(Collectors.toList());
			        
		} catch (IOException e) {
			e.printStackTrace();
		}
		

        
		//read the benchmark queries
		try (Stream<String> stream = Files.lines(Paths.get(pathToURIqueries))) {

			
			URIQueries = (ArrayList<String>) stream
					.filter(line -> !"".equals(line))
					.collect(Collectors.toList());
			        
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//read the benchmark queries
				try (Stream<String> stream = Files.lines(Paths.get(pathToBroaderQueries))) {

					
					broaderQueries = (ArrayList<String>) stream
							.filter(line -> !"".equals(line))
							.collect(Collectors.toList());
					        
				} catch (IOException e) {
					e.printStackTrace();
				}
		
	    
	  //read the benchmark queries
	    if (pathToCoreRelationsQueries != null) {
			try (Stream<String> stream = Files.lines(Paths.get(pathToCoreRelationsQueries))) {
	
						
						coreRelationsQueries = (ArrayList<String>) stream
								.filter(line -> !"".equals(line))
								.collect(Collectors.toList());
						        
					} catch (IOException e) {
						e.printStackTrace();
			}
	    }
	    
	  //read the benchmark queries
	    if (pathToAdaptedQueries != null) {
			try (Stream<String> stream = Files.lines(Paths.get(pathToAdaptedQueries))) {
	
						
						adaptedQueries = (ArrayList<String>) stream
								.filter(line -> !"".equals(line))
								.collect(Collectors.toList());
						        
					} catch (IOException e) {
						e.printStackTrace();
			}
	    }
				
		
		//read the benchmark queries
		if(pathToSimpleBroaderQueries != null) {
			try (Stream<String> stream = Files.lines(Paths.get(pathToSimpleBroaderQueries))) {
	
				
				URIBroaderQueries = (ArrayList<String>) stream
						.filter(line -> !"".equals(line))
						.collect(Collectors.toList());
				        
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
    }
    
    private static Rating createRatingCaddie(String line) {
    	String[] ratingArray = line.split("::");
    	
			return new RatingBioCaddie(
				new Integer(ratingArray[0]).intValue(), //first number is the topic/question number (second entry is just '0')
				new Integer(ratingArray[2]).intValue(), // third entry denotes the document number
				new Integer(ratingArray[3]).intValue()); // fourth number is the prediction
    	

	}



	private static Rating createRatingBefchina(String line){

		String[] ratingArray = line.split("::");
		return new RatingBefchina(
			new Integer(ratingArray[0]).intValue(), //first number is the topic/question
			new Integer(ratingArray[1]).intValue(), //second number is the document number
			new Integer(ratingArray[2]).intValue()); //third number denotes the prediction

    }
    
   
	
	
}
