package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.debug.DebugLog;
import zombie.iso.MultiStageBuilding;
import zombie.scripting.IScriptObjectStore;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.vehicles.VehicleEngineRPM;

public final class ScriptModule extends BaseScriptObject implements IScriptObjectStore {
   public String name;
   public String value;
   public final HashMap<String, Item> ItemMap = new HashMap();
   public final HashMap<String, GameSoundScript> GameSoundMap = new HashMap();
   public final ArrayList<GameSoundScript> GameSoundList = new ArrayList();
   public final HashMap<String, AnimationsMesh> AnimationsMeshMap = new HashMap();
   public final HashMap<String, MannequinScript> MannequinScriptMap = new HashMap();
   public final TreeMap<String, ModelScript> ModelScriptMap;
   public final HashMap<String, RuntimeAnimationScript> RuntimeAnimationScriptMap;
   public final HashMap<String, SoundTimelineScript> SoundTimelineMap;
   public final HashMap<String, VehicleScript> VehicleMap;
   public final HashMap<String, VehicleTemplate> VehicleTemplateMap;
   public final HashMap<String, VehicleEngineRPM> VehicleEngineRPMMap;
   public final ArrayList<Recipe> RecipeMap;
   public final HashMap<String, Recipe> RecipeByName;
   public final HashMap<String, Recipe> RecipesWithDotInName;
   public final ArrayList<EvolvedRecipe> EvolvedRecipeMap;
   public final ArrayList<UniqueRecipe> UniqueRecipeMap;
   public final HashMap<String, Fixing> FixingMap;
   public final ArrayList<String> Imports;
   public boolean disabled;

   public ScriptModule() {
      this.ModelScriptMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
      this.RuntimeAnimationScriptMap = new HashMap();
      this.SoundTimelineMap = new HashMap();
      this.VehicleMap = new HashMap();
      this.VehicleTemplateMap = new HashMap();
      this.VehicleEngineRPMMap = new HashMap();
      this.RecipeMap = new ArrayList();
      this.RecipeByName = new HashMap();
      this.RecipesWithDotInName = new HashMap();
      this.EvolvedRecipeMap = new ArrayList();
      this.UniqueRecipeMap = new ArrayList();
      this.FixingMap = new HashMap();
      this.Imports = new ArrayList();
      this.disabled = false;
   }

   public void Load(String var1, String var2) {
      this.name = var1;
      this.value = var2.trim();
      ScriptManager.instance.CurrentLoadingModule = this;
      this.ParseScriptPP(this.value);
      this.ParseScript(this.value);
      this.value = "";
   }

   private String GetTokenType(String var1) {
      int var2 = var1.indexOf(123);
      if (var2 == -1) {
         return null;
      } else {
         String var3 = var1.substring(0, var2).trim();
         int var4 = var3.indexOf(32);
         int var5 = var3.indexOf(9);
         if (var4 != -1 && var5 != -1) {
            return var3.substring(0, PZMath.min(var4, var5));
         } else if (var4 != -1) {
            return var3.substring(0, var4);
         } else {
            return var5 != -1 ? var3.substring(0, var5) : var3;
         }
      }
   }

   private void CreateFromTokenPP(String var1) {
      var1 = var1.trim();
      String var2 = this.GetTokenType(var1);
      if (var2 != null) {
         String[] var3;
         String var4;
         if ("item".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("item", "");
            var4 = var4.trim();
            Item var5 = new Item();
            this.ItemMap.put(var4, var5);
         } else if ("animationsMesh".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("animationsMesh", "");
            var4 = var4.trim();
            AnimationsMesh var9;
            if (this.AnimationsMeshMap.containsKey(var4)) {
               var9 = (AnimationsMesh)this.AnimationsMeshMap.get(var4);
               var9.reset();
            } else {
               var9 = new AnimationsMesh();
               this.AnimationsMeshMap.put(var4, var9);
            }
         } else if ("mannequin".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("mannequin", "");
            var4 = var4.trim();
            MannequinScript var10;
            if (this.MannequinScriptMap.containsKey(var4)) {
               var10 = (MannequinScript)this.MannequinScriptMap.get(var4);
               var10.reset();
            } else {
               var10 = new MannequinScript();
               this.MannequinScriptMap.put(var4, var10);
            }
         } else if ("model".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("model", "");
            var4 = var4.trim();
            ModelScript var11;
            if (this.ModelScriptMap.containsKey(var4)) {
               var11 = (ModelScript)this.ModelScriptMap.get(var4);
               var11.reset();
            } else {
               var11 = new ModelScript();
               this.ModelScriptMap.put(var4, var11);
            }
         } else if ("sound".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("sound", "");
            var4 = var4.trim();
            GameSoundScript var12;
            if (this.GameSoundMap.containsKey(var4)) {
               var12 = (GameSoundScript)this.GameSoundMap.get(var4);
               var12.reset();
            } else {
               var12 = new GameSoundScript();
               this.GameSoundMap.put(var4, var12);
               this.GameSoundList.add(var12);
            }
         } else if ("soundTimeline".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("soundTimeline", "");
            var4 = var4.trim();
            SoundTimelineScript var13;
            if (this.SoundTimelineMap.containsKey(var4)) {
               var13 = (SoundTimelineScript)this.SoundTimelineMap.get(var4);
               var13.reset();
            } else {
               var13 = new SoundTimelineScript();
               this.SoundTimelineMap.put(var4, var13);
            }
         } else if ("vehicle".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("vehicle", "");
            var4 = var4.trim();
            VehicleScript var14 = new VehicleScript();
            this.VehicleMap.put(var4, var14);
         } else if ("template".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("template", "");
            String[] var15 = var4.trim().split("\\s+");
            if (var15.length == 2) {
               String var6 = var15[0].trim();
               String var7 = var15[1].trim();
               if ("vehicle".equals(var6)) {
                  VehicleTemplate var8 = new VehicleTemplate(this, var7, var1);
                  var8.module = this;
                  this.VehicleTemplateMap.put(var7, var8);
               }
            }
         } else if ("animation".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("animation", "");
            var4 = var4.trim();
            RuntimeAnimationScript var16;
            if (this.RuntimeAnimationScriptMap.containsKey(var4)) {
               var16 = (RuntimeAnimationScript)this.RuntimeAnimationScriptMap.get(var4);
               var16.reset();
            } else {
               var16 = new RuntimeAnimationScript();
               this.RuntimeAnimationScriptMap.put(var4, var16);
            }
         } else if ("vehicleEngineRPM".equals(var2)) {
            var3 = var1.split("[{}]");
            var4 = var3[0];
            var4 = var4.replace("vehicleEngineRPM", "");
            var4 = var4.trim();
            VehicleEngineRPM var17;
            if (this.VehicleEngineRPMMap.containsKey(var4)) {
               var17 = (VehicleEngineRPM)this.VehicleEngineRPMMap.get(var4);
               var17.reset();
            } else {
               var17 = new VehicleEngineRPM();
               this.VehicleEngineRPMMap.put(var4, var17);
            }
         }

      }
   }

   private void CreateFromToken(String var1) {
      var1 = var1.trim();
      String var2 = this.GetTokenType(var1);
      if (var2 != null) {
         String[] var3;
         if ("imports".equals(var2)) {
            var3 = var1.split("[{}]");
            String[] var4 = var3[1].split(",");

            for(int var5 = 0; var5 < var4.length; ++var5) {
               if (var4[var5].trim().length() > 0) {
                  String var6 = var4[var5].trim();
                  if (var6.equals(this.getName())) {
                     DebugLog.log("ERROR: module \"" + this.getName() + "\" imports itself");
                  } else {
                     this.Imports.add(var6);
                  }
               }
            }
         } else {
            String var18;
            String[] var19;
            if ("item".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("item", "");
               var18 = var18.trim();
               var19 = var3[1].split(",");
               Item var20 = (Item)this.ItemMap.get(var18);
               var20.module = this;

               try {
                  var20.Load(var18, var19);
               } catch (Exception var17) {
                  DebugLog.log((Object)var17);
               }
            } else if ("recipe".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("recipe", "");
               var18 = var18.trim();
               var19 = var3[1].split(",");
               Recipe var21 = new Recipe();
               this.RecipeMap.add(var21);
               if (!this.RecipeByName.containsKey(var18)) {
                  this.RecipeByName.put(var18, var21);
               }

               if (var18.contains(".")) {
                  this.RecipesWithDotInName.put(var18, var21);
               }

               var21.module = this;
               var21.Load(var18, var19);
            } else if ("uniquerecipe".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("uniquerecipe", "");
               var18 = var18.trim();
               var19 = var3[1].split(",");
               UniqueRecipe var22 = new UniqueRecipe(var18);
               this.UniqueRecipeMap.add(var22);
               var22.module = this;
               var22.Load(var18, var19);
            } else if ("evolvedrecipe".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("evolvedrecipe", "");
               var18 = var18.trim();
               var19 = var3[1].split(",");
               boolean var23 = false;
               Iterator var7 = this.EvolvedRecipeMap.iterator();

               while(var7.hasNext()) {
                  EvolvedRecipe var8 = (EvolvedRecipe)var7.next();
                  if (var8.name.equals(var18)) {
                     var8.Load(var18, var19);
                     var8.module = this;
                     var23 = true;
                  }
               }

               if (!var23) {
                  EvolvedRecipe var25 = new EvolvedRecipe(var18);
                  this.EvolvedRecipeMap.add(var25);
                  var25.module = this;
                  var25.Load(var18, var19);
               }
            } else if ("fixing".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("fixing", "");
               var18 = var18.trim();
               var19 = var3[1].split(",");
               Fixing var27 = new Fixing();
               var27.module = this;
               this.FixingMap.put(var18, var27);
               var27.Load(var18, var19);
            } else if ("animationsMesh".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("animationsMesh", "");
               var18 = var18.trim();
               AnimationsMesh var24 = (AnimationsMesh)this.AnimationsMeshMap.get(var18);
               var24.module = this;

               try {
                  var24.Load(var18, var1);
               } catch (Throwable var16) {
                  ExceptionLogger.logException(var16);
               }
            } else if ("mannequin".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("mannequin", "");
               var18 = var18.trim();
               MannequinScript var26 = (MannequinScript)this.MannequinScriptMap.get(var18);
               var26.module = this;

               try {
                  var26.Load(var18, var1);
               } catch (Throwable var15) {
                  ExceptionLogger.logException(var15);
               }
            } else if ("multistagebuild".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("multistagebuild", "");
               var18 = var18.trim();
               var19 = var3[1].split(",");
               MultiStageBuilding.Stage var28 = new MultiStageBuilding().new Stage();
               var28.Load(var18, var19);
               MultiStageBuilding.addStage(var28);
            } else if ("model".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("model", "");
               var18 = var18.trim();
               ModelScript var29 = (ModelScript)this.ModelScriptMap.get(var18);
               var29.module = this;

               try {
                  var29.Load(var18, var1);
               } catch (Throwable var14) {
                  ExceptionLogger.logException(var14);
               }
            } else if ("sound".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("sound", "");
               var18 = var18.trim();
               GameSoundScript var30 = (GameSoundScript)this.GameSoundMap.get(var18);
               var30.module = this;

               try {
                  var30.Load(var18, var1);
               } catch (Throwable var13) {
                  ExceptionLogger.logException(var13);
               }
            } else if ("soundTimeline".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("soundTimeline", "");
               var18 = var18.trim();
               SoundTimelineScript var31 = (SoundTimelineScript)this.SoundTimelineMap.get(var18);
               var31.module = this;

               try {
                  var31.Load(var18, var1);
               } catch (Throwable var12) {
                  ExceptionLogger.logException(var12);
               }
            } else if ("vehicle".equals(var2)) {
               var3 = var1.split("[{}]");
               var18 = var3[0];
               var18 = var18.replace("vehicle", "");
               var18 = var18.trim();
               VehicleScript var32 = (VehicleScript)this.VehicleMap.get(var18);
               var32.module = this;

               try {
                  var32.Load(var18, var1);
                  var32.Loaded();
               } catch (Exception var11) {
                  ExceptionLogger.logException(var11);
               }
            } else if (!"template".equals(var2)) {
               if ("animation".equals(var2)) {
                  var3 = var1.split("[{}]");
                  var18 = var3[0];
                  var18 = var18.replace("animation", "");
                  var18 = var18.trim();
                  RuntimeAnimationScript var33 = (RuntimeAnimationScript)this.RuntimeAnimationScriptMap.get(var18);
                  var33.module = this;

                  try {
                     var33.Load(var18, var1);
                  } catch (Throwable var10) {
                     ExceptionLogger.logException(var10);
                  }
               } else if ("vehicleEngineRPM".equals(var2)) {
                  var3 = var1.split("[{}]");
                  var18 = var3[0];
                  var18 = var18.replace("vehicleEngineRPM", "");
                  var18 = var18.trim();
                  VehicleEngineRPM var34 = (VehicleEngineRPM)this.VehicleEngineRPMMap.get(var18);
                  var34.module = this;

                  try {
                     var34.Load(var18, var1);
                  } catch (Throwable var9) {
                     this.VehicleEngineRPMMap.remove(var18);
                     ExceptionLogger.logException(var9);
                  }
               } else {
                  DebugLog.Script.warn("unknown script object \"%s\"", var2);
               }
            }
         }

      }
   }

   public void ParseScript(String var1) {
      ArrayList var2 = ScriptParser.parseTokens(var1);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         String var4 = (String)var2.get(var3);
         this.CreateFromToken(var4);
      }

   }

   public void ParseScriptPP(String var1) {
      ArrayList var2 = ScriptParser.parseTokens(var1);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         String var4 = (String)var2.get(var3);
         this.CreateFromTokenPP(var4);
      }

   }

   public Item getItem(String var1) {
      if (var1.contains(".")) {
         return ScriptManager.instance.getItem(var1);
      } else if (!this.ItemMap.containsKey(var1)) {
         for(int var2 = 0; var2 < this.Imports.size(); ++var2) {
            String var3 = (String)this.Imports.get(var2);
            ScriptModule var4 = ScriptManager.instance.getModule(var3);
            Item var5 = var4.getItem(var1);
            if (var5 != null) {
               return var5;
            }
         }

         return null;
      } else {
         return (Item)this.ItemMap.get(var1);
      }
   }

   public ModelScript getModelScript(String var1) {
      if (var1.contains(".")) {
         return ScriptManager.instance.getModelScript(var1);
      } else {
         ModelScript var2 = (ModelScript)this.ModelScriptMap.get(var1);
         if (var2 == null) {
            for(int var3 = 0; var3 < this.Imports.size(); ++var3) {
               String var4 = (String)this.Imports.get(var3);
               ScriptModule var5 = ScriptManager.instance.getModule(var4);
               var2 = var5.getModelScript(var1);
               if (var2 != null) {
                  return var2;
               }
            }

            return null;
         } else {
            return var2;
         }
      }
   }

   public Recipe getRecipe(String var1) {
      if (var1.contains(".") && !this.RecipesWithDotInName.containsKey(var1)) {
         return ScriptManager.instance.getRecipe(var1);
      } else {
         Recipe var2 = (Recipe)this.RecipeByName.get(var1);
         if (var2 != null) {
            return var2;
         } else {
            for(int var3 = 0; var3 < this.Imports.size(); ++var3) {
               ScriptModule var4 = ScriptManager.instance.getModule((String)this.Imports.get(var3));
               if (var4 != null) {
                  var2 = var4.getRecipe(var1);
                  if (var2 != null) {
                     return var2;
                  }
               }
            }

            return null;
         }
      }
   }

   public VehicleScript getVehicle(String var1) {
      if (var1.contains(".")) {
         return ScriptManager.instance.getVehicle(var1);
      } else if (!this.VehicleMap.containsKey(var1)) {
         for(int var2 = 0; var2 < this.Imports.size(); ++var2) {
            VehicleScript var3 = ScriptManager.instance.getModule((String)this.Imports.get(var2)).getVehicle(var1);
            if (var3 != null) {
               return var3;
            }
         }

         return null;
      } else {
         return (VehicleScript)this.VehicleMap.get(var1);
      }
   }

   public VehicleTemplate getVehicleTemplate(String var1) {
      if (var1.contains(".")) {
         return ScriptManager.instance.getVehicleTemplate(var1);
      } else if (!this.VehicleTemplateMap.containsKey(var1)) {
         for(int var2 = 0; var2 < this.Imports.size(); ++var2) {
            VehicleTemplate var3 = ScriptManager.instance.getModule((String)this.Imports.get(var2)).getVehicleTemplate(var1);
            if (var3 != null) {
               return var3;
            }
         }

         return null;
      } else {
         return (VehicleTemplate)this.VehicleTemplateMap.get(var1);
      }
   }

   public VehicleEngineRPM getVehicleEngineRPM(String var1) {
      return var1.contains(".") ? ScriptManager.instance.getVehicleEngineRPM(var1) : (VehicleEngineRPM)this.VehicleEngineRPMMap.get(var1);
   }

   public boolean CheckExitPoints() {
      return false;
   }

   public String getName() {
      return this.name;
   }

   public void Reset() {
      this.ItemMap.clear();
      this.GameSoundMap.clear();
      this.GameSoundList.clear();
      this.AnimationsMeshMap.clear();
      this.MannequinScriptMap.clear();
      this.ModelScriptMap.clear();
      this.RuntimeAnimationScriptMap.clear();
      this.SoundTimelineMap.clear();
      this.VehicleMap.clear();
      this.VehicleTemplateMap.clear();
      this.VehicleEngineRPMMap.clear();
      this.RecipeMap.clear();
      this.RecipeByName.clear();
      this.RecipesWithDotInName.clear();
      this.EvolvedRecipeMap.clear();
      this.UniqueRecipeMap.clear();
      this.FixingMap.clear();
      this.Imports.clear();
   }

   public Item getSpecificItem(String var1) {
      return (Item)this.ItemMap.get(var1);
   }
}
