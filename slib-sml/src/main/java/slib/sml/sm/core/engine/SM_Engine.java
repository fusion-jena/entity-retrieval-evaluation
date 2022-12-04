

/***** inside the SM_Engine class add the following methods to compute the node level *****/

    /**
     * Get hierarchy level of a current vertex
     * assumption: all vertexes in that list are on the same hierarchy level
     * @param v
     * @return the hierarchy level of the given vertex
     * @throws SLIB_Ex_Critic
     */
    public long getNodeLevel(URI v) throws SLIB_Ex_Critic {
    	
    	throwErrorIfNotClass(v);
    	
    	long level = 0;
    	Set<URI> a = getAncestorsInc(v);
  	
    	Set<URI> ancestors = new HashSet<URI>();
    	
    	//getAncestorsInc returns an unmodifiable Set, so we need to copy the Set into a new one
    	//getAncestorsInc also contains the vertex itself - remove it
    	for(URI uri : a){
    		if(!uri.equals(v))
    			//System.out.println(uri);
    			ancestors.add(uri);
    	}
    	   	
    	Set<URI> parents;
    	Set<URI> vertexes = new HashSet<URI>();
    	vertexes.add(v);
    	boolean stop=false;
    	
    	//as long as we have ancestors
    	while(ancestors.size()>0 && stop==false){
    		
    		//per hierarchy level +1
    		level++;
    		//System.out.println("---------Level: " + level+"------");  
    		for(URI uri : vertexes){
	    		parents = topNodeAccessor.getNeighbors(uri);
	    		parents.remove(v);
	    		
	    		/*for(URI parent : parents){   		
        			System.out.println("Parent: " + parent);    			
        	     }*/
	    		
				if(parents!=null && parents.size()>0){
					   for(URI p : parents){
						   if(ancestors.contains(p)){
							   //remove all the parents from the ancestors (this hierarchy level has been counted already)
							   ancestors.remove(p);							   
						   }
					   }
					   //but analyze if the parents have parents (= grandparents)
					   vertexes = parents;					   					        	
				}else{
					//no parents left - stop, set ancestors to size=0
					stop=true;
				}	
    		}
    		
    		/*for(URI anc : ancestors){   		
    			System.out.println("Ancestor: " + anc);    			
    	     }*/
    	}
        
        return level;
    }
    
    public Map<URI,Long> computeNodeLevel() throws SLIB_Ex_Critic {
    	Map<URI,Long> nodes = new HashMap<URI,Long>();
    	
    	Set<URI> allClasses = this.getClasses();
    	
    	for(URI uri : allClasses){
    		//System.out.println(uri);
    		nodes.put(uri,getNodeLevel(uri));
    	}
    	   	
    	return nodes;
    } 

  