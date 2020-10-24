package com.google.errorprone.bugpatterns.testdata;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModifyCollectionInForEachMethodNegativeCases {

  public static void main(String[] args) {
    ArrayList<String> list = new ArrayList<>(), result = new ArrayList<>();
    list.add("test");

    list.parallelStream().forEach(s -> {
      if (!s.isEmpty()) {
        result.add(s);
      }
    });

    CopyOnWriteArrayList<String> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
    copyOnWriteArrayList.add("test concurrent");
    copyOnWriteArrayList.forEach(copyOnWriteArrayList::add);
  }
}
