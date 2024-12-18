package zombie.popman;

public final class DebugCommands {
   public static final byte PKT_SPAWN_TIME_TO_ZERO = 3;
   public static final byte PKT_CLEAR_ZOMBIES = 4;
   public static final byte PKT_SPAWN_NOW = 5;

   public DebugCommands() {
   }

   public static native void n_debugCommand(int var0, int var1, int var2);
}
