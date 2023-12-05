package zombie.util.map;

public interface IntKeyMapIterator<V> {
   boolean hasNext();

   void next();

   void remove();

   int getKey();

   V getValue();
}
