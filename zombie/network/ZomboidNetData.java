package zombie.network;

import java.nio.ByteBuffer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;

public class ZomboidNetData implements IZomboidPacket {
   public PacketTypes.PacketType type;
   public short length;
   public ByteBuffer buffer;
   public long connection;
   public long time;

   public ZomboidNetData() {
      this.buffer = ByteBuffer.allocate(2048);
   }

   public ZomboidNetData(int var1) {
      this.buffer = ByteBuffer.allocate(var1);
   }

   public void reset() {
      this.type = null;
      this.length = 0;
      this.connection = 0L;
      this.buffer.clear();
   }

   public void read(short var1, ByteBuffer var2, UdpConnection var3) {
      this.type = (PacketTypes.PacketType)PacketTypes.packetTypes.get(var1);
      if (this.type == null) {
         DebugLog.Multiplayer.error("Received unknown packet id=%d", var1);
      }

      this.connection = var3.getConnectedGUID();
      this.buffer.put(var2);
      this.buffer.flip();
   }

   public boolean isConnect() {
      return false;
   }

   public boolean isDisconnect() {
      return false;
   }
}
