package de.scrum_master.aspectj.graph;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.swing.mxGraphComponent;
import org.jgrapht.Graph;
import org.jgrapht.alg.CyclicTransitiveReduction;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.nio.csv.CSVExporter;
import org.jgrapht.nio.csv.CSVFormat;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static de.scrum_master.aspectj.graph.Advice.Type.*;

public class AspectJAdvicePrecedenceSimulator extends JFrame {
  private List<Advice> advices;
  private Graph<Advice, Advice.Edge> graph;
  private Advice.PrecedenceRule precedenceRule;

  public static void main(String[] args) throws IOException {
    boolean exportGraph = false;
    boolean visualiseGraph = true;
    boolean exitOnFirstWindowClose = true;
    List<List<Advice>> demoAspects = Arrays.asList(
      Advice.createList(AFTER, AROUND, AROUND, BEFORE, BEFORE, AFTER),
      Advice.createList(AROUND, AFTER, AROUND, BEFORE, BEFORE, AFTER),
      Advice.createList(BEFORE, AFTER, BEFORE)
    );

    for (Advice.PrecedenceRule precedenceRule : Advice.PrecedenceRule.values()) {
      for (List<Advice> advices : demoAspects) {
        new AspectJAdvicePrecedenceSimulator(precedenceRule, advices, exportGraph, visualiseGraph, exitOnFirstWindowClose);
        System.out.println(repeatString("-", 80));
      }
    }
  }

  public AspectJAdvicePrecedenceSimulator(
    Advice.PrecedenceRule precedenceRule,
    List<Advice> adviceList,
    boolean exportGraph,
    boolean visualiseGraph,
    boolean exitOnFirstWindowClose
  ) throws HeadlessException, IOException
  {
    super(precedenceRule + " | " + adviceList);
    this.precedenceRule = precedenceRule;
    advices = adviceList;
    graph = new DefaultDirectedGraph<>(Advice.Edge.class);

    System.out.println("Advice precedence mode = " + precedenceRule);
    populateGraph();
    if (exportGraph)
      exportGraphToFile(precedenceRule.name() + "-" + Instant.now().toEpochMilli() + ".csv");
    CyclicTransitiveReduction.INSTANCE.reduce(graph);
    System.out.println("Transitively reduced graph = " + graph);
    if (!new CycleDetector<>(graph).detectCycles())
      simulateAdviceExecution(getHighestPrecedenceAdvice(), 0);
    else
      System.out.println("Precedence graph contains cycles, cannot simulate advice execution");
    if (visualiseGraph)
      drawGraph(exitOnFirstWindowClose);
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

  private void exportGraphToFile(String fileName) throws IOException {
    System.out.println("Exporting graph to file " + fileName);
    CSVExporter<Advice, Advice.Edge> csvExporter = new CSVExporter<>(Advice::toString, CSVFormat.ADJACENCY_LIST, ',');
    csvExporter.exportGraph(graph, new FileWriter(fileName));
  }

  private Advice getHighestPrecedenceAdvice() {
    return advices.stream()
      .filter(advice -> graph.inDegreeOf(advice) == 0)
      .findFirst()
      .orElseThrow(() -> new RuntimeException("no highest precedence advice found"));
  }

  private void simulateAdviceExecution(Advice advice, int depth) {
    if (depth == 0)
      System.out.println();
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

  private void drawGraph(boolean exitOnFirstWindowClose) {
    JGraphXAdapter<Advice, Advice.Edge> jGraphXAdapter = new JGraphXAdapter<>(graph);
    getContentPane().add(new mxGraphComponent(jGraphXAdapter));
    new mxCircleLayout(jGraphXAdapter).execute(jGraphXAdapter.getDefaultParent());
    setDefaultCloseOperation(exitOnFirstWindowClose ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
    pack();
    setVisible(true);
  }

}
