package org.fog.placement.algorithm.util.routing;

/**
 * Class which defines edges for the Dijkstra Algorithm.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @see https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 * @since   July, 2019
 */
public class Edge {
    private final Vertex source;
    private final Vertex destination;
    private final double weight;

    public Edge(Vertex source, Vertex destination, double weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    public Vertex getDestination() {
        return destination;
    }

    public Vertex getSource() {
        return source;
    }
    public double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return source + " " + destination;
    }
}