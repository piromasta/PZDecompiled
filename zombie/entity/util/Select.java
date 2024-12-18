package zombie.entity.util;

import java.util.Comparator;

public class Select {
   private static Select instance;
   private QuickSelect quickSelect;

   public Select() {
   }

   public static Select instance() {
      if (instance == null) {
         instance = new Select();
      }

      return instance;
   }

   public <T> T select(T[] var1, Comparator<T> var2, int var3, int var4) {
      int var5 = this.selectIndex(var1, var2, var3, var4);
      return var1[var5];
   }

   public <T> int selectIndex(T[] var1, Comparator<T> var2, int var3, int var4) {
      if (var4 < 1) {
         throw new RuntimeException("cannot select from empty array (size < 1)");
      } else if (var3 > var4) {
         throw new RuntimeException("Kth rank is larger than size. k: " + var3 + ", size: " + var4);
      } else {
         int var5;
         if (var3 == 1) {
            var5 = this.fastMin(var1, var2, var4);
         } else if (var3 == var4) {
            var5 = this.fastMax(var1, var2, var4);
         } else {
            if (this.quickSelect == null) {
               this.quickSelect = new QuickSelect();
            }

            var5 = this.quickSelect.select(var1, var2, var3, var4);
         }

         return var5;
      }
   }

   private <T> int fastMin(T[] var1, Comparator<T> var2, int var3) {
      int var4 = 0;

      for(int var5 = 1; var5 < var3; ++var5) {
         int var6 = var2.compare(var1[var5], var1[var4]);
         if (var6 < 0) {
            var4 = var5;
         }
      }

      return var4;
   }

   private <T> int fastMax(T[] var1, Comparator<T> var2, int var3) {
      int var4 = 0;

      for(int var5 = 1; var5 < var3; ++var5) {
         int var6 = var2.compare(var1[var5], var1[var4]);
         if (var6 > 0) {
            var4 = var5;
         }
      }

      return var4;
   }
}
