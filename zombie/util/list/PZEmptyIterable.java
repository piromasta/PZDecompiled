package zombie.util.list;

import java.util.Iterator;

public final class PZEmptyIterable<T> implements Iterable<T> {
   private static final PZEmptyIterable<Object> instance = new PZEmptyIterable();
   private final Iterator<T> s_it = new Iterator<T>() {
      public boolean hasNext() {
         return false;
      }

      public T next() {
         throw new ArrayIndexOutOfBoundsException("Empty Iterator. Has no data.");
      }
   };

   private PZEmptyIterable() {
   }

   public static <E> PZEmptyIterable<E> getInstance() {
      return instance;
   }

   public Iterator<T> iterator() {
      return this.s_it;
   }
}
