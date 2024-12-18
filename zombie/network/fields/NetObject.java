package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.network.JSONField;

public class NetObject implements INetworkPacketField {
   private final byte objectTypeNone = 0;
   private final byte objectTypeObject = 1;
   @JSONField
   private byte objectType = 0;
   @JSONField
   private short objectId;
   @JSONField
   private int squareX;
   @JSONField
   private int squareY;
   @JSONField
   private byte squareZ;
   private boolean isProcessed = false;
   private IsoObject object;

   public NetObject() {
   }

   public void setObject(IsoObject var1) {
      this.object = var1;
      this.isProcessed = true;
      if (this.object == null) {
         this.objectType = 0;
         this.objectId = 0;
      } else {
         IsoGridSquare var2 = this.object.square;
         this.objectType = 1;
         this.objectId = (short)var2.getObjects().indexOf(this.object);
         this.squareX = var2.getX();
         this.squareY = var2.getY();
         this.squareZ = (byte)var2.getZ();
      }
   }

   public IsoObject getObject() {
      if (!this.isProcessed) {
         if (this.objectType == 0) {
            this.object = null;
         }

         if (this.objectType == 1) {
            IsoGridSquare var1 = IsoWorld.instance.CurrentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
            if (var1 == null) {
               this.object = null;
            } else {
               this.object = (IsoObject)var1.getObjects().get(this.objectId);
            }
         }

         this.isProcessed = true;
      }

      return this.object;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.objectType = var1.get();
      if (this.objectType == 1) {
         this.objectId = var1.getShort();
         this.squareX = var1.getInt();
         this.squareY = var1.getInt();
         this.squareZ = var1.get();
      }

      this.isProcessed = false;
   }

   public void write(ByteBufferWriter var1) {
      this.write(var1.bb);
   }

   public void write(ByteBuffer var1) {
      var1.put(this.objectType);
      if (this.objectType == 1) {
         var1.putShort(this.objectId);
         var1.putInt(this.squareX);
         var1.putInt(this.squareY);
         var1.put(this.squareZ);
      }

   }

   public int getPacketSizeBytes() {
      return this.objectType == 1 ? 12 : 1;
   }
}
