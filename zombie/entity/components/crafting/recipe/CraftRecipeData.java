package zombie.entity.components.crafting.recipe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.utils.ByteBlock;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.CraftUtil;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.energy.Energy;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemUser;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.Item;

public class CraftRecipeData {
   private static final float FLOAT_EPSILON = 1.0E-5F;
   private static final int MAX_CRAFT_COUNT = 100;
   private static final ArrayDeque<CraftRecipeData> DATA_POOL = new ArrayDeque();
   private CraftRecipeMonitor _m;
   private CraftRecipe recipe;
   public final ArrayList<InputScriptData> inputs = new ArrayList();
   private final ArrayList<OutputScriptData> outputs = new ArrayList();
   private final HashSet<Resource> usedResources = new HashSet();
   private final HashSet<InventoryItem> usedItems = new HashSet();
   private boolean allowInputResources = true;
   private boolean allowInputItems = false;
   private boolean allowOutputResources = true;
   private boolean allowOutputItems = false;
   private CraftMode craftMode;
   private boolean hasConsumedInputs;
   private boolean hasTestedInputs;
   private final ItemDataList toOutputItems;
   private final ArrayList<InventoryItem> consumedDestroyItems;
   private final ArrayList<InventoryItem> consumedUsedItems;
   private final ArrayList<InventoryItem> allViableItems;
   private final ArrayList<Resource> allViableResources;
   private final HashMap<CraftRecipe.LuaCall, Object> luaFunctionMap;
   private static String luaOnTestCacheString;
   private static Object luaOnTestCacheObject;
   private boolean luaInitialized;
   private KahluaTable modData;

   public static CraftRecipeData Alloc(CraftMode var0, boolean var1, boolean var2, boolean var3, boolean var4) {
      CraftRecipeData var5 = (CraftRecipeData)DATA_POOL.poll();
      if (var5 == null) {
         var5 = new CraftRecipeData();
      }

      var5.craftMode = var0;
      var5.allowInputResources = var1;
      var5.allowInputItems = var2;
      var5.allowOutputResources = var3;
      var5.allowOutputItems = var4;
      return var5;
   }

   private CraftRecipeData() {
      this.craftMode = CraftMode.Automation;
      this.hasConsumedInputs = false;
      this.hasTestedInputs = false;
      this.toOutputItems = new ItemDataList(32);
      this.consumedDestroyItems = new ArrayList();
      this.consumedUsedItems = new ArrayList();
      this.allViableItems = new ArrayList();
      this.allViableResources = new ArrayList();
      this.luaFunctionMap = new HashMap();
      this.luaInitialized = false;
   }

   public CraftRecipeData(CraftMode var1, boolean var2, boolean var3, boolean var4, boolean var5) {
      this.craftMode = CraftMode.Automation;
      this.hasConsumedInputs = false;
      this.hasTestedInputs = false;
      this.toOutputItems = new ItemDataList(32);
      this.consumedDestroyItems = new ArrayList();
      this.consumedUsedItems = new ArrayList();
      this.allViableItems = new ArrayList();
      this.allViableResources = new ArrayList();
      this.luaFunctionMap = new HashMap();
      this.luaInitialized = false;
      this.craftMode = var1;
      this.allowInputResources = var2;
      this.allowInputItems = var3;
      this.allowOutputResources = var4;
      this.allowOutputItems = var5;
   }

   public void setMonitor(CraftRecipeMonitor var1) {
      this._m = var1;
   }

   public boolean isAllowInputItems() {
      return this.allowInputItems;
   }

   public boolean isAllowOutputItems() {
      return this.allowOutputItems;
   }

   public boolean isAllowInputResources() {
      return this.allowInputResources;
   }

   public boolean isAllowOutputResources() {
      return this.allowOutputResources;
   }

   public ItemDataList getToOutputItems() {
      return this.toOutputItems;
   }

   public void reset() {
      this.allowInputResources = true;
      this.allowInputItems = false;
      this.allowOutputResources = true;
      this.allowOutputItems = false;
      this.craftMode = CraftMode.Automation;
      this.toOutputItems.reset();
      this.clearRecipe();
      this._m = null;
   }

   private void clearRecipe() {
      this.recipe = null;

      for(int var2 = 0; var2 < this.inputs.size(); ++var2) {
         InputScriptData var1 = (InputScriptData)this.inputs.get(var2);
         CraftRecipeData.InputScriptData.Release(var1);
      }

      this.inputs.clear();

      for(int var3 = 0; var3 < this.outputs.size(); ++var3) {
         OutputScriptData var4 = (OutputScriptData)this.outputs.get(var3);
         CraftRecipeData.OutputScriptData.Release(var4);
      }

      this.outputs.clear();
      this.luaFunctionMap.clear();
      luaOnTestCacheString = null;
      luaOnTestCacheObject = null;
      this.luaInitialized = false;
      this.clearCaches();
      this.allViableItems.clear();
      this.allViableResources.clear();
   }

   private void clearCaches() {
      int var2;
      if (this.inputs.size() > 0) {
         for(var2 = 0; var2 < this.inputs.size(); ++var2) {
            InputScriptData var1 = (InputScriptData)this.inputs.get(var2);
            var1.clearCache();
         }
      }

      if (this.outputs.size() > 0) {
         for(var2 = 0; var2 < this.outputs.size(); ++var2) {
            OutputScriptData var3 = (OutputScriptData)this.outputs.get(var2);
            var3.clearCache();
         }
      }

      this.hasConsumedInputs = false;
      this.hasTestedInputs = false;
      this.usedResources.clear();
      this.usedItems.clear();
      this.toOutputItems.clear();
      if (this.modData != null) {
         this.modData.wipe();
      }

   }

   public void setRecipe(CraftRecipe var1) {
      if (var1 == null) {
         this.clearRecipe();
      } else if (this.recipe != var1) {
         this.clearRecipe();
         this.recipe = var1;

         int var2;
         for(var2 = 0; var2 < var1.getInputs().size(); ++var2) {
            InputScriptData var3 = CraftRecipeData.InputScriptData.Alloc(this, (InputScript)var1.getInputs().get(var2));
            this.inputs.add(var3);
         }

         for(var2 = 0; var2 < var1.getOutputs().size(); ++var2) {
            OutputScriptData var4 = CraftRecipeData.OutputScriptData.Alloc(this, (OutputScript)var1.getOutputs().get(var2));
            this.outputs.add(var4);
         }
      } else {
         this.clearCaches();
      }

   }

   public CraftRecipe getRecipe() {
      return this.recipe;
   }

   public InputScriptData getDataForInputScript(InputScript var1) {
      for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
         if (var2.inputScript == var1) {
            return var2;
         }
      }

      return null;
   }

   protected OutputScriptData getDataForOutputScript(OutputScript var1) {
      for(int var3 = 0; var3 < this.outputs.size(); ++var3) {
         OutputScriptData var2 = (OutputScriptData)this.outputs.get(var3);
         if (var2.outputScript == var1) {
            return var2;
         }
      }

      return null;
   }

   public InventoryItem getFirstManualInputFor(InputScript var1) {
      if (this.recipe != null && this.recipe.containsIO(var1)) {
         InputScriptData var2 = this.getDataForInputScript(var1);
         return var2.getFirstInputItem();
      } else {
         return null;
      }
   }

   public boolean canOfferInputItem(InventoryItem var1) {
      return this.canOfferInputItem(var1, false);
   }

   public boolean canOfferInputItem(InventoryItem var1, boolean var2) {
      for(int var4 = 0; var4 < this.inputs.size(); ++var4) {
         InputScriptData var3 = (InputScriptData)this.inputs.get(var4);
         if (this.canOfferInputItem(var3.inputScript, var1, var2)) {
            return true;
         }
      }

      return false;
   }

   public boolean canOfferInputItem(InputScript var1, InventoryItem var2) {
      return this.canOfferInputItem(var1, var2, false);
   }

   public boolean canOfferInputItem(InputScript var1, InventoryItem var2, boolean var3) {
      Objects.requireNonNull(var1);
      Objects.requireNonNull(var2);
      if (this.recipe == null) {
         return false;
      } else if (!this.recipe.containsIO(var1)) {
         if (var3) {
            DebugLog.CraftLogic.warn("Input script not part of current recipe.");
         }

         return false;
      } else {
         InputScriptData var4 = this.getDataForInputScript(var1);
         if (var4 == null) {
            if (var3) {
               DebugLog.CraftLogic.warn("Data is null for input script");
            }

            return false;
         } else if (var4.getInputScript().getResourceType() != ResourceType.Item) {
            if (var3) {
               DebugLog.CraftLogic.warn("Cannot offer items to input scripts that are not ResourceType.Item");
            }

            return false;
         } else {
            return var4.acceptsInputItem(var2);
         }
      }
   }

   public boolean offerInputItem(InventoryItem var1) {
      for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
         if (this.canOfferInputItem(var2.inputScript, var1, false) && !this.containsInputItem(var1)) {
            if (var2.inputScript.isExclusive() && var2.getFirstInputItem() != null && !var2.getFirstInputItem().getFullType().equals(var1.getFullType())) {
               while(var2.getLastInputItem() != null) {
                  var2.removeInputItem(var2.getLastInputItem());
               }
            }

            if (var2.isInputItemsSatisfied()) {
               var2.removeInputItem(var2.getLastInputItem());
            }

            return var2.addInputItem(var1);
         }
      }

      return false;
   }

   public boolean offerInputItem(InputScript var1, InventoryItem var2) {
      return this.offerInputItem(var1, var2, false);
   }

   public boolean offerInputItem(InputScript var1, InventoryItem var2, boolean var3) {
      if (this.canOfferInputItem(var1, var2, var3) && !this.containsInputItem(var2)) {
         InputScriptData var4 = this.getDataForInputScript(var1);
         if (!var4.isInputItemsSatisfied()) {
            return var4.addInputItem(var2);
         }
      }

      return false;
   }

   public boolean containsInputItem(InventoryItem var1) {
      for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
         if (var2.inputScript.getResourceType() == ResourceType.Item && var2.inputItems.contains(var1)) {
            return true;
         }
      }

      return false;
   }

   public boolean removeInputItem(InventoryItem var1) {
      for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
         if (var2.removeInputItem(var1)) {
            return true;
         }
      }

      return false;
   }

   public boolean areAllInputItemsSatisfied() {
      for(int var1 = 0; var1 < this.inputs.size(); ++var1) {
         if (!((InputScriptData)this.inputs.get(var1)).isInputItemsSatisfied()) {
            return false;
         }
      }

      return true;
   }

   public boolean luaCallOnTest() {
      return true;
   }

   private boolean initLuaFunctions() {
      if (this.recipe == null) {
         return false;
      } else {
         CraftRecipe.LuaCall[] var1 = CraftRecipe.LuaCall.values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            CraftRecipe.LuaCall var4 = var1[var3];
            if (this.recipe.hasLuaCall(var4)) {
               Object var5 = LuaManager.getFunctionObject(this.recipe.getLuaCallString(var4), (DebugLogStream)null);
               if (var5 != null) {
                  this.luaFunctionMap.put(var4, var5);
               } else {
                  DebugLogStream var10000 = DebugLog.CraftLogic;
                  String var10001 = this.recipe.getLuaCallString(var4);
                  var10000.warn("Could not find lua function: " + var10001);
               }
            }
         }

         return true;
      }
   }

   public void luaCallOnStart() {
      this.luaCallOnStart((IsoGameCharacter)null);
   }

   public void luaCallOnStart(IsoGameCharacter var1) {
      if (this.initLuaFunctions()) {
         Object var2 = this.luaFunctionMap.get(CraftRecipe.LuaCall.OnStart);
         if (var2 != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, var2, this, var1);
         }

      }
   }

   public void luaCallOnUpdate() {
      if (this.initLuaFunctions()) {
         Object var1 = this.luaFunctionMap.get(CraftRecipe.LuaCall.OnUpdate);
         if (var1 != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, var1, this);
         }

      }
   }

   public void luaCallOnCreate() {
      this.luaCallOnCreate((IsoGameCharacter)null);
   }

   public void luaCallOnCreate(IsoGameCharacter var1) {
      if (this.initLuaFunctions()) {
         Object var2 = this.luaFunctionMap.get(CraftRecipe.LuaCall.OnCreate);
         if (var2 != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, var2, this, var1);
         }

      }
   }

   public void luaCallOnFailed() {
      if (this.initLuaFunctions()) {
         Object var1 = this.luaFunctionMap.get(CraftRecipe.LuaCall.OnFailed);
         if (var1 != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, var1, this);
         }

      }
   }

   public boolean canPerform(IsoGameCharacter var1, List<Resource> var2, List<InventoryItem> var3) {
      if (!CraftRecipeManager.isValidRecipeForCharacter(this.recipe, var1, this._m)) {
         return false;
      } else {
         return this.consumeInputsInternal(var1, true, var2, var3) && this.createOutputsInternal(true, (List)null, var1);
      }
   }

   public boolean perform(IsoGameCharacter var1, List<Resource> var2, List<InventoryItem> var3) {
      if (!CraftRecipeManager.isValidRecipeForCharacter(this.recipe, var1, this._m)) {
         return false;
      } else {
         boolean var4 = this.consumeInputsInternal(var1, false, var2, var3);
         if (var4) {
            this.luaCallOnStart(var1);
            if (this.createOutputsInternal(false, (List)null, var1)) {
               this.luaCallOnCreate(var1);
               this.processDestroyAndUsedItems(var1);
               if (this.recipe.xpAward != null) {
                  this.addXP(var1);
               }

               return true;
            }
         }

         return false;
      }
   }

   private void addXP(IsoGameCharacter var1) {
      if (this.recipe.xpAward == null) {
         DebugLog.CraftLogic.println("XP ATTEMPTING: No XP Award for " + this.recipe.getName());
      } else {
         DebugLog.CraftLogic.println("XP ATTEMPTING: Trying to Award XP for " + this.recipe.getName());
         DebugLog.CraftLogic.println("XP ATTEMPTING: Recipe has xpAward " + this.recipe.xpAward);
         if (this.recipe.getXPAwardCount() > 0) {
            for(int var2 = 0; var2 < this.recipe.getXPAwardCount(); ++var2) {
               CraftRecipe.xp_Award var3 = this.recipe.getXPAward(var2);
               PerkFactory.Perk var4 = var3.getPerk();
               int var5 = var3.getAmount();
               DebugLogStream var10000 = DebugLog.CraftLogic;
               String var10001 = var4.getName();
               var10000.println("XP ATTEMPTING: Trying to Award XP to " + var10001 + " for an XP amount of " + var5);
               if (GameServer.bServer) {
                  GameServer.addXp((IsoPlayer)var1, var4, (float)var5);
               } else if (!GameClient.bClient) {
                  var1.getXp().AddXP(var4, (float)var5);
               }
            }
         }

      }
   }

   private void processDestroyAndUsedItems(IsoGameCharacter var1) {
      this.recipe.getInputs();
      ArrayList var3 = this.recipe.getInputs();
      float[] var4 = new float[var3.size()];

      int var5;
      for(var5 = 0; var5 < var3.size(); ++var5) {
         var4[var5] = (float)((InputScript)var3.get(var5)).getIntAmount();
      }

      for(var5 = 0; var5 < this.consumedUsedItems.size(); ++var5) {
         InventoryItem var2 = (InventoryItem)this.consumedUsedItems.get(var5);
         Item var6 = var2.getScriptItem();
         DebugLog.CraftLogic.println("post process -> Sync using item: " + var2.getFullType());
         boolean var7 = true;

         for(int var8 = 0; var8 < var3.size(); ++var8) {
            if (((InputScript)var3.get(var8)).containsItem(var6)) {
               if (var2.getFluidContainer() != null && !((InputScript)var3.get(var8)).isItemCount()) {
                  continue;
               }

               InputScriptData var9 = this.getDataForInputScript((InputScript)var3.get(var8));
               this.processKeepInputItem(var9, var1, false, var2);
               if (var4[var8] > 1.0E-5F) {
                  float var10 = (float)Math.ceil((double)var4[var8]);
                  boolean var11 = ((InputScript)var3.get(var8)).isDestroy();
                  if (((InputScript)var3.get(var8)).hasFlag(InputFlag.DontReplace) && (var2.getReplaceOnUse() != null || var2.getScriptItem().getReplaceOnDeplete() != null)) {
                     var11 = true;
                  }

                  if (((InputScript)var3.get(var8)).isItemCount()) {
                     int var12 = var2.getCurrentUses();
                     ItemUser.UseItem(var2, true, false, var12, ((InputScript)var3.get(var8)).isKeep(), var11);
                     float var13 = 1.0F / ((InputScript)var3.get(var8)).getRelativeScale(var6.getFullName());
                     var4[var8] -= var13;
                  } else {
                     float var15 = ((InputScript)var3.get(var8)).getRelativeScale(var6.getFullName());
                     int var16 = (int)(var10 * var15);
                     var16 = Math.min(var16, (int)var10);
                     int var14 = ItemUser.UseItem(var2, true, false, var16, ((InputScript)var3.get(var8)).isKeep(), var11);
                     var4[var8] -= (float)var14 / var15;
                  }
               }
            }

            if (GameServer.bServer) {
               GameServer.sendItemStats(var2);
            }
         }
      }

   }

   public int getPossibleCraftCount(List<Resource> var1, List<InventoryItem> var2, List<Resource> var3, List<InventoryItem> var4) {
      var4.clear();
      var3.clear();
      int var5 = 100;

      for(int var6 = 0; var6 < this.inputs.size(); ++var6) {
         InputScriptData var7 = (InputScriptData)this.inputs.get(var6);
         float var8 = 0.0F;

         int var9;
         for(var9 = 0; var9 < var2.size(); ++var9) {
            InventoryItem var10 = (InventoryItem)var2.get(var9);
            if (var7.getInputScript().canUseItem(var10)) {
               float var11 = var7.getInputScript().getRelativeScale(var10.getScriptItem().getFullName());
               int var12 = var7.getInputScript().isItemCount() ? 1 : var10.getCurrentUses();
               float var13 = (float)var12 / var11;
               var8 += var13;
               var4.add(var10);
            }
         }

         if (var7.getInputScript().isKeep()) {
            var9 = var7.getInputScript().getIntAmount();
            if (var8 < (float)var9) {
               var5 = 0;
               break;
            }
         } else {
            float var14 = var7.getInputScript().getAmount();
            int var15 = (int)Math.floor((double)(var8 / var14));
            var5 = Math.min(var5, var15);
         }
      }

      return var5;
   }

   public boolean canConsumeInputs(List<Resource> var1, List<InventoryItem> var2, boolean var3) {
      return this.consumeInputsInternal((IsoGameCharacter)null, true, var1, var2, var3);
   }

   public boolean canConsumeInputs(List<Resource> var1) {
      return this.consumeInputsInternal((IsoGameCharacter)null, true, var1, (List)null);
   }

   public boolean consumeInputs(List<Resource> var1) {
      return this.consumeInputsInternal((IsoGameCharacter)null, false, var1, (List)null);
   }

   public boolean consumeOnTickInputs(List<Resource> var1) {
      if (this.recipe == null) {
         return false;
      } else {
         return !this.recipe.hasOnTickInputs() ? true : this.consumeRecipeInputsOnTick(var1);
      }
   }

   public boolean canCreateOutputs(List<Resource> var1) {
      return this.createOutputsInternal(true, var1, (IsoGameCharacter)null);
   }

   public boolean createOutputs(List<Resource> var1) {
      return this.createOutputsInternal(false, var1, (IsoGameCharacter)null);
   }

   public boolean canCreateOutputs(List<Resource> var1, IsoGameCharacter var2) {
      return this.createOutputsInternal(true, var1, var2);
   }

   public boolean createOutputs(List<Resource> var1, IsoGameCharacter var2) {
      return this.createOutputsInternal(false, var1, var2);
   }

   public boolean createOnTickOutputs(List<Resource> var1) {
      if (this.recipe == null) {
         return false;
      } else {
         return !this.recipe.hasOnTickOutputs() ? true : this.createRecipeOutputsOnTick(var1);
      }
   }

   private boolean consumeInputsInternal(IsoGameCharacter var1, boolean var2, List<Resource> var3, List<InventoryItem> var4) {
      return this.consumeInputsInternal(var1, var2, var3, var4, false);
   }

   private boolean consumeInputsInternal(IsoGameCharacter var1, boolean var2, List<Resource> var3, List<InventoryItem> var4, boolean var5) {
      if (!var2 && GameClient.bClient) {
         throw new RuntimeException("Cannot call with testOnly==false on client.");
      } else if (this.hasConsumedInputs) {
         return true;
      } else {
         if (this.recipe != null && !this.recipe.canBeDoneInDark() && var1 instanceof IsoPlayer) {
            IsoPlayer var6 = (IsoPlayer)var1;
            if (var6.tooDarkToRead()) {
               return false;
            }
         }

         this.clearCaches();
         boolean var7 = this.consumeRecipeInputs(var2, var3, var4, var5, var1);
         if (var7) {
            this.hasConsumedInputs = !var2;
            this.hasTestedInputs = var2;
         }

         return var7;
      }
   }

   private boolean createOutputsInternal(boolean var1, List<Resource> var2, IsoGameCharacter var3) {
      if (!var1 && GameClient.bClient) {
         throw new RuntimeException("Cannot call with testOnly==false on client.");
      } else if (!var1 && !this.hasConsumedInputs) {
         if (Core.bDebug) {
            throw new RuntimeException("createOutputs requires consumeInputs to be called first");
         } else {
            return false;
         }
      } else if (var1 && !this.hasTestedInputs && !this.hasConsumedInputs) {
         if (Core.bDebug) {
            throw new RuntimeException("(test) createOutputs requires consumeInputs to be called first");
         } else {
            return false;
         }
      } else {
         boolean var4 = this.createRecipeOutputs(var1, var2, var3);
         if (!var1) {
            this.hasConsumedInputs = false;
         } else {
            this.hasTestedInputs = false;
         }

         return var4;
      }
   }

   private boolean consumeRecipeInputsOnTick(List<Resource> var1) {
      if (this.recipe == null) {
         return false;
      } else {
         this.usedResources.clear();
         boolean var3 = false;
         boolean var4 = false;

         for(int var5 = 0; var5 < this.inputs.size(); ++var5) {
            InputScriptData var2 = (InputScriptData)this.inputs.get(var5);
            if (var2.inputScript.isApplyOnTick() && var2.inputScript.getResourceType() != ResourceType.Item && (!var2.inputScript.hasFlag(InputFlag.HandcraftOnly) || this.craftMode == CraftMode.Handcraft) && (!var2.inputScript.hasFlag(InputFlag.AutomationOnly) || this.craftMode == CraftMode.Automation)) {
               var3 = false;
               if (this.allowInputResources && var1 != null && var1.size() > 0) {
                  var3 = this.consumeInputFromResources(var2.inputScript, var1, false, (CacheData)null);
               }

               if (!var3) {
                  var4 = true;
                  break;
               }
            }
         }

         return !var4;
      }
   }

   private boolean consumeRecipeInputs(boolean var1, List<Resource> var2, List<InventoryItem> var3, boolean var4, IsoGameCharacter var5) {
      if (this.recipe == null) {
         return false;
      } else {
         this.usedResources.clear();
         this.usedItems.clear();
         if (var4) {
            this.allViableResources.clear();
            this.allViableItems.clear();
         }

         if (this._m != null) {
            this._m.log("[ConsumeRecipeInputs]");
            this._m.open();
            this._m.log("test = " + var1);
            this._m.log("overrideInputItems = " + (var3 != null));
         }

         boolean var8 = false;
         boolean var9 = false;

         int var11;
         for(var11 = 0; var11 < this.inputs.size(); ++var11) {
            InputScriptData var6 = (InputScriptData)this.inputs.get(var11);
            if (this._m != null) {
               this._m.log("[" + var11 + "] input, line = \"" + var6.inputScript.getOriginalLine().trim() + "\"");
            }

            if (var6.inputScript.hasFlag(InputFlag.HandcraftOnly) && this.craftMode != CraftMode.Handcraft) {
               if (this._m != null) {
                  this._m.log("-> skipping line, 'handcraft' only");
               }
            } else if (var6.inputScript.hasFlag(InputFlag.AutomationOnly) && this.craftMode != CraftMode.Automation) {
               if (this._m != null) {
                  this._m.log("-> skipping line, 'automation' only");
               }
            } else if (var6.inputScript.isApplyOnTick()) {
               if (this._m != null) {
                  this._m.log("-> skipping line, onTick = true");
               }
            } else {
               var8 = false;
               Object var10 = var3 != null ? var3 : var6.inputItems;
               if (this.allowInputItems && ((List)var10).size() > 0 && var6.inputScript.getResourceType() == ResourceType.Item) {
                  var8 = this.consumeInputFromItems(var6.inputScript, (List)var10, var1, var6, false, var5);
                  if (var8 && this._m != null) {
                     this._m.success("consumed from supplied items list");
                  }
               }

               if (!var8 && this.allowInputResources && var2 != null && var2.size() > 0) {
                  var8 = this.consumeInputFromResources(var6.inputScript, var2, var1, var6);
               }

               var6.cachedCanConsume = var8;
               if (!var8) {
                  var9 = true;
                  if (!var4) {
                     break;
                  }
               }
            }
         }

         if (var9) {
            if (this._m != null) {
               this._m.warn("NOT CONSUMED!");
               this._m.close();
            }

            return false;
         } else {
            if (this.allowInputResources && var2 != null && var2.size() > 0) {
               for(var11 = 0; var11 < var2.size(); ++var11) {
                  Resource var7 = (Resource)var2.get(var11);
                  if (var7.getType() == ResourceType.Item && !var7.isEmpty() && !this.usedResources.contains(var7)) {
                     if (this._m != null) {
                        this._m.warn("CANCEL, not all [Resource] items could be consumed!");
                        this._m.close();
                     }

                     return false;
                  }
               }
            }

            if (this._m != null) {
               this._m.log("[ALL PASSED] returning");
               this._m.close();
            }

            return true;
         }
      }
   }

   public boolean OnTestItem(InventoryItem var1) {
      return this.recipe.OnTestItem(var1);
   }

   private boolean consumeInputFromItems(InputScript var1, List<InventoryItem> var2, boolean var3, CacheData var4, boolean var5, IsoGameCharacter var6) {
      if (var1 != null && var1.getResourceType() == ResourceType.Item) {
         if (var2.size() == 0) {
            return false;
         } else {
            float var7 = (float)var1.getIntAmount();
            if (var7 <= 0.0F) {
               return false;
            } else {
               if (var1.hasFlag(InputFlag.IsEmpty)) {
                  var7 = 0.0F;
               }

               if (var5) {
                  this.usedItems.clear();
               }

               HashSet var9 = new HashSet();
               ArrayList var10 = new ArrayList();
               ArrayList var11 = new ArrayList();
               float var12 = var7;
               List var13 = var1.getPossibleInputItems();

               for(int var14 = 0; var14 < var13.size(); ++var14) {
                  for(int var15 = 0; var15 < var2.size(); ++var15) {
                     InventoryItem var8 = (InventoryItem)var2.get(var15);
                     if (var8.getScriptItem().getFullName().equals(((Item)var13.get(var14)).getFullName()) && !this.usedItems.contains(var8) && !var9.contains(var8) && CraftRecipeManager.consumeInputItem(var1, var8, var3, var4)) {
                        var9.add(var8);
                        var10.add(var8);
                        if (!var3) {
                           var11.add(var8);
                        }

                        float var16;
                        if (var1.isItemCount()) {
                           var16 = var1.getRelativeScale(var8.getFullType());
                           var12 -= 1.0F / var16;
                        } else {
                           var16 = (float)var8.getCurrentUses();
                           float var17 = var16 / var1.getRelativeScale(var8.getFullType());
                           var16 = Math.min(var17, var16);
                           var12 -= var16;
                        }

                        if (var12 <= 1.0E-5F) {
                           this.usedItems.addAll(var9);
                           this.consumedUsedItems.addAll(var11);
                           this.allViableItems.addAll(var10);
                           return true;
                        }
                     }
                  }

                  if (var1.isExclusive()) {
                     var12 = var7;
                     var9.clear();
                     var10.clear();
                     var11.clear();
                  }
               }

               return false;
            }
         }
      } else {
         return false;
      }
   }

   private boolean consumeInputFromResources(InputScript var1, List<Resource> var2, boolean var3, CacheData var4) {
      if (var1 != null && var2 != null && var2.size() != 0) {
         if (var4 == null) {
            throw new RuntimeException("Input requires cache data.");
         } else {
            for(int var6 = 0; var6 < var2.size(); ++var6) {
               Resource var5 = (Resource)var2.get(var6);
               if (var1.getResourceType() == var5.getType() && !this.usedResources.contains(var5)) {
                  boolean var7 = CraftRecipeManager.consumeInputFromResource(var1, var5, var3, var4);
                  if (var7) {
                     this.usedResources.add(var5);
                     this.allViableResources.add(var5);
                     if (this._m != null) {
                        this._m.success("consumed by resource: " + var5.getId());
                     }

                     return true;
                  }

                  var4.softReset();
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean createRecipeOutputsOnTick(List<Resource> var1) {
      if (this.recipe == null) {
         return false;
      } else {
         this.usedResources.clear();
         boolean var2 = false;

         for(int var5 = 0; var5 < this.outputs.size(); ++var5) {
            OutputScriptData var3 = (OutputScriptData)this.outputs.get(var5);
            if (var3.outputScript.isApplyOnTick() && var3.outputScript.getResourceType() != ResourceType.Item && (!var3.outputScript.hasFlag(OutputFlag.HandcraftOnly) || this.craftMode == CraftMode.Handcraft) && (!var3.outputScript.hasFlag(OutputFlag.AutomationOnly) || this.craftMode == CraftMode.Automation)) {
               boolean var4 = false;
               if (this.allowOutputResources && var1 != null && var1.size() > 0) {
                  var4 = this.createOutputToResources(var3.outputScript, var1, false, (CacheData)null);
               }

               if (!var4) {
                  var2 = true;
                  break;
               }
            }
         }

         return !var2;
      }
   }

   private boolean createRecipeOutputs(boolean var1, List<Resource> var2, IsoGameCharacter var3) {
      if (this.recipe == null) {
         return false;
      } else {
         this.usedResources.clear();
         if (this._m != null) {
            this._m.log("[CreateRecipeOutputs]");
            this._m.open();
            this._m.log("test = " + var1);
         }

         boolean var4 = false;

         for(int var7 = 0; var7 < this.outputs.size(); ++var7) {
            OutputScriptData var5 = (OutputScriptData)this.outputs.get(var7);
            if (this._m != null) {
               this._m.log("[" + var7 + "] output, line = \"" + var5.outputScript.getOriginalLine().trim() + "\"");
            }

            if (var5.outputScript.hasFlag(OutputFlag.HandcraftOnly) && this.craftMode != CraftMode.Handcraft) {
               if (this._m != null) {
                  this._m.log("-> skipping line, 'handcraft' only");
               }
            } else if (var5.outputScript.hasFlag(OutputFlag.AutomationOnly) && this.craftMode != CraftMode.Automation) {
               if (this._m != null) {
                  this._m.log("-> skipping line, 'automation' only");
               }
            } else if (var5.outputScript.isApplyOnTick()) {
               if (this._m != null) {
                  this._m.log("-> skipping line, onTick = true");
               }
            } else {
               boolean var6 = false;
               if (var5.outputScript.getResourceType() == ResourceType.Item) {
                  var6 = this.createOutputItems(var5.outputScript, this.toOutputItems, var1, var5);
               }

               if (!var6 && this.allowOutputResources && var2 != null && var2.size() > 0) {
                  var6 = this.createOutputToResources(var5.outputScript, var2, var1, var5);
               }

               if (!var6) {
                  var4 = true;
                  break;
               }
            }
         }

         if (!var4) {
            if (this._m != null) {
               this._m.log("[Collecting keep items]");
            }

            this.collectKeepItems(this.toOutputItems, var1);
         }

         if (!var4 && this.allowOutputResources && this.toOutputItems.size() > 0 && var2 != null && var2.size() > 0) {
            if (this._m != null) {
               this._m.log("[Distribute items to outputs]");
            }

            this.distributeItemsToResources(var2, this.toOutputItems, var1);
         }

         if (!var4 && !this.allowOutputItems && this.toOutputItems.hasUnprocessed()) {
            if (this._m != null) {
               this._m.warn("FAILED: unable to offload all created items to output resources!");
            }

            var4 = true;
         }

         if (var4) {
            if (this._m != null) {
               this._m.warn("NOT CREATED!");
               this._m.close();
            }

            return false;
         } else {
            if (this._m != null) {
               this._m.log("[ALL PASSED] returning");
               this._m.close();
            }

            return true;
         }
      }
   }

   private void processKeepInputItem(InputScriptData var1, IsoGameCharacter var2, boolean var3, InventoryItem var4) {
      CraftRecipe var5 = var1.getRecipeData().getRecipe();
      if (var1.getInputScript().isKeep() && !var3) {
         DebugLog.CraftLogic.debugln("Recipe is " + var1.getRecipeData().getRecipe().getName());
         DebugLog.CraftLogic.debugln("Item is " + var4.getType());
         InputScript var6 = var1.getInputScript();
         int var7;
         if (var6.hasFlag(InputFlag.MayDegradeHeavy)) {
            var7 = 0;
            if (var2 != null) {
               var7 = var5.getHighestRelevantSkillLevel(var2) + var4.getMaintenanceMod(var2);
            }

            var4.damageCheck(var7, 1.0F, false);
         } else if (var6.hasFlag(InputFlag.MayDegrade)) {
            var7 = 0;
            if (var2 != null) {
               var7 = var5.getHighestRelevantSkillLevel(var2) + var4.getMaintenanceMod(var2);
            }

            var4.damageCheck(var7, 2.0F, false);
         } else if (var6.hasFlag(InputFlag.MayDegradeLight)) {
            var7 = 0;
            if (var2 != null) {
               var7 = var5.getHighestRelevantSkillLevel(var2) + var4.getMaintenanceMod(var2);
            }

            var4.damageCheck(var7, 3.0F, false);
         }

         if (var6.hasFlag(InputFlag.SharpnessCheck)) {
            var7 = 0;
            if (var2 != null) {
               var7 = var5.getHighestRelevantSkillLevel(var2) + var4.getMaintenanceMod(var2);
            }

            if (!var4.hasSharpness()) {
               var4.damageCheck(var7, 1.0F, false);
            } else {
               var4.sharpnessCheck(var7, 1.0F, false);
            }
         }
      }

   }

   private boolean createOutputItems(OutputScript var1, ItemDataList var2, boolean var3, CacheData var4) {
      if (var1 != null && var1.getResourceType() == ResourceType.Item) {
         if (var1.getIntAmount() <= 0) {
            return false;
         } else {
            Item var5 = var1.getItem(this);
            if (var5 == null) {
               return false;
            } else {
               int var6;
               if (var3) {
                  for(var6 = 0; var6 < var1.getIntAmount(); ++var6) {
                     var2.addItem(var5);
                  }

                  return true;
               } else {
                  for(var6 = 0; var6 < var1.getIntAmount(); ++var6) {
                     if (!CraftRecipeManager.createOutputItem(var1, var5, var3, var4)) {
                        DebugLog.CraftLogic.warn("Failed to create output item for: " + var1.getOriginalLine());
                        if (this._m != null) {
                           this._m.warn("Failed to create item: " + var5.getFullName());
                        }

                        return true;
                     }

                     if (var4.getMostRecentItem() != null) {
                        InventoryItem var7 = var4.getMostRecentItem();
                        var2.addItem(var7);
                        if (this.getFirstInputItemWithFlag("InheritColor") != null) {
                           var7.setColorRed(this.getFirstInputItemWithFlag("InheritColor").getColorRed());
                           var7.setColorGreen(this.getFirstInputItemWithFlag("InheritColor").getColorGreen());
                           var7.setColorBlue(this.getFirstInputItemWithFlag("InheritColor").getColorBlue());
                           var7.setColor(new Color(this.getFirstInputItemWithFlag("InheritColor").getColorRed(), this.getFirstInputItemWithFlag("InheritColor").getColorGreen(), this.getFirstInputItemWithFlag("InheritColor").getColorBlue()));
                           var7.setCustomColor(true);
                        }

                        if (this.getFirstInputItemWithFlag("InheritCondition") != null && !var7.hasTag("DontInheritCondition")) {
                           var7.setConditionFrom(this.getFirstInputItemWithFlag("InheritCondition"));
                           var7.setHaveBeenRepaired(this.getFirstInputItemWithFlag("InheritCondition").getHaveBeenRepaired());
                        }

                        if (this.getFirstInputItemWithFlag("IsHeadPart") != null && var7.hasHeadCondition()) {
                           var7.setHeadConditionFromCondition(this.getFirstInputItemWithFlag("IsHeadPart"));
                           if (var7.hasSharpness() && this.getFirstInputItemWithFlag("IsHeadPart").hasSharpness()) {
                              var7.setSharpnessFrom(this.getFirstInputItemWithFlag("IsHeadPart"));
                           }

                           var7.setTimesHeadRepaired(this.getFirstInputItemWithFlag("IsHeadPart").getHaveBeenRepaired());
                        }

                        if (this.getFirstInputItemWithFlag("InheritHeadCondition") != null && var7.hasHeadCondition()) {
                           var7.setConditionFromHeadCondition(this.getFirstInputItemWithFlag("InheritHeadCondition"));
                           if (var7.hasSharpness() && this.getFirstInputItemWithFlag("InheritHeadCondition").hasSharpness()) {
                              var7.setSharpnessFrom(this.getFirstInputItemWithFlag("InheritHeadCondition"));
                           }

                           var7.setTimesHeadRepaired(this.getFirstInputItemWithFlag("InheritHeadCondition").getTimesHeadRepaired());
                        }

                        if (this.getFirstInputItemWithFlag("InheritSharpness") != null && this.getFirstInputItemWithFlag("InheritSharpness").hasSharpness() && var7.hasSharpness()) {
                           var7.setSharpnessFrom(this.getFirstInputItemWithFlag("InheritSharpness"));
                        }

                        if (this.getFirstInputItemWithFlag("InheritUses") != null) {
                           var7.setCurrentUsesFrom(this.getFirstInputItemWithFlag("InheritUses"));
                        }

                        if (this.getFirstInputItemWithFlag("InheritFavorite") != null) {
                           var7.setFavorite(this.getFirstInputItemWithFlag("InheritFavorite").isFavorite());
                        }

                        if (this.getFirstInputItemWithFlag("InheritFoodAge") != null && var7.isFood()) {
                           float var8 = 0.0F;

                           for(int var9 = 0; var9 < this.getAllInputItemsWithFlag("InheritFoodAge").size(); ++var9) {
                              InventoryItem var10 = (InventoryItem)this.getAllInputItemsWithFlag("InheritFoodAge").get(var9);
                              if (var10.getAge() > var8) {
                                 var8 = var10.getAge();
                              }
                           }

                           var7.setAge(var8);
                        }

                        if (this.getFirstInputItemWithFlag("InheritAmmunition") != null && var7 instanceof HandWeapon && this.getFirstInputItemWithFlag("InheritAmmunition") instanceof HandWeapon) {
                           ((HandWeapon)var7).inheritAmmunition((HandWeapon)this.getFirstInputItemWithFlag("InheritAmmunition"));
                        }

                        if (this.getFirstInputItemWithFlag("CopyClothing") != null && var7.getClothingItem() != null && this.getFirstInputItemWithFlag("CopyClothing").getClothingItem() != null) {
                           var7.copyClothing(this.getFirstInputItemWithFlag("CopyClothing"));
                        }

                        if (this._m != null) {
                           this._m.log("created item = " + var7.getFullType());
                        }
                     }
                  }

                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   private void collectKeepItems(ItemDataList var1, boolean var2) {
      for(int var4 = 0; var4 < this.inputs.size(); ++var4) {
         InputScriptData var3 = (InputScriptData)this.inputs.get(var4);
         if (var3.inputScript.getResourceType() == ResourceType.Item && var3.isMoveToOutputs() && var3.getAppliedItemsCount() != 0) {
            for(int var5 = 0; var5 < var3.getAppliedItemsCount(); ++var5) {
               var1.addItem(var3.getAppliedItem(var5), true);
               if (this._m != null) {
                  CraftRecipeMonitor var10000 = this._m;
                  InventoryItem var10001 = var3.getAppliedItem(var5);
                  var10000.log("item = " + var10001.getFullType());
               }
            }
         }
      }

   }

   private void distributeItemsToResources(List<Resource> var1, ItemDataList var2, boolean var3) {
      if (var1 != null && var2 != null && var1.size() != 0 && var2.size() != 0) {
         if (this._m != null) {
            this._m.log("items = " + var2.size());
            this._m.log("hasUnprocessed = " + var2.hasUnprocessed());
         }

         boolean var8 = false;
         int var9 = var1.size() * 2;

         for(int var10 = 0; var10 < var9; ++var10) {
            int var11 = var10;
            if (!var2.hasUnprocessed()) {
               break;
            }

            if (var10 >= var1.size()) {
               var8 = true;
               var11 = var10 - var1.size();
            }

            Resource var4 = (Resource)var1.get(var11);
            if (var4.getType() == ResourceType.Item && !var4.isFull() && (var8 || !var4.isEmpty()) && (!var8 || var4.isEmpty())) {
               int var7 = var4.getFreeItemCapacity();
               if (this._m != null) {
                  this._m.log("testing resource '" + var4.getId() + "'");
                  this._m.log("capacity = " + var7);
               }

               for(int var12 = 0; var12 < var2.size() && var7 > 0 && var2.hasUnprocessed(); ++var12) {
                  if (!var2.isProcessed(var12)) {
                     CraftRecipeMonitor var10000;
                     String var10001;
                     if (!var3) {
                        InventoryItem var5 = var2.getInventoryItem(var12);

                        assert var5 != null;

                        if (this._m != null) {
                           this._m.log("-> testing item = " + var5.getFullType());
                        }

                        if (CraftUtil.canResourceFitItem(var4, var5)) {
                           if (this._m != null) {
                              var10000 = this._m;
                              var10001 = var5.getFullType();
                              var10000.success("-> offloaded item '" + var10001 + "' to resource: " + var4.getId());
                           }

                           var4.offerItem(var5, true, true, false);
                           var2.setProcessed(var12);
                           --var7;
                        }
                     } else {
                        Item var6 = var2.getItem(var12);

                        assert var6 != null;

                        if (this._m != null) {
                           this._m.log("-> testing item = " + var6.getFullName());
                        }

                        if (CraftUtil.canResourceFitItem(var4, var6)) {
                           if (this._m != null) {
                              var10000 = this._m;
                              var10001 = var6.getFullName();
                              var10000.success("-> offloaded item '" + var10001 + "' to resource: " + var4.getId());
                           }

                           var2.setProcessed(var12);
                           --var7;
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private boolean createOutputToResources(OutputScript var1, List<Resource> var2, boolean var3, CacheData var4) {
      if (var1 != null && var2 != null && var2.size() != 0) {
         Resource var5 = null;
         switch (var1.getResourceType()) {
            case Fluid:
               var5 = CraftUtil.findResourceOrEmpty(ResourceIO.Output, var2, (Fluid)var1.getFluid(), var1.getAmount(), (Resource)null, this.usedResources);
               break;
            case Energy:
               var5 = CraftUtil.findResourceOrEmpty(ResourceIO.Output, var2, (Energy)var1.getEnergy(), var1.getAmount(), (Resource)null, this.usedResources);
         }

         if (var5 != null) {
            boolean var6 = CraftRecipeManager.createOutputToResource(var1, var5, var3, var4);
            if (var6) {
               this.usedResources.add(var5);
               if (this._m != null) {
                  this._m.success("created by resource: " + var5.getId());
               }

               return true;
            }

            var4.softReset();
         }

         return false;
      } else {
         return false;
      }
   }

   public void save(ByteBuffer var1) throws IOException {
      ByteBlock var2 = ByteBlock.Start(var1, ByteBlock.Mode.Save);
      var1.putInt(this.inputs.size());

      for(int var4 = 0; var4 < this.inputs.size(); ++var4) {
         InputScriptData var3 = (InputScriptData)this.inputs.get(var4);
         var3.saveInputs(var1);
      }

      var1.put((byte)(this.modData != null && !this.modData.isEmpty() ? 1 : 0));
      if (this.modData != null && !this.modData.isEmpty()) {
         this.modData.save(var1);
      }

      ByteBlock.End(var1, var2);
   }

   public boolean load(ByteBuffer var1, int var2, CraftRecipe var3, boolean var4) throws IOException {
      ByteBlock var5 = ByteBlock.Start(var1, ByteBlock.Mode.Load);
      var5.safelyForceSkipOnEnd(true);
      boolean var6 = true;
      if (!var4) {
         try {
            this.setRecipe(var3);
            int var8 = var1.getInt();
            if (var8 != this.inputs.size()) {
               DebugLog.CraftLogic.warn("Recipe inputs changed or mismatch with saved data.");
               var6 = false;
            } else {
               for(int var9 = 0; var9 < var8; ++var9) {
                  InputScriptData var7 = (InputScriptData)this.inputs.get(var9);
                  var7.loadInputs(var1, var2);
               }
            }

            if (var1.get() == 1) {
               if (this.modData == null) {
                  this.modData = LuaManager.platform.newTable();
               }

               this.modData.load(var1, var2);
            }
         } catch (Exception var10) {
            var10.printStackTrace();
            this.setRecipe((CraftRecipe)null);
            var6 = false;
         }
      }

      if (var4 || !var6) {
         this.setRecipe((CraftRecipe)null);
      }

      ByteBlock.End(var1, var5);
      return var6;
   }

   public KahluaTable getModData() {
      if (this.modData == null) {
         this.modData = LuaManager.platform.newTable();
      }

      return this.modData;
   }

   public String getModelHandOne() {
      return this.getModel(true);
   }

   public String getModelHandTwo() {
      return this.getModel(false);
   }

   private String getModel(boolean var1) {
      if (this.recipe == null) {
         return null;
      } else {
         for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
            InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
            if (var2.inputScript.getResourceType() == ResourceType.Item && (var1 && var2.inputScript.isProp1() || !var1 && var2.inputScript.isProp2()) && var2.getMostRecentItem() != null) {
               return var2.getMostRecentItem().getStaticModel();
            }
         }

         if (this.recipe != null && this.recipe.getTimedActionScript() != null) {
            if (var1) {
               return this.recipe.getTimedActionScript().getProp1();
            } else {
               return this.recipe.getTimedActionScript().getProp2();
            }
         } else {
            return null;
         }
      }
   }

   public ArrayList<InventoryItem> getAllConsumedItems() {
      return this.getAllConsumedItems(new ArrayList());
   }

   public ArrayList<InventoryItem> getAllConsumedItems(ArrayList<InventoryItem> var1) {
      return this.getAllConsumedItems(var1, false);
   }

   public ArrayList<InventoryItem> getAllConsumedItems(ArrayList<InventoryItem> var1, boolean var2) {
      for(int var4 = 0; var4 < this.inputs.size(); ++var4) {
         InputScriptData var3 = (InputScriptData)this.inputs.get(var4);
         if (var3.inputScript.getResourceType() == ResourceType.Item && (!var3.inputScript.isKeep() || var2)) {
            var3.addAppliedItemsToList(var1);
         }
      }

      return var1;
   }

   public ArrayList<InventoryItem> getAllKeepInputItems() {
      return this.getAllKeepInputItems(new ArrayList());
   }

   public ArrayList<InventoryItem> getAllKeepInputItems(ArrayList<InventoryItem> var1) {
      for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
         if (var2.inputScript.getResourceType() == ResourceType.Item && var2.inputScript.isKeep()) {
            var2.addAppliedItemsToList(var1);
         }
      }

      return var1;
   }

   public ArrayList<InventoryItem> getAllInputItemsWithFlag(String var1) {
      ArrayList var2 = new ArrayList();

      for(int var4 = 0; var4 < this.inputs.size(); ++var4) {
         InputScriptData var3 = (InputScriptData)this.inputs.get(var4);
         if (var3.inputScript.getResourceType() == ResourceType.Item) {
            InputFlag var5 = InputFlag.valueOf(var1);
            if (var3.getInputScript().hasFlag(var5)) {
               var3.addAppliedItemsToList(var2);
            }
         }
      }

      return var2;
   }

   public ArrayList<InventoryItem> getInputItems(Integer var1) {
      if (this.inputs.get(var1) != null) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var1);
         if (var2.inputScript.getResourceType() == ResourceType.Item) {
            return var2.inputItems;
         }
      }

      return null;
   }

   public InventoryItem getFirstInputItemWithFlag(String var1) {
      new ArrayList();

      for(int var4 = 0; var4 < this.inputs.size(); ++var4) {
         InputScriptData var3 = (InputScriptData)this.inputs.get(var4);
         if (var3.inputScript.getResourceType() == ResourceType.Item) {
            InputFlag var5 = InputFlag.valueOf(var1);
            if (var3.getInputScript().hasFlag(var5)) {
               return var3.getFirstAppliedItem();
            }
         }
      }

      return null;
   }

   public InventoryItem getFirstInputItemWithTag(String var1) {
      for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
         if (var2.inputScript.getResourceType() == ResourceType.Item) {
            InventoryItem var4 = var2.getFirstAppliedItem();
            if (var4.hasTag(var1)) {
            }

            return var4;
         }
      }

      return null;
   }

   public ArrayList<InventoryItem> getAllInputItems() {
      ArrayList var1 = new ArrayList();

      for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
         if (var2.inputScript.getResourceType() == ResourceType.Item) {
            var2.addAppliedItemsToList(var1);
         }
      }

      return var1;
   }

   public ArrayList<InventoryItem> getAllPutBackInputItems() {
      ArrayList var1 = new ArrayList();

      for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
         if (var2.inputScript.getResourceType() == ResourceType.Item && !var2.getInputScript().dontPutBack()) {
            var2.addAppliedItemsToList(var1);
         }
      }

      return var1;
   }

   public ArrayList<InventoryItem> getAllNotKeepInputItems() {
      ArrayList var1 = new ArrayList();

      for(int var3 = 0; var3 < this.inputs.size(); ++var3) {
         InputScriptData var2 = (InputScriptData)this.inputs.get(var3);
         if (var2.inputScript.getResourceType() == ResourceType.Item && !var2.inputScript.isKeep()) {
            var2.addAppliedItemsToList(var1);
         }
      }

      return var1;
   }

   public InventoryItem getFirstCreatedItem() {
      return !this.getAllCreatedItems().isEmpty() ? (InventoryItem)this.getAllCreatedItems().get(0) : null;
   }

   public ArrayList<InventoryItem> getAllCreatedItems() {
      return this.getAllCreatedItems(new ArrayList());
   }

   public ArrayList<InventoryItem> getAllCreatedItems(ArrayList<InventoryItem> var1) {
      for(int var3 = 0; var3 < this.outputs.size(); ++var3) {
         OutputScriptData var2 = (OutputScriptData)this.outputs.get(var3);
         if (var2.outputScript.getResourceType() == ResourceType.Item) {
            var2.addAppliedItemsToList(var1);
         }
      }

      return var1;
   }

   protected int getAllViableItemsCount() {
      return this.allViableItems.size();
   }

   public InventoryItem getViableItem(int var1) {
      return (InventoryItem)this.allViableItems.get(var1);
   }

   protected int getAllViableResourcesCount() {
      return this.allViableResources.size();
   }

   public Resource getViableResource(int var1) {
      return (Resource)this.allViableResources.get(var1);
   }

   public static class InputScriptData extends CacheData {
      private static final ArrayDeque<InputScriptData> pool = new ArrayDeque();
      private CraftRecipeData recipeData;
      private InputScript inputScript;
      private final ArrayList<InventoryItem> inputItems = new ArrayList();
      private boolean cachedCanConsume = false;

      public InputScriptData() {
      }

      private static InputScriptData Alloc(CraftRecipeData var0, InputScript var1) {
         InputScriptData var2 = (InputScriptData)pool.poll();
         if (var2 == null) {
            var2 = new InputScriptData();
         }

         var2.recipeData = var0;
         var2.inputScript = var1;
         return var2;
      }

      private static void Release(InputScriptData var0) {
      }

      protected CraftRecipeData getRecipeData() {
         return this.recipeData;
      }

      private void reset() {
         this.clearCache();
         this.inputScript = null;
         this.inputItems.clear();
         this.recipeData = null;
         this.cachedCanConsume = false;
      }

      public InputScript getInputScript() {
         return this.inputScript;
      }

      public boolean isCachedCanConsume() {
         return this.cachedCanConsume;
      }

      public void getManualInputItems(ArrayList<InventoryItem> var1) {
         var1.addAll(this.inputItems);
      }

      public int getInputItemCount() {
         return this.inputItems.size();
      }

      public int getInputItemUses() {
         if (this.getInputScript().isItemCount()) {
            return this.getInputItemCount();
         } else {
            int var1 = 0;

            for(int var2 = 0; var2 < this.inputItems.size(); ++var2) {
               if (this.inputItems.get(var2) != null) {
                  var1 += ((InventoryItem)this.inputItems.get(var2)).getCurrentUses();
               }
            }

            return var1;
         }
      }

      public float getInputItemFluidUses() {
         float var1 = 0.0F;

         for(int var2 = 0; var2 < this.inputItems.size(); ++var2) {
            if (this.inputItems.get(var2) != null) {
               FluidContainer var3 = ((InventoryItem)this.inputItems.get(var2)).getFluidContainer();
               if (var3 != null) {
                  var1 += var3.getAmount();
               }
            }
         }

         return var1;
      }

      public InventoryItem getFirstInputItem() {
         return this.inputItems.size() > 0 ? (InventoryItem)this.inputItems.get(0) : null;
      }

      public InventoryItem getLastInputItem() {
         return this.inputItems.size() > 0 ? (InventoryItem)this.inputItems.get(this.inputItems.size() - 1) : null;
      }

      public boolean isInputItemsSatisfied() {
         return this.inputScript.getResourceType() == ResourceType.Item && this.inputItems.size() > 0 ? this.recipeData.consumeInputFromItems(this.inputScript, this.inputItems, true, (CacheData)null, true, (IsoGameCharacter)null) : false;
      }

      public boolean acceptsInputItem(InventoryItem var1) {
         if (this.inputScript.getResourceType() == ResourceType.Item) {
            return this.inputItems.size() > 0 && this.inputItems.contains(var1) ? false : CraftRecipeManager.consumeInputItem(this.inputScript, (InventoryItem)var1, true, (CacheData)null);
         } else {
            return false;
         }
      }

      public boolean addInputItem(InventoryItem var1) {
         if (this.inputScript.getResourceType() == ResourceType.Item) {
            if (this.acceptsInputItem(var1) && !this.isInputItemsSatisfied()) {
               this.inputItems.add(var1);
               return true;
            }
         } else {
            DebugLog.CraftLogic.warn("input script does not accept items, line=" + this.inputScript.getOriginalLine());
         }

         return false;
      }

      public boolean removeInputItem(InventoryItem var1) {
         return this.inputItems.remove(var1);
      }

      public void verifyInputItems(ArrayList<InventoryItem> var1) {
         for(int var3 = this.inputItems.size() - 1; var3 >= 0; --var3) {
            InventoryItem var2 = (InventoryItem)this.inputItems.get(var3);
            boolean var4 = false;
            if (this.inputScript.getResourceType() == ResourceType.Item) {
               var4 = CraftRecipeManager.consumeInputItem(this.inputScript, (InventoryItem)var2, true, (CacheData)null);
            }

            if (!this.inputScript.isKeep() || !var1.contains(var2) || !var4) {
               DebugLogStream var10000 = DebugLog.CraftLogic;
               String var10001 = var2.getFullType();
               var10000.println(" :: REMOVING ITEM: " + var10001 + " [0]=" + (!this.inputScript.isKeep() || !this.inputScript.isTool()) + ", [1]=" + !var1.contains(var2) + ", [2]=" + !var4);
               this.inputItems.remove(var3);
            }
         }

      }
   }

   public static class OutputScriptData extends CacheData {
      private static final ArrayDeque<OutputScriptData> pool = new ArrayDeque();
      private CraftRecipeData recipeData;
      private OutputScript outputScript;

      public OutputScriptData() {
      }

      private static OutputScriptData Alloc(CraftRecipeData var0, OutputScript var1) {
         OutputScriptData var2 = (OutputScriptData)pool.poll();
         if (var2 == null) {
            var2 = new OutputScriptData();
         }

         var2.recipeData = var0;
         var2.outputScript = var1;
         return var2;
      }

      private static void Release(OutputScriptData var0) {
      }

      protected CraftRecipeData getRecipeData() {
         return this.recipeData;
      }

      public OutputScript getOutputScript() {
         return this.outputScript;
      }
   }

   public abstract static class CacheData {
      protected InventoryItem mostRecentItem;
      private final ArrayList<InventoryItem> appliedItems = new ArrayList();
      private boolean moveToOutputs = false;
      protected float usesConsumed = 0.0F;
      protected float fluidConsumed = 0.0F;
      protected float energyConsumed = 0.0F;
      protected FluidSample fluidSample = FluidSample.Alloc();
      protected FluidConsume fluidConsume = FluidConsume.Alloc();
      protected float usesCreated = 0.0F;
      protected float fluidCreated = 0.0F;
      protected float energyCreated = 0.0F;

      public CacheData() {
      }

      protected void addAppliedItem(InventoryItem var1) {
         assert !this.appliedItems.contains(var1) : "Item already added to applied list.";

         this.appliedItems.add(var1);
         this.mostRecentItem = var1;
      }

      public int getAppliedItemsCount() {
         return this.appliedItems.size();
      }

      public InventoryItem getMostRecentItem() {
         return this.mostRecentItem;
      }

      protected void setMostRecentItemNull() {
         this.mostRecentItem = null;
      }

      public InventoryItem getAppliedItem(int var1) {
         return (InventoryItem)this.appliedItems.get(var1);
      }

      public InventoryItem getFirstAppliedItem() {
         return (InventoryItem)this.appliedItems.get(0);
      }

      public void addAppliedItemsToList(ArrayList<InventoryItem> var1) {
         var1.addAll(this.appliedItems);
      }

      protected abstract CraftRecipeData getRecipeData();

      protected void clearCache() {
         this.moveToOutputs = false;
         this.mostRecentItem = null;
         this.appliedItems.clear();
         this.usesConsumed = 0.0F;
         this.fluidConsumed = 0.0F;
         this.energyConsumed = 0.0F;
         this.fluidSample.clear();
         this.fluidConsume.clear();
         this.usesCreated = 0.0F;
         this.fluidCreated = 0.0F;
         this.energyCreated = 0.0F;
      }

      public boolean isMoveToOutputs() {
         return this.moveToOutputs;
      }

      public void setMoveToOutputs(boolean var1) {
         this.moveToOutputs = var1;
      }

      protected void softReset() {
         this.softResetInput();
         this.softResetOutput();
      }

      protected void softResetInput() {
         this.mostRecentItem = null;
         if (this.appliedItems.size() > 0) {
            this.appliedItems.clear();
         }

         this.fluidSample.clear();
         this.fluidConsume.clear();
         this.usesConsumed = 0.0F;
         this.fluidConsumed = 0.0F;
         this.energyConsumed = 0.0F;
      }

      protected void softResetOutput() {
         if (this.appliedItems.size() > 0) {
            this.appliedItems.clear();
         }

         this.usesCreated = 0.0F;
         this.fluidCreated = 0.0F;
         this.energyCreated = 0.0F;
      }

      protected void saveInputs(ByteBuffer var1) throws IOException {
         var1.put((byte)(this.moveToOutputs ? 1 : 0));
         var1.putFloat(this.usesConsumed);
         var1.putFloat(this.fluidConsumed);
         var1.putFloat(this.energyConsumed);
         FluidSample.Save(this.fluidSample, var1);
         FluidConsume.Save(this.fluidConsume, var1);
         if (this.appliedItems.size() == 1) {
            CompressIdenticalItems.save(var1, (InventoryItem)this.appliedItems.get(0));
         } else {
            CompressIdenticalItems.save(var1, this.appliedItems, (IsoGameCharacter)null);
         }

      }

      protected void loadInputs(ByteBuffer var1, int var2) throws IOException {
         this.moveToOutputs = var1.get() == 1;
         this.usesConsumed = var1.getFloat();
         this.fluidConsumed = var1.getFloat();
         this.energyConsumed = var1.getFloat();
         FluidSample.Load(this.fluidSample, var1, var2);
         FluidConsume.Load(this.fluidConsume, var1, var2);
         this.appliedItems.clear();
         CompressIdenticalItems.load(var1, var2, this.appliedItems, (ArrayList)null);
      }
   }
}
