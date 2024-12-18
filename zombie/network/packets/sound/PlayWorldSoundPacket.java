package zombie.network.packets.sound;

import java.nio.ByteBuffer;
import zombie.GameSounds;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class PlayWorldSoundPacket implements INetworkPacket {
   public PlayWorldSoundPacket() {
   }

   public void set(Object... var1) {
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
      if (this.isConsistent(var2)) {
         int var3 = 70;
         GameSound var4 = GameSounds.getSound(this.getName());
         int var5;
         if (var4 != null) {
            for(var5 = 0; var5 < var4.clips.size(); ++var5) {
               GameSoundClip var6 = (GameSoundClip)var4.clips.get(var5);
               if (var6.hasMaxDistance()) {
                  var3 = Math.max(var3, (int)var6.distanceMax);
               }
            }
         }

         for(var5 = 0; var5 < GameServer.udpEngine.connections.size(); ++var5) {
            UdpConnection var9 = (UdpConnection)GameServer.udpEngine.connections.get(var5);
            if (var9.getConnectedGUID() != var2.getConnectedGUID() && var9.isFullyConnected()) {
               IsoPlayer var7 = GameServer.getAnyPlayerFromConnection(var9);
               if (var7 != null && var9.RelevantTo((float)this.getX(), (float)this.getY(), (float)var3)) {
                  ByteBufferWriter var8 = var9.startPacket();
                  PacketTypes.PacketType.PlayWorldSound.doPacket(var8);
                  this.write(var8);
                  PacketTypes.PacketType.PlayWorldSound.send(var9);
               }
            }
         }

      }
   }

   public void processClient(UdpConnection var1) {
   }

   public String getName() {
      return "";
   }

   public int getX() {
      return 0;
   }

   public int getY() {
      return 0;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
