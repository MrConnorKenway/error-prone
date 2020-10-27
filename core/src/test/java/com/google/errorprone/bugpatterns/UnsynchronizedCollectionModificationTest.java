package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnsynchronizedCollectionModificationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnsynchronizedCollectionModification.class, getClass());

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("UnsynchronizedCollectionModificationPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("UnsynchronizedCollectionModificationNegativeCases.java").doTest();
  }
}
