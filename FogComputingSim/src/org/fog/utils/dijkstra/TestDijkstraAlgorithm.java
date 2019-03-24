package org.fog.utils.dijkstra;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestDijkstraAlgorithm {

    private static List<Vertex> nodes;
    private static List<Edge> edges;
    
    public static void main(String[] args) {
    	 nodes = new ArrayList<Vertex>();
         edges = new ArrayList<Edge>();
         
         for (int i = 0; i < 11; i++)
             nodes.add(new Vertex("Node_" + i));

         edges.add(new Edge(nodes.get(0), nodes.get(1), 85));
         edges.add(new Edge(nodes.get(0), nodes.get(2), 217));
         edges.add(new Edge(nodes.get(0), nodes.get(4), 173));
         edges.add(new Edge(nodes.get(2), nodes.get(6), 186));
         edges.add(new Edge(nodes.get(2), nodes.get(7), 103));
         edges.add(new Edge(nodes.get(3), nodes.get(7), 183));
         edges.add(new Edge(nodes.get(5), nodes.get(8), 250));
         edges.add(new Edge(nodes.get(8), nodes.get(9), 84));
         edges.add(new Edge(nodes.get(7), nodes.get(9), 167));
         edges.add(new Edge(nodes.get(4), nodes.get(9), 502));
         edges.add(new Edge(nodes.get(9), nodes.get(10), 40));
         edges.add(new Edge(nodes.get(1), nodes.get(10), 613));
         edges.add(new Edge(nodes.get(1), nodes.get(0), 85));

         Graph graph = new Graph(nodes, edges);
         DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
                 
         dijkstra.execute(nodes.get(1));
         LinkedList<Vertex> path = dijkstra.getPath(nodes.get(10));
         for (Vertex vertex : path)
             System.out.println(vertex);
	}
}