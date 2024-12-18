package zombie.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketTypes;
import zombie.network.fields.Square;

public class FishingAction extends Action {
   public static byte flagStartFishing = 1;
   public static byte flagStopFishing = 2;
   public static byte flagUpdateFish = 4;
   public static byte flagUpdateBobberParameters = 8;
   public static byte flagCreateBobber = 16;
   public static byte flagDestroyBobber = 32;
   private static HashMap<IsoPlayer, InventoryItem> fishForPickUp = new HashMap();
   @JSONField
   public byte contentFlag = 0;
   @JSONField
   int fishingRodId;
   @JSONField
   Square position = new Square();
   private KahluaTable fishingRod;
   private KahluaTable bobber;
   private KahluaTable currentFish = null;
   private InventoryItem currentFishItem = null;
   private InventoryItem lastSentFishItem = null;
   private float currentBobberX = 0.0F;
   private float currentBobberY = 0.0F;
   private float lastBobberX = 0.0F;
   private float lastBobberY = 0.0F;
   private InventoryItem currentBobber = null;
   private InventoryItem lastBobber = null;
   KahluaTable tbl;

   public FishingAction() {
      this.tbl = LuaManager.platform.newTable();
   }

   public void setStartFishing(IsoPlayer var1, InventoryItem var2, IsoGridSquare var3, KahluaTable var4) {
      super.set(var1);
      this.contentFlag |= flagStartFishing;
      if (var2 == null) {
         this.fishingRodId = 0;
      } else {
         this.fishingRodId = var2.getID();
      }

      this.position.set(var3);
      this.bobber = var4;
   }

   float getDuration() {
      return 100000.0F;
   }

   void start() {
      fishForPickUp.put(super.playerID.getPlayer(), (Object)null);
      this.setTimeData();
      KahluaTable var1 = (KahluaTable)LuaManager.env.rawget("Fishing");
      KahluaTable var2 = (KahluaTable)var1.rawget("FishingRod");
      this.fishingRod = (KahluaTable)LuaManager.caller.pcall(LuaManager.thread, var2.rawget("new"), new Object[]{var2, this.playerID.getPlayer()})[1];
      this.fishingRod.rawset("mpAimX", (double)this.position.getX());
      this.fishingRod.rawset("mpAimY", (double)this.position.getY());
      LuaManager.caller.pcall(LuaManager.thread, this.fishingRod.rawget("cast"), this.fishingRod);
      this.bobber = (KahluaTable)this.fishingRod.rawget("bobber");
      DebugLog.Action.trace("FishingAction.start %s", this.getDescription());
   }

   void stop() {
      if (this.bobber != null) {
         LuaManager.caller.pcall(LuaManager.thread, this.bobber.rawget("destroy"), this.bobber);
      }

      DebugLog.Action.trace("FishingAction.stop %s", this.getDescription());
   }

   boolean isValid() {
      return true;
   }

   boolean isUsingTimeout() {
      return true;
   }

   void update() {
      this.setTimeData();
      boolean var1 = false;
      if (GameClient.bClient) {
         if (this.state == Transaction.TransactionState.Accept) {
            this.currentBobberX = ((Double)LuaManager.caller.pcall(LuaManager.thread, this.bobber.rawget("getX"), this.bobber)[1]).floatValue();
            this.currentBobberY = ((Double)LuaManager.caller.pcall(LuaManager.thread, this.bobber.rawget("getY"), this.bobber)[1]).floatValue();
            if (PZMath.fastfloor(this.lastBobberX) != PZMath.fastfloor(this.currentBobberX) || PZMath.fastfloor(this.lastBobberY) != PZMath.fastfloor(this.currentBobberY)) {
               this.contentFlag |= flagUpdateBobberParameters;
               var1 = true;
            }

            if (var1) {
               ByteBufferWriter var4 = GameClient.connection.startPacket();
               PacketTypes.PacketType.FishingAction.doPacket(var4);
               this.write(var4);
               PacketTypes.PacketType.FishingAction.send(GameClient.connection);
               this.contentFlag = 0;
            }

         }
      } else {
         LuaManager.caller.pcall(LuaManager.thread, this.fishingRod.rawget("update"), this.fishingRod);
         if (this.bobber == null) {
            this.bobber = (KahluaTable)this.fishingRod.rawget("bobber");
         } else {
            this.currentFish = (KahluaTable)this.bobber.rawget("fish");
            if (this.currentFish != null) {
               this.currentFishItem = (InventoryItem)this.currentFish.rawget("fishItem");
            } else {
               this.currentFishItem = null;
            }

            this.currentBobber = (InventoryItem)this.bobber.rawget("item");
            this.currentBobberX = ((Double)LuaManager.caller.pcall(LuaManager.thread, this.bobber.rawget("getX"), this.bobber)[1]).floatValue();
            this.currentBobberY = ((Double)LuaManager.caller.pcall(LuaManager.thread, this.bobber.rawget("getY"), this.bobber)[1]).floatValue();
            if (this.currentFishItem != this.lastSentFishItem) {
               this.contentFlag |= flagUpdateFish;
               var1 = true;
               fishForPickUp.put(super.playerID.getPlayer(), this.currentFishItem);
            }

            if (this.lastBobber == null && this.currentBobber != null) {
               this.contentFlag |= flagCreateBobber;
               var1 = true;
            }

            if (var1) {
               UdpConnection var2 = GameServer.getConnectionFromPlayer(this.playerID.getPlayer());
               ByteBufferWriter var3 = var2.startPacket();
               PacketTypes.PacketType.FishingAction.doPacket(var3);
               this.write(var3);
               PacketTypes.PacketType.FishingAction.send(var2);
               this.contentFlag = 0;
            }
         }

      }
   }

   boolean perform() {
      return false;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      this.contentFlag = var1.get();
      if ((this.contentFlag & flagStartFishing) != 0) {
         this.fishingRodId = var1.getInt();
         this.position.parse(var1, var2);
      }

      if ((this.contentFlag & flagUpdateFish) != 0) {
         boolean var3 = var1.get() != 0;
         if (var3) {
            try {
               this.currentFish = LuaManager.platform.newTable();
               this.currentFish.load(var1, 219);
               this.currentFishItem = InventoryItem.loadItem(var1, 219);
            } catch (Exception var10) {
               DebugLog.Objects.printException(var10, this.getDescription(), LogSeverity.Error);
            }
         } else {
            this.currentFish = null;
            this.currentFishItem = null;
         }
      }

      if ((this.contentFlag & flagUpdateBobberParameters) != 0) {
         this.currentBobberX = var1.getFloat();
         this.currentBobberY = var1.getFloat();
      }

      if ((this.contentFlag & flagCreateBobber) != 0) {
         int var11 = var1.getInt();
         int var4 = var1.getInt();
         byte var5 = var1.get();
         int var6 = var1.getInt();
         IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var11, var4, var5);

         for(int var8 = 0; var8 < var7.getWorldObjects().size(); ++var8) {
            IsoWorldInventoryObject var9 = (IsoWorldInventoryObject)var7.getWorldObjects().get(var8);
            if (var9.getItem() != null && var9.getItem().getID() == var6) {
               this.currentBobber = var9.getItem();
               return;
            }
         }
      }

      DebugLog.Action.trace("FishingAction.parse: %s", this.getDescription());
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      var1.putByte(this.contentFlag);
      if ((this.contentFlag & flagStartFishing) != 0) {
         var1.putInt(this.fishingRodId);
         this.position.write(var1);
      }

      if ((this.contentFlag & flagUpdateFish) != 0) {
         if (this.currentFishItem != null) {
            var1.putByte((byte)1);

            try {
               this.currentFish.save(var1.bb);
               this.currentFishItem.saveWithSize(var1.bb, false);
            } catch (IOException var6) {
               DebugLog.Objects.printException(var6, this.getDescription(), LogSeverity.Error);
            }
         } else {
            var1.putByte((byte)0);
         }

         this.lastSentFishItem = this.currentFishItem;
      }

      if ((this.contentFlag & flagUpdateBobberParameters) != 0) {
         var1.putFloat(this.currentBobberX);
         var1.putFloat(this.currentBobberY);
         this.lastBobberX = this.currentBobberX;
         this.lastBobberY = this.currentBobberY;
      }

      if ((this.contentFlag & flagCreateBobber) != 0) {
         int var2 = this.currentBobber.getWorldItem().getSquare().getX();
         int var3 = this.currentBobber.getWorldItem().getSquare().getY();
         byte var4 = (byte)this.currentBobber.getWorldItem().getSquare().getZ();
         int var5 = this.currentBobber.getWorldItem().getItem().getID();
         var1.putInt(var2);
         var1.putInt(var3);
         var1.putByte(var4);
         var1.putInt(var5);
         this.lastBobber = this.currentBobber;
      }

      DebugLog.Action.trace("FishingAction.write: %s", this.getDescription());
   }

   public KahluaTable getLuaTable() {
      this.tbl.wipe();
      if (ActionManager.getPlayer(this.id) == null) {
         return null;
      } else {
         this.tbl.rawset("player", ActionManager.getPlayer(this.id));
         this.tbl.rawset("Reject", this.state == Transaction.TransactionState.Reject);
         this.tbl.rawset("UpdateFish", (this.contentFlag & flagUpdateFish) != 0);
         this.tbl.rawset("UpdateBobberParameters", (this.contentFlag & flagUpdateBobberParameters) != 0);
         this.tbl.rawset("CreateBobber", (this.contentFlag & flagCreateBobber) != 0);
         this.tbl.rawset("DestroyBobber", (this.contentFlag & flagDestroyBobber) != 0);
         if ((this.contentFlag & flagUpdateFish) != 0) {
            this.tbl.rawset("fish", this.currentFish);
            this.tbl.rawset("fishItem", this.currentFishItem);
         }

         if ((this.contentFlag & flagCreateBobber) != 0) {
            this.tbl.rawset("bobberItem", this.currentBobber);
         }

         if ((this.contentFlag & flagUpdateBobberParameters) != 0) {
            this.tbl.rawset("bobberX", String.valueOf(this.currentBobberX));
            this.tbl.rawset("bobberY", String.valueOf(this.currentBobberY));
         }

         return this.tbl;
      }
   }

   public static InventoryItem getPickedUpFish(IsoPlayer var0) {
      return (InventoryItem)fishForPickUp.get(var0);
   }
}
