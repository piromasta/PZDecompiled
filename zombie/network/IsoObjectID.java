package zombie.network;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import zombie.core.Rand;

public class IsoObjectID<T> implements Iterable<T> {
   public static final short incorrect = -1;
   private final ConcurrentHashMap<Short, T> IDToObjectMap = new ConcurrentHashMap();
   private final String objectType;
   private short nextID = (short)Rand.Next(32766);

   public IsoObjectID(Class<T> var1) {
      this.objectType = var1.getSimpleName();
   }

   public void put(short var1, T var2) {
      if (var1 != -1) {
         this.IDToObjectMap.put(var1, var2);
      }

   }

   public void remove(short var1) {
      this.IDToObjectMap.remove(var1);
   }

   public void remove(T var1) {
      this.IDToObjectMap.values().remove(var1);
   }

   public T get(short var1) {
      return this.IDToObjectMap.get(var1);
   }

   public int size() {
      return this.IDToObjectMap.size();
   }

   public void clear() {
      this.IDToObjectMap.clear();
   }

   public short allocateID() {
      ++this.nextID;
      if (this.nextID == -1) {
         ++this.nextID;
      }

      return this.nextID;
   }

   public Iterator<T> iterator() {
      return this.IDToObjectMap.values().iterator();
   }

   public void getObjects(Collection<T> var1) {
      var1.addAll(this.IDToObjectMap.values());
   }
}
