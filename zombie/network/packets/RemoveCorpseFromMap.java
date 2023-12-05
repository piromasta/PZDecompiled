package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoDeadBody;

public class RemoveCorpseFromMap implements INetworkPacket {
   private short id;
   private IsoDeadBody deadBody = null;

   public RemoveCorpseFromMap() {
   }

   public void set(IsoDeadBody var1) {
      this.id = var1.getObjectID();
      this.deadBody = var1;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.id = var1.getShort();
      this.deadBody = IsoDeadBody.getDeadBody(this.id);
   }

   public void write(ByteBufferWriter var1) {
      var1.putShort(this.id);
   }

   public void process() {
      if (this.isConsistent()) {
         IsoDeadBody.removeDeadBody(this.id);
      }

   }

   public String getDescription() {
      return String.format(this.getClass().getSimpleName() + " id=%d", this.id);
   }

   public boolean isConsistent() {
      return this.deadBody != null && this.deadBody.getSquare() != null;
   }

   public boolean isRelevant(UdpConnection var1) {
      return var1.RelevantTo(this.deadBody.getX(), this.deadBody.getY());
   }
}
