package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.WornItems.WornItems;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.Shader;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.gameStates.GameLoadingState;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Moveable;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.MannequinScript;
import zombie.scripting.objects.ModelScript;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public class IsoMannequin extends IsoObject implements IHumanVisual {
   private static final ColorInfo inf = new ColorInfo();
   private boolean bInit = false;
   private boolean bFemale = false;
   private boolean bZombie = false;
   private boolean bSkeleton = false;
   private String mannequinScriptName = null;
   private String modelScriptName = null;
   private String textureName = null;
   private String animSet = null;
   private String animState = null;
   private String pose = null;
   private String outfit = null;
   private final HumanVisual humanVisual = new HumanVisual(this);
   private final ItemVisuals itemVisuals = new ItemVisuals();
   private final WornItems wornItems = new WornItems(BodyLocations.getGroup("Human"));
   private MannequinScript mannequinScript = null;
   private ModelScript modelScript = null;
   private final PerPlayer[] perPlayer = new PerPlayer[4];
   private boolean bAnimate = false;
   private AnimatedModel animatedModel = null;
   private Drawer[] drawers = null;
   private float screenX;
   private float screenY;
   private static final StaticPerPlayer[] staticPerPlayer = new StaticPerPlayer[4];

   public IsoMannequin(IsoCell var1) {
      super(var1);

      for(int var2 = 0; var2 < 4; ++var2) {
         this.perPlayer[var2] = new PerPlayer();
      }

   }

   public IsoMannequin(IsoCell var1, IsoGridSquare var2, IsoSprite var3) {
      super(var1, var2, var3);

      for(int var4 = 0; var4 < 4; ++var4) {
         this.perPlayer[var4] = new PerPlayer();
      }

   }

   public String getObjectName() {
      return "Mannequin";
   }

   public HumanVisual getHumanVisual() {
      return this.humanVisual;
   }

   public void getItemVisuals(ItemVisuals var1) {
      this.wornItems.getItemVisuals(var1);
   }

   public boolean isFemale() {
      return this.bFemale;
   }

   public boolean isZombie() {
      return this.bZombie;
   }

   public boolean isSkeleton() {
      return this.bSkeleton;
   }

   public boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2) {
      if (var2 instanceof Clothing && !StringUtils.isNullOrWhitespace(((Clothing)var2).getBodyLocation())) {
         return true;
      } else {
         return var2 instanceof InventoryContainer && !StringUtils.isNullOrWhitespace(((InventoryContainer)var2).canBeEquipped());
      }
   }

   public String getMannequinScriptName() {
      return this.mannequinScriptName;
   }

   public void setMannequinScriptName(String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         if (ScriptManager.instance.getMannequinScript(var1) != null) {
            this.mannequinScriptName = var1;
            this.bInit = true;
            this.mannequinScript = null;
            this.textureName = null;
            this.animSet = null;
            this.animState = null;
            this.pose = null;
            this.outfit = null;
            this.humanVisual.clear();
            this.itemVisuals.clear();
            this.wornItems.clear();
            this.initMannequinScript();
            this.initModelScript();
            if (this.outfit == null) {
               Outfit var2 = OutfitManager.instance.GetRandomNonProfessionalOutfit(this.bFemale);
               this.humanVisual.dressInNamedOutfit(var2.m_Name, this.itemVisuals);
            } else if (!"none".equalsIgnoreCase(this.outfit)) {
               this.humanVisual.dressInNamedOutfit(this.outfit, this.itemVisuals);
            }

            this.humanVisual.setHairModel("");
            this.humanVisual.setBeardModel("");
            this.createInventory(this.itemVisuals);
            this.validateSkinTexture();
            this.validatePose();
            this.syncModel();
         }
      }
   }

   public String getPose() {
      return this.pose;
   }

   public void setRenderDirection(IsoDirections var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      if (var1 != this.perPlayer[var2].renderDirection) {
         this.perPlayer[var2].renderDirection = var1;
      }
   }

   public void rotate(IsoDirections var1) {
      if (var1 != null && var1 != IsoDirections.Max) {
         this.dir = var1;

         for(int var2 = 0; var2 < 4; ++var2) {
            this.perPlayer[var2].atlasTex = null;
         }

         if (GameServer.bServer) {
            this.sendObjectChange("rotate");
         }

      }
   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      if ("rotate".equals(var1)) {
         var3.put((byte)this.dir.index());
      } else {
         super.saveChange(var1, var2, var3);
      }

   }

   public void loadChange(String var1, ByteBuffer var2) {
      if ("rotate".equals(var1)) {
         byte var3 = var2.get();
         this.rotate(IsoDirections.fromIndex(var3));
      } else {
         super.loadChange(var1, var2);
      }

   }

   public void getVariables(Map<String, String> var1) {
      var1.put("Female", this.bFemale ? "true" : "false");
      var1.put("Pose", this.getPose());
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.dir = IsoDirections.fromIndex(var1.get());
      this.bInit = var1.get() == 1;
      this.bFemale = var1.get() == 1;
      this.bZombie = var1.get() == 1;
      this.bSkeleton = var1.get() == 1;
      if (var2 >= 191) {
         this.mannequinScriptName = GameWindow.ReadString(var1);
      }

      this.pose = GameWindow.ReadString(var1);
      this.humanVisual.load(var1, var2);
      this.textureName = this.humanVisual.getSkinTexture();
      this.wornItems.clear();
      if (this.container == null) {
         this.container = new ItemContainer("mannequin", this.getSquare(), this);
         this.container.setExplored(true);
      }

      this.container.clear();
      if (var1.get() == 1) {
         try {
            this.container.ID = var1.getInt();
            ArrayList var4 = this.container.load(var1, var2);
            byte var5 = var1.get();

            for(int var6 = 0; var6 < var5; ++var6) {
               String var7 = GameWindow.ReadString(var1);
               short var8 = var1.getShort();
               if (var8 >= 0 && var8 < var4.size() && this.wornItems.getBodyLocationGroup().getLocation(var7) != null) {
                  this.wornItems.setItem(var7, (InventoryItem)var4.get(var8));
               }
            }
         } catch (Exception var9) {
            if (this.container != null) {
               DebugLog.log("Failed to stream in container ID: " + this.container.ID);
            }
         }
      }

   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      ItemContainer var3 = this.container;
      this.container = null;
      super.save(var1, var2);
      this.container = var3;
      var1.put((byte)this.dir.index());
      var1.put((byte)(this.bInit ? 1 : 0));
      var1.put((byte)(this.bFemale ? 1 : 0));
      var1.put((byte)(this.bZombie ? 1 : 0));
      var1.put((byte)(this.bSkeleton ? 1 : 0));
      GameWindow.WriteString(var1, this.mannequinScriptName);
      GameWindow.WriteString(var1, this.pose);
      this.humanVisual.save(var1);
      if (var3 != null) {
         var1.put((byte)1);
         var1.putInt(var3.ID);
         ArrayList var4 = var3.save(var1);
         if (this.wornItems.size() > 127) {
            throw new RuntimeException("too many worn items");
         }

         var1.put((byte)this.wornItems.size());
         this.wornItems.forEach((var2x) -> {
            GameWindow.WriteString(var1, var2x.getLocation());
            var1.putShort((short)var4.indexOf(var2x.getItem()));
         });
      } else {
         var1.put((byte)0);
      }

   }

   public void saveState(ByteBuffer var1) throws IOException {
      if (!this.bInit) {
         this.initOutfit();
      }

      this.save(var1);
   }

   public void loadState(ByteBuffer var1) throws IOException {
      var1.get();
      var1.get();
      this.load(var1, 195);
      this.initOutfit();
      this.validateSkinTexture();
      this.validatePose();
      this.syncModel();
   }

   public void addToWorld() {
      super.addToWorld();
      this.initOutfit();
      this.validateSkinTexture();
      this.validatePose();
      this.syncModel();
   }

   private void initMannequinScript() {
      if (!StringUtils.isNullOrWhitespace(this.mannequinScriptName)) {
         this.mannequinScript = ScriptManager.instance.getMannequinScript(this.mannequinScriptName);
      }

      if (this.mannequinScript == null) {
         this.modelScriptName = this.bFemale ? "FemaleBody" : "MaleBody";
         this.textureName = this.bFemale ? "F_Mannequin_White" : "M_Mannequin_White";
         this.animSet = "mannequin";
         this.animState = this.bFemale ? "female" : "male";
         this.outfit = null;
      } else {
         this.bFemale = this.mannequinScript.isFemale();
         this.modelScriptName = this.mannequinScript.getModelScriptName();
         if (this.textureName == null) {
            this.textureName = this.mannequinScript.getTexture();
         }

         this.animSet = this.mannequinScript.getAnimSet();
         this.animState = this.mannequinScript.getAnimState();
         if (this.pose == null) {
            this.pose = this.mannequinScript.getPose();
         }

         if (this.outfit == null) {
            this.outfit = this.mannequinScript.getOutfit();
         }
      }

   }

   private void initModelScript() {
      if (!StringUtils.isNullOrWhitespace(this.modelScriptName)) {
         this.modelScript = ScriptManager.instance.getModelScript(this.modelScriptName);
      }

   }

   private void validateSkinTexture() {
   }

   private void validatePose() {
      AnimationSet var1 = AnimationSet.GetAnimationSet(this.animSet, false);
      if (var1 == null) {
         DebugLog.General.warn("ERROR: mannequin AnimSet \"%s\" doesn't exist", this.animSet);
         this.pose = "Invalid";
      } else {
         AnimState var2 = var1.GetState(this.animState);
         if (var2 == null) {
            DebugLog.General.warn("ERROR: mannequin AnimSet \"%s\" state \"%s\" doesn't exist", this.animSet, this.animState);
            this.pose = "Invalid";
         } else {
            Iterator var3 = var2.m_Nodes.iterator();

            AnimNode var4;
            do {
               if (!var3.hasNext()) {
                  if (var2.m_Nodes == null) {
                     DebugLog.General.warn("ERROR: mannequin AnimSet \"%s\" state \"%s\" node \"%s\" doesn't exist", this.animSet, this.animState, this.pose);
                     this.pose = "Invalid";
                     return;
                  }

                  AnimNode var5 = (AnimNode)PZArrayUtil.pickRandom(var2.m_Nodes);
                  this.pose = var5.m_Name;
                  return;
               }

               var4 = (AnimNode)var3.next();
            } while(!var4.m_Name.equalsIgnoreCase(this.pose));

         }
      }
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      int var8 = IsoCamera.frameState.playerIndex;
      var1 += 0.5F;
      var2 += 0.5F;
      this.calcScreenPos(var1, var2, var3);
      this.renderShadow(var1, var2, var3);
      if (this.bAnimate) {
         this.animatedModel.update();
         Drawer var11 = this.drawers[SpriteRenderer.instance.getMainStateIndex()];
         var11.init(var1, var2, var3);
         SpriteRenderer.instance.drawGeneric(var11);
      } else {
         IsoDirections var9 = this.dir;
         PerPlayer var10 = this.perPlayer[var8];
         if (var10.renderDirection != null && var10.renderDirection != IsoDirections.Max) {
            this.dir = var10.renderDirection;
            var10.renderDirection = null;
            var10.bWasRenderDirection = true;
            var10.atlasTex = null;
         } else if (var10.bWasRenderDirection) {
            var10.bWasRenderDirection = false;
            var10.atlasTex = null;
         }

         if (var10.atlasTex == null) {
            var10.atlasTex = DeadBodyAtlas.instance.getBodyTexture(this);
            DeadBodyAtlas.instance.render();
         }

         this.dir = var9;
         if (var10.atlasTex != null) {
            if (this.isHighlighted()) {
               inf.r = this.getHighlightColor().r;
               inf.g = this.getHighlightColor().g;
               inf.b = this.getHighlightColor().b;
               inf.a = this.getHighlightColor().a;
            } else {
               inf.r = var4.r;
               inf.g = var4.g;
               inf.b = var4.b;
               inf.a = var4.a;
            }

            var4 = inf;
            if (!this.isHighlighted() && PerformanceSettings.LightingFrameSkip < 3) {
               this.square.interpolateLight(var4, var1 - (float)this.square.getX(), var2 - (float)this.square.getY());
            }

            var10.atlasTex.render((float)((int)this.screenX), (float)((int)this.screenY), var4.r, var4.g, var4.b, this.getAlpha(var8));
            if (Core.bDebug) {
            }
         }

      }
   }

   public void renderFxMask(float var1, float var2, float var3, boolean var4) {
   }

   private void calcScreenPos(float var1, float var2, float var3) {
      if (IsoSprite.globalOffsetX == -1.0F) {
         IsoSprite.globalOffsetX = -IsoCamera.frameState.OffX;
         IsoSprite.globalOffsetY = -IsoCamera.frameState.OffY;
      }

      this.screenX = IsoUtils.XToScreen(var1, var2, var3, 0);
      this.screenY = IsoUtils.YToScreen(var1, var2, var3, 0);
      this.sx = this.screenX;
      this.sy = this.screenY;
      this.screenX = this.sx + IsoSprite.globalOffsetX;
      this.screenY = this.sy + IsoSprite.globalOffsetY;
      IsoObject[] var4 = (IsoObject[])this.square.getObjects().getElements();

      for(int var5 = 0; var5 < this.square.getObjects().size(); ++var5) {
         IsoObject var6 = var4[var5];
         if (var6.isTableSurface()) {
            this.screenY -= (var6.getSurfaceOffset() + 1.0F) * (float)Core.TileScale;
         }
      }

   }

   private void renderShadow(float var1, float var2, float var3) {
      Texture var4 = Texture.getSharedTexture("dropshadow");
      int var5 = IsoCamera.frameState.playerIndex;
      float var6 = 0.8F * this.getAlpha(var5);
      ColorInfo var7 = this.square.lighting[var5].lightInfo();
      var6 *= (var7.r + var7.g + var7.b) / 3.0F;
      var6 *= 0.8F;
      float var8 = this.screenX - (float)var4.getWidth() / 2.0F * (float)Core.TileScale;
      float var9 = this.screenY - (float)var4.getHeight() / 2.0F * (float)Core.TileScale;
      SpriteRenderer.instance.render(var4, var8, var9, (float)var4.getWidth() * (float)Core.TileScale, (float)var4.getHeight() * (float)Core.TileScale, 1.0F, 1.0F, 1.0F, var6, (Consumer)null);
   }

   private void initOutfit() {
      if (this.bInit) {
         this.initMannequinScript();
         this.initModelScript();
      } else {
         this.bInit = true;
         this.getPropertiesFromSprite();
         this.getPropertiesFromZone();
         this.initMannequinScript();
         this.initModelScript();
         if (this.outfit == null) {
            Outfit var1 = OutfitManager.instance.GetRandomNonProfessionalOutfit(this.bFemale);
            this.humanVisual.dressInNamedOutfit(var1.m_Name, this.itemVisuals);
         } else if (!"none".equalsIgnoreCase(this.outfit)) {
            this.humanVisual.dressInNamedOutfit(this.outfit, this.itemVisuals);
         }

         this.humanVisual.setHairModel("");
         this.humanVisual.setBeardModel("");
         this.createInventory(this.itemVisuals);
      }
   }

   private void getPropertiesFromSprite() {
      switch (this.sprite.name) {
         case "location_shop_mall_01_65":
            this.mannequinScriptName = "FemaleWhite01";
            this.dir = IsoDirections.SE;
            break;
         case "location_shop_mall_01_66":
            this.mannequinScriptName = "FemaleWhite02";
            this.dir = IsoDirections.S;
            break;
         case "location_shop_mall_01_67":
            this.mannequinScriptName = "FemaleWhite03";
            this.dir = IsoDirections.SE;
            break;
         case "location_shop_mall_01_68":
            this.mannequinScriptName = "MaleWhite01";
            this.dir = IsoDirections.SE;
            break;
         case "location_shop_mall_01_69":
            this.mannequinScriptName = "MaleWhite02";
            this.dir = IsoDirections.S;
            break;
         case "location_shop_mall_01_70":
            this.mannequinScriptName = "MaleWhite03";
            this.dir = IsoDirections.SE;
            break;
         case "location_shop_mall_01_73":
            this.mannequinScriptName = "FemaleBlack01";
            this.dir = IsoDirections.SE;
            break;
         case "location_shop_mall_01_74":
            this.mannequinScriptName = "FemaleBlack02";
            this.dir = IsoDirections.S;
            break;
         case "location_shop_mall_01_75":
            this.mannequinScriptName = "FemaleBlack03";
            this.dir = IsoDirections.SE;
            break;
         case "location_shop_mall_01_76":
            this.mannequinScriptName = "MaleBlack01";
            this.dir = IsoDirections.SE;
            break;
         case "location_shop_mall_01_77":
            this.mannequinScriptName = "MaleBlack02";
            this.dir = IsoDirections.S;
            break;
         case "location_shop_mall_01_78":
            this.mannequinScriptName = "MaleBlack03";
            this.dir = IsoDirections.SE;
      }

   }

   private void getPropertiesFromZone() {
      if (this.getObjectIndex() != -1) {
         IsoMetaCell var1 = IsoWorld.instance.getMetaGrid().getCellData(this.square.x / 300, this.square.y / 300);
         if (var1 != null && var1.mannequinZones != null) {
            ArrayList var2 = var1.mannequinZones;
            MannequinZone var3 = null;

            for(int var4 = 0; var4 < var2.size(); ++var4) {
               var3 = (MannequinZone)var2.get(var4);
               if (var3.contains(this.square.x, this.square.y, this.square.z)) {
                  break;
               }

               var3 = null;
            }

            if (var3 != null) {
               if (var3.bFemale != -1) {
                  this.bFemale = var3.bFemale == 1;
               }

               if (var3.dir != IsoDirections.Max) {
                  this.dir = var3.dir;
               }

               if (var3.mannequinScript != null) {
                  this.mannequinScriptName = var3.mannequinScript;
               }

               if (var3.skin != null) {
                  this.textureName = var3.skin;
               }

               if (var3.pose != null) {
                  this.pose = var3.pose;
               }

               if (var3.outfit != null) {
                  this.outfit = var3.outfit;
               }

            }
         }
      }
   }

   private void syncModel() {
      this.humanVisual.setForceModelScript(this.modelScriptName);
      switch (this.modelScriptName) {
         case "FemaleBody":
            this.humanVisual.setForceModel(ModelManager.instance.m_femaleModel);
            break;
         case "MaleBody":
            this.humanVisual.setForceModel(ModelManager.instance.m_maleModel);
            break;
         default:
            this.humanVisual.setForceModel(ModelManager.instance.getLoadedModel(this.modelScriptName));
      }

      this.humanVisual.setSkinTextureName(this.textureName);
      this.wornItems.getItemVisuals(this.itemVisuals);

      int var3;
      for(var3 = 0; var3 < 4; ++var3) {
         this.perPlayer[var3].atlasTex = null;
      }

      if (this.bAnimate) {
         if (this.animatedModel == null) {
            this.animatedModel = new AnimatedModel();
            this.drawers = new Drawer[3];

            for(var3 = 0; var3 < this.drawers.length; ++var3) {
               this.drawers[var3] = new Drawer();
            }
         }

         this.animatedModel.setAnimSetName(this.getAnimSetName());
         this.animatedModel.setState(this.getAnimStateName());
         this.animatedModel.setVariable("Female", this.bFemale);
         this.animatedModel.setVariable("Pose", this.getPose());
         this.animatedModel.setAngle(this.dir.ToVector());
         this.animatedModel.setModelData(this.humanVisual, this.itemVisuals);
      }

   }

   private void createInventory(ItemVisuals var1) {
      if (this.container == null) {
         this.container = new ItemContainer("mannequin", this.getSquare(), this);
         this.container.setExplored(true);
      }

      this.container.clear();
      this.wornItems.setFromItemVisuals(var1);
      this.wornItems.addItemsToItemContainer(this.container);
   }

   public void wearItem(InventoryItem var1, IsoGameCharacter var2) {
      if (this.container.contains(var1)) {
         ItemVisual var3 = var1.getVisual();
         if (var3 != null) {
            if (var1 instanceof Clothing && !StringUtils.isNullOrWhitespace(((Clothing)var1).getBodyLocation())) {
               this.wornItems.setItem(((Clothing)var1).getBodyLocation(), var1);
            } else {
               if (!(var1 instanceof InventoryContainer) || StringUtils.isNullOrWhitespace(((InventoryContainer)var1).canBeEquipped())) {
                  return;
               }

               this.wornItems.setItem(((InventoryContainer)var1).canBeEquipped(), var1);
            }

            if (var2 != null) {
               ArrayList var4 = this.container.getItems();

               for(int var5 = 0; var5 < var4.size(); ++var5) {
                  InventoryItem var6 = (InventoryItem)var4.get(var5);
                  if (!this.wornItems.contains(var6)) {
                     this.container.removeItemOnServer(var6);
                     this.container.Remove(var6);
                     var2.getInventory().AddItem(var6);
                     --var5;
                  }
               }
            }

            this.syncModel();
         }
      }
   }

   public void checkClothing(InventoryItem var1) {
      for(int var2 = 0; var2 < this.wornItems.size(); ++var2) {
         InventoryItem var3 = this.wornItems.getItemByIndex(var2);
         if (this.container == null || this.container.getItems().indexOf(var3) == -1) {
            this.wornItems.remove(var3);
            this.syncModel();
            --var2;
         }
      }

   }

   public String getAnimSetName() {
      return this.animSet;
   }

   public String getAnimStateName() {
      return this.animState;
   }

   public void getCustomSettingsFromItem(InventoryItem var1) throws IOException {
      if (var1 instanceof Moveable) {
         ByteBuffer var2 = var1.getByteData();
         if (var2 == null) {
            return;
         }

         var2.rewind();
         int var3 = var2.getInt();
         var2.get();
         var2.get();
         this.load(var2, var3);
      }

   }

   public void setCustomSettingsToItem(InventoryItem var1) throws IOException {
      if (var1 instanceof Moveable) {
         synchronized(SliceY.SliceBufferLock) {
            ByteBuffer var3 = SliceY.SliceBuffer;
            var3.clear();
            var3.putInt(195);
            this.save(var3);
            var3.flip();
            var1.byteData = ByteBuffer.allocate(var3.limit());
            var1.byteData.put(var3);
         }

         if (this.container != null) {
            var1.setActualWeight(var1.getActualWeight() + this.container.getContentsWeight());
         }
      }

   }

   public static boolean isMannequinSprite(IsoSprite var0) {
      return "Mannequin".equals(var0.getProperties().Val("CustomName"));
   }

   private void resetMannequin() {
      this.bInit = false;
      this.bFemale = false;
      this.bZombie = false;
      this.bSkeleton = false;
      this.mannequinScriptName = null;
      this.modelScriptName = null;
      this.textureName = null;
      this.animSet = null;
      this.animState = null;
      this.pose = null;
      this.outfit = null;
      this.humanVisual.clear();
      this.itemVisuals.clear();
      this.wornItems.clear();
      this.mannequinScript = null;
      this.modelScript = null;
      this.bAnimate = false;
   }

   public static void renderMoveableItem(Moveable var0, int var1, int var2, int var3, IsoDirections var4) {
      int var5 = IsoCamera.frameState.playerIndex;
      StaticPerPlayer var6 = staticPerPlayer[var5];
      if (var6 == null) {
         var6 = staticPerPlayer[var5] = new StaticPerPlayer(var5);
      }

      var6.renderMoveableItem(var0, var1, var2, var3, var4);
   }

   public static void renderMoveableObject(IsoMannequin var0, int var1, int var2, int var3, IsoDirections var4) {
      var0.setRenderDirection(var4);
   }

   public static IsoDirections getDirectionFromItem(Moveable var0, int var1) {
      StaticPerPlayer var2 = staticPerPlayer[var1];
      if (var2 == null) {
         var2 = staticPerPlayer[var1] = new StaticPerPlayer(var1);
      }

      return var2.getDirectionFromItem(var0);
   }

   private static final class PerPlayer {
      private DeadBodyAtlas.BodyTexture atlasTex = null;
      IsoDirections renderDirection = null;
      boolean bWasRenderDirection = false;

      private PerPlayer() {
      }
   }

   private final class Drawer extends TextureDraw.GenericDrawer {
      float x;
      float y;
      float z;
      float m_animPlayerAngle;
      boolean bRendered;

      private Drawer() {
      }

      public void init(float var1, float var2, float var3) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
         this.bRendered = false;
         IsoMannequin.this.animatedModel.renderMain();
         this.m_animPlayerAngle = IsoMannequin.this.animatedModel.getAnimationPlayer().getRenderedAngle();
      }

      public void render() {
         IsoMannequin.this.animatedModel.DoRenderToWorld(this.x, this.y, this.z, this.m_animPlayerAngle);
         this.bRendered = true;
      }

      public void postRender() {
         IsoMannequin.this.animatedModel.postRender(this.bRendered);
      }
   }

   public static final class MannequinZone extends IsoMetaGrid.Zone {
      public int bFemale = -1;
      public IsoDirections dir;
      public String mannequinScript;
      public String pose;
      public String skin;
      public String outfit;

      public MannequinZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
         super(var1, var2, var3, var4, var5, var6, var7);
         this.dir = IsoDirections.Max;
         this.mannequinScript = null;
         this.pose = null;
         this.skin = null;
         this.outfit = null;
         if (var8 != null) {
            Object var9 = var8.rawget("Female");
            if (var9 instanceof Boolean) {
               this.bFemale = var9 == Boolean.TRUE ? 1 : 0;
            }

            var9 = var8.rawget("Direction");
            if (var9 instanceof String) {
               this.dir = IsoDirections.valueOf((String)var9);
            }

            var9 = var8.rawget("Outfit");
            if (var9 instanceof String) {
               this.outfit = (String)var9;
            }

            var9 = var8.rawget("Script");
            if (var9 instanceof String) {
               this.mannequinScript = (String)var9;
            }

            var9 = var8.rawget("Skin");
            if (var9 instanceof String) {
               this.skin = (String)var9;
            }

            var9 = var8.rawget("Pose");
            if (var9 instanceof String) {
               this.pose = (String)var9;
            }
         }

      }
   }

   private static final class StaticPerPlayer {
      final int playerIndex;
      Moveable _moveable = null;
      Moveable _failedItem = null;
      IsoMannequin _mannequin = null;

      StaticPerPlayer(int var1) {
         this.playerIndex = var1;
      }

      void renderMoveableItem(Moveable var1, int var2, int var3, int var4, IsoDirections var5) {
         if (this.checkItem(var1)) {
            if (this._moveable != var1) {
               this._moveable = var1;

               try {
                  this._mannequin.getCustomSettingsFromItem(this._moveable);
               } catch (IOException var7) {
               }

               this._mannequin.initOutfit();
               this._mannequin.validateSkinTexture();
               this._mannequin.validatePose();
               this._mannequin.syncModel();
               this._mannequin.perPlayer[this.playerIndex].atlasTex = null;
            }

            this._mannequin.square = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, var4);
            if (this._mannequin.square != null) {
               this._mannequin.perPlayer[this.playerIndex].renderDirection = var5;
               IsoMannequin.inf.set(1.0F, 1.0F, 1.0F, 1.0F);
               this._mannequin.render((float)var2, (float)var3, (float)var4, IsoMannequin.inf, false, false, (Shader)null);
            }
         }
      }

      IsoDirections getDirectionFromItem(Moveable var1) {
         if (!this.checkItem(var1)) {
            return IsoDirections.S;
         } else {
            this._moveable = null;

            try {
               this._mannequin.getCustomSettingsFromItem(var1);
               return this._mannequin.getDir();
            } catch (Exception var3) {
               return IsoDirections.S;
            }
         }
      }

      boolean checkItem(Moveable var1) {
         if (var1 == null) {
            return false;
         } else {
            String var2 = var1.getWorldSprite();
            IsoSprite var3 = IsoSpriteManager.instance.getSprite(var2);
            if (var3 != null && IsoMannequin.isMannequinSprite(var3)) {
               if (var1.getByteData() == null) {
                  Thread var4 = Thread.currentThread();
                  if (var4 != GameWindow.GameThread && var4 != GameLoadingState.loader && var4 == GameServer.MainThread) {
                     return false;
                  } else {
                     if (this._mannequin == null || this._mannequin.getCell() != IsoWorld.instance.CurrentCell) {
                        this._mannequin = new IsoMannequin(IsoWorld.instance.CurrentCell);
                     }

                     if (this._failedItem == var1) {
                        return false;
                     } else {
                        try {
                           this._mannequin.resetMannequin();
                           this._mannequin.sprite = var3;
                           this._mannequin.initOutfit();
                           this._mannequin.validateSkinTexture();
                           this._mannequin.validatePose();
                           this._mannequin.syncModel();
                           this._mannequin.setCustomSettingsToItem(var1);
                           return true;
                        } catch (IOException var6) {
                           this._failedItem = var1;
                           return false;
                        }
                     }
                  }
               } else {
                  if (this._mannequin == null || this._mannequin.getCell() != IsoWorld.instance.CurrentCell) {
                     this._mannequin = new IsoMannequin(IsoWorld.instance.CurrentCell);
                  }

                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }
}
