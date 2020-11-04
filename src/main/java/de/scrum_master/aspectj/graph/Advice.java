package de.scrum_master.aspectj.graph;

import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.List;

import static de.scrum_master.aspectj.graph.Advice.PrecedenceRule.ASPECTJ_CLASSIC;
import static de.scrum_master.aspectj.graph.Advice.Type.AFTER;
import static de.scrum_master.aspectj.graph.Advice.Type.BEFORE;

public class Advice {

  public enum Type {
    BEFORE, AFTER, AROUND
  }

  public enum PrecedenceRule {
    /**
     * Classical AspectJ precedence rules: Chronological order within the aspect source code applies, i.e. the advice
     * defined above has precedence over the one defined below. But if any of the two compared advices are of AFTER
     * type, the one defined below has precedence over the former.
     * <p></p>
     * Attention: This can lead to cycles in the precedence graph!
     */
    ASPECTJ_CLASSIC,
    /**
     * When comparing a BEFORE to an AFTER advice, the BEFORE advice always has preference, no matter if it is defined
     * above or below. Otherwise, classical AspectJ precedence rules apply.
     * <p></p>
     * Attention: This can lead to cycles in the precedence graph! While it can help avoid cycles with the classical
     * rules sometimes, at other times it creates cycles where the classical rules do not.
     */
    ASPECTJ_WITH_BEFORE_ALWAYS_PRECEDING_AFTER,
    /**
     * Chronological order within the aspect source code applies, i.e. the advice defined above has precedence over the
     * one defined below. No exceptions are made in any case, in contrast to the classical AspectJ rules
     * <p></p>
     * Note: This rule type always yields a clear, linear precedence chain and never leads to cycles in the precedence
     * graph. The behaviour for AFTER advices also do not require "thinking around the corner" anymore because they are
     * consistent with the behaviour of post-proceed code in AROUND advices.
     */
    CHRONOLOGICAL
  }

  public static class Edge extends DefaultEdge {
    @Override
    public String toString() {
      return "";
    }

    @Override
    public Advice getSource() {
      return (Advice) super.getSource();
    }

    @Override
    public Advice getTarget() {
      return (Advice) super.getTarget();
    }
  }

  public final int index;
  public final Type type;

  public Advice(int index, Type type) {
    this.index = index;
    this.type = type;
  }

  public boolean hasPrecedenceOver(Advice advice, PrecedenceRule precedenceRule) {
    if (equals(advice))
      throw new IllegalArgumentException("cannot compare to self");

    switch (precedenceRule) {
      case ASPECTJ_CLASSIC:
        return type == AFTER || advice.type == AFTER
          ? index > advice.index
          : index < advice.index;
      case ASPECTJ_WITH_BEFORE_ALWAYS_PRECEDING_AFTER:
        if (type == BEFORE && advice.type == AFTER)
          return index < advice.index;
        if (type == AFTER && advice.type == BEFORE)
          return index > advice.index;
        return hasPrecedenceOver(advice, ASPECTJ_CLASSIC);
      case CHRONOLOGICAL:
        return index < advice.index;
      default:
        throw new RuntimeException("unknown precedence rule");
    }
  }

  public static List<Advice> createList(Type... adviceTypes) {
    List<Advice> advices = new ArrayList<>();
    int index = 1;
    for (Type adviceType : adviceTypes)
      advices.add(new Advice(index++, adviceType));
    return advices;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Advice advice = (Advice) o;

    if (index != advice.index)
      return false;
    return type == advice.type;
  }

  @Override
  public int hashCode() {
    int result = index;
    result = 31 * result + type.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return type.toString().toLowerCase() + "-" + index;
//    return type.name().substring(0,2) + index;
  }

}
