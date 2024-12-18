package zombie.debug;

import java.io.PrintStream;

public enum DebugType {
   Packet,
   NetworkFileDebug,
   Network,
   General,
   DetailedInfo,
   Lua,
   Mod,
   Sound,
   Zombie,
   Combat,
   Objects,
   Fireplace,
   Radio,
   MapLoading,
   Clothing,
   Animation,
   AnimationDetailed,
   Asset,
   Script,
   Shader,
   Sprite,
   Input,
   Recipe,
   ActionSystem,
   IsoRegion,
   UnitTests,
   FileIO,
   Multiplayer,
   Ownership,
   Death,
   Damage,
   Discord,
   Statistic,
   Vehicle,
   Voice,
   Checksum,
   Animal,
   ItemPicker,
   CraftLogic,
   Action,
   Entity,
   Lightning,
   Grapple,
   ExitDebug,
   BodyDamage,
   Xml,
   Physics,
   Ballistics,
   PZBullet,
   ModelManager,
   LoadAnimation,
   Zone,
   WorldGen,
   Foraging,
   Saving,
   Fluid,
   Energy,
   Translation,
   Moveable,
   Basement;

   public static final DebugType Default = General;

   private DebugType() {
   }

   public boolean isEnabled() {
      return DebugLog.isEnabled(this);
   }

   public DebugLogStream getLogStream() {
      return DebugLog.getOrCreateDebugLogStream(this);
   }

   public void print(boolean var1) {
      this.getLogStream().print(var1);
   }

   public void print(char var1) {
      this.getLogStream().print(var1);
   }

   public void print(int var1) {
      this.getLogStream().print(var1);
   }

   public void print(long var1) {
      this.getLogStream().print(var1);
   }

   public void print(float var1) {
      this.getLogStream().print(var1);
   }

   public void print(double var1) {
      this.getLogStream().print(var1);
   }

   public void print(String var1) {
      this.getLogStream().print(var1);
   }

   public void print(Object var1) {
      this.getLogStream().print(var1);
   }

   public PrintStream printf(String var1, Object... var2) {
      return this.getLogStream().printf(var1, var2);
   }

   public void println() {
      this.getLogStream().println();
   }

   public void println(boolean var1) {
      this.getLogStream().println(var1);
   }

   public void println(char var1) {
      this.getLogStream().println(var1);
   }

   public void println(int var1) {
      this.getLogStream().println(var1);
   }

   public void println(long var1) {
      this.getLogStream().println(var1);
   }

   public void println(float var1) {
      this.getLogStream().println(var1);
   }

   public void println(double var1) {
      this.getLogStream().println(var1);
   }

   public void println(char[] var1) {
      this.getLogStream().println(var1);
   }

   public void println(String var1) {
      this.getLogStream().println(var1);
   }

   public void println(Object var1) {
      this.getLogStream().println(var1);
   }

   public void println(String var1, Object... var2) {
      this.getLogStream().println(var1, var2);
   }

   public void trace(Object var1) {
      this.getLogStream().trace(1, var1);
   }

   public void trace(String var1, Object... var2) {
      this.getLogStream().trace(1, var1, var2);
   }

   public void debugln(Object var1) {
      this.getLogStream().debugln(1, var1);
   }

   public void debugln(String var1, Object... var2) {
      this.getLogStream().debugln(1, var1, var2);
   }

   public void debugOnceln(Object var1) {
      this.getLogStream().debugOnceln(1, var1);
   }

   public void debugOnceln(String var1, Object... var2) {
      this.getLogStream().debugOnceln(1, var1, var2);
   }

   public void noise(Object var1) {
      this.getLogStream().noise(1, var1);
   }

   public void noise(String var1, Object... var2) {
      this.getLogStream().noise(1, var1, var2);
   }

   public void warn(Object var1) {
      this.getLogStream().warn(1, var1);
   }

   public void warn(String var1, Object... var2) {
      this.getLogStream().warn(1, var1, var2);
   }

   public void error(Object var1) {
      this.getLogStream().error(1, var1);
   }

   public void error(String var1, Object... var2) {
      this.getLogStream().error(1, var1, var2);
   }

   public void write(LogSeverity var1, String var2) {
      this.routedWrite(1, var1, var2);
   }

   public void routedWrite(int var1, LogSeverity var2, String var3) {
      switch (var2) {
         case Trace:
            this.getLogStream().trace(var1 + 1, var3);
            break;
         case Noise:
            this.getLogStream().noise(var1 + 1, var3);
            break;
         case Debug:
            this.getLogStream().debugln(var1 + 1, var3);
            break;
         case General:
            this.getLogStream().println(var3);
            break;
         case Warning:
            this.getLogStream().warn(var1 + 1, var3);
            break;
         case Error:
            this.getLogStream().error(var1 + 1, var3);
         case Off:
      }

   }

   public StackTraceContainer getStackTrace(LogSeverity var1, String var2, int var3) {
      return this.getStackTrace(var1, var2, 3, var3);
   }

   public StackTraceContainer getStackTrace(LogSeverity var1, String var2, int var3, int var4) {
      if (!this.isEnabled()) {
         return null;
      } else if (!this.getLogStream().isLogEnabled(var1)) {
         return null;
      } else {
         StackTraceElement[] var5 = Thread.currentThread().getStackTrace();
         return new StackTraceContainer(var5, var2, var3, var4);
      }
   }

   public void printException(Exception var1, String var2, LogSeverity var3) {
      this.getLogStream().printException(var1, var2, DebugLogStream.generateCallerPrefix(), var3);
   }
}
