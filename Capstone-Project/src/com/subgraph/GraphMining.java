package com.subgraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
/**
 * This class is used for Naive Subgraph Matching 
 * @author pragatiunde pxu3154
 *
 */
class NaiveSubGraphMining{
	
	private static Map<Long, ArrayList<Long>> searchSpace = new HashMap<Long, ArrayList<Long>>();
	private static Map<Long, Long> results = new HashMap<Long, Long>();
	private static boolean firstResult= false;
	
	static void naiveSubGraphMatching(GraphDatabaseService dbTargetData, GraphDatabaseService dbTargetQuery,Transaction dataTransaction, Transaction queryTransaction,  Set<String> frequentSet, String newEdge) {
		
		
		ResourceIterable<Node> queryTargetNodes = dbTargetQuery.getAllNodes();
		for (Node node : queryTargetNodes) {
			Iterable<Label> labels = node.getLabels();
			for (Label lb : labels) {
				ArrayList<Long> matchLabels = findMatchLabels(dbTargetData, lb);
				searchSpace.put(node.getId(), matchLabels);
			}
		}
		Long[] queryLabels = searchSpace.keySet().toArray(new Long[searchSpace.size()]);
		firstResult= false;
		subgraphMaching(dbTargetData, dbTargetQuery, queryLabels, 0, frequentSet, newEdge );
		
	}
	
	private static void subgraphMaching(GraphDatabaseService dbTargetData, GraphDatabaseService dbTargetQuery, Long[] queryLabels, int i, Set<String> frequentSet, String newEdge) {
			
			if (queryLabels.length == results.size()) {
				//System.out.println(results);
				firstResult=true;
				frequentSet.add(newEdge);
			} else {
				Long u = queryLabels[i];
				ArrayList<Long> searchSpaceOfU = searchSpace.get(u);
				if( !firstResult) {
					for (Long v : searchSpaceOfU) {
						if (!(results.containsValue(v)) && (canMap(dbTargetData,dbTargetQuery, u, v, queryLabels))) {
							results.put(u, v);
							subgraphMaching(dbTargetData, dbTargetQuery,queryLabels, i + 1, frequentSet,newEdge );
							results.remove(u);
						}
					}
				}
			}
	}
	
	private static boolean canMap(GraphDatabaseService dbTargetData, GraphDatabaseService dbTargetQuery, Long u, Long v, Long[] queryLabels) {
		for (Long id : queryLabels) {
			if (u.equals(id))
				continue;
			if (isEdge(id, u, dbTargetQuery) && (results.containsKey(id))) {
				if (!isEdge(results.get(id), v, dbTargetData))
					return false;
			}
		}
		return true;
	}

	private static boolean isEdge(Long u, Long id, GraphDatabaseService graph) {
		Node start = graph.getNodeById(u);
		Node end = graph.getNodeById(id);
		Iterable<Relationship> relations = start.getRelationships();
		for (Relationship rel : relations) {
			if (rel.getOtherNode(start).equals(end))
				return true;
		}
		return false;
	}

	private static ArrayList<Long> findMatchLabels(GraphDatabaseService dbTargetData, Label lb) {
		ArrayList<Long> matchLables = new ArrayList<Long>();
		ResourceIterator<Node> nodes = dbTargetData.findNodes(lb);
		while (nodes.hasNext()) {
			Node dataNode = nodes.next();
			matchLables.add(dataNode.getId());
		}
		return matchLables;
	}
	
	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
	
}

/**
 * This is the class for SkinnyMine Subgraph Mining Algorithm 
 * @author pragatiunde1990
 *
 */
public class GraphMining {
	private static String subgraphPathFile="C:/Users/pragatiunde1990/Documents/Neo4j/Subgraph2";
	private static File dataFile = new File(subgraphPathFile);
	private static Set<String> frequentSet = new HashSet<String>();
	
	private static Map<String, Integer> s0 = new HashMap<String, Integer>();
	private static GraphDatabaseService dbTargetData;
	private static GraphDatabaseService dbTargetQuery;

	private static ArrayList<String> readFile(String string) {
		ArrayList<String> dataSets = null;
		try {
			dataSets = new ArrayList<String>();
			File file = new File(string);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.contains("t"))
					continue;
				else
					dataSets.add(line);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataSets;

	}

	private static void createDatabase(ArrayList<String> datasets, String dbFileName) {
		try {
			Map<String, Long> graphID = new HashMap<String, Long>();
			File dbNewFile = new File(dbFileName);
			FileUtils.deleteRecursively(dbNewFile);
			BatchInserter inserter = BatchInserters.inserter(dbNewFile);
			for (String line : datasets) {
				String[] nodes = line.split(" ");
				if (nodes[0].equals("v")) {
					if (nodes.length == 3) {
						Label label = Label.label(nodes[2]);
						Map<String, Object> node = new HashMap<String, Object>();
						node.put("id", nodes[1]);
						Long id = inserter.createNode(node, label);
						graphID.put(nodes[1], id);
					}
				} else if (nodes[0].equals("e")) {
					Long nodeID1 = graphID.get(nodes[1]);
					Long nodeID2 = graphID.get(nodes[2]);
					inserter.createRelationship(nodeID1, nodeID2, RelationshipType.withName("edge"), null);
					inserter.createRelationship(nodeID2, nodeID1, RelationshipType.withName("edge"), null);
				}

			}
			inserter.shutdown();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String args[]) {
		ArrayList<String> subgraphDataSets = readFile("F:/Capstone Project/graphExample2.txt");
		createDatabase(subgraphDataSets,subgraphPathFile);
		System.out.println("Target Done");
		dbTargetData = new GraphDatabaseFactory().newEmbeddedDatabase(dataFile);
		
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
		
		System.out.println( "Set S0:" +s0.keySet() );
		Set<String> initialSet = new HashSet<String>();
		initialSet.addAll(s0.keySet());
		String[] setArray = initialSet.toArray(new String[initialSet.size()]);
		int lengthOfDiameter = 6;
		int suportThreshold = 1;
		Long beginTime = System.currentTimeMillis();
		Set<String> diaMine= findDiaMine( dataTransaction,setArray,lengthOfDiameter, suportThreshold, subgraphDataSets);
		System.out.println("Set T: "+diaMine);
		Long endTime = System.currentTimeMillis();
		Long totalTime = endTime - beginTime;
		System.out.println("Performance of graph: "+totalTime);
		dataTransaction.success();
		dataTransaction.close();
		dbTargetData.shutdown();
		
	}

	private static Set<String> findDiaMine(Transaction dataTransaction,String[] setArray, int lengthOfDiameter, int suportThreshold, ArrayList<String> subgraphDataSets) {
		int i = 0;
//		String targetPathFile = "C:/Users/pragatiunde1990/Documents/Neo4j/targetData";		
//		createDatabase(subgraphDataSets, targetPathFile);
		while (Math.pow(2, i) < lengthOfDiameter) {

			i = i + 1;
			if( i >= 2)
				setArray= frequentSet.toArray(new String[frequentSet.size()]);
			frequentSet = new HashSet<String>();
			int count =0;
			for (int j = 0; j < setArray.length; j++) {
				for (int k = 0; k < setArray.length; k++) {
					boolean concat = checkConcat(setArray[j], setArray[k]);
					String newEdge = "";
					if (concat) {
						newEdge = setArray[j] + setArray[k].charAt(setArray[k].length() - 1);
						String queryPathFile = "C:/Users/pragatiunde1990/Documents/Neo4j/queries/query";
						File queryFile = new File (queryPathFile);
					    createQueryGraph(newEdge, queryPathFile);
					    dbTargetQuery = new GraphDatabaseFactory().newEmbeddedDatabase(queryFile);
						Transaction queryTransaction = dbTargetQuery.beginTx();
						//System.out.println(newEdge + (count++) +" Query graph done");
						NaiveSubGraphMining.naiveSubGraphMatching(dbTargetData, dbTargetQuery, dataTransaction,queryTransaction,frequentSet, newEdge);
						queryTransaction.success();
						queryTransaction.close();
						dbTargetQuery.shutdown();
					}
				}
			}
		   System.out.println( "Set S"+i +" "+ frequentSet);
		}
//		setArray= frequentSet.toArray(new String[frequentSet.size()]);
//		frequentSet = new HashSet<String>();
//		if( Math.pow(2, 0) < lengthOfDiameter ){
//			for( int j=0; j< setArray.length; j++){
//				for( int k=0; k<setArray.length; k++){
//					int chkChar= ((setArray[j].length()-1)*2)-lengthOfDiameter;
//					  String diameter= "";
//					  if( checkMergeHead(setArray[j], setArray[k], chkChar)){
//						  diameter = setArray[j]+ setArray[k].substring( setArray[k].length()- chkChar );
//						  String queryPathFile = "C:/Users/pragatiunde1990/Documents/Neo4j/queries/query";
//						  createQueryGraph(diameter, queryPathFile);
//						  NaiveSubGraphMining.naiveSubGraphMatching(subgraphPathFile, queryPathFile, frequentSet, diameter);
//					  }
//					  
//				}
//			}
//		}
		return frequentSet;
	}

	private static boolean checkMergeHead(String first, String second,int chkChar) {
		
		String firstString=first.substring(chkChar);
		String secondString= second.substring(0,second.length()-chkChar);
		
		if(firstString.equalsIgnoreCase(secondString)){
			return true;
		}
		return false;
	}

	private static void createQueryGraph(String newEdge, String queryPathFile) {

		try {
			Map<Integer, String> edgeMap = new LinkedHashMap<Integer, String>();
			Map<Integer, Long> graphID = new HashMap<Integer, Long>();
			File dbNewFile = new File(queryPathFile);
			FileUtils.deleteRecursively(dbNewFile);
			BatchInserter inserter = BatchInserters.inserter(dbNewFile);
			int count = 0;

			for (int i = 0; i < newEdge.length(); i++) {
				edgeMap.put(count++, String.valueOf(newEdge.charAt(i)));
			}
			count = 0;
			for (Map.Entry<Integer, String> entry : edgeMap.entrySet()) {

				Label label = Label.label(edgeMap.get(count));
				Map<String, Object> node = new HashMap<String, Object>();
				node.put("id", count);
				Long id = inserter.createNode(node, label);
				graphID.put(count, id);
				count++;
			}
			for (int i = 0; i < newEdge.length() - 1; i++) {
				Long nodeID1 = graphID.get(i);
				Long nodeID2 = graphID.get(i + 1);
				inserter.createRelationship(nodeID1, nodeID2, RelationshipType.withName("edge"), null);
				inserter.createRelationship(nodeID2, nodeID1, RelationshipType.withName("edge"), null);
			}
			inserter.shutdown();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean checkConcat(String ldash, String ldoubledash) {
		if (ldash.substring(1).equalsIgnoreCase(ldoubledash.substring(0, ldoubledash.length() - 1))) {
			return true;
		}
		return false;
	}
}
