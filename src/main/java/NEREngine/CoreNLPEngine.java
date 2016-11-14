/**
 * 
 */
package main.java.NEREngine;

import java.util.*;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import main.java.NEREngine.NamedEntity.EntityType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
              // Strip out "O"s completely, makes code below easier to understand
              if (currNeToken.equals("O")) {
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
		String text = "This is a test to identify SAP in Walldorf with H. Plattner as founder. What happens with a duplicate of H. Plattner?";
	
		for (NamedEntity entity : e.getEntitiesFromText(text)) {
	        System.out.println(entity.getType() + ": " + entity.getName());
		}
		System.out.println("====================");
		text = "Does Stanford's CoreNLP from Stanford University identifies multiple sentences? Simply test it: Mannheim University is located in Baden-Wuerttemberg.";
		for (NamedEntity entity : e.getEntitiesFromText(text)) {
	        System.out.println(entity.getType() + ": " + entity.getName());
		}

	}
}
