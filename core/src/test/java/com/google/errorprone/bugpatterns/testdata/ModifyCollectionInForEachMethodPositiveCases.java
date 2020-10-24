package com.google.errorprone.bugpatterns.testdata;

import java.util.ArrayList;

public class ModifyCollectionInForEachMethodPositiveCases {

  public static void main(String[] args) {
    ArrayList<String> list = new ArrayList<>(), result = new ArrayList<>();
    list.add("test");

    // BUG: Diagnostic contains:
    list.forEach(list::remove);
    list.forEach(s -> {
      if (!s.isEmpty()) {
        // BUG: Diagnostic contains:
        list.add(s);
      }
    });
  }
}
