package zombie.entity.util.assoc;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.BiConsumer;
import zombie.debug.DebugLog;

public class AssocArray<K, V> {
   private static final int DEFAULT_CAPACITY = 10;
   private static final Object[] EMPTY_ELEMENTDATA = new Object[0];
   private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = new Object[0];
   transient Object[] elementData;
   private int size;
   protected transient int modCount = 0;

   public AssocArray(int var1) {
      var1 *= 2;
      if (var1 > 0) {
         this.elementData = new Object[var1];
      } else {
         if (var1 != 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + var1);
         }

         this.elementData = EMPTY_ELEMENTDATA;
      }

   }

   public AssocArray() {
      this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
   }

   public void trimToSize() {
      ++this.modCount;
      int var1 = this.realSize();
      if (var1 < this.elementData.length) {
         this.elementData = var1 == 0 ? EMPTY_ELEMENTDATA : Arrays.copyOf(this.elementData, var1);
      }

   }

   public void ensureCapacity(int var1) {
      if (var1 * 2 > this.elementData.length && (this.elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA || var1 > 10)) {
         ++this.modCount;
         this.grow(var1);
      }

   }

   private Object[] grow(int var1) {
      var1 *= 2;
      int var2 = this.elementData.length;
      if (var2 <= 0 && this.elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
         return this.elementData = new Object[Math.max(10, var1)];
      } else {
         int var3 = Math.max(var1 - var2, var2 >> 1) + var2;
         if (var3 < 0) {
            throw new OutOfMemoryError("Required array length too large");
         } else {
            return this.elementData = Arrays.copyOf(this.elementData, var3);
         }
      }
   }

   private Object[] grow() {
      return this.grow(this.size + 1);
   }

   protected int getBackingSize() {
      return this.elementData.length;
   }

   public int size() {
      return this.size;
   }

   protected int realSize() {
      return this.size * 2;
   }

   protected int realKeyIndex(int var1) {
      return var1 * 2;
   }

   protected int realValueIndex(int var1) {
      return var1 * 2 + 1;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public boolean containsKey(K var1) {
      return this.indexOfKey(var1) >= 0;
   }

   public boolean containsValue(V var1) {
      return this.indexOfValue(var1) >= 0;
   }

   public int indexOfKey(K var1) {
      return this.indexOfRange(var1, 0, this.realSize(), 0);
   }

   public int indexOfValue(V var1) {
      return this.indexOfRange(var1, 0, this.realSize(), 1);
   }

   int indexOfRange(Object var1, int var2, int var3, int var4) {
      Object[] var5 = this.elementData;
      if (var1 != null) {
         for(int var6 = var2 + var4; var6 < var3; var6 += 2) {
            if (var1.equals(var5[var6])) {
               return (var6 - var4) / 2;
            }
         }
      }

      return -1;
   }

   public int lastIndexOfKey(K var1) {
      return this.lastIndexOfRange(var1, 0, this.realSize(), 1);
   }

   public int lastIndexOfValue(V var1) {
      return this.lastIndexOfRange(var1, 0, this.realSize(), 0);
   }

   int lastIndexOfRange(Object var1, int var2, int var3, int var4) {
      Object[] var5 = this.elementData;
      if (var1 != null) {
         for(int var6 = var3 - var4 - 1; var6 >= var2; var6 -= 2) {
            if (var1.equals(var5[var6])) {
               return (var6 - (1 - var4)) / 2;
            }
         }
      }

      return -1;
   }

   K keyData(int var1) {
      return this.elementData[this.realKeyIndex(var1)];
   }

   V valueData(int var1) {
      return this.elementData[this.realValueIndex(var1)];
   }

   static <E> E valueAt(Object[] var0, int var1) {
      return var0[var1 * 2 + 1];
   }

   static <E> E keyAt(Object[] var0, int var1) {
      return var0[var1 * 2];
   }

   public K getKey(int var1) {
      Objects.checkIndex(var1, this.size);
      return this.keyData(var1);
   }

   public V getValue(int var1) {
      Objects.checkIndex(var1, this.size);
      return this.valueData(var1);
   }

   public V set(K var1, V var2) {
      Objects.requireNonNull(var1);
      Objects.requireNonNull(var2);
      int var3 = this.indexOfKey(var1);
      Objects.checkIndex(var3, this.size);
      return this.setInternal(var3, var2);
   }

   private V setInternal(int var1, V var2) {
      Object var3 = this.valueData(var1);
      this.elementData[this.realValueIndex(var1)] = var2;
      return var3;
   }

   public V put(K var1, V var2) {
      Objects.requireNonNull(var1);
      Objects.requireNonNull(var2);
      int var3 = this.indexOfKey(var1);
      if (var3 >= 0) {
         return this.setInternal(var3, var2);
      } else {
         this.add(var1, var2);
         return null;
      }
   }

   public V get(K var1) {
      int var2 = this.indexOfKey(var1);
      if (var2 < 0) {
         return null;
      } else {
         Object var3;
         return (var3 = this.getValue(var2)) == null ? null : var3;
      }
   }

   private void add(K var1, V var2, Object[] var3, int var4) {
      if (var4 * 2 + 1 >= var3.length) {
         var3 = this.grow();
      }

      var3[this.realKeyIndex(var4)] = var1;
      var3[this.realValueIndex(var4)] = var2;
      this.size = var4 + 1;
   }

   public boolean add(K var1, V var2) {
      Objects.requireNonNull(var1);
      Objects.requireNonNull(var2);
      if (this.containsKey(var1)) {
         throw new UnsupportedOperationException("Key already exists.");
      } else {
         ++this.modCount;
         this.add(var1, var2, this.elementData, this.size);
         return true;
      }
   }

   public void add(int var1, K var2, V var3) {
      Objects.requireNonNull(var2);
      Objects.requireNonNull(var3);
      if (this.containsKey(var2)) {
         throw new UnsupportedOperationException("Key already exists.");
      } else {
         this.rangeCheckForAdd(var1);
         ++this.modCount;
         int var4 = this.realSize();
         int var5 = this.realKeyIndex(var1);
         Object[] var6;
         if (var4 == (var6 = this.elementData).length) {
            var6 = this.grow();
         }

         System.arraycopy(var6, var5, var6, var5 + 2, var4 - var5);
         var6[var5] = var2;
         var6[var5 + 1] = var3;
         ++this.size;
      }
   }

   public V removeIndex(int var1) {
      Objects.checkIndex(var1, this.size);
      Object[] var2 = this.elementData;
      Object var3 = var2[this.realValueIndex(var1)];
      this.fastRemove(var2, this.realKeyIndex(var1));
      return var3;
   }

   public boolean equals(Object var1) {
      return var1.getClass() == AssocArray.class && var1 == this;
   }

   private void checkForComodification(int var1) {
      if (this.modCount != var1) {
         throw new ConcurrentModificationException();
      }
   }

   public int hashCode() {
      int var1 = this.modCount;
      int var2 = this.hashCodeRange(0, this.realSize());
      this.checkForComodification(var1);
      return var2;
   }

   int hashCodeRange(int var1, int var2) {
      Object[] var3 = this.elementData;
      if (var2 > var3.length) {
         throw new ConcurrentModificationException();
      } else {
         int var4 = 1;

         for(int var5 = var1; var5 < var2; ++var5) {
            Object var6 = var3[var5];
            var4 = 31 * var4 + (var6 == null ? 0 : var6.hashCode());
         }

         return var4;
      }
   }

   public V remove(K var1) {
      Object[] var2 = this.elementData;
      int var3 = this.realSize();
      int var5 = 0;
      if (var1 != null) {
         while(var5 < var3) {
            if (var1.equals(var2[var5])) {
               Object var4 = var2[var5 + 1];
               this.fastRemove(var2, var5);
               return var4;
            }

            var5 += 2;
         }
      }

      return null;
   }

   protected void fastRemove(Object[] var1, int var2) {
      ++this.modCount;
      int var3;
      if ((var3 = this.size - 1) > var2 / 2) {
         System.arraycopy(var1, var2 + 2, var1, var2, var3 * 2 - var2);
      }

      this.size = var3;
      var1[this.size * 2] = null;
      var1[this.size * 2 + 1] = null;
   }

   public void clear() {
      ++this.modCount;
      Object[] var1 = this.elementData;
      int var2 = this.realSize();

      for(int var3 = this.size = 0; var3 < var2; ++var3) {
         var1[var3] = null;
      }

   }

   public void putAll(AssocArray<K, V> var1) {
      for(int var2 = 0; var2 < var1.size; ++var2) {
         Object var3 = var1.getKey(var2);
         Object var4 = var1.getValue(var2);
         if (this.containsKey(var3)) {
            this.put(var3, var4);
         } else {
            this.add(var3, var4);
         }
      }

   }

   public void addAll(AssocArray<K, V> var1) {
      for(int var2 = 0; var2 < var1.size; ++var2) {
         Object var3 = var1.getKey(var2);
         Object var4 = var1.getValue(var2);
         if (!this.containsKey(var3)) {
            this.add(var3, var4);
         }
      }

   }

   public void setAll(AssocArray<K, V> var1) {
      for(int var2 = 0; var2 < var1.size; ++var2) {
         Object var3 = var1.getKey(var2);
         Object var4 = var1.getValue(var2);
         if (this.containsKey(var3)) {
            this.set(var3, var4);
         }
      }

   }

   private void rangeCheckForAdd(int var1) {
      if (var1 > this.size || var1 < 0) {
         throw new IndexOutOfBoundsException(this.outOfBoundsMsg(var1));
      }
   }

   private String outOfBoundsMsg(int var1) {
      return "Index: " + var1 + ", Size: " + this.size;
   }

   private static String outOfBoundsMsg(int var0, int var1) {
      return "From Index: " + var0 + " > To Index: " + var1;
   }

   public void forEach(BiConsumer<? super K, ? super V> var1) {
      Objects.requireNonNull(var1);
      int var2 = this.modCount;
      Object[] var3 = this.elementData;
      int var4 = this.size;

      for(int var5 = 0; this.modCount == var2 && var5 < var4; ++var5) {
         var1.accept(keyAt(var3, var5), valueAt(var3, var5));
      }

      if (this.modCount != var2) {
         throw new ConcurrentModificationException();
      }
   }

   protected void debugPrint() {
      DebugLog.log("--- Contents ---");
      int var10000 = this.size;
      DebugLog.log("Size = " + var10000 + ", real size = " + this.realSize());
      DebugLog.log("backing array size = " + this.elementData.length);

      for(int var1 = 0; var1 < this.size; ++var1) {
         DebugLog.log("[" + var1 + "][" + this.realKeyIndex(var1) + "] " + this.elementData[this.realKeyIndex(var1)]);
         var10000 = this.realValueIndex(var1);
         DebugLog.log("   [" + var10000 + "] " + this.elementData[this.realValueIndex(var1)]);
      }

   }
}
