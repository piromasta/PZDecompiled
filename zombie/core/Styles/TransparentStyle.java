package zombie.core.Styles;

import zombie.IndieGL;
import zombie.core.opengl.GLStateRenderThread;

public final class TransparentStyle extends AbstractStyle {
   private static final long serialVersionUID = 1L;
   public static final TransparentStyle instance = new TransparentStyle();

   public TransparentStyle() {
   }

   public void setupState() {
      IndieGL.glBlendFuncA(770, 771);
   }

   public void resetState() {
      GLStateRenderThread.BlendFuncSeparate.restore();
   }

   public AlphaOp getAlphaOp() {
      return AlphaOp.KEEP;
   }

   public int getStyleID() {
      return 2;
   }

   public boolean getRenderSprite() {
      return true;
   }
}
