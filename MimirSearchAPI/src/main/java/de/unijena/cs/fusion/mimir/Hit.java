package de.unijena.cs.fusion.mimir;

public class Hit {
	String documentId;
    String termPosition;
    String length;
    String hitText;
    String snippet;
    
    public Hit (){
    	
    }
    public Hit (String documentId, String termPosition, String length, String hitText){
    	this.documentId = documentId;
    	this.termPosition = termPosition;
    	this.length = length;
    	this.hitText = hitText;
    }
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getTermPosition() {
		return termPosition;
	}
	public void setTermPosition(String termPosition) {
		this.termPosition = termPosition;
	}
	public String getLength() {
		return length;
	}
	public void setLength(String length) {
		this.length = length;
	}
	public String getHitText() {
		return hitText;
	}
	public void setHitText(String hitText) {
		this.hitText = hitText;
	}
	public String getSnippet() {
		return snippet;
	}
	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}
}

