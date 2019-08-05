package org.fog.placement.algorithm.util.routing;

import java.util.List;

/**
 * Class which defines the graph for the Dijkstra Algorithm.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @see https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 * @since   July, 2019
 */
public class Graph {
	private final List<Vertex> vertexes;
    private final List<Edge> edges;

    public Graph(List<Vertex> vertexes, List<Edge> edges) {
        this.vertexes = vertexes;
        this.edges = edges;
    }

    public List<Vertex> getVertexes() {
        return vertexes;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}