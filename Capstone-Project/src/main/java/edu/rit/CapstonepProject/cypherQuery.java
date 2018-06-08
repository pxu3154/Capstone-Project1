package edu.rit.CapstonepProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class cypherQuery {
	private static final String Iteator = null;
	private static String subgraphPathFile = "C:/Users/pragatiunde1990/Documents/Neo4j/Subgraph2";
	private static File dataFile = new File(subgraphPathFile);
	private static String queryPathFile = "C:/Users/pragatiunde1990/Documents/Neo4j/Query";
	private static File queryFile = new File(queryPathFile);
	private static GraphDatabaseService dbTargetData;
	private static GraphDatabaseService dbTargetQuery;


	public static void main(String[] args) throws IOException {
		
      String[] str = {"NCCCNCCOCNC"};
		
		for(int i= 0 ; i< str.length; i++){
			StringBuilder sb= new StringBuilder("MATCH p= ");
			for( int j=0; j<str[i].length();j++){
				sb.append("(node"+(j+1)+":"+str[i].charAt(j)+")-[rel"+(j+1)+":edgeBetween]->");
			}
			sb.delete(sb.length()-22, sb.length());
			sb.append(" return p");
			String query= sb.toString();
			
			System.out.println(query);
		}
		
		
	}	

}
