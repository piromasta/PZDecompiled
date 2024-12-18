package zombie.util.list;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PZUnmodifiableCollection<E> implements Collection<E> {
   final Collection<? extends E> c;

   PZUnmodifiableCollection(Collection<? extends E> var1) {
      if (var1 == null) {
         throw new NullPointerException();
      } else {
         this.c = var1;
      }
   }

   public int size() {
      return this.c.size();
   }

   public boolean isEmpty() {
      return this.c.isEmpty();
   }

   public boolean contains(Object var1) {
      return this.c.contains(var1);
   }

   public Object[] toArray() {
      return this.c.toArray();
   }

   public <T> T[] toArray(T[] var1) {
      return this.c.toArray(var1);
   }

   public <T> T[] toArray(IntFunction<T[]> var1) {
      return this.c.toArray(var1);
   }

   public String toString() {
      return this.c.toString();
   }

   public Iterator<E> iterator() {
      return new Iterator<E>() {
         private final Iterator<? extends E> i;

         {
            this.i = PZUnmodifiableCollection.this.c.iterator();
         }

         public boolean hasNext() {
            return this.i.hasNext();
         }

         public E next() {
            return this.i.next();
         }

         public void remove() {
            throw new UnsupportedOperationException();
         }

         public void forEachRemaining(Consumer<? super E> var1) {
            this.i.forEachRemaining(var1);
         }
      };
   }

   public boolean add(E var1) {
      throw new UnsupportedOperationException();
   }

   public boolean remove(Object var1) {
      throw new UnsupportedOperationException();
   }

   public boolean containsAll(Collection<?> var1) {
      return this.c.containsAll(var1);
   }

   public boolean addAll(Collection<? extends E> var1) {
      throw new UnsupportedOperationException();
   }

   public boolean removeAll(Collection<?> var1) {
      throw new UnsupportedOperationException();
   }

   public boolean retainAll(Collection<?> var1) {
      throw new UnsupportedOperationException();
   }

   public void clear() {
      throw new UnsupportedOperationException();
   }

   public void forEach(Consumer<? super E> var1) {
      this.c.forEach(var1);
   }

   public boolean removeIf(Predicate<? super E> var1) {
      throw new UnsupportedOperationException();
   }

   public Spliterator<E> spliterator() {
      return this.c.spliterator();
   }

   public Stream<E> stream() {
      return this.c.stream();
   }

   public Stream<E> parallelStream() {
      return this.c.parallelStream();
   }
}
