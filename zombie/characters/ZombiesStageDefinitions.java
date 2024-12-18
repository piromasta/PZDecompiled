package zombie.characters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class ZombiesStageDefinitions {
   public static final ZombiesStageDefinitions instance = new ZombiesStageDefinitions();
   public boolean m_dirty = true;
   public final ArrayList<ZombiesStageDefinition> m_stageDefinition = new ArrayList();
   public static int daysEarly = 10;
   public static int daysMid = 30;
   public static int daysLate = 180;

   public ZombiesStageDefinitions() {
   }

   public void checkDirty() {
      if (this.m_dirty) {
         this.m_dirty = false;
         this.init();
      }

   }

   private void init() {
      ArrayList var1 = new ArrayList();
      var1.add("Bandit_Early");
      var1.add("Bandit_Mid");
      var1.add("Bandit_Late");
      ZombiesStageDefinition var2 = new ZombiesStageDefinition("Bandit", var1, true);
      this.m_stageDefinition.add(var2);
      ArrayList var3 = new ArrayList();
      var3.add("Survivalist");
      var3.add("Survivalist_Mid");
      var3.add("Survivalist_Late");
      var2 = new ZombiesStageDefinition("Survivalist", var3, false);
      this.m_stageDefinition.add(var2);
      var3 = new ArrayList();
      var3.add("Survivalist02");
      var3.add("Survivalist02_Mid");
      var3.add("Survivalist02_Late");
      var2 = new ZombiesStageDefinition("Survivalist02", var3, false);
      this.m_stageDefinition.add(var2);
      var3 = new ArrayList();
      var3.add("Survivalist03");
      var3.add("Survivalist03_Mid");
      var3.add("Survivalist03_Late");
      var2 = new ZombiesStageDefinition("Survivalist03", var3, false);
      this.m_stageDefinition.add(var2);
      var3 = new ArrayList();
      var3.add("Survivalist04");
      var3.add("Survivalist04_Mid");
      var3.add("Survivalist04_Late");
      var2 = new ZombiesStageDefinition("Survivalist04", var3, false);
      this.m_stageDefinition.add(var2);
      var3 = new ArrayList();
      var3.add("Survivalist05");
      var3.add("Survivalist05_Mid");
      var3.add("Survivalist05_Late");
      var2 = new ZombiesStageDefinition("Survivalist05", var3, false);
      this.m_stageDefinition.add(var2);
   }

   private static ArrayList<String> initStageList(String var0) {
      if (StringUtils.isNullOrWhitespace(var0)) {
         return null;
      } else {
         String[] var1 = var0.split(";");
         return new ArrayList(Arrays.asList(var1));
      }
   }

   public String getAdvancedOutfitName(String var1) {
      if (StringUtils.isNullOrEmpty(var1)) {
         return var1;
      } else {
         instance.checkDirty();

         for(int var2 = 0; var2 < instance.m_stageDefinition.size(); ++var2) {
            ZombiesStageDefinition var3 = (ZombiesStageDefinition)instance.m_stageDefinition.get(var2);
            if (Objects.equals(var3.outfit, var1)) {
               String var4 = this.getAdvancedOutfitName(var3);
               if (var4 != null) {
                  return var4;
               }

               return var1;
            }
         }

         return var1;
      }
   }

   public String getAdvancedOutfitName(ZombiesStageDefinition var1) {
      int var2 = var1.laterOutfits.size();
      if (var2 < 1) {
         return var1.outfit;
      } else {
         int var3 = (int)(GameTime.getInstance().getWorldAgeHours() / 24.0) + (SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30;
         int var4 = 0;
         if (var3 >= daysLate) {
            var4 = 3;
         } else if (var3 >= daysMid) {
            var4 = 2;
         } else if (var3 >= daysEarly) {
            var4 = 1;
         }

         var4 = Math.min(var4, var2);
         if (var4 < 1) {
            return null;
         } else {
            int var5 = var4;
            if (var1.mixed) {
               var5 = Rand.Next(var4) + 1;
            }

            if (var5 == 0) {
               return null;
            } else {
               String var6 = (String)var1.laterOutfits.get(var5 - 1);
               return var6;
            }
         }
      }
   }

   public static final class ZombiesStageDefinition {
      public String outfit;
      public ArrayList<String> laterOutfits;
      public boolean mixed;

      public ZombiesStageDefinition(String var1, ArrayList<String> var2, boolean var3) {
         DebugLog.Zombie.println("Adding Zombies Stage Definition: " + var1 + " - " + var2 + " - Mixed : " + var3);
         this.outfit = var1;
         this.laterOutfits = var2;
         this.mixed = var3;
      }
   }
}
