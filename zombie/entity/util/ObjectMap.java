package zombie.entity.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ObjectMap<K, V> implements Iterable<Entry<K, V>> {
   static final Object dummy = new Object();
   public int size;
   K[] keyTable;
   V[] valueTable;
   float loadFactor;
   int threshold;
   protected int shift;
   protected int mask;
   transient Entries entries1;
   transient Entries entries2;
   transient Values values1;
   transient Values values2;
   transient Keys keys1;
   transient Keys keys2;

   public ObjectMap() {
      this(51, 0.8F);
   }

   public ObjectMap(int var1) {
      this(var1, 0.8F);
   }

   public ObjectMap(int var1, float var2) {
      if (!(var2 <= 0.0F) && !(var2 >= 1.0F)) {
         this.loadFactor = var2;
         int var3 = ObjectSet.tableSize(var1, var2);
         this.threshold = (int)((float)var3 * var2);
         this.mask = var3 - 1;
         this.shift = Long.numberOfLeadingZeros((long)this.mask);
         this.keyTable = new Object[var3];
         this.valueTable = new Object[var3];
      } else {
         throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + var2);
      }
   }

   public ObjectMap(ObjectMap<? extends K, ? extends V> var1) {
      this((int)((float)var1.keyTable.length * var1.loadFactor), var1.loadFactor);
      System.arraycopy(var1.keyTable, 0, this.keyTable, 0, var1.keyTable.length);
      System.arraycopy(var1.valueTable, 0, this.valueTable, 0, var1.valueTable.length);
      this.size = var1.size;
   }

   protected int place(K var1) {
      return (int)((long)var1.hashCode() * -7046029254386353131L >>> this.shift);
   }

   int locateKey(K var1) {
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

   @Null
   public V put(K var1, @Null V var2) {
      int var3 = this.locateKey(var1);
      if (var3 >= 0) {
         Object var4 = this.valueTable[var3];
         this.valueTable[var3] = var2;
         return var4;
      } else {
         var3 = -(var3 + 1);
         this.keyTable[var3] = var1;
         this.valueTable[var3] = var2;
         if (++this.size >= this.threshold) {
            this.resize(this.keyTable.length << 1);
         }

         return null;
      }
   }

   public void putAll(ObjectMap<? extends K, ? extends V> var1) {
      this.ensureCapacity(var1.size);
      Object[] var2 = var1.keyTable;
      Object[] var3 = var1.valueTable;
      int var5 = 0;

      for(int var6 = var2.length; var5 < var6; ++var5) {
         Object var4 = var2[var5];
         if (var4 != null) {
            this.put(var4, var3[var5]);
         }
      }

   }

   private void putResize(K var1, @Null V var2) {
      Object[] var3 = this.keyTable;

      int var4;
      for(var4 = this.place(var1); var3[var4] != null; var4 = var4 + 1 & this.mask) {
      }

      var3[var4] = var1;
      this.valueTable[var4] = var2;
   }

   @Null
   public <T extends K> V get(T var1) {
      int var2 = this.locateKey(var1);
      return var2 < 0 ? null : this.valueTable[var2];
   }

   public V get(K var1, @Null V var2) {
      int var3 = this.locateKey(var1);
      return var3 < 0 ? var2 : this.valueTable[var3];
   }

   @Null
   public V remove(K var1) {
      int var2 = this.locateKey(var1);
      if (var2 < 0) {
         return null;
      } else {
         Object[] var3 = this.keyTable;
         Object[] var4 = this.valueTable;
         Object var5 = var4[var2];
         int var6 = this.mask;

         for(int var7 = var2 + 1 & var6; (var1 = var3[var7]) != null; var7 = var7 + 1 & var6) {
            int var8 = this.place(var1);
            if ((var7 - var8 & var6) > (var2 - var8 & var6)) {
               var3[var2] = var1;
               var4[var2] = var4[var7];
               var2 = var7;
            }
         }

         var3[var2] = null;
         var4[var2] = null;
         --this.size;
         return var5;
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
         this.resize(var2);
      }
   }

   public void clear() {
      if (this.size != 0) {
         this.size = 0;
         Arrays.fill(this.keyTable, (Object)null);
         Arrays.fill(this.valueTable, (Object)null);
      }
   }

   public boolean containsValue(@Null Object var1, boolean var2) {
      Object[] var3 = this.valueTable;
      if (var1 == null) {
         Object[] var4 = this.keyTable;

         for(int var5 = var3.length - 1; var5 >= 0; --var5) {
            if (var4[var5] != null && var3[var5] == null) {
               return true;
            }
         }
      } else {
         int var6;
         if (var2) {
            for(var6 = var3.length - 1; var6 >= 0; --var6) {
               if (var3[var6] == var1) {
                  return true;
               }
            }
         } else {
            for(var6 = var3.length - 1; var6 >= 0; --var6) {
               if (var1.equals(var3[var6])) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean containsKey(K var1) {
      return this.locateKey(var1) >= 0;
   }

   @Null
   public K findKey(@Null Object var1, boolean var2) {
      Object[] var3 = this.valueTable;
      if (var1 == null) {
         Object[] var4 = this.keyTable;

         for(int var5 = var3.length - 1; var5 >= 0; --var5) {
            if (var4[var5] != null && var3[var5] == null) {
               return var4[var5];
            }
         }
      } else {
         int var6;
         if (var2) {
            for(var6 = var3.length - 1; var6 >= 0; --var6) {
               if (var3[var6] == var1) {
                  return this.keyTable[var6];
               }
            }
         } else {
            for(var6 = var3.length - 1; var6 >= 0; --var6) {
               if (var1.equals(var3[var6])) {
                  return this.keyTable[var6];
               }
            }
         }
      }

      return null;
   }

   public void ensureCapacity(int var1) {
      int var2 = ObjectSet.tableSize(this.size + var1, this.loadFactor);
      if (this.keyTable.length < var2) {
         this.resize(var2);
      }

   }

   final void resize(int var1) {
      int var2 = this.keyTable.length;
      this.threshold = (int)((float)var1 * this.loadFactor);
      this.mask = var1 - 1;
      this.shift = Long.numberOfLeadingZeros((long)this.mask);
      Object[] var3 = this.keyTable;
      Object[] var4 = this.valueTable;
      this.keyTable = new Object[var1];
      this.valueTable = new Object[var1];
      if (this.size > 0) {
         for(int var5 = 0; var5 < var2; ++var5) {
            Object var6 = var3[var5];
            if (var6 != null) {
               this.putResize(var6, var4[var5]);
            }
         }
      }

   }

   public int hashCode() {
      int var1 = this.size;
      Object[] var2 = this.keyTable;
      Object[] var3 = this.valueTable;
      int var4 = 0;

      for(int var5 = var2.length; var4 < var5; ++var4) {
         Object var6 = var2[var4];
         if (var6 != null) {
            var1 += var6.hashCode();
            Object var7 = var3[var4];
            if (var7 != null) {
               var1 += var7.hashCode();
            }
         }
      }

      return var1;
   }

   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      } else if (!(var1 instanceof ObjectMap)) {
         return false;
      } else {
         ObjectMap var2 = (ObjectMap)var1;
         if (var2.size != this.size) {
            return false;
         } else {
            Object[] var3 = this.keyTable;
            Object[] var4 = this.valueTable;
            int var5 = 0;

            for(int var6 = var3.length; var5 < var6; ++var5) {
               Object var7 = var3[var5];
               if (var7 != null) {
                  Object var8 = var4[var5];
                  if (var8 == null) {
                     if (var2.get(var7, dummy) != null) {
                        return false;
                     }
                  } else if (!var8.equals(var2.get(var7))) {
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
      } else if (!(var1 instanceof ObjectMap)) {
         return false;
      } else {
         ObjectMap var2 = (ObjectMap)var1;
         if (var2.size != this.size) {
            return false;
         } else {
            Object[] var3 = this.keyTable;
            Object[] var4 = this.valueTable;
            int var5 = 0;

            for(int var6 = var3.length; var5 < var6; ++var5) {
               Object var7 = var3[var5];
               if (var7 != null && var4[var5] != var2.get(var7, dummy)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public String toString(String var1) {
      return this.toString(var1, false);
   }

   public String toString() {
      return this.toString(", ", true);
   }

   protected String toString(String var1, boolean var2) {
      if (this.size == 0) {
         return var2 ? "{}" : "";
      } else {
         StringBuilder var3 = new StringBuilder(32);
         if (var2) {
            var3.append('{');
         }

         Object[] var4 = this.keyTable;
         Object[] var5 = this.valueTable;
         int var6 = var4.length;

         Object var7;
         Object var8;
         while(var6-- > 0) {
            var7 = var4[var6];
            if (var7 != null) {
               var3.append(var7 == this ? "(this)" : var7);
               var3.append('=');
               var8 = var5[var6];
               var3.append(var8 == this ? "(this)" : var8);
               break;
            }
         }

         while(var6-- > 0) {
            var7 = var4[var6];
            if (var7 != null) {
               var3.append(var1);
               var3.append(var7 == this ? "(this)" : var7);
               var3.append('=');
               var8 = var5[var6];
               var3.append(var8 == this ? "(this)" : var8);
            }
         }

         if (var2) {
            var3.append('}');
         }

         return var3.toString();
      }
   }

   public Entries<K, V> iterator() {
      return this.entries();
   }

   public Entries<K, V> entries() {
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

   public Keys<K> keys() {
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

   public static class Entries<K, V> extends MapIterator<K, V, Entry<K, V>> {
      Entry<K, V> entry = new Entry();

      public Entries(ObjectMap<K, V> var1) {
         super(var1);
      }

      public Entry<K, V> next() {
         if (!this.hasNext) {
            throw new NoSuchElementException();
         } else if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            Object[] var1 = this.map.keyTable;
            this.entry.key = var1[this.nextIndex];
            this.entry.value = this.map.valueTable[this.nextIndex];
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

      public Entries<K, V> iterator() {
         return this;
      }
   }

   public static class Values<V> extends MapIterator<Object, V, V> {
      public Values(ObjectMap<?, V> var1) {
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
            Object var1 = this.map.valueTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return var1;
         }
      }

      public Values<V> iterator() {
         return this;
      }

      public Array<V> toArray() {
         return this.toArray(new Array(true, this.map.size));
      }

      public Array<V> toArray(Array<V> var1) {
         while(this.hasNext) {
            var1.add(this.next());
         }

         return var1;
      }
   }

   public static class Keys<K> extends MapIterator<K, Object, K> {
      public Keys(ObjectMap<K, ?> var1) {
         super(var1);
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
            Object var1 = this.map.keyTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return var1;
         }
      }

      public Keys<K> iterator() {
         return this;
      }

      public Array<K> toArray() {
         return this.toArray(new Array(true, this.map.size));
      }

      public Array<K> toArray(Array<K> var1) {
         while(this.hasNext) {
            var1.add(this.next());
         }

         return var1;
      }
   }

   private abstract static class MapIterator<K, V, I> implements Iterable<I>, Iterator<I> {
      public boolean hasNext;
      final ObjectMap<K, V> map;
      int nextIndex;
      int currentIndex;
      boolean valid = true;

      public MapIterator(ObjectMap<K, V> var1) {
         this.map = var1;
         this.reset();
      }

      public void reset() {
         this.currentIndex = -1;
         this.nextIndex = -1;
         this.findNextIndex();
      }

      void findNextIndex() {
         Object[] var1 = this.map.keyTable;
         int var2 = var1.length;

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
            Object[] var2 = this.map.keyTable;
            Object[] var3 = this.map.valueTable;
            int var4 = this.map.mask;

            Object var6;
            for(int var5 = var1 + 1 & var4; (var6 = var2[var5]) != null; var5 = var5 + 1 & var4) {
               int var7 = this.map.place(var6);
               if ((var5 - var7 & var4) > (var1 - var7 & var4)) {
                  var2[var1] = var6;
                  var3[var1] = var3[var5];
                  var1 = var5;
               }
            }

            var2[var1] = null;
            var3[var1] = null;
            --this.map.size;
            if (var1 != this.currentIndex) {
               --this.nextIndex;
            }

            this.currentIndex = -1;
         }
      }
   }

   public static class Entry<K, V> {
      public K key;
      @Null
      public V value;

      public Entry() {
      }

      public String toString() {
         return this.key + "=" + this.value;
      }
   }
}
