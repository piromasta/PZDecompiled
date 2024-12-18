package zombie.entity.components.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.debug.DebugOptions;
import zombie.network.GameClient;
import zombie.util.list.PZUnmodifiableList;

public class ResourceGroup {
   protected static final ConcurrentLinkedDeque<ResourceGroup> pool = new ConcurrentLinkedDeque();
   private boolean dirty = false;
   private String name;
   private final ArrayList<Resource> resources = new ArrayList();
   private final List<Resource> immutableResources;

   protected static ResourceGroup allocAnonymous() {
      ResourceGroup var0 = (ResourceGroup)pool.poll();
      if (var0 == null) {
         var0 = new ResourceGroup();
      }

      return var0;
   }

   protected static ResourceGroup alloc(String var0) {
      ResourceGroup var1 = (ResourceGroup)pool.poll();
      if (var1 == null) {
         var1 = new ResourceGroup();
      }

      var1.name = (String)Objects.requireNonNull(var0);
      return var1;
   }

   protected static void release(ResourceGroup var0) {
      var0.reset();
      if (!DebugOptions.instance.Checks.ObjectPoolContains.getValue() || !pool.contains(var0)) {
         pool.offer(var0);
      }
   }

   private ResourceGroup() {
      this.immutableResources = PZUnmodifiableList.wrap(this.resources);
   }

   public boolean isDirty() {
      return this.dirty;
   }

   void setDirty() {
      if (!GameClient.bClient) {
         this.dirty = true;
      }
   }

   protected void resetDirty() {
      this.dirty = false;
   }

   public List<Resource> getResources() {
      return this.immutableResources;
   }

   public String getName() {
      return this.name;
   }

   protected int size() {
      return this.resources.size();
   }

   protected Resource get(int var1) {
      return (Resource)this.resources.get(var1);
   }

   protected void add(Resource var1) {
      this.resources.add(var1);
   }

   protected boolean remove(Resource var1) {
      return this.resources.remove(var1);
   }

   private void reset() {
      this.resources.clear();
      this.name = null;
      this.dirty = false;
   }

   public Resource get(String var1) {
      if (var1 != null) {
         for(int var2 = 0; var2 < this.resources.size(); ++var2) {
            Resource var3 = (Resource)this.resources.get(var2);
            if (var3 != null && var3.getId() != null && var3.getId().equalsIgnoreCase(var1)) {
               return var3;
            }
         }
      }

      return null;
   }

   public int getIndex(Resource var1) {
      return this.resources.indexOf(var1);
   }

   public List<Resource> getResources(List<Resource> var1, ResourceIO var2) {
      return this.getResources(this.resources, var1, var2, ResourceType.Any, (ResourceChannel)null, true);
   }

   public List<Resource> getResources(List<Resource> var1, ResourceType var2) {
      return this.getResources(this.resources, var1, ResourceIO.Any, var2, (ResourceChannel)null, true);
   }

   public List<Resource> getResources(List<Resource> var1, ResourceIO var2, ResourceChannel var3) {
      return this.getResources(this.resources, var1, var2, ResourceType.Any, var3, true);
   }

   public List<Resource> getResources(List<Resource> var1, ResourceChannel var2) {
      return this.getResources(this.resources, var1, ResourceIO.Any, ResourceType.Any, var2, true);
   }

   public List<Resource> getResources(List<Resource> var1, ResourceIO var2, ResourceType var3) {
      return this.getResources(this.resources, var1, var2, var3, (ResourceChannel)null, true);
   }

   private List<Resource> getResources(List<Resource> var1, List<Resource> var2, ResourceIO var3, ResourceType var4, ResourceChannel var5, boolean var6) {
      if (var6 && !var2.isEmpty()) {
         var2.clear();
      }

      for(int var7 = 0; var7 < var1.size(); ++var7) {
         Resource var8 = (Resource)var1.get(var7);
         if (var8 != null && (var3 == ResourceIO.Any || var8.getIO() == var3) && (var4 == ResourceType.Any || var8.getType() == var4) && (var5 == null || var8.getChannel() == var5)) {
            var2.add(var8);
         }
      }

      return var2;
   }
}
