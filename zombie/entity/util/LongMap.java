package zombie.entity.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class LongMap<V> implements Iterable<Entry<V>> {
   public int size;
   long[] keyTable;
   V[] valueTable;
   V zeroValue;
   boolean hasZeroValue;
   private final float loadFactor;
   private int threshold;
   protected int shift;
   protected int mask;
   private transient Entries entries1;
   private transient Entries entries2;
   private transient Values values1;
   private transient Values values2;
   private transient Keys keys1;
   private transient Keys keys2;

   public LongMap() {
      this(51, 0.8F);
   }

   public LongMap(int var1) {
      this(var1, 0.8F);
   }

   public LongMap(int var1, float var2) {
      if (!(var2 <= 0.0F) && !(var2 >= 1.0F)) {
         this.loadFactor = var2;
         int var3 = ObjectSet.tableSize(var1, var2);
         this.threshold = (int)((float)var3 * var2);
         this.mask = var3 - 1;
         this.shift = Long.numberOfLeadingZeros((long)this.mask);
         this.keyTable = new long[var3];
         this.valueTable = new Object[var3];
      } else {
         throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + var2);
      }
   }

   public LongMap(LongMap<? extends V> var1) {
      this((int)((float)var1.keyTable.length * var1.loadFactor), var1.loadFactor);
      System.arraycopy(var1.keyTable, 0, this.keyTable, 0, var1.keyTable.length);
      System.arraycopy(var1.valueTable, 0, this.valueTable, 0, var1.valueTable.length);
      this.size = var1.size;
      this.zeroValue = var1.zeroValue;
      this.hasZeroValue = var1.hasZeroValue;
   }

   protected int place(long var1) {
      return (int)((var1 ^ var1 >>> 32) * -7046029254386353131L >>> this.shift);
   }

   private int locateKey(long var1) {
      long[] var3 = this.keyTable;
      int var4 = this.place(var1);

      while(true) {
         long var5 = var3[var4];
         if (var5 == 0L) {
            return -(var4 + 1);
         }

         if (var5 == var1) {
            return var4;
         }

         var4 = var4 + 1 & this.mask;
      }
   }

   @Null
   public V put(long var1, @Null V var3) {
      if (var1 == 0L) {
         Object var6 = this.zeroValue;
         this.zeroValue = var3;
         if (!this.hasZeroValue) {
            this.hasZeroValue = true;
            ++this.size;
         }

         return var6;
      } else {
         int var4 = this.locateKey(var1);
         if (var4 >= 0) {
            Object var5 = this.valueTable[var4];
            this.valueTable[var4] = var3;
            return var5;
         } else {
            var4 = -(var4 + 1);
            this.keyTable[var4] = var1;
            this.valueTable[var4] = var3;
            if (++this.size >= this.threshold) {
               this.resize(this.keyTable.length << 1);
            }

            return null;
         }
      }
   }

   public void putAll(LongMap<? extends V> var1) {
      this.ensureCapacity(var1.size);
      if (var1.hasZeroValue) {
         this.put(0L, var1.zeroValue);
      }

      long[] var2 = var1.keyTable;
      Object[] var3 = var1.valueTable;
      int var4 = 0;

      for(int var5 = var2.length; var4 < var5; ++var4) {
         long var6 = var2[var4];
         if (var6 != 0L) {
            this.put(var6, var3[var4]);
         }
      }

   }

   private void putResize(long var1, @Null V var3) {
      long[] var4 = this.keyTable;

      int var5;
      for(var5 = this.place(var1); var4[var5] != 0L; var5 = var5 + 1 & this.mask) {
      }

      var4[var5] = var1;
      this.valueTable[var5] = var3;
   }

   @Null
   public V get(long var1) {
      if (var1 == 0L) {
         return this.hasZeroValue ? this.zeroValue : null;
      } else {
         int var3 = this.locateKey(var1);
         return var3 >= 0 ? this.valueTable[var3] : null;
      }
   }

   public V get(long var1, @Null V var3) {
      if (var1 == 0L) {
         return this.hasZeroValue ? this.zeroValue : var3;
      } else {
         int var4 = this.locateKey(var1);
         return var4 >= 0 ? this.valueTable[var4] : var3;
      }
   }

   @Null
   public V remove(long var1) {
      if (var1 == 0L) {
         if (!this.hasZeroValue) {
            return null;
         } else {
            this.hasZeroValue = false;
            Object var10 = this.zeroValue;
            this.zeroValue = null;
            --this.size;
            return var10;
         }
      } else {
         int var3 = this.locateKey(var1);
         if (var3 < 0) {
            return null;
         } else {
            long[] var4 = this.keyTable;
            Object[] var5 = this.valueTable;
            Object var6 = var5[var3];
            int var7 = this.mask;

            for(int var8 = var3 + 1 & var7; (var1 = var4[var8]) != 0L; var8 = var8 + 1 & var7) {
               int var9 = this.place(var1);
               if ((var8 - var9 & var7) > (var3 - var9 & var7)) {
                  var4[var3] = var1;
                  var5[var3] = var5[var8];
                  var3 = var8;
               }
            }

            var4[var3] = 0L;
            var5[var3] = null;
            --this.size;
            return var6;
         }
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
         int var2 = ObjectSet.tableSize(var1, this.loadFactor);
         if (this.keyTable.length > var2) {
            this.resize(var2);
         }

      }
   }

   public void clear(int var1) {
      int var2 = ObjectSet.tableSize(var1, this.loadFactor);
      if (this.keyTable.length <= var2) {
         this.clear();
      } else {
         this.size = 0;
         this.hasZeroValue = false;
         this.zeroValue = null;
         this.resize(var2);
      }
   }

   public void clear() {
      if (this.size != 0) {
         this.size = 0;
         Arrays.fill(this.keyTable, 0L);
         Arrays.fill(this.valueTable, (Object)null);
         this.zeroValue = null;
         this.hasZeroValue = false;
      }
   }

   public boolean containsValue(@Null Object var1, boolean var2) {
      Object[] var3 = this.valueTable;
      if (var1 == null) {
         if (this.hasZeroValue && this.zeroValue == null) {
            return true;
         }

         long[] var4 = this.keyTable;

         for(int var5 = var3.length - 1; var5 >= 0; --var5) {
            if (var4[var5] != 0L && var3[var5] == null) {
               return true;
            }
         }
      } else {
         int var6;
         if (var2) {
            if (var1 == this.zeroValue) {
               return true;
            }

            for(var6 = var3.length - 1; var6 >= 0; --var6) {
               if (var3[var6] == var1) {
                  return true;
               }
            }
         } else {
            if (this.hasZeroValue && var1.equals(this.zeroValue)) {
               return true;
            }

            for(var6 = var3.length - 1; var6 >= 0; --var6) {
               if (var1.equals(var3[var6])) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean containsKey(long var1) {
      if (var1 == 0L) {
         return this.hasZeroValue;
      } else {
         return this.locateKey(var1) >= 0;
      }
   }

   public long findKey(@Null Object var1, boolean var2, long var3) {
      Object[] var5 = this.valueTable;
      if (var1 == null) {
         if (this.hasZeroValue && this.zeroValue == null) {
            return 0L;
         }

         long[] var6 = this.keyTable;

         for(int var7 = var5.length - 1; var7 >= 0; --var7) {
            if (var6[var7] != 0L && var5[var7] == null) {
               return var6[var7];
            }
         }
      } else {
         int var8;
         if (var2) {
            if (var1 == this.zeroValue) {
               return 0L;
            }

            for(var8 = var5.length - 1; var8 >= 0; --var8) {
               if (var5[var8] == var1) {
                  return this.keyTable[var8];
               }
            }
         } else {
            if (this.hasZeroValue && var1.equals(this.zeroValue)) {
               return 0L;
            }

            for(var8 = var5.length - 1; var8 >= 0; --var8) {
               if (var1.equals(var5[var8])) {
                  return this.keyTable[var8];
               }
            }
         }
      }

      return var3;
   }

   public void ensureCapacity(int var1) {
      int var2 = ObjectSet.tableSize(this.size + var1, this.loadFactor);
      if (this.keyTable.length < var2) {
         this.resize(var2);
      }

   }

   private void resize(int var1) {
      int var2 = this.keyTable.length;
      this.threshold = (int)((float)var1 * this.loadFactor);
      this.mask = var1 - 1;
      this.shift = Long.numberOfLeadingZeros((long)this.mask);
      long[] var3 = this.keyTable;
      Object[] var4 = this.valueTable;
      this.keyTable = new long[var1];
      this.valueTable = new Object[var1];
      if (this.size > 0) {
         for(int var5 = 0; var5 < var2; ++var5) {
            long var6 = var3[var5];
            if (var6 != 0L) {
               this.putResize(var6, var4[var5]);
            }
         }
      }

   }

   public int hashCode() {
      int var1 = this.size;
      if (this.hasZeroValue && this.zeroValue != null) {
         var1 += this.zeroValue.hashCode();
      }

      long[] var2 = this.keyTable;
      Object[] var3 = this.valueTable;
      int var4 = 0;

      for(int var5 = var2.length; var4 < var5; ++var4) {
         long var6 = var2[var4];
         if (var6 != 0L) {
            var1 = (int)((long)var1 + var6 * 31L);
            Object var8 = var3[var4];
            if (var8 != null) {
               var1 += var8.hashCode();
            }
         }
      }

      return var1;
   }

   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      } else if (!(var1 instanceof LongMap)) {
         return false;
      } else {
         LongMap var2 = (LongMap)var1;
         if (var2.size != this.size) {
            return false;
         } else if (var2.hasZeroValue != this.hasZeroValue) {
            return false;
         } else {
            if (this.hasZeroValue) {
               if (var2.zeroValue == null) {
                  if (this.zeroValue != null) {
                     return false;
                  }
               } else if (!var2.zeroValue.equals(this.zeroValue)) {
                  return false;
               }
            }

            long[] var3 = this.keyTable;
            Object[] var4 = this.valueTable;
            int var5 = 0;

            for(int var6 = var3.length; var5 < var6; ++var5) {
               long var7 = var3[var5];
               if (var7 != 0L) {
                  Object var9 = var4[var5];
                  if (var9 == null) {
                     if (var2.get(var7, ObjectMap.dummy) != null) {
                        return false;
                     }
                  } else if (!var9.equals(var2.get(var7))) {
                     return false;
                  }
               }
            }

            return true;
         }
      }
   }

   public boolean equalsIdentity(@Null Object var1) {
      if (var1 == this) {
         return true;
      } else if (!(var1 instanceof LongMap)) {
         return false;
      } else {
         LongMap var2 = (LongMap)var1;
         if (var2.size != this.size) {
            return false;
         } else if (var2.hasZeroValue != this.hasZeroValue) {
            return false;
         } else if (this.hasZeroValue && this.zeroValue != var2.zeroValue) {
            return false;
         } else {
            long[] var3 = this.keyTable;
            Object[] var4 = this.valueTable;
            int var5 = 0;

            for(int var6 = var3.length; var5 < var6; ++var5) {
               long var7 = var3[var5];
               if (var7 != 0L && var4[var5] != var2.get(var7, ObjectMap.dummy)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public String toString() {
      if (this.size == 0) {
         return "[]";
      } else {
         StringBuilder var1 = new StringBuilder(32);
         var1.append('[');
         long[] var2 = this.keyTable;
         Object[] var3 = this.valueTable;
         int var4 = var2.length;
         long var5;
         if (this.hasZeroValue) {
            var1.append("0=");
            var1.append(this.zeroValue);
         } else {
            while(var4-- > 0) {
               var5 = var2[var4];
               if (var5 != 0L) {
                  var1.append(var5);
                  var1.append('=');
                  var1.append(var3[var4]);
                  break;
               }
            }
         }

         while(var4-- > 0) {
            var5 = var2[var4];
            if (var5 != 0L) {
               var1.append(", ");
               var1.append(var5);
               var1.append('=');
               var1.append(var3[var4]);
            }
         }

         var1.append(']');
         return var1.toString();
      }
   }

   public Iterator<Entry<V>> iterator() {
      return this.entries();
   }

   public Entries<V> entries() {
      if (Collections.allocateIterators) {
         return new Entries(this);
      } else {
         if (this.entries1 == null) {
            this.entries1 = new Entries(this);
            this.entries2 = new Entries(this);
         }

         if (!this.entries1.valid) {
            this.entries1.reset();
            this.entries1.valid = true;
            this.entries2.valid = false;
            return this.entries1;
         } else {
            this.entries2.reset();
            this.entries2.valid = true;
            this.entries1.valid = false;
            return this.entries2;
         }
      }
   }

   public Values<V> values() {
      if (Collections.allocateIterators) {
         return new Values(this);
      } else {
         if (this.values1 == null) {
            this.values1 = new Values(this);
            this.values2 = new Values(this);
         }

         if (!this.values1.valid) {
            this.values1.reset();
            this.values1.valid = true;
            this.values2.valid = false;
            return this.values1;
         } else {
            this.values2.reset();
            this.values2.valid = true;
            this.values1.valid = false;
            return this.values2;
         }
      }
   }

   public Keys keys() {
      if (Collections.allocateIterators) {
         return new Keys(this);
      } else {
         if (this.keys1 == null) {
            this.keys1 = new Keys(this);
            this.keys2 = new Keys(this);
         }

         if (!this.keys1.valid) {
            this.keys1.reset();
            this.keys1.valid = true;
            this.keys2.valid = false;
            return this.keys1;
         } else {
            this.keys2.reset();
            this.keys2.valid = true;
            this.keys1.valid = false;
            return this.keys2;
         }
      }
   }

   public static class Entries<V> extends MapIterator<V> implements Iterable<Entry<V>>, Iterator<Entry<V>> {
      private final Entry<V> entry = new Entry();

      public Entries(LongMap var1) {
         super(var1);
      }

      public Entry<V> next() {
         if (!this.hasNext) {
            throw new NoSuchElementException();
         } else if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            long[] var1 = this.map.keyTable;
            if (this.nextIndex == -1) {
               this.entry.key = 0L;
               this.entry.value = this.map.zeroValue;
            } else {
               this.entry.key = var1[this.nextIndex];
               this.entry.value = this.map.valueTable[this.nextIndex];
            }

            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return this.entry;
         }
      }

      public boolean hasNext() {
         if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            return this.hasNext;
         }
      }

      public Iterator<Entry<V>> iterator() {
         return this;
      }
   }

   public static class Values<V> extends MapIterator<V> implements Iterable<V>, Iterator<V> {
      public Values(LongMap<V> var1) {
         super(var1);
      }

      public boolean hasNext() {
         if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            return this.hasNext;
         }
      }

      @Null
      public V next() {
         if (!this.hasNext) {
            throw new NoSuchElementException();
         } else if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            Object var1;
            if (this.nextIndex == -1) {
               var1 = this.map.zeroValue;
            } else {
               var1 = this.map.valueTable[this.nextIndex];
            }

            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return var1;
         }
      }

      public Iterator<V> iterator() {
         return this;
      }

      public Array<V> toArray() {
         Array var1 = new Array(true, this.map.size);

         while(this.hasNext) {
            var1.add(this.next());
         }

         return var1;
      }
   }

   public static class Keys extends MapIterator {
      public Keys(LongMap var1) {
         super(var1);
      }

      public long next() {
         if (!this.hasNext) {
            throw new NoSuchElementException();
         } else if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            long var1 = this.nextIndex == -1 ? 0L : this.map.keyTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return var1;
         }
      }

      public LongArray toArray() {
         LongArray var1 = new LongArray(true, this.map.size);

         while(this.hasNext) {
            var1.add(this.next());
         }

         return var1;
      }

      public LongArray toArray(LongArray var1) {
         while(this.hasNext) {
            var1.add(this.next());
         }

         return var1;
      }
   }

   private static class MapIterator<V> {
      private static final int INDEX_ILLEGAL = -2;
      static final int INDEX_ZERO = -1;
      public boolean hasNext;
      final LongMap<V> map;
      int nextIndex;
      int currentIndex;
      boolean valid = true;

      public MapIterator(LongMap<V> var1) {
         this.map = var1;
         this.reset();
      }

      public void reset() {
         this.currentIndex = -2;
         this.nextIndex = -1;
         if (this.map.hasZeroValue) {
            this.hasNext = true;
         } else {
            this.findNextIndex();
         }

      }

      void findNextIndex() {
         long[] var1 = this.map.keyTable;
         int var2 = var1.length;

         do {
            if (++this.nextIndex >= var2) {
               this.hasNext = false;
               return;
            }
         } while(var1[this.nextIndex] == 0L);

         this.hasNext = true;
      }

      public void remove() {
         int var1 = this.currentIndex;
         if (var1 == -1 && this.map.hasZeroValue) {
            this.map.hasZeroValue = false;
            this.map.zeroValue = null;
         } else {
            if (var1 < 0) {
               throw new IllegalStateException("next must be called before remove.");
            }

            long[] var2 = this.map.keyTable;
            Object[] var3 = this.map.valueTable;
            int var4 = this.map.mask;

            long var6;
            for(int var5 = var1 + 1 & var4; (var6 = var2[var5]) != 0L; var5 = var5 + 1 & var4) {
               int var8 = this.map.place(var6);
               if ((var5 - var8 & var4) > (var1 - var8 & var4)) {
                  var2[var1] = var6;
                  var3[var1] = var3[var5];
                  var1 = var5;
               }
            }

            var2[var1] = 0L;
            var3[var1] = null;
            if (var1 != this.currentIndex) {
               --this.nextIndex;
            }
         }

         this.currentIndex = -2;
         --this.map.size;
      }
   }

   public static class Entry<V> {
      public long key;
      @Null
      public V value;

      public Entry() {
      }

      public String toString() {
         return this.key + "=" + this.value;
      }
   }
}
