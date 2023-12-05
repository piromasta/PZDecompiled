package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.packets.hit.Instance;

public class PlayerID extends Instance implements INetworkPacket {
   protected IsoPlayer player;
   protected byte playerIndex;

   public PlayerID() {
   }

   public void set(IsoPlayer var1) {
      super.set(var1.OnlineID);
      this.playerIndex = var1.isLocal() ? (byte)var1.getPlayerNum() : -1;
      this.player = var1;
   }

   public void parsePlayer(UdpConnection var1) {
      if (GameServer.bServer) {
         if (var1 != null && this.playerIndex != -1) {
            this.player = GameServer.getPlayerFromConnection(var1, this.playerIndex);
         } else {
            this.player = (IsoPlayer)GameServer.IDToPlayerMap.get(this.ID);
         }
      } else if (GameClient.bClient) {
         this.player = (IsoPlayer)GameClient.IDToPlayerMap.get(this.ID);
      }

   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      this.playerIndex = var1.get();
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      var1.putByte(this.playerIndex);
   }

   public boolean isConsistent() {
      return super.isConsistent() && this.getCharacter() != null;
   }

   public String getDescription() {
      String var10000 = super.getDescription();
      return var10000 + "\n\tPlayer [ player " + (this.player == null ? "?" : "\"" + this.player.getUsername() + "\"") + " ]";
   }

   public IsoPlayer getCharacter() {
      return this.player;
   }
}
