package zombie.entity.util;

import java.util.Arrays;
import zombie.core.random.Rand;

public class LongArray {
   public long[] items;
   public int size;
   public boolean ordered;

   public LongArray() {
      this(true, 16);
   }

   public LongArray(int var1) {
      this(true, var1);
   }

   public LongArray(boolean var1, int var2) {
      this.ordered = var1;
      this.items = new long[var2];
   }

   public LongArray(LongArray var1) {
      this.ordered = var1.ordered;
      this.size = var1.size;
      this.items = new long[this.size];
      System.arraycopy(var1.items, 0, this.items, 0, this.size);
   }

   public LongArray(long[] var1) {
      this(true, var1, 0, var1.length);
   }

   public LongArray(boolean var1, long[] var2, int var3, int var4) {
      this(var1, var4);
      this.size = var4;
      System.arraycopy(var2, var3, this.items, 0, var4);
   }

   public void add(long var1) {
      long[] var3 = this.items;
      if (this.size == var3.length) {
         var3 = this.resize(Math.max(8, (int)((float)this.size * 1.75F)));
      }

      var3[this.size++] = var1;
   }

   public void add(long var1, long var3) {
      long[] var5 = this.items;
      if (this.size + 1 >= var5.length) {
         var5 = this.resize(Math.max(8, (int)((float)this.size * 1.75F)));
      }

      var5[this.size] = var1;
      var5[this.size + 1] = var3;
      this.size += 2;
   }

   public void add(long var1, long var3, long var5) {
      long[] var7 = this.items;
      if (this.size + 2 >= var7.length) {
         var7 = this.resize(Math.max(8, (int)((float)this.size * 1.75F)));
      }

      var7[this.size] = var1;
      var7[this.size + 1] = var3;
      var7[this.size + 2] = var5;
      this.size += 3;
   }

   public void add(long var1, long var3, long var5, long var7) {
      long[] var9 = this.items;
      if (this.size + 3 >= var9.length) {
         var9 = this.resize(Math.max(8, (int)((float)this.size * 1.8F)));
      }

      var9[this.size] = var1;
      var9[this.size + 1] = var3;
      var9[this.size + 2] = var5;
      var9[this.size + 3] = var7;
      this.size += 4;
   }

   public void addAll(LongArray var1) {
      this.addAll((long[])var1.items, 0, var1.size);
   }

   public void addAll(LongArray var1, int var2, int var3) {
      if (var2 + var3 > var1.size) {
         throw new IllegalArgumentException("offset + length must be <= size: " + var2 + " + " + var3 + " <= " + var1.size);
      } else {
         this.addAll(var1.items, var2, var3);
      }
   }

   public void addAll(long... var1) {
      this.addAll((long[])var1, 0, var1.length);
   }

   public void addAll(long[] var1, int var2, int var3) {
      long[] var4 = this.items;
      int var5 = this.size + var3;
      if (var5 > var4.length) {
         var4 = this.resize(Math.max(Math.max(8, var5), (int)((float)this.size * 1.75F)));
      }

      System.arraycopy(var1, var2, var4, this.size, var3);
      this.size += var3;
   }

   public long get(int var1) {
      if (var1 >= this.size) {
         throw new IndexOutOfBoundsException("index can't be >= size: " + var1 + " >= " + this.size);
      } else {
         return this.items[var1];
      }
   }

   public void set(int var1, long var2) {
      if (var1 >= this.size) {
         throw new IndexOutOfBoundsException("index can't be >= size: " + var1 + " >= " + this.size);
      } else {
         this.items[var1] = var2;
      }
   }

   public void incr(int var1, long var2) {
      if (var1 >= this.size) {
         throw new IndexOutOfBoundsException("index can't be >= size: " + var1 + " >= " + this.size);
      } else {
         long[] var10000 = this.items;
         var10000[var1] += var2;
      }
   }

   public void incr(long var1) {
      long[] var3 = this.items;
      int var4 = 0;

      for(int var5 = this.size; var4 < var5; ++var4) {
         var3[var4] += var1;
      }

   }

   public void mul(int var1, long var2) {
      if (var1 >= this.size) {
         throw new IndexOutOfBoundsException("index can't be >= size: " + var1 + " >= " + this.size);
      } else {
         long[] var10000 = this.items;
         var10000[var1] *= var2;
      }
   }

   public void mul(long var1) {
      long[] var3 = this.items;
      int var4 = 0;

      for(int var5 = this.size; var4 < var5; ++var4) {
         var3[var4] *= var1;
      }

   }

   public void insert(int var1, long var2) {
      if (var1 > this.size) {
         throw new IndexOutOfBoundsException("index can't be > size: " + var1 + " > " + this.size);
      } else {
         long[] var4 = this.items;
         if (this.size == var4.length) {
            var4 = this.resize(Math.max(8, (int)((float)this.size * 1.75F)));
         }

         if (this.ordered) {
            System.arraycopy(var4, var1, var4, var1 + 1, this.size - var1);
         } else {
            var4[this.size] = var4[var1];
         }

         ++this.size;
         var4[var1] = var2;
      }
   }

   public void insertRange(int var1, int var2) {
      if (var1 > this.size) {
         throw new IndexOutOfBoundsException("index can't be > size: " + var1 + " > " + this.size);
      } else {
         int var3 = this.size + var2;
         if (var3 > this.items.length) {
            this.items = this.resize(Math.max(Math.max(8, var3), (int)((float)this.size * 1.75F)));
         }

         System.arraycopy(this.items, var1, this.items, var1 + var2, this.size - var1);
         this.size = var3;
      }
   }

   public void swap(int var1, int var2) {
      if (var1 >= this.size) {
         throw new IndexOutOfBoundsException("first can't be >= size: " + var1 + " >= " + this.size);
      } else if (var2 >= this.size) {
         throw new IndexOutOfBoundsException("second can't be >= size: " + var2 + " >= " + this.size);
      } else {
         long[] var3 = this.items;
         long var4 = var3[var1];
         var3[var1] = var3[var2];
         var3[var2] = var4;
      }
   }

   public boolean contains(long var1) {
      int var3 = this.size - 1;
      long[] var4 = this.items;

      do {
         if (var3 < 0) {
            return false;
         }
      } while(var4[var3--] != var1);

      return true;
   }

   public int indexOf(long var1) {
      long[] var3 = this.items;
      int var4 = 0;

      for(int var5 = this.size; var4 < var5; ++var4) {
         if (var3[var4] == var1) {
            return var4;
         }
      }

      return -1;
   }

   public int lastIndexOf(char var1) {
      long[] var2 = this.items;

      for(int var3 = this.size - 1; var3 >= 0; --var3) {
         if (var2[var3] == (long)var1) {
            return var3;
         }
      }

      return -1;
   }

   public boolean removeValue(long var1) {
      long[] var3 = this.items;
      int var4 = 0;

      for(int var5 = this.size; var4 < var5; ++var4) {
         if (var3[var4] == var1) {
            this.removeIndex(var4);
            return true;
         }
      }

      return false;
   }

   public long removeIndex(int var1) {
      if (var1 >= this.size) {
         throw new IndexOutOfBoundsException("index can't be >= size: " + var1 + " >= " + this.size);
      } else {
         long[] var2 = this.items;
         long var3 = var2[var1];
         --this.size;
         if (this.ordered) {
            System.arraycopy(var2, var1 + 1, var2, var1, this.size - var1);
         } else {
            var2[var1] = var2[this.size];
         }

         return var3;
      }
   }

   public void removeRange(int var1, int var2) {
      int var3 = this.size;
      if (var2 >= var3) {
         throw new IndexOutOfBoundsException("end can't be >= size: " + var2 + " >= " + this.size);
      } else if (var1 > var2) {
         throw new IndexOutOfBoundsException("start can't be > end: " + var1 + " > " + var2);
      } else {
         int var4 = var2 - var1 + 1;
         int var5 = var3 - var4;
         if (this.ordered) {
            System.arraycopy(this.items, var1 + var4, this.items, var1, var3 - (var1 + var4));
         } else {
            int var6 = Math.max(var5, var2 + 1);
            System.arraycopy(this.items, var6, this.items, var1, var3 - var6);
         }

         this.size = var3 - var4;
      }
   }

   public boolean removeAll(LongArray var1) {
      int var2 = this.size;
      int var3 = var2;
      long[] var4 = this.items;
      int var5 = 0;

      for(int var6 = var1.size; var5 < var6; ++var5) {
         long var7 = var1.get(var5);

         for(int var9 = 0; var9 < var2; ++var9) {
            if (var7 == var4[var9]) {
               this.removeIndex(var9);
               --var2;
               break;
            }
         }
      }

      return var2 != var3;
   }

   public long pop() {
      return this.items[--this.size];
   }

   public long peek() {
      return this.items[this.size - 1];
   }

   public long first() {
      if (this.size == 0) {
         throw new IllegalStateException("Array is empty.");
      } else {
         return this.items[0];
      }
   }

   public boolean notEmpty() {
      return this.size > 0;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public void clear() {
      this.size = 0;
   }

   public long[] shrink() {
      if (this.items.length != this.size) {
         this.resize(this.size);
      }

      return this.items;
   }

   public long[] ensureCapacity(int var1) {
      if (var1 < 0) {
         throw new IllegalArgumentException("additionalCapacity must be >= 0: " + var1);
      } else {
         int var2 = this.size + var1;
         if (var2 > this.items.length) {
            this.resize(Math.max(Math.max(8, var2), (int)((float)this.size * 1.75F)));
         }

         return this.items;
      }
   }

   public long[] setSize(int var1) {
      if (var1 < 0) {
         throw new IllegalArgumentException("newSize must be >= 0: " + var1);
      } else {
         if (var1 > this.items.length) {
            this.resize(Math.max(8, var1));
         }

         this.size = var1;
         return this.items;
      }
   }

   protected long[] resize(int var1) {
      long[] var2 = new long[var1];
      long[] var3 = this.items;
      System.arraycopy(var3, 0, var2, 0, Math.min(this.size, var2.length));
      this.items = var2;
      return var2;
   }

   public void sort() {
      Arrays.sort(this.items, 0, this.size);
   }

   public void reverse() {
      long[] var1 = this.items;
      int var2 = 0;
      int var3 = this.size - 1;

      for(int var4 = this.size / 2; var2 < var4; ++var2) {
         int var5 = var3 - var2;
         long var6 = var1[var2];
         var1[var2] = var1[var5];
         var1[var5] = var6;
      }

   }

   public void shuffle() {
      long[] var1 = this.items;

      for(int var2 = this.size - 1; var2 >= 0; --var2) {
         int var3 = Rand.Next(var2);
         long var4 = var1[var2];
         var1[var2] = var1[var3];
         var1[var3] = var4;
      }

   }

   public void truncate(int var1) {
      if (this.size > var1) {
         this.size = var1;
      }

   }

   public long random() {
      return this.size == 0 ? 0L : this.items[Rand.Next(0, this.size - 1)];
   }

   public long[] toArray() {
      long[] var1 = new long[this.size];
      System.arraycopy(this.items, 0, var1, 0, this.size);
      return var1;
   }

   public int hashCode() {
      if (!this.ordered) {
         return super.hashCode();
      } else {
         long[] var1 = this.items;
         int var2 = 1;
         int var3 = 0;

         for(int var4 = this.size; var3 < var4; ++var3) {
            long var5 = var1[var3];
            var2 = var2 * 31 + (int)(var5 ^ var5 >>> 32);
         }

         return var2;
      }
   }

   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      } else if (!this.ordered) {
         return false;
      } else if (!(var1 instanceof LongArray)) {
         return false;
      } else {
         LongArray var2 = (LongArray)var1;
         if (!var2.ordered) {
            return false;
         } else {
            int var3 = this.size;
            if (var3 != var2.size) {
               return false;
            } else {
               long[] var4 = this.items;
               long[] var5 = var2.items;

               for(int var6 = 0; var6 < var3; ++var6) {
                  if (var4[var6] != var5[var6]) {
                     return false;
                  }
               }

               return true;
            }
         }
      }
   }

   public String toString() {
      if (this.size == 0) {
         return "[]";
      } else {
         long[] var1 = this.items;
         StringBuilder var2 = new StringBuilder(32);
         var2.append('[');
         var2.append(var1[0]);

         for(int var3 = 1; var3 < this.size; ++var3) {
            var2.append(", ");
            var2.append(var1[var3]);
         }

         var2.append(']');
         return var2.toString();
      }
   }

   public String toString(String var1) {
      if (this.size == 0) {
         return "";
      } else {
         long[] var2 = this.items;
         StringBuilder var3 = new StringBuilder(32);
         var3.append(var2[0]);

         for(int var4 = 1; var4 < this.size; ++var4) {
            var3.append(var1);
            var3.append(var2[var4]);
         }

         return var3.toString();
      }
   }

   public static LongArray with(long... var0) {
      return new LongArray(var0);
   }
}
