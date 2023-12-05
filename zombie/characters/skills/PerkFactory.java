package zombie.characters.skills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.core.Translator;
import zombie.core.math.PZMath;

public final class PerkFactory {
   public static final ArrayList<Perk> PerkList = new ArrayList();
   private static final HashMap<String, Perk> PerkById = new HashMap();
   private static final HashMap<String, Perk> PerkByName = new HashMap();
   private static final Perk[] PerkByIndex = new Perk[256];
   private static int NextPerkID = 0;
   static float PerkXPReqMultiplier = 1.5F;

   public PerkFactory() {
   }

   public static String getPerkName(Perk var0) {
      return var0.getName();
   }

   public static Perk getPerkFromName(String var0) {
      return (Perk)PerkByName.get(var0);
   }

   public static Perk getPerk(Perk var0) {
      return var0;
   }

   public static Perk AddPerk(Perk var0, String var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11) {
      return AddPerk(var0, var1, PerkFactory.Perks.None, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, false);
   }

   public static Perk AddPerk(Perk var0, String var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, boolean var12) {
      return AddPerk(var0, var1, PerkFactory.Perks.None, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12);
   }

   public static Perk AddPerk(Perk var0, String var1, Perk var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12) {
      return AddPerk(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, false);
   }

   public static Perk AddPerk(Perk var0, String var1, Perk var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, boolean var13) {
      var0.translation = var1;
      var0.name = Translator.getText("IGUI_perks_" + var1);
      var0.parent = var2;
      var0.passiv = var13;
      var0.xp1 = (int)((float)var3 * PerkXPReqMultiplier);
      var0.xp2 = (int)((float)var4 * PerkXPReqMultiplier);
      var0.xp3 = (int)((float)var5 * PerkXPReqMultiplier);
      var0.xp4 = (int)((float)var6 * PerkXPReqMultiplier);
      var0.xp5 = (int)((float)var7 * PerkXPReqMultiplier);
      var0.xp6 = (int)((float)var8 * PerkXPReqMultiplier);
      var0.xp7 = (int)((float)var9 * PerkXPReqMultiplier);
      var0.xp8 = (int)((float)var10 * PerkXPReqMultiplier);
      var0.xp9 = (int)((float)var11 * PerkXPReqMultiplier);
      var0.xp10 = (int)((float)var12 * PerkXPReqMultiplier);
      PerkByName.put(var0.getName(), var0);
      PerkList.add(var0);
      return var0;
   }

   public static void init() {
      PerkFactory.Perks.None.parent = PerkFactory.Perks.None;
      PerkFactory.Perks.MAX.parent = PerkFactory.Perks.None;
      AddPerk(PerkFactory.Perks.Combat, "Combat", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Axe, "Axe", PerkFactory.Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Blunt, "Blunt", PerkFactory.Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.SmallBlunt, "SmallBlunt", PerkFactory.Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.LongBlade, "LongBlade", PerkFactory.Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.SmallBlade, "SmallBlade", PerkFactory.Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Spear, "Spear", PerkFactory.Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Maintenance, "Maintenance", PerkFactory.Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Firearm, "Firearm", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Aiming, "Aiming", PerkFactory.Perks.Firearm, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Reloading, "Reloading", PerkFactory.Perks.Firearm, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Crafting, "Crafting", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Woodwork, "Carpentry", PerkFactory.Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Cooking, "Cooking", PerkFactory.Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Farming, "Farming", PerkFactory.Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Doctor, "Doctor", PerkFactory.Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Electricity, "Electricity", PerkFactory.Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.MetalWelding, "MetalWelding", PerkFactory.Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Mechanics, "Mechanics", PerkFactory.Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Tailoring, "Tailoring", PerkFactory.Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Survivalist, "Survivalist", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Fishing, "Fishing", PerkFactory.Perks.Survivalist, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Trapping, "Trapping", PerkFactory.Perks.Survivalist, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.PlantScavenging, "Foraging", PerkFactory.Perks.Survivalist, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Passiv, "Passive", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000, true);
      AddPerk(PerkFactory.Perks.Fitness, "Fitness", PerkFactory.Perks.Passiv, 1000, 2000, 4000, 6000, 12000, 20000, 40000, 60000, 80000, 100000, true);
      AddPerk(PerkFactory.Perks.Strength, "Strength", PerkFactory.Perks.Passiv, 1000, 2000, 4000, 6000, 12000, 20000, 40000, 60000, 80000, 100000, true);
      AddPerk(PerkFactory.Perks.Agility, "Agility", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Sprinting, "Sprinting", PerkFactory.Perks.Agility, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Lightfoot, "Lightfooted", PerkFactory.Perks.Agility, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Nimble, "Nimble", PerkFactory.Perks.Agility, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
      AddPerk(PerkFactory.Perks.Sneak, "Sneaking", PerkFactory.Perks.Agility, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
   }

   public static void initTranslations() {
      PerkByName.clear();
      Iterator var0 = PerkList.iterator();

      while(var0.hasNext()) {
         Perk var1 = (Perk)var0.next();
         var1.name = Translator.getText("IGUI_perks_" + var1.translation);
         PerkByName.put(var1.name, var1);
      }

   }

   public static void Reset() {
      NextPerkID = 0;

      for(int var0 = PerkByIndex.length - 1; var0 >= 0; --var0) {
         Perk var1 = PerkByIndex[var0];
         if (var1 != null) {
            if (var1.isCustom()) {
               PerkList.remove(var1);
               PerkById.remove(var1.getId());
               PerkByName.remove(var1.getName());
               PerkByIndex[var1.index] = null;
            } else if (var1 != PerkFactory.Perks.MAX && NextPerkID == 0) {
               NextPerkID = var0 + 1;
            }
         }
      }

      PerkFactory.Perks.MAX.index = NextPerkID;
   }

   public static final class Perk {
      private final String id;
      private int index;
      private boolean bCustom;
      public String translation;
      public String name;
      public boolean passiv;
      public int xp1;
      public int xp2;
      public int xp3;
      public int xp4;
      public int xp5;
      public int xp6;
      public int xp7;
      public int xp8;
      public int xp9;
      public int xp10;
      public Perk parent;

      public Perk(String var1) {
         this.bCustom = false;
         this.passiv = false;
         this.parent = PerkFactory.Perks.None;
         this.id = var1;
         this.index = PerkFactory.NextPerkID++;
         this.translation = var1;
         this.name = var1;
         PerkFactory.PerkById.put(var1, this);
         PerkFactory.PerkByIndex[this.index] = this;
         if (PerkFactory.Perks.MAX != null) {
            PerkFactory.Perks.MAX.index = PZMath.max(PerkFactory.Perks.MAX.index, this.index + 1);
         }

      }

      public Perk(String var1, Perk var2) {
         this(var1);
         this.parent = var2;
      }

      public String getId() {
         return this.id;
      }

      public int index() {
         return this.index;
      }

      public void setCustom() {
         this.bCustom = true;
      }

      public boolean isCustom() {
         return this.bCustom;
      }

      public boolean isPassiv() {
         return this.passiv;
      }

      public Perk getParent() {
         return this.parent;
      }

      public String getName() {
         return this.name;
      }

      public Perk getType() {
         return this;
      }

      public int getXp1() {
         return this.xp1;
      }

      public int getXp2() {
         return this.xp2;
      }

      public int getXp3() {
         return this.xp3;
      }

      public int getXp4() {
         return this.xp4;
      }

      public int getXp5() {
         return this.xp5;
      }

      public int getXp6() {
         return this.xp6;
      }

      public int getXp7() {
         return this.xp7;
      }

      public int getXp8() {
         return this.xp8;
      }

      public int getXp9() {
         return this.xp9;
      }

      public int getXp10() {
         return this.xp10;
      }

      public float getXpForLevel(int var1) {
         if (var1 == 1) {
            return (float)this.xp1;
         } else if (var1 == 2) {
            return (float)this.xp2;
         } else if (var1 == 3) {
            return (float)this.xp3;
         } else if (var1 == 4) {
            return (float)this.xp4;
         } else if (var1 == 5) {
            return (float)this.xp5;
         } else if (var1 == 6) {
            return (float)this.xp6;
         } else if (var1 == 7) {
            return (float)this.xp7;
         } else if (var1 == 8) {
            return (float)this.xp8;
         } else if (var1 == 9) {
            return (float)this.xp9;
         } else {
            return var1 == 10 ? (float)this.xp10 : -1.0F;
         }
      }

      public float getTotalXpForLevel(int var1) {
         int var2 = 0;

         for(int var3 = 1; var3 <= var1; ++var3) {
            float var4 = this.getXpForLevel(var3);
            if (var4 != -1.0F) {
               var2 = (int)((float)var2 + var4);
            }
         }

         return (float)var2;
      }

      public String toString() {
         return this.id;
      }
   }

   public static final class Perks {
      public static final Perk None = new Perk("None");
      public static final Perk Agility = new Perk("Agility");
      public static final Perk Cooking = new Perk("Cooking");
      public static final Perk Melee = new Perk("Melee");
      public static final Perk Crafting = new Perk("Crafting");
      public static final Perk Fitness = new Perk("Fitness");
      public static final Perk Strength = new Perk("Strength");
      public static final Perk Blunt = new Perk("Blunt");
      public static final Perk Axe = new Perk("Axe");
      public static final Perk Sprinting = new Perk("Sprinting");
      public static final Perk Lightfoot = new Perk("Lightfoot");
      public static final Perk Nimble = new Perk("Nimble");
      public static final Perk Sneak = new Perk("Sneak");
      public static final Perk Woodwork = new Perk("Woodwork");
      public static final Perk Aiming = new Perk("Aiming");
      public static final Perk Reloading = new Perk("Reloading");
      public static final Perk Farming = new Perk("Farming");
      public static final Perk Survivalist = new Perk("Survivalist");
      public static final Perk Fishing = new Perk("Fishing");
      public static final Perk Trapping = new Perk("Trapping");
      public static final Perk Passiv = new Perk("Passiv");
      public static final Perk Firearm = new Perk("Firearm");
      public static final Perk PlantScavenging = new Perk("PlantScavenging");
      public static final Perk Doctor = new Perk("Doctor");
      public static final Perk Electricity = new Perk("Electricity");
      public static final Perk Blacksmith = new Perk("Blacksmith");
      public static final Perk MetalWelding = new Perk("MetalWelding");
      public static final Perk Melting = new Perk("Melting");
      public static final Perk Mechanics = new Perk("Mechanics");
      public static final Perk Spear = new Perk("Spear");
      public static final Perk Maintenance = new Perk("Maintenance");
      public static final Perk SmallBlade = new Perk("SmallBlade");
      public static final Perk LongBlade = new Perk("LongBlade");
      public static final Perk SmallBlunt = new Perk("SmallBlunt");
      public static final Perk Combat = new Perk("Combat");
      public static final Perk Tailoring = new Perk("Tailoring");
      public static final Perk MAX = new Perk("MAX");

      public Perks() {
      }

      public static int getMaxIndex() {
         return MAX.index();
      }

      public static Perk fromIndex(int var0) {
         return var0 >= 0 && var0 <= PerkFactory.NextPerkID ? PerkFactory.PerkByIndex[var0] : null;
      }

      public static Perk FromString(String var0) {
         return (Perk)PerkFactory.PerkById.getOrDefault(var0, MAX);
      }
   }
}
