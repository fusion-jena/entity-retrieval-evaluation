package test;

import java.util.ArrayList;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unijena.cs.fusion.mimir.Document;
import de.unijena.cs.fusion.mimir.MimirSearch;

public class MimirSearchTest {
	
	private static final Logger logger = LoggerFactory.getLogger(MimirSearch.class);
	
	@Test
	public void SimpleSearch() throws Exception {
		
		//provide a valid index URL
		String INDEX_URL = "http://localhost:8080/mimir-cloud-6.2-SNAPSHOT/<your-index>/search/";
		
		MimirSearch searchSession = new MimirSearch(INDEX_URL);
		
		ArrayList<Document> docs = searchSession.search("quercus");
		
		
		docs.stream().forEach(doc->{
			logger.info("Rank: " + doc.getRank() + ", Score: "+doc.getScore() + ", Title: "+doc.getTitle() + ", Hits: "+ doc.getHits().size());
		});
		
		
	}
	
	@Test
	public void AnnotationSearch() throws Exception {
		
		//provide a valid index URL
		String INDEX_URL = "http://localhost:8080/mimir-cloud-6.2-SNAPSHOT/<your-index>/search/";
		
		MimirSearch searchSession = new MimirSearch(INDEX_URL);
		
		ArrayList<Document> docs = searchSession.search("{Organism} AND {Environment}");
		
		
		docs.stream().forEach(doc->{
			logger.info("Rank: " + doc.getRank() + ", Score: "+doc.getScore() + ", Title: "+doc.getTitle() + ", Hits: "+ doc.getHits().size());
		});
		
		
	}
}
