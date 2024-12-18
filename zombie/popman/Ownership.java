package zombie.popman;

import zombie.core.raknet.UdpConnection;

public class Ownership {
   private long timestamp = -1L;
   private UdpConnection connection = null;

   public Ownership() {
   }

   public void setOwnership(UdpConnection var1) {
      this.connection = var1;
      this.timestamp = System.currentTimeMillis();
   }

   public UdpConnection getConnection() {
      return this.connection;
   }

   public boolean isBlocked(int var1) {
      return System.currentTimeMillis() - this.timestamp < (long)var1 && this.connection != null;
   }
}
