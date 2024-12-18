package zombie.core.skinnedmodel.advancedanimation.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public class AnimEventBroadcaster implements IAnimEventListener {
   private final Map<String, AnimEventListenerList> m_listeners = new HashMap();

   public AnimEventBroadcaster() {
   }

   public void addListener(String var1, IAnimEventListener var2) {
      AnimEventListenerList var3 = this.getOrCreateListenerList(var1);
      var3.listeners.add(var2);
   }

   public void addListener(String var1, IAnimEventListenerBoolean var2) {
      this.addListener(var1, AnimEventListenerWrapperBoolean.wrapper(var2));
   }

   public void addListener(String var1, IAnimEventListenerString var2) {
      this.addListener(var1, AnimEventListenerWrapperString.wrapper(var2));
   }

   public void addListener(String var1, IAnimEventListenerNoParam var2) {
      this.addListener(var1, AnimEventListenerWrapperNoParam.wrapper(var2));
   }

   public void addListener(String var1, IAnimEventListenerFloat var2) {
      this.addListener(var1, AnimEventListenerWrapperFloat.wrapper(var2));
   }

   public void addListener(IAnimEventListenerSetVariableString var1) {
      this.addListener("SetVariable", AnimEventListenerSetVariableWrapperString.wrapper(var1));
   }

   private AnimEventListenerList getOrCreateListenerList(String var1) {
      AnimEventListenerList var2 = this.getAnimEventListenerList(var1);
      if (var2 == null) {
         var2 = new AnimEventListenerList();
         this.m_listeners.put(var1.toLowerCase(), var2);
      }

      return var2;
   }

   private AnimEventListenerList getAnimEventListenerList(String var1) {
      AnimEventListenerList var2 = (AnimEventListenerList)this.m_listeners.get(var1.toLowerCase());
      return var2;
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      if (!this.m_listeners.isEmpty()) {
         AnimEventListenerList var3 = this.getAnimEventListenerList(var2.m_EventName);
         if (var3 != null) {
            List var4 = var3.listeners;

            for(int var5 = 0; var5 < var4.size(); ++var5) {
               IAnimEventListener var6 = (IAnimEventListener)var4.get(var5);
               var6.animEvent(var1, var2);
            }

         }
      }
   }
}
