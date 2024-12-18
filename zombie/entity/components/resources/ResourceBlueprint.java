package zombie.entity.components.resources;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.util.enums.EnumBitStore;
import zombie.util.StringUtils;

public class ResourceBlueprint {
   private static final ConcurrentLinkedDeque<ResourceBlueprint> pool = new ConcurrentLinkedDeque();
   public static final String serialElementSeparator = "@";
   public static final String serialSubSeparator = ":";
   private static final String str_null = "null";
   private static final int initialSerialLength = 64;
   private String id;
   private ResourceType type;
   private ResourceIO io;
   private float capacity;
   private ResourceChannel channel;
   private final EnumBitStore<ResourceFlag> resourceFlags;
   private String filter;
   private static final ThreadLocal<StringBuilder> threadLocalSb = ThreadLocal.withInitial(StringBuilder::new);

   private static ResourceBlueprint alloc_empty() {
      ResourceBlueprint var0 = (ResourceBlueprint)pool.poll();
      if (var0 == null) {
         var0 = new ResourceBlueprint();
      }

      return var0;
   }

   public static ResourceBlueprint alloc(String var0, ResourceType var1, ResourceIO var2, float var3, String var4, ResourceChannel var5, EnumBitStore<ResourceFlag> var6) {
      ResourceBlueprint var7 = alloc_empty();
      var7.id = (String)Objects.requireNonNull(var0);
      var7.type = (ResourceType)Objects.requireNonNull(var1);
      var7.io = (ResourceIO)Objects.requireNonNull(var2);
      var7.capacity = var3;
      var7.channel = (ResourceChannel)Objects.requireNonNull(var5);
      var7.resourceFlags.addAll(var6);
      var7.filter = var4;
      return var7;
   }

   public static void release(ResourceBlueprint var0) {
      var0.reset();

      assert !Core.bDebug || pool.contains(var0) : "Object already in pool.";

      pool.offer(var0);
   }

   private ResourceBlueprint() {
      this.io = ResourceIO.Any;
      this.capacity = 1.0F;
      this.channel = ResourceChannel.NO_CHANNEL;
      this.resourceFlags = EnumBitStore.noneOf(ResourceFlag.class);
   }

   public String getId() {
      return this.id;
   }

   public ResourceType getType() {
      return this.type;
   }

   public ResourceIO getIO() {
      return this.io;
   }

   public float getCapacity() {
      return this.capacity;
   }

   public ResourceChannel getChannel() {
      return this.channel;
   }

   public boolean hasFlag(ResourceFlag var1) {
      return this.resourceFlags.contains(var1);
   }

   public int getFlagBits() {
      return this.resourceFlags.getBits();
   }

   public String getFilter() {
      return this.filter;
   }

   private void reset() {
      this.id = null;
      this.type = ResourceType.Any;
      this.io = ResourceIO.Any;
      this.capacity = 1.0F;
      this.channel = ResourceChannel.NO_CHANNEL;
      this.resourceFlags.clear();
      this.filter = null;
   }

   private static String checkCharacters(String var0) {
      if (!Core.bDebug || !var0.contains("@") && !var0.contains(":")) {
         return var0;
      } else {
         throw new IllegalArgumentException("String contains illegal characters.");
      }
   }

   public static String Serialize(ResourceBlueprint var0) {
      return Serialize(var0.id, var0.type, var0.io, var0.capacity, var0.filter, var0.channel, var0.resourceFlags);
   }

   public static String Serialize(String var0, ResourceType var1, ResourceIO var2, float var3, String var4, ResourceChannel var5, EnumBitStore<ResourceFlag> var6) {
      if (StringUtils.isNullOrWhitespace(var0)) {
         throw new IllegalArgumentException("Id cannot be null or whitespace");
      } else if (var1 == ResourceType.Energy && StringUtils.isNullOrWhitespace(var4)) {
         throw new IllegalArgumentException("Energy requires filter set.");
      } else {
         StringBuilder var7 = (StringBuilder)threadLocalSb.get();
         var7.setLength(0);
         var7.append(checkCharacters(var0));
         var7.append("@");
         var7.append(var1.toString());
         var7.append("@");
         var7.append(var2.toString());
         var7.append("@");
         if (var1 == ResourceType.Item) {
            var7.append((int)var3);
         } else {
            var7.append(var3);
         }

         if (var5 != null && var5 != ResourceChannel.NO_CHANNEL || var6 != null && !var6.isEmpty() || !StringUtils.isNullOrWhitespace(var4)) {
            var7.append("@");
            if (!StringUtils.isNullOrWhitespace(var4)) {
               var7.append(checkCharacters(var4));
            } else {
               var7.append("null");
            }

            var7.append("@");
            if (var5 != null && var5 != ResourceChannel.NO_CHANNEL) {
               var7.append(var5);
            } else {
               var7.append("null");
            }

            var7.append("@");
            if (var6 != null && !var6.isEmpty()) {
               var7.append(var6.getBits());
            } else {
               var7.append("null");
            }
         }

         String var8 = var7.toString();
         if (Core.bDebug && var8.length() > 64) {
            DebugLog.log("Created serial surpassed initial serial length: " + var8);
         }

         return var8;
      }
   }

   public static ResourceBlueprint DeserializeFromScript(String var0) {
      ResourceBlueprint var1 = alloc_empty();
      return Deserialize(var1, var0, true);
   }

   public static ResourceBlueprint Deserialize(String var0) {
      ResourceBlueprint var1 = alloc_empty();
      return Deserialize(var1, var0);
   }

   public static ResourceBlueprint Deserialize(ResourceBlueprint var0, String var1) {
      return Deserialize(var0, var1, false);
   }

   public static ResourceBlueprint Deserialize(ResourceBlueprint var0, String var1, boolean var2) {
      String[] var3 = var1.split("@");
      if (var3.length != 4 && var3.length != 7) {
         throw new IllegalArgumentException("Serial string has invalid number of elements.");
      } else {
         var0.reset();
         var0.id = var3[0];
         var0.type = ResourceType.valueOf(var3[1]);
         var0.io = ResourceIO.valueOf(var3[2]);
         if (var0.type == ResourceType.Item) {
            var0.capacity = (float)Integer.parseInt(var3[3]);
         } else {
            var0.capacity = Float.parseFloat(var3[3]);
         }

         if (var3.length == 7) {
            String var4 = var3[4];
            if (!"null".equalsIgnoreCase(var4)) {
               var0.filter = var4;
            }

            String var5 = var3[5];
            if (!"null".equalsIgnoreCase(var5)) {
               var0.channel = ResourceChannel.valueOf(var5);
            }

            String var6 = var3[6];
            if (!"null".equalsIgnoreCase(var6)) {
               if (var2) {
                  String[] var7 = var6.split(":");

                  for(int var8 = 0; var8 < var7.length; ++var8) {
                     ResourceFlag var9 = ResourceFlag.valueOf(var7[var8]);
                     var0.resourceFlags.add(var9);
                  }
               } else {
                  int var10 = Integer.parseInt(var6);
                  var0.resourceFlags.setBits(var10);
               }
            }
         }

         return var0;
      }
   }
}
