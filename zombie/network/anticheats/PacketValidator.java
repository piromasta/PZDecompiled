package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.utils.UpdateLimit;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public class PacketValidator extends SuspiciousActivity implements AntiCheatRecipeUpdate.IAntiCheatUpdate, AntiCheatTimeUpdate.IAntiCheatUpdate, AntiCheatPlayerUpdate.IAntiCheatUpdate {
   private static final long PLAYER_UPDATE_TIMEOUT = 5000L;
   private static final long GAMETIME_INTERVAL = 5000L;
   private static final long GAMETIME_TIMEOUT = 10000L;
   private static final long CHECKSUM_INTERVAL = 4000L;
   private static final long CHECKSUM_TIMEOUT = 10000L;
   private final UpdateLimit ulPlayerUpdateTimeout = new UpdateLimit(5000L);
   private final UpdateLimit ulGametimeInterval = new UpdateLimit(5000L);
   private final UpdateLimit ulGametimeTimeout = new UpdateLimit(10000L);
   private final UpdateLimit ulChecksumInterval = new UpdateLimit(4000L);
   private final UpdateLimit ulChecksumTimeout = new UpdateLimit(10000L);
   private int salt;

   public void checksumSend(boolean var1, boolean var2) {
      this.salt = Rand.Next(2147483647);
      INetworkPacket.send(this.connection, PacketTypes.PacketType.Validate, this.salt, var1, var2);
      this.ulChecksumInterval.Reset(4000L);
   }

   public boolean checksumIntervalCheck() {
      return this.ulChecksumInterval.Check();
   }

   public boolean checksumTimeoutCheck() {
      return this.ulChecksumTimeout.Check();
   }

   public void checksumTimeoutReset() {
      this.ulChecksumTimeout.Reset(10000L);
   }

   public void gametimeSend() {
      INetworkPacket.send(this.connection, PacketTypes.PacketType.TimeSync);
      this.ulGametimeInterval.Reset(5000L);
   }

   public boolean gametimeIntervalCheck() {
      return this.ulGametimeInterval.Check();
   }

   public boolean gametimeTimeoutCheck() {
      return this.ulGametimeTimeout.Check();
   }

   public void gametimeTimeoutReset() {
      this.ulGametimeTimeout.Reset(10000L);
   }

   public boolean playerUpdateTimeoutCheck() {
      return this.ulPlayerUpdateTimeout.Check();
   }

   public void playerUpdateTimeoutReset() {
      this.ulPlayerUpdateTimeout.Reset(5000L);
   }

   public PacketValidator(UdpConnection var1) {
      super(var1);
   }

   public void reset() {
      this.salt = Rand.Next(2147483647);
      this.resetTimers();
   }

   public int getSalt() {
      return this.salt;
   }

   private void resetTimers() {
      this.ulPlayerUpdateTimeout.Reset(5000L);
      this.ulGametimeInterval.Reset(5000L);
      this.ulGametimeTimeout.Reset(10000L);
      this.ulChecksumInterval.Reset(4000L);
      this.ulChecksumTimeout.Reset(10000L);
   }

   public void update() {
      if (!GameServer.bFastForward && !GameServer.isDelayedDisconnect(this.connection)) {
         super.update();
         if (this.connection.isFullyConnected()) {
            AntiCheat.update(this.connection);
         } else {
            AntiCheat.preUpdate(this.connection);
            this.resetTimers();
         }
      } else {
         this.resetTimers();
      }

   }
}
