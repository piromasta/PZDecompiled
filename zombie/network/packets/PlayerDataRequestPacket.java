package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

public class PlayerDataRequestPacket implements INetworkPacket {
   short playerId = -1;

   public PlayerDataRequestPacket() {
   }

   public void set(short var1) {
      this.playerId = var1;
   }

   public void process(UdpConnection var1) {
      IsoPlayer var2 = (IsoPlayer)GameServer.IDToPlayerMap.get(this.playerId);
      if (var1.RelevantTo(var2.x, var2.y) && !var2.isInvisible() || var1.accessLevel >= 1) {
         GameServer.sendPlayerConnect(var2, var1);
      }

   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.playerId = var1.getShort();
   }

   public void write(ByteBufferWriter var1) {
      var1.putShort(this.playerId);
   }
}
