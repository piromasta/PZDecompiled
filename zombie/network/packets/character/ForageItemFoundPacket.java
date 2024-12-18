package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 1
)
public class ForageItemFoundPacket implements INetworkPacket {
   @JSONField
   PlayerID player = new PlayerID();
   @JSONField
   String itemType;
   @JSONField
   float amount;

   public ForageItemFoundPacket() {
   }

   public void setData(Object... var1) {
      this.player.set((IsoPlayer)var1[0]);
      this.itemType = var1[1].toString();
      this.amount = (Float)var1[2];
   }

   public void write(ByteBufferWriter var1) {
      this.player.write(var1);
      var1.putUTF(this.itemType);
      var1.putFloat(this.amount);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.player.parse(var1, var2);
      this.player.parsePlayer(var2);
      this.itemType = GameWindow.ReadString(var1);
      this.amount = var1.getFloat();
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
      LuaEventManager.triggerEvent("OnItemFound", this.player.getPlayer(), this.itemType, this.amount);
   }
}
