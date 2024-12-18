package zombie.iso.worldgen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public class WGUtils {
   public static WGUtils instance = new WGUtils();
   private static int OFFSET = 2;
   private ArrayList<String> files = new ArrayList();

   private WGUtils() {
   }

   public String generateSeed() {
      Random var1 = new Random();
      String var2 = "";

      for(int var3 = 0; var3 < 16; ++var3) {
         int var4 = var1.nextInt(52);
         if (var4 < 26) {
            var2 = var2 + (char)(var4 + 65);
         } else {
            var2 = var2 + (char)(var4 + 97 - 26);
         }
      }

      return var2;
   }

   public void getFiles(String var1) {
      this.files = new ArrayList();
      File var2 = ZomboidFileSystem.instance.getMediaFile(var1);
      File var3 = ZomboidFileSystem.instance.base.canonicalFile;

      try {
         Stream var4 = Files.walk(Paths.get(var2.getPath()));

         try {
            var4.filter((var0) -> {
               return Files.isRegularFile(var0, new LinkOption[0]);
            }).filter((var0) -> {
               return var0.toString().endsWith("lua");
            }).forEach((var2x) -> {
               this.files.add(var3.toPath().relativize(var2x).toString().replaceAll("\\\\", "/"));
            });
         } catch (Throwable var8) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (IOException var9) {
         DebugLog.General.printException(var9, "", LogSeverity.Error);
      }

   }

   public int getFilesNum() {
      return this.files.size();
   }

   public String getFile(int var1) {
      return (String)this.files.get(var1);
   }

   public String displayTable(String var1) {
      StringBuilder var2 = new StringBuilder();
      Object var3 = LuaManager.env.rawget(var1);
      this.displayElement(var1, var3, var2, 0);
      return var2.toString();
   }

   public String displayTable(KahluaTable var1) {
      StringBuilder var2 = new StringBuilder();
      this.displayElement("", var1, var2, 0);
      return var2.toString();
   }

   private void displayElement(String var1, Object var2, StringBuilder var3, int var4) {
      String var5 = KahluaUtil.type(var2);
      String var6 = " ".repeat(Math.max(0, var4));
      if (var5.equals("nil") || var5.equals("string") || var5.equals("number") || var5.equals("boolean")) {
         var3.append(String.format("%s%s = %s\n", var6, var1, var2));
      }

      if (var5.equals("function") || var5.equals("coroutine") || var5.equals("userdata")) {
         var3.append(String.format("%s%s = %s\n", var6, var1, var2));
      }

      if (var5.equals("table")) {
         var3.append(String.format("%s%s = {\n", var6, var1));
         KahluaTable var7 = (KahluaTable)var2;
         KahluaTableIterator var8 = var7.iterator();

         while(var8.advance()) {
            String var9 = var8.getKey().toString();
            Object var10 = var8.getValue();
            this.displayElement(var9, var10, var3, var4 + OFFSET);
         }

         var3.append(String.format("%s}\n", var6));
      }

   }

   public boolean canPlace(List<String> var1, String var2) {
      List var3 = var1.stream().filter((var0) -> {
         return !var0.startsWith("!");
      }).toList();
      Iterator var4 = var3.iterator();
      if (var4.hasNext()) {
         String var5 = (String)var4.next();
         var5 = "^" + var5;
         var5 = var5.replace(".", "\\.");
         var5 = var5.replace("*", ".*");
         var5 = var5.replace("?", ".?");
         if (!var2.matches(var5)) {
            return false;
         }
      }

      List var7 = var1.stream().filter((var0) -> {
         return var0.startsWith("!");
      }).toList();
      Iterator var8 = var7.iterator();

      String var6;
      do {
         if (!var8.hasNext()) {
            return true;
         }

         var6 = (String)var8.next();
         var6 = "^" + var6.substring(1);
         var6 = var6.replace(".", "\\.");
         var6 = var6.replace("*", ".*");
         var6 = var6.replace("?", ".?");
      } while(!var2.matches(var6));

      return false;
   }

   public String methodName(StackTraceElement var1) {
      return String.format("%s.%s:%s", var1.getClassName(), var1.getMethodName(), var1.getLineNumber());
   }

   public String methodsCall(String var1, int var2, String... var3) {
      StackTraceElement[] var4 = Thread.currentThread().getStackTrace();
      StringBuilder var5 = new StringBuilder();
      var5.append(var1);
      int var6;
      if (var3.length != 0) {
         var5.append(" [");

         for(var6 = 0; var6 < var3.length; ++var6) {
            var5.append(var3[var6]);
            var5.append(", ");
         }

         var5.append("]");
      }

      for(var6 = 0; var6 <= var2; ++var6) {
         String var10001 = this.methodName(var4[var6 + 2]);
         var5.append(" <- " + var10001);
      }

      return var5.toString();
   }
}
