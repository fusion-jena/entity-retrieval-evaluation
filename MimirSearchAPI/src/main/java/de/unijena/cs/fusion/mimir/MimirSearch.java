package de.unijena.cs.fusion.mimir;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.ws.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
//import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MimirSearch {
	
	private static final Logger logger = LoggerFactory.getLogger(MimirSearch.class);

	public String INDEX_URL;
	
	private String DOCSCOUNT_URL;

	private String QUERY_URL;

	private String DOCUMENT_HITS_URL;

	private String DOCUMENT_METADATA_URL;

	private String DOCUMENT_SCORE_URL;
	
	private String CLOSE_URL;

	private URI RENDER_DOCUMENT_URL = null;
	
	public String query;
	public String queryId;
	public long docsCount; //number o f retrieved documents
	public int maxDocs = 1000; //maximum number of documents to be considered for ranking (default: 50), '0' means - consider all results, no maxDocs
	
	int timeoutSeconds = 10;
	
    RequestConfig requestConfig;
	
	public MimirSearch(String indexURL){
		
		this.INDEX_URL = indexURL;
		
		DOCSCOUNT_URL = INDEX_URL + "documentsCount";

		QUERY_URL = INDEX_URL + "postQuery";

		DOCUMENT_HITS_URL = INDEX_URL + "documentHits";

		DOCUMENT_METADATA_URL = INDEX_URL + "documentMetadata";

		DOCUMENT_SCORE_URL = INDEX_URL + "documentScore";
		
		CLOSE_URL = INDEX_URL + "close";
		
		int CONNECTION_TIMEOUT_MS = timeoutSeconds * 1000; // Timeout in millis.
	    
		requestConfig = RequestConfig.custom()
	           .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
	           .setConnectTimeout(CONNECTION_TIMEOUT_MS)
	           .setSocketTimeout(CONNECTION_TIMEOUT_MS)
	           .build();
		
	}
	
	
	public ArrayList<Document> search(String query){
		ArrayList<Document> documents = new ArrayList<Document>();
		
		this.query = query;
		
		logger.info("***** POST "+QUERY_URL+": queryString:'"+query +"'****");
		
		try {
             //1.) post the query and get the session ID
			 String postQueryResult = postQueryAsync(QUERY_URL, this.query);
			
			//String postQueryResult= postQuery(QUERY_URL, this.query);
			 this.queryId = readQueryIdFromXML(postQueryResult);
            
			 //it can take a couple of seconds, sometimes minutes for the statement to come back
			 //sleep and try it again after 5 seconds
             if (this.queryId == null || this.queryId.length() == 0){
            	 
            	 try
            	 {
            	     Thread.sleep(5*1000); // try it again after 5 seconds
            	 }
            	 catch(InterruptedException ex)
            	 {
            	     Thread.currentThread().interrupt();
            	 }
            	 //second try 
            	 postQueryResult= postQueryAsync(QUERY_URL, this.query);
            	 this.queryId = readQueryIdFromXML(postQueryResult);
             }
             
            	              
             logger.info("queryId: "+ this.queryId);
            	 
	         //2.) next, get the number of documents that match the query = docsCount
	         this.docsCount = documentsCountAsync(DOCSCOUNT_URL, this.queryId);
	         
	         //while docsCount == -1 poll every 5 sec (query still running)
	         while (this.docsCount < 0){
	        	 
		         try
            	 {
            	     Thread.sleep(5 * 1000); // try it every 5 seconds until result is either 0 (no documents) result is > 0
            	 }
            	 catch(InterruptedException ex)
            	 {
            	     Thread.currentThread().interrupt();
            	 }
            	 //try it again
		         this.docsCount = documentsCountAsync(DOCSCOUNT_URL,this.queryId);
            	 
             }
	         
	         logger.info("docsCount: "+ this.docsCount);
	         
	         //3.) now, interate through all ranks and get the hits per document
	           
	        if (this.docsCount >0){
	        	    int maxCount = new Long(docsCount).intValue(); //by default consider all retrieved documents for ranking
	        	    
	        	    //but if we have a large corpus and docsCount is larger than the max number of documents to be scored
	        	    //limit the scoring of maxDocs
	        		if(this.maxDocs>0 && this.docsCount > this.maxDocs) {
	        			maxCount = this.maxDocs;
	        		}
	            	             	    	 
	            	 for (int i=0; i< maxCount; i++){
	            		 
	            		 Document doc = new Document();
	            		 doc.setRank(i);
	            		 String rankS = String.valueOf(i);
	            		 
	            		 //get the hits per documents
	            		 
	            		 ArrayList<Hit> hitsPerRank = documentHits(this.queryId, rankS);
	            		 doc.setHits(hitsPerRank);
	            		// ToDo: collect the other document data: title, text snippets, text for highlighting
	                    // parallel async calls
	            		 Metadata documentMetadata = documentMetadataAsync(this.queryId, rankS);
	            		 if(documentMetadata.getTitle()!=null)
	            			 doc.setDocumentID(documentMetadata.getDocumentId());
	            		 
	            		 if(documentMetadata.getTitle()!=null)
	            			 doc.setTitle(documentMetadata.getTitle());
	            		 
	            		 double score = documentScore(this.queryId, rankS);
	            		 
	            		 if(score > 0.0)
	            			 doc.setScore(score);
	            		 
	            		 documents.add(doc);
	            		 
	            	 }
	             }
            
             // 4.) close the session
             close(this.queryId);
            
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
		return documents;
	}
	
	/**
	 * helper method to read query from xml result
	 * @param postQueryResult
	 * @return
	 */
	private String readQueryIdFromXML(String postQueryResult) {
		String queryId = "";
		
		if (postQueryResult == null) {
	         return queryId;
	      } else {
	    	  String pattern = "[0-9,A-Z,a-z,-]{36}";
	          Pattern r = Pattern.compile(pattern);  
	          Matcher m = r.matcher(postQueryResult);
	          if (m.find( )) {
	        	       
	        	  queryId = m.group(0);
	        	  
	          	  return queryId;
	          }else {
	              logger.error(postQueryResult);
	          }
	      }
	      return queryId;
	}


	/**
	 * posts query to Mimir URL
	 * @param queryString as String
	 * @return queryID as String
	 * @throws IOException
	 */
	public String postQuery(String url, String query) throws IOException{
		String result = "";
		String urlString = url + "?queryString="+encode(query);
		
		logger.info(urlString);
		
        HttpGet post = new HttpGet(urlString);
        
       

        // add request parameters or form parameters
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("queryString", encode(query)));

        //post.setEntity(new UrlEncodedFormEntity(urlParameters));

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
        		
             CloseableHttpResponse response = httpClient.execute(post)){
              
        	logger.info(result);
        	
        	result = EntityUtils.toString(response.getEntity());
            
            httpClient.close();
        }

        return result;
	}
	
	private String postQueryAsync (String url, String query) throws IOException, InterruptedException, ExecutionException {
		String result = "";
		
		CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
     try {
         httpclient.start();
         String urlString = url + "?queryString=" + encode(query);
         logger.info(urlString);
         HttpGet request = new HttpGet(urlString);
         
         Future<HttpResponse> future = httpclient.execute(request, null);
         HttpResponse response = future.get();
         
         result = EntityUtils.toString(response.getEntity());
         logger.info("Response Status: " + response.getStatusLine());
         logger.info("Response: " + result);
         logger.info("Shutting down");
     } finally {
         httpclient.close();
     }
     logger.info("Done");
     
     return result;
	}
     
	// Method to encode a string value using `UTF-8` encoding scheme
    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }


    public long documentsCountAsync(String url, String queryId) throws IOException, InterruptedException, ExecutionException, TimeoutException{
       long docsCount = -1;
		
		if (queryId!=null && queryId!=""){
			
			 CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
		     try {
		         httpclient.start();
		         String urlString = url + "?queryId=" + queryId;
		         logger.info(urlString);
		         HttpGet request = new HttpGet(urlString);
	     
			        
			        Future<HttpResponse> future = httpclient.execute(request, null);
			      
		         
		 
		          while(!future.isDone()) {
		                System.out.println("Task completion in progress...");
		                Thread.sleep(500);
		            }
		            
		            System.out.println("Task completed!");
		         
		            HttpResponse response = future.get(10, TimeUnit.SECONDS);
		            String result = EntityUtils.toString(response.getEntity());
		            docsCount = readDocsCountFromXML(result);
		         
		   
		         
		         //logger.info("Response Status: " + response.getStatusLine());
		         //logger.info("Response: " + result);
		         logger.info("Shutting down");
		     } finally {
		         httpclient.close();
		     }
		     logger.info("Done");
		     		
			}
		
		return docsCount;
	}
    
	public long documentsCount(String queryId) throws IOException{
		long docsCount = -1;
		
		if (queryId!=null && queryId!=""){
			
			 HttpPost post = new HttpPost(DOCSCOUNT_URL);
		

		        post.setConfig(requestConfig);
			 
			// add request parameters or form parameters
	        List<NameValuePair> urlParameters = new ArrayList<>();
	        urlParameters.add(new BasicNameValuePair("queryId", queryId));

	        post.setEntity(new UrlEncodedFormEntity(urlParameters));

	        try (CloseableHttpClient httpClient = HttpClients.createDefault();
	             CloseableHttpResponse response = httpClient.execute(post)){

	            String result = EntityUtils.toString(response.getEntity());
	            
	            logger.info(result);
	            
	            docsCount = readDocsCountFromXML(result);
	            
	            httpClient.close();
	        }

	       

			
		}
		return docsCount;
	}


	private long readDocsCountFromXML(String getDocsCountResult) {
		long docsCount = -1;
		
		if (getDocsCountResult == null) {
	         return docsCount;
	      } else {
	    	  String pattern = "<value>([0-9]+)<\\/value>";
	          Pattern r = Pattern.compile(pattern);  
	          Matcher m = r.matcher(getDocsCountResult);
	          if (m.find( )) {
	        	       
	        	  String count = m.group(1);
	        	  if(count!=null) {
	        		  docsCount = Long.valueOf(count);
	        	  }
	        	  
	          	  return docsCount;
	          }else {
	              logger.error("No docsCount found in "+ getDocsCountResult);
	          }
	      }
	      return docsCount;	
	}
	
	
	private ArrayList<Hit> documentHits(String queryId, String rank) throws IOException{
		ArrayList<Hit> hits = new ArrayList<Hit>();
		
		if (queryId!=null && queryId!=""){
			
			 HttpPost post = new HttpPost(DOCUMENT_HITS_URL);
			 
			 /*int CONNECTION_TIMEOUT_MS = timeoutSeconds * 1000; // Timeout in millis.
		        RequestConfig requestConfig = RequestConfig.custom()
		            .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
		            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
		            .setSocketTimeout(CONNECTION_TIMEOUT_MS)
		            .build();*/

		        //post.setConfig(requestConfig);
			 
			// add request parameters or form parameters
	        List<NameValuePair> urlParameters = new ArrayList<>();
	        urlParameters.add(new BasicNameValuePair("queryId", queryId));
	        urlParameters.add(new BasicNameValuePair("rank", rank));

	        post.setEntity(new UrlEncodedFormEntity(urlParameters));

	        try (CloseableHttpClient httpClient = HttpClients.createDefault();
	             CloseableHttpResponse response = httpClient.execute(post)){

	            String result = EntityUtils.toString(response.getEntity());
	            
	            hits = readHitsFromXML(result);
	            
	            httpClient.close();
	        }

	       

			
		}
		return hits;
	}


	private ArrayList<Hit> readHitsFromXML(String hitsFromXML) {

	     ArrayList<Hit> hits = new ArrayList<Hit>();

	      if (hitsFromXML == null) {
	              logger.error ("readHitsFromXML: result is null");
	      } else {
	                  
	    	  String patternHits = "<hits>.+<\\/hits>";
	          Pattern r = Pattern.compile(patternHits);  
	          Matcher m = r.matcher(hitsFromXML);
	          if (m.find( )) {
	        	       
	        	  String temp = m.group(0); //all hits in a list
	        	  
	        	  String patternHit = "(<hit\\s(documentId='[0-9]+')\\s(termPosition='[0-9]+')\\s(length='[0-9]+')\\/>)";
	        	  /**
                   * hitsArray[0] - full string of characters matched
                   * hitsArray[1] - first substring match: (<hit ... />)
                   * hitsArray[2] - second substring match: (documentId='[0-9]+')
                   * hitsArray[3] - third substring match: (termPosition='[0-9]+')
                   * hitsArray[4] - fourth substring match: (length='[0-9]+')
                   */
	        	  Pattern rHit = Pattern.compile(patternHit);
	        	  Matcher mHit = rHit.matcher(temp);
	        	  
	        	  while(mHit.find()){
	        		  Hit hit = new Hit();
                      hit.setDocumentId(mHit.group(2));
                      hit.setTermPosition(mHit.group(3));
                      hit.setLength(mHit.group(4));
                      
                      hits.add(hit);
	        	  }
	          	  
	          }else {
	              logger.error(hitsFromXML);
	          }
	      }  
	    	
	     return hits;
	}
	

	private Metadata documentMetadata(String queryId, String rank) throws IOException{
		
		Metadata metadata = new Metadata();
		
		if (queryId!=null && queryId!=""){
			
			 HttpPost post = new HttpPost(DOCUMENT_METADATA_URL);
			 
			 /*int CONNECTION_TIMEOUT_MS = timeoutSeconds * 1000; // Timeout in millis.
		        RequestConfig requestConfig = RequestConfig.custom()
		            .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
		            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
		            .setSocketTimeout(CONNECTION_TIMEOUT_MS)
		            .build();*/

		        post.setConfig(requestConfig);
			 
			 
			// add request parameters or form parameters
	        List<NameValuePair> urlParameters = new ArrayList<>();
	        urlParameters.add(new BasicNameValuePair("queryId", queryId));
	        urlParameters.add(new BasicNameValuePair("rank", rank));

	        post.setEntity(new UrlEncodedFormEntity(urlParameters));

	        try (CloseableHttpClient httpClient = HttpClients.createDefault();
	             CloseableHttpResponse response = httpClient.execute(post)){

	            String result = EntityUtils.toString(response.getEntity());
	            
	            metadata.setTitle(readTitleFromXML(result));
	            metadata.setDocumentId(readDocumentIdFromResult(result));
	            
	            httpClient.close();
	        }

	       

			
		}
		return metadata;
	}

private Metadata documentMetadataAsync(String queryId, String rank) throws IOException{
		
		Metadata metadata = new Metadata();
		
		if (queryId!=null && queryId!=""){
			HttpPost post = new HttpPost(DOCUMENT_METADATA_URL);
			

	        //post.setConfig(requestConfig);
		 
		 
			// add request parameters or form parameters
	        List<NameValuePair> urlParameters = new ArrayList<>();
	        urlParameters.add(new BasicNameValuePair("queryId", queryId));
	        urlParameters.add(new BasicNameValuePair("rank", rank));
	
	        post.setEntity(new UrlEncodedFormEntity(urlParameters)); 
			CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
		     try {
		         httpclient.start();
		         //String urlString = url + "?queryId=" + queryId;
		         //logger.info(urlString);
		         //HttpPost request = new HttpPost(urlString);
	     
			        
			        Future<HttpResponse> future = httpclient.execute(post, null);
			      
		         
		 
		          /*while(!future.isDone()) {
		                System.out.println("Task completion in progress...");
		                Thread.sleep(500);
		            }
		            
		            System.out.println("Task completed!");*/
		         
		            //HttpResponse response = future.get(10, TimeUnit.SECONDS);
			        HttpResponse response = future.get();
			        String result = EntityUtils.toString(response.getEntity());
		            
		            metadata.setTitle(readTitleFromXML(result));
		            metadata.setDocumentId(readDocumentIdFromResult(result));
		   
		         
		         //logger.info("Response Status: " + response.getStatusLine());
		         //logger.info("Response: " + result);
		        // logger.info("Shutting down");
		         
		     } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}/* catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} */ finally {
		         httpclient.close();
		     }
		     //logger.info("Done");
		     		
			}
		
	
		return metadata;
	}

	private String readDocumentIdFromResult(String documentIdFromXML) {
		//<documentTitle>([0-9]+)<\/documentTitle>
		String docId = "";
				
		if (documentIdFromXML == null) {
	         return null;
	      } else {
	    	  String pattern = "<documentURI>(.+)<\\/documentURI>";
	          Pattern r = Pattern.compile(pattern);  
	          Matcher m = r.matcher(documentIdFromXML);
	          if (m.find( )) {
	        	       
	        	  docId = m.group(1);	          
	          	  return docId;
	          }else {
	              logger.error("No ID found in "+ documentIdFromXML);
	          }
	      }
	      return null;	
	}


	private String readTitleFromXML(String titleFromXML) {
		
		//<documentTitle>([0-9]+)<\/documentTitle>
		String title = "";
		
		if (titleFromXML == null) {
	         return null;
	      } else {
	    	  String pattern = "<documentTitle>(.*)<\\/documentTitle>";
	          Pattern r = Pattern.compile(pattern);  
	          Matcher m = r.matcher(titleFromXML);
	          if (m.find( )) {
	        	       
	        	  title = m.group(1);	          
	          	  return title;
	          }else {
	              logger.error("No title found in "+ titleFromXML);
	          }
	      }
	      return null;	
	}
	
   private double documentScore(String queryId, String rank) throws IOException{
		
		double score = 0.0;
		
		if (queryId!=null && queryId!=""){
			
			 HttpPost post = new HttpPost(DOCUMENT_SCORE_URL);
			

		    post.setConfig(requestConfig);
		        
			// add request parameters or form parameters
	        List<NameValuePair> urlParameters = new ArrayList<>();
	        urlParameters.add(new BasicNameValuePair("queryId", queryId));
	        urlParameters.add(new BasicNameValuePair("rank", rank));

	        post.setEntity(new UrlEncodedFormEntity(urlParameters));

	        try (CloseableHttpClient httpClient = HttpClients.createDefault();
	             CloseableHttpResponse response = httpClient.execute(post)){

	            String result = EntityUtils.toString(response.getEntity());
	            
	            score = readScoreFromXML(result);
	            
	            httpClient.close();
	        }

	       

			
		}
		return score;
	}


   private double readScoreFromXML(String scoreFromXML) {
	 
	 		
	 		if (scoreFromXML == null) {
	 	         return 0.0;
	 	      } else {
	 	    	  String pattern = "<value>(.*)<\\/value>";
	 	          Pattern r = Pattern.compile(pattern);  
	 	          Matcher m = r.matcher(scoreFromXML);
	 	          if (m.find( )) {
	 	        	       
	 	        	  return Double.valueOf(m.group(1));	          

	 	          }else {
	 	              logger.error("No score found in "+ scoreFromXML);
	 	          }
	 	      }
	 	      return 0.0;	
   }


    
   private Metadata close(String queryId) throws IOException{
		
		Metadata metadata = new Metadata();
		
		if (queryId!=null && queryId!=""){
			
			 HttpPost post = new HttpPost(CLOSE_URL);
			 
			 post.setConfig(requestConfig);
			// add request parameters or form parameters
	        List<NameValuePair> urlParameters = new ArrayList<>();
	        urlParameters.add(new BasicNameValuePair("queryId", queryId));

	        post.setEntity(new UrlEncodedFormEntity(urlParameters));

	        try (CloseableHttpClient httpClient = HttpClients.createDefault();
	             CloseableHttpResponse response = httpClient.execute(post)){

	            String result = EntityUtils.toString(response.getEntity());
	            
	            logger.info("Session with queryID " + queryId + " closed. " + result);
	            
	            httpClient.close();
	        }

	       

			
		}
		return metadata;
	}
    
	
}
