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

public class UnsynchronizedCollectionModificationNegativeCases {

  public static void main(String[] args) throws InvocationTargetException, InterruptedException {
    ArrayList<String> list = new ArrayList<>(), unsync_result = new ArrayList<>();
    CopyOnWriteArrayList<String> sync_result = new CopyOnWriteArrayList<>();
    list.add("test");
    list.forEach(unsync_result::add);
    list.parallelStream().forEach(sync_result::add);
    list.forEach(s -> {
      if (!s.isEmpty()) {
        list.add(s);
      }
    });

    list.parallelStream().forEach(s -> {
      synchronized (unsync_result) {
        unsync_result.add(s);
      }
    });

    list.parallelStream().forEach(s -> {
      if (!s.isEmpty()) {
        sync_result.add(s);
      }
    });

    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<Future<String>> futureList = new ArrayList<>();
    for (Future<String> stringFuture : futureList) {
      try {
        System.out.println(stringFuture.get());
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    executor.shutdown();

    EventQueue.invokeAndWait(() -> sync_result.add("test"));

    Thread t1 = new Thread(() -> {
      sync_result.add("test");
    });

    t1.start();

    try {
      t1.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
