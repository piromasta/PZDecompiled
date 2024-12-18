package zombie.iso;

import com.google.common.base.Utf8;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.util.StringUtils;

public abstract class InstanceTracker {
   public static final String DEFAULT = "";
   public static final String ITEMS = "Item Spawns";
   public static final String CONTAINERS = "Container Rolls";
   public static final String STATS = "Stats";
   private static final HashMap<String, TObjectIntHashMap<String>> InstanceGroups = new HashMap();

   public InstanceTracker() {
   }

   public static int get(String var0, String var1) {
      return InstanceGroups.containsKey(var0) ? ((TObjectIntHashMap)InstanceGroups.get(var0)).get(var1) : 0;
   }

   public static int get(String var0) {
      return get("", var0);
   }

   public static void set(String var0, String var1, int var2) {
      if (var0 != null && var1 != null && !var1.isBlank()) {
         InstanceGroups.putIfAbsent(var0, new TObjectIntHashMap());
         ((TObjectIntHashMap)InstanceGroups.get(var0)).put(var1, var2);
      }
   }

   public static void set(String var0, int var1) {
      set("", var0, var1);
   }

   public static void adj(String var0, String var1, int var2) {
      InstanceGroups.putIfAbsent(var0, new TObjectIntHashMap());
      ((TObjectIntHashMap)InstanceGroups.get(var0)).put(var1, PZMath.clamp(((TObjectIntHashMap)InstanceGroups.get(var0)).get(var1) + var2, -2147483648, 2147483647));
   }

   public static void adj(String var0, int var1) {
      adj("", var0, var1);
   }

   public static void inc(String var0, String var1) {
      adj(var0, var1, 1);
   }

   public static void inc(String var0) {
      adj("", var0, 1);
   }

   public static void dec(String var0, String var1) {
      adj(var0, var1, -1);
   }

   public static void dec(String var0) {
      adj("", var0, -1);
   }

   public static List<String> sort(String var0, Sort var1) {
      List var2;
      switch (var1) {
         case NONE:
            var2 = ((TObjectIntHashMap)InstanceGroups.get(var0)).keySet().stream().toList();
            break;
         case KEY:
            var2 = ((TObjectIntHashMap)InstanceGroups.get(var0)).keySet().stream().sorted().toList();
            break;
         case COUNT:
            Stream var10000 = ((TObjectIntHashMap)InstanceGroups.get(var0)).keySet().stream();
            TObjectIntHashMap var10001 = (TObjectIntHashMap)InstanceGroups.get(var0);
            Objects.requireNonNull(var10001);
            var2 = var10000.sorted(Comparator.comparingInt(var10001::get).reversed()).toList();
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var2;
   }

   public static String exportGroup(String var0, Format var1, Sort var2) {
      if (!InstanceGroups.containsKey(var0)) {
         return "";
      } else {
         StringBuilder var3 = new StringBuilder();
         String var4 = System.lineSeparator();
         switch (var1) {
            case TEXT:
               var3.append("[%s] ; %d entries%s".formatted(var0, ((TObjectIntHashMap)InstanceGroups.get(var0)).size(), var4));
               sort(var0, var2).forEach((var3x) -> {
                  var3.append("%s = %d%s".formatted(var3x, get(var0, var3x), var4));
               });
               break;
            case CSV:
               sort(var0, var2).forEach((var3x) -> {
                  var3.append("%s,%s,%d%s".formatted(var0, var3x, get(var0, var3x), var4));
               });
         }

         var3.append(var4);
         return var3.toString();
      }
   }

   public static void exportFile(List<String> var0, String var1, Format var2, Sort var3) {
      if (!var1.isBlank() && !StringUtils.containsDoubleDot(var1) && !(new File(var1)).isAbsolute()) {
         String var10000 = ZomboidFileSystem.instance.getCacheDir();
         String var4 = var10000 + File.separator + var1;
         StringBuilder var5 = new StringBuilder();
         if (var0 == null || var0.isEmpty()) {
            var0 = InstanceGroups.keySet().stream().toList();
         }

         if (var2 == InstanceTracker.Format.CSV) {
            var5.append("group,key,count").append(System.lineSeparator());
         }

         var0.forEach((var3x) -> {
            var5.append(exportGroup(var3x, var2, var3));
         });
         File var6 = new File(var4);

         try {
            FileWriter var7 = new FileWriter(var6);

            try {
               BufferedWriter var8 = new BufferedWriter(var7);

               try {
                  var8.write(var5.toString());
               } catch (Throwable var13) {
                  try {
                     var8.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }

                  throw var13;
               }

               var8.close();
            } catch (Throwable var14) {
               try {
                  var7.close();
               } catch (Throwable var11) {
                  var14.addSuppressed(var11);
               }

               throw var14;
            }

            var7.close();
         } catch (Exception var15) {
            ExceptionLogger.logException(var15);
         }

      }
   }

   public static void save() {
      if (!GameClient.bClient && !Core.getInstance().isNoSave()) {
         try {
            int[] var0 = new int[]{8};
            InstanceGroups.forEach((var1x, var2x) -> {
               var0[0] += 6 + Utf8.encodedLength(var1x);
               var2x.forEachKey((var1) -> {
                  var0[0] += 6 + Utf8.encodedLength(var1);
                  return true;
               });
            });
            ByteBuffer var1 = ByteBuffer.allocate(var0[0]);

            try {
               var1.putInt(219);
               var1.putInt(InstanceGroups.size());
               InstanceGroups.forEach((var1x, var2x) -> {
                  GameWindow.WriteString(var1, var1x);
                  var1.putInt(var2x.size());
                  var2x.forEachEntry((var1xx, var2) -> {
                     GameWindow.WriteString(var1, var1xx);
                     var1.putInt(var2);
                     return true;
                  });
               });
            } catch (BufferOverflowException var4) {
               DebugLog.General.debugln("InstanceTracker Overflow");
               ExceptionLogger.logException(var4);
               return;
            }

            var1.flip();
            File var2 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("iTrack.bin"));
            FileOutputStream var3 = new FileOutputStream(var2);
            var3.getChannel().truncate(0L);
            var3.write(var1.array(), 0, var1.limit());
            var3.flush();
            var3.close();
         } catch (Exception var5) {
            ExceptionLogger.logException(var5);
         }

         exportFile(List.of("Item Spawns", "Container Rolls"), "ItemTracker.log", InstanceTracker.Format.TEXT, InstanceTracker.Sort.KEY);
      }
   }

   public static void load() {
      InstanceGroups.clear();
      if (!Core.getInstance().isNoSave()) {
         String var0 = ZomboidFileSystem.instance.getFileNameInCurrentSave("iTrack.bin");
         File var1 = new File(var0);
         if (var1.exists()) {
            try {
               FileInputStream var2 = new FileInputStream(var1);

               try {
                  ByteBuffer var3 = ByteBuffer.allocate((int)var1.length());
                  var3.clear();
                  int var4 = var2.read(var3.array());
                  var3.limit(var4);
                  int var5 = var3.getInt();
                  int var6 = var3.getInt();

                  for(int var7 = 0; var7 < var6; ++var7) {
                     String var8 = GameWindow.ReadString(var3);
                     int var9 = var3.getInt();
                     InstanceGroups.put(var8, new TObjectIntHashMap());

                     for(int var10 = 0; var10 < var9; ++var10) {
                        String var11 = GameWindow.ReadString(var3);
                        int var12 = var3.getInt();
                        set(var8, var11, var12);
                     }
                  }
               } catch (Throwable var14) {
                  try {
                     var2.close();
                  } catch (Throwable var13) {
                     var14.addSuppressed(var13);
                  }

                  throw var14;
               }

               var2.close();
            } catch (Exception var15) {
               ExceptionLogger.logException(var15);
            }

         }
      }
   }

   public static enum Sort {
      NONE,
      KEY,
      COUNT;

      private Sort() {
      }
   }

   public static enum Format {
      TEXT,
      CSV;

      private Format() {
      }
   }
}
