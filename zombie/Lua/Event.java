package zombie.Lua;

import java.util.ArrayList;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Platform;
import zombie.GameProfiler;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;

public final class Event {
   public static final int ADD = 0;
   public static final int NUM_FUNCTIONS = 1;
   private final Add add;
   private final Remove remove;
   public final ArrayList<LuaClosure> callbacks = new ArrayList();
   public String name;
   private int index = 0;

   public boolean trigger(KahluaTable var1, LuaCaller var2, Object[] var3) {
      if (this.callbacks.isEmpty()) {
         return false;
      } else {
         int var4;
         LuaClosure var5;
         GameProfiler.ProfileArea var6;
         if (DebugOptions.instance.Checks.SlowLuaEvents.getValue()) {
            for(var4 = 0; var4 < this.callbacks.size(); ++var4) {
               var5 = (LuaClosure)this.callbacks.get(var4);
               var6 = GameProfiler.getInstance().startIfEnabled("Lua - " + this.name);

               try {
                  long var7 = System.nanoTime();
                  var2.protectedCallVoid(LuaManager.thread, var5, var3);
                  double var9 = (double)(System.nanoTime() - var7) / 1000000.0;
                  if (var9 > 250.0) {
                     DebugLog.Lua.warn("SLOW Lua event callback %s %s %dms", var5.prototype.file, var5, (int)var9);
                  }
               } catch (Exception var21) {
                  ExceptionLogger.logException(var21);
               } finally {
                  GameProfiler.getInstance().end(var6);
               }

               if (!this.callbacks.contains(var5)) {
                  --var4;
               }
            }

            return true;
         } else {
            for(var4 = 0; var4 < this.callbacks.size(); ++var4) {
               var5 = (LuaClosure)this.callbacks.get(var4);
               var6 = GameProfiler.getInstance().startIfEnabled("Lua - " + this.name);

               try {
                  var2.protectedCallVoid(LuaManager.thread, var5, var3);
               } catch (Exception var23) {
                  ExceptionLogger.logException(var23);
               } finally {
                  GameProfiler.getInstance().end(var6);
               }

               if (!this.callbacks.contains(var5)) {
                  --var4;
               }
            }

            return true;
         }
      }
   }

   public Event(String var1, int var2) {
      this.index = var2;
      this.name = var1;
      this.add = new Add(this);
      this.remove = new Remove(this);
   }

   public void register(Platform var1, KahluaTable var2) {
      KahluaTable var3 = var1.newTable();
      var3.rawset("Add", this.add);
      var3.rawset("Remove", this.remove);
      var2.rawset(this.name, var3);
   }

   public static final class Add implements JavaFunction {
      Event e;

      public Add(Event var1) {
         this.e = var1;
      }

      public int call(LuaCallFrame var1, int var2) {
         if (LuaCompiler.rewriteEvents) {
            return 0;
         } else {
            Object var3 = var1.get(0);
            if (this.e.name.contains("CreateUI")) {
               boolean var4 = false;
            }

            if (var3 instanceof LuaClosure) {
               LuaClosure var5 = (LuaClosure)var3;
               this.e.callbacks.add(var5);
            }

            return 0;
         }
      }
   }

   public static final class Remove implements JavaFunction {
      Event e;

      public Remove(Event var1) {
         this.e = var1;
      }

      public int call(LuaCallFrame var1, int var2) {
         if (LuaCompiler.rewriteEvents) {
            return 0;
         } else {
            Object var3 = var1.get(0);
            if (var3 instanceof LuaClosure) {
               LuaClosure var4 = (LuaClosure)var3;
               this.e.callbacks.remove(var4);
            }

            return 0;
         }
      }
   }
}
