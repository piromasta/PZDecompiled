package zombie.entity.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import zombie.core.math.PZMath;

public class ObjectSet<T> implements Iterable<T> {
   public int size;
   T[] keyTable;
   float loadFactor;
   int threshold;
   protected int shift;
   protected int mask;
   private transient ObjectSetIterator iterator1;
   private transient ObjectSetIterator iterator2;

   public ObjectSet() {
      this(51, 0.8F);
   }

   public ObjectSet(int var1) {
      this(var1, 0.8F);
   }

   public ObjectSet(int var1, float var2) {
      if (!(var2 <= 0.0F) && !(var2 >= 1.0F)) {
         this.loadFactor = var2;
         int var3 = tableSize(var1, var2);
         this.threshold = (int)((float)var3 * var2);
         this.mask = var3 - 1;
         this.shift = Long.numberOfLeadingZeros((long)this.mask);
         this.keyTable = new Object[var3];
      } else {
         throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + var2);
      }
   }

   public ObjectSet(ObjectSet<? extends T> var1) {
      this((int)((float)var1.keyTable.length * var1.loadFactor), var1.loadFactor);
      System.arraycopy(var1.keyTable, 0, this.keyTable, 0, var1.keyTable.length);
      this.size = var1.size;
   }

   protected int place(T var1) {
      return (int)((long)var1.hashCode() * -7046029254386353131L >>> this.shift);
   }

   int locateKey(T var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("key cannot be null.");
      } else {
         Object[] var2 = this.keyTable;
         int var3 = this.place(var1);

         while(true) {
            Object var4 = var2[var3];
            if (var4 == null) {
               return -(var3 + 1);
            }

            if (var4.equals(var1)) {
               return var3;
            }

            var3 = var3 + 1 & this.mask;
         }
      }
   }

   public boolean add(T var1) {
      int var2 = this.locateKey(var1);
      if (var2 >= 0) {
         return false;
      } else {
         var2 = -(var2 + 1);
         this.keyTable[var2] = var1;
         if (++this.size >= this.threshold) {
            this.resize(this.keyTable.length << 1);
         }

         return true;
      }
   }

   public void addAll(Array<? extends T> var1) {
      this.addAll((Object[])var1.items, 0, var1.size);
   }

   public void addAll(Array<? extends T> var1, int var2, int var3) {
      if (var2 + var3 > var1.size) {
         throw new IllegalArgumentException("offset + length must be <= size: " + var2 + " + " + var3 + " <= " + var1.size);
      } else {
         this.addAll(var1.items, var2, var3);
      }
   }

   public boolean addAll(T... var1) {
      return this.addAll((Object[])var1, 0, var1.length);
   }

   public boolean addAll(T[] var1, int var2, int var3) {
      this.ensureCapacity(var3);
      int var4 = this.size;
      int var5 = var2;

      for(int var6 = var2 + var3; var5 < var6; ++var5) {
         this.add(var1[var5]);
      }

      return var4 != this.size;
   }

   public void addAll(ObjectSet<T> var1) {
      this.ensureCapacity(var1.size);
      Object[] var2 = var1.keyTable;
      int var3 = 0;

      for(int var4 = var2.length; var3 < var4; ++var3) {
         Object var5 = var2[var3];
         if (var5 != null) {
            this.add(var5);
         }
      }

   }

   private void addResize(T var1) {
      Object[] var2 = this.keyTable;

      int var3;
      for(var3 = this.place(var1); var2[var3] != null; var3 = var3 + 1 & this.mask) {
      }

      var2[var3] = var1;
   }

   public boolean remove(T var1) {
      int var2 = this.locateKey(var1);
      if (var2 < 0) {
         return false;
      } else {
         Object[] var3 = this.keyTable;
         int var4 = this.mask;

         for(int var5 = var2 + 1 & var4; (var1 = var3[var5]) != null; var5 = var5 + 1 & var4) {
            int var6 = this.place(var1);
            if ((var5 - var6 & var4) > (var2 - var6 & var4)) {
               var3[var2] = var1;
               var2 = var5;
            }
         }

         var3[var2] = null;
         --this.size;
         return true;
      }
   }

   public boolean notEmpty() {
      return this.size > 0;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public void shrink(int var1) {
      if (var1 < 0) {
         throw new IllegalArgumentException("maximumCapacity must be >= 0: " + var1);
      } else {
         int var2 = tableSize(var1, this.loadFactor);
         if (this.keyTable.length > var2) {
            this.resize(var2);
         }

      }
   }

   public void clear(int var1) {
      int var2 = tableSize(var1, this.loadFactor);
      if (this.keyTable.length <= var2) {
         this.clear();
      } else {
         this.size = 0;
         this.resize(var2);
      }
   }

   public void clear() {
      if (this.size != 0) {
         this.size = 0;
         Arrays.fill(this.keyTable, (Object)null);
      }
   }

   public boolean contains(T var1) {
      return this.locateKey(var1) >= 0;
   }

   @Null
   public T get(T var1) {
      int var2 = this.locateKey(var1);
      return var2 < 0 ? null : this.keyTable[var2];
   }

   public T first() {
      Object[] var1 = this.keyTable;
      int var2 = 0;

      for(int var3 = var1.length; var2 < var3; ++var2) {
         if (var1[var2] != null) {
            return var1[var2];
         }
      }

      throw new IllegalStateException("ObjectSet is empty.");
   }

   public void ensureCapacity(int var1) {
      int var2 = tableSize(this.size + var1, this.loadFactor);
      if (this.keyTable.length < var2) {
         this.resize(var2);
      }

   }

   private void resize(int var1) {
      int var2 = this.keyTable.length;
      this.threshold = (int)((float)var1 * this.loadFactor);
      this.mask = var1 - 1;
      this.shift = Long.numberOfLeadingZeros((long)this.mask);
      Object[] var3 = this.keyTable;
      this.keyTable = new Object[var1];
      if (this.size > 0) {
         for(int var4 = 0; var4 < var2; ++var4) {
            Object var5 = var3[var4];
            if (var5 != null) {
               this.addResize(var5);
            }
         }
      }

   }

   public int hashCode() {
      int var1 = this.size;
      Object[] var2 = this.keyTable;
      int var3 = 0;

      for(int var4 = var2.length; var3 < var4; ++var3) {
         Object var5 = var2[var3];
         if (var5 != null) {
            var1 += var5.hashCode();
         }
      }

      return var1;
   }

   public boolean equals(Object var1) {
      if (!(var1 instanceof ObjectSet var2)) {
         return false;
      } else if (var2.size != this.size) {
         return false;
      } else {
         Object[] var3 = this.keyTable;
         int var4 = 0;

         for(int var5 = var3.length; var4 < var5; ++var4) {
            if (var3[var4] != null && !var2.contains(var3[var4])) {
               return false;
            }
         }

         return true;
      }
   }

   public String toString() {
      return "{" + this.toString(", ") + "}";
   }

   public String toString(String var1) {
      if (this.size == 0) {
         return "";
      } else {
         StringBuilder var2 = new StringBuilder(32);
         Object[] var3 = this.keyTable;
         int var4 = var3.length;

         Object var5;
         while(var4-- > 0) {
            var5 = var3[var4];
            if (var5 != null) {
               var2.append(var5 == this ? "(this)" : var5);
               break;
            }
         }

         while(var4-- > 0) {
            var5 = var3[var4];
            if (var5 != null) {
               var2.append(var1);
               var2.append(var5 == this ? "(this)" : var5);
            }
         }

         return var2.toString();
      }
   }

   public ObjectSetIterator<T> iterator() {
      if (Collections.allocateIterators) {
         return new ObjectSetIterator(this);
      } else {
         if (this.iterator1 == null) {
            this.iterator1 = new ObjectSetIterator(this);
            this.iterator2 = new ObjectSetIterator(this);
         }

         if (!this.iterator1.valid) {
            this.iterator1.reset();
            this.iterator1.valid = true;
            this.iterator2.valid = false;
            return this.iterator1;
         } else {
            this.iterator2.reset();
            this.iterator2.valid = true;
            this.iterator1.valid = false;
            return this.iterator2;
         }
      }
   }

   public static <T> ObjectSet<T> with(T... var0) {
      ObjectSet var1 = new ObjectSet();
      var1.addAll(var0);
      return var1;
   }

   static int tableSize(int var0, float var1) {
      if (var0 < 0) {
         throw new IllegalArgumentException("capacity must be >= 0: " + var0);
      } else {
         int var2 = PZMath.nextPowerOfTwo(Math.max(2, (int)Math.ceil((double)((float)var0 / var1))));
         if (var2 > 1073741824) {
            throw new IllegalArgumentException("The required capacity is too large: " + var0);
         } else {
            return var2;
         }
      }
   }

   public static class ObjectSetIterator<K> implements Iterable<K>, Iterator<K> {
      public boolean hasNext;
      final ObjectSet<K> set;
      int nextIndex;
      int currentIndex;
      boolean valid = true;

      public ObjectSetIterator(ObjectSet<K> var1) {
         this.set = var1;
         this.reset();
      }

      public void reset() {
         this.currentIndex = -1;
         this.nextIndex = -1;
         this.findNextIndex();
      }

      private void findNextIndex() {
         Object[] var1 = this.set.keyTable;
         int var2 = this.set.keyTable.length;

         do {
            if (++this.nextIndex >= var2) {
               this.hasNext = false;
               return;
            }
         } while(var1[this.nextIndex] == null);

         this.hasNext = true;
      }

      public void remove() {
         int var1 = this.currentIndex;
         if (var1 < 0) {
            throw new IllegalStateException("next must be called before remove.");
         } else {
            Object[] var2 = this.set.keyTable;
            int var3 = this.set.mask;

            Object var5;
            for(int var4 = var1 + 1 & var3; (var5 = var2[var4]) != null; var4 = var4 + 1 & var3) {
               int var6 = this.set.place(var5);
               if ((var4 - var6 & var3) > (var1 - var6 & var3)) {
                  var2[var1] = var5;
                  var1 = var4;
               }
            }

            var2[var1] = null;
            --this.set.size;
            if (var1 != this.currentIndex) {
               --this.nextIndex;
            }

            this.currentIndex = -1;
         }
      }

      public boolean hasNext() {
         if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            return this.hasNext;
         }
      }

      public K next() {
         if (!this.hasNext) {
            throw new NoSuchElementException();
         } else if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            Object var1 = this.set.keyTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return var1;
         }
      }

      public ObjectSetIterator<K> iterator() {
         return this;
      }

      public Array<K> toArray(Array<K> var1) {
         while(this.hasNext) {
            var1.add(this.next());
         }

         return var1;
      }

      public Array<K> toArray() {
         return this.toArray(new Array(true, this.set.size));
      }
   }
}
