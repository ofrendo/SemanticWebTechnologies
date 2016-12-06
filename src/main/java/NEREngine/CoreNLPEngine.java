/**
 * 
 */
package main.java.NEREngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author Sascha Ulbrich
 *
 */
public class CoreNLPEngine implements NEREngine {
	private static CoreNLPEngine engine;
	private StanfordCoreNLP pipeline;
	private static final Logger LOG = LoggerFactory.getLogger(CoreNLPEngine.class);
	
	/*
	 * Singleton to ensure that CoreNLP is initialized just once
	 */
	private CoreNLPEngine(){
		//Initialize CoreNLP 
		setPropertiesForStanfordCoreNLP();
	}
	
	public static CoreNLPEngine getInstance() {
		if (CoreNLPEngine.engine == null) {
				CoreNLPEngine.engine = new CoreNLPEngine ();
		    }
		return CoreNLPEngine.engine;
	}
	
	private void setPropertiesForStanfordCoreNLP(){
        Properties props = new Properties();
        boolean useRegexner = false;
        if (useRegexner) {
          props.put("annotators", "tokenize, ssplit, pos, lemma, ner, regexner");
          props.put("regexner.mapping", "locations.txt");
        } else {
          props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
        }
        this.pipeline = new StanfordCoreNLP(props);
    }

	/* (non-Javadoc)
	 * @see NEREngine.NEREngine#getEntitiesFromText(java.lang.String)
	 */
	@Override
	public List<NamedEntity> getEntitiesFromText(String text) {
		// Analyze string
		//http://www.informit.com/articles/article.aspx?p=2265404
		//this.pipeline.clearAnnotatorPool();

        //replace intra-word ".", ":", "/"
        text = this.splitOnIntrawordPunctuation(text);

        //normalize punctuation to improve negation detection
        text = this.cleanNegators(text);

        Annotation document = new Annotation(text);
        // run all Annotators on this text
        this.pipeline.annotate(document);

        // these are all the sentences in this document
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        StringBuilder sb = new StringBuilder();
        List<NamedEntity> tokens = new ArrayList<NamedEntity>();
        
        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence, "O" is a sensible default to initialise
            // tokens to since we're not interested in unclassified / unknown things..
            String prevNeToken = "O";
            String currNeToken = "O";
            boolean newToken = true;
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
              currNeToken = token.get(NamedEntityTagAnnotation.class);
              String word = token.get(TextAnnotation.class);
              
              System.out.println("word=" + word + ", token=" + currNeToken);
              // Strip out "O"s and NUMBERS completely, makes code below easier to understand
              // Removing NUMBERS:
              // "Passenger Ruby Gupta, 20, was travelling to Azamagarh to be married on 1 December." --> 20Azamgarh
              // Removing MISC: 
              // "Indian Prime Minister Narendra Modi tweeted: \"Anguished beyond words on the loss of lives due to the derailing of the Patna-Indore express. My thoughts are with the bereaved families.\"" --> IndianNarendra Modi
              
              if (currNeToken.equals("O") || currNeToken.equals("NUMBER") || currNeToken.equals("MISC") 
            		  || currNeToken.equals("DATE") || currNeToken.equals("TIME") || currNeToken.equals("ORDINAL")
            		  || currNeToken.equals("MONEY")) {
                // LOG.debug("Skipping '{}' classified as {}", word, currNeToken);
                if (!prevNeToken.equals("O") && (sb.length() > 0)) {
                  handleEntity(prevNeToken, sb, tokens);
                  newToken = true;
                }
                continue;
              }

              if (newToken) {
                prevNeToken = currNeToken;
                newToken = false;
                sb.append(word);
                continue;
              }

              if (currNeToken.equals(prevNeToken)) {
                sb.append(" " + word);
              } else {
                // We're done with the current entity 
                handleEntity(prevNeToken, sb, tokens);
                newToken = true;
              }
              prevNeToken = currNeToken;
            }
        }     
        return tokens;
	}
	private void handleEntity(String inKey, StringBuilder inSb, List<NamedEntity> inTokens) {
	    LOG.debug("'{}' is a {}", inSb, inKey);
	    NamedEntity.EntityType et = null;
	    if (inKey.equals("PERSON")) {
	    	et = NamedEntity.EntityType.PERSON;
	    }
	    else if (inKey.equals("ORGANIZATION")) {
	    	et = NamedEntity.EntityType.ORGANIZATION;
	    }
	    else if (inKey.equals("LOCATION")) {
	    	et = NamedEntity.EntityType.LOCATION;
	    }
	    else {
	    	// Can also be "DATE"
	    	return;
	    }
	    
	    NamedEntity ne = new NamedEntity(inSb.toString(), et);
	    if(!inTokens.contains(ne)){
	    	inTokens.add(ne);
	    }
	    inSb.setLength(0);
	  }
        
	
	 private String splitOnIntrawordPunctuation(String t) {
	        //replace intra-word ".", ":", "/" (\S is non-whitespace character)
	        return t.replaceAll("(\\S)(\\.|:|/)(\\S)", "$1$2 $3");
	    }
	 
	 private String cleanNegators(String t) {
	        //´ and ` with normalized '
	        t = t.replaceAll("[´`]", "'");
	        // insert ' where it is missing
	        t = t.replaceAll("\\b" +
	                "(do|does|dos|doe|did" +
	                "|have|hav|has|hase|had" +
	                "|wo|is|are|was|were|wer" +
	                "|ca|could" +
	                "|would|should" +
	                "must)" +
	                "(n)(t)\\b"
	                , "$1$2'$3");
	        return t;
	    }

	/*
	 * Test execution
	 */
	public static void main(String[] args) {
		NEREngine e = CoreNLPEngine.getInstance();
		//String text = "This is a test to identify SAP in Walldorf with H. Plattner as founder. What happens with a duplicate of H. Plattner?";
		//String text = "The Kremlin revealed Mr Trump and Mr Putin had discussed Syria and agreed that current Russian-US relations were \"extremely unsatisfactory\".";
		String text = "Passenger Ruby Gupta, 20, was travelling to Azamagarh to be married on 1 December.";
		//String text = "She told the Times of India that most of the people travelling with her had been found but that her father was still missing.";
		
		//String text = "Indian Prime Minister Narendra Modi tweeted: \"Anguished beyond words on the loss of lives due to the derailing of the Patna-Indore express. My thoughts are with the bereaved families.\"";
		
		for (NamedEntity entity : e.getEntitiesFromText(text)) {
	        System.out.println(entity.getType() + ": " + entity.getName());
		}
		
		//System.out.println("====================");
		//text = "Does Stanford's CoreNLP from Stanford University identifies multiple sentences? Simply test it: Mannheim University is located in Baden-Wuerttemberg.";
		//for (NamedEntity entity : e.getEntitiesFromText(text)) {
	    //    System.out.println(entity.getType() + ": " + entity.getName());
		//}

	}
}
