package zombie.core.textures;

import java.util.ArrayList;
import java.util.function.Consumer;

public final class AnimatedTexture {
   private final AnimatedTextureID textureID;
   private int frameNumber = 0;
   private final ArrayList<Texture> textures = new ArrayList();
   private long renderTimeMS = 0L;
   private long lastRenderTimeMS = 0L;

   public AnimatedTexture(AnimatedTextureID var1) {
      this.textureID = var1;
   }

   public boolean isReady() {
      return this.textureID.isReady();
   }

   void initTextures() {
      if (this.textures.isEmpty()) {
         if (this.textureID.isReady()) {
            for(int var1 = 0; var1 < this.textureID.frames.size(); ++var1) {
               AnimatedTextureIDFrame var2 = (AnimatedTextureIDFrame)this.textureID.frames.get(var1);
               TextureID var10002 = var2.textureID;
               String var10003 = this.textureID.getPath().getPath();
               Texture var3 = new Texture(var10002, var10003 + "#" + (var1 + 1));
               this.textures.add(var3);
            }

         }
      }
   }

   public int getWidth() {
      return this.textureID.isReady() ? this.textureID.getWidth() : -1;
   }

   public int getHeight() {
      return this.textureID.isReady() ? this.textureID.getHeight() : -1;
   }

   public void renderToWidth(int var1, int var2, int var3, float var4, float var5, float var6, float var7) {
      if (!this.textureID.isReady()) {
         Texture.getErrorTexture().render((float)var1, (float)var2, (float)var3, (float)var3, var4, var5, var6, var7, (Consumer)null);
      } else {
         this.initTextures();
         AnimatedTextureIDFrame var9 = this.textureID.getFrame(this.frameNumber);
         if (var9 == null) {
            Texture.getErrorTexture().render((float)var1, (float)var2, (float)var3, (float)var3, var4, var5, var6, var7, (Consumer)null);
         } else {
            float var10 = (float)var3 / (float)this.textureID.getWidth();
            int var8 = (int)((float)this.textureID.getHeight() * var10);
            this.render(var1, var2, var3, var8, var4, var5, var6, var7);
         }
      }
   }

   public void render(int var1, int var2, int var3, int var4, float var5, float var6, float var7, float var8) {
      if (!this.textureID.isReady()) {
         Texture.getErrorTexture().render((float)var1, (float)var2, (float)var3, (float)var4, var5, var6, var7, var8, (Consumer)null);
      } else {
         this.initTextures();
         AnimatedTextureIDFrame var9 = this.textureID.getFrame(this.frameNumber);
         if (var9 == null) {
            Texture.getErrorTexture().render((float)var1, (float)var2, (float)var3, (float)var4, var5, var6, var7, var8, (Consumer)null);
         } else {
            Texture var10 = (Texture)this.textures.get(this.frameNumber);
            float var11 = (float)var3 / (float)this.textureID.getWidth();
            float var12 = (float)var4 / (float)this.textureID.getHeight();
            var10.render((float)var1 + (float)var9.apngFrame.x_offset * var11, (float)var2 + (float)var9.apngFrame.y_offset * var12, (float)var9.apngFrame.width * var11, (float)var9.apngFrame.height * var12, var5, var6, var7, var8, (Consumer)null);
            long var13 = System.currentTimeMillis();
            long var15 = this.lastRenderTimeMS == 0L ? 0L : var13 - this.lastRenderTimeMS;
            this.lastRenderTimeMS = var13;
            short var17 = var9.apngFrame.delay_num;
            short var18 = var9.apngFrame.delay_den;
            if (var18 == 0) {
               var18 = 100;
            }

            if (var17 == 0) {
               this.frameNumber = (this.frameNumber + 1) % this.textureID.getFrameCount();
            } else {
               this.renderTimeMS += var15;
               float var19 = 1000.0F / (float)var18;

               for(long var20 = (long)((float)(this.textureID.getFrameCount() * var17) * var19); this.renderTimeMS >= var20; this.renderTimeMS -= var20) {
               }

               this.frameNumber = (int)(this.renderTimeMS / (long)this.textureID.getFrameCount());
            }

         }
      }
   }
}
