package zombie.entity.util.assoc;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;

public class AssocEnumArray<K extends Enum<K>, V> extends AssocArray<K, V> {
   private final EnumSet<K> keys;

   public AssocEnumArray(Class<K> var1) {
      this.keys = EnumSet.noneOf(var1);
   }

   public AssocEnumArray(Class<K> var1, int var2) {
      super(var2);
      this.keys = EnumSet.noneOf(var1);
   }

   public boolean equalsKeys(AssocEnumArray<K, V> var1) {
      return var1 == this ? true : this.keys.equals(var1.keys);
   }

   public Iterator<K> keys() {
      return this.keys.iterator();
   }

   public boolean containsKey(K var1) {
      return this.keys.contains(var1);
   }

   public V put(K var1, V var2) {
      Object var3 = super.put(var1, var2);
      this.keys.add(var1);
      return var3;
   }

   public boolean add(K var1, V var2) {
      if (super.add(var1, var2)) {
         this.keys.add(var1);
         return true;
      } else {
         return false;
      }
   }

   public void add(int var1, K var2, V var3) {
      super.add(var1, var2, var3);
      this.keys.add(var2);
   }

   public V removeIndex(int var1) {
      Objects.checkIndex(var1, this.size());
      Object[] var2 = this.elementData;
      int var3 = this.realKeyIndex(var1);
      Object var4 = var2[var3];
      Object var5 = var2[var3 + 1];
      super.fastRemove(var2, var3);
      this.keys.remove(var4);
      return var5;
   }

   public boolean equals(Object var1) {
      return var1.getClass() == AssocEnumArray.class && var1 == this;
   }

   public V remove(K var1) {
      Object var2 = super.remove(var1);
      if (var2 != null) {
         this.keys.remove(var1);
         return var2;
      } else {
         return null;
      }
   }

   public void clear() {
      super.clear();
      this.keys.clear();
   }
}
