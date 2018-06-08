package edu.rit.CapstonepProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.cypher.internal.compiler.v2_3.commands.PathPattern;
import org.neo4j.cypher.internal.compiler.v3_0.commands.Pattern;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

/**
 * This is the class for SkinnyMine Subgraph Mining Algorithm
 * 
 * @author pragatiunde1990
 *
 */
public class SkinnyMineProteins {
	private static String subgraphPathFile = "C:/Users/pragatiunde1990/Documents/Neo4j/SubgraphProteins";
	private static File dataFile = new File(subgraphPathFile);
	private static String queryPathFile = "C:/Users/pragatiunde1990/Documents/Neo4j/QueryProteins";
	private static File queryFile = new File(queryPathFile);
	private static Set<String> finalResult = new HashSet<String>();
	private static GraphDatabaseService dbTargetData;
	private static GraphDatabaseService dbTargetQuery;
	private static Set<Node> visitedNodes = new HashSet<Node>();
	private static Set<String> frequentSet = new HashSet<String>();
	private static Set<String> E = new LinkedHashSet<String>();
	private static Set<String> patterns = new HashSet<String>();
	private static List<Node> levelNodes = new ArrayList<Node>();
	private static List<String> edgesForQueryGraph = new LinkedList<String>();
	private static Map<String, Integer> s0 = new HashMap<String, Integer>();
	private static Set<String> edges= new HashSet<String>();

	
	/**
	 * This method is used to create input grpah
	 * @param file input file of vertices and edges.
	 */
	private static void processFile(File file) {
		//System.out.println(file);
		try {
			Map<String, Long> graphID = new HashMap<String, Long>();
			FileUtils.deleteRecursively(dataFile);
			BatchInserter inserter = BatchInserters.inserter(dataFile);
			BufferedReader br = new BufferedReader(new FileReader(file));
		    edges= new HashSet<String>();
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] nodes=line.split(" ");
				if(nodes.length>1){
					//char[] chArray=nodes[1].toCharArray();
					if(Character.isLetter(nodes[1].charAt(0))){
						Label label = Label.label(nodes[1]);
						Map<String, Object> node = new HashMap<String, Object>();
						node.put("id", nodes[0]);
						Long id = inserter.createNode(node, label);
						graphID.put(nodes[0], id);
					}
					else{
						Long nodeID1 = graphID.get(nodes[0]);
						Long nodeID2 = graphID.get(nodes[1]);
						createEdges(nodeID1,nodeID2,inserter);
						
					}
				}
			}
			inserter.shutdown();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/**
	 * This method is used to create the edges based on the nodes
	 * @param nodeID1 node Id 1
	 * @param nodeID2 node Id 2
	 * @param inserter 
	 */
	public static void createEdges(Long nodeID1, Long nodeID2, BatchInserter inserter){
		StringBuilder edgeBuilder = new StringBuilder();
		edgeBuilder.append(nodeID1);
		edgeBuilder.append(nodeID2);
		StringBuilder reverseBuilder = new StringBuilder();
		reverseBuilder.append(nodeID2);
		reverseBuilder.append(nodeID1);
		String edge = edgeBuilder.toString();
		String reverseEdge = reverseBuilder.toString();
		//System.out.println(edge + " " + reverseEdge);
		if (!edges.contains(edge)) {
			if (!edges.contains(reverseEdge)) {
				edges.add(edge);
				inserter.createRelationship(nodeID1, nodeID2, RelationshipType.withName("edgeFrom"),
						null);
			} else {
				inserter.createRelationship(nodeID1, nodeID2, RelationshipType.withName("edgeTo"),
						null);
				//System.out.println("In to");
			}
		}
	}
	
	/**
	 * This is the main method.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
	
		String path="F:/SEM IV/Graph Databases/Assignment 4/Proteins/Proteins/part3_Proteins/Proteins/query/backbones_1NTN.128.sub.grf";
		File folder= new File(path);
		processFile(folder);
		System.out.println("Target Done");
		dbTargetData = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dataFile)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "512M")
				.setConfig(GraphDatabaseSettings.string_block_size, "60")
				.setConfig(GraphDatabaseSettings.array_block_size, "300").newGraphDatabase();

		Transaction dataTransaction = dbTargetData.beginTx();
		ResourceIterable<Node> nodes = dbTargetData.getAllNodes();
		for (Node node : nodes) {
			Iterable<Label> labels = node.getLabels();
			String start = "";
			for (Label lb : labels) {
				start = lb.toString();
			}
			Iterable<Relationship> relations = node.getRelationships();
			for (Relationship rel : relations) {
				String edge = "";
				Node end = rel.getOtherNode(node);
				Iterable<Label> endLabels = end.getLabels();
				String endNode = "";
				for (Label lb : endLabels) {
					endNode = lb.toString();
					edge = start + endNode;
				}

				if (s0.containsKey(edge)) {
					s0.put(edge, s0.get(edge) + 1);
				} else {
					s0.put(edge, 1);
				}
			}

		}

		System.out.println("Set S0:" + s0.keySet());
		Set<String> initialSet = new HashSet<String>();
		initialSet.addAll(s0.keySet());
		String[] setArray = initialSet.toArray(new String[initialSet.size()]);
		int lengthOfDiameter = 6 ; //need to be changed according to input
		int suportThreshold = 1; //need to be changed according to input
		int skinnyPattern = 2; //need to be changed according to input
		Long beginTime = System.currentTimeMillis();
		Set<String> diaMine = findDiaMine(dataTransaction, setArray, lengthOfDiameter, suportThreshold);
		System.out.println("Set of canonical diameters: " + diaMine);
		Long endTime = System.currentTimeMillis();
		Long totalTime = endTime - beginTime;
		System.out.println("Performance of Stage I: " +(totalTime/1000)  + " sec");
		Set<String> Si = new HashSet<String>();
		Long beginTimeLevelGrow = System.currentTimeMillis();
		for (int i = 1; i <= skinnyPattern; i++) {

			if (i == 1) {
				patterns = diaMine;
			} else {
				patterns = Si;
			}
			Si = LevelGrow(dataTransaction, "NCCNCCN", suportThreshold,i); // //need to be changed according to input
			if (!Si.isEmpty()) {
					finalResult.addAll(Si);
				} else {
					break;
				}
			 System.out.println("Set of patterns: " + finalResult);
			
		}

		Long endTimeLevelGrow = System.currentTimeMillis();
		Long totalTimeLevelGrow = endTimeLevelGrow - beginTimeLevelGrow;
		System.out.println("Performance of Stage II: " +(totalTimeLevelGrow / 1000) + " sec");
		System.out.println("Total time:"+ ((totalTime/1000)+(totalTimeLevelGrow/1000)) + " sec");
		dataTransaction.success();
		dataTransaction.close();
		dbTargetData.shutdown();

	}
	/**
	 * This is implementation of level grow subroutine
	 * @param dataTransaction
	 * @param diameter canonical diameter 
	 * @param suportThreshold 
	 * @param skinny skinny patterns
	 * @return  set of all valid edges 
	 * @throws IOException
	 */
	private static Set<String> LevelGrow(Transaction dataTransaction, String diameter, int suportThreshold, int skinny)
			throws IOException {
		String query = SkinnyMineProteins.createCypherQueryDia(diameter, "edgeFrom", 18);
		Map<Node, LinkedList<Node>> extendedEdges = new HashMap<Node, LinkedList<Node>>();
		List<Long> diameterIds = new LinkedList<Long>();
		List<Node> diameterNodes = new LinkedList<Node>();
		org.neo4j.graphdb.Result result = dbTargetData.execute(query);
		if(! ((org.neo4j.graphdb.Result) result).hasNext()){
			return new HashSet<String>();
		}
		while (((org.neo4j.graphdb.Result) result).hasNext()) {
			Map<String, Object> row = ((org.neo4j.graphdb.Result) result).next();
			//System.out.println("Nodes in diameter: " + row.get("nodes(p)"));
			diameterNodes = (List<Node>) row.get("nodes(p)");
			Iterable<Node> nodeInDiameter = (Iterable<Node>) row.get("nodes(p)");
			for (Node node : nodeInDiameter) {
				Iterable<Relationship> relations = node.getRelationships(RelationshipType.withName("edgeFrom"));
				LinkedList<Node> otherRelNodes = new LinkedList<Node>();
				for (Relationship relOther : relations) {
					otherRelNodes.add(relOther.getEndNode());
				}
				//extendedEdges.put(node, otherRelNodes);
				Iterable<Relationship> relations2 = node.getRelationships(RelationshipType.withName("edgeTo"));
				//LinkedList<Node> otherRelNodes = new LinkedList<Node>();
				for (Relationship relOther : relations2) {
					otherRelNodes.add(relOther.getEndNode());
				}
				extendedEdges.put(node, otherRelNodes);
			}
		}
		for (Node node : diameterNodes) {
			diameterIds.add(node.getId());
		}
	
		
		if ( skinny != 1 ) {
			List<String> edges = edgesForQueryGraph;
			createQueryGraph(diameterIds, edges);
			E = new LinkedHashSet<String>();
			extendedEdges = new HashMap<Node, LinkedList<Node>>();

			for (int i = 0; i < levelNodes.size(); i++) {
				Iterable<Relationship> relations = levelNodes.get(i).getRelationships(RelationshipType.withName("edgeFrom"));
				LinkedList<Node> otherRelNodes = new LinkedList<Node>();
				for (Relationship relOther : relations) {
					if(!otherRelNodes.contains(relOther.getEndNode()) && !(levelNodes.get(i).equals(relOther.getEndNode())))
						otherRelNodes.add(relOther.getEndNode());
				}
				Iterable<Relationship> relations2 = levelNodes.get(i).getRelationships(RelationshipType.withName("edgeTo"));
				for (Relationship relOther : relations2) {
					if(!otherRelNodes.contains(relOther.getEndNode()) && !(levelNodes.get(i).equals(relOther.getEndNode())))
						otherRelNodes.add(relOther.getEndNode());
				}
				if( !otherRelNodes.isEmpty())
					extendedEdges.put(levelNodes.get(i), otherRelNodes);
			}
			getLevelEdges(extendedEdges, diameterNodes);

		} else {
			getLevelEdges(extendedEdges, diameterNodes);

		}
		System.out.println("Level " +skinny+" nodes: "+ levelNodes);
		visitedNodes.addAll(levelNodes);
		System.out.println("Edges for level " +skinny+" grow: " + E);
		Set<LinkedList<Long>> Tpre = new HashSet<LinkedList<Long>>();
		Long Dh = diameterIds.get(0);
		Long Dt = diameterIds.get(diameterIds.size() - 1);
		Tpre.add((LinkedList<Long>) diameterIds);

		while (!Tpre.isEmpty()) {
			Tpre.remove(diameterIds);
			Iterator<String> iterator = E.iterator();
			while (iterator.hasNext()) {
				String Panchor = iterator.next();
				edgesForQueryGraph.add(Panchor);
				createQueryGraph(diameterIds, edgesForQueryGraph);
				dbTargetQuery = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(queryFile)
						.setConfig(GraphDatabaseSettings.pagecache_memory, "512M")
						.setConfig(GraphDatabaseSettings.string_block_size, "60")
						.setConfig(GraphDatabaseSettings.array_block_size, "300").newGraphDatabase();
				Transaction queryTransaction = dbTargetQuery.beginTx();
				String[] newEdges = Panchor.split(" ");
				Integer uDistanceH = Integer.parseInt(calculateDistance(Long.parseLong(newEdges[1]), Dh).toString());
				Integer uDistanceT = Integer.parseInt(calculateDistance(Long.parseLong(newEdges[1]), Dt).toString());
				Integer vDistanceH = Integer.parseInt(calculateDistance(Long.parseLong(newEdges[0]), Dh).toString());
				Integer vDistanceT = Integer.parseInt(calculateDistance(Long.parseLong(newEdges[0]), Dt).toString());
				boolean isValid = checkConstraint(uDistanceH, uDistanceT, vDistanceH, vDistanceT, diameter.length() - 1,
						newEdges, diameterIds, diameter, Dh, Dt);
				if (!isValid) {
					edgesForQueryGraph.remove(Panchor);
					checkvalidNode( Panchor, diameterNodes);
				}

				queryTransaction.success();
				queryTransaction.close();
				dbTargetQuery.shutdown();
			}
		}
		Set<String> Si = new HashSet<String>(edgesForQueryGraph);
		return Si;
	}
	
	/**
	 * This method is used to check whether node created by new edge is valid or not.
	 * @param panchor edge to check for validation of node
	 * @param diameterNodes all nodes present in the canonical diameter.
	 */
	private static void checkvalidNode( String panchor, List<Node> diameterNodes) {
		String[] newNodes= panchor.split(" ");
		int numberOfEdgesFrom= 1;
		int numberOfEdgesTo=1;
		int index= Integer.MIN_VALUE;
		boolean removed = false;
		Node parent = dbTargetData.getNodeById(Long.parseLong(newNodes[0]));
		Node child = dbTargetData.getNodeById(Long.parseLong( newNodes[1]));
		for (int i = 0; i < levelNodes.size(); i++) {
			if(!diameterNodes.contains(parent) && levelNodes.get(i).equals( parent) ){
				Iterable<Relationship> relations = levelNodes.get(i).getRelationships(RelationshipType.withName("edgeFrom"));
				for (Relationship relOther : relations) {
					if( !child.equals(relOther.getEndNode()))
						numberOfEdgesFrom++;
				}
			} else if( levelNodes.get(i).equals( child ) ){
				Iterable<Relationship> relations = levelNodes.get(i).getRelationships(RelationshipType.withName("edgeTo"));
				for (Relationship relOther : relations) {
					if( !child.equals(relOther.getEndNode()))
						numberOfEdgesTo++;
				}
				index= i;
			}
		}
		if( diameterNodes.contains(parent) ){
			if( numberOfEdgesTo == 2){
				 levelNodes.remove(index);
				 removed= true;
			}
		} else{
			if( numberOfEdgesFrom == 1 && numberOfEdgesTo == 2 && !removed)
				levelNodes.remove(index);
		}
		
		
	}

	/**
	 * This method is used to get all level edges which are missed into the extended edges set.
	 * @param extendedEdges  all level edges
	 * @param diameterNodes nodes present in the canonical diameter
	 */
	private static void getLevelEdges(Map<Node, LinkedList<Node>> extendedEdges, List<Node> diameterNodes) {

		levelNodes = new ArrayList<Node>();
		for (Map.Entry<Node, LinkedList<Node>> entry : extendedEdges.entrySet()) {
			for (Node node : entry.getValue()) {
				if ( !diameterNodes.contains(node) && !visitedNodes.contains(node) && !(String.valueOf(entry.getKey().getId()).equals(String.valueOf(node.getId())))  ) {
					E.add(String.valueOf(entry.getKey().getId()) + " " + String.valueOf(node.getId()));
					if( !levelNodes.contains(node))
						levelNodes.add(node);
				}
			}
		}
		

		for (int i = 0; i < levelNodes.size(); i++) {
			Iterable<Relationship> relations = levelNodes.get(i).getRelationships(RelationshipType.withName("edgeFrom"));
			for (Relationship relOther : relations) {
				if (levelNodes.contains(relOther.getEndNode()) && (!levelNodes.get(i).equals(relOther.getEndNode()))) {
					E.add(String.valueOf(levelNodes.get(i).getId()) + " "
							+ String.valueOf(relOther.getEndNode().getId()));
				}
			}
			Iterable<Relationship> relations2 = levelNodes.get(i).getRelationships(RelationshipType.withName("edgeTo"));
			for (Relationship relOther : relations2) {
				if (levelNodes.contains(relOther.getEndNode()) && (!levelNodes.get(i).equals(relOther.getEndNode()))) {
					E.add(String.valueOf(levelNodes.get(i).getId()) + " "
							+ String.valueOf(relOther.getEndNode().getId()));
				}
			}
			
		}
	}
	/**
	 * This is implementation of findDiamine subroutine.
	 * @param dataTransaction
	 * @param setArray Set S0 of nodes.
	 * @param lengthOfDiameter canonical diameter length
	 * @param suportThreshold
	 * @return
	 * @throws IOException
	 */
	private static Set<String> findDiaMine(Transaction dataTransaction, String[] setArray, int lengthOfDiameter,
			int suportThreshold ) throws IOException {
		int i = 0;
		Set<String> finalSet = new HashSet<String>();
		while ((lengthOfDiameter > Math.pow(2, i + 1))) {
			i = i + 1;
			if (i >= 2)
				setArray = frequentSet.toArray(new String[frequentSet.size()]);
			frequentSet = new HashSet<String>();
			for (int j = 0; j < setArray.length; j++) {
				for (int k = 0; k < setArray.length; k++) {
					boolean concat = checkConcat(setArray[j], setArray[k]);
					String newEdge = "";
					if (concat) {
						newEdge = setArray[j] + setArray[k].substring(1);
						frequentSet.add(newEdge);
					}
				}
			}

			findNextSet(dataTransaction);
			if (frequentSet.isEmpty()) {
				i = i - 1;
				frequentSet = finalSet;
				break;
			}
			finalSet = frequentSet;

			System.out.println("Set S" + i + ":" + frequentSet);
		}
		setArray = frequentSet.toArray(new String[frequentSet.size()]);
		frequentSet = new HashSet<String>();
		if (Math.pow(2, i) < lengthOfDiameter) {
			for (int j = 0; j < setArray.length; j++) {
				for (int k = 0; k < setArray.length; k++) {
					int chkChar = ((setArray[j].length() - 1) * 2) - lengthOfDiameter;
					String diameter = "";
					if (checkMergeHead(setArray[j], setArray[k], chkChar)) {
						diameter = setArray[j] + setArray[k].substring(chkChar + 1);
						if (diameter.length() - 1 == lengthOfDiameter) {
							findFrequentEdge(diameter, "edgeFrom", 18);
							findFrequentEdge(diameter, "edgeTo", 16);
						}
					}
				}
			}
		} else {
			return finalSet;
		}

		return frequentSet;
	}

	private static boolean checkMergeHead(String first, String second, int chkChar) {

		if (first.substring(first.length() - (chkChar + 1)).equalsIgnoreCase(second.substring(0, chkChar + 1))) {
			return true;
		}
		return false;
	}

	private static void findNextSet(Transaction dataTransaction) {

		try {

			String[] setArray = frequentSet.toArray(new String[frequentSet.size()]);
			frequentSet = new HashSet<String>();
			for (int arr = 0; arr < setArray.length; arr++) {

				findFrequentEdge(setArray[arr], "edgeFrom", 18);
				findFrequentEdge(setArray[arr], "edgeTo", 16);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void findFrequentEdge(String path, String direction, int deleteCharLength) {

		String query = createCypherQuery(path, direction, deleteCharLength);
		org.neo4j.graphdb.Result result = dbTargetData.execute(query);
		boolean hasResult = ((org.neo4j.graphdb.Result) result).hasNext();
		if (hasResult) {
			frequentSet.add(path);
		}

	}

	static String createCypherQueryDia(String path, String direction, int deleteCharLength) {
		StringBuilder sb = new StringBuilder("MATCH p= ");
		for (int j = 0; j < path.length(); j++) {
			sb.append("(node" + (j + 1) + ":" + path.charAt(j) + ")-[rel" + (j + 1) + ":" + direction + "]->");
		}
		sb.delete(sb.length() - deleteCharLength, sb.length());
		if( sb.substring(sb.length()-1).equals("-")){
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append(" return nodes(p)");
		return sb.toString();
	}

	private static String createCypherQuery(String path, String direction, int deleteCharLength) {
		StringBuilder sb = new StringBuilder("MATCH p= ");
		for (int j = 0; j < path.length(); j++) {
			sb.append("(node" + (j + 1) + ":" + path.charAt(j) + ")-[rel" + (j + 1) + ":" + direction + "]->");
		}
		sb.delete(sb.length() - deleteCharLength, sb.length());
		if( sb.substring(sb.length()-1).equals("-")){
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append(" return p");
		return sb.toString();
	}

	private static boolean checkConcat(String ldash, String ldoubledash) {
		if (ldash.substring(ldash.length() - 1).equalsIgnoreCase(ldoubledash.substring(0, 1))) {
			return true;
		}
		return false;
	}

	private static void createQueryGraph(List<Long> diameterIds, List<String> edgesForQueryGraph) throws IOException {

		FileUtils.deleteRecursively(queryFile);
		BatchInserter inserter = BatchInserters.inserter(queryFile);
		Map<String, Long> graphID = new HashMap<String, Long>();
		edges = new HashSet<String>();
		
		for (int i = 0; i < diameterIds.size(); i++) {
			String labelName = dbTargetData.getNodeById(diameterIds.get(i)).getLabels().iterator().next().name();
			Label label = Label.label(labelName);
			Map<String, Object> node = new HashMap<String, Object>();
			node.put("id", diameterIds.get(i) + 1);
			Long id = inserter.createNode(node, label);
			graphID.put(diameterIds.get(i).toString(), id);
		}
		Set<Long> mapValues = new HashSet<Long>();
		for (Map.Entry<String, Long> entry : graphID.entrySet()) {
			mapValues.add(entry.getValue());
		}
		Iterator<Long> iterator = mapValues.iterator();
		Long current = iterator.next();
		Long next = null;

		while (iterator.hasNext()) {
			next = iterator.next();
			if (current != null) {
				createEdges(current,next,inserter);
			}
			current = next;
		}
		for (int i = 0; i < edgesForQueryGraph.size(); i++) {
			String[] newEdgeNodes = edgesForQueryGraph.get(i).split(" ");
			if (!graphID.containsKey(newEdgeNodes[1])) {
				Long nodeId = Long.parseLong(newEdgeNodes[1]);
				String labelName = dbTargetData.getNodeById(nodeId).getLabels().iterator().next().name();
				Label label = Label.label(labelName);
				Map<String, Object> node = new HashMap<String, Object>();
				node.put("id", nodeId + 1);
				Long id = inserter.createNode(node, label);
				graphID.put(nodeId.toString(), id);

			}
			if (!graphID.containsKey(newEdgeNodes[0])) {
				Long nodeId = Long.parseLong(newEdgeNodes[0]);
				String labelName = dbTargetData.getNodeById(nodeId).getLabels().iterator().next().name();
				Label label = Label.label(labelName);
				Map<String, Object> node = new HashMap<String, Object>();
				node.put("id", nodeId + 1);
				Long id = inserter.createNode(node, label);
				graphID.put(nodeId.toString(), id);

			}
			Long nodeID1 = graphID.get(newEdgeNodes[0]);
			Long nodeID2 = graphID.get(newEdgeNodes[1]);
			createEdges(nodeID1,nodeID2,inserter);
		}

		inserter.shutdown();

	}

	private static boolean checkConstraint(Integer uDistanceH, Integer uDistanceT, Integer vDistanceH,
			Integer vDistanceT, int diameterLen, String[] newEdges, List<Long> diameterIds, String diameter, Long dh,
			Long dt) {

		boolean constraint1 = false;
		boolean constraint2 = false;
		boolean constraint3 = false;
		if (uDistanceH <= diameterLen && uDistanceT <= diameterLen){
			constraint1 = true;
		}else{
			System.out.print("Constraint 1 violated ");
		}
		if ((uDistanceH + uDistanceT) >= diameterLen){
			constraint2 = true;
		} else{
			System.out.print("Constraint 2 violated ");
		}
		if (diameterIds.contains(Long.parseLong(newEdges[0])) && (!diameterIds.contains(Long.parseLong(newEdges[1])))
				&& Math.max(vDistanceH, vDistanceT) == diameterLen - 1) {
			constraint3 = checkLexicograph(uDistanceH, uDistanceT, diameter, newEdges, dh, dt);
		} else if (diameterIds.contains(Long.parseLong(newEdges[0]))
				&& diameterIds.contains(Long.parseLong(newEdges[1]))
				&& (((uDistanceH + vDistanceT) == diameterLen - 1) || ((vDistanceH + uDistanceT) == diameterLen - 1))) {
			constraint3 = checkLexicograph(uDistanceH, uDistanceT, diameter, newEdges, dh, dt);
		} else {
			constraint3 = true;
		}
        if(!constraint3)
        	System.out.print("Constraint 3 violated ");
		return (constraint1 && constraint2 && constraint3);
	}

	private static boolean checkLexicograph(Integer uDistanceH, Integer uDistanceT, String diameter, String[] newEdges,
			Long dh, Long dt) {
		if (uDistanceH.equals(diameter.length() - 1)) {
			StringBuilder newDiameter = getAllLables(Long.parseLong(newEdges[1]), dh).reverse();
			String sb = newDiameter.toString();
			if (sb.length() == diameter.length()) {
				for (int i = 0; i < diameter.length(); i++) {
					if (!(diameter.charAt(i) == sb.charAt(i))) {
						return diameter.charAt(i) < sb.charAt(i) || diameter.charAt(i) == sb.charAt(i);
					}
					else if( diameter.charAt(diameter.length()-1) == sb.charAt(diameter.length()-1) ||
							diameter.charAt(0) == sb.charAt(0))
						return true;
				}
			}

		}
		if (uDistanceT.equals(diameter.length() - 1)) {
			String newDiameter = getAllLables(Long.parseLong(newEdges[1]), dt).toString();
			if (newDiameter.length() == diameter.length()) {
				for (int i = 0; i < diameter.length(); i++) {
					if (!(diameter.charAt(i) == newDiameter.charAt(i))) {
						return diameter.charAt(i) < newDiameter.charAt(i);
					}
				}
			}
		}

		return false;
	}

	private static StringBuilder getAllLables(long l, Long dh) {
		long first = l + 1;
		long second = dh + 1;
		Node firstNode = dbTargetData.getNodeById(l);
		Node secondNode = dbTargetData.getNodeById(dh);
		String fLabel = firstNode.getLabels().iterator().next().name();
		String sLabel = secondNode.getLabels().iterator().next().name();
		StringBuilder sb = new StringBuilder("MATCH (node1:" + fLabel + " { id:" + first + "})");
		sb.append(",(node2:" + sLabel + " { id:" + second + "})");
		sb.append(", path=shortestPath((node1)-[*..15]-(node2)) RETURN nodes(path) ");
		org.neo4j.graphdb.Result result = dbTargetQuery.execute(sb.toString());
		StringBuilder sb1 = new StringBuilder();
		while (((org.neo4j.graphdb.Result) result).hasNext()) {
			Map<String, Object> row = ((org.neo4j.graphdb.Result) result).next();
			Iterable<Node> nodeInDiameter = (Iterable<Node>) row.get("nodes(path)");
			for (Node node : nodeInDiameter) {
				sb1.append(node.getLabels().iterator().next().toString());
			}

		}
		return sb1;
	}

	private static Object calculateDistance(long u, Long dh) {
		long first = u + 1;
		long second = dh + 1;
		Node firstNode = dbTargetData.getNodeById(u);
		Node secondNode = dbTargetData.getNodeById(dh);
		String fLabel = firstNode.getLabels().iterator().next().name();
		String sLabel = secondNode.getLabels().iterator().next().name();
		StringBuilder sb = new StringBuilder("MATCH (node1:" + fLabel + " { id:" + first + "})");
		sb.append(",(node2:" + sLabel + " { id:" + second + "})");
		sb.append(", path=shortestPath((node1)-[*..15]-(node2)) RETURN length(path) ");
		org.neo4j.graphdb.Result result = dbTargetQuery.execute(sb.toString());
		while (((org.neo4j.graphdb.Result) result).hasNext()) {
			Map<String, Object> row = ((org.neo4j.graphdb.Result) result).next();
			return row.get("length(path)");
		}
		return 0;
	}

}
