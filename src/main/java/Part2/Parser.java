package part2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import graph.Link;
import graph.Node;

import guru.nidi.graphviz.model.MutableGraph;

import org.eclipse.jdt.core.dom.TypeDeclaration;

public class Parser {

	private static String projectSourcePath;
	private static String jrePath = "/System/Library/Frameworks/JavaVM.framework/";

	public static void main(String[] args) throws IOException {
				
		Properties properties = new Properties();
		String projectPath = null;
		
		try (InputStream fis = new FileInputStream("src/main/resources/projectPath.properties"))
		{
			properties.load(fis);
            projectPath = properties.getProperty("path");

        } catch (IOException io) {
            io.printStackTrace();
        }
        
		projectSourcePath = projectPath + "/src";
		
		// read java files
		final File folder = new File(projectSourcePath);
		ArrayList<File> javaFiles = listJavaFilesForFolder(folder);
		
		String rep= "";
		
		while (!rep.equalsIgnoreCase("x"))
		{
			System.out.println("Bienvenue sur l'analyse de programme !");
			System.out.println("1 -> Exercice 1");
			System.out.println("2 -> Exercice 2");
			System.out.println("x -> Exit");
			Scanner scan = new Scanner(System.in);
			rep = scan.next();
			
			if (rep.equalsIgnoreCase("1"))
			{
				rep= "";
				System.out.println("Exercice 1 :");
				System.out.println("1. Nombres de classes de l'application");
				System.out.println("2. Nombre de lignes de code de l’application");
				System.out.println("3. Nombre total de méthodes de l’application");
				System.out.println("4. Nombre total de packages de l’application");
				System.out.println("5. Nombre moyen de methodes par classe");
				System.out.println("6. Nombre moyen de lignes de code par méthode");
				System.out.println("7. Nombre moyen d’attributs par classe");
				System.out.println("8. Les 10% des classes qui possèdent le plus grand nombre de méthodes");
				System.out.println("9. Les 10% des classes qui possèdent le plus grand nombre d’attributs");
				System.out.println("10. Les classes qui font partie en même temps des deux catégories précédentes");
				System.out.println("11. Les classes qui possèdent plus de X méthodes");
				System.out.println("12. Les 10% des méthodes qui possèdent le plus grand nombre de lignes de code");
				System.out.println("13. Le nombre maximal de paramètres par rapport à toutes les méthodes l’application");
				System.out.print("Rentrer le numero de la question : ");
				rep = scan.next();
				System.out.println("Traitement en cours ...");
				printExo1(Integer.parseInt(rep), javaFiles);
				
			} else if (rep.equalsIgnoreCase("2"))
			{
				rep= "";
				System.out.println("Exercice 2 :");
				System.out.println("Traitement en cours ...");
		        printExo2(javaFiles);
			}
			System.out.println("-----------------------------------");
		}
		
		System.out.println("Au revoir !");
	}
	
	private static void printExo2(ArrayList<File> javaFiles) throws IOException
	{
		TypeDeclarationVisitor classVisitor = new TypeDeclarationVisitor();
		
		for (File fileEntry : javaFiles) {
			String content = FileUtils.readFileToString(fileEntry);
			CompilationUnit parse = parse(content.toCharArray());
			  
			visitAllClasses(parse, classVisitor);
		}
        
        printCallGraph(classVisitor);
        System.out.println("Voici le graphe d'appel du programme analysé, le fichier .dot est automatiquement créé dans le dossier /résultats.");
	}

	private static void printExo1(Integer rep, ArrayList<File> javaFiles) throws IOException
	{
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
		
		Map<String, Integer> classAttributCountMapSorted = sortDescending(classAttributCountMap);
		Map<String, Integer> top10ClassAttribut = getTopPercentage(classAttributCountMapSorted, 0.1);
		
		Map<String, Integer> classMethodCountMapSorted = sortDescending(classMethodCountMap);
		Map<String, Integer> top10ClassMethod = getTopPercentage(classMethodCountMapSorted, 0.1);
		
		Map<String, Integer> MethodLineCountMapSorted = sortDescending(MethodLineCountMap);        
		Map<String, Integer> top10MethodsByLineCount = getTopPercentage(MethodLineCountMapSorted, 0.1);
		
		//System.out.println("-----------------------------------");
		
		switch (rep)
		{
			case 1:
				System.out.println("1. Nombre de classes : " + numberClasses);
				break;
			case 2:
				System.out.println("2. Nombre de lignes de code de l’application : " + nbLines);
				break;
			case 3:
				System.out.println("3. Nombre total de méthodes de l’application : " + numberMethods);
				break;
			case 4:
				System.out.println("4. Nombre total de packages de l’application : " + numberPackages);
				break;
			case 5:
				System.out.println("5. Nombre moyen de methodes par classe : " + averageMethodsByClass);
				break;
			case 6:
				System.out.println("6. Nombre moyen de lignes de code par méthode : " + averageLinesByMethod);
				break;
			case 7:
				System.out.println("7. Nombre moyen d’attributs par classe : " + averageAttributsByClass);
				break;
			case 8:
				System.out.println("8. Les 10% des classes qui possèdent le plus grand nombre de méthodes : " );
				
		        for (Map.Entry<String, Integer> entry : top10ClassMethod.entrySet()) {
		        	System.out.println("- " + entry.getKey());
		        }
		        break;
			case 9:
				System.out.println("9. Les 10% des classes qui possèdent le plus grand nombre d’attributs : " );
				
		        for (Map.Entry<String, Integer> entry : top10ClassAttribut.entrySet()) {
		        	System.out.println("- " + entry.getKey() );
		        }
		        break;
			case 10:
				System.out.println("10. Les classes qui font partie en même temps des deux catégories précédentes" );
		        for (Map.Entry<String, Integer> entryMethod : top10ClassMethod.entrySet()) {
		            String className = entryMethod.getKey();
		            if (top10ClassAttribut.containsKey(className)) {
		                System.out.println("- " + className);
		            }
		        }
		        break;
			case 11:
				Scanner scan = new Scanner(System.in);
				System.out.print("11. Rentrer un nombre de méthode par classes : ");
				int X = Integer.parseInt(scan.next());
		        System.out.println("11. Les classes qui possèdent plus de " + X + " méthodes :");
		        for (Map.Entry<String, Integer> entry : classMethodCountMapSorted.entrySet())
		        {
		            String className = entry.getKey();
		            int methodCount = entry.getValue();
		            if (methodCount > X)
		            {
		                System.out.println("- " + className);
		            }
		        }
		        break;
			case 12:
				System.out.println("12. Les 10% des méthodes qui possèdent le plus grand nombre de lignes de code");
				MethodsByLineCount(MethodLineCountMap, methodVisitor);
				
		        for (Map.Entry<String, Integer> entry : top10MethodsByLineCount.entrySet())
		        {
		            String methodName = entry.getKey();
		            int methodCount = entry.getValue();
		            System.out.println("- " + methodName + " : " + methodCount);
		        }
		        break;
			case 13:
				System.out.println("13. Le nombre maximal de paramètres par rapport à toutes les méthodes l’application.");
		        showMethodWithMaximalParameters(methodVisitor);
		        break;
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
	
	public static void printCallGraph(TypeDeclarationVisitor classVisitor) throws IOException
	{
		List<Node> nodes = new ArrayList<Node>();
		List<Link> links = new ArrayList<Link>();
		
		for(TypeDeclaration type : classVisitor.getTypes())
		{
			for(MethodDeclaration method : type.getMethods())
			{
				MethodInvocationVisitor methodInvVisitor = new MethodInvocationVisitor();
				method.accept(methodInvVisitor);
				
				Node nodeMethodDecla = new Node(type.getName().toString() + "." + method.getName().toString());
				nodes.add(nodeMethodDecla);
				
				
				if (methodInvVisitor.getMethods().size() != 0)
				{

					for (MethodInvocation methodInvocation : methodInvVisitor.getMethods())
					{
						
						if (methodInvocation.getExpression() != null)
						{
							
							if (methodInvocation.getExpression().resolveTypeBinding() != null)
							{
								
								Node nodeMethodInv = new Node(methodInvocation.getExpression().resolveTypeBinding().getName() + "." + methodInvocation.getName().toString());
								nodes.add(nodeMethodInv);
								links.add(new Link(nodeMethodDecla.getNode(), nodeMethodInv.getNode()));
							}
						}
						else {
							
							Node nodeMethodInv = new Node(methodInvocation.getName().toString());
							nodes.add(nodeMethodInv);
							links.add(new Link(nodeMethodDecla.getNode(), nodeMethodInv.getNode()));
						}
					}
				}
			}
		}
		
		
		// Add vertex and edges to the graph
		Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
		for (Node node : nodes) {
			graph.addVertex(node.getNode());
		}
		for (Link link : links) {
			graph.addEdge(link.getNodeA(), link.getNodeB());
		}
		
		System.out.println(graph.toString());

		// Export the graph in a dot file
		DOTExporter<String, DefaultEdge> exporter = new DOTExporter<String, DefaultEdge>();
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<String, Attribute>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });
        Writer buffer = new StringWriter();
        exporter.exportGraph(graph, buffer);
        exporter.exportGraph(graph, new File("results/graph.dot"));
        
        MutableGraph mGraph = new guru.nidi.graphviz.parse.Parser().read(buffer.toString());
        System.out.println(buffer.toString());

        //Graphviz.fromGraph(graph).height(1000).render(Format.PNG).toFile(new File("results/graph.png"));
	}
}
