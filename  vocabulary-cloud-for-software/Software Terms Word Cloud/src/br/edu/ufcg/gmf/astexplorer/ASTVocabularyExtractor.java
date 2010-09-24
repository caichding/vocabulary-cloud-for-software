package br.edu.ufcg.gmf.astexplorer;

import irutils.util.GeneralVocabularyExtractor;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceType;

@SuppressWarnings("restriction")
public class ASTVocabularyExtractor extends GeneralVocabularyExtractor {

	private static final String PROJECT_NAME = "name";
	private static final String PROJECT_FOLDER = "folder";
	private static final String PROJECT_PACKAGE = "package";
	private static final String PROJECT_CLASS = "class";
	private static final String PROJECT_INTERFACE = "interface";
	private static final String PROJECT_SUPERCLASS = "superclass";
	private static final String PROJECT_ATTRIBUTE = "attribute";
	private static final String PROJECT_METHOD = "method";
	
	private int folderCount = 0; 
	private int packageCount = 0;
	private int classCount = 0;
	private int interfaceCount = 0;
	private int superclassCount = 0;
	private int attributeCount = 0;
	private int methodCount = 0;
	
	private Map<String, ASTNode> equivalentEntities;
	private Set<String> allClassNames, allSuperclassNames;
	
	public ASTVocabularyExtractor() {
		super();
		
		this.allClassNames = new HashSet<String>();
		this.allSuperclassNames = new HashSet<String>();
		this.equivalentEntities = new HashMap<String, ASTNode>();
	}
	
	public Map<String, ASTNode> getEquivalentEntities() {
		return this.equivalentEntities;
	}
	
	public void extractProjectInfo(IJavaProject project) throws JavaModelException, InvocationTargetException, InterruptedException {
		
		// adding project name
		if (includeProject)
			addEntityTermsToVocabulary(PROJECT_NAME, project.getElementName());
		
		for (IPackageFragmentRoot pfr: project.getPackageFragmentRoots())
			if (pfr instanceof PackageFragmentRoot) {
				this.folderCount++;
				extractProjectFolderInfo(folderCount, pfr);
			}
	}
	
	public void extractProjectFolderInfo(int folderIndex, IPackageFragmentRoot packageFragmentRoot) throws JavaModelException, InvocationTargetException, InterruptedException {
		
		if (!(packageFragmentRoot instanceof JarPackageFragmentRoot)) {
			
			// adding folder name
			if (includePackagesFolders) {
				String pfrId = PROJECT_FOLDER + "100" + folderIndex;
				addEntityTermsToVocabulary(pfrId, packageFragmentRoot.getElementName());
			}
			
			filterClassesAndSuperclasses(packageFragmentRoot);
			for (IJavaElement javaElement : packageFragmentRoot.getChildren())
				if (javaElement instanceof IPackageFragment) {
					this.packageCount++;
					extractPackageInfo(packageCount, (IPackageFragment) javaElement);
				}
		}
	}
	
	public void extractPackageInfo(int packageIndex, IPackageFragment pckg) throws JavaModelException, InvocationTargetException, InterruptedException {
		
		if (this.allClassNames.isEmpty() || this.allSuperclassNames.isEmpty()) {
			filterClassesAndSuperclasses((IPackageFragmentRoot) pckg.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT));
		}
		
		// adding package name
		if (includePackages) {
			String pckgId = PROJECT_PACKAGE + "100" + packageIndex;
			addEntityTermsToVocabulary(pckgId, pckg.getElementName());
		}
		
		for (ICompilationUnit compilationUnit : pckg.getCompilationUnits())
			extractCompilationUnitInfo(compilationUnit);
	}
	
	@SuppressWarnings("unchecked")
	public void extractCompilationUnitInfo(ICompilationUnit compilationUnit) throws JavaModelException {
		
		if (this.allClassNames.isEmpty() || this.allSuperclassNames.isEmpty()) {
			filterClassesAndSuperclasses((IPackageFragmentRoot) compilationUnit.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT));
		}
		
		char[] sourceCode = compilationUnit.getSource().toCharArray();
		ASTNode tree = getASTreeFromSourceCode(sourceCode);
		CompilationUnit astCompilationUnit = (CompilationUnit) tree;
		
		// for all types declared in this compilation unit
		for (AbstractTypeDeclaration type : (List<AbstractTypeDeclaration>) astCompilationUnit.types()) {
			
			if (type instanceof TypeDeclaration) {
				
				String typeId = null;
				TypeDeclaration typeDeclaration = (TypeDeclaration) type;
				String typeFullyQualifiedName = getTypeFullyQualifiedName(typeDeclaration);
				
				// verifies if the the type is a class, interface or superclass
				typeId = verifyType(typeDeclaration, typeFullyQualifiedName);
				
				if (typeId != null) {
					this.equivalentEntities.put(typeId, tree);
					extractTypeInfo(typeId, typeDeclaration);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void extractTypeInfo(int typeIndex, SourceType type) throws JavaModelException {
		
		ICompilationUnit compilationUnit = type.getCompilationUnit();
		char[] sourceCode = compilationUnit.getSource().toCharArray();
		
		ASTNode tree = getASTreeFromSourceCode(sourceCode);
		CompilationUnit astCompilationUnit = (CompilationUnit) tree;
		
		// for all types declared in this compilation unit
		for (AbstractTypeDeclaration compUnitType : (List<AbstractTypeDeclaration>) astCompilationUnit.types()) {
			if (compUnitType instanceof TypeDeclaration) {
				
				String typeId = PROJECT_CLASS + "100" + typeIndex;
				TypeDeclaration typeDeclaration = (TypeDeclaration) compUnitType;
				
				if (typeDeclaration.getName().toString().equals(type.getElementName())) {
					this.equivalentEntities.put(typeId, tree);
					extractTypeInfo(typeId, typeDeclaration);
				}
			}
		}
	}
	
	private void extractTypeInfo(String typeId, TypeDeclaration type) {
		
		// adding class and fields' info
		if (this.includeClasses) {
			addEntityTermsToVocabulary(typeId, type.getName().getIdentifier());
			for (FieldDeclaration field : ((TypeDeclaration) type).getFields()) {
				this.attributeCount++;
				extractFieldsInfo(field, PROJECT_ATTRIBUTE + "100" + this.attributeCount);
			}
		}
		
		// adding methods' info
		for (MethodDeclaration method : ((TypeDeclaration) type).getMethods()) {
			this.methodCount++;
			extractMethodsInfo(method, PROJECT_METHOD + "100" + this.methodCount);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void extractFieldsInfo(FieldDeclaration field, String attributeId) {
		for (VariableDeclarationFragment vdf : (List<VariableDeclarationFragment>) field.fragments())
			addEntityTermsToVocabulary(attributeId, vdf.getName().getIdentifier());
	}
	
	@SuppressWarnings("unchecked")
	private void extractMethodsInfo(MethodDeclaration method, String methodId) {
		
		List<String> methodTerms = new LinkedList<String>();
		
		// adding method name
		if (this.includeMethods) 
			addStrings(method.getName().getIdentifier(), methodTerms);
		
		// adding method parameter
		if(this.includeParameters)
			for (SingleVariableDeclaration parameter : (List<SingleVariableDeclaration>) method.parameters()) {
				addStrings(parameter.getName().getIdentifier(), methodTerms);
			}
		
		// adding local variables
		if (this.includeMethods)
			for (Statement statement : (List<Statement>) method.getBody().statements())
				if (statement instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement vds = (VariableDeclarationStatement) statement;
					for (VariableDeclarationFragment vdf : (List<VariableDeclarationFragment>) vds.fragments())
						addStrings(vdf.getName().getIdentifier(), methodTerms);
				}
		
		if (!methodTerms.isEmpty()) this.vocabulary.put(methodId, methodTerms);
	}
	
	private ASTNode getASTreeFromSourceCode(char[] source) {
		
		ASTParser parser= ASTParser.newParser(AST.JLS3); // handles JDK 1.0, 1.1, 1.2, 1.3, 1.4, 1.5
		
		parser.setSource(source);
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(true);
		
		return parser.createAST(null);
	}
	
	private String getTypeFullyQualifiedName(TypeDeclaration type) {
		
		CompilationUnit compilationUnit = (CompilationUnit) type.getParent();
		String compilationUnitQualifiedName = compilationUnit.getPackage().getName().getFullyQualifiedName();
		
		return compilationUnitQualifiedName + "." + type.getName().getIdentifier();
	}
	
	private String verifyType(TypeDeclaration type, String typeFullyQualifiedName) throws JavaModelException {
		
		if (type.isInterface()) {
			
			this.interfaceCount++;
			if (this.includeInterfaces)
				return PROJECT_INTERFACE + "100" + this.interfaceCount;
			
		} else if (this.allClassNames.contains(typeFullyQualifiedName)) {
			
			this.classCount++;
			if (this.includeClasses)
				return PROJECT_CLASS + "100" + this.classCount;
			
		} else if (this.allSuperclassNames.contains(typeFullyQualifiedName)) {
			
			this.superclassCount++;
			if (this.includeSuperClasses)
				return PROJECT_SUPERCLASS + "100" + this.classCount;
		}
		
		return null;
	}
	
	private void filterClassesAndSuperclasses(IPackageFragmentRoot pfr) throws JavaModelException {
		
		for (IJavaElement javaElement : pfr.getChildren())
			if (javaElement instanceof IPackageFragment)
				filterAllClassesFromPackage((IPackageFragment) javaElement);
		
		for (IJavaElement javaElement : pfr.getChildren())
			if (javaElement instanceof IPackageFragment)
				filterAllSuperclassesFromPackage((IPackageFragment) javaElement);
	}
	
	private void filterAllClassesFromPackage(IPackageFragment pckg) throws JavaModelException {
		
		if (pckg.getCompilationUnits().length != 0) {
			for (ICompilationUnit compilation : pckg.getCompilationUnits())
				for (IType type : compilation.getTypes())
					if (type instanceof SourceType && type.isClass() && !type.isInterface()) { // and is not interface
						SourceType sourceTypeClass = (SourceType) type;
						this.allClassNames.add(sourceTypeClass.getFullyQualifiedName());
					}
		}
	}
	
	private void filterAllSuperclassesFromPackage(IPackageFragment pckg) throws JavaModelException {
		
		if (pckg.getCompilationUnits().length != 0) {
			for (ICompilationUnit compilation : pckg.getCompilationUnits())
				for (IType type : compilation.getTypes())
					if (type instanceof SourceType && type.isClass()) {
						
						ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
						IType[] typeSuperclasses = typeHierarchy.getAllSuperclasses(type);
						
						for (IType superclass : typeSuperclasses) {
							String typePath = superclass.getFullyQualifiedName();
							
							if (this.allClassNames.contains(typePath)) {
								this.allClassNames.remove(typePath);
								this.allSuperclassNames.add(typePath);
							}
						}
					}
		}
	}
	
	private void addEntityTermsToVocabulary(String entityId, String entityName) {
//		System.out.println(entityId + " " + entityName);
		List<String> entityTerms = new LinkedList<String>();
		addStrings(entityName, entityTerms);
		
		this.vocabulary.put(entityId, entityTerms);
	}
	
	public static IJavaProject createJavaProjectFromPath(String projectPath) throws JavaModelException {
		
		IPath javaProjectFullPath = new Path(projectPath);
		
		IJavaProject javaProject = new JavaProject();
		javaProject = (IJavaProject) javaProject.findElement(javaProjectFullPath);
		
		return javaProject;
	}
	
	private boolean includeProject = false, includePackagesFolders = false, includePackages = false,
		includeClasses = false, includeSuperClasses = false, includeInterfaces = false, 
		includeMethods = false, includeParameters = false;
	
	public void setProperties(boolean includeProject, boolean includePackagesFolders, boolean includePackages,
			boolean includeClasses, boolean includeSuperClasses, boolean includeInterfaces,	
			boolean includeMethods, boolean includeParameters) {
		
		this.includeProject = includeProject; this.includePackagesFolders = includePackagesFolders;
		this.includePackages = includePackages; this.includeClasses = includeClasses;
		this.includeSuperClasses = includeSuperClasses; this.includeInterfaces = includeInterfaces; 
		this.includeMethods = includeMethods; this.includeParameters = includeParameters;
	}
	
	public static void main(String[] args) throws JavaModelException, InvocationTargetException, InterruptedException {
		
		String projectPath = args[0];
		
		IJavaProject javaProject = ASTVocabularyExtractor.createJavaProjectFromPath(projectPath);
		
		ASTVocabularyExtractor vocabularyExtractor = new ASTVocabularyExtractor();
		vocabularyExtractor.setProperties(true, true, true, true, true, true, true, true);
		vocabularyExtractor.extractProjectInfo(javaProject);
	}
}