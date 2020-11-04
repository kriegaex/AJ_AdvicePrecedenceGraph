package de.scrum_master.aspectj.graph;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.swing.mxGraphComponent;
import org.jgrapht.Graph;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.cycle.DirectedSimpleCycles;
import org.jgrapht.alg.cycle.TiernanSimpleCycles;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultListenableGraph;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static de.scrum_master.aspectj.graph.Advice.Type.*;

public class AspectJAdvicePrecedenceSimulator extends JFrame {
  private List<Advice> advices;
  private Graph<Advice, Advice.Edge> graph;
  private Advice.PrecedenceRule precedenceRule;

  public static void main(String[] args) {
    boolean visualiseGraph = true;
    List<List<Advice>> demoAspects = Arrays.asList(
      Advice.createList(AFTER, AROUND, AROUND, BEFORE, BEFORE, AFTER),
      Advice.createList(AROUND, AFTER, AROUND, BEFORE, BEFORE, AFTER),
      Advice.createList(BEFORE, AFTER, BEFORE)
    );

    for (Advice.PrecedenceRule precedenceRule : Advice.PrecedenceRule.values()) {
      for (List<Advice> advices : demoAspects) {
        new AspectJAdvicePrecedenceSimulator(precedenceRule, advices, visualiseGraph);
        System.out.println(repeatString("-", 80));
      }
    }
  }

  public AspectJAdvicePrecedenceSimulator(Advice.PrecedenceRule precedenceRule, List<Advice> adviceList, boolean visualiseGraph) throws HeadlessException {
    super(precedenceRule + " | " + adviceList);
    this.precedenceRule = precedenceRule;
    advices = adviceList;
    graph = new DefaultDirectedGraph<>(Advice.Edge.class);

    System.out.println("Advice precedence mode = " + precedenceRule);
    populateGraph();
    if (!cyclesDetected()) {
      System.out.println("No cycles detected -> performing transitive reduction");
      doTransitiveReduction();
      System.out.println();
      simulateAdviceExecution(getHighestPrecedenceAdvice(), 0);
    }
    if (visualiseGraph)
      drawGraph();
  }

  private void populateGraph() {
    advices.forEach(graph::addVertex);
    advices.forEach(firstAdvice -> advices.stream()
      .filter(secondAdvice -> secondAdvice.index > firstAdvice.index)
      .forEach(secondAdvice -> {
        if (firstAdvice.hasPrecedenceOver(secondAdvice, precedenceRule))
          graph.addEdge(firstAdvice, secondAdvice);
        else
          graph.addEdge(secondAdvice, firstAdvice);
      }));
    System.out.println("Original graph = " + graph);
  }

  private boolean cyclesDetected() {
    CycleDetector<Advice, Advice.Edge> cycleDetector = new CycleDetector<>(graph);
    if (!cycleDetector.detectCycles())
      return false;
    // TODO: After https://github.com/jgrapht/jgrapht/issues/667 is fixed, also perform transitive reduction for cyclic graphs
    System.out.println("Cycles detected -> cannot perform transitive reduction due to JGraphT bug");
    System.out.println("Vertices causing cycles = " + cycleDetector.findCycles());
    DirectedSimpleCycles<Advice, Advice.Edge> simpleCycles = new TiernanSimpleCycles<>(graph);
    System.out.println("List of simple cycles = " + simpleCycles.findSimpleCycles());
    return true;
  }

  private void doTransitiveReduction() {
    // Transitive reduction does not work with a ListenableGraph, throws NPE
    TransitiveReduction.INSTANCE.reduce(graph);
    assert advices.size() == graph.vertexSet().size();
    System.out.println("Reduced, cycle-free graph = " + graph);
  }

  private Advice getHighestPrecedenceAdvice() {
    return advices.stream()
      .filter(advice -> graph.inDegreeOf(advice) == 0)
      .findFirst()
      .orElseThrow(() -> new RuntimeException("no highest precedence advice found"));
  }

  private void simulateAdviceExecution(Advice advice, int depth) {
    final String indentation = repeatString("· ", depth);
    if (advice == null) {
      System.out.println(indentation + "JOINPOINT");
      return;
    }

    final String logPrefix = indentation + advice + " → ";
    String logPreMessage = logPrefix;
    String logPostMessage = logPrefix;
    switch (advice.type) {
      case BEFORE:
        logPreMessage += "pre-action";
        break;
      case AFTER:
        logPostMessage += "post-action";
        break;
      case AROUND:
        logPreMessage += "pre-action (can change arguments)";
        logPostMessage += "post-action (can change return value)";
        break;
    }

    Advice nextAdvice = graph.outgoingEdgesOf(advice).stream()
      .findFirst()
      .map(Advice.Edge::getTarget)
      .orElse(null);

    System.out.println(logPreMessage);
    simulateAdviceExecution(nextAdvice, ++depth);
    System.out.println(logPostMessage);
  }

  /**
   * In Java 11+, you can use String.repeat instead
   */
  private static String repeatString(String string, int times) {
    StringBuilder stringBuilder = new StringBuilder(string.length() * times);
    for (int i = 0; i < times; i++)
      stringBuilder.append(string);
    return stringBuilder.toString();
  }

  private void drawGraph() {
    JGraphXAdapter<Advice, Advice.Edge> jgxAdapter = new JGraphXAdapter<>(new DefaultListenableGraph<>(graph));

    mxGraphComponent component = new mxGraphComponent(jgxAdapter);
    component.setConnectable(false);
    component.getGraph().setAllowDanglingEdges(false);
    getContentPane().add(component);

    mxCircleLayout layout = new mxCircleLayout(jgxAdapter);
    layout.setResetEdges(true);
    int radius = 100;
    layout.setRadius(radius);
    layout.setMoveCircle(true);
    layout.execute(jgxAdapter.getDefaultParent());

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    pack();
    setVisible(true);
  }

}
