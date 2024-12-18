package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2
)
public class SyncPlayerStatsPacket implements INetworkPacket {
   public static final int Stat_Anger = 1;
   public static final int Stat_Endurance = 2;
   public static final int Stat_Fatigue = 4;
   public static final int Stat_Fitness = 8;
   public static final int Stat_Hunger = 16;
   public static final int Stat_Morale = 32;
   public static final int Stat_Stress = 64;
   public static final int Stat_Fear = 128;
   public static final int Stat_Panic = 256;
   public static final int Stat_Sanity = 512;
   public static final int Stat_Sickness = 1024;
   public static final int Stat_Boredom = 2048;
   public static final int Stat_Pain = 4096;
   public static final int Stat_Drunkennes = 8192;
   public static final int Stat_Thirst = 16384;
   public static final int Stat_StressFromCigarettes = 32768;

   public SyncPlayerStatsPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
