package graph;

public class Link {
	
	String nodeA;
	String nodeB;
	
	public Link (String nodeA, String nodeB) {
		this.nodeA = nodeA;
		this.nodeB = nodeB;
	}

	public String getNodeA() {
		return nodeA;
	}

	public String getNodeB() {
		return nodeB;
	}

}