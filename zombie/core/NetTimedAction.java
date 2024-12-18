package zombie.core;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Prototype;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.JSONField;
import zombie.network.PZNetKahluaNull;
import zombie.network.PZNetKahluaTableImpl;

public class NetTimedAction extends Action {
   @JSONField
   public String type = "";
   @JSONField
   public String name = "";
   public KahluaTable action = null;
   @JSONField
   PZNetKahluaTableImpl actionArgs = null;
   @JSONField
   boolean isUsingTimeout = true;

   public NetTimedAction() {
   }

   public void set(IsoPlayer var1, KahluaTable var2) {
      this.action = var2;
      this.type = this.action.getMetatable().getString("Type");
      this.name = this.action.getString("name");
      this.actionArgs = new PZNetKahluaTableImpl(new LinkedHashMap());
      Prototype var3 = ((LuaClosure)this.action.getMetatable().rawget("new")).prototype;

      for(int var4 = 1; var4 < var3.numParams; ++var4) {
         String var5 = var3.locvars[var4];
         Object var6 = this.action.rawget(var5);
         if (var6 == null) {
            var6 = PZNetKahluaNull.instance;
         }

         this.actionArgs.rawset(var5, var6);
      }

      this.isUsingTimeout = LuaManager.caller.protectedCallBoolean(LuaManager.thread, var2.rawget("isUsingTimeout"), var2);
      super.set(var1);
   }

   float getDuration() {
      if (this.action == null) {
         return 0.0F;
      } else {
         LuaReturn var1 = LuaManager.caller.protectedCall(LuaManager.thread, this.action.rawget("getDuration"), new Object[]{this.action});
         if (var1.isSuccess()) {
            float var2 = ((Double)var1.getFirst()).floatValue();
            return var2 == -1.0F ? -1.0F : var2 * 20.0F;
         } else {
            return 0.0F;
         }
      }
   }

   void start() {
      this.setTimeData();
      Object var1 = this.action.rawget("serverStart");
      if (var1 != null) {
         LuaReturn var2 = LuaManager.caller.protectedCall(LuaManager.thread, var1, new Object[]{this.action});
         if (!var2.isSuccess()) {
            DebugLog.Action.warn("Get function object \"serverStart\" failed");
         }
      }

   }

   void stop() {
      Object var1 = this.action.rawget("serverStop");
      if (var1 != null) {
         LuaReturn var2 = LuaManager.caller.protectedCall(LuaManager.thread, var1, new Object[]{this.action});
         if (!var2.isSuccess()) {
            DebugLog.Action.warn("Get function object \"serverStop\" failed");
         }
      }

   }

   boolean isValid() {
      return false;
   }

   boolean isUsingTimeout() {
      return this.isUsingTimeout;
   }

   void update() {
   }

   boolean perform() {
      try {
         return LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.action.rawget("complete"), this.action);
      } catch (Exception var2) {
         DebugLog.Action.printException(var2, "Perform filed", LogSeverity.Error);
         return false;
      }
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      if (this.state == Transaction.TransactionState.Request) {
         this.type = GameWindow.ReadString(var1).trim();
         this.name = GameWindow.ReadString(var1).trim();
         this.actionArgs = new PZNetKahluaTableImpl(new LinkedHashMap());
         this.actionArgs.load(var1, var2);
         Object var3 = LuaManager.get(this.type);
         Object var4 = LuaManager.getFunctionObject(this.type + ".new");
         byte var5 = (byte)(this.actionArgs.size() + 1);
         Object[] var6 = new Object[var5];
         int var7 = 1;
         var6[0] = var3;
         KahluaTableIterator var8 = this.actionArgs.iterator();

         while(true) {
            if (!var8.advance()) {
               LuaReturn var9 = LuaManager.caller.protectedCall(LuaManager.thread, var4, var6);
               if (!var9.isSuccess() || var9.getFirst() == null) {
                  this.action = null;
                  return;
               }

               this.action = (KahluaTable)var9.getFirst();
               this.action.rawset("name", this.name);
               this.action.rawset("netAction", this);
               break;
            }

            var6[var7++] = var8.getValue();
         }
      }

   }

   public void forceComplete() {
      this.endTime = GameTime.getServerTimeMills();
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      if (this.state == Transaction.TransactionState.Request) {
         var1.putUTF(this.type);
         var1.putUTF(this.name);
         this.actionArgs.save(var1.bb);
      }

   }

   public void animEvent(String var1, String var2) {
      Object var3 = this.action.rawget("animEvent");
      if (var3 != null) {
         LuaManager.caller.protectedCallVoid(LuaManager.thread, var3, this.action, var1, var2);
      }

   }
}
