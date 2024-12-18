package zombie.scripting.entity.components.crafting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import zombie.Lua.LuaManager;
import zombie.characters.HaloTextHelper;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.OutputMapper;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.util.BitSet;
import zombie.gameStates.ChooseGameInfo;
import zombie.inventory.InventoryItem;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.TimedActionScript;
import zombie.util.StringUtils;
import zombie.util.TaggedObjectManager;
import zombie.util.list.PZUnmodifiableList;

@DebugClassFields
public class CraftRecipe extends BaseScriptObject implements TaggedObjectManager.TaggedObject {
   private String name;
   private String translationName;
   private String iconName;
   private Texture iconTexture;
   private boolean needToBeLearn = false;
   public ArrayList<RequiredSkill> skillRequired = null;
   public ArrayList<RequiredSkill> autoLearn = null;
   public ArrayList<xp_Award> xpAward = null;
   public String metaRecipe;
   private boolean hasOnTickInputs = false;
   private boolean hasOnTickOutputs = false;
   private int time = 50;
   private String loadedTimedActionScript;
   private TimedActionScript timedActionScript;
   private final HashMap<LuaCall, String> luaCalls = new HashMap();
   private final ArrayList<InputScript> inputs = new ArrayList();
   private final ArrayList<OutputScript> outputs = new ArrayList();
   private final ArrayList<IOScript> ioLines = new ArrayList();
   private String category;
   private final ArrayList<String> categoryTags = new ArrayList();
   private final List<String> unmodifiableCategoryTags;
   private final BitSet categoryBits;
   private InputScript Prop1;
   private InputScript Prop2;
   private String animation;
   private InputScript toolLeft;
   private InputScript toolRight;
   private boolean existsAsVanilla;
   private String modID;
   private ChooseGameInfo.Mod modInfo;
   private final HashMap<String, OutputMapper> outputMappers;
   private boolean usesTools;
   private String tooltip;
   private boolean allowBatchCraft;
   private boolean canWalk;
   private static String luaOnTestCacheString;
   private static Object luaOnTestCacheObject;
   private String OnAddToMenu;

   public CraftRecipe() {
      super(ScriptType.CraftRecipe);
      this.unmodifiableCategoryTags = PZUnmodifiableList.wrap(this.categoryTags);
      this.categoryBits = new BitSet();
      this.Prop1 = null;
      this.Prop2 = null;
      this.toolLeft = null;
      this.toolRight = null;
      this.existsAsVanilla = false;
      this.outputMappers = new HashMap();
      this.usesTools = false;
      this.tooltip = null;
      this.allowBatchCraft = true;
      this.canWalk = false;
   }

   protected OutputMapper getOutputMapper(String var1) {
      return (OutputMapper)this.outputMappers.get(var1);
   }

   protected OutputMapper getOrCreateOutputMapper(String var1) {
      if (this.outputMappers.containsKey(var1)) {
         return (OutputMapper)this.outputMappers.get(var1);
      } else {
         OutputMapper var2 = new OutputMapper(var1);
         this.outputMappers.put(var1, var2);
         return var2;
      }
   }

   public boolean getExistsAsVanilla() {
      return this.existsAsVanilla;
   }

   public boolean isVanilla() {
      return this.modID != null && this.modID.equals("pz-vanilla");
   }

   public String getModID() {
      return this.modID;
   }

   public String getModName() {
      if (this.modID != null && this.modID.equals("pz-vanilla")) {
         return "Project Zomboid";
      } else {
         return this.modInfo != null && this.modInfo.getName() != null ? this.modInfo.getName() : "<unknown_mod>";
      }
   }

   public String getName() {
      return this.name;
   }

   public String getTranslationName() {
      return this.translationName;
   }

   public void overrideTranslationName(String var1) {
      this.translationName = var1;
   }

   public String getIconName() {
      return this.iconName;
   }

   public Texture getIconTexture() {
      return this.iconTexture;
   }

   public void overrideIconTexture(Texture var1) {
      this.iconTexture = var1;
   }

   /** @deprecated */
   @Deprecated
   public boolean isShapeless() {
      return true;
   }

   /** @deprecated */
   @Deprecated
   public boolean isConsumeOnFinish() {
      return false;
   }

   /** @deprecated */
   @Deprecated
   public boolean isRequiresPlayer() {
      return false;
   }

   public boolean needToBeLearn() {
      return this.needToBeLearn;
   }

   public int getTime() {
      return this.time;
   }

   public TimedActionScript getTimedActionScript() {
      return this.timedActionScript;
   }

   public String getCategory() {
      return StringUtils.isNullOrWhitespace(this.category) ? "Miscellaneous" : this.category;
   }

   public List<String> getTags() {
      return this.unmodifiableCategoryTags;
   }

   public BitSet getTagBits() {
      return this.categoryBits;
   }

   public int getInputCount() {
      return this.inputs.size();
   }

   public int getOutputCount() {
      return this.outputs.size();
   }

   public ArrayList<InputScript> getInputs() {
      return this.inputs;
   }

   public ArrayList<OutputScript> getOutputs() {
      return this.outputs;
   }

   public ArrayList<IOScript> getIoLines() {
      return this.ioLines;
   }

   public int getIndexForIO(IOScript var1) {
      return this.ioLines.indexOf(var1);
   }

   public IOScript getIOForIndex(int var1) {
      return var1 >= 0 && var1 < this.ioLines.size() ? (IOScript)this.ioLines.get(var1) : null;
   }

   public boolean containsIO(IOScript var1) {
      return this.ioLines.contains(var1);
   }

   public boolean isUsesTools() {
      return this.usesTools;
   }

   public InputScript getToolLeft() {
      return this.toolLeft;
   }

   public InputScript getToolRight() {
      return this.toolRight;
   }

   public InputScript getToolBoth() {
      return null;
   }

   public InputScript getProp1() {
      return this.Prop1;
   }

   public void setProp1(InputScript var1) {
      this.Prop1 = var1;
   }

   public InputScript getProp2() {
      return this.Prop2;
   }

   public void setProp2(InputScript var1) {
      this.Prop2 = var1;
   }

   public String getAnimation() {
      return this.animation;
   }

   public void setAnimation(String var1) {
      this.animation = var1;
   }

   public boolean hasOnTickInputs() {
      return this.hasOnTickInputs;
   }

   public boolean hasOnTickOutputs() {
      return this.hasOnTickOutputs;
   }

   public boolean hasLuaCall(LuaCall var1) {
      return this.getLuaCallString(var1) != null;
   }

   public String getLuaCallString(LuaCall var1) {
      return (String)this.luaCalls.get(var1);
   }

   private void setLuaCall(LuaCall var1, String var2) {
      this.luaCalls.put(var1, var2);
   }

   public String getTooltip() {
      return this.tooltip;
   }

   public boolean isAllowBatchCraft() {
      return this.allowBatchCraft;
   }

   public boolean isCanWalk() {
      return this.canWalk;
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
      this.modID = ScriptManager.getCurrentLoadFileMod();
      if (this.modID.equals("pz-vanilla")) {
         this.existsAsVanilla = true;
      } else {
         this.modInfo = ChooseGameInfo.getModDetails(this.modID);
      }

   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      this.Load(var1, var3);
   }

   public void Load(String var1, ScriptParser.Block var2) throws Exception {
      this.name = var1;
      this.translationName = Translator.getRecipeName(var1);
      super.LoadCommonBlock(var2);
      Iterator var3 = var2.values.iterator();

      while(true) {
         while(true) {
            String var5;
            String var6;
            do {
               do {
                  do {
                     if (!var3.hasNext()) {
                        var3 = var2.children.iterator();

                        while(true) {
                           while(var3.hasNext()) {
                              ScriptParser.Block var13 = (ScriptParser.Block)var3.next();
                              if (!"inputs".equalsIgnoreCase(var13.type) && !"outputs".equalsIgnoreCase(var13.type)) {
                                 if ("itemMapper".equalsIgnoreCase(var13.type)) {
                                    this.LoadOutputMapper(var13);
                                 } else {
                                    if ("fluidMapper".equalsIgnoreCase(var13.type)) {
                                       throw new Exception("Not yet implemented.");
                                    }

                                    if ("energyMapper".equalsIgnoreCase(var13.type)) {
                                       throw new Exception("Not yet implemented.");
                                    }

                                    DebugLog.Recipe.error("Unknown block '" + var13.type + "' in craft recipe: " + var1);
                                    if (Core.bDebug) {
                                       throw new Exception("CraftRecipe error in " + var1);
                                    }
                                 }
                              } else {
                                 this.LoadIO(var13, "inputs".equalsIgnoreCase(var13.type));
                              }
                           }

                           return;
                        }
                     }

                     ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
                     var5 = var4.getKey().trim();
                     var6 = var4.getValue().trim();
                  } while(var5.isEmpty());
               } while(var6.isEmpty());
            } while(var5.equalsIgnoreCase("consumeOnFinish"));

            if (var5.equalsIgnoreCase("icon")) {
               this.iconName = var6;
            } else if (var5.equalsIgnoreCase("time")) {
               this.time = Integer.parseInt(var6);
            } else if (var5.equalsIgnoreCase("OnTest")) {
               this.setLuaCall(CraftRecipe.LuaCall.OnTest, var6);
            } else if (var5.equalsIgnoreCase("OnAddToMenu")) {
               this.OnAddToMenu = var6;
            } else if (var5.equalsIgnoreCase("OnStart")) {
               this.setLuaCall(CraftRecipe.LuaCall.OnStart, var6);
            } else if (var5.equalsIgnoreCase("OnUpdate")) {
               this.setLuaCall(CraftRecipe.LuaCall.OnUpdate, var6);
            } else if (var5.equalsIgnoreCase("OnCreate")) {
               this.setLuaCall(CraftRecipe.LuaCall.OnCreate, var6);
            } else if (var5.equalsIgnoreCase("OnFailed")) {
               this.setLuaCall(CraftRecipe.LuaCall.OnFailed, var6);
            } else if (!var5.equalsIgnoreCase("requiresPlayer")) {
               if (var5.equalsIgnoreCase("needToBeLearn")) {
                  this.needToBeLearn = var6.equalsIgnoreCase("true");
               } else {
                  String[] var7;
                  int var8;
                  String[] var9;
                  PerkFactory.Perk var10;
                  int var11;
                  RequiredSkill var14;
                  if (var5.equalsIgnoreCase("SkillRequired")) {
                     this.skillRequired = new ArrayList();
                     var7 = var6.split(";");

                     for(var8 = 0; var8 < var7.length; ++var8) {
                        var9 = var7[var8].split(":");
                        var10 = PerkFactory.Perks.FromString(var9[0]);
                        if (var10 == PerkFactory.Perks.MAX) {
                           DebugLog.Recipe.warn("Unknown skill \"%s\" in recipe \"%s\"", var9[0], this.name);
                        } else {
                           var11 = PZMath.tryParseInt(var9[1], 1);
                           var14 = new RequiredSkill(var10, var11);
                           this.skillRequired.add(var14);
                        }
                     }
                  } else if (var5.equalsIgnoreCase("AutoLearn")) {
                     this.autoLearn = new ArrayList();
                     var7 = var6.split(";");

                     for(var8 = 0; var8 < var7.length; ++var8) {
                        var9 = var7[var8].split(":");
                        var10 = PerkFactory.Perks.FromString(var9[0]);
                        if (var10 == PerkFactory.Perks.MAX) {
                           DebugLog.Recipe.warn("Unknown skill \"%s\" in recipe \"%s\"", var9[0], this.name);
                        } else {
                           var11 = PZMath.tryParseInt(var9[1], 1);
                           var14 = new RequiredSkill(var10, var11);
                           this.autoLearn.add(var14);
                        }
                     }
                  } else if (var5.equalsIgnoreCase("xpAward")) {
                     this.xpAward = new ArrayList();
                     var7 = var6.split(";");

                     for(var8 = 0; var8 < var7.length; ++var8) {
                        var9 = var7[var8].split(":");
                        var10 = PerkFactory.Perks.FromString(var9[0]);
                        if (var10 == PerkFactory.Perks.MAX) {
                           DebugLog.Recipe.warn("Unknown skill \"%s\" in recipe \"%s\"", var9[0], this.name);
                        } else {
                           var11 = PZMath.tryParseInt(var9[1], 1);
                           xp_Award var12 = new xp_Award(var10, var11);
                           this.xpAward.add(var12);
                        }
                     }
                  } else if (var5.equalsIgnoreCase("MetaRecipe")) {
                     this.metaRecipe = var6.trim();
                  } else if (var5.equalsIgnoreCase("Animation")) {
                     this.animation = var6.trim();
                  } else if (var5.equalsIgnoreCase("timedAction")) {
                     this.loadedTimedActionScript = var6;
                  } else if (var5.equalsIgnoreCase("category")) {
                     this.category = var6;
                  } else if (var5.equalsIgnoreCase("Tags")) {
                     var7 = var6.split(";");

                     for(var8 = 0; var8 < var7.length; ++var8) {
                        this.categoryTags.add(var7[var8].trim().toLowerCase());
                     }
                  } else if (var5.equalsIgnoreCase("Tooltip")) {
                     this.tooltip = StringUtils.discardNullOrWhitespace(var6);
                  } else if (var5.equalsIgnoreCase("AllowBatchCraft")) {
                     this.allowBatchCraft = var6.equalsIgnoreCase("true");
                  } else if (var5.equalsIgnoreCase("CanWalk")) {
                     this.canWalk = var6.equalsIgnoreCase("true");
                  } else {
                     DebugLog.Recipe.error("Unknown key '" + var5 + "' val(" + var6 + ") in craft recipe: " + var1);
                     if (Core.bDebug) {
                        throw new Exception("CraftRecipe error in " + var1);
                     }
                  }
               }
            }
         }
      }
   }

   private void LoadIO(ScriptParser.Block var1, boolean var2) throws Exception {
      InputScript var3 = null;
      OutputScript var4 = null;
      Iterator var5 = var1.values.iterator();

      while(var5.hasNext()) {
         ScriptParser.Value var6 = (ScriptParser.Value)var5.next();
         if (var6.string != null) {
            String var7 = var6.string.trim();
            if (!StringUtils.isNullOrWhitespace(var7)) {
               if (!StringUtils.containsWhitespace(var7)) {
                  DebugLog.Recipe.warn("Cannot load: " + var6.string);
               }

               OutputScript var8;
               if (var2) {
                  if (var7.toLowerCase().startsWith("replace")) {
                     DebugLog.Recipe.error("Replace inputs have been deprecated in CraftRecipes.");
                  } else if (var7.startsWith("+")) {
                     if (var3 == null) {
                        throw new IOException("Previous input is null [" + this.name + "] line: " + var7);
                     }

                     if (var3.createToItemScript != null) {
                        throw new IOException("Previous input already has '+' output [" + this.name + "] line: " + var7);
                     }

                     var7 = var7.substring(1, var7.length());
                     var8 = OutputScript.Load(this, var7, true);
                     var3.createToItemScript = var8;
                     this.ioLines.add(var8);
                  } else {
                     InputScript var9;
                     if (var7.startsWith("-")) {
                        if (var3 == null) {
                           throw new IOException("Previous input is null [" + this.name + "] line: " + var7);
                        }

                        if (var3.consumeFromItemScript != null) {
                           throw new IOException("Previous input already has '-' input [" + this.name + "] line: " + var7);
                        }

                        var7 = var7.substring(1, var7.length());
                        var9 = InputScript.Load(this, var7, true);
                        var3.consumeFromItemScript = var9;
                        this.ioLines.add(var9);
                     } else {
                        var9 = InputScript.Load(this, var6.string);
                        this.inputs.add(var9);
                        this.ioLines.add(var9);
                        var3 = var9;
                     }
                  }
               } else if (var7.startsWith("+")) {
                  if (var4 == null) {
                     throw new IOException("Previous input is null [" + this.name + "] line: " + var7);
                  }

                  var7 = var7.substring(1, var7.length());
                  var8 = OutputScript.Load(this, var7, true);
                  var4.createToItemScript = var8;
                  this.ioLines.add(var8);
                  var4 = null;
               } else {
                  if (var7.startsWith("-")) {
                     throw new IOException("Cannot add '-' line to output, [" + this.name + "] line: " + var7);
                  }

                  var8 = OutputScript.Load(this, var6.string);
                  this.outputs.add(var8);
                  this.ioLines.add(var8);
                  var4 = var8;
               }
            }
         }
      }

   }

   private void LoadOutputMapper(ScriptParser.Block var1) throws Exception {
      OutputMapper var2 = this.getOrCreateOutputMapper(var1.id.trim());
      Iterator var3 = var1.values.iterator();

      while(true) {
         while(true) {
            String var5;
            String var6;
            do {
               do {
                  if (!var3.hasNext()) {
                     if (var2.isEmpty()) {
                        throw new Exception("Failed to load contents for output mapper: " + var1.id);
                     }

                     return;
                  }

                  ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
                  var5 = var4.getKey().trim();
                  var6 = var4.getValue().trim();
               } while(var5.isEmpty());
            } while(var6.isEmpty());

            if (var5.equalsIgnoreCase("default")) {
               var2.setDefaultOutputEntree(var6);
            } else {
               String[] var7 = var6.split(";");

               for(int var8 = 0; var8 < var7.length; ++var8) {
                  String var9 = var7[var8];
                  if (var9.contains("$")) {
                     switch (var9.substring(0, var9.indexOf("$")).toLowerCase()) {
                        case "fluid":
                           throw new Exception("Not yet implemented");
                        case "energy":
                           throw new Exception("Not yet implemented");
                        default:
                           var7[var8] = var9.substring(var9.indexOf("$") + 1);
                     }
                  }
               }

               var2.addOutputEntree(var5, var7);
            }
         }
      }
   }

   public void PreReload() {
      this.name = null;
      this.translationName = null;
      this.enabled = true;
      this.debugOnly = false;
      this.iconName = null;
      this.iconTexture = null;
      this.needToBeLearn = false;
      this.skillRequired = null;
      this.autoLearn = null;
      this.hasOnTickInputs = false;
      this.hasOnTickOutputs = false;
      this.time = 50;
      this.loadedTimedActionScript = null;
      this.timedActionScript = null;
      this.luaCalls.clear();
      this.inputs.clear();
      this.outputs.clear();
      this.ioLines.clear();
      this.category = null;
      this.categoryTags.clear();
      this.categoryBits.clear();
      this.Prop1 = null;
      this.Prop2 = null;
      this.animation = null;
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      String var2 = this.name;
      if (this.iconName != null) {
         if (!this.iconName.startsWith("Item_")) {
            this.iconName = "Item_" + this.iconName;
         }

         this.iconTexture = Texture.trygetTexture(this.iconName);
         if (Core.bDebug && this.iconName != null && this.iconTexture == null) {
            DebugLog.Recipe.error("Icon not found: " + this.iconName);
            DebugLog.Recipe.printStackTrace();
         }
      }

      Iterator var3 = this.inputs.iterator();

      InputScript var4;
      while(var3.hasNext()) {
         var4 = (InputScript)var3.next();
         var4.OnScriptsLoaded(var1);
      }

      var3 = this.outputs.iterator();

      OutputScript var5;
      while(var3.hasNext()) {
         var5 = (OutputScript)var3.next();
         var5.OnScriptsLoaded(var1);
      }

      var3 = this.inputs.iterator();

      do {
         if (!var3.hasNext()) {
            var3 = this.outputs.iterator();

            while(var3.hasNext()) {
               var5 = (OutputScript)var3.next();
               if (var5.isApplyOnTick()) {
                  this.hasOnTickOutputs = true;
               }
            }

            if (this.loadedTimedActionScript != null) {
               this.timedActionScript = ScriptManager.instance.getTimedActionScript(this.loadedTimedActionScript);
               if (this.timedActionScript == null) {
                  throw new Exception("TimedActionScript '" + this.loadedTimedActionScript + "' could not be found in recipe " + this.name);
               }
            }

            return;
         }

         var4 = (InputScript)var3.next();
         if (var4.isApplyOnTick()) {
            this.hasOnTickInputs = true;
         }

         if (var4.isTool()) {
            this.usesTools = true;
            if (var4.hasFlag(InputFlag.ToolLeft)) {
               if (this.toolLeft != null) {
                  throw new Exception("Duplicate tool left in recipe " + this.name);
               }

               this.toolLeft = var4;
            }

            if (var4.hasFlag(InputFlag.ToolRight)) {
               if (this.toolRight != null) {
                  throw new Exception("Duplicate tool left in recipe " + this.name);
               }

               this.toolRight = var4;
            }
         }

         if (var4.isProp1()) {
            if (this.getProp1() != null) {
               throw new IOException("Duplicate Prop1 in recipe " + this.name);
            }

            this.setProp1(var4);
         }

         if (var4.isProp2()) {
            if (this.getProp2() != null) {
               throw new IOException("Duplicate Prop2 in recipe " + this.name);
            }

            this.setProp2(var4);
         }
      } while(!var4.isProp1() || !var4.isProp2());

      throw new IOException("Set Prop1 and Prop2 for same input? in recipe " + this.name);
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
      Texture var1 = null;
      Iterator var2 = this.inputs.iterator();

      while(var2.hasNext()) {
         InputScript var3 = (InputScript)var2.next();
         var3.OnPostWorldDictionaryInit();
         if (var1 == null && var3.getResourceType() == ResourceType.Item && var3.getPossibleInputItems().size() > 0) {
            var1 = ((Item)var3.getPossibleInputItems().get(0)).getNormalTexture();
         }
      }

      var2 = this.outputs.iterator();

      while(var2.hasNext()) {
         OutputScript var4 = (OutputScript)var2.next();
         var4.OnPostWorldDictionaryInit();
         if (this.iconTexture == null && var4.getResourceType() == ResourceType.Item && var4.getPossibleResultItems().size() > 0) {
            this.iconTexture = ((Item)var4.getPossibleResultItems().get(0)).getNormalTexture();
         }
      }

      if (this.iconTexture == null) {
         this.iconTexture = var1;
      }

   }

   public ArrayList<String> getRequiredSkills() {
      ArrayList var1 = null;
      if (this.skillRequired != null) {
         var1 = new ArrayList();

         for(int var2 = 0; var2 < this.skillRequired.size(); ++var2) {
            RequiredSkill var3 = (RequiredSkill)this.skillRequired.get(var2);
            PerkFactory.Perk var4 = PerkFactory.getPerk(var3.getPerk());
            if (var4 == null) {
               if (var3.getPerk() != null) {
                  String var10001 = var3.getPerk().name;
                  var1.add(var10001 + " " + var3.getLevel());
               }
            } else {
               String var10000 = var4.name;
               String var5 = var10000 + " " + var3.getLevel();
               var1.add(var5);
            }
         }
      }

      return var1;
   }

   public ArrayList<String> getAutoLearnSkills() {
      ArrayList var1 = null;
      if (this.autoLearn != null) {
         var1 = new ArrayList();

         for(int var2 = 0; var2 < this.autoLearn.size(); ++var2) {
            RequiredSkill var3 = (RequiredSkill)this.autoLearn.get(var2);
            PerkFactory.Perk var4 = PerkFactory.getPerk(var3.getPerk());
            if (var4 == null) {
               if (var3.getPerk() != null) {
                  String var10001 = var3.getPerk().name;
                  var1.add(var10001 + " " + var3.getLevel());
               }
            } else {
               String var10000 = var4.name;
               String var5 = var10000 + " " + var3.getLevel();
               var1.add(var5);
            }
         }
      }

      return var1;
   }

   public void checkAutoLearnSkills(IsoGameCharacter var1) {
      if (!var1.isRecipeActuallyKnown(this) && this.getAutoLearnSkillCount() > 0 && this.validateHasAutoLearnSkill(var1)) {
         var1.learnRecipe(this.getName(), false);
         DebugLog.log("Recipe AutoLearned - " + this.getName());
         if ((IsoPlayer)var1 != null) {
            String var2 = Translator.getText(this.getTranslationName());
            HaloTextHelper.addGoodText((IsoPlayer)var1, Translator.getText("IGUI_HaloNote_LearnedRecipe", LuaManager.GlobalObject.getRecipeDisplayName(this.getName())));
         }
      }

   }

   private boolean validateHasAutoLearnSkill(IsoGameCharacter var1) {
      if (this.getAutoLearnSkillCount() > 0) {
         for(int var2 = 0; var2 < this.getAutoLearnSkillCount(); ++var2) {
            RequiredSkill var3 = this.getAutoLearnSkill(var2);
            if (var1.getPerkLevel(var3.getPerk()) < var3.getLevel()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public void checkMetaRecipe(IsoGameCharacter var1, String var2) {
      if (!var1.isRecipeActuallyKnown(this) && this.getMetaRecipe() != null && var1.isRecipeActuallyKnown(this.getMetaRecipe())) {
         var1.learnRecipe(this.getName(), false);
         String var10000 = this.getName();
         DebugLog.log("Recipe MetaRecipe-Learned - " + var10000 + " from " + var2);
      }

   }

   public void checkMetaRecipe(IsoGameCharacter var1) {
      if (!var1.isRecipeActuallyKnown(this) && this.getMetaRecipe() != null && var1.isRecipeActuallyKnown(this.getMetaRecipe())) {
         var1.learnRecipe(this.getName(), false);
         String var10000 = this.getName();
         DebugLog.log("Recipe MetaRecipe-Learned - " + var10000 + " from " + this.getMetaRecipe());
      }

   }

   public int getRequiredSkillCount() {
      return this.skillRequired == null ? 0 : this.skillRequired.size();
   }

   public RequiredSkill getRequiredSkill(int var1) {
      return this.skillRequired != null && var1 >= 0 && var1 < this.skillRequired.size() ? (RequiredSkill)this.skillRequired.get(var1) : null;
   }

   public int getAutoLearnSkillCount() {
      return this.autoLearn == null ? 0 : this.autoLearn.size();
   }

   public RequiredSkill getAutoLearnSkill(int var1) {
      return this.autoLearn != null && var1 >= 0 && var1 < this.autoLearn.size() ? (RequiredSkill)this.autoLearn.get(var1) : null;
   }

   public String getMetaRecipe() {
      return this.metaRecipe;
   }

   public int getXPAwardCount() {
      return this.xpAward == null ? 0 : this.xpAward.size();
   }

   public xp_Award getXPAward(int var1) {
      return this.xpAward != null && var1 >= 0 && var1 < this.xpAward.size() ? (xp_Award)this.xpAward.get(var1) : null;
   }

   public void clearRequiredSkills() {
      if (this.skillRequired != null) {
         this.skillRequired.clear();
      }
   }

   public void addRequiredSkill(PerkFactory.Perk var1, int var2) {
      if (this.skillRequired == null) {
         this.skillRequired = new ArrayList();
      }

      this.skillRequired.add(new RequiredSkill(var1, var2));
   }

   public boolean canUseItem(InventoryItem var1) {
      return CraftRecipeManager.getValidInputScriptForItem(this, var1) != null;
   }

   public boolean canUseItem(String var1) {
      ArrayList var2 = this.getInputs();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         if (((InputScript)var2.get(var3)).canUseItem(var1)) {
            return true;
         }
      }

      return false;
   }

   public boolean OnTestItem(InventoryItem var1) {
      if (var1 == null) {
         return false;
      } else {
         if (this.hasLuaCall(CraftRecipe.LuaCall.OnTest)) {
            String var3 = this.getLuaCallString(CraftRecipe.LuaCall.OnTest);
            Object var2;
            if (luaOnTestCacheObject != null && luaOnTestCacheString != null && luaOnTestCacheString.equals(var3)) {
               var2 = luaOnTestCacheObject;
            } else {
               var2 = LuaManager.getFunctionObject(this.getLuaCallString(CraftRecipe.LuaCall.OnTest), (DebugLogStream)null);
               luaOnTestCacheString = var3;
               luaOnTestCacheObject = var2;
            }

            if (var2 != null) {
               Boolean var4 = LuaManager.caller.protectedCallBoolean(LuaManager.thread, var2, var1);
               return var4 != null && var4;
            }
         }

         return true;
      }
   }

   public boolean hasTag(String var1) {
      return this.unmodifiableCategoryTags.contains(var1.toLowerCase());
   }

   public boolean isCanBeDoneFromFloor() {
      return false;
   }

   public boolean canBeDoneInDark() {
      return this.hasTag("CanBeDoneInDark");
   }

   public boolean isAnySurfaceCraft() {
      return this.hasTag("AnySurfaceCraft");
   }

   public boolean isInHandCraftCraft() {
      return this.hasTag("InHandCraft");
   }

   public int getHighestRelevantSkillLevel(IsoGameCharacter var1) {
      return this.getHighestRelevantSkillLevel(var1, true);
   }

   public int getHighestRelevantSkillLevel(IsoGameCharacter var1, boolean var2) {
      int var3 = 0;
      int var4;
      PerkFactory.Perk var5;
      if (this.getRequiredSkills() != null) {
         for(var4 = 0; var4 < this.getRequiredSkills().size(); ++var4) {
            var5 = this.getRequiredSkill(var4).getPerk();
            if (var5 != null && var1.getPerkLevel(var5) > var3) {
               var3 = var1.getPerkLevel(var5);
            }
         }
      }

      if (this.getXPAwardCount() > 0) {
         for(var4 = 0; var4 < this.getXPAwardCount(); ++var4) {
            var5 = this.getXPAward(var4).getPerk();
            if (var5 != null && var1.getPerkLevel(var5) > var3) {
               var3 = var1.getPerkLevel(var5);
            }
         }
      }

      if (var2 && this.getAutoLearnSkillCount() > 0) {
         for(var4 = 0; var4 < this.getAutoLearnSkillCount(); ++var4) {
            var5 = this.getAutoLearnSkill(var4).getPerk();
            if (var5 != null && var1.getPerkLevel(var5) > var3) {
               var3 = var1.getPerkLevel(var5);
            }
         }
      }

      return var3;
   }

   public static void onLuaFileReloaded() {
      luaOnTestCacheObject = null;
      luaOnTestCacheString = null;
   }

   public String getOnAddToMenu() {
      return this.OnAddToMenu;
   }

   public abstract static class IOScript {
      private final CraftRecipe parentRecipe;

      protected IOScript(CraftRecipe var1) {
         this.parentRecipe = var1;
      }

      public CraftRecipe getParentRecipe() {
         return this.parentRecipe;
      }

      public int getRecipeLineIndex() {
         if (this.parentRecipe != null && this.parentRecipe.containsIO(this)) {
            return this.parentRecipe.getIndexForIO(this);
         } else if (Core.bDebug) {
            throw new IllegalStateException("Script has not been added to parent recipe (yet)");
         } else {
            return -1;
         }
      }
   }

   public static enum LuaCall {
      OnTest,
      OnStart,
      OnUpdate,
      OnCreate,
      OnFailed;

      private LuaCall() {
      }
   }

   public static final class RequiredSkill {
      private final PerkFactory.Perk perk;
      private final int level;

      public RequiredSkill(PerkFactory.Perk var1, int var2) {
         this.perk = var1;
         this.level = var2;
      }

      public PerkFactory.Perk getPerk() {
         return this.perk;
      }

      public int getLevel() {
         return this.level;
      }
   }

   public static final class xp_Award {
      private final PerkFactory.Perk perk;
      private final int amount;

      public xp_Award(PerkFactory.Perk var1, int var2) {
         this.perk = var1;
         this.amount = var2;
      }

      public PerkFactory.Perk getPerk() {
         return this.perk;
      }

      public int getAmount() {
         return this.amount;
      }
   }
}
