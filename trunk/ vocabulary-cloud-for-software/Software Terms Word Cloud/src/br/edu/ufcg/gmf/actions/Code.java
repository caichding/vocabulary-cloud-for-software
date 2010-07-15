package br.edu.ufcg.gmf.actions;

public class Code {
	private String token;
	private final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	public Code(String token) {
		this.token = token;
	}
	
	public String getToken() {
		return token;
	}
	
	private void removeAll(String inicio, String fim) {
		int indiceInicio;
		while ((indiceInicio = token.indexOf(inicio)) != -1) {
			token = token.replace(token.substring(indiceInicio, token.indexOf(fim) + fim.length()), "");
		}
	}
	
	public void removeLineEnd() {
		token = token.replaceAll(LINE_SEPARATOR, "");
	}
	
	public void removePackage() {
		removeAll("package", ";");
	}
	
	public void removeImports() {
		removeAll("import", ";");
	}
	
	public void removeCaracteresEspeciais() {
		char[] charsText = token.toCharArray();
		char[] charsReservedText = {'{', '}', '[', ']', '(', ')', ' ', '	', ';', '.', '=', ',', '+', '!', ':', '<', '>', 39};
		boolean ehCaractereEspecial;
		String newStr = "";
		
		for (char charText: charsText) {
			ehCaractereEspecial = false;
			for (char charReservedText: charsReservedText) {
				if (charText == charReservedText) {
					ehCaractereEspecial = true;
					break;
				}
			} if (!(ehCaractereEspecial)) {
				newStr += charText;
			} else if (!(newStr.endsWith(" "))){
				newStr += " ";
			}
		}
		token = newStr;
	}
	
	public void removeLexico() {
		String[] palavrasLexico = {"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while", /**/ "String", "public", "throw", "if", "this", "super", "private", "return", "null"};

		for (String palavraLexico: palavrasLexico) {
			token = token.replaceAll(palavraLexico + " ", "");
		}
	}
	
	public void removeJavaDocComments() {
		removeAll("/**", "*/");
	}
	
	public void removeStrings() {
		int indicePrimeiraAspa;
		while (token.contains("\\\"")) {
			token = token.replaceFirst("\\\"", "");
		}

		while ((indicePrimeiraAspa = token.indexOf("\"")) != -1) {
			token = token.replaceFirst("\"", "");
			int prox = token.indexOf("\"") +1;
			token = token.replaceFirst(token.substring(indicePrimeiraAspa, prox), "");
		}
	}
	
	public void removeExceptions() {
		String newStr = "";
		for (String palavra: token.split(" ")) {
			if (!(palavra.contains("Exception"))) {
				newStr += palavra + " ";
			}
		}
		token = newStr;
	}
	
	public String[] split(String regex) {
		return token.split(regex);
	}
	
	public String toString() {
		return token;
	}
}