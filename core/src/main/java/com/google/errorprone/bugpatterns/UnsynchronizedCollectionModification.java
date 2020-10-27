package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

@BugPattern(
    name = "UnsynchronizedCollectionModification",
    summary =
        "Modifying a non-concurrent collection in parallel may lead to undefined behavior.",
    severity = WARNING)
public class UnsynchronizedCollectionModification extends BugChecker
    implements MemberReferenceTreeMatcher, MethodInvocationTreeMatcher {

  private static final ImmutableList<String> STATE_MUTATION_METHOD_NAMES =
      ImmutableList.of("add", "addAll", "clear", "remove", "removeAll", "retainAll");

  private static final Matcher<ExpressionTree> STREAM_API_INVOCATION_MATCHER =
      instanceMethod().onDescendantOfAny("java.util.stream.BaseStream");

  private static final Matcher<ExpressionTree> COLLECTION_TO_STREAM_MATCHER =
      instanceMethod()
          .onDescendantOf("java.util.Collection")
          .named("parallelStream");

  private static final Matcher<ExpressionTree> MUTATION_METHOD_MATCHER =
      instanceMethod()
          .onDescendantOf("java.util.Collection")
          .namedAnyOf(STATE_MUTATION_METHOD_NAMES);

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
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (!isSubtypeOf("java.util.Collection").matches(tree.getQualifierExpression(), state)
        || STATE_MUTATION_METHOD_NAMES.stream()
        .noneMatch(methodName -> methodName.contentEquals(tree.getName()))) {
      return NO_MATCH;
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
    while (STREAM_API_INVOCATION_MATCHER.matches(expressionTree, state)) {
      expressionTree = ASTHelpers.getReceiver(expressionTree);
    }

    return COLLECTION_TO_STREAM_MATCHER.matches(expressionTree, state) ? describeMatch(tree)
        : NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MUTATION_METHOD_MATCHER.matches(tree, state)) {
      return NO_MATCH;
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

    SynchronizedTree synchronizedTree = state.findEnclosing(SynchronizedTree.class);
    if (synchronizedTree != null) {
      return NO_MATCH;
    }

    TreePath pathToLambdaExpression = state.findPathToEnclosing(LambdaExpressionTree.class);
    // Case for a method reference not enclosed in a lambda expression,
    // e.g. BiConsumer<ArrayList, Integer> biConsumer = ArrayList::add;
    if (pathToLambdaExpression == null) {
      return NO_MATCH;
    }

    Tree parentNode = pathToLambdaExpression.getParentPath().getLeaf();
    if (!(parentNode instanceof ExpressionTree)) {
      return NO_MATCH;
    }

    NewClassTree newClassTree = state.findEnclosing(NewClassTree.class);
    if (newClassTree != null && THREAD_CREATION_MATCHER.matches(newClassTree, state)) {
      return describeMatch(tree);
    }

    ExpressionTree expressionTree = (ExpressionTree) parentNode;
    while (STREAM_API_INVOCATION_MATCHER.matches(expressionTree, state)) {
      if (THREAD_CREATION_MATCHER.matches(expressionTree, state)) {
        return describeMatch(tree);
      }
      expressionTree = ASTHelpers.getReceiver(expressionTree);
    }
    if (THREAD_CREATION_MATCHER.matches(expressionTree, state)) {
      return describeMatch(tree);
    }

    return COLLECTION_TO_STREAM_MATCHER.matches(expressionTree, state) ? describeMatch(tree)
        : NO_MATCH;
  }
}
