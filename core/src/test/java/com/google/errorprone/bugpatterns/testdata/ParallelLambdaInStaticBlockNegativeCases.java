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

package com.google.errorprone.bugpatterns.testdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class ParallelLambdaInStaticBlockNegativeCases {

  static class TestFutureUsingAnonymousClass {

    static {
      ExecutorService executor = Executors.newFixedThreadPool(10);
      List<Future<String>> list = new ArrayList<>();
      for (int i = 0; i < 100; ++i) {
        Future<String> future = executor
            .submit(new Callable<String>() {
              @Override
              public String call() throws Exception {
                return Thread.currentThread().getName();
              }
            });
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


  static class TestThreadUsingAnonymousClass {

    private static int state = 10;

    public static void main(String... strings) {
      Thread t1 = new Thread(new Runnable() {
        @Override
        public void run() {
          state = 11;
          System.out.println("Exit Thread");
        }
      });

      t1.start();

      try {
        t1.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      System.out.println("exiting static block");
      System.out.println(state);
    }
  }

  static class TestCollectionStream {

    static {
      Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).stream()
          .forEachOrdered(s -> System.out.println(s));
    }

    public static final void main(String[] args) {
    }
  }

  static class TestNonParallelAPI {

    static {
      long k = IntStream.range(0, 100).map(s -> s).count();
      System.out.println(k);
      System.out.println("done");
    }

    public static void main(final String[] args) {
    }
  }

  static class TestParallelStreamUsingMemberReference {

    static {
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

      map.keySet().parallelStream().forEach(System.out::println);
    }

    public static void main(String[] args) {
      new TestParallelStreamUsingMemberReference();
    }
  }

}
