package zombie.scripting;

public class FileName {
   private static final int NOT_FOUND = -1;
   private static final char UNIX_NAME_SEPARATOR = '/';
   private static final char WINDOWS_NAME_SEPARATOR = '\\';

   public FileName() {
   }

   public static int indexOfLastSeparator(String var0) {
      if (var0 == null) {
         return -1;
      } else {
         int var1 = var0.lastIndexOf(47);
         int var2 = var0.lastIndexOf(92);
         return Math.max(var1, var2);
      }
   }

   public static String getName(String var0) {
      return var0 == null ? null : requireNonNullChars(var0).substring(indexOfLastSeparator(var0) + 1);
   }

   private static String requireNonNullChars(String var0) {
      if (var0.indexOf(0) >= 0) {
         throw new IllegalArgumentException("Null character present in file/path name. There are no known legitimate use cases for such data, but several injection attacks may use it");
      } else {
         return var0;
      }
   }
}
