package zombie.network.packets.hit;

import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public interface HitCharacter extends INetworkPacket {
   boolean isRelevant(UdpConnection var1);

   default void attack() {
   }

   default void react() {
   }

   void preProcess();

   void process();

   void postProcess();

   default void postpone() {
   }

   default void log() {
   }

   default void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
      try {
         this.log();
      } catch (Exception var4) {
         DebugLog.Multiplayer.printException(var4, "Log error", LogSeverity.Error);
      }

      GameServer.sendHitCharacter(this, var1, var2);
      this.processClient(var2);
   }

   default void processClient(UdpConnection var1) {
      if (GameClient.bClient && this instanceof VehicleHit) {
         this.postpone();
      } else {
         this.tryProcessInternal();
      }

   }

   default void tryProcessInternal() {
      this.preProcess();
      this.process();
      this.postProcess();
      this.attack();
      this.react();
   }
}
