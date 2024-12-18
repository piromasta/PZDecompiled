package zombie.core.skinnedmodel.advancedanimation.events;

import java.util.ArrayList;
import java.util.List;

public class AnimEventListenerList {
   public final List<IAnimEventListener> listeners = new ArrayList();

   public AnimEventListenerList() {
   }
}
