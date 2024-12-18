package zombie.core.opengl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public class GLStateRenderThread {
   private static final GLState.C4BooleansValue temp4BooleansValue = new GLState.C4BooleansValue();
   private static final GLState.C3IntsValue temp3IntsValue = new GLState.C3IntsValue();
   private static final GLState.C4IntsValue temp4IntsValue = new GLState.C4IntsValue();
   private static final GLState.CIntFloatValue tempIntFloatValue = new GLState.CIntFloatValue();
   private static final GLState.CIntValue tempIntValue = new GLState.CIntValue();
   public static final CAlphaFunc AlphaFunc = new CAlphaFunc();
   public static final CAlphaTest AlphaTest = new CAlphaTest();
   public static final CBlend Blend = new CBlend();
   public static final CBlendFuncSeparate BlendFuncSeparate = new CBlendFuncSeparate();
   public static final CColorMask ColorMask = new CColorMask();
   public static final CDepthFunc DepthFunc = new CDepthFunc();
   public static final CDepthMask DepthMask = new CDepthMask();
   public static final CDepthTest DepthTest = new CDepthTest();
   public static final CScissorTest ScissorTest = new CScissorTest();
   public static final CStencilFunc StencilFunc = new CStencilFunc();
   public static final CStencilMask StencilMask = new CStencilMask();
   public static final CStencilOp StencilOp = new CStencilOp();
   public static final CStencilTest StencilTest = new CStencilTest();

   public GLStateRenderThread() {
   }

   public static void startFrame() {
      AlphaFunc.setDirty();
      AlphaTest.setDirty();
      Blend.setDirty();
      BlendFuncSeparate.setDirty();
      ColorMask.setDirty();
      DepthFunc.setDirty();
      DepthMask.setDirty();
      DepthTest.setDirty();
      ScissorTest.setDirty();
      StencilFunc.setDirty();
      StencilMask.setDirty();
      StencilOp.setDirty();
      StencilTest.setDirty();
   }

   public static void restore() {
      AlphaFunc.restore();
      AlphaTest.restore();
      Blend.restore();
      BlendFuncSeparate.restore();
      ColorMask.restore();
      DepthFunc.restore();
      DepthMask.restore();
      DepthTest.restore();
      ScissorTest.restore();
      StencilFunc.restore();
      StencilMask.restore();
      StencilOp.restore();
      StencilTest.restore();
   }

   public static final class CAlphaFunc extends GLState.BaseIntFloat {
      public CAlphaFunc() {
      }

      public void set(int var1, float var2) {
         this.set(GLStateRenderThread.tempIntFloatValue.set(var1, var2));
      }

      void Set(GLState.CIntFloatValue var1) {
         GL11.glAlphaFunc(var1.a, var1.b);
      }
   }

   public static final class CAlphaTest extends GLState.BaseBoolean {
      public CAlphaTest() {
      }

      public void set(boolean var1) {
         this.set(var1 ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
      }

      void Set(GLState.CBooleanValue var1) {
         if (var1.value) {
            GL11.glEnable(3008);
         } else {
            GL11.glDisable(3008);
         }

      }
   }

   public static final class CBlend extends GLState.BaseBoolean {
      public CBlend() {
      }

      public void set(boolean var1) {
         this.set(var1 ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
      }

      void Set(GLState.CBooleanValue var1) {
         if (var1.value) {
            GL11.glEnable(3042);
         } else {
            GL11.glDisable(3042);
         }

      }
   }

   public static final class CBlendFuncSeparate extends GLState.Base4Ints {
      public CBlendFuncSeparate() {
      }

      public void set(int var1, int var2, int var3, int var4) {
         this.set(GLStateRenderThread.temp4IntsValue.set(var1, var2, var3, var4));
      }

      public void restore() {
         this.Set((GLState.C4IntsValue)this.getCurrentValue());
      }

      void Set(GLState.C4IntsValue var1) {
         GL14.glBlendFuncSeparate(var1.a, var1.b, var1.c, var1.d);
      }
   }

   public static final class CColorMask extends GLState.Base4Booleans {
      public CColorMask() {
      }

      public void set(boolean var1, boolean var2, boolean var3, boolean var4) {
         this.set(GLStateRenderThread.temp4BooleansValue.set(var1, var2, var3, var4));
      }

      void Set(GLState.C4BooleansValue var1) {
         GL11.glColorMask(var1.a, var1.b, var1.c, var1.d);
      }
   }

   public static final class CDepthFunc extends GLState.BaseInt {
      public CDepthFunc() {
         ((GLState.CIntValue)this.currentValue).value = 513;
      }

      public void set(int var1) {
         this.set(GLStateRenderThread.tempIntValue.set(var1));
      }

      void Set(GLState.CIntValue var1) {
         GL11.glDepthFunc(var1.value);
      }
   }

   public static final class CDepthMask extends GLState.BaseBoolean {
      public CDepthMask() {
      }

      public void set(boolean var1) {
         this.set(var1 ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
      }

      void Set(GLState.CBooleanValue var1) {
         GL11.glDepthMask(var1.value);
      }
   }

   public static final class CDepthTest extends GLState.BaseBoolean {
      public CDepthTest() {
         ((GLState.CBooleanValue)this.currentValue).value = false;
      }

      public void set(boolean var1) {
         this.set(var1 ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
      }

      void Set(GLState.CBooleanValue var1) {
         if (var1.value) {
            GL11.glEnable(2929);
         } else {
            GL11.glDisable(2929);
         }

      }
   }

   public static final class CScissorTest extends GLState.BaseBoolean {
      public CScissorTest() {
         ((GLState.CBooleanValue)this.currentValue).value = false;
      }

      public void set(boolean var1) {
         this.set(var1 ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
      }

      void Set(GLState.CBooleanValue var1) {
         if (var1.value) {
            GL11.glEnable(3089);
         } else {
            GL11.glDisable(3089);
         }

      }
   }

   public static final class CStencilFunc extends GLState.Base3Ints {
      public CStencilFunc() {
         ((GLState.C3IntsValue)this.currentValue).a = 519;
         ((GLState.C3IntsValue)this.currentValue).b = 0;
         ((GLState.C3IntsValue)this.currentValue).c = 0;
      }

      public void set(int var1, int var2, int var3) {
         this.set(GLStateRenderThread.temp3IntsValue.set(var1, var2, var3));
      }

      void Set(GLState.C3IntsValue var1) {
         GL11.glStencilFunc(var1.a, var1.b, var1.c);
      }
   }

   public static final class CStencilMask extends GLState.BaseInt {
      public CStencilMask() {
         ((GLState.CIntValue)this.currentValue).value = -1;
      }

      public void set(int var1) {
         this.set(GLStateRenderThread.tempIntValue.set(var1));
      }

      void Set(GLState.CIntValue var1) {
         GL11.glStencilMask(var1.value);
      }
   }

   public static final class CStencilOp extends GLState.Base3Ints {
      public CStencilOp() {
      }

      public void set(int var1, int var2, int var3) {
         this.set(GLStateRenderThread.temp3IntsValue.set(var1, var2, var3));
      }

      void Set(GLState.C3IntsValue var1) {
         GL11.glStencilOp(var1.a, var1.b, var1.c);
      }
   }

   public static final class CStencilTest extends GLState.BaseBoolean {
      public CStencilTest() {
      }

      public void set(boolean var1) {
         this.set(var1 ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
      }

      void Set(GLState.CBooleanValue var1) {
         if (var1.value) {
            GL11.glEnable(2960);
         } else {
            GL11.glDisable(2960);
         }

      }
   }
}
