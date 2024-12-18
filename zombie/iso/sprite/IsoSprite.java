package zombie.iso.sprite;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.CharacterModelCamera;
import zombie.core.properties.PropertyContainer;
import zombie.core.properties.RoofProperties;
import zombie.core.skinnedmodel.ModelCameraRenderData;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Mask;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWater;
import zombie.iso.PlayerCamera;
import zombie.iso.SpriteModel;
import zombie.iso.Vector3;
import zombie.iso.WorldConverter;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderObjectOutline;
import zombie.iso.fboRenderChunk.ObjectRenderInfo;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.sprite.shapers.WallShaper;
import zombie.iso.sprite.shapers.WallShaperSliceN;
import zombie.iso.sprite.shapers.WallShaperSliceW;
import zombie.iso.sprite.shapers.WallShaperWhole;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.tileDepth.TileDepthModifier;
import zombie.tileDepth.TileDepthShader;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileSeamManager;
import zombie.tileDepth.TileSeamModifier;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleModelCamera;

public final class IsoSprite {
   public static int maxCount = 0;
   public static float alphaStep = 0.05F;
   public static float globalOffsetX = -1.0F;
   public static float globalOffsetY = -1.0F;
   private static final ColorInfo info = new ColorInfo();
   private static final HashMap<String, Object[]> AnimNameSet = new HashMap();
   public int firerequirement;
   public String burntTile;
   public boolean forceAmbient;
   public boolean solidfloor;
   public boolean canBeRemoved;
   public boolean attachedFloor;
   public boolean cutW;
   public boolean cutN;
   public boolean solid;
   public boolean solidTrans;
   public boolean invisible;
   public boolean alwaysDraw;
   public boolean forceRender;
   public boolean moveWithWind = false;
   public boolean isBush = false;
   public static final byte RL_DEFAULT = 0;
   public static final byte RL_FLOOR = 1;
   public byte renderLayer = 0;
   public int windType = 1;
   public Texture texture = null;
   public boolean Animate = true;
   public IsoAnim CurrentAnim = null;
   public boolean DeleteWhenFinished = false;
   public boolean Loop = true;
   public short soffX = 0;
   public short soffY = 0;
   public final PropertyContainer Properties = new PropertyContainer();
   public final ColorInfo TintMod = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
   public HashMap<String, IsoAnim> AnimMap = null;
   public ArrayList<IsoAnim> AnimStack = null;
   public String name;
   public String tilesetName;
   public int tileSheetIndex = 0;
   public static final int DEFAULT_SPRITE_ID = 20000000;
   public int ID = 20000000;
   public IsoSpriteInstance def;
   public ModelManager.ModelSlot modelSlot;
   IsoSpriteManager parentManager;
   private IsoObjectType type;
   private String parentObjectName;
   private IsoSpriteGrid spriteGrid;
   public boolean treatAsWallOrder;
   public SpriteModel spriteModel;
   public TileDepthTexture depthTexture;
   public int depthFlags;
   private boolean hideForWaterRender;
   public static final int SDF_USE_OBJECT_DEPTH_TEXTURE = 1;
   public static final int SDF_TRANSLUCENT = 2;
   public static final int SDF_OPAQUE_PIXELS_ONLY = 4;
   public static TileSeamManager.Tiles SEAM_FIX2 = null;
   public static boolean SEAM_EAST = true;
   public static boolean SEAM_SOUTH = true;
   private boolean bInitRoofProperties;
   private RoofProperties roofProperties;

   public void setHideForWaterRender() {
      this.hideForWaterRender = true;
   }

   public IsoSprite() {
      this.type = IsoObjectType.MAX;
      this.parentObjectName = null;
      this.treatAsWallOrder = false;
      this.spriteModel = null;
      this.depthTexture = null;
      this.depthFlags = 0;
      this.hideForWaterRender = false;
      this.bInitRoofProperties = false;
      this.roofProperties = null;
      this.parentManager = IsoSpriteManager.instance;
      this.def = IsoSpriteInstance.get(this);
   }

   public IsoSprite(IsoSpriteManager var1) {
      this.type = IsoObjectType.MAX;
      this.parentObjectName = null;
      this.treatAsWallOrder = false;
      this.spriteModel = null;
      this.depthTexture = null;
      this.depthFlags = 0;
      this.hideForWaterRender = false;
      this.bInitRoofProperties = false;
      this.roofProperties = null;
      this.parentManager = var1;
      this.def = IsoSpriteInstance.get(this);
   }

   public static IsoSprite CreateSprite(IsoSpriteManager var0) {
      IsoSprite var1 = new IsoSprite(var0);
      return var1;
   }

   public static IsoSprite CreateSpriteUsingCache(String var0, String var1, int var2) {
      IsoSprite var3 = CreateSprite(IsoSpriteManager.instance);
      return var3.setFromCache(var0, var1, var2);
   }

   public static IsoSprite getSprite(IsoSpriteManager var0, int var1) {
      if (WorldConverter.instance.TilesetConversions != null && !WorldConverter.instance.TilesetConversions.isEmpty() && WorldConverter.instance.TilesetConversions.containsKey(var1)) {
         var1 = (Integer)WorldConverter.instance.TilesetConversions.get(var1);
      }

      return var0.IntMap.containsKey(var1) ? (IsoSprite)var0.IntMap.get(var1) : null;
   }

   public static void setSpriteID(IsoSpriteManager var0, int var1, IsoSprite var2) {
      if (var0.IntMap.containsKey(var2.ID)) {
         var0.IntMap.remove(var2.ID);
         var2.ID = var1;
         var0.IntMap.put(var1, var2);
      }

   }

   public static IsoSprite getSprite(IsoSpriteManager var0, IsoSprite var1, int var2) {
      if (var1.name.contains("_")) {
         String[] var3 = var1.name.split("_");
         int var4 = Integer.parseInt(var3[var3.length - 1].trim());
         var4 += var2;
         HashMap var10000 = var0.NamedMap;
         String var10001 = var1.name.substring(0, var1.name.lastIndexOf("_"));
         return (IsoSprite)var10000.get(var10001 + "_" + var4);
      } else {
         return null;
      }
   }

   public static IsoSprite getSprite(IsoSpriteManager var0, String var1, int var2) {
      IsoSprite var3 = (IsoSprite)var0.NamedMap.get(var1);
      if (var3 == null) {
         return null;
      } else if (var2 == 0) {
         return var3;
      } else if (var3.tilesetName == null) {
         if (var3.name.contains("_")) {
            String var4 = var3.name.substring(0, var3.name.lastIndexOf(95));
            String var5 = var3.name.substring(var3.name.lastIndexOf(95) + 1);
            int var6 = Integer.parseInt(var5.trim());
            var6 += var2;
            return var0.getSprite(var4 + "_" + var6);
         } else {
            return null;
         }
      } else {
         return var0.getSprite(var3.tilesetName + "_" + (var3.tileSheetIndex + var2));
      }
   }

   public static void DisposeAll() {
      AnimNameSet.clear();
   }

   public static boolean HasCache(String var0) {
      return AnimNameSet.containsKey(var0);
   }

   public IsoSpriteInstance newInstance() {
      return IsoSpriteInstance.get(this);
   }

   public PropertyContainer getProperties() {
      return this.Properties;
   }

   public String getParentObjectName() {
      return this.parentObjectName;
   }

   public void setParentObjectName(String var1) {
      this.parentObjectName = var1;
   }

   public void save(DataOutputStream var1) throws IOException {
      GameWindow.WriteString(var1, this.name);
   }

   public void load(DataInputStream var1) throws IOException {
      this.name = GameWindow.ReadString(var1);
      this.LoadFramesNoDirPageSimple(this.name);
   }

   public void Dispose() {
      this.disposeAnimation();
      this.texture = null;
   }

   private void allocateAnimationIfNeeded() {
      if (this.CurrentAnim == null) {
         this.CurrentAnim = new IsoAnim();
      }

      if (this.AnimMap == null) {
         this.AnimMap = new HashMap(2);
      }

      if (this.AnimStack == null) {
         this.AnimStack = new ArrayList(1);
      }

   }

   public void disposeAnimation() {
      if (this.AnimMap != null) {
         Iterator var1 = this.AnimMap.values().iterator();

         while(var1.hasNext()) {
            IsoAnim var2 = (IsoAnim)var1.next();
            var2.Dispose();
         }

         this.AnimMap = null;
      }

      if (this.AnimStack != null) {
         this.AnimStack.clear();
         this.AnimStack = null;
      }

      this.CurrentAnim = null;
   }

   public boolean isMaskClicked(IsoDirections var1, int var2, int var3) {
      try {
         Texture var4 = this.getTextureForCurrentFrame(var1);
         if (var4 == null) {
            return false;
         } else {
            Mask var5 = var4.getMask();
            if (var5 == null) {
               return false;
            } else {
               var2 = (int)((float)var2 - var4.offsetX);
               var3 = (int)((float)var3 - var4.offsetY);
               return var5.get(var2, var3);
            }
         }
      } catch (Exception var6) {
         ExceptionLogger.logException(var6);
         return true;
      }
   }

   public boolean isMaskClicked(IsoDirections var1, int var2, int var3, boolean var4) {
      this.initSpriteInstance();

      try {
         Texture var5 = this.getTextureForCurrentFrame(var1);
         if (var5 == null) {
            return false;
         } else {
            Mask var6 = var5.getMask();
            if (var6 == null) {
               return false;
            } else {
               if (var4) {
                  var2 = (int)((float)var2 - ((float)(var5.getWidthOrig() - var5.getWidth()) - var5.offsetX));
                  var3 = (int)((float)var3 - var5.offsetY);
                  var2 = var5.getWidth() - var2;
               } else {
                  var2 = (int)((float)var2 - var5.offsetX);
                  var3 = (int)((float)var3 - var5.offsetY);
               }

               return var2 >= 0 && var3 >= 0 && var2 <= var5.getWidth() && var3 <= var5.getHeight() ? var6.get(var2, var3) : false;
            }
         }
      } catch (Exception var7) {
         ExceptionLogger.logException(var7);
         return true;
      }
   }

   public float getMaskClickedY(IsoDirections var1, int var2, int var3, boolean var4) {
      try {
         Texture var5 = this.getTextureForCurrentFrame(var1);
         if (var5 == null) {
            return 10000.0F;
         } else {
            Mask var6 = var5.getMask();
            if (var6 == null) {
               return 10000.0F;
            } else {
               if (var4) {
                  var2 = (int)((float)var2 - ((float)(var5.getWidthOrig() - var5.getWidth()) - var5.offsetX));
                  var3 = (int)((float)var3 - var5.offsetY);
                  var2 = var5.getWidth() - var2;
               } else {
                  var2 = (int)((float)var2 - var5.offsetX);
                  var3 = (int)((float)var3 - var5.offsetY);
                  var2 = var5.getWidth() - var2;
               }

               return (float)var3;
            }
         }
      } catch (Exception var7) {
         Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, (String)null, var7);
         return 10000.0F;
      }
   }

   public Texture LoadSingleTexture(String var1) {
      this.disposeAnimation();
      this.texture = Texture.getSharedTexture(var1);
      return this.texture;
   }

   public Texture LoadFrameExplicit(String var1) {
      this.CurrentAnim = new IsoAnim();
      this.allocateAnimationIfNeeded();
      this.AnimMap.put("default", this.CurrentAnim);
      this.CurrentAnim.ID = this.AnimStack.size();
      this.AnimStack.add(this.CurrentAnim);
      return this.CurrentAnim.LoadFrameExplicit(var1);
   }

   public void LoadFrames(String var1, String var2, int var3) {
      if (this.AnimMap == null || !this.AnimMap.containsKey(var2)) {
         this.CurrentAnim = new IsoAnim();
         this.allocateAnimationIfNeeded();
         this.AnimMap.put(var2, this.CurrentAnim);
         this.CurrentAnim.ID = this.AnimStack.size();
         this.AnimStack.add(this.CurrentAnim);
         this.CurrentAnim.LoadFrames(var1, var2, var3);
      }
   }

   public void LoadFramesReverseAltName(String var1, String var2, String var3, int var4) {
      if (this.AnimMap == null || !this.AnimMap.containsKey(var3)) {
         this.CurrentAnim = new IsoAnim();
         this.allocateAnimationIfNeeded();
         this.AnimMap.put(var3, this.CurrentAnim);
         this.CurrentAnim.ID = this.AnimStack.size();
         this.AnimStack.add(this.CurrentAnim);
         this.CurrentAnim.LoadFramesReverseAltName(var1, var2, var3, var4);
      }
   }

   public void LoadFramesNoDirPage(String var1, String var2, int var3) {
      this.CurrentAnim = new IsoAnim();
      this.allocateAnimationIfNeeded();
      this.AnimMap.put(var2, this.CurrentAnim);
      this.CurrentAnim.ID = this.AnimStack.size();
      this.AnimStack.add(this.CurrentAnim);
      this.CurrentAnim.LoadFramesNoDirPage(var1, var2, var3);
   }

   public void LoadFramesNoDirPageDirect(String var1, String var2, int var3) {
      this.CurrentAnim = new IsoAnim();
      this.allocateAnimationIfNeeded();
      this.AnimMap.put(var2, this.CurrentAnim);
      this.CurrentAnim.ID = this.AnimStack.size();
      this.AnimStack.add(this.CurrentAnim);
      this.CurrentAnim.LoadFramesNoDirPageDirect(var1, var2, var3);
   }

   public void LoadFramesNoDirPageSimple(String var1) {
      if (this.AnimMap != null && this.AnimMap.containsKey("default")) {
         IsoAnim var2 = (IsoAnim)this.AnimMap.get("default");
         this.AnimStack.remove(var2);
         this.AnimMap.remove("default");
      }

      this.CurrentAnim = new IsoAnim();
      this.allocateAnimationIfNeeded();
      this.AnimMap.put("default", this.CurrentAnim);
      this.CurrentAnim.ID = this.AnimStack.size();
      this.AnimStack.add(this.CurrentAnim);
      this.CurrentAnim.LoadFramesNoDirPage(var1);
   }

   public void ReplaceCurrentAnimFrames(String var1) {
      if (this.CurrentAnim != null) {
         this.CurrentAnim.Frames.clear();
         this.CurrentAnim.LoadFramesNoDirPage(var1);
      }
   }

   public void LoadFramesPageSimple(String var1, String var2, String var3, String var4) {
      this.CurrentAnim = new IsoAnim();
      this.allocateAnimationIfNeeded();
      this.AnimMap.put("default", this.CurrentAnim);
      this.CurrentAnim.ID = this.AnimStack.size();
      this.AnimStack.add(this.CurrentAnim);
      this.CurrentAnim.LoadFramesPageSimple(var1, var2, var3, var4);
   }

   public void LoadFramesPcx(String var1, String var2, int var3) {
      if (this.AnimMap == null || !this.AnimMap.containsKey(var2)) {
         this.CurrentAnim = new IsoAnim();
         this.allocateAnimationIfNeeded();
         this.AnimMap.put(var2, this.CurrentAnim);
         this.CurrentAnim.ID = this.AnimStack.size();
         this.AnimStack.add(this.CurrentAnim);
         this.CurrentAnim.LoadFramesPcx(var1, var2, var3);
      }
   }

   public void PlayAnim(IsoAnim var1) {
      if (this.CurrentAnim == null || this.CurrentAnim != var1) {
         this.CurrentAnim = var1;
      }

   }

   public void PlayAnim(String var1) {
      if ((this.CurrentAnim == null || !this.CurrentAnim.name.equals(var1)) && this.AnimMap != null && this.AnimMap.containsKey(var1)) {
         this.CurrentAnim = (IsoAnim)this.AnimMap.get(var1);
      }

   }

   public void PlayAnimUnlooped(String var1) {
      if (this.AnimMap != null) {
         if (this.AnimMap.containsKey(var1)) {
            if (this.CurrentAnim == null || !this.CurrentAnim.name.equals(var1)) {
               this.CurrentAnim = (IsoAnim)this.AnimMap.get(var1);
            }

            this.CurrentAnim.looped = false;
         }

      }
   }

   public void ChangeTintMod(ColorInfo var1) {
      this.TintMod.r = var1.r;
      this.TintMod.g = var1.g;
      this.TintMod.b = var1.b;
      this.TintMod.a = var1.a;
   }

   public void RenderGhostTile(int var1, int var2, int var3) {
      this.RenderGhostTileColor(var1, var2, var3, 1.0F, 1.0F, 1.0F, 0.6F);
   }

   public void RenderGhostTileRed(int var1, int var2, int var3) {
      this.RenderGhostTileColor(var1, var2, var3, 0.65F, 0.2F, 0.2F, 0.6F);
   }

   public void RenderGhostTileColor(int var1, int var2, int var3, float var4, float var5, float var6, float var7) {
      this.RenderGhostTileColor(var1, var2, var3, 0.0F, 0.0F, var4, var5, var6, var7);
   }

   public void RenderGhostTileColor(int var1, int var2, int var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      IsoSpriteInstance var10 = IsoSpriteInstance.get(this);
      var10.tintr = var6;
      var10.tintg = var7;
      var10.tintb = var8;
      var10.alpha = var10.targetAlpha = var9;
      IsoGridSquare.getDefColorInfo().set(1.0F, 1.0F, 1.0F, 1.0F);
      int var11 = Core.TileScale;
      if (PerformanceSettings.FBORenderChunk) {
         FBORenderObjectHighlight.getInstance().setRenderingGhostTile(true);
         IndieGL.StartShader(0);
      }

      IndieGL.glDefaultBlendFunc();
      Texture var12 = this.getTextureForCurrentFrame(IsoDirections.N);
      if (var12 != null && var12.getName() != null && var12.getName().contains("JUMBO")) {
         this.render(var10, (IsoObject)null, (float)var1, (float)var2, (float)var3, IsoDirections.N, (float)(96 * var11) + var4, (float)(224 * var11) + var5, IsoGridSquare.getDefColorInfo(), true);
      } else {
         this.render(var10, (IsoObject)null, (float)var1, (float)var2, (float)var3, IsoDirections.N, (float)(32 * var11) + var4, (float)(96 * var11) + var5, IsoGridSquare.getDefColorInfo(), true);
      }

      if (PerformanceSettings.FBORenderChunk) {
         FBORenderObjectHighlight.getInstance().setRenderingGhostTile(false);
      }

      IsoSpriteInstance.add(var10);
   }

   public boolean hasActiveModel() {
      if (!ModelManager.instance.bDebugEnableModels) {
         return false;
      } else if (!ModelManager.instance.isCreated()) {
         return false;
      } else {
         return this.modelSlot != null && this.modelSlot.active;
      }
   }

   public void renderVehicle(IsoSpriteInstance var1, IsoObject var2, float var3, float var4, float var5, float var6, float var7, ColorInfo var8, boolean var9) {
      if (var1 != null) {
         if (this.hasActiveModel()) {
            SpriteRenderer.instance.drawGeneric(((ModelCameraRenderData)ModelCameraRenderData.s_pool.alloc()).init(VehicleModelCamera.instance, this.modelSlot));
            SpriteRenderer.instance.drawModel(this.modelSlot);
            if (!BaseVehicle.RENDER_TO_TEXTURE) {
               return;
            }
         }

         info.r = var8.r;
         info.g = var8.g;
         info.b = var8.b;
         info.a = var8.a;

         try {
            if (var9) {
               var1.renderprep(var2);
            }

            float var10 = 0.0F;
            float var11 = 0.0F;
            if (globalOffsetX == -1.0F) {
               globalOffsetX = -IsoCamera.frameState.OffX;
               globalOffsetY = -IsoCamera.frameState.OffY;
            }

            if (var2 == null || var2.sx == 0.0F || var2 instanceof IsoMovingObject) {
               var10 = IsoUtils.XToScreen(var3 + var1.offX, var4 + var1.offY, var5 + var1.offZ, 0);
               var11 = IsoUtils.YToScreen(var3 + var1.offX, var4 + var1.offY, var5 + var1.offZ, 0);
               var10 -= var6;
               var11 -= var7;
               if (var2 != null) {
                  var2.sx = var10;
                  var2.sy = var11;
               }
            }

            if (var2 != null) {
               var10 = var2.sx + globalOffsetX;
               var11 = var2.sy + globalOffsetY;
               var10 += (float)this.soffX;
               var11 += (float)this.soffY;
            } else {
               var10 += globalOffsetX;
               var11 += globalOffsetY;
               var10 += (float)this.soffX;
               var11 += (float)this.soffY;
            }

            ColorInfo var10000;
            if (var9) {
               if (var1.tintr != 1.0F || var1.tintg != 1.0F || var1.tintb != 1.0F) {
                  var10000 = info;
                  var10000.r *= var1.tintr;
                  var10000 = info;
                  var10000.g *= var1.tintg;
                  var10000 = info;
                  var10000.b *= var1.tintb;
               }

               info.a = var1.alpha;
            }

            if (!this.hasActiveModel() && (this.TintMod.r != 1.0F || this.TintMod.g != 1.0F || this.TintMod.b != 1.0F)) {
               var10000 = info;
               var10000.r *= this.TintMod.r;
               var10000 = info;
               var10000.g *= this.TintMod.g;
               var10000 = info;
               var10000.b *= this.TintMod.b;
            }

            if (this.hasActiveModel()) {
               float var12 = var1.getScaleX() * (float)Core.TileScale;
               float var13 = -var1.getScaleY() * (float)Core.TileScale;
               float var14 = 0.666F;
               var12 /= 4.0F * var14;
               var13 /= 4.0F * var14;
               int var15 = ModelManager.instance.bitmap.getTexture().getWidth();
               int var16 = ModelManager.instance.bitmap.getTexture().getHeight();
               var10 -= (float)var15 * var12 / 2.0F;
               var11 -= (float)var16 * var13 / 2.0F;
               float var17 = ((BaseVehicle)var2).jniTransform.origin.y / 2.44949F;
               var11 += 96.0F * var17 / var13 / var14;
               var11 += 27.84F / var13 / var14;
               if (SceneShaderStore.WeatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                  SpriteRenderer.instance.render((Texture)ModelManager.instance.bitmap.getTexture(), var10, var11, (float)var15 * var12, (float)var16 * var13, 1.0F, 1.0F, 1.0F, info.a, (Consumer)null);
               } else {
                  SpriteRenderer.instance.render((Texture)ModelManager.instance.bitmap.getTexture(), var10, var11, (float)var15 * var12, (float)var16 * var13, info.r, info.g, info.b, info.a, (Consumer)null);
               }

               if (Core.bDebug && DebugOptions.instance.Model.Render.Bounds.getValue()) {
                  LineDrawer.drawRect(var10, var11, (float)var15 * var12, (float)var16 * var13, 1.0F, 1.0F, 1.0F, 1.0F, 1);
               }
            }

            info.r = 1.0F;
            info.g = 1.0F;
            info.b = 1.0F;
         } catch (Exception var18) {
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, (String)null, var18);
         }

      }
   }

   private IsoSpriteInstance getSpriteInstance() {
      this.initSpriteInstance();
      return this.def;
   }

   private void initSpriteInstance() {
      if (this.def == null) {
         this.def = IsoSpriteInstance.get(this);
      }

   }

   public final void render(IsoObject var1, float var2, float var3, float var4, IsoDirections var5, float var6, float var7, ColorInfo var8, boolean var9) {
      this.render(var1, var2, var3, var4, var5, var6, var7, var8, var9, (Consumer)null);
   }

   public final void render(IsoObject var1, float var2, float var3, float var4, IsoDirections var5, float var6, float var7, ColorInfo var8, boolean var9, Consumer<TextureDraw> var10) {
      this.render(this.getSpriteInstance(), var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
   }

   public final void renderDepth(IsoObject var1, IsoDirections var2, boolean var3, boolean var4, boolean var5, int var6, float var7, float var8, float var9, float var10, float var11, ColorInfo var12, boolean var13, Consumer<TextureDraw> var14) {
      this.renderDepth(this.getSpriteInstance(), var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13, var14);
   }

   public final void render(IsoSpriteInstance var1, IsoObject var2, float var3, float var4, float var5, IsoDirections var6, float var7, float var8, ColorInfo var9, boolean var10) {
      this.render(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, (Consumer)null);
   }

   public void renderWallSliceW(IsoObject var1, float var2, float var3, float var4, IsoDirections var5, float var6, float var7, ColorInfo var8, boolean var9, Consumer<TextureDraw> var10) {
      if (SEAM_SOUTH) {
         this.render(this.getSpriteInstance(), var1, var2, var3, var4, var5, var6 + (float)(32 * Core.TileScale), var7 - (float)(16 * Core.TileScale), var8, var9, WallShaperSliceW.instance);
      } else {
         this.render(this.getSpriteInstance(), var1, var2, var3, var4, var5, var6 - (float)(0 * Core.TileScale) - 6.0F, var7 + (float)(0 * Core.TileScale) + 3.0F, var8, var9, WallShaperSliceW.instance);
      }

   }

   public void renderWallSliceN(IsoObject var1, float var2, float var3, float var4, IsoDirections var5, float var6, float var7, ColorInfo var8, boolean var9, Consumer<TextureDraw> var10) {
      this.render(this.getSpriteInstance(), var1, var2, var3, var4, var5, var6 - (float)(32 * Core.TileScale), var7 - (float)(16 * Core.TileScale), var8, var9, WallShaperSliceN.instance);
   }

   public void render(IsoSpriteInstance var1, IsoObject var2, float var3, float var4, float var5, IsoDirections var6, float var7, float var8, ColorInfo var9, boolean var10, Consumer<TextureDraw> var11) {
      if (this.hasActiveModel()) {
         this.renderActiveModel();
      } else {
         this.renderCurrentAnim(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
      }

   }

   public void renderDepth(IsoSpriteInstance var1, IsoObject var2, IsoDirections var3, boolean var4, boolean var5, boolean var6, int var7, float var8, float var9, float var10, float var11, float var12, ColorInfo var13, boolean var14, Consumer<TextureDraw> var15) {
      this.renderCurrentAnimDepth(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13, var14, var15);
   }

   public void renderCurrentAnim(IsoSpriteInstance var1, IsoObject var2, float var3, float var4, float var5, IsoDirections var6, float var7, float var8, ColorInfo var9, boolean var10, Consumer<TextureDraw> var11) {
      if (DebugOptions.instance.IsoSprite.RenderSprites.getValue()) {
         if (!this.hasNoTextures()) {
            float var12 = this.getCurrentSpriteFrame(var1);
            info.set(var9);
            if (!WeatherFxMask.isRenderingMask() && !FBORenderObjectHighlight.getInstance().isRenderingGhostTile() && !FBORenderObjectOutline.getInstance().isRendering()) {
               if (PerformanceSettings.FBORenderChunk) {
                  this.renderCurrentAnim_FBORender(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
                  return;
               }
            } else {
               IndieGL.disableDepthTest();
            }

            Vector3 var13 = IsoSprite.l_renderCurrentAnim.colorInfoBackup.set(info.r, info.g, info.b);
            Vector3 var14 = IsoSprite.l_renderCurrentAnim.spritePos.set(0.0F, 0.0F, 0.0F);
            this.prepareToRenderSprite(var1, var2, var3, var4, var5, var6, var7, var8, var10, (int)var12, var14);
            this.performRenderFrame(var1, var2, var6, (int)var12, var14.x, var14.y, 0.0F, var11);
            info.r = var13.x;
            info.g = var13.y;
            info.b = var13.z;
         }
      }
   }

   private void renderCurrentAnim_FBORender(IsoSpriteInstance var1, IsoObject var2, float var3, float var4, float var5, IsoDirections var6, float var7, float var8, ColorInfo var9, boolean var10, Consumer<TextureDraw> var11) {
      float var12 = this.getCurrentSpriteFrame(var1);
      Vector3 var13 = IsoSprite.l_renderCurrentAnim.colorInfoBackup.set(info.r, info.g, info.b);
      if (this.getProperties().Is(IsoFlagType.unlit)) {
         info.r = 1.0F;
         info.g = 1.0F;
         info.b = 1.0F;
      }

      Vector3 var14 = IsoSprite.l_renderCurrentAnim.spritePos.set(0.0F, 0.0F, 0.0F);
      this.prepareToRenderSprite(var1, var2, var3, var4, var5, var6, var7, var8, var10, (int)var12, var14);
      if (DebugOptions.instance.FBORenderChunk.DepthTestAll.getValue()) {
         IndieGL.enableDepthTest();
         IndieGL.glDepthFunc(515);
      } else {
         IndieGL.enableDepthTest();
         IndieGL.glDepthFunc(519);
      }

      if (var11 == CutawayAttachedModifier.instance) {
         IndieGL.enableDepthTest();
         IndieGL.glDepthFunc(519);
         CutawayAttachedModifier.instance.setSprite(this);
         this.performRenderFrame(var1, var2, var6, (int)var12, var14.x, var14.y, var14.z, var11);
      } else {
         Object var15;
         Object var16;
         if (var6 == IsoDirections.NW) {
            if (var11 instanceof WallShaper && this.setupTileDepthWall(var2, var6, var3, var4, var5, true)) {
               var15 = SEAM_FIX2 == null ? TileDepthModifier.instance : TileSeamModifier.instance;
               var16 = SEAM_FIX2 == null ? ((Consumer)var15).andThen(var11) : var15;
               this.performRenderFrame(var1, var2, var6, (int)var12, var14.x, var14.y, var14.z, (Consumer)var16);
            }
         } else if (var11 != WallShaperWhole.instance && var11 != WallShaperSliceW.instance && var11 != WallShaperSliceN.instance) {
            float var21 = var5;
            if (var2 != null && var2.getRenderYOffset() != 0.0F) {
               var21 = var5 + var2.getRenderYOffset() / 96.0F;
            }

            if (this.setupTileDepth(var2, var3, var4, var5, var21, true)) {
               var16 = SEAM_FIX2 == null ? TileDepthModifier.instance : TileSeamModifier.instance;
               Object var23 = var11 != null && SEAM_FIX2 == null ? ((Consumer)var16).andThen(var11) : var16;
               if (FBORenderCell.instance.bRenderTranslucentOnly) {
                  IndieGL.glDepthMask(false);
                  IndieGL.enableDepthTest();
               }

               this.performRenderFrame(var1, var2, var6, (int)var12, var14.x, var14.y, var14.z, (Consumer)var23);
            } else {
               if (!(var2 instanceof IsoTree) || !((IsoTree)var2).bUseTreeShader) {
                  IndieGL.StartShader(0);
               }

               byte var24;
               if (FBORenderCell.instance.bRenderTranslucentOnly) {
                  var24 = 8;
                  IsoDepthHelper.Results var25 = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)var24), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)var24), PZMath.fastfloor(var3 / (float)var24), PZMath.fastfloor(var4 / (float)var24), PZMath.fastfloor(var5));
                  TextureDraw.nextChunkDepth = var25.depthStart;
                  if (this.soffY != 0 && !(var2 instanceof IsoFire)) {
                     TextureDraw.nextChunkDepth += (float)this.soffY / 96.0F * IsoDepthHelper.LevelDepth;
                  }

                  TextureDraw.nextChunkDepth += 0.5F;
                  TextureDraw.nextChunkDepth = TextureDraw.nextChunkDepth * 2.0F - 1.0F;
                  IndieGL.glDepthMask(false);
                  IndieGL.enableDepthTest();
                  if (var2 instanceof IsoTree && ((IsoTree)var2).bUseTreeShader) {
                     IsoTree.TreeShader.instance.setDepth(TextureDraw.nextChunkDepth, var14.z * 2.0F - 1.0F);
                  }

                  if (var2 instanceof IsoTree) {
                     IndieGL.glDepthMask(true);
                  }
               } else if (!FBORenderChunkManager.instance.isCaching()) {
                  var24 = 8;
                  var14.z += IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)var24), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)var24), PZMath.fastfloor(var3 / (float)var24), PZMath.fastfloor(var4 / (float)var24), PZMath.fastfloor(var5)).depthStart;
               }

               this.performRenderFrame(var1, var2, var6, (int)var12, var14.x, var14.y, var14.z * 2.0F - 1.0F, var11);
            }
         } else if (var11 == WallShaperWhole.instance || var11 == WallShaperSliceW.instance || var11 == WallShaperSliceN.instance) {
            if (var6 != IsoDirections.Max) {
               var15 = SEAM_FIX2 == null ? TileDepthModifier.instance : TileSeamModifier.instance;
               if (var11 != WallShaperSliceW.instance && var11 != WallShaperSliceN.instance) {
                  if (this.getParentSpriteDepthTextureToUse(var2) != null) {
                     TileDepthModifier.instance.setupTileDepthTexture(this, this.getParentSpriteDepthTextureToUse(var2));
                     this.startTileDepthShader(var2, var3, var4, var5, var5, true);
                     var16 = SEAM_FIX2 == null ? ((Consumer)var15).andThen(var11) : var15;
                     this.performRenderFrame(var1, var2, var6, (int)var12, var14.x, var14.y, var14.z, (Consumer)var16);
                  } else if (this.setupTileDepthWall(var2, var6, var3, var4, var5, true)) {
                     var16 = SEAM_FIX2 == null ? ((Consumer)var15).andThen(var11) : var15;
                     this.performRenderFrame(var1, var2, var6, (int)var12, var14.x, var14.y, var14.z, (Consumer)var16);
                  }
               } else {
                  int var22 = 0;
                  int var17 = 0;
                  int var18 = var11 == WallShaperSliceN.instance ? 1 : 0;
                  int var19 = var11 == WallShaperSliceW.instance ? 1 : 0;
                  if (!SEAM_SOUTH) {
                     var22 = var11 == WallShaperSliceN.instance ? 0 : 0;
                     var17 = var11 == WallShaperSliceW.instance ? 0 : 0;
                     var18 = var11 == WallShaperSliceN.instance ? 0 : 0;
                     var19 = var11 == WallShaperSliceW.instance ? 0 : 0;
                  }

                  if (this.setupTileDepthWall2(var6, var2.square.x + var22, var2.square.y + var17, var3 + (float)var18, var4 + (float)var19, var5, true)) {
                     Consumer var20 = ((Consumer)var15).andThen(var11);
                     this.performRenderFrame(var1, var2, var6, (int)var12, var14.x, var14.y, var14.z, var20);
                  }
               }
            } else {
               this.performRenderFrame(var1, var2, var6, (int)var12, var14.x, var14.y, var14.z, var11);
            }
         }
      }

      info.r = var13.x;
      info.g = var13.y;
      info.b = var13.z;
      if (var2 != null && FBORenderChunkManager.instance.isCaching()) {
         var2.sx = 0.0F;
      }

   }

   public void renderCurrentAnimDepth(IsoSpriteInstance var1, IsoObject var2, IsoDirections var3, boolean var4, boolean var5, boolean var6, int var7, float var8, float var9, float var10, float var11, float var12, ColorInfo var13, boolean var14, Consumer<TextureDraw> var15) {
      if (DebugOptions.instance.IsoSprite.RenderSprites.getValue()) {
         Texture var16 = this.getTextureForCurrentFrame(var3);
         if (var16 != null) {
            float var17 = this.getCurrentSpriteFrame(var1);
            info.set(var13);
            if (this.getProperties().Is(IsoFlagType.unlit)) {
               info.r = 1.0F;
               info.g = 1.0F;
               info.b = 1.0F;
            }

            Vector3 var18 = IsoSprite.l_renderCurrentAnim.colorInfoBackup.set(info.r, info.g, info.b);
            Vector3 var19 = IsoSprite.l_renderCurrentAnim.spritePos.set(0.0F, 0.0F, 0.0F);
            this.prepareToRenderSprite(var1, var2, var8, var9, var10, var3, var11, var12, var14, (int)var17, var19);
            if (this.setupTileDepthWall(var2, var3, var8, var9, var10, false)) {
               this.performRenderFrame(var1, var2, var3, (int)var17, var19.x, var19.y, var19.z, TileDepthModifier.instance);
            }

            info.r = var18.x;
            info.g = var18.y;
            info.b = var18.z;
         }
      }
   }

   private boolean setupTileDepth(IsoObject var1, float var2, float var3, float var4, float var5, boolean var6) {
      if (var1 instanceof IsoTree) {
         return false;
      } else if (var1 instanceof IsoBarbecue && var1.sprite != null && this != var1.sprite) {
         return false;
      } else if (var1 instanceof IsoFire && this != var1.sprite) {
         return false;
      } else if (var1 instanceof IsoCarBatteryCharger) {
         return false;
      } else if (var1 instanceof IsoMolotovCocktail) {
         return false;
      } else if (var1 instanceof IsoTrap && this.texture != null && this.texture.getName() != null && this.texture.getName().startsWith("Item_")) {
         return false;
      } else if (var1 instanceof IsoWorldInventoryObject) {
         return false;
      } else if (var1 instanceof IsoZombieGiblets) {
         return false;
      } else if (this.depthTexture != null && !this.depthTexture.isEmpty()) {
         if (SEAM_FIX2 == null) {
            TileDepthModifier.instance.setupTileDepthTexture(this, this.depthTexture);
         } else if (SEAM_FIX2 != TileSeamManager.Tiles.FloorSouthOneThird && SEAM_FIX2 != TileSeamManager.Tiles.FloorEastOneThird && SEAM_FIX2 != TileSeamManager.Tiles.FloorSouthTwoThirds && SEAM_FIX2 != TileSeamManager.Tiles.FloorEastTwoThirds) {
            if (SEAM_FIX2 == TileSeamManager.Tiles.FloorSouth || SEAM_FIX2 == TileSeamManager.Tiles.FloorEast) {
               TileSeamModifier.instance.setupFloorDepth(this, SEAM_FIX2, this.depthTexture);
            }
         } else {
            TileSeamModifier.instance.setupFloorDepth(this, SEAM_FIX2, this.depthTexture);
         }

         this.startTileDepthShader(var1, var2, var3, var4, var5, var6);
         return true;
      } else if (!this.getProperties().Is(IsoFlagType.solidfloor) && !this.getProperties().Is(IsoFlagType.FloorOverlay) && this.renderLayer != 1) {
         TileDepthTexture var7 = this.getParentSpriteDepthTextureToUse(var1);
         if (var7 != null) {
            TileDepthModifier.instance.setupTileDepthTexture(this, var7);
            this.startTileDepthShader(var1, var2, var3, var4, var5, var6);
            return true;
         } else if (!this.getProperties().Is(IsoFlagType.windowN) && (!(var1 instanceof IsoWindow) || !var1.getSprite().getProperties().Is(IsoFlagType.windowN))) {
            if (!this.getProperties().Is(IsoFlagType.windowW) && (!(var1 instanceof IsoWindow) || !var1.getSprite().getProperties().Is(IsoFlagType.windowW))) {
               if (this.getProperties().Is(IsoFlagType.WallOverlay) && this.getProperties().Is(IsoFlagType.attachedW)) {
                  TileDepthModifier.instance.setupWallDepth(this, IsoDirections.W);
                  this.startTileDepthShader(var1, var2, var3, var4, var5, var6);
                  return true;
               } else if (this.getProperties().Is(IsoFlagType.WallOverlay) && this.getProperties().Is(IsoFlagType.attachedN)) {
                  TileDepthModifier.instance.setupWallDepth(this, IsoDirections.N);
                  this.startTileDepthShader(var1, var2, var3, var4, var5, var6);
                  return true;
               } else if (var1 instanceof IsoFireplace && var1.sprite != null && this != var1.sprite && var1.sprite.depthTexture != null) {
                  TileDepthModifier.instance.setupTileDepthTexture(this, var1.sprite.depthTexture);
                  this.startTileDepthShader(var1, var2, var3, var4, var5, var6);
                  return true;
               } else {
                  TileDepthTexture var8 = TileDepthTextureManager.getInstance().getDefaultDepthTexture();
                  if (var8 != null && !var8.isEmpty()) {
                     TileDepthModifier.instance.setupTileDepthTexture(this, var8);
                     this.startTileDepthShader(var1, var2, var3, var4, var5, var6);
                     return true;
                  } else {
                     return false;
                  }
               }
            } else {
               TileDepthModifier.instance.setupWallDepth(this, IsoDirections.W);
               this.startTileDepthShader(var1, var2, var3, var4, var5, var6);
               return true;
            }
         } else {
            TileDepthModifier.instance.setupWallDepth(this, IsoDirections.N);
            this.startTileDepthShader(var1, var2, var3, var4, var5, var6);
            return true;
         }
      } else {
         if (SEAM_FIX2 == null) {
            TileDepthModifier.instance.setupFloorDepth(this);
         } else {
            TileSeamModifier.instance.setupFloorDepth(this, SEAM_FIX2);
         }

         this.startTileDepthShader(var1, var2, var3, var4, var5, var6);
         return true;
      }
   }

   private boolean setupTileDepthWall(IsoObject var1, IsoDirections var2, float var3, float var4, float var5, boolean var6) {
      if (this.depthTexture != null && !this.depthTexture.isEmpty()) {
         TileDepthModifier.instance.setupTileDepthTexture(this, this.depthTexture);
         this.startTileDepthShader(var1, var3, var4, var5, var5, var6);
         return true;
      } else {
         if (SEAM_FIX2 == null) {
            IsoSprite var7 = this;
            if (var1 != null && var1.getSprite() != null && var1.getSprite() != this && (this.depthFlags & 1) != 0) {
               var7 = var1.getSprite();
            }

            TileDepthModifier.instance.setupWallDepth(var7, var2);
         } else {
            TileSeamModifier.instance.setupWallDepth(this, var2);
         }

         this.startTileDepthShader(var1, var3, var4, var5, var5, var6);
         return true;
      }
   }

   private void startTileDepthShader(IsoObject var1, float var2, float var3, float var4, float var5, boolean var6) {
      if (FBORenderCell.instance.bRenderTranslucentOnly) {
         int var7 = IsoCamera.frameState.playerIndex;
         PlayerCamera var8 = IsoCamera.cameras[var7];
         var2 += var8.fixJigglyModelsSquareX;
         var3 += var8.fixJigglyModelsSquareY;
      }

      byte var16 = 8;
      float var17 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var2, var3, var4).depthStart;
      float var9 = var4 + 1.0F;
      float var10 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var2 + 1.0F, var3 + 1.0F, var9).depthStart;
      if (var4 != var5) {
         float var11 = var5 - var4 > 0.6F ? 0.994F : 1.0F;
         var11 = 1.0F;
         var17 -= (var5 - var4) * IsoDepthHelper.LevelDepth * var11;
         var10 -= (var5 - var4) * IsoDepthHelper.LevelDepth * var11;
      }

      if (FBORenderCell.instance.bRenderTranslucentOnly) {
         IndieGL.glDepthMask(false);
         IndieGL.enableDepthTest();
         IndieGL.glDepthFunc(515);
         if (FBORenderObjectHighlight.getInstance().isRendering()) {
            var10 -= 5.0E-5F;
            var17 -= 5.0E-5F;
         } else if (FBORenderCell.instance.bRenderAnimatedAttachments && var1 != null && var1.getOnOverlay() != null && this == var1.getOnOverlay().getParentSprite()) {
            var10 -= 5.0E-5F;
            var17 -= 5.0E-5F;
         }
      } else {
         int var18 = PZMath.fastfloor(var2 / (float)var16);
         int var12 = PZMath.fastfloor(var3 / (float)var16);
         int var13 = PZMath.fastfloor(var4);
         if (var1 != null && var1.renderSquareOverride != null) {
            var18 = var1.renderSquareOverride.chunk.wx;
            var12 = var1.renderSquareOverride.chunk.wy;
            var13 = var1.renderSquareOverride.z;
         } else if (var1 != null && var1.renderSquareOverride2 != null) {
            var18 = PZMath.fastfloor((float)var1.square.x / (float)var16);
            var12 = PZMath.fastfloor((float)var1.square.y / (float)var16);
         }

         IsoDepthHelper.Results var14 = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)var16), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)var16), var18, var12, var13);
         float var15 = var14.depthStart;
         if (var1 != null && var1.renderDepthAdjust != 0.0F) {
            var15 += var1.renderDepthAdjust;
         }

         var17 -= var15;
         var10 -= var15;
      }

      if (SEAM_FIX2 != null) {
         IndieGL.StartShader(SceneShaderStore.TileSeamShader.getID());
         IndieGL.shaderSetValue(SceneShaderStore.TileSeamShader, "zDepth", var10);
         IndieGL.shaderSetValue(SceneShaderStore.TileSeamShader, "drawPixels", var6 ? 1 : 0);
         IndieGL.shaderSetValue(SceneShaderStore.TileSeamShader, "zDepthBlendZ", var10);
         IndieGL.shaderSetValue(SceneShaderStore.TileSeamShader, "zDepthBlendToZ", var17);
      } else {
         TileDepthShader var19 = SceneShaderStore.TileDepthShader;
         if ((this.depthFlags & 4) != 0) {
            var19 = SceneShaderStore.OpaqueDepthShader;
         }

         IndieGL.StartShader(var19.getID());
         IndieGL.shaderSetValue(var19, "zDepth", var10);
         IndieGL.shaderSetValue(var19, "drawPixels", var6 ? 1 : 0);
         IndieGL.shaderSetValue(var19, "zDepthBlendZ", var10);
         IndieGL.shaderSetValue(var19, "zDepthBlendToZ", var17);
      }
   }

   private boolean setupTileDepthWall2(IsoDirections var1, int var2, int var3, float var4, float var5, float var6, boolean var7) {
      if (this.depthTexture != null && !this.depthTexture.isEmpty()) {
         TileDepthModifier.instance.setupTileDepthTexture(this, this.depthTexture);
         this.startTileDepthShader2(var2, var3, var4, var5, var6, var6, var7);
         return true;
      } else {
         TileDepthModifier.instance.setupWallDepth(this, var1);
         this.startTileDepthShader2(var2, var3, var4, var5, var6, var6, var7);
         return true;
      }
   }

   private void startTileDepthShader2(int var1, int var2, float var3, float var4, float var5, float var6, boolean var7) {
      byte var8 = 8;
      float var9 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var3, var4, var5).depthStart;
      float var10 = var5 + 1.0F;
      float var11 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var3 + 1.0F, var4 + 1.0F, var10).depthStart;
      if (var5 != var6) {
         float var12 = var6 - var5 > 0.6F ? 0.994F : 1.0F;
         var12 = 1.0F;
         var9 -= (var6 - var5) * IsoDepthHelper.LevelDepth * var12;
         var11 -= (var6 - var5) * IsoDepthHelper.LevelDepth * var12;
      }

      if (FBORenderCell.instance.bRenderTranslucentOnly) {
         IndieGL.glDepthMask(false);
         IndieGL.enableDepthTest();
         IndieGL.glDepthFunc(515);
      } else {
         IsoDepthHelper.Results var14 = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)var8), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)var8), PZMath.fastfloor((float)(var1 / var8)), PZMath.fastfloor((float)(var2 / var8)), PZMath.fastfloor(var5));
         float var13 = var14.depthStart;
         var13 -= 1.0E-4F;
         var9 -= var13;
         var11 -= var13;
      }

      IndieGL.StartShader(SceneShaderStore.TileDepthShader.getID());
      IndieGL.shaderSetValue(SceneShaderStore.TileDepthShader, "zDepth", var11);
      IndieGL.shaderSetValue(SceneShaderStore.TileDepthShader, "drawPixels", var7 ? 1 : 0);
      IndieGL.shaderSetValue(SceneShaderStore.TileDepthShader, "zDepthBlendZ", var11);
      IndieGL.shaderSetValue(SceneShaderStore.TileDepthShader, "zDepthBlendToZ", var9);
   }

   public static void renderTextureWithDepth(Texture var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      int var10 = IsoCamera.frameState.playerIndex;
      PlayerCamera var11 = IsoCamera.cameras[var10];
      var7 += var11.fixJigglyModelsSquareX;
      var8 += var11.fixJigglyModelsSquareY;
      if (FBORenderCell.instance.bRenderTranslucentOnly) {
         byte var12 = 8;
         IsoDepthHelper.Results var13 = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)var12), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)var12), PZMath.fastfloor(var7 / (float)var12), PZMath.fastfloor(var8 / (float)var12), PZMath.fastfloor(var9));
         TextureDraw.nextChunkDepth = var13.depthStart;
         TextureDraw.nextChunkDepth += 0.5F;
         TextureDraw.nextChunkDepth = TextureDraw.nextChunkDepth * 2.0F - 1.0F;
      }

      TextureDraw.nextZ = IsoDepthHelper.calculateDepth(var7, var8, var9) * 2.0F - 1.0F;
      IndieGL.StartShader(0);
      float var14 = IsoUtils.XToScreen(var7, var8, var9, 0) - var11.getOffX() - var1 / 2.0F;
      float var15 = IsoUtils.YToScreen(var7, var8, var9, 0) - var11.getOffY() - var2;
      SpriteRenderer.instance.render(var0, var14, var15, var1, var2, var3, var4, var5, var6, (Consumer)null);
      IndieGL.EndShader();
   }

   private TileDepthTexture getParentSpriteDepthTextureToUse(IsoObject var1) {
      if (var1 == null) {
         return null;
      } else {
         IsoSprite var2 = var1.getSprite();
         if (this == var2) {
            return null;
         } else {
            boolean var3 = false;
            if ((this.depthFlags & 1) != 0) {
               var3 = true;
            } else if (this.getProperties().Is(IsoFlagType.WallOverlay) && this.getProperties().Is(IsoFlagType.attachedN)) {
               var3 = true;
            } else if (this.getProperties().Is(IsoFlagType.WallOverlay) && this.getProperties().Is(IsoFlagType.attachedW)) {
               var3 = true;
            }

            return var3 && var2 != null && var2.depthTexture != null && !var2.depthTexture.isEmpty() ? var2.depthTexture : null;
         }
      }
   }

   public boolean hasAnimation() {
      return this.CurrentAnim != null;
   }

   public boolean hasNoTextures() {
      if (this.CurrentAnim == null) {
         return this.texture == null;
      } else {
         return this.CurrentAnim.hasNoTextures();
      }
   }

   private float getCurrentSpriteFrame(IsoSpriteInstance var1) {
      if (!this.hasAnimation()) {
         var1.Frame = 0.0F;
         return 0.0F;
      } else {
         if (this.CurrentAnim.FramesArray == null) {
            this.CurrentAnim.FramesArray = (IsoDirectionFrame[])this.CurrentAnim.Frames.toArray(new IsoDirectionFrame[0]);
         }

         if (this.CurrentAnim.FramesArray.length != this.CurrentAnim.Frames.size()) {
            this.CurrentAnim.FramesArray = (IsoDirectionFrame[])this.CurrentAnim.Frames.toArray(this.CurrentAnim.FramesArray);
         }

         float var2;
         if (var1.Frame >= (float)this.CurrentAnim.Frames.size()) {
            var2 = (float)(this.CurrentAnim.FramesArray.length - 1);
         } else if (var1.Frame < 0.0F) {
            var1.Frame = 0.0F;
            var2 = 0.0F;
         } else {
            var2 = var1.Frame;
         }

         return var2;
      }
   }

   private void prepareToRenderSprite(IsoSpriteInstance var1, IsoObject var2, float var3, float var4, float var5, IsoDirections var6, float var7, float var8, boolean var9, int var10, Vector3 var11) {
      float var12 = var3;
      float var13 = var4;
      if (var9) {
         var1.renderprep(var2);
      }

      float var14 = 0.0F;
      float var15 = 0.0F;
      if (globalOffsetX == -1.0F) {
         globalOffsetX = -IsoCamera.frameState.OffX;
         globalOffsetY = -IsoCamera.frameState.OffY;
      }

      float var16 = globalOffsetX;
      float var17 = globalOffsetY;
      if (FBORenderChunkManager.instance.isCaching()) {
         var16 = FBORenderChunkManager.instance.getXOffset();
         var17 = FBORenderChunkManager.instance.getYOffset();
         var3 = PZMath.coordmodulof(var3, 8);
         var4 = PZMath.coordmodulof(var4, 8);
         if (var2 != null && var2.getSquare() != var2.getRenderSquare()) {
            var3 = var12 - (float)(var2.getRenderSquare().chunk.wx * 8);
            var4 = var13 - (float)(var2.getRenderSquare().chunk.wy * 8);
         } else if (var2 != null && var2.renderSquareOverride2 != null) {
            var3 = var12 - (float)(var2.getSquare().chunk.wx * 8);
            var4 = var13 - (float)(var2.getSquare().chunk.wy * 8);
         }

         if (var2 != null) {
            var2.sx = 0.0F;
         }
      }

      if (var2 != null && var2.sx != 0.0F && !(var2 instanceof IsoMovingObject)) {
         if (var2 != null) {
            var14 = var2.sx + var16;
            var15 = var2.sy + var17;
            var14 += (float)this.soffX;
            var15 += (float)this.soffY;
         } else {
            var14 += var16;
            var15 += var17;
            var14 += (float)this.soffX;
            var15 += (float)this.soffY;
         }
      } else {
         var14 = IsoUtils.XToScreen(var3 + var1.offX, var4 + var1.offY, var5 + var1.offZ, 0);
         var15 = IsoUtils.YToScreen(var3 + var1.offX, var4 + var1.offY, var5 + var1.offZ, 0);
         var14 -= var7;
         var15 -= var8;
         if (var2 != null) {
            var2.sx = var14;
            var2.sy = var15;
         }

         var14 += var16;
         var15 += var17;
         var14 += (float)this.soffX;
         var15 += (float)this.soffY;
      }

      if (PerformanceSettings.FBORenderChunk && !FBORenderChunkManager.instance.isCaching()) {
         int var18 = IsoCamera.frameState.playerIndex;
         float var19 = IsoCamera.frameState.zoom;
         var14 += IsoCamera.cameras[var18].fixJigglyModelsX * var19;
         var15 += IsoCamera.cameras[var18].fixJigglyModelsY * var19;
      }

      if (var2 instanceof IsoMovingObject) {
         Texture var20 = this.getTextureForFrame(var10, var6);
         var14 -= (float)var20.getWidthOrig() / 2.0F * var1.getScaleX();
         var15 -= (float)var20.getHeightOrig() * var1.getScaleY();
      }

      ColorInfo var10000;
      if (var9) {
         if (var1.tintr != 1.0F || var1.tintg != 1.0F || var1.tintb != 1.0F) {
            var10000 = info;
            var10000.r *= var1.tintr;
            var10000 = info;
            var10000.g *= var1.tintg;
            var10000 = info;
            var10000.b *= var1.tintb;
         }

         info.a = var1.alpha;
         if (var1.bMultiplyObjectAlpha && var2 != null) {
            var10000 = info;
            var10000.a *= var2.getAlpha(IsoCamera.frameState.playerIndex);
         }
      }

      if (this.TintMod.r != 1.0F || this.TintMod.g != 1.0F || this.TintMod.b != 1.0F) {
         var10000 = info;
         var10000.r *= this.TintMod.r;
         var10000 = info;
         var10000.g *= this.TintMod.g;
         var10000 = info;
         var10000.b *= this.TintMod.b;
      }

      if (PerformanceSettings.FBORenderChunk && !WeatherFxMask.isRenderingMask() && !FBORenderObjectHighlight.getInstance().isRenderingGhostTile()) {
         if (var2 != null && var2.square == IsoPlayer.getInstance().getCurrentSquare()) {
            boolean var21 = false;
         }

         float var22 = calculateDepth(var3, var4, var5);
         if (var2 instanceof IsoTree) {
            var22 = calculateDepth(var3 + 0.99F, var4 + 0.99F, var5);
            if (!FBORenderCell.instance.bRenderTranslucentOnly && var2.getSquare() != var2.getRenderSquare()) {
               byte var23 = 8;
               var22 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var12 + 0.99F, var13 + 0.99F, var5).depthStart;
               var22 -= IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)var23), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)var23), PZMath.fastfloor((float)var2.getRenderSquare().x / (float)var23), PZMath.fastfloor((float)var2.getRenderSquare().y / (float)var23), PZMath.fastfloor(var5)).depthStart;
            }
         }

         if (var2 != null && var2.square != null && var2.square.getWater() != null && var2.square.getWater().isValid()) {
            var22 = calculateDepth(var3 + 0.99F, var4 + 0.99F, var5);
         }

         if (var2 instanceof IsoCarBatteryCharger || var2 instanceof IsoTrap || var2 instanceof IsoWorldInventoryObject) {
            var22 = calculateDepth(var3 + 0.25F, var4 + 0.25F, var5);
         }

         var11.set(var14, var15, var22);
      } else {
         var11.set(var14, var15, 0.0F);
      }
   }

   public static float calculateDepth(float var0, float var1, float var2) {
      return IsoDepthHelper.calculateDepth(var0, var1, var2);
   }

   private void performRenderFrame(IsoSpriteInstance var1, IsoObject var2, IsoDirections var3, int var4, float var5, float var6, float var7, Consumer<TextureDraw> var8) {
      Texture var9 = this.getTextureForFrame(var4, var3);
      if (var9 != null) {
         if (Core.TileScale == 2 && var9.getWidthOrig() == 64 && var9.getHeightOrig() == 128) {
            var1.setScale(2.0F, 2.0F);
         }

         if (Core.TileScale == 2 && var1.scaleX == 2.0F && var1.scaleY == 2.0F && var9.getWidthOrig() == 128 && var9.getHeightOrig() == 256) {
            var1.setScale(1.0F, 1.0F);
         }

         if (!(var1.scaleX <= 0.0F) && !(var1.scaleY <= 0.0F)) {
            float var10 = (float)var9.getWidth();
            float var11 = (float)var9.getHeight();
            float var12 = var1.scaleX;
            float var13 = var1.scaleY;
            if (var12 != 1.0F) {
               var5 += var9.getOffsetX() * (var12 - 1.0F);
               var10 *= var12;
            }

            if (var13 != 1.0F) {
               var6 += var9.getOffsetY() * (var13 - 1.0F);
               var11 *= var13;
            }

            if (DebugOptions.instance.IsoSprite.MovingObjectEdges.getValue() && var2 instanceof IsoMovingObject) {
               this.renderSpriteOutline(var5, var6, var9, var12, var13);
            }

            if (DebugOptions.instance.IsoSprite.DropShadowEdges.getValue() && StringUtils.equals(var9.getName(), "dropshadow")) {
               this.renderSpriteOutline(var5, var6, var9, var12, var13);
            }

            TextureDraw.nextZ = var7;
            if (!this.hideForWaterRender || !IsoWater.getInstance().getShaderEnable()) {
               if (this.hasAnimation()) {
                  IsoDirectionFrame var14 = this.getAnimFrame(var4);
                  if (var2 != null && var2.getObjectRenderEffectsToApply() != null) {
                     var14.render(var2.getObjectRenderEffectsToApply(), var5, var6, var10, var11, var3, info, var1.Flip, var8);
                  } else {
                     var14.render(var5, var6, var10, var11, var3, info, var1.Flip, var8);
                  }
               } else if (var2 != null && var2.getObjectRenderEffectsToApply() != null) {
                  if (var1.Flip) {
                     var9.flip = !var9.flip;
                  }

                  var9.render(var2.getObjectRenderEffectsToApply(), var5, var6, var10, var11, info.r, info.g, info.b, info.a, var8);
                  var9.flip = false;
               } else {
                  if (var1.Flip) {
                     var9.flip = !var9.flip;
                  }

                  var9.render(var5, var6, var10, var11, info.r, info.g, info.b, info.a, var8);
                  var9.flip = false;
               }
            }

            if (PerformanceSettings.FBORenderChunk) {
               if (var2 != null) {
                  if (!FBORenderCell.instance.bRenderAnimatedAttachments) {
                     if (!WeatherFxMask.isRenderingMask()) {
                        if (info.a != 0.0F) {
                           int var18 = IsoCamera.frameState.playerIndex;
                           ObjectRenderInfo var15 = var2.getRenderInfo(var18);
                           if (!FBORenderObjectHighlight.getInstance().isRendering() && !FBORenderObjectOutline.getInstance().isRendering()) {
                              if (FBORenderCell.instance.bRenderTranslucentOnly) {
                                 var15.m_renderX = var2.sx - IsoCamera.frameState.OffX;
                                 var15.m_renderY = var2.sy - IsoCamera.frameState.OffY;
                              } else if (FBORenderChunkManager.instance.renderChunk != null && FBORenderChunkManager.instance.renderChunk.bHighRes) {
                                 var15.m_renderX = var2.sx + FBORenderChunkManager.instance.getXOffset() - (float)FBORenderChunkManager.instance.renderChunk.w / 4.0F;
                                 var15.m_renderY = var2.sy + FBORenderChunkManager.instance.getYOffset();
                              } else {
                                 var15.m_renderX = var2.sx + FBORenderChunkManager.instance.getXOffset();
                                 var15.m_renderY = var2.sy + FBORenderChunkManager.instance.getYOffset();
                              }
                           } else {
                              boolean var16 = true;
                           }

                           var15.m_renderWidth = (float)var9.getWidthOrig() * var12;
                           var15.m_renderHeight = (float)var9.getHeightOrig() * var13;
                           var15.m_renderScaleX = var12;
                           var15.m_renderScaleY = var13;
                           var15.m_renderAlpha = info.a;
                        }
                     }
                  }
               }
            } else {
               if (IsoObjectPicker.Instance.wasDirty && IsoCamera.frameState.playerIndex == 0 && var2 != null) {
                  boolean var17 = var3 == IsoDirections.W || var3 == IsoDirections.SW || var3 == IsoDirections.S;
                  if (var1.Flip) {
                     var17 = !var17;
                  }

                  var5 = var2.sx + globalOffsetX;
                  var6 = var2.sy + globalOffsetY;
                  if (var2 instanceof IsoMovingObject) {
                     var5 -= (float)(var9.getWidthOrig() / 2) * var12;
                     var6 -= (float)var9.getHeightOrig() * var13;
                  }

                  IsoObjectPicker.Instance.Add((int)var5, (int)var6, (int)((float)var9.getWidthOrig() * var12), (int)((float)var9.getHeightOrig() * var13), var2.square, var2, var17, var12, var13);
               }

            }
         }
      }
   }

   private void renderSpriteOutline(float var1, float var2, Texture var3, float var4, float var5) {
      LineDrawer.drawRect(var1, var2, (float)var3.getWidthOrig() * var4, (float)var3.getHeightOrig() * var5, 1.0F, 1.0F, 1.0F, 1.0F, 1);
      LineDrawer.drawRect(var1 + var3.getOffsetX() * var4, var2 + var3.getOffsetY() * var5, (float)var3.getWidth() * var4, (float)var3.getHeight() * var5, 1.0F, 1.0F, 1.0F, 1.0F, 1);
   }

   public void renderActiveModel() {
      if (DebugOptions.instance.IsoSprite.RenderModels.getValue()) {
         GameProfiler.getInstance().invokeAndMeasure("Render Active Model", this.modelSlot, (var0) -> {
            var0.model.updateLights();
            ModelCameraRenderData var1 = (ModelCameraRenderData)ModelCameraRenderData.s_pool.alloc();
            var1.init(CharacterModelCamera.instance, var0);
            SpriteRenderer.instance.drawGeneric(var1);
            SpriteRenderer.instance.drawModel(var0);
         });
      }
   }

   public void renderBloodSplat(float var1, float var2, float var3, ColorInfo var4) {
      Texture var5 = this.getTextureForCurrentFrame(IsoDirections.N);
      if (var5 != null) {
         boolean var6 = true;
         boolean var7 = true;
         byte var15 = 0;
         byte var16 = 0;

         try {
            if (globalOffsetX == -1.0F) {
               globalOffsetX = -IsoCamera.frameState.OffX;
               globalOffsetY = -IsoCamera.frameState.OffY;
            }

            float var10 = globalOffsetX;
            float var11 = globalOffsetY;
            if (FBORenderChunkManager.instance.isCaching()) {
               var10 = FBORenderChunkManager.instance.getXOffset();
               var11 = FBORenderChunkManager.instance.getYOffset();
               var1 -= (float)(FBORenderChunkManager.instance.renderChunk.chunk.wx * 8);
               var2 -= (float)(FBORenderChunkManager.instance.renderChunk.chunk.wy * 8);
            }

            float var12 = IsoUtils.XToScreen(var1, var2, var3, 0);
            float var13 = IsoUtils.YToScreen(var1, var2, var3, 0);
            var12 = (float)((int)var12);
            var13 = (float)((int)var13);
            var12 -= (float)var15;
            var13 -= (float)var16;
            var12 -= (float)var5.getWidth() / 2.0F * (float)Core.TileScale;
            var13 -= (float)var5.getHeight() / 2.0F * (float)Core.TileScale;
            var12 += var10;
            var13 += var11;
            if (!PerformanceSettings.FBORenderChunk) {
               if (var12 >= (float)IsoCamera.frameState.OffscreenWidth || var12 + 64.0F <= 0.0F) {
                  return;
               }

               if (var13 >= (float)IsoCamera.frameState.OffscreenHeight || var13 + 64.0F <= 0.0F) {
                  return;
               }
            }

            info.r = var4.r;
            info.g = var4.g;
            info.b = var4.b;
            info.a = var4.a;
            SpriteRenderer.instance.StartShader(SceneShaderStore.DefaultShaderID, IsoCamera.frameState.playerIndex);
            IndieGL.disableDepthTest();
            IndieGL.glBlendFuncSeparate(770, 771, 773, 1);
            var5.render(var12, var13, (float)var5.getWidth(), (float)var5.getHeight(), info.r, info.g, info.b, info.a, TileDepthModifier.instance);
         } catch (Exception var14) {
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, (String)null, var14);
         }

      }
   }

   public void renderObjectPicker(IsoSpriteInstance var1, IsoObject var2, IsoDirections var3) {
      if (var1 != null) {
         if (IsoPlayer.getInstance() == IsoPlayer.players[0]) {
            Texture var4 = this.getTextureForFrame((int)var1.Frame, var3);
            if (var4 != null) {
               float var5 = var2.sx + globalOffsetX;
               float var6 = var2.sy + globalOffsetY;
               if (var2 instanceof IsoMovingObject) {
                  var5 -= (float)(var4.getWidthOrig() / 2) * var1.getScaleX();
                  var6 -= (float)var4.getHeightOrig() * var1.getScaleY();
               }

               if (IsoObjectPicker.Instance.wasDirty && IsoCamera.frameState.playerIndex == 0) {
                  boolean var7 = var3 == IsoDirections.W || var3 == IsoDirections.SW || var3 == IsoDirections.S;
                  if (var1.Flip) {
                     var7 = !var7;
                  }

                  IsoObjectPicker.Instance.Add((int)var5, (int)var6, (int)((float)var4.getWidthOrig() * var1.getScaleX()), (int)((float)var4.getHeightOrig() * var1.getScaleY()), var2.square, var2, var7, var1.getScaleX(), var1.getScaleY());
               }

            }
         }
      }
   }

   public IsoDirectionFrame getAnimFrame(int var1) {
      if (this.CurrentAnim != null && !this.CurrentAnim.Frames.isEmpty()) {
         if (this.CurrentAnim.FramesArray == null) {
            this.CurrentAnim.FramesArray = (IsoDirectionFrame[])this.CurrentAnim.Frames.toArray(new IsoDirectionFrame[0]);
         }

         if (this.CurrentAnim.FramesArray.length != this.CurrentAnim.Frames.size()) {
            this.CurrentAnim.FramesArray = (IsoDirectionFrame[])this.CurrentAnim.Frames.toArray(this.CurrentAnim.FramesArray);
         }

         if (var1 >= this.CurrentAnim.FramesArray.length) {
            var1 = this.CurrentAnim.FramesArray.length - 1;
         }

         if (var1 < 0) {
            var1 = 0;
         }

         return this.CurrentAnim.FramesArray[var1];
      } else {
         return null;
      }
   }

   public Texture getTextureForFrame(int var1, IsoDirections var2) {
      return this.CurrentAnim != null && !this.CurrentAnim.Frames.isEmpty() ? this.getAnimFrame(var1).getTexture(var2) : this.texture;
   }

   public Texture getTextureForCurrentFrame(IsoDirections var1) {
      this.initSpriteInstance();
      return this.getTextureForFrame((int)this.def.Frame, var1);
   }

   public void update() {
      this.update(this.def);
   }

   public void update(IsoSpriteInstance var1) {
      if (var1 == null) {
         var1 = IsoSpriteInstance.get(this);
      }

      if (this.CurrentAnim == null) {
         var1.Frame = 0.0F;
      } else {
         if (this.Animate && !var1.Finished) {
            float var2 = var1.Frame;
            if (!GameTime.isGamePaused()) {
               var1.Frame += var1.AnimFrameIncrease * GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F;
            }

            if ((int)var1.Frame >= this.CurrentAnim.Frames.size() && this.Loop && var1.Looped) {
               var1.Frame = 0.0F;
            }

            if ((int)var2 != (int)var1.Frame) {
               var1.NextFrame = true;
            }

            if ((int)var1.Frame >= this.CurrentAnim.Frames.size() && (!this.Loop || !var1.Looped)) {
               var1.Finished = true;
               var1.Frame = (float)this.CurrentAnim.FinishUnloopedOnFrame;
               if (this.DeleteWhenFinished) {
                  this.Dispose();
                  this.Animate = false;
               }
            }
         }

      }
   }

   public void CacheAnims(String var1) {
      this.name = var1;
      Stack var2 = new Stack();

      for(int var3 = 0; var3 < this.AnimStack.size(); ++var3) {
         IsoAnim var4 = (IsoAnim)this.AnimStack.get(var3);
         String var5 = var1 + var4.name;
         var2.add(var5);
         if (!IsoAnim.GlobalAnimMap.containsKey(var5)) {
            IsoAnim.GlobalAnimMap.put(var5, var4);
         }
      }

      AnimNameSet.put(var1, var2.toArray());
   }

   public void LoadCache(String var1) {
      this.allocateAnimationIfNeeded();
      Object[] var2 = (Object[])AnimNameSet.get(var1);
      this.name = var1;

      for(int var3 = 0; var3 < var2.length; ++var3) {
         String var4 = (String)var2[var3];
         IsoAnim var5 = (IsoAnim)IsoAnim.GlobalAnimMap.get(var4);
         this.AnimMap.put(var5.name, var5);
         this.AnimStack.add(var5);
         this.CurrentAnim = var5;
      }

   }

   public IsoSprite setFromCache(String var1, String var2, int var3) {
      String var4 = var1 + var2;
      if (HasCache(var4)) {
         this.LoadCache(var4);
      } else {
         this.LoadFramesNoDirPage(var1, var2, var3);
         this.CacheAnims(var4);
      }

      return this;
   }

   public IsoObjectType getType() {
      return this.type;
   }

   public void setType(IsoObjectType var1) {
      this.type = var1;
   }

   public void AddProperties(IsoSprite var1) {
      this.getProperties().AddProperties(var1.getProperties());
   }

   public int getID() {
      return this.ID;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public ColorInfo getTintMod() {
      return this.TintMod;
   }

   public void setTintMod(ColorInfo var1) {
      this.TintMod.set(var1);
   }

   public void setAnimate(boolean var1) {
      this.Animate = var1;
   }

   public IsoSpriteGrid getSpriteGrid() {
      return this.spriteGrid;
   }

   public void setSpriteGrid(IsoSpriteGrid var1) {
      this.spriteGrid = var1;
   }

   public boolean isMoveWithWind() {
      return this.moveWithWind;
   }

   public int getSheetGridIdFromName() {
      return this.name != null ? getSheetGridIdFromName(this.name) : -1;
   }

   public static int getSheetGridIdFromName(String var0) {
      if (var0 != null) {
         int var1 = var0.lastIndexOf(95);
         if (var1 > 0 && var1 + 1 < var0.length()) {
            return Integer.parseInt(var0.substring(var1 + 1));
         }
      }

      return -1;
   }

   public IsoDirections getFacing() {
      if (this.getProperties().Is("Facing")) {
         String var1 = this.getProperties().Val("Facing");
         if (var1 != null) {
            switch (var1) {
               case "N":
                  return IsoDirections.N;
               case "S":
                  return IsoDirections.S;
               case "W":
                  return IsoDirections.W;
               case "E":
                  return IsoDirections.E;
            }
         }
      }

      return null;
   }

   private void initRoofProperties() {
      if (!this.bInitRoofProperties) {
         this.bInitRoofProperties = true;
         this.roofProperties = RoofProperties.initSprite(this);
      }
   }

   public RoofProperties getRoofProperties() {
      this.initRoofProperties();
      return this.roofProperties;
   }

   private static class l_renderCurrentAnim {
      static final Vector3 colorInfoBackup = new Vector3();
      static final Vector3 spritePos = new Vector3();

      private l_renderCurrentAnim() {
      }
   }
}
