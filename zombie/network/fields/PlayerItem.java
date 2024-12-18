package zombie.network.fields;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.network.JSONField;

public class PlayerItem extends IDShort implements INetworkPacketField {
   @JSONField
   protected InventoryItem item;

   public PlayerItem() {
   }

   public void set(InventoryItem var1) {
      this.item = var1;
      if (var1 == null) {
         super.setID((short)-1);
      } else {
         super.setID(var1.getRegistry_id());
      }

   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      boolean var3 = var1.get() == 1;
      if (var3) {
         this.setID(var1.getShort());
         var1.get();

         try {
            this.item = InventoryItemFactory.CreateItem(this.getID());
            if (this.item != null) {
               this.item.load(var1, 219);
            }
         } catch (BufferUnderflowException | IOException var5) {
            DebugLog.Multiplayer.printException(var5, "Item load error", LogSeverity.Error);
            this.item = null;
         }
      } else {
         this.item = null;
      }

   }

   public void write(ByteBufferWriter var1) {
      if (this.item == null) {
         var1.putByte((byte)0);
      } else {
         var1.putByte((byte)1);

         try {
            this.item.save(var1.bb, false);
         } catch (IOException var3) {
            DebugLog.Multiplayer.printException(var3, "Item write error", LogSeverity.Error);
         }
      }

   }

   public boolean isConsistent(UdpConnection var1) {
      return super.isConsistent(var1) && this.item != null;
   }

   public InventoryItem getItem() {
      return this.item;
   }
}
