package zombie.util.map;

import java.util.Collection;
import zombie.util.set.IntSet;

public interface IntKeyMap<V> {
   void clear();

   boolean containsKey(int var1);

   boolean containsValue(Object var1);

   IntKeyMapIterator<V> entries();

   boolean equals(Object var1);

   V get(int var1);

   int hashCode();

   boolean isEmpty();

   IntSet keySet();

   V put(int var1, V var2);

   void putAll(IntKeyMap<V> var1);

   V remove(int var1);

   int size();

   Collection<V> values();
}
