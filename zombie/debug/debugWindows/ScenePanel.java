package zombie.debug.debugWindows;

import gnu.trove.list.array.TIntArrayList;
import imgui.ImColor;
import imgui.ImGui;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Stats;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.Nutrition;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.TraitFactory;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.debug.BaseDebugWindow;
import zombie.debug.BooleanDebugOption;
import zombie.debug.DebugContext;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.vehicles.BaseVehicle;

public class ScenePanel extends BaseDebugWindow {
   private String selectedNode = "";
   private int defaultflags = 192;
   private int selectedflags;
   private static final HashMap<String, String> filterMap = new HashMap();
   private final TIntArrayList zTracker;
   private boolean zVisibleFilter;
   private float zDistanceFilter;
   private ObjectTreeSort zSort;
   private final TIntArrayList aTracker;
   private boolean aVisibleFilter;
   private float aDistanceFilter;
   private ObjectTreeSort aSort;
   private float vDistanceFilter;
   private ObjectTreeSort vSort;

   public ScenePanel() {
      this.selectedflags = this.defaultflags | 1;
      this.zTracker = new TIntArrayList();
      this.zVisibleFilter = false;
      this.zDistanceFilter = 100.0F;
      this.zSort = ScenePanel.ObjectTreeSort.None;
      this.aTracker = new TIntArrayList();
      this.aVisibleFilter = false;
      this.aDistanceFilter = 100.0F;
      this.aSort = ScenePanel.ObjectTreeSort.None;
      this.vDistanceFilter = 100.0F;
      this.vSort = ScenePanel.ObjectTreeSort.None;
   }

   public String getTitle() {
      return "Scene";
   }

   public int getWindowFlags() {
      return 64;
   }

   boolean doTreeNode(String var1, String var2, boolean var3) {
      boolean var4 = var1.equals(this.selectedNode);
      int var5 = var4 ? this.selectedflags : this.defaultflags;
      if (var3) {
         var5 |= 256;
      }

      boolean var6 = ImGui.treeNodeEx(var1, var5, var2);
      if (ImGui.isItemClicked()) {
         this.selectedNode = var1;
      }

      return var6;
   }

   boolean doTreeNode(String var1, String var2, boolean var3, boolean var4) {
      boolean var5 = var1.equals(this.selectedNode);
      int var6 = var5 ? this.selectedflags : this.defaultflags;
      if (var3) {
         var6 |= 256;
      }

      var6 |= 32;
      boolean var7 = ImGui.treeNodeEx(var1, var6, var2);
      if (ImGui.isItemClicked()) {
         this.selectedNode = var1;
      }

      return var7;
   }

   protected void doWindowContents() {
      if (this.doTreeNode("optionsTree", "Settings", false)) {
         if (this.doTreeNode("debugOptionsTree", "Debug", false)) {
            this.updateFilter("options");

            for(int var1 = 0; var1 < DebugOptions.instance.getOptionCount(); ++var1) {
               BooleanDebugOption var2 = DebugOptions.instance.getOptionByIndex(var1);
               if (!this.isFiltered("options", var2.getName())) {
                  String var10000 = var2.getName();
                  Objects.requireNonNull(var2);
                  Supplier var10001 = var2::getValue;
                  Objects.requireNonNull(var2);
                  PZImGui.checkboxWithDefaultValueHighlight(var10000, var10001, var2::setValue, var2.getDefaultValue(), ImColor.rgb(255, 255, 0));
               }
            }

            ImGui.treePop();
         }

         if (this.doTreeNode("logOptionsTree", "Logging", false)) {
            this.updateFilter("logging");
            ArrayList var3 = DebugLog.getDebugTypes();

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               if (!this.isFiltered("logging", ((DebugType)var3.get(var4)).name())) {
                  Wrappers.checkbox(((DebugType)var3.get(var4)).name(), DebugLog::isEnabled, DebugLog::setLogEnabled, (DebugType)var3.get(var4));
               }
            }

            ImGui.treePop();
         }

         ImGui.treePop();
      }

      if (IsoWorld.instance.CurrentCell != null && this.doTreeNode("worldTree", "IsoWorld", false, true)) {
         if (this.doTreeNode("metaWorldTree", "IsoMetaWorld", false, true)) {
            ImGui.treePop();
         }

         if (this.doTreeNode("playerTree", "IsoPlayers", false, true)) {
            this.doAllPlayers();
            ImGui.treePop();
         }

         if (this.doTreeNode("zombieTree", "IsoZombies", false)) {
            this.doAllZombies((ArrayList)IsoWorld.instance.CurrentCell.getZombieList().clone());
            ImGui.treePop();
         }

         if (this.doTreeNode("animalTree", "IsoAnimal", false)) {
            this.doAllAnimals((ArrayList)IsoWorld.instance.CurrentCell.getObjectList().stream().filter((var0) -> {
               return var0 instanceof IsoAnimal;
            }).map((var0) -> {
               return (IsoAnimal)var0;
            }).collect(Collectors.toCollection(ArrayList::new)));
            ImGui.treePop();
         }

         if (this.doTreeNode("vehicleTree", "BaseVehicle", false)) {
            this.doAllVehicles((ArrayList)IsoWorld.instance.CurrentCell.vehicles.clone());
            ImGui.treePop();
         }

         if (this.doTreeNode("cellTree", "IsoCell", false, true)) {
            ImGui.treePop();
         }

         ImGui.treePop();
      }

   }

   private void highlight(IsoMovingObject var1) {
      var1.setAlphaAndTarget(IsoPlayer.getInstance().getPlayerNum(), 1.0F);
      var1.setOutlineHighlightCol(Core.getInstance().getBadHighlitedColor());
      var1.setOutlineHighlight(IsoPlayer.getInstance().getPlayerNum(), true);
   }

   private void doAllPlayers() {
      for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
         if (IsoPlayer.getPlayers().get(var1) != null) {
            ImGui.beginGroup();
            this.doPlayer("player" + var1, (IsoPlayer)IsoPlayer.getPlayers().get(var1));
            ImGui.endGroup();
            if (ImGui.isItemHovered(416)) {
               this.highlight((IsoMovingObject)IsoPlayer.getPlayers().get(var1));
            }
         }
      }

   }

   private void doPlayer(String var1, IsoPlayer var2) {
      if (this.doTreeNode(var1, var2.getDescriptor().getFullname(), false)) {
         this.doPopupMenu(var1, var2);
         Supplier var10001;
         if (this.doTreeNode(var1 + "Cheats", "Cheats", false)) {
            Objects.requireNonNull(var2);
            var10001 = var2::isInvisible;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Invisible", var10001, var2::setInvisible);
            Objects.requireNonNull(var2);
            var10001 = var2::isGodMod;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("God Mode", var10001, var2::setGodMod);
            Objects.requireNonNull(var2);
            var10001 = var2::isGhostMode;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Ghost Mode", var10001, var2::setGhostMode);
            Objects.requireNonNull(var2);
            var10001 = var2::isNoClip;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("No Clip", var10001, var2::setNoClip);
            Objects.requireNonNull(var2);
            var10001 = var2::isTimedActionInstantCheat;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Timed Actions", var10001, var2::setTimedActionInstantCheat);
            Objects.requireNonNull(var2);
            var10001 = var2::isUnlimitedCarry;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Unlimited Carry", var10001, var2::setUnlimitedCarry);
            Objects.requireNonNull(var2);
            var10001 = var2::isUnlimitedEndurance;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Unlimited Endurance", var10001, var2::setUnlimitedEndurance);
            DebugOptions var12 = DebugOptions.instance;
            Objects.requireNonNull(var12);
            Wrappers.SupplyConsumer var14 = var12::getBoolean;
            DebugOptions var10002 = DebugOptions.instance;
            Objects.requireNonNull(var10002);
            Wrappers.checkbox("Unlimited Ammo", var14, var10002::setBoolean, "Cheat.Player.UnlimitedAmmo");
            var12 = DebugOptions.instance;
            Objects.requireNonNull(var12);
            var14 = var12::getBoolean;
            var10002 = DebugOptions.instance;
            Objects.requireNonNull(var10002);
            Wrappers.checkbox("Recipes", var14, var10002::setBoolean, "Cheat.Recipe.KnowAll");
            Objects.requireNonNull(var2);
            var10001 = var2::isBuildCheat;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Build", var10001, var2::setBuildCheat);
            Objects.requireNonNull(var2);
            var10001 = var2::isFarmingCheat;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Farming", var10001, var2::setFarmingCheat);
            Objects.requireNonNull(var2);
            var10001 = var2::isFishingCheat;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Fishing", var10001, var2::setFishingCheat);
            Objects.requireNonNull(var2);
            var10001 = var2::isHealthCheat;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Health", var10001, var2::setHealthCheat);
            Objects.requireNonNull(var2);
            var10001 = var2::isMechanicsCheat;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Mechanics", var10001, var2::setMechanicsCheat);
            Objects.requireNonNull(var2);
            var10001 = var2::isMovablesCheat;
            Objects.requireNonNull(var2);
            Wrappers.checkbox("Movables", var10001, var2::setMovablesCheat);
            ImGui.treePop();
         }

         if (this.doTreeNode(var1 + "Moodles", "Moodles", false)) {
            Stats var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            Supplier var13 = var10003::getHunger;
            Stats var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Hunger", 0.0F, 1.0F, var13, var10004::setHunger);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getThirst;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Thirst", 0.0F, 1.0F, var13, var10004::setThirst);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getFatigue;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Fatigue", 0.0F, 1.0F, var13, var10004::setFatigue);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getEndurance;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Endurance", 0.0F, 1.0F, var13, var10004::setEndurance);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getFitness;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Fitness", -1.0F, 1.0F, var13, var10004::setFitness);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getDrunkenness;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Drunkenness", 0.0F, 100.0F, var13, var10004::setDrunkenness);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getAnger;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Anger", 0.0F, 1.0F, var13, var10004::setAnger);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getFear;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Fear", 0.0F, 1.0F, var13, var10004::setFear);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getPain;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Pain", 0.0F, 1.0F, var13, var10004::setPain);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getPanic;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Panic", 0.0F, 1.0F, var13, var10004::setPanic);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getMorale;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Morale", 0.0F, 1.0F, var13, var10004::setMorale);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getStress;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Stress", 0.0F, 1.0F, var13, var10004::setStress);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getStressFromCigarettes;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Stress (cigarettes)", 0.0F, 1.0F, var13, var10004::setStressFromCigarettes);
            Objects.requireNonNull(var2);
            var13 = var2::getTimeSinceLastSmoke;
            Objects.requireNonNull(var2);
            Wrappers.sliderFloat("Time since last smoke", 0.0F, 10.0F, var13, var2::setTimeSinceLastSmoke);
            BodyDamage var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getBoredomLevel;
            BodyDamage var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Boredom", 0.0F, 100.0F, var13, var16::setBoredomLevel);
            var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getUnhappynessLevel;
            var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Unhappiness", 0.0F, 100.0F, var13, var16::setUnhappynessLevel);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getSanity;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Sanity", 0.0F, 1.0F, var13, var10004::setSanity);
            var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getWetness;
            var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Wetness", 0.0F, 100.0F, var13, var16::setWetness);
            var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getTemperature;
            var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Temperature", 20.0F, 40.0F, var13, var16::setTemperature);
            var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getColdDamageStage;
            var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Cold Damage (hypo 4)", 0.0F, 1.0F, var13, var16::setColdDamageStage);
            var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getOverallBodyHealth;
            var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Overall Health", 0.0F, 100.0F, var13, var16::setOverallBodyHealth);
            var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getColdStrength;
            var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Catch Cold Strength", 0.0F, 100.0F, var13, var16::setColdStrength);
            var10003 = var2.getStats();
            Objects.requireNonNull(var10003);
            var13 = var10003::getSickness;
            var10004 = var2.getStats();
            Objects.requireNonNull(var10004);
            Wrappers.sliderFloat("Sickness", 0.0F, 1.0F, var13, var10004::setSickness);
            var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getInfectionLevel;
            var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Infection", 0.0F, 100.0F, var13, var16::setInfectionLevel);
            BodyDamage var15 = var2.getBodyDamage();
            Objects.requireNonNull(var15);
            var10001 = var15::IsInfected;
            BodyDamage var11 = var2.getBodyDamage();
            Objects.requireNonNull(var11);
            Wrappers.checkbox("Is Infected", var10001, var11::setInfected);
            var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getFakeInfectionLevel;
            var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Fake Infection", 0.0F, 100.0F, var13, var16::setFakeInfectionLevel);
            var15 = var2.getBodyDamage();
            Objects.requireNonNull(var15);
            var10001 = var15::IsFakeInfected;
            var11 = var2.getBodyDamage();
            Objects.requireNonNull(var11);
            Wrappers.checkbox("Is Fake Infected", var10001, var11::setIsFakeInfected);
            var17 = var2.getBodyDamage();
            Objects.requireNonNull(var17);
            var13 = var17::getFoodSicknessLevel;
            var16 = var2.getBodyDamage();
            Objects.requireNonNull(var16);
            Wrappers.sliderFloat("Food Sickness", 0.0F, 100.0F, var13, var16::setFoodSicknessLevel);
            Nutrition var19 = var2.getNutrition();
            Objects.requireNonNull(var19);
            var13 = var19::getCalories;
            Nutrition var18 = var2.getNutrition();
            Objects.requireNonNull(var18);
            Wrappers.sliderFloat("Calories", -2200.0F, 3700.0F, var13, var18::setCalories);
            var19 = var2.getNutrition();
            Objects.requireNonNull(var19);
            var13 = var19::getWeight;
            var18 = var2.getNutrition();
            Objects.requireNonNull(var18);
            Wrappers.sliderDouble("Weight", 30.0, 130.0, var13, var18::setWeight);
            ImGui.treePop();
         }

         if (this.doTreeNode(var1 + "Traits", "Traits", false)) {
            this.updateFilter("traits");
            Iterator var3 = TraitFactory.TraitMap.values().iterator();

            label74:
            while(true) {
               TraitFactory.Trait var4;
               do {
                  if (!var3.hasNext()) {
                     ImGui.treePop();
                     break label74;
                  }

                  var4 = (TraitFactory.Trait)var3.next();
               } while(this.isFiltered("traits", var4.name));

               boolean var5 = false;

               for(int var6 = 0; var6 < var4.MutuallyExclusive.size(); ++var6) {
                  if (var2.getTraits().contains((String)var4.MutuallyExclusive.get(var6))) {
                     var5 = true;
                     break;
                  }
               }

               ImGui.beginDisabled(var5);
               boolean var10 = var2.getTraits().contains(var4.traitID);
               boolean var7 = Wrappers.checkbox(var4.name + (var4.prof ? " (Non-selectable)" : ""), var10);
               if (var7 != var10) {
                  if (var7) {
                     var2.getTraits().add(var4.traitID);
                  } else {
                     var2.getTraits().remove(var4.traitID);
                  }
               }

               ImGui.endDisabled();
            }
         }

         if (this.doTreeNode(var1 + "Perks", "Perks", false)) {
            for(int var8 = 0; var8 < PerkFactory.PerkList.size(); ++var8) {
               PerkFactory.Perk var9 = (PerkFactory.Perk)PerkFactory.PerkList.get(var8);
               if (var9.getParent() != PerkFactory.Perks.None) {
                  String var10000 = var9.name;
                  Objects.requireNonNull(var2);
                  Wrappers.SupplyConsumer var20 = var2::getPerkLevel;
                  Objects.requireNonNull(var2);
                  Wrappers.sliderInt(var10000, 0, 10, var20, var2::setPerkLevelDebug, var9);
               }
            }

            ImGui.treePop();
         }

         this.doAnimTable(var1 + "Anim", var2);
         ImGui.treePop();
      } else {
         this.doPopupMenu(var1, var2);
      }

   }

   private void doAllZombies(ArrayList<IsoZombie> var1) {
      this.zVisibleFilter = Wrappers.checkbox("Visible Only", this.zVisibleFilter);
      this.zDistanceFilter = Wrappers.sliderFloat("Range", this.zDistanceFilter, 1.0F, 100.0F);
      if (ImGui.isItemHovered() || ImGui.isItemFocused()) {
         LineDrawer.DrawIsoCircle(IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ(), this.zDistanceFilter, 32, 0.0F, 1.0F, 0.0F, 0.3F);
      }

      this.zSort = this.doSortCombobox(this.zSort);
      if (this.zSort != ScenePanel.ObjectTreeSort.None) {
         var1.sort(this.zSort.comparator);
      }

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (!this.zTracker.contains(((IsoZombie)var1.get(var2)).ZombieID)) {
            if (this.zVisibleFilter && !IsoPlayer.getInstance().getSpottedList().contains(var1.get(var2)) || PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq((IsoMovingObject)var1.get(var2))) >= this.zDistanceFilter) {
               continue;
            }
         } else {
            this.highlight((IsoMovingObject)var1.get(var2));
         }

         ImGui.beginGroup();
         this.doZombie("zombie" + ((IsoZombie)var1.get(var2)).ZombieID, (IsoZombie)var1.get(var2));
         ImGui.endGroup();
         if (ImGui.isItemHovered(416)) {
            this.highlight((IsoMovingObject)var1.get(var2));
         }
      }

   }

   private void doZombie(String var1, IsoZombie var2) {
      if (this.doTreeNode(var1, String.valueOf(var2.ZombieID), false)) {
         this.doPopupMenu(var1, var2);
         boolean var3 = this.zTracker.contains(var2.ZombieID);
         if (Wrappers.checkbox("Track", var3) != var3) {
            if (var3) {
               this.zTracker.remove(var2.ZombieID);
            } else {
               this.zTracker.add(var2.ZombieID);
            }
         }

         if (ImGui.beginTable(var1 + "Table", 2, 0)) {
            this.doTextRow("X:", String.valueOf(var2.getX()));
            this.doTextRow("Y:", String.valueOf(var2.getY()));
            this.doTextRow("Distance:", String.valueOf(PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq(var2))));
            this.doTextRow("Outfit:", var2.getOutfitName());
            ImGui.tableNextRow();
            ImGui.tableSetColumnIndex(0);
            ImGui.textColored(1.0F, 0.0F, 0.0F, 1.0F, "Health:");
            ImGui.tableSetColumnIndex(1);
            Objects.requireNonNull(var2);
            Supplier var10004 = var2::getHealth;
            Objects.requireNonNull(var2);
            Wrappers.dragFloat("", 0.0F, 100.0F, 0.005F, var10004, var2::setHealth);
            ImGui.endTable();
         }

         this.doAnimTable(var1 + "Anim", var2);
         ImGui.treePop();
      } else {
         this.doPopupMenu(var1, var2);
      }

   }

   private ObjectTreeSort doSortCombobox(ObjectTreeSort var1) {
      if (ImGui.beginCombo("Sort", var1.name(), 0)) {
         for(int var2 = 0; var2 < ScenePanel.ObjectTreeSort.values.length; ++var2) {
            if (Wrappers.selectable(ScenePanel.ObjectTreeSort.values[var2].name(), var1 == ScenePanel.ObjectTreeSort.values[var2])) {
               ImGui.setItemDefaultFocus();
               var1 = ScenePanel.ObjectTreeSort.values[var2];
            }
         }

         ImGui.endCombo();
      }

      return var1;
   }

   private void doAllAnimals(ArrayList<IsoAnimal> var1) {
      this.aVisibleFilter = Wrappers.checkbox("Visible Only", this.aVisibleFilter);
      this.aDistanceFilter = Wrappers.sliderFloat("Range", this.aDistanceFilter, 1.0F, 100.0F);
      if (ImGui.isItemHovered() || ImGui.isItemFocused()) {
         LineDrawer.DrawIsoCircle(IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ(), this.aDistanceFilter, 32, 0.0F, 1.0F, 0.0F, 0.3F);
      }

      this.aSort = this.doSortCombobox(this.aSort);
      if (this.aSort != ScenePanel.ObjectTreeSort.None) {
         var1.sort(this.aSort.comparator);
      }

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (!this.aTracker.contains(((IsoAnimal)var1.get(var2)).animalID)) {
            if (this.aVisibleFilter && !IsoPlayer.getInstance().getSpottedList().contains(var1.get(var2)) || PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq((IsoMovingObject)var1.get(var2))) >= this.aDistanceFilter) {
               continue;
            }
         } else {
            this.highlight((IsoMovingObject)var1.get(var2));
         }

         ImGui.beginGroup();
         this.doAnimal("animal" + ((IsoAnimal)var1.get(var2)).animalID, (IsoAnimal)var1.get(var2));
         ImGui.endGroup();
         if (ImGui.isItemHovered(416)) {
            this.highlight((IsoMovingObject)var1.get(var2));
         }
      }

   }

   private void doAnimal(String var1, IsoAnimal var2) {
      if (this.doTreeNode(var1, var2.animalID + " " + var2.getAnimalType(), false)) {
         this.doPopupMenu(var1, var2);
         boolean var3 = this.aTracker.contains(var2.animalID);
         if (Wrappers.checkbox("Track", var3) != var3) {
            if (var3) {
               this.aTracker.remove(var2.animalID);
            } else {
               this.aTracker.add(var2.animalID);
            }
         }

         if (ImGui.beginTable(var1 + "Table", 2, 0)) {
            this.doTextRow("X:", String.valueOf(var2.getX()));
            this.doTextRow("Y:", String.valueOf(var2.getY()));
            this.doTextRow("Distance:", String.valueOf(PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq(var2))));
            ImGui.tableNextRow();
            ImGui.tableSetColumnIndex(0);
            ImGui.textColored(1.0F, 0.0F, 0.0F, 1.0F, "Health:");
            ImGui.tableSetColumnIndex(1);
            Objects.requireNonNull(var2);
            Supplier var10004 = var2::getHealth;
            Objects.requireNonNull(var2);
            Wrappers.dragFloat("", 0.0F, 100.0F, 0.005F, var10004, var2::setHealth);
            ImGui.endTable();
         }

         this.doAnimTable(var1 + "Anim", var2);
         ImGui.treePop();
      } else {
         this.doPopupMenu(var1, var2);
      }

   }

   private void doAllVehicles(ArrayList<BaseVehicle> var1) {
      for(int var2 = 0; var2 < var1.size(); ++var2) {
         ImGui.beginGroup();
         this.doVehicle("vehicle" + ((BaseVehicle)var1.get(var2)).VehicleID, (BaseVehicle)var1.get(var2));
         ImGui.endGroup();
      }

   }

   private void doVehicle(String var1, BaseVehicle var2) {
      if (this.doTreeNode(var1, var2.VehicleID + " " + var2.getScriptName(), false)) {
         this.doPopupMenu(var1, var2);
         if (ImGui.beginTable(var1 + "Table", 2, 0)) {
            this.doTextRow("X:", String.valueOf(var2.getX()));
            this.doTextRow("Y:", String.valueOf(var2.getY()));
            this.doTextRow("Distance:", String.valueOf(PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq(var2))));
            this.doTextRow("Type:", var2.getVehicleType());
            ImGui.endTable();
         }

         ImGui.treePop();
      } else {
         this.doPopupMenu(var1, var2);
      }

   }

   private void doAnimTable(String var1, IsoGameCharacter var2) {
      if (var2.advancedAnimator != null) {
         if (this.doTreeNode(var1, "Animation Data", false)) {
            ImGui.textWrapped(var2.advancedAnimator.getRootLayer().GetDebugString());
            if (ImGui.beginTable(var1 + "Table", 2, 1280)) {
               this.doTextRow("State:", var2.getCurrentState().getName());
               Iterator var3 = var2.getGameVariables().iterator();

               while(var3.hasNext()) {
                  IAnimationVariableSlot var4 = (IAnimationVariableSlot)var3.next();
                  this.doTextRow(var4.getKey() + ":", var4.getValueString());
               }

               ImGui.endTable();
            }

            ImGui.treePop();
         }

      }
   }

   private void doTextRow(String var1, String var2) {
      if (var1 == null) {
         var1 = "null";
      }

      if (var2 == null) {
         var2 = "null";
      }

      ImGui.tableNextRow();
      ImGui.tableSetColumnIndex(0);
      ImGui.textColored(1.0F, 0.0F, 0.0F, 1.0F, var1);
      ImGui.tableSetColumnIndex(1);
      ImGui.text(var2);
   }

   static String doTextFilter(String var0) {
      Wrappers.valueString.set(var0);
      ImGui.inputText("Filter", Wrappers.valueString);
      return Wrappers.valueString.get();
   }

   private void updateFilter(String var1) {
      filterMap.put(var1, doTextFilter((String)filterMap.get(var1)));
   }

   private boolean isFiltered(String var1, String var2) {
      return !((String)filterMap.getOrDefault(var1, "")).isBlank() && !var2.toLowerCase().contains(((String)filterMap.get(var1)).toLowerCase());
   }

   private void doPopupMenu(String var1, Object var2) {
      if (ImGui.beginPopupContextItem()) {
         if (ImGui.selectable("inspect class")) {
            DebugContext.instance.inspectJava(var2);
         }

         ImGui.endPopup();
      }

   }

   static {
      filterMap.put("options", "");
      filterMap.put("logging", "");
      filterMap.put("traits", "");
   }

   static enum ObjectTreeSort {
      None((var0, var1) -> {
         return 0;
      }),
      Distance((var0, var1) -> {
         return Float.compare(IsoPlayer.getInstance().getDistanceSq(var0), IsoPlayer.getInstance().getDistanceSq(var1));
      }),
      ID((var0, var1) -> {
         if (var0 instanceof IsoZombie) {
            return Integer.compare(((IsoZombie)var0).ZombieID, ((IsoZombie)var1).ZombieID);
         } else if (var0 instanceof IsoAnimal) {
            return Integer.compare(((IsoAnimal)var0).animalID, ((IsoAnimal)var1).animalID);
         } else {
            return var0 instanceof IsoPlayer ? Integer.compare(((IsoPlayer)var0).getPlayerNum(), ((IsoPlayer)var1).getPlayerNum()) : Integer.compare(var0.getID(), var1.getID());
         }
      });

      private static final ObjectTreeSort[] values = values();
      final Comparator<IsoMovingObject> comparator;

      private ObjectTreeSort(Comparator var3) {
         this.comparator = var3;
      }
   }
}
