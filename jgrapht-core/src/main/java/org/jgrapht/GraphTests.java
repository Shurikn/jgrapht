/*
 * (C) Copyright 2003-2016, by Barak Naveh, Dimitrios Michail and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.AbstractBaseGraph;

/**
 * A collection of utilities to test for various graph properties.
 * 
 * @author Barak Naveh
 * @author Dimitrios Michail
 */
public abstract class GraphTests
{

    /**
     * Test whether a graph is empty. An empty graph on n nodes consists of n isolated vertices with
     * no edges.
     * 
     * @param graph the input graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return true if the graph is empty, false otherwise
     */
    public static <V, E> boolean isEmpty(Graph<V, E> graph)
    {
        Objects.requireNonNull(graph, "Graph cannot be null");
        return graph.edgeSet().isEmpty();
    }

    /**
     * Check if a graph is simple, i.e. has no self-loops and multiple edges.
     * 
     * @param graph a graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return true if a graph is simple, false otherwise
     */
    public static <V, E> boolean isSimple(Graph<V, E> graph)
    {
        Objects.requireNonNull(graph, "Graph cannot be null");
        if (graph instanceof AbstractBaseGraph<?, ?>) {
            AbstractBaseGraph<V, E> abg = (AbstractBaseGraph<V, E>) graph;
            if (!abg.isAllowingLoops() && !abg.isAllowingMultipleEdges()) {
                return true;
            }
        }
        // no luck, we have to check
        boolean isDirected = graph instanceof DirectedGraph<?, ?>;
        for (V v : graph.vertexSet()) {
            Iterable<E> edgesOf;
            if (isDirected) {
                edgesOf = ((DirectedGraph<V, E>) graph).outgoingEdgesOf(v);
            } else {
                edgesOf = graph.edgesOf(v);
            }
            Set<V> neighbors = new HashSet<>();
            for (E e : edgesOf) {
                V u = Graphs.getOppositeVertex(graph, e, v);
                if (u.equals(v) || !neighbors.add(u)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Test whether a graph is complete. A complete undirected graph is a simple graph in which
     * every pair of distinct vertices is connected by a unique edge. A complete directed graph is a
     * directed graph in which every pair of distinct vertices is connected by a pair of unique
     * edges (one in each direction)
     * 
     * @param graph the input graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return true if the graph is complete, false otherwise
     */
    public static <V, E> boolean isComplete(Graph<V, E> graph)
    {
        Objects.requireNonNull(graph, "Graph cannot be null");
        int n = graph.vertexSet().size();
        int allEdges;
        if (graph instanceof DirectedGraph<?, ?>) {
            allEdges = Math.multiplyExact(n, n - 1);
        } else if (graph instanceof UndirectedGraph<?, ?>) {
            if (n % 2 == 0) {
                allEdges = Math.multiplyExact(n / 2, n - 1);
            } else {
                allEdges = Math.multiplyExact(n, (n - 1) / 2);
            }
        } else {
            throw new IllegalArgumentException("Graph must be directed or undirected");
        }
        return graph.edgeSet().size() == allEdges && isSimple(graph);
    }

    /**
     * Test whether an undirected graph is connected.
     * 
     * @param graph the input graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return true if the graph is connected, false otherwise
     */
    public static <V, E> boolean isConnected(UndirectedGraph<V, E> graph)
    {
        Objects.requireNonNull(graph, "Graph cannot be null");
        return new ConnectivityInspector<>(graph).isGraphConnected();
    }

    /**
     * Test whether a directed graph is weakly connected.
     * 
     * @param graph the input graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return true if the graph is weakly connected, false otherwise
     */
    public static <V, E> boolean isWeaklyConnected(DirectedGraph<V, E> graph)
    {
        Objects.requireNonNull(graph, "Graph cannot be null");
        return new ConnectivityInspector<>(graph).isGraphConnected();
    }

    /**
     * Test whether a directed graph is strongly connected.
     * 
     * @param graph the input graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return true if the graph is strongly connected, false otherwise
     */
    public static <V, E> boolean isStronglyConnected(DirectedGraph<V, E> graph)
    {
        Objects.requireNonNull(graph, "Graph cannot be null");
        return new KosarajuStrongConnectivityInspector<>(graph).isStronglyConnected();
    }

    /**
     * Test whether an undirected graph is a tree.
     * 
     * @param graph the input graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return true if the graph is tree, false otherwise
     */
    public static <V, E> boolean isTree(UndirectedGraph<V, E> graph)
    {
        return (graph.edgeSet().size() == (graph.vertexSet().size() - 1)) && isConnected(graph);
    }

    /**
     * Test whether a graph is bipartite.
     * 
     * @param graph the input graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return true if the graph is bipartite, false otherwise
     */
    public static <V, E> boolean isBipartite(Graph<V, E> graph)
    {
        if (isEmpty(graph)) {
            return true;
        }
        try {
            // at most n^2/4 edges
            if (Math.multiplyExact(4, graph.edgeSet().size()) > Math
                .multiplyExact(graph.vertexSet().size(), graph.vertexSet().size()))
            {
                return false;
            }
        } catch (ArithmeticException e) {
            // ignore
        }

        Set<V> unknown = new HashSet<>(graph.vertexSet());
        Set<V> odd = new HashSet<>();
        Deque<V> queue = new LinkedList<>();

        while (!unknown.isEmpty()) {
            if (queue.isEmpty()) {
                queue.add(unknown.iterator().next());
            }

            V v = queue.removeFirst();
            unknown.remove(v);

            for (E e : graph.edgesOf(v)) {
                V n = Graphs.getOppositeVertex(graph, e, v);
                if (unknown.contains(n)) {
                    queue.add(n);
                    if (!odd.contains(v)) {
                        odd.add(n);
                    }
                } else if (!(odd.contains(v) ^ odd.contains(n))) {
                    return false;
                }
            }
        }
        return true;
    }

}

// End GraphTests.java
