package zombie.core.skinnedmodel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;
import java.util.function.Consumer;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.AttachedItems.AttachedModelName;
import zombie.characters.AttachedItems.AttachedModelNames;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.interfaces.ITexture;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.PlayerCamera;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.ShadowParams;
import zombie.popman.ObjectPool;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.UI3DScene;

public final class DeadBodyAtlas {
   public static final int ATLAS_SIZE = 1024;
   private TextureFBO fbo;
   public static final DeadBodyAtlas instance = new DeadBodyAtlas();
   private static final Vector2 tempVector2 = new Vector2();
   private final HashMap<String, BodyTexture> EntryMap = new HashMap();
   private final ArrayList<Atlas> AtlasList = new ArrayList();
   private final BodyParams bodyParams = new BodyParams();
   private int updateCounter = -1;
   private final Checksummer checksummer = new Checksummer();
   private static final Stack<RenderJob> JobPool = new Stack();
   private final DebugDrawInWorld[] debugDrawInWorld = new DebugDrawInWorld[3];
   private long debugDrawTime;
   private final ArrayList<RenderJob> RenderJobs = new ArrayList();
   private final CharacterTextureVisual characterTextureVisualFemale = new CharacterTextureVisual(true);
   private final CharacterTextureVisual characterTextureVisualMale = new CharacterTextureVisual(false);
   private final CharacterTextures characterTexturesFemale = new CharacterTextures();
   private final CharacterTextures characterTexturesMale = new CharacterTextures();
   private final ObjectPool<BodyTextureDrawer> bodyTextureDrawerPool = new ObjectPool(BodyTextureDrawer::new);
   private static final ObjectPool<BodyTextureDepthDrawer> s_BodyTextureDepthDrawerPool = new ObjectPool(BodyTextureDepthDrawer::new);
   public static Shader DeadBodyAtlasShader;

   public DeadBodyAtlas() {
   }

   public void lightingUpdate(int var1, boolean var2) {
      if (var1 != this.updateCounter && var2) {
         this.updateCounter = var1;
      }

   }

   public BodyTexture getBodyTexture(IsoDeadBody var1) {
      this.bodyParams.init(var1);
      return this.getBodyTexture(this.bodyParams);
   }

   public BodyTexture getBodyTexture(IsoZombie var1) {
      this.bodyParams.init(var1);
      return this.getBodyTexture(this.bodyParams);
   }

   public BodyTexture getBodyTexture(IsoMannequin var1) {
      this.bodyParams.init(var1);
      return this.getBodyTexture(this.bodyParams);
   }

   public BodyTexture getBodyTexture(boolean var1, String var2, String var3, IsoDirections var4, int var5, float var6) {
      CharacterTextures var7 = var1 ? this.characterTexturesFemale : this.characterTexturesMale;
      BodyTexture var8 = var7.getTexture(var2, var3, var4, var5);
      if (var8 != null) {
         return var8;
      } else {
         this.bodyParams.init(var1 ? this.characterTextureVisualFemale : this.characterTextureVisualMale, var4, var2, var3, var6);
         this.bodyParams.variables.put("zombieWalkType", "1");
         BodyTexture var9 = this.getBodyTexture(this.bodyParams);
         var7.addTexture(var2, var3, var4, var5, var9);
         return var9;
      }
   }

   public BodyTexture getBodyTexture(BodyParams var1) {
      String var2 = this.getBodyKey(var1);
      BodyTexture var3 = (BodyTexture)this.EntryMap.get(var2);
      if (var3 != null) {
         return var3;
      } else {
         AtlasEntry var4 = new AtlasEntry();
         var4.key = var2;
         var4.lightKey = this.getLightKey(var1);
         var4.updateCounter = this.updateCounter;
         var3 = new BodyTexture();
         var3.entry = var4;
         this.EntryMap.put(var2, var3);
         this.RenderJobs.add(DeadBodyAtlas.RenderJob.getNew().init(var1, var4));
         return var3;
      }
   }

   public void invalidateBodyTexture(BodyTexture var1, IsoDeadBody var2) {
      if (var1 != null && var1.entry != null && var1.entry.ready && var1.entry.atlas.tex != null) {
         this.bodyParams.init(var2);
         var1.entry.ready = false;
         RenderJob var3 = DeadBodyAtlas.RenderJob.getNew().init(this.bodyParams, var1.entry);
         var3.bClearThisSlotOnly = true;
         this.RenderJobs.add(var3);
      }
   }

   public void checkLights(Texture var1, IsoDeadBody var2) {
      if (var1 != null) {
         BodyTexture var3 = (BodyTexture)this.EntryMap.get(var1.getName());
         if (var3 != null) {
            AtlasEntry var4 = var3.entry;
            if (var4 != null && var4.tex == var1) {
               if (var4.updateCounter != this.updateCounter) {
                  var4.updateCounter = this.updateCounter;
                  this.bodyParams.init(var2);
                  String var5 = this.getLightKey(this.bodyParams);
                  if (!var4.lightKey.equals(var5)) {
                     this.EntryMap.remove(var4.key);
                     var4.key = this.getBodyKey(this.bodyParams);
                     var4.lightKey = var5;
                     var1.setNameOnly(var4.key);
                     this.EntryMap.put(var4.key, var3);
                     RenderJob var6 = DeadBodyAtlas.RenderJob.getNew().init(this.bodyParams, var4);
                     var6.bClearThisSlotOnly = true;
                     this.RenderJobs.add(var6);
                     this.render();
                  }
               }
            }
         }
      }
   }

   public void checkLights(Texture var1, IsoZombie var2) {
      if (var1 != null) {
         BodyTexture var3 = (BodyTexture)this.EntryMap.get(var1.getName());
         if (var3 != null) {
            AtlasEntry var4 = var3.entry;
            if (var4 != null && var4.tex == var1) {
               if (var4.updateCounter != this.updateCounter) {
                  var4.updateCounter = this.updateCounter;
                  this.bodyParams.init(var2);
                  String var5 = this.getLightKey(this.bodyParams);
                  if (!var4.lightKey.equals(var5)) {
                     this.EntryMap.remove(var4.key);
                     var4.key = this.getBodyKey(this.bodyParams);
                     var4.lightKey = var5;
                     var1.setNameOnly(var4.key);
                     this.EntryMap.put(var4.key, var3);
                     RenderJob var6 = DeadBodyAtlas.RenderJob.getNew().init(this.bodyParams, var4);
                     var6.bClearThisSlotOnly = true;
                     this.RenderJobs.add(var6);
                     this.render();
                  }
               }
            }
         }
      }
   }

   private void assignEntryToAtlas(AtlasEntry var1, int var2, int var3) {
      if (var1.atlas == null) {
         for(int var4 = 0; var4 < this.AtlasList.size(); ++var4) {
            Atlas var5 = (Atlas)this.AtlasList.get(var4);
            if (!var5.isFull() && var5.ENTRY_WID == var2 && var5.ENTRY_HGT == var3) {
               var5.addEntry(var1);
               return;
            }
         }

         Atlas var6 = new Atlas(1024, 1024, var2, var3);
         var6.addEntry(var1);
         this.AtlasList.add(var6);
      }
   }

   private String getBodyKey(BodyParams var1) {
      if (var1.baseVisual == this.characterTextureVisualFemale.humanVisual) {
         return "SZF_" + var1.animSetName + "_" + var1.stateName + "_" + var1.dir + "_" + var1.trackTime;
      } else if (var1.baseVisual == this.characterTextureVisualMale.humanVisual) {
         return "SZM_" + var1.animSetName + "_" + var1.stateName + "_" + var1.dir + "_" + var1.trackTime;
      } else {
         try {
            this.checksummer.reset();
            AnimalVisual var2 = (AnimalVisual)Type.tryCastTo(var1.baseVisual, AnimalVisual.class);
            int var4;
            AttachedModelName var5;
            ItemVisual var6;
            int var8;
            ItemVisuals var11;
            int var12;
            float var13;
            float var14;
            float var15;
            if (var2 != null) {
               this.checksummer.update((byte)var1.dir.index());
               this.checksummer.update((int)(PZMath.wrap(var1.angle, 0.0F, 6.2831855F) * 57.295776F));
               Model var10 = var1.baseVisual.getModel();
               if (var10 != null) {
                  this.checksummer.update(var10.Name);
               }

               this.checksummer.update(var1.bFemale);
               this.checksummer.update(var1.bSkeleton);
               this.checksummer.update((int)(var2.getAnimalSize() * 100.0F));
               this.checksummer.update(var2.getSkinTexture());

               for(var4 = 0; var4 < var1.attachedModelNames.size(); ++var4) {
                  var5 = var1.attachedModelNames.get(var4);
                  this.checksummer.update(var5.attachmentNameSelf);
                  this.checksummer.update(var5.attachmentNameParent);
                  this.checksummer.update(var5.modelName);
                  this.checksummer.update((int)(var5.bloodLevel * 100.0F));
               }

               var11 = var1.itemVisuals;

               for(var12 = 0; var12 < var11.size(); ++var12) {
                  var6 = (ItemVisual)var11.get(var12);
               }

               this.checksummer.update(var1.fallOnFront);
               this.checksummer.update(var1.bStanding);
               this.checksummer.update(var1.bOutside);
               this.checksummer.update(var1.bRoom);
               var13 = (float)((int)(var1.ambient.r * 10.0F)) / 10.0F;
               this.checksummer.update((byte)((int)(var13 * 255.0F)));
               var14 = (float)((int)(var1.ambient.g * 10.0F)) / 10.0F;
               this.checksummer.update((byte)((int)(var14 * 255.0F)));
               var15 = (float)((int)(var1.ambient.b * 10.0F)) / 10.0F;
               this.checksummer.update((byte)((int)(var15 * 255.0F)));
               this.checksummer.update((int)var1.trackTime);

               for(var8 = 0; var8 < var1.lights.length; ++var8) {
                  this.checksummer.update(var1.lights[var8], var1.x, var1.y, var1.z);
               }

               return this.checksummer.checksumToString();
            } else {
               HumanVisual var3 = (HumanVisual)Type.tryCastTo(var1.baseVisual, HumanVisual.class);
               this.checksummer.update((byte)var1.dir.index());
               this.checksummer.update((int)(PZMath.wrap(var1.angle, 0.0F, 6.2831855F) * 57.295776F));
               this.checksummer.update(var3.getHairModel());
               this.checksummer.update(var3.getBeardModel());
               this.checksummer.update(var3.getSkinColor());
               this.checksummer.update(var3.getSkinTexture());
               this.checksummer.update((int)(var3.getTotalBlood() * 100.0F));
               this.checksummer.update(var1.primaryHandItem);
               this.checksummer.update(var1.secondaryHandItem);

               for(var4 = 0; var4 < var1.attachedModelNames.size(); ++var4) {
                  var5 = var1.attachedModelNames.get(var4);
                  this.checksummer.update(var5.attachmentNameSelf);
                  this.checksummer.update(var5.attachmentNameParent);
                  this.checksummer.update(var5.modelName);
                  this.checksummer.update((int)(var5.bloodLevel * 100.0F));
               }

               this.checksummer.update(var1.bFemale);
               this.checksummer.update(var1.bZombie);
               this.checksummer.update(var1.bSkeleton);
               this.checksummer.update(var1.animSetName);
               this.checksummer.update(var1.stateName);
               var11 = var1.itemVisuals;

               for(var12 = 0; var12 < var11.size(); ++var12) {
                  var6 = (ItemVisual)var11.get(var12);
                  ClothingItem var7 = var6.getClothingItem();
                  if (var7 != null) {
                     this.checksummer.update(var6.getBaseTexture(var7));
                     this.checksummer.update(var6.getTextureChoice(var7));
                     this.checksummer.update(var6.getTint(var7));
                     this.checksummer.update(var7.getModel(var3.isFemale()));
                     this.checksummer.update((int)(var6.getTotalBlood() * 100.0F));
                  }
               }

               this.checksummer.update(var1.fallOnFront);
               this.checksummer.update(var1.bStanding);
               this.checksummer.update(var1.bOutside);
               this.checksummer.update(var1.bRoom);
               var13 = (float)((int)(var1.ambient.r * 10.0F)) / 10.0F;
               this.checksummer.update((byte)((int)(var13 * 255.0F)));
               var14 = (float)((int)(var1.ambient.g * 10.0F)) / 10.0F;
               this.checksummer.update((byte)((int)(var14 * 255.0F)));
               var15 = (float)((int)(var1.ambient.b * 10.0F)) / 10.0F;
               this.checksummer.update((byte)((int)(var15 * 255.0F)));
               this.checksummer.update((int)var1.trackTime);

               for(var8 = 0; var8 < var1.lights.length; ++var8) {
                  this.checksummer.update(var1.lights[var8], var1.x, var1.y, var1.z);
               }

               return this.checksummer.checksumToString();
            }
         } catch (Throwable var9) {
            ExceptionLogger.logException(var9);
            return "bogus";
         }
      }
   }

   private String getLightKey(BodyParams var1) {
      try {
         this.checksummer.reset();
         this.checksummer.update(var1.bOutside);
         this.checksummer.update(var1.bRoom);
         float var2 = (float)((int)(var1.ambient.r * 10.0F)) / 10.0F;
         this.checksummer.update((byte)((int)(var2 * 255.0F)));
         float var3 = (float)((int)(var1.ambient.g * 10.0F)) / 10.0F;
         this.checksummer.update((byte)((int)(var3 * 255.0F)));
         float var4 = (float)((int)(var1.ambient.b * 10.0F)) / 10.0F;
         this.checksummer.update((byte)((int)(var4 * 255.0F)));

         for(int var5 = 0; var5 < var1.lights.length; ++var5) {
            this.checksummer.update(var1.lights[var5], var1.x, var1.y, var1.z);
         }

         return this.checksummer.checksumToString();
      } catch (Throwable var6) {
         ExceptionLogger.logException(var6);
         return "bogus";
      }
   }

   public void render() {
      int var1;
      for(var1 = 0; var1 < this.AtlasList.size(); ++var1) {
         Atlas var2 = (Atlas)this.AtlasList.get(var1);
         if (var2.clear) {
            SpriteRenderer.instance.drawGeneric(new ClearAtlasTexture(var2));
         }
      }

      if (!this.RenderJobs.isEmpty()) {
         for(var1 = 0; var1 < this.RenderJobs.size(); ++var1) {
            RenderJob var3 = (RenderJob)this.RenderJobs.get(var1);
            if (var3.done != 1 || var3.renderRefCount <= 0) {
               if (var3.done == 1 && var3.renderRefCount == 0) {
                  this.RenderJobs.remove(var1--);

                  assert !JobPool.contains(var3);

                  JobPool.push(var3);
               } else if (var3.renderMain()) {
                  ++var3.renderRefCount;
                  SpriteRenderer.instance.drawGeneric(var3);
               }
            }
         }

      }
   }

   public void renderDebug() {
   }

   public void renderUI() {
      if (Core.bDebug && DebugOptions.instance.DeadBodyAtlas.Render.getValue()) {
         boolean var1 = false;
         int var2 = 512 / Core.TileScale;
         int var3 = 0;
         int var4 = 0;

         for(int var5 = 0; var5 < this.AtlasList.size(); ++var5) {
            Atlas var6 = (Atlas)this.AtlasList.get(var5);
            Texture var7 = var1 ? var6.depth : var6.tex;
            SpriteRenderer.instance.renderi((Texture)null, var3, var4, var2, var2, 1.0F, 1.0F, 1.0F, 0.75F, (Consumer)null);
            SpriteRenderer.instance.renderi(var7, var3, var4, var2, var2, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
            float var8 = (float)var2 / (float)var7.getWidth();

            int var9;
            for(var9 = 0; var9 <= var7.getWidth() / var6.ENTRY_WID; ++var9) {
               SpriteRenderer.instance.renderline((Texture)null, (int)((float)var3 + (float)(var9 * var6.ENTRY_WID) * var8), var4, (int)((float)var3 + (float)(var9 * var6.ENTRY_WID) * var8), var4 + var2, 0.5F, 0.5F, 0.5F, 1.0F);
            }

            for(var9 = 0; var9 <= var7.getHeight() / var6.ENTRY_HGT; ++var9) {
               SpriteRenderer.instance.renderline((Texture)null, var3, (int)((float)(var4 + var2) - (float)(var9 * var6.ENTRY_HGT) * var8), var3 + var2, (int)((float)(var4 + var2) - (float)(var9 * var6.ENTRY_HGT) * var8), 0.5F, 0.5F, 0.5F, 1.0F);
            }

            var4 += var2 + 4;
            if (var4 + var2 > Core.getInstance().getScreenHeight()) {
               var4 = 0;
               var3 += var2 + 4;
            }
         }

         TextureFBO var10 = ModelManager.instance.bitmap;
         ITexture var11 = var1 ? var10.getDepthTexture() : var10.getTexture();
         SpriteRenderer.instance.renderi((Texture)null, var3, var4, var2, var2, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
         SpriteRenderer.instance.renderi((Texture)var11, var3, var4, var2, var2, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
      }

   }

   public void Reset() {
      if (this.fbo != null) {
         this.fbo.destroyLeaveTexture();
         this.fbo = null;
      }

      this.AtlasList.forEach(Atlas::Reset);
      this.AtlasList.clear();
      this.EntryMap.clear();
      this.characterTexturesFemale.clear();
      this.characterTexturesMale.clear();
      JobPool.forEach(RenderJob::Reset);
      JobPool.clear();
      this.RenderJobs.clear();
   }

   private void toBodyAtlas(RenderJob var1) {
      GL11.glPushAttrib(2048);
      if (this.fbo.getTexture() != var1.entry.atlas.tex) {
         this.fbo.setTextureAndDepth(var1.entry.atlas.tex, var1.entry.atlas.depth);
      }

      this.fbo.startDrawing();
      GL11.glViewport(0, 0, this.fbo.getWidth(), this.fbo.getHeight());
      Matrix4f var2 = Core.getInstance().projectionMatrixStack.alloc();
      int var3 = var1.entry.atlas.tex.getWidth();
      int var4 = var1.entry.atlas.tex.getHeight();
      var2.setOrtho2D(0.0F, (float)var3, (float)var4, 0.0F);
      Core.getInstance().projectionMatrixStack.push(var2);
      Matrix4f var5 = Core.getInstance().modelViewMatrixStack.alloc();
      var5.identity();
      Core.getInstance().modelViewMatrixStack.push(var5);
      GL11.glEnable(2929);
      GL11.glDepthFunc(519);
      GL11.glDepthMask(true);
      GL11.glEnable(3553);
      GL11.glDisable(3089);
      int var6 = GL11.glGetInteger(35725);
      GL20.glUseProgram(0);
      if (var1.entry.atlas.clear) {
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glClear(16640);
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
         var1.entry.atlas.clear = false;
      }

      GL11.glEnable(3089);
      GL11.glScissor(var1.entry.x, 1024 - var1.entry.y - var1.entry.h, var1.entry.w, var1.entry.h);
      if (var1.bClearThisSlotOnly) {
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glClear(16640);
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
      }

      TextureFBO var7 = ModelManager.instance.bitmap;
      int var8 = var7.getTexture().getWidth() / 8 * Core.TileScale;
      int var9 = var7.getTexture().getHeight() / 8 * Core.TileScale;
      int var10 = var1.entry.x - (var8 - var1.entry.atlas.ENTRY_WID) / 2;
      int var11 = var1.entry.y - (var9 - var1.entry.atlas.ENTRY_HGT) / 2;
      if (DeadBodyAtlasShader == null) {
         DeadBodyAtlasShader = new DeadBodyAtlasShader("DeadBodyAtlas");
      }

      if (DeadBodyAtlasShader.getShaderProgram().isCompiled()) {
         GL13.glActiveTexture(33985);
         GL11.glEnable(3553);
         GL11.glBindTexture(3553, var7.getDepthTexture().getID());
         GL13.glActiveTexture(33984);
         GL11.glDepthMask(true);
         GL11.glDepthFunc(519);
         VBORenderer var12 = VBORenderer.getInstance();
         var12.startRun(var12.FORMAT_PositionColorUV);
         var12.setMode(7);
         var12.setDepthTest(true);
         var12.setShaderProgram(DeadBodyAtlasShader.getShaderProgram());
         var12.setTextureID(((Texture)var7.getTexture()).getTextureId());
         var12.cmdUseProgram(DeadBodyAtlasShader.getShaderProgram());
         var12.cmdShader1f("zDepthBlendZ", 0.0F);
         var12.cmdShader1f("zDepthBlendToZ", 1.0F);
         var12.addQuad((float)var10, (float)var11, 0.0F, 0.0F, (float)(var10 + var8), (float)(var11 + var9), 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
         var12.endRun();
         var12.flush();
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, 0);
         GL11.glDisable(3553);
         GL13.glActiveTexture(33984);
         Texture.lastTextureID = 0;
         GL11.glBindTexture(3553, 0);
      } else {
         var7.getTexture().bind();
         GL11.glBegin(7);
         GL11.glColor3f(1.0F, 1.0F, 1.0F);
         GL11.glTexCoord2f(0.0F, 0.0F);
         GL11.glVertex2i(var10, var11);
         GL11.glTexCoord2f(1.0F, 0.0F);
         GL11.glVertex2i(var10 + var8, var11);
         GL11.glTexCoord2f(1.0F, 1.0F);
         GL11.glVertex2i(var10 + var8, var11 + var9);
         GL11.glTexCoord2f(0.0F, 1.0F);
         GL11.glVertex2i(var10, var11 + var9);
         GL11.glEnd();
         Texture.lastTextureID = 0;
         GL11.glBindTexture(3553, 0);
      }

      int var17 = SpriteRenderer.instance.getRenderingPlayerIndex();
      int var13 = var17 != 0 && var17 != 2 ? Core.getInstance().getOffscreenTrueWidth() / 2 : 0;
      int var14 = var17 != 0 && var17 != 1 ? Core.getInstance().getOffscreenTrueHeight() / 2 : 0;
      int var15 = Core.getInstance().getOffscreenTrueWidth();
      int var16 = Core.getInstance().getOffscreenTrueHeight();
      if (IsoPlayer.numPlayers > 1) {
         var15 /= 2;
      }

      if (IsoPlayer.numPlayers > 2) {
         var16 /= 2;
      }

      GL11.glScissor(var13, var14, var15, var16);
      this.fbo.endDrawing();
      ShaderHelper.glUseProgramObjectARB(var6);
      GL11.glEnable(3089);
      Core.getInstance().projectionMatrixStack.pop();
      Core.getInstance().modelViewMatrixStack.pop();
      GL11.glPopAttrib();
      var1.entry.ready = true;
      var1.done = 1;
   }

   public static final class BodyParams {
      public BaseVisual baseVisual;
      public final ItemVisuals itemVisuals = new ItemVisuals();
      public IsoDirections dir;
      public float angle;
      public boolean bFemale;
      public boolean bZombie;
      public boolean bSkeleton;
      public String animSetName;
      public String stateName;
      public final HashMap<String, String> variables = new HashMap();
      public boolean bStanding;
      public String primaryHandItem;
      public String secondaryHandItem;
      public final AttachedModelNames attachedModelNames = new AttachedModelNames();
      public float x;
      public float y;
      public float z;
      public float trackTime;
      public boolean bOutside;
      public boolean bRoom;
      public final ColorInfo ambient = new ColorInfo();
      public boolean fallOnFront = false;
      public boolean bKilledByFall = false;
      public final IsoGridSquare.ResultLight[] lights = new IsoGridSquare.ResultLight[5];
      public IGrappleable grappleable;
      public TwistableBoneTransform[] diedBoneTransforms = null;
      boolean ragdollFall = false;

      public BodyParams() {
         for(int var1 = 0; var1 < this.lights.length; ++var1) {
            this.lights[var1] = new IsoGridSquare.ResultLight();
         }

      }

      public void init(BodyParams var1) {
         this.baseVisual = var1.baseVisual;
         this.itemVisuals.clear();
         this.itemVisuals.addAll(var1.itemVisuals);
         this.dir = var1.dir;
         this.angle = var1.angle;
         this.bFemale = var1.bFemale;
         this.bZombie = var1.bZombie;
         this.bSkeleton = var1.bSkeleton;
         this.animSetName = var1.animSetName;
         this.stateName = var1.stateName;
         this.variables.clear();
         this.variables.putAll(var1.variables);
         this.bStanding = var1.bStanding;
         this.primaryHandItem = var1.primaryHandItem;
         this.secondaryHandItem = var1.secondaryHandItem;
         this.attachedModelNames.copyFrom(var1.attachedModelNames);
         this.x = var1.x;
         this.y = var1.y;
         this.z = var1.z;
         this.trackTime = var1.trackTime;
         this.fallOnFront = var1.fallOnFront;
         this.ragdollFall = var1.ragdollFall;
         this.bOutside = var1.bOutside;
         this.bRoom = var1.bRoom;
         this.ambient.set(var1.ambient.r, var1.ambient.g, var1.ambient.b, 1.0F);

         for(int var2 = 0; var2 < this.lights.length; ++var2) {
            this.lights[var2].copyFrom(var1.lights[var2]);
         }

         this.grappleable = var1.grappleable;
         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
         this.diedBoneTransforms = (TwistableBoneTransform[])PZArrayUtil.clone(var1.diedBoneTransforms, TwistableBoneTransform::alloc, TwistableBoneTransform::set);
      }

      public void init(IsoDeadBody var1) {
         this.baseVisual = var1.getVisual();
         var1.getItemVisuals(this.itemVisuals);
         this.dir = var1.dir;
         this.angle = var1.getAngle();
         this.bFemale = var1.isFemale();
         this.bZombie = var1.isZombie();
         this.bSkeleton = var1.isSkeleton();
         this.primaryHandItem = null;
         this.secondaryHandItem = null;
         this.attachedModelNames.initFrom(var1.getAttachedItems());
         this.animSetName = "zombie";
         this.stateName = var1.isBeingGrappled() ? "grappled" : "onground";
         this.variables.clear();
         this.bStanding = false;
         this.trackTime = 0.0F;
         if (var1.getPrimaryHandItem() == null && var1.getSecondaryHandItem() == null) {
            if (!var1.isAnimal() && !var1.isZombie() && var1.isKilledByFall()) {
               this.animSetName = "player";
               this.stateName = "deadbody";
               this.trackTime = 1.0F;
            }
         } else {
            if (var1.getPrimaryHandItem() != null && !StringUtils.isNullOrEmpty(var1.getPrimaryHandItem().getStaticModel())) {
               this.primaryHandItem = var1.getPrimaryHandItem().getStaticModel();
            }

            if (var1.getSecondaryHandItem() != null && !StringUtils.isNullOrEmpty(var1.getSecondaryHandItem().getStaticModel())) {
               this.secondaryHandItem = var1.getSecondaryHandItem().getStaticModel();
            }

            this.animSetName = "player";
            this.stateName = "deadbody";
            if (var1.isKilledByFall()) {
               this.trackTime = 1.0F;
            }
         }

         if (var1.isAnimal()) {
            this.animSetName = var1.animalAnimSet;
            this.stateName = "deadbody";
         }

         this.x = var1.getX();
         this.y = var1.getY();
         this.z = var1.getZ();
         this.fallOnFront = var1.isFallOnFront();
         this.bKilledByFall = var1.isKilledByFall();
         this.ragdollFall = var1.ragdollFall;
         this.bOutside = var1.square != null && var1.square.isOutside();
         this.bRoom = var1.square != null && var1.square.getRoom() != null;
         this.initAmbient(var1.square);
         this.initLights(var1.square);
         this.grappleable = var1;
         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
         this.diedBoneTransforms = (TwistableBoneTransform[])PZArrayUtil.clone(var1.getDiedBoneTransforms(), TwistableBoneTransform::alloc, TwistableBoneTransform::set);
      }

      public void init(IsoZombie var1) {
         this.baseVisual = var1.getHumanVisual();
         var1.getItemVisuals(this.itemVisuals);
         this.dir = var1.dir;
         this.angle = var1.getAnimAngleRadians();
         this.bFemale = var1.isFemale();
         this.bZombie = true;
         this.bSkeleton = var1.isSkeleton();
         this.primaryHandItem = null;
         this.secondaryHandItem = null;
         this.attachedModelNames.initFrom(var1.getAttachedItems());
         this.animSetName = "zombie";
         this.stateName = "onground";
         this.variables.clear();
         this.bStanding = false;
         this.x = var1.getX();
         this.y = var1.getY();
         this.z = var1.getZ();
         this.trackTime = 0.0F;
         this.fallOnFront = var1.isFallOnFront();
         this.bKilledByFall = var1.isKilledByFall();
         this.ragdollFall = var1.isRagdollFall();
         this.bOutside = var1.getCurrentSquare() != null && var1.getCurrentSquare().isOutside();
         this.bRoom = var1.getCurrentSquare() != null && var1.getCurrentSquare().getRoom() != null;
         this.initAmbient(var1.getCurrentSquare());
         this.initLights(var1.getCurrentSquare());
         this.grappleable = var1;
         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
         if (var1.isRagdollFall() && var1.hasAnimationPlayer()) {
            AnimationPlayer var2 = var1.getAnimationPlayer();
            int var3 = var2.getNumBones();
            this.diedBoneTransforms = TwistableBoneTransform.allocArray(var3);

            for(int var4 = 0; var4 < var3; ++var4) {
               var2.getBoneTransformAt(var4, this.diedBoneTransforms[var4]);
            }
         }

      }

      public void init(IsoMannequin var1) {
         this.baseVisual = var1.getHumanVisual();
         var1.getItemVisuals(this.itemVisuals);
         this.dir = var1.dir;
         this.angle = this.dir.ToVector().getDirection();
         this.bFemale = var1.isFemale();
         this.bZombie = var1.isZombie();
         this.bSkeleton = var1.isSkeleton();
         this.primaryHandItem = null;
         this.secondaryHandItem = null;
         this.attachedModelNames.clear();
         this.animSetName = var1.getAnimSetName();
         this.stateName = var1.getAnimStateName();
         this.variables.clear();
         var1.getVariables(this.variables);
         this.bStanding = true;
         this.x = var1.getX();
         this.y = var1.getY();
         this.z = var1.getZ();
         this.trackTime = 0.0F;
         this.fallOnFront = false;
         this.bOutside = var1.square != null && var1.square.isOutside();
         this.bRoom = var1.square != null && var1.square.getRoom() != null;
         this.initAmbient(var1.square);
         this.initLights((IsoGridSquare)null);
         this.grappleable = null;
         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
      }

      public void init(IHumanVisual var1, IsoDirections var2, String var3, String var4, float var5) {
         this.baseVisual = var1.getHumanVisual();
         var1.getItemVisuals(this.itemVisuals);
         this.dir = var2;
         this.angle = var2.ToVector().getDirection();
         this.bFemale = var1.isFemale();
         this.bZombie = var1.isZombie();
         this.bSkeleton = var1.isSkeleton();
         this.primaryHandItem = null;
         this.secondaryHandItem = null;
         this.attachedModelNames.clear();
         this.animSetName = var3;
         this.stateName = var4;
         this.variables.clear();
         this.bStanding = true;
         this.x = 0.0F;
         this.y = 0.0F;
         this.z = 0.0F;
         this.trackTime = var5;
         this.fallOnFront = false;
         this.bOutside = true;
         this.bRoom = false;
         this.ambient.set(1.0F, 1.0F, 1.0F, 1.0F);
         this.initLights((IsoGridSquare)null);
         this.grappleable = null;
         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
      }

      void initAmbient(IsoGridSquare var1) {
         this.ambient.set(1.0F, 1.0F, 1.0F, 1.0F);
      }

      void initLights(IsoGridSquare var1) {
         for(int var2 = 0; var2 < this.lights.length; ++var2) {
            this.lights[var2].radius = 0;
         }

         if (var1 != null) {
            IsoGridSquare.ILighting var5 = var1.lighting[0];
            int var3 = var5.resultLightCount();

            for(int var4 = 0; var4 < var3; ++var4) {
               this.lights[var4].copyFrom(var5.getResultLight(var4));
            }
         }

      }

      public void Reset() {
         this.baseVisual = null;
         this.itemVisuals.clear();
         Arrays.fill(this.lights, (Object)null);
         this.grappleable = null;
         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
      }
   }

   private static final class Checksummer {
      private MessageDigest md;
      private final StringBuilder sb = new StringBuilder();

      private Checksummer() {
      }

      public void reset() throws NoSuchAlgorithmException {
         if (this.md == null) {
            this.md = MessageDigest.getInstance("MD5");
         }

         this.md.reset();
      }

      public void update(byte var1) {
         this.md.update(var1);
      }

      public void update(boolean var1) {
         this.md.update((byte)(var1 ? 1 : 0));
      }

      public void update(int var1) {
         this.md.update((byte)(var1 & 255));
         this.md.update((byte)(var1 >> 8 & 255));
         this.md.update((byte)(var1 >> 16 & 255));
         this.md.update((byte)(var1 >> 24 & 255));
      }

      public void update(String var1) {
         if (var1 != null && !var1.isEmpty()) {
            this.md.update(var1.getBytes());
         }
      }

      public void update(ImmutableColor var1) {
         this.update((byte)((int)(var1.r * 255.0F)));
         this.update((byte)((int)(var1.g * 255.0F)));
         this.update((byte)((int)(var1.b * 255.0F)));
      }

      public void update(IsoGridSquare.ResultLight var1, float var2, float var3, float var4) {
         if (var1 != null && var1.radius > 0) {
            this.update((int)((float)var1.x - var2));
            this.update((int)((float)var1.y - var3));
            this.update((int)((float)var1.z - var4));
            this.update((byte)((int)(var1.r * 255.0F)));
            this.update((byte)((int)(var1.g * 255.0F)));
            this.update((byte)((int)(var1.b * 255.0F)));
            this.update((byte)var1.radius);
         }
      }

      public String checksumToString() {
         byte[] var1 = this.md.digest();
         this.sb.setLength(0);

         for(int var2 = 0; var2 < var1.length; ++var2) {
            this.sb.append(var1[var2] & 255);
         }

         return this.sb.toString();
      }
   }

   private static final class DebugDrawInWorld extends TextureDraw.GenericDrawer {
      RenderJob job;
      boolean bRendered;

      private DebugDrawInWorld() {
      }

      public void init(RenderJob var1) {
         this.job = var1;
         this.bRendered = false;
      }

      public void render() {
         this.job.animatedModel.DoRenderToWorld(this.job.body.x, this.job.body.y, this.job.body.z, this.job.m_animPlayerAngle);
         this.bRendered = true;
      }

      public void postRender() {
         if (this.job.animatedModel != null) {
            if (this.bRendered) {
               assert !DeadBodyAtlas.JobPool.contains(this.job);

               DeadBodyAtlas.JobPool.push(this.job);
            } else {
               assert !DeadBodyAtlas.JobPool.contains(this.job);

               DeadBodyAtlas.JobPool.push(this.job);
            }

            this.job.animatedModel.postRender(this.bRendered);
         }
      }
   }

   private static final class CharacterTextureVisual implements IHumanVisual {
      final HumanVisual humanVisual = new HumanVisual(this);
      boolean bFemale;

      CharacterTextureVisual(boolean var1) {
         this.bFemale = var1;
         this.humanVisual.setHairModel("");
         this.humanVisual.setBeardModel("");
      }

      public HumanVisual getHumanVisual() {
         return this.humanVisual;
      }

      public void getItemVisuals(ItemVisuals var1) {
         var1.clear();
      }

      public boolean isFemale() {
         return this.bFemale;
      }

      public boolean isZombie() {
         return true;
      }

      public boolean isSkeleton() {
         return false;
      }
   }

   public static final class BodyTexture {
      AtlasEntry entry;

      public BodyTexture() {
      }

      public ShadowParams getShadowParams() {
         return this.entry.shadowParams;
      }

      public void render(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
         if (PerformanceSettings.FBORenderChunk) {
            BodyTextureDepthDrawer var11 = (BodyTextureDepthDrawer)DeadBodyAtlas.s_BodyTextureDepthDrawerPool.alloc();
            var11.init(this, var1, var2, var3, var4, var5, var6, var7, var8, var9);
            SpriteRenderer.instance.drawGeneric(var11);
         } else if (this.entry.ready && this.entry.tex.isReady()) {
            this.entry.tex.render(var4 - (float)this.entry.w / 2.0F - this.entry.offsetX, var5 - (float)this.entry.h / 2.0F - this.entry.offsetY, (float)this.entry.w, (float)this.entry.h, var6, var7, var8, var9, (Consumer)null);
         } else {
            BodyTextureDrawer var10 = ((BodyTextureDrawer)DeadBodyAtlas.instance.bodyTextureDrawerPool.alloc()).init(this, var4, var5, var6, var7, var8, var9);
            SpriteRenderer.instance.drawGeneric(var10);
         }
      }

      public void renderObjectPicker(float var1, float var2, ColorInfo var3, IsoGridSquare var4, IsoObject var5) {
         if (this.entry.ready) {
            IsoObjectPicker.Instance.Add((int)(var1 - (float)(this.entry.w / 2)), (int)(var2 - (float)(this.entry.h / 2)), this.entry.w, this.entry.h, var4, var5, false, 1.0F, 1.0F);
         }
      }
   }

   private static final class AtlasEntry {
      public Atlas atlas;
      public String key;
      public String lightKey;
      public int updateCounter;
      public int x;
      public int y;
      public int w;
      public int h;
      public float offsetX;
      public float offsetY;
      public Texture tex;
      public final ShadowParams shadowParams = new ShadowParams(1.0F, 1.0F, 1.0F);
      public boolean ready = false;

      private AtlasEntry() {
      }

      public void Reset() {
         this.atlas = null;
         this.tex.destroy();
         this.tex = null;
         this.ready = false;
      }
   }

   private static final class RenderJob extends TextureDraw.GenericDrawer {
      static float SIZEV = 42.75F;
      public final BodyParams body = new BodyParams();
      public AtlasEntry entry;
      public AnimatedModel animatedModel;
      public float m_animPlayerAngle;
      public int done = 0;
      public int renderRefCount;
      public boolean bClearThisSlotOnly;
      int entryW;
      int entryH;
      final int[] m_viewport = new int[4];
      final Matrix4f m_matri4f = new Matrix4f();
      final Matrix4f m_projection = new Matrix4f();
      final Matrix4f m_modelView = new Matrix4f();
      final Vector3f m_scenePos = new Vector3f();
      final float[] m_bounds = new float[4];
      final float[] m_offset = new float[3];

      private RenderJob() {
      }

      public static RenderJob getNew() {
         return DeadBodyAtlas.JobPool.isEmpty() ? new RenderJob() : (RenderJob)DeadBodyAtlas.JobPool.pop();
      }

      public RenderJob init(BodyParams var1, AtlasEntry var2) {
         this.body.init(var1);
         this.entry = var2;
         if (this.animatedModel == null) {
            this.animatedModel = new AnimatedModel();
            this.animatedModel.setAnimate(false);
         }

         this.animatedModel.setAnimSetName(var1.animSetName);
         this.animatedModel.setState(var1.stateName);
         this.animatedModel.setPrimaryHandModelName(var1.primaryHandItem);
         this.animatedModel.setSecondaryHandModelName(var1.secondaryHandItem);
         this.animatedModel.setAttachedModelNames(var1.attachedModelNames);
         this.animatedModel.setAmbient(var1.ambient, var1.bOutside, var1.bRoom);
         this.animatedModel.setLights(var1.lights, var1.x, var1.y, var1.z);
         this.animatedModel.setVariable("FallOnFront", var1.fallOnFront);
         this.animatedModel.setVariable("KilledByFall", var1.bKilledByFall);
         var1.variables.forEach((var1x, var2x) -> {
            this.animatedModel.setVariable(var1x, var2x);
         });
         this.animatedModel.setModelData(var1.baseVisual, var1.itemVisuals);
         this.animatedModel.setAngle(DeadBodyAtlas.tempVector2.setLengthAndDirection(this.body.angle, 1.0F));
         if (var1.ragdollFall && !PZArrayUtil.isNullOrEmpty((Object[])var1.diedBoneTransforms)) {
            this.setRagdollDeathPose();
         }

         this.animatedModel.setTrackTime(var1.trackTime);
         this.animatedModel.setGrappleable(var1.grappleable);
         this.animatedModel.update();
         this.bClearThisSlotOnly = false;
         this.done = 0;
         this.renderRefCount = 0;
         return this;
      }

      private void setRagdollDeathPose() {
         AnimationPlayer var1 = this.animatedModel.getAnimationPlayer();
         var1.stopAll();
         AnimationTrack var2 = var1.play("DeathPose", true, true, -1.0F);
         if (var2 != null) {
            var2.BlendDelta = 1.0F;
            var2.initRagdollTransforms(this.body.diedBoneTransforms);
         }

      }

      public boolean renderMain() {
         if (!this.animatedModel.isReadyToRender()) {
            return false;
         } else {
            this.animatedModel.renderMain();
            this.m_animPlayerAngle = this.animatedModel.getAnimationPlayer().getRenderedAngle();
            boolean var1 = this.body.ragdollFall && !PZArrayUtil.isNullOrEmpty((Object[])this.body.diedBoneTransforms);
            this.animatedModel.calculateShadowParams(this.entry.shadowParams, var1);
            return true;
         }
      }

      public void render() {
         if (this.done != 1) {
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
            GL11.glDisable(3089);
            GL11.glPushAttrib(2048);
            ModelManager.instance.bitmap.startDrawing(true, true);
            GL11.glViewport(0, 0, ModelManager.instance.bitmap.getWidth(), ModelManager.instance.bitmap.getHeight());
            this.calcModelOffset(this.m_offset);
            this.animatedModel.setOffset(this.m_offset[0], this.m_offset[1], this.m_offset[2]);
            this.animatedModel.DoRender(0, 0, ModelManager.instance.bitmap.getTexture().getWidth(), ModelManager.instance.bitmap.getTexture().getHeight(), SIZEV, this.m_animPlayerAngle);
            if (this.animatedModel.isRendered()) {
               this.renderAABB();
            }

            ModelManager.instance.bitmap.endDrawing();
            GL11.glPopAttrib();
            if (this.animatedModel.isRendered()) {
               DeadBodyAtlas.instance.assignEntryToAtlas(this.entry, this.entryW, this.entryH);
               DeadBodyAtlas.instance.toBodyAtlas(this);
            }
         }
      }

      public void postRender() {
         if (this.animatedModel != null) {
            this.animatedModel.postRender(this.done == 1);

            assert this.renderRefCount > 0;

            --this.renderRefCount;
         }
      }

      void calcMatrices(Matrix4f var1, Matrix4f var2, float var3, float var4, float var5) {
         int var6 = ModelManager.instance.bitmap.getWidth();
         int var7 = ModelManager.instance.bitmap.getHeight();
         float var8 = SIZEV;
         float var9 = (float)var6 / (float)var7;
         boolean var10 = true;
         var1.identity();
         if (var10) {
            var1.ortho(-var8 * var9, var8 * var9, var8, -var8, -100.0F, 100.0F);
         } else {
            var1.ortho(-var8 * var9, var8 * var9, -var8, var8, -100.0F, 100.0F);
         }

         float var11 = Math.sqrt(2048.0F);
         var1.scale(-var11, var11, var11);
         var2.identity();
         boolean var13 = true;
         if (var13) {
            var2.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
            var2.rotate(this.m_animPlayerAngle + 0.7853982F, 0.0F, 1.0F, 0.0F);
         } else {
            var2.rotate(this.m_animPlayerAngle, 0.0F, 1.0F, 0.0F);
         }

         float var12 = this.animatedModel.getScale();
         var2.scale(var12);
         var2.translate(var3, var4, var5);
      }

      void calcModelBounds(float[] var1) {
         float var2 = 3.4028235E38F;
         float var3 = 3.4028235E38F;
         float var4 = -3.4028235E38F;
         float var5 = -3.4028235E38F;
         int var6 = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Dummy01", -1);
         int var7 = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Bip01", -1);
         int var8 = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Bip01_Pelvis", -1);
         int var9 = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Translation_Data", -1);
         org.lwjgl.util.vector.Matrix4f[] var11 = null;
         boolean[] var12 = null;
         boolean var13 = this.body.ragdollFall && !PZArrayUtil.isNullOrEmpty((Object[])this.body.diedBoneTransforms) && var8 != -1;
         float var14 = 0.0F;
         float var15 = 0.0F;
         org.lwjgl.util.vector.Matrix4f var10;
         int var16;
         if (var13) {
            var11 = (org.lwjgl.util.vector.Matrix4f[])PZArrayUtil.newInstance(org.lwjgl.util.vector.Matrix4f.class, this.body.diedBoneTransforms.length, org.lwjgl.util.vector.Matrix4f::new);
            var12 = new boolean[var11.length];
            this.body.diedBoneTransforms[var7].getMatrix(var11[var7]);
            this.body.diedBoneTransforms[var8].getMatrix(var11[var8]);
            BoneTransform.mul(this.body.diedBoneTransforms[var8], var11[var7], var11[var8]);
            var10 = var11[var8];
            this.sceneToUI(var10.m03, var10.m13, var10.m23, this.m_projection, this.m_modelView, this.m_scenePos);
            var14 = this.m_scenePos.x;
            var15 = this.m_scenePos.y;

            for(var16 = 0; var16 < this.body.diedBoneTransforms.length; ++var16) {
               var12[var16] = true;
               if (var16 != var6 && var16 != var7 && var16 != var8 && var16 != var9) {
                  SkinningBone var17 = this.animatedModel.getAnimationPlayer().getSkinningData().getBoneAt(var16);
                  SkinningBone var18 = var17.Parent;
                  if (var18.Index != var6 && var18.Index != var7) {
                     var12[var16] = false;
                     BoneTransform.mul(this.body.diedBoneTransforms[var17.Index], var11[var18.Index], var11[var17.Index]);
                  }
               }
            }

            for(var16 = 0; var16 < this.body.diedBoneTransforms.length; ++var16) {
               if (!var12[var16]) {
                  var11[var16].rotate(1.5707964F, new org.lwjgl.util.vector.Vector3f(1.0F, 0.0F, 0.0F));
                  var11[var16].rotate(3.1415927F, new org.lwjgl.util.vector.Vector3f(0.0F, 1.0F, 0.0F));
               }
            }
         }

         for(var16 = 0; var16 < this.animatedModel.getAnimationPlayer().getModelTransformsCount(); ++var16) {
            if ((!var13 || !var12[var16] && var16 != var6 && var16 != var7 && var16 != var8) && var16 != var9) {
               var10 = this.animatedModel.getAnimationPlayer().getModelTransformAt(var16);
               if (var13) {
                  var10 = var11[var16];
               }

               this.sceneToUI(var10.m03, var10.m13, var10.m23, this.m_projection, this.m_modelView, this.m_scenePos);
               if (var13) {
                  Vector3f var10000 = this.m_scenePos;
                  var10000.x -= var14;
                  var10000 = this.m_scenePos;
                  var10000.y -= var15;
               }

               var2 = PZMath.min(var2, this.m_scenePos.x);
               var4 = PZMath.max(var4, this.m_scenePos.x);
               var3 = PZMath.min(var3, this.m_scenePos.y);
               var5 = PZMath.max(var5, this.m_scenePos.y);
            }
         }

         if (var13) {
            var2 += 512.0F;
            var4 += 512.0F;
            var3 += 512.0F;
            var5 += 512.0F;
         }

         var1[0] = var2;
         var1[1] = var3;
         var1[2] = var4;
         var1[3] = var5;
      }

      void calcModelOffset(float[] var1) {
         int var2 = ModelManager.instance.bitmap.getWidth();
         int var3 = ModelManager.instance.bitmap.getHeight();
         float var4 = 0.0F;
         float var5 = this.body.bStanding ? -0.0F : 0.0F;
         boolean var6 = this.body.ragdollFall && !PZArrayUtil.isNullOrEmpty((Object[])this.body.diedBoneTransforms);
         if (var6) {
            var5 = 0.0F;
         }

         this.calcMatrices(this.m_projection, this.m_modelView, var4, 0.0F, var5);
         this.calcModelBounds(this.m_bounds);
         float var7 = this.m_bounds[0];
         float var8 = this.m_bounds[1];
         float var9 = this.m_bounds[2];
         float var10 = this.m_bounds[3];
         this.uiToScene(this.m_projection, this.m_modelView, var7, var8, this.m_scenePos);
         float var11 = this.m_scenePos.x;
         float var12 = this.m_scenePos.z;
         float var13 = ((float)var2 - (var9 - var7)) / 2.0F;
         float var14 = ((float)var3 - (var10 - var8)) / 2.0F;
         this.uiToScene(this.m_projection, this.m_modelView, var13, var14, this.m_scenePos);
         var4 += (this.m_scenePos.x - var11) * this.animatedModel.getScale();
         var5 += (this.m_scenePos.z - var12) * this.animatedModel.getScale();
         var1[0] = var4;
         var1[1] = 0.0F;
         var1[2] = var5;
         if (var6) {
            int var15 = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Bip01", -1);
            int var16 = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Bip01_Pelvis", -1);
            if (var15 != -1 && var16 != -1) {
               org.lwjgl.util.vector.Matrix4f var17 = new org.lwjgl.util.vector.Matrix4f();
               this.body.diedBoneTransforms[var15].getMatrix(var17);
               org.lwjgl.util.vector.Matrix4f var18 = new org.lwjgl.util.vector.Matrix4f();
               BoneTransform.mul(this.body.diedBoneTransforms[var16], var17, var18);
               var18.rotate(1.5707964F, new org.lwjgl.util.vector.Vector3f(1.0F, 0.0F, 0.0F));
               var18.rotate(3.1415927F, new org.lwjgl.util.vector.Vector3f(0.0F, 1.0F, 0.0F));
               var1[0] = var18.m03;
               var1[1] = var18.m13;
               var1[2] = var18.m23;
               this.entry.offsetX = 0.0F;
               this.entry.offsetY = 0.0F;
            }

         } else {
            this.entry.offsetX = (var13 - var7) / (8.0F / (float)Core.TileScale);
            this.entry.offsetY = (var14 - var8) / (8.0F / (float)Core.TileScale);
         }
      }

      void renderAABB() {
         this.calcMatrices(this.m_projection, this.m_modelView, this.m_offset[0], this.m_offset[1], this.m_offset[2]);
         this.calcModelBounds(this.m_bounds);
         float var1 = this.m_bounds[0];
         float var2 = this.m_bounds[1];
         float var3 = this.m_bounds[2];
         float var4 = this.m_bounds[3];
         int var5 = ModelManager.instance.bitmap.getWidth();
         int var6 = ModelManager.instance.bitmap.getHeight();
         float var7 = 128.0F;
         var1 -= var7;
         var2 -= var7;
         var3 += var7;
         var4 += var7;
         var1 = Math.floor(var1 / 128.0F) * 128.0F;
         var3 = Math.ceil(var3 / 128.0F) * 128.0F;
         var2 = Math.floor(var2 / 128.0F) * 128.0F;
         var4 = Math.ceil(var4 / 128.0F) * 128.0F;
         this.entryW = (int)(var3 - var1) / (8 / Core.TileScale);
         this.entryH = (int)(var4 - var2) / (8 / Core.TileScale);
      }

      UI3DScene.Ray getCameraRay(float var1, float var2, Matrix4f var3, Matrix4f var4, UI3DScene.Ray var5) {
         Matrix4f var6 = DeadBodyAtlas.RenderJob.L_getCameraRay.matrix4f;
         var6.set(var3);
         var6.mul(var4);
         var6.invert();
         this.m_viewport[0] = 0;
         this.m_viewport[1] = 0;
         this.m_viewport[2] = ModelManager.instance.bitmap.getWidth();
         this.m_viewport[3] = ModelManager.instance.bitmap.getHeight();
         Vector3f var7 = var6.unprojectInv(var1, var2, 0.0F, this.m_viewport, DeadBodyAtlas.RenderJob.L_getCameraRay.ray_start);
         Vector3f var8 = var6.unprojectInv(var1, var2, 1.0F, this.m_viewport, DeadBodyAtlas.RenderJob.L_getCameraRay.ray_end);
         var5.origin.set(var7);
         var5.direction.set(var8.sub(var7).normalize());
         return var5;
      }

      Vector3f sceneToUI(float var1, float var2, float var3, Matrix4f var4, Matrix4f var5, Vector3f var6) {
         Matrix4f var7 = this.m_matri4f;
         var7.set(var4);
         var7.mul(var5);
         this.m_viewport[0] = 0;
         this.m_viewport[1] = 0;
         this.m_viewport[2] = ModelManager.instance.bitmap.getWidth();
         this.m_viewport[3] = ModelManager.instance.bitmap.getHeight();
         var7.project(var1, var2, var3, this.m_viewport, var6);
         return var6;
      }

      Vector3f uiToScene(Matrix4f var1, Matrix4f var2, float var3, float var4, Vector3f var5) {
         UI3DScene.Plane var6 = DeadBodyAtlas.RenderJob.L_uiToScene.plane;
         var6.point.set(0.0F);
         var6.normal.set(0.0F, 1.0F, 0.0F);
         UI3DScene.Ray var7 = this.getCameraRay(var3, var4, var1, var2, DeadBodyAtlas.RenderJob.L_uiToScene.ray);
         if (UI3DScene.intersect_ray_plane(var6, var7, var5) != 1) {
            var5.set(0.0F);
         }

         return var5;
      }

      public void Reset() {
         this.body.Reset();
         this.entry = null;
         if (this.animatedModel != null) {
            this.animatedModel.releaseAnimationPlayer();
            this.animatedModel = null;
         }

      }

      static final class L_getCameraRay {
         static final Matrix4f matrix4f = new Matrix4f();
         static final Vector3f ray_start = new Vector3f();
         static final Vector3f ray_end = new Vector3f();

         L_getCameraRay() {
         }
      }

      static final class L_uiToScene {
         static final UI3DScene.Plane plane = new UI3DScene.Plane();
         static final UI3DScene.Ray ray = new UI3DScene.Ray();

         L_uiToScene() {
         }
      }
   }

   private final class Atlas {
      public final int ENTRY_WID;
      public final int ENTRY_HGT;
      public Texture tex;
      public Texture depth;
      public final ArrayList<AtlasEntry> EntryList = new ArrayList();
      public boolean clear = true;

      public Atlas(int var2, int var3, int var4, int var5) {
         this.ENTRY_WID = var4;
         this.ENTRY_HGT = var5;
         this.tex = new Texture(var2, var3, 16);
         this.depth = new Texture(var2, var3, 512);
         if (DeadBodyAtlas.this.fbo == null) {
            DeadBodyAtlas.this.fbo = new TextureFBO(this.tex, this.depth, false);
         }
      }

      public boolean isFull() {
         int var1 = this.tex.getWidth() / this.ENTRY_WID;
         int var2 = this.tex.getHeight() / this.ENTRY_HGT;
         return this.EntryList.size() >= var1 * var2;
      }

      public AtlasEntry addBody(String var1) {
         int var2 = this.tex.getWidth() / this.ENTRY_WID;
         int var3 = this.EntryList.size();
         int var4 = var3 % var2;
         int var5 = var3 / var2;
         AtlasEntry var6 = new AtlasEntry();
         var6.atlas = this;
         var6.key = var1;
         var6.x = var4 * this.ENTRY_WID;
         var6.y = var5 * this.ENTRY_HGT;
         var6.w = this.ENTRY_WID;
         var6.h = this.ENTRY_HGT;
         var6.tex = this.tex.split(var1, var6.x, this.tex.getHeight() - (var6.y + this.ENTRY_HGT), var6.w, var6.h);
         var6.tex.setName(var1);
         this.EntryList.add(var6);
         return var6;
      }

      public void addEntry(AtlasEntry var1) {
         int var2 = this.tex.getWidth() / this.ENTRY_WID;
         int var3 = this.EntryList.size();
         int var4 = var3 % var2;
         int var5 = var3 / var2;
         var1.atlas = this;
         var1.x = var4 * this.ENTRY_WID;
         var1.y = var5 * this.ENTRY_HGT;
         var1.w = this.ENTRY_WID;
         var1.h = this.ENTRY_HGT;
         var1.tex = this.tex.split(var1.key, var1.x, this.tex.getHeight() - (var1.y + this.ENTRY_HGT), var1.w, var1.h);
         var1.tex.setName(var1.key);
         this.EntryList.add(var1);
      }

      public void Reset() {
         this.EntryList.forEach(AtlasEntry::Reset);
         this.EntryList.clear();
         if (!this.tex.isDestroyed()) {
            RenderThread.invokeOnRenderContext(() -> {
               GL11.glDeleteTextures(this.tex.getID());
            });
         }

         this.tex = null;
         if (!this.depth.isDestroyed()) {
            RenderThread.invokeOnRenderContext(() -> {
               GL11.glDeleteTextures(this.depth.getID());
            });
         }

         this.depth = null;
      }
   }

   private static final class ClearAtlasTexture extends TextureDraw.GenericDrawer {
      Atlas m_atlas;

      ClearAtlasTexture(Atlas var1) {
         this.m_atlas = var1;
      }

      public void render() {
         TextureFBO var1 = DeadBodyAtlas.instance.fbo;
         if (var1 != null && this.m_atlas.tex != null) {
            if (this.m_atlas.clear) {
               if (var1.getTexture() != this.m_atlas.tex) {
                  var1.setTextureAndDepth(this.m_atlas.tex, this.m_atlas.depth);
               }

               var1.startDrawing(false, false);
               GL11.glPushAttrib(2048);
               GL11.glViewport(0, 0, var1.getWidth(), var1.getHeight());
               Matrix4f var2 = Core.getInstance().projectionMatrixStack.alloc();
               int var3 = this.m_atlas.tex.getWidth();
               int var4 = this.m_atlas.tex.getHeight();
               var2.setOrtho2D(0.0F, (float)var3, (float)var4, 0.0F);
               Core.getInstance().projectionMatrixStack.push(var2);
               Matrix4f var5 = Core.getInstance().modelViewMatrixStack.alloc();
               var5.identity();
               Core.getInstance().modelViewMatrixStack.push(var5);
               GL11.glDisable(3089);
               GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
               GL11.glClear(16640);
               GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
               var1.endDrawing();
               GL11.glEnable(3089);
               Core.getInstance().projectionMatrixStack.pop();
               Core.getInstance().modelViewMatrixStack.pop();
               GL11.glPopAttrib();
               this.m_atlas.clear = false;
            }
         }
      }
   }

   public static final class DeadBodyAtlasShader extends Shader {
      public DeadBodyAtlasShader(String var1) {
         super(var1);
      }

      protected void onCompileSuccess(ShaderProgram var1) {
         var1.Start();
         var1.setSamplerUnit("DIFFUSE", 0);
         var1.setSamplerUnit("DEPTH", 1);
         var1.End();
      }
   }

   private static final class BodyTextureDepthDrawer extends TextureDraw.GenericDrawer {
      BodyTexture bodyTexture;
      float ox;
      float oy;
      float oz;
      float x;
      float y;
      float r;
      float g;
      float b;
      float a;

      private BodyTextureDepthDrawer() {
      }

      BodyTextureDepthDrawer init(BodyTexture var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10) {
         this.bodyTexture = var1;
         this.ox = var2;
         this.oy = var3;
         this.oz = var4;
         this.x = var5;
         this.y = var6;
         float var11 = 1.25F;
         this.r = PZMath.clamp(var7 * var11, 0.0F, 1.0F);
         this.g = PZMath.clamp(var8 * var11, 0.0F, 1.0F);
         this.b = PZMath.clamp(var9 * var11, 0.0F, 1.0F);
         this.a = var10;
         return this;
      }

      public void render() {
         if (this.bodyTexture.entry.ready && this.bodyTexture.entry.tex.isReady()) {
            if (DeadBodyAtlas.DeadBodyAtlasShader == null) {
               DeadBodyAtlas.DeadBodyAtlasShader = new DeadBodyAtlasShader("DeadBodyAtlas");
            }

            if (DeadBodyAtlas.DeadBodyAtlasShader.getShaderProgram().isCompiled()) {
               AtlasEntry var1 = this.bodyTexture.entry;
               float var2 = this.x - (float)var1.w / 2.0F - var1.offsetX;
               float var3 = this.y - (float)var1.h / 2.0F - var1.offsetY;
               int var4 = var1.w;
               int var5 = var1.h;
               GL13.glActiveTexture(33985);
               GL11.glEnable(3553);
               GL11.glBindTexture(3553, var1.atlas.depth.getID());
               GL13.glActiveTexture(33984);
               GL11.glDepthMask(true);
               GL11.glDepthFunc(515);
               float var6 = (Float)Core.getInstance().FloatParamMap.get(0);
               float var7 = (Float)Core.getInstance().FloatParamMap.get(1);
               int var8 = SpriteRenderer.instance.getRenderingPlayerIndex();
               PlayerCamera var9 = SpriteRenderer.instance.getRenderingPlayerCamera(var8);
               float var10 = var9.fixJigglyModelsSquareX;
               float var11 = var9.fixJigglyModelsSquareY;
               float var12 = var1.offsetX;
               float var13 = var1.offsetY;
               float var14 = -(var12 + 2.0F * var13) / (64.0F * (float)Core.TileScale);
               float var15 = -(var12 - 2.0F * var13) / (-64.0F * (float)Core.TileScale);
               float var16 = 4.4F;
               float var17 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var6), PZMath.fastfloor(var7), this.ox + var10 + var14 + 0.5F * var16, this.oy + var11 + var15 + 0.5F * var16, this.oz + 1.0F).depthStart;
               float var18 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var6), PZMath.fastfloor(var7), this.ox + var10 + var14 - 0.5F * var16, this.oy + var11 + var15 - 0.5F * var16, this.oz).depthStart;
               var17 += 0.0014F;
               var18 += 0.0014F;
               VBORenderer var19 = VBORenderer.getInstance();
               var19.startRun(var19.FORMAT_PositionColorUV);
               var19.setMode(7);
               var19.setDepthTest(true);
               var19.setShaderProgram(DeadBodyAtlas.DeadBodyAtlasShader.getShaderProgram());
               Texture var20 = var1.tex;
               var19.setTextureID(var20.getTextureId());
               var19.cmdUseProgram(DeadBodyAtlas.DeadBodyAtlasShader.getShaderProgram());
               var19.cmdShader1f("zDepthBlendZ", var17);
               var19.cmdShader1f("zDepthBlendToZ", var18);
               var19.addQuad(var2, var3, var20.getXStart(), var20.getYStart(), var2 + (float)var4, var3 + (float)var5, var20.getXEnd(), var20.getYEnd(), 0.0F, this.r, this.g, this.b, this.a);
               var19.endRun();
               var19.flush();
               GL13.glActiveTexture(33985);
               GL11.glBindTexture(3553, 0);
               GL11.glDisable(3553);
               GL13.glActiveTexture(33984);
               ShaderHelper.glUseProgramObjectARB(0);
               Texture.lastTextureID = -1;
               SpriteRenderer.ringBuffer.restoreBoundTextures = true;
               GLStateRenderThread.restore();
            }
         }
      }

      public void postRender() {
         DeadBodyAtlas.s_BodyTextureDepthDrawerPool.release((Object)this);
      }
   }

   private static final class BodyTextureDrawer extends TextureDraw.GenericDrawer {
      BodyTexture bodyTexture;
      float x;
      float y;
      float r;
      float g;
      float b;
      float a;

      private BodyTextureDrawer() {
      }

      BodyTextureDrawer init(BodyTexture var1, float var2, float var3, float var4, float var5, float var6, float var7) {
         this.bodyTexture = var1;
         this.x = var2;
         this.y = var3;
         this.r = var4;
         this.g = var5;
         this.b = var6;
         this.a = var7;
         return this;
      }

      public void render() {
         AtlasEntry var1 = this.bodyTexture.entry;
         if (var1.ready && var1.tex.isReady()) {
            int var2 = (int)(this.x - (float)var1.w / 2.0F - var1.offsetX);
            int var3 = (int)(this.y - (float)var1.h / 2.0F - var1.offsetY);
            int var4 = var1.w;
            int var5 = var1.h;
            VBORenderer var6 = VBORenderer.getInstance();
            var6.startRun(var6.FORMAT_PositionColorUV);
            var6.setTextureID(var1.tex.getTextureId());
            var6.addQuad((float)var2, (float)var3, var1.tex.xStart, var1.tex.yStart, (float)(var2 + var4), (float)(var3 + var5), var1.tex.xEnd, var1.tex.yEnd, 0.0F, this.r, this.g, this.b, this.a);
            var6.endRun();
            var6.flush();
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
         }
      }

      public void postRender() {
         this.bodyTexture = null;
         DeadBodyAtlas.instance.bodyTextureDrawerPool.release((Object)this);
      }
   }
}
