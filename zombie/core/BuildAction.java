package zombie.core;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Prototype;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PZNetKahluaTableImpl;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public class BuildAction extends Action {
   @JSONField
   float x;
   @JSONField
   float y;
   @JSONField
   float z;
   @JSONField
   boolean north;
   @JSONField
   String spriteName;
   public KahluaTable item;
   @JSONField
   String objectType;
   IsoGridSquare square = null;
   private KahluaTable argTable;

   public BuildAction() {
   }

   public void set(IsoPlayer var1, float var2, float var3, float var4, boolean var5, String var6, KahluaTable var7) {
      this.x = var2;
      this.y = var3;
      this.z = var4;
      this.north = var5;
      this.spriteName = var6;
      this.item = var7;
      super.set(var1);
   }

   public void start() {
      this.setTimeData();
      this.square = IsoWorld.instance.CurrentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z);
      this.argTable = LuaManager.platform.newTable();
      this.argTable.rawset("x", (double)this.x);
      this.argTable.rawset("y", (double)this.y);
      this.argTable.rawset("z", (double)this.z);
      this.argTable.rawset("north", this.north);
      this.argTable.rawset("spriteName", this.spriteName);
      this.argTable.rawset("item", this.item);
   }

   public void stop() {
   }

   public boolean isValid() {
      return this.playerID.isConsistent((UdpConnection)null) && !this.spriteName.isBlank() ? LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.item.rawget("isValid"), this.item, this.square) : false;
   }

   public boolean isUsingTimeout() {
      return true;
   }

   public float getDuration() {
      float var1 = (float)(200 - this.playerID.getPlayer().getPerkLevel(PerkFactory.Perks.Woodwork) * 5);
      if (this.item.getMetatable().getString("Type").equals("ISEmptyGraves")) {
         var1 = 150.0F;
      }

      if (this.playerID.getPlayer().isTimedActionInstant()) {
         var1 = 1.0F;
      }

      if (this.playerID.getPlayer().HasTrait("Handy")) {
         var1 -= 50.0F;
      }

      return var1 * 20.0F;
   }

   public void update() {
      if (!GameClient.bClient) {
         this.playerID.getPlayer().setMetabolicTarget(Metabolics.HeavyWork);
      }
   }

   public boolean perform() {
      InventoryItem var1 = this.playerID.getPlayer().getPrimaryHandItem();
      if (var1 != null && var1.getType().equals("HammerStone") && LuaManager.GlobalObject.ZombRand((double)((HandWeapon)var1).getConditionLowerChance()) == 0.0) {
         var1.setCondition(var1.getCondition() - 1);
         INetworkPacket.send(PacketTypes.PacketType.SyncItemFields, this.playerID.getPlayer(), var1);
      }

      LuaEventManager.triggerEvent("OnProcessAction", "build", this.playerID.getPlayer(), this.argTable);
      return true;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      if (this.state == Transaction.TransactionState.Request) {
         this.x = var1.getFloat();
         this.y = var1.getFloat();
         this.z = var1.getFloat();
         this.north = var1.get() != 0;
         this.spriteName = GameWindow.ReadString(var1);
         PZNetKahluaTableImpl var3 = new PZNetKahluaTableImpl(new LinkedHashMap());
         var3.load(var1, var2);
         this.objectType = var3.getString("Type");
         Object var4 = LuaManager.get(this.objectType);
         Object var5 = LuaManager.getFunctionObject(this.objectType + ".new");
         PZNetKahluaTableImpl var6 = (PZNetKahluaTableImpl)var3.rawget("Arguments");
         if ("TrapBO".equals(this.objectType)) {
            var6.rawset("player", this.playerID.getPlayer());
         }

         byte var7 = (byte)(var6.size() + 1);
         Object[] var8 = new Object[var7];
         int var9 = 1;
         var8[0] = var4;

         for(KahluaTableIterator var10 = var6.iterator(); var10.advance(); var8[var9++] = var10.getValue()) {
         }

         LuaReturn var11 = LuaManager.caller.protectedCall(LuaManager.thread, var5, var8);
         if (var11.getFirst() == null) {
            this.item = null;
            return;
         }

         this.item = (KahluaTable)var11.getFirst();
         Object var12 = var3.rawget("Name");
         if (var12 instanceof String) {
            this.item.rawset("name", var12);
         }

         this.item.rawset("player", this.playerID.getPlayer());
      }

   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      if (this.state == Transaction.TransactionState.Request) {
         var1.putFloat(this.x);
         var1.putFloat(this.y);
         var1.putFloat(this.z);
         var1.putByte((byte)(this.north ? 1 : 0));
         var1.putUTF(this.spriteName);
         PZNetKahluaTableImpl var2 = new PZNetKahluaTableImpl(new LinkedHashMap());
         PZNetKahluaTableImpl var3 = new PZNetKahluaTableImpl(new LinkedHashMap());
         Prototype var4 = ((LuaClosure)this.item.getMetatable().rawget("new")).prototype;

         for(int var5 = 1; var5 < var4.numParams; ++var5) {
            String var6 = var4.locvars[var5];
            var3.rawset(var6, this.item.rawget(var6));
         }

         this.objectType = this.item.getMetatable().getString("Type");
         var2.rawset("Type", this.objectType);
         if (this.item.rawget("name") instanceof String) {
            var2.rawset("Name", this.item.getString("name"));
         }

         var2.rawset("Arguments", var3);
         var2.save(var1.bb);
      }

   }
}
