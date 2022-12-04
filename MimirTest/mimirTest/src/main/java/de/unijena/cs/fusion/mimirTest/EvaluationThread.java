package de.unijena.cs.fusion.mimirTest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unijena.cs.fusion.mimir.Document;
import de.unijena.cs.fusion.mimir.MimirSearch;

public class EvaluationThread implements Runnable {
    
	private static final Logger logger = LoggerFactory.getLogger(EvaluationThread.class);

	
	
	String threadName;
	String corpusName;
	String indexURL;
	long totalNumDocsInIndex;
    ArrayList<String> queries;
    HashMap<Integer, List<Rating>> groundTruth;
    String outputPath;
    
    EvaluationThread(String threadName, String corpusName, String indexURL, long totalNumDocsInIndex, ArrayList<String> queries, HashMap<Integer, List<Rating>> groundTruth, String outputPath ) {
        this.threadName = threadName;
    	this.corpusName = corpusName;
        this.totalNumDocsInIndex = totalNumDocsInIndex;
        this.queries = queries;
        this.indexURL = indexURL;
        this.outputPath = outputPath;
        this.groundTruth = groundTruth;
    }

   
	public void run() {
    	
        if(getQueries()!=null && getQueries().size()>0){
     	   int i = 1;
     	  
     	   for(Iterator<String> it = getQueries().iterator();it.hasNext();){
     		   String next = (String) it.next();
     		   
     		   //leave queries out that start with '#' - commented
     		   if(!next.startsWith("#")){
     			   Query query = new Query(i, next);
     		   
     			  logger.info("["+getCorpusName()+ "-" + getThreadName()+ "] Query: " +query.getQuery() +" ***");
     			  
     			  MimirSearch searchSession = new MimirSearch(getIndexURL());
     		       	
     		      ArrayList<Document> docs = searchSession.search(query.getQuery());
     		      
     		       
     		      //save results
     		      try {
					writeToFile(docs, new Integer(query.getID()).toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
     		      
     		      docs.stream().forEach(doc->{
     		       		logger.info("["+getCorpusName()+ "-" + getThreadName()+"] Rank: " + doc.getRank() + ", Score: "+doc.getScore() + ", Title: "+doc.getTitle() + ", Hits: "+ doc.getHits().size());
     		       		
    		       	    		       		
     		      });


     		   }
     		   i++;
     	   }
     	   
     	   try {
     		  
     		logger.info("["+getCorpusName()+ "-" + getThreadName()+"] Computing metrics started ...");
     		
 			
 			logger.info("["+getCorpusName()+ "-" + getThreadName()+"] Computing metrics finished. Results saved in " + getOutputPath()+"\\"+getCorpusName()+"_"+getThreadName()+".csv");
 			
 		} catch (Exception e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
    
         }
         
    }

	/**
	 * write result to a file in TREC format - TOPIC_NO Q0 ID RANK SCORE RUN_NAME
     TOPIC_NO is the topic number (1–30), 
     0 is a required but ignored constant, 
     ID is the identifier of the retrieved document (PMID or NCT ID), 
     RANK is the rank (1–1000) of the retrieved document, 
     SCORE is a floating point value representing the similarity score of the document, 
     and RUN_NAME is an identifier for the run. The RUN_NAME is limited to 12 alphanumeric characters (no punctuation).		       		
	 * @param docs ArrayList<Document>, queryID int
	 * @throws IOException 
	 */
	private void writeToFile(ArrayList<Document> docs, String queryId) throws IOException{
		
		if(docs!=null) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputPath() + "/" + getCorpusName()+"_"+getThreadName()+"_files.txt", true));
		    
		    docs.stream().forEach(doc->{
		       		logger.info("["+getCorpusName()+ "-" + getThreadName()+"] Rank: " + doc.getRank() + ", Score: "+doc.getScore() + ", Title: "+doc.getTitle() + ", Hits: "+ doc.getHits().size());
		       		try {
						writer.append(queryId);
						writer.append(' ');
			    	    writer.append("Q0");
			    	    writer.append(' ');
			    	    writer.append(doc.getTitle());
			    	    writer.append(' ');
			    	    writer.append(new Integer(doc.getRank()).toString());
			    	    writer.append(' ');
			    	    writer.append(new Double(doc.getScore()).toString());
			    	    writer.append(' ');
			    	    writer.append(getCorpusName()+"_"+getThreadName());
			    	    writer.newLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		       		
		    	    
		      });
			
		    writer.close();
		}
	}


	public String getCorpusName() {
		return corpusName;
	}

	public void setCorpusName(String corpusName) {
		this.corpusName = corpusName;
	}

	public long getTotalNumDocsInIndex() {
		return totalNumDocsInIndex;
	}

	public void setTotalNumDocsInIndex(long totalNumDocsInIndex) {
		this.totalNumDocsInIndex = totalNumDocsInIndex;
	}

	public ArrayList<String> getQueries() {
		return queries;
	}

	public void setQueries(ArrayList<String> queries) {
		this.queries = queries;
	}

	public HashMap<Integer, List<Rating>> getGroundTruth() {
		return groundTruth;
	}

	public void setGroundTruth(HashMap<Integer, List<Rating>> groundTruth) {
		this.groundTruth = groundTruth;
	}
	
	public String getIndexURL() {
		return indexURL;
	}

	public void setIndexURL(String indexURL) {
		this.indexURL = indexURL;
	}
	
	 public String getOutputPath() {
			return outputPath;
		}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public String getThreadName() {
		return threadName;
	}


	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	
	
}
