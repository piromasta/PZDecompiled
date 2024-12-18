package zombie.entity.util;

import java.util.Iterator;

public class ImmutableArray<T> implements Iterable<T> {
   private final Array<T> array;
   private Array.ArrayIterable<T> iterable;

   public ImmutableArray(Array<T> var1) {
      this.array = var1;
   }

   public int size() {
      return this.array.size;
   }

   public T get(int var1) {
      return this.array.get(var1);
   }

   public boolean contains(T var1, boolean var2) {
      return this.array.contains(var1, var2);
   }

   public int indexOf(T var1, boolean var2) {
      return this.array.indexOf(var1, var2);
   }

   public int lastIndexOf(T var1, boolean var2) {
      return this.array.lastIndexOf(var1, var2);
   }

   public T peek() {
      return this.array.peek();
   }

   public T first() {
      return this.array.first();
   }

   public T random() {
      return this.array.random();
   }

   public T[] toArray() {
      return this.array.toArray();
   }

   public <V> V[] toArray(Class<V> var1) {
      return this.array.toArray(var1);
   }

   public int hashCode() {
      return this.array.hashCode();
   }

   public boolean equals(Object var1) {
      return this.array.equals(var1);
   }

   public String toString() {
      return this.array.toString();
   }

   public String toString(String var1) {
      return this.array.toString(var1);
   }

   public Iterator<T> iterator() {
      if (this.iterable == null) {
         this.iterable = new Array.ArrayIterable(this.array, false);
      }

      return this.iterable.iterator();
   }
}
