package zombie.core.opengl;

import zombie.core.SpriteRenderer;
import zombie.util.Type;

public final class GLState {
   public static final CAlphaFunc AlphaFunc = new CAlphaFunc();
   public static final CAlphaTest AlphaTest = new CAlphaTest();
   public static final CBlendFunc BlendFunc = new CBlendFunc();
   public static final CBlendFuncSeparate BlendFuncSeparate = new CBlendFuncSeparate();
   public static final CColorMask ColorMask = new CColorMask();
   public static final CStencilFunc StencilFunc = new CStencilFunc();
   public static final CStencilMask StencilMask = new CStencilMask();
   public static final CStencilOp StencilOp = new CStencilOp();
   public static final CStencilTest StencilTest = new CStencilTest();

   public GLState() {
   }

   public static void startFrame() {
      AlphaFunc.setDirty();
      AlphaTest.setDirty();
      BlendFunc.setDirty();
      BlendFuncSeparate.setDirty();
      ColorMask.setDirty();
      StencilFunc.setDirty();
      StencilMask.setDirty();
      StencilOp.setDirty();
      StencilTest.setDirty();
   }

   public static final class CAlphaFunc extends BaseIntFloat {
      public CAlphaFunc() {
      }

      void Set(CIntFloatValue var1) {
         SpriteRenderer.instance.glAlphaFunc(var1.a, var1.b);
      }
   }

   public static final class CAlphaTest extends BaseBoolean {
      public CAlphaTest() {
      }

      void Set(CBooleanValue var1) {
         if (var1.value) {
            SpriteRenderer.instance.glEnable(3008);
         } else {
            SpriteRenderer.instance.glDisable(3008);
         }

      }
   }

   public static final class CBlendFunc extends Base2Ints {
      public CBlendFunc() {
      }

      void Set(C2IntsValue var1) {
         SpriteRenderer.instance.glBlendFunc(var1.a, var1.b);
      }
   }

   public static final class CBlendFuncSeparate extends Base4Ints {
      public CBlendFuncSeparate() {
      }

      void Set(C4IntsValue var1) {
         SpriteRenderer.instance.glBlendFuncSeparate(var1.a, var1.b, var1.c, var1.d);
      }
   }

   public static final class CColorMask extends Base4Booleans {
      public CColorMask() {
      }

      void Set(C4BooleansValue var1) {
         SpriteRenderer.instance.glColorMask(var1.a ? 1 : 0, var1.b ? 1 : 0, var1.c ? 1 : 0, var1.d ? 1 : 0);
      }
   }

   public static final class CStencilFunc extends Base3Ints {
      public CStencilFunc() {
      }

      void Set(C3IntsValue var1) {
         SpriteRenderer.instance.glStencilFunc(var1.a, var1.b, var1.c);
      }
   }

   public static final class CStencilMask extends BaseInt {
      public CStencilMask() {
      }

      void Set(CIntValue var1) {
         SpriteRenderer.instance.glStencilMask(var1.value);
      }
   }

   public static final class CStencilOp extends Base3Ints {
      public CStencilOp() {
      }

      void Set(C3IntsValue var1) {
         SpriteRenderer.instance.glStencilOp(var1.a, var1.b, var1.c);
      }
   }

   public static final class CStencilTest extends BaseBoolean {
      public CStencilTest() {
      }

      void Set(CBooleanValue var1) {
         if (var1.value) {
            SpriteRenderer.instance.glEnable(2960);
         } else {
            SpriteRenderer.instance.glDisable(2960);
         }

      }
   }

   public abstract static class Base4Ints extends IOpenGLState<C4IntsValue> {
      public Base4Ints() {
      }

      C4IntsValue defaultValue() {
         return new C4IntsValue();
      }
   }

   public abstract static class Base3Ints extends IOpenGLState<C3IntsValue> {
      public Base3Ints() {
      }

      C3IntsValue defaultValue() {
         return new C3IntsValue();
      }
   }

   public abstract static class Base2Ints extends IOpenGLState<C2IntsValue> {
      public Base2Ints() {
      }

      C2IntsValue defaultValue() {
         return new C2IntsValue();
      }
   }

   public abstract static class BaseInt extends IOpenGLState<CIntValue> {
      public BaseInt() {
      }

      CIntValue defaultValue() {
         return new CIntValue();
      }
   }

   public abstract static class BaseIntFloat extends IOpenGLState<CIntFloatValue> {
      public BaseIntFloat() {
      }

      CIntFloatValue defaultValue() {
         return new CIntFloatValue();
      }
   }

   public abstract static class Base4Booleans extends IOpenGLState<C4BooleansValue> {
      public Base4Booleans() {
      }

      C4BooleansValue defaultValue() {
         return new C4BooleansValue();
      }
   }

   public abstract static class BaseBoolean extends IOpenGLState<CBooleanValue> {
      public BaseBoolean() {
      }

      CBooleanValue defaultValue() {
         return new CBooleanValue(true);
      }
   }

   public static final class CIntFloatValue implements IOpenGLState.Value {
      int a;
      float b;

      public CIntFloatValue() {
      }

      public CIntFloatValue set(int var1, float var2) {
         this.a = var1;
         this.b = var2;
         return this;
      }

      public boolean equals(Object var1) {
         CIntFloatValue var2 = (CIntFloatValue)Type.tryCastTo(var1, CIntFloatValue.class);
         return var2 != null && var2.a == this.a && var2.b == this.b;
      }

      public IOpenGLState.Value set(IOpenGLState.Value var1) {
         CIntFloatValue var2 = (CIntFloatValue)var1;
         this.a = var2.a;
         this.b = var2.b;
         return this;
      }
   }

   public static final class C4IntsValue implements IOpenGLState.Value {
      int a;
      int b;
      int c;
      int d;

      public C4IntsValue() {
      }

      public C4IntsValue set(int var1, int var2, int var3, int var4) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
         return this;
      }

      public boolean equals(Object var1) {
         C4IntsValue var2 = (C4IntsValue)Type.tryCastTo(var1, C4IntsValue.class);
         return var2 != null && var2.a == this.a && var2.b == this.b && var2.c == this.c && var2.d == this.d;
      }

      public IOpenGLState.Value set(IOpenGLState.Value var1) {
         C4IntsValue var2 = (C4IntsValue)var1;
         this.a = var2.a;
         this.b = var2.b;
         this.c = var2.c;
         this.d = var2.d;
         return this;
      }
   }

   public static final class C3IntsValue implements IOpenGLState.Value {
      int a;
      int b;
      int c;

      public C3IntsValue() {
      }

      public C3IntsValue set(int var1, int var2, int var3) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         return this;
      }

      public boolean equals(Object var1) {
         C3IntsValue var2 = (C3IntsValue)Type.tryCastTo(var1, C3IntsValue.class);
         return var2 != null && var2.a == this.a && var2.b == this.b && var2.c == this.c;
      }

      public IOpenGLState.Value set(IOpenGLState.Value var1) {
         C3IntsValue var2 = (C3IntsValue)var1;
         this.a = var2.a;
         this.b = var2.b;
         this.c = var2.c;
         return this;
      }
   }

   public static final class C2IntsValue implements IOpenGLState.Value {
      int a;
      int b;

      public C2IntsValue() {
      }

      public C2IntsValue set(int var1, int var2) {
         this.a = var1;
         this.b = var2;
         return this;
      }

      public boolean equals(Object var1) {
         C2IntsValue var2 = (C2IntsValue)Type.tryCastTo(var1, C2IntsValue.class);
         return var2 != null && var2.a == this.a && var2.b == this.b;
      }

      public IOpenGLState.Value set(IOpenGLState.Value var1) {
         C2IntsValue var2 = (C2IntsValue)var1;
         this.a = var2.a;
         this.b = var2.b;
         return this;
      }
   }

   public static class CIntValue implements IOpenGLState.Value {
      int value;

      public CIntValue() {
      }

      public CIntValue set(int var1) {
         this.value = var1;
         return this;
      }

      public boolean equals(Object var1) {
         return var1 instanceof CIntValue && ((CIntValue)var1).value == this.value;
      }

      public IOpenGLState.Value set(IOpenGLState.Value var1) {
         this.value = ((CIntValue)var1).value;
         return this;
      }
   }

   public static final class C4BooleansValue implements IOpenGLState.Value {
      boolean a;
      boolean b;
      boolean c;
      boolean d;

      public C4BooleansValue() {
      }

      public C4BooleansValue set(boolean var1, boolean var2, boolean var3, boolean var4) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
         return this;
      }

      public boolean equals(Object var1) {
         C4BooleansValue var2 = (C4BooleansValue)Type.tryCastTo(var1, C4BooleansValue.class);
         return var2 != null && var2.a == this.a && var2.b == this.b && var2.c == this.c;
      }

      public IOpenGLState.Value set(IOpenGLState.Value var1) {
         C4BooleansValue var2 = (C4BooleansValue)var1;
         this.a = var2.a;
         this.b = var2.b;
         this.c = var2.c;
         this.d = var2.d;
         return this;
      }
   }

   public static class CBooleanValue implements IOpenGLState.Value {
      public static final CBooleanValue TRUE = new CBooleanValue(true);
      public static final CBooleanValue FALSE = new CBooleanValue(false);
      boolean value;

      CBooleanValue(boolean var1) {
         this.value = var1;
      }

      public boolean equals(Object var1) {
         return var1 instanceof CBooleanValue && ((CBooleanValue)var1).value == this.value;
      }

      public IOpenGLState.Value set(IOpenGLState.Value var1) {
         this.value = ((CBooleanValue)var1).value;
         return this;
      }
   }
}
