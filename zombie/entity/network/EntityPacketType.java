package zombie.entity.network;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import zombie.core.network.ByteBufferWriter;

public enum EntityPacketType {
   UpdateUsingPlayer(PacketGroup.GameEntity),
   SyncGameEntity(PacketGroup.GameEntity),
   RequestSyncGameEntity(PacketGroup.GameEntity),
   CraftLogicSync(PacketGroup.CraftLogic),
   CraftLogicSyncFull(PacketGroup.CraftLogic),
   CraftLogicStartRequest(PacketGroup.CraftLogic),
   CraftLogicStopRequest(PacketGroup.CraftLogic),
   MashingLogicSync(PacketGroup.MashingLogic),
   MashingLogicSyncFull(PacketGroup.MashingLogic),
   MashingLogicStartRequest(PacketGroup.MashingLogic),
   MashingLogicStopRequest(PacketGroup.MashingLogic),
   ResourcesSync(PacketGroup.Resources);

   private static final Map<Short, EntityPacketType> entityPacketMap = new HashMap();
   private short id = 0;
   private final PacketGroup group;

   private EntityPacketType() {
      this.group = PacketGroup.Generic;
   }

   private EntityPacketType(PacketGroup var3) {
      this.group = var3;
   }

   private EntityPacketType(short var3, PacketGroup var4) {
      this.group = var4;
      this.id = var3;
   }

   public PacketGroup getGroup() {
      return this.group;
   }

   public boolean isEntityPacket() {
      return this.group == PacketGroup.GameEntity;
   }

   public boolean isComponentPacket() {
      return this.group != PacketGroup.GameEntity;
   }

   public void saveToByteBuffer(ByteBufferWriter var1) {
      var1.putShort(this.id);
   }

   public void saveToByteBuffer(ByteBuffer var1) {
      var1.putShort(this.id);
   }

   public static EntityPacketType FromByteBuffer(ByteBuffer var0) {
      short var1 = var0.getShort();
      return (EntityPacketType)entityPacketMap.get(var1);
   }

   static {
      short var0 = 1000;
      EntityPacketType[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         EntityPacketType var4 = var1[var3];
         if (var4.id == 0) {
            var4.id = var0++;
         }

         entityPacketMap.put(var4.id, var4);
      }

   }
}
