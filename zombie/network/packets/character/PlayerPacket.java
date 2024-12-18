package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.ai.StateMachine;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.Vector3;
import zombie.network.NetworkVariables;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheatNoClip;
import zombie.network.anticheats.AntiCheatPlayer;
import zombie.network.anticheats.AntiCheatPower;
import zombie.network.anticheats.AntiCheatRole;
import zombie.network.anticheats.AntiCheatSpeed;
import zombie.network.fields.IMovable;
import zombie.network.fields.PlayerID;
import zombie.network.fields.PlayerVariables;
import zombie.network.packets.INetworkPacket;

public class PlayerPacket implements INetworkPacket, AntiCheatPower.IAntiCheat, AntiCheatRole.IAntiCheat, AntiCheatSpeed.IAntiCheat, AntiCheatNoClip.IAntiCheat, AntiCheatPlayer.IAntiCheat {
   public static final int PACKET_SIZE_BYTES = 47;
   public PlayerID id = new PlayerID();
   public float x;
   public float y;
   public byte z;
   public float direction;
   public boolean usePathFinder;
   public NetworkVariables.PredictionTypes moveType;
   public short VehicleID;
   public short VehicleSeat;
   public int booleanVariables;
   public int roleId;
   public byte footstepSoundRadius;
   public byte bleedingLevel;
   public float realx;
   public float realy;
   public byte realz;
   public byte realdir;
   public int realt;
   public float collidePointX;
   public float collidePointY;
   public float endurance;
   public PlayerVariables variables = new PlayerVariables();
   public boolean disconnected;
   public byte type;
   public int actionState;
   public StateMachine stateMachine;

   public PlayerPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.stateMachine = this.getPlayer().getStateMachine();
      this.stateMachine.parse(var1, this.actionState);
   }

   public void write(ByteBufferWriter var1) {
      this.stateMachine.write(var1, this.actionState);
   }

   public int getPacketSizeBytes() {
      return 3;
   }

   public void set(IsoPlayer var1) {
      this.stateMachine = var1.getStateMachine();
   }

   public void copy(PlayerPacket var1) {
      this.stateMachine = var1.stateMachine;
   }

   public void processClient(UdpConnection var1) {
   }

   public void processClientLoading(UdpConnection var1) {
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   public int getBooleanVariables() {
      return 3;
   }

   public int getRoleID() {
      return 1;
   }

   public IsoPlayer getPlayer() {
      return null;
   }

   public IMovable getMovable() {
      return this.getPlayer().getNetworkCharacterAI().speedChecker;
   }

   public byte getPlayerIndex() {
      return 1;
   }

   public Vector3 getPosition(Vector3 var1) {
      return var1.set(1.0F, 1.0F, 1.0F);
   }
}
