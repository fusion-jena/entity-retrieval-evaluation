
package de.unijena.cs.fusion.score;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import slib.utils.ex.SLIB_Ex_Critic;


public class TaxonomicScorer extends AbstractWeightedScorer implements MimirScorer{
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
	
	private ArrayList<String> annTypesInQuery;
	
    
    private HashMap<String, SM_Engine> graphMap;
    
    private HashMap<String, Map> graphNodeLevelMap;
    //private HashMap<String, Integer> countChildrenMap;
    
    private static double ALPHA = 0.5;
    
    double alphaE = 1.0;
    double alphaC = 1.0;
   
    boolean alpha_beta = true;

  private static final Logger logger = LoggerFactory.getLogger(TaxonomicScorer.class);
  
  public SML sml;
  

  public TaxonomicScorer(SML sml) {

	  termVisitor = new TermCollectionVisitor();
		setupVisitor = new CounterSetupVisitor( termVisitor );
		counterCollectionVisitor = new CounterCollectionVisitor( setupVisitor );
		this.sml = sml;
	
	
  }
  

  public synchronized TaxonomicScorer copy() {
		final TaxonomicScorer scorer = new TaxonomicScorer(new SML());
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
		//logger.info("document: "+document);
				
		final int[] count = setupVisitor.count; //TF - Array with frequencies of a term in a document
		final int[] indexNumber = setupVisitor.indexNumber;
		final double[] weightedIdfPart = this.weightedIdfPart; //idf weights
		final int[] size = this.size; //Array of document sizes (document length, important for normalization)
		
		String[] terms = setupVisitor.termId2Term;
		
		HashMap<String, Integer> URI_id = new HashMap<String, Integer>();
		HashMap<Integer, String> id_URI = new HashMap<Integer, String>();
		
		HashMap<String, Integer> URI_broader_id = new HashMap<String, Integer>();
		HashMap<Integer, String> id_broader_URI = new HashMap<Integer, String>();
		
		HashMap<String, Integer> URI_category = new HashMap<String, Integer>();
		HashMap<Integer, String> category_URI = new HashMap<Integer, String>();
		//create a URI-ID Map
		
		double cat_boost = 0.0;
		
		for (int i = 0; i< terms.length; i++){
			if(count[ i ] != 0 && terms[i].contains(":")){
				logger.info("terms["+i+"]="+terms[i]);
				String annType = terms[i].split(":")[0];
				//System.out.println(annType);
				SemanticAnnotationHelper annHelper = engine.getAnnotationHelper(annType);
				//String term = mimirIndex.getAnnotationIndex(annType).getDirectTerm(i).toString();
				//System.out.println(term);
				if(annHelper!=null){
					String annotation = annHelper.describeMention(terms[i]);
					//
					//System.out.println("Annotation: "+annotation);
					if(annType == "Category"){
						String inst = extractInstFromAnnotation(annotation);
						if(inst!=null){
							URI_category.put(inst, i);
							category_URI.put(i, inst);
						}
					}
					else{
						
					    //System.out.println(annotation);
						String URI = extractInstFromAnnotation(annotation);
						String broader = extractBroaderFromAnnotation(annotation);
						logger.info(URI);
						logger.info(broader);
						if(URI == null){
							//ToDo
						}
						if(URI != null){
							
							URI_id.put(URI, i);
							id_URI.put(i, URI);
						}
						if(broader != null){
							id_broader_URI.put(i, broader);
							URI_broader_id.put(URI, i);
						}
					}
				}
				
			}
		}
	
		//logger.info("inst_id:"+ URI_id.keySet());
		//logger.info("broader_id:"+ id_broader_URI.keySet());
		//i = number of total documents
		
		for( int i = size.length; i-- != 0; ) size[ i ] = sizes[ i ].getInt( document );

		int k;
		double score = 0;
		//System.out.println("count.length: "+ count.length);
		
		double alpha = 1.0;
		double beta = 1.0;
		
		for ( int i = count.length; i-- != 0; ) {
						
			k = indexNumber[ i ];
			
				
			if(count[ i ] != 0 && terms[i].contains(":")){
				

				String uri = id_URI.get(i);
				String relatedURI = id_broader_URI.get(i);
				
				logger.info("count["+i+"]:"+count[ i ]);
				logger.info("size["+k+"]:"+size[ k ]);
				logger.info("weightedIdfPart["+i+"]:"+weightedIdfPart[i ]);
				
				if(alpha_beta) {
					alphaE = computeAlphaE(ALPHA);
					alphaC = computeAlphaC(ALPHA);
				}
				
				//exact match
				if(uri!=null && URIsInQuery!=null && URIsInQuery.contains(uri)){
					score += ((double)count[ i ] / size[ k ] * weightedIdfPart[ i ]) * alphaE;
					
				}
				//no exact match but a related URI - compute semantic similarity and use that
				else if(relatedURI!=null){
					logger.info("relatedURI: "+relatedURI);
					 double semSim = semanticSimilarity(URIsInQuery, relatedURI);
					 score += ((double)count[ i ] / size[ k ] * weightedIdfPart[ i ] * semSim) * alphaC;
				}
				
				
				
			}			
			//no fallback, no URI - no ranking
			else{
			  System.out.println("No URI found for " + terms[i]);
			  
			}
		}
		
		
		logger.info("score: "+ score);
		logger.info("---------------------");
		return score;
		
	
		    
  }
  
  
  private double computeAlphaC(double alpha) {
	  double alphaC = 0.0;
		
		if(alpha > 0.0) {
			alphaC = (1 - alpha) / (Math.sqrt((alpha*alpha) + ((1 - alpha) * (1 - alpha))));
		}
		return alphaC;
 }


private double computeAlphaE(double alpha) {
	
	double alphaE = 0.0;
	
	if(alpha > 0.0) {
		alphaE = alpha / (Math.sqrt((alpha*alpha) + ((1 - alpha) * (1 - alpha))));
	}
	return alphaE;
}


private double semanticSimilarity(ArrayList<String> URIs, String uri) {
	
	  double semSim=0;
	  
	  for(int i = 0; i < URIs.size(); i++){
		  try {
			
			semSim += sml.computeSemSim(URIs.get(i), uri);
			//logger.info("TaxScorer: semSim ("+ URIs.get(i)+ "," + uri+"):" + String.valueOf(semSim));
			
		} catch (SLIB_Ex_Critic e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			//if no semantic similarity can be computed, e.g., two URIs don't have the same root
			//semSim=0
			semSim += 0;
		}
	  }
    logger.info("TaxScorer: semSim ("+ URIs.toString()+ "," + uri+"):" + String.valueOf(semSim));
   // System.out.println("TaxScorer: semSim ("+ URIs.toString()+ "," + uri+"):" + String.valueOf(semSim));
	return semSim;
}




  
/**
   * extracts the inst feature value from an annotation String in the form {Material inst = http://purl.obolibrary.org/obo/ENVO_00001998}
   * @param annotation
   * @return inst feature, e.g., http://purl.obolibrary.org/obo/ENVO_00000109
   */
  private String extractInstFromAnnotation(String annotation) {
    //System.out.println("Annotation: " + annotation);
	String patternInst = "\\binst = ((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]_[0-9]+)";  
	
	if(annotation!=null && annotation.length()>0){
		Pattern r = Pattern.compile(patternInst);
		Matcher m = r.matcher(annotation);
	      if (m.find()) {
	    	  
	  	    	return m.group(1); 
	  	      
	      }
	}
	
	return null;
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
   * extracts a URI from a QueryString in the form {Material inst = http://purl.obolibrary.org/obo/ENVO_00001998}
   * @param annotation
   * @return List<String>, e.g., http://purl.obolibrary.org/obo/ENVO_00000109
   */
  private ArrayList<String> extractURIFromQuery(String query) {
    //System.out.println("Annotation: " + annotation);
	ArrayList<String> uris = new ArrayList<String>();
	
	String patternUri = "inst EQ ((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]_[0-9]+)";  
	
	if(query!=null && query.length()>0){
		
		Pattern r = Pattern.compile(patternUri);
		Matcher m = r.matcher(query);
	    while (m.find()) {
	    	 //System.out.println(m.group(0));
	    	 //System.out.println(m.group(1));
	    	 
	         uris.add(m.group(1));         
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
	 annTypesInQuery = extractAnnTypesFromQuery(querySegmentString);
	 
	 logger.info("URIs in query:" +URIsInQuery);
    
    QueryEngine engine = this.underlyingExecutor.getQueryEngine();
     
   
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
		logger.info("frequency["+i+"]:"+frequency[i]);
		logger.info("indexNumber["+i+"]:"+indexNumber[i]);
		logger.info("index2Weight.index[indexNumber["+i+"]]:"+index2Weight.getDouble( index[ indexNumber[ i ] ] ));
		logger.info("index[indexNumber["+i+"]].numberOfDocuments:"+index[ indexNumber[ i ] ].numberOfDocuments );
		
		weightedIdfPart[ i ] = Math.log( index[ indexNumber[ i ] ].numberOfDocuments / (double)frequency[ i ] ) * index2Weight.getDouble( index[ indexNumber[ i ] ] );
		//System.out.println("weightedIdfPart[i]: " + weightedIdfPart[ i ]);
		logger.info("weightedIdfPart["+i+"]: " + weightedIdfPart[ i ]);
	}
	size = new int[ index.length ];
	
   
  }

private ArrayList<String> extractAnnTypesFromQuery(String query) {
	 //System.out.println("Annotation: " + annotation);
		ArrayList<String> annTypes = new ArrayList<String>();
		
		String patternType = "\\btype = ([A-Za-z]+)";
		if(query!=null && query.length()>0){
			
			
			Pattern r = Pattern.compile(patternType);
			Matcher m = r.matcher(query);
		    while (m.find()) {
		         annTypes.add(m.group(1));         
		    }
		}
		
		return annTypes;
}

protected QueryExecutor underlyingExecutor;



  public long nextDocument(long greaterThan) throws IOException {
    return underlyingExecutor.nextDocument(greaterThan);
  }
  
  public Binding nextHit() throws IOException {
	    return underlyingExecutor.nextHit();
	  }


}
