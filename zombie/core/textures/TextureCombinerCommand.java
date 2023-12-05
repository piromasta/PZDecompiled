package zombie.core.textures;

import java.util.ArrayList;
import zombie.core.opengl.SmartShader;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayUtil;

public final class TextureCombinerCommand {
   public static final int DEFAULT_SRC_A = 1;
   public static final int DEFAULT_DST_A = 771;
   public int x = -1;
   public int y = -1;
   public int w = -1;
   public int h = -1;
   public Texture mask;
   public Texture tex;
   public int blendSrc;
   public int blendDest;
   public int blendSrcA;
   public int blendDestA;
   public SmartShader shader;
   public ArrayList<TextureCombinerShaderParam> shaderParams = null;
   public static final ObjectPool<TextureCombinerCommand> pool = new ObjectPool(TextureCombinerCommand::new);

   public TextureCombinerCommand() {
   }

   public String toString() {
      String var1 = System.lineSeparator();
      return "{" + var1 + "\tpos: " + this.x + "," + this.y + var1 + "\tsize: " + this.w + "," + this.h + var1 + "\tmask:" + this.mask + var1 + "\ttex:" + this.tex + var1 + "\tblendSrc:" + this.blendSrc + var1 + "\tblendDest:" + this.blendDest + var1 + "\tblendSrcA:" + this.blendSrcA + var1 + "\tblendDestA:" + this.blendDestA + var1 + "\tshader:" + this.shader + var1 + "\tshaderParams:" + PZArrayUtil.arrayToString((Iterable)this.shaderParams) + var1 + "}";
   }

   public TextureCombinerCommand init(Texture var1) {
      this.tex = this.requireNonNull(var1);
      this.blendSrc = 770;
      this.blendDest = 771;
      this.blendSrcA = 1;
      this.blendDestA = 771;
      return this;
   }

   public TextureCombinerCommand initSeparate(Texture var1, int var2, int var3, int var4, int var5) {
      this.tex = this.requireNonNull(var1);
      this.blendSrc = var2;
      this.blendDest = var3;
      this.blendSrcA = var4;
      this.blendDestA = var5;
      return this;
   }

   public TextureCombinerCommand init(Texture var1, int var2, int var3) {
      return this.initSeparate(var1, var2, var3, 1, 771);
   }

   public TextureCombinerCommand init(Texture var1, SmartShader var2) {
      this.tex = this.requireNonNull(var1);
      this.shader = var2;
      this.blendSrc = 770;
      this.blendDest = 771;
      this.blendSrcA = 1;
      this.blendDestA = 771;
      return this;
   }

   public TextureCombinerCommand init(Texture var1, SmartShader var2, Texture var3, int var4, int var5) {
      this.tex = this.requireNonNull(var1);
      this.shader = var2;
      this.blendSrc = var4;
      this.blendDest = var5;
      this.blendSrcA = 1;
      this.blendDestA = 771;
      this.mask = this.requireNonNull(var3);
      return this;
   }

   public TextureCombinerCommand init(Texture var1, int var2, int var3, int var4, int var5) {
      this.tex = this.requireNonNull(var1);
      this.x = var2;
      this.y = var3;
      this.w = var4;
      this.h = var5;
      this.blendSrc = 770;
      this.blendDest = 771;
      this.blendSrcA = 1;
      this.blendDestA = 771;
      return this;
   }

   public TextureCombinerCommand initSeparate(Texture var1, SmartShader var2, ArrayList<TextureCombinerShaderParam> var3, Texture var4, int var5, int var6, int var7, int var8) {
      this.tex = this.requireNonNull(var1);
      this.shader = var2;
      this.blendSrc = var5;
      this.blendDest = var6;
      this.blendSrcA = var7;
      this.blendDestA = var8;
      this.mask = this.requireNonNull(var4);
      if (this.shaderParams == null) {
         this.shaderParams = new ArrayList();
      }

      this.shaderParams.clear();
      this.shaderParams.addAll(var3);
      return this;
   }

   public TextureCombinerCommand init(Texture var1, SmartShader var2, ArrayList<TextureCombinerShaderParam> var3, Texture var4, int var5, int var6) {
      return this.initSeparate(var1, var2, var3, var4, var5, var6, 1, 771);
   }

   public TextureCombinerCommand init(Texture var1, SmartShader var2, ArrayList<TextureCombinerShaderParam> var3) {
      this.tex = this.requireNonNull(var1);
      this.blendSrc = 770;
      this.blendDest = 771;
      this.blendSrcA = 1;
      this.blendDestA = 771;
      this.shader = var2;
      if (this.shaderParams == null) {
         this.shaderParams = new ArrayList();
      }

      this.shaderParams.clear();
      this.shaderParams.addAll(var3);
      return this;
   }

   private Texture requireNonNull(Texture var1) {
      return var1 == null ? Texture.getErrorTexture() : var1;
   }

   public static TextureCombinerCommand get() {
      TextureCombinerCommand var0 = (TextureCombinerCommand)pool.alloc();
      var0.x = -1;
      var0.tex = null;
      var0.mask = null;
      var0.shader = null;
      if (var0.shaderParams != null) {
         var0.shaderParams.clear();
      }

      return var0;
   }
}
