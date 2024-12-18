package zombie.core.skinnedmodel.advancedanimation;

import zombie.util.Pool;
import zombie.util.PooledObject;

public class AnimationVariableWhileAliveFlagCounter extends PooledObject {
   private AnimationVariableReference m_variableReference = null;
   private int m_counter = 0;
   private static final Pool<AnimationVariableWhileAliveFlagCounter> s_pool = new Pool(AnimationVariableWhileAliveFlagCounter::new);

   private AnimationVariableWhileAliveFlagCounter() {
   }

   public static AnimationVariableWhileAliveFlagCounter alloc(AnimationVariableReference var0) {
      AnimationVariableWhileAliveFlagCounter var1 = (AnimationVariableWhileAliveFlagCounter)s_pool.alloc();
      var1.m_variableReference = var0;
      var1.m_counter = 0;
      return var1;
   }

   public void onReleased() {
      this.m_variableReference = null;
      this.m_counter = 0;
      super.onReleased();
   }

   public AnimationVariableReference getVariableRerefence() {
      return this.m_variableReference;
   }

   public int increment() {
      ++this.m_counter;
      return this.m_counter;
   }

   public int decrement() {
      if (this.m_counter == 0) {
         throw new IndexOutOfBoundsException("Too many decrements. var: " + this.m_variableReference);
      } else {
         --this.m_counter;
         return this.m_counter;
      }
   }

   public int getCount() {
      return this.m_counter;
   }
}
