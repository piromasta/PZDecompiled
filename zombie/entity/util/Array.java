package zombie.entity.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import zombie.core.random.Rand;

public class Array<T> implements Iterable<T> {
   public T[] items;
   public int size;
   public boolean ordered;
   private ArrayIterable iterable;
   private Predicate.PredicateIterable<T> predicateIterable;

   public Array() {
      this(true, 16);
   }

   public Array(int var1) {
      this(true, var1);
   }

   public Array(boolean var1, int var2) {
      this.ordered = var1;
      this.items = new Object[var2];
   }

   public Array(boolean var1, int var2, Class var3) {
      this.ordered = var1;
      this.items = (Object[])ArrayReflection.newInstance(var3, var2);
   }

   public Array(Class var1) {
      this(true, 16, var1);
   }

   public Array(Array<? extends T> var1) {
      this(var1.ordered, var1.size, var1.items.getClass().getComponentType());
      this.size = var1.size;
      System.arraycopy(var1.items, 0, this.items, 0, this.size);
   }

   public Array(T[] var1) {
      this(true, var1, 0, var1.length);
   }

   public Array(boolean var1, T[] var2, int var3, int var4) {
      this(var1, var4, var2.getClass().getComponentType());
      this.size = var4;
      System.arraycopy(var2, var3, this.items, 0, this.size);
   }

   public void add(T var1) {
      Object[] var2 = this.items;
      if (this.size == var2.length) {
         var2 = this.resize(Math.max(8, (int)((float)this.size * 1.75F)));
      }

      var2[this.size++] = var1;
   }

   public void add(T var1, T var2) {
      Object[] var3 = this.items;
      if (this.size + 1 >= var3.length) {
         var3 = this.resize(Math.max(8, (int)((float)this.size * 1.75F)));
      }

      var3[this.size] = var1;
      var3[this.size + 1] = var2;
      this.size += 2;
   }

   public void add(T var1, T var2, T var3) {
      Object[] var4 = this.items;
      if (this.size + 2 >= var4.length) {
         var4 = this.resize(Math.max(8, (int)((float)this.size * 1.75F)));
      }

      var4[this.size] = var1;
      var4[this.size + 1] = var2;
      var4[this.size + 2] = var3;
      this.size += 3;
   }

   public void add(T var1, T var2, T var3, T var4) {
      Object[] var5 = this.items;
      if (this.size + 3 >= var5.length) {
         var5 = this.resize(Math.max(8, (int)((float)this.size * 1.8F)));
      }

      var5[this.size] = var1;
      var5[this.size + 1] = var2;
      var5[this.size + 2] = var3;
      var5[this.size + 3] = var4;
      this.size += 4;
   }

   public void addAll(Array<? extends T> var1) {
      this.addAll((Object[])var1.items, 0, var1.size);
   }

   public void addAll(Array<? extends T> var1, int var2, int var3) {
      if (var2 + var3 > var1.size) {
         throw new IllegalArgumentException("start + count must be <= size: " + var2 + " + " + var3 + " <= " + var1.size);
      } else {
         this.addAll(var1.items, var2, var3);
      }
   }

   public void addAll(T... var1) {
      this.addAll((Object[])var1, 0, var1.length);
   }

   public void addAll(T[] var1, int var2, int var3) {
      Object[] var4 = this.items;
      int var5 = this.size + var3;
      if (var5 > var4.length) {
         var4 = this.resize(Math.max(Math.max(8, var5), (int)((float)this.size * 1.75F)));
      }

      System.arraycopy(var1, var2, var4, this.size, var3);
      this.size = var5;
   }

   public T get(int var1) {
      if (var1 >= this.size) {
         throw new IndexOutOfBoundsException("index can't be >= size: " + var1 + " >= " + this.size);
      } else {
         return this.items[var1];
      }
   }

   public void set(int var1, T var2) {
      if (var1 >= this.size) {
         throw new IndexOutOfBoundsException("index can't be >= size: " + var1 + " >= " + this.size);
      } else {
         this.items[var1] = var2;
      }
   }

   public void insert(int var1, T var2) {
      if (var1 > this.size) {
         throw new IndexOutOfBoundsException("index can't be > size: " + var1 + " > " + this.size);
      } else {
         Object[] var3 = this.items;
         if (this.size == var3.length) {
            var3 = this.resize(Math.max(8, (int)((float)this.size * 1.75F)));
         }

         if (this.ordered) {
            System.arraycopy(var3, var1, var3, var1 + 1, this.size - var1);
         } else {
            var3[this.size] = var3[var1];
         }

         ++this.size;
         var3[var1] = var2;
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
         Object[] var3 = this.items;
         Object var4 = var3[var1];
         var3[var1] = var3[var2];
         var3[var2] = var4;
      }
   }

   public boolean contains(@Null T var1, boolean var2) {
      Object[] var3 = this.items;
      int var4 = this.size - 1;
      if (!var2 && var1 != null) {
         while(var4 >= 0) {
            if (var1.equals(var3[var4--])) {
               return true;
            }
         }
      } else {
         while(var4 >= 0) {
            if (var3[var4--] == var1) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean containsAll(Array<? extends T> var1, boolean var2) {
      Object[] var3 = var1.items;
      int var4 = 0;

      for(int var5 = var1.size; var4 < var5; ++var4) {
         if (!this.contains(var3[var4], var2)) {
            return false;
         }
      }

      return true;
   }

   public boolean containsAny(Array<? extends T> var1, boolean var2) {
      Object[] var3 = var1.items;
      int var4 = 0;

      for(int var5 = var1.size; var4 < var5; ++var4) {
         if (this.contains(var3[var4], var2)) {
            return true;
         }
      }

      return false;
   }

   public int indexOf(@Null T var1, boolean var2) {
      Object[] var3 = this.items;
      int var4;
      int var5;
      if (!var2 && var1 != null) {
         var4 = 0;

         for(var5 = this.size; var4 < var5; ++var4) {
            if (var1.equals(var3[var4])) {
               return var4;
            }
         }
      } else {
         var4 = 0;

         for(var5 = this.size; var4 < var5; ++var4) {
            if (var3[var4] == var1) {
               return var4;
            }
         }
      }

      return -1;
   }

   public int lastIndexOf(@Null T var1, boolean var2) {
      Object[] var3 = this.items;
      int var4;
      if (!var2 && var1 != null) {
         for(var4 = this.size - 1; var4 >= 0; --var4) {
            if (var1.equals(var3[var4])) {
               return var4;
            }
         }
      } else {
         for(var4 = this.size - 1; var4 >= 0; --var4) {
            if (var3[var4] == var1) {
               return var4;
            }
         }
      }

      return -1;
   }

   public boolean removeValue(@Null T var1, boolean var2) {
      Object[] var3 = this.items;
      int var4;
      int var5;
      if (!var2 && var1 != null) {
         var4 = 0;

         for(var5 = this.size; var4 < var5; ++var4) {
            if (var1.equals(var3[var4])) {
               this.removeIndex(var4);
               return true;
            }
         }
      } else {
         var4 = 0;

         for(var5 = this.size; var4 < var5; ++var4) {
            if (var3[var4] == var1) {
               this.removeIndex(var4);
               return true;
            }
         }
      }

      return false;
   }

   public T removeIndex(int var1) {
      if (var1 >= this.size) {
         throw new IndexOutOfBoundsException("index can't be >= size: " + var1 + " >= " + this.size);
      } else {
         Object[] var2 = this.items;
         Object var3 = var2[var1];
         --this.size;
         if (this.ordered) {
            System.arraycopy(var2, var1 + 1, var2, var1, this.size - var1);
         } else {
            var2[var1] = var2[this.size];
         }

         var2[this.size] = null;
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
         Object[] var4 = this.items;
         int var5 = var2 - var1 + 1;
         int var6 = var3 - var5;
         int var7;
         if (this.ordered) {
            System.arraycopy(var4, var1 + var5, var4, var1, var3 - (var1 + var5));
         } else {
            var7 = Math.max(var6, var2 + 1);
            System.arraycopy(var4, var7, var4, var1, var3 - var7);
         }

         for(var7 = var6; var7 < var3; ++var7) {
            var4[var7] = null;
         }

         this.size = var3 - var5;
      }
   }

   public boolean removeAll(Array<? extends T> var1, boolean var2) {
      int var3 = this.size;
      int var4 = var3;
      Object[] var5 = this.items;
      int var6;
      int var7;
      Object var8;
      int var9;
      if (var2) {
         var6 = 0;

         for(var7 = var1.size; var6 < var7; ++var6) {
            var8 = var1.get(var6);

            for(var9 = 0; var9 < var3; ++var9) {
               if (var8 == var5[var9]) {
                  this.removeIndex(var9);
                  --var3;
                  break;
               }
            }
         }
      } else {
         var6 = 0;

         for(var7 = var1.size; var6 < var7; ++var6) {
            var8 = var1.get(var6);

            for(var9 = 0; var9 < var3; ++var9) {
               if (var8.equals(var5[var9])) {
                  this.removeIndex(var9);
                  --var3;
                  break;
               }
            }
         }
      }

      return var3 != var4;
   }

   public T pop() {
      if (this.size == 0) {
         throw new IllegalStateException("Array is empty.");
      } else {
         --this.size;
         Object var1 = this.items[this.size];
         this.items[this.size] = null;
         return var1;
      }
   }

   public T peek() {
      if (this.size == 0) {
         throw new IllegalStateException("Array is empty.");
      } else {
         return this.items[this.size - 1];
      }
   }

   public T first() {
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
      Arrays.fill(this.items, 0, this.size, (Object)null);
      this.size = 0;
   }

   public T[] shrink() {
      if (this.items.length != this.size) {
         this.resize(this.size);
      }

      return this.items;
   }

   public T[] ensureCapacity(int var1) {
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

   public T[] setSize(int var1) {
      this.truncate(var1);
      if (var1 > this.items.length) {
         this.resize(Math.max(8, var1));
      }

      this.size = var1;
      return this.items;
   }

   protected T[] resize(int var1) {
      Object[] var2 = this.items;
      Object[] var3 = (Object[])ArrayReflection.newInstance(var2.getClass().getComponentType(), var1);
      System.arraycopy(var2, 0, var3, 0, Math.min(this.size, var3.length));
      this.items = var3;
      return var3;
   }

   public void sort() {
      Sort.instance().sort(this.items, 0, this.size);
   }

   public void sort(Comparator<? super T> var1) {
      Sort.instance().sort(this.items, var1, 0, this.size);
   }

   public T selectRanked(Comparator<T> var1, int var2) {
      if (var2 < 1) {
         throw new RuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
      } else {
         return Select.instance().select(this.items, var1, var2, this.size);
      }
   }

   public int selectRankedIndex(Comparator<T> var1, int var2) {
      if (var2 < 1) {
         throw new RuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
      } else {
         return Select.instance().selectIndex(this.items, var1, var2, this.size);
      }
   }

   public void reverse() {
      Object[] var1 = this.items;
      int var2 = 0;
      int var3 = this.size - 1;

      for(int var4 = this.size / 2; var2 < var4; ++var2) {
         int var5 = var3 - var2;
         Object var6 = var1[var2];
         var1[var2] = var1[var5];
         var1[var5] = var6;
      }

   }

   public void shuffle() {
      Object[] var1 = this.items;

      for(int var2 = this.size - 1; var2 >= 0; --var2) {
         int var3 = Rand.Next(var2);
         Object var4 = var1[var2];
         var1[var2] = var1[var3];
         var1[var3] = var4;
      }

   }

   public ArrayIterator<T> iterator() {
      if (Collections.allocateIterators) {
         return new ArrayIterator(this, true);
      } else {
         if (this.iterable == null) {
            this.iterable = new ArrayIterable(this);
         }

         return this.iterable.iterator();
      }
   }

   public Iterable<T> select(Predicate<T> var1) {
      if (Collections.allocateIterators) {
         return new Predicate.PredicateIterable(this, var1);
      } else {
         if (this.predicateIterable == null) {
            this.predicateIterable = new Predicate.PredicateIterable(this, var1);
         } else {
            this.predicateIterable.set(this, var1);
         }

         return this.predicateIterable;
      }
   }

   public void truncate(int var1) {
      if (var1 < 0) {
         throw new IllegalArgumentException("newSize must be >= 0: " + var1);
      } else if (this.size > var1) {
         for(int var2 = var1; var2 < this.size; ++var2) {
            this.items[var2] = null;
         }

         this.size = var1;
      }
   }

   @Null
   public T random() {
      return this.size == 0 ? null : this.items[Rand.Next(0, this.size - 1)];
   }

   public T[] toArray() {
      return this.toArray(this.items.getClass().getComponentType());
   }

   public <V> V[] toArray(Class<V> var1) {
      Object[] var2 = (Object[])ArrayReflection.newInstance(var1, this.size);
      System.arraycopy(this.items, 0, var2, 0, this.size);
      return var2;
   }

   public int hashCode() {
      if (!this.ordered) {
         return super.hashCode();
      } else {
         Object[] var1 = this.items;
         int var2 = 1;
         int var3 = 0;

         for(int var4 = this.size; var3 < var4; ++var3) {
            var2 *= 31;
            Object var5 = var1[var3];
            if (var5 != null) {
               var2 += var5.hashCode();
            }
         }

         return var2;
      }
   }

   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      } else if (!this.ordered) {
         return false;
      } else if (!(var1 instanceof Array)) {
         return false;
      } else {
         Array var2 = (Array)var1;
         if (!var2.ordered) {
            return false;
         } else {
            int var3 = this.size;
            if (var3 != var2.size) {
               return false;
            } else {
               Object[] var4 = this.items;
               Object[] var5 = var2.items;
               int var6 = 0;

               while(true) {
                  if (var6 >= var3) {
                     return true;
                  }

                  Object var7 = var4[var6];
                  Object var8 = var5[var6];
                  if (var7 == null) {
                     if (var8 != null) {
                        break;
                     }
                  } else if (!var7.equals(var8)) {
                     break;
                  }

                  ++var6;
               }

               return false;
            }
         }
      }
   }

   public boolean equalsIdentity(Object var1) {
      if (var1 == this) {
         return true;
      } else if (!this.ordered) {
         return false;
      } else if (!(var1 instanceof Array)) {
         return false;
      } else {
         Array var2 = (Array)var1;
         if (!var2.ordered) {
            return false;
         } else {
            int var3 = this.size;
            if (var3 != var2.size) {
               return false;
            } else {
               Object[] var4 = this.items;
               Object[] var5 = var2.items;

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
         Object[] var1 = this.items;
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
         Object[] var2 = this.items;
         StringBuilder var3 = new StringBuilder(32);
         var3.append(var2[0]);

         for(int var4 = 1; var4 < this.size; ++var4) {
            var3.append(var1);
            var3.append(var2[var4]);
         }

         return var3.toString();
      }
   }

   public static <T> Array<T> of(Class<T> var0) {
      return new Array(var0);
   }

   public static <T> Array<T> of(boolean var0, int var1, Class<T> var2) {
      return new Array(var0, var1, var2);
   }

   public static <T> Array<T> with(T... var0) {
      return new Array(var0);
   }

   public static class ArrayIterator<T> implements Iterator<T>, Iterable<T> {
      private final Array<T> array;
      private final boolean allowRemove;
      int index;
      boolean valid;

      public ArrayIterator(Array<T> var1) {
         this(var1, true);
      }

      public ArrayIterator(Array<T> var1, boolean var2) {
         this.valid = true;
         this.array = var1;
         this.allowRemove = var2;
      }

      public boolean hasNext() {
         if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            return this.index < this.array.size;
         }
      }

      public T next() {
         if (this.index >= this.array.size) {
            throw new NoSuchElementException(String.valueOf(this.index));
         } else if (!this.valid) {
            throw new RuntimeException("#iterator() cannot be used nested.");
         } else {
            return this.array.items[this.index++];
         }
      }

      public void remove() {
         if (!this.allowRemove) {
            throw new RuntimeException("Remove not allowed.");
         } else {
            --this.index;
            this.array.removeIndex(this.index);
         }
      }

      public void reset() {
         this.index = 0;
      }

      public ArrayIterator<T> iterator() {
         return this;
      }
   }

   public static class ArrayIterable<T> implements Iterable<T> {
      private final Array<T> array;
      private final boolean allowRemove;
      private ArrayIterator iterator1;
      private ArrayIterator iterator2;

      public ArrayIterable(Array<T> var1) {
         this(var1, true);
      }

      public ArrayIterable(Array<T> var1, boolean var2) {
         this.array = var1;
         this.allowRemove = var2;
      }

      public ArrayIterator<T> iterator() {
         if (Collections.allocateIterators) {
            return new ArrayIterator(this.array, this.allowRemove);
         } else {
            if (this.iterator1 == null) {
               this.iterator1 = new ArrayIterator(this.array, this.allowRemove);
               this.iterator2 = new ArrayIterator(this.array, this.allowRemove);
            }

            if (!this.iterator1.valid) {
               this.iterator1.index = 0;
               this.iterator1.valid = true;
               this.iterator2.valid = false;
               return this.iterator1;
            } else {
               this.iterator2.index = 0;
               this.iterator2.valid = true;
               this.iterator1.valid = false;
               return this.iterator2;
            }
         }
      }
   }
}
