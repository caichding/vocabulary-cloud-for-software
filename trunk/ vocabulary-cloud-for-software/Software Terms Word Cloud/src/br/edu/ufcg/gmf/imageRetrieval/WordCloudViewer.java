package br.edu.ufcg.gmf.imageRetrieval;

import javax.swing.JPanel;

import prefuse.data.Graph;
import prefuse.util.ui.UILib;
import br.edu.ufcg.gmf.util.Terms2TreeConverter;
import ca.utoronto.cs.prefuseextensions.demo.StarburstDemo;

public class WordCloudViewer {

	public static JPanel run(String wordCloudString) {
		
		UILib.setPlatformLookAndFeel();
		Terms2TreeConverter converter = new Terms2TreeConverter(wordCloudString);
		Graph graph = converter.convert2();

//		JFrame frame = new JFrame("WordCloudView");
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setContentPane(StarburstDemo.demo(graph, "term", "freq"));
//		frame.pack();
//		frame.setVisible(true);
		
		return StarburstDemo.demo(graph, "term", "freq");
	}
}