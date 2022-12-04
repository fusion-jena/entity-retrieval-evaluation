package de.unijena.cs.fusion.mimirTest;

public class Query {
   int ID;
   String query;
   
   public Query(){
	   
   }
   
   public Query(int ID, String query){
	   this.ID = ID;
	   this.query = query;
   }

public int getID() {
	return ID;
}

public void setID(int iD) {
	ID = iD;
}

public String getQuery() {
	return query;
}

public void setQuery(String query) {
	this.query = query;
}

   
}
