package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityNetwork;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.Resources;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.PlayerID;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.MashingLogicScript;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZUnmodifiableList;

public class MashingLogic extends Component {
   private static final List<CraftRecipe> _emptyRecipeList = PZUnmodifiableList.wrap(new ArrayList());
   private static final List<Resource> _emptyResourceList = PZUnmodifiableList.wrap(new ArrayList());
   private String recipeTagQuery;
   private List<CraftRecipe> recipes;
   private final CraftRecipeData craftData;
   private final CraftRecipeData craftTestData;
   private CraftRecipe currentRecipe;
   private String resourceFluidID;
   private String inputsGroupName;
   private double elapsedTime = 0.0;
   private double lastWorldAge = -1.0;
   private final List<Resource> internalResourceList = new ArrayList();
   private boolean startRequested = false;
   private boolean stopRequested = false;
   private IsoPlayer requestingPlayer = null;
   private float barrelConsumedAmount = 0.0F;

   private MashingLogic() {
      super(ComponentType.MashingLogic);
      this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
      this.craftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
      MashingLogicScript var2 = (MashingLogicScript)var1;
      this.recipeTagQuery = null;
      this.setRecipeTagQuery(var2.getRecipeTagQuery());
      this.inputsGroupName = var2.getInputsGroupName();
      this.resourceFluidID = var2.getResourceFluidID();
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
   }

   protected void renderlast() {
   }

   protected void reset() {
      super.reset();
      this.recipeTagQuery = null;
      this.recipes = null;
      this.craftData.reset();
      this.craftTestData.reset();
      this.startRequested = false;
      this.stopRequested = false;
      this.requestingPlayer = null;
      this.inputsGroupName = null;
      this.resourceFluidID = null;
      this.barrelConsumedAmount = 0.0F;
      this.internalResourceList.clear();
      this.clearRecipe();
   }

   private void clearRecipe() {
      this.elapsedTime = 0.0;
      this.lastWorldAge = -1.0;
      this.currentRecipe = null;
   }

   public double getElapsedTime() {
      return this.elapsedTime;
   }

   public void setElapsedTime(double var1) {
      this.elapsedTime = var1;
   }

   public double getLastWorldAge() {
      return this.lastWorldAge;
   }

   public void setLastWorldAge(double var1) {
      this.lastWorldAge = var1;
   }

   public boolean isStartRequested() {
      return this.startRequested;
   }

   void setStartRequested(boolean var1) {
      this.startRequested = var1;
   }

   public boolean isStopRequested() {
      return this.stopRequested;
   }

   void setStopRequested(boolean var1) {
      this.stopRequested = var1;
   }

   public IsoPlayer getRequestingPlayer() {
      return this.requestingPlayer;
   }

   void setRequestingPlayer(IsoPlayer var1) {
      this.requestingPlayer = var1;
   }

   CraftRecipeData getCraftData() {
      return this.craftData;
   }

   CraftRecipeData getCraftTestData() {
      return this.craftTestData;
   }

   public String getInputsGroupName() {
      return this.inputsGroupName;
   }

   public String getResourceFluidID() {
      return this.resourceFluidID;
   }

   public float getBarrelConsumedAmount() {
      return this.barrelConsumedAmount;
   }

   protected void setBarrelConsumedAmount(float var1) {
      this.barrelConsumedAmount = var1;
   }

   public boolean isValid() {
      return super.isValid();
   }

   public String getRecipeTagQuery() {
      return this.recipeTagQuery;
   }

   public void setRecipeTagQuery(String var1) {
      if (this.recipeTagQuery == null || !this.recipeTagQuery.equalsIgnoreCase(var1)) {
         this.recipeTagQuery = var1;
         this.recipes = null;
         if (!StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
            this.recipes = CraftRecipeManager.queryRecipes(var1);
         }
      }

   }

   public List<CraftRecipe> getRecipes(List<CraftRecipe> var1) {
      var1.clear();
      if (this.recipes != null) {
         var1.addAll(this.recipes);
      }

      return var1;
   }

   protected List<CraftRecipe> getRecipes() {
      return this.recipes != null ? this.recipes : _emptyRecipeList;
   }

   public List<Resource> getInputResources(List<Resource> var1) {
      Resources var2 = (Resources)this.getComponent(ComponentType.Resources);
      if (var2 != null) {
         ResourceGroup var3 = var2.getResourceGroup(this.inputsGroupName);
         if (var3 != null) {
            return var3.getResources(var1, ResourceIO.Input);
         }
      }

      return _emptyResourceList;
   }

   public ResourceFluid getFluidBarrel() {
      Resources var1 = (Resources)this.getComponent(ComponentType.Resources);
      if (var1 != null) {
         var1.getResource(this.resourceFluidID);
      }

      return null;
   }

   public boolean isRunning() {
      return this.currentRecipe != null;
   }

   public boolean isFinished() {
      if (this.isRunning()) {
         return this.elapsedTime >= (double)this.currentRecipe.getTime();
      } else {
         return false;
      }
   }

   public CraftRecipe getCurrentRecipe() {
      return this.currentRecipe;
   }

   public double getProgress() {
      if (this.isRunning() && this.elapsedTime != 0.0) {
         return this.elapsedTime >= (double)this.currentRecipe.getTime() ? 1.0 : this.elapsedTime / (double)this.currentRecipe.getTime();
      } else {
         return 0.0;
      }
   }

   protected void setRecipe(CraftRecipe var1) {
      if (var1 == null) {
         this.clearRecipe();
      } else if (this.currentRecipe != var1) {
         this.clearRecipe();
         this.currentRecipe = var1;
         this.craftData.setRecipe(this.currentRecipe);
         this.craftTestData.setRecipe(this.currentRecipe);
      }

   }

   public CraftRecipe getPossibleRecipe() {
      List var1 = this.getInputResources(this.internalResourceList);
      return var1 != null && !var1.isEmpty() ? CraftUtil.getPossibleRecipe(this.craftTestData, this.recipes, var1, (List)null) : null;
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

   public CraftRecipeMonitor debugCanStart(IsoPlayer var1) {
      CraftRecipeMonitor var3;
      try {
         DebugLog.General.println("=== Starting debug canStart test ===");
         CraftRecipeMonitor var2 = CraftRecipeMonitor.Create();
         if (this.isValid()) {
            if (!this.owner.isUsingPlayer(var1)) {
               var2.warn("Player is not the using player.");
               var2.close();
               var3 = var2.seal();
               return var3;
            }

            var2.logMashingLogic(this);
            List var10 = this.getInputResources(this.internalResourceList);
            CraftRecipeMonitor var4;
            if (var10 != null && !var10.isEmpty()) {
               var2.logResources(var10, (List)null);
               var4 = CraftUtil.debugCanStart(var1, this.craftTestData, this.recipes, var10, (List)null, var2);
               return var4;
            }

            var2.warn("Inputs = " + var10 + ", size = " + (var10 != null ? var10.size() : "NaN") + ".");
            var2.close();
            var4 = var2.seal();
            return var4;
         }

         var2.warn("Unable to start (not valid).");
         var2.close();
         var3 = var2.seal();
      } catch (Exception var8) {
         var8.printStackTrace();
         return null;
      } finally {
         this.craftData.setMonitor((CraftRecipeMonitor)null);
         this.craftTestData.setMonitor((CraftRecipeMonitor)null);
      }

      return var3;
   }

   public boolean canStart(IsoPlayer var1) {
      return this.canStart(StartMode.Manual, var1);
   }

   protected boolean canStart(StartMode var1, IsoPlayer var2) {
      if (!this.isValid()) {
         return false;
      } else if (this.isRunning()) {
         return false;
      } else if (var2 != null && this.owner.isUsingPlayer(var2)) {
         List var3 = this.getInputResources(this.internalResourceList);
         if (var3 != null && !var3.isEmpty()) {
            return CraftUtil.canStart(this.craftTestData, this.recipes, var3, (List)null);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void start(IsoPlayer var1) {
      if (var1 != null && this.owner.isUsingPlayer(var1)) {
         if (GameClient.bClient) {
            if (this.canStart(StartMode.Manual, var1)) {
               this.sendStartRequest(var1);
            }
         } else {
            this.startRequested = true;
            this.requestingPlayer = var1;
         }

      }
   }

   public void stop(IsoPlayer var1) {
      this.stop(var1, false);
   }

   public void stop(IsoPlayer var1, boolean var2) {
      if (this.isValid()) {
         if (var2 || this.owner.isUsingPlayer(var1)) {
            if (GameClient.bClient) {
               this.sendStopRequest(var1);
            } else {
               this.stopRequested = true;
               this.requestingPlayer = var1;
            }

         }
      }
   }

   protected boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      switch (var2) {
         case MashingLogicStartRequest:
            this.receiveStartRequest(var1, var3);
            return true;
         case MashingLogicStopRequest:
            this.receiveStopRequest(var1, var3);
            return true;
         default:
            return false;
      }
   }

   public void sendStartRequest(IsoPlayer var1) {
      if (GameClient.bClient) {
         EntityPacketData var2 = GameEntityNetwork.createPacketData(EntityPacketType.MashingLogicStartRequest);
         var2.bb.put((byte)(var1 != null ? 1 : 0));
         if (var1 != null) {
            PlayerID var3 = new PlayerID();
            var3.set(var1);
            var3.write(var2.bb);
         }

         this.sendClientPacket(var2);
      }
   }

   protected void receiveStartRequest(ByteBuffer var1, UdpConnection var2) throws IOException {
      if (GameServer.bServer) {
         IsoPlayer var3 = null;
         if (var1.get() == 1) {
            PlayerID var4 = new PlayerID();
            var4.parse(var1, var2);
            var4.parsePlayer(var2);
            var3 = var4.getPlayer();
            if (var3 == null) {
               throw new IOException("Player not found.");
            }
         }

         this.start(var3);
      }
   }

   public void sendStopRequest(IsoPlayer var1) {
      if (GameClient.bClient) {
         EntityPacketData var2 = GameEntityNetwork.createPacketData(EntityPacketType.MashingLogicStopRequest);
         var2.bb.put((byte)(var1 != null ? 1 : 0));
         if (var1 != null) {
            PlayerID var3 = new PlayerID();
            var3.set(var1);
            var3.write(var2.bb);
         }

         this.sendClientPacket(var2);
      }
   }

   protected void receiveStopRequest(ByteBuffer var1, UdpConnection var2) throws IOException {
      if (GameServer.bServer) {
         IsoPlayer var3 = null;
         if (var1.get() == 1) {
            PlayerID var4 = new PlayerID();
            var4.parse(var1, var2);
            var4.parsePlayer(var2);
            var3 = var4.getPlayer();
            if (var3 == null) {
               throw new IOException("Player not found.");
            }
         }

         this.stop(var3);
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
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      if (this.recipeTagQuery != null) {
         var2.addFlags(1);
         GameWindow.WriteString(var1, this.recipeTagQuery);
      }

      if (this.inputsGroupName != null) {
         var2.addFlags(2);
         GameWindow.WriteString(var1, this.inputsGroupName);
      }

      if (this.resourceFluidID != null) {
         var2.addFlags(4);
         GameWindow.WriteString(var1, this.resourceFluidID);
      }

      if (this.currentRecipe != null) {
         var2.addFlags(8);
         GameWindow.WriteString(var1, this.currentRecipe.getScriptObjectFullType());
         var1.putLong(this.currentRecipe.getScriptVersion());
         var1.putDouble(this.elapsedTime);
         var1.putDouble(this.lastWorldAge);
         this.craftData.save(var1);
      }

      if (this.barrelConsumedAmount > 0.0F) {
         var2.addFlags(16);
         var1.putFloat(this.barrelConsumedAmount);
      }

      var2.write();
      var2.release();
   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      this.recipeTagQuery = null;
      this.inputsGroupName = null;
      this.resourceFluidID = null;
      this.barrelConsumedAmount = 0.0F;
      boolean var4 = false;
      String var5;
      if (var3.hasFlags(1)) {
         var5 = GameWindow.ReadString(var1);
         this.setRecipeTagQuery(var5);
      }

      if (var3.hasFlags(2)) {
         this.inputsGroupName = GameWindow.ReadString(var1);
      }

      if (var3.hasFlags(4)) {
         this.resourceFluidID = GameWindow.ReadString(var1);
      }

      if (var3.hasFlags(8)) {
         var5 = GameWindow.ReadString(var1);
         CraftRecipe var6 = ScriptManager.instance.getCraftRecipe(var5);
         this.setRecipe(var6);
         long var7 = var1.getLong();
         if (var6 == null || var7 != var6.getScriptVersion()) {
            var4 = true;
            DebugLog.General.warn("CraftRecipe '" + var5 + "' is null (" + (var6 == null) + ", or has script version mismatch. Cancelling current craft.");
         }

         this.elapsedTime = var1.getDouble();
         this.lastWorldAge = var1.getDouble();
         if (this.currentRecipe == null) {
            this.elapsedTime = 0.0;
            this.lastWorldAge = -1.0;
         }

         if (!this.craftData.load(var1, var2, var6, var4)) {
            var4 = true;
            this.craftData.setRecipe((CraftRecipe)null);
         }
      } else {
         this.setRecipe((CraftRecipe)null);
         this.elapsedTime = 0.0;
         this.lastWorldAge = -1.0;
      }

      if (var3.hasFlags(16)) {
         this.barrelConsumedAmount = var1.getFloat();
      }

      var3.release();
      if (var4) {
         this.clearRecipe();
      }

   }
}
