package br.edu.ufcg.gmf.actions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import br.edu.ufcg.gmf.astexplorer.ASTVocabularyExtractor;
import br.edu.ufcg.gmf.astexplorer.VocabularyConverter;
import br.edu.ufcg.gmf.imageRetrieval.WordCloudViewer;

@SuppressWarnings("restriction")
public class TermsCloudGenerator implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {
	
	private IWorkbenchWindow window;
	private ISelection userSelection;
	private PackageExplorerHierarchy userSelectionType;
	
	private ASTVocabularyExtractor vocExtractor;
	private Map<String, Double> wordCloudMap = new HashMap<String, Double>();
	private String wordCloudString;

	public void run(IAction action) {
		wordCloudString = "";
		wordCloudMap.clear();
		
		try {
			identifyUserSelectionType();
			if (userSelectionType != null) {
				requestUserPreferences();
				while (!(Preferences.getUserPreferencesReceived())) { } // Wait ...
				loadWordsToMap();
				loadMapToWCString();
				showWCImageInBrowser();
			}
		} catch (IOException Error) {
			openMessageDialog("You probably do not have internet connection. It's necessary.");
		} catch (Exception Error) {
			openMessageDialog("Sorry! An exception occurred, could not generate the Word Cloud. \n" + Error.getMessage());
		} 
	}
	
	private void requestUserPreferences() {
		Preferences.run(userSelectionType);		
	}

	// extracts user selection
	private void identifyUserSelectionType() {
		if (userSelection instanceof TreeSelection) {
			Object firstElement = ((TreeSelection) userSelection).getFirstElement();
			
			if (firstElement == null) {
				openMessageDialog("Please, select something in Package Explorer.");
			} else {
				if (firstElement instanceof IJavaProject) { // Any Java Project in Eclipse
					userSelectionType = PackageExplorerHierarchy.I_JAVA_PROJECT;
				} else if (firstElement instanceof IPackageFragmentRoot) { // Folder Package's in any Project
					userSelectionType = PackageExplorerHierarchy.I_PACKAGE_FRAGMENT_ROOT;
				} else if (firstElement instanceof IPackageFragment) { // Package in any Folder
					userSelectionType = PackageExplorerHierarchy.I_PACKAGE_FRAGMENT;
				} else if (firstElement instanceof ICompilationUnit) { // Class in any Package
					userSelectionType = PackageExplorerHierarchy.I_COMPILATION_UNIT;
				} else if(firstElement instanceof SourceType) {
					userSelectionType = PackageExplorerHierarchy.SOURCE_TYPE;
				} else {
					openMessageDialog("Please, select something in Package Explorer.");
				}
			}
		} else if (userSelection instanceof TextSelection) {
			userSelectionType = PackageExplorerHierarchy.SELECTION;
		} 
	}
	
	// load terms from user's selected elements
	private void loadWordsToMap() throws Exception {
		
		this.vocExtractor = new ASTVocabularyExtractor();
		this.vocExtractor.setProperties(includeProject, includePackagesFolders, includePackages, includeClass, 
				includeSuperClass, includeInterfaces, includeMethods, includeParameters);
		
		if (!(userSelectionType.equals(PackageExplorerHierarchy.SELECTION))) {
			Object selectedElement = ((TreeSelection) userSelection).getFirstElement();
			
			if (userSelectionType.equals(PackageExplorerHierarchy.I_JAVA_PROJECT)) {
				this.vocExtractor.extractProjectInfo((IJavaProject) selectedElement);
			} else if (userSelectionType.equals(PackageExplorerHierarchy.I_PACKAGE_FRAGMENT_ROOT)) {
				this.vocExtractor.extractProjectFolderInfo(0, (IPackageFragmentRoot) selectedElement);
			} else if (userSelectionType.equals(PackageExplorerHierarchy.I_PACKAGE_FRAGMENT)) {
				this.vocExtractor.extractPackageInfo(0, (IPackageFragment) selectedElement);
			} else if (userSelectionType.equals(PackageExplorerHierarchy.I_COMPILATION_UNIT)) {
				this.vocExtractor.extractCompilationUnitInfo((ICompilationUnit) selectedElement);
			} else if (userSelectionType.equals(PackageExplorerHierarchy.SOURCE_TYPE)) {
				this.vocExtractor.extractTypeInfo(0, (SourceType) selectedElement);
			} 
		} else {
			TextSelection selectionText = (TextSelection) userSelection;
			this.wordCloudString = selectionText.getText();
		}
	}
	
	private void loadMapToWCString() {
		
		if (this.vocExtractor != null) {
			
			VocabularyConverter vocConverter = new VocabularyConverter(this.vocExtractor.getVocabulary());
			this.wordCloudMap = vocConverter.convertToFrequencyMap();
			
			for (String term : wordCloudMap.keySet()) {
				long frequency = Math.round(wordCloudMap.get(term));
				for (int i = 0; i < frequency; i++)
					wordCloudString += term + " ";
			}
		}
	}
	
	private void showWCImageInBrowser() throws IOException {
		if (!wordCloudString.isEmpty()) {
			wordCloudString = wordCloudString.trim();
//			WordCloudImageRetrieval.run(wordCloudString);
			
			JFrame frame = new JFrame("WordCloudView");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			JPanel graphView = WordCloudViewer.run(wordCloudString);
			frame.setContentPane(graphView);
			
			frame.pack();
			frame.setVisible(true);
			//openMessageDialog("Tag Cloud generated successfully!");
		}
	}
	
	private static boolean includeProject, includePackagesFolders, includePackages, includeClass, 
		includeSuperClass, includeInterfaces, includeMethods, includeParameters;
	
	public static void setIncludeProject(boolean newValue) {
		includeProject = newValue;
	}

	public static void setIncludePackagesFolders(boolean newValue) {
		includePackagesFolders = newValue;
	}
	
	public static void setIncludePackages(boolean newValue) {
		includePackages = newValue;
	}
	
	public static void setIncludeClass(boolean newValue) {
		includeClass = newValue;
	}

	public static void setIncludeSuperClass(boolean newValue) {
		includeSuperClass = newValue;
	}
	
	public static void setIncludeInterfaces(boolean newValue) {
		includeInterfaces = newValue;
	}
	
	public static void setIncludeMethods(boolean newValue) {
		includeMethods = newValue;
	}
	
	public static void setIncludeParameters(boolean newValue) {
		includeParameters = newValue;
	}
	
	private void openMessageDialog(String message) {
		MessageDialog.openInformation(
				window.getShell(),
				"Word Cloud Generator",
				message);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.userSelection = selection;
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {}
}