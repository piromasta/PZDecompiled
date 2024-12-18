package zombie.iso.fboRenderChunk;

import zombie.iso.IsoObject;

public final class ObjectRenderInfo {
   public final IsoObject m_object;
   public ObjectRenderLayer m_layer;
   public float m_targetAlpha;
   public boolean m_bCutaway;
   public float m_renderX;
   public float m_renderY;
   public float m_renderWidth;
   public float m_renderHeight;
   public float m_renderScaleX;
   public float m_renderScaleY;
   public float m_renderAlpha;

   public ObjectRenderInfo(IsoObject var1) {
      this.m_layer = ObjectRenderLayer.None;
      this.m_targetAlpha = 1.0F;
      this.m_object = var1;
   }
}
