package zombie.entity.meta;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.core.raknet.UdpConnection;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;

public class MetaTagComponent extends Component {
   private long storedID = -9223372036854775808L;

   private MetaTagComponent() {
      super(ComponentType.MetaTag);
   }

   public void setStoredID(long var1) {
      this.storedID = var1;
   }

   public long getStoredID() {
      return this.storedID;
   }

   protected void reset() {
      super.reset();
      this.storedID = -9223372036854775808L;
   }

   protected boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      return false;
   }

   protected void saveSyncData(ByteBuffer var1) throws IOException {
      this.save(var1);
   }

   protected void loadSyncData(ByteBuffer var1) throws IOException {
      this.load(var1, 219);
   }

   protected void save(ByteBuffer var1) throws IOException {
      super.save(var1);
      var1.putLong(this.storedID);
   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.storedID = var1.getLong();
   }
}
