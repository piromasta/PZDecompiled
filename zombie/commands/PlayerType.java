package zombie.commands;

public final class PlayerType {
   public static final byte deprecated = 0;
   public static final byte fromServerOnly = 0;
   public static final byte player = 1;
   public static final byte observer = 2;
   public static final byte gm = 4;
   public static final byte overseer = 8;
   public static final byte moderator = 16;
   public static final byte admin = 32;
   public static final byte all = 63;
   public static final byte allExceptPlayer = 62;

   private PlayerType() {
   }

   public static String toString(byte var0) {
      switch (var0) {
         case 0:
            return "from-server-only";
         case 1:
            return "";
         case 2:
            return "observer";
         case 4:
            return "gm";
         case 8:
            return "overseer";
         case 16:
            return "moderator";
         case 32:
            return "admin";
         default:
            return "";
      }
   }

   public static byte fromString(String var0) {
      var0 = var0.trim().toLowerCase();
      if (!"".equals(var0) && !"player".equals(var0) && !"none".equals(var0)) {
         if ("observer".equals(var0)) {
            return 2;
         } else if ("gm".equals(var0)) {
            return 4;
         } else if ("overseer".equals(var0)) {
            return 8;
         } else if ("moderator".equals(var0)) {
            return 16;
         } else {
            return (byte)("admin".equals(var0) ? 32 : 0);
         }
      } else {
         return 1;
      }
   }

   public static boolean isPrivileged(byte var0) {
      return (var0 & 62) != 0;
   }
}
