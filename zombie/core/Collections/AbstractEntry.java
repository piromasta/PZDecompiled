package zombie.core.Collections;

import java.util.Map;

abstract class AbstractEntry<TypeK, TypeV> implements Map.Entry<TypeK, TypeV> {
   protected final TypeK _key;
   protected TypeV _val;

   public AbstractEntry(TypeK var1, TypeV var2) {
      this._key = var1;
      this._val = var2;
   }

   public AbstractEntry(Map.Entry<TypeK, TypeV> var1) {
      this._key = var1.getKey();
      this._val = var1.getValue();
   }

   public String toString() {
      return this._key + "=" + this._val;
   }

   public TypeK getKey() {
      return this._key;
   }

   public TypeV getValue() {
      return this._val;
   }

   public boolean equals(Object var1) {
      if (!(var1 instanceof Map.Entry var2)) {
         return false;
      } else {
         return eq(this._key, var2.getKey()) && eq(this._val, var2.getValue());
      }
   }

   public int hashCode() {
      return (this._key == null ? 0 : this._key.hashCode()) ^ (this._val == null ? 0 : this._val.hashCode());
   }

   private static boolean eq(Object var0, Object var1) {
      return var0 == null ? var1 == null : var0.equals(var1);
   }
}
