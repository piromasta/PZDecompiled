package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.NetworkVariables;
import zombie.pathfind.Path;
import zombie.pathfind.PathFindBehavior2;
import zombie.util.Type;

public class PFBData implements INetworkPacketField {
   private static final byte goalNone = 0;
   private static final byte goalCharacter = 1;
   private static final byte goalLocation = 2;
   private static final byte goalSound = 3;
   public byte type;
   public float targetX;
   public float targetY;
   public float targetZ;
   public PlayerID targetPlayer = new PlayerID();
   public NetworkVariables.ZombieState realState;

   public PFBData() {
      this.realState = NetworkVariables.ZombieState.Idle;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.type = var1.get();
      this.realState = NetworkVariables.ZombieState.fromByte(var1.get());
      if (1 == this.type) {
         this.targetPlayer.parse(var1, var2);
         this.targetPlayer.parsePlayer(var2);
      } else {
         this.targetX = var1.getFloat();
         this.targetY = var1.getFloat();
         this.targetZ = var1.getFloat();
      }

   }

   public void write(ByteBufferWriter var1) {
      this.write(var1.bb);
   }

   private void write(ByteBuffer var1) {
      var1.put(this.type);
      var1.put(this.realState.toByte());
      if (1 == this.type) {
         this.targetPlayer.write(var1);
      } else {
         var1.putFloat(this.targetX);
         var1.putFloat(this.targetY);
         var1.putFloat(this.targetZ);
      }

   }

   public void copy(PFBData var1) {
      this.type = var1.type;
      this.targetX = var1.targetX;
      this.targetY = var1.targetY;
      this.targetZ = var1.targetZ;
      this.targetPlayer = var1.targetPlayer;
      this.realState = var1.realState;
   }

   public boolean isCanceled() {
      return 0 == this.type;
   }

   public void reset() {
      this.type = 0;
   }

   public void set(IsoGameCharacter var1) {
      PathFindBehavior2 var2 = var1.getPathFindBehavior2();
      var1.realState = NetworkVariables.ZombieState.fromString(var1.getAdvancedAnimator().getCurrentStateName());
      this.realState = var1.realState;
      if (!var2.getIsCancelled() && !var2.isGoalNone() && !var2.bStopping && !NetworkVariables.ZombieState.Idle.equals(var1.realState)) {
         if (var2.isGoalCharacter()) {
            IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var2.getTargetChar(), IsoPlayer.class);
            if (var3 != null) {
               this.type = 1;
               this.targetPlayer.set(var3);
            } else {
               this.type = 0;
               DebugLog.Multiplayer.error("NetworkZombieMind: goal character is not set");
            }
         } else if (var2.isGoalLocation()) {
            this.type = 2;
            this.targetX = var2.getTargetX();
            this.targetY = var2.getTargetY();
            this.targetZ = (float)((byte)((int)var2.getTargetZ()));
         } else if (var2.isGoalSound()) {
            this.type = 3;
            this.targetX = var2.getTargetX();
            this.targetY = var2.getTargetY();
            this.targetZ = (float)((byte)((int)var2.getTargetZ()));
         }
      } else {
         this.type = 0;
      }

   }

   public void restore(IsoGameCharacter var1) {
      var1.setPath2((Path)null);
      switch (this.type) {
         case 0:
         case 3:
         default:
            break;
         case 1:
            var1.pathToCharacter(this.targetPlayer.getPlayer());
            var1.spotted(this.targetPlayer.getPlayer(), true);
            break;
         case 2:
            var1.pathToLocationF(this.targetX, this.targetY, this.targetZ);
      }

   }
}
