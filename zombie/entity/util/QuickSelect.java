package zombie.entity.util;

import java.util.Comparator;

public class QuickSelect<T> {
   private T[] array;
   private Comparator<? super T> comp;

   public QuickSelect() {
   }

   public int select(T[] var1, Comparator<T> var2, int var3, int var4) {
      this.array = var1;
      this.comp = var2;
      return this.recursiveSelect(0, var4 - 1, var3);
   }

   private int partition(int var1, int var2, int var3) {
      Object var4 = this.array[var3];
      this.swap(var2, var3);
      int var5 = var1;

      for(int var6 = var1; var6 < var2; ++var6) {
         if (this.comp.compare(this.array[var6], var4) < 0) {
            this.swap(var5, var6);
            ++var5;
         }
      }

      this.swap(var2, var5);
      return var5;
   }

   private int recursiveSelect(int var1, int var2, int var3) {
      if (var1 == var2) {
         return var1;
      } else {
         int var4 = this.medianOfThreePivot(var1, var2);
         int var5 = this.partition(var1, var2, var4);
         int var6 = var5 - var1 + 1;
         int var7;
         if (var6 == var3) {
            var7 = var5;
         } else if (var3 < var6) {
            var7 = this.recursiveSelect(var1, var5 - 1, var3);
         } else {
            var7 = this.recursiveSelect(var5 + 1, var2, var3 - var6);
         }

         return var7;
      }
   }

   private int medianOfThreePivot(int var1, int var2) {
      Object var3 = this.array[var1];
      int var4 = (var1 + var2) / 2;
      Object var5 = this.array[var4];
      Object var6 = this.array[var2];
      if (this.comp.compare(var3, var5) > 0) {
         if (this.comp.compare(var5, var6) > 0) {
            return var4;
         } else {
            return this.comp.compare(var3, var6) > 0 ? var2 : var1;
         }
      } else if (this.comp.compare(var3, var6) > 0) {
         return var1;
      } else {
         return this.comp.compare(var5, var6) > 0 ? var2 : var4;
      }
   }

   private void swap(int var1, int var2) {
      Object var3 = this.array[var1];
      this.array[var1] = this.array[var2];
      this.array[var2] = var3;
   }
}
