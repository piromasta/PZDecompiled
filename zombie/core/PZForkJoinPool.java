package zombie.core;

import java.util.concurrent.ForkJoinPool;

public final class PZForkJoinPool {
   private static ForkJoinPool common;

   public PZForkJoinPool() {
   }

   public static ForkJoinPool commonPool() {
      int var0 = Runtime.getRuntime().availableProcessors() - 1;
      if (common == null) {
         common = new ForkJoinPool(var0);
      }

      return common;
   }
}
