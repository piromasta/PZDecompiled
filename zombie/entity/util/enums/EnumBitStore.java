package zombie.entity.util.enums;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import zombie.core.utils.Bits;

public class EnumBitStore<E extends Enum<E> & IOEnum> {
   private static final String emptyToString = "[]";
   private int bits = 0;
   final transient Class<E> elementType;

   private EnumBitStore(Class<E> var1) {
      this.elementType = var1;
   }

   public static <E extends Enum<E> & IOEnum> EnumBitStore<E> noneOf(Class<E> var0) {
      return new EnumBitStore(var0);
   }

   public static <E extends Enum<E> & IOEnum> EnumBitStore<E> allOf(Class<E> var0) {
      EnumBitStore var1 = noneOf(var0);
      var1.addAll();
      return var1;
   }

   public static <E extends Enum<E> & IOEnum> EnumBitStore<E> copyOf(EnumBitStore<E> var0) {
      EnumBitStore var1 = noneOf(var0.elementType);
      var1.copyFrom(var0);
      return var1;
   }

   public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E var0) {
      EnumBitStore var1 = noneOf(var0.getDeclaringClass());
      var1.add(var0);
      return var1;
   }

   public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E var0, E var1) {
      EnumBitStore var2 = noneOf(var0.getDeclaringClass());
      var2.add(var0);
      var2.add(var1);
      return var2;
   }

   public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E var0, E var1, E var2) {
      EnumBitStore var3 = noneOf(var0.getDeclaringClass());
      var3.add(var0);
      var3.add(var1);
      var3.add(var2);
      return var3;
   }

   public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E var0, E var1, E var2, E var3) {
      EnumBitStore var4 = noneOf(var0.getDeclaringClass());
      var4.add(var0);
      var4.add(var1);
      var4.add(var2);
      var4.add(var3);
      return var4;
   }

   public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E var0, E var1, E var2, E var3, E var4) {
      EnumBitStore var5 = noneOf(var0.getDeclaringClass());
      var5.add(var0);
      var5.add(var1);
      var5.add(var2);
      var5.add(var3);
      var5.add(var4);
      return var5;
   }

   @SafeVarargs
   public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E var0, E... var1) {
      EnumBitStore var2 = noneOf(var0.getDeclaringClass());
      var2.add(var0);
      Enum[] var3 = var1;
      int var4 = var1.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Enum var6 = var3[var5];
         var2.add(var6);
      }

      return var2;
   }

   public void copyFrom(EnumBitStore<E> var1) {
      this.bits = var1.bits;
   }

   public void addAll(EnumBitStore<E> var1) {
      this.bits = Bits.addFlags(this.bits, var1.bits);
   }

   public void addAll() {
      Enum[] var1 = (Enum[])this.elementType.getEnumConstants();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         Enum var4 = var1[var3];
         this.add(var4);
      }

   }

   public void add(E var1) {
      this.bits = Bits.addFlags(this.bits, ((IOEnum)var1).getBits());
   }

   public void remove(E var1) {
      this.bits = Bits.removeFlags(this.bits, ((IOEnum)var1).getBits());
   }

   public boolean contains(E var1) {
      return this.contains(((IOEnum)var1).getBits());
   }

   public boolean contains(int var1) {
      return Bits.hasFlags(this.bits, var1);
   }

   public int size() {
      return Integer.bitCount(this.bits);
   }

   public boolean isEmpty() {
      return this.bits == 0;
   }

   public void clear() {
      this.bits = 0;
   }

   public int getBits() {
      return this.bits;
   }

   public void setBits(int var1) {
      this.bits = var1;
   }

   public void save(ByteBuffer var1) throws IOException {
      var1.putInt(this.bits);
   }

   public void load(ByteBuffer var1) throws IOException {
      this.bits = var1.getInt();
   }

   public boolean equals(Object var1) {
      if (!(var1 instanceof EnumBitStore var2)) {
         return false;
      } else if (var2.elementType == this.elementType) {
         return var2.bits == this.bits;
      } else {
         return false;
      }
   }

   public String toString() {
      if (this.size() <= 0) {
         return "[]";
      } else {
         StringBuilder var1 = new StringBuilder();
         var1.append("[");
         EnumBitStoreIterator var2 = new EnumBitStoreIterator();

         while(var2.hasNext()) {
            var1.append(var2.next().toString());
            if (var2.returned < var2.size) {
               var1.append(",");
            }
         }

         var1.append("]");
         return var1.toString();
      }
   }

   public Iterator<E> iterator() {
      return new EnumBitStoreIterator();
   }

   private class EnumBitStoreIterator<E extends Enum<E> & IOEnum> implements Iterator<E> {
      int index = 0;
      int returned = 0;
      int size = EnumBitStore.this.size();

      EnumBitStoreIterator() {
      }

      public boolean hasNext() {
         return this.returned < this.size;
      }

      public E next() {
         while(true) {
            if (this.index < ((Enum[])EnumBitStore.this.elementType.getEnumConstants()).length) {
               Enum var1 = null;
               if (EnumBitStore.this.contains(((Enum[])EnumBitStore.this.elementType.getEnumConstants())[this.index])) {
                  var1 = ((Enum[])EnumBitStore.this.elementType.getEnumConstants())[this.index];
               }

               ++this.index;
               if (var1 == null) {
                  continue;
               }

               ++this.returned;
               return var1;
            }

            throw new IllegalStateException();
         }
      }
   }
}
