package zombie.text.templating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.debug.DebugLog;

public class TemplateTextBuilder implements ITemplateBuilder {
   private static final String fieldStart = "\\$\\{";
   private static final String fieldEnd = "\\}";
   private static final String regex = "\\$\\{([^}]+)\\}";
   private static final Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
   private final Map<String, IReplace> m_keys = new HashMap();

   protected TemplateTextBuilder() {
   }

   public void Reset() {
      this.m_keys.clear();
   }

   public String Build(String var1) {
      return this.format(var1, (IReplaceProvider)null);
   }

   public String Build(String var1, IReplaceProvider var2) {
      return this.format(var1, var2);
   }

   public String Build(String var1, KahluaTableImpl var2) {
      ReplaceProviderLua var3 = ReplaceProviderLua.Alloc();
      var3.fromLuaTable(var2);
      String var4 = this.format(var1, var3);
      var3.release();
      return var4;
   }

   private String format(String var1, IReplaceProvider var2) {
      Matcher var3 = pattern.matcher(var1);

      String var4;
      String var6;
      for(var4 = var1; var3.find(); var4 = var4.replaceFirst("\\$\\{([^}]+)\\}", var6)) {
         String var5 = var3.group(1).toLowerCase().trim();
         var6 = null;
         if (var2 != null && var2.hasReplacer(var5)) {
            var6 = var2.getReplacer(var5).getString();
         } else {
            IReplace var7 = (IReplace)this.m_keys.get(var5);
            if (var7 != null) {
               var6 = var7.getString();
            }
         }

         if (var6 == null) {
            var6 = "missing_" + var5;
         }
      }

      return var4;
   }

   public void RegisterKey(String var1, KahluaTableImpl var2) {
      try {
         ArrayList var3 = new ArrayList();

         for(int var4 = 1; var4 < var2.len() + 1; ++var4) {
            var3.add((String)var2.rawget(var4));
         }

         if (var3.size() > 0) {
            this.localRegisterKey(var1, new ReplaceList(var3));
         } else {
            DebugLog.log("TemplateTextBuilder -> key '" + var1 + "' contains no entries, ignoring.");
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }

   }

   public void RegisterKey(String var1, IReplace var2) {
      this.localRegisterKey(var1, var2);
   }

   private void localRegisterKey(String var1, IReplace var2) {
      if (this.m_keys.containsKey(var1.toLowerCase().trim())) {
         DebugLog.log("TemplateTextBuilder -> Warning: key '" + var1 + "' replaces an existing key.");
      }

      this.m_keys.put(var1.toLowerCase().trim(), var2);
   }

   public void CopyFrom(Object var1) {
      if (!(var1 instanceof TemplateTextBuilder)) {
         DebugLog.log("TemplateTextBuilder -> Warning: CopyFrom other not instance of TemplateTextBuilder.");
      } else {
         Iterator var2 = ((TemplateTextBuilder)var1).m_keys.entrySet().iterator();

         while(var2.hasNext()) {
            Map.Entry var3 = (Map.Entry)var2.next();
            this.m_keys.put((String)var3.getKey(), (IReplace)var3.getValue());
         }

      }
   }
}
