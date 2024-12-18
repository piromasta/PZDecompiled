package zombie.entity.util;

import java.util.Iterator;

public interface Predicate<T> {
   boolean evaluate(T var1);

   public static class PredicateIterable<T> implements Iterable<T> {
      public Iterable<T> iterable;
      public Predicate<T> predicate;
      public PredicateIterator<T> iterator = null;

      public PredicateIterable(Iterable<T> var1, Predicate<T> var2) {
         this.set(var1, var2);
      }

      public void set(Iterable<T> var1, Predicate<T> var2) {
         this.iterable = var1;
         this.predicate = var2;
      }

      public Iterator<T> iterator() {
         if (Collections.allocateIterators) {
            return new PredicateIterator(this.iterable.iterator(), this.predicate);
         } else {
            if (this.iterator == null) {
               this.iterator = new PredicateIterator(this.iterable.iterator(), this.predicate);
            } else {
               this.iterator.set(this.iterable.iterator(), this.predicate);
            }

            return this.iterator;
         }
      }
   }

   public static class PredicateIterator<T> implements Iterator<T> {
      public Iterator<T> iterator;
      public Predicate<T> predicate;
      public boolean end;
      public boolean peeked;
      public T next;

      public PredicateIterator(Iterable<T> var1, Predicate<T> var2) {
         this(var1.iterator(), var2);
      }

      public PredicateIterator(Iterator<T> var1, Predicate<T> var2) {
         this.end = false;
         this.peeked = false;
         this.next = null;
         this.set(var1, var2);
      }

      public void set(Iterable<T> var1, Predicate<T> var2) {
         this.set(var1.iterator(), var2);
      }

      public void set(Iterator<T> var1, Predicate<T> var2) {
         this.iterator = var1;
         this.predicate = var2;
         this.end = this.peeked = false;
         this.next = null;
      }

      public boolean hasNext() {
         if (this.end) {
            return false;
         } else if (this.next != null) {
            return true;
         } else {
            this.peeked = true;

            Object var1;
            do {
               if (!this.iterator.hasNext()) {
                  this.end = true;
                  return false;
               }

               var1 = this.iterator.next();
            } while(!this.predicate.evaluate(var1));

            this.next = var1;
            return true;
         }
      }

      public T next() {
         if (this.next == null && !this.hasNext()) {
            return null;
         } else {
            Object var1 = this.next;
            this.next = null;
            this.peeked = false;
            return var1;
         }
      }

      public void remove() {
         if (this.peeked) {
            throw new RuntimeException("Cannot remove between a call to hasNext() and next().");
         } else {
            this.iterator.remove();
         }
      }
   }
}
