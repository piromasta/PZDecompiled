package zombie.iso.objects;

import fmod.fmod.FMODManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL20;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.inventory.types.HandWeapon;
import zombie.iso.CellLoader;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.network.GameServer;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

public class IsoTree extends IsoObject {
   public static final int MAX_SIZE = 6;
   public int LogYield = 1;
   public int damage = 500;
   public int size = 4;
   public boolean bRenderFlag;
   public boolean bWasFaded = false;
   public boolean bUseTreeShader = false;
   public float fadeAlpha = 1.0F;
   private static final IsoGameCharacter.Location[] s_chopTreeLocation = new IsoGameCharacter.Location[4];
   private static final ArrayList<IsoGridSquare> s_chopTreeIndicators = new ArrayList();
   private static IsoTree s_chopTreeHighlighted = null;

   public static IsoTree getNew() {
      synchronized(CellLoader.isoTreeCache) {
         if (CellLoader.isoTreeCache.isEmpty()) {
            return new IsoTree();
         } else {
            IsoTree var1 = (IsoTree)CellLoader.isoTreeCache.pop();
            var1.sx = 0.0F;
            return var1;
         }
      }
   }

   public IsoTree() {
   }

   public IsoTree(IsoCell var1) {
      super(var1);
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.put((byte)this.LogYield);
      var1.put((byte)(this.damage / 10));
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.LogYield = var1.get();
      this.damage = var1.get() * 10;
      if (this.sprite != null && this.sprite.getProperties().Val("tree") != null) {
         this.size = Integer.parseInt(this.sprite.getProperties().Val("tree"));
         if (this.size < 1) {
            this.size = 1;
         }

         if (this.size > 6) {
            this.size = 6;
         }
      }

   }

   protected void checkMoveWithWind() {
      this.checkMoveWithWind(true);
   }

   public void reset() {
      super.reset();
   }

   public IsoTree(IsoGridSquare var1, String var2) {
      super(var1, var2, false);
      this.initTree();
   }

   public IsoTree(IsoGridSquare var1, IsoSprite var2) {
      super(var1.getCell(), var1, var2);
      this.initTree();
   }

   public void initTree() {
      this.setType(IsoObjectType.tree);
      if (this.sprite.getProperties().Val("tree") != null) {
         this.size = Integer.parseInt(this.sprite.getProperties().Val("tree"));
         if (this.size < 1) {
            this.size = 1;
         }

         if (this.size > 6) {
            this.size = 6;
         }
      } else {
         this.size = 4;
      }

      switch (this.size) {
         case 1:
         case 2:
            this.LogYield = 1;
            break;
         case 3:
         case 4:
            this.LogYield = 2;
            break;
         case 5:
            this.LogYield = 3;
            break;
         case 6:
            this.LogYield = 4;
      }

      this.damage = this.LogYield * 80;
   }

   public String getObjectName() {
      return "Tree";
   }

   public void Damage(float var1) {
      float var2 = var1 * 0.05F;
      this.damage = (int)((float)this.damage - var2);
      if (this.damage <= 0) {
         this.square.transmitRemoveItemFromSquare(this);
         this.square.RecalcAllWithNeighbours(true);
         int var3 = this.LogYield;

         int var4;
         for(var4 = 0; var4 < var3; ++var4) {
            this.square.AddWorldInventoryItem("Base.Log", 0.0F, 0.0F, 0.0F);
            if (var4 > 3 && Rand.NextBool(4)) {
               this.square.AddWorldInventoryItem("Base.LargeBranch", 0.0F, 0.0F, 0.0F);
            } else if (var4 > 2 && Rand.NextBool(4)) {
               this.square.AddWorldInventoryItem("Base.Sapling", 0.0F, 0.0F, 0.0F);
            } else if (Rand.NextBool(4)) {
               this.square.AddWorldInventoryItem("Base.TreeBranch2", 0.0F, 0.0F, 0.0F);
            }

            if (Rand.NextBool(4)) {
               this.square.AddWorldInventoryItem("Base.Twigs", 0.0F, 0.0F, 0.0F);
            }
         }

         this.reset();
         synchronized(CellLoader.isoTreeCache) {
            CellLoader.isoTreeCache.add(this);
         }

         for(var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
            LosUtil.cachecleared[var4] = true;
         }

         IsoGridSquare.setRecalcLightTime(-1.0F);
         GameTime.instance.lightSourceUpdate = 100.0F;
         LuaEventManager.triggerEvent("OnContainerUpdate");
      }

   }

   public void HitByVehicle(BaseVehicle var1, float var2) {
      BaseSoundEmitter var3 = IsoWorld.instance.getFreeEmitter((float)this.square.x + 0.5F, (float)this.square.y + 0.5F, (float)this.square.z);
      long var4 = var3.playSound("VehicleHitTree");
      var3.setParameterValue(var4, FMODManager.instance.getParameterDescription("VehicleSpeed"), var1.getCurrentSpeedKmHour());
      WorldSoundManager.instance.addSound((Object)null, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
      this.Damage((float)this.damage);
   }

   public void WeaponHitEffects(IsoGameCharacter var1, HandWeapon var2) {
      if (var1 instanceof IsoPlayer) {
         ((IsoPlayer)var1).setMeleeHitSurface(ParameterMeleeHitSurface.Material.Tree);
         var1.getEmitter().playSound(var2.getZombieHitSound());
      } else {
         var1.getEmitter().playSound("ChopTree");
      }

      WorldSoundManager.instance.addSound((Object)null, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
      this.setRenderEffect(RenderEffectType.Hit_Tree_Shudder, true);
   }

   public void WeaponHit(IsoGameCharacter var1, HandWeapon var2) {
      if (!GameServer.bServer) {
         this.WeaponHitEffects(var1, var2);
      }

      float var3 = (float)var2.getTreeDamage();
      if (var1.Traits.Axeman.isSet() && var2.getCategories().contains("Axe")) {
         var3 *= 1.5F;
      }

      this.damage = (int)((float)this.damage - var3);
      if (this.damage <= 0) {
         this.square.transmitRemoveItemFromSquare(this);
         var1.getEmitter().playSound("FallingTree");
         this.square.RecalcAllWithNeighbours(true);
         int var4 = this.LogYield;

         int var5;
         for(var5 = 0; var5 < var4; ++var5) {
            this.square.AddWorldInventoryItem("Base.Log", 0.0F, 0.0F, 0.0F);
            if (var5 > 3 && Rand.NextBool(4)) {
               this.square.AddWorldInventoryItem("Base.LargeBranch", 0.0F, 0.0F, 0.0F);
            } else if (var5 > 2 && Rand.NextBool(4)) {
               this.square.AddWorldInventoryItem("Base.Sapling", 0.0F, 0.0F, 0.0F);
            } else if (Rand.NextBool(4)) {
               this.square.AddWorldInventoryItem("Base.TreeBranch2", 0.0F, 0.0F, 0.0F);
            }

            if (Rand.NextBool(4)) {
               this.square.AddWorldInventoryItem("Base.Twigs", 0.0F, 0.0F, 0.0F);
            }
         }

         this.reset();
         synchronized(CellLoader.isoTreeCache) {
            CellLoader.isoTreeCache.add(this);
         }

         for(var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
            LosUtil.cachecleared[var5] = true;
         }

         IsoGridSquare.setRecalcLightTime(-1.0F);
         GameTime.instance.lightSourceUpdate = 100.0F;
         LuaEventManager.triggerEvent("OnContainerUpdate");
      }

   }

   public void setHealth(int var1) {
      this.damage = Math.max(var1, 0);
   }

   public int getHealth() {
      return this.damage;
   }

   public int getMaxHealth() {
      return this.LogYield * 80;
   }

   public int getSize() {
      return this.size;
   }

   public float getSlowFactor(IsoMovingObject var1) {
      float var2 = 1.0F;
      if (var1 instanceof IsoGameCharacter) {
         if ("parkranger".equals(((IsoGameCharacter)var1).getDescriptor().getProfession())) {
            var2 = 1.5F;
         }

         if ("lumberjack".equals(((IsoGameCharacter)var1).getDescriptor().getProfession())) {
            var2 = 1.2F;
         }
      }

      if (this.size != 1 && this.size != 2) {
         return this.size != 3 && this.size != 4 ? 0.3F * var2 : 0.5F * var2;
      } else {
         return 0.8F * var2;
      }
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.isHighlighted()) {
         if (this.square != null) {
            s_chopTreeHighlighted = this;
         }

      } else {
         int var8 = IsoCamera.frameState.playerIndex;
         if (!this.bRenderFlag && !(this.fadeAlpha < this.getTargetAlpha(var8)) || PerformanceSettings.FBORenderChunk && !FBORenderCell.instance.bRenderTranslucentOnly) {
            this.renderInner(var1, var2, var3, var4, var5, false);
         } else {
            IndieGL.enableStencilTest();
            IndieGL.glBlendFunc(770, 771);
            boolean var9 = IsoCamera.frameState.CamCharacterSquare != null && !IsoCamera.frameState.CamCharacterSquare.Is(IsoFlagType.exterior);
            IndieGL.glStencilFunc(517, 128, 128);
            this.renderInner(var1, var2, var3, var4, var5, false);
            float var10 = 0.044999998F * GameTime.getInstance().getThirtyFPSMultiplier();
            float var11 = var9 ? 0.05F : 0.25F;
            if (this.bRenderFlag && this.fadeAlpha > var11) {
               this.fadeAlpha -= var10;
               if (this.fadeAlpha < var11) {
                  this.fadeAlpha = var11;
               }
            }

            float var12;
            if (!this.bRenderFlag) {
               var12 = this.getTargetAlpha(var8);
               if (this.fadeAlpha < var12) {
                  this.fadeAlpha += var10;
                  if (this.fadeAlpha > var12) {
                     this.fadeAlpha = var12;
                  }
               }
            }

            var12 = this.getAlpha(var8);
            float var13 = this.getTargetAlpha(var8);
            this.setAlphaAndTarget(var8, this.fadeAlpha);
            IndieGL.glStencilFunc(514, 128, 128);
            this.renderInner(var1, var2, var3, var4, true, false);
            this.setAlpha(var8, var12);
            this.setTargetAlpha(var8, var13);
            if (IsoTree.TreeShader.instance.StartShader()) {
               IsoTree.TreeShader.instance.setOutlineColor(0.1F, 0.1F, 0.1F, var9 && this.fadeAlpha < 0.5F ? this.fadeAlpha : 1.0F - this.fadeAlpha);
               this.renderInner(var1, var2, var3, var4, true, true);
               IndieGL.EndShader();
            }

            IndieGL.glStencilFunc(519, 255, 255);
            IndieGL.glDefaultBlendFunc();
         }

         if (!PerformanceSettings.FBORenderChunk) {
            this.checkChopTreeIndicator();
         }

      }
   }

   private void renderInner(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6) {
      float var7;
      float var8;
      if (this.sprite != null && this.sprite.name != null && this.sprite.name.contains("JUMBO")) {
         var7 = this.offsetX;
         var8 = this.offsetY;
         this.offsetX = (float)(384 * Core.TileScale / 2 - 96 * Core.TileScale);
         this.offsetY = (float)(256 * Core.TileScale - 32 * Core.TileScale);
         if (this.offsetX != var7 || this.offsetY != var8) {
            this.sx = 0.0F;
         }
      } else {
         var7 = this.offsetX;
         var8 = this.offsetY;
         this.offsetX = (float)(32 * Core.TileScale);
         this.offsetY = (float)(96 * Core.TileScale);
         if (this.offsetX != var7 || this.offsetY != var8) {
            this.sx = 0.0F;
         }
      }

      if (var6 && this.sprite != null) {
         Texture var13 = this.sprite.getTextureForCurrentFrame(this.dir);
         if (var13 != null) {
            IsoTree.TreeShader.instance.setStepSize(0.25F, var13.getWidth(), var13.getHeight());
         }
      }

      boolean var14 = this.bRenderFlag;
      if (!var6) {
         this.bRenderFlag = false;
      }

      this.bUseTreeShader = var6;
      super.render(var1, var2, var3, var4, false, false, (Shader)null);
      if (this.AttachedAnimSprite != null) {
         int var15 = this.AttachedAnimSprite.size();

         for(int var9 = 0; var9 < var15; ++var9) {
            IsoSpriteInstance var10 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var9);
            int var11 = IsoCamera.frameState.playerIndex;
            float var12 = this.getTargetAlpha(var11);
            this.setTargetAlpha(var11, 1.0F);
            var10.render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY, this.isHighlighted() ? this.getHighlightColor() : var4);
            this.setTargetAlpha(var11, var12);
            var10.update();
         }
      }

      this.bRenderFlag = var14;
   }

   protected boolean isUpdateAlphaDuringRender() {
      return false;
   }

   public void setSprite(IsoSprite var1) {
      super.setSprite(var1);
      this.initTree();
   }

   public boolean isMaskClicked(int var1, int var2, boolean var3) {
      if (super.isMaskClicked(var1, var2, var3)) {
         return true;
      } else if (this.AttachedAnimSprite == null) {
         return false;
      } else {
         for(int var4 = 0; var4 < this.AttachedAnimSprite.size(); ++var4) {
            if (((IsoSpriteInstance)this.AttachedAnimSprite.get(var4)).parentSprite.isMaskClicked(this.dir, var1, var2, var3)) {
               return true;
            }
         }

         return false;
      }
   }

   public static void setChopTreeCursorLocation(int var0, int var1, int var2, int var3) {
      if (s_chopTreeLocation[var0] == null) {
         s_chopTreeLocation[var0] = new IsoGameCharacter.Location(-1, -1, -1);
      }

      IsoGameCharacter.Location var4 = s_chopTreeLocation[var0];
      var4.x = var1;
      var4.y = var2;
      var4.z = var3;
   }

   public void checkChopTreeIndicator() {
      if (!this.isHighlighted()) {
         int var1 = IsoCamera.frameState.playerIndex;
         IsoGameCharacter.Location var2 = s_chopTreeLocation[var1];
         if (var2 != null && var2.x != -1 && this.square != null) {
            if (this.getCell().getDrag(var1) == null) {
               var2.x = -1;
            } else {
               if (IsoUtils.DistanceToSquared((float)this.square.x + 0.5F, (float)this.square.y + 0.5F, (float)var2.x + 0.5F, (float)var2.y + 0.5F) < 12.25F) {
                  s_chopTreeIndicators.add(this.square);
               }

            }
         }
      }
   }

   public static void checkChopTreeIndicators(int var0) {
      IsoGameCharacter.Location var1 = s_chopTreeLocation[var0];
      if (var1 != null && var1.x != -1) {
         if (IsoWorld.instance.CurrentCell.getDrag(var0) == null) {
            var1.x = -1;
         } else {
            int var2 = ((int)Math.floor((double)var1.x) - 4) / 8 - 1;
            int var3 = ((int)Math.floor((double)var1.y) - 4) / 8 - 1;
            int var4 = (int)Math.ceil((double)((var1.x + 4) / 8)) + 1;
            int var5 = (int)Math.ceil((double)((var1.y + 4) / 8)) + 1;
            IsoChunkMap var6 = IsoWorld.instance.CurrentCell.getChunkMap(var0);
            if (!var6.ignore) {
               for(int var7 = var3; var7 < var5; ++var7) {
                  for(int var8 = var2; var8 < var4; ++var8) {
                     IsoChunk var9 = var6.getChunkForGridSquare(var8 * 8, var7 * 8);
                     if (var9 != null && var9.bLoaded && var9.IsOnScreen(true)) {
                        FBORenderLevels var10 = var9.getRenderLevels(var0);
                        if (var10.isOnScreen(0)) {
                           ArrayList var11 = var10.m_treeSquares;

                           for(int var12 = 0; var12 < var11.size(); ++var12) {
                              IsoGridSquare var13 = (IsoGridSquare)var11.get(var12);
                              IsoTree var14 = var13.getTree();
                              if (var14 != null && !var14.isHighlighted() && IsoUtils.DistanceToSquared((float)var13.x + 0.5F, (float)var13.y + 0.5F, (float)var1.x + 0.5F, (float)var1.y + 0.5F) < 12.25F) {
                                 s_chopTreeIndicators.add(var13);
                              }
                           }
                        }
                     }
                  }
               }

            }
         }
      }
   }

   public static void renderChopTreeIndicators() {
      if (!s_chopTreeIndicators.isEmpty()) {
         if (PerformanceSettings.FBORenderChunk) {
            IndieGL.disableDepthTest();
         }

         PZArrayUtil.forEach((List)s_chopTreeIndicators, IsoTree::renderChopTreeIndicator);
         s_chopTreeIndicators.clear();
      }

      if (s_chopTreeHighlighted != null) {
         IsoTree var0 = s_chopTreeHighlighted;
         s_chopTreeHighlighted = null;
         if (PerformanceSettings.FBORenderChunk) {
            FBORenderObjectHighlight.getInstance().setRenderingGhostTile(true);
         }

         var0.renderInner((float)var0.square.x, (float)var0.square.y, (float)var0.square.z, var0.getHighlightColor(), false, false);
         if (PerformanceSettings.FBORenderChunk) {
            FBORenderObjectHighlight.getInstance().setRenderingGhostTile(false);
         }
      }

   }

   private static void renderChopTreeIndicator(IsoGridSquare var0) {
      Texture var1 = Texture.getSharedTexture("media/ui/chop_tree.png");
      if (var1 != null && var1.isReady()) {
         float var2 = (float)var0.x;
         float var3 = (float)var0.y;
         float var4 = (float)var0.z;
         float var5 = IsoUtils.XToScreen(var2, var3, var4, 0) + IsoSprite.globalOffsetX;
         float var6 = IsoUtils.YToScreen(var2, var3, var4, 0) + IsoSprite.globalOffsetY;
         var5 -= (float)(32 * Core.TileScale);
         var6 -= (float)(96 * Core.TileScale);
         IndieGL.StartShader(0);
         SpriteRenderer.instance.render(var1, var5, var6, (float)(64 * Core.TileScale), (float)(128 * Core.TileScale), 0.0F, 0.5F, 0.0F, 0.75F, (Consumer)null);
      }
   }

   public IsoGridSquare getRenderSquare() {
      if (this.getSquare() == null) {
         return null;
      } else {
         Texture var1 = this.getSprite().getTextureForCurrentFrame(this.getDir());
         if (var1 != null && var1.getName() != null && var1.getName().contains("JUMBO")) {
            byte var2 = 8;
            if (PZMath.coordmodulo(this.square.x, var2) == 0 && PZMath.coordmodulo(this.square.y, var2) == var2 - 1) {
               return this.square.getAdjacentSquare(IsoDirections.S);
            }

            if (PZMath.coordmodulo(this.square.x, var2) == var2 - 1 && PZMath.coordmodulo(this.square.y, var2) == 0) {
               return this.square.getAdjacentSquare(IsoDirections.E);
            }
         }

         return this.getSquare();
      }
   }

   public static class TreeShader {
      public static final TreeShader instance = new TreeShader();
      private ShaderProgram shaderProgram;
      private int stepSize;
      private int outlineColor;
      private int chunkDepth;
      private int zDepth;

      public TreeShader() {
      }

      public void initShader() {
         this.shaderProgram = ShaderProgram.createShaderProgram("tree", false, false, true);
         if (this.shaderProgram.isCompiled()) {
            this.stepSize = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "stepSize");
            this.outlineColor = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "outlineColor");
            this.chunkDepth = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "chunkDepth");
            this.zDepth = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "zDepth");
            ShaderHelper.glUseProgramObjectARB(this.shaderProgram.getShaderID());
            GL20.glUniform2f(this.stepSize, 0.001F, 0.001F);
            ShaderHelper.glUseProgramObjectARB(0);
         }

      }

      public void setOutlineColor(float var1, float var2, float var3, float var4) {
         SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.outlineColor, var1, var2, var3, var4);
      }

      public void setStepSize(float var1, int var2, int var3) {
         SpriteRenderer.instance.ShaderUpdate2f(this.shaderProgram.getShaderID(), this.stepSize, var1 / (float)var2, var1 / (float)var3);
      }

      public void setDepth(float var1, float var2) {
         SpriteRenderer.instance.ShaderUpdate1f(this.shaderProgram.getShaderID(), this.chunkDepth, var1);
         SpriteRenderer.instance.ShaderUpdate1f(this.shaderProgram.getShaderID(), this.zDepth, var2);
      }

      public boolean StartShader() {
         if (this.shaderProgram == null) {
            RenderThread.invokeOnRenderContext(this::initShader);
         }

         if (this.shaderProgram.isCompiled()) {
            IndieGL.StartShader(this.shaderProgram.getShaderID(), 0);
            return true;
         } else {
            return false;
         }
      }
   }
}
