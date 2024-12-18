package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import zombie.GameSounds;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.debug.DebugLog;
import zombie.iso.MultiStageBuilding;
import zombie.iso.SpriteModel;
import zombie.scripting.IScriptObjectStore;
import zombie.scripting.ScriptBucket;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.GameEntityTemplate;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.ui.XuiScriptType;
import zombie.vehicles.VehicleEngineRPM;

public final class ScriptModule implements IScriptObjectStore {
   public String name;
   public String value;
   public final ArrayList<String> Imports = new ArrayList();
   public boolean disabled = false;
   private final ArrayList<ScriptBucket<?>> scriptBucketList = new ArrayList();
   private final HashMap<ScriptType, ScriptBucket<?>> scriptBucketMap = new HashMap();
   public final ScriptBucket.Template<VehicleTemplate> vehicleTemplates;
   public final ScriptBucket.Template<GameEntityTemplate> entityTemplates;
   public final ScriptBucket<Item> items;
   public final ScriptBucket<Recipe> recipes;
   public final ScriptBucket<UniqueRecipe> uniqueRecipes;
   public final ScriptBucket<EvolvedRecipe> evolvedRecipes;
   public final ScriptBucket<Fixing> fixings;
   public final ScriptBucket<AnimationsMesh> animationMeshes;
   public final ScriptBucket<MannequinScript> mannequins;
   public final ScriptBucket<ModelScript> models;
   public final ScriptBucket<PhysicsShapeScript> physicsShapes;
   public final ScriptBucket<SpriteModel> spriteModels;
   public final ScriptBucket<GameSoundScript> gameSounds;
   public final ScriptBucket<SoundTimelineScript> soundTimelines;
   public final ScriptBucket<VehicleScript> vehicles;
   public final ScriptBucket<RuntimeAnimationScript> animations;
   public final ScriptBucket<VehicleEngineRPM> vehicleEngineRPMs;
   public final ScriptBucket<ItemConfig> itemConfigs;
   public final ScriptBucket<GameEntityScript> entities;
   public final ScriptBucket<XuiConfigScript> xuiConfigScripts;
   public final ScriptBucket<XuiLayoutScript> xuiLayouts;
   public final ScriptBucket<XuiLayoutScript> xuiStyles;
   public final ScriptBucket<XuiLayoutScript> xuiDefaultStyles;
   public final ScriptBucket<XuiColorsScript> xuiGlobalColors;
   public final ScriptBucket<XuiSkinScript> xuiSkinScripts;
   public final ScriptBucket<ItemFilterScript> itemFilters;
   public final ScriptBucket<FluidFilterScript> fluidFilters;
   public final ScriptBucket<CraftRecipe> craftRecipes;
   public final ScriptBucket<StringListScript> stringLists;
   public final ScriptBucket<EnergyDefinitionScript> energyDefinitionScripts;
   public final ScriptBucket<FluidDefinitionScript> fluidDefinitionScripts;
   public final ScriptBucket<TimedActionScript> timedActionScripts;
   public final ScriptBucket<RagdollScript> ragdollScripts;

   private <T extends BaseScriptObject> ScriptBucket<T> addBucket(ScriptBucket<T> var1) {
      if (this.scriptBucketMap.containsKey(var1.getScriptType())) {
         throw new RuntimeException("ScriptType bucket already added.");
      } else {
         this.scriptBucketMap.put(var1.getScriptType(), var1);
         this.scriptBucketList.add(var1);
         return var1;
      }
   }

   private <T extends BaseScriptObject> ScriptBucket.Template<T> addBucket(ScriptBucket.Template<T> var1) {
      if (this.scriptBucketMap.containsKey(var1.getScriptType())) {
         throw new RuntimeException("ScriptType bucket already added.");
      } else {
         this.scriptBucketMap.put(var1.getScriptType(), var1);
         this.scriptBucketList.add(var1);
         return var1;
      }
   }

   public VehicleTemplate getVehicleTemplate(String var1) {
      return (VehicleTemplate)this.vehicleTemplates.get(var1);
   }

   public GameEntityTemplate getGameEntityTemplate(String var1) {
      return (GameEntityTemplate)this.entityTemplates.get(var1);
   }

   public Item getItem(String var1) {
      return (Item)this.items.get(var1);
   }

   public Recipe getRecipe(String var1) {
      return (Recipe)this.recipes.get(var1);
   }

   public UniqueRecipe getUniqueRecipe(String var1) {
      return (UniqueRecipe)this.uniqueRecipes.get(var1);
   }

   public EvolvedRecipe getEvolvedRecipe(String var1) {
      return (EvolvedRecipe)this.evolvedRecipes.get(var1);
   }

   public Fixing getFixing(String var1) {
      return (Fixing)this.fixings.get(var1);
   }

   public AnimationsMesh getAnimationsMesh(String var1) {
      return (AnimationsMesh)this.animationMeshes.get(var1);
   }

   public MannequinScript getMannequinScript(String var1) {
      return (MannequinScript)this.mannequins.get(var1);
   }

   public ModelScript getModelScript(String var1) {
      return (ModelScript)this.models.get(var1);
   }

   public PhysicsShapeScript getPhysicsShape(String var1) {
      return (PhysicsShapeScript)this.physicsShapes.get(var1);
   }

   public SpriteModel getSpriteModel(String var1) {
      return (SpriteModel)this.spriteModels.get(var1);
   }

   public GameSoundScript getGameSound(String var1) {
      return (GameSoundScript)this.gameSounds.get(var1);
   }

   public SoundTimelineScript getSoundTimeline(String var1) {
      return (SoundTimelineScript)this.soundTimelines.get(var1);
   }

   public VehicleScript getVehicle(String var1) {
      return (VehicleScript)this.vehicles.get(var1);
   }

   public RuntimeAnimationScript getAnimation(String var1) {
      return (RuntimeAnimationScript)this.animations.get(var1);
   }

   public VehicleEngineRPM getVehicleEngineRPM(String var1) {
      return (VehicleEngineRPM)this.vehicleEngineRPMs.get(var1);
   }

   public ItemConfig getItemConfig(String var1) {
      return (ItemConfig)this.itemConfigs.get(var1);
   }

   public GameEntityScript getGameEntityScript(String var1) {
      return (GameEntityScript)this.entities.get(var1);
   }

   public XuiConfigScript getXuiConfigScript(String var1) {
      return (XuiConfigScript)this.xuiConfigScripts.get(var1);
   }

   public XuiLayoutScript getXuiLayout(String var1) {
      return (XuiLayoutScript)this.xuiLayouts.get(var1);
   }

   public XuiLayoutScript getXuiStyle(String var1) {
      return (XuiLayoutScript)this.xuiStyles.get(var1);
   }

   public XuiLayoutScript getXuiDefaultStyle(String var1) {
      return (XuiLayoutScript)this.xuiDefaultStyles.get(var1);
   }

   public XuiColorsScript getXuiGlobalColors(String var1) {
      return (XuiColorsScript)this.xuiGlobalColors.get(var1);
   }

   public XuiSkinScript getXuiSkinScript(String var1) {
      return (XuiSkinScript)this.xuiSkinScripts.get(var1);
   }

   public ItemFilterScript getItemFilter(String var1) {
      return (ItemFilterScript)this.itemFilters.get(var1);
   }

   public FluidFilterScript getFluidFilter(String var1) {
      return (FluidFilterScript)this.fluidFilters.get(var1);
   }

   public CraftRecipe getCraftRecipe(String var1) {
      return (CraftRecipe)this.craftRecipes.get(var1);
   }

   public StringListScript getStringList(String var1) {
      return (StringListScript)this.stringLists.get(var1);
   }

   public EnergyDefinitionScript getEnergyDefinitionScript(String var1) {
      return (EnergyDefinitionScript)this.energyDefinitionScripts.get(var1);
   }

   public FluidDefinitionScript getFluidDefinitionScript(String var1) {
      return (FluidDefinitionScript)this.fluidDefinitionScripts.get(var1);
   }

   public TimedActionScript getTimedActionScript(String var1) {
      return (TimedActionScript)this.timedActionScripts.get(var1);
   }

   public RagdollScript getRagdollScript(String var1) {
      return (RagdollScript)this.ragdollScripts.get(var1);
   }

   public ScriptModule() {
      this.vehicleTemplates = this.addBucket(new ScriptBucket.Template<VehicleTemplate>(this, ScriptType.VehicleTemplate) {
         public VehicleTemplate createInstance(ScriptModule var1, String var2, String var3) {
            return new VehicleTemplate(var1, var2, var3);
         }

         protected VehicleTemplate getFromManager(String var1) {
            return ScriptManager.instance.getVehicleTemplate(var1);
         }

         protected VehicleTemplate getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getVehicleTemplate(var1) : null;
         }
      });
      this.entityTemplates = this.addBucket(new ScriptBucket.Template<GameEntityTemplate>(this, ScriptType.EntityTemplate) {
         public GameEntityTemplate createInstance(ScriptModule var1, String var2, String var3) {
            return new GameEntityTemplate(var1, var2, var3);
         }

         protected GameEntityTemplate getFromManager(String var1) {
            return ScriptManager.instance.getGameEntityTemplate(var1);
         }

         protected GameEntityTemplate getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getGameEntityTemplate(var1) : null;
         }
      });
      this.items = this.addBucket(new ScriptBucket<Item>(this, ScriptType.Item) {
         public Item createInstance(ScriptModule var1, String var2, String var3) {
            return new Item();
         }

         protected Item getFromManager(String var1) {
            return ScriptManager.instance.getItem(var1);
         }

         protected Item getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getItem(var1) : null;
         }
      });
      this.recipes = this.addBucket(new ScriptBucket<Recipe>(this, ScriptType.Recipe) {
         public Recipe createInstance(ScriptModule var1, String var2, String var3) {
            return new Recipe();
         }

         protected Recipe getFromManager(String var1) {
            return ScriptManager.instance.getRecipe(var1);
         }

         protected Recipe getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getRecipe(var1) : null;
         }
      });
      this.uniqueRecipes = this.addBucket(new ScriptBucket<UniqueRecipe>(this, ScriptType.UniqueRecipe) {
         public UniqueRecipe createInstance(ScriptModule var1, String var2, String var3) {
            return new UniqueRecipe(var2);
         }

         protected UniqueRecipe getFromManager(String var1) {
            return ScriptManager.instance.getUniqueRecipe(var1);
         }

         protected UniqueRecipe getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getUniqueRecipe(var1) : null;
         }
      });
      this.evolvedRecipes = this.addBucket(new ScriptBucket<EvolvedRecipe>(this, ScriptType.EvolvedRecipe) {
         public EvolvedRecipe createInstance(ScriptModule var1, String var2, String var3) {
            return new EvolvedRecipe(var2);
         }

         protected EvolvedRecipe getFromManager(String var1) {
            return ScriptManager.instance.getEvolvedRecipe(var1);
         }

         protected EvolvedRecipe getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getEvolvedRecipe(var1) : null;
         }
      });
      this.fixings = this.addBucket(new ScriptBucket<Fixing>(this, ScriptType.Fixing) {
         public Fixing createInstance(ScriptModule var1, String var2, String var3) {
            return new Fixing();
         }

         protected Fixing getFromManager(String var1) {
            return ScriptManager.instance.getFixing(var1);
         }

         protected Fixing getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getFixing(var1) : null;
         }
      });
      this.animationMeshes = this.addBucket(new ScriptBucket<AnimationsMesh>(this, ScriptType.AnimationMesh) {
         public AnimationsMesh createInstance(ScriptModule var1, String var2, String var3) {
            return new AnimationsMesh();
         }

         protected AnimationsMesh getFromManager(String var1) {
            return ScriptManager.instance.getAnimationsMesh(var1);
         }

         protected AnimationsMesh getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getAnimationsMesh(var1) : null;
         }
      });
      this.mannequins = this.addBucket(new ScriptBucket<MannequinScript>(this, ScriptType.Mannequin) {
         public MannequinScript createInstance(ScriptModule var1, String var2, String var3) {
            return new MannequinScript();
         }

         protected MannequinScript getFromManager(String var1) {
            return ScriptManager.instance.getMannequinScript(var1);
         }

         protected MannequinScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getMannequinScript(var1) : null;
         }
      });
      this.models = this.addBucket(new ScriptBucket<ModelScript>(this, ScriptType.Model) {
         public ModelScript createInstance(ScriptModule var1, String var2, String var3) {
            return new ModelScript();
         }

         protected ModelScript getFromManager(String var1) {
            return ScriptManager.instance.getModelScript(var1);
         }

         protected ModelScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getModelScript(var1) : null;
         }
      });
      this.physicsShapes = this.addBucket(new ScriptBucket<PhysicsShapeScript>(this, ScriptType.PhysicsShape) {
         public PhysicsShapeScript createInstance(ScriptModule var1, String var2, String var3) {
            return new PhysicsShapeScript();
         }

         protected PhysicsShapeScript getFromManager(String var1) {
            return ScriptManager.instance.getPhysicsShape(var1);
         }

         protected PhysicsShapeScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getPhysicsShape(var1) : null;
         }
      });
      this.spriteModels = this.addBucket(new ScriptBucket<SpriteModel>(this, ScriptType.SpriteModel) {
         public SpriteModel createInstance(ScriptModule var1, String var2, String var3) {
            return new SpriteModel();
         }

         protected SpriteModel getFromManager(String var1) {
            return ScriptManager.instance.getSpriteModel(var1);
         }

         protected SpriteModel getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getSpriteModel(var1) : null;
         }
      });
      this.gameSounds = this.addBucket(new ScriptBucket<GameSoundScript>(this, ScriptType.Sound) {
         public GameSoundScript createInstance(ScriptModule var1, String var2, String var3) {
            return new GameSoundScript();
         }

         protected GameSoundScript getFromManager(String var1) {
            return ScriptManager.instance.getGameSound(var1);
         }

         protected GameSoundScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getGameSound(var1) : null;
         }

         protected void onScriptLoad(ScriptLoadMode var1, GameSoundScript var2) {
            if (var1 == ScriptLoadMode.Reload) {
               GameSounds.OnReloadSound(var2);
            }

         }
      });
      this.soundTimelines = this.addBucket(new ScriptBucket<SoundTimelineScript>(this, ScriptType.SoundTimeline) {
         public SoundTimelineScript createInstance(ScriptModule var1, String var2, String var3) {
            return new SoundTimelineScript();
         }

         protected SoundTimelineScript getFromManager(String var1) {
            return ScriptManager.instance.getSoundTimeline(var1);
         }

         protected SoundTimelineScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getSoundTimeline(var1) : null;
         }
      });
      this.vehicles = this.addBucket(new ScriptBucket<VehicleScript>(this, ScriptType.Vehicle) {
         public VehicleScript createInstance(ScriptModule var1, String var2, String var3) {
            return new VehicleScript();
         }

         protected VehicleScript getFromManager(String var1) {
            return ScriptManager.instance.getVehicle(var1);
         }

         protected VehicleScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getVehicle(var1) : null;
         }

         protected void onScriptLoad(ScriptLoadMode var1, VehicleScript var2) {
            var2.Loaded();
         }
      });
      this.animations = this.addBucket(new ScriptBucket<RuntimeAnimationScript>(this, ScriptType.RuntimeAnimation, new TreeMap(String.CASE_INSENSITIVE_ORDER)) {
         public RuntimeAnimationScript createInstance(ScriptModule var1, String var2, String var3) {
            return new RuntimeAnimationScript();
         }

         protected RuntimeAnimationScript getFromManager(String var1) {
            return ScriptManager.instance.getRuntimeAnimationScript(var1);
         }

         protected RuntimeAnimationScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getAnimation(var1) : null;
         }
      });
      this.vehicleEngineRPMs = this.addBucket(new ScriptBucket<VehicleEngineRPM>(this, ScriptType.VehicleEngineRPM) {
         public VehicleEngineRPM createInstance(ScriptModule var1, String var2, String var3) {
            return new VehicleEngineRPM();
         }

         protected VehicleEngineRPM getFromManager(String var1) {
            return ScriptManager.instance.getVehicleEngineRPM(var1);
         }

         protected VehicleEngineRPM getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getVehicleEngineRPM(var1) : null;
         }
      });
      this.itemConfigs = this.addBucket(new ScriptBucket<ItemConfig>(this, ScriptType.ItemConfig) {
         public ItemConfig createInstance(ScriptModule var1, String var2, String var3) {
            return new ItemConfig();
         }

         protected ItemConfig getFromManager(String var1) {
            return ScriptManager.instance.getItemConfig(var1);
         }

         protected ItemConfig getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getItemConfig(var1) : null;
         }
      });
      this.entities = this.addBucket(new ScriptBucket<GameEntityScript>(this, ScriptType.Entity) {
         public GameEntityScript createInstance(ScriptModule var1, String var2, String var3) {
            return new GameEntityScript();
         }

         protected GameEntityScript getFromManager(String var1) {
            return ScriptManager.instance.getGameEntityScript(var1);
         }

         protected GameEntityScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getGameEntityScript(var1) : null;
         }
      });
      this.xuiConfigScripts = this.addBucket(new ScriptBucket<XuiConfigScript>(this, ScriptType.XuiConfig) {
         public XuiConfigScript createInstance(ScriptModule var1, String var2, String var3) {
            return new XuiConfigScript();
         }

         protected XuiConfigScript getFromManager(String var1) {
            return ScriptManager.instance.getXuiConfigScript(var1);
         }

         protected XuiConfigScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getXuiConfigScript(var1) : null;
         }
      });
      this.xuiLayouts = this.addBucket(new ScriptBucket<XuiLayoutScript>(this, ScriptType.XuiLayout) {
         public XuiLayoutScript createInstance(ScriptModule var1, String var2, String var3) {
            return new XuiLayoutScript(this.getScriptType(), XuiScriptType.Layout);
         }

         protected XuiLayoutScript getFromManager(String var1) {
            return ScriptManager.instance.getXuiLayout(var1);
         }

         protected XuiLayoutScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getXuiLayout(var1) : null;
         }
      });
      this.xuiStyles = this.addBucket(new ScriptBucket<XuiLayoutScript>(this, ScriptType.XuiStyle) {
         public XuiLayoutScript createInstance(ScriptModule var1, String var2, String var3) {
            return new XuiLayoutScript(this.getScriptType(), XuiScriptType.Style);
         }

         protected XuiLayoutScript getFromManager(String var1) {
            return ScriptManager.instance.getXuiStyle(var1);
         }

         protected XuiLayoutScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getXuiStyle(var1) : null;
         }
      });
      this.xuiDefaultStyles = this.addBucket(new ScriptBucket<XuiLayoutScript>(this, ScriptType.XuiDefaultStyle) {
         public XuiLayoutScript createInstance(ScriptModule var1, String var2, String var3) {
            return new XuiLayoutScript(this.getScriptType(), XuiScriptType.DefaultStyle);
         }

         protected XuiLayoutScript getFromManager(String var1) {
            return ScriptManager.instance.getXuiDefaultStyle(var1);
         }

         protected XuiLayoutScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getXuiDefaultStyle(var1) : null;
         }
      });
      this.xuiGlobalColors = this.addBucket(new ScriptBucket<XuiColorsScript>(this, ScriptType.XuiColor) {
         public XuiColorsScript createInstance(ScriptModule var1, String var2, String var3) {
            return new XuiColorsScript();
         }

         protected XuiColorsScript getFromManager(String var1) {
            return ScriptManager.instance.getXuiColor(var1);
         }

         protected XuiColorsScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getXuiGlobalColors(var1) : null;
         }
      });
      this.xuiSkinScripts = this.addBucket(new ScriptBucket<XuiSkinScript>(this, ScriptType.XuiSkin) {
         public XuiSkinScript createInstance(ScriptModule var1, String var2, String var3) {
            return new XuiSkinScript();
         }

         protected XuiSkinScript getFromManager(String var1) {
            return ScriptManager.instance.getXuiSkinScript(var1);
         }

         protected XuiSkinScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getXuiSkinScript(var1) : null;
         }
      });
      this.itemFilters = this.addBucket(new ScriptBucket<ItemFilterScript>(this, ScriptType.ItemFilter) {
         public ItemFilterScript createInstance(ScriptModule var1, String var2, String var3) {
            return new ItemFilterScript();
         }

         protected ItemFilterScript getFromManager(String var1) {
            return ScriptManager.instance.getItemFilter(var1);
         }

         protected ItemFilterScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getItemFilter(var1) : null;
         }
      });
      this.fluidFilters = this.addBucket(new ScriptBucket<FluidFilterScript>(this, ScriptType.FluidFilter) {
         public FluidFilterScript createInstance(ScriptModule var1, String var2, String var3) {
            return new FluidFilterScript();
         }

         protected FluidFilterScript getFromManager(String var1) {
            return ScriptManager.instance.getFluidFilter(var1);
         }

         protected FluidFilterScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getFluidFilter(var1) : null;
         }
      });
      this.craftRecipes = this.addBucket(new ScriptBucket<CraftRecipe>(this, ScriptType.CraftRecipe) {
         public CraftRecipe createInstance(ScriptModule var1, String var2, String var3) {
            return new CraftRecipe();
         }

         protected CraftRecipe getFromManager(String var1) {
            return ScriptManager.instance.getCraftRecipe(var1);
         }

         protected CraftRecipe getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getCraftRecipe(var1) : null;
         }
      });
      this.stringLists = this.addBucket(new ScriptBucket<StringListScript>(this, ScriptType.StringList) {
         public StringListScript createInstance(ScriptModule var1, String var2, String var3) {
            return new StringListScript();
         }

         protected StringListScript getFromManager(String var1) {
            return ScriptManager.instance.getStringList(var1);
         }

         protected StringListScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getStringList(var1) : null;
         }
      });
      this.energyDefinitionScripts = this.addBucket(new ScriptBucket<EnergyDefinitionScript>(this, ScriptType.EnergyDefinition) {
         public EnergyDefinitionScript createInstance(ScriptModule var1, String var2, String var3) {
            return new EnergyDefinitionScript();
         }

         protected EnergyDefinitionScript getFromManager(String var1) {
            return ScriptManager.instance.getEnergyDefinitionScript(var1);
         }

         protected EnergyDefinitionScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getEnergyDefinitionScript(var1) : null;
         }
      });
      this.fluidDefinitionScripts = this.addBucket(new ScriptBucket<FluidDefinitionScript>(this, ScriptType.FluidDefinition) {
         public FluidDefinitionScript createInstance(ScriptModule var1, String var2, String var3) {
            return new FluidDefinitionScript();
         }

         protected FluidDefinitionScript getFromManager(String var1) {
            return ScriptManager.instance.getFluidDefinitionScript(var1);
         }

         protected FluidDefinitionScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getFluidDefinitionScript(var1) : null;
         }
      });
      this.timedActionScripts = this.addBucket(new ScriptBucket<TimedActionScript>(this, ScriptType.TimedAction) {
         public TimedActionScript createInstance(ScriptModule var1, String var2, String var3) {
            return new TimedActionScript();
         }

         protected TimedActionScript getFromManager(String var1) {
            return ScriptManager.instance.getTimedActionScript(var1);
         }

         protected TimedActionScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getTimedActionScript(var1) : null;
         }
      });
      this.ragdollScripts = this.addBucket(new ScriptBucket<RagdollScript>(this, ScriptType.Ragdoll) {
         public RagdollScript createInstance(ScriptModule var1, String var2, String var3) {
            return new RagdollScript();
         }

         protected RagdollScript getFromManager(String var1) {
            return ScriptManager.instance.getRagdollScript(var1);
         }

         protected RagdollScript getFromModule(String var1, ScriptModule var2) {
            return var2 != null ? var2.getRagdollScript(var1) : null;
         }
      });
      this.xuiLayouts.setVerbose(false);
   }

   public void Load(ScriptLoadMode var1, String var2, String var3) {
      this.name = var2;
      this.value = var3.trim();
      ScriptManager.instance.CurrentLoadingModule = this;
      this.ParseScriptPP(var1, this.value);
      this.value = "";
   }

   public void ParseScriptPP(ScriptLoadMode var1, String var2) {
      ArrayList var3 = ScriptParser.parseTokens(var2);

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         String var5 = (String)var3.get(var4);
         this.CreateFromTokenPP(var1, var5);
      }

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

   private void CreateFromTokenPP(ScriptLoadMode var1, String var2) {
      var2 = var2.trim();
      String var3 = this.GetTokenType(var2);
      if (var3 != null) {
         String[] var4;
         if ("imports".equals(var3)) {
            var4 = var2.split("[{}]");
            String[] var5 = var4[1].split(",");

            for(int var6 = 0; var6 < var5.length; ++var6) {
               if (!var5[var6].trim().isEmpty()) {
                  String var7 = var5[var6].trim();
                  if (var7.equals(this.getName())) {
                     DebugLog.Script.error("ERROR: module \"" + this.getName() + "\" imports itself");
                  } else {
                     this.Imports.add(var7);
                  }
               }
            }
         } else if ("multistagebuild".equals(var3)) {
            var4 = var2.split("[{}]");
            String var9 = var4[0];
            var9 = var9.replace("multistagebuild", "");
            var9 = var9.trim();
            String[] var11 = var4[1].split(",");
            MultiStageBuilding.Stage var13 = new MultiStageBuilding().new Stage();
            var13.Load(var9, var11);
            MultiStageBuilding.addStage(var13);
         } else {
            boolean var8 = false;
            Iterator var10 = this.scriptBucketList.iterator();

            while(var10.hasNext()) {
               ScriptBucket var12 = (ScriptBucket)var10.next();
               if (var12.CreateFromTokenPP(var1, var3, var2)) {
                  var8 = true;
                  break;
               }
            }

            if (!var8) {
               DebugLog.Script.warn("unknown script object \"%s\" in '%s'", var3, ScriptManager.instance.currentFileName);
            }
         }

      }
   }

   public boolean CheckExitPoints() {
      return false;
   }

   public String getName() {
      return this.name;
   }

   public void Reset() {
      this.Imports.clear();
      Iterator var1 = this.scriptBucketList.iterator();

      while(var1.hasNext()) {
         ScriptBucket var2 = (ScriptBucket)var1.next();
         var2.reset();
      }

   }
}
