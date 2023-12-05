package zombie.scripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import zombie.GameSounds;
import zombie.SoundManager;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.IndieFileLoader;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.inventory.RecipeManager;
import zombie.iso.IsoWorld;
import zombie.iso.MultiStageBuilding;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetChecksum;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Fixing;
import zombie.scripting.objects.GameSoundScript;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.MannequinScript;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ScriptModule;
import zombie.scripting.objects.SoundTimelineScript;
import zombie.scripting.objects.UniqueRecipe;
import zombie.scripting.objects.VehicleScript;
import zombie.scripting.objects.VehicleTemplate;
import zombie.util.StringUtils;
import zombie.vehicles.VehicleEngineRPM;
import zombie.world.WorldDictionary;

public final class ScriptManager implements IScriptObjectStore {
   public static final ScriptManager instance = new ScriptManager();
   public String currentFileName;
   public final ArrayList<String> scriptsWithVehicles = new ArrayList();
   public final ArrayList<String> scriptsWithVehicleTemplates = new ArrayList();
   public final HashMap<String, ScriptModule> ModuleMap = new HashMap();
   public final ArrayList<ScriptModule> ModuleList = new ArrayList();
   private final HashMap<String, Item> FullTypeToItemMap = new HashMap();
   private final HashMap<String, SoundTimelineScript> SoundTimelineMap = new HashMap();
   public ScriptModule CurrentLoadingModule = null;
   private final HashMap<String, String> ModuleAliases = new HashMap();
   private final StringBuilder buf = new StringBuilder();
   private final HashMap<String, ScriptModule> CachedModules = new HashMap();
   private final ArrayList<Recipe> recipesTempList = new ArrayList();
   private final Stack<EvolvedRecipe> evolvedRecipesTempList = new Stack();
   private final Stack<UniqueRecipe> uniqueRecipesTempList = new Stack();
   private final ArrayList<Item> itemTempList = new ArrayList();
   private final HashMap<String, ArrayList<Item>> tagToItemMap = new HashMap();
   private final HashMap<String, ArrayList<Item>> typeToItemMap = new HashMap();
   private final ArrayList<AnimationsMesh> animationsMeshTempList = new ArrayList();
   private final ArrayList<MannequinScript> mannequinScriptTempList = new ArrayList();
   private final ArrayList<ModelScript> modelScriptTempList = new ArrayList();
   private final ArrayList<VehicleScript> vehicleScriptTempList = new ArrayList();
   private final HashMap<String, String> clothingToItemMap = new HashMap();
   private final ArrayList<String> visualDamagesList = new ArrayList();
   private static final String Base = "Base";
   private static final String Base_Module = "Base.";
   private String checksum = "";
   private HashMap<String, String> tempFileToModMap;
   private static String currentLoadFileMod;
   private static String currentLoadFileAbsPath;
   public static final String VanillaID = "pz-vanilla";

   public ScriptManager() {
   }

   public void ParseScript(String var1) {
      if (DebugLog.isEnabled(DebugType.Script)) {
         DebugLog.Script.debugln("Parsing...");
      }

      ArrayList var2 = ScriptParser.parseTokens(var1);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         String var4 = (String)var2.get(var3);
         this.CreateFromToken(var4);
      }

   }

   public void update() {
   }

   public void LoadFile(String var1, boolean var2) throws FileNotFoundException {
      if (DebugLog.isEnabled(DebugType.Script)) {
         DebugLog.Script.debugln(var1 + (var2 ? " bLoadJar" : ""));
      }

      if (!GameServer.bServer) {
         Thread.yield();
         Core.getInstance().DoFrameReady();
      }

      if (var1.contains(".tmx")) {
         IsoWorld.mapPath = var1.substring(0, var1.lastIndexOf("/"));
         IsoWorld.mapUseJar = var2;
         DebugLog.Script.debugln("  file is a .tmx (map) file. Set mapPath to " + IsoWorld.mapPath + (IsoWorld.mapUseJar ? " mapUseJar" : ""));
      } else if (!var1.endsWith(".txt")) {
         DebugLog.Script.warn(" file is not a .txt (script) file: " + var1);
      } else {
         InputStreamReader var3 = IndieFileLoader.getStreamReader(var1, !var2);
         BufferedReader var4 = new BufferedReader(var3);
         this.buf.setLength(0);
         String var5 = null;
         String var6 = "";

         label135: {
            try {
               while(true) {
                  if ((var5 = var4.readLine()) == null) {
                     break label135;
                  }

                  this.buf.append(var5);
                  this.buf.append('\n');
               }
            } catch (Exception var17) {
               DebugLog.Script.error("Exception thrown reading file " + var1 + "\n  " + var17);
            } finally {
               try {
                  var4.close();
                  var3.close();
               } catch (Exception var16) {
                  DebugLog.Script.error("Exception thrown closing file " + var1 + "\n  " + var16);
                  var16.printStackTrace(DebugLog.Script);
               }

            }

            return;
         }

         var6 = this.buf.toString();
         var6 = ScriptParser.stripComments(var6);
         this.currentFileName = var1;
         this.ParseScript(var6);
         this.currentFileName = null;
      }
   }

   private void CreateFromToken(String var1) {
      var1 = var1.trim();
      if (var1.indexOf("module") == 0) {
         int var2 = var1.indexOf("{");
         int var3 = var1.lastIndexOf("}");
         String[] var4 = var1.split("[{}]");
         String var5 = var4[0];
         var5 = var5.replace("module", "");
         var5 = var5.trim();
         String var6 = var1.substring(var2 + 1, var3);
         ScriptModule var7 = (ScriptModule)this.ModuleMap.get(var5);
         if (var7 == null) {
            if (DebugLog.isEnabled(DebugType.Script)) {
               DebugLog.Script.debugln("Adding new module: " + var5);
            }

            var7 = new ScriptModule();
            this.ModuleMap.put(var5, var7);
            this.ModuleList.add(var7);
         }

         var7.Load(var5, var6);
      }

   }

   public void searchFolders(URI var1, File var2, ArrayList<String> var3) {
      if (var2.isDirectory()) {
         String[] var4 = var2.list();

         for(int var5 = 0; var5 < var4.length; ++var5) {
            String var10004 = var2.getAbsolutePath();
            this.searchFolders(var1, new File(var10004 + File.separator + var4[var5]), var3);
         }
      } else if (var2.getAbsolutePath().toLowerCase().endsWith(".txt")) {
         String var6 = ZomboidFileSystem.instance.getRelativeFile(var1, var2.getAbsolutePath());
         var6 = var6.toLowerCase(Locale.ENGLISH);
         var3.add(var6);
      }

   }

   public static String getItemName(String var0) {
      int var1 = var0.indexOf(46);
      return var1 == -1 ? var0 : var0.substring(var1 + 1);
   }

   public ScriptModule getModule(String var1) {
      return this.getModule(var1, true);
   }

   public ScriptModule getModule(String var1, boolean var2) {
      if (!var1.trim().equals("Base") && !var1.startsWith("Base.")) {
         if (this.CachedModules.containsKey(var1)) {
            return (ScriptModule)this.CachedModules.get(var1);
         } else {
            ScriptModule var3 = null;
            if (this.ModuleAliases.containsKey(var1)) {
               var1 = (String)this.ModuleAliases.get(var1);
            }

            if (this.CachedModules.containsKey(var1)) {
               return (ScriptModule)this.CachedModules.get(var1);
            } else {
               if (this.ModuleMap.containsKey(var1)) {
                  if (((ScriptModule)this.ModuleMap.get(var1)).disabled) {
                     var3 = null;
                  } else {
                     var3 = (ScriptModule)this.ModuleMap.get(var1);
                  }
               }

               if (var3 != null) {
                  this.CachedModules.put(var1, var3);
                  return var3;
               } else {
                  int var4 = var1.indexOf(".");
                  if (var4 != -1) {
                     var3 = this.getModule(var1.substring(0, var4));
                  }

                  if (var3 != null) {
                     this.CachedModules.put(var1, var3);
                     return var3;
                  } else {
                     return var2 ? (ScriptModule)this.ModuleMap.get("Base") : null;
                  }
               }
            }
         }
      } else {
         return (ScriptModule)this.ModuleMap.get("Base");
      }
   }

   public ScriptModule getModuleNoDisableCheck(String var1) {
      if (this.ModuleAliases.containsKey(var1)) {
         var1 = (String)this.ModuleAliases.get(var1);
      }

      if (this.ModuleMap.containsKey(var1)) {
         return (ScriptModule)this.ModuleMap.get(var1);
      } else {
         return var1.indexOf(".") != -1 ? this.getModule(var1.split("\\.")[0]) : null;
      }
   }

   public Item getItem(String var1) {
      if (var1.contains(".") && this.FullTypeToItemMap.containsKey(var1)) {
         return (Item)this.FullTypeToItemMap.get(var1);
      } else {
         ScriptModule var2 = this.getModule(var1);
         return var2 == null ? null : var2.getItem(getItemName(var1));
      }
   }

   public Item FindItem(String var1) {
      return this.FindItem(var1, true);
   }

   public Item FindItem(String var1, boolean var2) {
      if (var1.contains(".") && this.FullTypeToItemMap.containsKey(var1)) {
         return (Item)this.FullTypeToItemMap.get(var1);
      } else {
         ScriptModule var3 = this.getModule(var1, var2);
         if (var3 == null) {
            return null;
         } else {
            Item var4 = var3.getItem(getItemName(var1));
            if (var4 == null) {
               for(int var5 = 0; var5 < this.ModuleList.size(); ++var5) {
                  ScriptModule var6 = (ScriptModule)this.ModuleList.get(var5);
                  if (!var6.disabled) {
                     var4 = var3.getItem(getItemName(var1));
                     if (var4 != null) {
                        return var4;
                     }
                  }
               }
            }

            return var4;
         }
      }
   }

   public boolean isDrainableItemType(String var1) {
      Item var2 = this.FindItem(var1);
      if (var2 != null) {
         return var2.getType() == Item.Type.Drainable;
      } else {
         return false;
      }
   }

   public Recipe getRecipe(String var1) {
      ScriptModule var2 = this.getModule(var1);
      return var2 == null ? null : var2.getRecipe(getItemName(var1));
   }

   public VehicleScript getVehicle(String var1) {
      ScriptModule var2 = this.getModule(var1);
      return var2 == null ? null : var2.getVehicle(getItemName(var1));
   }

   public VehicleTemplate getVehicleTemplate(String var1) {
      ScriptModule var2 = this.getModule(var1);
      return var2 == null ? null : var2.getVehicleTemplate(getItemName(var1));
   }

   public VehicleEngineRPM getVehicleEngineRPM(String var1) {
      ScriptModule var2 = this.getModule(var1);
      return var2 == null ? null : var2.getVehicleEngineRPM(getItemName(var1));
   }

   public void CheckExitPoints() {
      for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
         ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
         if (!var2.disabled && var2.CheckExitPoints()) {
            return;
         }
      }

   }

   public ArrayList<Item> getAllItems() {
      if (this.itemTempList.isEmpty()) {
         for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
            ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
            if (!var2.disabled) {
               Iterator var3 = var2.ItemMap.values().iterator();

               while(var3.hasNext()) {
                  Item var4 = (Item)var3.next();
                  this.itemTempList.add(var4);
               }
            }
         }
      }

      return this.itemTempList;
   }

   public ArrayList<Item> getItemsTag(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         throw new IllegalArgumentException("invalid tag \"" + var1 + "\"");
      } else {
         var1 = var1.toLowerCase(Locale.ENGLISH);
         ArrayList var2 = (ArrayList)this.tagToItemMap.get(var1);
         if (var2 != null) {
            return var2;
         } else {
            var2 = new ArrayList();
            ArrayList var3 = this.getAllItems();

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               Item var5 = (Item)var3.get(var4);

               for(int var6 = 0; var6 < var5.Tags.size(); ++var6) {
                  if (((String)var5.Tags.get(var6)).equalsIgnoreCase(var1)) {
                     var2.add(var5);
                     break;
                  }
               }
            }

            this.tagToItemMap.put(var1, var2);
            return var2;
         }
      }
   }

   public ArrayList<Item> getItemsByType(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         throw new IllegalArgumentException("invalid type \"" + var1 + "\"");
      } else {
         ArrayList var2 = (ArrayList)this.typeToItemMap.get(var1);
         if (var2 != null) {
            return var2;
         } else {
            var2 = new ArrayList();

            for(int var3 = 0; var3 < this.ModuleList.size(); ++var3) {
               ScriptModule var4 = (ScriptModule)this.ModuleList.get(var3);
               if (!var4.disabled) {
                  Item var5 = (Item)this.FullTypeToItemMap.get(StringUtils.moduleDotType(var4.name, var1));
                  if (var5 != null) {
                     var2.add(var5);
                  }
               }
            }

            this.tagToItemMap.put(var1, var2);
            return var2;
         }
      }
   }

   public List<Fixing> getAllFixing(List<Fixing> var1) {
      for(int var2 = 0; var2 < this.ModuleList.size(); ++var2) {
         ScriptModule var3 = (ScriptModule)this.ModuleList.get(var2);
         if (!var3.disabled) {
            var1.addAll(var3.FixingMap.values());
         }
      }

      return var1;
   }

   public ArrayList<Recipe> getAllRecipes() {
      this.recipesTempList.clear();

      for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
         ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
         if (!var2.disabled) {
            for(int var3 = 0; var3 < var2.RecipeMap.size(); ++var3) {
               Recipe var4 = (Recipe)var2.RecipeMap.get(var3);
               this.recipesTempList.add(var4);
            }
         }
      }

      return this.recipesTempList;
   }

   public Stack<EvolvedRecipe> getAllEvolvedRecipes() {
      this.evolvedRecipesTempList.clear();

      for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
         ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
         if (!var2.disabled) {
            for(int var3 = 0; var3 < var2.EvolvedRecipeMap.size(); ++var3) {
               EvolvedRecipe var4 = (EvolvedRecipe)var2.EvolvedRecipeMap.get(var3);
               this.evolvedRecipesTempList.add(var4);
            }
         }
      }

      return this.evolvedRecipesTempList;
   }

   public Stack<UniqueRecipe> getAllUniqueRecipes() {
      this.uniqueRecipesTempList.clear();

      for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
         ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
         if (!var2.disabled) {
            Iterator var3 = var2.UniqueRecipeMap.iterator();

            while(var3 != null && var3.hasNext()) {
               UniqueRecipe var4 = (UniqueRecipe)var3.next();
               this.uniqueRecipesTempList.add(var4);
            }
         }
      }

      return this.uniqueRecipesTempList;
   }

   public ArrayList<GameSoundScript> getAllGameSounds() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.ModuleList.size(); ++var2) {
         ScriptModule var3 = (ScriptModule)this.ModuleList.get(var2);
         if (!var3.disabled) {
            var1.addAll(var3.GameSoundList);
         }
      }

      return var1;
   }

   public ArrayList<RuntimeAnimationScript> getAllRuntimeAnimationScripts() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.ModuleList.size(); ++var2) {
         ScriptModule var3 = (ScriptModule)this.ModuleList.get(var2);
         if (!var3.disabled) {
            var1.addAll(var3.RuntimeAnimationScriptMap.values());
         }
      }

      return var1;
   }

   public AnimationsMesh getAnimationsMesh(String var1) {
      ScriptModule var2 = this.getModule(var1);
      if (var2 == null) {
         return null;
      } else {
         var1 = getItemName(var1);
         return (AnimationsMesh)var2.AnimationsMeshMap.get(var1);
      }
   }

   public ArrayList<AnimationsMesh> getAllAnimationsMeshes() {
      this.animationsMeshTempList.clear();

      for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
         ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
         if (!var2.disabled) {
            this.animationsMeshTempList.addAll(var2.AnimationsMeshMap.values());
         }
      }

      return this.animationsMeshTempList;
   }

   public MannequinScript getMannequinScript(String var1) {
      ScriptModule var2 = this.getModule(var1);
      if (var2 == null) {
         return null;
      } else {
         var1 = getItemName(var1);
         return (MannequinScript)var2.MannequinScriptMap.get(var1);
      }
   }

   public ArrayList<MannequinScript> getAllMannequinScripts() {
      this.mannequinScriptTempList.clear();

      for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
         ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
         if (!var2.disabled) {
            this.mannequinScriptTempList.addAll(var2.MannequinScriptMap.values());
         }
      }

      this.mannequinScriptTempList.sort((var0, var1x) -> {
         return String.CASE_INSENSITIVE_ORDER.compare(var0.getName(), var1x.getName());
      });
      return this.mannequinScriptTempList;
   }

   public ModelScript getModelScript(String var1) {
      ScriptModule var2 = this.getModule(var1);
      if (var2 == null) {
         return null;
      } else {
         var1 = getItemName(var1);
         return (ModelScript)var2.ModelScriptMap.get(var1);
      }
   }

   public ArrayList<ModelScript> getAllModelScripts() {
      this.modelScriptTempList.clear();

      for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
         ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
         if (!var2.disabled) {
            this.modelScriptTempList.addAll(var2.ModelScriptMap.values());
         }
      }

      return this.modelScriptTempList;
   }

   public ArrayList<VehicleScript> getAllVehicleScripts() {
      this.vehicleScriptTempList.clear();

      for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
         ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
         if (!var2.disabled) {
            this.vehicleScriptTempList.addAll(var2.VehicleMap.values());
         }
      }

      return this.vehicleScriptTempList;
   }

   public SoundTimelineScript getSoundTimeline(String var1) {
      if (this.SoundTimelineMap.isEmpty()) {
         for(int var2 = 0; var2 < this.ModuleList.size(); ++var2) {
            ScriptModule var3 = (ScriptModule)this.ModuleList.get(var2);
            if (!var3.disabled) {
               this.SoundTimelineMap.putAll(var3.SoundTimelineMap);
            }
         }
      }

      return (SoundTimelineScript)this.SoundTimelineMap.get(var1);
   }

   public void Reset() {
      Iterator var1 = this.ModuleList.iterator();

      while(var1.hasNext()) {
         ScriptModule var2 = (ScriptModule)var1.next();
         var2.Reset();
      }

      this.ModuleMap.clear();
      this.ModuleList.clear();
      this.ModuleAliases.clear();
      this.CachedModules.clear();
      this.FullTypeToItemMap.clear();
      this.itemTempList.clear();
      this.tagToItemMap.clear();
      this.typeToItemMap.clear();
      this.clothingToItemMap.clear();
      this.scriptsWithVehicles.clear();
      this.scriptsWithVehicleTemplates.clear();
      this.SoundTimelineMap.clear();
   }

   public String getChecksum() {
      return this.checksum;
   }

   public static String getCurrentLoadFileMod() {
      return currentLoadFileMod;
   }

   public static String getCurrentLoadFileAbsPath() {
      return currentLoadFileAbsPath;
   }

   public void Load() {
      try {
         WorldDictionary.StartScriptLoading();
         this.tempFileToModMap = new HashMap();
         ArrayList var1 = new ArrayList();
         this.searchFolders(ZomboidFileSystem.instance.baseURI, ZomboidFileSystem.instance.getMediaFile("scripts"), var1);
         Iterator var2 = var1.iterator();

         while(var2.hasNext()) {
            String var3 = (String)var2.next();
            this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(var3), "pz-vanilla");
         }

         ArrayList var15 = new ArrayList();
         ArrayList var17 = ZomboidFileSystem.instance.getModIDs();

         for(int var4 = 0; var4 < var17.size(); ++var4) {
            String var5 = ZomboidFileSystem.instance.getModDir((String)var17.get(var4));
            if (var5 != null) {
               File var6 = new File(var5);
               URI var7 = var6.toURI();
               int var8 = var15.size();
               File var9 = ZomboidFileSystem.instance.getCanonicalFile(var6, "media");
               File var10 = ZomboidFileSystem.instance.getCanonicalFile(var9, "scripts");
               this.searchFolders(var7, var10, var15);
               if (((String)var17.get(var4)).equals("pz-vanilla")) {
                  throw new RuntimeException("Warning mod id is named pz-vanilla!");
               }

               for(int var11 = var8; var11 < var15.size(); ++var11) {
                  String var12 = (String)var15.get(var11);
                  this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(var12), (String)var17.get(var4));
               }
            }
         }

         Comparator var19 = new Comparator<String>() {
            public int compare(String var1, String var2) {
               String var3 = (new File(var1)).getName();
               String var4 = (new File(var2)).getName();
               if (var3.startsWith("template_") && !var4.startsWith("template_")) {
                  return -1;
               } else {
                  return !var3.startsWith("template_") && var4.startsWith("template_") ? 1 : var1.compareTo(var2);
               }
            }
         };
         Collections.sort(var1, var19);
         Collections.sort(var15, var19);
         var1.addAll(var15);
         if (GameClient.bClient || GameServer.bServer) {
            NetChecksum.checksummer.reset(true);
            NetChecksum.GroupOfFiles.initChecksum();
         }

         MultiStageBuilding.stages.clear();
         HashSet var21 = new HashSet();
         Iterator var22 = var1.iterator();

         label76:
         while(true) {
            String var23;
            String var24;
            do {
               do {
                  if (!var22.hasNext()) {
                     if (GameClient.bClient || GameServer.bServer) {
                        this.checksum = NetChecksum.checksummer.checksumToString();
                        if (GameServer.bServer) {
                           DebugLog.General.println("scriptChecksum: " + this.checksum);
                        }
                     }
                     break label76;
                  }

                  var23 = (String)var22.next();
               } while(var21.contains(var23));

               var21.add(var23);
               var24 = ZomboidFileSystem.instance.getAbsolutePath(var23);
               currentLoadFileAbsPath = var24;
               currentLoadFileMod = (String)this.tempFileToModMap.get(var24);
               this.LoadFile(var23, false);
            } while(!GameClient.bClient && !GameServer.bServer);

            NetChecksum.checksummer.addFile(var23, var24);
         }
      } catch (Exception var13) {
         ExceptionLogger.logException(var13);
      }

      this.buf.setLength(0);

      for(int var14 = 0; var14 < this.ModuleList.size(); ++var14) {
         ScriptModule var16 = (ScriptModule)this.ModuleList.get(var14);
         Iterator var18 = var16.ItemMap.values().iterator();

         while(var18.hasNext()) {
            Item var20 = (Item)var18.next();
            this.FullTypeToItemMap.put(var20.getFullName(), var20);
         }
      }

      this.debugItems();
      this.resolveItemTypes();
      WorldDictionary.ScriptsLoaded();
      RecipeManager.Loaded();
      GameSounds.ScriptsLoaded();
      ModelScript.ScriptsLoaded();
      if (SoundManager.instance != null) {
         SoundManager.instance.debugScriptSounds();
      }

      Translator.debugItemEvolvedRecipeNames();
      Translator.debugItemNames();
      Translator.debugMultiStageBuildNames();
      Translator.debugRecipeNames();
      this.createClothingItemMap();
      this.createZedDmgMap();
   }

   private void debugItems() {
      ArrayList var1 = instance.getAllItems();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         Item var3 = (Item)var2.next();
         if (var3.getType() == Item.Type.Drainable && var3.getReplaceOnUse() != null) {
            DebugLog.Script.warn("%s ReplaceOnUse instead of ReplaceOnDeplete", var3.getFullName());
         }

         if (var3.getType() == Item.Type.Weapon && !var3.HitSound.equals(var3.hitFloorSound)) {
            boolean var4 = true;
         }

         if (!StringUtils.isNullOrEmpty(var3.worldStaticModel)) {
            ModelScript var6 = this.getModelScript(var3.worldStaticModel);
            if (var6 != null && var6.getAttachmentById("world") != null) {
               boolean var5 = true;
            }
         }
      }

   }

   public ArrayList<Recipe> getAllRecipesFor(String var1) {
      ArrayList var2 = this.getAllRecipes();
      ArrayList var3 = new ArrayList();

      for(int var4 = 0; var4 < var2.size(); ++var4) {
         String var5 = ((Recipe)var2.get(var4)).Result.type;
         if (var5.contains(".")) {
            var5 = var5.substring(var5.indexOf(".") + 1);
         }

         if (var5.equals(var1)) {
            var3.add((Recipe)var2.get(var4));
         }
      }

      return var3;
   }

   public String getItemTypeForClothingItem(String var1) {
      return (String)this.clothingToItemMap.get(var1);
   }

   public Item getItemForClothingItem(String var1) {
      String var2 = this.getItemTypeForClothingItem(var1);
      return var2 == null ? null : this.FindItem(var2);
   }

   private void createZedDmgMap() {
      this.visualDamagesList.clear();
      ScriptModule var1 = this.getModule("Base");
      Iterator var2 = var1.ItemMap.values().iterator();

      while(var2.hasNext()) {
         Item var3 = (Item)var2.next();
         if (!StringUtils.isNullOrWhitespace(var3.getBodyLocation()) && "ZedDmg".equals(var3.getBodyLocation())) {
            this.visualDamagesList.add(var3.getName());
         }
      }

   }

   public ArrayList<String> getZedDmgMap() {
      return this.visualDamagesList;
   }

   private void createClothingItemMap() {
      Iterator var1 = this.getAllItems().iterator();

      while(var1.hasNext()) {
         Item var2 = (Item)var1.next();
         if (!StringUtils.isNullOrWhitespace(var2.getClothingItem())) {
            if (DebugLog.isEnabled(DebugType.Script)) {
               DebugLog.Script.debugln("ClothingItem \"%s\" <---> Item \"%s\"", var2.getClothingItem(), var2.getFullName());
            }

            this.clothingToItemMap.put(var2.getClothingItem(), var2.getFullName());
         }
      }

   }

   private void resolveItemTypes() {
      Iterator var1 = this.getAllItems().iterator();

      while(var1.hasNext()) {
         Item var2 = (Item)var1.next();
         var2.resolveItemTypes();
      }

   }

   public String resolveItemType(ScriptModule var1, String var2) {
      if (StringUtils.isNullOrWhitespace(var2)) {
         return null;
      } else if (var2.contains(".")) {
         return var2;
      } else {
         Item var3 = var1.getItem(var2);
         if (var3 != null) {
            return var3.getFullName();
         } else {
            for(int var4 = 0; var4 < this.ModuleList.size(); ++var4) {
               ScriptModule var5 = (ScriptModule)this.ModuleList.get(var4);
               if (!var5.disabled) {
                  var3 = var5.getItem(var2);
                  if (var3 != null) {
                     return var3.getFullName();
                  }
               }
            }

            return "???." + var2;
         }
      }
   }

   public String resolveModelScript(ScriptModule var1, String var2) {
      if (StringUtils.isNullOrWhitespace(var2)) {
         return null;
      } else if (var2.contains(".")) {
         return var2;
      } else {
         ModelScript var3 = var1.getModelScript(var2);
         if (var3 != null) {
            return var3.getFullType();
         } else {
            for(int var4 = 0; var4 < this.ModuleList.size(); ++var4) {
               ScriptModule var5 = (ScriptModule)this.ModuleList.get(var4);
               if (var5 != var1 && !var5.disabled) {
                  var3 = var5.getModelScript(var2);
                  if (var3 != null) {
                     return var3.getFullType();
                  }
               }
            }

            return "???." + var2;
         }
      }
   }

   public Item getSpecificItem(String var1) {
      if (!var1.contains(".")) {
         DebugLog.log("ScriptManager.getSpecificItem requires a full type name, cannot find: " + var1);
         if (Core.bDebug) {
            throw new RuntimeException("ScriptManager.getSpecificItem requires a full type name, cannot find: " + var1);
         } else {
            return null;
         }
      } else if (this.FullTypeToItemMap.containsKey(var1)) {
         return (Item)this.FullTypeToItemMap.get(var1);
      } else {
         int var2 = var1.indexOf(".");
         String var3 = var1.substring(0, var2);
         String var4 = var1.substring(var2 + 1);
         ScriptModule var5 = this.getModule(var3, false);
         return var5 == null ? null : var5.getSpecificItem(var4);
      }
   }
}
