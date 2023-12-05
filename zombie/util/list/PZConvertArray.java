package zombie.util.list;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class PZConvertArray<S, T> extends AbstractList<T> implements RandomAccess {
   private final S[] m_array;
   private final Function<S, T> m_converterST;
   private final Function<T, S> m_converterTS;

   public PZConvertArray(S[] var1, Function<S, T> var2) {
      this(var1, var2, (Function)null);
   }

   public PZConvertArray(S[] var1, Function<S, T> var2, Function<T, S> var3) {
      this.m_array = (Object[])Objects.requireNonNull(var1);
      this.m_converterST = var2;
      this.m_converterTS = var3;
   }

   public boolean isReadonly() {
      return this.m_converterTS == null;
   }

   public int size() {
      return this.m_array.length;
   }

   public Object[] toArray() {
      return Arrays.asList(this.m_array).toArray();
   }

   public <R> R[] toArray(R[] var1) {
      int var2 = this.size();

      for(int var3 = 0; var3 < var2 && var3 < var1.length; ++var3) {
         Object var4 = this.get(var3);
         var1[var3] = var4;
      }

      if (var1.length > var2) {
         var1[var2] = null;
      }

      return var1;
   }

   public T get(int var1) {
      return this.convertST(this.m_array[var1]);
   }

   public T set(int var1, T var2) {
      Object var3 = this.get(var1);
      this.setS(var1, this.convertTS(var2));
      return var3;
   }

   public S setS(int var1, S var2) {
      Object var3 = this.m_array[var1];
      this.m_array[var1] = var2;
      return var3;
   }

   public int indexOf(Object var1) {
      int var2 = -1;
      int var3 = 0;

      for(int var4 = this.size(); var3 < var4; ++var3) {
         if (objectsEqual(var1, this.get(var3))) {
            var2 = var3;
            break;
         }
      }

      return var2;
   }

   private static boolean objectsEqual(Object var0, Object var1) {
      return var0 == var1 || var0 != null && var0.equals(var1);
   }

   public boolean contains(Object var1) {
      return this.indexOf(var1) != -1;
   }

   public void forEach(Consumer<? super T> var1) {
      int var2 = 0;

      for(int var3 = this.size(); var2 < var3; ++var2) {
         var1.accept(this.get(var2));
      }

   }

   public void replaceAll(UnaryOperator<T> var1) {
      Objects.requireNonNull(var1);
      Object[] var2 = this.m_array;

      for(int var3 = 0; var3 < var2.length; ++var3) {
         Object var4 = this.get(var3);
         Object var5 = var1.apply(var4);
         var2[var3] = this.convertTS(var5);
      }

   }

   public void sort(Comparator<? super T> var1) {
      Arrays.sort(this.m_array, (var2, var3) -> {
         return var1.compare(this.convertST(var2), this.convertST(var3));
      });
   }

   private T convertST(S var1) {
      return this.m_converterST.apply(var1);
   }

   private S convertTS(T var1) {
      return this.m_converterTS.apply(var1);
   }
}
