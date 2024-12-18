package zombie.network.id;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.fields.INetworkPacketField;

public abstract class ObjectID implements INetworkPacketField {
   protected long id;
   protected ObjectIDType type;

   ObjectID(ObjectIDType var1) {
      this.type = var1;
      this.reset();
   }

   long getObjectID() {
      return this.id;
   }

   ObjectIDType getType() {
      return this.type;
   }

   public IIdentifiable getObject() {
      return ObjectIDManager.get(this);
   }

   void set(long var1, ObjectIDType var3) {
      this.id = var1;
      this.type = var3;
   }

   public void set(ObjectID var1) {
      this.set(var1.id, var1.type);
   }

   public void reset() {
      this.id = -1L;
   }

   public void load(ByteBuffer var1) {
      this.type = ObjectIDType.valueOf(var1.get());
   }

   public void save(ByteBuffer var1) {
      var1.put(this.type.index);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.load(var1);
   }

   public void write(ByteBufferWriter var1) {
      this.save(var1.bb);
   }

   public String toString() {
      String var10000 = this.type.name();
      return var10000 + "-" + this.id;
   }

   public String getDescription() {
      long var10000 = this.id;
      return "{ \"ObjectID\" : { \"id\" : " + var10000 + ", \"type\" : \"" + this.type.name() + "\" } }";
   }

   public int hashCode() {
      return (int)(this.id * 10L + (long)this.type.index);
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         ObjectID var2 = (ObjectID)var1;
         return this.id == var2.id && this.type == var2.type;
      } else {
         return false;
      }
   }

   static class ObjectIDShort extends ObjectID {
      ObjectIDShort(ObjectIDType var1) {
         super(var1);
      }

      public void load(ByteBuffer var1) {
         this.id = (long)var1.getShort();
         super.load(var1);
      }

      public void save(ByteBuffer var1) {
         var1.putShort((short)((int)this.id));
         super.save(var1);
      }

      public int getPacketSizeBytes() {
         return 3;
      }
   }

   static class ObjectIDInteger extends ObjectID {
      ObjectIDInteger(ObjectIDType var1) {
         super(var1);
      }

      public void load(ByteBuffer var1) {
         this.id = (long)var1.getInt();
         super.load(var1);
      }

      public void save(ByteBuffer var1) {
         var1.putInt((int)this.id);
         super.save(var1);
      }

      public int getPacketSizeBytes() {
         return 5;
      }
   }
}
