package slimeknights.tmechworks.common.blocks.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import slimeknights.mantle.tileentity.InventoryTileEntity;
import slimeknights.tmechworks.client.model.DisguiseBakedModel;
import slimeknights.tmechworks.common.blocks.RedstoneMachineBlock;
import slimeknights.tmechworks.common.inventory.DisguiseContainer;
import slimeknights.tmechworks.integration.waila.IInformationProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class RedstoneMachineTileEntity extends InventoryTileEntity implements ITickableTileEntity, IInformationProvider {
    private Inventory disguiseInventory;

    private int redstoneState;
    private boolean isFirstTick = true;

    public RedstoneMachineTileEntity(TileEntityType<?> type, ITextComponent name, int inventorySize) {
        this(type, name, inventorySize, 64);
    }

    public RedstoneMachineTileEntity(TileEntityType<?> type, ITextComponent name, int inventorySize, int maxStackSize) {
        super(type, name, inventorySize, maxStackSize);

        disguiseInventory = new Inventory(1) {
            @Override
            public boolean isItemValidForSlot(int index, ItemStack stack) {
                return stack.getItem() instanceof BlockItem;
            }

            @Override
            public int getInventoryStackLimit() {
                return 1;
            }
        };
    }

    /**
     * Updates redstone state
     */
    public void updateRedstone() {
        if (isFirstTick)
            return;

        Direction facing = getWorld().getBlockState(getPos()).get(RedstoneMachineBlock.FACING);
        int oldPow = redstoneState;

        Direction[] directions = Direction.values();
        int maxPow = 0;

        for(Direction dir : directions) {
            if(dir != facing){
                int pow = world.getRedstonePower(pos.offset(dir), dir);
                if(pow > maxPow)
                    maxPow = pow;
            }
        }

        int downPow = world.getRedstonePower(pos, Direction.DOWN);
        if(downPow > maxPow)
            maxPow = downPow;

        redstoneState = maxPow;

        if(maxPow != oldPow) {
            onRedstoneUpdate();
        }
        onBlockUpdate();

        if (oldPow != redstoneState) {
            sync();
        }
    }

    /**
     * Called when the redstone state changes
     */
    public void onRedstoneUpdate() {
    }

    /**
     * Called when redstone state is updated, but the redstone state remains unchanged
     */
    public void onBlockUpdate() {}

    /**
     * @return The redstone power level
     */
    public int getRedstoneState() {
        return redstoneState;
    }

    @Override
    public void tick() {
        if (isFirstTick) {
            isFirstTick = false;
            updateRedstone();
            init();
            sync();
        }
    }

    /**
     * Gets called the first tick this tile exists
     */
    public void init() {
        disguiseInventory.addListener(inv -> refreshDisguise());
    }

    public ItemStack getDisguiseBlock() {
        return disguiseInventory.getStackInSlot(0);
    }

    public void setDisguiseBlock(ItemStack disguise) {
        disguiseInventory.setInventorySlotContents(0, disguise);
    }

    public Inventory getDisguiseInventory() {
        return disguiseInventory;
    }

    public void refreshDisguise() {
        requestModelDataUpdate();
        markDirtyFast();

        BlockState state = getBlockState();
        ItemStack item = getDisguiseBlock();
        boolean hasDisguise = !item.isEmpty() && item.getItem() instanceof BlockItem;
        state = state.with(RedstoneMachineBlock.HAS_DISGUISE, hasDisguise);

        if(hasDisguise) {
            BlockState disguiseState = ((BlockItem)item.getItem()).getBlock().getDefaultState();

            state = state.with(RedstoneMachineBlock.LIGHT_VALUE, Math.max(disguiseState.getBlock().getLightValue(disguiseState, getWorld(), getPos()), disguiseState.getBlock().getLightValue(disguiseState)));
        }

        getWorld().setBlockState(getPos(), state);
        getWorld().notifyBlockUpdate(getPos(), state, state, 3);
    }

    /**
     * Writes inventory information
     */
    public CompoundNBT writeItemData(CompoundNBT tags) {
        tags.putInt("InventorySize", getSizeInventory());
        writeInventoryToNBT(tags);

        if(this.hasCustomName()) {
            tags.putString("CustomName", ITextComponent.Serializer.toJson(this.inventoryTitle));
        }

        ItemStack disguise = getDisguiseBlock();

        if (!disguise.isEmpty()) {
            CompoundNBT itemNBT = new CompoundNBT();

            itemNBT = disguise.write(itemNBT);

            tags.put("Disguise", itemNBT);
        }

        return tags;
    }

    /**
     * Reads inventory information
     */
    public void readItemData(CompoundNBT tags) {
        super.read(tags);

        if(tags.contains("Disguise")){
            CompoundNBT itemNBT = tags.getCompound("Disguise");

            ItemStack disguise = ItemStack.read(itemNBT);

            setDisguiseBlock(disguise);
        }
    }

    @Override
    @Nonnull
    public CompoundNBT write(CompoundNBT tags) {
        super.write(tags);
        tags = writeItemData(tags);

        tags.putInt("Redstone", redstoneState);

        return tags;
    }

    @Override
    public void read(CompoundNBT tags) {
        readItemData(tags);

        redstoneState = tags.getInt("Redstone");
    }

    @Override
    public void writeInventoryToNBT(CompoundNBT tag) {
        if(!isEmpty())
            super.writeInventoryToNBT(tag);
    }

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT tags = new CompoundNBT();

        write(tags);

        return new SUpdateTileEntityPacket(pos, 0, tags);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        CompoundNBT tags = pkt.getNbtCompound();

        handleUpdateTag(tags);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return write(new CompoundNBT());
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundNBT tag) {
        read(tag);

        // Mark block range for render update (if still needed)
    }

    public void sync() {
        markDirty();
        // Mark block range for render update (if still needed)

        if (EffectiveSide.get() == LogicalSide.SERVER) {
            SUpdateTileEntityPacket packetUpdateTileEntity = getUpdatePacket();

            if (packetUpdateTileEntity == null) {
                return;
            }

            for (PlayerEntity player : world.getPlayers()) {
                ((ServerPlayerEntity)player).connection.sendPacket(packetUpdateTileEntity);
            }
        }
    }

    /**
     * Stores information in an itemstack
     */
    public ItemStack storeTileData(ItemStack stack) {
        CompoundNBT tags = writeItemData(new CompoundNBT());

        stack.setTagInfo("BlockEntityTag", tags);

        if(this.hasCustomName()){
            CompoundNBT name = new CompoundNBT();
            name.putString("Name", ITextComponent.Serializer.toJson(inventoryTitle));

            stack.setTagInfo("display", name);
        }

        return stack;
    }

    @Nonnull
    @Override
    public IModelData getModelData() {
        return new ModelDataMap.Builder().withInitial(DisguiseBakedModel.DISGUISE, getDisguiseBlock()).build();
    }

    @Override
    public void getInformation(@Nonnull List<ITextComponent> info, InformationType type, PlayerEntity player) {
        if(type != InformationType.BODY)
            return;

        info.add(new TranslationTextComponent("tooltip.waila.power", getRedstoneState()));
    }

    @Override
    public void getInformation(@Nonnull List<ITextComponent> info, InformationType type, CompoundNBT serverData, PlayerEntity player) {
        getInformation(info, type, player);
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new DisguiseContainer(id, playerInventory, this);
    }
}
