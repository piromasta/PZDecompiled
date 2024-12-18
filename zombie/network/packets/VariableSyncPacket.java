package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.HashSet;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.PlayerID;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 7
)
public class VariableSyncPacket implements INetworkPacket {
   public PlayerID player = new PlayerID();
   public String key;
   public String value;
   public boolean boolValue;
   public float floatValue;
   public VariableType varType;
   public static HashSet<String> syncedVariables = new HashSet();

   public VariableSyncPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public static enum VariableType {
      String,
      Boolean,
      Float;

      private VariableType() {
      }
   }
}
