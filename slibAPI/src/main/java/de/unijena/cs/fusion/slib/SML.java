

package de.unijena.cs.fusion.slib;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slib.graph.io.conf.GDataConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.metrics.ic.utils.ICconf;
import slib.sml.sm.core.utils.SMConstants;
import slib.sml.sm.core.utils.SMconf;
import slib.utils.ex.SLIB_Ex_Critic;



public class SML {
	 static Logger logger = LoggerFactory.getLogger(SML.class);
	
	 static URIFactory factory;
		
	 public ArrayList<String> URIsInQuery;
		
	 //adjust if needed - provide the path to your slibAPI/res/vocabs folder with all ontologies to be loaded
	 public static String FOLDER_WITH_GRAPHS = "C:/slibAPI/res/vocabs/"; 
	
	    
	 public static HashMap<String, SM_Engine> graphMap;
	 
	 public HashMap<String, Map> graphNodeLevelMap;
	 
	 public HashMap<String, Map> graphURINodeLevelMap;
	
	public HashMap<String, Map> getGraphURINodeLevelMap() {
		return graphURINodeLevelMap;
	}


	public void setGraphURINodeLevelMap(HashMap<String, Map> graphURINodeLevelMap) {
		this.graphURINodeLevelMap = graphURINodeLevelMap;
	}


	public ArrayList<String> getURIsInQuery() {
		return URIsInQuery;
	}


	public void setURIsInQuery(ArrayList<String> uRIsInQuery) {
		URIsInQuery = uRIsInQuery;
	}


	public HashMap<String, SM_Engine> getGraphMap() {
		return graphMap;
	}


	public void setGraphMap(HashMap<String, SM_Engine> graphMap) {
		this.graphMap = graphMap;
	}


	public HashMap<String, Map> getGraphNodeLevelMap() {
		return graphNodeLevelMap;
	}


	public void setGraphNodeLevelMap(HashMap<String, Map> graphNodeLevelMap) {
		this.graphNodeLevelMap = graphNodeLevelMap;
	}
  
	

	public SML(){
		logger.info("Loading required Data");
		
			  factory = URIFactoryMemory.getSingleton();    
			  graphMap = new HashMap<String, SM_Engine>();
			  graphNodeLevelMap = new HashMap<String,Map>();
			  graphURINodeLevelMap = new HashMap<String,Map>();
		   
			 
	    	  
	    	  
	    	  
		      //for all graphs in a folder - load graphs
			  List<File> allGraphs =  iterateOverFiles(new File(FOLDER_WITH_GRAPHS));
		      
		      //get graph name
		      for(File g : allGraphs){
		    	 
		    	  String[] pathArray = g.getPath().split("\\\\");
		    	  String fileName = pathArray[pathArray.length-1];
		    	  
		    	  if(fileName!=null){
		    		String[] fileNameA = fileName.split("\\.");
		    		String vocabulary = fileNameA[0];
		    		System.out.println("graph: " + vocabulary);
		    		
		    		
		    		
		    		URI graph_uri = factory.getURI("http://"+vocabulary+"/");
		    		G graph = new GraphMemory(graph_uri);
		    		GDataConf graphconf;
		    		
		    		if (g.getName().toLowerCase().endsWith("ttl")) {
		    			graphconf = new GDataConf(GFormat.TURTLE, g.getPath());
		    		}
		    		
		    	    else if(g.getName().toLowerCase().endsWith("obo")){
		    	    	graphconf = new GDataConf(GFormat.OBO, g.getPath());
		    	    }
		    	    else{
		    	    	graphconf = new GDataConf(GFormat.RDF_XML, g.getPath());
		    	    }
		    	    
		    		try {
						GraphLoaderGeneric.populate(graphconf, graph);
						
						// General information about the graph
			    		SM_Engine engine =  new SM_Engine(graph);
			    		int countClasses = engine.getClasses().size();
			    		//System.out.println("CountClasses: "+countClasses);
			    		 		
			    	    graphMap.put(vocabulary.toLowerCase(), engine);
			    	    
			    	    logger.info("Computing Node Level");
			    	    Map<URI, Long> URI_NL_Map = engine.computeNodeLevel();
			    	    this.graphURINodeLevelMap.put(vocabulary, URI_NL_Map );
			    	    logger.info("Node Level Computation done");
			    	    
			    	    Map<Long, Set<URI>> nodeLevelMap = computeURIsPerNodeLevel(URI_NL_Map);
			    	    graphNodeLevelMap.put(vocabulary, nodeLevelMap);
			    	    
			    	    
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logger.error("Error " + e.getMessage());
					}
		    		 
		    	    
		    	  }
		    	  
		      }
		
	}
	
	
	private Map<Long, Set<URI>> computeURIsPerNodeLevel(Map<URI, Long> URI_NL_Map) {
	

		Map<Long, Set<URI>> nodeLevelMap = new HashMap<Long, Set<URI>>();
		
		if (URI_NL_Map!=null) {
			for (Iterator<URI> it = URI_NL_Map.keySet().iterator(); it.hasNext();) {
				URI uri = it.next();
				long nodeLevel = URI_NL_Map.get(uri);
				
				if(!nodeLevelMap.keySet().contains(nodeLevel)) {
					Set<URI> URIs = new HashSet<URI>();
					URIs.add(uri);
					nodeLevelMap.put(nodeLevel, URIs);
				}else {
					Set<URI> URIs = nodeLevelMap.get(nodeLevel);
					
					if(!URIs.contains(uri))
						URIs.add(uri);
					
					nodeLevelMap.put(nodeLevel, URIs);
				}
			}
		}
		return nodeLevelMap;
	}


	public static List<File> iterateOverFiles(File folder) {
		  
	    List<File> result = new ArrayList<File>();
	   
	     if (folder.isDirectory()) {
	    	 File[] allFiles = folder.listFiles();
	    	 
	    	 for(File file : allFiles){
	    		 File semFile = findFileswithSemanticExtension(file);
		            if(semFile != null) {
		                result.add(semFile);
		            }
	    	 }
	            
	        } else {
	            logger.error("file " + folder +" is not a directory");
	        }
	    

	    return result;
	}
  
  public static File findFileswithSemanticExtension(File file) {
	  //System.out.println(file.getAbsolutePath());
      if(file.getName().toLowerCase().endsWith("owl") || file.getName().toLowerCase().endsWith("rdf")|| file.getName().toLowerCase().endsWith("ttl") || file.getName().toLowerCase().endsWith("obo") ||file.getName().toLowerCase().endsWith("nt")) {
          return file;
      }
      else{
    	  logger.error("no semantic formats found! Please provide *.owl, *.rdf, *.obo or *.nt files");
      }
      return null;
  }
  
  
  private static String getGraphName(String URI) {
		String[] graphURIName = URI.split("/");
		String[] vocabNameArray = graphURIName[graphURIName.length-1].split("_");
		String vocab=vocabNameArray[0];
		
		return vocab.toLowerCase();
		
	}
  
  public double computeSemSim(String URIA, String URIB) throws SLIB_Ex_Critic {
	  URI nodeA = factory.getURI(URIA.toString());
      String vocab = getGraphName(URIA);
      
      URI nodeB = factory.getURI(URIB);
      String vocabB = getGraphName(URIB);
      
      double sim = 0.0;
      
      	  
		  
		 SM_Engine engine = graphMap.get(vocab);
		
		  
		  if(engine!=null){
	      Set<URI> nodeA_Ancs = engine.getAncestorsInc(nodeA);
	      
	     
	      
	      // Retrieve the inclusive descendants of a vertex
	      Set<URI> nodeA_Descs = engine.getDescendantsInc(nodeA);
	     
	      
	      // First we define the information content (IC) we will use
	      //ICconf icConf = new IC_Conf_Topo("Sanchez", SMConstants.FLAG_ICI_SANCHEZ_2011);
	      ICconf icConf = new IC_Conf_Topo("Zhou", SMConstants.FLAG_ICI_ZHOU_2008);
	      //ICconf icConf = new IC_Conf_Topo("Harispe", SMConstants.FLAG_ICI_HARISPE_2012);
	   
	      // Then we define the Semantic measure configuration
	      //SMconf smConf = new SMconf("Lin", SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_LIN_1998);
	      SMconf smConf = new SMconf("Resnik", SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_RESNIK_1995);
	      //SMconf smConf = new SMconf("Harispe", SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_HARISPE_2013); //node-based pairwise
	      //SMconf smConf = new SMconf("Lin", SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_LIN_1998);
	      smConf.setICconf(icConf);
	     
	      
	      // Finally, we compute the similarity between the concepts Forest Biome and shrub
	     
	      
	      try{
	    	  sim = engine.compare(smConf, nodeA,nodeB);
	      }
	      catch(Exception e){
	    	  //the semantic similarity can not be computed for URIs that do not share the same root
	    	  logger.info("Semantic Similarity can not be computed for URIs: "+URIA + ", "+URIB);
	    	  logger.info(e.getMessage());
	    	  
	    	  //e.printStackTrace();
	    	  
	    	  sim = 0.0;
	    	  
	      }
	      //System.out.println("Sim " +URIA + ", " +URIB+" : "+sim);
	      
		  }
      //}
      //else - similarity is 0.0 if URIs are located in different graphs
      return sim;
      
      
}
}
