package zombie.entity.components.attributes;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Attribute {
   private static final HashMap<String, AttributeType> attributeTypeNameMap = new HashMap();
   private static final HashMap<Short, AttributeType> attributeTypeIdMap = new HashMap();
   private static final ArrayList<AttributeType> attributeTypes = new ArrayList();
   public static final AttributeType.Float TestQuality = (AttributeType.Float)registerType(new AttributeType.Float((short)103, "TestQuality", 0.0F));
   public static final AttributeType.Int TestUses;
   public static final AttributeType.Float TestCondition;
   public static final AttributeType.Bool TestBool;
   public static final AttributeType.String TestString = (AttributeType.String)registerType(new AttributeType.String((short)100, "TestString", "Test string for attribute."));
   public static final AttributeType.String TestString2 = (AttributeType.String)registerType(new AttributeType.String((short)102, "TestString2", ""));
   public static final AttributeType.Enum<TestEnum> TestItemType;
   public static final AttributeType.EnumSet<TestEnum> TestCategories;
   public static final AttributeType.EnumStringSet<TestEnum> TestTags;
   public static final AttributeType.Float Sharpness;
   public static final AttributeType.Int HeadCondition;
   public static final AttributeType.Int HeadConditionMax;
   public static final AttributeType.Int TimesHeadRepaired;
   public static final AttributeType.Int Quality;

   public Attribute() {
   }

   private static <E extends AttributeType> E registerType(E var0) {
      short var10002;
      if (attributeTypeNameMap.containsKey(var0.getName().toLowerCase())) {
         var10002 = var0.id();
         throw new RuntimeException("Attribute name registered twice id = '" + var10002 + ", attribute = '" + var0.getName() + "'");
      } else if (attributeTypeIdMap.containsKey(var0.id())) {
         var10002 = var0.id();
         throw new RuntimeException("Attribute id registered twice id = '" + var10002 + ", attribute = '" + var0.getName() + "'");
      } else {
         attributeTypeIdMap.put(var0.id(), var0);
         attributeTypeNameMap.put(var0.getName().toLowerCase(), var0);
         attributeTypes.add(var0);
         return var0;
      }
   }

   public static AttributeType TypeFromName(String var0) {
      return (AttributeType)attributeTypeNameMap.get(var0.toLowerCase());
   }

   public static AttributeType TypeFromId(short var0) {
      return (AttributeType)attributeTypeIdMap.get(var0);
   }

   public static ArrayList<AttributeType> GetAllTypes() {
      return attributeTypes;
   }

   public static void init() {
   }

   static {
      TestQuality.setBounds(0.0F, 100.0F);
      TestCondition = (AttributeType.Float)registerType(new AttributeType.Float((short)104, "TestCondition", 0.0F));
      TestCondition.setBounds(0.0F, 1.0F);
      TestBool = (AttributeType.Bool)registerType(new AttributeType.Bool((short)105, "TestBool", false));
      TestUses = (AttributeType.Int)registerType(new AttributeType.Int((short)106, "TestUses", 5));
      TestItemType = (AttributeType.Enum)registerType(new AttributeType.Enum((short)121, "TestItemType", TestEnum.TestValueA));
      TestCategories = (AttributeType.EnumSet)registerType(new AttributeType.EnumSet((short)123, "TestCategories", TestEnum.class));
      TestCategories.getInitialValue().add(TestEnum.TestValueC);
      TestTags = (AttributeType.EnumStringSet)registerType(new AttributeType.EnumStringSet((short)124, "TestTags", TestEnum.class));
      Sharpness = (AttributeType.Float)registerType(new AttributeType.Float((short)0, "Sharpness", 1.0F, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, ""));
      Sharpness.setBounds(0.0F, 1.0F);
      HeadCondition = (AttributeType.Int)registerType(new AttributeType.Int((short)1, "HeadCondition", 10, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, ""));
      HeadCondition.setBounds(0, 1000);
      HeadConditionMax = (AttributeType.Int)registerType(new AttributeType.Int((short)2, "HeadConditionMax", 10, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, ""));
      HeadConditionMax.setBounds(0, 1000);
      Quality = (AttributeType.Int)registerType(new AttributeType.Int((short)3, "Quality", 50, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, ""));
      Quality.setBounds(0, 100);
      TimesHeadRepaired = (AttributeType.Int)registerType(new AttributeType.Int((short)4, "TimesHeadRepaired", 0, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, ""));
      TimesHeadRepaired.setBounds(0, 1000);
   }

   public static final class UI {
      public UI() {
      }

      public static enum DisplayAsBar {
         Default,
         ForceIfBounds,
         Never;

         private DisplayAsBar() {
         }
      }

      public static enum Display {
         Visible,
         Hidden;

         private Display() {
         }
      }
   }
}
