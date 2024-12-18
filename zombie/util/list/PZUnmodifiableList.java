package zombie.util.list;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class PZUnmodifiableList<E> extends PZUnmodifiableCollection<E> implements List<E> {
   final List<? extends E> list;

   public static <T> List<T> wrap(List<? extends T> var0) {
      return (List)(var0.getClass() == PZUnmodifiableList.class ? var0 : new PZUnmodifiableList(var0));
   }

   PZUnmodifiableList(List<? extends E> var1) {
      super(var1);
      this.list = var1;
   }

   public boolean equals(Object var1) {
      return var1 == this || this.list.equals(var1);
   }

   public int hashCode() {
      return this.list.hashCode();
   }

   public E get(int var1) {
      return this.list.get(var1);
   }

   public E set(int var1, E var2) {
      throw new UnsupportedOperationException();
   }

   public void add(int var1, E var2) {
      throw new UnsupportedOperationException();
   }

   public E remove(int var1) {
      throw new UnsupportedOperationException();
   }

   public int indexOf(Object var1) {
      return this.list.indexOf(var1);
   }

   public int lastIndexOf(Object var1) {
      return this.list.lastIndexOf(var1);
   }

   public boolean addAll(int var1, Collection<? extends E> var2) {
      throw new UnsupportedOperationException();
   }

   public void replaceAll(UnaryOperator<E> var1) {
      throw new UnsupportedOperationException();
   }

   public void sort(Comparator<? super E> var1) {
      throw new UnsupportedOperationException();
   }

   public ListIterator<E> listIterator() {
      return this.listIterator(0);
   }

   public ListIterator<E> listIterator(final int var1) {
      return new ListIterator<E>() {
         private final ListIterator<? extends E> i;

         {
            this.i = PZUnmodifiableList.this.list.listIterator(var1);
         }

         public boolean hasNext() {
            return this.i.hasNext();
         }

         public E next() {
            return this.i.next();
         }

         public boolean hasPrevious() {
            return this.i.hasPrevious();
         }

         public E previous() {
            return this.i.previous();
         }

         public int nextIndex() {
            return this.i.nextIndex();
         }

         public int previousIndex() {
            return this.i.previousIndex();
         }

         public void remove() {
            throw new UnsupportedOperationException();
         }

         public void set(E var1x) {
            throw new UnsupportedOperationException();
         }

         public void add(E var1x) {
            throw new UnsupportedOperationException();
         }

         public void forEachRemaining(Consumer<? super E> var1x) {
            this.i.forEachRemaining(var1x);
         }
      };
   }

   public List<E> subList(int var1, int var2) {
      return new PZUnmodifiableList(this.list.subList(var1, var2));
   }
}
