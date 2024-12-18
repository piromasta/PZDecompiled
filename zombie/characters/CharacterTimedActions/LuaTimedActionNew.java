package zombie.characters.CharacterTimedActions;

import java.util.ArrayList;
import java.util.Iterator;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.ai.astar.IPathfinder;
import zombie.ai.astar.Mover;
import zombie.ai.astar.Path;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.MoveDeltaModifiers;
import zombie.chat.ChatManager;
import zombie.core.ActionManager;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;

public final class LuaTimedActionNew extends BaseAction implements IPathfinder {
   KahluaTable table;
   boolean useCustomRemoteTimedActionSync = false;
   byte transactionId = -1;
   boolean started = false;
   private static final ArrayList<Object> keys = new ArrayList();

   public LuaTimedActionNew(KahluaTable var1, IsoGameCharacter var2) {
      super(var2);
      this.table = var1;
      Object var3 = var1.rawget("maxTime");
      this.MaxTime = (Integer)LuaManager.converterManager.fromLuaToJava(var3, Integer.class);
      Object var4 = var1.rawget("stopOnWalk");
      Object var5 = var1.rawget("stopOnRun");
      Object var6 = var1.rawget("stopOnAim");
      Object var7 = var1.rawget("caloriesModifier");
      Object var8 = var1.rawget("useProgressBar");
      Object var9 = var1.rawget("forceProgressBar");
      Object var10 = var1.rawget("loopedAction");
      if (var4 != null) {
         this.StopOnWalk = (Boolean)LuaManager.converterManager.fromLuaToJava(var4, Boolean.class);
      }

      if (var5 != null) {
         this.StopOnRun = (Boolean)LuaManager.converterManager.fromLuaToJava(var5, Boolean.class);
      }

      if (var6 != null) {
         this.StopOnAim = (Boolean)LuaManager.converterManager.fromLuaToJava(var6, Boolean.class);
      }

      if (var7 != null) {
         this.caloriesModifier = (Float)LuaManager.converterManager.fromLuaToJava(var7, Float.class);
      }

      if (var8 != null) {
         this.UseProgressBar = (Boolean)LuaManager.converterManager.fromLuaToJava(var8, Boolean.class);
      }

      if (var9 != null) {
         this.ForceProgressBar = (Boolean)LuaManager.converterManager.fromLuaToJava(var9, Boolean.class);
      }

      if (var10 != null) {
         this.loopAction = (Boolean)LuaManager.converterManager.fromLuaToJava(var10, Boolean.class);
      }

      if (var1.getMetatable().rawget("complete") == null) {
         this.useCustomRemoteTimedActionSync = true;
      }

   }

   public void waitToStart() {
      Boolean var1 = LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.table.rawget("waitToStart"), this.table);
      if (var1 == Boolean.FALSE) {
         super.waitToStart();
      }

   }

   public void update() {
      super.update();
      LuaManager.caller.pcallvoid(LuaManager.thread, this.table.rawget("update"), this.table);
      if (GameClient.bClient && !this.useCustomRemoteTimedActionSync) {
         if (ActionManager.isDone(this.transactionId)) {
            this.forceComplete();
         } else if (ActionManager.isRejected(this.transactionId)) {
            this.forceStop();
         }

         if (this.getTime() == -1) {
            float var1 = (float)ActionManager.getDuration(this.transactionId) / 20.0F;
            if (var1 > 0.0F && !ActionManager.isLooped(this.transactionId)) {
               this.table.rawset("maxTime", (double)var1);
               this.setTime((int)var1);
            }
         }
      }

   }

   public boolean valid() {
      Object[] var1 = LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("isValid"), this.table);
      return var1.length > 1 && var1[1] instanceof Boolean && (Boolean)var1[1];
   }

   public void start() {
      DebugLog.Action.trace("%s: start", this.table.rawget("Type"));
      super.start();
      this.CurrentTime = 0.0F;
      LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("start"), this.table);
      if (GameClient.bClient && !this.useCustomRemoteTimedActionSync) {
         this.setWaitForFinished(true);
         this.transactionId = ActionManager.getInstance().createNetTimedAction((IsoPlayer)this.chr, this.table);
         this.started = true;
      }

      if (GameClient.bClient && DebugLog.isLogEnabled(DebugType.Action, LogSeverity.Trace)) {
         ChatManager.getInstance().showServerChatMessage(" -> " + this.table.rawget("Type"));
      }

   }

   public void stop() {
      DebugLog.Action.trace("%s: stop", this.table.rawget("Type"));
      super.stop();
      LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("stop"), this.table);
      if (GameClient.bClient && !this.useCustomRemoteTimedActionSync) {
         ActionManager.remove(this.transactionId, true);
         this.started = false;
      }

   }

   public void perform() {
      DebugLog.Action.trace("%s: perform", this.table.rawget("Type"));
      super.perform();
      LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("perform"), this.table);
      if (GameClient.bClient && !this.useCustomRemoteTimedActionSync) {
         ActionManager.remove(this.transactionId, false);
      }

   }

   public void complete() {
      DebugLog.Action.trace("%s: complete", this.table.rawget("Type"));
      super.complete();
      if (!GameClient.bClient) {
         LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("complete"), this.table);
      }

   }

   public void Failed(Mover var1) {
      this.table.rawset("path", (Object)null);
      LuaManager.caller.pcallvoid(LuaManager.thread, this.table.rawget("failedPathfind"), this.table);
   }

   public void Succeeded(Path var1, Mover var2) {
      this.table.rawset("path", var1);
      LuaManager.caller.pcallvoid(LuaManager.thread, this.table.rawget("succeededPathfind"), this.table);
   }

   public void Pathfind(IsoGameCharacter var1, int var2, int var3, int var4) {
   }

   public String getName() {
      return "timedActionPathfind";
   }

   public void setCurrentTime(float var1) {
      this.CurrentTime = PZMath.clamp(var1, 0.0F, (float)this.MaxTime);
   }

   public int getTime() {
      return this.MaxTime;
   }

   public void setCustomRemoteTimedActionSync(boolean var1) {
      this.useCustomRemoteTimedActionSync = var1;
   }

   public void setTime(int var1) {
      this.MaxTime = var1;
   }

   public void OnAnimEvent(AnimEvent var1) {
      Object var2 = this.table.rawget("animEvent");
      if (var2 != null) {
         LuaManager.caller.pcallvoid(LuaManager.thread, var2, this.table, var1.m_EventName, var1.m_ParameterValue);
      }

   }

   public void getDeltaModifiers(MoveDeltaModifiers var1) {
      Object var2 = this.table.rawget("getDeltaModifiers");
      if (var2 != null) {
         LuaManager.caller.pcallvoid(LuaManager.thread, var2, this.table, var1);
      }

   }

   public String getMetaType() {
      return this.table != null && this.table.getMetatable() != null ? this.table.getMetatable().getString("Type") : "";
   }

   public void replaceObjectInTable(Object var1, Object var2) {
      if (this.table != null) {
         synchronized(keys) {
            KahluaTableIterator var4 = this.table.iterator();

            while(var4.advance()) {
               if (var4.getValue() == var1) {
                  keys.add(var4.getKey());
               }
            }

            if (!keys.isEmpty()) {
               Iterator var5 = keys.iterator();

               while(var5.hasNext()) {
                  Object var6 = var5.next();
                  this.table.rawset(var6, var2);
               }
            }

            keys.clear();
         }
      }

   }
}
