package zombie.core.random;

import java.util.Random;

public abstract class RandAbstract implements RandInterface {
   private int id = 0;
   protected Random rand;

   protected RandAbstract() {
   }

   public abstract void init();

   protected int Next(int var1, Random var2) {
      if (var1 <= 0) {
         return 0;
      } else {
         ++this.id;
         if (this.id >= 10000) {
            this.id = 0;
         }

         return var2.nextInt(var1);
      }
   }

   protected long Next(long var1, Random var3) {
      return (long)this.Next((int)var1, var3);
   }

   protected int Next(int var1, int var2, Random var3) {
      if (var2 == var1) {
         return var1;
      } else {
         int var4;
         if (var1 > var2) {
            var4 = var1;
            var1 = var2;
            var2 = var4;
         }

         ++this.id;
         if (this.id >= 10000) {
            this.id = 0;
         }

         var4 = var3.nextInt(var2 - var1);
         return var4 + var1;
      }
   }

   protected long Next(long var1, long var3, Random var5) {
      if (var3 == var1) {
         return var1;
      } else {
         if (var1 > var3) {
            long var6 = var1;
            var1 = var3;
            var3 = var6;
         }

         ++this.id;
         if (this.id >= 10000) {
            this.id = 0;
         }

         int var8 = var5.nextInt((int)(var3 - var1));
         return (long)var8 + var1;
      }
   }

   protected float Next(float var1, float var2, Random var3) {
      if (var2 == var1) {
         return var1;
      } else {
         if (var1 > var2) {
            float var4 = var1;
            var1 = var2;
            var2 = var4;
         }

         ++this.id;
         if (this.id >= 10000) {
            this.id = 0;
         }

         return var1 + var3.nextFloat() * (var2 - var1);
      }
   }

   public int Next(int var1) {
      return this.Next(var1, this.rand);
   }

   public long Next(long var1) {
      return this.Next(var1, this.rand);
   }

   public int Next(int var1, int var2) {
      return this.Next(var1, var2, this.rand);
   }

   public long Next(long var1, long var3) {
      return this.Next(var1, var3, this.rand);
   }

   public float Next(float var1, float var2) {
      return this.Next(var1, var2, this.rand);
   }
}
