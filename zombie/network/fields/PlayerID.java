package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class PlayerID extends IDShort implements INetworkPacketField, IPositional {
   protected IsoPlayer player;
   protected byte playerIndex;

   public PlayerID() {
   }

   public void set(IsoPlayer var1) {
      super.setID(var1.OnlineID);
      this.playerIndex = var1.isLocal() ? (byte)var1.getPlayerNum() : -1;
      this.player = var1;
   }

   public void parsePlayer(UdpConnection var1) {
      if (GameServer.bServer) {
         if (var1 != null && this.playerIndex != -1) {
            this.player = GameServer.getPlayerFromConnection(var1, this.playerIndex);
         } else {
            this.player = (IsoPlayer)GameServer.IDToPlayerMap.get(this.getID());
         }
      } else if (GameClient.bClient) {
         this.player = (IsoPlayer)GameClient.IDToPlayerMap.get(this.getID());
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

   public void write(ByteBuffer var1) {
      super.write(var1);
      var1.put(this.playerIndex);
   }

   public boolean isConsistent(UdpConnection var1) {
      return super.isConsistent(var1) && this.getPlayer() != null;
   }

   public String toString() {
      return this.player == null ? "?" : "(" + this.player.getOnlineID() + ")";
   }

   public IsoPlayer getPlayer() {
      return this.player;
   }

   public void copy(PlayerID var1) {
      this.setID(var1.getID());
      this.player = var1.player;
      this.playerIndex = var1.playerIndex;
   }

   public float getX() {
      return this.player != null ? this.player.getX() : 0.0F;
   }

   public float getY() {
      return this.player != null ? this.player.getY() : 0.0F;
   }

   public byte getPlayerIndex() {
      return this.playerIndex;
   }
}
