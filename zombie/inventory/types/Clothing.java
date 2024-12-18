package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.Lua.LuaEventManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.WornItems.WornItem;
import zombie.characters.WornItems.WornItems;
import zombie.characters.skills.PerkFactory;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

public class Clothing extends InventoryItem {
   private float temperature;
   private float insulation = 0.0F;
   private float windresistance = 0.0F;
   private float waterResistance = 0.0F;
   HashMap<Integer, ClothingPatch> patches;
   protected String SpriteName = null;
   protected String palette;
   public float bloodLevel = 0.0F;
   private float dirtyness = 0.0F;
   private float wetness = 0.0F;
   private float WeightWet = 0.0F;
   private float lastWetnessUpdate = -1.0F;
   private final String dirtyString = Translator.getText("IGUI_ClothingName_Dirty");
   private final String bloodyString = Translator.getText("IGUI_ClothingName_Bloody");
   private final String wetString = Translator.getText("IGUI_ClothingName_Wet");
   private final String soakedString = Translator.getText("IGUI_ClothingName_Soaked");
   private final String wornString = Translator.getText("IGUI_ClothingName_Worn");
   private String brokenString = Translator.getText("Tooltip_broken");
   private int ConditionLowerChance = 10;
   private float stompPower = 1.0F;
   private float runSpeedModifier = 1.0F;
   private float combatSpeedModifier = 1.0F;
   private Boolean removeOnBroken = false;
   private Boolean canHaveHoles = true;
   private float biteDefense = 0.0F;
   private float scratchDefense = 0.0F;
   private float bulletDefense = 0.0F;
   public static final int CONDITION_PER_HOLES = 3;
   private float neckProtectionModifier = 1.0F;
   private int chanceToFall = 0;

   public String getCategory() {
      return this.mainCategory != null ? this.mainCategory : "Clothing";
   }

   public Clothing(String var1, String var2, String var3, String var4, String var5, String var6) {
      super(var1, var2, var3, var4);
      this.SpriteName = var6;
      this.col = new Color(Rand.Next(255), Rand.Next(255), Rand.Next(255));
      this.palette = var5;
      this.lastWetnessUpdate = (float)GameTime.getInstance().getWorldAgeHours();
   }

   public Clothing(String var1, String var2, String var3, Item var4, String var5, String var6) {
      super(var1, var2, var3, var4);
      this.SpriteName = var6;
      this.col = new Color(Rand.Next(255), Rand.Next(255), Rand.Next(255));
      this.palette = var5;
      this.lastWetnessUpdate = (float)GameTime.getInstance().getWorldAgeHours();
   }

   public boolean IsClothing() {
      return true;
   }

   public int getSaveType() {
      return Item.Type.Clothing.ordinal();
   }

   public void Unwear() {
      this.Unwear(false);
   }

   public void Unwear(boolean var1) {
      if (this.isWorn()) {
         if (this.container != null && this.container.parent instanceof IsoGameCharacter) {
            IsoGameCharacter var2 = (IsoGameCharacter)this.container.parent;
            var2.removeWornItem(this);
            if (var2 instanceof IsoPlayer) {
               LuaEventManager.triggerEvent("OnClothingUpdated", var2);
            }

            if (var1 && var2.getSquare() != null && var2.getVehicle() == null) {
               var2.getInventory().Remove((InventoryItem)this);
               var2.getSquare().AddWorldInventoryItem((InventoryItem)this, (float)(Rand.Next(100) / 100), (float)(Rand.Next(100) / 100), 0.0F);
               LuaEventManager.triggerEvent("OnContainerUpdate");
            }

            IsoWorld.instance.CurrentCell.addToProcessItemsRemove((InventoryItem)this);
         }

      }
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      ColorInfo var4 = new ColorInfo();
      ColorInfo var5 = new ColorInfo();
      float var6 = 1.0F;
      float var7 = 1.0F;
      float var8 = 0.8F;
      float var9 = 1.0F;
      ObjectTooltip.LayoutItem var3;
      float var10;
      if (!this.isCosmetic()) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_weapon_Condition") + ":", var6, var7, var8, var9);
         var10 = (float)this.Condition / (float)this.ConditionMax;
         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var10, var4);
         var3.setProgress(var10, var4.getR(), var4.getG(), var4.getB(), 1.0F);
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_item_Insulation") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
         var10 = this.getInsulation();
         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var10, var4);
         var3.setProgress(var10, var4.getR(), var4.getG(), var4.getB(), 1.0F);
         var10 = this.getWindresistance();
         if (var10 > 0.0F) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_item_Windresist") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var10, var4);
            var3.setProgress(var10, var4.getR(), var4.getG(), var4.getB(), 1.0F);
         }

         var10 = this.getWaterResistance();
         if (var10 > 0.0F) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_item_Waterresist") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var10, var4);
            var3.setProgress(var10, var4.getR(), var4.getG(), var4.getB(), 1.0F);
         }
      }

      if (this.bloodLevel != 0.0F) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_clothing_bloody") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var10 = this.bloodLevel / 100.0F;
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), var10, var5);
         var3.setProgress(var10, var5.getR(), var5.getG(), var5.getB(), 1.0F);
      }

      if (this.dirtyness >= 1.0F) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_clothing_dirty") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var10 = this.dirtyness / 100.0F;
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), var10, var5);
         var3.setProgress(var10, var5.getR(), var5.getG(), var5.getB(), 1.0F);
      }

      if (this.wetness != 0.0F) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_clothing_wet") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var10 = this.wetness / 100.0F;
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), var10, var5);
         var3.setProgress(var10, var5.getR(), var5.getG(), var5.getB(), 1.0F);
      }

      int var11 = 0;
      ItemVisual var12 = this.getVisual();

      int var13;
      for(var13 = 0; var13 < BloodBodyPartType.MAX.index(); ++var13) {
         if (var12.getHole(BloodBodyPartType.FromIndex(var13)) > 0.0F) {
            ++var11;
         }
      }

      if (var11 > 0) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_clothing_holes") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValueRightNoPlus(var11);
      }

      float var14;
      if (!this.isEquipped() && var1.getCharacter() != null) {
         float var20 = 0.0F;
         var14 = 0.0F;
         float var15 = 0.0F;
         WornItems var16 = var1.getCharacter().getWornItems();

         for(int var17 = 0; var17 < var16.size(); ++var17) {
            WornItem var18 = var16.get(var17);
            if (var18.getItem().IsClothing() && var18.getLocation() != null && (this.getBodyLocation().equals(var18.getLocation()) || var16.getBodyLocationGroup().isExclusive(this.getBodyLocation(), var18.getLocation()))) {
               var20 += ((Clothing)var18.getItem()).getBiteDefense();
               var14 += ((Clothing)var18.getItem()).getScratchDefense();
               var15 += ((Clothing)var18.getItem()).getBulletDefense();
            }
         }

         float var22 = this.getBiteDefense();
         if (var22 != var20) {
            var3 = var2.addItem();
            if (var22 > 0.0F || var20 > 0.0F) {
               var3.setLabel(Translator.getText("Tooltip_BiteDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               if (var22 > var20) {
                  var3.setValue((int)var22 + " (+" + (int)(var22 - var20) + ")", Core.getInstance().getGoodHighlitedColor().getR(), Core.getInstance().getGoodHighlitedColor().getG(), Core.getInstance().getGoodHighlitedColor().getB(), 1.0F);
               } else {
                  var3.setValue((int)var22 + " (-" + (int)(var20 - var22) + ")", Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0F);
               }
            }
         } else if (this.getBiteDefense() != 0.0F) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_BiteDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var3.setValueRightNoPlus((int)this.getBiteDefense());
         }

         float var23 = this.getScratchDefense();
         if (var23 != var14) {
            var3 = var2.addItem();
            if (var23 > 0.0F || var14 > 0.0F) {
               var3.setLabel(Translator.getText("Tooltip_ScratchDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               if (var23 > var14) {
                  var3.setValue((int)var23 + " (+" + (int)(var23 - var14) + ")", Core.getInstance().getGoodHighlitedColor().getR(), Core.getInstance().getGoodHighlitedColor().getG(), Core.getInstance().getGoodHighlitedColor().getB(), 1.0F);
               } else {
                  var3.setValue((int)var23 + " (-" + (int)(var14 - var23) + ")", Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0F);
               }
            }
         } else if (this.getScratchDefense() != 0.0F) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_ScratchDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var3.setValueRightNoPlus((int)this.getScratchDefense());
         }

         float var19 = this.getBulletDefense();
         if (var19 != var15) {
            var3 = var2.addItem();
            if (var19 > 0.0F || var15 > 0.0F) {
               var3.setLabel(Translator.getText("Tooltip_BulletDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               if (var19 > var15) {
                  var3.setValue((int)var19 + " (+" + (int)(var19 - var15) + ")", Core.getInstance().getGoodHighlitedColor().getR(), Core.getInstance().getGoodHighlitedColor().getG(), Core.getInstance().getGoodHighlitedColor().getB(), 1.0F);
               } else {
                  var3.setValue((int)var19 + " (-" + (int)(var15 - var19) + ")", Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0F);
               }
            }
         } else if (this.getBulletDefense() != 0.0F) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_BulletDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var3.setValueRightNoPlus((int)this.getBulletDefense());
         }
      } else {
         if (this.getBiteDefense() != 0.0F) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_BiteDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var3.setValueRightNoPlus((int)this.getBiteDefense());
         }

         if (this.getScratchDefense() != 0.0F) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_ScratchDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var3.setValueRightNoPlus((int)this.getScratchDefense());
         }

         if (this.getBulletDefense() != 0.0F) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_BulletDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var3.setValueRightNoPlus((int)this.getBulletDefense());
         }
      }

      String var21;
      if (this.hasTag("GasMask")) {
         if (this.hasFilter()) {
            var21 = ScriptManager.instance.getItem(this.getFilterType()).getDisplayName();
            var3 = var2.addItem();
            var3.setLabel(Translator.getText(var21) + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var14 = this.getUsedDelta();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var14, var4);
            var3.setProgress(var14, var4.getR(), var4.getG(), var4.getB(), 1.0F);
         } else {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_NoFilter"), 1.0F, 1.0F, 0.8F, 1.0F);
         }
      } else if (this.hasTag("GasMaskNoFilter")) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_NoFilter"), 1.0F, 1.0F, 0.8F, 1.0F);
      }

      if (this.hasTag("SCBA")) {
         if (this.hasTank()) {
            var21 = ScriptManager.instance.getItem(this.getTankType()).getDisplayName();
            var3 = var2.addItem();
            var3.setLabel(Translator.getText(var21) + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var14 = this.getUsedDelta();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var14, var4);
            var3.setProgress(var14, var4.getR(), var4.getG(), var4.getB(), 1.0F);
         } else {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_NoTank"), 1.0F, 1.0F, 0.8F, 1.0F);
         }
      } else if (this.hasTag("SCBANoTank")) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_NoTank"), 1.0F, 1.0F, 0.8F, 1.0F);
      }

      if (this.getRunSpeedModifier() != 1.0F) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_RunSpeedModifier") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), this.getDiscomfortModifier(), var4);
         var3.setProgress(1.0F - this.getRunSpeedModifier(), var4.getR(), var4.getG(), var4.getB(), 1.0F);
      }

      if (this.getCombatSpeedModifier() != 1.0F) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_CombatSpeedModifier") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), this.getDiscomfortModifier(), var4);
         var3.setProgress(1.0F - this.getCombatSpeedModifier(), var4.getR(), var4.getG(), var4.getB(), 1.0F);
      }

      if (Core.bDebug && DebugOptions.instance.TooltipInfo.getValue()) {
         if (this.bloodLevel != 0.0F) {
            var3 = var2.addItem();
            var3.setLabel("DBG: bloodLevel:", 1.0F, 1.0F, 0.8F, 1.0F);
            var13 = (int)Math.ceil((double)this.bloodLevel);
            var3.setValueRight(var13, false);
         }

         if (this.dirtyness != 0.0F) {
            var3 = var2.addItem();
            var3.setLabel("DBG: dirtyness:", 1.0F, 1.0F, 0.8F, 1.0F);
            var13 = (int)Math.ceil((double)this.dirtyness);
            var3.setValueRight(var13, false);
         }

         if (this.wetness != 0.0F) {
            var3 = var2.addItem();
            var3.setLabel("DBG: wetness:", 1.0F, 1.0F, 0.8F, 1.0F);
            var13 = (int)Math.ceil((double)this.wetness);
            var3.setValueRight(var13, false);
         }
      }

   }

   public boolean isDirty() {
      return this.dirtyness > 15.0F;
   }

   public boolean isBloody() {
      return this.getBloodlevel() > 25.0F;
   }

   public String getName() {
      String var1 = "";
      if (this.isDirty()) {
         var1 = var1 + this.dirtyString + ", ";
      }

      if (this.isBloody()) {
         var1 = var1 + this.bloodyString + ", ";
      }

      if (this.getWetness() >= 100.0F) {
         var1 = var1 + this.soakedString + ", ";
      } else if (this.getWetness() > 25.0F) {
         var1 = var1 + this.wetString + ", ";
      }

      if (this.isBroken()) {
         var1 = var1 + this.brokenString + ", ";
      } else if ((float)this.getCondition() < (float)this.getConditionMax() / 3.0F) {
         var1 = var1 + this.wornString + ", ";
      }

      if (var1.length() > 2) {
         var1 = var1.substring(0, var1.length() - 2);
      }

      var1 = var1.trim();
      if (this.getFluidContainer() != null) {
         return this.getFluidContainer().getUiName();
      } else {
         return var1.isEmpty() ? this.name : Translator.getText("IGUI_ClothingNaming", var1, this.name);
      }
   }

   public void update() {
      if (this.isActivated() && !this.isWorn()) {
         this.setActivated(false);
      }

      if (this.container == null || SandboxOptions.instance.ClothingDegradation.getValue() == 1) {
         ;
      }
   }

   public void updateWetness() {
      this.updateWetness(false);
   }

   public void updateWetness(boolean var1) {
      if (var1 || !this.isEquipped()) {
         if (this.getBloodClothingType() == null) {
            this.setWetness(0.0F);
         } else {
            float var2 = (float)GameTime.getInstance().getWorldAgeHours();
            if (this.lastWetnessUpdate < 0.0F) {
               this.lastWetnessUpdate = var2;
            } else if (this.lastWetnessUpdate > var2) {
               this.lastWetnessUpdate = var2;
            }

            float var3 = var2 - this.lastWetnessUpdate;
            if (!(var3 < 0.016666668F)) {
               this.lastWetnessUpdate = var2;
               if (this.hasTag("BreakWhenWet") && this.getWetness() >= 100.0F) {
                  this.setCondition(0);
               }

               float var4;
               switch (this.getWetDryState()) {
                  case Invalid:
                  default:
                     break;
                  case Dryer:
                     if (this.getWetness() > 0.0F) {
                        var4 = var3 * 20.0F;
                        if (this.isEquipped()) {
                           var4 *= 2.0F;
                        }

                        this.setWetness(this.getWetness() - var4);
                     }
                     break;
                  case Wetter:
                     if (this.getWetness() < 100.0F) {
                        var4 = ClimateManager.getInstance().getRainIntensity();
                        if (var4 < 0.1F) {
                           var4 = 0.0F;
                        }

                        float var5 = var4 * var3 * 100.0F;
                        this.setWetness(this.getWetness() + var5);
                     }
               }

            }
         }
      }
   }

   public float getBulletDefense() {
      return this.getCondition() <= 0 ? 0.0F : this.bulletDefense;
   }

   public void setBulletDefense(float var1) {
      this.bulletDefense = Math.min(var1, 100.0F);
   }

   private WetDryState getWetDryState() {
      if (this.getWorldItem() != null) {
         if (this.getWorldItem().getSquare() == null) {
            return Clothing.WetDryState.Invalid;
         } else if (this.getWorldItem().getSquare().isInARoom()) {
            return Clothing.WetDryState.Dryer;
         } else {
            return ClimateManager.getInstance().isRaining() ? Clothing.WetDryState.Wetter : Clothing.WetDryState.Dryer;
         }
      } else if (this.container == null) {
         return Clothing.WetDryState.Invalid;
      } else if (this.container.parent instanceof IsoDeadBody) {
         IsoDeadBody var6 = (IsoDeadBody)this.container.parent;
         if (var6.getSquare() == null) {
            return Clothing.WetDryState.Invalid;
         } else if (var6.getSquare().isInARoom()) {
            return Clothing.WetDryState.Dryer;
         } else {
            return ClimateManager.getInstance().isRaining() ? Clothing.WetDryState.Wetter : Clothing.WetDryState.Dryer;
         }
      } else if (this.container.parent instanceof IsoGameCharacter) {
         IsoGameCharacter var5 = (IsoGameCharacter)this.container.parent;
         if (var5.getCurrentSquare() == null) {
            return Clothing.WetDryState.Invalid;
         } else if (!var5.getCurrentSquare().isInARoom() && !var5.getCurrentSquare().haveRoof) {
            if (ClimateManager.getInstance().isRaining()) {
               if (!this.isEquipped()) {
                  return Clothing.WetDryState.Dryer;
               } else if ((var5.isAsleep() || var5.isResting()) && var5.getBed() != null && (var5.getBed().isTent() || "Tent".equalsIgnoreCase(var5.getBed().getName()) || "Shelter".equalsIgnoreCase(var5.getBed().getName()))) {
                  return Clothing.WetDryState.Dryer;
               } else {
                  BaseVehicle var2 = var5.getVehicle();
                  if (var2 != null && var2.hasRoof(var2.getSeat(var5))) {
                     VehiclePart var3 = var2.getPartById("Windshield");
                     if (var3 != null) {
                        VehicleWindow var4 = var3.getWindow();
                        if (var4 != null && var4.isHittable()) {
                           return Clothing.WetDryState.Dryer;
                        }
                     }
                  }

                  return Clothing.WetDryState.Wetter;
               }
            } else {
               return Clothing.WetDryState.Dryer;
            }
         } else {
            return Clothing.WetDryState.Dryer;
         }
      } else if (this.container.parent == null) {
         return Clothing.WetDryState.Dryer;
      } else if (this.container.parent instanceof IsoClothingDryer && ((IsoClothingDryer)this.container.parent).isActivated()) {
         return Clothing.WetDryState.Invalid;
      } else if (this.container.parent instanceof IsoClothingWasher && ((IsoClothingWasher)this.container.parent).isActivated()) {
         return Clothing.WetDryState.Invalid;
      } else {
         IsoCombinationWasherDryer var1 = (IsoCombinationWasherDryer)Type.tryCastTo(this.container.parent, IsoCombinationWasherDryer.class);
         return var1 != null && var1.isActivated() ? Clothing.WetDryState.Invalid : Clothing.WetDryState.Dryer;
      }
   }

   public void flushWetness() {
      if (!(this.lastWetnessUpdate < 0.0F)) {
         this.updateWetness(true);
         this.lastWetnessUpdate = -1.0F;
      }
   }

   public boolean finishupdate() {
      if (this.container != null && this.container.parent instanceof IsoGameCharacter) {
         return !this.isEquipped();
      } else {
         return true;
      }
   }

   public void Use(boolean var1, boolean var2) {
      if (this.uses <= 1) {
         this.Unwear();
      }

      super.Use(var1, var2, false);
   }

   public boolean CanStack(InventoryItem var1) {
      return this.ModDataMatches(var1) && this.palette == null && ((Clothing)var1).palette == null || this.palette.equals(((Clothing)var1).palette);
   }

   public static Clothing CreateFromSprite(String var0) {
      try {
         Clothing var1 = null;
         var1 = (Clothing)InventoryItemFactory.CreateItem(var0, 1.0F);
         return var1;
      } catch (Exception var2) {
         return null;
      }
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      BitHeaderWrite var3 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      if (this.getSpriteName() != null) {
         var3.addFlags(1);
         GameWindow.WriteString(var1, this.getSpriteName());
      }

      if (this.dirtyness != 0.0F) {
         var3.addFlags(2);
         var1.putFloat(this.dirtyness);
      }

      if (this.bloodLevel != 0.0F) {
         var3.addFlags(4);
         var1.putFloat(this.bloodLevel);
      }

      if (this.wetness != 0.0F) {
         var3.addFlags(8);
         var1.putFloat(this.wetness);
      }

      if (this.lastWetnessUpdate != 0.0F) {
         var3.addFlags(16);
         var1.putFloat(this.lastWetnessUpdate);
      }

      if (this.patches != null) {
         var3.addFlags(32);
         var1.put((byte)this.patches.size());
         Iterator var4 = this.patches.keySet().iterator();

         while(var4.hasNext()) {
            int var5 = (Integer)var4.next();
            var1.put((byte)var5);
            ((ClothingPatch)this.patches.get(var5)).save(var1, false);
         }
      }

      var3.write();
      var3.release();
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      if (!var3.equals(0)) {
         if (var3.hasFlags(1)) {
            this.setSpriteName(GameWindow.ReadString(var1));
         }

         if (var3.hasFlags(2)) {
            this.dirtyness = var1.getFloat();
         }

         if (var3.hasFlags(4)) {
            this.bloodLevel = var1.getFloat();
         }

         if (var3.hasFlags(8)) {
            this.wetness = var1.getFloat();
         }

         if (var3.hasFlags(16)) {
            this.lastWetnessUpdate = var1.getFloat();
         }

         if (var3.hasFlags(32)) {
            byte var4 = var1.get();

            for(int var5 = 0; var5 < var4; ++var5) {
               byte var6 = var1.get();
               ClothingPatch var7 = new ClothingPatch();
               var7.load(var1, var2);
               if (this.patches == null) {
                  this.patches = new HashMap();
               }

               this.patches.put(Integer.valueOf(var6), var7);
            }
         }
      }

      var3.release();
      this.synchWithVisual();
      this.lastWetnessUpdate = (float)GameTime.getInstance().getWorldAgeHours();
   }

   public String getSpriteName() {
      return this.SpriteName;
   }

   public void setSpriteName(String var1) {
      this.SpriteName = var1;
   }

   public String getPalette() {
      return this.palette == null ? "Trousers_White" : this.palette;
   }

   public void setPalette(String var1) {
      this.palette = var1;
   }

   public float getTemperature() {
      return this.temperature;
   }

   public void setTemperature(float var1) {
      this.temperature = var1;
   }

   public void setDirtyness(float var1) {
      this.dirtyness = PZMath.clamp(var1, 0.0F, 100.0F);
   }

   public void setBloodLevel(float var1) {
      this.bloodLevel = PZMath.clamp(var1, 0.0F, 100.0F);
   }

   public float getDirtyness() {
      return this.dirtyness;
   }

   public float getBloodlevel() {
      return this.bloodLevel;
   }

   public float getBloodlevelForPart(BloodBodyPartType var1) {
      return this.getVisual().getBlood(var1);
   }

   public float getBloodLevel() {
      return this.bloodLevel;
   }

   public float getBloodLevelForPart(BloodBodyPartType var1) {
      return this.getVisual().getBlood(var1);
   }

   public float getWeight() {
      float var1 = this.getActualWeight();
      float var2 = this.getWeightWet();
      if (var2 <= 0.0F) {
         var2 = var1 * 1.25F;
      }

      return PZMath.lerp(var1, var2, this.getWetness() / 100.0F);
   }

   public void setWetness(float var1) {
      this.wetness = PZMath.clamp(var1, 0.0F, 100.0F);
   }

   public float getWetness() {
      return this.wetness;
   }

   public float getWeightWet() {
      return this.WeightWet;
   }

   public void setWeightWet(float var1) {
      this.WeightWet = var1;
   }

   public int getConditionLowerChance() {
      return this.ConditionLowerChance;
   }

   public void setConditionLowerChance(int var1) {
      this.ConditionLowerChance = var1;
   }

   public void setCondition(int var1) {
      this.setCondition(var1, true);
      if (var1 <= 0) {
         this.setBroken(true);
         if (this.isWorn()) {
            this.Unwear(true);
         }

         if (this.getContainer() != null) {
            this.getContainer().setDrawDirty(true);
         }

         if (this.isRemoveOnBroken() && this.getContainer() != null) {
            this.container.Remove((InventoryItem)this);
         }
      }

   }

   public float getClothingDirtynessIncreaseLevel() {
      if (SandboxOptions.instance.ClothingDegradation.getValue() == 2) {
         return 2.5E-4F;
      } else {
         return SandboxOptions.instance.ClothingDegradation.getValue() == 4 ? 0.025F : 0.0025F;
      }
   }

   public float getInsulation() {
      return this.insulation;
   }

   public void setInsulation(float var1) {
      this.insulation = var1;
   }

   public float getStompPower() {
      return this.stompPower;
   }

   public void setStompPower(float var1) {
      this.stompPower = var1;
   }

   public float getRunSpeedModifier() {
      return this.runSpeedModifier;
   }

   public void setRunSpeedModifier(float var1) {
      this.runSpeedModifier = var1;
   }

   public float getCombatSpeedModifier() {
      return this.combatSpeedModifier;
   }

   public void setCombatSpeedModifier(float var1) {
      this.combatSpeedModifier = var1;
   }

   public Boolean isRemoveOnBroken() {
      return this.removeOnBroken;
   }

   public void setRemoveOnBroken(Boolean var1) {
      this.removeOnBroken = var1;
   }

   public Boolean getCanHaveHoles() {
      return this.canHaveHoles;
   }

   public void setCanHaveHoles(Boolean var1) {
      this.canHaveHoles = var1;
   }

   public boolean isCosmetic() {
      return this.getScriptItem().isCosmetic();
   }

   public String toString() {
      String var10000 = this.getClass().getSimpleName();
      return var10000 + "{ clothingItemName=\"" + this.getClothingItemName() + "\" }";
   }

   public float getBiteDefense() {
      return this.getCondition() <= 0 ? 0.0F : this.biteDefense;
   }

   public void setBiteDefense(float var1) {
      this.biteDefense = Math.min(var1, 100.0F);
   }

   public float getScratchDefense() {
      return this.getCondition() <= 0 ? 0.0F : this.scratchDefense;
   }

   public void setScratchDefense(float var1) {
      this.scratchDefense = Math.min(var1, 100.0F);
   }

   public float getNeckProtectionModifier() {
      return this.neckProtectionModifier;
   }

   public void setNeckProtectionModifier(float var1) {
      this.neckProtectionModifier = var1;
   }

   public int getChanceToFall() {
      return this.chanceToFall;
   }

   public void setChanceToFall(int var1) {
      this.chanceToFall = var1;
   }

   public float getWindresistance() {
      return this.windresistance;
   }

   public void setWindresistance(float var1) {
      this.windresistance = var1;
   }

   public float getWaterResistance() {
      return this.waterResistance;
   }

   public void setWaterResistance(float var1) {
      this.waterResistance = var1;
   }

   public int getHolesNumber() {
      return this.getVisual() != null ? this.getVisual().getHolesNumber() : 0;
   }

   public int getPatchesNumber() {
      return this.patches == null ? 0 : this.patches.size();
   }

   public float getDefForPart(BloodBodyPartType var1, boolean var2, boolean var3) {
      if (this.getVisual().getHole(var1) > 0.0F) {
         return 0.0F;
      } else {
         ClothingPatch var4 = this.getPatchType(var1);
         float var5 = this.getScratchDefense();
         if (var2) {
            var5 = this.getBiteDefense();
         }

         if (var3) {
            var5 = this.getBulletDefense();
         }

         if (var1 == BloodBodyPartType.Neck && this.getScriptItem().neckProtectionModifier < 1.0F) {
            var5 *= this.getScriptItem().neckProtectionModifier;
         }

         if (var4 != null) {
            int var6 = var4.scratchDefense;
            if (var2) {
               var6 = var4.biteDefense;
            }

            if (var3) {
               var6 = var4.biteDefense;
            }

            if (!var4.hasHole) {
               var5 += (float)var6;
            } else {
               var5 = (float)var6;
            }
         }

         return var5;
      }
   }

   public static int getBiteDefenseFromItem(IsoGameCharacter var0, InventoryItem var1) {
      int var2 = Math.max(1, var0.getPerkLevel(PerkFactory.Perks.Tailoring));
      ClothingPatchFabricType var3 = Clothing.ClothingPatchFabricType.fromType(var1.getFabricType());
      return var3.maxBiteDef > 0 ? (int)Math.max(1.0F, (float)var3.maxBiteDef * ((float)var2 / 10.0F)) : 0;
   }

   public static int getScratchDefenseFromItem(IsoGameCharacter var0, InventoryItem var1) {
      int var2 = Math.max(1, var0.getPerkLevel(PerkFactory.Perks.Tailoring));
      ClothingPatchFabricType var3 = Clothing.ClothingPatchFabricType.fromType(var1.getFabricType());
      return (int)Math.max(1.0F, (float)var3.maxScratchDef * ((float)var2 / 10.0F));
   }

   public ClothingPatch getPatchType(BloodBodyPartType var1) {
      return this.patches != null ? (ClothingPatch)this.patches.get(var1.index()) : null;
   }

   public void removePatch(BloodBodyPartType var1) {
      if (this.patches != null) {
         this.getVisual().removePatch(var1.index());
         ClothingPatch var2 = (ClothingPatch)this.patches.get(var1.index());
         if (var2 != null && var2.hasHole) {
            this.getVisual().setHole(var1);
            this.setCondition(this.getCondition() - var2.conditionGain, false);
         }

         this.patches.remove(var1.index());
         if (GameServer.bServer && this.getContainer() != null && this.getContainer().getParent() instanceof IsoPlayer) {
            INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this.getContainer().getParent());
         }

      }
   }

   public boolean canFullyRestore(IsoGameCharacter var1, BloodBodyPartType var2, InventoryItem var3) {
      return var1.getPerkLevel(PerkFactory.Perks.Tailoring) > 7 && var3.getFabricType().equals(this.getFabricType()) && this.getVisual().getHole(var2) > 0.0F;
   }

   public void fullyRestore() {
      this.setCondition(this.getConditionMax());
      this.setDirtyness(0.0F);
      this.setBloodLevel(0.0F);

      for(int var1 = 0; var1 < BloodBodyPartType.MAX.index(); ++var1) {
         BloodBodyPartType var2 = BloodBodyPartType.FromIndex(var1);
         if (this.patches != null) {
            this.getVisual().removePatch(var1);
            this.patches.remove(var2.index());
         }

         if (this.getVisual().getHole(var2) != 0.0F) {
            this.getVisual().removeHole(var1);
         }

         this.getVisual().setBlood(var2, 0.0F);
      }

      if (GameServer.bServer) {
         INetworkPacket.sendToAll(PacketTypes.PacketType.SyncClothing, (UdpConnection)null, this.getOwner());
      }

   }

   public void addPatchForClient(int var1, int var2, int var3, boolean var4) {
      if (this.patches == null) {
         this.patches = new HashMap();
      }

      ClothingPatch var5 = new ClothingPatch(var2, var3, var4);
      this.patches.put(var1, var5);
   }

   public void addPatch(IsoGameCharacter var1, BloodBodyPartType var2, InventoryItem var3) {
      ClothingPatchFabricType var4 = Clothing.ClothingPatchFabricType.fromType(var3.getFabricType());
      if (this.canFullyRestore(var1, var2, var3)) {
         this.getVisual().removeHole(var2.index());
         this.setCondition((int)((float)this.getCondition() + this.getCondLossPerHole()), false);
      } else {
         if (var4 == Clothing.ClothingPatchFabricType.Cotton) {
            this.getVisual().setBasicPatch(var2);
         } else if (var4 == Clothing.ClothingPatchFabricType.Denim) {
            this.getVisual().setDenimPatch(var2);
         } else {
            this.getVisual().setLeatherPatch(var2);
         }

         if (this.patches == null) {
            this.patches = new HashMap();
         }

         int var5 = Math.max(1, var1.getPerkLevel(PerkFactory.Perks.Tailoring));
         float var6 = this.getVisual().getHole(var2);
         float var7 = this.getCondLossPerHole();
         if (var5 < 3) {
            var7 -= 2.0F;
         } else if (var5 < 6) {
            --var7;
         }

         ClothingPatch var8 = new ClothingPatch(var5, var4.index, var6 > 0.0F);
         if (var6 > 0.0F) {
            var7 = Math.max(1.0F, var7);
            this.setCondition((int)((float)this.getCondition() + var7), false);
            var8.conditionGain = (int)var7;
         }

         this.patches.put(var2.index(), var8);
         this.getVisual().removeHole(var2.index());
         if (GameServer.bServer && var1 instanceof IsoPlayer) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.SyncClothing, (UdpConnection)null, var1);
         }

      }
   }

   public ArrayList<BloodBodyPartType> getCoveredParts() {
      ArrayList var1 = this.getScriptItem().getBloodClothingType();
      return BloodClothingType.getCoveredParts(var1);
   }

   public int getNbrOfCoveredParts() {
      ArrayList var1 = this.getScriptItem().getBloodClothingType();
      return BloodClothingType.getCoveredPartCount(var1);
   }

   public float getCondLossPerHole() {
      int var2 = this.getNbrOfCoveredParts();
      float var1 = (float)PZMath.max(1, this.getConditionMax() / var2);
      return var1;
   }

   public void copyPatchesTo(Clothing var1) {
      var1.patches = this.patches;
   }

   public String getClothingExtraSubmenu() {
      return this.ScriptItem.clothingExtraSubmenu;
   }

   public boolean canBe3DRender() {
      if (!StringUtils.isNullOrEmpty(this.getWorldStaticItem())) {
         return true;
      } else {
         return "Bip01_Head".equalsIgnoreCase(this.getClothingItem().m_AttachBone) && (!this.isCosmetic() || "Eyes".equals(this.getBodyLocation()));
      }
   }

   public boolean isWorn() {
      return this.container != null && this.container.parent instanceof IsoGameCharacter && ((IsoGameCharacter)this.container.parent).getWornItems().contains(this);
   }

   public void addRandomHole() {
      if (this.getCanHaveHoles() && this.getCoveredParts() != null) {
         ArrayList var1 = this.getCoveredParts();
         BloodBodyPartType var2 = (BloodBodyPartType)var1.get(Rand.Next(var1.size()));
         int var3 = 0;
         if (this.getVisual().getHole(var2) <= 0.0F) {
            this.getVisual().setHole(var2);
            ++var3;
         }

         this.setCondition(this.getCondition() - (int)((float)var3 * this.getCondLossPerHole()), false);
      }
   }

   public void addRandomDirt() {
      if (this.getCoveredParts() != null) {
         ArrayList var1 = this.getCoveredParts();
         BloodBodyPartType var2 = (BloodBodyPartType)var1.get(Rand.Next(var1.size()));
         int var3 = Rand.Next(100) + 1;
         float var4 = (float)var3 / 100.0F;
         this.getVisual().setDirt(var2, var4);
         BloodClothingType.calcTotalDirtLevel(this);
      }
   }

   public void addRandomBlood() {
      if (this.getCoveredParts() != null) {
         ArrayList var1 = this.getCoveredParts();
         BloodBodyPartType var2 = (BloodBodyPartType)var1.get(Rand.Next(var1.size()));
         int var3 = Rand.Next(100) + 1;
         float var4 = (float)var3 / 100.0F;
         this.getVisual().setBlood(var2, var4);
         BloodClothingType.calcTotalBloodLevel(this);
      }
   }

   public void randomizeCondition(int var1, int var2, int var3, int var4) {
      if (!this.isCosmetic()) {
         if (Rand.Next(100) < var1) {
            this.setWetness((float)Rand.Next(0, 100));
         }

         int var5 = this.getNbrOfCoveredParts();
         if (var5 >= 1) {
            int var6;
            if (Rand.Next(100) < var2) {
               for(var6 = 0; var6 < var5; ++var6) {
                  this.addRandomDirt();
               }
            }

            if (Rand.Next(100) < var3) {
               for(var6 = 0; var6 < var5; ++var6) {
                  this.addRandomBlood();
               }
            }

            if (Rand.Next(100) < var4) {
               if (this.getCanHaveHoles()) {
                  var6 = Rand.Next(var5 + 1);

                  for(int var7 = 0; var7 < var6; ++var7) {
                     this.addRandomHole();
                  }
               } else {
                  this.setCondition(Rand.Next(this.getCondition()) + 1, false);
               }
            }
         }
      }

   }

   public boolean hasFilter() {
      if (this.hasTag("GasMask") && this.getModData().rawget("filterType") != null && this.getModData().rawget("filterType") != "none") {
         return this.getFilterType() != null;
      } else {
         return false;
      }
   }

   public void setNoFilter() {
      if (this.hasTag("GasMask")) {
         this.getModData().rawset("filterType", (Object)null);
      }
   }

   public String getFilterType() {
      return this.getModData().rawget("filterType") != null && this.getModData().rawget("filterType") != "none" ? (String)this.getModData().rawget("filterType") : null;
   }

   public void setFilterType(String var1) {
      this.getModData().rawset("filterType", var1);
   }

   public boolean hasTank() {
      if (this.hasTag("SCBA") && this.getModData().rawget("tankType") != null && this.getModData().rawget("tankType") != "none") {
         return this.getTankType() != null;
      } else {
         return false;
      }
   }

   public void setNoTank() {
      if (this.hasTag("SCBA")) {
         this.getModData().rawset("tankType", (Object)null);
      }
   }

   public String getTankType() {
      return this.getModData().rawget("tankType") != null && this.getModData().rawget("tankType") != "none" ? (String)this.getModData().rawget("tankType") : null;
   }

   public void setTankType(String var1) {
      this.getModData().rawset("tankType", var1);
   }

   public float getUsedDelta() {
      if (this.getModData().rawget("usedDelta") == null) {
         this.setUsedDelta(0.0F);
      }

      return (float)(Double)this.getModData().rawget("usedDelta");
   }

   public void setUsedDelta(float var1) {
      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      this.getModData().rawset("usedDelta", (double)var1);
   }

   public float getUseDelta() {
      return this.getScriptItem().getUseDelta();
   }

   public void drainGasMask() {
      this.drainGasMask(1.0F);
   }

   public void drainGasMask(float var1) {
      if (this.hasTag("GasMask") && this.hasFilter() && !(this.getUsedDelta() <= 0.0F)) {
         float var2 = var1 * ScriptManager.instance.getItem(this.getFilterType()).getUseDelta() * GameTime.getInstance().getMultiplier();
         this.setUsedDelta(this.getUsedDelta() - var2);
         if (this.getUsedDelta() < 0.0F) {
            this.setUsedDelta(0.0F);
         }

      }
   }

   public void drainSCBA() {
      if (this.getUsedDelta() <= 0.0F) {
         this.setActivated(false);
      }

      if (this.hasTag("SCBA") && this.hasTank() && this.isActivated() && !(this.getUsedDelta() <= 0.0F)) {
         float var1 = 0.001F;
         float var2 = var1 * ScriptManager.instance.getItem(this.getTankType()).getUseDelta() * GameTime.getInstance().getMultiplier();
         this.setUsedDelta(this.getUsedDelta() - var2);
         if (this.getUsedDelta() < 0.0F) {
            this.setUsedDelta(0.0F);
         }

         if (this.getUsedDelta() <= 0.0F) {
            this.setActivated(false);
         }

      }
   }

   public float getCorpseSicknessDefense() {
      if (this.getCondition() <= 0) {
         return 0.0F;
      } else {
         float var1 = this.getScriptItem().getCorpseSicknessDefense();
         if (this.hasFilter()) {
            float var2 = 25.0F;
            if (this.getUsedDelta() > 0.0F) {
               var2 = 100.0F;
            }

            if (var2 > var1) {
               var1 = var2;
            }
         }

         if (this.hasFilter() && 25.0F > var1) {
            var1 = 25.0F;
         }

         return var1;
      }
   }

   public void copyClothingFrom(Clothing var1) {
   }

   private static enum WetDryState {
      Invalid,
      Dryer,
      Wetter;

      private WetDryState() {
      }
   }

   public class ClothingPatch {
      public int tailorLvl = 0;
      public int fabricType = 0;
      public int scratchDefense = 0;
      public int biteDefense = 0;
      public boolean hasHole;
      public int conditionGain = 0;

      public String getFabricTypeName() {
         return Translator.getText("IGUI_FabricType_" + this.fabricType);
      }

      public int getScratchDefense() {
         return this.scratchDefense;
      }

      public int getBiteDefense() {
         return this.biteDefense;
      }

      public int getFabricType() {
         return this.fabricType;
      }

      public ClothingPatch() {
      }

      public ClothingPatch(int var2, int var3, boolean var4) {
         this.tailorLvl = var2;
         this.fabricType = var3;
         this.hasHole = var4;
         ClothingPatchFabricType var5 = Clothing.ClothingPatchFabricType.fromIndex(var3);
         this.scratchDefense = (int)Math.max(1.0F, (float)var5.maxScratchDef * ((float)var2 / 10.0F));
         if (var5.maxBiteDef > 0) {
            this.biteDefense = (int)Math.max(1.0F, (float)var5.maxBiteDef * ((float)var2 / 10.0F));
         }

      }

      public void save(ByteBuffer var1, boolean var2) throws IOException {
         var1.put((byte)this.tailorLvl);
         var1.put((byte)this.fabricType);
         var1.put((byte)this.scratchDefense);
         var1.put((byte)this.biteDefense);
         var1.put((byte)(this.hasHole ? 1 : 0));
         var1.putShort((short)this.conditionGain);
      }

      public void load(ByteBuffer var1, int var2) throws IOException {
         this.tailorLvl = var1.get();
         this.fabricType = var1.get();
         this.scratchDefense = var1.get();
         this.biteDefense = var1.get();
         this.hasHole = var1.get() == 1;
         this.conditionGain = var1.getShort();
      }

      /** @deprecated */
      @Deprecated
      public void save_old(ByteBuffer var1, boolean var2) throws IOException {
         var1.putInt(this.tailorLvl);
         var1.putInt(this.fabricType);
         var1.putInt(this.scratchDefense);
         var1.putInt(this.biteDefense);
         var1.put((byte)(this.hasHole ? 1 : 0));
         var1.putInt(this.conditionGain);
      }

      /** @deprecated */
      @Deprecated
      public void load_old(ByteBuffer var1, int var2, boolean var3) throws IOException {
         this.tailorLvl = var1.getInt();
         this.fabricType = var1.getInt();
         this.scratchDefense = var1.getInt();
         this.biteDefense = var1.getInt();
         this.hasHole = var1.get() == 1;
         this.conditionGain = var1.getInt();
      }
   }

   public static enum ClothingPatchFabricType {
      Cotton(1, "Cotton", 5, 0),
      Denim(2, "Denim", 10, 5),
      Leather(3, "Leather", 20, 10);

      public int index;
      public String type;
      public int maxScratchDef;
      public int maxBiteDef;

      private ClothingPatchFabricType(int var3, String var4, int var5, int var6) {
         this.index = var3;
         this.type = var4;
         this.maxScratchDef = var5;
         this.maxBiteDef = var6;
      }

      public String getType() {
         return this.type;
      }

      public static ClothingPatchFabricType fromType(String var0) {
         if (StringUtils.isNullOrEmpty(var0)) {
            return null;
         } else if (Cotton.type.equals(var0)) {
            return Cotton;
         } else if (Denim.type.equals(var0)) {
            return Denim;
         } else {
            return Leather.type.equals(var0) ? Leather : null;
         }
      }

      public static ClothingPatchFabricType fromIndex(int var0) {
         if (var0 == 1) {
            return Cotton;
         } else if (var0 == 2) {
            return Denim;
         } else {
            return var0 == 3 ? Leather : null;
         }
      }
   }
}
