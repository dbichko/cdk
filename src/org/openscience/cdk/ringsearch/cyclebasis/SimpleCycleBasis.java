/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 * 
 * Copyright (C) 2004  The Chemistry Development Kit (CDK) project
 * 
 * Contact: cdk-devel@lists.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
 * 
 */

package org.openscience.cdk.ringsearch.cyclebasis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org._3pq.jgrapht.DirectedGraph;
import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.Graph;
import org._3pq.jgrapht.UndirectedGraph;
import org._3pq.jgrapht.alg.ConnectivityInspector;
import org._3pq.jgrapht.alg.DijkstraShortestPath;
import org._3pq.jgrapht.graph.SimpleDirectedGraph;
import org._3pq.jgrapht.graph.SimpleGraph;
import org._3pq.jgrapht.graph.Subgraph;
import org.openscience.cdk.graph.MinimalPathIterator;

/**
 * Auxiliary class for <code>CycleBasis</code>.
 * 
 * @author Ulrich Bauer <baueru@cs.tum.edu>
 * 
 *
 * @cdk.module standard
 *
 * @cdk.builddepends jgrapht-0.5.3.jar
 * @cdk.depends jgrapht-0.5.3.jar
 */

public class SimpleCycleBasis {
	
	List edgeList;
	List cycles;
	UndirectedGraph graph;
	
	private boolean isMinimized = false;
	
	public SimpleCycleBasis (List cycles, List edgeList, UndirectedGraph graph) {
		this.edgeList = edgeList;
		this.cycles = cycles;
		this.graph = graph;
	}
	
	
	public SimpleCycleBasis (UndirectedGraph graph) {
		this.cycles = new ArrayList();
		this.edgeList = new ArrayList();
		this.graph = graph;
		
		createMinimalCycleBase();
		
	}
	
	
	private void createFundamentalTreeBase() {
		
		// To build a cycle base from a graph, we perform a breadth first traversal
		// and build a fundamental tree base ("Kirchhoff base")
		
		
		Object currentVertex = graph.vertexSet().iterator().next();
		
		// We build a spanning tree as a directed graph to easily find the parent of a
		// vertex in the tree. This means however that we have to create new Edge objects
		// for the tree and can't just use the Edge objects of the graph, since the
		// the edge in the graph might have a wrong direction.
		
		DirectedGraph spanningTree = new SimpleDirectedGraph();
		
		// We need to remember the non-tree edges because we create one cycle for each
		Set visitedEdges = new HashSet();
		
		// FIFO for the BFS
		LinkedList vertexQueue = new LinkedList();
		
		// currentVertex is the root of the spanning tree
		spanningTree.addVertex(currentVertex);
		
		vertexQueue.addLast(currentVertex);
		
		List treeEdges = new Vector();
		
		while (!vertexQueue.isEmpty()) {
			currentVertex = vertexQueue.removeFirst();
			
			Iterator edges = graph.edgesOf(currentVertex).iterator();
			while (edges.hasNext()) {
				// find a neighbour vertex of the current vertex 
				Edge edge = (Edge)edges.next();
				
				if (!visitedEdges.contains(edge)) {
					
					// mark edge as visited
					visitedEdges.add(edge);
					
					Object nextVertex = edge.oppositeVertex(currentVertex);
					
					if (!spanningTree.containsVertex(nextVertex)) {
						// tree edge
						
						treeEdges.add(edge);
						
						spanningTree.addVertex(nextVertex);
						
						// create a new (directed) Edge object (as explained above)
						spanningTree.addEdge(currentVertex, nextVertex);
						
						// add the next vertex to the BFS-FIFO
						vertexQueue.addLast(nextVertex);
					} else {
						// non-tree edge
						
						// This edge defines a cycle together with the edges of the spanning tree
						// along the path to the root of the tree. We create a new cycle containing 
						// these edges (not the tree edges, but the corresponding edges in the graph)
						
						List edgesOfCycle = new Vector();
						
						// follow the path to the root of the tree
						
						Object vertex = currentVertex;
						
						// get parent of vertex
						List incomingEdgesOfVertex = spanningTree.incomingEdgesOf(vertex);
						Object parent = incomingEdgesOfVertex.isEmpty() ? null : ((Edge)incomingEdgesOfVertex.get(0)).oppositeVertex(vertex);	
						
						while (parent != null) {
							// add the corresponding edge to the cycle
							edgesOfCycle.add(graph.getEdge(vertex, parent));
							
							// go up the tree
							vertex = parent;
							
							// get parent of vertex
							incomingEdgesOfVertex = spanningTree.incomingEdgesOf(vertex);
							parent = incomingEdgesOfVertex.isEmpty() ? null : ((Edge)incomingEdgesOfVertex.get(0)).oppositeVertex(vertex);	
						}
						
						// do the same thing for nextVertex
						vertex = nextVertex;
						
						// get parent of vertex
						incomingEdgesOfVertex = spanningTree.incomingEdgesOf(vertex);
						parent = incomingEdgesOfVertex.isEmpty() ? null : ((Edge)incomingEdgesOfVertex.get(0)).oppositeVertex(vertex);
						
						while (parent != null) {
							edgesOfCycle.add(graph.getEdge(vertex, parent));
							vertex = parent;
							
							// get parent of vertex
							incomingEdgesOfVertex = spanningTree.incomingEdgesOf(vertex);
							parent = incomingEdgesOfVertex.isEmpty() ? null : ((Edge)incomingEdgesOfVertex.get(0)).oppositeVertex(vertex);	
						}
						
						// finally, add the non-tree edge to the cycle
						edgesOfCycle.add(edge);
						
						// add the edge to the index list for the incidence matrix
						edgeList.add(edge);
						
						Cycle newCycle = new Cycle(graph, edgesOfCycle);
						
						cycles.add(newCycle);
						
					}
				}
			}
			
		}
		
		edgeList.addAll(treeEdges);
		
	}
	
	private void createMinimalCycleBase() {
		
		Graph subgraph = new Subgraph(graph, null, null);
		
		Set remainingEdges = new HashSet(graph.edgeSet());
		Set selectedEdges = new HashSet();
		
		while (!remainingEdges.isEmpty()) {
			Edge edge = (Edge)remainingEdges.iterator().next();
			
			subgraph.removeEdge(edge);
			
			// Compute a shortest cycle through edge
			List path = DijkstraShortestPath.findPathBetween(subgraph, edge.getSource(), edge.getTarget());
			path.add(edge);
			Cycle cycle = new Cycle(graph, path);
			
			subgraph.addEdge(edge);
			
			selectedEdges.add(edge);
			
			
			cycles.add(cycle);
			edgeList.add(edge);
			
			remainingEdges.removeAll(path);
		}
		
		subgraph.removeAllEdges(selectedEdges);
		
		// The cycles just created are already minimal, so we can start minimizing at startIndex
		int startIndex = cycles.size();
		
		// Now we perform a breadth first traversal and build a fundamental tree base
		// ("Kirchhoff base") of the remaining subgraph
		
		Object currentVertex = graph.vertexSet().iterator().next();
		
		// We build a spanning tree as a directed graph to easily find the parent of a
		// vertex in the tree. This means however that we have to create new Edge objects
		// for the tree and can't just use the Edge objects of the graph, since the
		// the edge in the graph might have a wrong or no direction.
		
		DirectedGraph spanningTree = new SimpleDirectedGraph();
		
		Set visitedEdges = new HashSet();
		
		// FIFO for the BFS
		LinkedList vertexQueue = new LinkedList();
		
		// currentVertex is the root of the spanning tree
		spanningTree.addVertex(currentVertex);
		
		vertexQueue.addLast(currentVertex);
		
		// We need to remember the tree edges so we can add them at once to the
		// index list for the incidence matrix
		
		List treeEdges = new Vector();
		
		while (!vertexQueue.isEmpty()) {
			currentVertex = vertexQueue.removeFirst();
			
			Iterator edges = subgraph.edgesOf(currentVertex).iterator();
			while (edges.hasNext()) {
				// find a neighbour vertex of the current vertex 
				Edge edge = (Edge)edges.next();
				
				if (!visitedEdges.contains(edge)) {
					
					// mark edge as visited
					visitedEdges.add(edge);
					
					Object nextVertex = edge.oppositeVertex(currentVertex);
					
					if (!spanningTree.containsVertex(nextVertex)) {
						// tree edge
						
						treeEdges.add(edge);
						
						spanningTree.addVertex(nextVertex);
						
						// create a new (directed) Edge object (as explained above)
						spanningTree.addEdge(currentVertex, nextVertex);
						
						// add the next vertex to the BFS-FIFO
						vertexQueue.addLast(nextVertex);
					} else {
						// non-tree edge
						
						// This edge defines a cycle together with the edges of the spanning tree
						// along the path to the root of the tree. We create a new cycle containing 
						// these edges (not the tree edges, but the corresponding edges in the graph)
						
						List edgesOfCycle = new Vector();
						
						// follow the path to the root of the tree
						
						Object vertex = currentVertex;
						
						// get parent of vertex
						List incomingEdgesOfVertex = spanningTree.incomingEdgesOf(vertex);
						Object parent = incomingEdgesOfVertex.isEmpty() ? null : ((Edge)incomingEdgesOfVertex.get(0)).oppositeVertex(vertex);	
						
						while (parent != null) {
							// add the corresponding edge to the cycle
							edgesOfCycle.add(subgraph.getEdge(vertex, parent));
							
							// go up the tree
							vertex = parent;
							
							// get parent of vertex
							incomingEdgesOfVertex = spanningTree.incomingEdgesOf(vertex);
							parent = incomingEdgesOfVertex.isEmpty() ? null : ((Edge)incomingEdgesOfVertex.get(0)).oppositeVertex(vertex);	
						}
						
						// do the same thing for nextVertex
						vertex = nextVertex;
						
						// get parent of vertex
						incomingEdgesOfVertex = spanningTree.incomingEdgesOf(vertex);
						parent = incomingEdgesOfVertex.isEmpty() ? null : ((Edge)incomingEdgesOfVertex.get(0)).oppositeVertex(vertex);
						
						while (parent != null) {
							edgesOfCycle.add(subgraph.getEdge(vertex, parent));
							vertex = parent;
							
							// get parent of vertex
							incomingEdgesOfVertex = spanningTree.incomingEdgesOf(vertex);
							parent = incomingEdgesOfVertex.isEmpty() ? null : ((Edge)incomingEdgesOfVertex.get(0)).oppositeVertex(vertex);	
						}
						
						// finally, add the non-tree edge to the cycle
						edgesOfCycle.add(edge);
						
						// add the edge to the index list for the incidence matrix
						edgeList.add(edge);
						
						Cycle newCycle = new Cycle(graph, edgesOfCycle);
						
						cycles.add(newCycle);
						
					}
				}
			}
			
		}
		
		// Add all the tree edges to the index list for the incidence matrix
		edgeList.addAll(treeEdges);
		
		// Now the index list is ordered: first the non-tree edges, then the tree edge.
		// Moreover, since the cycles and the corresponding non-tree edge have been added
		// to their lists in the same order, the incidence matrix is in upper triangular form.
		
		// Now we can minimize the cycles created from the tree base
		minimize(startIndex);
		
	}
	
	boolean[][] getCycleEdgeIncidenceMatrix () {
		return getCycleEdgeIncidenceMatrix((Object[]) cycles.toArray());
	}
	
	
	boolean[][] getCycleEdgeIncidenceMatrix (Object[] cycleArray) {
		boolean[][] result = new boolean[cycleArray.length][edgeList.size()];
		
		for (int i=0; i<cycleArray.length; i++) {
			Cycle cycle = (Cycle) cycleArray[i];
			for (int j=0; j<edgeList.size(); j++) {
				Edge edge = (Edge)edgeList.get(j);
				result[i][j] = cycle.containsEdge(edge);
			}
		}
		
		return result;
	}
	
	private void minimize() {
		
		if (isMinimized) 
			return;
		
		if (cycles.size()==0) 
			return;
		else 
			minimize(0);
		
		isMinimized = true;
	}
	
	private void minimize(int startIndex) {
		
		if (isMinimized) 
			return;
		
		// Implementation of "Algorithm 1" from [BGdV04]
		
		Map edgeMap = new HashMap();
		
		for (int i=0; i<edgeList.size(); i++) {
			Edge edge = (Edge) edgeList.get(i);
			
			edgeMap.put(edge, new Integer(i));
		}
		
		boolean[][] a = getCycleEdgeIncidenceMatrix();
		
		// perform gaussian elimination on the incidence matrix
		// to bring it to upper triangular form
		for (int i=0; i<startIndex; i++) {
			for (int j=0; j<i; j++) {
				if (a[i][j]) {
					for (int k=0; k<edgeList.size(); k++) {
						a[i][k] = (a[i][k]!=a[j][k]);
					}
				}
			}
		}
		
		for (int i=startIndex; i<cycles.size(); i++) {
			// "Subroutine 2"
			
			// Construct kernel vector u
			boolean[] u = constructKernelVector(edgeList.size(), a, i);
			
			// Construct auxiliary graph gu
			AuxiliaryGraph gu = new AuxiliaryGraph(graph, edgeList, u);
			
			Cycle shortestCycle = (Cycle) cycles.get(i);
			
			Iterator vertexIterator = graph.vertexSet().iterator();
			while (vertexIterator.hasNext()) {
				Object vertex = vertexIterator.next();
				
				Collection incidentEdges = graph.edgesOf(vertex);
				
				// check if the vertex is incident to an edge with u[edge] == 1
				boolean shouldSearchCycle = false;
				
				Iterator edgeIterator = incidentEdges.iterator();
				while (edgeIterator.hasNext()) {
					Object edge = edgeIterator.next();
					int index = ((Integer) edgeMap.get(edge)).intValue();
					if (u[index]) {
						shouldSearchCycle = true;
						break;
					}
				}
				
				if (shouldSearchCycle) {
					
					Object auxVertex0 = gu.auxVertex0(vertex);
					Object auxVertex1 = gu.auxVertex1(vertex);
					
					// Search for shortest path
					
					List auxPath = DijkstraShortestPath.findPathBetween(gu, auxVertex0, auxVertex1);
					
					List edgesOfNewCycle = new Vector();
					
					// Save all vertices on the path in a HashMap
					// to check if there is a shorter x-x' path in the path
					
					Set verticesOnPath = new HashSet();
					
					Object v = vertex;
					
					boolean pathIsCycle = true;
					
					edgeIterator = auxPath.iterator();
					while (edgeIterator.hasNext() && pathIsCycle) {
						Edge auxEdge = (Edge) edgeIterator.next();
						
						// Get the edge corresponding to the aux. edge
						Edge e = (Edge) gu.edge(auxEdge);
						
						edgesOfNewCycle.add(e);					
						
						// Get next vertex on path
						v = e.oppositeVertex(v);
						
						// if we have already seen this edge, the path is not a cycle
						
						if (verticesOnPath.contains(v)) {
							pathIsCycle = false;
						} else {
							verticesOnPath.add(v);
						}
						
						
					}
					
					
					if (pathIsCycle) {
						
						Cycle newCycle = new Cycle(graph, edgesOfNewCycle);
						
						if (newCycle.weight() < shortestCycle.weight()) {
							shortestCycle = newCycle;
						}
						
					}
					
				}
				
			}
			
			cycles.set(i, shortestCycle);
			
			// insert the new cycle into the matrix
			for (int j=1; j<edgeList.size(); j++) {
				a[i][j] = shortestCycle.containsEdge((Edge) edgeList.get(j));
			}
			
			// perform gaussian elimination on the inserted row
			for (int j=0; j<i; j++) {
				if (a[i][j]) {
					for (int k=0; k<edgeList.size(); k++) {
						a[i][k] = (a[i][k]!=a[j][k]);
					}
				}
			}
		}
		
		isMinimized = true;
		
	}
	
	static boolean[] constructKernelVector(int m, boolean[][] am, int i) {
		// Construct kernel vector u by setting u[i] = true ...
		boolean[] u = new boolean[m];
		u[i] = true;
		
		// ... u[j] = 0 (false) for j > i (by initialization)...
		
		// ... and solving Am u = 0
		
		for (int j=i-1; j>=0; j--) {
			u[j] = false;
			for (int k=i; k>j; k--) {
				u[j] = (u[j] != (am[j][k] && u[k]));
			}
		}
		return u;
	}
	
	
	public void printIncidenceMatrix() {
		
		/*
		 for (int j=0; j<edgeList.size(); j++) {
		 System.out.print(((Edge) edgeList.get(j)).getSource());
		 }
		 System.out.println();
		 for (int j=0; j<edgeList.size(); j++) {
		 System.out.print(((Edge) edgeList.get(j)).getTarget());
		 }
		 System.out.println();
		 for (int j=0; j<edgeList.size(); j++) {
		 System.out.print('-');
		 }
		 System.out.println();
		 */
		
		boolean[][] incidMatr = getCycleEdgeIncidenceMatrix();
		for (int i=0; i<incidMatr.length; i++) {
			for (int j=0; j<incidMatr[i].length; j++) {
				System.out.print(incidMatr[i][j]?1:0);
			}
			System.out.println();
		}
	}
	
	public int[] weightVector() {
		
		int[] result = new int[cycles.size()];
		for (int i=0; i<cycles.size(); i++) {
			Cycle cycle = (Cycle) cycles.get(i);
			result[i] = (int) cycle.weight();
		}
		Arrays.sort(result);
		
		return result;
	}
	
	public Collection edges() {
		return edgeList;
	}
	
	public Collection cycles() {
		return cycles;
	}
	
	static boolean[][] inverseBinaryMatrix(boolean[][] m, int n) {
		
		boolean[][] a = new boolean[n][n];
		for (int i=0; i<n; i++) {
			for (int j=0; j<n; j++) {
				a[i][j] = m[i][j];
			}
		}
		
		boolean[][] r = new boolean[n][n];
		
		for (int i=0; i<n; i++) {
			r[i][i] = true;
		}		
		
		for (int i=0; i<n; i++) {
			for (int j=i; j<n; j++) {
				if (a[j][i]) {
					for (int k=0; k<n; k++) {
						if ((k!=j) && (a[k][i])) {
							for (int l=0; l<n; l++) {
								a[k][l] = (a[k][l] != a[j][l]);
								r[k][l] = (r[k][l] != r[j][l]);
							}
						}
					}
					if (i!=j) {
						boolean[] swap = a[i];
						a[i] = a[j];
						a[j] = swap;
						swap = r[i];
						r[i] = r[j];
						r[j] = swap;
					}
					break;
				}
			}
		}
		
		return r;
	}
	
	public Collection essentialCycles() {
		Collection result = new HashSet();
		minimize();
		
		
		boolean[][] a = getCycleEdgeIncidenceMatrix();
		
		boolean[][] ai = inverseBinaryMatrix(a, cycles.size());
		
		for (int i=0; i<cycles.size(); i++) {
			
			// Construct kernel vector u
			boolean[] u = new boolean[edgeList.size()];
			for (int j=0; j<cycles.size(); j++) {
				u[j] = ai[j][i];
			}
			
			// Construct kernel vector u from a column of the inverse of a
			AuxiliaryGraph gu = new AuxiliaryGraph(graph, edgeList, u);
			
			boolean isEssential = true;
			
			Iterator vertexIterator = graph.vertexSet().iterator();
			while (vertexIterator.hasNext()) {
				Object vertex = vertexIterator.next();
				
				Object auxVertex0 = gu.auxVertex0(vertex);
				Object auxVertex1 = gu.auxVertex1(vertex);
				
				
				// Search for shortest paths
				
				for (Iterator minPaths = new MinimalPathIterator(gu, auxVertex0, auxVertex1); minPaths.hasNext();) {
					List auxPath = (List) minPaths.next();
					List edgesOfNewCycle = new ArrayList(auxPath.size());
					
					Iterator edgeIterator = auxPath.iterator();
					while (edgeIterator.hasNext()) {
						Edge auxEdge = (Edge) edgeIterator.next();
						
						// Get the edge corresponding to the aux. edge
						Edge e = (Edge) gu.edge(auxEdge);
						
						edgesOfNewCycle.add(e);
						
					}
					
					
					Cycle cycle = new Cycle(graph, edgesOfNewCycle);
					
					
					if (cycle.weight() > ((Cycle)cycles.get(i)).weight()) {
						break;
					}
					
					if (!cycle.equals((Cycle)cycles.get(i))) {
						isEssential = false;
						break;
					}
					
				}
				
			}
			
			if (isEssential) {
				result.add((Cycle)cycles.get(i));
			}
			
		}
		
		return result;
	}
	
	
	public Map relevantCycles() {
		Map result = new HashMap();
		minimize();
		
		boolean[][] a = getCycleEdgeIncidenceMatrix();
		
		boolean[][] ai = inverseBinaryMatrix(a, cycles.size());
		
		for (int i=0; i<cycles.size(); i++) {
			
			// Construct kernel vector u from a column of the inverse of a
			boolean[] u = new boolean[edgeList.size()];
			for (int j=0; j<cycles.size(); j++) {
				u[j] = ai[j][i];
			}
			
			// Construct auxiliary graph gu
			AuxiliaryGraph gu = new AuxiliaryGraph(graph, edgeList, u);
			
			Iterator vertexIterator = graph.vertexSet().iterator();
			while (vertexIterator.hasNext()) {
				Object vertex = vertexIterator.next();
				
				Object auxVertex0 = gu.auxVertex0(vertex);
				Object auxVertex1 = gu.auxVertex1(vertex);
				
				// Search for shortest paths
				
				for (Iterator minPaths = new MinimalPathIterator(gu, auxVertex0, auxVertex1); minPaths.hasNext();) {
					List auxPath = (List) minPaths.next();
					List edgesOfNewCycle = new ArrayList(auxPath.size());
					
					Iterator edgeIterator = auxPath.iterator();
					while (edgeIterator.hasNext()) {
						Edge auxEdge = (Edge) edgeIterator.next();
						
						// Get the edge corresponding to the aux. edge
						Edge e = (Edge) gu.edge(auxEdge);
						
						edgesOfNewCycle.add(e);
						
					}
					
					
					Cycle cycle = new Cycle(graph, edgesOfNewCycle);
					
					if (cycle.weight() > ((Cycle)cycles.get(i)).weight()) {
						break;
					}
					
					result.put(cycle, (Cycle)cycles.get(i));
				}
				
			}
		}
		
		
		return result;
	}
	
	public List equivalenceClasses() {
		int[] weight = weightVector();
		
		Object[] cyclesArray = (Object[]) cycles.toArray();
		Arrays.sort(cyclesArray, new Comparator() {
			public int compare(Object o1, Object o2) {
				return (int) (((Cycle)o1).weight() - ((Cycle)o2).weight());
			}
		});
		
		Collection essentialCycles = essentialCycles();
		
		boolean[][] u = new boolean[cyclesArray.length][edgeList.size()];
		
		boolean[][] a = getCycleEdgeIncidenceMatrix(cyclesArray);
		boolean[][] ai = inverseBinaryMatrix(a, cyclesArray.length);
		
		for (int i=0; i<cyclesArray.length; i++) {
			for (int j=0; j<cyclesArray.length; j++) {
				u[i][j] = ai[j][i];
			}
		}
		
		
		
		UndirectedGraph h = new SimpleGraph();
		h.addAllVertices(cycles);
		
		ConnectivityInspector connectivityInspector = new ConnectivityInspector(h);
		
		int left=0;
		for (int right=0; right<weight.length; right++) {
			if ((right<weight.length-1) && (weight[right+1]==weight[right]))
				continue;
			
			// cyclesArray[left] to cyclesArray[right] have same weight
			
			for (int i=left; i<=right; i++) {
				if (essentialCycles.contains((Cycle) cyclesArray[i]))
					continue;
				
				for (int j=i+1; j<=right; j++) {
					if (essentialCycles.contains((Cycle) cyclesArray[j]))
						continue;
					
					// check if cyclesArray[i] and cyclesArray[j] are already in the same class
					if (connectivityInspector.pathExists(cyclesArray[i], cyclesArray[j])) 
						continue;
					
					boolean sameClass = false;
										
					AuxiliaryGraph2 auxGraph = new AuxiliaryGraph2(graph, edgeList, u[i], u[j]);
					
					for (Iterator it = graph.vertexSet().iterator(); it.hasNext();) {
						Object vertex = it.next();
						
						Object auxVertex00 = auxGraph.vertexMap00.get(vertex);
						Object auxVertex11 = auxGraph.vertexMap11.get(vertex);
						
						List auxPath = DijkstraShortestPath.findPathBetween(auxGraph, auxVertex00, auxVertex11);
						
						double pathWeight = auxPath.size();
						
						if (pathWeight == weight[left]) {
							sameClass = true;
							break;
						}	
					}
					
					if (sameClass) {
						h.addEdge(cyclesArray[i], cyclesArray[j]);
					}
				}			
			}
			
			for (int i=left; i<=right; i++) {
				if (essentialCycles.contains((Cycle) cyclesArray[i]))
					continue;
				
				for (int j=i+1; j<=right; j++) {
					if (essentialCycles.contains((Cycle) cyclesArray[j]))
						continue;
					
					// check if cyclesArray[i] and cyclesArray[j] are already in the same class
					if (connectivityInspector.pathExists(cyclesArray[i], cyclesArray[j])) 
						continue;
					
					boolean sameClass = false;

					for (int k=0; ((Cycle)cyclesArray[k]).weight() < weight[left]; k++) {
						
						AuxiliaryGraph2 auxGraph1 = new AuxiliaryGraph2(graph, edgeList, u[i], u[k]);
						
						boolean shortestPathFound = false;
						for (Iterator it = graph.vertexSet().iterator(); it.hasNext();) {
							Object vertex = it.next();
							
							Object auxVertex00 = auxGraph1.vertexMap00.get(vertex);
							Object auxVertex11 = auxGraph1.vertexMap11.get(vertex);
							
							List auxPath = DijkstraShortestPath.findPathBetween(auxGraph1, auxVertex00, auxVertex11);
							
							double pathWeight = auxPath.size();
							
							if (pathWeight == weight[left]) {
								shortestPathFound = true;
								break;
							}	
						}
						
						if (!shortestPathFound) 
							continue;
						
						AuxiliaryGraph2 auxGraph2 = new AuxiliaryGraph2(graph, edgeList, u[j], u[k]);
						
						for (Iterator it = graph.vertexSet().iterator(); it.hasNext();) {
							Object vertex = it.next();
							
							Object auxVertex00 = auxGraph2.vertexMap00.get(vertex);
							Object auxVertex11 = auxGraph2.vertexMap11.get(vertex);
							
							List auxPath = DijkstraShortestPath.findPathBetween(auxGraph2, auxVertex00, auxVertex11);
							
							double pathWeight = auxPath.size();
							
							if (pathWeight == weight[left]) {
								sameClass = true;
								break;
							}	
						}
						
						if (sameClass)
							break;
					}
					
					if (sameClass) {
						h.addEdge(cyclesArray[i], cyclesArray[j]);
					}
				}
			}
			

			
			left=right+1;
		}
		
		return connectivityInspector.connectedSets();
	}
	
	private class AuxiliaryGraph extends SimpleGraph {
		
		// graph to aux. graph
		HashMap vertexMap0 = new HashMap();
		HashMap vertexMap1 = new HashMap();
		
		// aux. edge to edge
		Map auxEdgeMap = new HashMap();
		
		AuxiliaryGraph(Graph graph, List edgeList, boolean[] u) {
			Iterator vertexIterator = graph.vertexSet().iterator();
			
			for (int k = 1; vertexIterator.hasNext(); k++) {
				Object vertex = vertexIterator.next();
				
				//vertexArray[j] = vertex;
				//vertexArray[j + graph.vertexSet().size()] = vertex;
				
				Object newVertex0 = new Integer(k);
				vertexMap0.put(vertex, newVertex0);
				addVertex(newVertex0);
				
				Object newVertex1 = new Integer(-k);
				vertexMap1.put(vertex, newVertex1);
				addVertex(newVertex1);
			}
			
			
			for (int j = 0; j < edgeList.size(); j++) {
				Edge edge = (Edge)edgeList.get(j);
				
				Object vertex1 = edge.getSource();
				Object vertex2 = edge.getTarget();
				
				
				if (u[j]) {
					Object vertex1u = vertexMap0.get(vertex1);
					Object vertex2u = vertexMap1.get(vertex2);
					Edge auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap1.get(vertex1);
					vertex2u = vertexMap0.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
				} else {
					Object vertex1u = vertexMap0.get(vertex1);
					Object vertex2u = vertexMap0.get(vertex2);
					Edge auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap1.get(vertex1);
					vertex2u = vertexMap1.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
				}
				
			}
		}
		
		Object auxVertex0(Object vertex) {
			return vertexMap0.get(vertex);
		}
		
		Object auxVertex1(Object vertex) {
			return vertexMap1.get(vertex);
		}
		
		Object edge(Object auxEdge) {
			return auxEdgeMap.get(auxEdge);
		}
	}
	
	private class AuxiliaryGraph2 extends SimpleGraph {
		
		// graph to aux. graph
		HashMap vertexMap00 = new HashMap();
		HashMap vertexMap01 = new HashMap();
		HashMap vertexMap10 = new HashMap();
		HashMap vertexMap11 = new HashMap();
		
		// aux. edge to edge
		Map auxEdgeMap = new HashMap();
		
		AuxiliaryGraph2(Graph graph, List edgeList, boolean[] ui, boolean[] uj) {
			Iterator vertexIterator = graph.vertexSet().iterator();
			
			for (int k = 1; vertexIterator.hasNext(); k++) {
				Object vertex = vertexIterator.next();
				
				//vertexArray[j] = vertex;
				//vertexArray[j + graph.vertexSet().size()] = vertex;
				
				Object newVertex00 = vertex + "-00";
				vertexMap00.put(vertex, newVertex00);
				addVertex(newVertex00);
				
				Object newVertex01 = vertex + "-01";
				vertexMap01.put(vertex, newVertex01);
				addVertex(newVertex01);
				
				Object newVertex10 = vertex + "-10";
				vertexMap10.put(vertex, newVertex10);
				addVertex(newVertex10);
				
				Object newVertex11 = vertex + "-11";
				vertexMap11.put(vertex, newVertex11);
				addVertex(newVertex11);
			}
			
			
			for (int k = 0; k < edgeList.size(); k++) {
				Edge edge = (Edge)edgeList.get(k);
				
				Object vertex1 = edge.getSource();
				Object vertex2 = edge.getTarget();
				
				
				if (!ui[k] && !uj[k]) {
					Object vertex1u = vertexMap00.get(vertex1);
					Object vertex2u = vertexMap00.get(vertex2);
					Edge auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap01.get(vertex1);
					vertex2u = vertexMap01.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap10.get(vertex1);
					vertex2u = vertexMap10.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap11.get(vertex1);
					vertex2u = vertexMap11.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
				} else if (ui[k] && !uj[k]) {
					Object vertex1u = vertexMap00.get(vertex1);
					Object vertex2u = vertexMap10.get(vertex2);
					Edge auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap01.get(vertex1);
					vertex2u = vertexMap11.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap10.get(vertex1);
					vertex2u = vertexMap00.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap11.get(vertex1);
					vertex2u = vertexMap01.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
				} else if (!ui[k] && uj[k]) {
					Object vertex1u = vertexMap00.get(vertex1);
					Object vertex2u = vertexMap01.get(vertex2);
					Edge auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap01.get(vertex1);
					vertex2u = vertexMap00.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap10.get(vertex1);
					vertex2u = vertexMap11.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap11.get(vertex1);
					vertex2u = vertexMap10.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
				} else if (ui[k] && uj[k]) {
					Object vertex1u = vertexMap00.get(vertex1);
					Object vertex2u = vertexMap11.get(vertex2);
					Edge auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap01.get(vertex1);
					vertex2u = vertexMap10.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap10.get(vertex1);
					vertex2u = vertexMap01.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
					
					vertex1u = vertexMap11.get(vertex1);
					vertex2u = vertexMap00.get(vertex2);
					auxEdge = addEdge(vertex1u, vertex2u);
					auxEdgeMap.put(auxEdge, edge);
				}
			}
		}
	}
}
