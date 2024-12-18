package zombie.characters.animals;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;

public class AnimalGenomeDefinitions {
   public String name;
   public float currentValue;
   public HashMap<String, Float> ratios;
   public float minValue = 0.2F;
   public float maxValue = 0.6F;
   public static HashMap<String, AnimalGenomeDefinitions> fullGenomeDef = null;
   public static ArrayList<String> geneticDisorder;
   public boolean forcedValues = false;

   public AnimalGenomeDefinitions() {
   }

   public static void loadGenomeDefinition() {
      fullGenomeDef = new HashMap();
      KahluaTableImpl var0 = (KahluaTableImpl)LuaManager.env.rawget("AnimalGenomeDefinitions");
      if (var0 != null) {
         KahluaTableImpl var1 = (KahluaTableImpl)var0.rawget("genes");
         KahluaTableIterator var2 = var1.iterator();

         String var4;
         while(var2.advance()) {
            AnimalGenomeDefinitions var3 = new AnimalGenomeDefinitions();
            var4 = var2.getKey().toString().toLowerCase();
            var3.name = var4;
            KahluaTableIterator var5 = ((KahluaTableImpl)var2.getValue()).iterator();

            while(var5.advance()) {
               String var6 = (String)var5.getKey();
               Object var7 = var5.getValue();
               String var8 = var7.toString().trim();
               if ("minValue".equalsIgnoreCase(var6)) {
                  var3.minValue = Float.parseFloat(var8);
               }

               if ("maxValue".equalsIgnoreCase(var6)) {
                  var3.maxValue = Float.parseFloat(var8);
               }

               if ("forcedValues".equalsIgnoreCase(var6)) {
                  var3.forcedValues = Boolean.parseBoolean(var8);
               }

               if ("ratio".equalsIgnoreCase(var6)) {
                  var3.loadRatio((KahluaTableImpl)var7);
               }
            }

            fullGenomeDef.put(var4, var3);
         }

         KahluaTableImpl var9 = (KahluaTableImpl)var0.rawget("geneticDisorder");
         geneticDisorder = new ArrayList();
         var2 = var9.iterator();

         while(var2.advance()) {
            var4 = var2.getKey().toString().toLowerCase();
            if (!geneticDisorder.contains(var4)) {
               geneticDisorder.add(var4);
            }
         }

      }
   }

   private void loadRatio(KahluaTableImpl var1) {
      this.ratios = new HashMap();
      KahluaTableIterator var2 = var1.iterator();

      while(var2.advance()) {
         String var3 = var2.getKey().toString().toLowerCase();
         String var4 = var2.getValue().toString().trim();
         this.ratios.put(var3, Float.parseFloat(var4));
      }

   }

   public static ArrayList<String> getGeneticDisorderList() {
      return geneticDisorder;
   }
}
