package zombie.iso.worldgen.rules;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import zombie.ZomboidFileSystem;

public class Rules {
   private int version;
   private Map<String, Alias> aliases;
   private Map<String, Rule> rules;
   private Map<String, int[]> colors;
   private Map<String, Integer> colorsInt;

   public Rules() {
   }

   public static Rules load(String var0) {
      File var1 = ZomboidFileSystem.instance.getMediaFile(var0);
      if (!var1.exists()) {
         return null;
      } else {
         Rules var2 = new Rules();
         var2.aliases = new HashMap();
         var2.rules = new HashMap();
         var2.colors = new HashMap();
         var2.colorsInt = new HashMap();

         try {
            BufferedReader var3 = new BufferedReader(new FileReader(var1));

            while(true) {
               String var4 = var3.readLine();
               if (var4 == null) {
                  var3.close();
                  break;
               }

               var4 = var4.strip();
               if (!var4.isEmpty()) {
                  String[] var5 = var4.split("\\h+");
                  switch (var5[0]) {
                     case "version":
                        var2.version = Integer.parseInt(var5[2]);
                        break;
                     case "alias":
                        Alias var17 = Alias.load(var3, var5);
                        var2.aliases.put(var17.name(), var17);
                        break;
                     case "rule":
                        Rule var8 = Rule.load(var3, var5);
                        var2.rules.put(var8.label(), var8);
                  }
               }
            }
         } catch (IOException var12) {
            var12.printStackTrace();
            return null;
         }

         List var13 = var2.rules.values().stream().filter((var0x) -> {
            return var0x.condition()[0] == -1;
         }).toList();
         Iterator var14 = var13.iterator();

         while(var14.hasNext()) {
            Rule var15 = (Rule)var14.next();
            Iterator var16 = var15.tiles().iterator();

            while(var16.hasNext()) {
               String var18 = (String)var16.next();
               Object var19 = var2.aliases.containsKey(var18) ? ((Alias)var2.aliases.get(var18)).tiles() : Lists.newArrayList(new String[]{var18});
               Iterator var9 = ((List)var19).iterator();

               while(var9.hasNext()) {
                  String var10 = (String)var9.next();
                  int[] var11 = var15.color();
                  var2.colors.put(var10, var11);
                  var2.colorsInt.put(var10, var11[0] << 16 | var11[1] << 8 | var11[2]);
               }
            }
         }

         return var2;
      }
   }

   public int getVersion() {
      return this.version;
   }

   public Map<String, Alias> getAliases() {
      return this.aliases;
   }

   public Map<String, Rule> getRules() {
      return this.rules;
   }

   public Map<String, int[]> getColors() {
      return this.colors;
   }

   public Map<String, Integer> getColorsInt() {
      return this.colorsInt;
   }
}
