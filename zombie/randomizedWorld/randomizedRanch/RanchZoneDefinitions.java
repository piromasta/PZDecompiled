package zombie.randomizedWorld.randomizedRanch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.debug.DebugLog;

public class RanchZoneDefinitions {
   public int chance = 0;
   public String femaleType;
   public String maleType;
   public int minFemaleNb = 0;
   public int maxFemaleNb = 0;
   public int minMaleNb = 0;
   public int maxMaleNb = 0;
   public String forcedBreed;
   public int chanceForBaby = 0;
   public static int totalChance = 0;
   public String globalName;
   public ArrayList<RanchZoneDefinitions> possibleDef = new ArrayList();
   public static HashMap<String, ArrayList<RanchZoneDefinitions>> defs;
   public int maleChance = 0;

   public RanchZoneDefinitions() {
   }

   public static HashMap<String, ArrayList<RanchZoneDefinitions>> getDefs() {
      if (defs == null) {
         loadDefinitions();
      }

      return defs;
   }

   public static void loadDefinitions() {
      defs = new HashMap();
      KahluaTableImpl var0 = (KahluaTableImpl)LuaManager.env.rawget("RanchZoneDefinitions");
      if (var0 != null) {
         KahluaTableImpl var1 = (KahluaTableImpl)var0.rawget("type");
         KahluaTableIterator var2 = var1.iterator();

         while(var2.advance()) {
            RanchZoneDefinitions var3 = new RanchZoneDefinitions();
            KahluaTableIterator var4 = ((KahluaTableImpl)var2.getValue()).iterator();
            String var5 = null;

            while(var4.advance()) {
               String var6 = var4.getKey().toString();
               Object var7 = var4.getValue();
               String var8 = var7.toString().trim();
               if ("type".equalsIgnoreCase(var6)) {
                  var5 = var8;
               }

               if ("chance".equalsIgnoreCase(var6)) {
                  var3.chance = Float.valueOf(var8).intValue();
                  totalChance += var3.chance;
               }

               if ("minFemaleNb".equalsIgnoreCase(var6)) {
                  var3.minFemaleNb = Float.valueOf(var8).intValue();
               }

               if ("maleChance".equalsIgnoreCase(var6)) {
                  var3.maleChance = Float.valueOf(var8).intValue();
               }

               if ("globalName".equalsIgnoreCase(var6)) {
                  var3.globalName = var8;
               }

               if ("maxFemaleNb".equalsIgnoreCase(var6)) {
                  var3.maxFemaleNb = Float.valueOf(var8).intValue();
               }

               if ("minMaleNb".equalsIgnoreCase(var6)) {
                  var3.minMaleNb = Float.valueOf(var8).intValue();
               }

               if ("maxMaleNb".equalsIgnoreCase(var6)) {
                  var3.maxMaleNb = Float.valueOf(var8).intValue();
               }

               if ("chanceForBaby".equalsIgnoreCase(var6)) {
                  var3.chanceForBaby = Float.valueOf(var8).intValue();
               }

               if ("forcedBreed".equalsIgnoreCase(var6)) {
                  var3.forcedBreed = var8;
               }

               if ("femaleType".equalsIgnoreCase(var6)) {
                  var3.femaleType = var8;
               }

               if ("maleType".equalsIgnoreCase(var6)) {
                  var3.maleType = var8;
               }

               if ("possibleDef".equalsIgnoreCase(var6)) {
                  var3.possibleDef = var3.loadPossibleDef(var5, (KahluaTableImpl)var7);
               }
            }

            ArrayList var9 = (ArrayList)defs.get(var5);
            if (var9 == null) {
               var9 = new ArrayList();
            }

            var9.add(var3);
            defs.put(var5, var9);
         }

      }
   }

   private ArrayList<RanchZoneDefinitions> loadPossibleDef(String var1, KahluaTableImpl var2) {
      ArrayList var3 = new ArrayList();
      KahluaTableIterator var4 = var2.iterator();

      while(var4.advance()) {
         String var5 = var4.getValue().toString().trim();
         if (defs.get(var5) == null) {
            DebugLog.Animal.debugln("Couldn't find ranch definition " + var5 + " for definition " + var1);
         } else {
            var3.addAll((Collection)defs.get(var5));
         }
      }

      return var3;
   }
}
