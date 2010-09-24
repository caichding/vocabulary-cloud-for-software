package br.edu.ufcg.gmf.astexplorer;

import irutils.Corpus;
import irutils.IRPropertyKeys;
import irutils.info.IRInfo;
import irutils.info.LSIInfo;
import irutils.info.RetrievedInfoIF;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public class VocabularyConverter {

	private Properties props;
	
	private Corpus corpus;
	private RetrievedInfoIF info;
	
	public VocabularyConverter(Map<String, List<String>> vocabulary) {
		
		this.props = generateProperties();
		this.corpus = new Corpus(vocabulary, this.props);
		
		this.validateProperties(this.props);
	}
	
	public Map<String, Double> convertToFrequencyMap() {
		
		Map<String, Double> termFrequencies = new HashMap<String, Double>();
		
		DoubleMatrix2D termDocumentMatrix = this.info.getTermDocumentMatrix();
		Map<String, Integer> allTerms = this.info.getAllTermIdsMap();
		
		for (String term : allTerms.keySet()) {
			
			// getting all frequencies of the term
			int termIndex = allTerms.get(term);
			DoubleMatrix1D matrixRow = termDocumentMatrix.viewRow(termIndex);
			
			// calculating total frequency of the term
			double frequency = matrixRow.zSum();
			
			// putting frequency into result
			termFrequencies.put(term, frequency);
		}
		
		return termFrequencies;
	}
	
	private void validateProperties(Properties props) {
		
		if (props.containsKey(IRPropertyKeys.IR_FUNCTION_TYPE)) {
			String irFunctionProp = (String) props.getProperty(IRPropertyKeys.IR_FUNCTION_TYPE);
			
			if (irFunctionProp.equalsIgnoreCase(IRPropertyKeys.IRFunctionType.LSI.toString())) {
				this.info = new LSIInfo(this.corpus);
			} else {
				this.info = new IRInfo(this.corpus);
			}
		}
	}
	
	private Properties generateProperties() {
		
		Properties props = new Properties();
		
		props.put(IRPropertyKeys.IR_FUNCTION_TYPE, IRPropertyKeys.IRFunctionType.LSI.toString());
		props.put(IRPropertyKeys.DISTANCE_FUNCTION, IRPropertyKeys.DistanceFunctionType.EUCLIDEAN.toString());
		props.put(IRPropertyKeys.TERM_FREQUENCY_VARIANT_TYPE, IRPropertyKeys.TermFrequencyVariant.ABSOLUTE.toString());
		props.put(IRPropertyKeys.SCORE_CALCULATOR_TYPE, IRPropertyKeys.ScoreCalculatorType.TF.toString());
		
		return props;
	}
}