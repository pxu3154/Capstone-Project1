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
public class InputGraph {
	private static String subgraphPathFile = "C:/Users/pragatiunde1990/Documents/Neo4j/InputGraph";
	private static File dataFile = new File(subgraphPathFile);

	private static Set<String> edges= new HashSet<String>();

	
	
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
	public static void main(String args[]) throws IOException {
		//ArrayList<String> subgraphDataSets = readFile("F:/Capstone Project/graphExample1.txt");
		//createDatabase(subgraphDataSets, subgraphPathFile);
		String path="F:/SEM IV/Graph Databases/Assignment 4/Proteins/Proteins/part3_Proteins/Proteins/query/backbones_1NTN.128.sub.grf";
		File folder= new File(path);
		processFile(folder);
		System.out.println("Target Done");

	}

	
}
