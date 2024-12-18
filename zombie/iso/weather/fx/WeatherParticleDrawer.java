package zombie.iso.weather.fx;

import gnu.trove.list.array.TIntArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import zombie.core.DefaultShader;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;

public final class WeatherParticleDrawer extends TextureDraw.GenericDrawer {
   private int PARTICLE_BYTES = 48;
   private ByteBuffer particleBuffer;
   private final ArrayList<Texture> textures;
   private final TIntArrayList[] particlesByTexture;

   public WeatherParticleDrawer() {
      this.particleBuffer = ByteBuffer.allocate(this.PARTICLE_BYTES * 128);
      this.textures = new ArrayList();
      this.particlesByTexture = new TIntArrayList[8];

      for(int var1 = 0; var1 < this.particlesByTexture.length; ++var1) {
         this.particlesByTexture[var1] = new TIntArrayList();
      }

   }

   public void render() {
      boolean var1 = DefaultShader.isActive;
      int var2 = GL11.glGetInteger(35725);
      int var3 = Texture.lastTextureID;
      GL11.glPushAttrib(1048575);
      GL11.glPushClientAttrib(-1);
      VBORenderer var4 = VBORenderer.getInstance();

      for(int var5 = 0; var5 < this.particlesByTexture.length; ++var5) {
         TIntArrayList var6 = this.particlesByTexture[var5];
         if (!var6.isEmpty()) {
            Texture var7 = (Texture)this.textures.get(var5);
            var4.startRun(VBORenderer.getInstance().FORMAT_PositionColorUV);
            var4.setMode(7);
            var4.setTextureID(var7.getTextureId());

            for(int var8 = 0; var8 < var6.size(); ++var8) {
               int var9 = var6.get(var8);
               int var10 = 0;
               float var11 = this.particleBuffer.getFloat(var9);
               ++var10;
               float var12 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var13 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var14 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var15 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var16 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var17 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var18 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var19 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var20 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var21 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var22 = this.particleBuffer.getFloat(var9 + var10++ * 4);
               float var23 = 0.0F;
               var4.addQuad(var11, var12, var7.getXStart(), var7.getYStart(), var13, var14, var7.getXEnd(), var7.getYStart(), var15, var16, var7.getXEnd(), var7.getYEnd(), var17, var18, var7.getXStart(), var7.getYEnd(), var23, var19, var20, var21, var22);
            }

            var4.endRun();
         }
      }

      var4.flush();
      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      GL20.glUseProgram(var2);
      DefaultShader.isActive = var1;
      ShaderHelper.forgetCurrentlyBound();
      Texture.lastTextureID = var3;
   }

   public void startFrame() {
      this.particleBuffer.clear();

      for(int var1 = 0; var1 < this.particlesByTexture.length; ++var1) {
         this.particlesByTexture[var1].clear();
      }

      this.textures.clear();
   }

   public void endFrame() {
      if (this.particleBuffer.position() != 0) {
         this.particleBuffer.flip();
         SpriteRenderer.instance.drawGeneric(this);
      }
   }

   public void addParticle(Texture var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      this.addParticle(var1, var2, var3, var2 + var4, var3, var2 + var4, var3 + var5, var2, var3 + var5, var6, var7, var8, var9);
   }

   public void addParticle(Texture var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13) {
      if (this.particleBuffer.capacity() < this.particleBuffer.position() + this.PARTICLE_BYTES) {
         ByteBuffer var14 = ByteBuffer.allocate(this.particleBuffer.capacity() + this.PARTICLE_BYTES * 128);
         this.particleBuffer.flip();
         var14.put(this.particleBuffer);
         this.particleBuffer = var14;
      }

      int var15 = this.textures.indexOf(var1);
      if (var15 == -1) {
         var15 = this.textures.size();
         this.textures.add(var1);
      }

      this.particlesByTexture[var15].add(this.particleBuffer.position());
      this.particleBuffer.putFloat(var2);
      this.particleBuffer.putFloat(var3);
      this.particleBuffer.putFloat(var4);
      this.particleBuffer.putFloat(var5);
      this.particleBuffer.putFloat(var6);
      this.particleBuffer.putFloat(var7);
      this.particleBuffer.putFloat(var8);
      this.particleBuffer.putFloat(var9);
      this.particleBuffer.putFloat(var10);
      this.particleBuffer.putFloat(var11);
      this.particleBuffer.putFloat(var12);
      this.particleBuffer.putFloat(var13);
   }

   public void Reset() {
      this.particleBuffer = null;
      this.textures.clear();
      Arrays.fill(this.particlesByTexture, (Object)null);
   }
}
