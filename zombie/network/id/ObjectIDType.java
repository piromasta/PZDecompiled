package zombie.network.id;

import astar.datastructures.HashPriorityQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

public enum ObjectIDType {
   Unknown(-1, false, ObjectID.ObjectIDShort.class),
   Player(0, false, ObjectID.ObjectIDShort.class),
   Zombie(1, false, ObjectID.ObjectIDShort.class),
   Item(2, true, ObjectID.ObjectIDInteger.class),
   Container(3, true, ObjectID.ObjectIDInteger.class),
   DeadBody(4, true, ObjectID.ObjectIDShort.class),
   Vehicle(5, true, ObjectID.ObjectIDShort.class);

   static byte permanentObjectIDTypes = 0;
   private static final HashMap<Byte, ObjectIDType> objectIDTypes = new HashMap();
   final HashPriorityQueue<Long, IIdentifiable> IDToObjectMap = new HashPriorityQueue(Comparator.comparingLong((var0) -> {
      return var0.getObjectID().getObjectID();
   }));
   final byte index;
   final boolean isPermanent;
   final Class<?> type;
   long lastID;
   long countNewID;

   private ObjectIDType(int var3, boolean var4, Class var5) {
      this.index = (byte)var3;
      this.isPermanent = var4;
      this.type = var5;
   }

   static ObjectIDType valueOf(byte var0) {
      return (ObjectIDType)objectIDTypes.getOrDefault(var0, Unknown);
   }

   long allocateID() {
      ++this.lastID;
      ++this.countNewID;
      return this.lastID;
   }

   public String toString() {
      return String.format("ObjectID type=%s last=%d new=%d", this.name(), this.lastID, this.countNewID);
   }

   public Collection<IIdentifiable> getObjects() {
      return this.IDToObjectMap.getHashMap().values();
   }

   static {
      ObjectIDType[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         ObjectIDType var3 = var0[var2];
         objectIDTypes.put(var3.index, var3);
         if (var3.isPermanent) {
            ++permanentObjectIDTypes;
         }
      }

   }
}
