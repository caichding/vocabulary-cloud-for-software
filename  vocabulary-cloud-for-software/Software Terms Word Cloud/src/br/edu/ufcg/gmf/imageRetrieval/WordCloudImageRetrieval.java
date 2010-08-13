package br.edu.ufcg.gmf.imageRetrieval;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import br.edu.ufcg.gmf.util.ClientHttpRequest;

public class WordCloudImageRetrieval {

	public static void run(String wordCloudString) throws IOException {
		// Sending HTML "POST" Request
		URL url = new URL("http://www.wordle.net/compose");
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("method", "post");
		parameters.put("action", "http://www.wordle.net/compose");
		parameters.put("target", "_blank");
		// the text
		parameters.put("text", wordCloudString);

		// Getting the response
		InputStream readerStream = ClientHttpRequest.post(url, parameters);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				readerStream));
		StringBuilder content = new StringBuilder();
		String line = "";
		boolean firstLine = true;
		while ((line = reader.readLine()) != null) {
			if (!firstLine) {
				System.out.println(line);
				content.append(line);
			} else {
				firstLine = false;
			}
		}
		reader.close();

		// Saving HTML Response on a temporary file
		File tempFile = File.createTempFile("htmlResponse", ".html");
		PrintStream stream = new PrintStream(new FileOutputStream(tempFile));
		stream.println(content.toString());
		stream.close();

		// Showing content
		Desktop.getDesktop().open(tempFile);
	}
}
