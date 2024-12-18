package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.IModelCamera;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.TextureDraw;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.popman.ObjectPool;

public final class FBORenderItems {
   private static FBORenderItems instance = null;
   private final ArrayList<RenderJob> RenderJobs = new ArrayList();
   private final ObjectPool<RenderJob> JobPool = new ObjectPool(RenderJob::new);
   private final ChunkCamera m_chunkCamera = new ChunkCamera();

   public FBORenderItems() {
   }

   public static FBORenderItems getInstance() {
      if (instance == null) {
         instance = new FBORenderItems();
      }

      return instance;
   }

   public void render(int var1, IsoTrap var2) {
      InventoryItem var3 = var2.getItem();
      if (ItemModelRenderer.itemHasModel(var3)) {
         RenderJob var4 = (RenderJob)this.JobPool.alloc();
         var4.init(var1, var2);
         this.RenderJobs.add(var4);
      }
   }

   public void render(int var1, IsoWorldInventoryObject var2) {
      InventoryItem var3 = var2.getItem();
      if (ItemModelRenderer.itemHasModel(var3)) {
         RenderJob var4 = (RenderJob)this.JobPool.alloc();
         var4.init(var1, var2);
         this.RenderJobs.add(var4);
      }
   }

   public void update() {
      for(int var1 = 0; var1 < this.RenderJobs.size(); ++var1) {
         RenderJob var2 = (RenderJob)this.RenderJobs.get(var1);
         if (var2.getObject().getObjectIndex() == -1) {
            var2.done = 1;
         }

         if (var2.getObject().getRenderSquare() == null) {
            var2.done = 1;
         }

         if (var2.done != 1 || var2.renderRefCount <= 0) {
            if (var2.done == 1 && var2.renderRefCount == 0) {
               this.RenderJobs.remove(var1--);
               this.JobPool.release((Object)var2);
            } else {
               FBORenderChunk var3 = (FBORenderChunk)FBORenderChunkManager.instance.chunks.get(var2.renderChunkIndex);
               if (var3 == null) {
                  var2.done = 1;
               } else if (var2.playerIndex == IsoCamera.frameState.playerIndex) {
                  ItemModelRenderer.RenderStatus var4 = var2.renderMain();
                  if (var4 == ItemModelRenderer.RenderStatus.Loading) {
                     boolean var5 = true;
                  } else if (var4 == ItemModelRenderer.RenderStatus.Ready) {
                     ++var2.renderRefCount;
                     if (!FBORenderChunkManager.instance.toRenderThisFrame.contains(var3)) {
                        FBORenderChunkManager.instance.toRenderThisFrame.add(var3);
                     }

                     int var6 = IsoCamera.frameState.playerIndex;
                     SpriteRenderer.instance.glDoEndFrame();
                     SpriteRenderer.instance.glDoStartFrameFlipY(var3.w, var3.h, 1.0F, var6);
                     var3.beginMainThread(false);
                     SpriteRenderer.instance.drawGeneric(var2);
                     var3.endMainThread();
                     SpriteRenderer.instance.glDoEndFrame();
                     SpriteRenderer.instance.glDoStartFrame(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), Core.getInstance().getZoom(var6), var6);
                  } else {
                     var2.done = 1;
                  }
               }
            }
         }
      }

   }

   public void Reset() {
      this.JobPool.forEach(RenderJob::Reset);
      this.JobPool.clear();
      this.RenderJobs.clear();
   }

   public IModelCamera getCamera() {
      return this.m_chunkCamera;
   }

   public void setCamera(FBORenderChunk var1, float var2, float var3, float var4, Vector3f var5) {
      this.m_chunkCamera.set(var1, var2, var3, var4, 0.0F);
      this.m_chunkCamera.m_angle.set(var5);
   }

   private static final class ChunkCamera extends FBORenderChunkCamera {
      final Vector3f m_angle = new Vector3f();

      private ChunkCamera() {
      }

      public void Begin() {
         super.Begin();
         Matrix4f var1 = Core.getInstance().modelViewMatrixStack.peek();
         var1.rotate(-3.1415927F, 0.0F, 1.0F, 0.0F);
         var1.rotate(this.m_angle.x * 0.017453292F, 1.0F, 0.0F, 0.0F);
         var1.rotate(this.m_angle.y * 0.017453292F, 0.0F, 1.0F, 0.0F);
         var1.rotate(this.m_angle.z * 0.017453292F, 0.0F, 0.0F, 1.0F);
      }

      public void End() {
         super.End();
      }
   }

   private static final class RenderJob extends TextureDraw.GenericDrawer {
      int playerIndex;
      int renderChunkIndex;
      int done = 0;
      int renderRefCount;
      IsoTrap trap;
      IsoWorldInventoryObject worldInventoryObject;
      final ItemModelRenderer[] m_renderer = new ItemModelRenderer[3];
      boolean bRendered = false;

      RenderJob() {
         for(int var1 = 0; var1 < this.m_renderer.length; ++var1) {
            this.m_renderer[var1] = new ItemModelRenderer();
         }

      }

      void init(int var1, IsoWorldInventoryObject var2) {
         this.playerIndex = IsoCamera.frameState.playerIndex;
         this.renderChunkIndex = var1;
         this.done = 0;
         this.renderRefCount = 0;
         this.worldInventoryObject = var2;
         this.trap = null;
         this.bRendered = false;
      }

      void init(int var1, IsoTrap var2) {
         this.playerIndex = IsoCamera.frameState.playerIndex;
         this.renderChunkIndex = var1;
         this.done = 0;
         this.renderRefCount = 0;
         this.worldInventoryObject = null;
         this.trap = var2;
         this.bRendered = false;
      }

      IsoObject getObject() {
         return (IsoObject)(this.trap != null ? this.trap : this.worldInventoryObject);
      }

      ItemModelRenderer.RenderStatus renderMain() {
         IsoGridSquare var1;
         IsoGridSquare var2;
         boolean var3;
         int var4;
         if (this.trap != null) {
            var1 = this.trap.getSquare();
            var2 = this.trap.getRenderSquare();
            var3 = true;
            var4 = SpriteRenderer.instance.getMainStateIndex();
            return this.m_renderer[var4].renderMain(this.trap.getItem(), var1, var2, (float)var1.x + 0.5F, (float)var1.y + 0.5F, (float)var1.z, 0.0F, -1.0F, var3);
         } else {
            var1 = this.worldInventoryObject.getSquare();
            var2 = this.worldInventoryObject.getRenderSquare();
            var3 = true;
            var4 = SpriteRenderer.instance.getMainStateIndex();
            return this.m_renderer[var4].renderMain(this.worldInventoryObject.getItem(), var1, var2, (float)var1.x + this.worldInventoryObject.xoff, (float)var1.y + this.worldInventoryObject.yoff, (float)var1.z + this.worldInventoryObject.zoff, 0.0F, -1.0F, var3);
         }
      }

      public void render() {
         if (this.done != 1) {
            SpriteRenderState var1 = SpriteRenderer.instance.getRenderingState();
            FBORenderChunk var2 = (FBORenderChunk)var1.cachedRenderChunkIndexMap.get(this.renderChunkIndex);
            if (var2 != null) {
               int var3 = SpriteRenderer.instance.getRenderStateIndex();
               ItemModelRenderer var4 = this.m_renderer[var3];
               FBORenderItems.instance.m_chunkCamera.set(var2, var4.m_x, var4.m_y, var4.m_z, 0.0F);
               FBORenderItems.instance.m_chunkCamera.m_angle.set(var4.m_angle);
               var4.DoRender(FBORenderItems.instance.m_chunkCamera, true, var2.bHighRes);
               if (var4.isRendered()) {
                  this.bRendered = true;
               }

            }
         }
      }

      public void postRender() {
         if (this.bRendered) {
            this.bRendered = false;
            this.done = 1;
         }

         int var1 = SpriteRenderer.instance.getMainStateIndex();
         this.m_renderer[var1].reset();
         --this.renderRefCount;
      }

      void Reset() {
      }
   }
}
