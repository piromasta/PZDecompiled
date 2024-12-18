package zombie.entity.components.attributes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import zombie.debug.DebugLog;
import zombie.entity.util.enums.IOEnum;
import zombie.inventory.InventoryItem;
import zombie.util.StringUtils;

public class AttributeUtil {
   public static final String enum_prefix = "enum.";
   private static final ArrayDeque<ArrayList<InventoryItem>> itemListPool = new ArrayDeque();
   private static final ArrayDeque<ArrayList<Double>> doubleListPool = new ArrayDeque();

   public AttributeUtil() {
   }

   public static boolean isEnumString(String var0) {
      return StringUtils.startsWithIgnoreCase(var0, "enum.");
   }

   private static String getSanitizedEnumString(String var0) {
      Objects.requireNonNull(var0);
      if (var0.toLowerCase().startsWith("enum.")) {
         return var0.substring("enum.".length());
      } else {
         throw new IllegalArgumentException("Valid enum string should start with 'enum.'");
      }
   }

   public static <E extends Enum<E> & IOEnum> E enumValueFromScriptString(Class<E> var0, String var1) {
      try {
         return Enum.valueOf(var0, getSanitizedEnumString(var1));
      } catch (Exception var3) {
         var3.printStackTrace();
         return null;
      }
   }

   public static <E extends Enum<E> & IOEnum> E tryEnumValueFromScriptString(Class<E> var0, String var1) {
      try {
         return Enum.valueOf(var0, getSanitizedEnumString(var1));
      } catch (Exception var3) {
         return null;
      }
   }

   public static ArrayList<InventoryItem> allocItemList() {
      ArrayList var0 = (ArrayList)itemListPool.poll();
      if (var0 == null) {
         var0 = new ArrayList();
      }

      return var0;
   }

   public static void releaseItemList(ArrayList<InventoryItem> var0) {
      var0.clear();

      assert !itemListPool.contains(var0);

      itemListPool.offer(var0);
   }

   public static ArrayList<InventoryItem> getItemsFromList(String var0, ArrayList<InventoryItem> var1, ArrayList<InventoryItem> var2) {
      for(int var3 = 0; var3 < var1.size(); ++var3) {
         InventoryItem var4 = (InventoryItem)var1.get(var3);
         boolean var5 = var0.contains(".");
         if (var5 && var4.getFullType().equalsIgnoreCase(var0) || !var5 && var4.getType().equalsIgnoreCase(var0)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public static float getAttributeAverage(ArrayList<InventoryItem> var0, AttributeType var1) {
      float var2 = 0.0F;

      try {
         if (!var1.isNumeric()) {
            DebugLog.General.warn("Attribute '" + var1 + " is not numeric.");
            return 0.0F;
         } else {
            AttributeType.Numeric var3 = (AttributeType.Numeric)var1;
            float var4 = (float)var0.size();

            for(int var5 = 0; var5 < var0.size(); ++var5) {
               var2 += ((InventoryItem)var0.get(var5)).getAttributes().getFloatValue(var3);
            }

            return var2 / var4;
         }
      } catch (Exception var6) {
         var6.printStackTrace();
         return var2;
      }
   }

   public static float convertAttributeToUnit(InventoryItem var0, AttributeType var1) {
      float var2 = 0.0F;

      try {
         AttributeType.Numeric var3 = (AttributeType.Numeric)var1;
         if (!var3.hasBounds()) {
            throw new Exception("Attribute '" + var1 + " has no bounds, cannot convert.");
         }

         float var4 = var3.getMin().floatValue();
         float var5 = var3.getMax().floatValue();
         float var6 = var0.getAttributes().getFloatValue(var3);
         var2 = (var6 - var4) / (var5 - var4);
      } catch (Exception var7) {
         var7.printStackTrace();
      }

      return var2;
   }

   public static float convertAttribute(InventoryItem var0, AttributeType var1, AttributeType var2) {
      float var3 = 0.0F;

      try {
         float var4 = convertAttributeToUnit(var0, var1);
         AttributeType.Numeric var5 = (AttributeType.Numeric)var2;
         if (!var5.hasBounds()) {
            throw new Exception("Target attribute '" + var2 + " has no bounds, cannot convert.");
         }

         float var6 = var5.getMin().floatValue();
         float var7 = var5.getMax().floatValue();
         var3 = var6 + (var7 - var6) * var4;
      } catch (Exception var8) {
         var8.printStackTrace();
      }

      return var3;
   }

   public static float convertAttributeToRange(InventoryItem var0, AttributeType var1, float var2, float var3) {
      float var4 = 0.0F;

      try {
         float var5 = convertAttributeToUnit(var0, var1);
         var4 = var2 + (var3 - var2) * var5;
      } catch (Exception var6) {
         var6.printStackTrace();
      }

      return var4;
   }

   public static ArrayList<Double> allocDoubleList() {
      ArrayList var0 = (ArrayList)doubleListPool.poll();
      if (var0 == null) {
         var0 = new ArrayList();
      }

      return var0;
   }

   public static void releaseDoubleList(ArrayList<Double> var0) {
      var0.clear();

      assert !doubleListPool.contains(var0);

      doubleListPool.offer(var0);
   }
}
