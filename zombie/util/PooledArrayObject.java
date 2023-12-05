package zombie.util;

import java.util.function.Function;

public class PooledArrayObject<T> extends PooledObject {
   private T[] m_array = null;

   public PooledArrayObject() {
   }

   public T[] array() {
      return this.m_array;
   }

   public int length() {
      return this.m_array.length;
   }

   public T get(int var1) {
      return this.m_array[var1];
   }

   public void set(int var1, T var2) {
      this.m_array[var1] = var2;
   }

   protected void initCapacity(int var1, Function<Integer, T[]> var2) {
      if (this.m_array == null || this.m_array.length != var1) {
         this.m_array = (Object[])var2.apply(var1);
      }

   }

   public boolean isEmpty() {
      return this.m_array == null || this.m_array.length == 0;
   }
}
