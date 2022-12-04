package de.unijena.cs.fusion.score;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unijena.cs.fusion.slib.SML;
import gate.mimir.ConstraintType;
import gate.mimir.SemanticAnnotationHelper;
import gate.mimir.search.QueryEngine;
import gate.mimir.search.query.Binding;
import gate.mimir.search.query.QueryExecutor;
import gate.mimir.search.query.QueryNode;
import gate.mimir.search.score.MimirScorer;
import gate.mimir.util.DelegatingSemanticAnnotationHelper;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.index.IndexIterator;
import it.unimi.di.big.mg4j.search.DocumentIterator;
import it.unimi.di.big.mg4j.search.score.AbstractWeightedScorer;
import it.unimi.di.big.mg4j.search.visitor.CounterCollectionVisitor;
import it.unimi.di.big.mg4j.search.visitor.CounterSetupVisitor;
import it.unimi.di.big.mg4j.search.visitor.TermCollectionVisitor;
import it.unimi.dsi.fastutil.ints.IntBigList;
import slib.sml.sm.core.engine.SM_Engine;


public class HCFIDFScorer extends AbstractWeightedScorer implements MimirScorer{
  private static final long serialVersionUID = 3855212427922484546L;
  
	private static final boolean DEBUG = true;

	/** The counter collection visitor used to estimate counts. */
	private final CounterCollectionVisitor counterCollectionVisitor;
	/** The counter setup visitor used to estimate counts. */
	private final CounterSetupVisitor setupVisitor;
	/** The term collection visitor used to estimate counts. */
	private final TermCollectionVisitor termVisitor;

	/** An array (parallel to {@link #currIndex}) that caches size lists. */
	private IntBigList sizes[];
	/** An array (parallel to {@link #currIndex}) used by {@link #score()} to cache the current document sizes. */
	private int[] size;
	/** An array indexed by offsets that caches the inverse document-frequency part of the formula, multiplied by the index weight. */
	private double[] weightedIdfPart;
	
	
	private ArrayList<String> URIsInQuery;
	
    
    private HashMap<String, SM_Engine> graphMap;
    
    private HashMap<String, Map> graphNodeLevelMap;
    private HashMap<String, Map> graphURINodeLevelMap;

    

    private static final Logger logger = LoggerFactory.getLogger(HCFIDFScorer.class);

  public HCFIDFScorer(SML sml) {

	  termVisitor = new TermCollectionVisitor();
		setupVisitor = new CounterSetupVisitor( termVisitor );
		counterCollectionVisitor = new CounterCollectionVisitor( setupVisitor );

		graphMap = sml.getGraphMap();
		graphNodeLevelMap = sml.getGraphNodeLevelMap();
		graphURINodeLevelMap = sml.getGraphURINodeLevelMap();

  }
  

  public synchronized HCFIDFScorer copy() {
		final HCFIDFScorer scorer = new HCFIDFScorer(new SML());
		scorer.setWeights( index2Weight );
		return scorer;
	}

  @Override
  public double score(Index index) throws IOException {
    return score();
  }
  
  /**
   * computes the score per document for a given query
   */
  @Override
  public double score() throws IOException {
	 
	  
	  setupVisitor.clear();
		documentIterator.acceptOnTruePaths( counterCollectionVisitor );
		this.underlyingExecutor = (QueryExecutor)documentIterator;
		QueryEngine engine = this.underlyingExecutor.getQueryEngine();
		//MimirIndex mimirIndex = engine.getIndex();
		
		if ( documentIterator instanceof IndexIterator ) indexIterator = new IndexIterator[] { (IndexIterator)documentIterator };
		
		final long document = documentIterator.document();
		//System.out.println("document: "+document);
				
		final int[] count = setupVisitor.count; //TF - Array with frequencies of a term in a document
		final int[] indexNumber = setupVisitor.indexNumber;
		final double[] weightedIdfPart = this.weightedIdfPart; //idf weights
		final int[] size = this.size; //Array of document sizes
		
		String[] terms = setupVisitor.termId2Term;
		
		HashMap<String, Integer> URI_id = new HashMap<String, Integer>();
		HashMap<Integer, String> id_URI = new HashMap<Integer, String>();
		//create a URI-ID Map		
		
		for (int i = 0; i< terms.length; i++){
			if(count[ i ] != 0 && terms[i].contains(":")){
				//System.out.println("terms["+i+"]="+terms[i]);
				String annType = terms[i].split(":")[0];
				//System.out.println(annType);
				SemanticAnnotationHelper annHelper = engine.getAnnotationHelper(annType);
				//System.out.println(term);
				if(annHelper!=null){
					String annotation = annHelper.describeMention(terms[i]);
				    logger.info(annotation);
					String URI = extractURIFromAnnotation(annotation);
					logger.info(URI);
					if(URI == null){
						//ToDO
					}
					if(URI != null){
						URI_id.put(URI, i);
						id_URI.put(i, URI);
					}

				}
			}
		}
	
		//System.out.println("URI_id:"+ URI_id.keySet());
		
		//i = number of total documents
		
		for( int i = size.length; i-- != 0; ) size[ i ] = sizes[ i ].getInt( document );

		int k;
		double score = 0;
		//System.out.println("count.length: "+ count.length);
		
		
		
		for ( int i = count.length; i-- != 0; ) {
						
			k = indexNumber[ i ];
			
			//if we have URIs - compute HCF-IDF
			
			
			
			if(count[ i ] != 0 && terms[i].contains(":")){
				
				Set<URI> URIs = new HashSet<URI>();
				String uri = id_URI.get(i);
				//System.out.println(uri);
				
				if(uri!=null){
					URIs.add(new URIImpl(uri));						
					double bellLog = (double)BellLog(URIs,count,size,indexNumber,URI_id);
					//System.out.println("bellLog:" + bellLog);
					score += bellLog * weightedIdfPart[i];
				}
				else{
					//System.out.println("no URI found for id:"+i);
					
				}
				
				
			}			
			//no fallback, no URI - no ranking
			
		}
		
		
		logger.info("score: "+ score);
		logger.info("---------------------");
		return score;
		
	
		    
  }
  
  
  private double BellLog(Set<URI> uris, int[] count, int[] size, int[] indexNumber, Map URI_id){
	double bellLog = 0.01; 
	
	//long nodeLevel = getNodeLevel(uri);
	if(uris!=null && uris.size()>0){
		for(URI uri : uris){
			Set<URI> children = getNextChildren(uri);
			Object o = URI_id.get(uri.toString());
			String graphName = getGraphName(uri.toString());
			
			if(children==null || children.size()<1){
				bellLog+= 0.01;
			}
			
			if(o!=null){
				int id = (int) o;
				int k = indexNumber[id];			
				
				double childrenBellLog = BellLog(children,count, size,indexNumber,URI_id);
				//System.out.println("childrenBellLog: "+childrenBellLog);
				
				
				bellLog+= (double)(count[id] / size[k]) + FL(uri, graphName) * childrenBellLog;
				//System.out.println("temp bellLog: "+bellLog);
			}
			
			
			
		} 
	}
	//System.out.println("BellLog: "+bellLog);
	return bellLog;
  }
  
  
  private Set<URI> getNextChildren(URI uri) {
	  Set<URI> children = new HashSet<URI>();
	  
	  String vocab = getGraphName(uri.toString());
	  long nodeLevel = getNodeLevel(uri);
	  
	  
	  SM_Engine engine = (SM_Engine)graphMap.get(vocab);
	  
	  if(engine==null)
		  return children;
	  
	  Set<URI> allChildren = engine.getChildren(uri);
		  
	  Set<URI> nodesOnNextLevel = nodesAtLevel(nodeLevel+1,vocab);
	  
	  if(nodesOnNextLevel==null)
		  return children;
	  
	  //intersection of allChildren and nodesOnNextLevel = children
	   for (URI u: nodesOnNextLevel)
	   {
	        if (allChildren.contains(u))
	           children.add(u);
	   }
	   

	return children;
}


private double FL(URI uri, String graphName){
	//System.out.println("FL for URI "+uri);
	long nodeLevel = 0;
	double FL = 0.01;

		nodeLevel = getNodeLevel(uri);
		//System.out.println("NodeLevel: "+nodeLevel);
		Set<URI> nodesAtLevel = nodesAtLevel(nodeLevel+1,graphName);
		//System.out.println("NodesAtLevel.size : "+nodesAtLevel.size());
		if(nodesAtLevel == null || nodesAtLevel.size() == 0 ){
			//System.out.println("FL: 0.01");
			return 0.01;
		}
		else{
			
			//add 0.5 for case: only 1 leave -> nodesAtLevel.size()=1, log1=0, 1/0=N/A
			FL =  (double)1/Math.log10(nodesAtLevel.size()+0.5);
			//System.out.println("FL: " + FL);
			return FL;
		}

}

private Set<URI> nodesAtLevel(long nodeLevel, String graphName){
	  Set<URI> URISet = null;
	  
	  //SM_Engine engine = (SM_Engine)graphMap.get(vocab.toLowerCase());
	  HashMap<Long, Set<URI>> nodeLevelMap = (HashMap<Long, Set<URI>>) graphNodeLevelMap.get(graphName);
	  
	
	  if(nodeLevelMap!=null && nodeLevelMap.get(nodeLevel)!=null){
		
			  URISet = nodeLevelMap.get(nodeLevel);
		
	  }
	  
	  return URISet;
	  

}


private Long getNodeLevel(URI URI){
	  
	  String vocab = getGraphName(URI.toString());
  
	  //HashMap<String,Map> nodeLevelMap = (HashMap<String, Map>) graphNodeLevelMap.get(vocab);
	  HashMap<URI,Long> nodeLevelMap = (HashMap<URI,Long>) graphURINodeLevelMap.get(vocab);
	  
	  if(nodeLevelMap!=null)
		  return  nodeLevelMap.get(URI);
	  
	return new Long(0);
  }


private static String getGraphName(String URI) {
	String[] graphURIName = URI.split("/");
	String[] vocabNameArray = graphURIName[graphURIName.length-1].split("_");
	String vocab=vocabNameArray[0];
	
		return vocab.toLowerCase();
	
}
  
/**
 * extracts the broader feature value from an annotation String in the form {Material broader = http://purl.obolibrary.org/obo/ENVO_00001998}
 * @param annotation
 * @return broader feature, e.g., http://purl.obolibrary.org/obo/ENVO_00000109
 */
private String extractBroaderFromAnnotation(String annotation) {
  //System.out.println("Annotation: " + annotation);
	String pattern = "\\bbroader = ((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]_[0-9]+)";  
	
	if(annotation!=null && annotation.length()>0){
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(annotation);
	      if (m.find()) {
	          return m.group(1);         
	      }
	}
	
	return null;
}

  
/**
   * extracts a URI from an AnnotationString in the form {Material inst = http://purl.obolibrary.org/obo/ENVO_00001998}
   * @param annotation
   * @return URI, e.g., http://purl.obolibrary.org/obo/ENVO_00000109
   */
  private String extractURIFromAnnotation(String annotation) {
    //System.out.println("Annotation: " + annotation);
	String pattern = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]_[0-9]+";  
	
	if(annotation!=null && annotation.length()>0){
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(annotation);
	      if (m.find()) {
	          return m.group(0);         
	      }
	}
	
	return null;
}
  
  /**
   * extracts a URI from a QueryString in the form {Material inst = http://purl.obolibrary.org/obo/ENVO_00001998}
   * @param annotation
   * @return List<String>, e.g., http://purl.obolibrary.org/obo/ENVO_00000109
   */
  private ArrayList<String> extractURIFromQuery(String query) {
    //System.out.println("Annotation: " + annotation);
	ArrayList<String> uris = new ArrayList<String>();
	
	String patternUri = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]_[0-9]+";  
	String wc ="";
	
	if(query!=null && query.length()>0){
		
		
		Pattern r = Pattern.compile(patternUri);
		Matcher m = r.matcher(query);
	    while (m.find()) {
	         uris.add(m.group(0));         
	    }
	}
	
	return uris;
}





@Override
  public boolean usesIntervals() {
    return false;
  }


  @Override
  public void wrap(DocumentIterator documentIterator) throws IOException {
    super.wrap(documentIterator);
    this.underlyingExecutor = (QueryExecutor)documentIterator;
    
    QueryNode query = this.underlyingExecutor.getQueryNode();
	
	 String querySegmentString = query.toString();
	 logger.info("Query: " +querySegmentString);
	 URIsInQuery = extractURIFromQuery(querySegmentString);
	 logger.info("URIs in query:" +URIsInQuery);
    
    //QueryEngine engine = this.underlyingExecutor.getQueryEngine();

   
	/* Note that we use the index array provided by the weight function, *not* by the visitor or by the iterator.
	 * If the function has an empty domain, this call is equivalent to prepare(). */
	termVisitor.prepare( index2Weight.keySet() );
	//System.out.println("index2Weight:"+index2Weight.keySet());
	
	//Visitor pattern, accept method calls the visit Method in TermCollectionVisitor
	//here, the actual "filling" takes place
	documentIterator.accept( termVisitor );

	//if ( DEBUG ) logger.debug( "Term Visitor found " + termVisitor.numberOfPairs() + " leaves" );

	// Note that we use the index array provided by the visitor, *not* by the iterator.
	final Index[] index = termVisitor.indices();
	


	if ( DEBUG ) logger.debug( "Indices: " + Arrays.toString( index ) );

	// Some caching of frequently-used values
	sizes = new IntBigList[ index.length ];
	//System.out.println("Index.length:"+index.length);
	for( int i = index.length; i-- != 0; )
		if ( ( sizes[ i ] = index[ i ].sizes ) == null ) throw new IllegalStateException( "A BM25 scorer requires document sizes" );
		
	setupVisitor.prepare();

	
	documentIterator.accept( setupVisitor );

	final long[] frequency = setupVisitor.frequency;
	final int[] indexNumber = setupVisitor.indexNumber;
	
	
	
	// We do all logs here, and multiply by the weight
	weightedIdfPart = new double[ frequency.length ];
	for( int i = weightedIdfPart.length; i-- != 0; ) {
		//System.out.println("frequency:"+frequency[i]);
		weightedIdfPart[ i ] = Math.log( index[ indexNumber[ i ] ].numberOfDocuments / (double)frequency[ i ] ) * index2Weight.getDouble( index[ indexNumber[ i ] ] );
		//System.out.println("weightedIdfPart[i]: " + weightedIdfPart[ i ]);
	}
	size = new int[ index.length ];
    
   
  }

protected QueryExecutor underlyingExecutor;



  public long nextDocument(long greaterThan) throws IOException {
    return underlyingExecutor.nextDocument(greaterThan);
  }
  
  public Binding nextHit() throws IOException {
	    return underlyingExecutor.nextHit();
	  }


}
