package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.iso.areas.NonPvpZone;
import zombie.util.StringUtils;

public class SyncNonPvpZonePacket implements INetworkPacket {
   public final NonPvpZone zone = new NonPvpZone();
   public boolean doRemove;

   public SyncNonPvpZonePacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.zone.load(var1, IsoWorld.getWorldVersion());
      this.doRemove = var1.get() == 1;
   }

   public void write(ByteBufferWriter var1) {
      this.zone.save(var1.bb);
      var1.putBoolean(this.doRemove);
   }

   public boolean isConsistent() {
      return !StringUtils.isNullOrEmpty(this.zone.getTitle());
   }

   public String getDescription() {
      return String.format("\"%s\" remove=%b size=%d (%d;%d) (%d;%d)", this.zone.getTitle(), this.doRemove, this.zone.getSize(), this.zone.getX(), this.zone.getY(), this.zone.getX2(), this.zone.getY2());
   }

   public void process() {
      if (this.doRemove) {
         NonPvpZone.getAllZones().removeIf((var1) -> {
            return var1.getTitle().equals(this.zone.getTitle());
         });
      } else if (NonPvpZone.getZoneByTitle(this.zone.getTitle()) == null) {
         NonPvpZone.getAllZones().add(this.zone);
      }

   }
}
