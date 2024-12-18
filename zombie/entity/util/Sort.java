package zombie.entity.util;

import java.util.Comparator;

public class Sort {
   private static Sort instance;
   private TimSort timSort;
   private ComparableTimSort comparableTimSort;

   public Sort() {
   }

   public <T extends Comparable> void sort(Array<T> var1) {
      if (this.comparableTimSort == null) {
         this.comparableTimSort = new ComparableTimSort();
      }

      this.comparableTimSort.doSort(var1.items, 0, var1.size);
   }

   public void sort(Object[] var1) {
      if (this.comparableTimSort == null) {
         this.comparableTimSort = new ComparableTimSort();
      }

      this.comparableTimSort.doSort(var1, 0, var1.length);
   }

   public void sort(Object[] var1, int var2, int var3) {
      if (this.comparableTimSort == null) {
         this.comparableTimSort = new ComparableTimSort();
      }

      this.comparableTimSort.doSort(var1, var2, var3);
   }

   public <T> void sort(Array<T> var1, Comparator<? super T> var2) {
      if (this.timSort == null) {
         this.timSort = new TimSort();
      }

      this.timSort.doSort(var1.items, var2, 0, var1.size);
   }

   public <T> void sort(T[] var1, Comparator<? super T> var2) {
      if (this.timSort == null) {
         this.timSort = new TimSort();
      }

      this.timSort.doSort(var1, var2, 0, var1.length);
   }

   public <T> void sort(T[] var1, Comparator<? super T> var2, int var3, int var4) {
      if (this.timSort == null) {
         this.timSort = new TimSort();
      }

      this.timSort.doSort(var1, var2, var3, var4);
   }

   public static Sort instance() {
      if (instance == null) {
         instance = new Sort();
      }

      return instance;
   }
}
