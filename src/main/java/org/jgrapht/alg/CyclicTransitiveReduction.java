package org.jgrapht.alg;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.cycle.TiernanSimpleCycles;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultEdge;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implements a <a href="https://en.wikipedia.org/wiki/Transitive_reduction">transitive reduction algorithm</a>
 * algorithm which also works correctly for directed graphs containing cycles because {@link TransitiveReduction} fails
 * in this case.
 * <p></p>
 * <b>Please note:</b>
 * <ol>
 *   <li>
 *     This is a home-brew algorithm, not implemented from scratch and not optimised for performance and/or memory
 *     efficiency as it should be. Instead, it utilises existing JGraphT functionality such as:
 *     <ul>
 *       <li>transitive reduction for DAGs (directed acyclical graphs)</li>
 *       <li>cycle detection</li>
 *       <li>strong connectivity inspector incl. graph condensation</li>
 *     </ul>
 *     In addition to that the algorithm performs edge pruning both within and between SSCs (strongly connected
 *     components) as well as condensed graph expansion (basically the inverse operation) back into the original graph.
 *   </li>
 *   <li>
 *     While the resulting graph has the same vertex reachability as the original one, there are no guarantees that
 *     existing edges will be retained. E.g. an edge connecting two SCCs could be between two random vertices in both
 *     SCCs, not necessarily a previously existing edge. This does not affect reachability and was a choice deliberately
 *     made in order to avoid further performance penalties due do checks for existing edges.
 *   </li>
 * </ol>
 *
 * @author Alexander Kriegisch
 */
public class CyclicTransitiveReduction {
  public static final CyclicTransitiveReduction INSTANCE = new CyclicTransitiveReduction();

  private CyclicTransitiveReduction() {}

  /**
   * Removes all transitive edges from the directed graph passed as input parameter, e.g.
   *
   * <pre>
   *  DirectedGraph directedGraphToBePruned;
   *  CyclicTransitiveReduction.INSTANCE.reduce(directedGraphToBePruned);
   * </pre>
   *
   * @param directedGraph the directed graph that will be reduced transitively
   * @param <V>           the graph vertex type
   * @param <E>           the graph edge type
   */
  public <V, E> void reduce(final Graph<V, E> directedGraph) {
    GraphTests.requireDirected(directedGraph, "Graph must be directed");
    /*
    // Shortcut for cycle-free digraph (DAG) - optional, i.e. can be omitted but might speed up performance for DAG
    if (!new CycleDetector<>(directedGraph).detectCycles()) {
      TransitiveReduction.INSTANCE.reduce(directedGraph);
      return;
    }
    */
    Graph<Graph<V, E>, DefaultEdge> condensedGraph = condenseGraph(directedGraph);
    pruneCondensedGraph(condensedGraph);
    expandCondensedGraph(condensedGraph, directedGraph);
  }

  /**
   * Condense a directed, possibly cyclical graph by {@link StrongConnectivityAlgorithm#getCondensation()}.
   */
  protected <V, E> Graph<Graph<V, E>, DefaultEdge> condenseGraph(Graph<V, E> directedGraph) {
    StrongConnectivityAlgorithm<V, E> scAlgorithm = new KosarajuStrongConnectivityInspector<>(directedGraph);
//    List<Set<V>> components = scAlgorithm.stronglyConnectedSets();
//    System.out.println("Strongly connected vertex sets = " + components);
    Graph<Graph<V, E>, DefaultEdge> condensedGraph = scAlgorithm.getCondensation();
//    System.out.println("Condensed graph = " + condensedGraph);
    return condensedGraph;
  }

  /**
   * Transitively reduce the given condensed graph derived from an original directed graph by
   * {@link #condenseGraph(Graph)} and replace each strongly connected component by a simple circle connecting all its
   * respective vertices.
   */
  protected <V, E> void pruneCondensedGraph(Graph<Graph<V, E>, DefaultEdge> condensedGraph) {
    TransitiveReduction.INSTANCE.reduce(condensedGraph);
//    System.out.println("Condensed graph after transitive reduction = " + condensedGraph);

    for (Graph<V, E> scComponent : condensedGraph.vertexSet()) {
      Set<E> sccEdges = scComponent.edgeSet();
      if (sccEdges.size() < 2)
        continue;
//      System.out.println("SCC = " + scComponent);
      List<V> cycle = new TiernanSimpleCycles<V, E>(scComponent)
        .findSimpleCycles()
        .stream()
        .max(Comparator.comparingInt(List::size))
        .orElseThrow(() -> new RuntimeException("longest cycle not found (should never happen)"));
      Set<E> sccEdgesCopy = new HashSet<>(sccEdges);

      for (E edge : sccEdgesCopy) {
        int sourceIndex = cycle.indexOf(scComponent.getEdgeSource(edge));
        assert sourceIndex >= 0;
        int targetIndex = cycle.indexOf(scComponent.getEdgeTarget(edge));
        assert targetIndex >= 0;
        int indexDelta = Math.abs(sourceIndex - targetIndex);
        if (indexDelta != 1 && indexDelta + 1 != cycle.size())
          scComponent.removeEdge(edge);
      }

      scComponent.removeAllEdges(sccEdgesCopy);
      for (int i = 0; i < cycle.size(); i++)
        scComponent.addEdge(cycle.get(i), cycle.get(i + 1 == cycle.size() ? 0 : i + 1));
    }
//    System.out.println("Condensed, transitively reduced graph after SCC pruning = " + condensedGraph);
  }

  /**
   * Re-expand a condensed graph previously created by {@link #condenseGraph(Graph)} after possible clean-ups by
   * {@link #pruneCondensedGraph(Graph)}) back into the original directed graph, also cleaning up  redundant edges
   * between strongly connected components during the process.
   */
  protected <V, E> void expandCondensedGraph(Graph<Graph<V, E>, DefaultEdge> condensedGraph, Graph<V, E> directedGraph) {
    directedGraph.removeAllEdges(new HashSet<E>(directedGraph.edgeSet()));
    condensedGraph.edgeSet().forEach(edge ->
      directedGraph.addEdge(
        condensedGraph.getEdgeSource(edge).vertexSet().iterator().next(),
        condensedGraph.getEdgeTarget(edge).vertexSet().iterator().next()
      ));
    condensedGraph.vertexSet().forEach(scComponent ->
      scComponent.edgeSet().forEach(edge ->
        directedGraph.addEdge(scComponent.getEdgeSource(edge), scComponent.getEdgeTarget(edge))
      )
    );
//    System.out.println("Expanded graph = " + directedGraph);
  }

}
