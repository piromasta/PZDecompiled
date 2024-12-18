package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.core.Color;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(
   ordering = 0,
   priority = 2,
   reliability = 2,
   requiredCapability = Capability.RolesWrite,
   handlingType = 1
)
public class RolesEditPacket implements INetworkPacket {
   @JSONField
   public Command command;
   @JSONField
   public String name;
   @JSONField
   String description;
   @JSONField
   Color color;
   @JSONField
   ArrayList<Capability> capabilities;
   @JSONField
   String defaultId;

   public RolesEditPacket() {
   }

   public void setData(Object... var1) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   public static enum Command {
      AddRole,
      DeleteRole,
      SetupRole,
      SetDefaultRole;

      private Command() {
      }
   }
}
