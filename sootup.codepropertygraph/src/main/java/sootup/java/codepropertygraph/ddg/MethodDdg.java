package sootup.java.codepropertygraph.ddg;

import sootup.core.graph.StmtGraph;
import sootup.core.model.Body;
import sootup.core.model.SootMethod;

public class MethodDdg {
  private final String name;
  private final StmtGraph<?> stmtGraph;

  private final Body body;

  public MethodDdg(SootMethod sootMethod) {
    this.name = sootMethod.getName();
    this.stmtGraph = sootMethod.getBody().getStmtGraph();
    this.body = sootMethod.getBody();
  }

  public String getName() {
    return name;
  }

  public StmtGraph<?> getStmtGraph() {
    return stmtGraph;
  }

  public Body getBody() {
    return body;
  }
}
