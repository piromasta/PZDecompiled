package zombie.scripting.itemConfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableBuilder {
   private static final String fieldStart = "\\$\\[";
   private static final String fieldEnd = "\\]";
   private static final String regex = "\\$\\[([^]]+)\\]";
   private static final Pattern pattern = Pattern.compile("\\$\\[([^]]+)\\]");
   private static Map<String, String> m_keys = new HashMap();

   public VariableBuilder() {
   }

   public static void clear() {
      m_keys.clear();
   }

   public static void addKey(String var0, String var1) {
      m_keys.put(var0, var1);
   }

   public static void setKeys(Map<String, String> var0) {
      m_keys.clear();
      Iterator var1 = var0.entrySet().iterator();

      while(var1.hasNext()) {
         Map.Entry var2 = (Map.Entry)var1.next();
         m_keys.put((String)var2.getKey(), (String)var2.getValue());
      }

   }

   public static String Build(String var0) throws ItemConfig.ItemConfigException {
      String var1 = format(var0);
      return var1;
   }

   private static String format(String var0) throws ItemConfig.ItemConfigException {
      Matcher var1 = pattern.matcher(var0);

      String var2;
      String var4;
      for(var2 = var0; var1.find(); var2 = var2.replaceFirst("\\$\\[([^]]+)\\]", var4)) {
         String var3 = var1.group(1).toLowerCase().trim();
         var4 = (String)m_keys.get(var3);
         if (var4 == null) {
            throw new ItemConfig.ItemConfigException("Variable not found: " + var3);
         }
      }

      return var2;
   }
}
