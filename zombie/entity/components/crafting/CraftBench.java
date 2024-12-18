package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.core.raknet.UdpConnection;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceChannel;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.enums.EnumBitStore;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftBenchScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.ui.ObjectTooltip;

public class CraftBench extends Component {
   private final EnumBitStore<ResourceChannel> fluidInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
   private final EnumBitStore<ResourceChannel> energyInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
   private String recipeTagQuery;

   private CraftBench() {
      super(ComponentType.CraftBench);
   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
      CraftBenchScript var2 = (CraftBenchScript)var1;
      this.recipeTagQuery = var2.getRecipeTagQuery();
      this.fluidInputChannels.copyFrom(var2.getFluidInputChannels());
      this.energyInputChannels.copyFrom(var2.getEnergyInputChannels());
   }

   public EnumBitStore<ResourceChannel> getFluidInputChannels() {
      return this.fluidInputChannels;
   }

   public EnumBitStore<ResourceChannel> getEnergyInputChannels() {
      return this.energyInputChannels;
   }

   public String getRecipeTagQuery() {
      return this.recipeTagQuery;
   }

   public void setRecipeTagQuery(String var1) {
      if (this.recipeTagQuery == null || !this.recipeTagQuery.equalsIgnoreCase(var1)) {
         this.recipeTagQuery = var1;
      }

   }

   public List<CraftRecipe> getRecipes() {
      return CraftRecipeManager.queryRecipes(this.recipeTagQuery);
   }

   public ArrayList<Resource> getResources() {
      return new ArrayList();
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
   }

   protected void renderlast() {
   }

   protected void reset() {
      super.reset();
      this.recipeTagQuery = null;
      this.fluidInputChannels.clear();
      this.energyInputChannels.clear();
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

   protected void onComponentEvent(ComponentEvent var1) {
   }

   protected void onEntityEvent(EntityEvent var1) {
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
      GameWindow.WriteString(var1, this.recipeTagQuery);
      this.fluidInputChannels.save(var1);
      this.energyInputChannels.save(var1);
   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.recipeTagQuery = GameWindow.ReadString(var1);
      this.fluidInputChannels.load(var1);
      this.energyInputChannels.load(var1);
   }
}
