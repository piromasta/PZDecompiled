package zombie.text.templating;

import java.util.Random;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.Lua.LuaEventManager;
import zombie.characters.SurvivorFactory;

public class TemplateText {
   private static final ITemplateBuilder builder = new TemplateTextBuilder();
   private static final Random m_random = new Random(4397238L);

   public TemplateText() {
   }

   public static ITemplateBuilder CreateBlanc() {
      return new TemplateTextBuilder();
   }

   public static ITemplateBuilder CreateCopy() {
      TemplateTextBuilder var0 = new TemplateTextBuilder();
      var0.CopyFrom(builder);
      return var0;
   }

   public static String Build(String var0) {
      return builder.Build(var0);
   }

   public static String Build(String var0, IReplaceProvider var1) {
      return builder.Build(var0, var1);
   }

   public static String Build(String var0, KahluaTableImpl var1) {
      try {
         return builder.Build(var0, var1);
      } catch (Exception var3) {
         var3.printStackTrace();
         return var0;
      }
   }

   public static void RegisterKey(String var0, KahluaTableImpl var1) {
      builder.RegisterKey(var0, var1);
   }

   public static void RegisterKey(String var0, IReplace var1) {
      builder.RegisterKey(var0, var1);
   }

   public static void Initialize() {
      builder.RegisterKey("lastname", new IReplace() {
         public String getString() {
            return SurvivorFactory.getRandomSurname();
         }
      });
      builder.RegisterKey("firstname", new IReplace() {
         public String getString() {
            return TemplateText.RandNext(100) > 50 ? SurvivorFactory.getRandomForename(true) : SurvivorFactory.getRandomForename(false);
         }
      });
      builder.RegisterKey("maleName", new IReplace() {
         public String getString() {
            return SurvivorFactory.getRandomForename(false);
         }
      });
      builder.RegisterKey("femaleName", new IReplace() {
         public String getString() {
            return SurvivorFactory.getRandomForename(true);
         }
      });
      LuaEventManager.triggerEvent("OnTemplateTextInit");
   }

   public static void Reset() {
      builder.Reset();
   }

   public static float RandNext(float var0, float var1) {
      if (var0 == var1) {
         return var0;
      } else {
         if (var0 > var1) {
            var0 = var1;
            var1 = var1;
         }

         return var0 + m_random.nextFloat() * (var1 - var0);
      }
   }

   public static float RandNext(float var0) {
      return m_random.nextFloat() * var0;
   }

   public static int RandNext(int var0, int var1) {
      if (var0 == var1) {
         return var0;
      } else {
         if (var0 > var1) {
            var0 = var1;
            var1 = var1;
         }

         return var0 + m_random.nextInt(var1 - var0);
      }
   }

   public static int RandNext(int var0) {
      return m_random.nextInt(var0);
   }
}
