package zombie.util.list;

import java.util.Iterator;

public final class PZPrimitiveArrayIterable {
   public PZPrimitiveArrayIterable() {
   }

   public static Iterable<Float> fromArray(final float[] var0) {
      return new Iterable<Float>() {
         private final float[] m_list = var0;

         public Iterator<Float> iterator() {
            return new Iterator<Float>() {
               private int pos = 0;

               public boolean hasNext() {
                  return m_list.length > this.pos;
               }

               public Float next() {
                  return m_list[this.pos++];
               }
            };
         }
      };
   }
}
