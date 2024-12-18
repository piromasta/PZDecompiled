package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.NetworkVariables;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;

public class ZombiePacket implements INetworkPacket {
   private static final int PACKET_SIZE_BYTES = 55;
   public short id;
   public float x;
   public float y;
   public byte z;
   public int descriptorID;
   public NetworkVariables.PredictionTypes moveType;
   public short booleanVariables;
   public short target;
   public int timeSinceSeenFlesh;
   public int smParamTargetAngle;
   public short speedMod;
   public NetworkVariables.WalkType walkType;
   public float realX;
   public float realY;
   public byte realZ;
   public short realHealth;
   public NetworkVariables.ZombieState realState;
   public final ObjectID reanimatedBodyID;
   public byte pfbType;
   public short pfbTarget;
   public float pfbTargetX;
   public float pfbTargetY;
   public byte pfbTargetZ;

   public ZombiePacket() {
      this.reanimatedBodyID = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBuffer var1) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public int getPacketSizeBytes() {
      return 6;
   }

   public void copy(ZombiePacket var1) {
   }

   public void set(IsoZombie var1) {
   }
}
