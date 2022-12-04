package de.unijena.cs.fusion.mimir;

import java.util.ArrayList;

public class Document {
	
	//ID
	String documentID;
	

	//title
	String title;

	// document rank (in search result)
	int rank;
	
	//score
	double score;

	// document text snippet for display (contains the hit and surrounding tokens)
	String documentText;

	String[] documentTextArray;

	// array for collecting the hits (text) for highlighting
	String[] highlighting;

	// hits array with hits (documentId, termPosition, tokenLength)
	ArrayList<Hit> hits;

	public void Document () {
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}
	
	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String documentID) {
		this.documentID = documentID;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public String getDocumentText() {
		return documentText;
	}

	public void setDocumentText(String documentText) {
		this.documentText = documentText;
	}

	public String[] getDocumentTextArray() {
		return documentTextArray;
	}

	public void setDocumentTextArray(String[] documentTextArray) {
		this.documentTextArray = documentTextArray;
	}

	public String[] getHighlighting() {
		return highlighting;
	}

	public void setHighlighting(String[] highlighting) {
		this.highlighting = highlighting;
	}

	public ArrayList<Hit> getHits() {
		return hits;
	}

	public void setHits(ArrayList<Hit> hits) {
		this.hits = hits;
	}
	
	public ArrayList<Integer> getTermPosOfHits(){
	      ArrayList<Integer> termPos = new ArrayList<Integer>();
	       
	       if (this.hits.size() > 0) {
	           this.hits.stream().forEach(hit -> {
	               // let i = 1;
	               termPos.add(Integer.valueOf(hit.getTermPosition())); 
	              
	           });
	       }
	       // console.log(termPos);
	       return termPos;

	   }
	
	public Hit getHitByTermPos(String termPos){
		Hit hit = new Hit();

	       if (this.hits.size() > 0) {
	           this.hits.stream().forEach(h -> {
	               if (h.getTermPosition() == termPos) {
	            	   hit.setDocumentId(h.getDocumentId());
	            	   hit.setTermPosition(h.getTermPosition());
	            	   hit.setLength(h.getLength());
	            	   hit.setHitText(h.getHitText());
	            	   hit.setSnippet(h.getSnippet());
	            	   
	               }
	           });
	       }
	       return hit;
	   }
}
