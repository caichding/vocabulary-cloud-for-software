package br.edu.ufcg.gmf.util;

public class TermTuple implements Comparable<TermTuple> {

	private String term;
	private int freq;
	
	public TermTuple(String term, int frequency) {
		this.term = term;
		this.freq = frequency;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public Integer getFrequency() {
		return freq;
	}

	public void setFrequency(int frequency) {
		this.freq = frequency;
	}

	@Override
	public int compareTo(TermTuple o) {
		return (o.getFrequency() - this.getFrequency());
	}
	
	@Override
	public String toString() {
		return "[" + this.getTerm() + "," + this.getFrequency() + "]";
	}
}