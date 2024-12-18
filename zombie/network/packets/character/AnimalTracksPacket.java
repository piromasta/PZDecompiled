package zombie.network.packets.character;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalTracks;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
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
   handlingType = 3
)
public class AnimalTracksPacket implements INetworkPacket {
   @JSONField
   PlayerID player = new PlayerID();
   @JSONField
   ArrayList<AnimalTracks> tracks = new ArrayList();

   public AnimalTracksPacket() {
   }

   public void setData(Object... var1) {
      if (var1.length == 2) {
         this.set((IsoPlayer)var1[0], (ArrayList)var1[1]);
      } else if (var1.length == 1) {
         this.set((IsoPlayer)var1[0]);
      } else {
         DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
      }

   }

   public void set(IsoPlayer var1) {
      this.player.set(var1);
   }

   public void set(IsoPlayer var1, ArrayList<AnimalTracks> var2) {
      this.player.set(var1);
      this.tracks.clear();
      this.tracks.addAll(var2);
   }

   public void write(ByteBufferWriter var1) {
      this.player.write(var1);
      var1.putShort((short)this.tracks.size());
      Iterator var2 = this.tracks.iterator();

      while(var2.hasNext()) {
         AnimalTracks var3 = (AnimalTracks)var2.next();

         try {
            var3.save(var1.bb);
         } catch (IOException var5) {
            throw new RuntimeException(var5);
         }
      }

   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.player.parse(var1, var2);
      this.player.parsePlayer(var2);
      this.tracks.clear();
      short var3 = var1.getShort();
      AnimalTracks var4 = new AnimalTracks();

      for(int var5 = 0; var5 < var3; ++var5) {
         try {
            var4.load(var1, 0);
         } catch (IOException var7) {
            throw new RuntimeException(var7);
         }

         this.tracks.add(var4);
      }

   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
      this.tracks = AnimalTracks.getAndFindNearestTracks(this.player.getPlayer());
      if (this.tracks != null) {
         for(int var3 = 0; var3 < GameServer.udpEngine.connections.size(); ++var3) {
            UdpConnection var4 = (UdpConnection)GameServer.udpEngine.connections.get(var3);
            if (var4.getConnectedGUID() == (Long)GameServer.PlayerToAddressMap.get(this.player.getPlayer())) {
               ByteBufferWriter var5 = var4.startPacket();
               PacketTypes.PacketType.AnimalTracks.doPacket(var5);
               this.write(var5);
               PacketTypes.PacketType.AnimalTracks.send(var4);
               break;
            }
         }

      }
   }

   public void processClient(UdpConnection var1) {
      if (this.tracks != null) {
         LuaEventManager.triggerEvent("OnAnimalTracks", this.player.getPlayer(), this.tracks);
      }

   }
}
