package zombie.scripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import zombie.GameSounds;
import zombie.SoundManager;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.IndieFileLoader;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.entity.energy.Energy;
import zombie.gameStates.ChooseGameInfo;
import zombie.inventory.ItemTags;
import zombie.inventory.RecipeManager;
import zombie.iso.IsoWorld;
import zombie.iso.MultiStageBuilding;
import zombie.iso.SpriteModel;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetChecksum;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.GameEntityTemplate;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.EnergyDefinitionScript;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Fixing;
import zombie.scripting.objects.FluidDefinitionScript;
import zombie.scripting.objects.FluidFilterScript;
import zombie.scripting.objects.GameSoundScript;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemFilterScript;
import zombie.scripting.objects.MannequinScript;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.RagdollScript;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ScriptModule;
import zombie.scripting.objects.SoundTimelineScript;
import zombie.scripting.objects.StringListScript;
import zombie.scripting.objects.TimedActionScript;
import zombie.scripting.objects.UniqueRecipe;
import zombie.scripting.objects.VehicleScript;
import zombie.scripting.objects.VehicleTemplate;
import zombie.scripting.objects.XuiColorsScript;
import zombie.scripting.objects.XuiConfigScript;
import zombie.scripting.objects.XuiLayoutScript;
import zombie.scripting.objects.XuiSkinScript;
import zombie.scripting.ui.XuiManager;
import zombie.util.StringUtils;
import zombie.vehicles.VehicleEngineRPM;
import zombie.world.WorldDictionary;

public final class ScriptManager implements IScriptObjectStore {
   public static final ScriptManager instance = new ScriptManager();
   private static final EnumSet<ScriptType> debugTypes = EnumSet.noneOf(ScriptType.class);
   public String currentFileName;
   private final ArrayList<String> loadFileNames = new ArrayList();
   public final HashMap<String, ScriptModule> ModuleMap = new HashMap();
   public final ArrayList<ScriptModule> ModuleList = new ArrayList();
   public ScriptModule CurrentLoadingModule = null;
   private final HashMap<String, String> ModuleAliases = new HashMap();
   private final StringBuilder buf = new StringBuilder();
   private final HashMap<String, ScriptModule> CachedModules = new HashMap();
   private final HashMap<String, ArrayList<Item>> tagToItemMap = new HashMap();
   private final HashMap<String, ArrayList<Item>> typeToItemMap = new HashMap();
   private final HashMap<String, String> clothingToItemMap = new HashMap();
   private final ArrayList<String> visualDamagesList = new ArrayList();
   private final ArrayList<ScriptBucketCollection<?>> bucketCollectionList = new ArrayList();
   private final HashMap<ScriptType, ScriptBucketCollection<?>> bucketCollectionMap = new HashMap();
   private boolean hasLoadErrors = false;
   private final ScriptBucketCollection<VehicleTemplate> vehicleTemplates;
   private final ScriptBucketCollection<GameEntityTemplate> entityTemplates;
   private final ScriptBucketCollection<Item> items;
   private final ScriptBucketCollection<Recipe> recipes;
   private final ScriptBucketCollection<UniqueRecipe> uniqueRecipes;
   private final Stack<UniqueRecipe> uniqueRecipeTempStack;
   private final ScriptBucketCollection<EvolvedRecipe> evolvedRecipes;
   private final Stack<EvolvedRecipe> evolvedRecipeTempStack;
   private final ScriptBucketCollection<Fixing> fixings;
   private final ScriptBucketCollection<AnimationsMesh> animationMeshes;
   private final ScriptBucketCollection<MannequinScript> mannequins;
   private final ScriptBucketCollection<ModelScript> models;
   private final ScriptBucketCollection<PhysicsShapeScript> physicsShapes;
   private final ScriptBucketCollection<GameSoundScript> gameSounds;
   private final ScriptBucketCollection<SoundTimelineScript> soundTimelines;
   private final ScriptBucketCollection<SpriteModel> spriteModels;
   private final ScriptBucketCollection<VehicleScript> vehicles;
   private final ScriptBucketCollection<RuntimeAnimationScript> animations;
   private final ScriptBucketCollection<VehicleEngineRPM> vehicleEngineRPMs;
   private final ScriptBucketCollection<ItemConfig> itemConfigs;
   private final ScriptBucketCollection<GameEntityScript> entities;
   private final ScriptBucketCollection<XuiConfigScript> xuiConfigScripts;
   private final ScriptBucketCollection<XuiLayoutScript> xuiLayouts;
   private final ScriptBucketCollection<XuiLayoutScript> xuiStyles;
   private final ScriptBucketCollection<XuiLayoutScript> xuiDefaultStyles;
   private final ScriptBucketCollection<XuiColorsScript> xuiGlobalColors;
   private final ScriptBucketCollection<XuiSkinScript> xuiSkinScripts;
   private final ScriptBucketCollection<ItemFilterScript> itemFilters;
   private final ScriptBucketCollection<FluidFilterScript> fluidFilters;
   private final ScriptBucketCollection<CraftRecipe> craftRecipes;
   private final ScriptBucketCollection<StringListScript> stringLists;
   private final ScriptBucketCollection<EnergyDefinitionScript> energyDefinitionScripts;
   private final ScriptBucketCollection<FluidDefinitionScript> fluidDefinitionScripts;
   private final ScriptBucketCollection<TimedActionScript> timedActionScripts;
   private final ScriptBucketCollection<RagdollScript> ragdollScripts;
   public static final String Base = "Base";
   public static final String Base_Module = "Base.";
   private String checksum;
   private HashMap<String, String> tempFileToModMap;
   private static String currentLoadFileMod;
   private static String currentLoadFileAbsPath;
   private static String currentLoadFileName;
   public static final String VanillaID = "pz-vanilla";

   public static void EnableDebug(ScriptType var0, boolean var1) {
      if (var1) {
         debugTypes.add(var0);
      } else {
         debugTypes.remove(var0);
      }

   }

   public static boolean isDebugEnabled(ScriptType var0) {
      return debugTypes.contains(var0);
   }

   public static void println(ScriptType var0, String var1) {
      if (debugTypes.contains(var0)) {
         DebugLogStream var10000 = DebugLog.Script;
         String var10001 = var0.toString();
         var10000.println("[" + var10001 + "] " + var1);
      }

   }

   public static void println(BaseScriptObject var0, String var1) {
      println(var0.getScriptObjectType(), var1);
   }

   private <T extends BaseScriptObject> ScriptBucketCollection<T> addBucketCollection(ScriptBucketCollection<T> var1) {
      if (this.bucketCollectionMap.containsKey(var1.getScriptType())) {
         throw new RuntimeException("ScriptType collection already added.");
      } else {
         this.bucketCollectionMap.put(var1.getScriptType(), var1);
         this.bucketCollectionList.add(var1);
         return var1;
      }
   }

   public ArrayList<?> getScriptsForType(ScriptType var1) {
      if (this.bucketCollectionMap.containsKey(var1)) {
         return ((ScriptBucketCollection)this.bucketCollectionMap.get(var1)).getAllScripts();
      } else {
         DebugLog.General.warn("Type has no bucket collection: " + var1);
         return new ArrayList();
      }
   }

   public VehicleTemplate getVehicleTemplate(String var1) {
      return (VehicleTemplate)this.vehicleTemplates.getScript(var1);
   }

   public ArrayList<VehicleTemplate> getAllVehicleTemplates() {
      return this.vehicleTemplates.getAllScripts();
   }

   public GameEntityTemplate getGameEntityTemplate(String var1) {
      return (GameEntityTemplate)this.entityTemplates.getScript(var1);
   }

   public ArrayList<GameEntityTemplate> getAllGameEntityTemplates() {
      return this.entityTemplates.getAllScripts();
   }

   public Item getItem(String var1) {
      return (Item)this.items.getScript(var1);
   }

   public ArrayList<Item> getAllItems() {
      return this.items.getAllScripts();
   }

   public Recipe getRecipe(String var1) {
      return (Recipe)this.recipes.getScript(var1);
   }

   public ArrayList<Recipe> getAllRecipes() {
      return this.recipes.getAllScripts();
   }

   public UniqueRecipe getUniqueRecipe(String var1) {
      return (UniqueRecipe)this.uniqueRecipes.getScript(var1);
   }

   public Stack<UniqueRecipe> getAllUniqueRecipes() {
      this.uniqueRecipeTempStack.clear();
      this.uniqueRecipeTempStack.addAll(this.uniqueRecipes.getAllScripts());
      return this.uniqueRecipeTempStack;
   }

   public EvolvedRecipe getEvolvedRecipe(String var1) {
      return (EvolvedRecipe)this.evolvedRecipes.getScript(var1);
   }

   public ArrayList<EvolvedRecipe> getAllEvolvedRecipesList() {
      return this.evolvedRecipes.getAllScripts();
   }

   public Stack<EvolvedRecipe> getAllEvolvedRecipes() {
      this.evolvedRecipeTempStack.clear();
      this.evolvedRecipeTempStack.addAll(this.evolvedRecipes.getAllScripts());
      return this.evolvedRecipeTempStack;
   }

   public Fixing getFixing(String var1) {
      return (Fixing)this.fixings.getScript(var1);
   }

   public ArrayList<Fixing> getAllFixing(ArrayList<Fixing> var1) {
      var1.addAll(this.fixings.getAllScripts());
      return var1;
   }

   public AnimationsMesh getAnimationsMesh(String var1) {
      return (AnimationsMesh)this.animationMeshes.getScript(var1);
   }

   public ArrayList<AnimationsMesh> getAllAnimationsMeshes() {
      return this.animationMeshes.getAllScripts();
   }

   public MannequinScript getMannequinScript(String var1) {
      return (MannequinScript)this.mannequins.getScript(var1);
   }

   public ArrayList<MannequinScript> getAllMannequinScripts() {
      return this.mannequins.getAllScripts();
   }

   public ModelScript getModelScript(String var1) {
      return (ModelScript)this.models.getScript(var1);
   }

   public ArrayList<ModelScript> getAllModelScripts() {
      return this.models.getAllScripts();
   }

   public void addModelScript(ModelScript var1) {
      ScriptModule var2 = var1.getModule();
      var2.models.scriptList.add(var1);
      var2.models.scriptMap.put(var1.getScriptObjectName(), var1);
      if (var1.getScriptObjectName().contains(".")) {
         var2.models.dotInName.add(var1.getScriptObjectName());
      }

      this.models.getAllScripts().clear();
      this.models.getFullTypeToScriptMap().put(var1.getScriptObjectFullType(), var1);
   }

   public PhysicsShapeScript getPhysicsShape(String var1) {
      return (PhysicsShapeScript)this.physicsShapes.getScript(var1);
   }

   public ArrayList<PhysicsShapeScript> getAllPhysicsShapes() {
      return this.physicsShapes.getAllScripts();
   }

   public GameSoundScript getGameSound(String var1) {
      return (GameSoundScript)this.gameSounds.getScript(var1);
   }

   public ArrayList<GameSoundScript> getAllGameSounds() {
      return new ArrayList(this.gameSounds.getAllScripts());
   }

   public SoundTimelineScript getSoundTimeline(String var1) {
      return (SoundTimelineScript)this.soundTimelines.getScript(var1);
   }

   public ArrayList<SoundTimelineScript> getAllSoundTimelines() {
      return this.soundTimelines.getAllScripts();
   }

   public SpriteModel getSpriteModel(String var1) {
      return (SpriteModel)this.spriteModels.getScript(var1);
   }

   public ArrayList<SpriteModel> getAllSpriteModels() {
      return this.spriteModels.getAllScripts();
   }

   public void addSpriteModel(SpriteModel var1) {
      ScriptModule var2 = var1.getModule();
      var2.spriteModels.getScriptList().add(var1);
      var2.spriteModels.getScriptMap().put(var1.getScriptObjectName(), var1);
      if (var1.getScriptObjectName().contains(".")) {
         var2.spriteModels.dotInName.add(var1.getScriptObjectName());
      }

      this.spriteModels.getFullTypeToScriptMap().put(var1.getScriptObjectFullType(), var1);
   }

   public VehicleScript getVehicle(String var1) {
      return (VehicleScript)this.vehicles.getScript(var1);
   }

   public ArrayList<VehicleScript> getAllVehicleScripts() {
      return this.vehicles.getAllScripts();
   }

   public RuntimeAnimationScript getRuntimeAnimationScript(String var1) {
      return (RuntimeAnimationScript)this.animations.getScript(var1);
   }

   public ArrayList<RuntimeAnimationScript> getAllRuntimeAnimationScripts() {
      return new ArrayList(this.animations.getAllScripts());
   }

   public VehicleEngineRPM getVehicleEngineRPM(String var1) {
      return (VehicleEngineRPM)this.vehicleEngineRPMs.getScript(var1);
   }

   public ArrayList<VehicleEngineRPM> getAllVehicleEngineRPMs() {
      return this.vehicleEngineRPMs.getAllScripts();
   }

   public ItemConfig getItemConfig(String var1) {
      return (ItemConfig)this.itemConfigs.getScript(var1);
   }

   public ArrayList<ItemConfig> getAllItemConfigs() {
      return this.itemConfigs.getAllScripts();
   }

   public GameEntityScript getGameEntityScript(String var1) {
      return (GameEntityScript)this.entities.getScript(var1);
   }

   public ArrayList<GameEntityScript> getAllGameEntities() {
      return this.entities.getAllScripts();
   }

   public XuiConfigScript getXuiConfigScript(String var1) {
      return (XuiConfigScript)this.xuiConfigScripts.getScript(var1);
   }

   public ArrayList<XuiConfigScript> getAllXuiConfigScripts() {
      return this.xuiConfigScripts.getAllScripts();
   }

   public XuiLayoutScript getXuiLayout(String var1) {
      return (XuiLayoutScript)this.xuiLayouts.getScript(var1);
   }

   public ArrayList<XuiLayoutScript> getAllXuiLayouts() {
      return this.xuiLayouts.getAllScripts();
   }

   public XuiLayoutScript getXuiStyle(String var1) {
      return (XuiLayoutScript)this.xuiStyles.getScript(var1);
   }

   public ArrayList<XuiLayoutScript> getAllXuiStyles() {
      return this.xuiStyles.getAllScripts();
   }

   public XuiLayoutScript getXuiDefaultStyle(String var1) {
      return (XuiLayoutScript)this.xuiDefaultStyles.getScript(var1);
   }

   public ArrayList<XuiLayoutScript> getAllXuiDefaultStyles() {
      return this.xuiDefaultStyles.getAllScripts();
   }

   public XuiColorsScript getXuiColor(String var1) {
      return (XuiColorsScript)this.xuiGlobalColors.getScript(var1);
   }

   public ArrayList<XuiColorsScript> getAllXuiColors() {
      return this.xuiGlobalColors.getAllScripts();
   }

   public XuiSkinScript getXuiSkinScript(String var1) {
      return (XuiSkinScript)this.xuiSkinScripts.getScript(var1);
   }

   public ArrayList<XuiSkinScript> getAllXuiSkinScripts() {
      return this.xuiSkinScripts.getAllScripts();
   }

   public ItemFilterScript getItemFilter(String var1) {
      return (ItemFilterScript)this.itemFilters.getScript(var1);
   }

   public ArrayList<ItemFilterScript> getAllItemFilters() {
      return this.itemFilters.getAllScripts();
   }

   public FluidFilterScript getFluidFilter(String var1) {
      return (FluidFilterScript)this.fluidFilters.getScript(var1);
   }

   public ArrayList<FluidFilterScript> getAllFluidFilters() {
      return this.fluidFilters.getAllScripts();
   }

   public CraftRecipe getCraftRecipe(String var1) {
      return (CraftRecipe)this.craftRecipes.getScript(var1);
   }

   public ArrayList<CraftRecipe> getAllCraftRecipes() {
      return this.craftRecipes.getAllScripts();
   }

   public void checkAutoLearn(IsoGameCharacter var1) {
      ArrayList var2 = this.craftRecipes.getAllScripts();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         CraftRecipe var4 = (CraftRecipe)var2.get(var3);
         if (!var1.isRecipeActuallyKnown(var4) && var4.getAutoLearnSkillCount() > 0) {
            var4.checkAutoLearnSkills(var1);
         }
      }

   }

   public void checkMetaRecipes(IsoGameCharacter var1) {
      ArrayList var2 = this.craftRecipes.getAllScripts();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         CraftRecipe var4 = (CraftRecipe)var2.get(var3);
         if (!var1.isRecipeActuallyKnown(var4) && var4.getMetaRecipe() != null) {
            var4.checkMetaRecipe(var1);
         }
      }

   }

   public void checkMetaRecipe(IsoGameCharacter var1, String var2) {
      ArrayList var3 = this.craftRecipes.getAllScripts();

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         CraftRecipe var5 = (CraftRecipe)var3.get(var4);
         if (!var1.isRecipeActuallyKnown(var5) && var5.getMetaRecipe() != null && Objects.equals(var5.getMetaRecipe(), var2)) {
            var5.checkMetaRecipe(var1, var2);
         }
      }

   }

   public StringListScript getStringList(String var1) {
      return (StringListScript)this.stringLists.getScript(var1);
   }

   public ArrayList<StringListScript> getAllStringLists() {
      return this.stringLists.getAllScripts();
   }

   public EnergyDefinitionScript getEnergyDefinitionScript(String var1) {
      return (EnergyDefinitionScript)this.energyDefinitionScripts.getScript(var1);
   }

   public ArrayList<EnergyDefinitionScript> getAllEnergyDefinitionScripts() {
      return this.energyDefinitionScripts.getAllScripts();
   }

   public FluidDefinitionScript getFluidDefinitionScript(String var1) {
      return (FluidDefinitionScript)this.fluidDefinitionScripts.getScript(var1);
   }

   public ArrayList<FluidDefinitionScript> getAllFluidDefinitionScripts() {
      return this.fluidDefinitionScripts.getAllScripts();
   }

   public TimedActionScript getTimedActionScript(String var1) {
      return (TimedActionScript)this.timedActionScripts.getScript(var1);
   }

   public ArrayList<TimedActionScript> getAllTimedActionScripts() {
      return this.timedActionScripts.getAllScripts();
   }

   public RagdollScript getRagdollScript(String var1) {
      return (RagdollScript)this.ragdollScripts.getScript(var1);
   }

   public ScriptManager() {
      this.vehicleTemplates = this.addBucketCollection(new ScriptBucketCollection<VehicleTemplate>(this, ScriptType.VehicleTemplate) {
         public ScriptBucket<VehicleTemplate> getBucketFromModule(ScriptModule var1) {
            return var1.vehicleTemplates;
         }
      });
      this.entityTemplates = this.addBucketCollection(new ScriptBucketCollection<GameEntityTemplate>(this, ScriptType.EntityTemplate) {
         public ScriptBucket<GameEntityTemplate> getBucketFromModule(ScriptModule var1) {
            return var1.entityTemplates;
         }
      });
      this.items = this.addBucketCollection(new ScriptBucketCollection<Item>(this, ScriptType.Item) {
         public ScriptBucket<Item> getBucketFromModule(ScriptModule var1) {
            return var1.items;
         }

         public void LoadScripts(ScriptLoadMode var1) {
            super.LoadScripts(var1);
            ItemTags.Init(ScriptManager.this.getAllItems());
         }
      });
      this.recipes = this.addBucketCollection(new ScriptBucketCollection<Recipe>(this, ScriptType.Recipe) {
         public ScriptBucket<Recipe> getBucketFromModule(ScriptModule var1) {
            return var1.recipes;
         }

         public void OnLoadedAfterLua() throws Exception {
            super.OnLoadedAfterLua();
            RecipeManager.LoadedAfterLua();
         }
      });
      this.uniqueRecipes = this.addBucketCollection(new ScriptBucketCollection<UniqueRecipe>(this, ScriptType.UniqueRecipe) {
         public ScriptBucket<UniqueRecipe> getBucketFromModule(ScriptModule var1) {
            return var1.uniqueRecipes;
         }
      });
      this.uniqueRecipeTempStack = new Stack();
      this.evolvedRecipes = this.addBucketCollection(new ScriptBucketCollection<EvolvedRecipe>(this, ScriptType.EvolvedRecipe) {
         public ScriptBucket<EvolvedRecipe> getBucketFromModule(ScriptModule var1) {
            return var1.evolvedRecipes;
         }
      });
      this.evolvedRecipeTempStack = new Stack();
      this.fixings = this.addBucketCollection(new ScriptBucketCollection<Fixing>(this, ScriptType.Fixing) {
         public ScriptBucket<Fixing> getBucketFromModule(ScriptModule var1) {
            return var1.fixings;
         }
      });
      this.animationMeshes = this.addBucketCollection(new ScriptBucketCollection<AnimationsMesh>(this, ScriptType.AnimationMesh) {
         public ScriptBucket<AnimationsMesh> getBucketFromModule(ScriptModule var1) {
            return var1.animationMeshes;
         }
      });
      this.mannequins = this.addBucketCollection(new ScriptBucketCollection<MannequinScript>(this, ScriptType.Mannequin) {
         public ScriptBucket<MannequinScript> getBucketFromModule(ScriptModule var1) {
            return var1.mannequins;
         }

         public void onSortAllScripts(ArrayList<MannequinScript> var1) {
            var1.sort((var0, var1x) -> {
               return String.CASE_INSENSITIVE_ORDER.compare(var0.getName(), var1x.getName());
            });
         }
      });
      this.models = this.addBucketCollection(new ScriptBucketCollection<ModelScript>(this, ScriptType.Model) {
         public ScriptBucket<ModelScript> getBucketFromModule(ScriptModule var1) {
            return var1.models;
         }
      });
      this.physicsShapes = this.addBucketCollection(new ScriptBucketCollection<PhysicsShapeScript>(this, ScriptType.PhysicsShape) {
         public ScriptBucket<PhysicsShapeScript> getBucketFromModule(ScriptModule var1) {
            return var1.physicsShapes;
         }
      });
      this.gameSounds = this.addBucketCollection(new ScriptBucketCollection<GameSoundScript>(this, ScriptType.Sound) {
         public ScriptBucket<GameSoundScript> getBucketFromModule(ScriptModule var1) {
            return var1.gameSounds;
         }
      });
      this.soundTimelines = this.addBucketCollection(new ScriptBucketCollection<SoundTimelineScript>(this, ScriptType.SoundTimeline) {
         public ScriptBucket<SoundTimelineScript> getBucketFromModule(ScriptModule var1) {
            return var1.soundTimelines;
         }
      });
      this.spriteModels = this.addBucketCollection(new ScriptBucketCollection<SpriteModel>(this, ScriptType.SpriteModel) {
         public ScriptBucket<SpriteModel> getBucketFromModule(ScriptModule var1) {
            return var1.spriteModels;
         }
      });
      this.vehicles = this.addBucketCollection(new ScriptBucketCollection<VehicleScript>(this, ScriptType.Vehicle) {
         public ScriptBucket<VehicleScript> getBucketFromModule(ScriptModule var1) {
            return var1.vehicles;
         }
      });
      this.animations = this.addBucketCollection(new ScriptBucketCollection<RuntimeAnimationScript>(this, ScriptType.RuntimeAnimation) {
         public ScriptBucket<RuntimeAnimationScript> getBucketFromModule(ScriptModule var1) {
            return var1.animations;
         }
      });
      this.vehicleEngineRPMs = this.addBucketCollection(new ScriptBucketCollection<VehicleEngineRPM>(this, ScriptType.VehicleEngineRPM) {
         public ScriptBucket<VehicleEngineRPM> getBucketFromModule(ScriptModule var1) {
            return var1.vehicleEngineRPMs;
         }
      });
      this.itemConfigs = this.addBucketCollection(new ScriptBucketCollection<ItemConfig>(this, ScriptType.ItemConfig) {
         public ScriptBucket<ItemConfig> getBucketFromModule(ScriptModule var1) {
            return var1.itemConfigs;
         }
      });
      this.entities = this.addBucketCollection(new ScriptBucketCollection<GameEntityScript>(this, ScriptType.Entity) {
         public ScriptBucket<GameEntityScript> getBucketFromModule(ScriptModule var1) {
            return var1.entities;
         }

         public void OnPostTileDefinitions() throws Exception {
            super.OnPostTileDefinitions();
            SpriteConfigManager.InitScriptsPostTileDef();
         }
      });
      this.xuiConfigScripts = this.addBucketCollection(new ScriptBucketCollection<XuiConfigScript>(this, ScriptType.XuiConfig) {
         public ScriptBucket<XuiConfigScript> getBucketFromModule(ScriptModule var1) {
            return var1.xuiConfigScripts;
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            XuiManager.ParseScripts();
         }
      });
      this.xuiLayouts = this.addBucketCollection(new ScriptBucketCollection<XuiLayoutScript>(this, ScriptType.XuiLayout) {
         public ScriptBucket<XuiLayoutScript> getBucketFromModule(ScriptModule var1) {
            return var1.xuiLayouts;
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            XuiManager.ParseScripts();
         }
      });
      this.xuiStyles = this.addBucketCollection(new ScriptBucketCollection<XuiLayoutScript>(this, ScriptType.XuiStyle) {
         public ScriptBucket<XuiLayoutScript> getBucketFromModule(ScriptModule var1) {
            return var1.xuiStyles;
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            XuiManager.ParseScripts();
         }
      });
      this.xuiDefaultStyles = this.addBucketCollection(new ScriptBucketCollection<XuiLayoutScript>(this, ScriptType.XuiDefaultStyle) {
         public ScriptBucket<XuiLayoutScript> getBucketFromModule(ScriptModule var1) {
            return var1.xuiDefaultStyles;
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            XuiManager.ParseScripts();
         }
      });
      this.xuiGlobalColors = this.addBucketCollection(new ScriptBucketCollection<XuiColorsScript>(this, ScriptType.XuiColor) {
         public ScriptBucket<XuiColorsScript> getBucketFromModule(ScriptModule var1) {
            return var1.xuiGlobalColors;
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            XuiManager.ParseScripts();
         }
      });
      this.xuiSkinScripts = this.addBucketCollection(new ScriptBucketCollection<XuiSkinScript>(this, ScriptType.XuiSkin) {
         public ScriptBucket<XuiSkinScript> getBucketFromModule(ScriptModule var1) {
            return var1.xuiSkinScripts;
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            XuiManager.ParseScripts();
         }
      });
      this.itemFilters = this.addBucketCollection(new ScriptBucketCollection<ItemFilterScript>(this, ScriptType.ItemFilter) {
         public ScriptBucket<ItemFilterScript> getBucketFromModule(ScriptModule var1) {
            return var1.itemFilters;
         }

         public void OnPostWorldDictionaryInit() throws Exception {
            super.OnPostWorldDictionaryInit();
         }
      });
      this.fluidFilters = this.addBucketCollection(new ScriptBucketCollection<FluidFilterScript>(this, ScriptType.FluidFilter) {
         public ScriptBucket<FluidFilterScript> getBucketFromModule(ScriptModule var1) {
            return var1.fluidFilters;
         }

         public void OnPostWorldDictionaryInit() throws Exception {
            super.OnPostWorldDictionaryInit();
         }
      });
      this.craftRecipes = this.addBucketCollection(new ScriptBucketCollection<CraftRecipe>(this, ScriptType.CraftRecipe) {
         public ScriptBucket<CraftRecipe> getBucketFromModule(ScriptModule var1) {
            return var1.craftRecipes;
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            CraftRecipeManager.Init();
         }

         public void OnLoadedAfterLua() throws Exception {
            super.OnLoadedAfterLua();
         }
      });
      this.stringLists = this.addBucketCollection(new ScriptBucketCollection<StringListScript>(this, ScriptType.StringList) {
         public ScriptBucket<StringListScript> getBucketFromModule(ScriptModule var1) {
            return var1.stringLists;
         }
      });
      this.energyDefinitionScripts = this.addBucketCollection(new ScriptBucketCollection<EnergyDefinitionScript>(this, ScriptType.EnergyDefinition) {
         public ScriptBucket<EnergyDefinitionScript> getBucketFromModule(ScriptModule var1) {
            return var1.energyDefinitionScripts;
         }

         public void PreReloadScripts() throws Exception {
            Energy.PreReloadScripts();
            super.PreReloadScripts();
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            Energy.Init(var1);
         }
      });
      this.fluidDefinitionScripts = this.addBucketCollection(new ScriptBucketCollection<FluidDefinitionScript>(this, ScriptType.FluidDefinition) {
         public ScriptBucket<FluidDefinitionScript> getBucketFromModule(ScriptModule var1) {
            return var1.fluidDefinitionScripts;
         }

         public void PreReloadScripts() throws Exception {
            Fluid.PreReloadScripts();
            super.PreReloadScripts();
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            Fluid.Init(var1);
         }
      });
      this.timedActionScripts = this.addBucketCollection(new ScriptBucketCollection<TimedActionScript>(this, ScriptType.TimedAction) {
         public ScriptBucket<TimedActionScript> getBucketFromModule(ScriptModule var1) {
            return var1.timedActionScripts;
         }
      });
      this.ragdollScripts = this.addBucketCollection(new ScriptBucketCollection<RagdollScript>(this, ScriptType.Ragdoll) {
         public ScriptBucket<RagdollScript> getBucketFromModule(ScriptModule var1) {
            return var1.ragdollScripts;
         }

         public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
            super.PostLoadScripts(var1);
            RagdollScript.toBullet(false);
         }
      });
      this.checksum = "";
      Collections.sort(this.bucketCollectionList, Comparator.comparing(ScriptBucketCollection::isTemplate, Comparator.reverseOrder()));
   }

   public void update() {
   }

   public void LoadFile(ScriptLoadMode var1, String var2, boolean var3) throws FileNotFoundException {
      if (DebugLog.isEnabled(DebugType.Script)) {
         DebugLog.Script.debugln(var2 + (var3 ? " bLoadJar" : ""));
      }

      if (!GameServer.bServer) {
         Thread.yield();
         Core.getInstance().DoFrameReady();
      }

      if (var2.contains(".tmx")) {
         IsoWorld.mapPath = var2.substring(0, var2.lastIndexOf("/"));
         IsoWorld.mapUseJar = var3;
         DebugLog.Script.debugln("  file is a .tmx (map) file. Set mapPath to " + IsoWorld.mapPath + (IsoWorld.mapUseJar ? " mapUseJar" : ""));
      } else if (!var2.endsWith(".txt")) {
         DebugLog.Script.warn(" file is not a .txt (script) file: " + var2);
      } else {
         InputStreamReader var4 = IndieFileLoader.getStreamReader(var2, !var3);
         BufferedReader var5 = new BufferedReader(var4);
         this.buf.setLength(0);
         String var6 = null;
         String var7 = "";

         label135: {
            try {
               while(true) {
                  if ((var6 = var5.readLine()) == null) {
                     break label135;
                  }

                  this.buf.append(var6);
                  this.buf.append('\n');
               }
            } catch (Exception var18) {
               DebugLog.Script.error("Exception thrown reading file " + var2 + "\n  " + var18);
            } finally {
               try {
                  var5.close();
                  var4.close();
               } catch (Exception var17) {
                  DebugLog.Script.error("Exception thrown closing file " + var2 + "\n  " + var17);
                  var17.printStackTrace(DebugLog.Script);
               }

            }

            return;
         }

         var7 = this.buf.toString();
         var7 = ScriptParser.stripComments(var7);
         this.currentFileName = var2;
         this.registerLoadFileName(this.currentFileName);
         this.ParseScript(var1, var7);
         this.currentFileName = null;
      }
   }

   private void registerLoadFileName(String var1) {
      if (!this.loadFileNames.contains(var1)) {
         this.loadFileNames.add(var1);
      }

   }

   public void ParseScript(ScriptLoadMode var1, String var2) {
      if (DebugLog.isEnabled(DebugType.Script)) {
         DebugLog.Script.debugln("Parsing...");
      }

      ArrayList var3 = ScriptParser.parseTokens(var2);

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         String var5 = (String)var3.get(var4);
         this.CreateFromToken(var1, var5);
      }

   }

   private void CreateFromToken(ScriptLoadMode var1, String var2) {
      var2 = var2.trim();
      if (var2.indexOf("module") == 0) {
         int var3 = var2.indexOf("{");
         int var4 = var2.lastIndexOf("}");
         String[] var5 = var2.split("[{}]");
         String var6 = var5[0];
         var6 = var6.replace("module", "");
         var6 = var6.trim();
         String var7 = var2.substring(var3 + 1, var4);
         ScriptModule var8 = (ScriptModule)this.ModuleMap.get(var6);
         if (var8 == null) {
            if (DebugLog.isEnabled(DebugType.Script)) {
               DebugLog.Script.debugln("Adding new module: " + var6);
            }

            var8 = new ScriptModule();
            this.ModuleMap.put(var6, var8);
            this.ModuleList.add(var8);
            Iterator var9 = this.bucketCollectionList.iterator();

            while(var9.hasNext()) {
               ScriptBucketCollection var10 = (ScriptBucketCollection)var9.next();
               var10.registerModule(var8);
            }
         }

         var8.Load(var1, var6, var7);
      }

   }

   public void searchFolders(URI var1, File var2, ArrayList<String> var3) {
      if (var2.isDirectory()) {
         if (var2.getAbsolutePath().contains("tempNotWorking")) {
            return;
         }

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

   public Item FindItem(String var1) {
      return this.FindItem(var1, true);
   }

   public Item FindItem(String var1, boolean var2) {
      if (var1.contains(".") && this.items.hasFullType(var1)) {
         return (Item)this.items.getFullType(var1);
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

   public void CheckExitPoints() {
      for(int var1 = 0; var1 < this.ModuleList.size(); ++var1) {
         ScriptModule var2 = (ScriptModule)this.ModuleList.get(var1);
         if (!var2.disabled && var2.CheckExitPoints()) {
            return;
         }
      }

   }

   public ArrayList<Item> getAllItemsWithTag(String var1) {
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

   public ArrayList<Item> getItemsTag(String var1) {
      return this.getAllItemsWithTag(var1);
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
                  Item var5 = (Item)this.items.getFullType(StringUtils.moduleDotType(var4.name, var1));
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

   public void Reset() {
      Iterator var1 = this.ModuleList.iterator();

      while(var1.hasNext()) {
         ScriptModule var2 = (ScriptModule)var1.next();
         var2.Reset();
      }

      var1 = this.bucketCollectionList.iterator();

      while(var1.hasNext()) {
         ScriptBucketCollection var3 = (ScriptBucketCollection)var1.next();
         var3.reset();
      }

      this.ModuleMap.clear();
      this.ModuleList.clear();
      this.ModuleAliases.clear();
      this.CachedModules.clear();
      this.tagToItemMap.clear();
      this.typeToItemMap.clear();
      this.clothingToItemMap.clear();
      this.hasLoadErrors = false;
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

   public static String getCurrentLoadFileName() {
      return currentLoadFileName;
   }

   public void Load() throws IOException {
      try {
         this.loadFileNames.clear();
         WorldDictionary.StartScriptLoading();
         this.tempFileToModMap = new HashMap();
         ArrayList var1 = new ArrayList();
         this.searchFolders(ZomboidFileSystem.instance.base.lowercaseURI, ZomboidFileSystem.instance.getMediaFile("scripts"), var1);
         Iterator var2 = var1.iterator();

         while(var2.hasNext()) {
            String var3 = (String)var2.next();
            this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(var3), "pz-vanilla");
         }

         ArrayList var16 = new ArrayList();
         ArrayList var17 = ZomboidFileSystem.instance.getModIDs();

         for(int var4 = 0; var4 < var17.size(); ++var4) {
            ChooseGameInfo.Mod var5 = ChooseGameInfo.getAvailableModDetails((String)var17.get(var4));
            if (var5 != null) {
               String var6 = var5.getCommonDir();
               File var7;
               URI var8;
               URI var9;
               int var10;
               File var11;
               File var12;
               int var13;
               String var14;
               if (var6 != null) {
                  var7 = new File(var6);
                  var8 = var7.toURI();
                  var9 = (new File(var7.getCanonicalFile().getPath().toLowerCase(Locale.ENGLISH))).toURI();
                  var10 = var16.size();
                  var11 = ZomboidFileSystem.instance.getCanonicalFile(var7, "media");
                  var12 = ZomboidFileSystem.instance.getCanonicalFile(var11, "scripts");
                  this.searchFolders(var9, var12, var16);
                  if (((String)var17.get(var4)).equals("pz-vanilla")) {
                     throw new RuntimeException("Warning mod id is named pz-vanilla!");
                  }

                  for(var13 = var10; var13 < var16.size(); ++var13) {
                     var14 = (String)var16.get(var13);
                     this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(var14), (String)var17.get(var4));
                  }
               }

               var6 = var5.getVersionDir();
               if (var6 != null) {
                  var7 = new File(var6);
                  var8 = var7.toURI();
                  var9 = (new File(var7.getCanonicalFile().getPath().toLowerCase(Locale.ENGLISH))).toURI();
                  var10 = var16.size();
                  var11 = ZomboidFileSystem.instance.getCanonicalFile(var7, "media");
                  var12 = ZomboidFileSystem.instance.getCanonicalFile(var11, "scripts");
                  this.searchFolders(var9, var12, var16);
                  if (((String)var17.get(var4)).equals("pz-vanilla")) {
                     throw new RuntimeException("Warning mod id is named pz-vanilla!");
                  }

                  for(var13 = var10; var13 < var16.size(); ++var13) {
                     var14 = (String)var16.get(var13);
                     this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(var14), (String)var17.get(var4));
                  }
               }
            }
         }

         Comparator var18 = new Comparator<String>() {
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
         Collections.sort(var1, var18);
         Collections.sort(var16, var18);
         var1.addAll(var16);
         if (GameClient.bClient || GameServer.bServer) {
            NetChecksum.checksummer.reset(true);
            NetChecksum.GroupOfFiles.initChecksum();
         }

         MultiStageBuilding.stages.clear();
         HashSet var19 = new HashSet();
         Iterator var20 = var1.iterator();

         label76:
         while(true) {
            String var21;
            String var22;
            do {
               do {
                  if (!var20.hasNext()) {
                     if (GameClient.bClient || GameServer.bServer) {
                        this.checksum = NetChecksum.checksummer.checksumToString();
                        if (GameServer.bServer) {
                           DebugLog.General.println("scriptChecksum: " + this.checksum);
                        }
                     }
                     break label76;
                  }

                  var21 = (String)var20.next();
               } while(var19.contains(var21));

               var19.add(var21);
               var22 = ZomboidFileSystem.instance.getAbsolutePath(var21);
               currentLoadFileAbsPath = var22;
               currentLoadFileName = FileName.getName(var22);
               currentLoadFileMod = (String)this.tempFileToModMap.get(var22);
               this.LoadFile(ScriptLoadMode.Init, var21, false);
            } while(!GameClient.bClient && !GameServer.bServer);

            NetChecksum.checksummer.addFile(var21, var22);
         }
      } catch (Exception var15) {
         ExceptionLogger.logException(var15);
      }

      this.buf.setLength(0);
      this.loadScripts(ScriptLoadMode.Init, EnumSet.allOf(ScriptType.class));
      if (Core.bDebug && this.hasLoadErrors()) {
         throw new IOException("Script load errors.");
      } else {
         this.debugItems();
         this.resolveItemTypes();
         WorldDictionary.ScriptsLoaded();
         RecipeManager.ScriptsLoaded();
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
   }

   public void ReloadScripts(ScriptType var1) {
      this.ReloadScripts(EnumSet.of(var1));
   }

   public void ReloadScripts(EnumSet<ScriptType> var1) {
      DebugLog.General.debugln("Reloading scripts = " + var1);
      this.loadScripts(ScriptLoadMode.Reload, var1);
   }

   private void loadScripts(ScriptLoadMode var1, EnumSet<ScriptType> var2) {
      try {
         XuiManager.setParseOnce(true);
         Iterator var3;
         ScriptBucketCollection var4;
         if (var1 == ScriptLoadMode.Reload) {
            var3 = this.bucketCollectionList.iterator();

            while(var3.hasNext()) {
               var4 = (ScriptBucketCollection)var3.next();
               if (var2.contains(var4.getScriptType())) {
                  var4.setReloadBuckets(true);
                  var4.PreReloadScripts();
               }
            }

            var3 = this.loadFileNames.iterator();

            label81:
            while(true) {
               if (!var3.hasNext()) {
                  var3 = this.bucketCollectionList.iterator();

                  while(true) {
                     if (!var3.hasNext()) {
                        break label81;
                     }

                     var4 = (ScriptBucketCollection)var3.next();
                     var4.setReloadBuckets(false);
                  }
               }

               String var7 = (String)var3.next();
               String var5 = ZomboidFileSystem.instance.getAbsolutePath(var7);
               currentLoadFileAbsPath = var5;
               currentLoadFileName = FileName.getName(var5);
               currentLoadFileMod = (String)this.tempFileToModMap.get(var5);
               this.LoadFile(ScriptLoadMode.Reload, var7, true);
            }
         }

         var3 = this.bucketCollectionList.iterator();

         while(var3.hasNext()) {
            var4 = (ScriptBucketCollection)var3.next();
            if (var2.contains(var4.getScriptType())) {
               var4.LoadScripts(var1);
            }
         }

         var3 = this.bucketCollectionList.iterator();

         while(var3.hasNext()) {
            var4 = (ScriptBucketCollection)var3.next();
            if (var2.contains(var4.getScriptType())) {
               var4.PostLoadScripts(var1);
            }
         }

         var3 = this.bucketCollectionList.iterator();

         while(var3.hasNext()) {
            var4 = (ScriptBucketCollection)var3.next();
            if (var2.contains(var4.getScriptType())) {
               var4.OnScriptsLoaded(var1);
            }
         }

         if (var1 == ScriptLoadMode.Reload) {
            var3 = this.bucketCollectionList.iterator();

            while(var3.hasNext()) {
               var4 = (ScriptBucketCollection)var3.next();
               if (var2.contains(var4.getScriptType())) {
                  var4.OnLoadedAfterLua();
                  var4.OnPostTileDefinitions();
                  var4.OnPostWorldDictionaryInit();
               }
            }
         }
      } catch (Exception var6) {
         ExceptionLogger.logException(var6);
         this.hasLoadErrors = true;
      }

      XuiManager.setParseOnce(false);
   }

   public void LoadedAfterLua() {
      Iterator var1 = this.bucketCollectionList.iterator();

      while(var1.hasNext()) {
         ScriptBucketCollection var2 = (ScriptBucketCollection)var1.next();

         try {
            var2.OnLoadedAfterLua();
         } catch (Exception var4) {
            var4.printStackTrace();
            this.hasLoadErrors = true;
         }
      }

   }

   public void PostTileDefinitions() {
      Iterator var1 = this.bucketCollectionList.iterator();

      while(var1.hasNext()) {
         ScriptBucketCollection var2 = (ScriptBucketCollection)var1.next();

         try {
            var2.OnPostTileDefinitions();
         } catch (Exception var4) {
            var4.printStackTrace();
            this.hasLoadErrors = true;
         }
      }

   }

   public void PostWorldDictionaryInit() {
      Iterator var1 = this.bucketCollectionList.iterator();

      while(var1.hasNext()) {
         ScriptBucketCollection var2 = (ScriptBucketCollection)var1.next();

         try {
            var2.OnPostWorldDictionaryInit();
         } catch (Exception var4) {
            var4.printStackTrace();
            this.hasLoadErrors = true;
         }
      }

   }

   public boolean hasLoadErrors() {
      return this.hasLoadErrors(false);
   }

   public boolean hasLoadErrors(boolean var1) {
      if (this.hasLoadErrors) {
         return true;
      } else {
         Iterator var2 = this.bucketCollectionList.iterator();

         ScriptBucketCollection var3;
         do {
            if (!var2.hasNext()) {
               return false;
            }

            var3 = (ScriptBucketCollection)var2.next();
         } while(!var3.hasLoadErrors(var1));

         return true;
      }
   }

   public static void resolveGetItemTypes(ArrayList<String> var0, ArrayList<Item> var1) {
      for(int var2 = var0.size() - 1; var2 >= 0; --var2) {
         String var3 = (String)var0.get(var2);
         if (var3.startsWith("[")) {
            var0.remove(var2);
            String var4 = var3.substring(1, var3.indexOf("]"));
            Object var5 = LuaManager.getFunctionObject(var4);
            if (var5 != null) {
               var1.clear();
               LuaManager.caller.protectedCallVoid(LuaManager.thread, var5, var1);

               for(int var6 = 0; var6 < var1.size(); ++var6) {
                  Item var7 = (Item)var1.get(var6);
                  var0.add(var2 + var6, var7.getFullName());
               }
            }
         }
      }

   }

   private void debugItems() {
      ArrayList var1 = instance.getAllItems();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         Item var3 = (Item)var2.next();
         if (var3.getType() == Item.Type.Drainable && var3.getReplaceOnUse() != null) {
            DebugLog.Script.warn("%s ReplaceOnUse instead of ReplaceOnDeplete", var3.getFullName());
         }

         boolean var4;
         if (var3.getType() == Item.Type.Weapon && !var3.HitSound.equals(var3.hitFloorSound)) {
            var4 = true;
         }

         if (!StringUtils.isNullOrEmpty(var3.worldStaticModel)) {
            if (var3.getType() == Item.Type.Food && var3.getStaticModel() == null) {
               var4 = true;
            }

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
      Iterator var2 = var1.items.getScriptMap().values().iterator();

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
      } else if (this.items.hasFullType(var1)) {
         return (Item)this.items.getFullType(var1);
      } else {
         int var2 = var1.indexOf(".");
         String var3 = var1.substring(0, var2);
         String var4 = var1.substring(var2 + 1);
         ScriptModule var5 = this.getModule(var3, false);
         return var5 == null ? null : var5.getItem(var4);
      }
   }

   public GameEntityScript getSpecificEntity(String var1) {
      if (!var1.contains(".")) {
         DebugLog.log("ScriptManager.getSpecificEntity requires a full type name, cannot find: " + var1);
         if (Core.bDebug) {
            throw new RuntimeException("ScriptManager.getSpecificEntity requires a full type name, cannot find: " + var1);
         } else {
            return null;
         }
      } else if (this.entities.hasFullType(var1)) {
         return (GameEntityScript)this.entities.getFullType(var1);
      } else {
         int var2 = var1.indexOf(".");
         String var3 = var1.substring(0, var2);
         String var4 = var1.substring(var2 + 1);
         ScriptModule var5 = this.getModule(var3, false);
         return var5 == null ? null : var5.getGameEntityScript(var4);
      }
   }
}
