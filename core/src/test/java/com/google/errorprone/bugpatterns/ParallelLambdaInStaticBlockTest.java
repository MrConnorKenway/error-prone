package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ParallelLambdaInStaticBlockTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ParallelLambdaInStaticBlock.class, getClass());

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("ParallelLambdaInStaticBlockPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper.addSourceFile("ParallelLambdaInStaticBlockNegativeCases.java").doTest();
  }
}
