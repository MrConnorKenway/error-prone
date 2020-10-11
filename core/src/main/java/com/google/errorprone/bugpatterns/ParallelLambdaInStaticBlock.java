/*
 * Copyright 2020 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import javax.lang.model.element.Modifier;


@BugPattern(
    name = "ParallelLambdaInStaticBlock",
    summary = "Using lambda expression in class static block in parallel may cause deadlock.",
    severity = WARNING)
public class ParallelLambdaInStaticBlock extends BugChecker implements
    LambdaExpressionTreeMatcher {

  private static final Matcher<ExpressionTree> PARALLEL_STREAM_MATCHER =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.util.stream.BaseStream")
              .named("parallel"),
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .named("parallelStream")
      );

  private static final Matcher<ExpressionTree> THREAD_CREATION_MATCHER =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.util.concurrent.ExecutorService")
              .named("submit"),
          constructor()
              .forClass("java.lang.Thread"),
          staticMethod()
              .onClass("java.awt.EventQueue")
              .namedAnyOf("invokeLater", "invokeAndWait")
      );

  @Override
  public Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state) {
    boolean isInStaticBlock = false;
    for (Tree parent : state.getPath()) {
      if (parent instanceof BlockTree && ((BlockTree) parent).isStatic()) {
        isInStaticBlock = true;
        break;
      }
    }

    if (!isInStaticBlock) {
      VariableTree variableTree = state.findEnclosing(VariableTree.class);
      if (variableTree == null
          || !variableTree.getModifiers().getFlags().contains(Modifier.STATIC)) {
        return Description.NO_MATCH;
      }
    }

    ExpressionTree expressionTree = state.findEnclosing(MethodInvocationTree.class);
    while (expressionTree instanceof MethodInvocationTree) {
      if (PARALLEL_STREAM_MATCHER.matches(expressionTree, state)
          || THREAD_CREATION_MATCHER.matches(expressionTree, state)) {
        return describeMatch(tree);
      }
      expressionTree = ASTHelpers.getReceiver(expressionTree);
    }

    NewClassTree newClassTree = state.findEnclosing(NewClassTree.class);
    if (newClassTree != null && THREAD_CREATION_MATCHER.matches(newClassTree, state)) {
      return describeMatch(tree);
    }

    return Description.NO_MATCH;
  }
}
