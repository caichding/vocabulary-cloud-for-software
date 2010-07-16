package br.edu.ufcg.gmf.actions;

import java.io.IOException;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.jface.dialogs.MessageDialog;

@SuppressWarnings("restriction")
public class TermsCloudGenerator implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {
	private IWorkbenchWindow window;
	private ISelection userSelection;
	private PackageExplorerHierarchy userSelectionType;
	private TreeMap<String, Integer> wordCloudMap = new TreeMap<String, Integer>();
	private String wordCloudString;
	private static boolean includeProject, includePackagesFolders, includePackages, includeClass, includeSuperClass, includeInterfaces, includeMethods, includeParameters;

	public TermsCloudGenerator() {
	}

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
			//Error.printStackTrace();
		} catch (Exception Error) {
			openMessageDialog("Sorry! An exception occurred, could not generate the Word Cloud.");
			//Error.printStackTrace();
		} 
	}
	
	private void requestUserPreferences() {
		Preferences.run(userSelectionType);		
	}

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
				} else if (firstElement instanceof SourceMethod) { // Method in any Class
					userSelectionType = PackageExplorerHierarchy.SOURCE_METHOD;
				} else {
					openMessageDialog("Please, select something in Package Explorer.");
				}
			}
		} else if (userSelection instanceof TextSelection) {
			userSelectionType = PackageExplorerHierarchy.SELECTION;
		} 
	}
	
	private void showWCImageInBrowser() throws IOException {
		if (!wordCloudString.isEmpty()) {
			WordCloudImageRetrieval.run(wordCloudString);
			//openMessageDialog("Tag Cloud generated successfully!");
		}
	}
	
	private void loadMapToWCString() {
		//while (!(wordCloudMap.isEmpty())) {
		//	wordCloudString += getMostImportantWordInWCMap() + " ";
		//}
		for (Entry<String, Integer> entry : wordCloudMap.entrySet()) {
			for (int i = 0; i < entry.getValue(); i++) {
				wordCloudString += entry.getKey() + " ";
			}
		} 
	}
	
	private void loadWordsToMap() throws CoreException {
		if (!(userSelectionType.equals(PackageExplorerHierarchy.SELECTION))) {
			Object firstElement = ((TreeSelection) userSelection).getFirstElement();
			
			if (userSelectionType.equals(PackageExplorerHierarchy.I_JAVA_PROJECT)) {
				loadJavaProjectInformations((IJavaProject) firstElement);
			} else if (userSelectionType.equals(PackageExplorerHierarchy.I_PACKAGE_FRAGMENT_ROOT)) {
				loadPackageFragmentRootInformations((IPackageFragmentRoot) firstElement);
			} else if (userSelectionType.equals(PackageExplorerHierarchy.I_PACKAGE_FRAGMENT)) {
				loadPackageFragmentInformations((IPackageFragment) firstElement);
			} else if (userSelectionType.equals(PackageExplorerHierarchy.I_COMPILATION_UNIT)) {
				loadCompilationUnitInformations((ICompilationUnit) firstElement);
			} else if (userSelectionType.equals(PackageExplorerHierarchy.SOURCE_TYPE)) {
				loadSourceTypeInformations((SourceType) firstElement);
			} else if (userSelectionType.equals(PackageExplorerHierarchy.SOURCE_METHOD)) {
				loadSourceMethodsInformations((SourceMethod) firstElement);
			}
		} else {
			TextSelection selectionText = (TextSelection) userSelection;
			wordCloudString = selectionText.getText();
		}
	}
	
	private void loadJavaProjectInformations(IJavaProject project) throws JavaModelException {
		if (includeProject) {
			addInWCMap(project.getElementName(), PackageExplorerLevels.I_JAVA_PROJECT.ordinal());
		} for (IPackageFragmentRoot pfr: project.getPackageFragmentRoots()) {
			if (!(pfr instanceof JarPackageFragmentRoot)) {
				loadPackageFragmentRootInformations(pfr);
			}
		}
	}
	
	private void loadPackageFragmentRootInformations(IPackageFragmentRoot packageFragmentRoot) throws JavaModelException {
		if (includePackagesFolders) {
			addInWCMap(packageFragmentRoot.getElementName(), PackageExplorerLevels.I_PACKAGE_FRAGMENT_ROOT.ordinal());
		} for (IJavaElement javaElement : packageFragmentRoot.getChildren()) {
			if (javaElement instanceof IPackageFragment) {
				loadPackageFragmentInformations((IPackageFragment) javaElement);
			}
		}
	}
	
	private void loadPackageFragmentInformations(IPackageFragment pack) throws JavaModelException {
		if (includePackages) {
			addInWCMap(pack.getElementName(), PackageExplorerLevels.I_PACKAGE_FRAGMENT.ordinal());	
		} for (ICompilationUnit compilation : pack.getCompilationUnits()) {
			for (IType type : compilation.getTypes()) {
				if (type instanceof SourceType) {
					SourceType sourceTypeClass = (SourceType) type;
					loadSourceTypeInformations(sourceTypeClass);
				}
			}
		}
	}
	
	private void loadCompilationUnitInformations(ICompilationUnit clasS) throws JavaModelException {
		for (IType type : clasS.getTypes()) {
			if (type instanceof SourceType) {
				SourceType sourceType = (SourceType) type;
				loadSourceTypeInformations(sourceType);
			}
		}
	}
	
	private void loadSourceTypeInformations(SourceType sourceTypeClass) throws JavaModelException {
		if (includeClass) {
			addInWCMap(sourceTypeClass.getElementName(), PackageExplorerLevels.SOURCE_TYPE.ordinal());
		} if (includeSuperClass && sourceTypeClass.getSuperclassName() != null) {
			addInWCMap(sourceTypeClass.getSuperclassName());
		} if (includeInterfaces) {
			for (String interfacE : sourceTypeClass.getSuperInterfaceNames()) {
				addInWCMap(interfacE);
			}
		} for (IMethod method : sourceTypeClass.getMethods()) {
				loadSourceMethodsInformations(method);
		}
	}
	
	private void loadSourceMethodsInformations(IMethod method) throws JavaModelException {
		if (includeMethods) {
			addInWCMap(method.getElementName(), PackageExplorerLevels.SOURCE_METHOD.ordinal());
		} if (includeParameters) {
			for (String parameter : method.getParameterNames()) {
				addInWCMap(parameter);
			}
		}
	}
	
	private void addInWCMap(String word) {
		if (!(wordCloudMap.containsKey(word))) {
			wordCloudMap.put(word, 0);
		} wordCloudMap.put(word, wordCloudMap.get(word) + 1);
	}
	
	private void addInWCMap(String word, int amount) {
		for (int iteration = 0; iteration < amount; iteration++) {
			if (iteration == 0 && !(wordCloudMap.containsKey(word))) {
				wordCloudMap.put(word, 1);
				continue;
			} wordCloudMap.put(word, wordCloudMap.get(word) + 1);
		}
	}
	
	private String getMostImportantWordInWCMap() {
		String wordOfMoreFrequently = null;
		int moreFrequently = (Integer) wordCloudMap.values().toArray()[0];
		
		for (int frequently : wordCloudMap.values()) {
			if (frequently > moreFrequently) {
				moreFrequently = frequently;
			}
		}
		
		for (Entry<String, Integer> entry : wordCloudMap.entrySet()) {
			if (entry.getValue() == moreFrequently) {
				wordOfMoreFrequently = entry.getKey();
				wordCloudMap.remove(wordOfMoreFrequently);
				break;
			}
		}
		return wordOfMoreFrequently;
	}
	
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
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
	
	}
}