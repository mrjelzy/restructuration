package part2;

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
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

public class Parser {

	private static String projectSourcePath;
	private static String jrePath = "/System/Library/Frameworks/JavaVM.framework/";

	public static void main(String[] args) throws IOException {
				
        // Utilisation du ClassLoader pour charger la ressource du fichier.
        ClassLoader classLoader = Main.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("projectPath.txt");
		
		String projectPath = (getLink(inputStream));
		projectSourcePath = projectPath + "/src";
		
		// read java files
		final File folder = new File(projectSourcePath);
		ArrayList<File> javaFiles = listJavaFilesForFolder(folder);
		
		PackageDeclarationVisitor packageVisitor = new PackageDeclarationVisitor();
		MethodDeclarationVisitor methodVisitor = new MethodDeclarationVisitor();
		TypeDeclarationVisitor classVisitor = new TypeDeclarationVisitor();
		FieldDeclarationVisitor fieldVisitor = new FieldDeclarationVisitor();
		
		Map<String, Integer> classMethodCountMap = new HashMap<String, Integer>();
		Map<String, Integer> classAttributCountMap = new HashMap<String, Integer>();
		Map<String, Integer> MethodLineCountMap = new HashMap<String, Integer>();
		
		int nbLines = 0;

		//
		for (File fileEntry : javaFiles) {
			String content = FileUtils.readFileToString(fileEntry);
			// System.out.println(content);

			CompilationUnit parse = parse(content.toCharArray());
			nbLines += countLineNumber(parse);
			  
			visitAllClasses(parse, classVisitor);
			visitAllPackages(parse, packageVisitor);
			visitAllMethods(parse, methodVisitor);
			visitAllFields(parse,fieldVisitor);
			
			ClassesByMethodCount(parse, classMethodCountMap);
			ClassesByAttributCount(parse, classAttributCountMap);

		}
		
		int numberLinesInMethods = countMethodsLines(methodVisitor);
		
		int numberPackages = packageVisitor.getPackages().size();
		int numberClasses = classVisitor.getTypes().size();
		int numberMethods = methodVisitor.getMethods().size();
		int numberAttributs = fieldVisitor.getFields().size();
				
		float averageMethodsByClass = numberMethods / (float)numberClasses;
		float averageLinesByMethod = numberLinesInMethods / (float)numberMethods;
		float averageAttributsByClass = numberAttributs / (float)numberClasses;
		
		System.out.println("Exercice 1 :");
		System.out.println("1. Nombre de classes : " + numberClasses);
		System.out.println("2. Nombre de lignes de code de l’application : " + nbLines);
		System.out.println("3. Nombre total de méthodes de l’application : " + numberMethods);
		System.out.println("4. Nombre total de packages de l’application : " + numberPackages);
		System.out.println("5. Nombre moyen de methodes par classe : " + averageMethodsByClass);
		System.out.println("6. Nombre moyen de lignes de code par méthode : " + averageLinesByMethod);
		System.out.println("7. Nombre moyen d’attributs par classe : " + averageAttributsByClass);
				
		System.out.println("8. Les 10% des classes qui possèdent le plus grand nombre de méthodes : " );
		Map<String, Integer> classMethodCountMapSorted = sortDescending(classMethodCountMap);
		Map<String, Integer> top10ClassMethod = getTopPercentage(classMethodCountMapSorted, 0.1);
        for (Map.Entry<String, Integer> entry : top10ClassMethod.entrySet()) {
        	System.out.println("- " + entry.getKey());
        }
        
		System.out.println("9. Les 10% des classes qui possèdent le plus grand nombre d’attributs : " );
		Map<String, Integer> classAttributCountMapSorted = sortDescending(classAttributCountMap);
		Map<String, Integer> top10ClassAttribut = getTopPercentage(classAttributCountMapSorted, 0.1);
        for (Map.Entry<String, Integer> entry : top10ClassAttribut.entrySet()) {
        	System.out.println("- " + entry.getKey() );
        }
        
        System.out.println("10. Les classes qui font partie en même temps des deux catégories précédentes" );
        for (Map.Entry<String, Integer> entryMethod : top10ClassMethod.entrySet()) {
            String className = entryMethod.getKey();
            if (top10ClassAttribut.containsKey(className)) {
                System.out.println("- " + className);
            }
        }
        
        int X = 10;
        System.out.println("11. Les classes qui possèdent plus de " + X + " méthodes :");
        for (Map.Entry<String, Integer> entry : classMethodCountMapSorted.entrySet()) {
            String className = entry.getKey();
            int methodCount = entry.getValue();

            if (methodCount > X) {
                System.out.println("- " + className);
            }
        }
        
        System.out.println("12. Les 10% des méthodes qui possèdent le plus grand nombre de lignes de code");
		MethodsByLineCount(MethodLineCountMap, methodVisitor);
		Map<String, Integer> MethodLineCountMapSorted = sortDescending(MethodLineCountMap);        
		Map<String, Integer> top10MethodsByLineCount = getTopPercentage(MethodLineCountMapSorted, 0.1);
        for (Map.Entry<String, Integer> entry : top10MethodsByLineCount.entrySet()) {
            String methodName = entry.getKey();
            int methodCount = entry.getValue();
            System.out.println("- " + methodName + " : " + methodCount);
        }

        System.out.println("13. Le nombre maximal de paramètres par rapport à toutes les méthodes l’application.");
        showMethodWithMaximalParameters(methodVisitor);
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
	
	public static int countLineNumber(CompilationUnit parse) {
		return parse.getLineNumber(parse.getLength() - 1);
	}

	
	public static void visitAllClasses(CompilationUnit parse, TypeDeclarationVisitor visitor) {
		parse.accept(visitor);
	}
	
	public static void visitAllPackages(CompilationUnit parse, PackageDeclarationVisitor visitor) {
		parse.accept(visitor);
	}
	
	public static void visitAllMethods(CompilationUnit parse, MethodDeclarationVisitor visitor) {
		parse.accept(visitor);
	}
	
	public static void visitAllFields(CompilationUnit parse, FieldDeclarationVisitor visitor) {
		parse.accept(visitor);
	}

	public static int countMethodsLines(MethodDeclarationVisitor visitor) {

		int totalLinesOfCodeInMethod = 0;
		
		for(MethodDeclaration m : visitor.getMethods()) {
			if(m.getBody() != null)
				totalLinesOfCodeInMethod += m.getBody().toString().split("\n").length - 2;
		}
			
		return totalLinesOfCodeInMethod;
	}
	
	public static void ClassesByMethodCount(CompilationUnit parse, Map<String, Integer> classMethodCountMap) {
		
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
		
	public static void ClassesByAttributCount(CompilationUnit parse, Map<String, Integer> classAttributCountMap) {
		
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
	
	public static void MethodsByLineCount(Map<String, Integer> MethodLineCountMap, MethodDeclarationVisitor visitor) {
		int totalLinesOfCodeInMethod = 0;
		
        for (MethodDeclaration method : visitor.getMethods()) {
        	if(method.getBody() != null) {
	            String methodName = method.getName().getFullyQualifiedName();
				totalLinesOfCodeInMethod = method.getBody().toString().split("\n").length - 2;
	            MethodLineCountMap.put(methodName, totalLinesOfCodeInMethod);
        	}
        }
		
	}
	
    private static Map<String, Integer> getTopPercentage(Map<String, Integer> sortedMap, double percentage) {
        int size = sortedMap.size();
        int countToKeep = (int) (size * percentage);

        Map<String, Integer> topPercentageMap = new HashMap<>();

        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            if (count < countToKeep) {
                topPercentageMap.put(entry.getKey(), entry.getValue());
                count++;
            }
        }

        return topPercentageMap;
    }
    
    private static void showMethodWithMaximalParameters(MethodDeclarationVisitor visitor){
    	int maxParameters = 0;
		MethodDeclaration bestMethod = null;
		
    	for(MethodDeclaration method : visitor.getMethods()) {
    		int nbParameters = method.parameters().size();
    		
			if (nbParameters > maxParameters) {
				maxParameters = method.parameters().size();
				bestMethod = method;
			}
    	}
    	
    	System.out.println("Le nombre maximal de paramètre(s) pour une methode dans toute l'application : " + maxParameters);
		System.out.println("La methode ayant ce nombre de paramètre(s) est : " + bestMethod.getName());
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
