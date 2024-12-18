package zombie.network.packets;

import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.PlayerID;
import zombie.network.fields.Square;
import zombie.network.fields.Vehicle;
import zombie.vehicles.BaseVehicle;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2
)
public class ObjectChangePacket implements INetworkPacket {
   @JSONField
   IsoObject o;
   @JSONField
   String change;
   @JSONField
   KahluaTable tbl;

   public ObjectChangePacket() {
   }

   public void setData(Object... var1) {
      this.set((IsoObject)var1[0], (String)var1[1], (KahluaTable)var1[2]);
   }

   private void set(IsoObject var1, String var2, KahluaTable var3) {
      this.o = var1;
      this.change = var2;
      this.tbl = var3;
   }

   public void write(ByteBufferWriter var1) {
      if (this.o instanceof IsoPlayer) {
         var1.putByte((byte)1);
         PlayerID var2 = new PlayerID();
         var2.set((IsoPlayer)this.o);
         var2.write(var1);
      } else if (this.o instanceof BaseVehicle) {
         var1.putByte((byte)2);
         Vehicle var3 = new Vehicle();
         var3.set((BaseVehicle)this.o);
         var3.write(var1);
      } else {
         Square var4;
         if (this.o instanceof IsoWorldInventoryObject) {
            var1.putByte((byte)3);
            var4 = new Square();
            var4.set(this.o.getSquare());
            var4.write(var1);
            var1.putInt(((IsoWorldInventoryObject)this.o).getItem().getID());
         } else if (this.o instanceof IsoDeadBody) {
            var1.putByte((byte)4);
            var4 = new Square();
            var4.set(this.o.getSquare());
            var4.write(var1);
            var1.putInt(this.o.getStaticMovingObjectIndex());
         } else {
            var1.putByte((byte)0);
            var4 = new Square();
            var4.set(this.o.getSquare());
            var4.write(var1);
            var1.putInt(this.o.getSquare().getObjects().indexOf(this.o));
         }
      }

      var1.putUTF(this.change);
      this.o.saveChange(this.change, this.tbl, var1.bb);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      byte var3 = var1.get();
      String var5;
      if (var3 == 1) {
         PlayerID var4 = new PlayerID();
         var4.parse(var1, var2);
         var4.parsePlayer(var2);
         this.o = var4.getPlayer();
         var5 = GameWindow.ReadString(var1);
         if (Core.bDebug) {
            DebugLog.log("receiveObjectChange " + var5);
         }

         if (var4.isConsistent(var2)) {
            var4.getPlayer().loadChange(var5, var1);
         } else if (Core.bDebug) {
            DebugLog.log("receiveObjectChange: player can't be found=" + this.getDescription());
         }
      } else if (var3 == 2) {
         Vehicle var9 = new Vehicle();
         var9.parse(var1, var2);
         this.o = var9.getVehicle();
         var5 = GameWindow.ReadString(var1);
         if (Core.bDebug) {
            DebugLog.log("receiveObjectChange " + var5);
         }

         if (var9.isConsistent(var2)) {
            var9.getVehicle().loadChange(var5, var1);
         } else if (Core.bDebug) {
            DebugLog.log("receiveObjectChange: vehicle can't be found=" + this.getDescription());
         }
      } else {
         String var6;
         Square var10;
         int var11;
         String var10000;
         if (var3 == 3) {
            var10 = new Square();
            var10.parse(var1, var2);
            var11 = var1.getInt();
            var6 = GameWindow.ReadString(var1);
            if (Core.bDebug) {
               DebugLog.log("receiveObjectChange " + var6);
            }

            if (!var10.isConsistent(var2)) {
               GameClient.instance.delayPacket((int)var10.getX(), (int)var10.getY(), (int)var10.getZ());
               return;
            }

            for(int var7 = 0; var7 < var10.getSquare().getWorldObjects().size(); ++var7) {
               IsoWorldInventoryObject var8 = (IsoWorldInventoryObject)var10.getSquare().getWorldObjects().get(var7);
               if (var8.getItem() != null && var8.getItem().getID() == var11) {
                  var8.loadChange(var6, var1);
                  return;
               }
            }

            if (Core.bDebug) {
               var10000 = var10.getDescription();
               DebugLog.log("receiveObjectChange: object can't be found (square=" + var10000 + ", itemID=" + var11 + ")");
            }
         } else if (var3 == 4) {
            var10 = new Square();
            var10.parse(var1, var2);
            var11 = var1.getInt();
            var6 = GameWindow.ReadString(var1);
            if (!var10.isConsistent(var2)) {
               GameClient.instance.delayPacket((int)var10.getX(), (int)var10.getY(), (int)var10.getZ());
               return;
            }

            if (var11 >= 0 && var11 < var10.getSquare().getStaticMovingObjects().size()) {
               this.o = (IsoObject)var10.getSquare().getStaticMovingObjects().get(var11);
               this.o.loadChange(var6, var1);
            } else if (Core.bDebug) {
               var10000 = var10.getDescription();
               DebugLog.log("receiveObjectChange: object can't be found (square=" + var10000 + ", index=" + var11 + ")");
            }
         } else {
            var10 = new Square();
            var10.parse(var1, var2);
            var11 = var1.getInt();
            var6 = GameWindow.ReadString(var1);
            if (Core.bDebug) {
               DebugLog.log("receiveObjectChange " + var6);
            }

            if (!var10.isConsistent(var2)) {
               GameClient.instance.delayPacket((int)var10.getX(), (int)var10.getY(), (int)var10.getZ());
               return;
            }

            if (var11 >= 0 && var11 < var10.getSquare().getObjects().size()) {
               this.o = (IsoObject)var10.getSquare().getObjects().get(var11);
               this.o.loadChange(var6, var1);
            } else if (Core.bDebug) {
               var10000 = var10.getDescription();
               DebugLog.log("receiveObjectChange: object can't be found (square=" + var10000 + ", index=" + var11 + ")");
            }
         }
      }

   }
}
