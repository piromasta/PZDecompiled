package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class SyncInjuriesPacket implements INetworkPacket {
   public short id;
   public float strafeSpeed;
   public float walkSpeed;
   public float walkInjury;
   public IsoPlayer player;

   public SyncInjuriesPacket() {
   }

   public boolean set(IsoPlayer var1) {
      if (GameClient.bClient) {
         this.id = (short)var1.getPlayerNum();
      } else if (GameServer.bServer) {
         this.id = var1.getOnlineID();
      }

      this.strafeSpeed = var1.getVariableFloat("StrafeSpeed", 1.0F);
      this.walkSpeed = var1.getVariableFloat("WalkSpeed", 1.0F);
      this.walkInjury = var1.getVariableFloat("WalkInjury", 0.0F);
      this.player = var1;
      return true;
   }

   public boolean process() {
      if (this.player != null && !this.player.isLocalPlayer()) {
         this.player.setVariable("StrafeSpeed", this.strafeSpeed);
         this.player.setVariable("WalkSpeed", this.walkSpeed);
         this.player.setVariable("WalkInjury", this.walkInjury);
         return true;
      } else {
         return false;
      }
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.id = var1.getShort();
      this.strafeSpeed = var1.getFloat();
      this.walkSpeed = var1.getFloat();
      this.walkInjury = var1.getFloat();
      if (GameServer.bServer) {
         this.player = GameServer.getPlayerFromConnection(var2, this.id);
      } else if (GameClient.bClient) {
         this.player = (IsoPlayer)GameClient.IDToPlayerMap.get(this.id);
      } else {
         this.player = null;
      }

   }

   public void write(ByteBufferWriter var1) {
      var1.putShort(this.id);
      var1.putFloat(this.strafeSpeed);
      var1.putFloat(this.walkSpeed);
      var1.putFloat(this.walkInjury);
   }

   public int getPacketSizeBytes() {
      return 14;
   }

   public String getDescription() {
      return "SyncInjuriesPacket: id=" + this.id + ", strafeSpeed=" + this.strafeSpeed + ", walkSpeed=" + this.walkSpeed + ", walkInjury=" + this.walkInjury;
   }
}
