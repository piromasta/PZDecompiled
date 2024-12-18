package zombie.iso.worldgen.utils;

import java.util.HashMap;

public class CacheMap<K, V> extends HashMap<K, V> {
   private final int size;

   public CacheMap(int var1) {
      this.size = var1;
   }

   public V put(K var1, V var2) {
      if (this.size() > this.size) {
         this.clear();
      }

      return super.put(var1, var2);
   }
}
