package Part2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.compiler.batch.Main;

import step2.MethodDeclarationVisitor;
import step2.TypeDeclarationVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

public class Parser {

	private static String projectSourcePath;
	private static String jrePath = "/System/Library/Frameworks/JavaVM.framework/";

	public static void main(String[] args) throws IOException {
		
		PackageDeclarationVisitor packageVisitor = new PackageDeclarationVisitor();

        // Utilisation du ClassLoader pour charger la ressource du fichier.
        ClassLoader classLoader = Main.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("projectPath.txt");
		
		int numberClasses = 0;
		int numberMethods = 0;
		int numberPackages = 0;
		long numberLinesInMethods = 0;
		int numberAttributs = 0;
		
		Map<String, Integer> classMethodCountMap = new HashMap<String, Integer>();
		Map<String, Integer> classAttributCountMap = new HashMap<String, Integer>();

		String projectPath = (getLink(inputStream));
		projectSourcePath = projectPath + "/src";
		
		// read java files
		final File folder = new File(projectSourcePath);
		ArrayList<File> javaFiles = listJavaFilesForFolder(folder);

		//
		for (File fileEntry : javaFiles) {
			String content = FileUtils.readFileToString(fileEntry);
			// System.out.println(content);

			CompilationUnit parse = parse(content.toCharArray());

			// print methods info
			//printMethodInfo(parse);

			// print variables info
			//printVariableInfo(parse);
			
			//print method invocations
			//printMethodInvocationInfo(parse);
			numberClasses += countNbClasses(parse);
			numberMethods += countNbMethods(parse);
			countNbPackages(parse, packageVisitor);
			numberLinesInMethods += countMethodsLines(parse);
			numberAttributs += countNbAttributs(parse);
			
			top10PercentClassesByMethodCount(parse, classMethodCountMap);
			top10PercentClassesByAttributCount(parse, classAttributCountMap);

		}
		
		numberPackages = packageVisitor.getPackages().size();
				
		float averageMethodsByClass = numberMethods / (float)numberClasses;
		float averageLinesByMethod = numberLinesInMethods / (float)numberMethods;
		float averageAttributsByClass = numberAttributs / (float)numberClasses;
		
		System.out.println("Nombre de classes : " + numberClasses);
		System.out.println("Nombre de lignes de code de l'application :");
		System.out.println("Nombre de methodes : " + numberMethods);
		System.out.println("Nombre de packages : " + numberPackages);
		System.out.println("Nombre d'attibuts : " + numberAttributs);
		
		
		System.out.println("Nombre moyen de methodes par classe : " + averageMethodsByClass);
		System.out.println("Nombre moyen de lignes de code par méthode : " + averageLinesByMethod);
		System.out.println("Nombre moyen d’attributs par classe : " + averageAttributsByClass);
		
		
		Map<String, Integer> classMethodCountMapSorted = sortDescending(classMethodCountMap);
		
		Map<String, Integer> classAttributCountMapSorted = sortDescending(classAttributCountMap);
		
        for (Map.Entry<String, Integer> entry : classMethodCountMapSorted.entrySet()) {
            System.out.println("Classe : " + entry.getKey() + ", Nombre de méthodes : " + entry.getValue());
        }
        
        for (Map.Entry<String, Integer> entry : classAttributCountMapSorted.entrySet()) {
            System.out.println("Classe : " + entry.getKey() + ", Nombre d'attributs : " + entry.getValue());
        }
		
	}

	// read all java files from specific folder
	public static ArrayList<File> listJavaFilesForFolder(final File folder) {
		ArrayList<File> javaFiles = new ArrayList<File>();
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				javaFiles.addAll(listJavaFilesForFolder(fileEntry));
			} else if (fileEntry.getName().contains(".java")) {
				//System.out.println(fileEntry.getName());
				javaFiles.add(fileEntry);
			}
		}

		return javaFiles;
	}

	// create AST
	private static CompilationUnit parse(char[] classSource) {
		ASTParser parser = ASTParser.newParser(AST.JLS4); // java +1.6
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
 
		parser.setBindingsRecovery(true);
 
		Map options = JavaCore.getOptions();
		parser.setCompilerOptions(options);
 
		parser.setUnitName("");
 
		String[] sources = { projectSourcePath }; 
		String[] classpath = {jrePath};
 
		parser.setEnvironment(classpath, sources, new String[] { "UTF-8"}, true);
		parser.setSource(classSource);
		
		return (CompilationUnit) parser.createAST(null); // create and parse
	}

	// navigate method information
	public static int countNbMethods(CompilationUnit parse) {
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
		parse.accept(visitor);
		
		return visitor.getMethods().size();

	}
	
	public static int countNbClasses(CompilationUnit parse) {
		TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
		parse.accept(visitor);
		

		return visitor.getTypes().size();
	}
	
	public static void countNbPackages(CompilationUnit parse, PackageDeclarationVisitor visitor) {
		parse.accept(visitor);
	}
	

	public static int countMethodsLines(CompilationUnit parse) {
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
		parse.accept(visitor);

		int totalLinesOfCodeInMethod = 0;
		
		for(MethodDeclaration m : visitor.getMethods()) {
			if(m.getBody() != null)
				totalLinesOfCodeInMethod += m.getBody().toString().split("\n").length - 2;
		}
			
		return totalLinesOfCodeInMethod;


	}
	
	public static int countNbAttributs(CompilationUnit parse) {
		FieldDeclarationVisitor visitor = new FieldDeclarationVisitor();
		parse.accept(visitor);
		
		return visitor.getFields().size();
	}
	
	public static void top10PercentClassesByMethodCount(CompilationUnit parse, Map<String, Integer> classMethodCountMap) {
		
		TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
		
		parse.accept(visitor);
		
		List<TypeDeclaration> types = visitor.getTypes();

        for (TypeDeclaration type : types) {
            String className = type.getName().getFullyQualifiedName();
            
            MethodDeclaration[] methodDeclarations = type.getMethods();
            
            int methodCount = 0;
            
            for (MethodDeclaration method : methodDeclarations) {
                methodCount++;
            }
            
            classMethodCountMap.put(className, methodCount);
        }
	}
		
	public static void top10PercentClassesByAttributCount(CompilationUnit parse, Map<String, Integer> classAttributCountMap) {
		
		TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
		
		parse.accept(visitor);
		
		List<TypeDeclaration> types = visitor.getTypes();

        for (TypeDeclaration type : types) {
            String className = type.getName().getFullyQualifiedName();
            
            FieldDeclaration[] fieldDeclarations = type.getFields();
            
            int fieldCount = 0;
            
            for (FieldDeclaration field : fieldDeclarations) {
            	fieldCount++;
            }
            
            classAttributCountMap.put(className, fieldCount);
        }
		
	}
	
	private static HashMap sortDescending(Map map) {
	       List linkedlist = new LinkedList(map.entrySet());

	       Collections.sort(linkedlist, new Comparator() {
	            public int compare(Object o1, Object o2) {
	               return ((Comparable) ((Map.Entry) (o2)).getValue())
	                  .compareTo(((Map.Entry) (o1)).getValue());
	            }
	       });

	       HashMap sortedHashMap = new LinkedHashMap();
	       for (Iterator it = linkedlist.iterator(); it.hasNext();) {
	              Map.Entry entry = (Map.Entry) it.next();
	              sortedHashMap.put(entry.getKey(), entry.getValue());
	       } 
	       return sortedHashMap;
	 }
	
	public static String getLink(InputStream inputStream) throws IOException
	{
		if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)))
            {
                StringBuilder contenu = new StringBuilder();
                String ligne;
                while ((ligne = reader.readLine()) != null)
                {
                    contenu.append(ligne);
                }
                return(contenu.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Le fichier n'a pas été trouvé.");
        }
		return null;
    }


}
