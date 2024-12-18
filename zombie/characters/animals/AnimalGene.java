package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public class AnimalGene {
   public String name;
   public int id = Rand.Next(1000000);
   public AnimalAllele allele1;
   public AnimalAllele allele2;

   public AnimalGene() {
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      var1.putInt(this.id);
      GameWindow.WriteString(var1, this.name);
      this.allele1.save(var1, var2);
      this.allele2.save(var1, var2);
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      this.id = var1.getInt();
      this.name = GameWindow.ReadString(var1);
      this.allele1 = new AnimalAllele();
      this.allele1.load(var1, var2, var3);
      this.allele2 = new AnimalAllele();
      this.allele2.load(var1, var2, var3);
   }

   public static void initGenome(IsoAnimal var0) {
      if (var0.adef.genes != null) {
         new AnimalGene();
         ArrayList var2 = var0.adef.genes;
         var0.fullGenome = new HashMap();
         HashMap var3 = var0.getBreed().forcedGenes;
         AnimalGene var1;
         Iterator var4;
         String var5;
         AnimalGenomeDefinitions var7;
         int var8;
         if (var3 != null) {
            var4 = var3.keySet().iterator();

            label79:
            while(true) {
               while(true) {
                  if (!var4.hasNext()) {
                     break label79;
                  }

                  var1 = new AnimalGene();
                  var5 = ((String)var4.next()).toLowerCase();
                  var1.name = var5;
                  AnimalBreed.ForcedGenes var6 = (AnimalBreed.ForcedGenes)var3.get(var5);
                  var7 = (AnimalGenomeDefinitions)AnimalGenomeDefinitions.fullGenomeDef.get(var5);
                  if (var7 == null) {
                     DebugLog.Animal.debugln(var5 + " wasn't found in AnimalGenomeDefinitions.lua");
                  } else {
                     for(var8 = 0; var8 < 2; ++var8) {
                        AnimalAllele var9 = initAllele(var5, true, Rand.Next(var6.minValue, var6.maxValue), var7.forcedValues);
                        if (var8 == 0) {
                           var1.allele1 = var9;
                        } else {
                           var1.allele2 = var9;
                        }
                     }

                     var1.initUsedGene();
                     var0.fullGenome.put(var5, var1);
                  }
               }
            }
         }

         for(int var10 = 0; var10 < var2.size(); ++var10) {
            var1 = new AnimalGene();
            var5 = ((String)var2.get(var10)).toLowerCase();
            var1.name = var5;
            if (!var0.fullGenome.containsKey(var5)) {
               new AnimalAllele();
               var7 = (AnimalGenomeDefinitions)AnimalGenomeDefinitions.fullGenomeDef.get(var5);
               if (var7 == null) {
                  DebugLog.Animal.debugln(var5 + " wasn't found in AnimalGenomeDefinitions.lua");
               } else {
                  for(var8 = 0; var8 < 2; ++var8) {
                     AnimalAllele var12 = initAllele(var5, false, Rand.Next(var7.minValue, var7.maxValue), var7.forcedValues);
                     if (var8 == 0) {
                        var1.allele1 = var12;
                     } else {
                        var1.allele2 = var12;
                     }
                  }

                  var1.initUsedGene();
                  var0.fullGenome.put(var5, var1);
               }
            }
         }

         AnimalGenomeDefinitions var13;
         AnimalAllele var14;
         for(var4 = var0.fullGenome.keySet().iterator(); var4.hasNext(); doRatio(var13, var0.fullGenome, var14)) {
            AnimalGene var11 = (AnimalGene)var0.fullGenome.get(var4.next());
            var13 = (AnimalGenomeDefinitions)AnimalGenomeDefinitions.fullGenomeDef.get(var11.name);
            var14 = var11.allele1;
            if (var11.allele2.used) {
               var14 = var11.allele2;
            }
         }

      }
   }

   public void initUsedGene() {
      boolean var1 = false;
      int var2;
      if (this.allele1.dominant) {
         if (this.allele2.dominant) {
            var2 = Rand.Next(2);
         } else {
            var2 = 0;
         }
      } else if (this.allele2.dominant) {
         if (this.allele1.dominant) {
            var2 = Rand.Next(2);
         } else {
            var2 = 1;
         }
      } else {
         var2 = Rand.Next(2);
      }

      if (var2 == 0) {
         this.allele1.used = true;
      } else {
         this.allele2.used = true;
      }

   }

   private static AnimalAllele initAllele(String var0, boolean var1, float var2, boolean var3) {
      if (var2 < 0.0F) {
         var2 = 0.0F;
      }

      AnimalAllele var4 = new AnimalAllele();
      if (var1) {
         var4.dominant = true;
      } else {
         if (!var3 && Rand.Next(100) < 15) {
            var2 = Rand.Next(0.0F, 1.0F);
         }

         float var5 = Math.abs(var2 - 0.5F);
         byte var6 = 30;
         if ((double)var5 > 0.45) {
            var6 = 90;
         } else if ((double)var5 > 0.4) {
            var6 = 80;
         } else if ((double)var5 > 0.3) {
            var6 = 75;
         } else if ((double)var5 > 0.2) {
            var6 = 60;
         } else if ((double)var5 > 0.15) {
            var6 = 50;
         } else if ((double)var5 > 0.1) {
            var6 = 40;
         }

         var4.dominant = Rand.Next(100) > var6;
      }

      var4.name = var0;
      var4.currentValue = var2;
      doMutation(var4);
      return var4;
   }

   public static void doRatio(AnimalGenomeDefinitions var0, HashMap<String, AnimalGene> var1, AnimalAllele var2) {
      if (var0.ratios != null) {
         Iterator var3 = var0.ratios.keySet().iterator();

         while(var3.hasNext()) {
            String var4 = ((String)var3.next()).toLowerCase();
            Float var5 = (Float)var0.ratios.get(var4);
            AnimalGene var6 = (AnimalGene)var1.get(var4);
            if (var6 == null) {
               DebugLog.Animal.debugln("RATIO CALC: " + var4 + " wasn't found in animal genome but define in animal's genes ratio");
            } else {
               float var7 = var5 - var2.currentValue;
               var6.allele1.trueRatioValue = Math.max(Rand.Next(0.01F, 0.05F), var6.allele1.currentValue + var7);
               var6.allele2.trueRatioValue = Math.max(Rand.Next(0.01F, 0.05F), var6.allele2.currentValue + var7);
            }
         }

      }
   }

   public static HashMap<String, AnimalGene> initGenesFromParents(HashMap<String, AnimalGene> var0, HashMap<String, AnimalGene> var1) {
      HashMap var2 = new HashMap();
      if (var1 == null || var1.isEmpty()) {
         var1 = var0;
      }

      Iterator var3 = var0.keySet().iterator();

      String var4;
      AnimalGene var5;
      AnimalAllele var7;
      while(var3.hasNext()) {
         var4 = (String)var3.next();
         var5 = (AnimalGene)var0.get(var4);
         AnimalGene var6 = (AnimalGene)var1.get(var4);
         if (var6 == null) {
            var6 = var5;
         }

         if (var5 == null) {
            var5 = var6;
         }

         var7 = Rand.NextBool(2) ? var5.allele1 : var5.allele2;
         AnimalAllele var8 = Rand.NextBool(2) ? var6.allele1 : var6.allele2;
         AnimalGene var9 = new AnimalGene();
         var9.name = var4;
         var9.allele1 = new AnimalAllele();
         var9.allele1.currentValue = var7.currentValue;
         var9.allele1.dominant = var7.dominant;
         var9.allele1.name = var7.name;
         var9.allele1.geneticDisorder = var7.geneticDisorder;
         var9.allele2 = new AnimalAllele();
         var9.allele2.currentValue = var8.currentValue;
         var9.allele2.dominant = var8.dominant;
         var9.allele2.name = var8.name;
         var9.allele2.geneticDisorder = var8.geneticDisorder;
         var9.initUsedGene();
         var2.put(var4, var9);
      }

      var3 = var2.keySet().iterator();

      AnimalGenomeDefinitions var10;
      while(var3.hasNext()) {
         var4 = (String)var3.next();
         var5 = (AnimalGene)var2.get(var4);
         var10 = (AnimalGenomeDefinitions)AnimalGenomeDefinitions.fullGenomeDef.get(var5.name);
         doMutation(var5.allele1);
         doMutation(var5.allele2);
      }

      for(var3 = var2.keySet().iterator(); var3.hasNext(); doRatio(var10, var2, var7)) {
         var4 = (String)var3.next();
         var5 = (AnimalGene)var2.get(var4);
         var10 = (AnimalGenomeDefinitions)AnimalGenomeDefinitions.fullGenomeDef.get(var5.name);
         var7 = var5.allele1;
         if (var5.allele2.used) {
            var7 = var5.allele2;
         }
      }

      return var2;
   }

   public static void checkGeneticDisorder(IsoAnimal var0) {
      for(int var1 = 0; var1 < var0.getFullGenomeList().size(); ++var1) {
         AnimalGene var2 = (AnimalGene)var0.getFullGenomeList().get(var1);
         if (!StringUtils.isNullOrEmpty(var2.allele1.geneticDisorder) && var2.allele1.geneticDisorder.equals(var2.allele2.geneticDisorder) && !var0.geneticDisorder.contains(var2.allele1.geneticDisorder)) {
            var0.geneticDisorder.add(var2.allele1.geneticDisorder);
         }
      }

   }

   public static void doMutation(AnimalAllele var0) {
      if (Rand.Next(100) <= 10) {
         var0.currentValue += Rand.NextBool(2) ? 0.05F : -0.05F;
      }

      if (Rand.Next(100) <= 5) {
         var0.currentValue += 0.2F;
      }

      if (Rand.Next(100) <= 5) {
         var0.dominant = !var0.dominant;
      }

      if (StringUtils.isNullOrEmpty(var0.geneticDisorder) && Rand.Next(100) <= 2) {
         var0.geneticDisorder = (String)AnimalGenomeDefinitions.geneticDisorder.get(Rand.Next(0, AnimalGenomeDefinitions.geneticDisorder.size()));
      }

      if (Rand.Next(100) <= 2) {
         var0.geneticDisorder = null;
      }

      if (var0.currentValue <= 0.05F) {
         var0.currentValue = 0.05F;
      }

   }

   public String getName() {
      return this.name;
   }

   public AnimalAllele getAllele1() {
      return this.allele1;
   }

   public AnimalAllele getAllele2() {
      return this.allele2;
   }

   public AnimalAllele getUsedGene() {
      return this.allele1.used ? this.allele1 : this.allele2;
   }
}
