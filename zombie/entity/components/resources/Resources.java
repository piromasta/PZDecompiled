package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.GameWindow;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.enums.EnumBitStore;
import zombie.network.GameClient;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.resources.ResourcesScript;
import zombie.ui.ObjectTooltip;
import zombie.util.list.PZUnmodifiableList;

@DebugClassFields
public class Resources extends Component {
   public static final String defaultGroup = "resources";
   private static final List<Resource> _emptyResources = PZUnmodifiableList.wrap(new ArrayList());
   private final ArrayList<Resource> resources = new ArrayList();
   private final Map<String, Resource> idToResourceMap = new HashMap();
   private final ArrayList<ResourceGroup> namedGroups = new ArrayList();
   private final Map<String, ResourceGroup> namedGroupMap = new HashMap();
   private final EnumBitStore<ResourceChannel> inputChannels = EnumBitStore.noneOf(ResourceChannel.class);
   private final EnumBitStore<ResourceChannel> outputChannels = EnumBitStore.noneOf(ResourceChannel.class);
   private final ArrayList<ResourceGroup> channelGroups = new ArrayList();
   private final Map<ResourceChannel, ResourceGroup> inputChannelMap = new HashMap();
   private final Map<ResourceChannel, ResourceGroup> outputChannelMap = new HashMap();
   private final List<Resource> immutableResources;
   private boolean dirty;

   private Resources() {
      super(ComponentType.Resources);
      this.immutableResources = PZUnmodifiableList.wrap(this.resources);
      this.dirty = false;
   }

   boolean isDirty() {
      return this.dirty;
   }

   void setDirty() {
      if (!GameClient.bClient) {
         this.dirty = true;
      }
   }

   void resetDirty() {
      if (this.dirty) {
         int var1;
         for(var1 = 0; var1 < this.namedGroups.size(); ++var1) {
            ((ResourceGroup)this.namedGroups.get(var1)).resetDirty();
         }

         for(var1 = 0; var1 < this.resources.size(); ++var1) {
            ((Resource)this.resources.get(var1)).resetDirty();
         }

         this.dirty = false;
      }

   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
      ResourcesScript var2 = (ResourcesScript)var1;
      ArrayList var3 = var2.getGroupNames();

      for(int var6 = 0; var6 < var3.size(); ++var6) {
         String var4 = (String)var3.get(var6);
         ArrayList var5 = var2.getBlueprintGroup(var4);

         for(int var7 = 0; var7 < var5.size(); ++var7) {
            this.createResource(var4, (ResourceBlueprint)var5.get(var7));
         }
      }

   }

   protected void reset() {
      super.reset();
      this.resetResources();
      this.dirty = false;
   }

   private void resetResources() {
      int var1;
      for(var1 = 0; var1 < this.namedGroups.size(); ++var1) {
         ResourceGroup var2 = (ResourceGroup)this.namedGroups.get(var1);

         for(int var3 = 0; var3 < var2.getResources().size(); ++var3) {
            Resource var4 = (Resource)var2.getResources().get(var3);
            ResourceFactory.releaseResource(var4);
         }

         ResourceGroup.release(var2);
      }

      for(var1 = 0; var1 < this.channelGroups.size(); ++var1) {
         ResourceGroup.release((ResourceGroup)this.channelGroups.get(var1));
      }

      this.resources.clear();
      this.idToResourceMap.clear();
      this.namedGroups.clear();
      this.namedGroupMap.clear();
      this.inputChannels.clear();
      this.outputChannels.clear();
      this.channelGroups.clear();
      this.inputChannelMap.clear();
      this.outputChannelMap.clear();
   }

   public boolean isValid() {
      return super.isValid();
   }

   protected void onAddedToOwner() {
   }

   protected void onRemovedFromOwner() {
   }

   protected void onConnectComponents() {
   }

   protected void onFirstCreation() {
   }

   public List<Resource> getResources() {
      return this.immutableResources;
   }

   public ResourceGroup getResourceGroup(String var1) {
      return (ResourceGroup)this.namedGroupMap.get(var1);
   }

   public List<Resource> getResourcesForGroup(String var1) {
      ResourceGroup var2 = this.getResourceGroup(var1);
      return var2 != null ? var2.getResources() : null;
   }

   public void createResourceFromSerial(String var1) {
      this.createResourceFromSerial("resources", var1);
   }

   public void createResourceFromSerial(String var1, String var2) {
      try {
         ResourceBlueprint var3 = ResourceBlueprint.Deserialize(var2);
         this.createResource(var1, var3);
         ResourceBlueprint.release(var3);
      } catch (Exception var4) {
         DebugLog.General.warn("ResourceBlueprint serial: " + var2);
         var4.printStackTrace();
      }

   }

   public void createResource(ResourceBlueprint var1) {
      this.createResource("resources", var1);
   }

   public void createResource(String var1, ResourceBlueprint var2) {
      Resource var3 = ResourceFactory.createResource(var2);
      this.addResourceInternal(var1, var3);
      if (this.isAddedToEngine()) {
      }

   }

   private void addResourceInternal(String var1, Resource var2) {
      if (var2 == null) {
         DebugLog.General.warn("unable to add resource 'null'");
      } else if (this.idToResourceMap.containsKey(var2.getId())) {
         DebugLog.General.warn("unable to add resource, duplicate ID '" + var2.getId() + "'");
      } else {
         this.resources.add(var2);
         this.idToResourceMap.put(var2.getId(), var2);
         ResourceGroup var3 = (ResourceGroup)this.namedGroupMap.get(var1);
         if (var3 == null) {
            var3 = ResourceGroup.alloc(var1);
            this.namedGroups.add(var3);
            this.namedGroupMap.put(var1, var3);
         }

         var3.add(var2);
         this.addChannelResource(var2);
         var2.setGroup(var3);
         var2.setResourcesComponent(this);
      }
   }

   public void removeResourceGroup(String var1) {
      ResourceGroup var2 = (ResourceGroup)this.namedGroupMap.remove(var1);
      this.removeResourceGroup(var2);
   }

   public void removeResourceGroup(ResourceGroup var1) {
      this.removeResourceGroupInternal(var1);
   }

   private void removeResourceGroupInternal(ResourceGroup var1) {
      if (var1 != null) {
         if (var1.size() > 0) {
            for(int var2 = 0; var2 < var1.getResources().size(); ++var2) {
               this.removeResourceInternal((Resource)var1.getResources().get(var2), false);
            }
         }

         this.namedGroups.remove(var1);
         ResourceGroup.release(var1);
      }
   }

   public void removeResource(String var1) {
      Resource var2 = (Resource)this.idToResourceMap.get(var1);
      this.removeResourceInternal(var2, true);
   }

   public void removeResource(Resource var1) {
      this.removeResourceInternal(var1, true);
   }

   private void removeResourceInternal(Resource var1, boolean var2) {
      if (var1 != null) {
         if (this.resources.remove(var1)) {
            this.idToResourceMap.remove(var1.getId());
            if (var2) {
               ResourceGroup var3 = var1.getGroup();
               if (var3.size() > 0) {
                  var3.remove(var1);
               }

               if (var3.size() == 0) {
                  this.removeResourceGroupInternal(var3);
               }
            }

            this.removeChannelResource(var1);
            var1.setGroup((ResourceGroup)null);
            var1.setResourcesComponent((Resources)null);
            ResourceFactory.releaseResource(var1);
         }
      }
   }

   private void addChannelResource(Resource var1) {
      if (var1.getChannel() != ResourceChannel.NO_CHANNEL) {
         if (var1.getIO() == ResourceIO.Input && var1.getIO() == ResourceIO.Output) {
            ResourceIO var2 = var1.getIO();
            Map var3 = var2 == ResourceIO.Input ? this.inputChannelMap : this.outputChannelMap;
            EnumBitStore var4 = var2 == ResourceIO.Input ? this.inputChannels : this.outputChannels;
            ResourceGroup var5 = (ResourceGroup)var3.get(var1.getChannel());
            if (var5 == null) {
               var5 = ResourceGroup.allocAnonymous();
               this.channelGroups.add(var5);
               var3.put(var1.getChannel(), var5);
               var4.add(var1.getChannel());
            }

            var5.add(var1);
         }
      }
   }

   private void removeChannelResource(Resource var1) {
      if (var1.getChannel() != ResourceChannel.NO_CHANNEL) {
         if (var1.getIO() == ResourceIO.Input && var1.getIO() == ResourceIO.Output) {
            ResourceIO var2 = var1.getIO();
            Map var3 = var2 == ResourceIO.Input ? this.inputChannelMap : this.outputChannelMap;
            EnumBitStore var4 = var2 == ResourceIO.Input ? this.inputChannels : this.outputChannels;
            ResourceGroup var5 = (ResourceGroup)var3.get(var1.getChannel());
            if (var5 != null) {
               var5.remove(var1);
               if (var5.size() == 0) {
                  this.channelGroups.remove(var5);
                  var3.remove(var1.getChannel());
                  var4.remove(var1.getChannel());
                  ResourceGroup.release(var5);
               }
            }

         }
      }
   }

   public Resource getResource(String var1) {
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

   public Resource getResource(int var1) {
      return var1 >= 0 && var1 < this.resources.size() ? (Resource)this.resources.get(var1) : null;
   }

   public int getResourceIndex(Resource var1) {
      return this.resources.indexOf(var1);
   }

   public int getResourceCount() {
      return this.resources.size();
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

   public List<Resource> getResourcesFromGroup(String var1, List<Resource> var2, ResourceIO var3) {
      return this.getResourcesFromGroup(var1, var2, var3, ResourceType.Any, (ResourceChannel)null, true);
   }

   public List<Resource> getResourcesFromGroup(String var1, List<Resource> var2, ResourceType var3) {
      return this.getResourcesFromGroup(var1, var2, ResourceIO.Any, var3, (ResourceChannel)null, true);
   }

   public List<Resource> getResourcesFromGroup(String var1, List<Resource> var2, ResourceIO var3, ResourceChannel var4) {
      return this.getResourcesFromGroup(var1, var2, var3, ResourceType.Any, var4, true);
   }

   public List<Resource> getResourcesFromGroup(String var1, List<Resource> var2, ResourceChannel var3) {
      return this.getResourcesFromGroup(var1, var2, ResourceIO.Any, ResourceType.Any, var3, true);
   }

   public List<Resource> getResourcesFromGroup(String var1, List<Resource> var2, ResourceIO var3, ResourceType var4) {
      return this.getResourcesFromGroup(var1, var2, var3, var4, (ResourceChannel)null, true);
   }

   private List<Resource> getResourcesFromGroup(String var1, List<Resource> var2, ResourceIO var3, ResourceType var4, ResourceChannel var5, boolean var6) {
      List var7 = _emptyResources;
      if (var1 != null) {
         ResourceGroup var8 = (ResourceGroup)this.namedGroupMap.get(var1);
         if (var8 == null) {
            DebugLog.General.warn("Group '" + var1 + "' does not exist.");
            return var2;
         }

         var7 = var8.getResources();
      }

      return this.getResources(var7, var2, var3, var4, var5, var6);
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

   protected boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      switch (var2) {
         default:
            return false;
      }
   }

   protected void saveSyncData(ByteBuffer var1) throws IOException {
      this.save(var1);
   }

   protected void loadSyncData(ByteBuffer var1) throws IOException {
      this.load(var1, 219);
   }

   protected void save(ByteBuffer var1) throws IOException {
      super.save(var1);
      var1.putInt(this.namedGroups.size());

      for(int var4 = 0; var4 < this.namedGroups.size(); ++var4) {
         ResourceGroup var3 = (ResourceGroup)this.namedGroups.get(var4);
         GameWindow.WriteString(var1, var3.getName());
         var1.putInt(var3.size());

         for(int var5 = 0; var5 < var3.getResources().size(); ++var5) {
            Resource var2 = (Resource)var3.getResources().get(var5);
            var1.put(var2.getType().getId());
            var2.save(var1);
         }
      }

   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      if (!this.resources.isEmpty()) {
         this.resetResources();
      }

      int var3 = var1.getInt();
      boolean var4 = false;

      for(int var7 = 0; var7 < var3; ++var7) {
         String var5 = GameWindow.ReadString(var1);
         int var10 = var1.getInt();

         for(int var8 = 0; var8 < var10; ++var8) {
            ResourceType var9 = ResourceType.fromId(var1.get());
            Resource var6 = ResourceFactory.createBlancResource(var9);
            var6.load(var1, var2);
            this.addResourceInternal(var5, var6);
         }
      }

   }

   protected void onComponentEvent(ComponentEvent var1) {
   }

   protected void onEntityEvent(EntityEvent var1) {
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
   }

   protected void renderlast() {
   }
}
