package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoWindow;
import zombie.network.JSONField;

public class HitInfo implements INetworkPacketField {
   @JSONField
   public float x;
   @JSONField
   public float y;
   @JSONField
   public float z;
   @JSONField
   public float dot;
   @JSONField
   public float distSq;
   @JSONField
   public int chance;
   @JSONField
   public MovingObject object = new MovingObject();
   @JSONField
   public NetObject window = new NetObject();

   public HitInfo() {
   }

   public HitInfo init(IsoMovingObject var1, float var2, float var3, float var4, float var5, float var6) {
      this.object = new MovingObject();
      this.window = new NetObject();
      this.object.setMovingObject(var1);
      this.window.setObject((IsoObject)null);
      this.x = var4;
      this.y = var5;
      this.z = var6;
      this.dot = var2;
      this.distSq = var3;
      return this;
   }

   public HitInfo init(IsoWindow var1, float var2, float var3) {
      this.object = new MovingObject();
      this.window = new NetObject();
      this.object.setMovingObject((IsoMovingObject)null);
      this.window.setObject(var1);
      this.z = var1.getZ();
      this.dot = var2;
      this.distSq = var3;
      return this;
   }

   public HitInfo init(HitInfo var1) {
      this.object = new MovingObject();
      this.window = new NetObject();
      this.object.setMovingObject(var1.object.getMovingObject());
      this.window.setObject(var1.window.getObject());
      this.x = var1.x;
      this.y = var1.y;
      this.z = var1.z;
      this.dot = var1.dot;
      this.distSq = var1.distSq;
      return this;
   }

   public IsoMovingObject getObject() {
      return this.object.getMovingObject();
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.object.parse(var1, var2);
      this.window.parse(var1, var2);
      this.x = var1.getFloat();
      this.y = var1.getFloat();
      this.z = var1.getFloat();
      this.dot = var1.getFloat();
      this.distSq = var1.getFloat();
      this.chance = var1.getInt();
   }

   public void write(ByteBufferWriter var1) {
      this.object.write(var1);
      this.window.write(var1);
      var1.putFloat(this.x);
      var1.putFloat(this.y);
      var1.putFloat(this.z);
      var1.putFloat(this.dot);
      var1.putFloat(this.distSq);
      var1.putInt(this.chance);
   }

   public int getPacketSizeBytes() {
      return 24 + this.object.getPacketSizeBytes() + this.window.getPacketSizeBytes();
   }
}
