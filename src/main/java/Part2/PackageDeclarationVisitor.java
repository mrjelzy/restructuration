package Part2;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.PackageDeclaration;

public class PackageDeclarationVisitor extends ASTVisitor {
	HashSet<String> packages = new HashSet<String>();
	
	public boolean visit(PackageDeclaration node) {
        String packageName = node.toString();
        if(!packages.contains(packageName))
        	packages.add(packageName);
		return super.visit(node);
	}
	
	public HashSet<String> getPackages() {
		return packages;
	}
}
