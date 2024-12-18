package zombie.network.packets.world;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.util.StringUtils;

@PacketSetting(
   ordering = 0,
   priority = 2,
   reliability = 2,
   requiredCapability = Capability.CreateStory,
   handlingType = 1
)
public class DebugStoryPacket implements INetworkPacket {
   @JSONField
   protected final Square square = new Square();
   @JSONField
   protected int type;
   @JSONField
   protected String name;

   public DebugStoryPacket() {
   }

   public void setData(Object... var1) {
      this.square.set((IsoGridSquare)var1[0]);
      this.type = (Integer)var1[1];
      this.name = (String)var1[2];
   }

   public void write(ByteBufferWriter var1) {
      this.square.write(var1);
      var1.putInt(this.type);
      var1.putUTF(this.name);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.square.parse(var1, var2);
      this.type = var1.getInt();
      this.name = GameWindow.ReadString(var1);
   }

   public boolean isConsistent(UdpConnection var1) {
      return this.square.isConsistent(var1) && !StringUtils.isNullOrEmpty(this.name);
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
      if (this.type == 0) {
         RandomizedVehicleStoryBase var3 = IsoWorld.instance.getRandomizedVehicleStoryByName(this.name);
         if (var3 != null) {
            var3.randomizeVehicleStory(this.square.getSquare().getZone(), this.square.getSquare().getChunk());
         }
      } else if (this.type == 1) {
         RandomizedZoneStoryBase var5 = IsoWorld.instance.getRandomizedZoneStoryByName(this.name);
         if (var5 != null) {
            Zone var4 = new Zone("debugstoryzone", "debugstoryzone", this.square.getSquare().getX() - 20, this.square.getSquare().getY() - 20, this.square.getSquare().getZ(), this.square.getSquare().getX() + 20, this.square.getSquare().getX() + 20);
            var4.setPickedXForZoneStory(this.square.getSquare().getX());
            var4.setPickedYForZoneStory(this.square.getSquare().getY());
            var4.setX(this.square.getSquare().getX() - var5.getMinimumWidth() / 2);
            var4.setY(this.square.getSquare().getY() - var5.getMinimumHeight() / 2);
            var4.setW(var5.getMinimumWidth() + 2);
            var4.setH(var5.getMinimumHeight() + 2);
            var5.randomizeZoneStory(var4);
         }
      }

   }
}
