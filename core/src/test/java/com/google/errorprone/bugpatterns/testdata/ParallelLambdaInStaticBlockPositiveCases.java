package com.google.errorprone.bugpatterns.testdata;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class ParallelLambdaInStaticBlockPositiveCases {

  static class TestInvokeAndWait {

    static {
      try {
        System.out.println("initializer start");

        System.out.println("\nanonymous inner-class: Print.print");
        EventQueue.invokeLater(new Runnable() {
          @Override
          public void run() {
            Print.print();
          }
        });

        System.out.println("\nmethod ref: Print.print");
        EventQueue.invokeAndWait(Print::print);

        System.out.println("\nlambda: Print.print");
        // BUG: Diagnostic contains:
        EventQueue.invokeAndWait(() -> Print.print());

        System.out.println("\ninitializer end");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public static void main(String[] args) {
      new TestInvokeAndWait();
    }
  }

  static class Print {

    public static void print() {
      System.out.println("Print.print");
    }
  }

  static class TestFuture {

    static {
      ExecutorService executor = Executors.newFixedThreadPool(10);
      List<Future<String>> list = new ArrayList<>();
      for (int i = 0; i < 100; ++i) {
        Future<String> future = executor
            // BUG: Diagnostic contains:
            .submit(() -> Thread.currentThread().getName());
        list.add(future);
      }

      for (Future<String> stringFuture : list) {
        try {
          System.out.println(stringFuture.get());
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
      executor.shutdown();
    }

    public static void main(String[] args) {
    }
  }

  static class TestNonStaticBlock {

    // BUG: Diagnostic contains:
    static final int SUM = IntStream.range(0, 100).parallel().reduce((n, m) -> n + m).getAsInt();

    public static void main(String[] args) {
      System.out.println(SUM);
    }
  }

  static class TestThread {

    private static int state = 10;

    static {
      // BUG: Diagnostic contains:
      Thread t1 = new Thread(() -> {
        state = 11;
      });

      t1.start();

      try {
        t1.join();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      System.out.println("exiting static block");
    }

    public static void main(String... strings) {
      System.out.println(state);
    }
  }

  static class TestCollectionParallelStream {

    static {
      Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallelStream()
          // BUG: Diagnostic contains:
          .forEachOrdered(s -> System.out.println(s));

      Map<Integer, String> map = new HashMap<>();
      int i = 0;
      map.put(++i, "1");
      map.put(++i, "1");
      map.put(++i, "1");
      map.put(++i, "1");
      map.put(++i, "1");
      map.put(++i, "1");
      map.put(++i, "1");
      map.put(++i, "1");
      map.put(++i, "1");

      // BUG: Diagnostic contains:
      map.keySet().parallelStream().forEach(e -> {
        System.out.println(e);
      });
    }

    public static void main(String[] args) {
    }
  }

  static class TestDirectParallelStream {

    static {
      long k = IntStream.range(0, 100).parallel()
          // BUG: Diagnostic contains:
          .map(s -> s).count();
      System.out.println(k);
      System.out.println("done");
    }

    public static void main(final String[] args) {
    }
  }

}
