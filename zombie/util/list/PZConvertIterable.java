package zombie.util.list;

import java.util.Iterator;
import java.util.function.Function;

public final class PZConvertIterable<T, S> implements Iterable<T> {
   private final Iterable<S> m_srcIterable;
   private final Function<S, T> m_converter;

   public PZConvertIterable(Iterable<S> var1, Function<S, T> var2) {
      this.m_srcIterable = var1;
      this.m_converter = var2;
   }

   public Iterator<T> iterator() {
      return new Iterator<T>() {
         private Iterator<S> m_srcIterator;

         {
            this.m_srcIterator = PZConvertIterable.this.m_srcIterable.iterator();
         }

         public boolean hasNext() {
            return this.m_srcIterator.hasNext();
         }

         public T next() {
            return PZConvertIterable.this.m_converter.apply(this.m_srcIterator.next());
         }
      };
   }
}
