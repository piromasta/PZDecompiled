package zombie.core;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.PlayerID;
import zombie.network.server.AnimEventEmulator;

abstract class Action implements INetworkPacketField {
   protected static int TimeoutForInfinitiveActions = 1000000;
   protected static byte lastId = 0;
   @JSONField
   protected byte id = 0;
   @JSONField
   protected Transaction.TransactionState state;
   @JSONField
   PlayerID playerID = new PlayerID();
   @JSONField
   public long duration = 0L;
   protected long startTime;
   protected long endTime;

   Action() {
   }

   public void setTimeData() {
      this.startTime = GameTime.getServerTimeMills();
      this.duration = (long)this.getDuration();
      if (this.duration < 0L) {
         this.endTime = this.startTime + AnimEventEmulator.getInstance().getDurationMax();
      } else {
         this.endTime = this.startTime + this.duration;
      }

   }

   public void set(IsoPlayer var1) {
      this.state = Transaction.TransactionState.Request;
      if (lastId == 0) {
         ++lastId;
      }

      byte var10001 = lastId;
      lastId = (byte)(var10001 + 1);
      this.id = var10001;
      this.playerID.set(var1);
      this.setTimeData();
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.id = var1.get();
      this.state = Transaction.TransactionState.values()[var1.get()];
      if (this.state == Transaction.TransactionState.Request || this.state == Transaction.TransactionState.Reject) {
         this.playerID.parse(var1, var2);
         this.playerID.parsePlayer(var2);
      }

      if (this.state == Transaction.TransactionState.Accept) {
         this.duration = var1.getLong();
      }

   }

   public void write(ByteBufferWriter var1) {
      var1.putByte(this.id);
      var1.putByte((byte)this.state.ordinal());
      if (this.state == Transaction.TransactionState.Request || this.state == Transaction.TransactionState.Reject) {
         this.playerID.write(var1);
      }

      if (this.state == Transaction.TransactionState.Accept) {
         var1.putLong(this.duration);
      }

   }

   public void setState(Transaction.TransactionState var1) {
      this.state = var1;
   }

   public void setDuration(long var1) {
      this.endTime = this.startTime + var1;
   }

   public boolean isConsistent(UdpConnection var1) {
      return this.state == Transaction.TransactionState.Request ? this.playerID.isConsistent(var1) : true;
   }

   public float getProgress() {
      return this.endTime == this.startTime ? 1.0F : (float)(GameTime.getServerTimeMills() - this.startTime) / (float)(this.endTime - this.startTime);
   }

   abstract float getDuration();

   abstract void start();

   abstract void stop();

   abstract boolean isValid();

   abstract void update();

   abstract boolean perform();

   abstract boolean isUsingTimeout();
}
