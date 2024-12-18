package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.globalObjects.CGlobalObjectNetwork;
import zombie.globalObjects.SGlobalObjectNetwork;
import zombie.network.GameServer;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class GlobalObjectsPacket implements INetworkPacket {
   ByteBuffer buffer = null;

   public GlobalObjectsPacket() {
   }

   public void set(ByteBuffer var1) {
      this.buffer = var1;
   }

   public void write(ByteBufferWriter var1) {
      this.buffer.flip();
      var1.bb.put(this.buffer);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      if (GameServer.bServer) {
         byte var3 = var1.get();
         IsoPlayer var4 = GameServer.getPlayerFromConnection(var2, var3);
         if (var3 == -1) {
            var4 = GameServer.getAnyPlayerFromConnection(var2);
         }

         if (var4 == null) {
            DebugLog.log("receiveGlobalObjects: player is null");
            return;
         }

         SGlobalObjectNetwork.receive(var1, var4);
      } else {
         try {
            CGlobalObjectNetwork.receive(var1);
         } catch (IOException var5) {
            throw new RuntimeException(var5);
         }
      }

   }
}
