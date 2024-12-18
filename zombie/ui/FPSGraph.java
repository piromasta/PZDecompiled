package zombie.ui;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.utils.BoundedQueue;
import zombie.input.Mouse;

public final class FPSGraph extends UIElement {
   public static FPSGraph instance;
   private static final int NUM_BARS = 30;
   private static final int BAR_WID = 8;
   private final Graph fpsGraph = new Graph();
   private final Graph upsGraph = new Graph();
   private final Graph lpsGraph = new Graph();
   private final Graph uiGraph = new Graph();

   public FPSGraph() {
      this.setVisible(false);
      this.setWidth(232.0);
   }

   public void addRender(long var1) {
      while(this.fpsGraph.queue.size() >= 64) {
         this.fpsGraph.queue.poll();
      }

      this.fpsGraph.queue.add(var1);
   }

   public void addUpdate(long var1) {
      this.upsGraph.add(var1);
   }

   public void addLighting(long var1) {
      while(this.lpsGraph.queue.size() >= 64) {
         this.lpsGraph.queue.poll();
      }

      this.lpsGraph.queue.add(var1);
   }

   public void addUI(long var1) {
      this.uiGraph.add(var1);
   }

   public void update() {
      if (this.isVisible()) {
         this.setHeight(108.0);
         this.setWidth(232.0);
         this.setX(20.0);
         this.setY((double)(Core.getInstance().getScreenHeight() - 20) - this.getHeight());
         super.update();
      }
   }

   public void render() {
      if (this.isVisible()) {
         if (UIManager.VisibleAllUI) {
            int var1 = this.getHeight().intValue() - 4;
            int var2 = -1;
            if (this.isMouseOver()) {
               this.DrawTextureScaledCol(UIElement.white, 0.0, 0.0, this.getWidth(), this.getHeight(), 0.0, 0.20000000298023224, 0.0, 0.5);
               int var3 = Mouse.getXA() - this.getAbsoluteX().intValue();
               var2 = var3 / 8;
            }

            this.fpsGraph.flushQueue();
            this.fpsGraph.render(0.0F, 1.0F, 0.0F);
            if (var2 >= 0 && var2 < this.fpsGraph.bars.size()) {
               this.DrawText("FPS: " + this.fpsGraph.bars.get(var2), 20.0, (double)(var1 / 2 - 10), 0.0, 1.0, 0.0, 1.0);
            }

            this.lpsGraph.flushQueue();
            this.lpsGraph.render(1.0F, 1.0F, 0.0F);
            if (var2 >= 0 && var2 < this.lpsGraph.bars.size()) {
               this.DrawText("LPS: " + this.lpsGraph.bars.get(var2), 20.0, (double)(var1 / 2 + 20), 1.0, 1.0, 0.0, 1.0);
            }

            this.upsGraph.render(0.0F, 1.0F, 1.0F);
            if (var2 >= 0 && var2 < this.upsGraph.bars.size()) {
               this.DrawText("UPS: " + this.upsGraph.bars.get(var2), 20.0, (double)(var1 / 2 + 5), 0.0, 1.0, 1.0, 1.0);
               this.DrawTextureScaledCol(UIElement.white, (double)(var2 * 8 + 4), 0.0, 1.0, this.getHeight(), 1.0, 1.0, 1.0, 0.5);
            }

            this.uiGraph.render(1.0F, 0.0F, 1.0F);
            if (var2 >= 0 && var2 < this.uiGraph.bars.size()) {
               this.DrawText("UI: " + this.uiGraph.bars.get(var2), 20.0, (double)(var1 / 2 - 26), 1.0, 0.0, 1.0, 1.0);
            }

         }
      }
   }

   private final class Graph {
      private final ArrayList<Long> times = new ArrayList();
      private final BoundedQueue<Long> times2 = new BoundedQueue(300);
      private final ArrayList<Integer> bars = new ArrayList();
      private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue();

      private Graph() {
      }

      void flushQueue() {
         for(Long var1 = (Long)this.queue.poll(); var1 != null; var1 = (Long)this.queue.poll()) {
            this.add(var1);
         }

      }

      public void add(long var1) {
         this.times.add(var1);
         this.bars.clear();
         long var3 = (Long)this.times.get(0);
         int var5 = 1;

         int var6;
         for(var6 = 1; var6 < this.times.size(); ++var6) {
            if (var6 != this.times.size() - 1 && (Long)this.times.get(var6) - var3 <= 1000L) {
               ++var5;
            } else {
               long var7 = ((Long)this.times.get(var6) - var3) / 1000L - 1L;

               for(int var9 = 0; (long)var9 < var7; ++var9) {
                  this.bars.add(0);
               }

               this.bars.add(var5);
               var5 = 1;
               var3 = (Long)this.times.get(var6);
            }
         }

         while(this.bars.size() > 30) {
            var6 = (Integer)this.bars.get(0);

            for(int var10 = 0; var10 < var6; ++var10) {
               this.times.remove(0);
            }

            this.bars.remove(0);
         }

         this.times2.add(var1);
      }

      public void render(float var1, float var2, float var3) {
         if (!this.bars.isEmpty()) {
            float var4 = (float)(FPSGraph.this.getHeight().intValue() - 4);
            float var5 = (float)(FPSGraph.this.getHeight().intValue() - 2);
            int var6 = Math.max(PerformanceSettings.getLockFPS(), PerformanceSettings.LightingFPS);
            int var7 = 8;
            float var8 = var4 * ((float)Math.min(var6, (Integer)this.bars.get(0)) / (float)var6);

            for(int var9 = 1; var9 < this.bars.size() - 1; ++var9) {
               float var10 = var4 * ((float)Math.min(var6, (Integer)this.bars.get(var9)) / (float)var6);
               SpriteRenderer.instance.renderline((Texture)null, FPSGraph.this.getAbsoluteX().intValue() + var7 - 8 + 4, FPSGraph.this.getAbsoluteY().intValue() + (int)(var5 - var8), FPSGraph.this.getAbsoluteX().intValue() + var7 + 4, FPSGraph.this.getAbsoluteY().intValue() + (int)(var5 - var10), var1, var2, var3, 0.35F, 1.0F);
               var7 += 8;
               var8 = var10;
            }

         }
      }

      public void renderFrameTimes(float var1, float var2, float var3) {
         if (!this.times2.isEmpty()) {
            int var4 = 0;
            int var5 = (int)((double)Core.getInstance().getScreenWidth() - FPSGraph.this.getAbsoluteX() * 2.0) / 8;
            var5 = PZMath.min(var5, this.times2.size());

            for(int var6 = 0; var6 < var5 - 1; ++var6) {
               long var7 = (Long)this.times2.get(this.times2.size() - var5 + var6 + 1) - (Long)this.times2.get(this.times2.size() - var5 + var6);
               float var9 = (float)(var7 * 10L);
               SpriteRenderer.instance.renderi((Texture)null, FPSGraph.this.getAbsoluteX().intValue() + var4, FPSGraph.this.getAbsoluteY().intValue() + FPSGraph.this.getHeight().intValue() - (int)var9, 8, (int)var9, var1, var2, var3, 0.35F, (Consumer)null);
               var4 += 8;
            }

            float var10 = 1000.0F / (float)PerformanceSettings.getLockFPS() * 10.0F;
            SpriteRenderer.instance.render((Texture)null, (float)FPSGraph.this.getAbsoluteX().intValue(), (float)((int)(FPSGraph.this.getAbsoluteY() + FPSGraph.this.getHeight() - (double)var10)), (float)Core.getInstance().getScreenWidth(), 2.0F, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
         }
      }
   }
}
