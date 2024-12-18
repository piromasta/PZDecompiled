package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.List;
import zombie.util.IPooledObject;
import zombie.util.list.PZArrayUtil;

public class AnimationVariableWhileAliveFlagsContainer {
   private final ArrayList<AnimationVariableWhileAliveFlagCounter> m_list = new ArrayList();

   public AnimationVariableWhileAliveFlagsContainer() {
   }

   private AnimationVariableWhileAliveFlagCounter findCounter(AnimationVariableReference var1) {
      return (AnimationVariableWhileAliveFlagCounter)PZArrayUtil.find((List)this.m_list, (var1x) -> {
         return var1x.getVariableRerefence().equals(var1);
      });
   }

   private AnimationVariableWhileAliveFlagCounter getOrCreateCounter(AnimationVariableReference var1) {
      AnimationVariableWhileAliveFlagCounter var2 = this.findCounter(var1);
      if (var2 != null) {
         return var2;
      } else {
         AnimationVariableWhileAliveFlagCounter var3 = AnimationVariableWhileAliveFlagCounter.alloc(var1);
         this.m_list.add(var3);
         return var3;
      }
   }

   public boolean incrementWhileAliveFlagOnce(AnimationVariableReference var1) {
      AnimationVariableWhileAliveFlagCounter var2 = this.getOrCreateCounter(var1);
      if (var2.getCount() > 0) {
         return false;
      } else {
         return var2.increment() > 0;
      }
   }

   public int incrementWhileAliveFlag(AnimationVariableReference var1) {
      return this.getOrCreateCounter(var1).increment();
   }

   public int decrementWhileAliveFlag(AnimationVariableReference var1) {
      AnimationVariableWhileAliveFlagCounter var2 = this.findCounter(var1);
      if (var2 == null) {
         throw new NullPointerException("No counter found for variable: " + var1);
      } else {
         return var2.decrement();
      }
   }

   public void clear() {
      IPooledObject.release((List)this.m_list);
   }

   public int numCounters() {
      return this.m_list.size();
   }

   public AnimationVariableWhileAliveFlagCounter getCounterAt(int var1) {
      return (AnimationVariableWhileAliveFlagCounter)this.m_list.get(var1);
   }
}
