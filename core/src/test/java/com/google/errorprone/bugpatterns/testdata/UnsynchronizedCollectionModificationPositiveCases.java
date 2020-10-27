package com.google.errorprone.bugpatterns.testdata;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UnsynchronizedCollectionModificationPositiveCases {

  public static void main(String[] args) throws InvocationTargetException, InterruptedException {
    ArrayList<String> list = new ArrayList<>(), unsync_result = new ArrayList<>();
    list.add("test");
    list.forEach(unsync_result::add);
    // BUG: Diagnostic contains:
    list.parallelStream().forEach(unsync_result::add);

    list.parallelStream().forEach(s -> {
      if (!s.isEmpty()) {
        // BUG: Diagnostic contains:
        unsync_result.add(s);
      }
    });

    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<Future<String>> futureList = new ArrayList<>();
    for (int i = 0; i < 100; ++i) {
      Future<String> future = executor
          .submit(() -> {
            // BUG: Diagnostic contains:
            unsync_result.add(Thread.currentThread().getName());
            return Thread.currentThread().getName();
          });
      futureList.add(future);
    }

    for (Future<String> stringFuture : futureList) {
      try {
        System.out.println(stringFuture.get());
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    executor.shutdown();

    // BUG: Diagnostic contains:
    EventQueue.invokeAndWait(() -> unsync_result.add("test"));

    Thread t1 = new Thread(() -> {
      // BUG: Diagnostic contains:
      unsync_result.add("test");
    });

    t1.start();

    try {
      t1.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
