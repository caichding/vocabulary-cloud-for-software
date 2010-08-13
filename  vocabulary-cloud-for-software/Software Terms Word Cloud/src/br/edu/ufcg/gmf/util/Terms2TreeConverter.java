package br.edu.ufcg.gmf.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Tree;
import prefuse.util.ui.UILib;
import ca.utoronto.cs.prefuseextensions.demo.StarburstDemo;

public class Terms2TreeConverter {

	private Map<String, Integer> termsMap;
	
	public static final String TERM_DATA = "term";
	public static final String TERM_FREQUENCY_DATA = "freq";
	
	private Tree aTree;
	private List<TermTuple> orderedTerms;
	
	public Terms2TreeConverter(String termsString) {
		initTermsMap(termsString);
	}
	
	private void initTermsMap(String termsString) {
		
		final int FIRST_OCCURRENCE = 1;
		final String TERM_SEPARATOR = " ";
		
		this.termsMap = new HashMap<String, Integer>();
		
		String[] terms = termsString.split(TERM_SEPARATOR);
		for (String term : terms) {
			if (!this.termsMap.containsKey(term)) {
				this.termsMap.put(term, FIRST_OCCURRENCE);
			} else {
				this.termsMap.put(term, this.termsMap.get(term) + 1);
			}
		}
	}
	
	public Tree convert() {
		
		// Sorting the terms according to their frequency
		this.orderedTerms = new ArrayList<TermTuple>();
		for (String term : this.termsMap.keySet()) {
			orderedTerms.add(new TermTuple(term, this.termsMap.get(term)));
		}
		Collections.sort(orderedTerms);
		
		// Building the tree
		this.aTree = new Tree();
		
		Schema schema = new Schema(); // adding columns to the data table
		schema.addColumn("term", String.class);
		schema.addColumn("freq", int.class, 1);
		aTree.getNodes().addColumns(schema);
		
		// Adding Nodes to the tree
		TermTuple rootTuple = this.orderedTerms.get(0);
		Node root = addRoot(rootTuple);
		
		addChildren(root, 0);
		
		return aTree;
	}
	
	public Tree convert2() {
		
		// Sorting the terms according to their frequency
		this.orderedTerms = new ArrayList<TermTuple>();
		for (String term : this.termsMap.keySet()) {
			orderedTerms.add(new TermTuple(term, this.termsMap.get(term)));
		}
		Collections.sort(orderedTerms);
		
		// Building the tree
		this.aTree = new Tree();
		
		Schema schema = new Schema(); // adding columns to the data table
		schema.addColumn("term", String.class);
		schema.addColumn("freq", int.class, 1);
		aTree.getNodes().addColumns(schema);
		
		// Adding Nodes to the tree
		TermTuple rootTuple = this.orderedTerms.get(0);
		Node root = addRoot(rootTuple);
		
		TermTuple lastAddedTuple = rootTuple;
		int lastAddedFreq = rootTuple.getFrequency();
		
		int nextIndex = 1;
		Node lastAddedNode = root;
		
		List<Node> nextNodes = new LinkedList<Node>();
		nextNodes.add(lastAddedNode);
		for (int i = 1; i < this.orderedTerms.size(); i++) {
			
			TermTuple tuple = this.orderedTerms.get(i); 
			if (tuple.getFrequency() <= lastAddedFreq) {
				
				//add child
				lastAddedFreq -= tuple.getFrequency();
				
				Node child = this.addChild(lastAddedNode, tuple);
				nextNodes.add(child);
				
			} else {
				
				// go to next tuple
				lastAddedTuple = this.orderedTerms.get(nextIndex);
				lastAddedNode = nextNodes.get(nextIndex);
				lastAddedFreq = lastAddedTuple.getFrequency();
				nextIndex++;
				i--;
			}
		}
		
		return aTree;
	}
	
	private void addChildren(Node parent, int parentIndex) {
		
		TermTuple leftChildTuple, rightChildTuple;
		
		int leftChildIndex = getLeftChildIndex(parentIndex);
		if (leftChildIndex < orderedTerms.size()) {
			
			leftChildTuple = this.orderedTerms.get(leftChildIndex);
			Node leftChild = addChild(parent, leftChildTuple);
			
			addChildren(leftChild, leftChildIndex);
		}
		
		int rightChildIndex = getRightChildIndex(parentIndex);
		if (rightChildIndex < orderedTerms.size()) {
			
			rightChildTuple = this.orderedTerms.get(rightChildIndex);
			Node rightChild = addChild(parent, rightChildTuple);
			
			addChildren(rightChild, rightChildIndex);
		}
	}
	
	private Node addRoot(TermTuple rootTuple) {
		
		Node root = this.aTree.addRoot();
		root.set(TERM_DATA, rootTuple.getTerm());
		root.set(TERM_FREQUENCY_DATA, rootTuple.getFrequency());
		
		return root;
	}
	
	private Node addChild(Node parent, TermTuple childTuple) {
		
		Node child = this.aTree.addChild(parent);
		child.set(TERM_DATA, childTuple.getTerm());
		child.set(TERM_FREQUENCY_DATA, childTuple.getFrequency());
		
		return child;
	}
	
	private int getLeftChildIndex(int i) {
		return 2*i + 1;
	}
	
	private int getRightChildIndex(int i) {
		return 2*i + 2;
	}
	
	public static void main(String[] args) {
		UILib.setPlatformLookAndFeel();
		
		Terms2TreeConverter converter = new Terms2TreeConverter("How much wood wood wood would a woodchuck chuck if " +
        	"a woodchuck could chuck wood");
		Graph graph = converter.convert2();

		JFrame frame = new JFrame("Starburst Demo for prefuse");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(StarburstDemo.demo(graph, "term", "freq"));
		frame.pack();
		frame.setVisible(true);
	}
}