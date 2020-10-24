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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;

@BugPattern(
    name = "ModifyCollectionInForEachMethod",
    summary =
        "Modifying a collection while iterating over it in forEach method may cause a"
            + " ConcurrentModificationException to be thrown or lead to undefined behavior.",
    severity = WARNING)
public class ModifyCollectionInForEachMethod extends BugChecker
    implements MemberReferenceTreeMatcher, MethodInvocationTreeMatcher {

  private static final ImmutableList<String> STATE_MUTATION_METHOD_NAMES =
      ImmutableList.of("add", "addAll", "clear", "remove", "removeAll", "retainAll");

  private static final Matcher<ExpressionTree> MUTATION_METHOD_MATCHER =
      MethodMatchers.instanceMethod()
          .onDescendantOf("java.util.Collection")
          .namedAnyOf(STATE_MUTATION_METHOD_NAMES);

  private static final Matcher<ExpressionTree> FOR_EACH_METHOD_MATCHER =
      instanceMethod()
          .onDescendantOf("java.util.Collection")
          .named("forEach");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MUTATION_METHOD_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    if (state.getTypes().closure(ASTHelpers.getSymbol(tree).enclClass().asType()).stream()
        .anyMatch(
            s ->
                s.asElement()
                    .packge()
                    .getQualifiedName()
                    .toString()
                    .startsWith("java.util.concurrent"))) {
      return NO_MATCH;
    }

    // The enclosing method invocation of the method reference doesn't dereferenced an expression.
    // e.g. calling other methods defined in the same class.
    ExpressionTree mutatedReceiver = ASTHelpers.getReceiver(tree);
    if (mutatedReceiver == null) {
      return Description.NO_MATCH;
    }

    TreePath pathToLambdaExpression = state.findPathToEnclosing(LambdaExpressionTree.class);
    // Case for a method reference not enclosed in a lambda expression,
    // e.g. BiConsumer<ArrayList, Integer> biConsumer = ArrayList::add;
    if (pathToLambdaExpression == null) {
      return Description.NO_MATCH;
    }

    // Starting from the immediate enclosing method invocation of the lambda expression.
    Tree parentNode = pathToLambdaExpression.getParentPath().getLeaf();
    if (!(parentNode instanceof ExpressionTree) || !FOR_EACH_METHOD_MATCHER
        .matches((ExpressionTree) parentNode, state)) {
      return Description.NO_MATCH;
    }

    return isSameExpression(ASTHelpers.getReceiver((ExpressionTree) parentNode), mutatedReceiver)
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (!isSubtypeOf("java.util.Collection").matches(tree.getQualifierExpression(), state)
        || STATE_MUTATION_METHOD_NAMES.stream()
        .noneMatch(methodName -> methodName.contentEquals(tree.getName()))) {
      return Description.NO_MATCH;
    }

    if (state.getTypes().closure(ASTHelpers.getSymbol(tree).enclClass().asType()).stream()
        .anyMatch(
            s ->
                s.asElement()
                    .packge()
                    .getQualifiedName()
                    .toString()
                    .startsWith("java.util.concurrent"))) {
      return NO_MATCH;
    }

    ExpressionTree expressionTree = state.findEnclosing(MethodInvocationTree.class);
    if (!FOR_EACH_METHOD_MATCHER.matches(expressionTree, state)) {
      return Description.NO_MATCH;
    }

    return isSameExpression(ASTHelpers.getReceiver(expressionTree), ASTHelpers.getReceiver(tree))
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }

  private static boolean isSameExpression(ExpressionTree leftTree, ExpressionTree rightTree) {
    // The left tree and right tree must have the same symbol resolution.
    // This ensures the symbol kind on field, parameter or local var.
    if (ASTHelpers.getSymbol(leftTree) != ASTHelpers.getSymbol(rightTree)) {
      return false;
    }

    String leftTreeTextRepr = stripPrefixIfPresent(leftTree.toString(), "this.");
    String rightTreeTextRepr = stripPrefixIfPresent(rightTree.toString(), "this.");
    return leftTreeTextRepr.contentEquals(rightTreeTextRepr);
  }

  private static String stripPrefixIfPresent(String originalText, String prefix) {
    return originalText.startsWith(prefix) ? originalText.substring(prefix.length()) : originalText;
  }
}